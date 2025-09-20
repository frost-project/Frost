/*
 TextPane.java / Frost
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

import java.awt.image.ImageObserver;

import javax.swing.JTextPane;

public class TextPane extends JTextPane implements ImageObserver {

	private static final long serialVersionUID = 1L;

	private transient Decoder decoder;

	public TextPane() {
		super();
		setEditable(false);
	}

	public TextPane(final Decoder decoder) {
		this();
		this.decoder = decoder;
	}

	public TextPane(final String message, final Decoder decoder) {
		this(decoder);
		setText(message);
	}

	public TextPane(final String message) {
		this();
		setText(message);
	}

	public Decoder getDecoder() {
		return decoder;
	}

	public void setDecoder(final Decoder decoder) {
		this.decoder = decoder;
	}

	@Override
    public void setText(final String message) {
		if (decoder != null) {
			decoder.decode(message, this);
		} else {
			super.setText(message);
		}
	}
}
