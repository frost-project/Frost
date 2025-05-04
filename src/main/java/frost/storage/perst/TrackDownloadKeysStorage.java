/*
  TrackDownloadKeysStorage.java / Frost
  Copyright (C) 2010  Frost Project <jtcfrost.sourceforge.net>

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

import java.util.Iterator;

import org.garret.perst.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Settings;
import frost.storage.ExitSavable;
import frost.storage.StorageException;

public class TrackDownloadKeysStorage extends AbstractFrostStorage implements ExitSavable {

	private static final Logger logger =  LoggerFactory.getLogger(TrackDownloadKeysStorage.class);

	private TrackDownloadKeysStorageRoot storageRoot = null;

	private static final String STORAGE_FILENAME = "TrackDownloadKeys.dbs";

	private static TrackDownloadKeysStorage instance = new TrackDownloadKeysStorage();

	protected TrackDownloadKeysStorage() {
		super();
	}

	public static TrackDownloadKeysStorage inst() {
		return instance;
	}

	private boolean addToIndices(final TrackDownloadKeys trackDownloadKeys) {
		return storageRoot.downloadKeyList.put(trackDownloadKeys.getChkKey(), trackDownloadKeys);
	}

	public void storeItem(final TrackDownloadKeys trackDownloadKeys) {
		if (getStorage() == null) {
			return;
		}
		if (!beginExclusiveThreadTransaction()) {
			return;
		}
		try {
			if (trackDownloadKeys.getStorage() == null) {
				if (addToIndices(trackDownloadKeys)) {
					trackDownloadKeys.makePersistent(getStorage());
				}
			} else {
				trackDownloadKeys.modify();
			}
		} finally {
			endThreadTransaction();
		}
	}

	public boolean searchItemKey(final String chkKey) {
		return storageRoot.downloadKeyList.get(chkKey) != null;
	}

	public TrackDownloadKeys getItemByKey(final String chkKey) {
		return storageRoot.downloadKeyList.get(chkKey);
	}

	public TrackDownloadKeys removeItemByKey(final String chkKey) {
		return storageRoot.downloadKeyList.remove(chkKey);
	}

	public void exitSave() throws StorageException {
		close();
		storageRoot = null;
		logger.info("TrackDownloadKeyStorage closed.");
	}

	public final Index<TrackDownloadKeys> getDownloadKeyList() {
		return storageRoot.downloadKeyList;
	}

	@Override
	public String getStorageFilename() {
		return STORAGE_FILENAME;
	}

	@Override
	public boolean initStorage() {
		final String databaseFilePath = buildStoragePath(getStorageFilename()); // path to the database file
		final long pagePoolSize = getPagePoolSize(Settings.PERST_PAGEPOOLSIZE_TRACKDOWNLOADKEYS);

		open(databaseFilePath, pagePoolSize, true, true, false);

		storageRoot = (TrackDownloadKeysStorageRoot)getStorage().getRoot();
		if (storageRoot == null || storageRoot.downloadKeyList == null) {
			// Storage was not initialized yet
			storageRoot = new TrackDownloadKeysStorageRoot();
			// unique index of chkKeys
			storageRoot.downloadKeyList = getStorage().createIndex(String.class, true);

			getStorage().setRoot(storageRoot);
			commit(); // commit transaction
		}
		logger.info("TrackDownloadKeyStorage initialized.");
		return true;
	}

	/**
	 * Delete all entries that were downloaded maxDaysOld dayes ago.
	 * @return  count of deleted rows
	 */
	public int cleanupTable(final int maxDaysOld) {

		final long minVal = System.currentTimeMillis() - (maxDaysOld * 24L * 60L * 60L * 1000L);

		// delete all items with lastSeen < minVal, but lastSeen > 0
		int deletedCount = 0;

		beginExclusiveThreadTransaction();
		try {
			final Iterator<TrackDownloadKeys> trackDownloadKeyIterator = storageRoot.downloadKeyList.iterator();
			while(trackDownloadKeyIterator.hasNext()) {
				final TrackDownloadKeys trackDownloadKeys = trackDownloadKeyIterator.next();
				if( trackDownloadKeys.getDownloadFinishedTime() > 0 && trackDownloadKeys.getDownloadFinishedTime() < minVal ) {
					trackDownloadKeyIterator.remove(); // remove from iterated index
					trackDownloadKeys.deallocate(); // remove from Storage
					deletedCount++;
				}
			}
		} finally {
			endThreadTransaction();
		}

		return deletedCount;
	}
}
