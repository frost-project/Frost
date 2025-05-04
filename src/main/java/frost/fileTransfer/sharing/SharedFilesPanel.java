/*
  SharedFilesPanel.java / Frost
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.event.PopupMenuEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.filelist.FileListUploadThread;
import frost.storage.perst.NewUploadFile;
import frost.util.ClipboardUtil;
import frost.util.FileAccess;
import frost.util.FormatterUtils;
import frost.util.gui.MiscToolkit;
import frost.util.gui.SelectRowOnRightClick;
import frost.util.gui.SimplePopupMenuListener;
import frost.util.gui.action.BaseAction;
import frost.util.gui.search.TableFindAction;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;
import frost.util.model.ModelTable;
import frost.util.model.SortedModelListener;
import frost.util.model.SortedModelTable;

public class SharedFilesPanel extends JPanel implements LanguageListener, SimplePopupMenuListener {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(SharedFilesPanel.class);

	private CopyKeysAndNamesAction copyKeysAndNamesAction;
	private CopyExtendedInfoAction copyExtendedInfoAction;
	private JMenu copyToClipboardMenu;
	private RemoveSelectedFilesAction removeSelectedFilesAction;
	private UploadSelectedFilesAction uploadSelectedFilesAction;
	private PropertiesAction propertiesAction;
	private PopupMenu popupMenu;

	private transient Listener listener = new Listener();

	private transient SharedFilesModel model;

	private transient FileTransferManager fileTransferManager;

	private transient Language language;

    private final JToolBar sharedFilesToolBar = new JToolBar();
    private final JButton addSharedFilesButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/folder-open.png"));

    private int sharedFilesCount = 0;
    private final JLabel sharedFilesCountLabel = new JLabel();

    private SortedModelTable<FrostSharedFileItem> modelTable;

	protected SharedFilesPanel(FileTransferManager fileTransferManager, SharedFilesModel model) {
		super();

		this.fileTransferManager = fileTransferManager;
		this.model = model;

		language = Language.getInstance();
		language.addLanguageListener(this);

		// create the top panel
		MiscToolkit.configureButton(addSharedFilesButton);

		sharedFilesToolBar.setRollover(true);
		sharedFilesToolBar.setFloatable(false);

		sharedFilesToolBar.add(addSharedFilesButton);
		sharedFilesToolBar.add(Box.createRigidArea(new Dimension(80, 0)));
		sharedFilesToolBar.add(Box.createHorizontalGlue());
		sharedFilesToolBar.add(sharedFilesCountLabel);

		// create the main upload panel
		modelTable = new SortedModelTable<FrostSharedFileItem>(model);
		new TableFindAction().install(modelTable.getTable());
		setLayout(new BorderLayout());
		add(sharedFilesToolBar, BorderLayout.NORTH);
		add(modelTable.getScrollPane(), BorderLayout.CENTER);
		fontChanged();

		copyKeysAndNamesAction = new CopyKeysAndNamesAction();
		copyExtendedInfoAction = new CopyExtendedInfoAction();
		copyToClipboardMenu = new JMenu();
		removeSelectedFilesAction = new RemoveSelectedFilesAction();
		uploadSelectedFilesAction = new UploadSelectedFilesAction();
		propertiesAction = new PropertiesAction();
		popupMenu = new PopupMenu();
		popupMenu.addPopupMenuListener(this);
		modelTable.getTable().setComponentPopupMenu(popupMenu);

		modelTable.getTable().addMouseListener(new SelectRowOnRightClick(modelTable.getTable()));
		addSharedFilesButton.addActionListener(listener);
		modelTable.getTable().addKeyListener(listener);
		modelTable.getTable().addMouseListener(listener);

		Core.frostSettings.addPropertyChangeListener(Settings.FILE_LIST_FONT_NAME, listener);
		Core.frostSettings.addPropertyChangeListener(Settings.FILE_LIST_FONT_SIZE, listener);
		Core.frostSettings.addPropertyChangeListener(Settings.FILE_LIST_FONT_STYLE, listener);

		model.addOrderedModelListener(new SortedModelListener<FrostSharedFileItem>() {
			public void modelCleared() {
				updateSharedFilesItemCount();
			}

			public void itemAdded(int position, FrostSharedFileItem item) {
				updateSharedFilesItemCount();
			}

			public void itemChanged(int position, FrostSharedFileItem item) {
			}

			public void itemsRemoved(int[] positions, List<FrostSharedFileItem> items) {
				updateSharedFilesItemCount();
			}
		});

		languageChanged(null);
	}

    public SharedFilesTableFormat getTableFormat() {
        return (SharedFilesTableFormat) modelTable.getTableFormat();
    }

    public ModelTable<FrostSharedFileItem> getModelTable() {
        return modelTable;
    }

    public void setAddFilesButtonEnabled(final boolean enabled) {
        addSharedFilesButton.setEnabled(enabled);
    }

    private Dimension calculateLabelSize(final String text) {
        final JLabel dummyLabel = new JLabel(text);
        dummyLabel.doLayout();
        return dummyLabel.getPreferredSize();
    }

    public void uploadAddFilesButton_actionPerformed(final ActionEvent e) {

        final JFileChooser fc = new JFileChooser(Core.frostSettings.getString(Settings.DIR_LAST_USED));
        fc.setDialogTitle(language.getString("SharedFilesPane.filechooser.title"));
        fc.setFileHidingEnabled(true);
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        fc.setPreferredSize(new Dimension(600, 400));

        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File[] selectedFiles = fc.getSelectedFiles();
        if( selectedFiles == null || selectedFiles.length == 0 ) {
            return;
        }

        // ask for owner to use
        final SharedFilesOwnerDialog dlg =
            new SharedFilesOwnerDialog(MainFrame.getInstance(), language.getString("SharedFilesOwnerDialog.title"));
        if( dlg.showDialog() == SharedFilesOwnerDialog.CANCEL ) {
            return;
        }
        final String owner = dlg.getChoosedIdentityName();
        final boolean replacePathIfFileExists = dlg.isReplacePathIfFileExists();

        // start external thread to check the filelength and add NewUploadFile objects
        new AddNewSharedFilesThread(selectedFiles, owner, replacePathIfFileExists).start();
    }

    /**
     * Thread reads a list of new shared files, creates NewUploadFile objects and adds them to the NewUploadFilesManager.
     */
    private class AddNewSharedFilesThread extends Thread {
        private final File[] selectedFiles;
        private final String owner;
        private final boolean replacePathIfFileExists;
        public AddNewSharedFilesThread(final File[] selectedFiles, final String owner, final boolean replacePath) {
            super();
            this.selectedFiles = selectedFiles;
            this.owner = owner;
            this.replacePathIfFileExists = replacePath;
        }
        @Override
        public void run() {
            final List<File> uploadFileItems = new LinkedList<File>();
            for( final File element : selectedFiles ) {
                // collect all choosed files + files in all choosed directories
                uploadFileItems.addAll( FileAccess.getAllEntries(element) );
            }

            // remember last upload dir
            if (uploadFileItems.size() > 0) {
                final File file = uploadFileItems.get(0);
                Core.frostSettings.setValue(Settings.DIR_LAST_USED, file.getParent());
            }

            // create list of NewUploadFile objects
            final List<NewUploadFile> uploadItems = new LinkedList<NewUploadFile>();
            for( final File file : uploadFileItems ) {
                final NewUploadFile nuf = new NewUploadFile(file, owner, replacePathIfFileExists);
                uploadItems.add(nuf);
            }

            // notify list upload thread about a change in the filelist
            FileListUploadThread.getInstance().userActionOccured();

            fileTransferManager.getNewUploadFilesManager().addNewUploadFiles(uploadItems);
        }
    }

    private void fontChanged() {
        final String fontName = Core.frostSettings.getString(Settings.FILE_LIST_FONT_NAME);
        final int fontStyle = Core.frostSettings.getInteger(Settings.FILE_LIST_FONT_STYLE);
        final int fontSize = Core.frostSettings.getInteger(Settings.FILE_LIST_FONT_SIZE);
        Font font = new Font(fontName, fontStyle, fontSize);
        if (!font.getFamily().equals(fontName)) {
            logger.error("The selected font was not found in your system");
            logger.error("That selection will be changed to \"SansSerif\".");
            Core.frostSettings.setValue(Settings.FILE_LIST_FONT_NAME, "SansSerif");
            font = new Font("SansSerif", fontStyle, fontSize);
        }
        modelTable.setFont(font);
    }

    private void updateSharedFilesItemCount() {
        sharedFilesCount = model.getItemCount();
        long sharedFilesSize = 0;
        for( final FrostSharedFileItem sfi : model.getItems() ) {
            sharedFilesSize += sfi.getFileSize();
        }

        final String s =
            new StringBuilder()
                .append(language.getString("SharedFilesPane.toolbar.files"))
                .append(": ")
                .append(sharedFilesCount)
                .append("  (")
                .append(FormatterUtils.formatSize(sharedFilesSize))
                .append(")")
                .toString();
        sharedFilesCountLabel.setText(s);
    }

    private boolean stringsEqual(final String oldStr, final String newStr) {
        if( oldStr == null && newStr != null ) {
            return false;
        }
        if( oldStr != null && newStr == null ) {
            return false;
        }
        if( oldStr == null && newStr == null ) {
            return true;
        }
        if( oldStr.equals(newStr) ) {
            return true;
        } else {
            return false;
        }
    }

	private class Listener extends MouseAdapter implements KeyListener, ActionListener, PropertyChangeListener {

		public void keyPressed(KeyEvent e) {
			if (e.getSource() == modelTable.getTable() && e.getKeyChar() == KeyEvent.VK_DELETE
					&& !modelTable.getTable().isEditing()) {
				removeSelectedFilesAction.actionPerformed(null);
			}
		}

		public void keyReleased(KeyEvent e) {
		}

		public void keyTyped(KeyEvent e) {
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == addSharedFilesButton) {
				uploadAddFilesButton_actionPerformed(e);
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1
					&& e.getSource() == modelTable.getTable()) {
				// Start file from download table. Is this a good idea?
				propertiesAction.actionPerformed(null);
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
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		List<FrostSharedFileItem> selectedItems = modelTable.getSelectedItems();
		Boolean isSelected = !selectedItems.isEmpty();

		// if all selected items are valid, then show long menu
		Boolean isAllValid = true;
		for (FrostSharedFileItem sfItem : selectedItems) {
			if (!sfItem.isValid()) {
				isAllValid = false;
				break;
			}
		}

		copyToClipboardMenu.setEnabled(isSelected && isAllValid);
		removeSelectedFilesAction.setEnabled(isSelected);
		uploadSelectedFilesAction.setEnabled(isSelected && isAllValid);
		propertiesAction.setEnabled(isSelected && isAllValid);
	}

	@Override
	public void languageChanged(LanguageEvent event) {
		addSharedFilesButton.setToolTipText(language.getString("SharedFilesPane.toolbar.tooltip.browse") + "...");

		String waiting = language.getString("SharedFilesPane.toolbar.files");
		Dimension labelSize = calculateLabelSize(waiting + ": 00000  (9876MB)");
		sharedFilesCountLabel.setPreferredSize(labelSize);
		sharedFilesCountLabel.setMinimumSize(labelSize);
		sharedFilesCountLabel.setText(waiting + ": " + sharedFilesCount);

		copyKeysAndNamesAction.setText(language.getString("Common.copyToClipBoard.copyKeysWithFilenames"));
		copyExtendedInfoAction.setText(language.getString("Common.copyToClipBoard.copyExtendedInfo"));
		copyToClipboardMenu.setText(language.getString("Common.copyToClipBoard") + "...");
		removeSelectedFilesAction
				.setText(language.getString("SharedFilesPane.fileTable.popupmenu.removeSelectedFiles"));
		uploadSelectedFilesAction
				.setText(language.getString("SharedFilesPane.fileTable.popupmenu.uploadSelectedFiles"));
		propertiesAction.setText(language.getString("Common.properties"));
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

	private class RemoveSelectedFilesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			List<FrostSharedFileItem> selectedItems = modelTable.getSelectedItems();
			model.removeItems(selectedItems);
			modelTable.getTable().clearSelection();

			// currently running upload items are removed during next startup

			// notify list upload thread that user changed something
			FileListUploadThread.getInstance().userActionOccured();
		}
	}

	private class UploadSelectedFilesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			model.requestItems(modelTable.getSelectedItems());
		}
	}

	private class PropertiesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			List<FrostSharedFileItem> selectedItems = modelTable.getSelectedItems();

			List<FrostSharedFileItem> items = new LinkedList<FrostSharedFileItem>();
			for (FrostSharedFileItem item : selectedItems) {
				items.add(item);
			}
			FrostSharedFileItem defaultItem = items.get(0);
			SharedFilesPropertiesDialog dlg = new SharedFilesPropertiesDialog(MainFrame.getInstance());

			String singleFilename = null;
			int fileCount = 0;

			if (items.size() == 1) {
				singleFilename = defaultItem.getFile().getName();
			} else {
				fileCount = items.size();
			}

			boolean okClicked = dlg.startDialog(singleFilename, fileCount, defaultItem);
			if (!okClicked) {
				return;
			}

			for (FrostSharedFileItem item : items) {
				// check if item was really changed, calling a setter will mark the item changed
				String oldStr, newStr;

				oldStr = item.getComment();
				newStr = dlg.getComment();
				if (!stringsEqual(oldStr, newStr)) {
					item.setComment(dlg.getComment());
				}

				oldStr = item.getKeywords();
				newStr = dlg.getKeywords();
				if (!stringsEqual(oldStr, newStr)) {
					item.setKeywords(dlg.getKeywords());
				}

				if (item.getRating() != dlg.getRating()) {
					item.setRating(dlg.getRating());
				}
			}
		}
	}

	private class PopupMenu extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		public PopupMenu() {
			copyToClipboardMenu.add(copyKeysAndNamesAction);
			copyToClipboardMenu.add(copyExtendedInfoAction);
			add(copyToClipboardMenu);
			addSeparator();
			add(removeSelectedFilesAction);
			addSeparator();
			add(uploadSelectedFilesAction);
			addSeparator();
			add(propertiesAction);
		}
	}
}
