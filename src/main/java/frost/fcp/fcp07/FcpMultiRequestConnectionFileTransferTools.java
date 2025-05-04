/*
  FcpMultiRequestConnectionTools.java / Frost
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
package frost.fcp.fcp07;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.ext.DefaultMIMETypes;
import frost.fileTransfer.FreenetPriority;
import frost.fileTransfer.upload.FreenetCompatibilityMode;

public class FcpMultiRequestConnectionFileTransferTools {

	private static final Logger logger = LoggerFactory.getLogger(FcpMultiRequestConnectionFileTransferTools.class);

    private final FcpListenThreadConnection fcpPersistentConnection;

    public FcpMultiRequestConnectionFileTransferTools(final FcpListenThreadConnection fcpPersistentConnection) {
        this.fcpPersistentConnection = fcpPersistentConnection;
    }

    public FcpListenThreadConnection getFcpPersistentConnection() {
        return fcpPersistentConnection;
    }

    /**
     * No answer from node is expected.
     */
    public void changeRequestPriority(final String id, final FreenetPriority newPrio) {
        final List<String> msg = new LinkedList<String>();
        msg.add("ModifyPersistentRequest");
        msg.add("Global=true");
        msg.add("Identifier="+id);
        msg.add("PriorityClass=" + newPrio.getNumber());
        fcpPersistentConnection.sendMessage(msg);
    }

    /**
     * No answer from node is expected.
     */
    public void removeRequest(final String id) {
        final List<String> msg = new LinkedList<String>();
        msg.add("RemovePersistentRequest");
        msg.add("Global=true");
        msg.add("Identifier="+id);
        fcpPersistentConnection.sendMessage(msg);
    }

    /**
     * No answer from node is expected.
     */
    public void watchGlobal(final boolean enabled) {
        final List<String> msg = new LinkedList<String>();
        msg.add("WatchGlobal");
        msg.add("Enabled="+enabled);
        msg.add("VerbosityMask=1");
        fcpPersistentConnection.sendMessage(msg);
    }

    /**
     * Starts a persistent put.
     * First tests DDA and does NOT start a request if DDA is not possible!
     *
     * @return true when DDA is possible and a request was started.
     *         false when DDA is not possible and nothing was started
     */
    public boolean startPersistentPutUsingDda(
            final String id,
            final File sourceFile,
            final String fileName,
            final boolean doMime,
            final boolean setTargetFileName,
            final boolean compress,
            final FreenetCompatibilityMode freenetCompatibilityMode,
            final FreenetPriority prio
    ) {
        final File uploadDir = sourceFile.getParentFile();
        final boolean isDda = TestDDAHelper.isDDAPossiblePersistent(FcpSocket.DDAModes.WANT_UPLOAD, uploadDir, fcpPersistentConnection);
        if (!isDda) {
            return false;
        }

        final List<String> msg = getDefaultPutMessage(id, sourceFile, fileName, doMime, setTargetFileName, compress, freenetCompatibilityMode, prio);
        msg.add("UploadFrom=disk");
        msg.add("Filename=" + sourceFile.getAbsolutePath());

        fcpPersistentConnection.sendMessage(msg);
        return true;
    }

    /**
     * Starts a new persistent get.
     * Uses TestDDA to figure out if DDA can be used or not.
     * If DDA=false then the request is enqueued as DIRECT and must be retrieved manually
     * after the get completed successfully. Use startDirectPersistentGet to fetch the data.
     *
     * @return true when the download was started using DDA, false if it is using DIRECT
     */
    public boolean startPersistentGet(final String key, final String id, final File targetFile, final FreenetPriority priority) {
        // start the persistent get. if DDA=false, then we have to fetch the file after the successful get from node
        final File downloadDir = targetFile.getParentFile();
        final boolean isDda = TestDDAHelper.isDDAPossiblePersistent(FcpSocket.DDAModes.WANT_DOWNLOAD, downloadDir, fcpPersistentConnection);

        final List<String> msg = new LinkedList<String>();
        msg.add("ClientGet");
        msg.add("IgnoreDS=false");
        msg.add("DSOnly=false");
        msg.add("URI=" + key);
        msg.add("Identifier="+id );
        msg.add("MaxRetries=-1");
        msg.add("Verbosity=-1");
        msg.add("Persistence=forever");
        msg.add("Global=true");
        msg.add("PriorityClass=" + priority.getNumber());

        if (isDda) {
            msg.add("ReturnType=disk");
            msg.add("Filename=" + targetFile.getAbsolutePath());
            final File ddaTempFile = new File( targetFile.getAbsolutePath() + "-f");
            if( ddaTempFile.isFile() ) {
                // delete before download, else download fails, node will not overwrite anything!
                ddaTempFile.delete();
            }
            msg.add("TempFilename=" + ddaTempFile.getAbsolutePath());
         } else {
             msg.add("ReturnType=direct");
        }

        fcpPersistentConnection.sendMessage(msg);

        return isDda;
    }

    public void listPersistentRequests() {
        final List<String> msg = new LinkedList<String>();
        msg.add("ListPersistentRequests");
        fcpPersistentConnection.sendMessage(msg);
    }

    /**
     * Returns the common part of a put request, used for a put with type DIRECT and DISK together.
     */
    private List<String> getDefaultPutMessage(
            final String id,
            final File sourceFile,
            final String fileName,
            final boolean doMime,
            final boolean setTargetFileName,
            final boolean compress,
            final FreenetCompatibilityMode freenetCompatibilityMode,
            final FreenetPriority prio
    ) {
        final LinkedList<String> lst = new LinkedList<String>();
        lst.add("ClientPut");
        lst.add("URI=CHK@");
        lst.add("Identifier=" + id);
        lst.add("Verbosity=-1");
        lst.add("MaxRetries=-1");
        if( ! compress ) {
         	lst.add("DontCompress=true");
        }
        lst.add("CompatibilityMode=" + freenetCompatibilityMode);
        if( setTargetFileName ) {
            lst.add("TargetFilename="+fileName); // prevents problems downloading this file with other apps
        } else {
            lst.add("TargetFilename="); // default for shared files: we always want the same key for the same content!
        }
        if (doMime) {
            lst.add("Metadata.ContentType=" + DefaultMIMETypes.guessMIMEType(sourceFile.getAbsolutePath()));
        } else {
            lst.add("Metadata.ContentType=application/octet-stream"); // force this to prevent the node from filename guessing due dda!
        }
        lst.add("PriorityClass=" + prio.getNumber());
        lst.add("Persistence=forever");
        lst.add("Global=true");
        return lst;
    }

    /**
     * Adds a file to the global queue DIRECT, means the file is transferred into the node completely.
     * Returns the first NodeMessage sent by the node after the transfer (PersistentPut or any error message).
     * After this method was called this connection is unuseable.
     */
    public NodeMessage startDirectPersistentPut(
            final String id,
            final File sourceFile,
            final String fileName,
            final boolean doMime,
            final boolean setTargetFileName,
            final boolean compress,
            final FreenetCompatibilityMode freenetCompatibilityMode,
            final FreenetPriority prio)
    throws IOException
    {
        final FcpSocket newSocket = FcpSocket.create(fcpPersistentConnection.getNodeAddress());
        if( newSocket == null ) {
            return null;
        }

        final List<String> msg = getDefaultPutMessage(id, sourceFile, fileName, doMime, setTargetFileName, compress, freenetCompatibilityMode, prio);
        msg.add("UploadFrom=direct");
        msg.add("DataLength=" + Long.toString(sourceFile.length()));
        msg.add("Data");

        for( final String line : msg ) {
            newSocket.getFcpOut().println(line);
        }
        newSocket.getFcpOut().flush();

		// write complete file to socket
		try (BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(sourceFile));) {
			while (true) {
				final int d = fileInput.read();
				if (d < 0) {
					break; // EOF
				}
				newSocket.getFcpRawOut().write(d);
			}
		}
		newSocket.getFcpRawOut().flush();

        // XXX: For some reason Freenet never responds with an OK-message?

        // wait for a message from node
        // good: PersistentPut
        // -> IdentifierCollision {Global=true, Identifier=myid1} EndMessage
        final NodeMessage nodeMsg = NodeMessage.readMessage(newSocket.getFcpIn());

        logger.debug("*PPUT** NodeMessage:");
        logger.debug("{}", nodeMsg);

        newSocket.close();

        //return nodeMsg;
        return new NodeMessage("Dummy");
    }

    /**
     * Retrieves a completed get from the node buffer DIRECT.
     * After this method was called this connection is unuseable.
     */
    public NodeMessage startDirectPersistentGet(final String id, final File targetFile) throws IOException {

        final FcpSocket newSocket = FcpSocket.create(fcpPersistentConnection.getNodeAddress());
        if( newSocket == null ) {
            return null;
        }

        newSocket.getFcpOut().println("GetRequestStatus");
        newSocket.getFcpOut().println("Global=true");
        newSocket.getFcpOut().println("Identifier=" + id);
        newSocket.getFcpOut().println("OnlyData=true");
        newSocket.getFcpOut().println("EndMessage");
        newSocket.getFcpOut().flush();

        // we expect an immediate answer
        final NodeMessage nodeMsg = NodeMessage.readMessage(newSocket.getFcpIn());
        if( nodeMsg == null ) {
            return null;
        }

        logger.debug("*PGET** NodeMessage:");
        logger.debug("{}", nodeMsg);

        final String endMarker = nodeMsg.getMessageEnd();
        if( endMarker == null ) {
            // should never happen
            logger.error("*PGET** ENDMARKER is NULL! {}", nodeMsg);
            return null;
        }

        if( nodeMsg.isMessageName("AllData") && endMarker.equals("Data") ) {
            // data follow, first get datalength
            final long dataLength = nodeMsg.getLongValue("DataLength");

			long bytesWritten = 0;
			try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(targetFile));) {
				final byte[] b = new byte[4096];
				long bytesLeft = dataLength;

				while (bytesLeft > 0) {
					final int count = newSocket.getFcpIn().read(b, 0,
							((bytesLeft > b.length) ? b.length : (int) bytesLeft));
					if (count < 0) {
						break;
					} else {
						bytesLeft -= count;
					}
					fileOut.write(b, 0, count);
					bytesWritten += count;
				}
			}
            logger.debug("*GET** Wrote {} of {} bytes to file.", bytesWritten, dataLength);

            newSocket.close();

            if( bytesWritten == dataLength ) {
                return nodeMsg;
            } else {
                return null;
            }
        } else {
            logger.error("Invalid node answer, expected AllData: {}", nodeMsg);
            newSocket.close();
            return null;
        }
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Methods to get/put messages ////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void startDirectGet(final String id, final String key, final FreenetPriority prio, final int maxSize, int maxRetries) {
        final List<String> msg = new LinkedList<String>();
        msg.add("ClientGet");
        msg.add("IgnoreDS=false");
        msg.add("DSOnly=false");
        msg.add("URI=" + key);
        msg.add("Identifier=" + id );
        if( maxRetries <= 0 ) {
            maxRetries = 1;
        }
        msg.add("MaxRetries=" + Integer.toString(maxRetries));
        msg.add("Verbosity=0");
        msg.add("ReturnType=direct");
        msg.add("Persistence=connection");
        msg.add("PriorityClass="+ prio.getNumber());
        if( maxSize > 0 ) {
            msg.add("MaxSize="+maxSize);
        }
        fcpPersistentConnection.sendMessage(msg);
    }

    public void startDirectPut(final String id, final String key, final FreenetPriority prio, final File sourceFile) {
        final List<String> msg = new LinkedList<String>();
        msg.add("ClientPut");
        msg.add("URI=" + key);
        msg.add("Identifier=" + id );
        msg.add("Verbosity=0");
        msg.add("MaxRetries=1");
        //msg.add("DontCompress=true");
        boolean useEarlyEncode = Core.frostSettings.getBoolValue(Settings.FCP2_USE_EARLY_ENCODE);
        if (useEarlyEncode) {
            msg.add("EarlyEncode=true");
        }
        msg.add("PriorityClass=" + prio.getNumber());
        msg.add("Persistence=connection");
        msg.add("UploadFrom=direct");

        fcpPersistentConnection.sendMessageAndData(msg, sourceFile);
    }
}
