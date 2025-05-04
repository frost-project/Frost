/*
  SearchTable.java / Frost
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
package frost.fileTransfer.search;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fileTransfer.common.FileListFileDetailsDialog;
import frost.storage.perst.filelist.FileListStorage;
import frost.util.ClipboardUtil;
import frost.util.gui.CloseableTabbedPane;
import frost.util.gui.SelectRowOnRightClick;
import frost.util.gui.SimplePopupMenuListener;
import frost.util.gui.action.BaseAction;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;
import frost.util.model.SortedModelTable;

public class SearchTable extends SortedModelTable<FrostSearchItem>
		implements LanguageListener, SimplePopupMenuListener {

	private static final long serialVersionUID = 1L;

	private transient SearchModel searchModel;
    private final CloseableTabbedPane tabPane;
    private final String searchText;

	private CopyKeysAndNamesAction copyKeysAndNamesAction;
	private CopyExtendedInfoAction copyExtendedInfoAction;
	private JMenu copyToClipboardMenu;
	private DownloadSelectedKeysAction downloadSelectedKeysAction;
	private DownloadAllKeysAction downloadAllKeysAction;
	private HideSelectedKeysAction hideSelectedKeysAction;
	private DetailsAction detailsAction;
	private PopupMenuSearch popupMenuSearch;

	private transient Language language;

	private transient List<FrostSearchItem> searchItems = new LinkedList<FrostSearchItem>();

	public SearchTable(SearchModel m, CloseableTabbedPane t, String searchText) {
		super(m);

		language = Language.getInstance();
		language.addLanguageListener(this);

        searchModel = m;
        tabPane = t;
        this.searchText = searchText;

        setupTableFont();

		copyKeysAndNamesAction = new CopyKeysAndNamesAction();
		copyExtendedInfoAction = new CopyExtendedInfoAction();
		copyToClipboardMenu = new JMenu();
		downloadSelectedKeysAction = new DownloadSelectedKeysAction();
		downloadAllKeysAction = new DownloadAllKeysAction();
		hideSelectedKeysAction = new HideSelectedKeysAction();
		detailsAction = new DetailsAction();
		popupMenuSearch = new PopupMenuSearch();
		popupMenuSearch.addPopupMenuListener(this);
		getTable().setComponentPopupMenu(popupMenuSearch);

		getTable().addMouseListener(new DoubleClickListener());
		getTable().addMouseListener(new SelectRowOnRightClick(getTable()));

		languageChanged(null);
	}

    public void addSearchItem(final FrostSearchItem i) {
        searchItems.add(i);
    }

    /**
     * Called if the searchthread finished.
     */
    public void searchFinished(final Component tabComponent) {
		// add all cached items to model
        for( final FrostSearchItem fsi : searchItems ) {
            searchModel.addSearchItem(fsi);
        }
        searchItems.clear();

        final int myIx = tabPane.indexOfComponent(tabComponent);
        final String newTitle = searchText + " ("+searchModel.getItemCount()+")";
        tabPane.setTitleAt(myIx, newTitle);
    }

    public void searchCancelled() {
        searchItems.clear();
    }

    private void setupTableFont() {
        final String fontName = Core.frostSettings.getValue(Settings.FILE_LIST_FONT_NAME);
        final int fontStyle = Core.frostSettings.getIntValue(Settings.FILE_LIST_FONT_STYLE);
        final int fontSize = Core.frostSettings.getIntValue(Settings.FILE_LIST_FONT_SIZE);
        Font font = new Font(fontName, fontStyle, fontSize);
        if (!font.getFamily().equals(fontName)) {
            Core.frostSettings.setValue(Settings.FILE_LIST_FONT_NAME, "SansSerif");
            font = new Font("SansSerif", fontStyle, fontSize);
        }
        getTable().setFont(font);
    }

    /**
     * Add selected items, or all item if called with null, to the download table.
     * Updates state of item in search table.
     */
    private void addItemsToDownloadTable(List <FrostSearchItem> selectedItems) {
        if( selectedItems == null ) {
            // add all items
            selectedItems = searchModel.getItems();
        }

        searchModel.addItemsToDownloadTable(selectedItems);

        // redraw items in model
        for( final FrostSearchItem selectedItem : selectedItems ) {
            final int i = model.indexOf(selectedItem);
            fireTableRowsUpdated(i,i);
        }
    }

	private class DoubleClickListener extends MouseAdapter {

		@Override
		public void mousePressed(final MouseEvent e) {
			if (e.getClickCount() == 2 && e.getSource() == getTable()) {
				// if double click was on the sourceCount cell then maybe show details
				int row = getTable().rowAtPoint(e.getPoint());
				int col = getTable().columnAtPoint(e.getPoint());
				if (row > -1 && col == 8) {
					detailsAction.actionPerformed(null);
					return;
				}

				addItemsToDownloadTable(getSelectedItems());
			}
		}
	}

	@Override
	public void languageChanged(LanguageEvent event) {
		copyKeysAndNamesAction.setText(language.getString("Common.copyToClipBoard.copyKeysWithFilenames"));
		copyExtendedInfoAction.setText(language.getString("Common.copyToClipBoard.copyExtendedInfo"));
		copyToClipboardMenu.setText(language.getString("Common.copyToClipBoard") + "...");
		downloadSelectedKeysAction.setText(language.getString("SearchPane.resultTable.popupmenu.downloadSelectedKeys"));
		downloadAllKeysAction.setText(language.getString("SearchPane.resultTable.popupmenu.downloadAllKeys"));
		hideSelectedKeysAction.setText(language.getString("SearchPane.resultTable.popupmenu.hideSelectedKeys"));
		detailsAction.setText(language.getString("Common.details"));
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		List<FrostSearchItem> selectedItems = getSelectedItems();
		Boolean isSelected = !selectedItems.isEmpty();
		Boolean isOneSelected = selectedItems.size() == 1;

		copyToClipboardMenu.setEnabled(isSelected);
		downloadSelectedKeysAction.setEnabled(isSelected);
		hideSelectedKeysAction.setEnabled(isSelected);
		detailsAction.setEnabled(isOneSelected);
	}

	private class CopyKeysAndNamesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			ClipboardUtil.copyKeysAndFilenames(getSelectedItems());
		}
	}

	private class CopyExtendedInfoAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			ClipboardUtil.copyExtendedInfo(getSelectedItems());
		}
	}

	private class DownloadSelectedKeysAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			addItemsToDownloadTable(getSelectedItems());
		}
	}

	private class DownloadAllKeysAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			addItemsToDownloadTable(null);
		}
	}

	private class HideSelectedKeysAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			List<FrostSearchItem> selectedItems = getSelectedItems();

			// update the filelistfiles in database (but not in Swing thread)
			new Thread() {
				@Override
				public void run() {

					if (FileListStorage.inst().beginExclusiveThreadTransaction()) {
						try {
							for (int x = selectedItems.size() - 1; x >= 0; x--) {
								FrostSearchItem si = selectedItems.get(x);
								if (si.getFrostFileListFileObject() != null) {
									FileListStorage.inst().markFileListFileHidden(si.getFrostFileListFileObject());
								}
							}
						} finally {
							FileListStorage.inst().endThreadTransaction();
						}
					}
				}
			}.start();

			// remove from table
			model.removeItems(selectedItems);
		}
	}

	private class DetailsAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			List<FrostSearchItem> selectedItems = getSelectedItems();
			new FileListFileDetailsDialog(MainFrame.getInstance(), true)
					.startDialog(selectedItems.get(0).getFrostFileListFileObject());
		}
	}

	private class PopupMenuSearch extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		public PopupMenuSearch() {
			copyToClipboardMenu.add(copyKeysAndNamesAction);
			copyToClipboardMenu.add(copyExtendedInfoAction);
			add(copyToClipboardMenu);
			addSeparator();
			add(downloadSelectedKeysAction);
			add(downloadAllKeysAction);
			addSeparator();
			add(hideSelectedKeysAction);
			addSeparator();
			add(detailsAction);
		}
	}
}
