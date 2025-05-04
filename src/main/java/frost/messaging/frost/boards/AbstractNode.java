/*
  AbstractNode.java / Frost
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

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * The base for board, folder, ...
 */
public abstract class AbstractNode extends DefaultMutableTreeNode implements Comparable<AbstractNode> {

	private static final long serialVersionUID = 1L;

	protected String name = null;
    protected String nameLowerCase = null; // often used

    protected AbstractNode(final String newName) {
        super();
        name = newName;
    }

    public boolean containsUnreadMessages() {
        return false;
    }

    public String getName() {
        return name;
    }

    public String getNameLowerCase() {
        if( nameLowerCase == null ) {
            nameLowerCase = getName().toLowerCase();
        }
        return nameLowerCase;
    }

    public int compareTo(final AbstractNode o) {
        final AbstractNode board = o;
        // If both objects are of the same kind, sort by name
        if (board.isFolder() == isFolder()
            || board.isBoard() == isBoard() ) {
            //If both objects are of the same kind, sort by name
            return getNameLowerCase().compareTo(board.getNameLowerCase());
        } else {
            //If they are of a different kind, the folder is first.
            return isFolder() ? -1 : 1;
        }
    }

    @Override
    public boolean isLeaf() {
        return true; // all return true except Folder
    }

    public boolean isBoard() {
        return false;
    }
    public boolean isFolder() {
        return false;
    }
    public boolean isUnsentMessagesFolder() {
        return false;
    }
    public boolean isSentMessagesFolder() {
        return false;
    }

    @Override
    public String toString() {
        return getName();
    }
}
