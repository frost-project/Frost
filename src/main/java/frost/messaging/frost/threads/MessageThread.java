/*
  MessageDownloadThread.java / Frost
  Copyright (C) 2001  Frost Project <jtcfrost.sourceforge.net>

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License as
  published by the Free Software Foundation; either version 2 of
  the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package frost.messaging.frost.threads;

import java.io.File;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.identities.Identity;
import frost.identities.LocalIdentity;
import frost.messaging.frost.FrostMessageObject;
import frost.messaging.frost.FrostUnsentMessageObject;
import frost.messaging.frost.MessageXmlFile;
import frost.messaging.frost.SentMessagesManager;
import frost.messaging.frost.UnsentMessagesManager;
import frost.messaging.frost.boards.Board;
import frost.messaging.frost.boards.BoardUpdateInformation;
import frost.messaging.frost.boards.BoardUpdateThread;
import frost.messaging.frost.boards.BoardUpdateThreadObject;
import frost.messaging.frost.boards.TOF;
import frost.messaging.frost.transfer.MessageDownloader;
import frost.messaging.frost.transfer.MessageDownloaderResult;
import frost.messaging.frost.transfer.MessageUploader;
import frost.messaging.frost.transfer.MessageUploaderCallback;
import frost.messaging.frost.transfer.MessageUploaderResult;
import frost.storage.perst.IndexSlot;
import frost.storage.perst.IndexSlotsStorage;
import frost.util.DateFun;
import frost.util.FileAccess;
import frost.util.Mixed;

/**
 * Download and upload messages for a board.
 */
public class MessageThread extends BoardUpdateThreadObject implements BoardUpdateThread, MessageUploaderCallback {

	private static final Logger logger = LoggerFactory.getLogger(MessageThread.class);

    private final Board board;
    private final int maxMessageDownload;
    private final boolean downloadToday;

    public MessageThread(final boolean downloadToday, final Board boa, final int maxmsgdays) {
        super(boa);
        this.downloadToday = downloadToday;
        this.board = boa;
        this.maxMessageDownload = maxmsgdays;
    }

    public int getThreadType() {
        if (downloadToday) {
            return BoardUpdateThread.MSG_DNLOAD_TODAY;
        } else {
            return BoardUpdateThread.MSG_DNLOAD_BACK;
        }
    }

    @Override
    public void run() {

        notifyThreadStarted(this);

        try {
            String tofType;
            if (downloadToday) {
                tofType = "TOF Download";
            } else {
                tofType = "TOF Download Back";
            }

            // wait a max. of 5 seconds between start of threads
            Mixed.waitRandom(5000);

            logger.info("TOFDN: {} Thread started for board {}", tofType, board.getName());

            if (isInterrupted()) {
                notifyThreadFinished(this);
                return;
            }

			OffsetDateTime localDate = OffsetDateTime.now(DateFun.getTimeZone());
            final int boardId = board.getPerstFrostBoardObject().getBoardId();
            // start a thread if allowed,
            if (this.downloadToday) {
				final long dateMillis = DateFun.toStartOfDayInMilli(localDate);
                // get IndexSlot for today
                final IndexSlot gis = IndexSlotsStorage.inst().getSlotForDate(boardId, dateMillis);
                // download only current date
                final BoardUpdateInformation todayBui = downloadDate(localDate, gis, dateMillis);

                // after update, check if there are messages for upload and upload them
                // ... but only when we didn't stop because of too much invalid messages
                if( todayBui.isBoardUpdateAllowed() ) {
                    uploadMessages(gis); // doesn't get a message when message upload is disabled
                }
            } else {
                // download up to maxMessages days to the past
                int daysBack = 0;
                while (!isInterrupted() && daysBack < maxMessageDownload) {
                    daysBack++;
                    localDate = localDate.minusDays(1);
					final long dateMillis = DateFun.toStartOfDayInMilli(localDate);
                    final IndexSlot gis = IndexSlotsStorage.inst().getSlotForDate(boardId, dateMillis);
                    downloadDate(localDate, gis, dateMillis);
                    // Only after a complete backload run, remember finish time.
                    // this ensures we always update the complete backload days.
                    if( !isInterrupted() ) {
                        board.setLastBackloadUpdateFinishedMillis(System.currentTimeMillis());
                    }
                }
            }
            logger.info("TOFDN: {} Thread stopped for board {}", tofType, board.getName());
        } catch (final Throwable t) {
            logger.error("{}: Oo. Exception in MessageDownloadThread:", Thread.currentThread().getName(), t);
        }
        notifyThreadFinished(this);
    }

