/*
  FcpHandler07.java / Frost
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
package frost.fcp;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.fcp.fcp07.FcpConnection;
import frost.fcp.fcp07.FcpFactory;
import frost.fcp.fcp07.FcpInsert;
import frost.fcp.fcp07.FcpRequest;
import frost.fcp.fcp07.FcpSocket;
import frost.fcp.fcp07.messagetransfer.MessageTransferHandler;
import frost.fcp.fcp07.messagetransfer.MessageTransferTask;
import frost.fileTransfer.FreenetPriority;
import frost.fileTransfer.download.FrostDownloadItem;
import frost.fileTransfer.upload.FrostUploadItem;

public class FcpHandler07 extends FcpHandler {

	private static final Logger logger = LoggerFactory.getLogger(FcpHandler07.class);

    private MessageTransferHandler msgTransferConnection = null;

    @Override
    public void initialize(final String node) throws Exception {
        FcpFactory.init(node); // init the factory with configured nodes
    }

    /**
     * Invoked when the node is online.
     */
    @Override
    public void goneOnline() {
        if( Core.frostSettings.getBoolean(Settings.FCP2_USE_ONE_CONNECTION_FOR_MESSAGES) ) {
            try {
                msgTransferConnection = new MessageTransferHandler();
                msgTransferConnection.start();
            } catch (final Throwable e) {
                logger.error("Initialization of MessageTransferConnection failed", e);
            }
        }
    }

    @Override
    public NodeAddress getFreenetNode() {
        return FcpFactory.getFreenetNode();
    }

    @Override
    public FcpResultGet getFile(
            final int type,
            String key,
            final Long size,
            final File targetFile,
            final int maxSize,
            final int maxRetries,
            final boolean createTempFile,
            final FrostDownloadItem dlItem)
    {
        // unused by 07: htl, doRedirect, fastDownload,
        key = FcpConnection.stripSlashes(key);
        final int cnt = count++;
        final long l = System.currentTimeMillis();
        final FcpResultGet result;
        if( type == FcpHandler.TYPE_MESSAGE && msgTransferConnection != null ) {
            // use the shared socket
            logger.debug("GET_START(S)({}): {}", cnt, key);
            final String id = "get-" + FcpSocket.getNextFcpId();
            final FreenetPriority prio = Core.frostSettings.getPriority(Settings.FCP2_DEFAULT_PRIO_MESSAGE_DOWNLOAD);
            final MessageTransferTask task = new MessageTransferTask(id, key, targetFile, prio, maxSize, maxRetries);

            // enqueue task
            msgTransferConnection.enqueueTask(task);
            // wait for task to finish
            task.waitForFinished();

            result = task.getFcpResultGet();

            logger.debug("GET_END(S)({}): {}, duration = {}", cnt, key, System.currentTimeMillis() - l);
        } else {
            // use a new socket
            logger.debug("GET_START(N)({}): {}", cnt, key);
            result = FcpRequest.getFile(type, key, size, targetFile, maxSize, maxRetries, createTempFile, dlItem);
            logger.debug("GET_END(N)({}): {}, duration = {}", cnt, key, System.currentTimeMillis() - l);
        }
        return result;
    }

    int count = 0;

    @Override
    public FcpResultPut putFile(
            final int type,
            String key,
            final File sourceFile,
            final boolean doMime,
            final FrostUploadItem ulItem)
    {
        key = FcpConnection.stripSlashes(key);
        final int cnt = count++;
        final long l = System.currentTimeMillis();
        final FcpResultPut result;
        if( type == FcpHandler.TYPE_MESSAGE && msgTransferConnection != null ) {
            // use the shared socket
            logger.debug("PUT_START(S)({}): {}", cnt, key);
            final String id = "get-" + FcpSocket.getNextFcpId();
            final FreenetPriority prio = Core.frostSettings.getPriority(Settings.FCP2_DEFAULT_PRIO_MESSAGE_UPLOAD);
            final MessageTransferTask task = new MessageTransferTask(id, key, sourceFile, prio);

            // enqueue task
            msgTransferConnection.enqueueTask(task);
            // wait for task to finish
            task.waitForFinished();

            result = task.getFcpResultPut();

            logger.debug("PUT_END(S)({}): {}, duration = {}", cnt, key, System.currentTimeMillis() - l);
        } else {
            logger.debug("PUT_START(N)({}): {}", cnt, key);
            result = FcpInsert.putFile(type, key, sourceFile, doMime, ulItem);
            logger.debug("PUT_END(N)({}): {}, duration = {}", cnt, key, System.currentTimeMillis() - l);
        }

        if( result == null ) {
            return FcpResultPut.ERROR_RESULT;
        } else {
            return result;
        }
    }

    @Override
    public String generateCHK(final File file) throws IOException, ConnectException {

        final FcpConnection connection = FcpFactory.getFcpConnectionInstance();
        if (connection == null) {
            return null;
        }
        final String chkkey = connection.generateCHK(file);
        connection.close();
        return chkkey;
    }

    @Override
    public BoardKeyPair generateBoardKeyPair() throws IOException, ConnectException {

        final FcpConnection connection = FcpFactory.getFcpConnectionInstance();
        if (connection == null) {
            return null;
        }

        final String[] keyPair = connection.getKeyPair();
        connection.close();
        if( keyPair == null ) {
            return null;
        }
        final String privKey = keyPair[0];
        final String pubKey = keyPair[1];
        return new BoardKeyPair(pubKey, privKey);
    }
}
