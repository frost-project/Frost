/*
  IdentitiesBrowser.java / Frost
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fileTransfer.common.TableBackgroundColors;
import frost.gui.model.SortedTableModel;
import frost.gui.model.TableMember;
import frost.identities.Identity;
import frost.messaging.frost.boards.Board;
import frost.storage.IdentitiesXmlDAO;
import frost.storage.perst.identities.IdentitiesStorage;
import frost.util.ClipboardUtil;
import frost.util.DateFun;
import frost.util.Mixed;
import frost.util.gui.FrostSwingWorker;
import frost.util.gui.MiscToolkit;
import frost.util.gui.SelectRowOnRightClick;
import frost.util.gui.SimplePopupMenuListener;
import frost.util.gui.action.BaseAction;
import frost.util.gui.translation.Language;

public class IdentitiesBrowser extends JDialog implements SimplePopupMenuListener {

	private static final long serialVersionUID = 1L;

	private static final Logger logger =  LoggerFactory.getLogger(IdentitiesBrowser.class);

	private transient Language language;

	private JPanel contentPane = null;
	private JScrollPane identitiesScrollPane = null;
	private SortedTable<InnerTableMember> identitiesTable = null;
	private JPanel filterPanel = null;
	private JPanel mainPanel = null;
	private JButton closeButton = null;
	private JButton markGOODButton = null;
	private JButton markOBSERVEButton = null;
	private JButton markCHECKButton = null;
	private JButton markBADButton = null;
	private JButton deleteButton = null;

	private InnerTableModel identitiesTableModel = null;

	private final JFrame parent;

	private transient List<InnerTableMember> allTableMembers;

	private final boolean showColoredLines;

	private ArrayList<Identity> selectedIds;
	private CopyUniqueNameAction copyUniqueNameAction;
	private CopyUniqueNameAndPublicKeyAction copyUniqueNameAndPublicKeyAction;
	private ExportSelectedIdentitiesAction exportSelectedIdentitiesAction;
	private PopupMenu popupMenu;

	private final long minCleanupTime;

	private transient ProgressMonitor progressMonitor;

	private JTextField lookupTextField = null;

	private JTextField filterTextField = null;

	private JButton importButton = null;

	private JButton exportButton = null;

	private JButton cleanupButton = null;

	private JTextField cleanupLastSeenTextField = new JTextField(10);

	private JTextField cleanupReveivedMessageCountTextField = new JTextField(10);

	/**
	 * This is the default constructor
	 */
	public IdentitiesBrowser(final JFrame parent) {
		super(parent);
		this.parent = parent;
		language = Language.getInstance();
		minCleanupTime = getMinCleanupTime();
		setModal(true);
		showColoredLines = Core.frostSettings.getBoolean(Settings.SHOW_COLORED_ROWS);
		initialize();


		setLocationRelativeTo(parent);
	}

	/**
	 * This method initializes this
	 */
	private void initialize() {
		setBounds(new Rectangle(0, 0, 630, 420));
		setContentPane(getJContentPane());

		getMarkGOODButton().setText("");
		getMarkBADButton().setText("");
		getMarkCHECKButton().setText("");
		getMarkOBSERVEButton().setText("");

		getMarkGOODButton().setIcon(MiscToolkit.loadImageIcon("/data/toolbar/weather-clear.png"));
		getMarkOBSERVEButton().setIcon(MiscToolkit.loadImageIcon("/data/toolbar/weather-few-clouds.png"));
		getMarkCHECKButton().setIcon(MiscToolkit.loadImageIcon("/data/toolbar/weather-overcast.png"));
		getMarkBADButton().setIcon(MiscToolkit.loadImageIcon("/data/toolbar/weather-storm.png"));

		MiscToolkit.configureButton(getMarkGOODButton(), "MessagePane.toolbar.tooltip.setToGood", language);
		MiscToolkit.configureButton(getMarkBADButton(), "MessagePane.toolbar.tooltip.setToBad", language);
		MiscToolkit.configureButton(getMarkCHECKButton(), "MessagePane.toolbar.tooltip.setToCheck", language);
		MiscToolkit.configureButton(getMarkOBSERVEButton(), "MessagePane.toolbar.tooltip.setToObserve", language);

		setTitle(language.getString("IdentitiesBrowser.title"));
		getDeleteButton().setText(language.getString("IdentitiesBrowser.button.delete"));
		getCleanupButton().setText(language.getString("IdentitiesBrowser.button.cleanup"));
		getCleanupButton().setToolTipText(language.getString("IdentitiesBrowser.button.cleanup.tooltip"));
		getCloseButton().setText(language.getString("IdentitiesBrowser.button.close"));
		getImportButton().setText(language.getString("IdentitiesBrowser.button.import"));
		getExportButton().setText(language.getString("IdentitiesBrowser.button.export"));

		copyUniqueNameAction = new CopyUniqueNameAction();
		copyUniqueNameAndPublicKeyAction = new CopyUniqueNameAndPublicKeyAction();
		exportSelectedIdentitiesAction = new ExportSelectedIdentitiesAction();
		popupMenu = new PopupMenu();
		popupMenu.addPopupMenuListener(this);
		identitiesTable.setComponentPopupMenu(popupMenu);
		identitiesTable.addMouseListener(new SelectRowOnRightClick(identitiesTable));
	}

	private void updateTitle() {
		final int idCount = identitiesTableModel.getRowCount();
		setTitle(language.formatMessage("IdentitiesBrowser.title", Integer.toString(idCount)));
	}

	/**
	 * This method initializes contentPane
	 *
	 * @return JPanel
	 */
	private JPanel getJContentPane() {
		if( contentPane == null ) {
			contentPane = new JPanel();
			contentPane.setLayout(new BorderLayout());
			contentPane.add(getFilterPanel(), BorderLayout.SOUTH);
			contentPane.add(getMainPanel(), BorderLayout.CENTER);
		}
		return contentPane;
	}

	/**
	 * This method initializes identitiesScrollPane
	 *
	 * @return JScrollPane
	 */
	private JScrollPane getIdentitiesScrollPane() {
		if( identitiesScrollPane == null ) {
			identitiesScrollPane = new JScrollPane();
			identitiesScrollPane.setViewportView(getIdentitiesTable());
			identitiesScrollPane.setWheelScrollingEnabled(true);
		}
		return identitiesScrollPane;
	}

	/**
	 * Compute expire earliest time of any board.
	 * Cleanup could remove BAD identities when they are not seen since than the returned value.
	 *
	 * @return  maximum backload days of any board
	 */
	private long getMinCleanupTime() {
		// take maximum
		int minDaysOld = Core.frostSettings.getInteger(Settings.MESSAGE_EXPIRE_DAYS) + 1;

		if( minDaysOld < Core.frostSettings.getInteger(Settings.MAX_MESSAGE_DISPLAY) ) {
			minDaysOld = Core.frostSettings.getInteger(Settings.MAX_MESSAGE_DISPLAY) + 1;
		}
		if( minDaysOld < Core.frostSettings.getInteger(Settings.MAX_MESSAGE_DOWNLOAD) ) {
			minDaysOld = Core.frostSettings.getInteger(Settings.MAX_MESSAGE_DOWNLOAD) + 1;
		}

		for( final Board board : MainFrame.getInstance().getMessagingTab().getTofTreeModel().getAllBoards() ) {

			if( board.isConfigured() ) {
				minDaysOld = Math.max(board.getMaxMessageDisplay(), minDaysOld);
				minDaysOld = Math.max(board.getMaxMessageDownload(), minDaysOld);
			}
		}

		final long time = System.currentTimeMillis() - (minDaysOld * 24L * 60L * 60L * 1000L);
		return time;
	}

	/**
	 * This method initializes identitiesTable
	 *
	 * @return JTable
	 */
	private SortedTable<InnerTableMember> getIdentitiesTable() {
		if( identitiesTable == null ) {
			identitiesTableModel = new InnerTableModel();
			identitiesTable = new SortedTable<InnerTableMember>(identitiesTableModel);
			// set column sizes
			final int[] widths = { 130, 30, 30, 70, 20, 20 };
			for (int i = 0; i < widths.length; i++) {
				identitiesTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
			}

			final ShowColoredLinesRenderer showColoredLinesRenderer = new ShowColoredLinesRenderer();
			identitiesTable.getColumnModel().getColumn(0).setCellRenderer(showColoredLinesRenderer);
			identitiesTable.getColumnModel().getColumn(1).setCellRenderer(new StringCellRenderer());
			identitiesTable.getColumnModel().getColumn(2).setCellRenderer(showColoredLinesRenderer);
			identitiesTable.getColumnModel().getColumn(3).setCellRenderer(showColoredLinesRenderer);
			identitiesTable.getColumnModel().getColumn(4).setCellRenderer(showColoredLinesRenderer);
			identitiesTable.getColumnModel().getColumn(5).setCellRenderer(showColoredLinesRenderer);

			identitiesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(final ListSelectionEvent e) {
					final int[] selRows = identitiesTable.getSelectedRows();

					// nothing selected
					if( selRows.length == 0 ) {
						// disable all
						getDeleteButton().setEnabled(false);
						getDeleteButton().setToolTipText(null);
						updateStateButtons(false, false, false, false);
						return;
					}

					if( selRows.length == 1 ) {
						// one selected: enable good,bad,... buttons, disable button with current state
						final Identity id = identitiesTableModel.getRow(selRows[0]).getIdentity();
						// setting all together avoids flickering buttons
						if( id.isBAD() ) {
							updateStateButtons(false, true, true, true);
						} else if( id.isCHECK() ) {
							updateStateButtons(true, false, true, true);
						} else if( id.isGOOD() ) {
							updateStateButtons(true, true, false, true);
						} else if( id.isOBSERVE() ) {
							updateStateButtons(true, true, true, false);
						}
					} else {
						// multiple selected: enable all buttons
						updateStateButtons(true, true, true, true);
					}

					// if one in selection has more than 0 msgs / files, disable delete button
					boolean enableDelete = true;
					for( final int element : selRows ) {
						if( identitiesTableModel.getRow(element).isDeleteable() == false ) {
							enableDelete = false;
							break;
						}
					}
					if( enableDelete ) {
						if( selRows.length > 1 ) {
							getDeleteButton().setToolTipText(language.getString("IdentitiesBrowser.button.delete.tooltip.multiple"));
						} else {
							getDeleteButton().setToolTipText(language.getString("IdentitiesBrowser.button.delete.tooltip.single"));
						}
					} else {
						getDeleteButton().setToolTipText(null);
					}
					getDeleteButton().setEnabled(enableDelete);
				}
			});
		}
		return identitiesTable;
	}

	private void updateStateButtons(final boolean badState, final boolean checkState, final boolean goodState, final boolean observeState) {
		getMarkBADButton().setEnabled(badState);
		getMarkCHECKButton().setEnabled(checkState);
		getMarkGOODButton().setEnabled(goodState);
		getMarkOBSERVEButton().setEnabled(observeState);
	}

	/**
	 * This method initializes filterPanel
	 *
	 * @return JPanel
	 */
	private JPanel getFilterPanel() {
		if( filterPanel == null ) {

			Dimension rigidAreaDimension = new Dimension(5,3);

			filterPanel = new JPanel();
			filterPanel.setLayout(new BoxLayout(getFilterPanel(), BoxLayout.X_AXIS));
			filterPanel.add(Box.createRigidArea(rigidAreaDimension));
			filterPanel.add(
					new JLabel(language.getString("IdentitiesBrowser.label.lookup")+":")
			);

			filterPanel.add(Box.createRigidArea(rigidAreaDimension));
			filterPanel.add(getLookupTextField(), null);

			filterPanel.add(Box.createRigidArea(rigidAreaDimension));
			filterPanel.add(
					new JLabel(language.getString("IdentitiesBrowser.label.filter")+":")
			);

			filterPanel.add(Box.createRigidArea(rigidAreaDimension));
			filterPanel.add(getFilterTextField(), null);

			filterPanel.add(Box.createHorizontalGlue() );

			filterPanel.add(getCloseButton(), null);
			filterPanel.add(Box.createRigidArea(new Dimension(2,3)));
		}
		return filterPanel;
	}

	/**
	 * This method initializes mainPanel
	 *
	 * @return JPanel
	 */
	private JPanel getMainPanel() {
		if( mainPanel == null ) {
			mainPanel = new JPanel();
			mainPanel.setLayout(new GridBagLayout());

			// Identity list
			final GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.fill = GridBagConstraints.BOTH;
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.gridheight = 9;
			gridBagConstraints.insets = new Insets(5, 5, 5, 5);
			mainPanel.add(getIdentitiesScrollPane(), gridBagConstraints);

			// Trust buttons
			final GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 1;
			gridBagConstraints1.insets = new Insets(5, 2, 5, 2);
			gridBagConstraints1.gridy = 0;
			mainPanel.add(getMarkGOODButton(), gridBagConstraints1);

			gridBagConstraints1.gridx += 1;
			mainPanel.add(getMarkOBSERVEButton(), gridBagConstraints1);

			gridBagConstraints1.gridx += 1;
			mainPanel.add(getMarkCHECKButton(), gridBagConstraints1);

			gridBagConstraints1.gridx += 1;
			// gridBagConstraints1.insets = new Insets(5, 0, 0, 5);
			mainPanel.add(getMarkBADButton(), gridBagConstraints1);

			// Button list
			final GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridx = 1;
			gridBagConstraints5.gridwidth = 4;
			gridBagConstraints5.weighty = 0.0;
			gridBagConstraints5.anchor = GridBagConstraints.NORTH;
			gridBagConstraints5.insets = new Insets(8, 5, 5, 5);
			gridBagConstraints5.gridy = 1;
			mainPanel.add(getDeleteButton(), gridBagConstraints5);

			gridBagConstraints5.gridy += 1;
			mainPanel.add(getImportButton(), gridBagConstraints5);

			gridBagConstraints5.gridy += 1;
			mainPanel.add(getExportButton(), gridBagConstraints5);

			// Cleanup
			gridBagConstraints5.gridy += 1;
			gridBagConstraints5.insets = new Insets(20, 5, 0, 5);
			String minDays = Long.toString((DateFun.toStartOfDayInMilli(OffsetDateTime.now(DateFun.getTimeZone()))
					- DateFun.toStartOfDayInMilli(minCleanupTime, DateFun.getTimeZone())) / (1000 * 60 * 60 * 24) + 1);

			JLabel clenupLastSeenLabel = new JLabel(language.formatMessage("IdentitiesBrowser.cleanup.lastSeenLabel.text", minDays));
			clenupLastSeenLabel.setToolTipText(language.getString("IdentitiesBrowser.cleanup.lastSeenLabel.toolTip"));
			mainPanel.add(clenupLastSeenLabel, gridBagConstraints5);

			gridBagConstraints5.gridy += 1;
			gridBagConstraints5.insets = new Insets(3, 5, 0, 5);
			gridBagConstraints5.fill = GridBagConstraints.BOTH;
			cleanupLastSeenTextField.setToolTipText(language.getString("IdentitiesBrowser.cleanup.lastSeenLabel.toolTip"));
			cleanupLastSeenTextField.setText(minDays);
			mainPanel.add(cleanupLastSeenTextField,gridBagConstraints5);

			gridBagConstraints5.gridy += 1;
			JLabel cleanupReceivedMessagesCountLabel = new JLabel(language.getString("IdentitiesBrowser.cleanup.receivedMessageCountLabel.text"));
			cleanupReceivedMessagesCountLabel.setToolTipText(language.getString("IdentitiesBrowser.cleanup.receivedMessageCountLabel.toolTip"));
			mainPanel.add(cleanupReceivedMessagesCountLabel, gridBagConstraints5);

			gridBagConstraints5.gridy += 1;
			cleanupReveivedMessageCountTextField.setToolTipText(language.getString("IdentitiesBrowser.cleanup.receivedMessageCountLabel.toolTip"));
			cleanupReveivedMessageCountTextField.setText("50");
			mainPanel.add(cleanupReveivedMessageCountTextField, gridBagConstraints5);

			gridBagConstraints5.gridy += 1;
			gridBagConstraints5.weighty = 1.0;
			gridBagConstraints5.fill = GridBagConstraints.NONE;
			mainPanel.add(getCleanupButton(), gridBagConstraints5);
		}
		return mainPanel;
	}


	/**
	 * This method initializes closeButton
	 *
	 * @return JButton
	 */
	private JButton getCloseButton() {
		if( closeButton == null ) {
			closeButton = new JButton();
			closeButton.setText("IdentitiesBrowser.button.close");
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					// update messages if a board is shown
					MainFrame.getInstance().getMessagePanel().updateTableAfterChangeOfIdentityState();
				}
			});
		}
		return closeButton;
	}

	/**
	 * This method initializes markGOODButton
	 *
	 * @return JButton
	 */
	private JButton getMarkGOODButton() {
		if( markGOODButton == null ) {
			markGOODButton = new JButton();
			markGOODButton.setText("G");
			markGOODButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final int[] selRows = getIdentitiesTable().getSelectedRows();
					for( final int element : selRows ) {
						final InnerTableMember itm = identitiesTableModel.getRow(element);
						final Identity id = itm.getIdentity();
						if( id.isGOOD() == false ) {
							id.setGOOD();
						}
						identitiesTableModel.updateRow(itm);
					}
				}
			});
		}
		return markGOODButton;
	}

	/**
	 * This method initializes markOBSERVEButton
	 *
	 * @return JButton
	 */
	private JButton getMarkOBSERVEButton() {
		if( markOBSERVEButton == null ) {
			markOBSERVEButton = new JButton();
			markOBSERVEButton.setText("O");
			markOBSERVEButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final int[] selRows = getIdentitiesTable().getSelectedRows();
					for( final int element : selRows ) {
						final InnerTableMember itm = identitiesTableModel.getRow(element);
						final Identity id = itm.getIdentity();
						if( id.isOBSERVE() == false ) {
							id.setOBSERVE();
						}
						identitiesTableModel.updateRow(itm);
					}
				}
			});
		}
		return markOBSERVEButton;
	}

	/**
	 * This method initializes markCHECKButton
	 *
	 * @return JButton
	 */
	private JButton getMarkCHECKButton() {
		if( markCHECKButton == null ) {
			markCHECKButton = new JButton();
			markCHECKButton.setText("C");
			markCHECKButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final int[] selRows = getIdentitiesTable().getSelectedRows();
					for( final int element : selRows ) {
						final InnerTableMember itm = identitiesTableModel.getRow(element);
						final Identity id = itm.getIdentity();
						if( id.isCHECK() == false ) {
							id.setCHECK();
						}
						identitiesTableModel.updateRow(itm);
					}
				}
			});
		}
		return markCHECKButton;
	}

	/**
	 * This method initializes markBADButton
	 *
	 * @return JButton
	 */
	private JButton getMarkBADButton() {
		if( markBADButton == null ) {
			markBADButton = new JButton();
			markBADButton.setText("B");
			markBADButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final int[] selRows = getIdentitiesTable().getSelectedRows();
					for( final int element : selRows ) {
						final InnerTableMember itm = identitiesTableModel.getRow(element);
						final Identity id = itm.getIdentity();
						if( id.isBAD() == false ) {
							id.setBAD();
						}
						identitiesTableModel.updateRow(itm);
					}
				}
			});
		}
		return markBADButton;
	}

	/**
	 * This method initializes deleteButton
	 *
	 * @return JButton
	 */
	private JButton getDeleteButton() {
		if( deleteButton == null ) {
			deleteButton = new JButton();
			deleteButton.setText("IdentitiesBrowser.button.delete");
			deleteButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final int[] selRows = getIdentitiesTable().getSelectedRows();
					final int answer = JOptionPane.showConfirmDialog(
							IdentitiesBrowser.this,
							language.formatMessage("IdentitiesBrowser.deleteDialog.body", Integer.toString(selRows.length)),
							language.getString("IdentitiesBrowser.deleteDialog.title"),
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE);
					if( answer == JOptionPane.NO_OPTION ) {
						return;
					}
					Arrays.sort(selRows); // ensure sorted, we must delete from end to begin
					if( !IdentitiesStorage.inst().beginExclusiveThreadTransaction() ) {
						return;
					}
					for( int x=selRows.length-1; x>=0; x-- ) {
						final InnerTableMember m = identitiesTableModel.getRow(selRows[x]);
						final Identity id = m.getIdentity();
						Core.getIdentitiesManager().deleteIdentity(id);
						identitiesTableModel.removeRow(selRows[x]);
					}
					IdentitiesStorage.inst().endThreadTransaction();
					updateTitle();
				}
			});
		}
		return deleteButton;
	}

	private class InnerTableMember implements TableMember<InnerTableMember> {

		Identity identity;
		Integer msgCount;
		Integer fileCount;
		String lastSeenStr;
		String htmlName;
		Integer receivedMsgs;

		public InnerTableMember(final Identity i, final Hashtable<String,IdentitiesStorage.IdentityMsgAndFileCount> idDatas) {
			identity = i;

			final IdentitiesStorage.IdentityMsgAndFileCount data = idDatas.get(identity.getUniqueName());
			if( data != null ) {
				msgCount = data.getMessageCount();
				fileCount = data.getFileCount();
			} else {
				// error
				msgCount = -1;
				fileCount = -1;
			}
			lastSeenStr = buildLastSeenString(identity.getLastSeenTimestamp());
			receivedMsgs = identity.getReceivedMessageCount();
			htmlName = buildHtmlName(i.getUniqueName());
		}
		public Identity getIdentity() {
			return identity;
		}
		public boolean isDeleteable() {
			if( (msgCount.intValue() == 0) && (fileCount.intValue() == 0) ) {
				return true;
			}
			return false;
		}
		public boolean isCleanupable() {
			if( !isDeleteable() ) {
				return false;
			}
			// always keep identities marked GOOD and OBSERVE
			if( identity.isGOOD() || identity.isOBSERVE()) {
				return false;
			}
			// keep identities marked BAD, if not expired
			if( identity.isBAD() && (identity.getLastSeenTimestamp() > minCleanupTime) ) {
				return false;
			}

			return true;
		}

		private String buildHtmlName(final String n) {
			// TODO: HTML mode wraps words with blanks, maybe replace blanks by &nbsp;
			String a = n.substring(0, n.indexOf("@"));
			String b = n.substring(n.indexOf("@"));
			String r = "<html><b>" + a + "</b>" + b + "</html>";
			logger.debug("r = {}", r);
			// return r;
			return n;
		}

		private String buildLastSeenString(final long lastSeen) {
			// date (days_before)
			if( lastSeen < 0 ) {
				// not set!
				return "";
			}
			String lsStr = DateFun.FORMAT_DATE_EXT.format(Instant.ofEpochMilli(lastSeen));
			long days = DateFun.toStartOfDayInMilli(OffsetDateTime.now(DateFun.getTimeZone()))
					- DateFun.toStartOfDayInMilli(lastSeen, DateFun.getTimeZone());
			days /= 1000L * 60L * 60L * 24L;
			lsStr += "  ("+days+")";

			return lsStr;
		}

		public Comparable<?> getValueAt(final int column) {
			switch(column) {
			case 0: return htmlName;
			case 1: return getIdentity().getStateString();
			case 2: return receivedMsgs;
			case 3: return lastSeenStr;
			case 4: return msgCount;
			case 5: return fileCount;
			}
			return "*ERR*";
		}

		public int compareTo(final InnerTableMember anOther, final int tableColumnIndex) {
			if( (tableColumnIndex == 0) || (tableColumnIndex == 1) ) {
				final String s1 = (String)getValueAt(tableColumnIndex);
				final String s2 = (String)anOther.getValueAt(tableColumnIndex);
				return s1.compareToIgnoreCase(s2);
			}
			if( tableColumnIndex == 2 ) {
				final int l1 = getIdentity().getReceivedMessageCount();
				final int l2 = anOther.getIdentity().getReceivedMessageCount();
				return Mixed.compareLong(l1, l2);
			}
			if( tableColumnIndex == 3 ) {
				final long l1 = getIdentity().getLastSeenTimestamp();
				final long l2 = anOther.getIdentity().getLastSeenTimestamp();
				return Mixed.compareLong(l1, l2);
			}
			if( tableColumnIndex == 4 ) {
				Integer i1 = (Integer)getValueAt(tableColumnIndex);
				Integer i2 = (Integer)anOther.getValueAt(tableColumnIndex);
				final int res = i1.compareTo(i2);
				if( res == 0) {
					// same msgcount, compare filecount
					i1 = (Integer)getValueAt(5);
					i2 = (Integer)anOther.getValueAt(5);
					return i1.compareTo(i2);
				}
				return res;
			}
			if( tableColumnIndex == 5 ) {
				Integer i1 = (Integer)getValueAt(tableColumnIndex);
				Integer i2 = (Integer)anOther.getValueAt(tableColumnIndex);
				final int res = i1.compareTo(i2);
				if( res == 0) {
					// same filecount, compare msgcount
					i1 = (Integer)getValueAt(4);
					i2 = (Integer)anOther.getValueAt(4);
					return i1.compareTo(i2);
				}
				return res;
			}
			return 0;
		}
	}

	private class InnerTableModel extends SortedTableModel<InnerTableMember> {

		private static final long serialVersionUID = 1L;

		protected final String columnNames[] = new String[6];

		protected final Class<?> columnClasses[] = {
				String.class, // name
				String.class, // state
				Integer.class, // received msgs
				String.class, // lastSeen,
				Integer.class, // msgs
				Integer.class  // files
		};

		public InnerTableModel() {
			super();
			setLanguage();
		}

		@Override
		public boolean isCellEditable(final int row, final int col) {
			return false;
		}

		@Override
		public String getColumnName(final int column) {
			if( (column >= 0) && (column < columnNames.length) ) {
				return columnNames[column];
			}
			return null;
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public Class<?> getColumnClass(final int column) {
			if( (column >= 0) && (column < columnClasses.length) ) {
				return columnClasses[column];
			}
			return null;
		}

		private void setLanguage() {
			columnNames[0] = language.getString("IdentitiesBrowser.identitiesTable.name");
			columnNames[1] = language.getString("IdentitiesBrowser.identitiesTable.state");
			columnNames[2] = language.getString("IdentitiesBrowser.identitiesTable.receivedMessages");
			columnNames[3] = language.getString("IdentitiesBrowser.identitiesTable.lastSeen");
			columnNames[4] = language.getString("IdentitiesBrowser.identitiesTable.messages");
			columnNames[5] = language.getString("IdentitiesBrowser.identitiesTable.files");
		}
	}

	private class StringCellRenderer extends ShowColoredLinesRenderer {

		private static final long serialVersionUID = 1L;

		private Font boldFont = null;
		private Font normalFont = null;
		private final Color col_good    = new Color(0x00, 0x80, 0x00);
		private final Color col_check   = new Color(0xFF, 0xCC, 0x00);
		private final Color col_observe = new Color(0x00, 0xD0, 0x00);
		private final Color col_bad     = new Color(0xFF, 0x00, 0x00);

		public StringCellRenderer() {
			final Font baseFont = getIdentitiesTable().getFont();
			normalFont = baseFont.deriveFont(Font.PLAIN);
			boldFont = baseFont.deriveFont(Font.BOLD);

			setVerticalAlignment(CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(
				final JTable table,
				final Object value,
				final boolean isSelected,
				final boolean hasFocus,
				final int row,
				int column) {

			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			setAlignmentY(CENTER_ALIGNMENT);

			final InnerTableMember tableMember = identitiesTableModel.getRow(row);

			// get the original model column index (maybe columns were reordered by user)
			final TableColumn tableColumn = getIdentitiesTable().getColumnModel().getColumn(column);
			column = tableColumn.getModelIndex();

			// defaults
			setFont(normalFont);
			if (!isSelected) {
				setForeground(Color.BLACK);
			}
			setToolTipText(null);

			if( column == 0 ) {
				setToolTipText(tableMember.getIdentity().getUniqueName());
			} else if( column == 1 ) {
				final Identity id = tableMember.getIdentity();
				// STATE
				// state == good/bad/check/observe -> bold and colored
				if (Core.getIdentitiesManager().isMySelf(id.getUniqueName())) {
					if( !Core.frostSettings.getBoolean(Settings.SHOW_OWN_MESSAGES_AS_ME_DISABLED) ) {
						setText("ME");
					}
					setFont(boldFont);
					setForeground(col_good);
				} else if( id.isGOOD() ) {
					setFont(boldFont);
					setForeground(col_good);
				} else if( id.isCHECK() ) {
					setFont(boldFont);
					setForeground(col_check);
				} else if( id.isOBSERVE() ) {
					setFont(boldFont);
					setForeground(col_observe);
				} else if( id.isBAD() ) {
					setFont(boldFont);
					setForeground(col_bad);
				}
			}
			return this;
		}
	}

	private void startProgressMonitor(final int max) {
		final String title = language.getString("IdentitiesBrowser.progressDialog.title");
		final String msg = language.getString("IdentitiesBrowser.progressDialog.body");
		UIManager.put("ProgressMonitor.progressText", title);
		progressMonitor = new ProgressMonitor(parent, msg, null, 0, max);
		//        progressMonitor.setNote(0+"/"+max);
	}

	public void startDialog() {

		startProgressMonitor(3);

		// disables mainframe
		final FrostSwingWorker worker = new FrostSwingWorker(parent) {
			@Override
			protected void doNonUILogic() throws RuntimeException {

				progressMonitor.setProgress(1);
				if( progressMonitor.isCanceled() ) {
					return;
				}

				// query ALL data for all identities, each InnerTableMember gets its values from the complete list
				Hashtable<String,IdentitiesStorage.IdentityMsgAndFileCount> idDatas;
				if( !IdentitiesStorage.inst().beginExclusiveThreadTransaction() ) {
					return;
				}

				idDatas = IdentitiesStorage.inst().retrieveMsgAndFileCountPerIdentity();

				IdentitiesStorage.inst().endThreadTransaction();

				progressMonitor.setProgress(2);
				if( progressMonitor.isCanceled() ) {
					return;
				}

				allTableMembers = new LinkedList<InnerTableMember>(); // remember all table data for filter
				final List<Identity> allIdentities = Core.getIdentitiesManager().getIdentities();
				// show own identities also
				allIdentities.addAll(Core.getIdentitiesManager().getLocalIdentities());
				for( final Identity identity : allIdentities ) {
					final InnerTableMember memb = new InnerTableMember(identity, idDatas);
					identitiesTableModel.addRow(memb);
					allTableMembers.add(memb);
				}

				progressMonitor.setProgress(3);

				idDatas.clear();
			}
			@Override
			protected void doUIUpdateLogic() throws RuntimeException {
				updateTitle();
				showDialog();
			}
		};
		worker.start();
	}

	private void showDialog() {
		if( progressMonitor.isCanceled() ) {
			progressMonitor.close();
		} else {
			progressMonitor.close();
			getIdentitiesTable().getSelectionModel().setSelectionInterval(0, 0);
			setVisible(true);
		}
	}

	/**
	 * This method initializes cleanupButton
	 *
	 * @return JButton
	 */
	private JButton getCleanupButton() {
		if( cleanupButton == null ) {
			cleanupButton = new JButton();
			cleanupButton.setText("IdentitiesBrowser.button.cleanup");
			cleanupButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {

					long minLastSeenTimestamp = 0L;
					int minReceivedMessageCount = 0;
					try {
						minLastSeenTimestamp = System.currentTimeMillis() - (Long.parseLong(cleanupLastSeenTextField.getText()) * 24L * 60L * 60L * 1000L);
						minReceivedMessageCount =  Integer.parseInt(cleanupReveivedMessageCountTextField.getText());
					} catch(NumberFormatException ex) {
						JOptionPane.showMessageDialog(
							IdentitiesBrowser.this,
							language.getString("IdentitiesBrowser.cleanupDialog.nothingToDelete.body"),
							language.getString("IdentitiesBrowser.cleanupDialog.nothingToDelete.title"),
							JOptionPane.INFORMATION_MESSAGE
						);
						return;
					}

					final LinkedList<Integer> li = new LinkedList<Integer>();
					for( int i=identitiesTableModel.getRowCount()-1; i >= 0; i-- ) {
						final InnerTableMember m = identitiesTableModel.getRow(i);
						final Identity id = m.getIdentity();
						if( m.isCleanupable() && (id.getReceivedMessageCount() <= minReceivedMessageCount) && (id.getLastSeenTimestamp() < minLastSeenTimestamp) ) {
							li.add(i);
						}
					}
					if( li.size() == 0 ) {
						JOptionPane.showMessageDialog(
								IdentitiesBrowser.this,
								language.getString("IdentitiesBrowser.cleanupDialog.nothingToDelete.body"),
								language.getString("IdentitiesBrowser.cleanupDialog.nothingToDelete.title"),
								JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					final int answer = JOptionPane.showConfirmDialog(
							IdentitiesBrowser.this,
							language.formatMessage("IdentitiesBrowser.cleanupDialog.deleteIdentities.body", Integer.toString(li.size())),
							language.getString("IdentitiesBrowser.cleanupDialog.deleteIdentities.title"),
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE);
					if( answer == JOptionPane.NO_OPTION ) {
						return;
					}

					// batch delete, turn off autocommit
					if( !IdentitiesStorage.inst().beginExclusiveThreadTransaction() ) {
						return;
					}
					for( final Integer element : li ) {
						final InnerTableMember m = identitiesTableModel.getRow(element.intValue());
						final Identity id = m.getIdentity();
						Core.getIdentitiesManager().deleteIdentity(id);
						identitiesTableModel.removeRow(element.intValue());
					}
					IdentitiesStorage.inst().endThreadTransaction();

					updateTitle();
				}
			});
		}
		return cleanupButton;
	}

	/**
	 * This method initializes lookupTextField
	 *
	 * @return JTextField
	 */
	private JTextField getLookupTextField() {
		if( lookupTextField == null ) {
			lookupTextField = new JTextField(10);
			// force a max size, needed for BoxLayout
			lookupTextField.setMaximumSize(lookupTextField.getPreferredSize());
			lookupTextField.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(final DocumentEvent e) {
					lookupContentChanged();
				}
				public void insertUpdate(final DocumentEvent e) {
					lookupContentChanged();
				}
				public void removeUpdate(final DocumentEvent e) {
					lookupContentChanged();
				}
			});
		}
		return lookupTextField;
	}

	/**
	 * This method initializes filterTextField
	 *
	 * @return JTextField
	 */
	private JTextField getFilterTextField() {
		if( filterTextField == null ) {
			filterTextField = new JTextField(10);
			filterTextField.setMaximumSize(filterTextField.getPreferredSize());

			filterTextField.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(final DocumentEvent e) {
					filterContentChanged();
				}
				public void insertUpdate(final DocumentEvent e) {
					filterContentChanged();
				}
				public void removeUpdate(final DocumentEvent e) {
					filterContentChanged();
				}
			});
		}
		return filterTextField;
	}

	/**
	 * Called whenever the content of the lookup text field changes
	 */
	private void lookupContentChanged() {
		try {
			final String txt = lookupTextField.getDocument().getText(0, lookupTextField.getDocument().getLength());
			// now try to find the first board name that starts with this txt (case insensitiv),
			// if we found one set selection to it, else leave selection untouched
			for( int row=0; row < identitiesTableModel.getRowCount(); row++ ) {
				final InnerTableMember memb = identitiesTableModel.getRow(row);
				if( memb.getIdentity().getUniqueName().toLowerCase().startsWith(txt.toLowerCase()) ) {
					getIdentitiesTable().getSelectionModel().setSelectionInterval(row, row);
					// now scroll to selected row, try to show it on top of table

					// determine the count of showed rows
					final int visibleRows = (int)(getIdentitiesTable().getVisibleRect().getHeight() / getIdentitiesTable().getCellRect(row,0,true).getHeight());
					int scrollToRow;
					if( (row + visibleRows) > identitiesTableModel.getRowCount() ) {
						scrollToRow = identitiesTableModel.getRowCount()-1;
					} else {
						scrollToRow = (row + visibleRows) - 1;
					}
					if( scrollToRow > row ) {
						scrollToRow--;
					}
					// scroll 2 times to make sure row is displayed
					getIdentitiesTable().scrollRectToVisible(getIdentitiesTable().getCellRect(row,0,true));
					getIdentitiesTable().scrollRectToVisible(getIdentitiesTable().getCellRect(scrollToRow,0,true));
					break;
				}
			}
		} catch(final Exception ex) {}
	}

	/**
	 * Called whenever the content of the filter text field changes
	 */
	private void filterContentChanged() {
		try {
			lookupTextField.setText(""); // clear
			String txt = filterTextField.getDocument().getText(0, filterTextField.getDocument().getLength()).trim();
			txt = txt.toLowerCase();
			// filter: show all boards that have this txt in name
			identitiesTableModel.clearDataModel();
			for( final Object element : allTableMembers ) {
				final InnerTableMember tm = (InnerTableMember)element;
				if( txt.length() > 0 ) {
					final String bn = tm.getIdentity().getUniqueName().toLowerCase();
					if( bn.indexOf(txt) < 0 ) {
						continue;
					}
				}
				identitiesTableModel.addRow(tm);
			}
		} catch(final Exception ex) {}
	}

	private File chooseXmlImportFile() {

		final FileFilter myFilter = new FileFilter() {
			@Override
			public boolean accept(final File file) {
				if( file.isDirectory() ) {
					return true;
				}
				if( file.getName().endsWith(".xml") ) {
					return true;
				}
				return false;
			}
			@Override
			public String getDescription() {
				return "identities_export.xml";
			}
		};

		final JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(myFilter);
		final int returnVal = chooser.showOpenDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile();
		}
		return null;
	}

	private File chooseXmlExportFile() {

		final FileFilter myFilter = new FileFilter() {
			@Override
			public boolean accept(final File file) {
				if( file.isDirectory() ) {
					return true;
				}
				if( file.getName().endsWith(".xml") ) {
					return true;
				}
				return false;
			}
			@Override
			public String getDescription() {
				return "identities_export.xml";
			}
		};

		final JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(myFilter);
		final int returnVal = chooser.showSaveDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			File f = chooser.getSelectedFile();
			if( !f.getName().endsWith(".xml") ) {
				f = new File(f.getPath() + ".xml");
			}
			if( f.exists() ) {
				final int answer = JOptionPane.showConfirmDialog(
						this,
						language.formatMessage("IdentitiesBrowser.exportIdentitiesConfirmXmlFileOverwrite.body", f.getName()),
						language.getString("IdentitiesBrowser.exportIdentitiesConfirmXmlFileOverwrite.title"),
						JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE);
				if( answer == JOptionPane.NO_OPTION ) {
					return null;
				}
			}
			return f;
		}
		return null;
	}

	/**
	 * This method initializes importButton
	 *
	 * @return JButton
	 */
	private JButton getImportButton() {
		if( importButton == null ) {
			importButton = new JButton();
			importButton.setText("IdentitiesBrowser.button.import");
			importButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final File xmlFile = chooseXmlImportFile();
					if( xmlFile == null ) {
						return;
					}

					final List<Identity> importedIdentities = IdentitiesXmlDAO.loadIdentities(xmlFile);
					if( importedIdentities.size() == 0 ) {
						// nothing loaded
						JOptionPane.showMessageDialog(
								IdentitiesBrowser.this,
								language.getString("IdentitiesBrowser.noIdentityToImport.body"),
								language.getString("IdentitiesBrowser.noIdentityToImport.title"),
								JOptionPane.INFORMATION_MESSAGE);
						return;
					}

					final int importedCount = Core.getIdentitiesManager().importIdentities(importedIdentities);
					final int skippedCount = importedIdentities.size() - importedCount;

					JOptionPane.showMessageDialog(
							IdentitiesBrowser.this,
							language.formatMessage(
									"IdentitiesBrowser.identitiesImported.body",
									Integer.toString(importedCount),
									Integer.toString(skippedCount)),
									language.getString("IdentitiesBrowser.identitiesImported.title"),
									JOptionPane.INFORMATION_MESSAGE);

					updateTitle();
				}
			});
		}
		return importButton;
	}

	/**
	 * This method initializes exportButton
	 *
	 * @return JButton
	 */
	private JButton getExportButton() {
		if( exportButton == null ) {
			exportButton = new JButton();
			exportButton.setText("IdentitiesBrowser.button.export");
			exportButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final List<Identity> allIdentities = Core.getIdentitiesManager().getIdentities();
					exportIdentities(allIdentities);
				}
			});
		}
		return exportButton;
	}

	private void exportIdentities(final List<Identity> ids) {
		// saves only good,observe,bad
		final File xmlFile = chooseXmlExportFile();
		if( xmlFile == null ) {
			return;
		}

		final int count = IdentitiesXmlDAO.saveIdentities(xmlFile, ids);
		if( count > 0 ) {
			// 'count' identities exported
			JOptionPane.showMessageDialog(
					IdentitiesBrowser.this,
					language.formatMessage("IdentitiesBrowser.identitiesExported.body", Integer.toString(count)),
					language.getString("IdentitiesBrowser.identitiesExported.title"),
					JOptionPane.INFORMATION_MESSAGE);
		} else if( count < 0 ) {
			// identities export failed
			JOptionPane.showMessageDialog(
					IdentitiesBrowser.this,
					language.getString("IdentitiesBrowser.identitiesExportFailed.body"),
					language.getString("IdentitiesBrowser.identitiesExportFailed.title"),
					JOptionPane.ERROR_MESSAGE);
		} else {
			// no identities to export, all are CHECK?
			JOptionPane.showMessageDialog(
					IdentitiesBrowser.this,
					language.getString("IdentitiesBrowser.noIdentityToExport.body"),
					language.getString("IdentitiesBrowser.noIdentityToExport.title"),
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private class ShowColoredLinesRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public ShowColoredLinesRenderer() {
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
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		selectedIds = new ArrayList<>();
		int[] selRows = getIdentitiesTable().getSelectedRows();
		for (int x = selRows.length - 1; x >= 0; x--) {
			InnerTableMember m = identitiesTableModel.getRow(selRows[x]);
			selectedIds.add(m.getIdentity());
		}

		Boolean isEnabled = !selectedIds.isEmpty();
		copyUniqueNameAction.setEnabled(isEnabled);
		copyUniqueNameAndPublicKeyAction.setEnabled(isEnabled);
		exportSelectedIdentitiesAction.setEnabled(isEnabled);
	}

	private class CopyUniqueNameAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			StringBuilder sb = new StringBuilder();
			for (Identity id : selectedIds) {
				sb.append(id.getUniqueName());
				sb.append("\n");
			}
			// We remove the additional \n at the end
			sb.deleteCharAt(sb.length() - 1);

			ClipboardUtil.copyText(sb.toString());
		}
	}

	private class CopyUniqueNameAndPublicKeyAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			StringBuilder sb = new StringBuilder();
			for (Identity id : selectedIds) {
				sb.append(id.getUniqueName());
				sb.append("\n");
				sb.append(id.getPublicKey());
				sb.append("\n\n");
			}
			// We remove the both additional \n at the end
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);

			ClipboardUtil.copyText(sb.toString());
		}
	}

	private class ExportSelectedIdentitiesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			exportIdentities(selectedIds);
		}
	}

	private class PopupMenu extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		private JMenu copyToClipboardMenu;

		public PopupMenu() {
			copyToClipboardMenu = new JMenu();
			copyToClipboardMenu.add(copyUniqueNameAction);
			copyToClipboardMenu.add(copyUniqueNameAndPublicKeyAction);

			copyUniqueNameAction.setText(language.getString("IdentitiesBrowser.popupmenu.copyUniqueName"));
			copyUniqueNameAndPublicKeyAction
					.setText(language.getString("IdentitiesBrowser.popupmenu.copyUniqueNameAndPublicKey"));
			copyToClipboardMenu.setText(language.getString("Common.copyToClipBoard") + "...");
			exportSelectedIdentitiesAction
					.setText(language.getString("IdentitiesBrowser.popupmenu.exportSelectedIdentities"));

			add(copyToClipboardMenu);
			addSeparator();
			add(exportSelectedIdentitiesAction);
		}
	}
}
