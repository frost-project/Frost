/*
  UploadTicker.java / Frost
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

import frost.Core;
import frost.Settings;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.PersistenceManager;
import frost.util.Mixed;

public class UploadTicker extends Thread {

    //To be able to increase this value, we have to add support for that. Without it,
    //the loops in generateCHK and prepareUpladHashes would process the same file
    //several times.
    private final int MAX_GENERATING_THREADS = 1;

    private final UploadModel model;

    private int removeNotExistingFilesCounter = 0;

    /**
     * The number of allocated threads is used to limit the total of threads
     * that can be running at a given time, whereas the number of running
     * threads is the number of threads that are actually running.
     */
    private int allocatedUploadingThreads = 0;
    private int allocatedGeneratingThreads = 0;
    private int runningUploadingThreads = 0;
    private int runningGeneratingThreads = 0;

    private final Object uploadingCountLock = new Object();
    private final Object generatingCountLock = new Object();

    public UploadTicker(final UploadModel newModel) {
        super("Upload");
        model = newModel;
    }

    /**
     * This method is called to find out if a new uploading thread can start. It
     * temporarily allocates it and it will have to be relased when it is no longer
     * needed (no matter whether the thread was actually used or not).
     * @return true if a new uploading thread can start. False otherwise.
     */
    private void allocateUploadingThread() {
        synchronized (uploadingCountLock) {
            allocatedUploadingThreads++;
        }
    }

    private boolean canAllocateUploadingThread() {
        synchronized (uploadingCountLock) {
            if (allocatedUploadingThreads < Core.frostSettings.getInteger(Settings.UPLOAD_MAX_THREADS)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is called to find out if a new generating thread can start. It
     * temporarily allocates it and it will have to be relased when it is no longer
     * needed (no matter whether the thread was actually used or not).
     * @return true if a new generating thread can start. False otherwise.
     */
    private boolean allocateGeneratingThread() {
        synchronized (generatingCountLock) {
            if (allocatedGeneratingThreads < MAX_GENERATING_THREADS) {
                allocatedGeneratingThreads++;
                return true;
            }
        }
        return false;
    }

    /**
     * This method is called from a generating thread to notify the ticker that
     * the thread has started (so that it can notify its listeners of the fact)
     */
    public void generatingThreadStarted() {
        runningGeneratingThreads++;
    }

    /**
     * This method is usually called from a generating thread to notify the ticker that
     * the thread has finished (so that it can notify its listeners of the fact). It also
     * releases the thread so that new generating threads can start if needed.
     */
    public void generatingThreadFinished() {
        runningGeneratingThreads--;
        releaseGeneratingThread();
    }

    /**
     * This method is called from an uploading thread to notify the ticker that
     * the thread has started (so that it can notify its listeners of the fact)
     */
    void uploadingThreadStarted() {
        runningUploadingThreads++;
    }

    /**
     * This method is called from an uploading thread to notify the ticker that the
     * thread has finished (so that it can notify its listeners of the fact). It also
     * releases the thread so that new generating threads can start if needed.
     */
    void uploadThreadFinished() {
        runningUploadingThreads--;
        releaseUploadingThread();
    }

    /**
     * This method is used to release an uploading thread.
     */
    private void releaseUploadingThread() {
        synchronized (uploadingCountLock) {
            if (allocatedUploadingThreads > 0) {
                allocatedUploadingThreads--;
            }
        }
    }

    /**
     * This method is used to release a generating thread.
     */
    private void releaseGeneratingThread() {
        synchronized (generatingCountLock) {
            if (allocatedGeneratingThreads > 0) {
                allocatedGeneratingThreads--;
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        super.run();
        while (true) {
            Mixed.wait(1000);
            // this is executed each second, so this counter counts seconds
            if (Core.frostSettings.getBoolean(Settings.UPLOAD_REMOVE_NOT_EXISTING_FILES)) {
            removeNotExistingFilesCounter++;
            removeNotExistingFiles();
            }
            generateCHKs();
            if( PersistenceManager.isPersistenceEnabled() == false ) {
                startUploadThread();
            }
        }
    }

    /**
     * This method generates CHK's for upload table entries
     */
    private void generateCHKs() {

        if (Core.isFreenetOnline() && allocateGeneratingThread()) {
            boolean threadLaunched = false;

            for (int i = 0; i < model.getItemCount() && !threadLaunched; i++) {
				final FrostUploadItem ulItem = model.getItemAt(i);
                // encode if requested by user
                if (ulItem.getState() == FrostUploadItem.STATE_ENCODING_REQUESTED) {
                    // next state will be IDLE (=default)
                    final GenerateChkThread newInsert = new GenerateChkThread(this, ulItem);
                    ulItem.setState(FrostUploadItem.STATE_ENCODING);
                    newInsert.start();
                    threadLaunched = true;  // start only 1 thread per loop (=second)
                }
            }
            if (!threadLaunched) {
                releaseGeneratingThread();
            }
        }
    }

    /**
     * Maybe start a new upload automatically.
     */
    private void startUploadThread() {
        if (Core.isFreenetOnline() && canAllocateUploadingThread()) {
            final FrostUploadItem uploadItem = FileTransferManager.inst().getUploadManager().selectNextUploadItem();
            startUpload(uploadItem);
        }
    }

    public boolean startUpload(final FrostUploadItem ulItem) {
        if (!Core.isFreenetOnline() ) {
            return false;
        }
        if( ulItem == null || ulItem.getState() != FrostUploadItem.STATE_WAITING ) {
            return false;
        }

        // increase allocated threads
        allocateUploadingThread();

        ulItem.setUploadStartedMillis(System.currentTimeMillis());

        ulItem.setState(FrostUploadItem.STATE_PROGRESS);
        boolean doMime;
        // shared files are always inserted as octet-stream
        if( ulItem.isSharedFile() ) {
            doMime = false;
        } else {
            doMime = true;
        }
        final UploadThread newInsert = new UploadThread(this, ulItem, doMime);
        newInsert.start();
        return true;
    }

    private void removeNotExistingFiles() {
        // Check uploadTable every 5 minutes
        if (removeNotExistingFilesCounter >= 5*60 ) {
            model.removeNotExistingFiles();
            removeNotExistingFilesCounter = 0;
        }
    }

    /**
     * This method returns the number of generating threads that are running
     * @return the number of generating threads that are running
     */
    public int getRunningGeneratingThreads() {
        return runningGeneratingThreads;
    }

    /**
     * This method returns the number of uploading threads that are running
     * @return the number of uploading threads that are running
     */
    public int getRunningUploadingThreads() {
        return runningUploadingThreads;
    }
}
