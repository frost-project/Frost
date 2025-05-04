/*
  UploadPanel.java / Frost
  Copyright (C) 2001  Frost Project <jtcfrost.sourceforge.net>

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
package frost.fileTransfer.upload;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.ext.ExecuteDocument;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.FreenetPriority;
import frost.fileTransfer.PersistenceManager;
import frost.gui.AddNewUploadsDialog;
import frost.util.ClipboardUtil;
import frost.util.gui.MiscToolkit;
import frost.util.gui.SelectRowOnRightClick;
import frost.util.gui.SimplePopupMenuListener;
import frost.util.gui.action.BaseAction;
import frost.util.gui.search.TableFindAction;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;
import frost.util.model.SortedModelTable;

public class UploadPanel extends JPanel implements LanguageListener, SimplePopupMenuListener {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(UploadPanel.class);

	private CopyKeysAndNamesAction copyKeysAndNamesAction;
	private CopyExtendedInfoAction copyExtendedInfoAction;
	private HashMap<FreenetPriority, ChangePriorityAction> changePriorityActions;
	private JMenu changePriorityMenu;
	private SetEnabledSelectedItemsAction enableSelectedAction;
	private SetEnabledSelectedItemsAction disableSelectedAction;
	private SetEnabledSelectedItemsAction invertEnabledSelectedAction;
	private SetEnabledAllItemsAction enableAllAction;
	private SetEnabledAllItemsAction disableAllAction;
	private SetEnabledAllItemsAction invertEnabledAllAction;
	private JMenu enabledMenu;
	private StartSelectedNowAction startSelectedNowAction;
	private GenerateCHKForSelectedFilesAction generateCHKForSelectedFilesAction;
	private UploadSelectedFilesAction uploadSelectedFilesAction;
	private RemoveSelectedFilesAction removeSelectedFilesAction;
	private RemoveFromGlobalQueueAction removeFromGlobalQueueAction;
	private OpenFileAction openFileAction;
	private PopupMenuUpload popupMenuUpload;

	private transient Listener listener = new Listener();

	private transient UploadModel model;

	private transient Language language;

    private final JToolBar uploadToolBar = new JToolBar();
    private final JButton uploadAddFilesButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/folder-open.png"));
    private final JCheckBox removeFinishedUploadsCheckBox = new JCheckBox();
    private final JCheckBox showExternalGlobalQueueItems = new JCheckBox();
    private final JCheckBox compressUploadsCheckBox = new JCheckBox();

    private SortedModelTable<FrostUploadItem> modelTable;

    private final JLabel uploadItemCountLabel = new JLabel();
    private int uploadItemCount = 0;

    private boolean initialized = false;

    public UploadPanel() {
        super();

        language = Language.getInstance();
		language.addLanguageListener(this);
    }

    public UploadTableFormat getTableFormat() {
        return (UploadTableFormat) modelTable.getTableFormat();
    }

    public void initialize() {
        if (!initialized) {
            uploadToolBar.setRollover(true);
            uploadToolBar.setFloatable(false);

            removeFinishedUploadsCheckBox.setOpaque(false);
            showExternalGlobalQueueItems.setOpaque(false);
            compressUploadsCheckBox.setOpaque(false);

            // create the top panel
            MiscToolkit.configureButton(uploadAddFilesButton);
            uploadToolBar.add(uploadAddFilesButton);
            uploadToolBar.add(Box.createRigidArea(new Dimension(8, 0)));
            uploadToolBar.add(removeFinishedUploadsCheckBox);
            if( PersistenceManager.isPersistenceEnabled() ) {
                uploadToolBar.add(Box.createRigidArea(new Dimension(8, 0)));
                uploadToolBar.add(showExternalGlobalQueueItems);
            }
            uploadToolBar.add(Box.createRigidArea(new Dimension(8, 0)));
            uploadToolBar.add(compressUploadsCheckBox);
            
            uploadToolBar.add(Box.createRigidArea(new Dimension(80, 0)));
            uploadToolBar.add(Box.createHorizontalGlue());
            uploadToolBar.add(uploadItemCountLabel);

            // create the main upload panel
            modelTable = new SortedModelTable<FrostUploadItem>(model);
            new TableFindAction().install(modelTable.getTable());
            setLayout(new BorderLayout());
            add(uploadToolBar, BorderLayout.NORTH);
            add(modelTable.getScrollPane(), BorderLayout.CENTER);
            fontChanged();

            // listeners
            uploadAddFilesButton.addActionListener(listener);
            modelTable.getScrollPane().addMouseListener(listener);
            modelTable.getTable().addKeyListener(listener);
            modelTable.getTable().addMouseListener(listener);
			modelTable.getTable().addMouseListener(new SelectRowOnRightClick(modelTable.getTable()));
            removeFinishedUploadsCheckBox.addItemListener(listener);
            showExternalGlobalQueueItems.addItemListener(listener);
            compressUploadsCheckBox.addItemListener(listener);
            Core.frostSettings.addPropertyChangeListener(Settings.FILE_LIST_FONT_NAME, listener);
            Core.frostSettings.addPropertyChangeListener(Settings.FILE_LIST_FONT_SIZE, listener);
            Core.frostSettings.addPropertyChangeListener(Settings.FILE_LIST_FONT_STYLE, listener);

            removeFinishedUploadsCheckBox.setSelected(Core.frostSettings.getBoolean(Settings.UPLOAD_REMOVE_FINISHED));
            showExternalGlobalQueueItems.setSelected(Core.frostSettings.getBoolean(Settings.GQ_SHOW_EXTERNAL_ITEMS_UPLOAD));
            compressUploadsCheckBox.setSelected(Core.frostSettings.getBoolean(Settings.COMPRESS_UPLOADS));

            assignHotkeys();

			copyKeysAndNamesAction = new CopyKeysAndNamesAction();
			copyExtendedInfoAction = new CopyExtendedInfoAction();
			changePriorityMenu = new JMenu();
			changePriorityActions = new HashMap<>();
			for (FreenetPriority priority : FreenetPriority.values()) {
				changePriorityActions.put(priority, new ChangePriorityAction(priority));
			}
			enabledMenu = new JMenu();
			enableSelectedAction = new SetEnabledSelectedItemsAction(true);
			disableSelectedAction = new SetEnabledSelectedItemsAction(false);
			invertEnabledSelectedAction = new SetEnabledSelectedItemsAction(null);
			enableAllAction = new SetEnabledAllItemsAction(true);
			disableAllAction = new SetEnabledAllItemsAction(false);
			invertEnabledAllAction = new SetEnabledAllItemsAction(null);
			startSelectedNowAction = new StartSelectedNowAction();
			generateCHKForSelectedFilesAction = new GenerateCHKForSelectedFilesAction();
			uploadSelectedFilesAction = new UploadSelectedFilesAction();
			removeSelectedFilesAction = new RemoveSelectedFilesAction();
			removeFromGlobalQueueAction = new RemoveFromGlobalQueueAction();
			openFileAction = new OpenFileAction();
			popupMenuUpload = new PopupMenuUpload();
			popupMenuUpload.addPopupMenuListener(this);
			modelTable.getTable().setComponentPopupMenu(popupMenuUpload);

			languageChanged(null);

            initialized = true;
        }
    }

    private Dimension calculateLabelSize(final String text) {
        final JLabel dummyLabel = new JLabel(text);
        dummyLabel.doLayout();
        return dummyLabel.getPreferredSize();
    }

    /**
     * Remove selected files
     */
    private void removeSelectedFiles() {
        final List<FrostUploadItem> selectedItems = modelTable.getSelectedItems();

        final List<String> externalRequestsToRemove = new LinkedList<String>();
        final List<FrostUploadItem> requestsToRemove = new LinkedList<FrostUploadItem>();
        for( final FrostUploadItem mi : selectedItems ) {
            requestsToRemove.add(mi);
            if( mi.isExternal() ) {
                externalRequestsToRemove.add(mi.getGqIdentifier());
            }
        }
        model.removeItems(requestsToRemove);

        modelTable.getTable().clearSelection();

        if( FileTransferManager.inst().getPersistenceManager() != null && externalRequestsToRemove.size() > 0 ) {
            new Thread() {
                @Override
                public void run() {
                    FileTransferManager.inst().getPersistenceManager().removeRequests(externalRequestsToRemove);
                }
            }.start();
        }
    }

    private void openFile(FrostUploadItem ulItem) {
    	if (ulItem == null) {
			return;
		}
		
		final File targetFile = ulItem.getFile();
		if (targetFile == null || !targetFile.isFile()) {
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

	private void fontChanged() {
		modelTable.setFont(Core.frostSettings.getFont(Settings.FILE_LIST_FONT_NAME, Settings.FILE_LIST_FONT_STYLE,
				Settings.FILE_LIST_FONT_SIZE, "SansSerif"));
	}

    public void setModel(final UploadModel model) {
        this.model = model;
    }

    public void setUploadItemCount(final int newUploadItemCount) {
        uploadItemCount = newUploadItemCount;

        final String s =
            new StringBuilder()
                .append(language.getString("UploadPane.toolbar.waiting"))
                .append(": ")
                .append(uploadItemCount)
                .toString();
        uploadItemCountLabel.setText(s);
    }
    
    public void changeItemPriorites(final List<FrostUploadItem> items, final FreenetPriority newPrio) {
        if (items == null || items.size() == 0 || FileTransferManager.inst().getPersistenceManager() == null) {
            return;
        }
        for (final FrostUploadItem ui : items) {
            String gqid = null;
            if (ui.getState() == FrostUploadItem.STATE_PROGRESS) {
                ui.setPriority(newPrio);
                gqid = ui.getGqIdentifier();
            }
            if (gqid != null) {
            	FileTransferManager.inst().getPersistenceManager().getFcpTools().changeRequestPriority(gqid, newPrio);
            }
        }
    }

    private void assignHotkeys() {

		// assign keys 1-6 - set priority of selected items
		Action setPriorityAction = new AbstractAction() {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent event) {
				FreenetPriority prio = FreenetPriority.getPriority(Integer.parseInt(event.getActionCommand()));
				changeItemPriorites(modelTable.getSelectedItems(), prio);
			}
		};

    	getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0), "SETPRIO");
    	getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0), "SETPRIO");
    	getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0), "SETPRIO");
    	getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_4, 0), "SETPRIO");
    	getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_5, 0), "SETPRIO");
    	getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_6, 0), "SETPRIO");
    	getActionMap().put("SETPRIO", setPriorityAction);

       	getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),"OpenFile");
    	getActionMap().put("OpenFile", openFileAction);
    }

	private class Listener extends MouseAdapter
			implements ActionListener, KeyListener, PropertyChangeListener, ItemListener {

		public void keyPressed(KeyEvent e) {
			if (e.getSource() == modelTable.getTable() && e.getKeyChar() == KeyEvent.VK_DELETE
					&& !modelTable.getTable().isEditing()) {
				removeSelectedFiles();
			}
		}

		public void keyReleased(KeyEvent e) {
		}

		public void keyTyped(KeyEvent e) {
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == uploadAddFilesButton) {
				new AddNewUploadsDialog(MainFrame.getInstance()).startDialog();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getSource() == modelTable.getTable() && e.getClickCount() == 2) {
				// Start file from download table. Is this a good idea?
				openFile(modelTable.getSelectedItem());
			}
		}

		public void propertyChange(PropertyChangeEvent evt) {
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

		public void itemStateChanged(ItemEvent e) {
			if (removeFinishedUploadsCheckBox.isSelected()) {
				model.removeFinishedUploads();
			}
			if (!showExternalGlobalQueueItems.isSelected()) {
				model.removeExternalUploads();
			}

			Core.frostSettings.setValue(Settings.UPLOAD_REMOVE_FINISHED,
					removeFinishedUploadsCheckBox.isSelected());
			Core.frostSettings.setValue(Settings.GQ_SHOW_EXTERNAL_ITEMS_UPLOAD,
					showExternalGlobalQueueItems.isSelected());
		}
	}

	@Override
	public void languageChanged(LanguageEvent event) {
		uploadAddFilesButton.setToolTipText(language.getString("UploadPane.toolbar.tooltip.browse") + "...");

		String waiting = language.getString("UploadPane.toolbar.waiting");
		Dimension labelSize = calculateLabelSize(waiting + ": 00000");
		uploadItemCountLabel.setPreferredSize(labelSize);
		uploadItemCountLabel.setMinimumSize(labelSize);
		uploadItemCountLabel.setText(waiting + ": " + uploadItemCount);

		removeFinishedUploadsCheckBox.setText(language.getString("UploadPane.removeFinishedUploads"));
		showExternalGlobalQueueItems.setText(language.getString("UploadPane.showExternalGlobalQueueItems"));
		compressUploadsCheckBox.setText(language.getString("UploadPane.compressUploads"));

		copyKeysAndNamesAction.setText(language.getString("Common.copyToClipBoard.copyKeysWithFilenames"));
		copyExtendedInfoAction.setText(language.getString("Common.copyToClipBoard.copyExtendedInfo"));

		for (FreenetPriority priority : FreenetPriority.values()) {
			changePriorityActions.get(priority).setText(priority.getName());
		}
		changePriorityMenu.setText(language.getString("Common.priority.changePriority"));

		enableSelectedAction
				.setText(language.getString("UploadPane.fileTable.popupmenu.enableUploads.enableSelectedUploads"));
		disableSelectedAction
				.setText(language.getString("UploadPane.fileTable.popupmenu.enableUploads.disableSelectedUploads"));
		invertEnabledSelectedAction.setText(language
				.getString("UploadPane.fileTable.popupmenu.enableUploads.invertEnabledStateForSelectedUploads"));
		enableAllAction.setText(language.getString("UploadPane.fileTable.popupmenu.enableUploads.enableAllUploads"));
		disableAllAction.setText(language.getString("UploadPane.fileTable.popupmenu.enableUploads.disableAllUploads"));
		invertEnabledAllAction.setText(
				language.getString("UploadPane.fileTable.popupmenu.enableUploads.invertEnabledStateForAllUploads"));
		enabledMenu.setText(language.getString("UploadPane.fileTable.popupmenu.enableUploads") + "...");

		startSelectedNowAction.setText(language.getString("UploadPane.fileTable.popupmenu.startSelectedUploadsNow"));
		generateCHKForSelectedFilesAction
				.setText(language.getString("UploadPane.fileTable.popupmenu.startEncodingOfSelectedFiles"));
		uploadSelectedFilesAction.setText(language.getString("UploadPane.fileTable.popupmenu.uploadSelectedFiles"));
		removeSelectedFilesAction
				.setText(language.getString("UploadPane.fileTable.popupmenu.remove.removeSelectedFiles"));
		removeFromGlobalQueueAction.setText(language.getString("UploadPane.fileTable.popupmenu.removeFromGlobalQueue"));
		openFileAction.setText(language.getString("UploadPane.fileTable.popupmenu.showSharedFile"));
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		List<FrostUploadItem> selectedItems = modelTable.getSelectedItems();
		Boolean isSelected = !selectedItems.isEmpty();
		Boolean isPersistenceEnabled = PersistenceManager.isPersistenceEnabled();

		Boolean isItemInGlobalQueue = false;
		for (FrostUploadItem frostUploadItem : selectedItems) {
			if (FileTransferManager.inst().getPersistenceManager().isItemInGlobalQueue(frostUploadItem)) {
				isItemInGlobalQueue = true;
			}
		}

		copyKeysAndNamesAction.setEnabled(isSelected);
		copyExtendedInfoAction.setEnabled(isSelected);
		changePriorityMenu.setEnabled(isSelected && isPersistenceEnabled);
		enabledMenu.setEnabled(isSelected);
		startSelectedNowAction.setEnabled(isSelected);
		generateCHKForSelectedFilesAction.setEnabled(isSelected);
		uploadSelectedFilesAction.setEnabled(isSelected);
		removeSelectedFilesAction.setEnabled(isSelected);
		removeFromGlobalQueueAction.setEnabled(isSelected && isPersistenceEnabled && isItemInGlobalQueue);
		openFileAction.setEnabled(isSelected);
	}

	private class CopyKeysAndNamesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			ClipboardUtil.copyKeysAndFilenames(modelTable.getSelectedItems());
		}
	}

	private class CopyExtendedInfoAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			ClipboardUtil.copyExtendedInfo(modelTable.getSelectedItems());
		}
	}

	private class ChangePriorityAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		private FreenetPriority priority;

		public ChangePriorityAction(FreenetPriority priority) {
			this.priority = priority;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			changeItemPriorites(modelTable.getSelectedItems(), priority);
		}
	}

	private class SetEnabledSelectedItemsAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		private Boolean enabled;

		public SetEnabledSelectedItemsAction(Boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			model.setItemsEnabled(enabled, modelTable.getSelectedItems());
		}
	}

	private class SetEnabledAllItemsAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		private Boolean enabled;

		public SetEnabledAllItemsAction(Boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			model.setAllItemsEnabled(enabled);
		}
	}

	private class StartSelectedNowAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			List<FrostUploadItem> itemsToStart = new LinkedList<FrostUploadItem>();

			for (FrostUploadItem frostUploadItem : modelTable.getSelectedItems()) {
				if (frostUploadItem.isExternal() || frostUploadItem.getState() != FrostUploadItem.STATE_WAITING) {
					continue;
				}
				itemsToStart.add(frostUploadItem);
			}

			for (FrostUploadItem ulItem : itemsToStart) {
				ulItem.setEnabled(true);
				FileTransferManager.inst().getUploadManager().startUpload(ulItem);
			}
		}
	}

	private class GenerateCHKForSelectedFilesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			model.generateChkItems(modelTable.getSelectedItems());
		}
	}

	private class UploadSelectedFilesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			model.uploadItems(modelTable.getSelectedItems());
		}
	}

	private class RemoveSelectedFilesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			removeSelectedFiles();
		}
	}

	private class RemoveFromGlobalQueueAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (FileTransferManager.inst().getPersistenceManager() == null) {
				return;
			}
			List<FrostUploadItem> selectedItems = modelTable.getSelectedItems();
			List<String> requestsToRemove = new ArrayList<String>();
			List<FrostUploadItem> itemsToUpdate = new ArrayList<FrostUploadItem>();
			for (FrostUploadItem item : selectedItems) {
				if (FileTransferManager.inst().getPersistenceManager().isItemInGlobalQueue(item)) {
					requestsToRemove.add(item.getGqIdentifier());
					itemsToUpdate.add(item);
					item.setInternalRemoveExpected(true);
				}
			}
			FileTransferManager.inst().getPersistenceManager().removeRequests(requestsToRemove);
			// after remove, update state of removed items
			for (FrostUploadItem item : itemsToUpdate) {
				item.setState(FrostUploadItem.STATE_WAITING);
				item.setEnabled(false);
				item.setPriority(FreenetPriority.PAUSE);
				item.fireValueChanged();
			}
		}
	}

	private class OpenFileAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			openFile(modelTable.getSelectedItem());
		}
	}

	private class PopupMenuUpload extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		public PopupMenuUpload() {
			add(copyKeysAndNamesAction);
			add(copyExtendedInfoAction);
			addSeparator();

			for (FreenetPriority priority : FreenetPriority.values()) {
				changePriorityMenu.add(changePriorityActions.get(priority));
			}
			add(changePriorityMenu);
			addSeparator();

			enabledMenu.add(enableSelectedAction);
			enabledMenu.add(disableSelectedAction);
			enabledMenu.add(invertEnabledSelectedAction);
			enabledMenu.addSeparator();
			enabledMenu.add(enableAllAction);
			enabledMenu.add(disableAllAction);
			enabledMenu.add(invertEnabledAllAction);
			add(enabledMenu);

			add(startSelectedNowAction);
			add(generateCHKForSelectedFilesAction);
			add(uploadSelectedFilesAction);
			addSeparator();
			add(removeSelectedFilesAction);
			add(removeFromGlobalQueueAction);
			addSeparator();
			add(openFileAction);
		}
	}
}
