/*
  SharedFilesTableFormat.java / Frost
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
package frost.fileTransfer.sharing;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Comparator;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import frost.Core;
import frost.Settings;
import frost.fileTransfer.common.TableBackgroundColors;
import frost.gui.RatingStringProvider;
import frost.util.DateFun;
import frost.util.FormatterUtils;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;
import frost.util.model.ModelTable;
import frost.util.model.SortedModelTable;
import frost.util.model.SortedTableFormat;

class SharedFilesTableFormat extends SortedTableFormat<FrostSharedFileItem> implements LanguageListener, PropertyChangeListener  {

    private static final String CFGKEY_SORTSTATE_SORTEDCOLUMN = "SharedFilesTable.sortState.sortedColumn";
    private static final String CFGKEY_SORTSTATE_SORTEDASCENDING = "SharedFilesTable.sortState.sortedAscending";
    private static final String CFGKEY_COLUMN_TABLEINDEX = "SharedFilesTable.tableindex.modelcolumn.";
    private static final String CFGKEY_COLUMN_WIDTH = "SharedFilesTable.columnwidth.modelcolumn.";

    private Language language;

    private final static int COLUMN_COUNT = 13;

    private String stateNever;
    private String unknown;
    
    private boolean showColoredLines;
    
    SortedModelTable<FrostSharedFileItem> modelTable;

    public SharedFilesTableFormat() {
        super(COLUMN_COUNT);

        language = Language.getInstance();
        language.addLanguageListener(this);
        refreshLanguage();

        setComparator(new NameComparator(), 0);
        setComparator(new FileSizeComparator(), 1);
        setComparator(new OwnerComparator(), 2);
        setComparator(new UploadCountComparator(), 3);
        setComparator(new LastUploadComparator(), 4);
        setComparator(new RequestCountComparator(), 5);
        setComparator(new LastRequestComparator(), 6);
        setComparator(new KeyComparator(), 7);
        setComparator(new RatingComparator(), 8);
        setComparator(new CommentComparator(), 9);
        setComparator(new KeywordsComparator(), 10);
        setComparator(new LastSharedComparator(), 11);
        setComparator(new PathComparator(), 12);
        
        showColoredLines = Core.frostSettings.getBoolValue(Settings.SHOW_COLORED_ROWS);
        Core.frostSettings.addPropertyChangeListener(this);
    }

    private void refreshLanguage() {
        setColumnName(0, language.getString("SharedFilesPane.fileTable.filename"));
        setColumnName(1, language.getString("SharedFilesPane.fileTable.size"));
        setColumnName(2, language.getString("SharedFilesPane.fileTable.owner"));
        setColumnName(3, language.getString("SharedFilesPane.fileTable.uploadCount"));
        setColumnName(4, language.getString("SharedFilesPane.fileTable.lastUpload"));
        setColumnName(5, language.getString("SharedFilesPane.fileTable.requestCount"));
        setColumnName(6, language.getString("SharedFilesPane.fileTable.lastRequest"));
        setColumnName(7, language.getString("SharedFilesPane.fileTable.key"));
        setColumnName(8, language.getString("SharedFilesPane.fileTable.rating"));
        setColumnName(9, language.getString("SharedFilesPane.fileTable.comment"));
        setColumnName(10, language.getString("SharedFilesPane.fileTable.keywords"));
        setColumnName(11, language.getString("SharedFilesPane.fileTable.lastShared"));
        setColumnName(12, language.getString("SharedFilesPane.fileTable.path"));

        stateNever = language.getString("SharedFilesPane.fileTable.state.never");
        unknown =    language.getString("SharedFilesPane.fileTable.state.unknown");

        refreshColumnNames();
    }

    @Override
    public void setCellValue(Object value, FrostSharedFileItem sfItem, int columnIndex) { }

    public Object getCellValue(FrostSharedFileItem sfItem, int columnIndex) {
        if( sfItem == null ) {
            return "*null*";
        }
        switch (columnIndex) {
            case 0 : // name
                return sfItem.getFile().getName();
            case 1 : // size
                return FormatterUtils.formatSize(sfItem.getFileSize());
            case 2 : // owner
                return sfItem.getOwner();
            case 3 : // uploadCount
                return Integer.toString(sfItem.getUploadCount());
            case 4 : // lastUploaded
                if( sfItem.getLastUploaded() == 0 ) {
                    return stateNever;
                } else {
                    return DateFun.getExtendedDateFromMillis(sfItem.getLastUploaded());
                }
            case 5 : // requestCount
                return Integer.toString(sfItem.getRequestsReceived());
            case 6 : // lastRequestDate
                if( sfItem.getRequestLastReceived() == 0 ) {
                    return stateNever;
                } else {
                    return DateFun.getExtendedDateFromMillis(sfItem.getRequestLastReceived());
                }
            case 7 :    // Key
                if (sfItem.getKey() == null) {
                    return unknown;
                } else {
                    return sfItem.getKey();
                }
            case 8 : // rating
                return RatingStringProvider.getRatingString(sfItem.getRating());
            case 9 : // comment
                if( sfItem.getComment() == null ) {
                    return "";
                } else {
                    return sfItem.getComment();
                }
            case 10: // keywords
                if( sfItem.getKeywords() == null ) {
                    return "";
                } else {
                    return sfItem.getKeywords();
                }
            case 11: // refLastSend
                if( sfItem.getRefLastSent() == 0 ) {
                    return stateNever;
                } else {
                    return DateFun.getExtendedDateFromMillis(sfItem.getRefLastSent());
                }
            case 12 : // path
                if( sfItem.isValid() ) {
                    return sfItem.getFile().getPath();
                } else {
                    return "???";
                }
            default:
                return "**ERROR**";
        }
    }

    @Override
    public void customizeTable(ModelTable<FrostSharedFileItem> lModelTable) {
        super.customizeTable(lModelTable);
        
        modelTable = (SortedModelTable<FrostSharedFileItem>) lModelTable;
        
        if( Core.frostSettings.getBoolValue(Settings.SAVE_SORT_STATES)
                && Core.frostSettings.getObjectValue(CFGKEY_SORTSTATE_SORTEDCOLUMN) != null
                && Core.frostSettings.getObjectValue(CFGKEY_SORTSTATE_SORTEDASCENDING) != null )
        {
            int sortedColumn = Core.frostSettings.getIntValue(CFGKEY_SORTSTATE_SORTEDCOLUMN);
            boolean isSortedAsc = Core.frostSettings.getBoolValue(CFGKEY_SORTSTATE_SORTEDASCENDING);
            if( sortedColumn > -1 ) {
                modelTable.setSortedColumn(sortedColumn, isSortedAsc);
            }
        } else {
            modelTable.setSortedColumn(0, true);
        }

        lModelTable.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);

        TableColumnModel columnModel = lModelTable.getTable().getColumnModel();

        ShowContentTooltipRenderer showContentTooltipRenderer = new ShowContentTooltipRenderer();
        RightAlignRenderer numberRightRenderer = new RightAlignRenderer();
        BaseRenderer baseRenderer = new BaseRenderer();

        columnModel.getColumn(0).setCellRenderer(showContentTooltipRenderer); // filename
        columnModel.getColumn(1).setCellRenderer(numberRightRenderer); // fileSize
        columnModel.getColumn(2).setCellRenderer(showContentTooltipRenderer); // owner
        columnModel.getColumn(3).setCellRenderer(numberRightRenderer); // uploadCount
        columnModel.getColumn(4).setCellRenderer(baseRenderer); // lastUpload
        columnModel.getColumn(5).setCellRenderer(numberRightRenderer); // requestCount
        columnModel.getColumn(6).setCellRenderer(baseRenderer); // lastRequest
        columnModel.getColumn(7).setCellRenderer(showContentTooltipRenderer); // key
        columnModel.getColumn(8).setCellRenderer(baseRenderer); // rating
        columnModel.getColumn(9).setCellRenderer(showContentTooltipRenderer); // comment
        columnModel.getColumn(10).setCellRenderer(showContentTooltipRenderer); // keywords
        columnModel.getColumn(11).setCellRenderer(baseRenderer); // lastShared
        columnModel.getColumn(12).setCellRenderer(showContentTooltipRenderer); // path
        
        if( !loadTableLayout(columnModel) ) {
            //Sets the relative widths of the columns
            int[] widths = { 150, 65, 80, 40, 60, 40, 60, 80, 30, 50, 50, 60, 50 };
            for (int i = 0; i < widths.length; i++) {
                columnModel.getColumn(i).setPreferredWidth(widths[i]);
            }
        }
    }
    
    public void saveTableLayout() {
        TableColumnModel tcm = modelTable.getTable().getColumnModel();
        for(int columnIndexInTable=0; columnIndexInTable < tcm.getColumnCount(); columnIndexInTable++) {
            TableColumn tc = tcm.getColumn(columnIndexInTable);
            int columnIndexInModel = tc.getModelIndex();
            // save the current index in table for column with the fix index in model
            Core.frostSettings.setValue(CFGKEY_COLUMN_TABLEINDEX + columnIndexInModel, columnIndexInTable);
            // save the current width of the column
            int columnWidth = tc.getWidth();
            Core.frostSettings.setValue(CFGKEY_COLUMN_WIDTH + columnIndexInModel, columnWidth);
        }
        
        if( Core.frostSettings.getBoolValue(Settings.SAVE_SORT_STATES) && modelTable.getSortedColumn() > -1 ) {
            int sortedColumn = modelTable.getSortedColumn();
            boolean isSortedAsc = modelTable.isSortedAscending();
            Core.frostSettings.setValue(CFGKEY_SORTSTATE_SORTEDCOLUMN, sortedColumn);
            Core.frostSettings.setValue(CFGKEY_SORTSTATE_SORTEDASCENDING, isSortedAsc);
        }
    }
    
    private boolean loadTableLayout(TableColumnModel tcm) {
        
        // load the saved tableindex for each column in model, and its saved width
        int[] tableToModelIndex = new int[tcm.getColumnCount()];
        int[] columnWidths = new int[tcm.getColumnCount()];

        for(int x=0; x < tableToModelIndex.length; x++) {
            String indexKey = CFGKEY_COLUMN_TABLEINDEX + x;
            if( Core.frostSettings.getObjectValue(indexKey) == null ) {
                return false; // column not found, abort
            }
            // build array of table to model associations
            int tableIndex = Core.frostSettings.getIntValue(indexKey);
            if( tableIndex < 0 || tableIndex >= tableToModelIndex.length ) {
                return false; // invalid table index value
            }
            tableToModelIndex[tableIndex] = x;

            String widthKey = CFGKEY_COLUMN_WIDTH + x;
            if( Core.frostSettings.getObjectValue(widthKey) == null ) {
                return false; // column not found, abort
            }
            // build array of table to model associations
            int columnWidth = Core.frostSettings.getIntValue(widthKey);
            if( columnWidth <= 0 ) {
                return false; // invalid column width
            }
            columnWidths[x] = columnWidth;
        }
        // columns are currently added in model order, remove them all and save in an array
        // while on it, set the loaded width of each column
        TableColumn[] tcms = new TableColumn[tcm.getColumnCount()];
        for(int x=tcms.length-1; x >= 0; x--) {
            tcms[x] = tcm.getColumn(x);
            tcm.removeColumn(tcms[x]);
            tcms[x].setPreferredWidth(columnWidths[x]);
        }
        // add the columns in order loaded from settings
        for(int x=0; x < tableToModelIndex.length; x++) {
            tcm.addColumn(tcms[tableToModelIndex[x]]);
        }
        return true;
    }

    public int[] getColumnNumbers(int fieldID) {
        return null;
    }

    public void languageChanged(LanguageEvent event) {
        refreshLanguage();
    }

	private class ShowContentTooltipRenderer extends BaseRenderer {

		private static final long serialVersionUID = 1L;

		public ShowContentTooltipRenderer() {
			super();
		}

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) 
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String tooltip = null;
            if( value != null ) {
                tooltip = value.toString();
                if( tooltip.length() == 0 ) {
                    tooltip = null;
                }
            }
            setToolTipText(tooltip);
            return this;
        }
    }

	private class RightAlignRenderer extends BaseRenderer {

		private static final long serialVersionUID = 1L;

		private final EmptyBorder border = new EmptyBorder(0, 0, 0, 3);

		public RightAlignRenderer() {
			super();
		}

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) 
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            // col is right aligned, give some space to next column
            setBorder(border);
            return this;
        }
    }

    private class NameComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem item1, FrostSharedFileItem item2) {
            return item1.getFile().getName().compareToIgnoreCase(item2.getFile().getName());
        }
    }
    private class FileSizeComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem item1, FrostSharedFileItem item2) {
            if( item1.getFileSize() > item2.getFileSize() ) {
                return 1;
            } else if( item1.getFileSize() < item2.getFileSize() ) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    private class OwnerComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem item1, FrostSharedFileItem item2) {
            return item1.getOwner().compareToIgnoreCase(item2.getOwner());
        }
    }
    private class UploadCountComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem item1, FrostSharedFileItem item2) {
            if( item1.getUploadCount() > item2.getUploadCount() ) {
                return 1;
            } else if( item1.getUploadCount() < item2.getUploadCount() ) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    private class LastUploadComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem item1, FrostSharedFileItem item2) {
            if( item1.getLastUploaded() > item2.getLastUploaded() ) {
                return 1;
            } else if( item1.getLastUploaded() < item2.getLastUploaded() ) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    private class RequestCountComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem item1, FrostSharedFileItem item2) {
            if( item1.getRequestsReceived() > item2.getRequestsReceived() ) {
                return 1;
            } else if( item1.getRequestsReceived() < item2.getRequestsReceived() ) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    private class LastRequestComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem item1, FrostSharedFileItem item2) {
            if( item1.getRequestLastReceived() > item2.getRequestLastReceived() ) {
                return 1;
            } else if( item1.getRequestLastReceived() < item2.getRequestLastReceived() ) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    private class KeyComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem o1, FrostSharedFileItem o2) {
            String key1 = o1.getKey();
            String key2 = o2.getKey();
            if (key1 == null) {
                key1 = unknown;
            }
            if (key2 == null) {
                key2 = unknown;
            }
            return key1.compareToIgnoreCase(key2);
        }
    }
    private class RatingComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem item1, FrostSharedFileItem item2) {
            if( item1.getRating() > item2.getRating() ) {
                return 1;
            } else if( item1.getRating() < item2.getRating() ) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    private class CommentComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem o1, FrostSharedFileItem o2) {
            String key1 = o1.getComment();
            String key2 = o2.getComment();
            if (key1 == null) {
                key1 = unknown;
            }
            if (key2 == null) {
                key2 = unknown;
            }
            return key1.compareToIgnoreCase(key2);
        }
    }
    private class KeywordsComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem o1, FrostSharedFileItem o2) {
            String key1 = o1.getKeywords();
            String key2 = o2.getKeywords();
            if (key1 == null) {
                key1 = unknown;
            }
            if (key2 == null) {
                key2 = unknown;
            }
            return key1.compareToIgnoreCase(key2);
        }
    }
    private class LastSharedComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem item1, FrostSharedFileItem item2) {
            if( item1.getRefLastSent() > item2.getRefLastSent() ) {
                return 1;
            } else if( item1.getRefLastSent() < item2.getRefLastSent() ) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    private class PathComparator implements Comparator<FrostSharedFileItem> {
        public int compare(FrostSharedFileItem item1, FrostSharedFileItem item2) {
            return item1.getFile().getPath().compareToIgnoreCase(item2.getFile().getPath());
        }
    }

	private class BaseRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public BaseRenderer() {
			super();
		}

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) 
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if( !isSelected ) {
                Color newBackground = TableBackgroundColors.getBackgroundColor(table, row, showColoredLines);
                
                FrostSharedFileItem sfItem = modelTable.getItemAt(row); //It may be null
                if (sfItem != null) {
                    if( !sfItem.isValid() ) {
                        newBackground = TableBackgroundColors.getBackgroundColorFailed(table, row, showColoredLines);
                    }
                }
                setBackground(newBackground);
                setForeground(Color.black);
            }
            return this;
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Settings.SHOW_COLORED_ROWS)) {
            showColoredLines = Core.frostSettings.getBoolValue(Settings.SHOW_COLORED_ROWS);
            modelTable.fireTableDataChanged();
        }
    }
}
