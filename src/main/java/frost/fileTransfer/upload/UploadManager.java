/*
  UploadManager.java / Frost
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
package frost.fileTransfer.upload;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fcp.FcpResultPut;
import frost.fileTransfer.FileTransferInformation;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.FreenetPriority;
import frost.fileTransfer.sharing.FrostSharedFileItem;
import frost.storage.ExitSavable;
import frost.storage.StorageException;
import frost.util.FileAccess;
import frost.util.Mixed;

public class UploadManager implements ExitSavable {

	private static final Logger logger = LoggerFactory.getLogger(UploadManager.class);

    private UploadModel model;
    private UploadPanel panel;
    private UploadTicker ticker;

    public UploadManager() {
        super();
    }

    public void initialize(final List<FrostSharedFileItem> sharedFiles) throws StorageException {
        getPanel();
        getModel().initialize(sharedFiles);
    }

    /**
     * Count running items in model.
     */
    public void updateFileTransferInformation(final FileTransferInformation infos) {
        int waitingItems = 0;
        int runningItems = 0;
        for (int x = 0; x < model.getItemCount(); x++) {
			final FrostUploadItem ulItem = model.getItemAt(x);
            if (ulItem.getState() != FrostUploadItem.STATE_DONE
                    && ulItem.getState() != FrostUploadItem.STATE_FAILED)
            {
                waitingItems++;
            }
            if (ulItem.getState() == FrostUploadItem.STATE_PROGRESS) {
                runningItems++;
            }
        }
        infos.setUploadsRunning(runningItems);
        infos.setUploadsWaiting(waitingItems);
    }

    public void startTicker() {
        if (Core.isFreenetOnline()) {
            getTicker().start();
        }
    }

    public void exitSave() throws StorageException {
        getPanel().getTableFormat().saveTableLayout();
        getModel().exitSave();
    }

    public void addPanelToMainFrame(final MainFrame mainFrame) {
        mainFrame.addPanel("MainFrame.tabbedPane.uploads", getPanel());
    }

    public UploadPanel getPanel() {
        if (panel == null) {
            panel = new UploadPanel();
            panel.setModel(getModel());
            panel.initialize();
        }
        return panel;
    }

    private UploadTicker getTicker() {
        if (ticker == null) {
            ticker = new UploadTicker(getModel());
        }
        return ticker;
    }

    public UploadModel getModel() {
        if (model == null) {
            model = new UploadModel(new UploadTableFormat());
        }
        return model;
    }

    /**
     * Handle a finished file upload, either successful or failed.
     */
    public void notifyUploadFinished(final FrostUploadItem uploadItem, final FcpResultPut result) {

        if (result != null && (result.isSuccess() || result.isKeyCollision()) ) {

            logger.info("Upload of {} ({}) was successful.", uploadItem.getFile().getName(), uploadItem.getFileName());

            // upload successful
            uploadItem.setKey(result.getChkKey());
            if( uploadItem.isSharedFile() ) {
                uploadItem.getSharedFileItem().notifySuccessfulUpload(result.getChkKey());
            }

            uploadItem.setEnabled(false);
            uploadItem.setState(FrostUploadItem.STATE_DONE);

            uploadItem.setUploadFinishedMillis(System.currentTimeMillis());

            // notify model that shared upload file can be removed
            if( uploadItem.isSharedFile() ) {
                getModel().notifySharedFileUploadWasSuccessful(uploadItem);
            } else {
                // maybe log successful manual upload to file localdata/uploads.txt
                if( Core.frostSettings.getBoolValue(Settings.LOG_UPLOADS_ENABLED) && !uploadItem.isLoggedToFile() ) {
                    final String line = uploadItem.getKey() + "/" + uploadItem.getFileName();
                    final String fileName = Core.frostSettings.getValue(Settings.DIR_LOCALDATA) + "Frost-Uploads.log";
                    final File targetFile = new File(fileName);
                    FileAccess.appendLineToTextfile(targetFile, line);
                    uploadItem.setLoggedToFile(true);
                }

                final String execProg = Core.frostSettings.getValue(Settings.EXEC_ON_UPLOAD);
                if( execProg != null && execProg.length() > 0 && !uploadItem.isCompletionProgRun() ) {
                    final File dir = uploadItem.getFile().getParentFile();
                    final Map<String, String> oldEnv = System.getenv();
                    final String[] newEnv = new String[oldEnv.size() + 2];
                    String args[] = new String[3];
                    int i;

                    args[0] = execProg;
                    args[1] = uploadItem.getFileName();
                    args[2] = result.getChkKey();

                    for( i = 0; i < args.length; i++ ) {
                        if( args[i] == null ) {
                            args[i] = "";
                        }
                    }

                    i = 0;
                    for (final Map.Entry<String, String> entry : oldEnv.entrySet()) {
                        newEnv[i++] = entry.getKey() + "=" + entry.getValue();
                    }

                    newEnv[i++] = "FROST_FILENAME=" + uploadItem.getFileName();
                    newEnv[i++] = "FROST_KEY=" + result.getChkKey();

                    try {
                        Runtime.getRuntime().exec(args, newEnv, dir);
                    } catch (Exception e) {
                        logger.error("Could not exec {}:", execProg, e);
                    }
                }

                uploadItem.setCompletionProgRun(true);
            }

            // maybe remove finished upload immediately
            if( Core.frostSettings.getBoolValue(Settings.UPLOAD_REMOVE_FINISHED) ) {
                getModel().removeFinishedUploads();
            }
        } else {
            // upload failed
            logger.warn("Upload of {} was NOT successful.", uploadItem.getFile().getName());

            if( result != null && result.isFatal() ) {
                uploadItem.setEnabled(Boolean.FALSE);
                uploadItem.setState(FrostUploadItem.STATE_FAILED);
            } else {
                uploadItem.setRetries(uploadItem.getRetries() + 1);

                if (uploadItem.getRetries() > Core.frostSettings.getIntValue(Settings.UPLOAD_MAX_RETRIES)) {
                    uploadItem.setEnabled(Boolean.FALSE);
                    uploadItem.setState(FrostUploadItem.STATE_FAILED);
                } else {
                    // retry
                    uploadItem.setState(FrostUploadItem.STATE_WAITING);
                }
            }
            if( result != null ) {
                uploadItem.setErrorCodeDescription(result.getCodeDescription());
            }
        }
        uploadItem.setLastUploadStopTimeMillis(System.currentTimeMillis());
    }

    /**
     * Start upload now (manually).
     */
    public boolean startUpload(final FrostUploadItem ulItem) {
        if( FileTransferManager.inst().getPersistenceManager() != null ) {
            return FileTransferManager.inst().getPersistenceManager().startUpload(ulItem);
        } else {
            return ticker.startUpload(ulItem);
        }
    }

    /**
     * Chooses next upload item to start from upload table.
     * @return the next upload item to start uploading or null if a suitable one was not found.
     */
    public FrostUploadItem selectNextUploadItem() {

        final ArrayList<FrostUploadItem> waitingItems = new ArrayList<FrostUploadItem>();

        final long currentTime = System.currentTimeMillis();

        for (int i = 0; i < model.getItemCount(); i++) {
			final FrostUploadItem ulItem = model.getItemAt(i);

            // don't start disabled items
            final boolean itemIsEnabled = (ulItem.isEnabled()==null?true:ulItem.isEnabled().booleanValue());
            if( !itemIsEnabled ) {
                continue;
            }
            // don't start external 0.7 items
            if( ulItem.isExternal() ) {
                continue;
            }
            // don't start items whose direct transfer to the node is already in progress
            if( FileTransferManager.inst().getPersistenceManager() != null ) {
                if( FileTransferManager.inst().getPersistenceManager().isDirectTransferInProgress(ulItem) ) {
                    continue;
                }
            }
            // only start waiting items
            if (ulItem.getState() != FrostUploadItem.STATE_WAITING) {
                continue;
            }
            // check if items waittime between tries is expired so we could restart it
            final long waittimeMillis = Core.frostSettings.getIntValue(Settings.UPLOAD_WAITTIME) * 60L * 1000L;
            if ((currentTime - ulItem.getLastUploadStopTimeMillis()) < waittimeMillis) {
                continue;
            }

            // we could start this item
            waitingItems.add(ulItem);
        }

        if (waitingItems.size() == 0) {
            return null;
        }

        if (waitingItems.size() > 1) {
            Collections.sort(waitingItems, nextItemCmp);
        }

        return waitingItems.get(0);
    }

    public void notifyUploadItemEnabledStateChanged(final FrostUploadItem ulItem) {
        // for persistent items, set priority to 6 (pause) when disabled; and to configured default if enabled
        if( FileTransferManager.inst().getPersistenceManager() == null ) {
            return;
        }
        if( ulItem.isExternal() ) {
            return;
        }
        if( ulItem.getState() != FrostUploadItem.STATE_PROGRESS ) {
            // not running, not in queue
            return;
        }
        final boolean itemIsEnabled = (ulItem.isEnabled() == null ? true : ulItem.isEnabled().booleanValue());
        List<FrostUploadItem> frostUploadItems = new ArrayList<FrostUploadItem>();
        frostUploadItems.add(ulItem);
        FreenetPriority prio = FreenetPriority.PAUSE;
        if( itemIsEnabled ) {
            prio = FreenetPriority.getPriority(Core.frostSettings.getIntValue(Settings.FCP2_DEFAULT_PRIO_FILE_UPLOAD));
        }
        panel.changeItemPriorites(frostUploadItems, prio);
    }

    private static final Comparator<FrostUploadItem> nextItemCmp = new Comparator<FrostUploadItem>() {
        public int compare(final FrostUploadItem value1, final FrostUploadItem value2) {

            // choose item that with lowest addedTime
            final int cmp1 = Mixed.compareLong(value1.getUploadAddedMillis(), value2.getUploadAddedMillis());
            if( cmp1 != 0 ) {
                return cmp1;
            }

            // equal addedTimes, choose by blocksRemaining
            int blocksTodo1;
            int blocksTodo2;

            // compute remaining blocks
            if( value1.getTotalBlocks() > 0 && value1.getDoneBlocks() > 0 ) {
                blocksTodo1 = value1.getTotalBlocks() - value1.getDoneBlocks();
            } else {
                blocksTodo1 = Integer.MAX_VALUE; // never started
            }
            if( value2.getTotalBlocks() > 0 && value2.getDoneBlocks() > 0 ) {
                blocksTodo2 = value2.getTotalBlocks() - value2.getDoneBlocks();
            } else {
                blocksTodo2 = Integer.MAX_VALUE; // never started
            }

            final int cmp2 = Mixed.compareInt(blocksTodo1, blocksTodo2);
            if( cmp2 != 0 ) {
                return cmp2;
            }

            // equal remainingBlocks, choose smaller file
            return Mixed.compareLong(value1.getFileSize(), value2.getFileSize());
        }
    };
}
