/*
  MessageStorage.java / Frost
  Copyright (C) 2007  Frost Project <jtcfrost.sourceforge.net>

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
package frost.storage.perst.messages;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.garret.perst.GenericIndex;
import org.garret.perst.Index;
import org.garret.perst.Persistent;
import org.garret.perst.PersistentIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.messaging.frost.AbstractMessageStatusProvider;
import frost.messaging.frost.FileAttachment;
import frost.messaging.frost.FrostMessageObject;
import frost.messaging.frost.FrostUnsentMessageObject;
import frost.messaging.frost.boards.Board;
import frost.storage.ExitSavable;
import frost.storage.MessageArchivingCallback;
import frost.storage.MessageCallback;
import frost.storage.perst.AbstractFrostStorage;
import frost.util.DateFun;

public class MessageStorage extends AbstractFrostStorage implements ExitSavable {

	private static final Logger logger = LoggerFactory.getLogger(MessageStorage.class);

    private static final String STORAGE_FILENAME = "messages.dbs";

    public static final int INSERT_OK        = 1;
    public static final int INSERT_DUPLICATE = 2;
    public static final int INSERT_ERROR     = 3;

    public static final int SHOW_DEFAULT = 0;
    public static final int SHOW_UNREAD_ONLY = 1;
    public static final int SHOW_FLAGGED_ONLY = 2;
    public static final int SHOW_STARRED_ONLY = 3;

    private MessageStorageRoot storageRoot = null;

    private static MessageStorage instance = new MessageStorage();

    private final boolean storeInvalidMessages;

    protected MessageStorage() {
        super();
        storeInvalidMessages = Core.frostSettings.getBoolValue(Settings.STORAGE_STORE_INVALID_MESSAGES);
    }

    public static MessageStorage inst() {
        return instance;
    }

    @Override
    public String getStorageFilename() {
        return STORAGE_FILENAME;
    }

    @Override
    public boolean initStorage() {
        final String databaseFilePath = buildStoragePath(getStorageFilename()); // path to the database file
        final long pagePoolSize = getPagePoolSize(Settings.PERST_PAGEPOOLSIZE_MESSAGES);

        open(databaseFilePath, pagePoolSize, true, true, false);

        storageRoot = (MessageStorageRoot)getStorage().getRoot();
        if (storageRoot == null) {
            // Storage was not initialized yet
            storageRoot = new MessageStorageRoot(getStorage());
            getStorage().setRoot(storageRoot);
            commit(); // commit transaction
        }
        return true;
    }

    @Override
    public synchronized void commit() {
        // also commit the MessageContentStorage
        MessageContentStorage.inst().commit();
        super.commit();
    }

    @Override
    public boolean endThreadTransaction() {
        // also commit the MessageContentStorage, its part of the transaction
        MessageContentStorage.inst().commit();
        return super.endThreadTransaction();
    }

    public void exitSave() {
        close();
        storageRoot = null;
        logger.info("MessagesStorage closed.");
    }

    /**
     * Retrieve the primary key of the board, or insert it into storage.
     */
    public boolean assignPerstFrostBoardObject(final Board newNode) {
        if( !beginExclusiveThreadTransaction() ) {
            return false;
        }
        try {
            PerstFrostBoardObject pbo = storageRoot.getBoardsByName().get(newNode.getNameLowerCase());
            if( pbo == null ) {
                // not yet in perst, create new one
                addBoard(newNode);

                pbo = storageRoot.getBoardsByName().get(newNode.getNameLowerCase());
                if( pbo == null ) {
                    logger.error("board still not added!");
                    return false;
                }
            }

            newNode.setPerstFrostBoardObject(pbo);
            pbo.setRefBoard(newNode);

            return true;
        } finally {
            endThreadTransaction();
        }
    }

    /**
     * Adds a new board and returns the Board object with the perst object assigned.
     */
    public Board addBoard(final Board board) {

        if( board == null ) {
            return null;
        }

        if( !beginExclusiveThreadTransaction() ) {
            return null;
        }
        try {
            // prevent duplicate board names
            PerstFrostBoardObject pfbo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
            if( pfbo != null ) {
                board.setPerstFrostBoardObject(pfbo);
                return board; // dup!
            }

            final int boardId = storageRoot.getNextUniqueBoardId();

            pfbo = new PerstFrostBoardObject(getStorage(), board.getNameLowerCase(), boardId);
            storageRoot.getBoardsByName().put(board.getNameLowerCase(), pfbo);
            storageRoot.getBoardsById().put(boardId, pfbo);

            return board;
        } finally {
            endThreadTransaction();
        }
    }

    private void removeAll(final Iterator<? extends Persistent> i) {
        while(i.hasNext()) {
            i.next().deallocate();
        }
    }

    /**
     * Removes a board from the board list.
     */
    public void removeBoard(final Board board) {
        final PerstFrostBoardObject boardToRemove = board.getPerstFrostBoardObject();
        if( boardToRemove == null ) {
            return;
        }

        if( !beginExclusiveThreadTransaction() ) {
            return;
        }
        try {
            // delete ALL valid messages
            for( final PerstFrostMessageObject pmo : boardToRemove.getMessageIndex() ) {
                if( AbstractMessageStatusProvider.isSignatureStatusVERIFIED(pmo.signatureStatus) ) {
                    final PerstIdentitiesMessages pim = storageRoot.getIdentitiesMessages().get(pmo.fromName);
                    if( pim != null ) {
                        pim.getMessagesFromIdentity().remove(pmo);
                        if( pim.getMessagesFromIdentity().size() == 0 ) {
                            storageRoot.getIdentitiesMessages().remove(pmo.fromName);
                            pim.deallocate();
                        }
                    }
                }
                pmo.deallocate();
            }

            boardToRemove.getMessageIndex().clear();
            boardToRemove.getUnreadMessageIndex().clear();
            boardToRemove.getFlaggedMessageIndex().clear();
            boardToRemove.getStarredMessageIndex().clear();

            // delete ALL invalid messages
            removeAll(boardToRemove.getInvalidMessagesIndex().iterator());
            boardToRemove.getInvalidMessagesIndex().clear();

            // delete ALL sent and unsent messages
            removeAll(boardToRemove.getSentMessagesList().iterator());
            boardToRemove.getSentMessagesList().clear();
            removeAll(boardToRemove.getUnsentMessagesList().iterator());
            boardToRemove.getUnsentMessagesList().clear();
            removeAll(boardToRemove.getDraftMessagesList().iterator());
            boardToRemove.getDraftMessagesList().clear();
            storageRoot.getBoardsByName().remove(boardToRemove);
            storageRoot.getBoardsById().remove(boardToRemove);
            boardToRemove.deallocate();
        } finally {
            endThreadTransaction();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    public int getInvalidMessageCount() {
        if( !beginCooperativeThreadTransaction() ) {
            return -1;
        }
        try {
            int invalidMsgCount = 0;
            for( final PerstFrostBoardObject bo : storageRoot.getBoardsByName() ) {
                if( bo.getInvalidMessagesIndex() != null ) {
                    invalidMsgCount += bo.getInvalidMessagesIndex().size();
                }
            }
            return invalidMsgCount;
        } finally {
            endThreadTransaction();
        }
    }

    public int getMessageCount() {
        if( !beginCooperativeThreadTransaction() ) {
            return -1;
        }
        try {
            int msgCount = 0;
            for(final PerstFrostBoardObject bo : storageRoot.getBoardsByName()) {
                if( bo.getMessageIndex() != null ) {
                    msgCount += bo.getMessageIndex().size();
                }
            }
            return msgCount;
        } finally {
            endThreadTransaction();
        }
    }

    public int getMessageCount(final String uniqueIdentityName) {
        if( !beginCooperativeThreadTransaction() ) {
            return -1;
        }
        try {
            final PerstIdentitiesMessages pim = storageRoot.getIdentitiesMessages().get(uniqueIdentityName);
            if( pim != null ) {
                return pim.getMessagesFromIdentity().size();
            } else {
                return 0;
            }
        } finally {
            endThreadTransaction();
        }
    }

    /**
     * Returns count of all msgs AND all unread msgs (no matter how old they are).
     * If maxDaysBack is < 0 then ALL msgs for this board are counted.
     */
    public int getMessageCount(final Board board, final int maxDaysBack) {
        if( !beginCooperativeThreadTransaction() ) {
            return -1;
        }
        try {
            final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
            if( bo == null ) {
                return -1;
            }
            if( maxDaysBack < 0 ) {
                return bo.getMessageIndex().size();
            }

			final OffsetDateTime localDate = OffsetDateTime.now(DateFun.getTimeZone()).minusDays(maxDaysBack);
			final long minDateTime = DateFun.toStartOfDayInMilli(localDate);
            // normal messages in date range
            final Iterator<?> i1 = bo.getMessageIndex().iterator(minDateTime, Long.MAX_VALUE, GenericIndex.ASCENT_ORDER);
            // unread messages in range
            final Iterator<?> i2 = bo.getUnreadMessageIndex().iterator(minDateTime, Long.MAX_VALUE, GenericIndex.ASCENT_ORDER);
            // join all results
            final Iterator<?> i = getStorage().join( new Iterator<?>[] {i1, i2} );

            int count = 0;

            while(i.hasNext()) {
                ((PersistentIterator)i).nextOid();
                count++;
            }
            return count;
        } finally {
            endThreadTransaction();
        }
    }

    public int getUnreadMessageCount(final Board board) {
        if( !beginCooperativeThreadTransaction() ) {
            return -1;
        }
        try {
            final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
            if( bo == null ) {
                return -1;
            } else {
                // ALL new messages
                return bo.getUnreadMessageIndex().size();
            }
        } finally {
            endThreadTransaction();
        }
    }

    public int getFlaggedMessageCount(final Board board) {
        if( !beginCooperativeThreadTransaction() ) {
            return -1;
        }
        try {
            final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
            if( bo == null ) {
                return 0;
            } else {
                return bo.getFlaggedMessageIndex().size();
            }
        } finally {
            endThreadTransaction();
        }
    }

    public boolean hasFlaggedMessages(final Board board) {
        return (getFlaggedMessageCount(board) > 0);
    }

    public int getStarredMessageCount(final Board board) {
        if( !beginCooperativeThreadTransaction() ) {
            return -1;
        }
        try {
            final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
            if( bo == null ) {
                return 0;
            } else {
                return bo.getStarredMessageIndex().size();
            }
        } finally {
            endThreadTransaction();
        }
    }

    public boolean hasStarredMessages(final Board board) {
        return (getStarredMessageCount(board) > 0);
    }

    /**
     * Insert the message with an enclosing EXCLUSIVE transaction.
     */
    public int insertMessage(final FrostMessageObject mo) {
        return insertMessage(mo, true);
    }

	/**
	 * Insert the message directly, without an enclosing transaction.
	 */
    public int insertMessage(final FrostMessageObject mo, final boolean useTransaction) {
        if( useTransaction ) {
            if( !beginExclusiveThreadTransaction() ) {
                return INSERT_ERROR;
            }
        }
        // add to indices, check for duplicate msgId
        try {
            if( mo.getPerstFrostMessageObject() != null ) {
                logger.error("msgInsertError: perst obj already set");
                return INSERT_ERROR; // skip msg
            }
            final Board targetBoard = mo.getBoard();
            if( targetBoard == null ) {
                logger.error("msgInsertError: no board in msg");
                return INSERT_ERROR; // skip msg
            }
            if( !storeInvalidMessages && !mo.isValid() ) {
                // don't store invalid messages, they are usually not needed
                return INSERT_OK;
            }
            PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(targetBoard.getNameLowerCase());
            if( bo == null ) {
                // create new perst board
                logger.info("Creating new perst board: {}", targetBoard.getName());
                addBoard(targetBoard);
                bo = storageRoot.getBoardsByName().get(targetBoard.getNameLowerCase());
                if( bo == null ) {
                    logger.error("duplicate board???");
                    return INSERT_ERROR;
                }
            }
            final PerstFrostMessageObject pmo = new PerstFrostMessageObject(mo, getStorage(), useTransaction);
            if( !mo.isValid() ) {
                // invalid message
				bo.getInvalidMessagesIndex().put(DateFun.toMilli(mo.getDateAndTime()), pmo);
            } else {
                if( mo.getMessageId() != null ) {
                    if( !bo.getMessageIdIndex().put(mo.getMessageId(), pmo) ) {
                        // duplicate messageId!
                        return INSERT_DUPLICATE; // skip msg
                    }
                }

                mo.setPerstFrostMessageObject(pmo);

				bo.getMessageIndex().put(DateFun.toMilli(mo.getDateAndTime()), pmo);
                if( pmo.isNew ) {
					bo.getUnreadMessageIndex().put(DateFun.toMilli(mo.getDateAndTime()), pmo);
                }
                if( pmo.isFlagged ) {
					bo.getFlaggedMessageIndex().put(DateFun.toMilli(mo.getDateAndTime()), pmo);
                }
                if( pmo.isStarred ) {
					bo.getStarredMessageIndex().put(DateFun.toMilli(mo.getDateAndTime()), pmo);
                }

                // add to id, maybe create id for this msg
                if( AbstractMessageStatusProvider.isSignatureStatusVERIFIED(pmo.signatureStatus) ) {
                    PerstIdentitiesMessages pim = storageRoot.getIdentitiesMessages().get(pmo.fromName);
                    if( pim == null ) {
                        pim = new PerstIdentitiesMessages(pmo.fromName, getStorage());
                        storageRoot.getIdentitiesMessages().put(pmo.fromName, pim);
                    }
                    pim.getMessagesFromIdentity().add(pmo);
                }
            }
            return INSERT_OK;
        } finally {
            if( useTransaction ) {
                endThreadTransaction();
            }
        }
    }

    public FrostMessageObject retrieveMessageByMessageId(
            final Board board,
            final String msgId,
            final boolean withContent,
            final boolean withAttachments,
            final boolean showDeleted)
    {
        if( !beginCooperativeThreadTransaction() ) {
            return null;
        }
        try {
            final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
            if( bo == null ) {
                return null;
            }

            final PerstFrostMessageObject p = bo.getMessageIdIndex().get(msgId);
            if( p == null ) {
                return null;
            }
            if(!showDeleted && p.isDeleted) {
                return null;
            }
            final FrostMessageObject mo = p.toFrostMessageObject(board, withContent, withAttachments);
            return mo;
        } finally {
            endThreadTransaction();
        }
    }

    public void retrieveMessageContent(final FrostMessageObject mo) {
        if( mo.getPerstFrostMessageObject() != null ) {
            mo.getPerstFrostMessageObject().retrieveMessageContent(mo);
        }
    }

    public void retrievePublicKey(final FrostMessageObject mo) {
        if( mo.getPerstFrostMessageObject() != null ) {
            mo.getPerstFrostMessageObject().retrievePublicKey(mo);
        }
    }

    public void retrieveSignature(final FrostMessageObject mo) {
        if( mo.getPerstFrostMessageObject() != null ) {
            mo.getPerstFrostMessageObject().retrieveSignature(mo);
        }
    }

    public void retrieveAttachments(final FrostMessageObject mo) {
        if( mo.getPerstFrostMessageObject() != null ) {
            mo.getPerstFrostMessageObject().retrieveAttachments(mo);
        }
    }

    /**
     * Runs during startup only, does not need transaction locking.
     */
    public void retrieveMessagesForArchive(
            final Board board,
            final int maxDaysOld,
            final boolean archiveKeepUnread,
            final boolean archiveKeepFlaggedAndStarred,
            final MessageArchivingCallback mc)
    {
		final OffsetDateTime localDate = OffsetDateTime.now(DateFun.getTimeZone()).minusDays(maxDaysOld);
		final long maxDateTime = DateFun.toStartOfDayInMilli(localDate);

        final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
        if( bo == null ) {
            logger.error("no perst board for archive");
            return;
        }
        // normal messages in date range
        final Iterator<PerstFrostMessageObject> i = bo.getMessageIndex().iterator(Long.MIN_VALUE, maxDateTime, GenericIndex.ASCENT_ORDER);
        while(i.hasNext()) {
            final PerstFrostMessageObject p = i.next();
            if( archiveKeepUnread && p.isNew) {
                continue;
            }
            if( archiveKeepFlaggedAndStarred && (p.isFlagged || p.isStarred) ) {
                continue;
            }
            final FrostMessageObject mo = p.toFrostMessageObject(board, false, false);
            final int mode = mc.messageRetrieved(mo);
            if( mode == MessageArchivingCallback.STOP_ERROR ) {
                return;
            } else if( mode == MessageArchivingCallback.DELETE_MESSAGE ) {
                // delete msg and internal perst objs, also remove from indices and maybe remove empty identitiesMessages

                i.remove();

                if( p.isNew) {
                    bo.getUnreadMessageIndex().remove(p.dateAndTime, p);
                }
                if( p.isFlagged ) {
                    bo.getFlaggedMessageIndex().remove(p.dateAndTime, p);
                }
                if( p.isStarred ) {
                    bo.getStarredMessageIndex().remove(p.dateAndTime, p);
                }

                if( mo.isSignatureStatusVERIFIED() ) {
                    final PerstIdentitiesMessages pim = storageRoot.getIdentitiesMessages().get(p.fromName);
                    if( pim != null ) {
                        pim.getMessagesFromIdentity().remove(p);
                        if( pim.getMessagesFromIdentity().size() == 0 ) {
                            storageRoot.getIdentitiesMessages().remove(p.fromName);
                            pim.deallocate();
                        }
                    }
                }

                if( p.messageId != null ) {
                    bo.getMessageIdIndex().remove(p.messageId);
                }

                p.deallocate();
            }
        }

        // delete invalid messages in date range
        final Iterator<PerstFrostMessageObject> ii = bo.getInvalidMessagesIndex().iterator(Long.MIN_VALUE, maxDateTime, GenericIndex.ASCENT_ORDER);
        while(ii.hasNext()) {
            final PerstFrostMessageObject p = ii.next();
            ii.remove();
            p.deallocate();
        }
    }

    public void retrieveMessagesForSearch(
            final Board board,
            final long startDate,
            final long endDate,
            final boolean searchInDisplayedMessages,
            final boolean withContent,
            final boolean withAttachments,
            final boolean showDeleted,
            final MessageCallback mc)
    {
        if( !beginCooperativeThreadTransaction() ) {
            return;
        }
        try {
            final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
            if( bo == null ) {
                logger.error("no perst board for search");
                return;
            }

            final Iterator<?> i;

            // normal messages in date range
            final Iterator<?> i1 = bo.getMessageIndex().iterator(startDate, endDate, GenericIndex.ASCENT_ORDER);

            if( searchInDisplayedMessages ) {
                // add ALL unread messages, also those which are not in date range
                final Iterator<?> i2 = bo.getUnreadMessageIndex().iterator();
                // add ALL flagged and starred messages, also those which are not in date range
                final Iterator<?> i3 = bo.getStarredMessageIndex().iterator();
                final Iterator<?> i4 = bo.getFlaggedMessageIndex().iterator();

                // join all results
                i = getStorage().join(new Iterator<?>[] {i1, i2, i3, i4} );
            } else {
                i = i1;
            }

            while(i.hasNext()) {
                final PerstFrostMessageObject p = (PerstFrostMessageObject) i.next();
                if(!showDeleted && p.isDeleted) {
                    continue;
                }
                final FrostMessageObject mo = p.toFrostMessageObject(board, withContent, withAttachments);
                final boolean shouldStop = mc.messageRetrieved(mo);
                if( shouldStop ) {
                    break;
                }
            }
        } finally {
            endThreadTransaction();
        }
    }

	public OffsetDateTime getDateTimeOfLatestMessage(final Board board) {
        if( !beginCooperativeThreadTransaction() ) {
            return null;
        }
        try {
            final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
            if( bo == null ) {
                logger.error("no perst board for show");
                return null;
            }

            final Iterator<PerstFrostMessageObject> i = bo.getMessageIndex().iterator(null, Long.MAX_VALUE, GenericIndex.DESCENT_ORDER);

            if (i.hasNext()) {
                final PerstFrostMessageObject p = i.next();
                return p.getDateTime();
            }
        } finally {
            endThreadTransaction();
        }
        return null;
    }

    public void retrieveMessagesForShow(
            final Board board,
            final int maxDaysBack,
            final boolean withContent,
            final boolean withAttachments,
            final boolean showDeleted,
            final int whatToShow,
            final MessageCallback mc)
    {
		final OffsetDateTime localDate = OffsetDateTime.now(DateFun.getTimeZone()).minusDays(maxDaysBack);
		final long minDateTime = DateFun.toStartOfDayInMilli(localDate);

        if( !beginCooperativeThreadTransaction() ) {
            return;
        }
        try {
            final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
            if( bo == null ) {
                logger.error("no perst board for show");
                return;
            }

            Iterator<?> i;
            if( whatToShow == SHOW_UNREAD_ONLY ) {
                // ALL new messages
                i = bo.getUnreadMessageIndex().iterator();
            } else if( whatToShow == SHOW_FLAGGED_ONLY ) {
                i = bo.getFlaggedMessageIndex().iterator();
            } else if( whatToShow == SHOW_STARRED_ONLY ) { 
                i = bo.getStarredMessageIndex().iterator();
            } else {
                // normal messages in date range
                final Iterator<?> i1 = bo.getMessageIndex().iterator(minDateTime, Long.MAX_VALUE, GenericIndex.ASCENT_ORDER);
                // add ALL unread messages, also those which are not in date range
                final Iterator<?> i2 = bo.getUnreadMessageIndex().iterator();
                // add ALL flagged and starred messages, also those which are not in date range
                final Iterator<?> i3 = bo.getStarredMessageIndex().iterator();
                final Iterator<?> i4 = bo.getFlaggedMessageIndex().iterator();

                // join all results
                i = getStorage().join(new Iterator<?>[] {i1, i2, i3, i4} );
            }

            while(i.hasNext()) {
                final PerstFrostMessageObject p = (PerstFrostMessageObject) i.next();
                if(!showDeleted && p.isDeleted) {
                    continue;
                }
                final FrostMessageObject mo = p.toFrostMessageObject(board, withContent, withAttachments);
                final boolean shouldStop = mc.messageRetrieved(mo);
                if( shouldStop ) {
                    break;
                }
            }
        } finally {
            endThreadTransaction();
        }
    }

    public void setAllMessagesRead(final Board board) {
        if( !beginExclusiveThreadTransaction() ) {
            return;
        }
        try {
            final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
            if( bo == null ) {
                logger.error("no perst board for update");
                return;
            }
            final Iterator<PerstFrostMessageObject> i = bo.getUnreadMessageIndex().iterator();
            while(i.hasNext()) {
                final PerstFrostMessageObject pmo = i.next();
                pmo.isNew = false;
                pmo.modify();
            }
            bo.getUnreadMessageIndex().clear();
        } finally {
            endThreadTransaction();
        }
    }

    public void setMessagesRead(final Board board, final List<FrostMessageObject> msgs) {
        if( msgs == null || msgs.size() == 0 ) {
            return;
        }
        if( !beginExclusiveThreadTransaction() ) {
            return;
        }
        try {
            final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
            if( bo == null ) {
                logger.error("no perst board for update");
                return;
            }

            for( final FrostMessageObject mo : msgs ) {
                final PerstFrostMessageObject pmo = mo.getPerstFrostMessageObject();
                if( pmo == null ) {
                    logger.error("no perst obj in msg");
                    continue;
                }
                if( pmo.isNew ) {
                    // was unread, is read now -> remove from unreadIndex
                    mo.setNew(false);
                    pmo.isNew = false;
                    bo.getUnreadMessageIndex().remove(pmo.dateAndTime, pmo);
                    pmo.modify();
                }
            }
        } finally {
            endThreadTransaction();
        }
    }

    public void updateMessage(final FrostMessageObject mo) {
        updateMessage(mo, true);
    }

    public void updateMessage(final FrostMessageObject mo, final boolean useTransaction) {
        if( mo.getPerstFrostMessageObject() == null ) {
            return;
        }
        if( useTransaction ) {
            if( !beginExclusiveThreadTransaction() ) {
                return;
            }
        }
        try {
            final PerstFrostBoardObject bo = storageRoot.getBoardsByName().get(mo.getBoard().getNameLowerCase());
            if( bo == null ) {
                logger.error("no perst board for update");
                return;
            }

            final PerstFrostMessageObject p = mo.getPerstFrostMessageObject();

            if( p.isNew && !mo.isNew() ) {
                // was unread, is read now -> remove from unreadIndex
                bo.getUnreadMessageIndex().remove(p.dateAndTime, p);
            } else if( !p.isNew && mo.isNew() ) {
                // was read, is unread now -> add to unreadIndex
                bo.getUnreadMessageIndex().put(p.dateAndTime, p);
            }

            if( p.isFlagged && !mo.isFlagged() ) {
                // was flagged, is not flagged now -> remove from flaggedIndex
                bo.getFlaggedMessageIndex().remove(p.dateAndTime, p);
            } else if( !p.isFlagged && mo.isFlagged() ) {
                // was not flagged, is flagged now -> add to flaggedIndex
                bo.getFlaggedMessageIndex().put(p.dateAndTime, p);
            }

            if( p.isStarred && !mo.isStarred() ) {
                // was starred, is not starred now -> remove from starredIndex
                bo.getStarredMessageIndex().remove(p.dateAndTime, p);
            } else if( !p.isStarred && mo.isStarred() ) {
                // was not starred, is starred now -> add to starredIndex
                bo.getStarredMessageIndex().put(p.dateAndTime, p);
            }

            p.isDeleted = mo.isDeleted();
            p.isNew = mo.isNew();
            p.isReplied = mo.isReplied();
            p.isJunk = mo.isJunk();
            p.isFlagged = mo.isFlagged();
            p.isStarred = mo.isStarred();

            p.modify();
        } finally {
            if( useTransaction ) {
                endThreadTransaction();
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    public List<FrostMessageObject> retrieveAllSentMessages() {
        final List<FrostMessageObject> lst = new ArrayList<FrostMessageObject>();
        if( !beginCooperativeThreadTransaction() ) {
            return lst;
        }
        try {
            for( final PerstFrostBoardObject bo : storageRoot.getBoardsByName() ) {
                for( final PerstFrostMessageObject pmo : bo.getSentMessagesList() ) {
                    final FrostMessageObject mo = pmo.toFrostMessageObject(bo.getRefBoard(), false, false);
                    lst.add(mo);
                }
            }
            return lst;
        } finally {
            endThreadTransaction();
        }
    }

    /**
     * Insert the sent message with an enclosing EXCLUSIVE transaction.
     */
    public boolean insertSentMessage(final FrostMessageObject sentMo) {
        return insertSentMessage(sentMo, true);
    }

	/**
	 * Insert the message directly, without an enclosing transaction.
	 */
    public boolean insertSentMessage(final FrostMessageObject sentMo, final boolean useTransaction) {
        if( useTransaction ) {
            if( !beginExclusiveThreadTransaction() ) {
                return false;
            }
        }
        try {
            final PerstFrostBoardObject bo = sentMo.getBoard().getPerstFrostBoardObject();
            if( bo == null ) {
                logger.error("no board for new sent msg!");
                return false;
            }
            final PerstFrostMessageObject pmo = new PerstFrostMessageObject(sentMo, getStorage(), useTransaction);
            sentMo.setPerstFrostMessageObject(pmo);

            bo.getSentMessagesList().add( pmo );
            return true;
        } finally {
            if( useTransaction ) {
                endThreadTransaction();
            }
        }
    }

    public int deleteSentMessages(final List<FrostMessageObject> msgObjects) {
        int count = 0;
        if( !beginExclusiveThreadTransaction() ) {
            return 0;
        }
        try {
        	final Index<PerstFrostBoardObject> boardsByName = storageRoot.getBoardsByName();
            for( final FrostMessageObject mo : msgObjects ) {
                if (mo.getPerstFrostMessageObject() == null || mo.getBoard() == null) {
                    logger.error("delete not possible");
                    continue;
                }
                final PerstFrostBoardObject bo = boardsByName.get(mo.getBoard().getNameLowerCase());
                if( bo == null ) {
                    logger.error("board not found");
                    continue;
                }
                bo.getSentMessagesList().remove(mo.getPerstFrostMessageObject());
                mo.getPerstFrostMessageObject().deallocate();
                mo.setPerstFrostMessageObject(null);
                count++;
            }
            return count;
        } finally {
            endThreadTransaction();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    public List<FrostUnsentMessageObject> retrieveAllUnsentMessages() {
        final List<FrostUnsentMessageObject> lst = new ArrayList<FrostUnsentMessageObject>();
        if( !beginCooperativeThreadTransaction() ) {
            return lst;
        }
        try {
            for( final PerstFrostBoardObject bo : storageRoot.getBoardsByName() ) {
                for( final PerstFrostUnsentMessageObject pmo : bo.getUnsentMessagesList() ) {
                    final FrostUnsentMessageObject mo = pmo.toFrostUnsentMessageObject(bo.getRefBoard());
                    lst.add(mo);
                }
            }
            return lst;
        } finally {
            endThreadTransaction();
        }
    }

    /**
     * Insert the message with an enclosing EXCLUSIVE transaction.
     */
    public boolean insertUnsentMessage(final FrostUnsentMessageObject mo) {
        return insertUnsentMessage(mo, true);
    }

    /**
     * Insert the unsent message directly, without an enclosing transaction.
     */
    public boolean insertUnsentMessage(final FrostUnsentMessageObject mo, final boolean useTransaction) {
        if( useTransaction ) {
            if( !beginExclusiveThreadTransaction() ) {
                return false;
            }
        }
        try {
            final PerstFrostBoardObject bo = mo.getBoard().getPerstFrostBoardObject();
            if( bo == null ) {
                logger.error("no board for new unsent msg!");
                return false;
            }
            final PerstFrostUnsentMessageObject pmo = new PerstFrostUnsentMessageObject(getStorage(), mo);
            bo.getUnsentMessagesList().add(pmo);
            return true;
        } finally {
            if( useTransaction ) {
                endThreadTransaction();
            }
        }
    }

    public boolean deleteUnsentMessage(final FrostUnsentMessageObject mo) {
        final PerstFrostBoardObject bo = mo.getBoard().getPerstFrostBoardObject();
        if( bo == null ) {
            logger.error("no board for unsent msg!");
            return false;
        }
        final PerstFrostUnsentMessageObject pmo = mo.getPerstFrostUnsentMessageObject();
        if( pmo == null ) {
            logger.error("no perst unsent msg obj!");
            return false;
        }
        if( !beginExclusiveThreadTransaction() ) {
            return false;
        }
        try {
            bo.getUnsentMessagesList().remove( pmo );
            pmo.deallocate();
            return true;
        } finally {
            endThreadTransaction();
        }
    }

    /**
     * Updates the CHK keys of fileattachments after upload of attachments.
     */
    public void updateUnsentMessageFileAttachmentKey(final FrostUnsentMessageObject mo, final FileAttachment fa) {

        final PerstFrostBoardObject bo = mo.getBoard().getPerstFrostBoardObject();
        if( bo == null ) {
            logger.error("no board for new unsent msg update!");
            return;
        }
        final PerstFrostUnsentMessageObject pmo = mo.getPerstFrostUnsentMessageObject();
        if( pmo == null ) {
            logger.error("no perst unsent msg obj for update!");
            return;
        }
        if( !beginExclusiveThreadTransaction() ) {
            return;
        }
        try {
            pmo.updateUnsentMessageFileAttachmentKey(fa);
        } finally {
            endThreadTransaction();
        }
    }
}
