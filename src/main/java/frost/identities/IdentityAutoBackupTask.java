/*
  IdentityAutoBackupTask.java / Frost
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
package frost.identities;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.storage.AutoSavable;
import frost.storage.ExitSavable;
import frost.storage.LocalIdentitiesXmlDAO;
import frost.storage.StorageException;

/**
 * Automatically backups the LocalIdentities to an XML file located in the localdata directory.
 */
public class IdentityAutoBackupTask implements AutoSavable, ExitSavable {

	private static final Logger logger = LoggerFactory.getLogger(IdentityAutoBackupTask.class);

    public void exitSave() throws StorageException {
        backupLocalIdentities();
    }

    public void autoSave() throws StorageException {
        backupLocalIdentities();
    }

    /**
     * Backup local identities, only done if enabled in options!
     */
    private void backupLocalIdentities() {

        if( !Core.frostSettings.getBoolValue(Settings.AUTO_SAVE_LOCAL_IDENTITIES) ) {
            // autosave disabled
            return;
        }

        final String newName = Core.frostSettings.getValue(Settings.DIR_LOCALDATA) + "localIdentitiesBackup.new";
        final String xmlName = Core.frostSettings.getValue(Settings.DIR_LOCALDATA) + "localIdentitiesBackup.xml";
        final String bakName = Core.frostSettings.getValue(Settings.DIR_LOCALDATA) + "localIdentitiesBackup.bak";
        final String oldName = Core.frostSettings.getValue(Settings.DIR_LOCALDATA) + "localIdentitiesBackup.old";
        final File newFile = new File(newName);
        final File xmlFile = new File(xmlName);
        final File bakFile = new File(bakName);
        final File oldFile = new File(oldName);

        final List<LocalIdentity> lIds = Core.getIdentitiesManager().getLocalIdentities();
        final boolean wasOk = LocalIdentitiesXmlDAO.saveLocalIdentities(newFile, lIds);
        if( !wasOk ) {
            logger.error("Failed to backup the local identities!");
            return;
        }

        // rename rename bak to old, xml to bak, new to xml, delete old
        oldFile.delete();

        if( bakFile.isFile() ) {
            bakFile.renameTo(oldFile);
        }
        if( xmlFile.isFile() ) {
            xmlFile.renameTo(bakFile);
        }

        newFile.renameTo(xmlFile);

        oldFile.delete();
    }
}
