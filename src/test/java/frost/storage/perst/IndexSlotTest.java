/*
  IndexSlotTest.java / Frost
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
package frost.storage.perst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import frost.junit.InitExtension;

@ExtendWith(InitExtension.class)
public class IndexSlotTest {

	@Test
	public void slotUsed() {
		IndexSlot gis = new IndexSlot(1, 123L);
		gis.setDownloadSlotUsed(1);
		gis.setDownloadSlotUsed(2);
		gis.setDownloadSlotUsed(4);
		gis.setUploadSlotUsed(3);

		assertEquals(0, gis.findFirstDownloadSlot());
		assertEquals(3, gis.findNextDownloadSlot(0));
		assertEquals(5, gis.findNextDownloadSlot(3));

		assertEquals(5, gis.findFirstUploadSlot());
		assertEquals(6, gis.findNextUploadSlot(5));

		assertFalse(gis.isDownloadIndexBehindLastSetIndex(0));
		assertFalse(gis.isDownloadIndexBehindLastSetIndex(1));
		assertFalse(gis.isDownloadIndexBehindLastSetIndex(2));
		assertFalse(gis.isDownloadIndexBehindLastSetIndex(3));
		assertFalse(gis.isDownloadIndexBehindLastSetIndex(4));
		assertTrue(gis.isDownloadIndexBehindLastSetIndex(5));
	}
}
