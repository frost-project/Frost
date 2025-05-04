/*
  FrostDownloadItem.java / Frost

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

import frost.Core;
import frost.Settings;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.FreenetPriority;
import frost.fileTransfer.FrostFileListFileObject;
import frost.messaging.frost.FrostMessageObject;
import frost.storage.perst.filelist.FileListStorage;
import frost.util.CopyToClipboardItem;
import frost.util.FileAccess;
import frost.util.Mixed;
import frost.util.model.ModelItem;

public class FrostDownloadItem extends ModelItem<FrostDownloadItem> implements CopyToClipboardItem {

    // the constants representing download states
    public transient final static int STATE_WAITING    = 1; // wait for start
    public transient final static int STATE_TRYING     = 2; // download running
    public transient final static int STATE_DONE       = 3;
    public transient final static int STATE_FAILED     = 4;
    public transient final static int STATE_PROGRESS   = 5; // download runs
    public transient final static int STATE_DECODING   = 6; // decoding runs

	private String fileName = null;
	private String prefix = null;
    private String downloadDir = null;
	private long fileSize = -1;
	private String key = null;
	private String associatedMessageId = null;
	private String associatedBoardName = null;

    private Boolean enabled = Boolean.TRUE;
    private int state = STATE_WAITING;
    private long downloadAddedTime = 0;
    private long downloadStartedTime = 0;
    private long downloadFinishedTime = 0;
	private int retries = 0;
    private long lastDownloadStopTime = 0;
    private String gqIdentifier = null;

    private boolean isLoggedToFile = false;
    private boolean isTracked = false;
    private boolean isCompletionProgRun = false;

    private int runtimeSecondsWithoutProgress = 0;
    private int oldDoneBlocks = 0;

    // if this downloadfile is a shared file then this object is set
    private transient FrostFileListFileObject fileListFileObject = null;

    private FreenetPriority priority = FreenetPriority.getPriority(Core.frostSettings.getIntValue(Settings.FCP2_DEFAULT_PRIO_FILE_DOWNLOAD));

    // non persistent fields
	private transient int doneBlocks = 0;
	private transient int requiredBlocks = 0;
	private transient int totalBlocks = 0;
    private transient Boolean isFinalized = null;
    private transient String errorCodeDescription = null;

    private transient boolean isDirect = false;
    private transient boolean isExternal = false;

    private transient boolean internalRemoveExpected = false;
    private transient boolean stateShouldBeProgress = false;

    /**
     * Add a file from download text box.
     */
	public FrostDownloadItem(String fileName, final String key) {

        fileName = FileTransferManager.inst().getDownloadManager().ensureUniqueFilename(fileName);

        this.fileName = Mixed.makeFilename(fileName);
		this.key = key;

        gqIdentifier = buildGqIdentifier(fileName);

        downloadAddedTime = System.currentTimeMillis();

		state = STATE_WAITING;
	}

    /**
     * Add a file attachment.
     */
    public FrostDownloadItem(final String fileName, final String key, final long s) {
        this(fileName, key);
        this.fileSize = s;
    }

    /**
     * Add a shared file from filelist (user searched file and choosed one of the names).
     */
    public FrostDownloadItem(final FrostFileListFileObject newSfo, String newName) {

        newName = FileTransferManager.inst().getDownloadManager().ensureUniqueFilename(newName);

        FrostFileListFileObject sfo = null;

        // update the shared file object from database (key, owner, sources, ... may have changed)
        final FrostFileListFileObject updatedSfo = FileListStorage.inst().getFileBySha(newSfo.getSha());
        if( updatedSfo != null ) {
            sfo = updatedSfo;
        } else {
            // paranoia fallback
            sfo = newSfo;
        }

        fileName = newName;
        fileSize = sfo.getSize();
        key = sfo.getKey();

        gqIdentifier = buildGqIdentifier(fileName);

        setFileListFileObject(sfo);

        downloadAddedTime = System.currentTimeMillis();

        state = STATE_WAITING;
    }

    /**
     * Add a saved file from database.
     */
	public FrostDownloadItem(
            final String newFilename,
            final String newFilenamePrefix,
            final String newDownloadDir,
            final long newSize,
            final String newKey,
            final Boolean newEnabledownload,
            final int newState,
            final long newDownloadAddedTime,
            final long newDownloadStartedTime,
            final long newDownloadFinishedTime,
            final int newRetries,
            final long newLastDownloadStopTime,
            final String newGqId,
            final boolean newIsLoggedToFile,
            final boolean newIsTracked,
            final boolean newIsCompletionProgRun,
            final int newRuntimeSecondsWithoutProgress,
            final int newOldDoneBlocks,
            final String newAssociatedBoardName,
            final String newAssociatedMessageId,
            final FreenetPriority newPriority
            
    ) {
        fileName = newFilename;
        prefix = newFilenamePrefix;
        downloadDir = FileAccess.appendSeparator(newDownloadDir);
        fileSize = newSize;
        key = newKey;
        enabled = newEnabledownload;
        state = newState;
        downloadAddedTime = newDownloadAddedTime;
        downloadStartedTime = newDownloadStartedTime;
        downloadFinishedTime = newDownloadFinishedTime;
        retries = newRetries;
        lastDownloadStopTime = newLastDownloadStopTime;
        gqIdentifier = newGqId;
        isLoggedToFile= newIsLoggedToFile;
        isTracked= newIsTracked;
        isCompletionProgRun= newIsCompletionProgRun;
        runtimeSecondsWithoutProgress = newRuntimeSecondsWithoutProgress;
        oldDoneBlocks = newOldDoneBlocks;
        associatedBoardName = newAssociatedBoardName;
        associatedMessageId = newAssociatedMessageId;
        if( newPriority == null ) {
    		priority = FreenetPriority.getPriority(Core.frostSettings.getIntValue(Settings.FCP2_DEFAULT_PRIO_FILE_DOWNLOAD));
    	} else {
    		priority = newPriority;
    	}
        

        if( this.state == FrostDownloadItem.STATE_PROGRESS ) {
            // download was running at end of last shutdown
            stateShouldBeProgress = true;
        }

        if (this.state != FrostDownloadItem.STATE_DONE && this.state != FrostDownloadItem.STATE_FAILED) {
            this.state = FrostDownloadItem.STATE_WAITING;
        }
	}

    public boolean isSharedFile() {
        return getFileListFileObject() != null;
    }

    /**
     * Used only to set a new name if an item with same name is already in download table.
     */
    public void setFileName(final String s) {
        fileName = s;
    }
	public String getFileName() {
		if (prefix == null || prefix.length() == 0) {
			return fileName;
		}
		return prefix + "_" + fileName;
	}
	public String getUnprefixedFilename() {
		return fileName;
	}

    public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(final Long newFileSize) {
		fileSize = newFileSize;
        fireChange();
	}

	public String getKey() {
		return key;
	}
	public void setKey(final String newKey) {
	    setKey(newKey, true);
	}
	public void setKey(final String newKey, final boolean fireChange) {
	    key = newKey;
	    if (fireChange) {
	        fireChange();
	    }
	}

	public int getState() {
		return state;
	}
	public void setState(final int newState) {
		state = newState;
        fireChange();
	}

	public long getLastDownloadStopTime() {
		return lastDownloadStopTime;
	}
	public void setLastDownloadStopTime(final long val) {
        lastDownloadStopTime = val;
	}

	public int getRetries() {
		return retries;
	}
	public void setRetries(final int newRetries) {
		retries = newRetries;
        fireChange();
	}

	public Boolean isEnabled() {
		return enabled;
	}
	/**
	 * @param enabled new enable status of the item. If null, the current status is inverted
	 */
	public void setEnabled(Boolean newEnabled) {
		if (newEnabled == null && enabled != null) {
			//Invert the enable status
			newEnabled = !enabled;
		}
		enabled = newEnabled;
        fireChange();
	}

	public int getDoneBlocks() {
		return doneBlocks;
	}
	public void setDoneBlocks(final int newDoneBlocks) {
	    doneBlocks = newDoneBlocks;
	}

	public int getRequiredBlocks() {
		return requiredBlocks;
	}
	public void setRequiredBlocks(final int newRequiredBlocks) {
	    requiredBlocks = newRequiredBlocks;
	}

	public int getTotalBlocks() {
		return totalBlocks;
	}
	public void setTotalBlocks(final int newTotalBlocks) {
		totalBlocks = newTotalBlocks;
	}

    public Boolean isFinalized() {
        return isFinalized;
    }
    public void setFinalized(final boolean finalized) {
        if( finalized ) {
            isFinalized = Boolean.TRUE;
        } else {
            isFinalized = Boolean.FALSE;
        }
    }

    public long getDownloadAddedMillis() {
        return downloadAddedTime;
    }

    public long getDownloadFinishedMillis() {
        return downloadFinishedTime;
    }

    public void setDownloadFinishedTime(final long downloadFinishedTime) {
        this.downloadFinishedTime = downloadFinishedTime;
    }

    public long getDownloadStartedMillis() {
        return downloadStartedTime;
    }

    public void setDownloadStartedTime(final long downloadStartedTime) {
        this.downloadStartedTime = downloadStartedTime;
    }

    public String getGqIdentifier() {
        return gqIdentifier;
    }

    public void setGqIdentifier(final String gqId) {
        this.gqIdentifier = gqId;
    }

    public String getDownloadFilename() {
        return getDownloadDir() + getFileName();
    }

    public String getDownloadDir() {
        if (downloadDir == null) {
            return FileAccess.appendSeparator(Core.frostSettings.getValue(Settings.DIR_DOWNLOAD));
		}
        return downloadDir;
    }

    public boolean setDownloadDir(final String dir) {
    	String dirName = FileAccess.appendSeparator(dir);
    	if( FileAccess.createDir(new java.io.File(dirName)) ) {
    		downloadDir = dirName;
    		return true;
    	}
    	return false;
    }

    public void setFilenamePrefix(final String newPrefix) {
    	prefix = newPrefix;
    }

	public final String getFilenamePrefix() {
		return prefix;
    }

    public long getLastReceived() {
        if( getFileListFileObject() == null ) {
            return 0;
        } else {
            return getFileListFileObject().getLastReceived();
        }
    }

    public long getLastUploaded() {
        if( getFileListFileObject() == null ) {
            return 0;
        } else {
            return getFileListFileObject().getLastUploaded();
        }
    }

    public FrostFileListFileObject getFileListFileObject() {
        return fileListFileObject;
    }

    public void setFileListFileObject(final FrostFileListFileObject sharedFileObject) {
        if( this.fileListFileObject != null ) {
            this.fileListFileObject.removeListener(this);
        }

        int newState = -1;

        // if lastUploaded value changed, maybe restart failed download
        if( sharedFileObject != null && this.fileListFileObject != null ) {
//            if( sharedFileObject.getLastUploaded() > this.fileListFileObject.getLastUploaded() ) {
                if( getState() == STATE_FAILED ) {
                    newState = STATE_WAITING;
                }
//            }
        }

        this.fileListFileObject = sharedFileObject;

        if( this.fileListFileObject != null ) {
            this.fileListFileObject.addListener(this);
        }
        // take over key and update gui
        fireValueChanged();

        if( newState > -1 ) {
            setState(newState);
        }
    }

    /**
     * Called by a FrostFileListFileObject if a value interesting for FrostDownloadItem was set.
     */
    public void fireValueChanged() {
        // maybe take over the key, or set new key
        // NOTE: once a key is set, the ticker will allow to start this item!

        if( this.fileListFileObject != null ) {
            if( this.fileListFileObject.getKey() != null && this.fileListFileObject.getKey().length() > 0 ) {
                setKey( this.fileListFileObject.getKey(), false );
            }
        }

        // if progress increased, reset runtimeSecondsWithoutProgress
        if( isSharedFile() && getState() == STATE_PROGRESS ) {
            // check if progress changed, maybe reset
            if( oldDoneBlocks != getDoneBlocks() ) {
                // progress changed
                oldDoneBlocks = getDoneBlocks();
                resetRuntimeSecondsWithoutProgress();
            }
        }

        // remaining values are dynamically fetched from FrostFileListFileObject
        super.fireChange();
    }

    /**
     * Builds a global queue identifier.
     */
    private String buildGqIdentifier(final String filename) {
        return new StringBuilder()
            .append("Frost-")
            .append(filename.replace(' ', '_'))
            .append("-")
            .append(System.currentTimeMillis())
            .append(Core.getCrypto().getSecureRandom().nextInt(10)) // 0-9
            .toString();
    }

    public String getErrorCodeDescription() {
        return errorCodeDescription;
    }
    public void setErrorCodeDescription(final String errorCodeDescription) {
        this.errorCodeDescription = errorCodeDescription;
    }

    /**
     * @return  true if this item is an external global queue item
     */
    public boolean isExternal() {
        return isExternal;
    }
    public void setExternal(final boolean e) {
        isExternal = e;
    }

    public boolean isDirect() {
        return isDirect;
    }
    public void setDirect(final boolean d) {
        isDirect = d;
        super.fireChange();
    }

    public FreenetPriority getPriority() {
        return priority;
    }
    
    public void setPriority(final FreenetPriority priority) {
        this.priority = priority;
        super.fireChange();
    }

    /**
     * @return  true if the remove of this request was expected and item should not be removed from the table
     */
    public boolean isInternalRemoveExpected() {
        return internalRemoveExpected;
    }
    /**
     * Set to true if we restart a download (code=11 or 27).
     * onPersistentRequestRemoved method checks this and does not remove the request
     * from the table if the remove was expected.
     */
    public void setInternalRemoveExpected(final boolean internalRemoveExpected) {
        this.internalRemoveExpected = internalRemoveExpected;
        if( isSharedFile() && internalRemoveExpected ) {
            resetRuntimeSecondsWithoutProgress();
        }
    }

    @Override
    public String toString() {
        return getFileName();
    }

    public boolean isLoggedToFile() {
        return isLoggedToFile;
    }
    
    public boolean isTracked() {
        return isTracked;
    }

    public void setLoggedToFile(final boolean isLoggedToFile) {
        this.isLoggedToFile = isLoggedToFile;
    }
    
    public void setTracked(final boolean isTracked) {
        this.isTracked = isTracked;
    }

    /**
     * @return  the runtime of this download item in seconds
     */
    public synchronized int getRuntimeSecondsWithoutProgress() {
        return runtimeSecondsWithoutProgress;
    }

    /**
     * Sets runtimeSeconds to 0.
     */
    public synchronized void resetRuntimeSecondsWithoutProgress() {
        runtimeSecondsWithoutProgress = 0;
        // FIXME: called when download made progress, maybe remember last progress time
    }

    /**
     * Adds the specified amount of seconds to the value
     */
	public synchronized void addToRuntimeSecondsWithoutProgress(int s) {
        runtimeSecondsWithoutProgress += s;
    }

    public int getOldDoneBlocks() {
        return oldDoneBlocks;
    }

    public boolean isStateShouldBeProgress() {
        return stateShouldBeProgress;
    }

    public String getAssociatedMessageId() {
		return associatedMessageId;
	}

    public void setAssociatedMessageId(final String messageId) {
		associatedMessageId = messageId;
	}

	public String getAssociatedBoardName() {
		return associatedBoardName;
	}

	public void setAssociatedBoardName(final String boardName) {
		associatedBoardName = boardName;
	}

    public void associateWithFrostMessageObject(FrostMessageObject associatedFrostMessageObject) {
    	associatedBoardName = associatedFrostMessageObject.getBoard().getName();
    	associatedMessageId = associatedFrostMessageObject.getMessageId();
    }

    public boolean isCompletionProgRun() {
        return isCompletionProgRun;
    }

    public void setCompletionProgRun(final boolean isCompletionProgRun) {
        this.isCompletionProgRun = isCompletionProgRun;
    }
}
