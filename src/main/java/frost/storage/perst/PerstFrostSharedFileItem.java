/*
  PerstFrostSharedFileItem.java / Frost
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

import javax.swing.JOptionPane;

import org.garret.perst.Persistent;
import org.slf4j.Logger;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fileTransfer.sharing.FrostSharedFileItem;
import frost.util.gui.StartupMessage;
import frost.util.gui.translation.Language;

public class PerstFrostSharedFileItem extends Persistent {

	private static final long serialVersionUID = 1L;

    public String sha;

    public String filePath;
    public long fileSize;
    public String key;

    public String owner;
    public String comment;
    public int rating;
    public String keywords;

    public long lastUploaded;
    public int uploadCount;

    public long refLastSent;

    public long requestLastReceived;
    public int requestsReceived;

    public long lastModified;

    public PerstFrostSharedFileItem() {}

    public PerstFrostSharedFileItem(final FrostSharedFileItem sfItem) {
        filePath = sfItem.getFile().getPath();
        fileSize = sfItem.getFileSize();
        key = sfItem.getKey();
        sha = sfItem.getSha();
        owner = sfItem.getOwner();
        comment = sfItem.getComment();
        rating = sfItem.getRating();
        keywords = sfItem.getKeywords();
        lastUploaded = sfItem.getLastUploaded();
        uploadCount = sfItem.getUploadCount();
        refLastSent = sfItem.getRefLastSent();
        requestLastReceived = sfItem.getRequestLastReceived();
        requestsReceived = sfItem.getRequestsReceived();
        lastModified = sfItem.getLastModified();
    }

    public FrostSharedFileItem toFrostSharedFileItem(final Logger logger, final Language language) {
        boolean fileIsOk = true;
        final File file = new File(filePath);

        // report modified/missing shared files only if filesharing is enabled
        if( !Core.frostSettings.getBoolValue(Settings.FILESHARING_DISABLE) ) {
            if( !file.isFile() ) {
                final String title = language.getString("StartupMessage.sharedFile.sharedFileNotFound.title");
                final String text = language.formatMessage("StartupMessage.sharedFile.sharedFileNotFound.text", filePath);
                final StartupMessage sm = new StartupMessage(
                        StartupMessage.MessageType.SharedFileNotFound,
                        title,
                        text,
                        JOptionPane.WARNING_MESSAGE,
                        true);
                MainFrame.enqueueStartupMessage(sm);
                logger.error("Shared file does not exist: {}", filePath);
                fileIsOk = false;
            } else if( file.length() != fileSize ) {
                final String title = language.getString("StartupMessage.sharedFile.sharedFileSizeChanged.title");
                final String text = language.formatMessage("StartupMessage.sharedFile.sharedFileSizeChanged.text", filePath);
                final StartupMessage sm = new StartupMessage(
                        StartupMessage.MessageType.SharedFileSizeChanged,
                        title,
                        text,
                        JOptionPane.WARNING_MESSAGE,
                        true);
                MainFrame.enqueueStartupMessage(sm);
                logger.error("Size of shared file changed: {}", filePath);
                fileIsOk = false;
            } else if( file.lastModified() != lastModified ) {
                final String title = language.getString("StartupMessage.sharedFile.sharedFileLastModifiedChanged.title");
                final String text = language.formatMessage("StartupMessage.sharedFile.sharedFileLastModifiedChanged.text", filePath);
                final StartupMessage sm = new StartupMessage(
                        StartupMessage.MessageType.SharedFileLastModifiedChanged,
                        title,
                        text,
                        JOptionPane.WARNING_MESSAGE,
                        true);
                MainFrame.enqueueStartupMessage(sm);
                logger.error("Last modified date of shared file changed: {}", filePath);
                fileIsOk = false;
            }
        }

//        // check if we share a file with an old CHK key, if yes, remove the key and reset lastUploaded date
//        if( FcpHandler.isFreenet07() && FreenetKeys.isOld07ChkKey(key) ) {
//            // reset key, the change is saved to perst during shutdown
//            key = null;
//            lastUploaded = 0;
//            uploadCount = 0;
//        }

        final FrostSharedFileItem sfItem = new FrostSharedFileItem(
                file,
                fileSize,
                key,
                sha,
                owner,
                comment,
                rating,
                keywords,
                lastUploaded,
                uploadCount,
                refLastSent,
                requestLastReceived,
                requestsReceived,
                lastModified,
                fileIsOk);

        return sfItem;
    }
}
