/*
  UnsentMessagestable.java / Frost
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
package frost.messaging.frost.gui.unsentmessages;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.messaging.frost.FrostMessageObject;
import frost.messaging.frost.FrostUnsentMessageObject;
import frost.messaging.frost.gui.MessageWindow;
import frost.util.gui.SelectRowOnRightClick;
import frost.util.gui.SimplePopupMenuListener;
import frost.util.gui.action.BaseAction;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;
import frost.util.model.SortedModelTable;

public class UnsentMessagesTable extends SortedModelTable<UnsentMessagesTableItem>
		implements LanguageListener, SimplePopupMenuListener {

	private static final long serialVersionUID = 1L;

	private transient UnsentMessagesTableModel tableModel;
	private transient UnsentMessagesTableFormat tableFormat;

	private DeleteMessageAction deleteMessageAction;
	private PopupMenu popup;

	private transient Language language = Language.getInstance();

    public UnsentMessagesTable() {
        this(new UnsentMessagesTableModel(new UnsentMessagesTableFormat()));
    }

    private UnsentMessagesTable(final UnsentMessagesTableModel m) {
        super(m);
        tableModel = m;
        tableFormat = (UnsentMessagesTableFormat)m.getTableFormat();

        setupTableFont();
        getTable().setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

		deleteMessageAction = new DeleteMessageAction();
		popup = new PopupMenu();
		popup.addPopupMenuListener(this);
		getTable().setComponentPopupMenu(popup);
		getTable().addMouseListener(new DoubleClickListener());
		getTable().addMouseListener(new SelectRowOnRightClick(getTable()));

		language.addLanguageListener(this);
		languageChanged(null);
	}

    public void addUnsentMessage(final FrostUnsentMessageObject i) {
        tableModel.addFrostUnsentMessageObject(i);
        MainFrame.getInstance().getMessagingTab().getUnsentMessagesPanel().updateUnsentMessagesCount();
    }

    public void removeUnsentMessage(final FrostUnsentMessageObject i) {
        tableModel.removeFrostUnsentMessageObject(i);
        MainFrame.getInstance().getMessagingTab().getUnsentMessagesPanel().updateUnsentMessagesCount();
    }

    public void updateUnsentMessage(final FrostUnsentMessageObject i) {
        tableModel.updateFrostUnsentMessageObject(i);
    }

    public void saveTableFormat() {
        tableFormat.saveTableLayout();
    }

    public void loadTableModel() {
        tableModel.loadTableModel();
        MainFrame.getInstance().getMessagingTab().getUnsentMessagesPanel().updateUnsentMessagesCount();
    }

    public void clearTableModel() {
        tableModel.clear();
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

	@Override
	public void languageChanged(LanguageEvent event) {
		deleteMessageAction.setText(language.getString("UnsentMessages.table.popup.deleteMessage"));
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		List<UnsentMessagesTableItem> selectedItems = getSelectedItems();
		Boolean isUploading = false;
		for (UnsentMessagesTableItem item : selectedItems) {
			if (item.getState() == UnsentMessagesTableItem.STATE_UPLOADING) {
				isUploading = true;
			}
		}

		deleteMessageAction.setEnabled(!selectedItems.isEmpty() && !isUploading);
	}

	private class DoubleClickListener extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getClickCount() == 2) {
				int row = getTable().rowAtPoint(e.getPoint());
				if (row > -1) {
					UnsentMessagesTableItem item = getItemAt(row); // It may be null
					if (item != null) {
						FrostMessageObject sm = item.getFrostUnsentMessageObject();
						MessageWindow messageWindow = new MessageWindow(MainFrame.getInstance(), sm,
								MainFrame.getInstance().getMessagingTab().getUnsentMessagesPanel().getSize(), false);
						messageWindow.setVisible(true);
					}
				}
			}
		}
	}

	private class DeleteMessageAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			List<UnsentMessagesTableItem> selectedItems = getSelectedItems();
			int answer;
			if (selectedItems.size() == 1) {
				answer = JOptionPane.showConfirmDialog(MainFrame.getInstance(),
						language.getString("UnsentMessages.confirmDeleteOneMessageDialog.text"),
						language.getString("UnsentMessages.confirmDeleteOneMessageDialog.title"),
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			} else {
				answer = JOptionPane.showConfirmDialog(MainFrame.getInstance(),
						language.formatMessage("UnsentMessages.confirmDeleteMessagesDialog.text", selectedItems.size()),
						language.getString("UnsentMessages.confirmDeleteMessagesDialog.title"),
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			}

			if (answer == JOptionPane.YES_OPTION) {
				FrostUnsentMessageObject failedItem = tableModel.deleteItems(selectedItems);
				if (failedItem != null) {
					JOptionPane.showMessageDialog(MainFrame.getInstance(),
							language.getString("UnsentMessages.deleteNotPossibleDialog.text"),
							language.getString("UnsentMessages.deleteNotPossibleDialog.title"),
							JOptionPane.ERROR_MESSAGE);
				}
				MainFrame.getInstance().getMessagingTab().getUnsentMessagesPanel().updateUnsentMessagesCount();
			}
		}
	}

	private class PopupMenu extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		public PopupMenu() {
			add(deleteMessageAction);
		}
	}
}
