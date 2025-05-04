/*
  DownloadTicker.java / Frost

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
package frost.fileTransfer.download;

import java.io.File;
import java.time.Duration;
import java.time.Instant;

import frost.Core;
import frost.Settings;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.PersistenceManager;
import frost.util.Mixed;

public class DownloadTicker extends Thread {

	private final DownloadPanel panel;

	/**
	 * The number of allocated threads is used to limit the total of threads
	 * that can be running at a given time, whereas the number of running
	 * threads is the number of threads that are actually running.
	 */
	private int allocatedThreads = 0;
	private int runningThreads = 0;

	private final Object threadCountLock = new Object();

	public DownloadTicker(final DownloadPanel newPanel) {
		super("Download");
		panel = newPanel;
	}

	/**
	 * This method is called to find out if a new thread can start. It temporarily
	 * allocates it and it will have to be relased when it is no longer
	 * needed (no matter whether the thread was actually used or not).
	 * @return true if a new thread can start. False otherwise.
	 */
	private void allocateDownloadThread() {
		synchronized (threadCountLock) {
			allocatedThreads++;
		}
	}

    private boolean canAllocateDownloadThread() {
         synchronized (threadCountLock) {
             if (allocatedThreads < Core.frostSettings.getInteger(Settings.DOWNLOAD_MAX_THREADS)) {
                 return true;
             }
         }
         return false;
    }

	/**
	 * This method is used to release a thread.
	 */
	private void releaseThread() {
		synchronized (threadCountLock) {
			if (allocatedThreads > 0) {
				allocatedThreads--;
			}
		}
	}

	/**
	 * This method returns the number of threads that are running
	 * @return the number of threads that are running
	 */
	public int getRunningThreads() {
		return runningThreads;
	}

	@Override
    public void run() {
		super.run();
		Instant loopEnd = null;
		Instant loopStart = Instant.now();
		while (true) {
			Mixed.wait(1000);
			
			if( PersistenceManager.isPersistenceEnabled() == false ) {
				startDownloadThread();
			} else {

				// No need to check every second if persistance was enabled
				Mixed.wait(29000);
			}

			loopEnd = Instant.now();
			long seconds = Duration.between(loopStart, loopEnd).toSeconds();
			if( seconds >= 60 ) {
				increaseDownloadItemRuntime(Math.toIntExact(seconds));
				loopStart = loopEnd;
			}
		}
	}

	/**
	 * Increase the runtime of shared, running download items.
	 * Called each X seconds, adds the specified amount of seconds to the runtime.
	 */
	private void increaseDownloadItemRuntime(int incSecs) {
	    // if we use persistence, check if we are currently connected
	    if( FileTransferManager.inst().getPersistenceManager() != null ) {
	        if( !FileTransferManager.inst().getPersistenceManager().isConnected() ) {
	            // we are not connected, don't count runtime
	            return;
	        }
	    }

	    final DownloadModel model = FileTransferManager.inst().getDownloadManager().getModel();
	    for(int x=0; x < model.getItemCount(); x++ ) {
			final FrostDownloadItem item = model.getItemAt(x);
	        if( item == null ) {
	            continue;
	        }
	        if( !item.isSharedFile() ) {
	            continue;
	        }
	        if( item.getState() != FrostDownloadItem.STATE_PROGRESS ) {
	            continue;
	        }
	        item.addToRuntimeSecondsWithoutProgress(incSecs);
	    }
	}

	/**
	 * This method is usually called from a thread to notify the ticker that
	 * the thread has finished (so that it can notify its listeners of the fact). It also
	 * releases the thread so that new threads can start if needed.
	 */
	void threadFinished() {
		runningThreads--;
		releaseThread();
	}

	/**
	 * This method is called from a thread to notify the ticker that
	 * the thread has started (so that it can notify its listeners of the fact)
	 */
	void threadStarted() {
		runningThreads++;
	}

    /**
     * Maybe start a new download automatically.
     */
	private void startDownloadThread() {
        if( Core.isFreenetOnline() && panel.isDownloadingActivated() && canAllocateDownloadThread() ) {
            final FrostDownloadItem dlItem = FileTransferManager.inst().getDownloadManager().selectNextDownloadItem();
            startDownload(dlItem);
        }
    }

	public boolean startDownload(final FrostDownloadItem dlItem) {

	    if (!Core.isFreenetOnline() ) {
            return false;
        }
        if( dlItem == null || dlItem.getState() != FrostDownloadItem.STATE_WAITING ) {
            return false;
        }

        dlItem.setDownloadStartedTime(System.currentTimeMillis());

        // increase allocated threads
        allocateDownloadThread();

        dlItem.setState(FrostDownloadItem.STATE_TRYING);

        final File targetFile = new File(Core.frostSettings.getString(Settings.DIR_DOWNLOAD) + dlItem.getFileName());
        final DownloadThread newRequest = new DownloadThread(this, dlItem, targetFile);
        newRequest.start();
        return true;
	}
}
