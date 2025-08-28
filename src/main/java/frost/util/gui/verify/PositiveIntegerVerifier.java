/*
  PositiveIntegerVerifier.java / Frost
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
package frost.util.gui.verify;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

import frost.util.Convert;

public class PositiveIntegerVerifier extends InputVerifier {

	@Override
	public boolean verify(JComponent inputComponent) {
		Boolean isValid = true;
		if (inputComponent instanceof JTextComponent textComponent) {
			try {
				Integer value = Convert.toInteger(textComponent.getText());
				if (value < 0) {
					isValid = false;
				}
			} catch (NumberFormatException e) {
				isValid = false;
			}
			if (isValid) {
				textComponent.setBackground(VerifierUtil.COLOR_NORMAL);
			} else {
				textComponent.setBackground(VerifierUtil.COLOR_ERROR);
			}
		}
		return isValid;
	}
}