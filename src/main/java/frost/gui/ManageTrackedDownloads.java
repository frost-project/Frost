/*
  ManageTrackedDownloads.java / Frost
  Copyright (C) 2010  Frost Project <jtcfrost.sourceforge.net>

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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.fcp.FreenetKeys;
import frost.gui.model.SortedTableModel;
import frost.gui.model.TableMember;
import frost.storage.perst.TrackDownloadKeys;
import frost.storage.perst.TrackDownloadKeysStorage;
import frost.util.DateFun;
import frost.util.FormatterUtils;
import frost.util.gui.SimplePopupMenuListener;
import frost.util.gui.action.BaseAction;
import frost.util.gui.translation.Language;

public class ManageTrackedDownloads extends JDialog implements SimplePopupMenuListener {

	private static final Logger logger = LoggerFactory.getLogger(ManageTrackedDownloads.class);

	private static final long serialVersionUID = 1L;

	private transient final Language language;
	private transient final TrackDownloadKeysStorage trackDownloadKeysStorage;

	private TrackedDownloadsModel trackedDownloadsModel;
	private TrackedDownloadsTable trackedDownloadsTable;

	private JTextField maxAgeTextField;
	private JButton maxAgeButton;
	private JButton addKeysButton;
	private JButton closeButton;

	private RemoveDownloadAction removeDownloadAction;
	private RemoveDownloadSameBoardAction removeDownloadSameBoardAction;
	private PopupMenuTrackDownloads tablePopupMenu;

	public ManageTrackedDownloads(final JFrame frame) {
		super(frame);
		setModal(true);
		language = Language.getInstance();
		trackDownloadKeysStorage = TrackDownloadKeysStorage.inst();

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		initGUI();
	}

	private void initGUI() {
		setTitle(language.getString("ManageDownloadTrackingDialog.title"));
		setSize(800, 600);
		setResizable(true);

		// Max Age
		final JLabel maxAgeLabel = new JLabel(language.getString("ManageDownloadTrackingDialog.button.maxAge"));
		maxAgeTextField = new JTextField(6);
		maxAgeTextField.setText("100");
		maxAgeTextField.setMaximumSize(new Dimension(30, 20));
		maxAgeButton = new JButton(language.getString("ManageDownloadTrackingDialog.button.maxAgeButton"));
		maxAgeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				maxAgeButton_actionPerformed(e);
			}
		});
		maxAgeButton.setToolTipText(language.getString("ManageDownloadTrackingDialog.buttonTooltip.maxAgeButton"));

		// Load files
		addKeysButton = new JButton(language.getString("ManageDownloadTrackingDialog.button.addKeys"));
		addKeysButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addKeysButton_actionPerformed(e);
			}
		});
		addKeysButton.setToolTipText(language.getString("ManageDownloadTrackingDialog.buttonTooltip.addKeys"));

		// Close Button
		closeButton = new JButton(language.getString("Common.close"));
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		// Button row
		final JPanel buttonsPanel = new JPanel(new BorderLayout());
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

		buttonsPanel.add(maxAgeLabel);
		buttonsPanel.add(Box.createRigidArea(new Dimension(10, 3)));
		buttonsPanel.add(maxAgeTextField);
		buttonsPanel.add(Box.createRigidArea(new Dimension(10, 3)));
		buttonsPanel.add(maxAgeButton);

		buttonsPanel.add(Box.createHorizontalGlue());

		buttonsPanel.add(addKeysButton);
		buttonsPanel.add(Box.createRigidArea(new Dimension(20, 3)));
		buttonsPanel.add(closeButton);

		// Download Table
		trackedDownloadsModel = new TrackedDownloadsModel();
		trackedDownloadsTable = new TrackedDownloadsTable(trackedDownloadsModel);
		trackedDownloadsTable.setRowSelectionAllowed(true);
		trackedDownloadsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		trackedDownloadsTable.setRowHeight(18);
		final JScrollPane scrollPane = new JScrollPane(trackedDownloadsTable);
		scrollPane.setWheelScrollingEnabled(true);

		// main panel
		final JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 7, 7, 7));

		getContentPane().setLayout(new BorderLayout());
		mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
		getContentPane().add(mainPanel, null);

		removeDownloadAction = new RemoveDownloadAction();
		removeDownloadSameBoardAction = new RemoveDownloadSameBoardAction();
		tablePopupMenu = new PopupMenuTrackDownloads();
		tablePopupMenu.addPopupMenuListener(this);
		trackedDownloadsTable.setComponentPopupMenu(tablePopupMenu);
	}

	public void startDialog(final Frame owner) {
		loadTrackedDownloadsIntoTable();
		setLocationRelativeTo(owner);

		setVisible(true); // blocking!
	}

	private void loadTrackedDownloadsIntoTable() {
		trackedDownloadsModel.clearDataModel();
		for( final TrackDownloadKeys trackDownloadkey : trackDownloadKeysStorage.getDownloadKeyList()) {
			final TrackedDownloadTableMember trackedDownloadTableMember = new TrackedDownloadTableMember(trackDownloadkey);
			trackedDownloadsModel.addRow(trackedDownloadTableMember);
		}
	}

	private void addKeysButton_actionPerformed(final ActionEvent event) {
		// Open choose Directory dialog
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(new File(Core.frostSettings.getDefaultValue(Settings.DIR_DOWNLOAD)));
		fileChooser.setDialogTitle(language.getString("AddNewDownloadsDialog.changeDirDialog.title"));
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setAcceptAllFileFilterUsed(false);
		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		final File selectedFile = fileChooser.getSelectedFile();
		try (FileReader fileReader = new FileReader(selectedFile);
				BufferedReader bufferedReader = new BufferedReader(fileReader);) {
			String strLine;
			while ((strLine = bufferedReader.readLine()) != null) {
				if (strLine.startsWith("CHK@") && FreenetKeys.isValidKey(strLine)) {
					final String fileName = strLine.substring(strLine.lastIndexOf("/") + 1);
					trackDownloadKeysStorage.storeItem(new TrackDownloadKeys(strLine, fileName, "",
							selectedFile.length(), System.currentTimeMillis()));
				}
			}
		} catch (final IOException e) {
			logger.error("IOException", e);
		}
		loadTrackedDownloadsIntoTable();
	}

	private void maxAgeButton_actionPerformed(final ActionEvent e) {
		int max_age = 4;
		try {
			max_age = Integer.parseInt(maxAgeTextField.getText());
		} catch( final NumberFormatException ex ) {
			return;
		}

		if( max_age < 0) {
			return;
		}

		trackDownloadKeysStorage.cleanupTable(max_age);
		loadTrackedDownloadsIntoTable();
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		Boolean isSelected = trackedDownloadsTable.getSelectedRowCount() > 0;
		Boolean isOneRowSelected = trackedDownloadsTable.getSelectedRowCount() == 1;

		removeDownloadAction.setEnabled(isSelected);
		removeDownloadSameBoardAction.setEnabled(isOneRowSelected);
	}

	private static class TrackedDownloadsModel extends SortedTableModel<TrackedDownloadTableMember> {

		private static final long serialVersionUID = 1L;

		private transient Language language;

		protected final static String columnNames[] = new String[5];

		protected final static Class<?> columnClasses[] =  {
			String.class,
			String.class,
			String.class,
			String.class,
			String.class
		};

		public TrackedDownloadsModel() {
			super();
			assert columnClasses.length == columnNames.length;
			language = Language.getInstance();
			refreshLanguage();
		}

		private void refreshLanguage() {
			columnNames[0] = language.getString("ManageDownloadTrackingDialog.table.name");
			columnNames[1] = language.getString("ManageDownloadTrackingDialog.table.key");
			columnNames[2] = language.getString("ManageDownloadTrackingDialog.table.board");
			columnNames[3] = language.getString("ManageDownloadTrackingDialog.table.size");
			columnNames[4] = language.getString("ManageDownloadTrackingDialog.table.finished");
		}

		public boolean isCellEditable(int row, int col) {
			return false;
		}

		public String getColumnName(int column) {
			if( (column >= 0) && (column < columnNames.length) ) {
                return columnNames[column];
            }
			return null;
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public Class<?> getColumnClass(int column) {
			if( (column >= 0) && (column < columnClasses.length) ) {
                return columnClasses[column];
            }
			return null;
		}
	}

	private class TrackedDownloadTableMember extends TableMember.BaseTableMember<TrackedDownloadTableMember> {

		TrackDownloadKeys trackDownloadKey;

		public TrackedDownloadTableMember(final TrackDownloadKeys trackDownloadkey){
			trackDownloadKey = trackDownloadkey;
		}

		public Comparable<?> getValueAt(final int column) {
			switch( column ) {
				case 0:
					return trackDownloadKey.getFileName();
				case 1:
					return trackDownloadKey.getChkKey();
				case 2:
					return trackDownloadKey.getBoardName();
				case 3:
					return FormatterUtils.formatSize(trackDownloadKey.getFileSize());
				case 4:
					final long date = trackDownloadKey.getDownloadFinishedTime();
					return DateFun.FORMAT_DATE_TIME_VISIBLE.format(Instant.ofEpochMilli(date));
				default :
					throw new RuntimeException("Unknown Column pos");
			}
		}

		public TrackDownloadKeys getTrackDownloadKeys() {
			return trackDownloadKey;
		}
	}

	private class TrackedDownloadsTable extends SortedTable<TrackedDownloadTableMember> {

		private static final long serialVersionUID = 1L;

		private transient final TableCellRenderer sizeColumnRenderer;

		public TrackedDownloadsTable(final TrackedDownloadsModel trackDownloadsModel) {
			super(trackDownloadsModel);
			setIntercellSpacing(new Dimension(5, 1));

			sizeColumnRenderer = new SizeColumnTableCellRenderer();
		}

		public String getToolTipText(final MouseEvent mouseEvent) {
			final Point point = mouseEvent.getPoint();
			final int rowIndex = rowAtPoint(point);
			final int colIndex = columnAtPoint(point);
			final int realColumnIndex = convertColumnIndexToModel(colIndex);
			final TableModel tableModel = getModel();
			return tableModel.getValueAt(rowIndex, realColumnIndex).toString();
		}

		public TableCellRenderer getCellRenderer(final int row, final int column) {
			if(column == 3) {
				return sizeColumnRenderer;
			}
			return super.getCellRenderer(row, column);
		}

		private class SizeColumnTableCellRenderer extends JLabel implements TableCellRenderer {

			private static final long serialVersionUID = 1L;

			public Component getTableCellRendererComponent(final JTable table,
					final Object value, final boolean isSelected, final boolean hasFocus,
					final int row, final int column) {
				setText(value.toString());
				setHorizontalAlignment(SwingConstants.RIGHT);
				return this;
			}
		}
	}

	private class RemoveDownloadAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			int[] selectedRows = trackedDownloadsTable.getSelectedRows();
			for (int z = selectedRows.length - 1; z > -1; z--) {
				TrackedDownloadTableMember row = trackedDownloadsModel.getRow(selectedRows[z]);
				trackDownloadKeysStorage.removeItemByKey(row.getTrackDownloadKeys().getChkKey());
				trackedDownloadsModel.deleteRow(row);
			}
			trackedDownloadsTable.clearSelection();
		}
	}

	private class RemoveDownloadSameBoardAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			int rowIdx = trackedDownloadsTable.getSelectedRow();
			TrackedDownloadTableMember selectedRow = trackedDownloadsModel.getRow(rowIdx);
			String boardName = selectedRow.getTrackDownloadKeys().getBoardName();

			for (int z = trackedDownloadsModel.getRowCount() - 1; z >= 0; z--) {
				TrackedDownloadTableMember row = trackedDownloadsModel.getRow(z);
				if (boardName.equals(row.getTrackDownloadKeys().getBoardName())) {
					trackDownloadKeysStorage.removeItemByKey(row.getTrackDownloadKeys().getChkKey());
					trackedDownloadsModel.deleteRow(row);
				}
			}
			trackedDownloadsTable.clearSelection();
		}
	}

	private class PopupMenuTrackDownloads extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		public PopupMenuTrackDownloads() {
			removeDownloadAction.setText(language.getString("ManageDownloadTrackingDialog.button.remove"));
			removeDownloadSameBoardAction
					.setText(language.getString("ManageDownloadTrackingDialog.button.removeSameBoard"));

			add(removeDownloadAction);
			addSeparator();
			add(removeDownloadSameBoardAction);
		}
	}
}
