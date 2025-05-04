/*
  DownloadModel.java / Frost

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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.FrostFileListFileObject;
import frost.storage.ExitSavable;
import frost.storage.StorageException;
import frost.storage.perst.FrostFilesStorage;
import frost.storage.perst.TrackDownloadKeysStorage;
import frost.util.gui.translation.Language;
import frost.util.model.SortedModel;
import frost.util.model.SortedTableFormat;

/**
 * This is the model that stores all FrostDownloadItems.
 * 
 * Its implementation is thread-safe (subclasses should synchronize against protected attribute data when necessary). It
 * is also assumed that the load and save methods will not be used while other threads are under way.
 */
public class DownloadModel extends SortedModel<FrostDownloadItem> implements ExitSavable {

	private static final Logger logger = LoggerFactory.getLogger(DownloadModel.class);

	public DownloadModel(final SortedTableFormat<FrostDownloadItem> f) {
		super(f);
	}
	
	public synchronized boolean addDownloadItemList(final List<FrostDownloadItem> itemsToAddList) {
		for(final FrostDownloadItem frostDownloadItem : itemsToAddList ) {
    		if( ! this.addDownloadItem(frostDownloadItem) ) {
    			return false;
    		}
    	}
		return true;
	}

	/**
	 * Will add this item to model if not already in model.
	 */
	public synchronized boolean addDownloadItem(final FrostDownloadItem itemToAdd) {

		final FrostFileListFileObject flfToAdd = itemToAdd.getFileListFileObject(); 

		// If download tracking is enabled, check if file has not been already downloaded
		if (Core.frostSettings.getBoolValue(Settings.TRACK_DOWNLOADS_ENABLED)) {

			// Only check if the file is not lingering around in finished
			// state...
			if (!itemToAdd.isTracked()) {
				final TrackDownloadKeysStorage trackDownloadKeysStorage = TrackDownloadKeysStorage.inst();
				if (trackDownloadKeysStorage.searchItemKey(itemToAdd.getKey())) {
					final Language language = Language.getInstance();

					// Ask if to download again
					if (JOptionPane.showConfirmDialog(null, language.formatMessage(
							"DownloadPane.alreadyTrackedDialog.body", itemToAdd.getKey()), language
							.getString("DownloadPane.alreadyTrackedDialog.title"), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
						return false;
					}
				}
			}
		}

		for (int x = 0; x < getItemCount(); x++) {
			final FrostDownloadItem item = getItemAt(x);

			// maybe null of manually added
			final FrostFileListFileObject flf = item.getFileListFileObject();

			if (flfToAdd != null && flf != null) {
				if (flfToAdd.getSha().equals(flf.getSha())) {
					// already in model (compared by SHA)
					return false;
				}
			}

			// FIXME: 0.7: if we add a new uri chk/name also check if we already download chk!
			// Problem: what if CHK is wrong, then we have to add chk/name. But in the reverse case we add chk/name and
			// name gets stripped because node reports rc=11, then we have 2 with same chk! ==> if node reports 11 then
			// check if we have already same plain chk.

			if (itemToAdd.getKey() != null && item.getKey() != null && item.getKey().equals(itemToAdd.getKey())) {
				// already in model (compared by key)
				return false;
			}

			// FIXME: also check downloaddir for same filename and build new name
			if (item.getFileName().equals(itemToAdd.getFileName())) {
				// same name, but different key. - rename quitely
				int cnt = 2;
				while (true) {
					final String nextNewName = itemToAdd.getFileName() + "_" + cnt;
					itemToAdd.setFileName(nextNewName);
					if (addDownloadItem(itemToAdd) == true) {
						// added to model
						return true;
					}
					cnt++;
				}
				// we should never come here
			}
		}

		// add directory of item to recent used dirs
		FileTransferManager.inst().getDownloadManager().addRecentDownloadDir(itemToAdd.getDownloadDir());

		// not in model, add
		addItem(itemToAdd);
		return true;
	}

	public void addExternalItem(final FrostDownloadItem i) {
		addItem(i);
	}

	/**
	 * Returns true if the model contains an item with the given sha.
	 */
	public synchronized boolean containsItemWithSha(final String sha) {
		for (int x = 0; x < getItemCount(); x++) {
			final FrostFileListFileObject flf = getItemAt(x).getFileListFileObject();
			if (flf != null) {
				if (flf.getSha().equals(sha)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Removes finished downloads from the download model.
	 */
	public synchronized void removeFinishedDownloads() {
		final ArrayList<FrostDownloadItem> items = new ArrayList<FrostDownloadItem>();
		for (int i = getItemCount() - 1; i >= 0; i--) {
			final FrostDownloadItem dlItem = getItemAt(i);
			if (dlItem.getState() == FrostDownloadItem.STATE_DONE) {
				items.add(dlItem);
			}
		}
		if (items.size() > 0) {
			removeItems(items);
		}
	}

	/**
	 * Removes external downloads from the download model.
	 */
	public synchronized void removeExternalDownloads() {
		final ArrayList<FrostDownloadItem> items = new ArrayList<FrostDownloadItem>();
		for (int i = getItemCount() - 1; i >= 0; i--) {
			final FrostDownloadItem dlItem = getItemAt(i);
			if (dlItem.isExternal()) {
				items.add(dlItem);
			}
		}
		if (items.size() > 0) {
			removeItems(items);
		}
	}

	/**
	 * Called to restart the item.
	 */
	public void restartItems(final List<FrostDownloadItem> items) {
		final LinkedList<FrostDownloadItem> running = new LinkedList<FrostDownloadItem>();

		for (int x = items.size() - 1; x >= 0; x--) {
			final FrostDownloadItem dlItem = items.get(x);

			if (dlItem.getState() == FrostDownloadItem.STATE_FAILED
					|| dlItem.getState() == FrostDownloadItem.STATE_WAITING
					|| dlItem.getState() == FrostDownloadItem.STATE_DONE) {
				dlItem.setState(FrostDownloadItem.STATE_WAITING);
				dlItem.setRetries(0);
				dlItem.setLastDownloadStopTime(0);
				dlItem.setEnabled(Boolean.valueOf(true)); // enable download on
				// restart
			} else {
				running.add(dlItem);
			}
		}

		restartRunningDownloads(running);
	}

	/**
	 * This method enables / disables download items in the model. If the enabled parameter is null, the current state
	 * of the item is inverted.
	 * 
	 * @param enabled
	 *            new state of the items. If null, the current state is inverted
	 */
	public synchronized void setAllItemsEnabled(final Boolean enabled) {
		for (int x = 0; x < getItemCount(); x++) {
			final FrostDownloadItem dlItem = getItemAt(x);
			if (dlItem.getState() != FrostDownloadItem.STATE_DONE) {
				dlItem.setEnabled(enabled);
				FileTransferManager.inst().getDownloadManager().notifyDownloadItemEnabledStateChanged(dlItem);
			}
		}
	}

	/**
	 * This method enables / disables download items in the model. If the enabled parameter is null, the current state
	 * of the item is inverted.
	 * 
	 * @param enabled
	 *            new state of the items. If null, the current state is inverted
	 * @param items
	 *            items to modify
	 */
	public void setItemsEnabled(final Boolean enabled, final List<FrostDownloadItem> items) {
		for (final FrostDownloadItem item : items) {
			if (item.getState() != FrostDownloadItem.STATE_DONE) {
				item.setEnabled(enabled);
				FileTransferManager.inst().getDownloadManager().notifyDownloadItemEnabledStateChanged(item);
			}
		}
	}

	/**
	 * Saves the download model to database.
	 */
	public void exitSave() throws StorageException {

		final List<FrostDownloadItem> itemList = getItems();
		try {
			FrostFilesStorage.inst().saveDownloadFiles(itemList);
		} catch (final Throwable e) {
			logger.error("Error saving download items", e);
			throw new StorageException("Error saving download items", e);
		}
	}

	/**
	 * Initializes the model
	 */
	public void initialize() throws StorageException {

		List<FrostDownloadItem> downloadItems;
		try {
			downloadItems = FrostFilesStorage.inst().loadDownloadFiles();
		} catch (final Throwable e) {
			logger.error("Error loading download items", e);
			throw new StorageException("Error loading download items", e);
		}
		for (final FrostDownloadItem di : downloadItems) {
			addDownloadItem(di);
		}
	}

	public boolean restartRunningDownloads(final List<FrostDownloadItem> dlItems) {

		// don't flag as failed later when item is removed from gq
		for (final FrostDownloadItem dlItem : dlItems) {
			dlItem.setInternalRemoveExpected(true);
		}

		removeItems(dlItems);

		new Thread() {
			@Override
			public void run() {
				// TODO: (ugly) wait until item is removed from global queue
				// before starting download with same gq identifier
				try {
					Thread.sleep(1500);
				} catch (final InterruptedException e) {
				}
				for (final FrostDownloadItem dlItem : dlItems) {
					dlItem.setState(FrostDownloadItem.STATE_WAITING);
					dlItem.setRetries(0);
					dlItem.setLastDownloadStopTime(0);
					addDownloadItem(dlItem);
				}
			}
		}.start();
		return true;
	}
}
