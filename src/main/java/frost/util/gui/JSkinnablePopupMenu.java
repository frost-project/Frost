/*
 JSkinnablePopupMenu.java / Frost
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
package frost.util.gui;

import java.lang.ref.WeakReference;
import java.util.Vector;

import javax.swing.JPopupMenu;

public class JSkinnablePopupMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;

	/*
	 * We insert a weak reference into the Vector of all Frames
	 * instead of 'this' so that garbage collection can still take
	 * place correctly.
	 */
	transient private final WeakReference<JSkinnablePopupMenu> weakThis;

	private static Vector<WeakReference<JSkinnablePopupMenu>> popupList = new Vector<WeakReference<JSkinnablePopupMenu>>();

	public JSkinnablePopupMenu() {
		super();
		weakThis = new WeakReference<JSkinnablePopupMenu>(this);
		addToPopupList();
	}

	private void addToPopupList() {
		synchronized (JSkinnablePopupMenu.class) {
			popupList.addElement(weakThis);
		}
	}

	/**
	 * Returns an array containing all JSkinnablePopupMenus created by
	 * the application.
	 */
	public static JSkinnablePopupMenu[] getSkinnablePopupMenus() {
		synchronized (JSkinnablePopupMenu.class) {
			JSkinnablePopupMenu realCopy[];
			// Recall that popupList is actually a Vector of WeakReferences
			// and calling get() on one of these references may return
			// null. Make two arrays-- one the size of the Vector
			// (fullCopy with size fullSize), and one the size of all
			// non-null get()s (realCopy with size realSize).
			final int fullSize = popupList.size();
			int realSize = 0;
			final JSkinnablePopupMenu fullCopy[] = new JSkinnablePopupMenu[fullSize];

			for (int i = 0; i < fullSize; i++) {
				fullCopy[realSize] = popupList.elementAt(i).get();

				if (fullCopy[realSize] != null) {
					realSize++;
				}
			}

			if (fullSize != realSize) {
				realCopy = new JSkinnablePopupMenu[realSize];
				System.arraycopy(fullCopy, 0, realCopy, 0, realSize);
			} else {
				realCopy = fullCopy;
			}
			return realCopy;
		}
	}

	public JSkinnablePopupMenu(final String label) {
		super(label);
		weakThis = new WeakReference<JSkinnablePopupMenu>(this);
		addToPopupList();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
    protected void finalize() throws Throwable {
		removeFromPopupList();
		super.finalize();
	}

	private void removeFromPopupList() {
		synchronized (JSkinnablePopupMenu.class) {
			popupList.removeElement(weakThis);
		}
	}
}
