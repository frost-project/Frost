/*
 NewUploadFilesManager.java / Frost
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
package frost.fileTransfer;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.fileTransfer.upload.GenerateShaThread;
import frost.storage.ExitSavable;
import frost.storage.StorageException;
import frost.storage.perst.FrostFilesStorage;
import frost.storage.perst.NewUploadFile;

public class NewUploadFilesManager implements ExitSavable {

	private static final Logger logger = LoggerFactory.getLogger(NewUploadFilesManager.class);

    private LinkedList<NewUploadFile> newUploadFiles;
    private GenerateShaThread generateShaThread;

    public NewUploadFilesManager() {
        super();
    }
    
    public void initialize() throws StorageException {
        try {
            newUploadFiles = FrostFilesStorage.inst().loadNewUploadFiles();
        } catch (final Throwable e) {
            logger.error("Error loading new upload files", e);
            throw new StorageException("Error loading new upload files");
        }
        generateShaThread = new GenerateShaThread(this);
    }

    /**
     * Start the generate SHA thread.
     */
    public void start() {
        generateShaThread.start();
    }

    public void exitSave() throws StorageException {
        synchronized(newUploadFiles) {
            try {
                FrostFilesStorage.inst().saveNewUploadFiles(newUploadFiles);
            } catch (final Throwable e) {
                logger.error("Error saving new upload files", e);
                throw new StorageException("Error saving new upload files");
            }
        }
    }

    public void addNewUploadFiles(final List<NewUploadFile> newFiles) {
        synchronized(newUploadFiles) {
            for( final NewUploadFile nuf : newFiles ) {
                newUploadFiles.add(nuf);

                // feed thread
                generateShaThread.addToFileQueue(nuf);
            }
        }
    }

    public void deleteNewUploadFile(final NewUploadFile nuf) {
        synchronized(newUploadFiles) {
            newUploadFiles.remove(nuf);
        }
    }
}
