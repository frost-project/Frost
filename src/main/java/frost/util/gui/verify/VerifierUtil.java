/*
  VerifierUtil.java / Frost
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

public class VerifierUtil {

	public static Color COLOR_NORMAL = UIManager.getColor("TextField.background");
	public static Color COLOR_ERROR = Color.PINK;

	private static List<JTextComponent> getTextComponentsWithVerifier(Container container) {
		List<JTextComponent> textComponents = new ArrayList<>();
		for (Component component : container.getComponents()) {
			if (component instanceof JTextComponent textComponent && textComponent.getInputVerifier() != null) {
				textComponents.add(textComponent);
			} else if (component instanceof Container child) {
				textComponents.addAll(getTextComponentsWithVerifier(child));
			}
		}
		return textComponents;
	}

	public static Boolean verifyAll(Container container) {
		Boolean isAllValid = true;
		for (JTextComponent textComponent : getTextComponentsWithVerifier(container)) {
			if (!textComponent.getInputVerifier().verify(textComponent)) {
				isAllValid = false;
			}
		}
		return isAllValid;
	}
}