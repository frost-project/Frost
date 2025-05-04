/*
  IdentitiesStorage.java / Frost
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
package frost.storage.perst.identities;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.garret.perst.IPersistentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.identities.Identity;
import frost.identities.LocalIdentity;
import frost.storage.ExitSavable;
import frost.storage.StorageException;
import frost.storage.perst.AbstractFrostStorage;
import frost.storage.perst.filelist.FileListStorage;
import frost.storage.perst.messages.MessageStorage;

public class IdentitiesStorage extends AbstractFrostStorage implements ExitSavable {

	private static final Logger logger = LoggerFactory.getLogger(IdentitiesStorage.class);

    private static final String STORAGE_FILENAME = "identities.dbs";

    private IdentitiesStorageRoot storageRoot = null;

    private static IdentitiesStorage instance = new IdentitiesStorage();

    protected IdentitiesStorage() {
        super();
    }

    public static IdentitiesStorage inst() {
        return instance;
    }

    @Override
    public String getStorageFilename() {
        return STORAGE_FILENAME;
    }

    @Override
    public boolean initStorage() {
        final String databaseFilePath = buildStoragePath(getStorageFilename()); // path to the database file
        final long pagePoolSize = getPagePoolSize(Settings.PERST_PAGEPOOLSIZE_IDENTITIES);

        open(databaseFilePath, pagePoolSize, true, true, false);

        storageRoot = (IdentitiesStorageRoot)getStorage().getRoot();
        if (storageRoot == null) {
            // Storage was not initialized yet
            storageRoot = new IdentitiesStorageRoot(getStorage());
            getStorage().setRoot(storageRoot);
            commit(); // commit transaction
        }
        return true;
    }

    public void exitSave() throws StorageException {
        close();
        storageRoot = null;
        logger.info("IdentitiesStorage closed.");
    }

    public void importLocalIdentities(final List<LocalIdentity> lids, final Hashtable<String,Integer> msgCounts) {
        if( !beginExclusiveThreadTransaction() ) {
            return;
        }
        try {
            for( final LocalIdentity li : lids ) {
                final Integer i = msgCounts.get(li.getUniqueName());
                if( i != null ) {
                    li.setReceivedMessageCount(i.intValue());
                }
                li.correctUniqueName();
                storageRoot.getLocalIdentities().add(li);
            }
        } finally {
            endThreadTransaction();
        }
    }

    public void importIdentities(final List<Identity> ids, final Hashtable<String,Integer> msgCounts) {
        if( !beginExclusiveThreadTransaction() ) {
            return;
        }
        try {
            int cnt = 0;
            for( final Identity li : ids ) {
                final Integer i = msgCounts.get(li.getUniqueName());
                if( i != null ) {
                    li.setReceivedMessageCount(i.intValue());
                }
                li.correctUniqueName();
                storageRoot.getIdentities().add(li);
                cnt++;
                if( cnt % 100 == 0 ) {
                    logger.debug("Committing after {} identities", cnt);
                    endThreadTransaction();
                    beginExclusiveThreadTransaction();
                }
            }
        } finally {
            endThreadTransaction();
        }
    }

    public Hashtable<String,Identity> loadIdentities() {
        final Hashtable<String,Identity> result = new Hashtable<String,Identity>();

        final boolean migrateIdStorage;
        if( storageRoot.getMigrationLevel() < IdentitiesStorageRoot.MIGRATION_LEVEL_1 ) {
            migrateIdStorage = true;
            // read and maybe remove ids
            beginExclusiveThreadTransaction();
        } else {
            migrateIdStorage = false;
            // only read ids
            beginCooperativeThreadTransaction();
        }

        try {
            for( final Iterator<Identity> i = storageRoot.getIdentities().iterator(); i.hasNext();  ) {
                final Identity id = i.next();
                if( id == null ) {
                    logger.error("Retrieved a null id !!! Please repair identities.dbs.");
                } else {
                    // one-time migration, remove all ids that have a '_' instead of an '@'
                    if( migrateIdStorage && !Core.getIdentitiesManager().isIdentityValid(id) ) {
                        i.remove();
                        logger.error("Dropped an invalid identity: {}", id.getUniqueName());
                    } else {
                        result.put(id.getUniqueName(), id);
                    }
                }
            }
        } finally {
            if( migrateIdStorage ) {
                // migration finished
                storageRoot.setMigrationLevel(IdentitiesStorageRoot.MIGRATION_LEVEL_1);
            }
            endThreadTransaction();
        }
        return result;
    }

    public boolean insertIdentity(final Identity id) {
        if( id == null ) {
            logger.error("Rejecting to insert a null id!");
            return false;
        }
        storageRoot.getIdentities().add( id );
        return true;
    }

    public boolean removeIdentity(final Identity id) {
        if( id.getStorage() == null ) {
            logger.error("id not in store");
            return false;
        }
        final boolean isRemoved = storageRoot.getIdentities().remove(id);
        if( isRemoved ) {
            id.deallocate();
        }
        return isRemoved;
    }

    public int getIdentityCount() {
        return storageRoot.getIdentities().size();
    }

    public Hashtable<String,LocalIdentity> loadLocalIdentities() {
        final Hashtable<String,LocalIdentity> result = new Hashtable<String,LocalIdentity>();
        beginCooperativeThreadTransaction();
        try {
            for( final LocalIdentity id : storageRoot.getLocalIdentities() ) {
                if( id == null ) {
                    logger.error("Retrieved a null id !!! Please repair identities.dbs.");
                } else {
                    result.put(id.getUniqueName(), id);
                }
            }
        } finally {
            endThreadTransaction();
        }
        return result;
    }

    public boolean insertLocalIdentity(final LocalIdentity id) {
        if( id == null ) {
            logger.error("Rejecting to insert a null id!");
            return false;
        }
        storageRoot.getLocalIdentities().add(id);
        return true;
    }

    public boolean removeLocalIdentity(final LocalIdentity lid) {
        if( lid.getStorage() == null ) {
            logger.error("lid not in store");
            return false;
        }
        final boolean isRemoved = storageRoot.getLocalIdentities().remove(lid);
        if( isRemoved ) {
            lid.deallocate();
        }
        return isRemoved;
    }

    public static class IdentityMsgAndFileCount {
        final int fileCount;
        final int messageCount;
        public IdentityMsgAndFileCount(final int mc, final int fc) {
            messageCount = mc;
            fileCount = fc;
        }
        public int getFileCount() {
            return fileCount;
        }
        public int getMessageCount() {
            return messageCount;
        }
    }

    /**
     * Retrieve msgCount and fileCount for each identity.
     */
    public Hashtable<String,IdentityMsgAndFileCount> retrieveMsgAndFileCountPerIdentity() {

        final Hashtable<String,IdentityMsgAndFileCount> data = new Hashtable<String,IdentityMsgAndFileCount>();

        for( final Identity id : Core.getIdentitiesManager().getIdentities() ) {
        	final String uniqueName = id.getUniqueName();
            final int messageCount = MessageStorage.inst().getMessageCount(uniqueName);
            final int fileCount = FileListStorage.inst().getFileCount(uniqueName);
            final IdentityMsgAndFileCount identityMsgAndFileCount = new IdentityMsgAndFileCount(messageCount, fileCount);
            data.put(uniqueName, identityMsgAndFileCount);
        }

        for( final LocalIdentity id : Core.getIdentitiesManager().getLocalIdentities() ) {
        	final String uniqueName = id.getUniqueName();
            final int messageCount = MessageStorage.inst().getMessageCount(uniqueName);
            final int fileCount = FileListStorage.inst().getFileCount(uniqueName);
            final IdentityMsgAndFileCount identityMsgAndFilecount = new IdentityMsgAndFileCount(messageCount, fileCount);
            data.put(uniqueName, identityMsgAndFilecount);
        }
        return data;
    }

    public void repairStorage() {

        logger.info("Repairing identities.dbs (may take some time!)...");

        final String databaseFilePath = buildStoragePath("identities.dbs"); // path to the database file
        final long pagePoolSize = 2L*1024L*1024L;

        open(databaseFilePath, pagePoolSize, true, true, false);

        storageRoot = (IdentitiesStorageRoot)getStorage().getRoot();
        if (storageRoot == null) {
            // Storage was not initialized yet
            logger.warn("No identities.dbs found");
            return;
        }

        int brokenEntries = 0;
        int validEntries = 0;

        final List<Identity> lst = new ArrayList<Identity>();
        IPersistentList<Identity> identities = storageRoot.getIdentities();
        int numIdentities = identities.size();

        final int progressSteps = numIdentities / 75; // all 'progressSteps' entries print one dot
        int progress = progressSteps;

        for( int x=0; x < numIdentities; x++ ) {
            if( x > progress ) {
                progress += progressSteps;
            }
            Identity sfk;
            try {
                sfk = identities.get(x);
            } catch(final Throwable t) {
                brokenEntries++;
                continue;
            }
            if( sfk == null ) {
                brokenEntries++;
                continue;
            }
            validEntries++;
            lst.add(sfk);
        }

        identities.clear();
        commit();

        for( final Identity sfk : lst ) {
        	identities.add(sfk);
        }
        commit();

        close();
        storageRoot = null;

        logger.info("Repair finished, brokenEntries = {}; validEntries = {}", brokenEntries, validEntries);
    }
}
