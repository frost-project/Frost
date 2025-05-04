/*
  TestDDAHelper.java / Frost
  Copyright (C) 2011  Frost Project <jtcfrost.sourceforge.net>

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.util.FileAccess;

/**
 * Helper class to provide TestDDA functionality for DIRECT or SHARED FCP connections.
 * 
 * Operates either on the shared FcpSocket and uses a NodeMessageListener to receive node messages;
 * or operates directly on FcpSocket and needs exclusive use of it.
 * 
 * Note: currently we ignore DDAMode and always check for read+write.
 * This is done because the design of TestDDA checks if WE (the client) can read or
 * write to a directory, but the freneet node itself is not checked. But the node is 
 * checked when we test the opposite of what we want. So when we check for READ then
 * the node writes to the target directory, and this finally tests WRITE from node side.
 * 
 *      If you send a TestDDARequest with WantWriteDirectory=true, 
 *         then you will have to write ContentToWrite into the file 
 *         WriteFilename (to prove that you're allowed to write here).
 *      If you send a TestDDARequest with WantReadDirectory=true, 
 *         then you will have to read ReadFilename (to prove that 
 *         you're allowed to read here). 
 */
public class TestDDAHelper {

	private static final Logger logger = LoggerFactory.getLogger(TestDDAHelper.class);

    /**
     * Receives a node message using a NodeMessageListener on the FcpListenThreadConnection.
     */
    public static boolean isDDAPossiblePersistent(
            final FcpSocket.DDAModes mode, 
            final File dir, 
            final FcpListenThreadConnection listenThread) 
    {
        return performTestDDA(mode, dir, null, listenThread);
    }

    /**
     * Receives a node message from the FcpSocket (direct).
     */
    public static boolean isDDAPossibleDirect(
            final FcpSocket.DDAModes mode, 
            final File dir,
            final FcpSocket fcpSocket) 
    {
        return performTestDDA(mode, dir, fcpSocket, null);
    }
    
    /**
     * Actual worker method that performs the TestDDA processing.
     * 
     * @return true if DDA is permitted for the specified mode and directory
     */
    private static boolean performTestDDA(
            final FcpSocket.DDAModes mode, 
            final File dir,
            final FcpSocket fcpSocket, // maybe null  
            final FcpListenThreadConnection listenThread) // maybe null
    {
        if( mode == null || dir == null) {
            return false;
        }
        
        if (!Core.frostSettings.getBoolValue(Settings.FCP2_USE_DDA)) {
            return false;
        }
        
        if ((listenThread == null && fcpSocket == null) || (listenThread != null && fcpSocket != null)) {
            logger.error("TestDDA: Invalid call, listenThread and fcpSocket both null or both set.");
            return false;
        }
        
        final Set<String> checkedDirectories;
        if (listenThread != null) {
            checkedDirectories = listenThread.getCheckedDirectories();
        } else {
            checkedDirectories = fcpSocket.getCheckedDirectories();
        }

        if (checkedDirectories.contains(dir.getAbsolutePath())) {
            logger.warn("TestDDA: directory is already permitted: {}", dir.getAbsolutePath());
            return true;
        }

        final WaitingNodeMessageListener nodeMessageListener;
        if (listenThread != null) {
            listenThread.aquireFcpWriteLock(); // don't start other requests while we process the TestDDA sequence
            nodeMessageListener = new WaitingNodeMessageListener();
            listenThread.addNodeMessageListener(nodeMessageListener);
        } else {
            nodeMessageListener = null;
        }
        
        try {

            // *** TestDDARequest ***
            
            // start request
            {
                List<String> sendNodeMsg = new ArrayList<String>();
                sendNodeMsg.add("TestDDARequest");
                sendNodeMsg.add("Directory="+dir.getAbsolutePath());
                sendNodeMsg.add("WantReadDirectory=true");
                sendNodeMsg.add("WantWriteDirectory=true");

                if (listenThread != null) {
                    nodeMessageListener.reset();
                    listenThread.sendMessage(sendNodeMsg);
                } else {
                    NodeMessage.sendMessage(sendNodeMsg, fcpSocket.getFcpOut());
                }
            }

            NodeMessage nodeMsg = null;
            
            nodeMsg = receiveNodeMessage(fcpSocket, listenThread, nodeMessageListener);
            
            if (nodeMsg == null || !nodeMsg.isMessageName("TestDDAReply")) {
                logger.warn("TestDDA failed, TestDDAReply expected: {}", nodeMsg);
                return false;
            }
            
            // evaluate node answer
            /*
                TestDDAReply
                Directory=/tmp/
                ReadFilename=/tmp/testr.tmp
                WriteFilename=/tmp/testw.tmp
                ContentToWrite=RANDOM
                EndMessage
             */
            if (!dir.getAbsolutePath().equals(nodeMsg.getStringValue("Directory"))) {
                logger.warn("TestDDA failed, different directory returned: {}", nodeMsg);
                return false;
            }
            
            final String readFilename =   nodeMsg.getStringValue("ReadFilename");
            final String writeFilename =  nodeMsg.getStringValue("WriteFilename");
            final String contentToWrite = nodeMsg.getStringValue("ContentToWrite");
            
            if (readFilename == null || writeFilename == null || contentToWrite == null) {
                logger.warn("TestDDA failed, invalid parameters returned: {}", nodeMsg);
                return false;
            }
            
            final File readFile  = new File(readFilename);
            final File writeFile = new File(writeFilename);
            
            if (!FileAccess.writeFile(contentToWrite, writeFile, "UTF-8")) {
                logger.warn("TestDDA failed, could not write requested writeFile: {}", nodeMsg);
                return false;
            }
            
            final String readFileContent = FileAccess.readFile(readFile, "UTF-8").trim();

            // *** TestDDAResponse ***

            {
                List<String> sendNodeMsg = new ArrayList<String>();
                sendNodeMsg.add("TestDDAResponse");
                sendNodeMsg.add("Directory="+dir.getAbsolutePath());
                sendNodeMsg.add("ReadContent="+readFileContent);

                if (listenThread != null) {
                    nodeMessageListener.reset();
                    listenThread.sendMessage(sendNodeMsg);
                } else {
                    NodeMessage.sendMessage(sendNodeMsg, fcpSocket.getFcpOut());
                }
            }
            
            // *** TestDDAComplete ***

            nodeMsg = receiveNodeMessage(fcpSocket, listenThread, nodeMessageListener);
            
            // clean up written file ...
            writeFile.delete();
            
            if (nodeMsg == null || !nodeMsg.isMessageName("TestDDAComplete")) {
                logger.warn("TestDDA failed, TestDDAComplete expected: {}", nodeMsg);
                return false;
            }
            
            // evaluate node answer
            /*
                TestDDAComplete
                Directory=/tmp/
                ReadDirectoryAllowed=true
                WriteDirectoryAllowed=true
                EndMessage
             */
            if (!dir.getAbsolutePath().equals(nodeMsg.getStringValue("Directory"))) {
                logger.warn("TestDDA failed, different directory returned: {}", nodeMsg);
                return false;
            }
            
            boolean readDirectoryAllowed  = nodeMsg.getBoolValue("ReadDirectoryAllowed");
            boolean writeDirectoryAllowed = nodeMsg.getBoolValue("WriteDirectoryAllowed");
            
            if (!readDirectoryAllowed || !writeDirectoryAllowed) {
                logger.warn("TestDDA completed, DDA not permitted: {}", nodeMsg);
                return false;
            }

            // *** finished with success ***
            
            checkedDirectories.add(dir.getAbsolutePath());
            
            logger.warn("TestDDA: DDA permitted for dir='{}'", dir.getAbsolutePath());
            return true;
            
        } finally {
            if (listenThread != null) {
                listenThread.removeNodeMessageListener(nodeMessageListener);
                listenThread.releaseFcpWriteLock();
            }
        }
    }

