/*
  UnsentMessagesManager.java / Frost
  Copyright (C) 2006  Frost Project <jtcfrost.sourceforge.net>

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
package frost.messaging.frost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.messaging.frost.boards.Board;
import frost.messaging.frost.threads.FileAttachmentUploadThread;
import frost.storage.perst.messages.MessageStorage;

/**
 * Holds unsent messages, makes changes persistent.
 */
public class UnsentMessagesManager {

    private static final LinkedList<FrostUnsentMessageObject> unsentMessages = new LinkedList<FrostUnsentMessageObject>();
    private static int runningMessageUploads = 0;

    /**
     * Retrieves all unsend messages from database table.
     */
    public static void initialize() {
        List<FrostUnsentMessageObject> msgs;

        msgs = MessageStorage.inst().retrieveAllUnsentMessages();

        if( (msgs == null) || (msgs.size() == 0) ) {
            return;
        }

        unsentMessages.addAll(msgs);

        // initialize the file attachments to upload
        for( final FrostUnsentMessageObject msg : unsentMessages ) {
            FileAttachmentUploadThread.getInstance().checkAndEnqueueNewMessage(msg);
        }

        MainFrame.getInstance().updateTofTree( MainFrame.getInstance().getMessagingTab().getTofTree().getUnsentMessagesFolder() );
    }

    public static int getUnsentMessageCount() {
        return unsentMessages.size();
    }

    public static List<FrostUnsentMessageObject> getUnsentMessages() {
        return unsentMessages;
    }

    /**
     * Returns a message to upload. The message must have no unsend file attachments.
     * When a message is returned it is dequeued.
     * @param targetBoard  target board for the message
     * @return  a message, or null
     */
    public static FrostUnsentMessageObject getUnsentMessage(final Board targetBoard) {
        return getUnsentMessage(targetBoard, null);
    }

    /**
     * Returns a message to upload. The message must have no unsend file attachments.
     * Takes care that the returned message is from the same userName as specified,
     * because we don't want to send messages from different userNames together,
     * this compromises anonymity!
     * @param targetBoard  target board for the message
     * @return  a message, or null
     */
    public static FrostUnsentMessageObject getUnsentMessage(final Board targetBoard, final String fromName) {

        if( Core.frostSettings.getBoolean(Settings.MESSAGE_UPLOAD_DISABLED) ) {
            return null;
        }

        for( final FrostUnsentMessageObject mo : unsentMessages ) {
            if( mo.getCurrentUploadThread() != null ) {
                continue; // msg is currently uploading
            }
            if( mo.getBoard().getPerstFrostBoardObject().getBoardId() ==
                    targetBoard.getPerstFrostBoardObject().getBoardId() )
            {
                if( (fromName == null) || fromName.equals(mo.getFromName()) ) {
                    if( mo.getUnsentFileAttachments().size() == 0 ) {
                        return mo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a List of all Boards that currently have sendable messages.
     */
    public static List<Board> getBoardsWithSendableMessages() {

        if( Core.frostSettings.getBoolean(Settings.MESSAGE_UPLOAD_DISABLED) ) {
            return Collections.emptyList();
        }

        final Hashtable<Integer,Board> ht = new Hashtable<Integer,Board>();
        for( final FrostUnsentMessageObject mo : unsentMessages ) {
            if( mo.getCurrentUploadThread() != null ) {
                continue; // msg is currently uploading
            }
            // skip boards that are currently DoSed, we try them during the board update loop
            if( mo.getBoard().isDosForToday() ) {
                continue;
            }
            if( !ht.containsKey(mo.getBoard().getPerstFrostBoardObject().getBoardId()) ) {
                ht.put(mo.getBoard().getPerstFrostBoardObject().getBoardId(), mo.getBoard());
            }
        }
        final List<Board> result = new ArrayList<Board>(ht.values());
        return result;
    }

    public static void addNewUnsentMessage(final FrostUnsentMessageObject mo) {

        mo.setTimeAdded(System.currentTimeMillis());

        MessageStorage.inst().insertUnsentMessage(mo);

        unsentMessages.add(mo);

        // enqueue in file attachment upload thread if needed
        FileAttachmentUploadThread.getInstance().checkAndEnqueueNewMessage(mo);

        MainFrame.getInstance().getMessagingTab().getUnsentMessagesPanel().addUnsentMessage(mo);
        MainFrame.getInstance().updateTofTree( MainFrame.getInstance().getMessagingTab().getTofTree().getUnsentMessagesFolder() );
    }

    /**
     * @return  false if message is currently uploading and delete is not possible
     */
    public static boolean deleteMessage(final FrostUnsentMessageObject unsentMsg) {

        if( unsentMsg.getCurrentUploadThread() != null ) {
            return false; // msg currently uploaded, delete not possible
        }

        MessageStorage.inst().deleteUnsentMessage(unsentMsg);

        for(final Iterator<FrostUnsentMessageObject> i = unsentMessages.iterator(); i.hasNext(); ) {
            final FrostUnsentMessageObject mo = i.next();
            if( unsentMsg.getMessageId().equals(mo.getMessageId()) ) {
                i.remove();
                break;
            }
        }


        FileAttachmentUploadThread.getInstance().messageWasDeleted(unsentMsg.getMessageId());

        MainFrame.getInstance().getMessagingTab().getUnsentMessagesPanel().removeUnsentMessage(unsentMsg);
        MainFrame.getInstance().updateTofTree( MainFrame.getInstance().getMessagingTab().getTofTree().getUnsentMessagesFolder() );

        return true;
    }

    public static boolean dequeueMessage(final FrostUnsentMessageObject unsentMsg) {

        for(final Iterator<FrostUnsentMessageObject> i = unsentMessages.iterator(); i.hasNext(); ) {
            final FrostUnsentMessageObject mo = i.next();
            if( unsentMsg.getMessageId().equals(mo.getMessageId()) ) {
                i.remove();
                break;
            }
        }

        MainFrame.getInstance().getMessagingTab().getUnsentMessagesPanel().removeUnsentMessage(unsentMsg);
        MainFrame.getInstance().updateTofTree( MainFrame.getInstance().getMessagingTab().getTofTree().getUnsentMessagesFolder() );

        return true;
    }

    public static void updateMessageFileAttachmentKey(final FrostUnsentMessageObject mo, final FileAttachment fa) {
        MessageStorage.inst().updateUnsentMessageFileAttachmentKey(mo, fa);
    }

    public synchronized static int getRunningMessageUploads() {
        return runningMessageUploads;
    }
    public synchronized static void incRunningMessageUploads() {
        runningMessageUploads++;
    }
    public synchronized static void decRunningMessageUploads() {
        runningMessageUploads--;
    }
}
