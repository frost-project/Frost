/*
  MixedTest.java / Frost
  Copyright (C) 2023  Frost Project <jtcfrost.sourceforge.net>

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
package frost.util;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import frost.junit.InitExtension;

@ExtendWith(InitExtension.class)
public class MixedTest {

	@Test
	public void createUniqueIds() {
		String id1 = Mixed.createUniqueId();
		String id2 = Mixed.createUniqueId();
		String id3 = Mixed.createUniqueId();

		assertNotNull(id1);
		assertNotNull(id2);
		assertNotNull(id3);
		assertNotEquals(id1, id2);
		assertNotEquals(id2, id3);
		assertNotEquals(id1, id3);
	}
}
