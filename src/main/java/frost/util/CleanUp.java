/*
 CleanUp.java / Frost
 Copyright (C) 2003  Frost Project <jtcfrost.sourceforge.net>

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

package frost.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.gui.Splashscreen;
import frost.messaging.frost.FrostMessageObject;
import frost.messaging.frost.boards.Board;
import frost.storage.MessageArchivingCallback;
import frost.storage.perst.IndexSlotsStorage;
import frost.storage.perst.SharedFilesCHKKeyStorage;
import frost.storage.perst.filelist.FileListStorage;
import frost.storage.perst.messagearchive.ArchiveMessageStorage;
import frost.storage.perst.messages.MessageStorage;

/**
 * Expire messages and cleans several database tables.
 * Is only called during startup of frost, never during runtime.
 */
public class CleanUp {

	private static final Logger logger = LoggerFactory.getLogger(CleanUp.class);

    public static final int DELETE_MESSAGES  = 1;
    public static final int ARCHIVE_MESSAGES = 2;
    public static final int KEEP_MESSAGES    = 3;

    // we hold indices, chk keys, ... for at least the following count of days:
    private final static int MINIMUM_DAYS_OLD = 28;
    private final static int SFCHKKEYS_MINIMUM_DAYS_OLD = 16;

    private static Splashscreen splashScreen;

    /**
     * Expire messages during startup of Frost.
     * Clean indexslot tables and file tables.
     * Gets the mode to use from settings.
     */
    public static void runExpirationTasks(final Splashscreen sp, final List<Board> boardList) {

        splashScreen = sp;

        // each time run cleanup for perst storages
        cleanPerstStorages(boardList);

        // cleanup and archive all X days
        final int cleanupDatabaseInterval = Core.frostSettings.getInteger(Settings.DB_CLEANUP_INTERVAL);
        final long lastCleanupTime = Core.frostSettings.getLong(Settings.DB_CLEANUP_LASTRUN);
        final long now = System.currentTimeMillis();
        final long intervalMillis = (cleanupDatabaseInterval) * 24L * 60L * 60L * 1000L; // interval days into millis

        // when last cleanup was before the chosen interval days then run cleanup and archiving
        if( lastCleanupTime < (now - intervalMillis) ) {
            cleanStorages(boardList);
            Core.frostSettings.setValue(Settings.DB_CLEANUP_LASTRUN, now);
        }
    }

    private static void cleanPerstStorages(final List<Board> boardList) {
        splashScreen.setText("Cleaning index tables");
        cleanupIndexSlotsStorage(boardList);

        splashScreen.setText("Cleaning CHK filelist tables");
        cleanupSharedCHKKeyStorage();
    }

    private static void cleanStorages(final List<Board> boardList) {
        int mode;

        final String strMode = Core.frostSettings.getString(Settings.MESSAGE_EXPIRATION_MODE);
        if( strMode.toUpperCase().equals("KEEP") ) {
            mode = KEEP_MESSAGES;
        } else if( strMode.toUpperCase().equals("ARCHIVE") ) {
            mode = ARCHIVE_MESSAGES;
        } else if( strMode.toUpperCase().equals("DELETE") ) {
            mode = DELETE_MESSAGES;
        } else {
            mode = KEEP_MESSAGES;
        }

        processExpiredMessages(boardList, mode);

        splashScreen.setText("Cleaning file list owners");
        cleanupFileListFileOwners();
        splashScreen.setText("Cleaning file list files");
        cleanupFileListFiles();
    }

    private static void processExpiredMessages(final List<Board> boardList, final int mode) {

        if( mode == ARCHIVE_MESSAGES ) {
            logger.info("Expiration mode is ARCHIVE_MESSAGES.");
        } else if( mode == DELETE_MESSAGES ) {
            logger.info("Expiration mode is DELETE_MESSAGES.");
        } else if( mode == KEEP_MESSAGES ) {
            logger.info("Expiration mode is KEEP_MESSAGES.");
        } else {
            logger.error("invalid MODE specified: {}", mode);
            return;
        }

        if( mode == KEEP_MESSAGES ) {
            // nothing to do here
            return;
        }

        // take maximum
        int defaultDaysOld = Core.frostSettings.getInteger(Settings.MESSAGE_EXPIRE_DAYS) + 1;

        if( defaultDaysOld < Core.frostSettings.getInteger(Settings.MAX_MESSAGE_DISPLAY) ) {
            defaultDaysOld = Core.frostSettings.getInteger(Settings.MAX_MESSAGE_DISPLAY) + 1;
        }
        if( defaultDaysOld < Core.frostSettings.getInteger(Settings.MAX_MESSAGE_DOWNLOAD) ) {
            defaultDaysOld = Core.frostSettings.getInteger(Settings.MAX_MESSAGE_DOWNLOAD) + 1;
        }

        for( final Board board : boardList ) {

            int currentDaysOld = defaultDaysOld;
            if( board.isConfigured() ) {
                currentDaysOld = Math.max(board.getMaxMessageDisplay(), currentDaysOld);
                currentDaysOld = Math.max(board.getMaxMessageDownload(), currentDaysOld);
            }

            final boolean archiveKeepUnread = Core.frostSettings.getBoolean(Settings.ARCHIVE_KEEP_UNREAD);
            final boolean archiveKeepFlaggedAndStarred = Core.frostSettings.getBoolean(Settings.ARCHIVE_KEEP_FLAGGED_AND_STARRED);

            splashScreen.setText("Archiving messages in board: "+board.getName());

            final MessageTableCallback mtCallback = new MessageTableCallback(mode);
            try {
                MessageStorage.inst().retrieveMessagesForArchive(
                        board,
                        currentDaysOld,
                        archiveKeepUnread,
                        archiveKeepFlaggedAndStarred,
                        mtCallback);
            } catch(final Throwable t) {
                logger.error("Exception during retrieveMessagesForArchive", t);
                continue;
            }
            if( mtCallback.getCount() > 0 ) {
                logger.info("Processed {} expired messages for board {}", mtCallback.getCount(), board.getName());
            }

            MessageStorage.inst().commit();
            ArchiveMessageStorage.inst().commit();
        }
        MessageStorage.inst().commit();
        ArchiveMessageStorage.inst().commit();

        logger.info("Finished to process expired messages.");
    }

