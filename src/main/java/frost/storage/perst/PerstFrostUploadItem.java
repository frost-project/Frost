/*
  PerstFrostUploadItem.java / Frost
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
import java.util.List;

import javax.swing.JOptionPane;

import org.garret.perst.Persistent;
import org.slf4j.Logger;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fileTransfer.sharing.FrostSharedFileItem;
import frost.fileTransfer.upload.FreenetCompatibilityMode;
import frost.fileTransfer.upload.FrostUploadItem;
import frost.util.gui.StartupMessage;
import frost.util.gui.translation.Language;

public class PerstFrostUploadItem extends Persistent {

	private static final long serialVersionUID = 1L;

    public String filePath;
    public String fileName;
    public String fileNamePrefix;
    public long fileSize;
    public String chkKey;
    public boolean enabled;
    public int state;
    public long uploadAddedMillis;
    public long uploadStartedMillis;
    public long uploadFinishedMillis;
    public int retries;
    public long lastUploadStopTimeMillis;
    public String gqIdentifier;
    public String sharedFilesSha;
    public boolean compress;
    public FreenetCompatibilityMode freenetCompatibilityMode;

    public boolean isLoggedToFile;
    public boolean isCompletionProgRun;

    public PerstFrostUploadItem() {}

    public PerstFrostUploadItem(final FrostUploadItem ulItem) {
        filePath = ulItem.getFile().getPath();
        fileName = ulItem.getUnprefixedFileName();
        fileNamePrefix = ulItem.getFileNamePrefix();
        fileSize = ulItem.getFileSize();
        chkKey = ulItem.getKey();
        enabled = (ulItem.isEnabled()==null?true:ulItem.isEnabled().booleanValue());
        state = ulItem.getState();
        uploadAddedMillis = ulItem.getUploadAddedMillis();
        uploadStartedMillis = ulItem.getUploadStartedMillis();
        uploadFinishedMillis = ulItem.getUploadFinishedMillis();
        retries = ulItem.getRetries();
        lastUploadStopTimeMillis = ulItem.getLastUploadStopTimeMillis();
        gqIdentifier = ulItem.getGqIdentifier();
        isLoggedToFile = ulItem.isLoggedToFile();
        isCompletionProgRun = ulItem.isCompletionProgRun();
        compress = ulItem.getCompress();
        freenetCompatibilityMode = ulItem.getFreenetCompatibilityMode();
        sharedFilesSha = (ulItem.getSharedFileItem()==null?null:ulItem.getSharedFileItem().getSha());
    }

    public FrostUploadItem toFrostUploadItem(final List<FrostSharedFileItem> sharedFiles, final Logger logger, final Language language) {

        final File file = new File(filePath);
        if( Core.frostSettings.getBoolValue(Settings.UPLOAD_REMOVE_NOT_EXISTING_FILES) && !file.isFile() ) {
            final String title = language.getString("StartupMessage.uploadFile.uploadFileNotFound.title");
            final String text = language.formatMessage("StartupMessage.uploadFile.uploadFileNotFound.text", filePath);
            final StartupMessage sm = new StartupMessage(
                    StartupMessage.MessageType.UploadFileNotFound,
                    title,
                    text,
                    JOptionPane.ERROR_MESSAGE,
                    true);
            MainFrame.enqueueStartupMessage(sm);
            logger.warn("Upload items file does not exist, removed from upload files: {}", filePath);
            return null;
        }
        if( Core.frostSettings.getBoolValue(Settings.UPLOAD_REMOVE_NOT_EXISTING_FILES) && file.length() != fileSize ) {
            final String title = language.getString("StartupMessage.uploadFile.uploadFileSizeChanged.title");
            final String text = language.formatMessage("StartupMessage.uploadFile.uploadFileSizeChanged.text", filePath);
            final StartupMessage sm = new StartupMessage(
                    StartupMessage.MessageType.UploadFileSizeChanged,
                    title,
                    text,
                    JOptionPane.ERROR_MESSAGE,
                    true);
            MainFrame.enqueueStartupMessage(sm);
            logger.warn("Upload items file size changed, removed from upload files: {}", filePath);
            return null;
        }

        FrostSharedFileItem sharedFileItem = null;
        if( sharedFilesSha != null && sharedFilesSha.length() > 0 ) {
            for( final FrostSharedFileItem s : sharedFiles ) {
                if( s.getSha().equals(sharedFilesSha) ) {
                    sharedFileItem = s;
                    break;
                }
            }
            if( sharedFileItem == null ) {
                logger.warn("Upload items shared file object does not exist, removed from upload files: {}", filePath);
                return null;
            }
            if( !sharedFileItem.isValid() ) {
                logger.warn("Upload items shared file is invalid, removed from upload files: {}", filePath);
                return null;
            }
        }

        final FrostUploadItem ulItem = new FrostUploadItem(
                file,
                fileName,
                fileNamePrefix,
                fileSize,
                chkKey,
                enabled,
                state,
                uploadAddedMillis,
                uploadStartedMillis,
                uploadFinishedMillis,
                retries,
                lastUploadStopTimeMillis,
                gqIdentifier,
                isLoggedToFile,
                isCompletionProgRun,
                compress,
                freenetCompatibilityMode);

        ulItem.setSharedFileItem(sharedFileItem);

        return ulItem;
    }
}
