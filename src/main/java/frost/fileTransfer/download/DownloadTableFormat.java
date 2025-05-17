/*
  DownloadTableFormat.java / Frost

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
package frost.fileTransfer.download;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import frost.Core;
import frost.Settings;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.FrostFileListFileObject;
import frost.fileTransfer.PersistenceManager;
import frost.fileTransfer.common.TableBackgroundColors;
import frost.util.DateFun;
import frost.util.FormatterUtils;
import frost.util.Mixed;
import frost.util.gui.BooleanCell;
import frost.util.gui.MiscToolkit;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;
import frost.util.model.ModelTable;
import frost.util.model.SortedModelTable;
import frost.util.model.SortedTableFormat;

public class DownloadTableFormat extends SortedTableFormat<FrostDownloadItem>
		implements LanguageListener, PropertyChangeListener {

    private static ImageIcon isSharedIcon = MiscToolkit.loadImageIcon("/data/shared.png");
    private static ImageIcon isRequestedIcon = MiscToolkit.loadImageIcon("/data/signal.png");
    private static ImageIcon isDDAIcon = MiscToolkit.loadImageIcon("/data/hook.png");

    private static final long CONST_32k = 32 * 1024;

    private SortedModelTable<FrostDownloadItem> modelTable = null;

    private boolean showColoredLines;
    
    private boolean fileSharingDisabled;
    
    private Map<Integer, Integer> mapCurrentColumntToPossibleColumn;
    
    /***
     * List of all possible columns in the download table
     * @author jgerrits
     *
     */
    private enum Columns {
    	ENABLED,
    	SHARED_FILE,
    	FILE_REQUESTED,
    	FILE_NAME,
    	SIZE,
    	STATE,
    	LAST_SEEN,
    	LAST_UPLOADED,
    	BLOCKS,
    	TRIES,
    	KEY,
    	DOWNLOAD_DIRECTORY,
    	DDA,
    	PRIORITY;
    	
    	private static TreeMap<Integer, Columns> numMap;
    	static {
    		numMap = new TreeMap<Integer, Columns>();
    		for (final Columns column: Columns.values()) {
				numMap.put(column.ordinal(), column);
    		}
    	}
    	
    	public static Columns lookup(int number) {
    		return numMap.get(number);
    	}
    }

	private class BaseRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public BaseRenderer() {
			super();
		}

        @Override
        public Component getTableCellRendererComponent(
            final JTable table,
            final Object value,
            final boolean isSelected,
            final boolean hasFocus,
            final int row,
            final int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if( !isSelected ) {

                Color newBackground = TableBackgroundColors.getBackgroundColor(table, row, showColoredLines);

                final FrostDownloadItem downloadItem = modelTable.getItemAt(row);
                if (downloadItem != null) {
                    final int itemState = downloadItem.getState();
                    if( itemState == FrostDownloadItem.STATE_DONE) {
                        newBackground = TableBackgroundColors.getBackgroundColorDone(table, row, showColoredLines);
                    } else if( itemState == FrostDownloadItem.STATE_FAILED) {
                        newBackground = TableBackgroundColors.getBackgroundColorFailed(table, row, showColoredLines);
                    }
                }
                setBackground(newBackground);
                setForeground(Color.black);
            }
            return this;
        }
    }

	private class BlocksProgressRenderer extends JProgressBar implements TableCellRenderer {

		private static final long serialVersionUID = 1L;

		public BlocksProgressRenderer() {
            super();
            setMinimum(0);
            setMaximum(100);
            setStringPainted(true);
            setBorderPainted(false);
        }
        
        public Component getTableCellRendererComponent(
            final JTable table,
            final Object value,
            final boolean isSelected,
            final boolean hasFocus,
            final int row,
            final int column) {

            final Color newBackground = TableBackgroundColors.getBackgroundColor(table, row, showColoredLines);
            setBackground(newBackground);

            setValue(0);

            final FrostDownloadItem downloadItem = modelTable.getItemAt(row); //It may be null
            if (downloadItem != null) {
                final int totalBlocks = downloadItem.getTotalBlocks();
                final int doneBlocks = downloadItem.getDoneBlocks();
                final int requiredBlocks = downloadItem.getRequiredBlocks();

                if( totalBlocks > 0 ) {
                    // format: ~0% 0/60 [60]

                    int percentDone = 0;

                    if (requiredBlocks > 0) {
                        percentDone = ((doneBlocks * 100) / requiredBlocks);
                    }
                    if( percentDone > 100 ) {
                        percentDone = 100;
                    }
                    setValue(percentDone);
                }
            }
            setString(value.toString());

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
			final JTable table,
			final Object value,
			final boolean isSelected,
			final boolean hasFocus,
			final int row,
			final int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setHorizontalAlignment(SwingConstants.RIGHT);
			// col is right aligned, give some space to next column
			setBorder(border);
			return this;
		}
	}

	private class ShowContentTooltipRenderer extends BaseRenderer {

		private static final long serialVersionUID = 1L;

		public ShowContentTooltipRenderer() {
            super();
        }
        
        @Override
        public Component getTableCellRendererComponent(
            final JTable table,
            final Object value,
            final boolean isSelected,
            final boolean hasFocus,
            final int row,
            final int column) {
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

	private class ShowNameTooltipRenderer extends BaseRenderer {

		private static final long serialVersionUID = 1L;

		public ShowNameTooltipRenderer() {
            super();
        }
        
        @Override
        public Component getTableCellRendererComponent(
            final JTable table,
            final Object value,
            final boolean isSelected,
            final boolean hasFocus,
            final int row,
            final int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String tooltip = null;
            final FrostDownloadItem downloadItem = modelTable.getItemAt(row); //It may be null
            if (downloadItem != null) {
                final StringBuilder sb = new StringBuilder();
                sb.append("<html>").append(downloadItem.getFileName());
                if( downloadItem.getDownloadAddedMillis() > 0 ) {
                    sb.append("<br>Added: ");
					sb.append(DateFun.FORMAT_DATE_TIME_VISIBLE
							.format(Instant.ofEpochMilli(downloadItem.getDownloadAddedMillis())));
                }
                if( downloadItem.getDownloadStartedMillis() > 0 ) {
                    sb.append("<br>Started: ");
					sb.append(DateFun.FORMAT_DATE_TIME_VISIBLE
							.format(Instant.ofEpochMilli(downloadItem.getDownloadStartedMillis())));
                }
                if( downloadItem.getDownloadFinishedMillis() > 0 ) {
                    sb.append("<br>Finished: ");
					sb.append(DateFun.FORMAT_DATE_TIME_VISIBLE
							.format(Instant.ofEpochMilli(downloadItem.getDownloadFinishedMillis())));
                }
                sb.append("</html>");
                tooltip = sb.toString();
            }
            setToolTipText(tooltip);
            return this;
        }
    }

	private class ShowStateContentTooltipRenderer extends BaseRenderer {

		private static final long serialVersionUID = 1L;

		public ShowStateContentTooltipRenderer() {
            super();
        }
        
        @Override
        public Component getTableCellRendererComponent(
            final JTable table,
            final Object value,
            final boolean isSelected,
            final boolean hasFocus,
            final int row,
            final int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String tooltip = null;
            final FrostDownloadItem downloadItem = modelTable.getItemAt(row); //It may be null
            if (downloadItem != null) {
                final String errorCodeDescription = downloadItem.getErrorCodeDescription();
                if( errorCodeDescription != null && errorCodeDescription.length() > 0 ) {
                    tooltip = "Last error: "+errorCodeDescription;
                }
            }
            setToolTipText(tooltip);
            return this;
        }
    }

	private class IsEnabledRenderer extends JCheckBox implements TableCellRenderer {

		private static final long serialVersionUID = 1L;

		public IsEnabledRenderer() {
            super();
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
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }

            final FrostDownloadItem downloadItem = modelTable.getItemAt(row); //It may be null
            if (downloadItem != null) {
                if( downloadItem.isExternal() ) {
                    setEnabled(false);
                    setSelected(true); // external items are always enabled
                } else {
                    setEnabled(true);
                    setSelected((
                            value != null
                            && value instanceof Boolean
                            && ((Boolean) value).booleanValue()));
                }
            }
            return this;
        }
    }

	private class IsSharedRenderer extends BaseRenderer {

		private static final long serialVersionUID = 1L;

		public IsSharedRenderer() {
            super();
        }
        
        @Override
        public Component getTableCellRendererComponent(
            final JTable table,
            final Object value,
            final boolean isSelected,
            final boolean hasFocus,
            final int row,
            final int column)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText("");
            setToolTipText(isSharedTooltip);
            if (value instanceof Boolean) {
                final Boolean b = (Boolean)value;
                if( b.booleanValue() ) {
                    // show shared icon
                    setIcon(isSharedIcon);
                } else {
                    setIcon(null);
                }
            }
            return this;
        }
    }

	private class IsRequestedRenderer extends BaseRenderer {

		private static final long serialVersionUID = 1L;

		public IsRequestedRenderer() {
            super();
        }
        
        @Override
        public Component getTableCellRendererComponent(
            final JTable table,
            final Object value,
            final boolean isSelected,
            final boolean hasFocus,
            final int row,
            final int column)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText("");
            setToolTipText(isRequestedTooltip);
            if (value instanceof Boolean) {
                final Boolean b = (Boolean)value;
                if( b.booleanValue() ) {
                    // show icon
                    setIcon(isRequestedIcon);
                } else {
                    setIcon(null);
                }
            }
            return this;
        }
    }

	private class IsDDARenderer extends BaseRenderer {

		private static final long serialVersionUID = 1L;

		public IsDDARenderer() {
            super();
        }
        
        @Override
        public Component getTableCellRendererComponent(
            final JTable table,
            final Object value,
            final boolean isSelected,
            final boolean hasFocus,
            final int row,
            final int column)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText("");
            setToolTipText(isDDATooltip);
            if (value instanceof Boolean) {
                final Boolean b = (Boolean)value;
                if( b.booleanValue() ) {
                    // show icon
                    setIcon(isDDAIcon);
                } else {
                    setIcon(null);
                }
            }
            return this;
        }
    }

	private class KeyComparator implements Comparator<FrostDownloadItem> {
		public int compare(final FrostDownloadItem o1, final FrostDownloadItem o2) {
			String key1 = o1.getKey();
			String key2 = o2.getKey();
			if (key1 == null) {
				key1 = "";
			}
			if (key2 == null) {
				key2 = "";
			}
			return key1.compareToIgnoreCase(key2);
		}
	}

	private class TriesComparator implements Comparator<FrostDownloadItem> {
		public int compare(final FrostDownloadItem o1, final FrostDownloadItem o2) {
			final int retries1 = o1.getRetries();
			final int retries2 = o2.getRetries();
			return Mixed.compareInt(retries1, retries2);
		}
	}

    private class PriorityComparator implements Comparator<FrostDownloadItem> {
        public int compare(final FrostDownloadItem o1, final FrostDownloadItem o2) {
        	return o1.getPriority().compareTo(o2.getPriority());
        }
    }

	private class BlocksComparator implements Comparator<FrostDownloadItem> {
		public int compare(final FrostDownloadItem item1, final FrostDownloadItem item2) {

		    // compare by percent completed. Finalized items are grouped.
            final int percentDone1 = calculatePercentDone(item1);
            final int percentDone2 = calculatePercentDone(item2);

            return Mixed.compareInt(percentDone1, percentDone2);
		}

        private int calculatePercentDone(final FrostDownloadItem item)
        {
            final Boolean isFinalized = item.isFinalized();
            final int totalBlocks     = item.getTotalBlocks();
            final int doneBlocks      = item.getDoneBlocks();
            final int requiredBlocks  = item.getRequiredBlocks();

            int percentDone = 0;
            if (isFinalized != null) {
                // isFinalized is set because the node sent progress
                if( totalBlocks > 0 ) {
                    if (requiredBlocks > 0) {
                        percentDone = ((doneBlocks * 100) / requiredBlocks);
                    }
                    if( percentDone > 100 ) {
                        percentDone = 100;
                    }
                }
            }
            if (isFinalized != null && isFinalized.booleanValue()) {
                // finalized get highest value
                percentDone = (percentDone+2)*100;
            } else if (isFinalized != null && !isFinalized.booleanValue()) {
                // not finalized, but obviously started by node
                percentDone = percentDone+1;
            } else {
                // not started by node
                percentDone = 0;
            }
            return percentDone;
        }
	}

	private class StateComparator implements Comparator<FrostDownloadItem> {
		public int compare(final FrostDownloadItem item1, final FrostDownloadItem item2) {
			final String state1 =	getStateAsString(item1.getState());
			final String state2 =	getStateAsString(item2.getState());
			return state1.compareToIgnoreCase(state2);
		}
	}

	private class SizeComparator implements Comparator<FrostDownloadItem> {

		private final Long unknownSize = -1L;

		public int compare(final FrostDownloadItem dli1, final FrostDownloadItem dli2) {
			Long size1 = dli1.getFileSize();
			Long size2 = dli2.getFileSize();

			if (dli1.getFileSize() >= 0) {
                size1 = dli1.getFileSize();
            } else if( dli1.getTotalBlocks() > 0
                           && dli1.isFinalized() != null
                           && dli1.isFinalized().booleanValue() == true )
            {
                // on 0.7, compute appr. size out of finalized block count
				size1 = dli1.getTotalBlocks() * CONST_32k;
            } else {
                size1 = unknownSize;
            }

            if (dli2.getFileSize() >= 0) {
                size2 = dli2.getFileSize();
            } else if( dli2.getTotalBlocks() > 0
                           && dli2.isFinalized() != null
                           && dli2.isFinalized().booleanValue() == true )
            {
                // on 0.7, compute appr. size out of finalized block count
				size2 = dli2.getTotalBlocks() * CONST_32k;
            } else {
                size2 = unknownSize;
            }

			return size1.compareTo(size2);
		}
	}

	private class FileNameComparator implements Comparator<FrostDownloadItem> {
		public int compare(final FrostDownloadItem item1, final FrostDownloadItem item2) {
			return item1.getFileName().compareToIgnoreCase(item2.getFileName());
		}
	}

	private class DownloadDirComparator implements Comparator<FrostDownloadItem> {
		public int compare(final FrostDownloadItem item1, final FrostDownloadItem item2) {
			return item1.getDownloadFilename().compareToIgnoreCase(item2.getDownloadFilename());
		}
	}

	private class EnabledComparator implements Comparator<FrostDownloadItem> {
		public int compare(final FrostDownloadItem item1, final FrostDownloadItem item2) {
            final Boolean b1 = Boolean.valueOf( item1.isEnabled().booleanValue() );
            final Boolean b2 = Boolean.valueOf( item2.isEnabled().booleanValue() );
            return b1.compareTo(b2);
		}
	}

    private class IsSharedComparator implements Comparator<FrostDownloadItem> {
        public int compare(final FrostDownloadItem item1, final FrostDownloadItem item2) {
            final Boolean b1 = Boolean.valueOf( item1.isSharedFile() );
            final Boolean b2 = Boolean.valueOf( item2.isSharedFile() );
            return b1.compareTo(b2);
        }
    }

    private class IsRequestedComparator implements Comparator<FrostDownloadItem> {
        public int compare(final FrostDownloadItem item1, final FrostDownloadItem item2) {
            final Boolean b1 = getIsRequested(item1.getFileListFileObject());
            final Boolean b2 = getIsRequested(item2.getFileListFileObject());
            return b1.compareTo(b2);
        }
    }

    private class IsDDAComparator implements Comparator<FrostDownloadItem> {
        public int compare(final FrostDownloadItem item1, final FrostDownloadItem item2) {
            final Boolean b1 = Boolean.valueOf(item1.isDirect());
            final Boolean b2 = Boolean.valueOf(item2.isDirect());
            return b1.compareTo(b2);
        }
    }

    private class LastReceivedComparator implements Comparator<FrostDownloadItem> {
        public int compare(final FrostDownloadItem item1, final FrostDownloadItem item2) {
            return Mixed.compareLong(item1.getLastReceived(), item2.getLastReceived());
        }
    }

    private class LastUploadedComparator implements Comparator<FrostDownloadItem> {
        public int compare(final FrostDownloadItem item1, final FrostDownloadItem item2) {
            return Mixed.compareLong(item1.getLastUploaded(), item2.getLastUploaded());
        }
    }

	private final Language language;

    private String stateWaiting;
    private String stateTrying;
    private String stateFailed;
    private String stateDone;
    private String stateDecoding;
    private String stateDownloading;

    private String unknown;

    private String isSharedTooltip;
    private String isRequestedTooltip;
    private String isDDATooltip;

	public DownloadTableFormat() {
		super();

		fileSharingDisabled = Core.frostSettings.getBoolean(Settings.FILESHARING_DISABLE);
		
		language = Language.getInstance();
		language.addLanguageListener(this);
		refreshLanguage();

		int columnCounter = 0;
		setComparator(new EnabledComparator(), columnCounter++);
		if( fileSharingDisabled == false ) {
			setComparator(new IsSharedComparator(), columnCounter++);
			setComparator(new IsRequestedComparator(), columnCounter++);
		}
		setComparator(new FileNameComparator(), columnCounter++);
		setComparator(new SizeComparator(), columnCounter++);
		setComparator(new StateComparator(), columnCounter++);
		if( fileSharingDisabled == false ) {
			setComparator(new LastReceivedComparator(), columnCounter++);
			setComparator(new LastUploadedComparator(), columnCounter++);
		}
		setComparator(new BlocksComparator(), columnCounter++);
		setComparator(new TriesComparator(), columnCounter++);
		setComparator(new KeyComparator(), columnCounter++);
		setComparator(new DownloadDirComparator(), columnCounter++);
		if( PersistenceManager.isPersistenceEnabled() ) {
			setComparator(new IsDDAComparator(), columnCounter++);
			setComparator(new PriorityComparator(), columnCounter++);
		}

		showColoredLines = Core.frostSettings.getBoolean(Settings.SHOW_COLORED_ROWS);
		Core.frostSettings.addPropertyChangeListener(this);
	}

	private void refreshLanguage() {
		int columnCounter = 0;

		setColumnName(columnCounter++, language.getString("Common.enabled"));
		if( fileSharingDisabled == false ) {
			setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.shared"));
			setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.requested"));
		}
		setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.filename"));
		setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.size"));
		setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.state"));
		if( fileSharingDisabled == false ) {
			setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.lastReceived"));
			setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.lastUploaded"));
		}
		setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.blocks"));
		setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.tries"));
		setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.key"));
		setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.downloadDir"));
		if( PersistenceManager.isPersistenceEnabled() ) {
			setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.isDDA"));
			setColumnName(columnCounter++, language.getString("DownloadPane.fileTable.priority"));
		}

		stateWaiting =  language.getString("DownloadPane.fileTable.states.waiting");
		stateTrying =   language.getString("DownloadPane.fileTable.states.trying");
		stateFailed =   language.getString("DownloadPane.fileTable.states.failed");
		stateDone =     language.getString("DownloadPane.fileTable.states.done");
		stateDecoding = language.getString("DownloadPane.fileTable.states.decodingSegment") + "...";
		stateDownloading = language.getString("DownloadPane.fileTable.states.downloading");

		unknown =   language.getString("DownloadPane.fileTable.states.unknown");

		isSharedTooltip = language.getString("DownloadPane.fileTable.shared.tooltip");
		isRequestedTooltip = language.getString("DownloadPane.fileTable.requested.tooltip");
		isDDATooltip = language.getString("DownloadPane.fileTable.isDDA.tooltip");

		refreshColumnNames();
	}

	public void languageChanged(final LanguageEvent event) {
		refreshLanguage();
	}

	public Object getCellValue(final FrostDownloadItem downloadItem, final int columnIndex) {
        if( downloadItem == null ) {
            return "*null*";
        }
		switch(Columns.lookup(mapCurrentColumntToPossibleColumn.get(columnIndex))) {

			case ENABLED :
				return downloadItem.isEnabled();

            case SHARED_FILE : // isShared
                return Boolean.valueOf( downloadItem.isSharedFile() );

            case FILE_REQUESTED : // isRequested
                return getIsRequested( downloadItem.getFileListFileObject() );

			case FILE_NAME : // Filename
				return downloadItem.getFileName();

			case SIZE : // Size
                if( downloadItem.getFileSize() >= 0 ) {
                    // size is set
                    return FormatterUtils.formatSize(downloadItem.getFileSize());

                } else if( downloadItem.getRequiredBlocks() > 0
                           && downloadItem.isFinalized() != null
                           && downloadItem.isFinalized().booleanValue() == true )
                {
                    // on 0.7, compute appr. size out of finalized block count
                    final long apprSize = downloadItem.getRequiredBlocks() * CONST_32k;
                    return "~" + FormatterUtils.formatSize(apprSize);
                } else {
					return unknown;
				}

			case STATE : // State
				return getStateAsString(downloadItem.getState());

            case LAST_SEEN : // lastReceived
                if( downloadItem.getLastReceived() > 0 ) {
                    return DateFun.getExtendedDateFromMillis(downloadItem.getLastReceived());
                } else {
                    return "";
                }

            case LAST_UPLOADED : // lastUploaded
                if( downloadItem.getLastUploaded() > 0 ) {
                    return DateFun.getExtendedDateFromMillis(downloadItem.getLastUploaded());
                } else {
                    return "";
                }

			case BLOCKS : // Blocks
				return getBlocksAsString(downloadItem);

			case TRIES : // Tries
				return downloadItem.getRetries();

			case KEY : // Key
				if (downloadItem.getKey() == null) {
					return " ?";
				} else {
					return downloadItem.getKey();
				}

            case DOWNLOAD_DIRECTORY :    // Download dir
                return downloadItem.getDownloadDir();

            case DDA : // IsDDA
                return Boolean.valueOf(!downloadItem.isDirect());

            case PRIORITY : // Priority
				return downloadItem.getPriority().getNumber();

			default :
				return "**ERROR**";
		}
	}

    private Boolean getIsRequested(final FrostFileListFileObject flfo) {
        if( flfo == null ) {
            return Boolean.FALSE;
        }
        final long now = System.currentTimeMillis();
        final long before24hours = now - (24L * 60L * 60L * 1000L);
        if( flfo.getRequestLastReceived() > before24hours
                || flfo.getRequestLastSent() > before24hours)
        {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

	private String getBlocksAsString(final FrostDownloadItem downloadItem) {

        final int totalBlocks = downloadItem.getTotalBlocks();
        final int doneBlocks = downloadItem.getDoneBlocks();
        final int requiredBlocks = downloadItem.getRequiredBlocks();
        final Boolean isFinalized = downloadItem.isFinalized();

        if( totalBlocks <= 0 ) {
            return "";
        }

        // format: ~0% 0/60 [60]

        int percentDone = 0;

        if (requiredBlocks > 0) {
            percentDone = ((doneBlocks * 100) / requiredBlocks);
        }
        if( percentDone > 100 ) {
            percentDone = 100;
        }

        final StringBuilder sb = new StringBuilder();

        if( isFinalized != null && isFinalized.booleanValue() == false ) {
            sb.append("~");
        }

        sb.append(percentDone).append("% ");
        sb.append(doneBlocks).append("/").append(requiredBlocks).append(" [").append(totalBlocks).append("]");

		return sb.toString();
	}

	private String getStateAsString(final int state) {
		switch (state) {
			case FrostDownloadItem.STATE_WAITING :
				return stateWaiting;

			case FrostDownloadItem.STATE_TRYING :
				return stateTrying;

			case FrostDownloadItem.STATE_FAILED :
				return stateFailed;

			case FrostDownloadItem.STATE_DONE :
				return stateDone;

			case FrostDownloadItem.STATE_DECODING :
				return stateDecoding;

			case FrostDownloadItem.STATE_PROGRESS :
				return stateDownloading;

			default :
				return "**ERROR**";
		}
	}

    public void customizeTable(final ModelTable<FrostDownloadItem> lModelTable) {
		super.customizeTable(lModelTable);

        modelTable = (SortedModelTable<FrostDownloadItem>) lModelTable;

		if (Core.frostSettings.getBoolean(Settings.SAVE_SORT_STATES)
				&& Core.frostSettings.getObjectValue(Settings.DOWNLOAD_TABLE_SORT_STATE_SORTED_COLUMN) != null
				&& Core.frostSettings.getObjectValue(Settings.DOWNLOAD_TABLE_SORT_STATE_SORTED_ASCENDING) != null) {
			final int sortedColumn = Core.frostSettings.getInteger(Settings.DOWNLOAD_TABLE_SORT_STATE_SORTED_COLUMN);
			final boolean isSortedAsc = Core.frostSettings
					.getBoolean(Settings.DOWNLOAD_TABLE_SORT_STATE_SORTED_ASCENDING);
            if( sortedColumn > -1 ) {
                modelTable.setSortedColumn(sortedColumn, isSortedAsc);
            }
        } else {
            modelTable.setSortedColumn(3, true);
        }

        lModelTable.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);

        final BaseRenderer baseRenderer = new BaseRenderer();
        final TableColumnModel columnModel = lModelTable.getTable().getColumnModel();
        final RightAlignRenderer rightAlignRenderer = new RightAlignRenderer();
        int columnCounter = 0;
        mapCurrentColumntToPossibleColumn = new HashMap<Integer, Integer>();
        
        // Column "Enabled"
        columnModel.getColumn(columnCounter).setCellEditor(BooleanCell.EDITOR);
        setColumnEditable(columnCounter, true);
        columnModel.getColumn(columnCounter).setMinWidth(20);
        columnModel.getColumn(columnCounter).setMaxWidth(20);
        columnModel.getColumn(columnCounter).setPreferredWidth(20);
        columnModel.getColumn(columnCounter).setCellRenderer(new IsEnabledRenderer());
        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.ENABLED.ordinal());

        if( fileSharingDisabled == false ) {
	        // hard set sizes of icon column - Shared file
	        columnModel.getColumn(columnCounter).setMinWidth(20);
	        columnModel.getColumn(columnCounter).setMaxWidth(20);
	        columnModel.getColumn(columnCounter).setPreferredWidth(20);
	        columnModel.getColumn(columnCounter).setCellRenderer(new IsSharedRenderer());
	        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.SHARED_FILE.ordinal());
	        
	        // hard set sizes of icon column - File requested
	        columnModel.getColumn(columnCounter).setMinWidth(20);
	        columnModel.getColumn(columnCounter).setMaxWidth(20);
	        columnModel.getColumn(columnCounter).setPreferredWidth(20);
	        columnModel.getColumn(columnCounter).setCellRenderer(new IsRequestedRenderer());
	        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.FILE_REQUESTED.ordinal());
        }

        // fileName
        columnModel.getColumn(columnCounter).setCellRenderer(new ShowNameTooltipRenderer());
        columnModel.getColumn(columnCounter).setPreferredWidth(150);
        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.FILE_NAME.ordinal());
        
        // size
        columnModel.getColumn(columnCounter).setCellRenderer(rightAlignRenderer); 
        columnModel.getColumn(columnCounter).setPreferredWidth(30);
        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.SIZE.ordinal());
        
        // state
        columnModel.getColumn(columnCounter).setCellRenderer(new ShowStateContentTooltipRenderer()); // state
        columnModel.getColumn(columnCounter).setPreferredWidth(30);
        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.STATE.ordinal());
        
        if( fileSharingDisabled == false ) {
	        // lastSeen
	        columnModel.getColumn(columnCounter).setCellRenderer(baseRenderer); // last 
	        columnModel.getColumn(columnCounter).setPreferredWidth(20);
	        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.LAST_SEEN.ordinal());
	        
	        // lastUloaded
	        columnModel.getColumn(columnCounter).setCellRenderer(baseRenderer); // lastUploaded
	        columnModel.getColumn(columnCounter).setPreferredWidth(20);
	        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.LAST_UPLOADED.ordinal());
        }
        
        // blocks
        columnModel.getColumn(columnCounter).setCellRenderer(new BlocksProgressRenderer()); // blocks
        columnModel.getColumn(columnCounter).setPreferredWidth(70);
        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.BLOCKS.ordinal());
        
        // tries
        columnModel.getColumn(columnCounter).setCellRenderer(rightAlignRenderer); // tries
        columnModel.getColumn(columnCounter).setPreferredWidth(10);
        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.TRIES.ordinal());
        
        // key
        columnModel.getColumn(columnCounter).setCellRenderer(new ShowContentTooltipRenderer()); // key
        columnModel.getColumn(columnCounter).setPreferredWidth(60);
        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.KEY.ordinal());
        
        // download dir
        columnModel.getColumn(columnCounter).setCellRenderer(baseRenderer); // download dir
        columnModel.getColumn(columnCounter).setPreferredWidth(60);
        mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.DOWNLOAD_DIRECTORY.ordinal());
        
        if( PersistenceManager.isPersistenceEnabled() ) {
        	// IsDDA 
        	columnModel.getColumn(columnCounter).setMinWidth(20);
        	columnModel.getColumn(columnCounter).setMaxWidth(20);
        	columnModel.getColumn(columnCounter).setPreferredWidth(20);
            columnModel.getColumn(columnCounter).setCellRenderer(new IsDDARenderer());
            mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.DDA.ordinal());
            
            // priority
            columnModel.getColumn(columnCounter).setMinWidth(20);
            columnModel.getColumn(columnCounter).setMaxWidth(20);
            columnModel.getColumn(columnCounter).setPreferredWidth(20);
            columnModel.getColumn(columnCounter).setCellRenderer(rightAlignRenderer); // prio
            mapCurrentColumntToPossibleColumn.put(columnCounter++, Columns.PRIORITY.ordinal());
        }

        loadTableLayout(columnModel);
	}

    public void saveTableLayout() {
        final TableColumnModel tcm = modelTable.getTable().getColumnModel();
        
        for(int columnIndexInTable=0; columnIndexInTable < tcm.getColumnCount(); columnIndexInTable++) {
            final TableColumn tc = tcm.getColumn(columnIndexInTable);
            final int columnIndexInModel = tc.getModelIndex();
            final int columnIndexAll = mapCurrentColumntToPossibleColumn.get(columnIndexInModel);
            
            // save the current index in table for column with the fix index in model
			Core.frostSettings.setValue(Settings.DOWNLOAD_TABLE_COLUMN_TABLE_INDEX_PREFIX + columnIndexAll,
					columnIndexInTable);

            // save the current width of the column
            final int columnWidth = tc.getWidth();
			Core.frostSettings.setValue(Settings.DOWNLOAD_TABLE_COLUMN_WIDTH_PREFIX + columnIndexAll, columnWidth);
        }

        if( Core.frostSettings.getBoolean(Settings.SAVE_SORT_STATES) && modelTable.getSortedColumn() > -1 ) {
            final int sortedColumn = modelTable.getSortedColumn();
            final boolean isSortedAsc = modelTable.isSortedAscending();
			Core.frostSettings.setValue(Settings.DOWNLOAD_TABLE_SORT_STATE_SORTED_COLUMN, sortedColumn);
			Core.frostSettings.setValue(Settings.DOWNLOAD_TABLE_SORT_STATE_SORTED_ASCENDING, isSortedAsc);
        }
    }

    private boolean loadTableLayout(final TableColumnModel tcm) {

        // load the saved tableindex for each column in model, and its saved width
        final int[] tableToModelIndex = new int[tcm.getColumnCount()];
        final int[] columnWidths = new int[tcm.getColumnCount()];
        
        // Reverse map
        HashMap<Integer,Integer> mapPossibleColumnToCurrentColumnt = new HashMap<Integer,Integer>();
        for( int num = 0; num < mapCurrentColumntToPossibleColumn.size(); num++) {
        	mapPossibleColumnToCurrentColumnt.put(mapCurrentColumntToPossibleColumn.get(num), num);
        }

        for(int columnIndexAll = 0; columnIndexAll < Columns.values().length; columnIndexAll++) {

        	// check if column is currently displayed
        	if( ! mapPossibleColumnToCurrentColumnt.containsKey(columnIndexAll) ) {
        		continue;
        	}

        	// Map numbers
        	final int columnIndexInModel = mapPossibleColumnToCurrentColumnt.get(columnIndexAll);

        	// Check if position was saved for column
        	final String indexKey = Settings.DOWNLOAD_TABLE_COLUMN_TABLE_INDEX_PREFIX + columnIndexAll;
        	if( Core.frostSettings.getObjectValue(indexKey) == null ) {
        		return false; // column not found, abort
        	}

        	// get saved position
        	final int tableIndex = Core.frostSettings.getInteger(indexKey);
        	if( tableIndex < 0 || tableIndex >= tableToModelIndex.length ) {
        		return false; // invalid table index value
        	}
        	tableToModelIndex[tableIndex] = columnIndexInModel;

        	// Check if width was saved for column
        	final String widthKey = Settings.DOWNLOAD_TABLE_COLUMN_WIDTH_PREFIX + columnIndexAll;
        	if( Core.frostSettings.getObjectValue(widthKey) == null ) {
				return false;
			}

        	// Get saved width
        	final int columnWidth = Core.frostSettings.getInteger(widthKey);
        	if( columnWidth <= 0 || columnIndexInModel < 0 || columnIndexInModel >= tableToModelIndex.length) {
        		return false; 
        	}
        	columnWidths[columnIndexInModel] = columnWidth;
        }

        // columns are currently added in model order, remove them all and save in an array
        // while on it, set the loaded width of each column
        final TableColumn[] tcms = new TableColumn[tcm.getColumnCount()];
        for(int x=tcms.length-1; x >= 0; x--) {
        	tcms[x] = tcm.getColumn(x);
        	tcm.removeColumn(tcms[x]);
        	// keep icon columns 0,1,2 as is

        	if(x != 0 && x != 1 && x != 2) {
        		tcms[x].setPreferredWidth(columnWidths[x]);
        	}
        }

        // add the columns in order loaded from settings
        for( final int element : tableToModelIndex ) {
        	tcm.addColumn(tcms[element]);
        }

        return true;
    }

	@Override
    public void setCellValue(final Object value, final FrostDownloadItem downloadItem, final int columnIndex) {
		switch (columnIndex) {

			case 0 : //Enabled
				final Boolean valueBoolean = (Boolean) value;
				downloadItem.setEnabled(valueBoolean);
				FileTransferManager.inst().getDownloadManager().notifyDownloadItemEnabledStateChanged(downloadItem);
				break;

			default :
				super.setCellValue(value, downloadItem, columnIndex);
		}
	}

    public int[] getColumnNumbers(final int fieldID) {
        return null;
    }

    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Settings.SHOW_COLORED_ROWS)) {
            showColoredLines = Core.frostSettings.getBoolean(Settings.SHOW_COLORED_ROWS);
            modelTable.fireTableDataChanged();
        }
    }
}
