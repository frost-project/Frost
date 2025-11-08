/*
  SearchParametersTest.java / Frost
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
package frost.fileTransfer.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import frost.junit.InitExtension;

@ExtendWith(InitExtension.class)
public class SearchParametersTest {

	@Test
	public void getNameAndGetNotName() {
		SearchParameters s = new SearchParameters(true);
		s.setNameString("hello not world \"und so weiter\" aber NOT dieses hier \"und so\"");

		assertEquals("[hello, not, world, und so weiter, aber]", s.getName().toString());
		assertEquals("[dieses, hier, und so]", s.getNotName().toString());
	}
}
