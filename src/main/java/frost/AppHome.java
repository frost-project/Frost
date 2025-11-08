/*
  AppHome.java / Frost
  Copyright (C) 2025  Frost Project <jtcfrost.sourceforge.net>

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

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppHome {

	public static final String APP_HOME = "APP_HOME";

	public static void init() {
		try {
			Path jarPath = Paths.get(AppHome.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			Path appHome = null;
			if (Files.isRegularFile(jarPath)) {
				// jarPath = ROOT/lib/APPNAME.jar
				appHome = jarPath.getParent().getParent();
			} else {
				// jarPath = ROOT/build/classes/java/main OR
				// jarPath = ROOT/bin/main
				while (jarPath != null) {
					if (Files.exists(jarPath.resolve("build.gradle"))) {
						appHome = jarPath;
						break;
					}
					jarPath = jarPath.getParent();
				}
			}
			System.setProperty(APP_HOME, appHome.toString());
		} catch (URISyntaxException e) {
			e.printStackTrace(); // Logger is unavailable here
		}
	}
}
