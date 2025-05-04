/*
  RunningBoardUpdateThreads.java / Frost
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
package frost.messaging.frost.threads;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import frost.Settings;
import frost.messaging.frost.UnsentMessagesManager;
import frost.messaging.frost.boards.Board;
import frost.messaging.frost.boards.BoardUpdateInformation;
import frost.messaging.frost.boards.BoardUpdateThread;
import frost.messaging.frost.boards.BoardUpdateThreadListener;

/**
 * This class maintains the message download and upload threads.
 * Listeners for thread started and thread finished are provided.
 */
public class RunningBoardUpdateThreads implements BoardUpdateThreadListener {

    // listeners are notified of each finished thread
    Hashtable<String,Vector<BoardUpdateThreadListener>> threadListenersForBoard = null; // contains all listeners registered for 1 board
    Vector<BoardUpdateThreadListener> threadListenersForAllBoards = null; // contains all listeners for all boards

    // contains key=board, data=vector of BoardUpdateThread's, (max. 1 of a kind (MSG_DOWNLOAD_TODAY,...)
    Hashtable<String,Vector<BoardUpdateThread>> runningDownloadThreads = null;

    public RunningBoardUpdateThreads() {
        threadListenersForBoard = new Hashtable<String,Vector<BoardUpdateThreadListener>>();
        threadListenersForAllBoards = new Vector<BoardUpdateThreadListener>();

        runningDownloadThreads = new Hashtable<String,Vector<BoardUpdateThread>>();
    }

    /**
     * if you specify a listener and the method returns true (thread is started), the listener
     * will be notified if THIS thread is finished
     * before starting a thread you should check if it is'nt updating already.
     */
    public boolean startMessageDownloadToday(
        final Board board,
        final Settings config,
        final BoardUpdateThreadListener listener) {

        final MessageThread tofd = new MessageThread(
                true,
                board,
                board.getMaxMessageDownload());

        // register listener and this class as listener
        tofd.addBoardUpdateThreadListener(this);
        if (listener != null) {
            tofd.addBoardUpdateThreadListener(listener);
        }
        // store thread in threads list
        getVectorFromHashtable(runningDownloadThreads, board).add(tofd);

        // start thread
        tofd.start();
        return true;
    }
    /**
     * if you specify a listener and the method returns true (thread is started), the listener
     * will be notified if THIS thread is finished
     * before starting a thread you should check if it is'nt updating already.
     */
    public boolean startMessageDownloadBack(
        final Board board,
        final Settings config,
        final BoardUpdateThreadListener listener,
        final boolean downloadCompleteBackload)
    {
        int daysBackward;
        if( downloadCompleteBackload ) {
            daysBackward = board.getMaxMessageDownload();
        } else {
            daysBackward = 1;
        }

        final MessageThread backload = new MessageThread(
                false,
                board,
                daysBackward);

        // register listener and this class as listener
        backload.addBoardUpdateThreadListener(this);
        if (listener != null) {
            backload.addBoardUpdateThreadListener(listener);
        }
        // store thread in threads list
        getVectorFromHashtable(runningDownloadThreads, board).add(backload);

        // start thread
        backload.start();

        return true;
    }

    /**
     * Gets an Vector from a Hashtable with given key. If key is not contained
     * in Hashtable, an empty Vector will be created and put in the Hashtable.
     */
    private <T> Vector<T> getVectorFromHashtable(final Hashtable<String,Vector<T>> t, final Board key) {
        Vector<T> retval = null;
        synchronized( t ) {
            retval = t.get(key.getName());
            if( retval == null ) {
                retval = new Vector<T>();
                t.put(key.getName(), retval);
            }
        }
        return retval;
    }

    /**
     * Returns the list of current download threads for a given board. Returns an empty list of no thread is running.
     */
    public Vector<BoardUpdateThread> getDownloadThreadsForBoard(final Board board) {
        return getVectorFromHashtable( runningDownloadThreads, board );
    }

    /**
     * Adds a listener that gets notified if any thread for a given board did update its state.
     * For supported states see BoardUpdateThreadListener methods.
     */
    public void addBoardUpdateThreadListener(final Board board, final BoardUpdateThreadListener listener) {
        getVectorFromHashtable(threadListenersForBoard, board).remove(listener); // no doubles allowed
        getVectorFromHashtable(threadListenersForBoard, board).add(listener);
    }

    /**
     * Adds a listener that gets notified if any thread for any board did update its state.
     * For supported states see BoardUpdateThreadListener methods.
     */
    public void addBoardUpdateThreadListener(final BoardUpdateThreadListener listener) {
        threadListenersForAllBoards.remove( listener ); // no doubles allowed
        threadListenersForAllBoards.add( listener );
    }
    /**
     * Removes a listener that gets notified if any thread for a given board did update its state.
     * For supported states see BoardUpdateThreadListener methods.
     * Method will do nothing if listener is null or not contained in the list of listeners.
     */
    public void removeBoardUpdateThreadListener(final Board board, final BoardUpdateThreadListener listener) {
        getVectorFromHashtable(threadListenersForBoard, board).remove(listener);
    }

    /**
     * Removes a listener that gets notified if any thread for any board did update its state.
     * For supported states see BoardUpdateThreadListener methods.
     * Method will do nothing if listener is null or not contained in the list of listeners.
     */
    public void removeBoardUpdateThreadListener(final BoardUpdateThreadListener listener) {
        threadListenersForAllBoards.remove( listener );
    }