    protected String composeDownKey(final int index, final String dirdate) {
        String downKey = null;
        // switch public / secure board
        if (board.isPublicBoard() == false) {
            downKey = new StringBuilder()
                    .append(board.getPublicKey())
                    .append("/")
                    .append(board.getBoardFilename())
                    .append("/")
                    .append(dirdate)
                    .append("-")
                    .append(index)
                    .append(".xml")
                    .toString();
        } else {
            downKey = new StringBuilder()
                    .append("KSK@frost/message/")
                    .append(Core.frostSettings.getString(Settings.MESSAGE_BASE))
                    .append("/")
                    .append(dirdate)
                    .append("-")
                    .append(board.getBoardFilename())
                    .append("-")
                    .append(index)
                    .append(".xml")
                    .toString();
        }
        return downKey;
    }

	protected BoardUpdateInformation downloadDate(final OffsetDateTime localDate, final IndexSlot gis,
			final long dateMillis) {
		final String dirDateString = DateFun.FORMAT_DATE.format(localDate);

        final BoardUpdateInformation boardUpdateInformation = board.getOrCreateBoardUpdateInformationForDay(dirDateString, dateMillis);

        // new run, reset subsequentFailures
        boardUpdateInformation.resetSubsequentInvalidMsgs();

        int index = -1;
        int failures = 0;
        final int maxFailures = 2; // skip a maximum of 2 empty slots at the end of known indices

        while (failures < maxFailures) {

            if (isInterrupted()) {
                break;
            }

            // check if allowed state changed
            if( !boardUpdateInformation.checkBoardUpdateAllowedState() ) {
                break;
            }

            if( index < 0 ) {
                index = gis.findFirstDownloadSlot();
            } else {
                index = gis.findNextDownloadSlot(index);
            }

            String logInfo = null;

            try { // we don't want to die for any reason

                Mixed.waitRandom(2000); // don't hurt node

                final String downKey = composeDownKey(index, dirDateString);
                logInfo = new StringBuilder()
                            .append(" board=")
                            .append(board.getName())
                            .append(", key=")
                            .append(downKey)
                            .toString();

                final boolean quicklyFailOnAdnf;
                final int maxRetries;
                if( Core.frostSettings.getBoolean(Settings.FCP2_QUICKLY_FAIL_ON_ADNF) ) {
                    quicklyFailOnAdnf = true;
                    maxRetries = 2;
                } else {
                    // default
                    quicklyFailOnAdnf = false;
                    maxRetries = -1;
                }

                boardUpdateInformation.setCurrentIndex(index);
                notifyBoardUpdateInformationChanged(this, boardUpdateInformation);

                final long millisBefore = System.currentTimeMillis();

                final MessageDownloaderResult mdResult = MessageDownloader.downloadMessage(downKey, index, maxRetries, logInfo);

                boardUpdateInformation.incCountTriedIndices();
                boardUpdateInformation.addNodeTime(System.currentTimeMillis() - millisBefore);

                if( mdResult == null ) {
                    // file not found
                    if( gis.isDownloadIndexBehindLastSetIndex(index) ) {
                        // we stop if we tried maxFailures indices behind the last known index
                        failures++;
                    }
                    boardUpdateInformation.incCountDNF(); notifyBoardUpdateInformationChanged(this, boardUpdateInformation);
                    continue;
                }

                failures = 0;

                if( mdResult.isFailure()
                        && mdResult.getErrorMessage() != null
                        && mdResult.getErrorMessage().equals(MessageDownloaderResult.ALLDATANOTFOUND) )
                {
                    boardUpdateInformation.incCountADNF(); notifyBoardUpdateInformationChanged(this, boardUpdateInformation);
                    if( quicklyFailOnAdnf ) {
                        logger.warn("TOFDN: Index {} got ADNF, will never try this index again.", index);
                        gis.setDownloadSlotUsed(index);
                        IndexSlotsStorage.inst().storeSlot(gis); // remember each progress
                    } else {
                        // don't set slot used, try to retrieve the file again
                        logger.warn("TOFDN: Skipping index {} for now, will try again later.", index);
                    }
                    continue;
                }

                gis.setDownloadSlotUsed(index);

                if( mdResult.isFailure() ) {
                    // some error occured, don't try this file again
                    receivedInvalidMessage(board, localDate, index, mdResult.getErrorMessage());
                    boardUpdateInformation.incCountInvalid(); notifyBoardUpdateInformationChanged(this, boardUpdateInformation);
                } else if( mdResult.getMessage() != null ) {
                    // message is loaded, delete underlying received file
                    mdResult.getMessage().getFile().delete();
                    // basic validation, isValid() of FrostMessageObject was already called during instanciation of MessageXmlFile
                    if (isValidFormat(mdResult.getMessage(), localDate, board)) {
                        receivedValidMessage(
                                mdResult.getMessage(),
                                mdResult.getOwner(),
                                board,
                                index);

                        boardUpdateInformation.incCountValid();
                        boardUpdateInformation.updateMaxSuccessfulIndex(index);

                        notifyBoardUpdateInformationChanged(this, boardUpdateInformation);
                    } else {
                        receivedInvalidMessage(board, localDate, index, MessageDownloaderResult.INVALID_MSG);
                        logger.warn("TOFDN: Message was dropped, format validation failed: {}", logInfo);
                        boardUpdateInformation.incCountInvalid(); notifyBoardUpdateInformationChanged(this, boardUpdateInformation);
                    }
                }

                IndexSlotsStorage.inst().storeSlot(gis); // remember each progress

            } catch(final Throwable t) {
                logger.error("TOFDN: Exception thrown in downloadDate: {}", logInfo, t);
                // download failed, try next file
            }
        } // end-of: while

        boardUpdateInformation.setCurrentIndex(-1);
        boardUpdateInformation.updateBoardUpdateAllowedState();
        notifyBoardUpdateInformationChanged(this, boardUpdateInformation);

        return boardUpdateInformation;
    }

