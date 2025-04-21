/*
 AntialiasedTextPane.java / Frost
 Copyright (C) 2006  Frost Project <jtcfrost.sourceforge.net>

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
package frost.util.gui.textpane;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Extend {@link TextPane} class with antialiasing render
 * Original code of Frost software
 * @author ET
 */
public class AntialiasedTextPane extends TextPane {

	private static final long serialVersionUID = 1L;

	private boolean antiAliasEnabled = true;

	/**
	 * {@inheritDoc}
	 */
	public AntialiasedTextPane() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public AntialiasedTextPane(Decoder decoder) {
		super(decoder);
	}

	/**
	 * {@inheritDoc}
	 */
	public AntialiasedTextPane(String message, Decoder decoder) {
		super(message, decoder);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public AntialiasedTextPane(String message) {
		super(message);
	}

	/**
	 * @return True if antialiasing is enable
	 */
	public boolean isAntiAliasEnabled() {
		return antiAliasEnabled;
	}

	/**
	 * Set antialiasing
	 * @param value
	 */
	public void setAntiAliasEnabled(boolean value) {
		antiAliasEnabled = value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void paint(Graphics g) {
		if (antiAliasEnabled) {
			Graphics2D graphics2D = (Graphics2D) g;
			graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		super.paint(g);
	}

}
