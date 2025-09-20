/*
 SortedTableHeader.java / Frost
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
package frost.util.model;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import frost.util.gui.MiscToolkit;

/**
 * This is subclass of ModelTableHeader that listens for mouse clicks on it.
 * When the user clicks on the header of one column, it notifies the associated
 * SortedModelTable of the event, so that it can change its sorting.
 * It also paints arrows on the header depending of that sorting.
 */
public class SortedTableHeader extends ModelTableHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * This inner class paints an arrow on the header of the column the model
	 * table is sorted by. The arrow will point upwards or downwards depending
	 * if the sorting is ascending or descending.
	 */
	private class ArrowRenderer implements TableCellRenderer {

		/**
		 * This constructor creates a new instance of ArrowRenderer
		 */
		public ArrowRenderer() {
			super();
		}

		/**
		 * This method assumes that this is the renderer of the header of a column.
		 * If the defaultRenderer of the JTableHeader is an instance of JLabel
		 * (like DefaultTableCellRenderer), it paints an arrow if necessary. Then,
		 * it calls the defaultRenderer so that it finishes the job.
		 * @see TableCellRenderer#getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)
	 	 */
		public Component getTableCellRendererComponent(final JTable lTable, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
			final TableCellRenderer defaultRenderer = lTable.getTableHeader().getDefaultRenderer();
			if (defaultRenderer instanceof JLabel) {
				final JLabel labelRenderer = (JLabel)defaultRenderer;
				// This translation is done so the real column number is used when the user moves columns around.
				final int modelIndex = lTable.getColumnModel().getColumn(column).getModelIndex();
				if (sortedTable.getSortedColumn() == modelIndex) {
					if (sortedTable.isSortedAscending()) {
						labelRenderer.setIcon(ascendingIcon);
					} else {
						labelRenderer.setIcon(descendingIcon);
					}
					labelRenderer.setHorizontalTextPosition(SwingConstants.LEADING);
				} else {
					labelRenderer.setIcon(null);
				}
                labelRenderer.setToolTipText(value.toString());
			}
			return defaultRenderer.getTableCellRendererComponent(lTable, value, isSelected, hasFocus, row, column);
		}

	}

	private static Icon ascendingIcon = MiscToolkit.loadImageIcon("/data/SortedTable_ascending.png");
	private static Icon descendingIcon = MiscToolkit.loadImageIcon("/data/SortedTable_descending.png");

	private transient final ArrowRenderer arrowRenderer = new ArrowRenderer();

	private final SortedModelTable<? extends ModelItem<?>> sortedTable;

	/**
	 * This constructor creates a new instance of ModelTableHeader associated
	 * to the SortedModelTable that is passed as a parameter.
	 * @param cm the SortedModelTable that is going to have this header
	 */
	public SortedTableHeader(final SortedModelTable<? extends ModelItem<?>> newSortedTable) {
		super(newSortedTable);

		sortedTable = newSortedTable;
		//The defaultRenderer of the JTableHeader is not touched because that is what
		//the skins system will change (if is is enabled).
		final Enumeration<TableColumn> enumeration = sortedTable.getTable().getColumnModel().getColumns();
		while (enumeration.hasMoreElements()) {
			final TableColumn column = enumeration.nextElement();
			column.setHeaderRenderer(arrowRenderer);
		}
	}

	/**
	 * This method is called by the superclass when the user clicks on a column (the mouse
	 * button is pressed). It gets the number of the column whose header was clicked and
	 * notifies the associated SortedModelTable of the event.
	 * @see ModelTableHeader#headerClicked(MouseEvent)
	 */
	@Override
    protected void headerClicked(final MouseEvent e) {
		super.headerClicked(e);
		if (e.getButton() == MouseEvent.BUTTON1) {
			final TableColumnModel lColumnModel = getTable().getColumnModel();
			final int columnNumber = lColumnModel.getColumnIndexAtX(e.getX());
			if (columnNumber != -1) {
				//This translation is done so the real column number is used when the user moves columns around.
				final int modelIndex = lColumnModel.getColumn(columnNumber).getModelIndex();
				sortedTable.columnClicked(modelIndex);
			}
		}
	}
}
