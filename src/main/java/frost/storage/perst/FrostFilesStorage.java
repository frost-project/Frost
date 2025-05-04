/*
  FrostFilesStorage.java / Frost
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
package frost.storage.perst;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.garret.perst.IPersistentList;
import org.garret.perst.Persistent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Settings;
import frost.fileTransfer.download.FrostDownloadItem;
import frost.fileTransfer.sharing.FrostSharedFileItem;
import frost.fileTransfer.upload.FrostUploadItem;
import frost.gui.KnownBoard;
import frost.messaging.frost.boards.Board;
import frost.storage.ExitSavable;
import frost.storage.StorageException;
import frost.util.gui.translation.Language;

/**
 * A Storage for FrostDownloadFiles, FrostUploadFiles and SharedFiles.
 * Loaded during startup, and saved during shutdown (or during autosave).
 */
public class FrostFilesStorage extends AbstractFrostStorage implements ExitSavable {

	private static final Logger logger = LoggerFactory.getLogger(FrostFilesStorage.class);

    private static final String STORAGE_FILENAME = "filesStore.dbs";

    private FrostFilesStorageRoot storageRoot = null;

    private static FrostFilesStorage instance = new FrostFilesStorage();

    protected FrostFilesStorage() {
        super();
    }

    public static FrostFilesStorage inst() {
        return instance;
    }

    @Override
    public String getStorageFilename() {
        return STORAGE_FILENAME;
    }

    @Override
    public boolean initStorage() {
        final String databaseFilePath = buildStoragePath(getStorageFilename()); // path to the database file
        final long pagePoolSize = getPagePoolSize(Settings.PERST_PAGEPOOLSIZE_FILES);

        open(databaseFilePath, pagePoolSize, true, true, false);

        storageRoot = (FrostFilesStorageRoot)getStorage().getRoot();
        if (storageRoot == null) {
            // Storage was not initialized yet
            storageRoot = new FrostFilesStorageRoot();

            storageRoot.downloadFiles = getStorage().createScalableList();
            storageRoot.uploadFiles = getStorage().createScalableList();
            storageRoot.sharedFiles = getStorage().createScalableList();
            storageRoot.newUploadFiles = getStorage().createScalableList();

            storageRoot.hiddenBoardNames = getStorage().createScalableList();
            storageRoot.knownBoards = getStorage().createIndex(String.class, true);

            getStorage().setRoot(storageRoot);
            commit(); // commit transaction
        } else if( storageRoot.hiddenBoardNames == null ) {
            // add new root items
            storageRoot.hiddenBoardNames = getStorage().createScalableList();
            storageRoot.knownBoards = getStorage().createIndex(String.class, true);
            storageRoot.modify();
            commit(); // commit transaction
        }
        return true;
    }

    public void exitSave() throws StorageException {
        close();
        storageRoot = null;
        logger.info("FrostFilesStorage closed.");
    }

    /**
     * Removes all items from the given List and deallocates each item from Storage.
     * @param plst  IPersistentList of persistent items
     */
    private void removeAllFromStorage(final IPersistentList<? extends Persistent> plst) {
        for(final Iterator<? extends Persistent> i=plst.iterator(); i.hasNext(); ) {
            final Persistent pi = i.next();
            i.remove(); // remove from List
            pi.deallocate(); // remove from Storage
        }
        plst.clear(); // paranoia
    }

    public void saveDownloadFiles(final List<FrostDownloadItem> downloadFiles) {
        beginExclusiveThreadTransaction();
        try {
            removeAllFromStorage(storageRoot.downloadFiles); // delete all old items
            for( final FrostDownloadItem dlItem : downloadFiles ) {
                if( dlItem.isExternal() ) {
                    continue;
                }
                final PerstFrostDownloadItem pi = new PerstFrostDownloadItem(dlItem);
                storageRoot.downloadFiles.add(pi);
            }
        } finally {
            endThreadTransaction();
        }
    }

    public List<FrostDownloadItem> loadDownloadFiles() {
        final LinkedList<FrostDownloadItem> downloadItems = new LinkedList<FrostDownloadItem>();
        beginCooperativeThreadTransaction();
        try {
            for( final PerstFrostDownloadItem pi : storageRoot.downloadFiles ) {
                final FrostDownloadItem dlItem = pi.toFrostDownloadItem(logger);
                if( dlItem != null ) {
                    downloadItems.add(dlItem);
                }
            }
        } finally {
            endThreadTransaction();
        }
        return downloadItems;
    }

    public void saveUploadFiles(final List<FrostUploadItem> uploadFiles) {
        beginExclusiveThreadTransaction();
        try {
            removeAllFromStorage(storageRoot.uploadFiles); // delete all old items
            for( final FrostUploadItem ulItem : uploadFiles ) {
                if( ulItem.isExternal() ) {
                    continue;
                }
                final PerstFrostUploadItem pi = new PerstFrostUploadItem(ulItem);
                storageRoot.uploadFiles.add(pi);
            }
        } finally {
            endThreadTransaction();
        }
    }

    public List<FrostUploadItem> loadUploadFiles(final List<FrostSharedFileItem> sharedFiles) {
        final LinkedList<FrostUploadItem> uploadItems = new LinkedList<FrostUploadItem>();
        final Language language = Language.getInstance();
        beginCooperativeThreadTransaction();
        try {
            for( final PerstFrostUploadItem pi : storageRoot.uploadFiles ) {
                final FrostUploadItem ulItem = pi.toFrostUploadItem(sharedFiles, logger, language);
                if( ulItem != null ) {
                    uploadItems.add(ulItem);
                }
            }
        } finally {
            endThreadTransaction();
        }
        return uploadItems;
    }

