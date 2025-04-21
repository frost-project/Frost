/*
 AntiAliasedTextArea.java / Frost
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
package frost.util.gui.textpane;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JTextArea;
import javax.swing.text.Document;

/**
 * @author Administrator
 *
 *	This a subclass of JTextArea that lets the programmer specify whether to use 
 *	antialising when rendering the text or not.
 *
 */

//TODO: Bug: if the text contains arabic characters, antialias will be disabled

public class AntialiasedTextArea extends JTextArea {

	private static final long serialVersionUID = 1L;

	private boolean antiAliasEnabled = false;

	/**
	 * 
	 */
	public AntialiasedTextArea() {
		super();
	}

	/**
	 * @param rows
	 * @param columns
	 */
	public AntialiasedTextArea(int rows, int columns) {
		super(rows, columns);
	}

	/**
	 * @param text
	 */
	public AntialiasedTextArea(String text) {
		super(text);
	}

	/**
	 * @param text
	 * @param rows
	 * @param columns
	 */
	public AntialiasedTextArea(String text, int rows, int columns) {
		super(text, rows, columns);
	}

	/**
	 * @param doc
	 */
	public AntialiasedTextArea(Document doc) {
		super(doc);
	}

	/**
	 * @param doc
	 * @param text
	 * @param rows
	 * @param columns
	 */
	public AntialiasedTextArea(Document doc, String text, int rows, int columns) {
		super(doc, text, rows, columns);
	}

	/**
	 * @return
	 */
	public boolean isAntiAliasEnabled() {
		return antiAliasEnabled;
	}

	/**
	 * @param b
	 */
	public void setAntiAliasEnabled(boolean b) {
		antiAliasEnabled = b;
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#paint(java.awt.Graphics)
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
