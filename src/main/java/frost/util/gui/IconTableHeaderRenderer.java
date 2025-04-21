/*
 IconTableHeaderRenderer.java / Frost
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
package frost.util.gui;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

/**
 * Renderer to use an icon in table header.
 */
public class IconTableHeaderRenderer extends JLabel implements TableCellRenderer {

	private static final long serialVersionUID = 1L;

	public IconTableHeaderRenderer(final ImageIcon i) {
        setIcon(i);
        setText("");
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    public IconTableHeaderRenderer(final ImageIcon i, final String tooltipText) {
        setIcon(i);
        setText("");
        setToolTipText(tooltipText);
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    public Component getTableCellRendererComponent(
            final JTable table,
            final Object value,
            final boolean isSelected,
            final boolean hasFocus,
            final int row,
            final int column)
    {
        // Extract the original header renderer for this column.
        final TableCellRenderer tcr = table.getTableHeader().getDefaultRenderer();
        // Extract the component used to render the column header.
        final Component c = tcr.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Establish the font, foreground color, and border for the
        // JLabel so that the rendered header will look the same as the
        // other rendered headers.
        setFont(c.getFont());
        setForeground(c.getForeground());
        setBorder(((JComponent)c).getBorder());

        return this;
    }
}