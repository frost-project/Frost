/*
  DownloadPanel.java / Frost

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.SettingsUpdater;
import frost.ext.ExecuteDocument;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.FreenetPriority;
import frost.fileTransfer.PersistenceManager;
import frost.fileTransfer.common.FileListFileDetailsDialog;
import frost.gui.AddNewDownloadsDialog;
import frost.messaging.frost.boards.Board;
import frost.messaging.frost.boards.TofTree;
import frost.util.ClipboardUtil;
import frost.util.FileAccess;
import frost.util.gui.JSkinnablePopupMenu;
import frost.util.gui.MiscToolkit;
import frost.util.gui.SelectRowOnRightClick;
import frost.util.gui.TextComponentClipboardMenu;
import frost.util.gui.search.TableFindAction;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;
import frost.util.model.SortedModelTable;

public class DownloadPanel extends JPanel implements SettingsUpdater {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(DownloadPanel.class);

	private PopupMenuDownload popupMenuDownload = null;

	private final Listener listener = new Listener();

	private DownloadModel model = null;

	private Language language = null;

	private final JToolBar downloadToolBar = new JToolBar();
	private final JButton downloadPasteButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/edit-paste.png"));
	private final JButton submitDownloadTextfieldButton = new JButton(MiscToolkit
			.loadImageIcon("/data/toolbar/document-save-as.png"));
	private final JButton downloadActivateButton = new JButton(MiscToolkit
			.loadImageIcon("/data/toolbar/media-playback-start.png"));
	private final JButton downloadPauseButton = new JButton(MiscToolkit
			.loadImageIcon("/data/toolbar/media-playback-pause.png"));
	private final JButton downloadPrefixApplyButton = new JButton(MiscToolkit
			.loadImageIcon("/data/toolbar/view-refresh.png"));
	private final JButton downloadDirSelectButton = new JButton(MiscToolkit
			.loadImageIcon("/data/toolbar/folder-open.png"));
	private final JButton downloadDirCreateButton = new JButton(MiscToolkit
			.loadImageIcon("/data/toolbar/folder-new.png"));
	private final JButton downloadDirApplyButton = new JButton(MiscToolkit
			.loadImageIcon("/data/toolbar/view-refresh.png"));
	private final JMenu downloadDirRecentMenu = new JMenu();
	private final JTextField downloadPrefixTextField = new JTextField(30);
	private final JTextField downloadDirTextField = new JTextField(30);
	private final JTextField downloadTextField = new JTextField(30);
	private final JLabel downloadItemCountLabel = new JLabel();
	private final JLabel downloadQuickloadLabel = new JLabel();
	private final JLabel downloadPrefixLabel = new JLabel();
	private final JLabel downloadDirLabel = new JLabel();
	private final JCheckBox removeFinishedDownloadsCheckBox = new JCheckBox();
	private final JCheckBox showExternalGlobalQueueItems = new JCheckBox();
	private Color downloadDirDefaultBackground;
	private SortedModelTable<FrostDownloadItem> modelTable;

	private boolean initialized = false;

	private boolean downloadingActivated = false;
	private int downloadItemCount = 0;

	public DownloadPanel() {
		super();
		Core.frostSettings.addUpdater(this);

		language = Language.getInstance();
		language.addLanguageListener(listener);
	}

	public DownloadTableFormat getTableFormat() {
		return (DownloadTableFormat) modelTable.getTableFormat();
	}

	/**
	 * This Document changes all newlines in the text into semicolons. Needed if
	 * the user pastes multiple download keys, each on a line, into the download
	 * text field.
	 */
	protected class HandleMultiLineKeysDocument extends PlainDocument {

		private static final long serialVersionUID = 1L;

		@Override
		public void insertString(final int offs, String str, final AttributeSet a) throws BadLocationException {
			str = str.replace('\n', ';');
			str = str.replace('\r', ' ');
			super.insertString(offs, str, a);
		}
	}

	public void initialize() {
		if (!initialized) {
			refreshLanguage();

			MiscToolkit.configureButton(downloadPasteButton);
			MiscToolkit.configureButton(submitDownloadTextfieldButton);
			MiscToolkit.configureButton(downloadPrefixApplyButton);
			MiscToolkit.configureButton(downloadDirSelectButton);
			MiscToolkit.configureButton(downloadDirCreateButton);
			MiscToolkit.configureButton(downloadDirApplyButton);

			MiscToolkit.configureButton(downloadActivateButton); // play_rollover
			MiscToolkit.configureButton(downloadPauseButton); // pause_rollover

			new TextComponentClipboardMenu(downloadTextField, language);
			new TextComponentClipboardMenu(downloadPrefixTextField, language);
			final TextComponentClipboardMenu tcmenu = new TextComponentClipboardMenu(downloadDirTextField, language);

			final JPopupMenu menu = tcmenu.getPopupMenu();

			menu.addSeparator();
			menu.add(downloadDirRecentMenu);
			downloadDirRecentMenu.addMenuListener(listener);

			downloadToolBar.setRollover(true);
			downloadToolBar.setFloatable(false);

			removeFinishedDownloadsCheckBox.setOpaque(false);
			showExternalGlobalQueueItems.setOpaque(false);

			// Toolbar
			downloadToolBar.add(downloadActivateButton);
			downloadToolBar.add(downloadPauseButton);
			downloadToolBar.add(Box.createRigidArea(new Dimension(8, 0)));
			downloadToolBar.add(removeFinishedDownloadsCheckBox);
			if (PersistenceManager.isPersistenceEnabled()) {
				downloadToolBar.add(Box.createRigidArea(new Dimension(8, 0)));
				downloadToolBar.add(showExternalGlobalQueueItems);
			}
			downloadToolBar.add(Box.createHorizontalGlue());
			downloadToolBar.add(downloadItemCountLabel);

			final GridBagConstraints gridBagConstraints = new GridBagConstraints();
			final JPanel gridBagLayout = new JPanel(new GridBagLayout());

			gridBagConstraints.anchor = GridBagConstraints.WEST;
			gridBagConstraints.fill = GridBagConstraints.NONE;
			gridBagConstraints.insets = new Insets(0, 3, 0, 3);
			gridBagConstraints.weightx = 0.0;
			gridBagConstraints.weighty = 0.0;
			gridBagConstraints.gridwidth = 1;
			gridBagConstraints.gridheight = 1;

			// Quickload
			gridBagConstraints.fill = GridBagConstraints.NONE;
			gridBagConstraints.weightx = 0.0;
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 0;
			gridBagLayout.add(downloadQuickloadLabel, gridBagConstraints);
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = 0;
			gridBagLayout.add(downloadTextField, gridBagConstraints);
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.gridx = 2;
			gridBagConstraints.gridy = 0;
			{
				JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
				p.add(submitDownloadTextfieldButton);
				p.add(downloadPasteButton);
				gridBagLayout.add(p, gridBagConstraints);
			}

			// Prefix
			gridBagConstraints.fill = GridBagConstraints.NONE;
			gridBagConstraints.weightx = 0.0;
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 1;
			gridBagLayout.add(downloadPrefixLabel, gridBagConstraints);
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = 1;
			gridBagLayout.add(downloadPrefixTextField, gridBagConstraints);
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.gridx = 2;
			gridBagConstraints.gridy = 1;
			{
				JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
				p.add(downloadPrefixApplyButton);
				gridBagLayout.add(p, gridBagConstraints);
			}

			// Download directory
			gridBagConstraints.fill = GridBagConstraints.NONE;
			gridBagConstraints.weightx = 0.0;
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 2;
			gridBagLayout.add(downloadDirLabel, gridBagConstraints);
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = 2;
			gridBagLayout.add(downloadDirTextField, gridBagConstraints);
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.gridx = 2;
			gridBagConstraints.gridy = 2;
			{
				JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
				p.add(downloadDirSelectButton);
				p.add(downloadDirCreateButton);
				p.add(downloadDirApplyButton);
				gridBagLayout.add(p, gridBagConstraints);
			}

			downloadTextField.setMinimumSize(downloadTextField.getPreferredSize());
			downloadPrefixTextField.setMinimumSize(downloadTextField.getPreferredSize());
			downloadDirTextField.setMinimumSize(downloadTextField.getPreferredSize());

			downloadTextField.setDocument(new HandleMultiLineKeysDocument());
			downloadDirTextField.setText(Core.frostSettings.getString(Settings.DIR_DOWNLOAD));

			downloadDirDefaultBackground = downloadDirTextField.getBackground();
			updateDownloadDirTextFieldBackground();

			// create the main download panel
			modelTable = new SortedModelTable<FrostDownloadItem>(model);
			new TableFindAction().install(modelTable.getTable());
			setLayout(new BorderLayout());

			final JPanel panelHeader = new JPanel(new BorderLayout());
			panelHeader.add(downloadToolBar, BorderLayout.PAGE_START);
			panelHeader.add(gridBagLayout, BorderLayout.CENTER);

			add(panelHeader, BorderLayout.NORTH);
			add(modelTable.getScrollPane(), BorderLayout.CENTER);
			fontChanged();

			modelTable.getTable().setDefaultRenderer(Object.class, new CellRenderer());

			// listeners
			downloadTextField.addActionListener(listener);
			downloadPasteButton.addActionListener(listener);
			submitDownloadTextfieldButton.addActionListener(listener);
			downloadActivateButton.addActionListener(listener);
			downloadPauseButton.addActionListener(listener);
			modelTable.getScrollPane().addMouseListener(listener);
			modelTable.getTable().addKeyListener(listener);
			modelTable.getTable().addMouseListener(listener);
			modelTable.getTable().addMouseListener(new SelectRowOnRightClick(modelTable.getTable()));
			removeFinishedDownloadsCheckBox.addItemListener(listener);
			showExternalGlobalQueueItems.addItemListener(listener);
			downloadPrefixApplyButton.addActionListener(listener);
			downloadDirTextField.addKeyListener(listener);
			downloadDirTextField.addFocusListener(listener);
			downloadDirSelectButton.addActionListener(listener);
			downloadDirCreateButton.addActionListener(listener);
			downloadDirApplyButton.addActionListener(listener);
			Core.frostSettings.addPropertyChangeListener(Settings.FILE_LIST_FONT_NAME, listener);
			Core.frostSettings.addPropertyChangeListener(Settings.FILE_LIST_FONT_SIZE, listener);
			Core.frostSettings.addPropertyChangeListener(Settings.FILE_LIST_FONT_STYLE, listener);

			// Settings
			removeFinishedDownloadsCheckBox.setSelected(Core.frostSettings
					.getBoolean(Settings.DOWNLOAD_REMOVE_FINISHED));
			showExternalGlobalQueueItems.setSelected(Core.frostSettings
					.getBoolean(Settings.GQ_SHOW_EXTERNAL_ITEMS_DOWNLOAD));
			setDownloadingActivated(Core.frostSettings.getBoolean(Settings.DOWNLOADING_ACTIVATED));

			assignHotkeys();

			initialized = true;
		}
	}

	private Dimension calculateLabelSize(final String text) {
		final JLabel dummyLabel = new JLabel(text);
		dummyLabel.doLayout();
		return dummyLabel.getPreferredSize();
	}

	private void refreshLanguage() {
		downloadPasteButton.setToolTipText(language.getString("DownloadPane.toolbar.tooltip.pasteKeys"));
		submitDownloadTextfieldButton.setToolTipText(language.getString("DownloadPane.toolbar.tooltip.downloadKeys"));
		downloadActivateButton.setToolTipText(language.getString("DownloadPane.toolbar.tooltip.activateDownloading"));
		downloadPauseButton.setToolTipText(language.getString("DownloadPane.toolbar.tooltip.pauseDownloading"));
		removeFinishedDownloadsCheckBox.setText(language.getString("DownloadPane.removeFinishedDownloads"));
		showExternalGlobalQueueItems.setText(language.getString("DownloadPane.showExternalGlobalQueueItems"));

		downloadTextField.setToolTipText(language.getString("DownloadPane.toolbar.tooltip.addKeys"));
		downloadPrefixTextField.setToolTipText(language.getString("DownloadPane.toolbar.tooltip.downloadPrefix"));
		downloadDirTextField.setToolTipText(language.getString("DownloadPane.toolbar.tooltip.downloadDir"));

		downloadPrefixApplyButton
				.setToolTipText(language.getString("DownloadPane.toolbar.tooltip.applyDownloadPrefix"));
		downloadDirSelectButton.setToolTipText(language.getString("DownloadPane.toolbar.tooltip.selectDownloadDir"));
		downloadDirCreateButton.setToolTipText(language.getString("DownloadPane.toolbar.tooltip.createDownloadDir"));
		downloadDirApplyButton.setToolTipText(language.getString("DownloadPane.toolbar.tooltip.applyDownloadDir"));

		downloadDirRecentMenu.setText(language.getString("DownloadPane.toolbar.downloadDirMenu.setDownloadDirTo"));

		downloadQuickloadLabel.setText(language.getString("DownloadPane.toolbar.label.downloadQuickload") + ": ");
		downloadPrefixLabel.setText(language.getString("DownloadPane.toolbar.label.downloadPrefix") + ": ");
		downloadDirLabel.setText(language.getString("DownloadPane.toolbar.label.downloadDir") + ": ");

		final String waiting = language.getString("DownloadPane.toolbar.waiting");
		final Dimension labelSize = calculateLabelSize(waiting + ": 00000");
		downloadItemCountLabel.setPreferredSize(labelSize);
		downloadItemCountLabel.setMinimumSize(labelSize);
		downloadItemCountLabel.setText(waiting + ": " + downloadItemCount);
	}

	public void setModel(final DownloadModel model) {
		this.model = model;
	}

	private void updateDownloadDirTextFieldBackground() {
		final File file = new File(downloadDirTextField.getText());
		if (file.isDirectory()) {
			downloadDirTextField.setBackground(downloadDirDefaultBackground);
		} else {
			downloadDirTextField.setBackground(Color.YELLOW);
		}
	}

	private void downloadDirTextField_keyReleased(final KeyEvent e) {
		updateDownloadDirTextFieldBackground();
	}

	private void downloadDirTextField_focusLost(final FocusEvent e) {
		updateDownloadDirTextFieldBackground();
	}

	private final String getDownloadPrefix() {
		return downloadPrefixTextField.getText();
	}

	private final String getDownloadDir() {
		final String dir = downloadDirTextField.getText();

		if (dir.length() == 0) {
			return null;
		} else {
			return dir;
		}
	}

	private void downloadDirSelectButton_actionPerformed(final ActionEvent e) {
		final JFileChooser fc = new JFileChooser(FileAccess.appendSeparator(downloadDirTextField.getText()));
		fc.setDialogTitle(language.getString("Options.downloads.filechooser.title"));
		fc.setFileHidingEnabled(true);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setMultiSelectionEnabled(false);

		final int returnVal = fc.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			final File file = fc.getSelectedFile();
			Core.frostSettings.setValue(Settings.DIR_LAST_USED, file.getParent());
			downloadDirTextField.setText(file.getPath());
			updateDownloadDirTextFieldBackground();
		}
	}

	private void downloadDirCreateButton_actionPerformed(final ActionEvent e) {
		File dir = new File(downloadDirTextField.getText());

		try {
			dir.mkdirs();
		} catch (Exception foo) {
		}

		updateDownloadDirTextFieldBackground();
	}

	private void applyDownloadPrefixToSelectedDownloads() {
		final List<FrostDownloadItem> selectedItems = modelTable.getSelectedItems();

		for (final FrostDownloadItem i : selectedItems) {

			if (!i.isExternal()) {
				i.setFilenamePrefix(getDownloadPrefix());
				i.fireValueChanged();
			}
		}
	}

	private void applyDownloadDirToSelectedDownloads() {
		final List<FrostDownloadItem> selectedItems = modelTable.getSelectedItems();

		for (final FrostDownloadItem i : selectedItems) {

			if (!i.isExternal()) {
				i.setDownloadDir(getDownloadDir());
				i.fireValueChanged();
			}
		}
	}

	private void downloadDirApplyButton_actionPerformed(final ActionEvent e) {
		applyDownloadDirToSelectedDownloads();
	}

	private void downloadPrefixApplyButton_actionPerformed(final ActionEvent e) {
		applyDownloadPrefixToSelectedDownloads();
	}

	/**
	 * downloadTextField Action Listener (Download/Quickload) The textfield can
	 * contain 1 key to download or multiple keys separated by ';'.
	 */
	private void downloadTextField_actionPerformed(final ActionEvent e) {
		String keylist = downloadTextField.getText();
		if ((keylist != null) && (keylist.length() != 0)) {
			openAddNewDownloadsDialog(keylist);
		}
	}

	/**
	 * Get keyTyped for downloadTable
	 */
	private void downloadTable_keyPressed(final KeyEvent e) {
		final char key = e.getKeyChar();
		if ((key == KeyEvent.VK_DELETE) && !modelTable.getTable().isEditing()) {
			removeSelectedDownloads();
		}
	}

	private void removeSelectedDownloads() {
		final List<FrostDownloadItem> selectedItems = modelTable.getSelectedItems();

		final List<String> externalRequestsToRemove = new LinkedList<String>();
		final List<FrostDownloadItem> requestsToRemove = new LinkedList<FrostDownloadItem>();
		for (final FrostDownloadItem frostDownloadItem : selectedItems) {
			requestsToRemove.add(frostDownloadItem);
			if (frostDownloadItem.isExternal()) {
				externalRequestsToRemove.add(frostDownloadItem.getGqIdentifier());
			}
		}

		model.removeItems(requestsToRemove);

		modelTable.getTable().clearSelection();

		if ((FileTransferManager.inst().getPersistenceManager() != null) && (externalRequestsToRemove.size() > 0)) {
			new Thread() {
				@Override
				public void run() {
					FileTransferManager.inst().getPersistenceManager().removeRequests(externalRequestsToRemove);
				}
			}.start();
		}
	}

	public boolean isDownloadingActivated() {
		return downloadingActivated;
	}

	public void setDownloadingActivated(final boolean b) {
		downloadingActivated = b;

		downloadActivateButton.setEnabled(!downloadingActivated);
		downloadPauseButton.setEnabled(downloadingActivated);
	}

	public void setDownloadItemCount(final int newDownloadItemCount) {
		downloadItemCount = newDownloadItemCount;

		final String s = new StringBuilder().append(language.getString("DownloadPane.toolbar.waiting")).append(": ")
				.append(downloadItemCount).toString();
		downloadItemCountLabel.setText(s);
	}

	private PopupMenuDownload getPopupMenuDownload() {
		if (popupMenuDownload == null) {
			popupMenuDownload = new PopupMenuDownload();
			language.addLanguageListener(popupMenuDownload);
		}
		return popupMenuDownload;
	}

	private void showDownloadTablePopupMenu(final MouseEvent e) {
		getPopupMenuDownload().show(e.getComponent(), e.getX(), e.getY());
	}

	private void fontChanged() {
		final String fontName = Core.frostSettings.getString(Settings.FILE_LIST_FONT_NAME);
		final int fontStyle = Core.frostSettings.getInteger(Settings.FILE_LIST_FONT_STYLE);
		final int fontSize = Core.frostSettings.getInteger(Settings.FILE_LIST_FONT_SIZE);
		Font font = new Font(fontName, fontStyle, fontSize);
		if (!font.getFamily().equals(fontName)) {
			logger.error("The selected font was not found in your system.");
			logger.error("That selection will be changed to \"SansSerif\".");
			Core.frostSettings.setValue(Settings.FILE_LIST_FONT_NAME, "SansSerif");
			font = new Font("SansSerif", fontStyle, fontSize);
		}
		modelTable.setFont(font);
	}

	private void downloadPasteButtonPressed(final ActionEvent e) {
		Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		if (transferable == null) {
			return;
		}

		// try to get data from clipboard
		String clipboardText;
		try {
			if (!transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				return;
			}
			clipboardText = (String) transferable.getTransferData(DataFlavor.stringFlavor);
		} catch (Exception stfu) {
			return;
		}

		if ((clipboardText != null) && (clipboardText.length() != 0)) {
			openAddNewDownloadsDialog(clipboardText);
		}
	}

	private void openAddNewDownloadsDialog(final String keylist) {
		// parse plaintext to get key list
		List<FrostDownloadItem> frostDownloadItemList = DownloadManager.parseKeys(keylist);
		if (frostDownloadItemList.size() == 0) {
			return;
		}

		// add default download dir and prefix
		for (final FrostDownloadItem frostDownloadItem : frostDownloadItemList) {
			final String downloadDir = this.downloadDirTextField.getText();
			if ((downloadDir != null) && (downloadDir.length() != 0)) {
				frostDownloadItem.setDownloadDir(downloadDir);
			}

			final String filenamePrefix = this.downloadPrefixTextField.getText();
			if ((filenamePrefix != null) && (filenamePrefix.length() != 0)) {
				frostDownloadItem.setFilenamePrefix(this.downloadPrefixTextField.getText());
			}
		}

		// open dialog - blocking
		new AddNewDownloadsDialog(MainFrame.getInstance()).startDialog(frostDownloadItemList);
	}

	private void downloadActivateButtonPressed(final ActionEvent e) {
		setDownloadingActivated(true);
	}

	private void downloadPauseButtonPressed(final ActionEvent e) {
		setDownloadingActivated(false);
	}

	private void openFile(FrostDownloadItem dlItem) {
		if (dlItem == null) {
			return;
		}

		final File targetFile = new File(dlItem.getDownloadFilename());
		if (!targetFile.isFile()) {
			logger.error("Executing: File not found: {}", targetFile.getAbsolutePath());
			return;
		}
		logger.info("Executing: {}", targetFile.getAbsolutePath());
		try {
			ExecuteDocument.openDocument(targetFile);
		} catch (final Throwable t) {
			JOptionPane.showMessageDialog(this, "Could not open the file: " + targetFile.getAbsolutePath() + "\n"
					+ t.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void updateSettings() {
		Core.frostSettings.setValue(Settings.DOWNLOADING_ACTIVATED, isDownloadingActivated());
	}

	public void changeItemPriorites(final List<FrostDownloadItem> items, final FreenetPriority newPrio) {
		if ((items == null) || (items.size() == 0) || (FileTransferManager.inst().getPersistenceManager() == null)) {
			return;
		}
		for (final FrostDownloadItem di : items) {
			String gqid = null;
			di.setPriority(newPrio);
			if (di.getState() == FrostDownloadItem.STATE_PROGRESS) {
				gqid = di.getGqIdentifier();
			}
			if (gqid != null) {
				FileTransferManager.inst().getPersistenceManager().getFcpTools().changeRequestPriority(gqid, newPrio);
			}
		}
	}

	private void assignHotkeys() {

		// assign keys 1-6 - set priority of selected items
		final Action setPriorityAction = new AbstractAction() {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(final ActionEvent event) {
				final FreenetPriority prio = FreenetPriority.getPriority(Integer.parseInt(event.getActionCommand()));
				final List<FrostDownloadItem> selectedItems = modelTable.getSelectedItems();
				changeItemPriorites(selectedItems, prio);

			}
		};
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0),
				"SETPRIO");
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0),
				"SETPRIO");
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0),
				"SETPRIO");
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_4, 0),
				"SETPRIO");
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_5, 0),
				"SETPRIO");
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_6, 0),
				"SETPRIO");
		getActionMap().put("SETPRIO", setPriorityAction);

		// Enter
		final Action setOpenFileAction = new AbstractAction() {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(final ActionEvent event) {
				openFile(modelTable.getSelectedItem());
			}
		};
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),"OpenFile");
		getActionMap().put("OpenFile", setOpenFileAction);
	}

	/**
	 * Renderer draws background of DONE items in green.
	 */
	private class CellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		private final Color col_green = new Color(0x00, 0x80, 0x00);

		public CellRenderer() {
			super();
		}

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value,
				final boolean isSelected, final boolean hasFocus, final int row, final int column) {

			super.getTableCellRendererComponent(table, value, isSelected, /* hasFocus */
			false, row, column);

			final FrostDownloadItem item = model.getItemAt(row);

			// set background of DONE downloads green
			if (item.getState() == FrostDownloadItem.STATE_DONE) {
				setBackground(col_green);
			} else {
				setBackground(modelTable.getTable().getBackground());
			}

			return this;
		}
	}

	private class PopupMenuDownload extends JSkinnablePopupMenu implements ActionListener, LanguageListener {

		private static final long serialVersionUID = 1L;

		private final JMenuItem detailsItem = new JMenuItem();
		private final JMenuItem copyKeysAndNamesItem = new JMenuItem();
		private final JMenuItem copyExtendedInfoItem = new JMenuItem();
		private final JMenuItem disableAllDownloadsItem = new JMenuItem();
		private final JMenuItem disableSelectedDownloadsItem = new JMenuItem();
		private final JMenuItem enableAllDownloadsItem = new JMenuItem();
		private final JMenuItem enableSelectedDownloadsItem = new JMenuItem();
		private final JMenuItem invertEnabledAllItem = new JMenuItem();
		private final JMenuItem invertEnabledSelectedItem = new JMenuItem();
		private final JMenuItem removeSelectedDownloadsItem = new JMenuItem();
		private final JMenuItem restartSelectedDownloadsItem = new JMenuItem();

		private final JMenuItem startSelectedDownloadsNow = new JMenuItem();

		private final JMenuItem useThisDownloadDirItem = new JMenuItem();
		private final JMenuItem jumpToAssociatedMessage = new JMenuItem();

		private JMenu changePriorityMenu = null;
		private JMenuItem removeFromGqItem = null;

		private JMenuItem retrieveDirectExternalDownloads = null;

		public PopupMenuDownload() {
			super();
			initialize();
		}

		private void initialize() {

			if (PersistenceManager.isPersistenceEnabled()) {
				changePriorityMenu = new JMenu();
        		for(final FreenetPriority priority : FreenetPriority.values()) {
        			JMenuItem priorityMenuItem = new JMenuItem();
        			priorityMenuItem.addActionListener(new ActionListener() {
        				public void actionPerformed(final ActionEvent actionEvent) {
        					changeItemPriorites(modelTable.getSelectedItems(), priority);
        				}
        			});
        			changePriorityMenu.add(priorityMenuItem);
        		}

				removeFromGqItem = new JMenuItem();

				removeFromGqItem.addActionListener(this);

				retrieveDirectExternalDownloads = new JMenuItem();
				retrieveDirectExternalDownloads.addActionListener(this);
			}

			refreshLanguage();

			// TODO: implement cancel of downloads

			copyKeysAndNamesItem.addActionListener(this);
			copyExtendedInfoItem.addActionListener(this);
			restartSelectedDownloadsItem.addActionListener(this);
			removeSelectedDownloadsItem.addActionListener(this);
			enableAllDownloadsItem.addActionListener(this);
			disableAllDownloadsItem.addActionListener(this);
			enableSelectedDownloadsItem.addActionListener(this);
			disableSelectedDownloadsItem.addActionListener(this);
			invertEnabledAllItem.addActionListener(this);
			invertEnabledSelectedItem.addActionListener(this);
			detailsItem.addActionListener(this);
			startSelectedDownloadsNow.addActionListener(this);
			useThisDownloadDirItem.addActionListener(this);
			jumpToAssociatedMessage.addActionListener(this);
		}

		private void refreshLanguage() {
			detailsItem.setText(language.getString("Common.details"));
			copyKeysAndNamesItem.setText(language.getString("Common.copyToClipBoard.copyKeysWithFilenames"));
			copyExtendedInfoItem.setText(language.getString("Common.copyToClipBoard.copyExtendedInfo"));
			restartSelectedDownloadsItem.setText(language
					.getString("DownloadPane.fileTable.popupmenu.restartSelectedDownloads"));
			removeSelectedDownloadsItem.setText(language
					.getString("DownloadPane.fileTable.popupmenu.remove.removeSelectedDownloads"));
			enableAllDownloadsItem.setText(language
					.getString("DownloadPane.fileTable.popupmenu.enableDownloads.enableAllDownloads"));
			disableAllDownloadsItem.setText(language
					.getString("DownloadPane.fileTable.popupmenu.enableDownloads.disableAllDownloads"));
			enableSelectedDownloadsItem.setText(language
					.getString("DownloadPane.fileTable.popupmenu.enableDownloads.enableSelectedDownloads"));
			disableSelectedDownloadsItem.setText(language
					.getString("DownloadPane.fileTable.popupmenu.enableDownloads.disableSelectedDownloads"));
			invertEnabledAllItem.setText(language
					.getString("DownloadPane.fileTable.popupmenu.enableDownloads.invertEnabledStateForAllDownloads"));
			invertEnabledSelectedItem
					.setText(language
							.getString("DownloadPane.fileTable.popupmenu.enableDownloads.invertEnabledStateForSelectedDownloads"));
			startSelectedDownloadsNow.setText(language
					.getString("DownloadPane.fileTable.popupmenu.startSelectedDownloadsNow"));
			useThisDownloadDirItem.setText(language.getString("DownloadPane.fileTable.popupmenu.useThisDownloadDir"));
			jumpToAssociatedMessage.setText(language
					.getString("DownloadPane.fileTable.popupmenu.jumpToAssociatedMessage"));

			if (PersistenceManager.isPersistenceEnabled()) {
				changePriorityMenu.setText(language.getString("Common.priority.changePriority"));

				for(int itemNum = 0; itemNum < changePriorityMenu.getItemCount() ; itemNum++) {
                	changePriorityMenu.getItem(itemNum).setText(FreenetPriority.getName(itemNum));
                }

				removeFromGqItem.setText(language.getString("DownloadPane.fileTable.popupmenu.removeFromGlobalQueue"));

				retrieveDirectExternalDownloads.setText(language
						.getString("DownloadPane.fileTable.popupmenu.retrieveDirectExternalDownloads"));
			}
		}

		public void actionPerformed(final ActionEvent e) {
			if (e.getSource() == copyKeysAndNamesItem) {
				ClipboardUtil.copyKeysAndFilenames(modelTable.getSelectedItems());
			} else if (e.getSource() == copyExtendedInfoItem) {
				ClipboardUtil.copyExtendedInfo(modelTable.getSelectedItems());
			} else if (e.getSource() == restartSelectedDownloadsItem) {
				restartSelectedDownloads();
			} else if (e.getSource() == useThisDownloadDirItem) {
				useThisDownloadDirectory();
			} else if (e.getSource() == jumpToAssociatedMessage) {
				jumpToAssociatedMessage();
			} else if (e.getSource() == removeSelectedDownloadsItem) {
				removeSelectedDownloads();
			} else if (e.getSource() == enableAllDownloadsItem) {
				enableAllDownloads();
			} else if (e.getSource() == disableAllDownloadsItem) {
				disableAllDownloads();
			} else if (e.getSource() == enableSelectedDownloadsItem) {
				enableSelectedDownloads();
			} else if (e.getSource() == disableSelectedDownloadsItem) {
				disableSelectedDownloads();
			} else if (e.getSource() == invertEnabledAllItem) {
				invertEnabledAll();
			} else if (e.getSource() == invertEnabledSelectedItem) {
				invertEnabledSelected();
			} else if (e.getSource() == detailsItem) {
				showDetails();
			} else if (e.getSource() == removeFromGqItem) {
				removeSelectedUploadsFromGlobalQueue();
			} else if (e.getSource() == retrieveDirectExternalDownloads) {
				retrieveDirectExternalDownloads();
			} else if (e.getSource() == startSelectedDownloadsNow) {
				startSelectedDownloadsNow();
			}
		}

		private void removeSelectedUploadsFromGlobalQueue() {
			if (FileTransferManager.inst().getPersistenceManager() == null) {
				return;
			}
			final List<FrostDownloadItem> selectedItems = modelTable.getSelectedItems();
			final List<String> requestsToRemove = new ArrayList<String>();
			final List<FrostDownloadItem> itemsToUpdate = new ArrayList<FrostDownloadItem>();
			for (final FrostDownloadItem item : selectedItems) {
				if (FileTransferManager.inst().getPersistenceManager().isItemInGlobalQueue(item)) {
					requestsToRemove.add(item.getGqIdentifier());
					itemsToUpdate.add(item);
					item.setInternalRemoveExpected(true);
				}
			}
			FileTransferManager.inst().getPersistenceManager().removeRequests(requestsToRemove);
			// after remove, update state of removed items
			for (final FrostDownloadItem item : itemsToUpdate) {
				item.setState(FrostDownloadItem.STATE_WAITING);
				item.setEnabled(Boolean.FALSE);
				item.setPriority(FreenetPriority.PAUSE);
				item.fireValueChanged();
			}
		}

		private void retrieveDirectExternalDownloads() {
			if (FileTransferManager.inst().getPersistenceManager() == null) {
				return;
			}
			final List<FrostDownloadItem> selectedItems = modelTable.getSelectedItems();
			for (final FrostDownloadItem item : selectedItems) {
				if (item.isExternal() && item.isDirect() && (item.getState() == FrostDownloadItem.STATE_DONE)) {
					final long expectedFileSize = item.getFileSize(); // set
					// from
					// global
					// queue
					FileTransferManager.inst().getPersistenceManager().maybeEnqueueDirectGet(item, expectedFileSize);
				}
			}
		}

		private void startSelectedDownloadsNow() {
			final List<FrostDownloadItem> selectedItems = modelTable.getSelectedItems();

			final List<FrostDownloadItem> itemsToStart = new LinkedList<FrostDownloadItem>();
			for (final FrostDownloadItem i : selectedItems) {
				if (i.isExternal()) {
					continue;
				}
				if (i.getState() != FrostDownloadItem.STATE_WAITING) {
					continue;
				}
				if (i.getKey() == null) {
					continue;
				}
				itemsToStart.add(i);
			}

			for (final FrostDownloadItem dlItem : itemsToStart) {
				dlItem.setEnabled(true);
				FileTransferManager.inst().getDownloadManager().startDownload(dlItem);
			}
		}

		private void showDetails() {
			final List<FrostDownloadItem> selectedItems = modelTable.getSelectedItems();
			if (selectedItems.size() != 1) {
				return;
			}
			if (!selectedItems.get(0).isSharedFile()) {
				return;
			}
			new FileListFileDetailsDialog(MainFrame.getInstance(), false)
					.startDialog(selectedItems.get(0).getFileListFileObject());
		}

		private void invertEnabledSelected() {
			model.setItemsEnabled(null, modelTable.getSelectedItems());
		}

		private void invertEnabledAll() {
			model.setAllItemsEnabled(null);
		}

		private void disableSelectedDownloads() {
			model.setItemsEnabled(Boolean.FALSE, modelTable.getSelectedItems());
		}

		private void enableSelectedDownloads() {
			model.setItemsEnabled(Boolean.TRUE, modelTable.getSelectedItems());
		}

		private void disableAllDownloads() {
			model.setAllItemsEnabled(Boolean.FALSE);
		}

		private void enableAllDownloads() {
			model.setAllItemsEnabled(Boolean.TRUE);
		}

		private void restartSelectedDownloads() {
			model.restartItems(modelTable.getSelectedItems());
		}

		private void useThisDownloadDirectory() {
			if (modelTable.getSelectedItems().size() > 0) {
				downloadDirTextField.setText(modelTable.getSelectedItems().get(0).getDownloadDir());
			}
		}

		private void jumpToAssociatedMessage() {
			if (modelTable.getSelectedItems().size() > 0) {
				final FrostDownloadItem item = modelTable.getSelectedItems().get(0);
				final String boardName = item.getAssociatedBoardName();
				final String messageId = item.getAssociatedMessageId();

				if ((boardName != null) && (messageId != null)) {
					final Board board = MainFrame.getInstance().getMessagingTab().getTofTreeModel().getBoardByName(
							boardName);
					final TofTree t = MainFrame.getInstance().getMessagingTab().getTofTree();

					if ((board != null) && (t != null)) {
						t.clearSelection();
						MainFrame.getInstance().getMessagingTab().forceSelectMessageId(messageId);
						t.setSelectionPath(new TreePath(board.getPath()));
						MainFrame.getInstance().selectTabbedPaneTab("MainFrame.tabbedPane.news");
					}
				}
			}
		}

		public void languageChanged(final LanguageEvent event) {
			refreshLanguage();
		}

		@Override
		public void show(final Component invoker, final int x, final int y) {
			removeAll();

			final List<FrostDownloadItem> selectedItems = modelTable.getSelectedItems();

			if (selectedItems.size() == 0) {
				return;
			}

			add(copyKeysAndNamesItem);
			add(copyExtendedInfoItem);
			addSeparator();
			add(startSelectedDownloadsNow);
			add(restartSelectedDownloadsItem);
			addSeparator();

			if (PersistenceManager.isPersistenceEnabled()) {
				add(changePriorityMenu);
				addSeparator();
			}

			final JMenu enabledSubMenu = new JMenu(language
					.getString("DownloadPane.fileTable.popupmenu.enableDownloads")
					+ "...");
			enabledSubMenu.add(enableSelectedDownloadsItem);
			enabledSubMenu.add(disableSelectedDownloadsItem);
			enabledSubMenu.add(invertEnabledSelectedItem);
			enabledSubMenu.addSeparator();

			enabledSubMenu.add(enableAllDownloadsItem);
			enabledSubMenu.add(disableAllDownloadsItem);
			enabledSubMenu.add(invertEnabledAllItem);
			add(enabledSubMenu);

			// we only find external items if persistence is enabled
			if (PersistenceManager.isPersistenceEnabled()) {
				for (final FrostDownloadItem item : selectedItems) {
					if (item.isExternal() && item.isDirect() && (item.getState() == FrostDownloadItem.STATE_DONE)) {
						add(retrieveDirectExternalDownloads);
						break;
					}
				}
			}
			add(removeSelectedDownloadsItem);
			if ((FileTransferManager.inst().getPersistenceManager() != null) && (selectedItems != null)) {
				// add only if there are removable items selected
				for (final FrostDownloadItem item : selectedItems) {
					if (FileTransferManager.inst().getPersistenceManager().isItemInGlobalQueue(item)) {
						add(removeFromGqItem);
						break;
					}
				}
			}
			if (selectedItems.size() == 1) {
				final FrostDownloadItem item = selectedItems.get(0);
				if (item.isSharedFile()) {
					addSeparator();
					add(detailsItem);
				}
				addSeparator();
				add(useThisDownloadDirItem);
				if (item.getAssociatedMessageId() != null) {
					addSeparator();
					add(jumpToAssociatedMessage);
				}
			}

			super.show(invoker, x, y);
		}
	}

	private class Listener extends MouseAdapter implements LanguageListener, ActionListener, KeyListener,
			MouseListener, PropertyChangeListener, ItemListener, FocusListener, MenuListener {

		public Listener() {
			super();
		}

		public void languageChanged(final LanguageEvent event) {
			refreshLanguage();
		}

		public void actionPerformed(final ActionEvent e) {
			if (e.getSource() == downloadDirSelectButton) {
				downloadDirSelectButton_actionPerformed(e);
			} else if (e.getSource() == downloadDirCreateButton) {
				downloadDirCreateButton_actionPerformed(e);
			} else if (e.getSource() == downloadPrefixApplyButton) {
				downloadPrefixApplyButton_actionPerformed(e);
			} else if (e.getSource() == downloadDirApplyButton) {
				downloadDirApplyButton_actionPerformed(e);
			} else if (e.getSource() == submitDownloadTextfieldButton) {
				downloadTextField_actionPerformed(e);
			} else if (e.getSource() == downloadTextField) {
				downloadTextField_actionPerformed(e);
			} else if (e.getSource() == downloadPasteButton) {
				downloadPasteButtonPressed(e);
			} else if (e.getSource() == downloadActivateButton) {
				downloadActivateButtonPressed(e);
			} else if (e.getSource() == downloadPauseButton) {
				downloadPauseButtonPressed(e);
			} else {
				for (int i = 0; i < downloadDirRecentMenu.getItemCount(); i++) {
					final JMenuItem item = downloadDirRecentMenu.getItem(i);
					if (e.getSource() == item) {
						downloadDirTextField.setText(item.getText());
					}
				}
			}
		}

		public void keyPressed(final KeyEvent e) {
			if (e.getSource() == modelTable.getTable()) {
				downloadTable_keyPressed(e);
			}
		}

		public void keyReleased(final KeyEvent e) {
			if (e.getSource() == downloadDirTextField) {
				downloadDirTextField_keyReleased(e);
			}
		}

		public void keyTyped(final KeyEvent e) {
		}

		public void focusGained(final FocusEvent e) {
		}

		public void focusLost(final FocusEvent e) {
			if (e.getSource() == downloadDirTextField) {
				downloadDirTextField_focusLost(e);
			}
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			if (e.getClickCount() == 2) {
				if (e.getSource() == modelTable.getTable()) {
					// Start file from download table. Is this a good idea?
					openFile(modelTable.getSelectedItem());
				}
			} else if (e.isPopupTrigger()) {
				if ((e.getSource() == modelTable.getTable()) || (e.getSource() == modelTable.getScrollPane())) {
					showDownloadTablePopupMenu(e);
				}
			}
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			if ((e.getClickCount() == 1) && (e.isPopupTrigger())) {

				if ((e.getSource() == modelTable.getTable()) || (e.getSource() == modelTable.getScrollPane())) {
					showDownloadTablePopupMenu(e);
				}

			}
		}

		public void propertyChange(final PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals(Settings.FILE_LIST_FONT_NAME)) {
				fontChanged();
			}
			if (evt.getPropertyName().equals(Settings.FILE_LIST_FONT_SIZE)) {
				fontChanged();
			}
			if (evt.getPropertyName().equals(Settings.FILE_LIST_FONT_STYLE)) {
				fontChanged();
			}
		}

		public void itemStateChanged(final ItemEvent e) {
			if (removeFinishedDownloadsCheckBox.isSelected()) {
				Core.frostSettings.setValue(Settings.DOWNLOAD_REMOVE_FINISHED, true);
				model.removeFinishedDownloads();
			} else {
				Core.frostSettings.setValue(Settings.DOWNLOAD_REMOVE_FINISHED, false);
			}
			if (showExternalGlobalQueueItems.isSelected()) {
				Core.frostSettings.setValue(Settings.GQ_SHOW_EXTERNAL_ITEMS_DOWNLOAD, true);
			} else {
				Core.frostSettings.setValue(Settings.GQ_SHOW_EXTERNAL_ITEMS_DOWNLOAD, false);
				model.removeExternalDownloads();
			}
		}

		public void menuCanceled(MenuEvent e) {
		}

		public void menuDeselected(MenuEvent e) {
		}

		public void menuSelected(MenuEvent e) {
			if (e.getSource() == downloadDirRecentMenu) {
				JMenuItem item;

				downloadDirRecentMenu.removeAll();

				item = new JMenuItem(Core.frostSettings.getString(Settings.DIR_DOWNLOAD));
				downloadDirRecentMenu.add(item);
				item.addActionListener(this);

				final LinkedList<String> dirs = FileTransferManager.inst().getDownloadManager().getRecentDownloadDirs();
				if( dirs.size() > 0 ) {
					downloadDirRecentMenu.addSeparator();

					final ListIterator<String> iter = dirs.listIterator(dirs.size());
					while (iter.hasPrevious()) {
						final String dir = iter.previous();

						item = new JMenuItem(dir);
						downloadDirRecentMenu.add(item);
						item.addActionListener(this);
					}
				}
			}
		}
	}
}
