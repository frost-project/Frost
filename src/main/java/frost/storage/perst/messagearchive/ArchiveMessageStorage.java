/*
  MessageArchiveStorage.java / Frost
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
package frost.storage.perst.messagearchive;

import java.util.Iterator;

import org.garret.perst.GenericIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Settings;
import frost.messaging.frost.FrostMessageObject;
import frost.messaging.frost.boards.Board;
import frost.storage.ExitSavable;
import frost.storage.MessageCallback;
import frost.storage.perst.AbstractFrostStorage;
import frost.util.DateFun;

public class ArchiveMessageStorage extends AbstractFrostStorage implements ExitSavable {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveMessageStorage.class);

    private static final String STORAGE_FILENAME = "messageArchive.dbs";

    public static final int INSERT_OK        = 1;
    public static final int INSERT_DUPLICATE = 2;
    public static final int INSERT_ERROR     = 3;

    private ArchiveMessageStorageRoot storageRoot = null;

    private static ArchiveMessageStorage instance = new ArchiveMessageStorage();

    protected ArchiveMessageStorage() {
        super();
    }

    public static ArchiveMessageStorage inst() {
        return instance;
    }

    @Override
    public String getStorageFilename() {
        return STORAGE_FILENAME;
    }

    @Override
    public boolean initStorage() {
        final String databaseFilePath = buildStoragePath(getStorageFilename()); // path to the database file
        final long pagePoolSize = getPagePoolSize(Settings.PERST_PAGEPOOLSIZE_MESSAGEARCHIVE);

        open(databaseFilePath, pagePoolSize, true, false, false);

        storageRoot = (ArchiveMessageStorageRoot)getStorage().getRoot();
        if (storageRoot == null) {
            // Storage was not initialized yet
            storageRoot = new ArchiveMessageStorageRoot(getStorage());
            getStorage().setRoot(storageRoot);
            commit(); // commit transaction
        }
        return true;
    }

    public void exitSave() {
        close();
        storageRoot = null;
        logger.info("MessageArchiveStorage closed.");
    }

    public int getMessageCount() {
        int msgCount = 0;
        for(final PerstFrostArchiveBoardObject bo : storageRoot.getBoardsByName()) {
            if( bo.getMessageIndex() != null ) {
                msgCount += bo.getMessageIndex().size();
            }
        }
        return msgCount;
    }

	private void addBoardIfNotExists(String boardName) {
		if (storageRoot.getBoardsByName().get(boardName) != null) {
			logger.debug("Ignore duplicate board {}", boardName);
			return;
		}

		PerstFrostArchiveBoardObject pfbo = new PerstFrostArchiveBoardObject(getStorage(), boardName);
		storageRoot.getBoardsByName().put(boardName, pfbo);

		logger.info("Added archive board: {}", boardName);

		commit();
	}

    /**
     * Called by cleanup during startup, no transaction locking needed.
     */
    public synchronized int insertMessage(final FrostMessageObject mo) {
        final Board targetBoard = mo.getBoard();
        if( targetBoard == null ) {
            logger.error("msgInsertError: no board in msg");
            return INSERT_ERROR; // skip msg
        }
        return insertMessage(mo, targetBoard.getNameLowerCase());
    }

    /**
     * Called by cleanup during startup and during migration, no transaction locking needed.
     */
    public synchronized int insertMessage(final FrostMessageObject mo, final String boardName) {

        if( !mo.isValid() ) {
            return INSERT_OK; // ignore invalid msgs
        }
        if( mo.isDeleted() ) {
            return INSERT_OK; // ignore deleted msgs
        }
        if( mo.isJunk() ) {
            return INSERT_OK; // ignore junk msgs
        }
        if( mo.getFromIdentity() != null
                && mo.getFromIdentity().isBAD() )
        {
            return INSERT_OK; // ignore BAD msgs
        }

        // add to indices, check for duplicate msgId

		addBoardIfNotExists(boardName);
		PerstFrostArchiveBoardObject bo = storageRoot.getBoardsByName().get(boardName);
		if (bo == null) {
			logger.error("still no board???");
			return INSERT_ERROR;
		}

        final PerstFrostArchiveMessageObject pmo = new PerstFrostArchiveMessageObject(mo, getStorage());

        if( mo.getMessageId() != null ) {
            if( !bo.getMessageIdIndex().put(mo.getMessageId(), pmo) ) {
                // duplicate messageId!
                return INSERT_DUPLICATE; // skip msg
            }
        }

		bo.getMessageIndex().put(DateFun.toMilli(mo.getDateAndTime()), pmo);

        return INSERT_OK;
    }

    public void retrieveMessagesForSearch(
            final Board board,
            final long startDate,
            final long endDate,
            final MessageCallback mc)
    {
        final PerstFrostArchiveBoardObject bo = storageRoot.getBoardsByName().get(board.getNameLowerCase());
        if( bo == null ) {
            // this board does not exist in the archive
            return;
        }
        // normal messages in date range
        final Iterator<PerstFrostArchiveMessageObject> i = bo.getMessageIndex().iterator(startDate, endDate, GenericIndex.ASCENT_ORDER);
        while(i.hasNext()) {
            final PerstFrostArchiveMessageObject p = i.next();
            final FrostMessageObject mo = p.toFrostMessageObject(board);
            final boolean shouldStop = mc.messageRetrieved(mo);
            if( shouldStop ) {
                break;
            }
        }
    }
}