	private void receivedInvalidMessage(final Board b, final OffsetDateTime calDL, final int index,
			final String reason) {
		TOF.getInstance().receivedInvalidMessage(b, DateFun.toStartOfDay(calDL), index, reason);
	}

    private void receivedValidMessage(
            final MessageXmlFile mo,
            final Identity owner, // maybe null if unsigned
            final Board b,
            final int index)
    {
        TOF.getInstance().receivedValidMessage(mo, owner, b, index);
    }

    //////////////////////////////////////////////////
    ///  validation after receive
    //////////////////////////////////////////////////

    /**
     * First time verify.
     */
	public boolean isValidFormat(final MessageXmlFile mo, final OffsetDateTime dirDate, final Board b) {
        try {
			final OffsetDateTime dateTime;
            try {
                dateTime = mo.getDateAndTime();
            } catch(final Throwable ex) {
                logger.error("Exception in isValidFormat() - skipping message.", ex);
                return false;
            }

            // e.g. "E6936D085FC1AE75D43275161B50B0CEDB43716C1CE54E420F3C6FEB9352B462" (len=64)
            if( mo.getMessageId() == null || mo.getMessageId().length() < 60 || mo.getMessageId().length() > 68 ) {
                logger.error("Message has no unique message id - skipping Message: {}; {}", dirDate, dateTime);
                return false;
            }

            // ensure that time/date of msg is max. 1 day before/after dirDate
			final OffsetDateTime dm = DateFun.toStartOfDay(dateTime);
			if (dm.isAfter(DateFun.toStartOfDay(dirDate.plusDays(1)))
					|| dm.isBefore(DateFun.toStartOfDay(dirDate.minusDays(1)))) {
                logger.error("Invalid date - skipping Message: {}; {}", dirDate, dateTime);
                return false;
            }

            // ensure that board inside xml message is the board we currently download
            if( mo.getBoardName() == null ) {
                logger.error("No boardname in message - skipping message: (null)");
                return false;
            }
            final String boardNameInMsg = mo.getBoardName().toLowerCase();
            final String downloadingBoardName = b.getName().toLowerCase();
            if( boardNameInMsg.equals(downloadingBoardName) == false ) {
                logger.error("Different boardnames - skipping message: {}; {}", mo.getBoardName().toLowerCase(), b.getName().toLowerCase());
                return false;
            }

        } catch (final Throwable t) {
            logger.error("Exception in isValidFormat() - skipping message.", t);
            return false;
        }
        return true;
    }

