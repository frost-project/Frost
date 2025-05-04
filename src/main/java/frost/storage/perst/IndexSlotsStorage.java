/*
  IndexSlotsStorage.java / Frost
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

import java.time.OffsetDateTime;
import java.util.Iterator;

import org.garret.perst.GenericIndex;
import org.garret.perst.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Settings;
import frost.storage.ExitSavable;
import frost.storage.StorageException;
import frost.util.DateFun;

/**
 * Storage with an compound index of indexName and msgDate (int/long)
 */
public class IndexSlotsStorage extends AbstractFrostStorage implements ExitSavable {

	private static final Logger logger = LoggerFactory.getLogger(IndexSlotsStorage.class);

    private static final String STORAGE_FILENAME = "gixSlots.dbs";

    // boards have positive indexNames (their primkey)
    public static final int FILELISTS = -1;
    public static final int REQUESTS  = -2;

    private IndexSlotsStorageRoot storageRoot = null;

    private static IndexSlotsStorage instance = new IndexSlotsStorage();

    protected IndexSlotsStorage() {
        super();
    }

    public static IndexSlotsStorage inst() {
        return instance;
    }

    private boolean addToIndices(final IndexSlot gis) {
        if( getStorage() == null ) {
            return false;
        }
        final boolean wasOk = storageRoot.slotsIndexIL.put(new Key(gis.getIndexName(), gis.getMsgDate()), gis);
        storageRoot.slotsIndexLI.put(new Key(gis.getMsgDate(), gis.getIndexName()), gis);
        return wasOk;
    }

    @Override
    public String getStorageFilename() {
        return STORAGE_FILENAME;
    }

    @Override
    public boolean initStorage() {
        final String databaseFilePath = buildStoragePath(getStorageFilename()); // path to the database file
        final long pagePoolSize = getPagePoolSize(Settings.PERST_PAGEPOOLSIZE_INDEXSLOTS);

        open(databaseFilePath, pagePoolSize, false, true, true);

        storageRoot = (IndexSlotsStorageRoot)getStorage().getRoot();
        if (storageRoot == null) {
            // Storage was not initialized yet
            storageRoot = new IndexSlotsStorageRoot();
            // unique compound index of indexName and msgDate
            storageRoot.slotsIndexIL = getStorage().createIndex(new Class<?>[] { Integer.class, Long.class }, true);
            // index for cleanup
            storageRoot.slotsIndexLI = getStorage().createIndex(new Class<?>[] { Long.class, Integer.class }, true);
            getStorage().setRoot(storageRoot);
            commit(); // commit transaction
        }
        return true;
    }


	private static final Long minLongObj = Long.MIN_VALUE;
	private static final Integer minIntObj = Integer.MIN_VALUE;
	private static final Integer maxIntObj = Integer.MAX_VALUE;

    /**
     * Deletes any items with a date < maxDaysOld
     */
    public int cleanup(final int maxDaysOld) {

        // millis before maxDaysOld days
		final Long date = DateFun
				.toStartOfDayInMilli(OffsetDateTime.now(DateFun.getTimeZone()).minusDays(maxDaysOld + 1));

        // delete all items with msgDate < maxDaysOld
        int deletedCount = 0;

        beginExclusiveThreadTransaction();
        try {
            final Iterator<IndexSlot> i =
                storageRoot.slotsIndexLI.iterator(
                        new Key(
                                minLongObj,
                                minIntObj,
                                true),
                        new Key(
									date,
                                maxIntObj,
                                true),
                        GenericIndex.ASCENT_ORDER);
            while( i.hasNext() ) {
                final IndexSlot gis = i.next();
                storageRoot.slotsIndexIL.remove(gis); // also remove from IL index
                i.remove(); // remove from iterated LI index
                gis.deallocate(); // remove from Storage
                deletedCount++;
            }
        } finally {
            endThreadTransaction();
        }

        return deletedCount;
    }

    public IndexSlot getSlotForDate(final int indexName, final long date) {
        final Key dateKey = new Key(indexName, date);
        if( !beginCooperativeThreadTransaction() ) {
            logger.error("Failed to gather cooperative storage lock, returning new indexslot!");
            return new IndexSlot(indexName, date);
        }
        IndexSlot gis;
        try {
            gis = storageRoot.slotsIndexIL.get(dateKey);
//        String s = "";
//        s += "getSlotForDate: indexName="+indexName+", date="+date+"\n";
            if( gis == null ) {
                // not yet in storage
                gis = new IndexSlot(indexName, date);
            }
        } finally {
            endThreadTransaction();
        }
        return gis;
    }

    public void storeSlot(final IndexSlot gis) {
        if( !beginExclusiveThreadTransaction() ) {
            logger.error("Failed to gather exclusive storage lock, don't stored the indexslot!");
            return;
        }
        try {
            if( gis.getStorage() == null ) {
                gis.makePersistent(getStorage());
                addToIndices(gis);
            } else {
                gis.modify();
            }
        } finally {
            endThreadTransaction();
        }
    }

    public void exitSave() throws StorageException {
        close();
        storageRoot = null;
        logger.info("GlobalIndexSlotsStorage closed.");
    }

	// tests
	public static void main(String[] args) {
		IndexSlotsStorage s = IndexSlotsStorage.inst();

		s.initStorage();

		IndexSlotsStorageRoot root = (IndexSlotsStorageRoot) s.getStorage().getRoot();

		for (Iterator<IndexSlot> i = root.slotsIndexIL.iterator(); i.hasNext();) {
			IndexSlot gi = i.next();
			logger.info("----GI-------");
			logger.info("{}", gi);
		}

		s.getStorage().close();
	}
}