    /**
     * Callback that gets each expired message and tries to insert it into MessageArchive.
     */
    private static class MessageTableCallback implements MessageArchivingCallback {
        int mode;
        int count = 0;
        public MessageTableCallback(final int m) { mode = m; }
        public int messageRetrieved(final FrostMessageObject mo) {
            // mode is either ARCHIVE or DELETE
            if( count%100 == 0 ) {
                MessageStorage.inst().commit();
                ArchiveMessageStorage.inst().commit();
            }
            if( mode == ARCHIVE_MESSAGES ) {
                // maybe insert into archive
                final int rc = ArchiveMessageStorage.inst().insertMessage(mo);
                if( rc == ArchiveMessageStorage.INSERT_ERROR ) {
                    return MessageArchivingCallback.KEEP_MESSAGE;
                }
            }
            // for ARCHIVE or DELETE delete the message from keypool
            count++;
            return MessageArchivingCallback.DELETE_MESSAGE;
        }
        public int getCount() { return count; }
    }

    /**
     * Cleanup old indexslot table entries.
     * Keep indices files for maxMessageDownload*2 days, but at least MINIMUM_DAYS_OLD days.
     */
    private static void cleanupIndexSlotsStorage(final List<Board> boardList) {

        int maxDaysOld = Core.frostSettings.getInteger(Settings.MAX_MESSAGE_DOWNLOAD) * 2;

        // max from any board
        for( final Board board : boardList ) {
            if( board.isConfigured() ) {
                maxDaysOld = Math.max(board.getMaxMessageDownload(), maxDaysOld);
            }
        }
        if( maxDaysOld < MINIMUM_DAYS_OLD ) {
            maxDaysOld = MINIMUM_DAYS_OLD;
        }

        int deletedCount = 0;

        try {
            deletedCount += IndexSlotsStorage.inst().cleanup(maxDaysOld);
        } catch(final Throwable t) {
            logger.error("Exception during cleanup of IndexSlots", t);
        }
        if( deletedCount > 0 ) {
            logger.info("Finished to delete expired index slots, deleted {} rows.", deletedCount);
        }
    }

    /**
     * Cleanup old CHK keys from pointer files.
     * All keys we did'nt see for SFCHKKEYS_MINIMUM_DAYS_OLD days will be deleted.
     */
    private static void cleanupSharedCHKKeyStorage() {
        int deletedCount = 0;
        try {
            deletedCount = SharedFilesCHKKeyStorage.inst().cleanupTable(SFCHKKEYS_MINIMUM_DAYS_OLD);
        } catch(final Throwable t) {
            logger.error("Exception during cleanup of SharedFilesCHKKeys", t);
        }
        if( deletedCount > 0 ) {
            logger.info("Finished to delete expired SharedFilesCHKKeys, deleted {} rows.", deletedCount);
        }
    }

    /**
     * Remove owners that were not seen for more than MINIMUM_DAYS_OLD days and have no CHK key set.
     */
    private static void cleanupFileListFileOwners() {
        final boolean removeOfflineFilesWithKey = Core.frostSettings.getBoolean(Settings.DB_CLEANUP_REMOVEOFFLINEFILEWITHKEY);
        final int offlineFilesMaxDaysOld = Core.frostSettings.getInteger(Settings.DB_CLEANUP_OFFLINEFILESMAXDAYSOLD);

        int deletedCount = 0;
        try {
            deletedCount = FileListStorage.inst().cleanupFileListFileOwners(removeOfflineFilesWithKey, offlineFilesMaxDaysOld);
        } catch(final Throwable t) {
            logger.error("Exception during cleanup of FileListFileOwners", t);
        }
        if( deletedCount > 0 ) {
            logger.info("Finished to delete expired FileListFileOwners, deleted {} rows.", deletedCount);
        }
    }

    /**
     * Remove files that have no owner and no CHK key.
     */
    private static void cleanupFileListFiles() {

        int deletedCount = 0;
        try {
            deletedCount = FileListStorage.inst().cleanupFileListFiles();
        } catch(final Throwable t) {
            logger.error("Exception during cleanup of FileListFiles", t);
        }
        if( deletedCount > 0 ) {
            logger.info("Finished to delete expired FileListFiles, deleted {} rows.", deletedCount);
        }
    }
}
