/*
  DownloadManager.java / Frost

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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fcp.FcpResultGet;
import frost.fcp.FreenetKeys;
import frost.fileTransfer.FileTransferInformation;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.FreenetPriority;
import frost.storage.ExitSavable;
import frost.storage.StorageException;
import frost.storage.perst.TrackDownloadKeys;
import frost.storage.perst.TrackDownloadKeysStorage;
import frost.storage.perst.filelist.FileListStorage;
import frost.util.FileAccess;
import frost.util.Mixed;

public class DownloadManager implements ExitSavable {

	private static final Logger logger = LoggerFactory.getLogger(DownloadManager.class);

	private DownloadModel model;
	private DownloadPanel panel;
	private DownloadTicker ticker;

	private static final int MAX_RECENT_DOWNLOAD_DIRS = 20;
	private LinkedList<String> recentDownloadDirs;

	public DownloadManager() {
		super();
		loadRecentDownloadDirs();
	}

	public void initialize() throws StorageException {
		getPanel();
		getModel().initialize();
	}

	/**
	 * Start download now (manually).
	 */
	public boolean startDownload(final FrostDownloadItem dlItem) {
		if (FileTransferManager.inst().getPersistenceManager() != null) {
			return FileTransferManager.inst().getPersistenceManager()
					.startDownload(dlItem);
		} else {
			return ticker.startDownload(dlItem);
		}
	}

	public void startTicker() {
		if (Core.isFreenetOnline()) {
			getTicker().start();
		}
	}

	public void exitSave() throws StorageException {
		getPanel().getTableFormat().saveTableLayout();
		getModel().exitSave();
	}

	public void addPanelToMainFrame(final MainFrame mainFrame) {
		mainFrame.addPanel("MainFrame.tabbedPane.downloads", getPanel());
	}

	/**
	 * Count running items in model.
	 */
	public void updateFileTransferInformation(
			final FileTransferInformation infos) {
		int waitingItems = 0;
		int runningItems = 0;
		for (int x = 0; x < model.getItemCount(); x++) {
			final FrostDownloadItem dlItem = model.getItemAt(x);
			if (dlItem == null) {
				continue;
			}
			if (dlItem.getState() != FrostDownloadItem.STATE_DONE
					&& dlItem.getState() != FrostDownloadItem.STATE_FAILED) {
				waitingItems++;
			}
			if (dlItem.getState() == FrostDownloadItem.STATE_PROGRESS) {
				runningItems++;
			}
		}
		infos.setDownloadsRunning(runningItems);
		infos.setDownloadsWaiting(waitingItems);
	}

	/**
	 * Checks if a file with this name is already in model, and returns a new
	 * name if needed.
	 */
	public String ensureUniqueFilename(final String filename) {

		String newFilename = filename;
		int count = 2;

		while (true) {
			boolean loopAgain = false;
			for (int x = 0; x < getModel().getItemCount(); x++) {
				final FrostDownloadItem dlItem = getModel().getItemAt(x);
				if (dlItem.getFileName().equalsIgnoreCase(newFilename)) {
					loopAgain = true;
					// we have a duplicate filename
					// build new filename like "filename_2.ext"
					final int pos = filename.lastIndexOf('.');
					if (pos > 0) {
						final String beforeDot = filename.substring(0, pos);
						final String afterDot = filename.substring(pos);
						newFilename = beforeDot + "_" + (count++) + afterDot;
					} else {
						// no '.' in filename
						newFilename = filename + "_" + (count++);
					}
				}
			}
			if (!loopAgain) {
				break;
			}
		}
		return newFilename;
	}

	public DownloadPanel getPanel() {
		if (panel == null) {
			panel = new DownloadPanel();
			panel.setModel(getModel());
			panel.initialize();
		}
		return panel;
	}

	public DownloadModel getModel() {
		if (model == null) {
			model = new DownloadModel(new DownloadTableFormat());
		}
		return model;
	}

	private DownloadTicker getTicker() {
		if (ticker == null) {
			ticker = new DownloadTicker(getPanel());
		}
		return ticker;
	}

	private void loadRecentDownloadDirs() {
		recentDownloadDirs = new LinkedList<String>();

		for (int i = 0; i < MAX_RECENT_DOWNLOAD_DIRS; i++) {
			final String key = "DownloadManager.recentDownloadDir." + i;
			if (Core.frostSettings.getObjectValue(key) == null) {
				break;
			}
			recentDownloadDirs.add(Core.frostSettings.getString(key));
		}
	}

	private void saveRecentDownloadDirs() {
		int i = 0;

		for (final String dir : recentDownloadDirs) {
			final String key = "DownloadManager.recentDownloadDir." + i++;
			Core.frostSettings.setValue(key, dir);
		}
	}

	public void addRecentDownloadDir(final String downloadDir) {
		final String defaultDlDir = FileAccess
				.appendSeparator(Core.frostSettings
						.getString(Settings.DIR_DOWNLOAD));
		final String dlDir = FileAccess.appendSeparator(downloadDir);
		final ListIterator<String> i = recentDownloadDirs.listIterator();
		boolean found = false;

		if (dlDir == null || dlDir.length() == 0 || dlDir.equals(defaultDlDir)) {
			return;
		}

		// If the dlDir is already in the list...
		while (i.hasNext()) {
			final String dir = i.next();
			if (dir.equals(dlDir)) {
				// ... make it the most recently used.
				i.remove();
				recentDownloadDirs.add(dlDir);
				found = true;
				break;
			}
		}

		if (!found) {
			recentDownloadDirs.add(dlDir);
		}

		while (recentDownloadDirs.size() > MAX_RECENT_DOWNLOAD_DIRS) {
			recentDownloadDirs.remove();
		}

		saveRecentDownloadDirs();
	}

	public final LinkedList<String> getRecentDownloadDirs() {
		return recentDownloadDirs;
	}

	/**
	 * Add a new download item to the download table model.
	 * 
	 * @param key
	 *            complete key of download item
	 * @param fileName
	 *            file name
	 */
	public FrostDownloadItem addNewDownload(final String key,
		final String fileName, final String dlDir, final String prefix) {
		// TODO: enhancement: search for key in shared files, maybe add as shared file
		final FrostDownloadItem dlItem = new FrostDownloadItem(fileName, key);
		dlItem.setDownloadDir(dlDir);
		dlItem.setFilenamePrefix(prefix);
		model.addDownloadItem(dlItem); // false if file is already in table
		return dlItem;
	}

	public FrostDownloadItem addNewDownload(final String key,
			final String fileName, final String dlDir) {
		return addNewDownload(key, fileName, dlDir, null);
	}

	public FrostDownloadItem addNewDownload(final String key,
			final String fileName) {
		return addNewDownload(key, fileName, null, null);
	}
	
	public static ArrayList<FrostDownloadItem> parseKeys(final String text) {
		ArrayList<FrostDownloadItem> frostDownloadItemList = new ArrayList<FrostDownloadItem>();
		
		final String[] keyList = text.trim().split("[;\n]");
		if (keyList == null || keyList.length == 0) {
			return frostDownloadItemList;
		}

		for (final String element : keyList) {
			String key = element.trim();

			if (key.length() < 5) {
				continue;
			}

			// maybe convert html codes (e.g. %2c -> , )
			if (key.indexOf("%") > 0) {
				try {
					key = URLDecoder.decode(key, "UTF-8");
				} catch (final UnsupportedEncodingException ex) {
					logger.error("Decode of HTML code failed", ex);
				}
			}

			// find key type (chk,ssk,...)
			int pos = -1;
			for (int i = 0; i < FreenetKeys.getFreenetKeyTypes().length; i++) {
				final String string = FreenetKeys.getFreenetKeyTypes()[i];
				pos = key.indexOf(string);
				if (pos >= 0) {
					break;
				}
			}
			if (pos < 0) {
				// no valid keytype found
				continue;
			}

			// strip all before key type
			if (pos > 0) {
				key = key.substring(pos);
			}

			if (key.length() < 5) {
				// at least the SSK@? is needed
				continue;
			}

			// take the filename from the last part of the key
			String fileName;
			final int sepIndex = key.lastIndexOf("/");
			if (sepIndex > -1) {
				fileName = key.substring(sepIndex + 1);
			} else {
				// fallback: use key as filename
				fileName = key.substring(4);
			}

			String checkKey = key;
			// remove filename from CHK key
			if (key.startsWith("CHK@") && key.indexOf("/") > -1) {
				checkKey = key.substring(0, key.indexOf("/"));
			}

			// On 0.7 we remember the full provided download uri as key.
			// If the node reports download failed, error code 11 later,
			// then we strip the filename
			// from the uri and keep trying with chk only

			// finally check if the key is valid for this network
			if (!FreenetKeys.isValidKey(checkKey)) {
				// showInvalidKeyErrorDialog(key);
				continue;
			}

			// add valid key to download table
			frostDownloadItemList.add( new FrostDownloadItem(fileName, key));
		}
		return frostDownloadItemList;
	}
	

	/**
	 * @return true if request should be retried
	 */
	public boolean notifyDownloadFinished(final FrostDownloadItem downloadItem,
			final FcpResultGet result, final File targetFile) {

		final String filename = downloadItem.getFileName();
		final String key = downloadItem.getKey();

		boolean retryImmediately = false;

		if (result == null || result.isSuccess() == false) {
			// download failed

			if (result != null) {
				downloadItem.setErrorCodeDescription(result
						.getCodeDescription());
			}

			if (result != null && result.getReturnCode() == 5
					&& key.startsWith("CHK@") && key.indexOf("/") > 0) {
				// 5 - Archive failure
				// node tries to access the file as a .zip file, try download
				// again without any path
				final String newKey = key.substring(0, key.indexOf("/"));
				downloadItem.setKey(newKey);
				downloadItem.setState(FrostDownloadItem.STATE_WAITING);
				downloadItem.setLastDownloadStopTime(0);
				downloadItem.setInternalRemoveExpected(true);
				retryImmediately = true;

				logger.warn("Removed all path levels from key: {} ; {}",  key, newKey);

			} else if (result != null && result.getReturnCode() == 11
					&& key.startsWith("CHK@") && key.indexOf("/") > 0) {
				// 11 - The URI has more metastrings and I can't deal with them
				// remove one path level from CHK
				final String newKey = key.substring(0, key.lastIndexOf("/"));
				downloadItem.setKey(newKey);
				downloadItem.setState(FrostDownloadItem.STATE_WAITING);
				downloadItem.setLastDownloadStopTime(0);
				downloadItem.setInternalRemoveExpected(true);
				retryImmediately = true;

				logger.warn("Removed one path level from key: {} ; {}", key, newKey);

			} else if (result != null && result.getReturnCode() == 27
					&& result.getRedirectURI() != null) {
				// permanent redirect, use new uri
				downloadItem.setKey(result.getRedirectURI());
				downloadItem.setState(FrostDownloadItem.STATE_WAITING);
				downloadItem.setInternalRemoveExpected(true);
				retryImmediately = true;

				logger.warn("Redirected to URI: {}", result.getRedirectURI());

			} else if (result != null && result.isFatal()) {
				// fatal, don't retry
				// downloadItem.setEnabled(Boolean.FALSE); // keep enabled to
				// allow sending of requests for shared files
				downloadItem.setState(FrostDownloadItem.STATE_FAILED);
				logger.warn("FILEDN: Download of {} failed FATALLY.", filename);
			} else {
				downloadItem.setRetries(downloadItem.getRetries() + 1);

				logger.warn("FILEDN: Download of {} failed.", filename);
				// set new state -> failed or waiting for another try
				if (downloadItem.getRetries() > Core.frostSettings
						.getInteger(Settings.DOWNLOAD_MAX_RETRIES)) {
					// downloadItem.setEnabled(Boolean.valueOf(false)); // keep
					// enabled to allow sending of requests for shared files
					downloadItem.setState(FrostDownloadItem.STATE_FAILED);
				} else {
					downloadItem.setState(FrostDownloadItem.STATE_WAITING);
				}
			}
		} else {

			logger.info("FILEDN: Download of {} was successful.", filename);

			// download successful
			downloadItem.setFileSize(targetFile.length());
			downloadItem.setState(FrostDownloadItem.STATE_DONE);
			downloadItem.setEnabled(Boolean.valueOf(false));

			downloadItem.setDownloadFinishedTime(System.currentTimeMillis());

			// update lastDownloaded time in filelist
			if (downloadItem.isSharedFile()) {
				FileListStorage.inst()
						.updateFrostFileListFileObjectAfterDownload(
								downloadItem.getFileListFileObject().getSha(),
								System.currentTimeMillis());
			}

			// maybe track download
			if (Core.frostSettings
					.getBoolean(Settings.TRACK_DOWNLOADS_ENABLED)
					&& !downloadItem.isTracked()) {
				TrackDownloadKeysStorage trackDownloadKeysStorage = TrackDownloadKeysStorage
						.inst();
				trackDownloadKeysStorage.storeItem(new TrackDownloadKeys(
						downloadItem.getKey(), downloadItem.getFileName(),
						downloadItem.getAssociatedBoardName(), downloadItem
								.getFileSize(), downloadItem
								.getDownloadFinishedMillis()));
				downloadItem.setTracked(true);
			}

			// maybe log successful download to file localdata/downloads.txt
			if (Core.frostSettings
					.getBoolean(Settings.LOG_DOWNLOADS_ENABLED)
					&& !downloadItem.isLoggedToFile()) {
				final String line = downloadItem.getKey() + "/"
						+ downloadItem.getFileName();
				final String fileName = Core.frostSettings
						.getString(Settings.DIR_LOCALDATA)
						+ "Frost-Downloads.log";
				final File targetLogFile = new File(fileName);
				FileAccess.appendLineToTextfile(targetLogFile, line);
				downloadItem.setLoggedToFile(true);
			}

			final String execProg = Core.frostSettings
					.getString(Settings.EXEC_ON_DOWNLOAD);
			if (execProg != null && execProg.length() > 0
					&& !downloadItem.isExternal()
					&& !downloadItem.isCompletionProgRun()) {
				final File dir = new File(downloadItem.getDownloadDir());
				final Map<String, String> oldEnv = System.getenv();
				final String[] newEnv = new String[oldEnv.size() + 5];
				final String args[] = new String[6];
				int i;

				args[0] = execProg;
				args[1] = downloadItem.getFileName();
				args[2] = downloadItem.getFilenamePrefix();
				args[3] = downloadItem.getKey();
				args[4] = downloadItem.getAssociatedBoardName();
				args[5] = downloadItem.getAssociatedMessageId();

				for (i = 0; i < args.length; i++) {
					if (args[i] == null) {
						args[i] = "";
					}
				}

				i = 0;
				for (final Map.Entry<String, String> entry : oldEnv.entrySet()) {
					newEnv[i++] = entry.getKey() + "=" + entry.getValue();
				}
				newEnv[i++] = "FROST_FILENAME=" + downloadItem.getFileName();
				newEnv[i++] = "FROST_FILENAME_PREFIX="
						+ downloadItem.getFilenamePrefix();
				newEnv[i++] = "FROST_KEY=" + downloadItem.getKey();
				newEnv[i++] = "FROST_ASSOC_BOARD_NAME="
						+ downloadItem.getAssociatedBoardName();
				newEnv[i++] = "FROST_ASSOC_MSG_ID="
						+ downloadItem.getAssociatedMessageId();

				try {
					Runtime.getRuntime().exec(args, newEnv, dir);
				} catch (final Exception e) {
					logger.error("Could not exec {}", execProg, e);
				}
			}

			downloadItem.setCompletionProgRun(true);

			// maybe remove finished download immediately
			if (Core.frostSettings
					.getBoolean(Settings.DOWNLOAD_REMOVE_FINISHED)) {
				FileTransferManager.inst().getDownloadManager().getModel()
						.removeFinishedDownloads();
			}
		}

		if (retryImmediately) {
			downloadItem.setLastDownloadStopTime(-1);
		} else {
			downloadItem.setLastDownloadStopTime(System.currentTimeMillis());
		}

		return retryImmediately;
	}

	public List<FrostDownloadItem> getDownloadItemList() {
		return getModel().getItems();
	}

	/**
	 * Chooses next download item to start from download table.
	 * 
	 * @return the next download item to start downloading or null if a suitable
	 *         one was not found.
	 */
	public FrostDownloadItem selectNextDownloadItem() {

		// get the item with state "Waiting"
		final ArrayList<FrostDownloadItem> waitingItems = new ArrayList<FrostDownloadItem>();
		for (int i = 0; i < model.getItemCount(); i++) {
			final FrostDownloadItem dlItem = model.getItemAt(i);
			
			final boolean itemIsEnabled = (dlItem.isEnabled() == null ? true
					: dlItem.isEnabled().booleanValue());
			if (!itemIsEnabled) {
				continue;
			}
			if (dlItem.isExternal()) {
				continue;
			}
			if (dlItem.getKey() == null) {
				// still no key, wait
				continue;
			}

			if (dlItem.getState() != FrostDownloadItem.STATE_WAITING) {
				continue;
			}

			// check if waittime is expired
			final long waittimeMillis = Core.frostSettings
					.getInteger(Settings.DOWNLOAD_WAITTIME) * 60L * 1000L;
			// min->millisec
			if (dlItem.getLastDownloadStopTime() == 0 // never started
					|| (System.currentTimeMillis() - dlItem
							.getLastDownloadStopTime()) > waittimeMillis) {
				waitingItems.add(dlItem);
			}
		}

		if (waitingItems.size() == 0) {
			return null;
		}

		if (waitingItems.size() > 1) { // performance issues
			Collections.sort(waitingItems, nextItemCmp);
		}
		return waitingItems.get(0);
	}

	public void notifyDownloadItemEnabledStateChanged(final FrostDownloadItem dlItem) {
		// for persistent items, set priority to 6 (pause) when disabled; and to
		// configured default if enabled
		if (dlItem.isExternal()) {
			return;
		}

		if (dlItem.getState() != FrostDownloadItem.STATE_PROGRESS) {
			// not running, not in queue
			return;
		}
		
		final boolean itemIsEnabled = (dlItem.isEnabled() == null ? true
				: dlItem.isEnabled().booleanValue());
		FreenetPriority prio = FreenetPriority.PAUSE;
		if (itemIsEnabled) {
			prio = FreenetPriority.getPriority(Core.frostSettings.getInteger(Settings.FCP2_DEFAULT_PRIO_FILE_DOWNLOAD));
		}
		
		List<FrostDownloadItem> frostDownloadItems = new ArrayList<FrostDownloadItem>();
		frostDownloadItems.add(dlItem);
		
		panel.changeItemPriorites(frostDownloadItems, prio);
	}

	/**
	 * Used to sort FrostDownloadItems by lastUpdateStartTimeMillis ascending.
	 */
	private static final Comparator<FrostDownloadItem> nextItemCmp = new Comparator<FrostDownloadItem>() {
		public int compare(final FrostDownloadItem value1,
				final FrostDownloadItem value2) {

			// choose item that with lowest addedTime
			final int cmp1 = Mixed.compareLong(value1.getDownloadAddedMillis(),
					value2.getDownloadAddedMillis());
			if (cmp1 != 0) {
				return cmp1;
			}

			// equal addedTimes, choose by blocksRemaining
			int blocksTodo1;
			int blocksTodo2;

			// compute remaining blocks
			if (value1.getRequiredBlocks() > 0 && value1.getDoneBlocks() > 0) {
				blocksTodo1 = value1.getRequiredBlocks()
						- value1.getDoneBlocks();
			} else {
				blocksTodo1 = Integer.MAX_VALUE; // never started
			}
			if (value2.getRequiredBlocks() > 0 && value2.getDoneBlocks() > 0) {
				blocksTodo2 = value2.getRequiredBlocks()
						- value2.getDoneBlocks();
			} else {
				blocksTodo2 = Integer.MAX_VALUE; // never started
			}

			final int cmp2 = Mixed.compareInt(blocksTodo1, blocksTodo2);
			if (cmp2 != 0) {
				return cmp2;
			}

			// equal remainingBlocks, choose smaller file (filesize can be -1)
			return Mixed
					.compareLong(value1.getFileSize(), value2.getFileSize());
		}
	};
}
