/*
 Startup.java / Frost
 Copyright (C) 2001  Frost Project <jtcfrost.sourceforge.net>

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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does some things that have to be done when starting Frost.
 */
public class Startup {

	private static final Logger logger = LoggerFactory.getLogger(Startup.class);

	/**
	 * The Main method, check if allowed to run and starts the other startup work.
	 */
	public static void startupCheck(final Settings settings) {
		checkDirectories(settings);
		copyFiles();
		cleanTempDir(settings);

		ArrayList<String> obsoleteFiles = new ArrayList<>();
		obsoleteFiles.add("exec/JSysTray.dll");
		obsoleteFiles.add("exec");
		obsoleteFiles.add("frost.sh");
		obsoleteFiles.add("frost-debug.bat");
		obsoleteFiles.add("frost-debug.sh");
		obsoleteFiles.add("frost.jar");
		obsoleteFiles.add("frost0.log");
		obsoleteFiles.add("frost1.log");
		obsoleteFiles.add("help/help.zip");

		for (String filename : obsoleteFiles) {
			new File(filename).delete();
		}

		ArrayList<String> questionableFiles = new ArrayList<>();
		questionableFiles.add("frost.bat");
		try {
			for (String filename : questionableFiles) {
				if (new File(filename).exists()) {
					String content = Files.readString(Path.of(filename), StandardCharsets.UTF_8);
					if (!content.contains("FROST_OPTS")) {
						new File(filename).delete();
					}
				}
			}
		} catch (IOException e) {
			logger.error("IO-Error!", e);
		}
	}

    // Copy some files from the jar file, if they don't exist
    private static void copyFiles() {
//        final String fileSeparator = System.getProperty("file.separator");
//
//        try {
//            boolean copyResource = false;
//            final File systrayDllFile = new File("exec" + fileSeparator + "JSysTray.dll");
//            if( !systrayDllFile.isFile() ) {
//                copyResource = true;
//            } else {
//                // check if size of existing dll file is different. If yes extract new version from jar.
//                final URL url = MainFrame.class.getResource("/data/JSysTray.dll");
//                final URLConnection urlConn = url.openConnection();
//                final long len = urlConn.getContentLength();
//                if( len != systrayDllFile.length() ) {
//                    systrayDllFile.delete();
//                    copyResource = true;
//                }
//            }
//            if( copyResource ) {
//                FileAccess.copyFromResource("/data/JSysTray.dll", systrayDllFile);
//            }
//        } catch (final Throwable e) {
//            e.printStackTrace();
//        }
    }

    private static void checkDirectories(final Settings settings) {
        final File downloadDirectory = new File(settings.getValue(Settings.DIR_DOWNLOAD));
        if( !downloadDirectory.isDirectory() ) {
            logger.info("Creating download directory");
            downloadDirectory.mkdirs();
        }

//        final File execDirectory = new File("exec");
//        if( !execDirectory.isDirectory() ) {
//            logger.info("Creating exec directory");
//            execDirectory.mkdirs();
//        }

        final File tempDirectory = new File(settings.getValue(Settings.DIR_TEMP));
        if( !tempDirectory.isDirectory() ) {
            logger.info("Creating temp directory");
            tempDirectory.mkdirs();
        }

        final File storeDirectory = new File(settings.getValue(Settings.DIR_STORE));
        if( !storeDirectory.isDirectory() ) {
            logger.info("Creating store directory");
            storeDirectory.mkdirs();
        }
    }

    private static void cleanTempDir(final Settings settings) {
        final File[] entries = new File(settings.getValue(Settings.DIR_TEMP)).listFiles();
        for( final File entry : entries ) {
            if( entry.isDirectory() == false ) {
                entry.delete();
            }
        }
    }
}