    public void saveSharedFiles(final List<FrostSharedFileItem> sfFiles) {
        beginExclusiveThreadTransaction();
        try {
            removeAllFromStorage(storageRoot.sharedFiles);
            for( final FrostSharedFileItem sfItem : sfFiles ) {
                final PerstFrostSharedFileItem pi = new PerstFrostSharedFileItem(sfItem);
                storageRoot.sharedFiles.add(pi);
            }
        } finally {
            endThreadTransaction();
        }
    }

    public List<FrostSharedFileItem> loadSharedFiles() {
        final LinkedList<FrostSharedFileItem> sfItems = new LinkedList<FrostSharedFileItem>();
        final Language language = Language.getInstance();
        beginCooperativeThreadTransaction();
        try {
            for( final PerstFrostSharedFileItem pi : storageRoot.sharedFiles ) {
                final FrostSharedFileItem sfItem = pi.toFrostSharedFileItem(logger, language);
                if( sfItem != null ) {
                    sfItems.add(sfItem);
                }
            }
        } finally {
            endThreadTransaction();
        }
        return sfItems;
    }

    public void saveNewUploadFiles(final List<NewUploadFile> newUploadFiles) {
        beginExclusiveThreadTransaction();
        try {
            removeAllFromStorage(storageRoot.newUploadFiles);
            for( final NewUploadFile nuf : newUploadFiles ) {
                nuf.makePersistent(getStorage());
                nuf.modify(); // for already persistent items

                storageRoot.newUploadFiles.add(nuf);
            }
        } finally {
            endThreadTransaction();
        }
    }

    public LinkedList<NewUploadFile> loadNewUploadFiles() {

        final LinkedList<NewUploadFile> newUploadFiles = new LinkedList<NewUploadFile>();
        beginCooperativeThreadTransaction();
        try {
            for( final NewUploadFile nuf : storageRoot.newUploadFiles ) {
                final File f = new File(nuf.getFilePath());
                if( !f.isFile() ) {
                    logger.warn("File ({}) is missing. File removed.", nuf.getFilePath());
                    continue;
                }
                newUploadFiles.add(nuf);
            }
        } finally {
            endThreadTransaction();
        }
        return newUploadFiles;
    }

    /**
     * Load all hidden board names.
     */
    public HashSet<String> loadHiddenBoardNames() {
        final HashSet<String> result = new HashSet<String>();
        beginCooperativeThreadTransaction();
        try {
            for( final PerstHiddenBoardName hbn : storageRoot.hiddenBoardNames ) {
                result.add(hbn.getHiddenBoardName());
            }
        } finally {
            endThreadTransaction();
        }
        return result;
    }

    /**
     * Clear table and save all hidden board names.
     */
    public void saveHiddenBoardNames(final HashSet<String> names) {
        beginExclusiveThreadTransaction();
        try {
            removeAllFromStorage(storageRoot.hiddenBoardNames);
            for( final String s : names ) {
                final PerstHiddenBoardName h = new PerstHiddenBoardName(s);
                storageRoot.hiddenBoardNames.add(h);
            }
        } finally {
            endThreadTransaction();
        }
    }

    private String buildBoardIndex(final Board b) {
        final StringBuilder sb = new StringBuilder();
        sb.append(b.getNameLowerCase());
        if( b.getPublicKey() != null ) {
            sb.append(b.getPublicKey());
        }
        if( b.getPrivateKey() != null ) {
            sb.append(b.getPrivateKey());
        }
        return sb.toString();
    }

    /**
     * @return  List of KnownBoard
     */
    public List<KnownBoard> getKnownBoards() {
        beginCooperativeThreadTransaction();
        final List<KnownBoard> lst;
        try {
            lst = new ArrayList<KnownBoard>();
            for( final PerstKnownBoard pkb : storageRoot.knownBoards ) {
                final KnownBoard kb = new KnownBoard(pkb.getBoardName(), pkb.getPublicKey(), pkb.getPrivateKey(), pkb
                        .getDescription());
                lst.add(kb);
            }
        } finally {
            endThreadTransaction();
        }
        return lst;
    }

    public synchronized boolean deleteKnownBoard(final Board b) {
        final String newIx = buildBoardIndex(b);
        beginExclusiveThreadTransaction();
        final boolean retval;
        try {
            final PerstKnownBoard pkb = storageRoot.knownBoards.get(newIx);
            if( pkb != null ) {
                storageRoot.knownBoards.remove(newIx, pkb);
                pkb.deallocate();
                retval = true;
            } else {
                retval = false;
            }
        } finally {
            endThreadTransaction();
        }
        return retval;
    }

    /**
     * Called with a list of Board, should add all boards that are not contained already
     * @param lst  List of Board
     * @return  number of added boards
     */
    public synchronized int addNewKnownBoards( final List<? extends Board> lst ) {
        if( lst == null || lst.size() == 0 ) {
            return 0;
        }
        beginExclusiveThreadTransaction();
        int added = 0;
        try {
            for( final Board b : lst ) {
                final String newIx = buildBoardIndex(b);
                final PerstKnownBoard pkb = new PerstKnownBoard(b.getName(), b.getPublicKey(), b.getPrivateKey(), b
                        .getDescription());
                if( storageRoot.knownBoards.put(newIx, pkb) ) {
                    added++;
                }
            }
        } finally {
            endThreadTransaction();
        }
        return added;
    }
}
