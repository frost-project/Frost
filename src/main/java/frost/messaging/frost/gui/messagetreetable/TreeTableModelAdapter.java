/*
 * Copyright 1997-1999 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT OF OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THIS SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

package frost.messaging.frost.gui.messagetreetable;

import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This wrapper class takes a TreeTableModel and implements the table model interface. The implementation is trivial,
 * with all of the event dispatching support provided by the superclass: the AbstractTableModel.
 *
 * @version 1.2 10/27/98
 *
 * @author Philip Milne
 * @author Scott Violet
 */
public class TreeTableModelAdapter extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(TreeTableModelAdapter.class);

    final JTree tree;
    final MessageTreeTable treeTable;
    final TreeTableModel treeTableModel;

    private int collapsedToRow = -1;

    private final Listener listener = new Listener();

    /**
     *
     */
    private class Listener implements TreeWillExpandListener, TreeExpansionListener, TreeModelListener {

        /**
         *
         */
        public Listener() {
            super();
        }

        /* (non-Javadoc)
         * @see javax.swing.event.TreeWillExpandListener#treeWillExpand(javax.swing.event.TreeExpansionEvent)
         */
        public void treeWillExpand(final TreeExpansionEvent event) throws ExpandVetoException {
        }

        /**
         * Remember child rows to fire a tableRowDeleted event later in collapse listener
         *
         * @see javax.swing.event.TreeWillExpandListener#treeWillCollapse(javax.swing.event.TreeExpansionEvent)
         */
        public void treeWillCollapse(final TreeExpansionEvent event) throws ExpandVetoException {
            final DefaultMutableTreeNode collapsedNode = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
            // X new rows are below the expanded node
            final int nodeRow = treeTable.getRowForNode(collapsedNode);
            final int fromRow = nodeRow + 1;
            int toRow = nodeRow;
            if( collapsedNode.getChildCount() > 0 ) {
                toRow += collapsedNode.getChildCount();
            }

            final Enumeration<TreePath> e = treeTable.getTree().getExpandedDescendants(event.getPath());
            // count children of this tree, and children of all expanded sub-children
            while( e.hasMoreElements() ) {
                final DefaultMutableTreeNode n = (DefaultMutableTreeNode)e.nextElement().getLastPathComponent();
                toRow += n.getChildCount();
            }
            if( toRow < fromRow ) {
                toRow = fromRow;
            }
            collapsedToRow = toRow;
        }

        /* (non-Javadoc)
         * @see javax.swing.event.TreeExpansionListener#treeExpanded(javax.swing.event.TreeExpansionEvent)
         */
        public void treeExpanded(final TreeExpansionEvent event) {
            final DefaultMutableTreeNode expandedNode = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
            // X new rows are below the expanded node
            final int nodeRow = treeTable.getRowForNode(expandedNode);
            final int fromRow = nodeRow + 1;
            int toRow = nodeRow;
            if( expandedNode.getChildCount() > 0 ) {
                toRow += expandedNode.getChildCount();
            }
            // check if new children are expanded too
            final Enumeration<TreePath> e = treeTable.getTree().getExpandedDescendants(event.getPath());
            // count children of this tree, and children of all expanded sub-children
            while(e.hasMoreElements()) {
                final DefaultMutableTreeNode n = (DefaultMutableTreeNode)(e.nextElement()).getLastPathComponent();
                toRow += n.getChildCount();
            }
            if( toRow < fromRow ) {
                toRow = fromRow;
            }

            final int endRow = toRow;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fireTableRowsInserted(fromRow, endRow);
                }
            });

        }

        /**
         * Fire table event, use toRow computed in treeWillCollpaseListener
         *
         * @see javax.swing.event.TreeExpansionListener#treeCollapsed(javax.swing.event.TreeExpansionEvent)
         */
        public void treeCollapsed(final TreeExpansionEvent event) {
            final DefaultMutableTreeNode collapsedNode = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
            final int nodeRow = treeTable.getRowForNode(collapsedNode);
            final int fromRow = nodeRow + 1;
            final int toRow = collapsedToRow;
            fireTableRowsDeleted(fromRow, toRow);
        }

        /* (non-Javadoc)
         * @see javax.swing.event.TreeModelListener#treeNodesChanged(javax.swing.event.TreeModelEvent)
         */
        public void treeNodesChanged(final TreeModelEvent e) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.getTreePath().getLastPathComponent();
            final int[] childIndices = e.getChildIndices();
            // we always insert only one child at a time
            if( childIndices.length != 1 ) {
                logger.warn("****** FIXME1: more than 1 child: {} ********", childIndices.length);
            }
            // update the row for this node
            final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)node.getChildAt(childIndices[0]);
            final int row = treeTable.getRowForNode(childNode);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    logger.debug("treeNodesChanged: {}", row);
                    fireTableRowsUpdated(row, row);
                }
            });
        }

        /* (non-Javadoc)
         * @see javax.swing.event.TreeModelListener#treeNodesInserted(javax.swing.event.TreeModelEvent)
         */
        public void treeNodesInserted(final TreeModelEvent e) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getChildren()[0];
            final int[] childIndices = e.getChildIndices();
            // we always insert only one child at a time
            if( childIndices.length != 1 ) {
                logger.warn("****** FIXME2: more than 1 child: {} ********", childIndices.length);
            }
            //            final int row = MainFrame.getInstance().getMessageTreeTable().getRowForNode(node) + 1 + childIndices[0];
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final int row = treeTable.getRowForNode(node);
                    fireTableRowsInserted(row, row);
                }
            });
        }        
        
        /* (non-Javadoc)
         * @see javax.swing.event.TreeModelListener#treeNodesRemoved(javax.swing.event.TreeModelEvent)
         */
        public void treeNodesRemoved(final TreeModelEvent e) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.getTreePath().getLastPathComponent();
            final int[] childIndices = e.getChildIndices();
            // we always remove only one child at a time
            if( childIndices.length != 1 ) {
                logger.warn("****** FIXME3: more than 1 child: {} ********", childIndices.length);
            }
            // ATTN: will getRowForNode work if node was already removed from tree?
            //  -> we currently don't remove nodes from tree anywhere in Frost
            final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)node.getChildAt(childIndices[0]);
            final int row = treeTable.getRowForNode(childNode);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    fireTableRowsDeleted(row, row);
                }
            });
        }

        /* (non-Javadoc)
         * @see javax.swing.event.TreeModelListener#treeStructureChanged(javax.swing.event.TreeModelEvent)
         */
        public void treeStructureChanged(final TreeModelEvent e) {
//            delayedFireTableDataChanged();
            fireTableDataChanged();
        }
    }

    /**
     * @param treeTableModel
     * @param tree
     * @param tt
     */
    public TreeTableModelAdapter(final TreeTableModel treeTableModel, final JTree tree, final MessageTreeTable tt) {
        this.tree = tree;
        this.treeTable = tt;
        this.treeTableModel = treeTableModel;

        tree.addTreeWillExpandListener(listener);
        tree.addTreeExpansionListener(listener);
        treeTableModel.addTreeModelListener(listener);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() {
        return treeTableModel.getColumnCount();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#getColumnName(int)
     */
    public String getColumnName(final int column) {
        return treeTableModel.getColumnName(column);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
     */
    public Class<?> getColumnClass(final int column) {
        return treeTableModel.getColumnClass(column);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount() {
        return tree.getRowCount();
    }

    /**
     * @param row
     * @return
     */
    protected Object nodeForRow(final int row) {
        final TreePath treePath = tree.getPathForRow(row);
        if( treePath != null ) {
            return treePath.getLastPathComponent();
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(final int row, final int column) {
        return treeTableModel.getValueAt(nodeForRow(row), column);
    }

    /**
     * @param row
     * @return
     */
    public Object getRow(final int row) {
        return nodeForRow(row);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
     */
    public boolean isCellEditable(final int row, final int column) {
        return treeTableModel.isCellEditable(nodeForRow(row), column);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
     */
    public void setValueAt(final Object value, final int row, final int column) {
        treeTableModel.setValueAt(value, nodeForRow(row), column);
    }

//    /**
//     * Invokes fireTableDataChanged after all the pending events have been processed. SwingUtilities.invokeLater is used
//     * to handle this.
//     */
//    protected void delayedFireTableDataChanged() {
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                fireTableDataChanged();
//            }
//        });
//    }
}