    /**
     * Upload pending messages for this board.
     */
    protected void uploadMessages(final IndexSlot gis) {

        FrostUnsentMessageObject unsendMsg = UnsentMessagesManager.getUnsentMessage(board);
        if( unsendMsg == null ) {
            // currently no msg to send for this board
            return;
        }

        final String fromName = unsendMsg.getFromName();
        while( unsendMsg != null ) {

            // create a MessageXmlFile, sign, and send

            Identity recipient = null;
            if( unsendMsg.getRecipientName() != null && unsendMsg.getRecipientName().length() > 0) {
                recipient = Core.getIdentitiesManager().getIdentity(unsendMsg.getRecipientName());
                if( recipient == null ) {
                    logger.error("Can't send Message '{}', the recipient is not longer in your identites list!", unsendMsg.getSubject());
                    UnsentMessagesManager.deleteMessage(unsendMsg);
                    continue;
                }
            }

            UnsentMessagesManager.incRunningMessageUploads();

            uploadMessage(unsendMsg, recipient, gis);

            UnsentMessagesManager.decRunningMessageUploads();

            Mixed.waitRandom(5000); // wait some time

            // get next message to upload
            unsendMsg = UnsentMessagesManager.getUnsentMessage(board, fromName);
        }
    }

    private void uploadMessage(final FrostUnsentMessageObject mo, final Identity recipient, final IndexSlot gis) {

        logger.info("Preparing upload of message to board '{}'", board.getName());

        mo.setCurrentUploadThread(this);

        try {
            // prepare upload

            LocalIdentity senderId = null;
            if( mo.getFromName().indexOf("@") > 0 ) {
                // not anonymous
                if( mo.getFromIdentity() instanceof LocalIdentity ) {
                    senderId = (LocalIdentity) mo.getFromIdentity();
                } else {
                    // apparently the LocalIdentity used to write the msg was deleted
                    logger.error("The LocalIdentity used to write this unsent msg was deleted: {}", mo.getFromName());
                    mo.setCurrentUploadThread(null); // must be marked as not uploading before delete!
                    UnsentMessagesManager.deleteMessage(mo);
                    return;
                }
            }

            final MessageXmlFile message = new MessageXmlFile(mo);

			final OffsetDateTime now = OffsetDateTime.now(DateFun.getTimeZone());
            message.setDateAndTime(now);

            final File unsentMessageFile = FileAccess.createTempFile("unsendMsg", ".xml");
            message.setFile(unsentMessageFile);
            if (!message.save()) {
                logger.error("This was a HARD error and the file to upload is lost, please report to a dev!");
                mo.setCurrentUploadThread(null); // must be marked as not uploading before delete!
                return;
            }
            unsentMessageFile.deleteOnExit();

            // start upload, this signs and encrypts if needed

            final MessageUploaderResult result = MessageUploader.uploadMessage(
                    message,
                    recipient,
                    senderId,
                    this,
                    gis,
                    MainFrame.getInstance(),
                    board.getName());

            // file is not any longer needed
            message.getFile().delete();

            if( !result.isSuccess() ) {
                // upload failed, unsend message was handled by MessageUploader (kept or deleted, user choosed)
                mo.setCurrentUploadThread(null); // must be marked as not uploading before delete!
                if( !result.isKeepMessage() ) {
                    // user choosed to drop the message
                    UnsentMessagesManager.deleteMessage(mo);
                } else {
                    // user choosed to retry after next startup, dequeue message now and find it again on next startup
                    UnsentMessagesManager.dequeueMessage(mo);
                }
                return;
            }

            // success, store used slot
            IndexSlotsStorage.inst().storeSlot(gis);

            final int index = result.getUploadIndex();

            // upload was successful, store message in sentmessages database
            final FrostMessageObject sentMo = new FrostMessageObject(message, senderId, board, index);

            if( !SentMessagesManager.addSentMessage(sentMo) ) {
                // not added to gui, perst obj is not needed
                sentMo.setPerstFrostMessageObject(null);
            }

            // save own private messages into the message table
            if( sentMo.getRecipientName() != null && sentMo.getRecipientName().length() > 0 ) {
                // maybe sentMo has a perst obj set
                final FrostMessageObject moForMsgTable;
                if( sentMo.getPerstFrostMessageObject() != null ) {
                    // create a new FrostMessageObject
                    moForMsgTable = new FrostMessageObject(message, senderId, board, index);
                } else {
                    // reuseable
                    moForMsgTable = sentMo;
                }
                moForMsgTable.setSignatureStatusVERIFIED_V2();
                TOF.getInstance().receivedValidMessage(moForMsgTable, board, index);
            }

            // finally delete the message in unsend messages db table
            mo.setCurrentUploadThread(null); // must be marked as not uploading before delete!
            UnsentMessagesManager.deleteMessage(mo);

        } catch (final Throwable t) {
            logger.error("Catched exception", t);
        }
        mo.setCurrentUploadThread(null); // paranoia

        logger.info("Message upload finished");
    }

