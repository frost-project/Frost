/*
  PersistenceManager.java / Frost
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
package frost.fileTransfer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fcp.FcpHandler;
import frost.fcp.FcpResultGet;
import frost.fcp.FcpResultPut;
import frost.fcp.NodeAddress;
import frost.fcp.fcp07.FcpListenThreadConnection;
import frost.fcp.fcp07.FcpMultiRequestConnectionFileTransferTools;
import frost.fcp.fcp07.NodeMessage;
import frost.fcp.fcp07.filepersistence.FcpPersistentGet;
import frost.fcp.fcp07.filepersistence.FcpPersistentPut;
import frost.fcp.fcp07.filepersistence.FcpPersistentQueue;
import frost.fcp.fcp07.filepersistence.IFcpPersistentRequestsHandler;
import frost.fileTransfer.download.DownloadModel;
import frost.fileTransfer.download.FrostDownloadItem;
import frost.fileTransfer.upload.FrostUploadItem;
import frost.fileTransfer.upload.UploadModel;
import frost.util.Mixed;
import frost.util.model.ModelItem;
import frost.util.model.SortedModelListener;

/**
 * This class starts/stops/monitors the persistent requests on Freenet 0.7.
 */
public class PersistenceManager implements IFcpPersistentRequestsHandler {

	private static final Logger logger = LoggerFactory.getLogger(PersistenceManager.class);

	// FIXME Problem: positiv abgleich klappt, aber woher weiss ich wann LIST durch ist um zu checken ob welche fehlen?

    // this would belong to the models, but its not needed there without persistence, hence we maintain it here
    private final Hashtable<String,FrostUploadItem> uploadModelItems = new Hashtable<String,FrostUploadItem>();
    private final Hashtable<String,FrostDownloadItem> downloadModelItems = new Hashtable<String,FrostDownloadItem>();

    private final UploadModel uploadModel;
    private final DownloadModel downloadModel;

    private final DirectTransferQueue directTransferQueue;
    private final DirectTransferThread directTransferThread;

    private boolean showExternalItemsDownload;
    private boolean showExternalItemsUpload;

    private boolean isConnected = true; // we start in connected state

    private final FcpPersistentQueue persistentQueue;
    private final FcpListenThreadConnection fcpConn;
    private final FcpMultiRequestConnectionFileTransferTools fcpTools;


	private final Set<String> directGETsInProgress = new HashSet<String>();
    private final Set<String> directPUTsInProgress = new HashSet<String>();

    private final Set<String> directPUTsWithoutAnswer = new HashSet<String>();

    /**
     * @return  true if Frost is configured to use persistent uploads and downloads, false if not
     */
    public static boolean isPersistenceEnabled() {
    	return Core.frostSettings.getBoolean(Settings.FCP2_USE_PERSISTENCE);
    }

