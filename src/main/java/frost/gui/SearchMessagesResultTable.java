/*
  SearchMessagesResultTable.java / Frost
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
package frost.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import frost.Core;
import frost.Settings;
import frost.fileTransfer.common.TableBackgroundColors;
import frost.gui.model.SearchMessagesTableModel;
import frost.messaging.frost.FrostSearchResultMessageObject;
import frost.util.gui.IconTableHeaderRenderer;
import frost.util.gui.MiscToolkit;

public class SearchMessagesResultTable extends SortedTable<FrostSearchResultMessageObject> {

	private static final long serialVersionUID = 1L;

    private final CellRenderer cellRenderer = new CellRenderer();
    private final BooleanCellRenderer booleanCellRenderer = new BooleanCellRenderer();

    private final ImageIcon flaggedIcon = MiscToolkit.loadImageIcon("/data/flagged.gif");
    private final ImageIcon starredIcon = MiscToolkit.loadImageIcon("/data/starred.gif");

    private final ImageIcon messageDummyIcon = MiscToolkit.loadImageIcon("/data/messagedummyicon.gif");
    private final ImageIcon messageNewIcon = MiscToolkit.loadImageIcon("/data/messagenewicon.gif");
    private final ImageIcon messageReadIcon = MiscToolkit.loadImageIcon("/data/messagereadicon.gif");
    private final ImageIcon messageNewRepliedIcon = MiscToolkit.loadImageIcon("/data/messagenewrepliedicon.gif");
    private final ImageIcon messageReadRepliedIcon = MiscToolkit.loadImageIcon("/data/messagereadrepliedicon.gif");

    private final boolean showColoredLines;

    public SearchMessagesResultTable(final SearchMessagesTableModel m) {
        super(m);

        setDefaultRenderer(String.class, cellRenderer);
        setDefaultRenderer(Boolean.class, booleanCellRenderer);

        // default for messages: sort by date descending
        sortedColumnIndex = 5;
        sortedColumnAscending = false;
        resortTable();

        initLayout();

        showColoredLines = Core.frostSettings.getBoolean(Settings.SHOW_COLORED_ROWS);
    }

    private void initLayout() {
        final TableColumnModel tcm = getColumnModel();

        // hard set sizes of icons column
        tcm.getColumn(0).setMinWidth(20);
        tcm.getColumn(0).setMaxWidth(20);
        tcm.getColumn(0).setPreferredWidth(20);
        // hard set sizes of icons column
        tcm.getColumn(1).setMinWidth(20);
        tcm.getColumn(1).setMaxWidth(20);
        tcm.getColumn(1).setPreferredWidth(20);

        // set icon table header renderer for icon columns
        tcm.getColumn(0).setHeaderRenderer(new IconTableHeaderRenderer(flaggedIcon));
        tcm.getColumn(1).setHeaderRenderer(new IconTableHeaderRenderer(starredIcon));
    }

    /**
     * Save the current column positions and column sizes for restore on next startup.
     *
     * @param frostSettings
     */
//    public void saveLayout(SettingsClass frostSettings) {
//        TableColumnModel tcm = getColumnModel();
//        for(int columnIndexInTable=0; columnIndexInTable < tcm.getColumnCount(); columnIndexInTable++) {
//            TableColumn tc = tcm.getColumn(columnIndexInTable);
//            int columnIndexInModel = tc.getModelIndex();
//            // save the current index in table for column with the fix index in model
//            frostSettings.setValue(SettingsClass.SEARCH_MESSAGE_TABLE_TABLE_INDEX_MODEL_COLUMN_PREFIX + columnIndexInModel,columnIndexInTable);
//            // save the current width of the column
//            int columnWidth = tc.getWidth();
//            frostSettings.setValue(SettingsClass.SEARCH_MESSAGE_TABLE_COLUMN_WIDTH_MODEL_COLUMN_PREFIX + columnIndexInModel, columnWidth);
//        }
//    }

    /**
     * Load the saved column positions and column sizes.
     *
     * @param frostSettings
     */
