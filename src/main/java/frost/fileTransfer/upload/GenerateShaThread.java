/*
  GenerateShaThread.java / Frost
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
package frost.fileTransfer.upload;

import java.io.File;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.NewUploadFilesManager;
import frost.fileTransfer.sharing.FrostSharedFileItem;
import frost.storage.perst.NewUploadFile;
import frost.util.Mixed;

/**
 * Generates the sha checksum of a file.
 */
public class GenerateShaThread extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(GenerateShaThread.class);

    private NewUploadFilesManager newUploadFilesManager;
    
    private static final int wait1minute = 1 * 60 * 1000;
    FileQueue fileQueue;

    public GenerateShaThread(NewUploadFilesManager newUploadFilesManager) {
        this.newUploadFilesManager = newUploadFilesManager;
        fileQueue = new FileQueue();
    }

    public void addToFileQueue(final NewUploadFile f) {
        // awakes thread
        fileQueue.appendFileToQueue(f);
    }

    public int getQueueSize() {
        return fileQueue.getQueueSize();
    }

    @Override
    public void run() {

        final int maxAllowedExceptions = 5;
        int occuredExceptions = 0;

        while(true) {
            try {
                // if now work is in queue this call waits for a new queueitem
                final NewUploadFile newUploadFile = fileQueue.getFileFromQueue();

                if( newUploadFile == null ) {
                    // paranoia
                    Mixed.wait(wait1minute);
                    continue;
                }

                final File newFile = new File(newUploadFile.getFilePath());

                final String sha = Core.getCrypto().computeChecksumSHA256(newFile);

                if( sha != null ) {

                    // create new item
                    final FrostSharedFileItem sfi = new FrostSharedFileItem(
                            newFile,
                            newUploadFile.getFrom(),
                            sha);

                    // add to shared files
                    FileTransferManager.inst().getSharedFilesManager().getModel().addNewSharedFile(sfi, newUploadFile.isReplacePathIfFileExists());

                    // delete from newuploadfiles database
                    newUploadFilesManager.deleteNewUploadFile(newUploadFile);
                }

            } catch(final Throwable t) {
                logger.error("Exception catched", t);
                occuredExceptions++;
            }

            if( occuredExceptions > maxAllowedExceptions ) {
                logger.error("Stopping GenerateShaThread because of too much exceptions");
                break;
            }
        }
    }

    private class FileQueue {

        private final LinkedList<NewUploadFile> queue = new LinkedList<NewUploadFile>();

        /**
         * @return
         */
        public synchronized NewUploadFile getFileFromQueue() {
            try {
                // let dequeueing threads wait for work
                while( queue.isEmpty() ) {
                    wait();
                }
            } catch (final InterruptedException e) {
                return null; // waiting abandoned
            }

            if( queue.isEmpty() == false ) {
                final NewUploadFile key = queue.removeFirst();
                return key;
            }
            return null;
        }

        public synchronized void appendFileToQueue(final NewUploadFile f) {
            queue.addLast(f);
            notifyAll(); // notify all waiters (if any) of new record
        }

        public synchronized int getQueueSize() {
            return queue.size();
        }
    }
}