    /**
     * Must be called after the upload and download model is initialized!
     */
    public PersistenceManager(final UploadModel um, final DownloadModel dm) throws Throwable {

        showExternalItemsDownload = Core.frostSettings.getBoolean(Settings.GQ_SHOW_EXTERNAL_ITEMS_DOWNLOAD);
        showExternalItemsUpload = Core.frostSettings.getBoolean(Settings.GQ_SHOW_EXTERNAL_ITEMS_UPLOAD);

        if (FcpHandler.inst().getFreenetNode() == null) {
            throw new Exception("No freenet nodes defined");
        }
        final NodeAddress na = FcpHandler.inst().getFreenetNode();
        fcpConn = FcpListenThreadConnection.createInstance(na);
        fcpTools = new FcpMultiRequestConnectionFileTransferTools(fcpConn);

        Core.frostSettings.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                if( evt.getPropertyName().equals(Settings.GQ_SHOW_EXTERNAL_ITEMS_DOWNLOAD) ) {
                    showExternalItemsDownload = Core.frostSettings.getBoolean(Settings.GQ_SHOW_EXTERNAL_ITEMS_DOWNLOAD);
                    if( showExternalItemsDownload ) {
                        // get external items
                        showExternalDownloadItems();
                    }
                } else if( evt.getPropertyName().equals(Settings.GQ_SHOW_EXTERNAL_ITEMS_UPLOAD) ) {
                    showExternalItemsUpload = Core.frostSettings.getBoolean(Settings.GQ_SHOW_EXTERNAL_ITEMS_UPLOAD);
                    if( showExternalItemsUpload ) {
                        // get external items
                        showExternalUploadItems();
                    }
                }
            }
        });

        uploadModel = um;
        downloadModel = dm;

        // initially get all items from model
        for(int x=0; x < uploadModel.getItemCount(); x++) {
			final FrostUploadItem ul = uploadModel.getItemAt(x);
            if( ul.getGqIdentifier() != null ) {
                uploadModelItems.put(ul.getGqIdentifier(), ul);
            }
        }
        for(int x=0; x < downloadModel.getItemCount(); x++) {
			final FrostDownloadItem ul = downloadModel.getItemAt(x);
            if( ul.getGqIdentifier() != null ) {
                downloadModelItems.put(ul.getGqIdentifier(), ul);
            }
        }

        // enqueue listeners to keep updated about the model items
        uploadModel.addOrderedModelListener(
                new SortedModelListener<FrostUploadItem>() {
                    public void modelCleared() {
                        for( final FrostUploadItem ul : uploadModelItems.values() ) {
                            if( ul.isExternal() == false ) {
                                fcpTools.removeRequest(ul.getGqIdentifier());
                            }
                        }
                        uploadModelItems.clear();
                    }
                    public void itemAdded(final int position, final FrostUploadItem item) {
                        uploadModelItems.put(item.getGqIdentifier(), item);
                        if( !item.isExternal() ) {
                            // maybe start immediately
                            startNewUploads();
                        }
                    }
                    public void itemChanged(final int position, final FrostUploadItem item) {
                    }
                    public void itemsRemoved(final int[] positions, final List<FrostUploadItem> items) {
                        for(final FrostUploadItem item : items) {
                            uploadModelItems.remove(item.getGqIdentifier());
                            if( item.isExternal() == false ) {
                                fcpTools.removeRequest(item.getGqIdentifier());
                            }
                        }
                    }
                });

        downloadModel.addOrderedModelListener(
                new SortedModelListener<FrostDownloadItem>() {
                    public void modelCleared() {
                        for( final FrostDownloadItem ul : downloadModelItems.values() ) {
                            if( ul.isExternal() == false ) {
                                fcpTools.removeRequest(ul.getGqIdentifier());
                            }
                        }
                        downloadModelItems.clear();
                    }
                    public void itemAdded(final int position, final FrostDownloadItem item) {
                        downloadModelItems.put(item.getGqIdentifier(), item);
                        if( !item.isExternal() ) {
                            // maybe start immediately
                            startNewDownloads();
                        }
                    }
                    public void itemChanged(final int position, final FrostDownloadItem item) {
                    }
                    public void itemsRemoved(final int[] positions, final List<FrostDownloadItem> items) {
                        for(final FrostDownloadItem item : items) {
                            downloadModelItems.remove(item.getGqIdentifier());
                            if( item.isExternal() == false ) {
                                fcpTools.removeRequest(item.getGqIdentifier());
                            }
                        }
                    }
                });

        directTransferQueue = new DirectTransferQueue();
        directTransferThread = new DirectTransferThread();

        persistentQueue = new FcpPersistentQueue(fcpTools, this);
    }

    public FcpMultiRequestConnectionFileTransferTools getFcpTools() {
    	return fcpTools;
    }

    public void startThreads() {
        directTransferThread.start();
        persistentQueue.startThreads();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                maybeStartRequests();
            }
        };
        Core.schedule(task, 3000, 3000);
    }

    public void removeRequests(final List<String> requests) {
        for( final String id : requests ) {
            fcpTools.removeRequest(id);
        }
    }

    
    /**
     * @param dlItem  items whose global identifier is to check
     * @return  true if this item is currently in the global queue, no matter in what state
     */
    public boolean isItemInGlobalQueue(final FrostDownloadItem dlItem) {
        return persistentQueue.isIdInGlobalQueue(dlItem.getGqIdentifier());
    }
    /**
     * @param ulItem  items whose global identifier is to check
     * @return  true if this item is currently in the global queue, no matter in what state
     */
    public boolean isItemInGlobalQueue(final FrostUploadItem ulItem) {
        return persistentQueue.isIdInGlobalQueue(ulItem.getGqIdentifier());
    }

    /**
     * Periodically check if we could start a new request.
     * This could be done better if we check if a request finished, but later...
     */
    private void maybeStartRequests() {
        // start new requests
        startNewUploads();
        startNewDownloads();
    }

    public void connected() {
        isConnected = true;
        MainFrame.getInstance().setConnected();
        logger.info("now connected");
    }
    public void disconnected() {
        isConnected = false;

        MainFrame.getInstance().setDisconnected();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                uploadModel.removeExternalUploads();
                downloadModel.removeExternalDownloads();
            }
        });
        logger.info("disconnected!");
    }

    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Enqueue a direct GET if not already enqueued, or already downloaded to download dir.
     * @return true if item was enqueued
     */
    public boolean maybeEnqueueDirectGet(final FrostDownloadItem dlItem, final long expectedFileSize) {
        if( !isDirectTransferInProgress(dlItem) ) {
            final File targetFile = new File(dlItem.getDownloadFilename());
            if( !targetFile.isFile() || targetFile.length() != expectedFileSize ) {
                directTransferQueue.appendItemToQueue(dlItem);
                return true;
            }
        }
        return false;
    }

    private void applyPriority(final FrostDownloadItem dlItem, final FcpPersistentGet getReq) {
        // apply externally changed priority
        if( dlItem.getPriority() != getReq.getPriority() ) {
            if (Core.frostSettings.getBoolean(Settings.FCP2_ENFORCE_FROST_PRIO_FILE_DOWNLOAD)) {
                // reset priority with our current value
                fcpTools.changeRequestPriority(getReq.getIdentifier(), dlItem.getPriority());
            } else {
                // apply to downloaditem
                dlItem.setPriority(getReq.getPriority());
            }
        }
    }

    /**
     * Apply the states of FcpRequestGet to the FrostDownloadItem.
     */
    private void applyState(final FrostDownloadItem dlItem, final FcpPersistentGet getReq) {
        // when cancelled and we expect this, don't set failed; don't even set the old priority!
        if( dlItem.isInternalRemoveExpected() && getReq.isFailed() ) {
            final int returnCode = getReq.getCode();
            if( returnCode == 25 ) {
                return;
            }
        }

        applyPriority(dlItem, getReq);

        if( dlItem.isDirect() != getReq.isDirect() ) {
            dlItem.setDirect(getReq.isDirect());
        }

        if( !getReq.isProgressSet() && !getReq.isSuccess() && !getReq.isFailed() ) {
            if( dlItem.getState() == FrostDownloadItem.STATE_WAITING ) {
                dlItem.setState(FrostDownloadItem.STATE_PROGRESS);
            }
            return;
        }

        if( getReq.isProgressSet() ) {
            final int doneBlocks = getReq.getDoneBlocks();
            final int requiredBlocks = getReq.getRequiredBlocks();
            final int totalBlocks = getReq.getTotalBlocks();
            final boolean isFinalized = getReq.isFinalized();
            if( totalBlocks > 0 ) {
                dlItem.setDoneBlocks(doneBlocks);
                dlItem.setRequiredBlocks(requiredBlocks);
                dlItem.setTotalBlocks(totalBlocks);
                dlItem.setFinalized(isFinalized);
                dlItem.fireValueChanged();
            }
            if( dlItem.getState() != FrostDownloadItem.STATE_PROGRESS ) {
                dlItem.setState(FrostDownloadItem.STATE_PROGRESS);
            }
        }
        if( getReq.isSuccess() ) {
            // maybe progress was not completely sent
            dlItem.setFinalized(true);
            if( dlItem.getTotalBlocks() > 0 && dlItem.getDoneBlocks() < dlItem.getRequiredBlocks() ) {
                dlItem.setDoneBlocks(dlItem.getRequiredBlocks());
                dlItem.fireValueChanged();
            }
            if( dlItem.isExternal() ) {
                dlItem.setFileSize(getReq.getFilesize());
                dlItem.setState(FrostDownloadItem.STATE_DONE);
            } else {
                if( dlItem.isDirect() ) {
                    maybeEnqueueDirectGet(dlItem, getReq.getFilesize());
                } else {
                    final FcpResultGet result = new FcpResultGet(true);
                    final File targetFile = new File(dlItem.getDownloadFilename());
                    FileTransferManager.inst().getDownloadManager().notifyDownloadFinished(dlItem, result, targetFile);
                }
            }
        }
        if( getReq.isFailed() ) {
            final String desc = getReq.getCodeDesc();
            if( dlItem.isExternal() ) {
                dlItem.setState(FrostDownloadItem.STATE_FAILED);
                dlItem.setErrorCodeDescription(desc);
            } else {
                final int returnCode = getReq.getCode();
                final boolean isFatal = getReq.isFatal();

                final String redirectURI = getReq.getRedirectURI();
                final FcpResultGet result = new FcpResultGet(false, returnCode, desc, isFatal, redirectURI);
                final File targetFile = new File(dlItem.getDownloadFilename());
                final boolean retry = FileTransferManager.inst().getDownloadManager().notifyDownloadFinished(dlItem, result, targetFile);
                if( retry ) {
                    fcpTools.removeRequest(getReq.getIdentifier());
                    startDownload(dlItem); // restart immediately
                }
            }
        }
    }

    /**
     * Apply the states of FcpRequestPut to the FrostUploadItem.
     */
    private void applyState(final FrostUploadItem ulItem, final FcpPersistentPut putReq) {

        // when cancelled and we expect this, don't set failed; don't even set the old priority!
        if( ulItem.isInternalRemoveExpected() && putReq.isFailed() ) {
            final int returnCode = putReq.getCode();
            if( returnCode == 25 ) {
                return;
            }
        }

        if( directPUTsWithoutAnswer.contains(ulItem.getGqIdentifier()) ) {
            // we got an answer
            directPUTsWithoutAnswer.remove(ulItem.getGqIdentifier());
        }

        // apply externally changed priority
        if( ulItem.getPriority() != putReq.getPriority() ) {
            if (Core.frostSettings.getBoolean(Settings.FCP2_ENFORCE_FROST_PRIO_FILE_UPLOAD)) {
                // reset priority with our current value
                fcpTools.changeRequestPriority(putReq.getIdentifier(), ulItem.getPriority());
            } else {
                // apply to uploaditem
                ulItem.setPriority(putReq.getPriority());
            }
        }

        if( !putReq.isProgressSet() && !putReq.isSuccess() && !putReq.isFailed() ) {
            if( ulItem.getState() == FrostUploadItem.STATE_WAITING ) {
                ulItem.setState(FrostUploadItem.STATE_PROGRESS);
            }
            return;
        }

        if( putReq.isProgressSet() ) {
            final int doneBlocks = putReq.getDoneBlocks();
            final int totalBlocks = putReq.getTotalBlocks();
            final boolean isFinalized = putReq.isFinalized();
            if( totalBlocks > 0 ) {
                ulItem.setDoneBlocks(doneBlocks);
                ulItem.setTotalBlocks(totalBlocks);
                ulItem.setFinalized(isFinalized);
                ulItem.fireValueChanged();
            }
            if( ulItem.getState() != FrostUploadItem.STATE_PROGRESS ) {
                ulItem.setState(FrostUploadItem.STATE_PROGRESS);
            }
        }
        if( putReq.isSuccess() ) {
            // maybe progress was not completely sent
            ulItem.setFinalized(true);
            if( ulItem.getTotalBlocks() > 0 && ulItem.getDoneBlocks() != ulItem.getTotalBlocks() ) {
                ulItem.setDoneBlocks(ulItem.getTotalBlocks());
            }
            final String chkKey = putReq.getUri();
            if( ulItem.isExternal() ) {
                ulItem.setState(FrostUploadItem.STATE_DONE);
                ulItem.setKey(chkKey);
            } else {
                final FcpResultPut result = new FcpResultPut(FcpResultPut.Success, chkKey);
                FileTransferManager.inst().getUploadManager().notifyUploadFinished(ulItem, result);
            }
        }
        if( putReq.isFailed() ) {
            final String desc = putReq.getCodeDesc();
            if( ulItem.isExternal() ) {
                ulItem.setState(FrostUploadItem.STATE_FAILED);
                ulItem.setErrorCodeDescription(desc);
            } else {
                final int returnCode = putReq.getCode();
                final boolean isFatal = putReq.isFatal();

                final FcpResultPut result;
                if( returnCode == 9 ) {
                    result = new FcpResultPut(FcpResultPut.KeyCollision, returnCode, desc, isFatal);
                } else if( returnCode == 5 ) {
                    result = new FcpResultPut(FcpResultPut.Retry, returnCode, desc, isFatal);
                } else {
                    result = new FcpResultPut(FcpResultPut.Error, returnCode, desc, isFatal);
                }
                FileTransferManager.inst().getUploadManager().notifyUploadFinished(ulItem, result);
            }
        }
    }

    private void startNewUploads() {
        boolean isLimited = true;
        int currentAllowedUploadCount = 0;
        {
            final int allowedConcurrentUploads = Core.frostSettings.getInteger(Settings.UPLOAD_MAX_THREADS);
            if( allowedConcurrentUploads <= 0 ) {
                isLimited = false;
            } else {
                int runningUploads = 0;
                for(final FrostUploadItem ulItem : uploadModelItems.values() ) {
                    if( !ulItem.isExternal() && ulItem.getState() == FrostUploadItem.STATE_PROGRESS) {
                        runningUploads++;
                    }
                }
                currentAllowedUploadCount = allowedConcurrentUploads - runningUploads;
                if( currentAllowedUploadCount < 0 ) {
                    currentAllowedUploadCount = 0;
                }
            }
        }
        {
            while( !isLimited || currentAllowedUploadCount > 0 ) {
                final FrostUploadItem ulItem = FileTransferManager.inst().getUploadManager().selectNextUploadItem();
                if( ulItem == null ) {
                    break;
                }
                if( startUpload(ulItem) ) {
                    currentAllowedUploadCount--;
                }
            }
        }
    }

    public boolean startUpload(final FrostUploadItem ulItem) {
        if( ulItem == null || ulItem.getState() != FrostUploadItem.STATE_WAITING ) {
            return false;
        }

        ulItem.setUploadStartedMillis(System.currentTimeMillis());

        ulItem.setState(FrostUploadItem.STATE_PROGRESS);

        // start the upload
        final boolean doMime;
        final boolean setTargetFileName;
        if( ulItem.isSharedFile() ) {
            doMime = false;
            setTargetFileName = false;
        } else {
            doMime = true;
            setTargetFileName = true;
        }
        
        // try to start using DDA
        boolean isDda = fcpTools.startPersistentPutUsingDda(
                ulItem.getGqIdentifier(),
                ulItem.getFile(),
                ulItem.getFileName(),
                doMime,
                setTargetFileName,
                ulItem.getCompress(),
                ulItem.getFreenetCompatibilityMode(),
                ulItem.getPriority()
        );

        if( !isDda ) {
            // upload was not startet because DDA is not allowed...
            // if UploadManager selected this file then it is not already in progress!
            directTransferQueue.appendItemToQueue(ulItem);
        }
        return true;
    }

    private void startNewDownloads() {
        boolean isLimited = true;
        int currentAllowedDownloadCount = 0;
        {
            final int allowedConcurrentDownloads = Core.frostSettings.getInteger(Settings.DOWNLOAD_MAX_THREADS);
            if( allowedConcurrentDownloads <= 0 ) {
                isLimited = false;
            } else {
                int runningDownloads = 0;
                for(final FrostDownloadItem dlItem : downloadModelItems.values() ) {
                    if( !dlItem.isExternal() && dlItem.getState() == FrostDownloadItem.STATE_PROGRESS) {
                        runningDownloads++;
                    }
                }
                currentAllowedDownloadCount = allowedConcurrentDownloads - runningDownloads;
                if( currentAllowedDownloadCount < 0 ) {
                    currentAllowedDownloadCount = 0;
                }
            }
        }
        {
            while( !isLimited || currentAllowedDownloadCount > 0 ) {
                final FrostDownloadItem dlItem = FileTransferManager.inst().getDownloadManager().selectNextDownloadItem();
                if (dlItem == null) {
                    break;
                }
                // start the download
                if( startDownload(dlItem) ) {
                    currentAllowedDownloadCount--;
                }
            }
        }
    }

    public boolean startDownload(final FrostDownloadItem dlItem) {

        if( dlItem == null || dlItem.getState() != FrostDownloadItem.STATE_WAITING ) {
            return false;
        }

        dlItem.setDownloadStartedTime(System.currentTimeMillis());

        dlItem.setState(FrostDownloadItem.STATE_PROGRESS);

        final String gqid = dlItem.getGqIdentifier();
        final File targetFile = new File(dlItem.getDownloadFilename());
        boolean isDda = fcpTools.startPersistentGet(
                dlItem.getKey(),
                gqid,
                targetFile,
                dlItem.getPriority()
        );
        dlItem.setDirect( !isDda );

        return true;
    }

    private void showExternalUploadItems() {
        final Map<String,FcpPersistentPut> items = persistentQueue.getUploadRequests();
        for(final FcpPersistentPut uploadRequest : items.values() ) {
            if( !uploadModelItems.containsKey(uploadRequest.getIdentifier()) ) {
                addExternalItem(uploadRequest);
            }
        }
    }

    private void showExternalDownloadItems() {
        final Map<String,FcpPersistentGet> items = persistentQueue.getDownloadRequests();
        for(final FcpPersistentGet downloadRequest : items.values() ) {
            if( !downloadModelItems.containsKey(downloadRequest.getIdentifier()) ) {
                addExternalItem(downloadRequest);
            }
        }
    }

    private void addExternalItem(final FcpPersistentPut uploadRequest) {
        final FrostUploadItem ulItem = new FrostUploadItem();
        ulItem.setGqIdentifier(uploadRequest.getIdentifier());
        ulItem.setExternal(true);
        // direct uploads maybe have no filename, use identifier
        String fileName = uploadRequest.getFilename();
        if( fileName == null ) {
            fileName = uploadRequest.getIdentifier();
        } else if( fileName.indexOf('/') > -1 || fileName.indexOf('\\') > -1 ) {
            // filename contains directories, use only filename
            final String stmp = new File(fileName).getName();
            if( stmp.length() > 0 ) {
                fileName = stmp; // use plain filename
            }
        }
        ulItem.setFile(new File(fileName));
        ulItem.setFileName(fileName);
        ulItem.setFileSize(uploadRequest.getFileSize());
        ulItem.setPriority(uploadRequest.getPriority());
        
        ulItem.setState(FrostUploadItem.STATE_PROGRESS);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                uploadModel.addExternalItem(ulItem);
            }
        });
        applyState(ulItem, uploadRequest);
    }

    private void addExternalItem(final FcpPersistentGet downloadRequest) {
        // direct downloads maybe have no filename, use identifier
        String fileName = downloadRequest.getFilename();
        if( fileName == null ) {
            fileName = downloadRequest.getIdentifier();
        } else if( fileName.indexOf('/') > -1 || fileName.indexOf('\\') > -1 ) {
            // filename contains directories, use only filename
            final String stmp = new File(fileName).getName();
            if( stmp.length() > 0 ) {
                fileName = stmp; // use plain filename
            }
        }
        final FrostDownloadItem dlItem = new FrostDownloadItem(
                fileName,
                downloadRequest.getUri());
        dlItem.setExternal(true);
        dlItem.setGqIdentifier(downloadRequest.getIdentifier());
        dlItem.setState(FrostDownloadItem.STATE_PROGRESS);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                downloadModel.addExternalItem(dlItem);
            }
        });
        applyState(dlItem, downloadRequest);
    }

    public boolean isDirectTransferInProgress(final FrostDownloadItem dlItem) {
        final String id = dlItem.getGqIdentifier();
        return directGETsInProgress.contains(id);
    }

    public boolean isDirectTransferInProgress(final FrostUploadItem ulItem) {
        final String id = ulItem.getGqIdentifier();
        if( directPUTsInProgress.contains(id) ) {
            return true;
        }
        if( directPUTsWithoutAnswer.contains(id) ) {
            return true;
        }
        return false;
    }

    private class DirectTransferThread extends Thread {

        @Override
        public void run() {

            final int maxAllowedExceptions = 5;
            int catchedExceptions = 0;

            while(true) {
                try {
                    // if there is no work in queue this call waits for a new queue item
                    final ModelItem<?> item = directTransferQueue.getItemFromQueue();

                    if( item == null ) {
                        // paranoia, should never happen
                        Mixed.wait(5*1000);
                        continue;
                    }

                    if( item instanceof FrostUploadItem ) {
                        // transfer bytes to node
                        final FrostUploadItem ulItem = (FrostUploadItem) item;
                        // FIXME: provide item, state=Transfer to node, % shows progress
                        final String gqid = ulItem.getGqIdentifier();
                        final boolean doMime;
                        final boolean setTargetFileName;
                        if( ulItem.isSharedFile() ) {
                            doMime = false;
                            setTargetFileName = false;
                        } else {
                            doMime = true;
                            setTargetFileName = true;
                        }
                        final NodeMessage answer = fcpTools.startDirectPersistentPut(gqid, ulItem.getFile(), ulItem.getFileName(), doMime, setTargetFileName, ulItem.getCompress(), ulItem.getFreenetCompatibilityMode(), ulItem.getPriority());
                        if( answer == null ) {
                            final String desc = "Could not open a new FCP2 socket for direct put!";
                            final FcpResultPut result = new FcpResultPut(FcpResultPut.Error, -1, desc, false);
                            FileTransferManager.inst().getUploadManager().notifyUploadFinished(ulItem, result);

                            logger.error("{}", desc);
                        } else {
                            // wait for an answer, don't start request again
                            directPUTsWithoutAnswer.add(gqid);
                        }

                        directPUTsInProgress.remove(gqid);

                    } else if( item instanceof FrostDownloadItem ) {
                        // transfer bytes from node
                        final FrostDownloadItem dlItem = (FrostDownloadItem) item;
                        // FIXME: provide item, state=Transfer from node, % shows progress
                        final String gqid = dlItem.getGqIdentifier();
                        final File targetFile = new File(dlItem.getDownloadFilename());

                        final boolean retryNow;
                        NodeMessage answer = null;

                        try {
                            answer = fcpTools.startDirectPersistentGet(gqid, targetFile);
                        } catch (final FileNotFoundException e) {
                            logger.error("Could not write to {}: ", dlItem.getDownloadFilename(), e);
                        }

                        if( answer != null ) {
                            final FcpResultGet result = new FcpResultGet(true);
                            FileTransferManager.inst().getDownloadManager().notifyDownloadFinished(dlItem, result, targetFile);
                            retryNow = false;
                        } else {
                            logger.error("Could not open a new fcp socket for direct get!");
                            final FcpResultGet result = new FcpResultGet(false);
                            retryNow = FileTransferManager.inst().getDownloadManager().notifyDownloadFinished(dlItem, result, targetFile);
                        }

                        directGETsInProgress.remove(gqid);

                        if( retryNow ) {
                            startDownload(dlItem);
                        }
                    }

                } catch(final Throwable t) {
                    logger.error("Exception catched", t);
                    catchedExceptions++;
                }

                if( catchedExceptions > maxAllowedExceptions ) {
                    logger.error("Stopping DirectTransferThread because of too much exceptions");
                    break;
                }
            }
        }
    }

    /**
     * A queue class that queues items waiting for its direct transfer (put to node or get from node).
     */
    private class DirectTransferQueue {

        private final LinkedList<ModelItem<?>> queue = new LinkedList<ModelItem<?>>();

        public synchronized ModelItem<?> getItemFromQueue() {
            try {
                // let dequeueing threads wait for work
                while( queue.isEmpty() ) {
                    wait();
                }
            } catch (final InterruptedException e) {
                return null; // waiting abandoned
            }

            if( queue.isEmpty() == false ) {
                return queue.removeFirst();
            }
            return null;
        }

        public synchronized void appendItemToQueue(final FrostDownloadItem item) {
            final String id = item.getGqIdentifier();
            directGETsInProgress.add(id);

            queue.addLast(item);
            notifyAll(); // notify all waiters (if any) of new record
        }

        public synchronized void appendItemToQueue(final FrostUploadItem item) {
            final String id = item.getGqIdentifier();
            directPUTsInProgress.add(id);

            queue.addLast(item);
            notifyAll(); // notify all waiters (if any) of new record
        }

    }

    public void persistentRequestError(final String id, final NodeMessage nm) {
        if( uploadModelItems.containsKey(id) ) {
            final FrostUploadItem item = uploadModelItems.get(id);
            item.setEnabled(false);
            item.setState(FrostUploadItem.STATE_FAILED);
            item.setErrorCodeDescription(nm.getStringValue("CodeDescription"));
        } else if( downloadModelItems.containsKey(id) ) {
            final FrostDownloadItem item = downloadModelItems.get(id);
            item.setEnabled(false);
            item.setState(FrostDownloadItem.STATE_FAILED);
            item.setErrorCodeDescription(nm.getStringValue("CodeDescription"));
        } else {
            logger.warn("persistentRequestError: ID not in any model: {}", id);
        }
    }

    public void persistentRequestAdded(final FcpPersistentPut uploadRequest) {
        final FrostUploadItem ulItem = uploadModelItems.get(uploadRequest.getIdentifier());
        if( ulItem != null ) {
            // own item added to global queue, or existing external item
            applyState(ulItem, uploadRequest);
        } else {
            if( showExternalItemsUpload ) {
                addExternalItem(uploadRequest);
            }
        }
    }

    public void persistentRequestAdded(final FcpPersistentGet downloadRequest) {
        final FrostDownloadItem dlItem = downloadModelItems.get(downloadRequest.getIdentifier());
        if( dlItem != null ) {
            // own item added to global queue, or existing external item
            applyState(dlItem, downloadRequest);
        } else {
            if ( showExternalItemsDownload ) {
                addExternalItem(downloadRequest);
            }
        }
    }

    public void persistentRequestModified(final FcpPersistentPut uploadRequest) {
        if( uploadModelItems.containsKey(uploadRequest.getIdentifier()) ) {
            final FrostUploadItem ulItem = uploadModelItems.get(uploadRequest.getIdentifier());
            ulItem.setPriority(uploadRequest.getPriority());
        }
    }

    public void persistentRequestModified(final FcpPersistentGet downloadRequest) {
        if( downloadModelItems.containsKey(downloadRequest.getIdentifier()) ) {
            final FrostDownloadItem dlItem = downloadModelItems.get(downloadRequest.getIdentifier());
            applyPriority(dlItem, downloadRequest);
        }
    }

    public void persistentRequestRemoved(final FcpPersistentPut uploadRequest) {
        if( uploadModelItems.containsKey(uploadRequest.getIdentifier()) ) {
            final FrostUploadItem ulItem = uploadModelItems.get(uploadRequest.getIdentifier());
            if( ulItem.isExternal() ) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                    	List<FrostUploadItem> itemList = new ArrayList<FrostUploadItem>();
                    	itemList.add(ulItem);
                        uploadModel.removeItems(itemList);
                    }
                });
            } else {
                if( ulItem.isInternalRemoveExpected() ) {
                    ulItem.setInternalRemoveExpected(false); // clear flag
                } else if( ulItem.getState() != FrostUploadItem.STATE_DONE ) {
                    ulItem.setEnabled(false);
                    ulItem.setState(FrostUploadItem.STATE_FAILED);
                    ulItem.setErrorCodeDescription("Disappeared from global queue");
                }
            }
        }
    }

    public void persistentRequestRemoved(final FcpPersistentGet downloadRequest) {
        if( downloadModelItems.containsKey(downloadRequest.getIdentifier()) ) {
            final FrostDownloadItem dlItem = downloadModelItems.get(downloadRequest.getIdentifier());
            if( dlItem.isExternal() ) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                    	List<FrostDownloadItem> itemList = new ArrayList<FrostDownloadItem>();
                    	itemList.add(dlItem);
                        downloadModel.removeItems(itemList);
                    }
                });
            } else {
                if( dlItem.isInternalRemoveExpected() ) {
                    dlItem.setInternalRemoveExpected(false); // clear flag
                } else if( dlItem.getState() != FrostDownloadItem.STATE_DONE ) {
                    dlItem.setEnabled(false);
                    dlItem.setState(FrostDownloadItem.STATE_FAILED);
                    dlItem.setErrorCodeDescription("Disappeared from global queue");
                }
            }
        }
    }

    public void persistentRequestUpdated(final FcpPersistentPut uploadRequest) {
        final FrostUploadItem ui = uploadModelItems.get(uploadRequest.getIdentifier());
        if( ui == null ) {
            // not (yet) in our model
            return;
        }
        applyState(ui, uploadRequest);
    }

    public void persistentRequestUpdated(final FcpPersistentGet downloadRequest) {
        final FrostDownloadItem dl = downloadModelItems.get( downloadRequest.getIdentifier() );
        if( dl == null ) {
            // not (yet) in our model
            return;
        }
        applyState(dl, downloadRequest);
    }
}
