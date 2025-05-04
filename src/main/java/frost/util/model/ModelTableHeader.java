/*
 ModelTableHeader.java / Frost
 Copyright (C) 2003  Frost Project <jtcfrost.sourceforge.net>

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
package frost.util.model;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

/**
 * This is subclass of JTableHeader that listens for mouse clicks on it.
 * It does nothing with them, but provides a couple of methods (headerClicked 
 * and headerReleased) that subclasses can override as necessary.
 */
abstract public class ModelTableHeader extends JTableHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * This inner class is the popup menu that will be shown to let the user choose
	 * which columns the table should show.
	 */
	private class ColumnsPopupMenu extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		@Override
        public void show(Component invoker, int x, int y) {
			removeAll();
			Iterator<TableColumn> columns = modelTable.getColumns();
			int i = 0;
			int shownColumns = 0;
			JCheckBoxMenuItem lastShownItem = null;
			while (columns.hasNext()) {
				JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(columns.next().getIdentifier().toString());
				if (modelTable.isColumnVisible(i)) {
					menuItem.setSelected(true);
					shownColumns++;
					lastShownItem = menuItem;
				} else {
					menuItem.setSelected(false);
				}
				menuItem.addActionListener(listener);
				add(menuItem);
				i++;
			}
			//If there is only one column showing, we disable its
			//checkbox to prevent the user from hiding that one too.
			if (shownColumns == 1) {
				lastShownItem.setEnabled(false);
			}
			super.show(invoker, x, y);
		}

	}

	/**
	 * This inner class listens for mouse clicks on the header and
	 * for selections in the popup menu
	 */
	private class Listener extends MouseAdapter implements ActionListener {

		/**
		 *	This constructor creates a new instance of Listener
		 */
		public Listener() {
			super();
		}

		@Override
        public void mouseClicked(MouseEvent e) {
			headerClicked(e);
		}

		@Override
        public void mouseReleased(MouseEvent e) {
			headerReleased(e);
		}

		public void actionPerformed(ActionEvent e) {
			if (popup != null) {
				int position = popup.getComponentIndex((Component) e.getSource());
				if (position != -1) {
					popupMenu_actionPerformed(position);
				}
			}
		}

	}

	private Listener listener = new Listener();

	private ModelTable<? extends ModelItem<?>> modelTable;

	private ColumnsPopupMenu popup;

	/**
	 * This constructor creates a new instance of ModelTableHeader associated
	 * to the ModelTable that is passed as a parameter.
	 * @param cm the ModelTable that is going to have this header
	 */
	public ModelTableHeader(ModelTable<? extends ModelItem<?>> newModelTable) {
		super(newModelTable.getTable().getColumnModel());

		modelTable = newModelTable;

		addMouseListener(listener);
	}

	/**
	 * This method is called whenever the user clicks on the header
	 * (when the mouse button is pressed)
	 * @param e the MouseEvent
	 */
	protected void headerClicked(MouseEvent e) {
		if (e.isPopupTrigger()) {
			getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
		}
	}

	/**
	 * This method is called whenever the user clicks on the header
	 * (when the mouse button is released)
	 * @param e the MouseEvent
	 */
	protected void headerReleased(MouseEvent e) {
		if (e.isPopupTrigger()) {
			getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
		}
	}

	/**
	 * This method is called whenever a MenuItem of the
	 * ColumnsPopupMenu is selected
	 * @param position the position of the MenuItem in the 
	 * 					ColumnsPopupMenu that was selected
	 */
	private void popupMenu_actionPerformed(int position) {
		JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) popup.getComponent(position);
		if (menuItem.isSelected()) {
			modelTable.setColumnVisible(position, true);
		} else {
			modelTable.setColumnVisible(position, false);
		}
	}

	/**
	 * This method returns a reference to the inner ColumnsPopupMenu, 
	 * creating one instance of it if it hadn't been created yet.
	 * @return an instance of ColumnsPopupMenu
	 */
	private JPopupMenu getPopupMenu() {
		if (popup == null) {
			popup = new ColumnsPopupMenu();
		}
		return popup;
	}

}
