/*
 StorageManager.java / Frost
 Copyright (C) 2003  Frost Project <jtcfrost.sourceforge.net>

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
package frost.storage;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Frost;
import frost.Settings;

/**
 * @author $Author: bback $
 * @version $Revision: 3112 $
 */
public class StorageManager extends Timer {

	private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    private final ShutdownThread shutdownThread = new ShutdownThread();
    private final AutoTask autoTask = new AutoTask();

    private final Vector<AutoSavable> autoSavables = new Vector<AutoSavable>();
    private final Vector<ExitSavable> exitSavables = new Vector<ExitSavable>();

	private class AutoTask extends TimerTask {
		public AutoTask() {
			super();
		}
		@Override
        public void run() {
			if (autoSavables != null) {
			    for( final AutoSavable savable : autoSavables ) {
					try {
						savable.autoSave();
					} catch (final StorageException se) {
						logger.error("Error while saving a resource inside the timer.", se);
						if (Core.getInstance() != null) {
						    Core.getInstance().showAutoSaveError(se);
						}
					}
				}
                // autosave consumes some memory, clean up now
                System.gc();
			}
		}
	}

    private class ShutdownThread extends Thread {
		public ShutdownThread() {
            super();
		}
		/**
		 * Called by shutdown hook.
		 */
		@Override
        public void run() {
			logger.info("Saving ...");
			if (exitSavables != null) {
                for( final ExitSavable savable : exitSavables ) {
					try {
						savable.exitSave();
					} catch (final Throwable se) {
						logger.error("Error while saving a resource inside the shutdown hook.", se);
					}
				}
			}

			logger.info("Frost shutdown completed.");

            Frost.releaseLockFile();
		}
	}

	public StorageManager(final Settings frostSettings) {
		Runtime.getRuntime().addShutdownHook(shutdownThread);
		final int autoSaveIntervalMinutes = frostSettings.getIntValue(Settings.AUTO_SAVE_INTERVAL);
		schedule(
			autoTask,
			autoSaveIntervalMinutes * 60L * 1000L,
			autoSaveIntervalMinutes * 60L * 1000L);
	}

	/**
	 * Adds a Savable to the autoSavables list.
	 * If autoSavable is null, no exception is thrown and no action is performed.
	 *
	 * @param    autoSavable  the Savable to be added
	 *
	 * @see #removeAutoSavable
	 */
	public synchronized void addAutoSavable(final AutoSavable autoSavable) {
		if (autoSavable == null) {
			return;
		}
		autoSavables.addElement(autoSavable);
	}

	/**
	 * Adds a Savable to the exitSavables list.
	 * If exitSavable is null, no exception is thrown and no action is performed.
	 *
	 * @param    exitSavable  the Savable to be added
	 *
	 * @see #removeExitSavable
	 */
	public synchronized void addExitSavable(final ExitSavable exitSavable) {
		if (exitSavable == null) {
			return;
		}
		exitSavables.addElement(exitSavable);
	}
}