    /**
     * Implementing the listener for thread finished.
     * Notifies all interested listeners for change of the thread state.
     * @see frost.messaging.frost.boards.BoardUpdateThreadListener#boardUpdateThreadFinished(frost.messaging.frost.boards.BoardUpdateThread)
     */
    public void boardUpdateThreadFinished(final BoardUpdateThread thread) {
        // remove from thread list
        Vector<BoardUpdateThread> threads = getVectorFromHashtable(runningDownloadThreads, thread.getTargetBoard());

        if( threads != null ) {
            threads.remove(thread);
        }

        synchronized( threadListenersForAllBoards ) {
            // notify listeners
            Iterator<BoardUpdateThreadListener> i = threadListenersForAllBoards.iterator();
            while( i.hasNext() ) {
                i.next().boardUpdateThreadFinished(thread);
            }
            i = getVectorFromHashtable(threadListenersForBoard, thread.getTargetBoard()).iterator();
            while( i.hasNext() ) {
                i.next().boardUpdateThreadFinished(thread);
            }
        }
    }

    /**
     * Implementing the listener for thread started. Notifies all interested listeners for change of the thread state.
     * @see frost.messaging.frost.boards.BoardUpdateThreadListener#boardUpdateThreadStarted(frost.messaging.frost.boards.BoardUpdateThread)
     */
    public void boardUpdateThreadStarted(final BoardUpdateThread thread) {
        synchronized( threadListenersForAllBoards ) {
            Iterator<BoardUpdateThreadListener> i = threadListenersForAllBoards.iterator();
            while( i.hasNext() ) {
                (i.next()).boardUpdateThreadStarted(thread);
            }
            i = getVectorFromHashtable(threadListenersForBoard, thread.getTargetBoard()).iterator();
            while( i.hasNext() ) {
                i.next().boardUpdateThreadStarted(thread);
            }
        }
    }

    public void boardUpdateInformationChanged(final BoardUpdateThread thread, final BoardUpdateInformation bui) {
        synchronized( threadListenersForAllBoards ) {
            Iterator<BoardUpdateThreadListener> i = threadListenersForAllBoards.iterator();
            while( i.hasNext() ) {
                i.next().boardUpdateInformationChanged(thread, bui);
            }
            i = getVectorFromHashtable(threadListenersForBoard, thread.getTargetBoard()).iterator();
            while( i.hasNext() ) {
                i.next().boardUpdateInformationChanged(thread, bui);
            }
        }
    }

    /**
     * Returns the count of ALL running download threads (of all boards).
     */
    public int getRunningDownloadThreadCount() { // msg_today, msg_back, files_update, update_id
        int downloadingThreads = 0;

        synchronized( runningDownloadThreads ) {
            final Iterator<Vector<BoardUpdateThread>> i = runningDownloadThreads.values().iterator();
            while( i.hasNext() ) {
                final Vector<BoardUpdateThread> v = i.next();
                if( v.size() > 0 ) {
                    downloadingThreads += v.size();
                }
            }
        }
        return downloadingThreads;
    }

    /**
     * Returns the count of boards that currently have running download threads.
     */
    public int getDownloadingBoardCount() {
        int downloadingBoards = 0;

        synchronized( runningDownloadThreads ) {
            final Iterator<Vector<BoardUpdateThread>> i = runningDownloadThreads.values().iterator();
            while( i.hasNext() ) {
                final Vector<BoardUpdateThread> v = i.next();
                if( v.size() > 0 ) {
                    downloadingBoards++;
                }
            }
        }
        return downloadingBoards;
    }

    /**
     * Returns all information together, faster than calling all single methods.
     *
     * @return a new information class containing status informations
     * @see RunningMessageThreadsInformation
     */
    public RunningMessageThreadsInformation getRunningMessageThreadsInformation() {

        final RunningMessageThreadsInformation info = new RunningMessageThreadsInformation();

        final int uploadingMessages = UnsentMessagesManager.getRunningMessageUploads();
        info.setUploadingMessageCount(uploadingMessages);
        // the manager count uploading messages as unsent, we show Uploading/Waiting in statusbar,
        // hence we decrease the unsent by uploading messages
        info.setUnsentMessageCount(UnsentMessagesManager.getUnsentMessageCount() - uploadingMessages);

        info.addToAttachmentsToUploadRemainingCount(FileAttachmentUploadThread.getInstance().getQueueSize());

        synchronized( runningDownloadThreads ) {
            final Iterator<Vector<BoardUpdateThread>> i = runningDownloadThreads.values().iterator();
            while( i.hasNext() ) {
                final Vector<BoardUpdateThread> v = i.next();
                final int vsize = v.size();
                if( vsize > 0 ) {
                    info.addToDownloadingBoardCount(1);
                    info.addToRunningDownloadThreadCount(vsize);
                }
            }
        }
        return info;
    }

    /**
     * Returns true if the given board have running download threads.
     */
    public boolean isUpdating(final Board board) {
        if( getVectorFromHashtable(runningDownloadThreads, board).size() > 0 ) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isThreadOfTypeRunning(final Board board, final int type) {
        final List<BoardUpdateThread> threads = getDownloadThreadsForBoard(board);
        for( int x = 0; x < threads.size(); x++ ) {
            if( threads.get(x).getThreadType() == type ) {
                return true;
            }
        }
        return false;
    }
}