    /**
     * This method composes the downloading key for the message, given a
     * certain index number
     * @param index index number to use to compose the key
     * @return they composed key
     */
    public String composeDownloadKey(final MessageXmlFile message, final int index) {
        String key;
        if (board.isWriteAccessBoard()) {
            key = new StringBuilder()
                    .append(board.getPublicKey())
                    .append("/")
                    .append(board.getBoardFilename())
                    .append("/")
                    .append(message.getDateStr())
                    .append("-")
                    .append(index)
                    .append(".xml")
                    .toString();
        } else {
            key = new StringBuilder()
                    .append("KSK@frost/message/")
                    .append(Core.frostSettings.getString(Settings.MESSAGE_BASE))
                    .append("/")
                    .append(message.getDateStr())
                    .append("-")
                    .append(board.getBoardFilename())
                    .append("-")
                    .append(index)
                    .append(".xml")
                    .toString();
        }
        return key;
    }

    /**
     * This method composes the uploading key for the message, given a
     * certain index number
     * @param index index number to use to compose the key
     * @return they composed key
     */
    public String composeUploadKey(final MessageXmlFile message, final int index) {
        String key;
        if (board.isWriteAccessBoard()) {
            key = new StringBuilder()
                    .append(board.getPrivateKey())
                    .append("/")
                    .append(board.getBoardFilename())
                    .append("/")
                    .append(message.getDateStr())
                    .append("-")
                    .append(index)
                    .append(".xml")
                    .toString();
        } else {
            key = new StringBuilder()
                    .append("KSK@frost/message/")
                    .append(Core.frostSettings.getString(Settings.MESSAGE_BASE))
                    .append("/")
                    .append(message.getDateStr())
                    .append("-")
                    .append(board.getBoardFilename())
                    .append("-")
                    .append(index)
                    .append(".xml")
                    .toString();
        }
        return key;
    }
}