//    public void loadLayout(SettingsClass frostSettings) {
//        TableColumnModel tcm = getColumnModel();
//
//        // load the saved tableindex for each column in model, and its saved width
//        int[] tableToModelIndex = new int[tcm.getColumnCount()];
//        int[] columnWidths = new int[tcm.getColumnCount()];
//
//        for(int x=0; x < tableToModelIndex.length; x++) {
//            String indexKey = SEARCH_MESSAGE_TABLE_TABLE_INDEX_MODEL_COLUMN_PREFIX + x;
//            if( frostSettings.getObjectValue(indexKey) == null ) {
//                return; // column not found, abort
//            }
//            // build array of table to model associations
//            int tableIndex = frostSettings.getIntValue(indexKey);
//            if( tableIndex < 0 || tableIndex >= tableToModelIndex.length ) {
//                return; // invalid table index value
//            }
//            tableToModelIndex[tableIndex] = x;
//
//            String widthKey = frostSettings.getInteger(SEARCH_MESSAGE_TABLE_COLUMN_WIDTH_MODEL_COLUMN_PREFIX + x;
//            if( frostSettings.getObjectValue(widthKey) == null ) {
//                return; // column not found, abort
//            }
//            // build array of table to model associations
//            int columnWidth = frostSettings.getIntValue(widthKey);
//            if( columnWidth <= 0 ) {
//                return; // invalid column width
//            }
//            columnWidths[x] = columnWidth;
//        }
//        // columns are currently added in model order, remove them all and save in an array
//        // while on it, set the loaded width of each column
//        TableColumn[] tcms = new TableColumn[tcm.getColumnCount()];
//        for(int x=tcms.length-1; x >= 0; x--) {
//            tcms[x] = tcm.getColumn(x);
//            tcm.removeColumn(tcms[x]);
//
//            tcms[x].setPreferredWidth(columnWidths[x]);
//        }
//        // add the columns in order loaded from settings
//        for(int x=0; x < tableToModelIndex.length; x++) {
//            tcm.addColumn(tcms[tableToModelIndex[x]]);
//        }
//    }

    /**
	 * This renderer renders rows in different colors. New messages gets a bold
	 * look, messages with attachments a blue color. Encrypted messages get a red
	 * color, no matter if they have attachments.
     */
    private class CellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

        private Font boldFont = null;
        private Font normalFont = null;
        private boolean isDeleted = false;
        private final Color col_good    = new Color(0x00, 0x80, 0x00);
        private final Color col_check   = new Color(0xFF, 0xCC, 0x00);
        private final Color col_observe = new Color(0x00, 0xD0, 0x00);
        private final Color col_bad     = new Color(0xFF, 0x00, 0x00);

        public CellRenderer() {
            final Font baseFont = SearchMessagesResultTable.this.getFont();
            normalFont = baseFont.deriveFont(Font.PLAIN);
            boldFont = baseFont.deriveFont(Font.BOLD);
        }

        @Override
        public void paintComponent (final Graphics g) {
            super.paintComponent(g);
            if(isDeleted) {
                final Dimension size = getSize();
                g.drawLine(0, size.height / 2, size.width, size.height / 2);
            }
        }

        @Override
        public Component getTableCellRendererComponent(
            final JTable table,
            final Object value,
            boolean isSelected,
            final boolean hasFocus,
            final int row,
            int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            final SearchMessagesTableModel model = (SearchMessagesTableModel) getModel();
			final FrostSearchResultMessageObject msg = model.getRow(row);

            // get the original model column index (maybe columns were reordered by user)
            final TableColumn tableColumn = getColumnModel().getColumn(column);
            column = tableColumn.getModelIndex();

            setIcon(null);
            setToolTipText(null);

            if (!isSelected) {
                final Color newBackground = TableBackgroundColors.getBackgroundColor(table, row, showColoredLines);
                setBackground(newBackground);
            }

            // do nice things for FROM and SIG column and BOARD column
            if( column == 3 ) {
                // FROM
                // first set font, bold for new msg or normal
                if (msg.getMessageObject().isNew()) {
                    setFont(boldFont);
                } else {
                    setFont(normalFont);
                }
                // now set color
                if (!isSelected) {
                    if( msg.getMessageObject().getRecipientName() != null && msg.getMessageObject().getRecipientName().length() > 0) {
                        setForeground(Color.RED);
                    } else if (msg.getMessageObject().containsAttachments()) {
                        setForeground(Color.BLUE);
                    } else {
                        setForeground(Color.BLACK);
                    }
                }
                setToolTipText(msg.getMessageObject().getFromName());

//            } else if( column == 2 ) {
//                // BOARD - gray for archived msgs
//                setFont(normalFont);
//                if (!isSelected) {
//                    if( msg.getMessageObject().isMessageArchived() ) {
//                        setForeground(Color.GRAY);
//                    } else {
//                        setForeground(Color.BLACK);
//                    }
//                }
            } else if( column == 5 ) {
                ImageIcon icon;
                if( msg.getMessageObject().isDummy() ) {
                    icon = messageDummyIcon;
                } else if( msg.getMessageObject().isNew() ) {
                    if( msg.getMessageObject().isReplied() ) {
                        icon = messageNewRepliedIcon;
                    } else {
                        icon = messageNewIcon;
                    }
                } else {
                    if( msg.getMessageObject().isReplied() ) {
                        icon = messageReadRepliedIcon;
                    } else {
                        icon = messageReadIcon;
                    }
                }
                setIcon(icon);
                setToolTipText(msg.getMessageObject().getSubject());
            } else if( column == 6 ) {
                // SIG
                // state == good/bad/check/observe -> bold and coloured
                if( msg.getMessageObject().isMessageStatusGOOD() ) {
                    setFont(boldFont);
                    setForeground(col_good);
                } else if( msg.getMessageObject().isMessageStatusCHECK() ) {
                    setFont(boldFont);
                    setForeground(col_check);
                } else if( msg.getMessageObject().isMessageStatusOBSERVE() ) {
                    setFont(boldFont);
                    setForeground(col_observe);
                } else if( msg.getMessageObject().isMessageStatusBAD() ) {
                    setFont(boldFont);
                    setForeground(col_bad);
                } else if( msg.getMessageObject().isMessageStatusTAMPERED() ) {
                    setFont(boldFont);
                    setForeground(col_bad);
                } else {
                    setFont(normalFont);
                    if (!isSelected) {
                        setForeground(Color.BLACK);
                    }
                }
            } else {
                setFont(normalFont);
                if (!isSelected) {
                    setForeground(Color.BLACK);
                }
            }

            setDeleted(msg.getMessageObject().isDeleted());

            return this;
        }

        @Override
        public void setFont(final Font font) {
            super.setFont(font);
            normalFont = font.deriveFont(Font.PLAIN);
            boldFont = font.deriveFont(Font.BOLD);
        }

        public void setDeleted(final boolean value) {
            isDeleted = value;
        }
    }

    private class BooleanCellRenderer extends JLabel implements TableCellRenderer {

		private static final long serialVersionUID = 1L;

        public BooleanCellRenderer() {
            super();
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
        }

        @Override
        public void paintComponent (final Graphics g) {
            final Dimension size = getSize();
            g.setColor(getBackground());
            g.fillRect(0, 0, size.width, size.height);
            super.paintComponent(g);
        }

        public Component getTableCellRendererComponent(
                final JTable table,
                final Object value,
                boolean isSelected,
                final boolean hasFocus,
                final int row,
                int column)
        {
            final boolean val = ((Boolean)value).booleanValue();

            // get the original model column index (maybe columns were reordered by user)
            final TableColumn tableColumn = getColumnModel().getColumn(column);
            column = tableColumn.getModelIndex();

            if( column == 0 ) {
                if( val ) {
                    setIcon(flaggedIcon);
                } else {
                    setIcon(null);
                }
            } else if( column == 1 ) {
                if( val ) {
                    setIcon(starredIcon);
                } else {
                    setIcon(null);
                }
            }

            if (!isSelected) {
                final Color newBackground = TableBackgroundColors.getBackgroundColor(table, row, showColoredLines);
                setBackground(newBackground);
            } else {
                setBackground(table.getSelectionBackground());
            }
            return this;
        }
    }

    @Override
    public void createDefaultColumnsFromModel() {
        super.createDefaultColumnsFromModel();

        // set column sizes
        final int[] widths = { 20, 20, 30, 125, 80, 250, 75, 150 };
        for (int i = 0; i < widths.length; i++) {
            getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    @Override
    public void setFont(final Font font) {
        super.setFont(font);
        if (cellRenderer != null) {
            cellRenderer.setFont(font);
        }
        setRowHeight(font.getSize() + 5);
    }
}
