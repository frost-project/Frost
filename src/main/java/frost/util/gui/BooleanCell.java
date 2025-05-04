/*
 BooleanCell.java / Frost
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
package frost.util.gui;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class BooleanCell {

	/**
	 * This inner class implements the default editor of a boolean table cell
	 */
	private static class Editor extends DefaultCellEditor implements TableCellEditor {

		private static final long serialVersionUID = 1L;

		public Editor() {
			super(new JCheckBox());
			JCheckBox checkBox = (JCheckBox) getComponent();
			checkBox.setHorizontalAlignment(SwingConstants.CENTER);
		}
	}
	
	/**
	 * This inner class implements the default renderer of a boolean table cell
	 */
	private static class Renderer extends JCheckBox implements TableCellRenderer {

		private static final long serialVersionUID = 1L;

		public Renderer() {
			super();
			setHorizontalAlignment(SwingConstants.CENTER);	
		}

		public Component getTableCellRendererComponent(
			JTable table,
			Object value,
			boolean isSelected,
			boolean hasFocus,
			int row,
			int column) {

			if (isSelected) {
				setForeground(table.getSelectionForeground());
				super.setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(table.getBackground());
			}
			setSelected((value != null && ((Boolean) value).booleanValue()));
			return this;
		}
	}
	
	public static Renderer RENDERER = new Renderer();
	public static Editor EDITOR = new Editor();
	
	public BooleanCell() {
		super();
	}
}
