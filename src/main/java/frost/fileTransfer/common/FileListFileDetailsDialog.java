/*
 SearchItemPropertiesDialog.java / Frost
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
package frost.fileTransfer.common;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.FrostFileListFileObject;
import frost.fileTransfer.FrostFileListFileObjectOwner;
import frost.fileTransfer.search.SearchParameters;
import frost.identities.Identity;
import frost.identities.LocalIdentity;
import frost.messaging.frost.gui.MessagePanel.IdentityState;
import frost.util.ClipboardUtil;
import frost.util.gui.SelectRowOnRightClick;
import frost.util.gui.SimplePopupMenuListener;
import frost.util.gui.action.BaseAction;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;
import frost.util.model.SortedModelTable;

public class FileListFileDetailsDialog extends JDialog implements LanguageListener, SimplePopupMenuListener {

	private static final long serialVersionUID = 1L;

	private transient Language language;

    private JPanel jContentPane = null;
    private JPanel buttonPanel = null;
    private JPanel mainPanel = null;
    private JButton Bclose = null;

	private SortedModelTable<FileListFileDetailsItem> modelTable;
	private transient FileListFileDetailsTableModel model;
	private transient FileListFileDetailsTableFormat tableFormat;

	private CopyKeysAndNamesAction copyKeysAndNamesAction;
	private JMenu copyToClipboardMenu;
	private ChangeTrustStateAction changeTrustStateGoodAction;
	private ChangeTrustStateAction changeTrustStateObserveAction;
	private ChangeTrustStateAction changeTrustStateCheckAction;
	private ChangeTrustStateAction changeTrustStateBadAction;
	private ShowOwnerFilesAction showOwnerFilesAction;
	private PopupMenu popupMenu;

    private boolean isOwnerSearchAllowed;

	public FileListFileDetailsDialog(Frame owner, boolean allowOwnerSearch) {
		super(owner);
		this.isOwnerSearchAllowed = allowOwnerSearch;

		language = Language.getInstance();
		language.addLanguageListener(this);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setContentPane(getJContentPane());
		setTitle(language.getString("FileListFileDetailsDialog.title"));
		loadLayout();
		setLocationRelativeTo(owner);

		copyKeysAndNamesAction = new CopyKeysAndNamesAction();
		copyToClipboardMenu = new JMenu();
		changeTrustStateGoodAction = new ChangeTrustStateAction(IdentityState.GOOD);
		changeTrustStateObserveAction = new ChangeTrustStateAction(IdentityState.OBSERVE);
		changeTrustStateCheckAction = new ChangeTrustStateAction(IdentityState.CHECK);
		changeTrustStateBadAction = new ChangeTrustStateAction(IdentityState.BAD);
		showOwnerFilesAction = new ShowOwnerFilesAction();
		popupMenu = new PopupMenu();
		popupMenu.addPopupMenuListener(this);
		modelTable.getTable().setComponentPopupMenu(popupMenu);
		modelTable.getTable().addMouseListener(new SelectRowOnRightClick(modelTable.getTable()));

		languageChanged(null);
	}

	@Override
	public void dispose() {
		language.removeLanguageListener(this);
		super.dispose();
	}

	private void loadLayout() {
        int lastHeight = Core.frostSettings.getInteger(Settings.FILE_LIST_FILE_DETAILS_DIALOG_HEIGHT);
        int lastWidth = Core.frostSettings.getInteger(Settings.FILE_LIST_FILE_DETAILS_DIALOG_WIDTH);

        final Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();

        if (lastWidth < 100) {
            lastWidth = 600;
        }
        if (lastWidth > scrSize.width) {
            lastWidth = scrSize.width;
        }

        if (lastHeight < 100) {
            lastHeight = 370;
        }
        if (lastHeight > scrSize.height) {
            lastWidth = scrSize.height;
        }
        setSize(lastWidth, lastHeight);
    }

    private void saveLayout() {
        // dialog size
        final Rectangle bounds = getBounds();
        Core.frostSettings.setValue(Settings.FILE_LIST_FILE_DETAILS_DIALOG_HEIGHT, bounds.height);
        Core.frostSettings.setValue(Settings.FILE_LIST_FILE_DETAILS_DIALOG_WIDTH, bounds.width);

        tableFormat.saveTableLayout(getModelTable());
    }

    /**
     * This method initializes jContentPane
     */
    private JPanel getJContentPane() {
        if( jContentPane == null ) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(getButtonPanel(), BorderLayout.SOUTH);
            jContentPane.add(getMainPanel(), BorderLayout.CENTER);
        }
        return jContentPane;
    }

    /**
     * This method initializes buttonPanel
     */
    private JPanel getButtonPanel() {
        if( buttonPanel == null ) {
            final FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(FlowLayout.RIGHT);
            buttonPanel = new JPanel();
            buttonPanel.setLayout(flowLayout);
            buttonPanel.add(getBclose(), null);
        }
        return buttonPanel;
    }

    /**
     * This method initializes mainPanel
     */
    private JPanel getMainPanel() {
        if( mainPanel == null ) {
            mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            mainPanel.add( getModelTable().getScrollPane(), BorderLayout.CENTER);
        }
        return mainPanel;
    }

    private SortedModelTable<FileListFileDetailsItem> getModelTable() {
        if( modelTable == null ) {
            tableFormat = new FileListFileDetailsTableFormat();
            model = new FileListFileDetailsTableModel(tableFormat);
            modelTable = new SortedModelTable<FileListFileDetailsItem>(model);
        }
        return modelTable;
    }

    /**
     * This method initializes Bok
     */
    private JButton getBclose() {
        if( Bclose == null ) {
            Bclose = new JButton();
            Bclose.setText(language.getString("FileListFileDetailsDialog.button.close"));
            Bclose.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    saveLayout();
                    setVisible(false);
                }
            });
        }
        return Bclose;
    }

    public void startDialog(final FrostFileListFileObject fileObject) {
        for( final Iterator<FrostFileListFileObjectOwner> i = fileObject.getFrostFileListFileObjectOwnerIterator(); i.hasNext(); ) {
            final FrostFileListFileObjectOwner o = i.next();
            final FileListFileDetailsItem item = new FileListFileDetailsItem(o);
            model.addPropertiesItem(item);
        }
        setVisible(true);
    }

	@Override
	public void languageChanged(LanguageEvent event) {
		copyKeysAndNamesAction.setText(language.getString("Common.copyToClipBoard.copyKeysWithFilenames"));
		copyToClipboardMenu.setText(language.getString("Common.copyToClipBoard") + "...");
		changeTrustStateGoodAction.setText(language.getString("MessagePane.messageTable.popupmenu.setToGood"));
		changeTrustStateObserveAction.setText(language.getString("MessagePane.messageTable.popupmenu.setToObserve"));
		changeTrustStateCheckAction.setText(language.getString("MessagePane.messageTable.popupmenu.setToCheck"));
		changeTrustStateBadAction.setText(language.getString("MessagePane.messageTable.popupmenu.setToBad"));
		showOwnerFilesAction.setText(language.getString("FileListFileDetailsDialog.popupmenu.searchFilesOfOwner"));
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		List<FileListFileDetailsItem> selectedItems = modelTable.getSelectedItems();
		Boolean isSelected = !selectedItems.isEmpty();
		Boolean isOneSelected = selectedItems.size() == 1;

		Identity ownerId = null;
		Boolean isLocalOwner = false;
		if (isOneSelected) {
			ownerId = selectedItems.get(0).getOwnerIdentity();
			isLocalOwner = ownerId instanceof LocalIdentity;
		}

		copyToClipboardMenu.setEnabled(isSelected);
		changeTrustStateGoodAction.setEnabled(isOneSelected && !isLocalOwner && !ownerId.isGOOD());
		changeTrustStateObserveAction.setEnabled(isOneSelected && !isLocalOwner && !ownerId.isOBSERVE());
		changeTrustStateCheckAction.setEnabled(isOneSelected && !isLocalOwner && !ownerId.isCHECK());
		changeTrustStateBadAction.setEnabled(isOneSelected && !isLocalOwner && !ownerId.isBAD());
		showOwnerFilesAction.setEnabled(isOneSelected && isOwnerSearchAllowed);
	}

	private class CopyKeysAndNamesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			ClipboardUtil.copyKeysAndFilenames(modelTable.getSelectedItems());
		}
	}

	private class ChangeTrustStateAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		private IdentityState identityState;

		public ChangeTrustStateAction(IdentityState identityState) {
			this.identityState = identityState;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			List<FileListFileDetailsItem> selectedItems = modelTable.getSelectedItems();
			FileListFileDetailsItem item = selectedItems.get(0);
			if (identityState == IdentityState.GOOD) {
				item.getOwnerIdentity().setGOOD();
			} else if (identityState == IdentityState.CHECK) {
				item.getOwnerIdentity().setCHECK();
			} else if (identityState == IdentityState.OBSERVE) {
				item.getOwnerIdentity().setOBSERVE();
			} else if (identityState == IdentityState.BAD) {
				item.getOwnerIdentity().setBAD();
			}
			modelTable.fireTableRowsUpdated(0, modelTable.getRowCount() - 1);
			// also update message panel to reflect the identity change
			MainFrame.getInstance().getMessagePanel().updateTableAfterChangeOfIdentityState();
		}
	}

	private class ShowOwnerFilesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			List<FileListFileDetailsItem> selectedItems = modelTable.getSelectedItems();
			FileListFileDetailsItem item = selectedItems.get(0);
			String owner = item.getOwnerIdentity().getUniqueName();

			SearchParameters sp = new SearchParameters(false);
			sp.setOwnerString(owner);
			FileTransferManager.inst().getSearchManager().getPanel().startNewSearch(sp);
		}
	}

	private class PopupMenu extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		public PopupMenu() {
			copyToClipboardMenu.add(copyKeysAndNamesAction);
			add(copyToClipboardMenu);
			addSeparator();
			add(changeTrustStateGoodAction);
			add(changeTrustStateObserveAction);
			add(changeTrustStateCheckAction);
			add(changeTrustStateBadAction);
			addSeparator();
			add(showOwnerFilesAction);
		}
	}
}
