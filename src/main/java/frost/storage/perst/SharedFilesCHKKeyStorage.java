/*
  SharedFilesCHKKeyStorage.java / Frost
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.garret.perst.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Settings;
import frost.storage.ExitSavable;
import frost.storage.StorageException;
import frost.util.Mixed;

public class SharedFilesCHKKeyStorage extends AbstractFrostStorage implements ExitSavable {

	private static final Logger logger =  LoggerFactory.getLogger(SharedFilesCHKKeyStorage.class);

    private SharedFilesCHKKeyStorageRoot storageRoot = null;

    private static final String STORAGE_FILENAME = "sfChkKeys.dbs";

    private static SharedFilesCHKKeyStorage instance = new SharedFilesCHKKeyStorage();

    protected SharedFilesCHKKeyStorage() {
        super();
    }

    public static SharedFilesCHKKeyStorage inst() {
        return instance;
    }

    private boolean addToIndices(final SharedFilesCHKKey gis) {
        return storageRoot.chkKeys.put(gis.getChkKey(), gis);
    }

    public void storeItem(final SharedFilesCHKKey gis) {
        if( getStorage() == null ) {
            return;
        }
        if( gis.getStorage() == null ) {
            gis.makePersistent(getStorage());
            addToIndices(gis);
        } else {
            gis.modify();
        }
    }

    public void exitSave() throws StorageException {
        close();
        storageRoot = null;
        logger.info("SharedFilesCHKKeyStorage closed.");
    }

    @Override
    public String getStorageFilename() {
        return STORAGE_FILENAME;
    }

    @Override
    public boolean initStorage() {
        final String databaseFilePath = buildStoragePath(getStorageFilename()); // path to the database file
        final long pagePoolSize = getPagePoolSize(Settings.PERST_PAGEPOOLSIZE_SHAREDFILESCHKKEYS);

        open(databaseFilePath, pagePoolSize, true, true, false);

        storageRoot = (SharedFilesCHKKeyStorageRoot)getStorage().getRoot();
        if (storageRoot == null) {
            // Storage was not initialized yet
            storageRoot = new SharedFilesCHKKeyStorageRoot();
            // unique index of chkKeys
            storageRoot.chkKeys = getStorage().createIndex(String.class, true);
            getStorage().setRoot(storageRoot);
            commit(); // commit transaction
        }
        return true;
    }

    ////////////////////////////////////////////////////////////

    public List<SharedFilesCHKKey> getSharedFilesCHKKeysToSend(int maxKeys) {
        // get a number of CHK keys from database that must be send
        // include (5 to 10) of our new CHK keys into this list, don't send CHK keys of different identities
        // (sending only 1 of our in one list is not needed, because others might pick up more of
        //  our keys also)

        final int ownKeysToSend = 5 + (int) (Math.random() * 5);

        final List<SharedFilesCHKKey> keysToSend = new LinkedList<SharedFilesCHKKey>();

        if( !beginCooperativeThreadTransaction() ) {
            return keysToSend;
        }
        try {
            // first search for CHK keys that were created by us, but were never send
            {
                for( final SharedFilesCHKKey sfk : storageRoot.chkKeys ) {
                    if (sfk.getSeenCount() == 0 && sfk.getChkKey() != null && sfk.getChkKey().length() > 0) {
                        keysToSend.add(sfk);
                        if( keysToSend.size() >= ownKeysToSend ) {
                            break;
                        }
                    }
                }
            }
            maxKeys -= keysToSend.size();
            // then search for other files to send, but don't include other new files from us
            // - the CHK key must be downloaded already (our new files are not yet downloaded)
            // - key must be valid
            // - keys firstseen must be not earlier than 7 days (don't send old stuff around)
            // - keys lastseen must be more than 24h before (don't send keys we just received)
            // - keys lastsent must be more than 24h before (don't send keys we just sent)
            // - order by seencount asc -> collect keys that are not seen often
            // - collect a maximum of 300 keys
            {
                final long now = System.currentTimeMillis();
                final long minFirstSeen = now - (7L * 24L * 60L * 60L * 1000L); // now - 7 days
                final long maxLastSeen = now - (1L * 24L * 60L * 60L * 1000L); // now - 1 day

                // first collect ALL other keys to send, then sort them and choose maxKeys items
                final List<SharedFilesCHKKey> otherKeysToSend = new ArrayList<SharedFilesCHKKey>();
                for( final SharedFilesCHKKey sfk : storageRoot.chkKeys ) {
                    if( sfk.isDownloaded()
                            && sfk.isValid()
                            && sfk.getLastSeen() < maxLastSeen
                            && sfk.getLastSent() < maxLastSeen
                            && sfk.getFirstSeen() > minFirstSeen )
                    {
                        otherKeysToSend.add(sfk);
                    }
                }

                Collections.sort(otherKeysToSend, seenCountComparator);

                if( otherKeysToSend.size() > 0 ) {
                    keysToSend.addAll(otherKeysToSend.subList(0, Math.min(maxKeys, otherKeysToSend.size())));
                }

                otherKeysToSend.clear();
            }
        } finally {
            endThreadTransaction();
        }

        return keysToSend;
    }

    /**
     * Returns SharedFilesCHKKey for this chkKey or null if not in Storage.
     */
    public SharedFilesCHKKey retrieveSharedFilesCHKKey(final String chkKey) {
        if( !beginCooperativeThreadTransaction() ) {
            return null;
        }
        final SharedFilesCHKKey key;
        try {
            key = storageRoot.chkKeys.get(new Key(chkKey));
        } finally {
            endThreadTransaction();
        }
        return key;
    }

    /**
     * Retrieves all unretrieved CHK keys to download.
     * Called one time during startup.
     * @return  List of Strings
     */
    public List<String> retrieveSharedFilesCHKKeysToDownload(final int maxRetries) {

        final List<SharedFilesCHKKey> keysToDownload = new ArrayList<SharedFilesCHKKey>();

        if( !beginCooperativeThreadTransaction() ) {
            return Collections.emptyList();
        }
        try {
            for( final SharedFilesCHKKey sfk : storageRoot.chkKeys ) {
                if( !sfk.isDownloaded() && sfk.getDownloadRetries() < maxRetries ) {
                    keysToDownload.add(sfk);
                }
            }
        } finally {
            endThreadTransaction();
        }

        Collections.sort(keysToDownload, lastDownloadTryStopTimeComparator);

        // now create a list of string only
        final List<String> chkKeys = new LinkedList<String>();
        for( final SharedFilesCHKKey sfk : keysToDownload ) {
            chkKeys.add( sfk.getChkKey() );
        }
        keysToDownload.clear();

        return chkKeys;
    }

    /**
     * Updates newkey in database.
     */
    public boolean updateSharedFilesCHKKeyAfterDownloadSuccessful(
            final String chkKey,
            final long timestamp,
            final boolean isValid)
    {
        if( !beginExclusiveThreadTransaction() ) {
            return false;
        }
        try {
            final SharedFilesCHKKey key = storageRoot.chkKeys.get(new Key(chkKey));
            if( key == null ) {
                return false;
            }
            key.setDownloaded(true);
            // we set the firstSeen of this CHK to the timestamp from inside the content,
            // this should be the earliest firstSeen time
            if( key.getFirstSeen() > timestamp ) {
                key.setFirstSeen(timestamp);
            }
            key.setValid(isValid);
            key.modify();
        } finally {
            endThreadTransaction();
        }

        return true;
    }

    /**
     * Updates newkey in database.
     * @return true if download should be retried
     */
    public boolean updateSharedFilesCHKKeyAfterDownloadFailed(final String chkKey, final int maxRetries) {

        if( !beginExclusiveThreadTransaction() ) {
            return false;
        }
        try {
            final SharedFilesCHKKey key = storageRoot.chkKeys.get(new Key(chkKey) );
            if( key == null ) {
                endThreadTransaction();
                return false;
            }

            key.incDownloadRetries();
            key.setLastDownloadTryStopTime(System.currentTimeMillis());

            key.modify();

            if( key.getDownloadRetries() < maxRetries ) {
                return true; // retry download
            } else {
                return false;
            }
        } finally {
            endThreadTransaction();
        }
    }

    /**
     * Delete all table entries that were not seen longer than maxDaysOld.
     * @return  count of deleted rows
     */
    public int cleanupTable(final int maxDaysOld) {

        final long minVal = System.currentTimeMillis() - (maxDaysOld * 24L * 60L * 60L * 1000L);

        // delete all items with lastSeen < minVal, but lastSeen > 0
        int deletedCount = 0;

        beginExclusiveThreadTransaction();
        try {
            final Iterator<SharedFilesCHKKey> i = storageRoot.chkKeys.iterator();
            while(i.hasNext()) {
                final SharedFilesCHKKey sfk = i.next();
                if( sfk.getLastSeen() > 0 && sfk.getLastSeen() < minVal ) {
                    i.remove(); // remove from iterated index
                    sfk.deallocate(); // remove from Storage
                    deletedCount++;
                }
            }
        } finally {
            endThreadTransaction();
        }

        return deletedCount;
    }

	public static void main(String[] args) {

		logger.info("a = {}", Math.random() * 5);

		SharedFilesCHKKeyStorage s = new SharedFilesCHKKeyStorage();
		s.initStorage();

		// find:
//      String sql = "SELECT chkkey FROM SHAREDFILESCHK WHERE isdownloaded=FALSE AND downloadretries<? " +
//      "ORDER BY lastdownloadtrystop ASC";

		long t1 = System.currentTimeMillis();

		SharedFilesCHKKeyStorageRoot root = (SharedFilesCHKKeyStorageRoot) s.getStorage().getRoot();

		ArrayList<SharedFilesCHKKey> foundItems = new ArrayList<>();
		Iterator<SharedFilesCHKKey> i = root.chkKeys.iterator();
		int count = 0;
		while (i.hasNext()) {
			SharedFilesCHKKey sfk = i.next();

			if (!sfk.isDownloaded() && sfk.getDownloadRetries() < 50) {
				count++;
				logger.info("Found #{}", count);
				foundItems.add(sfk);
				sfk.modify();
			}
		}

		logger.info("found: {}", count);

		for (Iterator<SharedFilesCHKKey> j = foundItems.iterator(); j.hasNext();) {
			SharedFilesCHKKey k1 = j.next();
			logger.info("-> {}", k1.getLastDownloadTryStopTime());
		}
		logger.info("sorting!");
		Collections.sort(foundItems, lastDownloadTryStopTimeComparator);
		for (Iterator<SharedFilesCHKKey> j = foundItems.iterator(); j.hasNext();) {
			SharedFilesCHKKey k1 = j.next();
			logger.info("-> {}", k1.getLastDownloadTryStopTime());
		}

		logger.info("duration = {}", System.currentTimeMillis() - t1);

		logger.info("ready");

		s.getStorage().commit();
		s.getStorage().close();
	}

    protected static class LastDownloadTryStopTimeComparator implements Comparator<SharedFilesCHKKey> {
        public int compare(final SharedFilesCHKKey arg0, final SharedFilesCHKKey arg1) {
            return Mixed.compareLong(arg0.getLastDownloadTryStopTime(), arg1.getLastDownloadTryStopTime());
        }
    }
    protected final static LastDownloadTryStopTimeComparator lastDownloadTryStopTimeComparator = new LastDownloadTryStopTimeComparator();

    protected static class SeenCountComparator implements Comparator<SharedFilesCHKKey> {
        public int compare(final SharedFilesCHKKey arg0, final SharedFilesCHKKey arg1) {
            return Mixed.compareInt(arg0.getSeenCount(), arg0.getSeenCount());
        }
    }
    protected final static SeenCountComparator seenCountComparator = new SeenCountComparator();
}
