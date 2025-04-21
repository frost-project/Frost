/*
  Folder.java / Frost
  Copyright (C) 2007  Frost Project <jtcfrost.sourceforge.net>

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
package frost.messaging.frost.boards;

import java.util.Collections;
import java.util.Vector;

import javax.swing.tree.TreeNode;

/**
 * Represents a folder in the board tree.
 */
public class Folder extends AbstractNode {

	private static final long serialVersionUID = 1L;

	public Folder(String newName) {
        super(newName);
    }
    
    public void setName(String newName) {
        name = newName;
        nameLowerCase = name.toLowerCase();
    }

	public void sortChildren() {
		Vector<? extends TreeNode> rawChildren = children;

		@SuppressWarnings("unchecked")
		// Only AbstractNode implements the interface Comparable.
		Vector<AbstractNode> childNodes = (Vector<AbstractNode>) rawChildren;

		Collections.sort(childNodes);
	}

    @Override
    public boolean containsUnreadMessages() {
        for (int i = 0; i < getChildCount(); i++) {
            AbstractNode an = (AbstractNode) getChildAt(i);
            if (an.containsUnreadMessages()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFolder() {
        return true;
    }
    
    @Override
    public boolean isLeaf() {
        return false;
    }
}