    /**
     * Receives a node message. Either from the FcpSocket (direct) or
     * via a NodeMessageListener on the FcpListenThreadConnection.
     */
    private static NodeMessage receiveNodeMessage(
            final FcpSocket fcpSocket, 
            final FcpListenThreadConnection listenThread,
            final WaitingNodeMessageListener nodeMessageListener) 
    {
        final NodeMessage nodeMsg;
        if (listenThread != null) {
            // wait 10 seconds for node answer
            try {
                synchronized (nodeMessageListener) {
                    if (!nodeMessageListener.isWaitCompleted()) {
                        nodeMessageListener.wait(10000L);
                    }
                }
            } catch (InterruptedException e) {
                logger.error("InterruptedException", e);
            }
            nodeMsg = nodeMessageListener.getReceivedNodeMessage();
        } else {
            // receive node message, blocking read
            nodeMsg = NodeMessage.readMessage(fcpSocket.getFcpIn());
        }
        return nodeMsg;
    }
    
    /**
     * A node message listener that receives one NodeMessage.
     */
    private static class WaitingNodeMessageListener implements NodeMessageListener {
        
        private NodeMessage receivedNodeMessage = null;
        boolean waitCompleted = false;
        
        public NodeMessage getReceivedNodeMessage() {
            return receivedNodeMessage;
        }
        
        public boolean isWaitCompleted() {
            return waitCompleted;
        }
        public void reset() {
            receivedNodeMessage = null;
            waitCompleted = false;
        }
        
        private void completeWait() {
            synchronized (this){
                waitCompleted = true;
                this.notify();
            }
        }

        @Override
        public void handleNodeMessage(NodeMessage nm) {
            receivedNodeMessage = nm;
            completeWait();
        }
        @Override
        public void handleNodeMessage(String id, NodeMessage nm) {
        }
        @Override
        public void connected() {
            completeWait();
        }
        @Override
        public void disconnected() {
            completeWait();
        }
    }
}
