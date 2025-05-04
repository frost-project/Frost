/*
  TreeTableHeader.java / Frost
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
package frost.messaging.freetalk.gui.messagetreetable;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import frost.util.gui.MiscToolkit;

public class FreetalkMessageTreeTableHeader extends JTableHeader {

	private static final long serialVersionUID = 1L;

    private final FreetalkMessageTreeTable messageTreeTable;

    private static Icon ascendingIcon = MiscToolkit.loadImageIcon("/data/SortedTable_ascending.png");
    private static Icon descendingIcon = MiscToolkit.loadImageIcon("/data/SortedTable_descending.png");

	private transient ArrowRenderer arrowRenderer = new ArrowRenderer();

    public FreetalkMessageTreeTableHeader(final FreetalkMessageTreeTable t) {
        super(t.getColumnModel());

        messageTreeTable = t;

        addMouseListener(new Listener());

        // The defaultRenderer of the JTableHeader is not touched because that is what
        // the skins system will change (if is is enabled).
        final Enumeration<TableColumn> enumeration = messageTreeTable.getColumnModel().getColumns();
        while (enumeration.hasMoreElements()) {
            final TableColumn column = enumeration.nextElement();
            column.setHeaderRenderer(arrowRenderer);
        }
    }

    protected void headerClicked(final MouseEvent e) {

        if (e.getButton() == MouseEvent.BUTTON1) {

            if( FreetalkMessageTreeTableSortStateBean.isThreaded() ) {
                // ignore clicks if threads are shown, no sorting allowed
                return;
            }

            final TableColumnModel lColumnModel = getTable().getColumnModel();
            final int columnNumber = lColumnModel.getColumnIndexAtX(e.getX());
            if (columnNumber != -1) {
                //This translation is done so the real column number is used when the user moves columns around.
                final int modelIndex = lColumnModel.getColumn(columnNumber).getModelIndex();

                if( FreetalkMessageTreeTableSortStateBean.getSortedColumn() == modelIndex ) {
                    // toggle ascending
                    FreetalkMessageTreeTableSortStateBean.setAscending( !FreetalkMessageTreeTableSortStateBean.isAscending() );
                } else {
                    FreetalkMessageTreeTableSortStateBean.setSortedColumn(modelIndex);
                }

                messageTreeTable.resortTable();
            }
        }
    }

    /**
     * This inner class listens for mouse clicks on the header and
     * for selections in the popup menu
     */
    private class Listener extends MouseAdapter {
        public Listener() {
            super();
        }
        @Override
        public void mouseClicked(final MouseEvent e) {
            headerClicked(e);
        }
        @Override
        public void mouseReleased(final MouseEvent e) {
        }
    }

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
                if( FreetalkMessageTreeTableSortStateBean.isThreaded() ) {
                    labelRenderer.setIcon(null);
                } else if (FreetalkMessageTreeTableSortStateBean.getSortedColumn() == modelIndex) {
                    if (FreetalkMessageTreeTableSortStateBean.isAscending()) {
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
}
