/*
  FileListManager.java / Frost
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
package frost.fileTransfer.filelist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.FrostFileListFileObject;
import frost.fileTransfer.SharedFileXmlFile;
import frost.fileTransfer.download.FrostDownloadItem;
import frost.fileTransfer.sharing.FrostSharedFileItem;
import frost.identities.Identity;
import frost.identities.LocalIdentity;
import frost.storage.perst.filelist.FileListStorage;
import frost.storage.perst.identities.IdentitiesStorage;

public class FileListManager {

	private static final Logger logger = LoggerFactory.getLogger(FileListManager.class);

    public static final int MAX_FILES_PER_FILE = 250; // TODO: count utf-8 size of sharedxmlfiles, not more than 512kb!

    /**
     * Used to sort FrostSharedFileItems by refLastSent ascending.
     */
    private static final Comparator<FrostSharedFileItem> refLastSentComparator = new Comparator<FrostSharedFileItem>() {
        public int compare(final FrostSharedFileItem value1, final FrostSharedFileItem value2) {
            if (value1.getRefLastSent() > value2.getRefLastSent()) {
                return 1;
            } else if (value1.getRefLastSent() < value2.getRefLastSent()) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    /**
     * @return  an info class that is guaranteed to contain an owner and files
     */
    public static FileListManagerFileInfo getFilesToSend() {

        // get files to share from UPLOADFILES
        // - max. 250 keys per fileindex
        // - get keys of only 1 owner/anonymous, next time get keys from different owner
        //   (this wrap-around ensures that each file will be send over the time)

        // compute minDate, items last shared before this date must be reshared
        final int maxAge = Core.frostSettings.getInteger(Settings.MIN_DAYS_BEFORE_FILE_RESHARE);
        final long maxDiff = maxAge * 24L * 60L * 60L * 1000L;
        final long now = System.currentTimeMillis();
        final long minDate = now - maxDiff;

        final List<LocalIdentity> localIdentities = Core.getIdentitiesManager().getLocalIdentities();
        int identityCount = localIdentities.size();

        // we modify several own identities (id.setLastFilesSharedMillis())
        if( !IdentitiesStorage.inst().beginExclusiveThreadTransaction() ) {
            return null;
        }
        try {
            while(identityCount > 0) {

                LocalIdentity idToUpdate = null;
                long minUpdateMillis = Long.MAX_VALUE;

                // find next identity to update
                for(final LocalIdentity id : localIdentities ) {
                    final long lastShared = id.getLastFilesSharedMillis();
                    if( lastShared < minUpdateMillis ) {
                        minUpdateMillis = lastShared;
                        idToUpdate = id;
                    }
                }

                // mark that we tried this owner
                idToUpdate.setLastFilesSharedMillis(now);

                final LinkedList<SharedFileXmlFile> filesToShare =
                    getUploadItemsToShare(idToUpdate.getUniqueName(), MAX_FILES_PER_FILE, minDate);
                if( filesToShare != null && filesToShare.size() > 0 ) {
                    final FileListManagerFileInfo fif = new FileListManagerFileInfo(filesToShare, idToUpdate);
                    return fif;
                } else {
                    // else try next owner
                    identityCount--;
                }
            }

            // currently there is nothing to share
            return null;

        } finally {
            IdentitiesStorage.inst().endThreadTransaction();
        }
    }

    private static LinkedList<SharedFileXmlFile> getUploadItemsToShare(
            final String owner,
            final int maxItems,
            final long minDate)
    {

        final LinkedList<SharedFileXmlFile> result = new LinkedList<SharedFileXmlFile>();

        final ArrayList<FrostSharedFileItem> sorted = new ArrayList<FrostSharedFileItem>();

        {
            final List<FrostSharedFileItem> sharedFileItems = FileTransferManager.inst().getSharedFilesManager().getSharedFileItemList();

            // first collect all items for this owner and sort them
            for( final FrostSharedFileItem sfo : sharedFileItems ) {
                if( !sfo.isValid() ) {
                    continue;
                }
                if( !sfo.getOwner().equals(owner) ) {
                    continue;
                }
                sorted.add(sfo);
            }
        }

        if( sorted.isEmpty() ) {
            // no shared files for this owner
            return result;
        }

        // sort ascending, oldest items at the beginning
        Collections.sort(sorted, refLastSentComparator);

        {
            // check if oldest item must be shared (maybe its new or updated)
            final FrostSharedFileItem sfo = sorted.get(0);
            if( sfo.getRefLastSent() > minDate ) {
                // oldest item is'nt too old, don't share
                return result;
            }
        }

        // finally add up to MAX_FILES items from the sorted list
        for( final FrostSharedFileItem sfo : sorted ) {
            result.add( sfo.getSharedFileXmlFileInstance() );
            if( result.size() >= maxItems ) {
                return result;
            }
        }

        return result;
    }

    /**
     * Update sent files.
     * @param files  List of SharedFileXmlFile objects that were successfully sent inside a CHK file
     */
    public static boolean updateFileListWasSuccessfullySent(final List<SharedFileXmlFile> files) {

        final long now = System.currentTimeMillis();

        final List<FrostSharedFileItem> sharedFileItems = FileTransferManager.inst().getSharedFilesManager().getSharedFileItemList();

        for( final SharedFileXmlFile sfx : files ) {
            // update FrostSharedUploadFileObject
            for( final FrostSharedFileItem sfo : sharedFileItems ) {
                if( sfo.getSha().equals(sfx.getSha()) ) {
                    sfo.setRefLastSent(now);
                }
            }
        }
        return true;
    }

    /**
     * Add or update received files from owner
     */
    public static boolean processReceivedFileList(final FileListFileContent content) {

        if( content == null
            || content.getReceivedOwner() == null
            || content.getFileList() == null
            || content.getFileList().size() == 0 )
        {
            return false;
        }

        final boolean fileListAntiSpamMode = Core.frostSettings.getBoolean(Settings.FILESHARING_IGNORE_CHECK_AND_BELOW);

        Identity localOwner;
        synchronized( Core.getIdentitiesManager().getLockObject() ) {
            localOwner = Core.getIdentitiesManager().getIdentity(content.getReceivedOwner().getUniqueName());

            if (fileListAntiSpamMode) {
                // anti-spam mode. Ignore file lists from CHECK, BAD and just newly received identities
                if (localOwner == null
                        || localOwner.isCHECK()
                        || localOwner.isBAD())
                {
                    // only GOOD and OBSERVE allowed
                    // we intentionally don't update timestamp of CHECK or BAD identities to avoid DOS of our database
                    return false;

                } else {
                    if( localOwner.getLastSeenTimestamp() < content.getTimestamp() ) {
                        localOwner.setLastSeenTimestamp(content.getTimestamp());
                    }
                }

            } else {
                // normal mode: add all newly received identities with CHECK and add their file lists
                if( localOwner == null ) {
                    // new identity, add. Validated inside FileListFile.readFileListFile()
                    localOwner = content.getReceivedOwner();
                    localOwner.setLastSeenTimestampWithoutUpdate(content.getTimestamp());
                    if( !Core.getIdentitiesManager().addIdentity(content.getReceivedOwner()) ) {
                        logger.error("Core.getIdentitiesManager().addIdentity() returned false for identity: {}", content.getReceivedOwner());
                        return false;
                    }
                } else {
                    if( localOwner.getLastSeenTimestamp() < content.getTimestamp() ) {
                        localOwner.setLastSeenTimestamp(content.getTimestamp());
                    }
                }
            }
        }

        if (localOwner.isBAD() && Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_BAD)) {
            logger.info("Skipped index file from BAD user {}", localOwner.getUniqueName());
            return true;
        }

        final List<FrostDownloadItem> downloadItems = FileTransferManager.inst().getDownloadManager().getDownloadItemList();

        // update all filelist files, maybe restart failed downloads
        final List<FrostDownloadItem> failedDownloadsToRestart = new ArrayList<FrostDownloadItem>();
        final List<FrostDownloadItem> runningDownloadsToRestart = new ArrayList<FrostDownloadItem>();
        boolean errorOccured = false;

        if( !FileListStorage.inst().beginExclusiveThreadTransaction() ) {
            logger.error("Failed to begin an EXCLUSIVE thread transaction, aborting.");
            return false;
        }

//        final boolean isFreenet07 = FcpHandler.isFreenet07();

        try {
            for( final SharedFileXmlFile sfx : content.getFileList() ) {

                final FrostFileListFileObject sfo = new FrostFileListFileObject(sfx, localOwner, content.getTimestamp());

//                if( isFreenet07 && FreenetKeys.isOld07ChkKey( sfo.getKey() )) {
//                    // ignore old chk keys
//                    continue;
//                }

                // before updating the file list object (this overwrites the current lastUploaded time),
                // check if there is a failed download item for this shared file. If yes, and the lastUpload
                // time is later than the current one, restart the download automatically.
                for( final FrostDownloadItem dlItem : downloadItems ) {

                    if( !dlItem.isSharedFile() ) {
                        continue;
                    }

                    if( dlItem.getState() == FrostDownloadItem.STATE_FAILED ) {
                        final FrostFileListFileObject dlSfo = dlItem.getFileListFileObject();
                        if( dlSfo.getSha().equals( sfx.getSha() ) ) {
                            if( dlSfo.getLastUploaded() < sfo.getLastUploaded() ) {
                                // restart failed download, file was uploaded again
                                failedDownloadsToRestart.add(dlItem); // restart later if no error occured
                            }
                        }
                    } else if( dlItem.getState() == FrostDownloadItem.STATE_PROGRESS ) {
                        final FrostFileListFileObject dlSfo = dlItem.getFileListFileObject();
                        if( dlSfo.getSha().equals( sfx.getSha() )
                                && dlItem.isEnabled() != null && dlItem.isEnabled().booleanValue() == true
                                && dlSfo.getLastUploaded() < sfo.getLastUploaded()
                                && dlSfo.getKey() != null
                                && sfx.getKey() != null
                                && !dlSfo.getKey().equals( sfx.getKey() ) )
                        {
                            // restart running download, file got new key from a newer upload
                            runningDownloadsToRestart.add(dlItem); // restart later if no error occured
                        }
                    }
                }

                // update filelist storage (applies new key)
                final boolean wasOk = FileListStorage.inst().insertOrUpdateFileListFileObject(sfo);
                if( wasOk == false ) {
                    errorOccured = true;
                    break;
                }
            }
        } catch(final Throwable t) {
            logger.error("Exception during insertOrUpdateFrostSharedFileObject", t);
            errorOccured = true;
        }

        if( errorOccured ) {
            FileListStorage.inst().rollbackTransaction();
            return false;
        } else {
            FileListStorage.inst().endThreadTransaction();
        }

        // after updating the db, check if we have to update download items with the new informations
        for( final SharedFileXmlFile sfx : content.getFileList() ) {

            // if a FrostDownloadItem references this file (by sha), retrieve the updated file from db and set it
            for( final FrostDownloadItem dlItem : downloadItems ) {

                if( !dlItem.isSharedFile() ) {
                    continue;
                }

                final FrostFileListFileObject dlSfo = dlItem.getFileListFileObject();
                if( dlSfo.getSha().equals( sfx.getSha() ) ) {
                    // this download item references the updated file
                    // update the shared file object from database (owner, sources, ... may have changed)

                    // NOTE: if no key was set before, this sets the chkKey and the ticker will start to download this file!

                    FrostFileListFileObject updatedSfo = null;
                    if( !FileListStorage.inst().beginCooperativeThreadTransaction() ) {
                        logger.error("Failed to begin a COOPERATIVE thread transaction.");
                    } else {
                        updatedSfo = FileListStorage.inst().getFileBySha(sfx.getSha());
                        FileListStorage.inst().endThreadTransaction();
                    }
                    if( updatedSfo != null ) {
                        dlItem.setFileListFileObject(updatedSfo);
                    } else {
                        logger.warn("no file for sha!");
                    }
                    break; // there is only one file in download table with same SHA
                }
            }
        }

        // restart failed downloads, as collected above
        for( final FrostDownloadItem dlItem : failedDownloadsToRestart ) {
            dlItem.setState(FrostDownloadItem.STATE_WAITING);
            dlItem.setRetries(0);
            dlItem.setLastDownloadStopTime(0);
            dlItem.setEnabled(Boolean.valueOf(true)); // enable download on restart
        }

        // restart running downloads, as collected above. New key is already applied.
        FileTransferManager.inst().getDownloadManager().getModel().restartRunningDownloads(runningDownloadsToRestart);

        return true;
    }
}
