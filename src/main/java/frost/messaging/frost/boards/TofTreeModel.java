/*
  TofTreeModel.java / Frost
  Copyright (C) 2001  Frost Project <jtcfrost.sourceforge.net>
  Some changes by Stefan Majewski <e9926279@stud3.tuwien.ac.at>

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

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import frost.gui.KnownBoardsManager;
import frost.storage.perst.messages.MessageStorage;

/**
 * This class serves as both the data and selection models of the TofTree.
 */
public class TofTreeModel extends DefaultTreeModel {

	private static final long serialVersionUID = 1L;

	private final DefaultTreeSelectionModel selectionModel;

    /**
     * This method creates a new TofTreeModel with the given TreeNode
     * as its root.
     * @param root TreeNode that will be the root of the new TofTreeModel.
     */
    public TofTreeModel(final TreeNode root) {
        super(root);
        selectionModel = new DefaultTreeSelectionModel();
    }

    /**
     * Fill the boards in tree with its primary keys after the board tree was loaded from xml file.
     */
    public void initialAssignPerstFrostBoardObjects() {
        // assign boards from storage, create if not existing (can happen if user changed board.xml file)
        final List<Board> boardList = getAllBoards();
        for( final Board b : boardList ) {
            MessageStorage.inst().assignPerstFrostBoardObject(b);
        }
    }

    /**
     * This method adds a new board to the model.
     * If the last node of the first selected path is a folder,
     * that new board is added as a child of it.
     * If the last node of the first selected path is a board, the new
     * board is added as a child of its parent.
     * If nothing is selected, the new board is added as a child of the root.
     * begins with selected node.
     *
     * @param newNode Board to be added to the model.
     */
    public void addNodeToTree(final AbstractNode newNode) {
        final AbstractNode selectedNode = getSelectedNode();
        final Folder targetFolder;
        if (selectedNode.isFolder() != true) {
            // add to parent of selected node
            targetFolder = (Folder) selectedNode.getParent();
        } else {
            targetFolder = (Folder) selectedNode;
        }
        addNodeToTree(newNode, targetFolder);
    }

    /**
     * Adds a new boards to the specified target folder.
     */
    public void addNodeToTree(final AbstractNode newNode, final Folder targetFolder) {
        targetFolder.add(newNode);

        if( newNode.isBoard() ) {
            if( MessageStorage.inst().assignPerstFrostBoardObject((Board)newNode) == false ) {
                return;
            }
        }

        // last in list is the newly added
        final int insertedIndex[] = { targetFolder.getChildCount() - 1 };
        nodesWereInserted(targetFolder, insertedIndex);
    }

    /**
     * Removes the node from the board tree.
     * If node is a folder ALL subfolders and boards and messages are deleted too.
     */
    public void removeNode(final AbstractNode node, final boolean removeFromDatabase) {
        final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (node != null && parent != null) {
            final List<Board> boardsToDelete = new LinkedList<Board>();
            if( removeFromDatabase ) {
                if( node.isFolder() ) {
					for (final Enumeration<TreeNode> e = node.breadthFirstEnumeration(); e.hasMoreElements();) {
						final AbstractNode b = (AbstractNode) e.nextElement();
                        if( !b.isFolder() ) {
                            boardsToDelete.add((Board)b);
                        }
                    }
                } else if( node.isBoard() ) {
                    boardsToDelete.add((Board)node);
                }
            }

            // add to known boards
            KnownBoardsManager.addNewKnownBoards(boardsToDelete);

            // find item to select after delete (the item before or after the deleted node)
            TreeNode nextSelectedNode = node.getPreviousSibling();
            if( nextSelectedNode == null ) {
                nextSelectedNode = node.getNextSibling();
                if( nextSelectedNode == null ) {
                    nextSelectedNode = parent;
                }
            }
            final TreePath nextSelectionPath = new TreePath(getPathToRoot(nextSelectedNode));

            // remove from tree
            final int[] childIndices = { parent.getIndex(node) };
            final Object[] removedChilds = { node };

            node.removeFromParent();
            nodesWereRemoved(parent, childIndices, removedChilds);

            selectionModel.setSelectionPath(nextSelectionPath);

            // maybe delete all boards
            if( !boardsToDelete.isEmpty() ) {
                final Thread worker = new Thread() {
                    @Override
                    public void run() {
                        for( Board board : boardsToDelete ) {
                            // due to cascade delete this deletes all messages of this board too
                            MessageStorage.inst().removeBoard(board);
                        }
                    }
                };
                worker.start();
            }
        }
    }

    /**
     * Returns Vector containing all Boards of the model.
     * @return Vector containing all the Boards of the model.
     */
    public LinkedList<Board> getAllBoards() {
        final LinkedList<Board> boards = new LinkedList<Board>();
		final Enumeration<TreeNode> e = getRoot().depthFirstEnumeration();
        while (e.hasMoreElements()) {
			final AbstractNode child = (AbstractNode) e.nextElement();
            if (child.isBoard()) {
                boards.add((Board)child);
            }
        }
        return boards;
    }

    @Override
    public AbstractNode getRoot() {
        return (AbstractNode)super.getRoot();
    }

    /**
     * Called if user changed the days to download backward in options.
     * Resets LastBackloadUpdateFinishedMillis for all boards.
     */
    public void resetLastBackloadUpdateFinishedMillis() {
		final Enumeration<TreeNode> e = getRoot().breadthFirstEnumeration();
        while (e.hasMoreElements()) {
			final AbstractNode child = (AbstractNode) e.nextElement();
            if (child.isBoard()) {
                ((Board)child).setLastBackloadUpdateFinishedMillis(0);
            }
        }
    }

    /**
     * This method looks for a board with the name passed as a parameter. The
     * comparison is not case sensitive. Folders are ignored.
     * @param boardName the name of the board to look for
     * @return the FrostBoardObject if there was a board with that name. Null otherwise.
     */
    public Board getBoardByName(final String boardName) {
        if( boardName == null ) {
            return null;
        }
		final Enumeration<TreeNode> e = getRoot().depthFirstEnumeration();
        while (e.hasMoreElements()) {
			final AbstractNode child = (AbstractNode) e.nextElement();
            if (child.isBoard()
                    && child.getName().compareToIgnoreCase(boardName) == 0)
            {
                return (Board)child;
            }
        }
        return null; // not found
    }

    /**
     * This method returns the last node of the first selected path.
     * If no path is selected, the root of the model is selected and
     * returned.
     * @return the last node of the first selected path or the root if
     *          nothing was selected.
     */
    public AbstractNode getSelectedNode() {
        final TreePath selectedPath = selectionModel.getSelectionPath();
        AbstractNode node;
        if (selectedPath != null) {
            node = (AbstractNode) selectedPath.getLastPathComponent();
        } else {
            // nothing selected? unbelievable! so select the root ...
            node = getRoot();
            selectionModel.setSelectionPath(new TreePath(node));
        }
        return node;
    }

    /**
     * This method returns the selection model
     * @return the selection model.
     */
    TreeSelectionModel getSelectionModel() {
        return selectionModel;
    }
}
