/*
  AppLock.java / Frost
  Copyright (C) 2026  Frost Project <jtcfrost.sourceforge.net>

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
package frost;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppLock {

	private static final Logger logger = LoggerFactory.getLogger(AppLock.class);

	private String filename;
	private RandomAccessFile file;
	private FileLock lock;

	/**
	 * This method checks if the lockfile is present (therefore indicating that
	 * another instance of Frost is running off the same directory). If it is, it
	 * return false. If not, it creates a lockfile and returns true.
	 * 
	 * @return Boolean False if there was a problem while initializing the lockfile.
	 *         True otherwise.
	 */
	public Boolean tryLock() {
		filename = Core.frostSettings.resolveFile("frost.lock");
		try {
			logger.info("Try to create lock file {}", filename);
			file = new RandomAccessFile(filename, "rw");
			lock = file.getChannel().tryLock();
		} catch (IOException e) {
			logger.error("IO-Error!", e);
		}
		return lock != null;
	}

	public void release() {
		try {
			if (lock != null) {
				lock.release();
			}
			if (file != null) {
				file.close();
			}
			if (lock != null && file != null) {
				Files.deleteIfExists(Path.of(filename));
			}
		} catch (IOException e) {
			logger.error("IO-Error!", e);
		}
	}

	public String getFilename() {
		return filename;
	}
}
