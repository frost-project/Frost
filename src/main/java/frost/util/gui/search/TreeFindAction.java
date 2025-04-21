/*
  TreeFindAction.java / Frost
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
package frost.util.gui.search;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.text.Position;
import javax.swing.tree.TreePath;

//@author Santhosh Kumar T - santhosh@in.fiorano.com 
public class TreeFindAction extends FindAction {

	private static final long serialVersionUID = 1L;

	@Override
    protected boolean changed(JComponent comp2, String searchString, Position.Bias bias){ 
        JTree tree = (JTree)comp2; 
        boolean startingFromSelection = true; 
        int max = tree.getRowCount(); 
        int increment = 0; 
        if(bias!=null) 
            increment = (bias == Position.Bias.Forward) ? 1 : -1; 
        int startingRow = (tree.getLeadSelectionRow() + increment + max) % max; 
        if (startingRow < 0 || startingRow >= tree.getRowCount()) { 
            startingFromSelection = false; 
            startingRow = 0; 
        } 
 
        TreePath path = getNextMatch(tree, searchString, startingRow, bias); 
        if (path != null) { 
            changeSelection(tree, path); 
            return true; 
        } else if (startingFromSelection) { 
            path = getNextMatch(tree, searchString, 0, bias); 
            if (path != null) { 
                changeSelection(tree, path); 
                return true; 
            } 
        } 
        return false; 
    } 
 
    // takes care of modifiers - control 
    protected void changeSelection(JTree tree, TreePath path){ 
        if(controlDown){ 
            tree.addSelectionPath(path); 
        }else 
            tree.setSelectionPath(path); 
        tree.scrollPathToVisible(path); 
    } 
 
    public TreePath getNextMatch(JTree tree, String prefix, int startingRow, Position.Bias bias) { 
        int max = tree.getRowCount(); 
        if (prefix == null) { 
            throw new IllegalArgumentException(); 
        } 
        if (startingRow < 0 || startingRow >= max) { 
            throw new IllegalArgumentException(); 
        } 
        if(ignoreCase) 
            prefix = prefix.toUpperCase(); 
 
        // start search from the next/previous element froom the 
        // selected element 
        int increment = (bias==null || bias == Position.Bias.Forward) ? 1 : -1; 
        int row = startingRow; 
        do { 
            TreePath path = tree.getPathForRow(row); 
            String text = tree.convertValueToText( 
                    path.getLastPathComponent(), tree.isRowSelected(row), 
                    tree.isExpanded(row), true, row, false); 
 
            if(ignoreCase) 
                text = text.toUpperCase(); 
                if (text.indexOf(prefix)>-1) { 
                    return path; 
                } 
//            if (text.startsWith(prefix)) { 
//                return path; 
//            } 
            row = (row + increment + max) % max; 
        } while (row != startingRow); 
        return null; 
    } 
}
