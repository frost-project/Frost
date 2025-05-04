/*
 JDragTree.java / Frost
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.messaging.frost.boards.AbstractNode;

@SuppressWarnings("serial")
public class JDragTree extends JTree implements DragGestureListener, DragSourceListener {

	private static final Logger logger = LoggerFactory.getLogger(JDragTree.class);

	 //	DropTargetListener interface object...
	 private class CDropTargetListener implements DropTargetListener 
	 { 	
	 	
		 // Fields...
		 private TreePath        _pathLast       = null;
		 private Rectangle2D     _raCueLine      = new Rectangle2D.Float();
		 private Rectangle2D     _raGhost        = new Rectangle2D.Float();
		 private Color           _colorCueLine;
		 private Point           _ptLast         = new Point();
		 private javax.swing.Timer           _timerHover;
		 private int             _nLeftRight     = 0;    // Cumulative left/right mouse movement
//		 private int             _nShift         = 0;

		 // Constructor...
		 public CDropTargetListener()
		 {
			 _colorCueLine = new Color(
					 SystemColor.controlShadow.getRed(),
					 SystemColor.controlShadow.getGreen(),
					 SystemColor.controlShadow.getBlue(),
					 64
				   );

			 // Set up a hover timer, so that a node will be automatically expanded or collapsed
			 // if the user lingers on it for more than a short time
			 _timerHover = new javax.swing.Timer(1000, new ActionListener()
			 {
				 public void actionPerformed(ActionEvent e)
				 {
					 _nLeftRight = 0;    // Reset left/right movement trend
					 if (isRootPath(_pathLast))
						 return; // Do nothing if we are hovering over the root node
					 if (isExpanded(_pathLast))
						 collapsePath(_pathLast);
					 else
						 expandPath(_pathLast);
				 }
			 });
			 _timerHover.setRepeats(false);  // Set timer to one-shot mode
		 }

		 // DropTargetListener interface
		 public void dragEnter(DropTargetDragEvent e)
		 {
			 if (!isDragAcceptable(e))
				 e.rejectDrag();
			 else
				 e.acceptDrag(e.getDropAction());
		 }

		 public void dragExit(DropTargetEvent e)
		 {
			 if (!DragSource.isDragImageSupported())
			 {
				 repaint(_raGhost.getBounds());
			 }
		 }

		 /**
		 * This is where the ghost image is drawn
		 */
		 public void dragOver(DropTargetDragEvent e)
		 {
             if( e==null || _raGhost == null || _ptLast == null || 
                 _ptOffset == null || _imgGhost == null || _raCueLine == null ) {
                 return;
             }
			 // Even if the mouse is not moving, this method is still invoked 10 times per second
			 Point pt = e.getLocation();
             if(pt==null) {
                 return;
             }
			 if (pt.equals(_ptLast))
				 return;

			 // Try to determine whether the user is flicking the cursor right or left
			 int nDeltaLeftRight = pt.x - _ptLast.x;
			 if ( (_nLeftRight > 0 && nDeltaLeftRight < 0) || (_nLeftRight < 0 && nDeltaLeftRight > 0) )
				 _nLeftRight = 0;
			 _nLeftRight += nDeltaLeftRight;
			 _ptLast = pt;
			 Graphics2D g2 = (Graphics2D) getGraphics();
             if( g2 == null ) {
                 return;
             }
             
			 // If a drag image is not supported by the platform, then draw my own drag image
			 if (!DragSource.isDragImageSupported())
			 {
				 paintImmediately(_raGhost.getBounds()); // Rub out the last ghost image and cue line
				 // And remember where we are about to draw the new ghost image
				 _raGhost.setRect(pt.x - _ptOffset.x, pt.y - _ptOffset.y, _imgGhost.getWidth(), _imgGhost.getHeight());
				 g2.drawImage(_imgGhost, AffineTransform.getTranslateInstance(_raGhost.getX(), _raGhost.getY()), null);
			 }
			 else    // Just rub out the last cue line
				 paintImmediately(_raCueLine.getBounds());

			 TreePath path = getClosestPathForLocation(pt.x, pt.y);
			 if (!(path == _pathLast))
			 {
				 _nLeftRight = 0;    // We've moved up or down, so reset left/right movement trend
				 _pathLast = path;
				 _timerHover.restart();
			 }

			 // In any case draw (over the ghost image if necessary) a cue line indicating where a drop will occur
			 Rectangle raPath = getPathBounds(path);
			 _raCueLine.setRect(0,  raPath.y+(int)raPath.getHeight(), getWidth(), 2);

			 g2.setColor(_colorCueLine);
			 g2.fill(_raCueLine);

//			 _nShift = 0;

			 // And include the cue line in the area to be rubbed out next time
			 _raGhost = _raGhost.createUnion(_raCueLine);

			 // Do this if you want to prohibit dropping onto the drag source
			 if (path.equals(_pathSource))
				 e.rejectDrag();
			 else
				 e.acceptDrag(e.getDropAction());
		 }

		 public void dropActionChanged(DropTargetDragEvent e)
		 {
			 if (!isDragAcceptable(e))
				 e.rejectDrag();
			 else
				 e.acceptDrag(e.getDropAction());
		 }

		 public void drop(DropTargetDropEvent e)
		 {
			 _timerHover.stop(); // Prevent hover timer from doing an unwanted expandPath or collapsePath

			 if (!isDropAcceptable(e))
			 {
				 e.rejectDrop();
				 return;
			 }

			 e.acceptDrop(e.getDropAction());

			 Transferable transferable = e.getTransferable();

			 DataFlavor[] flavors = transferable.getTransferDataFlavors();

			 for (int i = 0; i < flavors.length; i++ )
			 {
				 DataFlavor flavor = flavors[i];
				 if (flavor.isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType))
				 {
					 try
					 {
						 Point pt = e.getLocation();
						 TreePath pathTarget = getClosestPathForLocation(pt.x, pt.y);
						 TreePath pathSource = (TreePath) transferable.getTransferData(flavor);

						 if( pathTarget == null || pathSource == null )
						 {
							 e.dropComplete(false);
							 return;
						 }

						 DefaultMutableTreeNode sourceNode = (DefaultMutableTreeNode)pathSource.getLastPathComponent();
						 DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode)sourceNode.getParent();

						 DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode)pathTarget.getLastPathComponent();
						 DefaultMutableTreeNode newParent = (DefaultMutableTreeNode)targetNode.getParent();

						 if( !sourceNode.isLeaf() && targetNode.getParent() == sourceNode )
						 {
							 // trying to drag a folder into its own childs
							 e.dropComplete(false);
							 return;
						 }

						 DefaultTreeModel model = (DefaultTreeModel)getModel();
						 TreePath pathNewChild = null;
						 if( targetNode.isLeaf() || isCollapsed(pathTarget) )
						 {
							 // collapsed tree node or leaf
							 // dropped on a leaf, insert into leaf's parent AFTER leaf
							 int idx = newParent.getIndex(targetNode);
							 if( idx < 0 )
							 {
								 logger.warn("child not found in parent!!!");
								 e.dropComplete(false);
								 return;
							 }
							 else
							 {
								 idx++; // insert AFTER targetNode

								 // remove node from oldParent ...
								 Object[] removedChilds = { sourceNode };
								 int[] childIndices = { oldParent.getIndex(sourceNode) };
								 sourceNode.removeFromParent();
								 model.nodesWereRemoved( oldParent, childIndices, removedChilds );

								 // ... and insert into newParent
								 if( idx >= newParent.getChildCount() )
								 {
									 newParent.add( sourceNode );
									 int insertedIndex[] = { newParent.getChildCount()-1 };
									 model.nodesWereInserted( newParent, insertedIndex );
								 }
								 else
								 {
									 newParent.insert(sourceNode, idx);
									 int insertedIndex[] = { idx };
									 model.nodesWereInserted( newParent, insertedIndex );
								 }
                                 pathNewChild = new TreePath(model.getPathToRoot(sourceNode));
							 }
						 }
						 else
						 {
							 // expanded node, insert UNDER the node (before first child)
							 // remove node from oldParent ...
							 Object[] removedChilds = { sourceNode };
							 int[] childIndices = { oldParent.getIndex(sourceNode) };
							 sourceNode.removeFromParent();
							 model.nodesWereRemoved( oldParent, childIndices, removedChilds );
							 // ... and add to newParent
							 targetNode.insert( sourceNode, 0 );
							 int insertedIndex[] = { 0 };
							 model.nodesWereInserted( targetNode, insertedIndex );
                             
                             pathNewChild = new TreePath(model.getPathToRoot(sourceNode));
						 }

						 if (pathNewChild != null) {
							 setSelectionPath(pathNewChild); // Mark this as the selected path in the tree
                         }
						 break; // No need to check remaining flavors
					 }
					 catch (UnsupportedFlavorException ufe)
					 {
						 logger.error("Exception thrown in drop(DropTargetDropEvent e)", ufe);
						 e.dropComplete(false);
						 return;
					 }
					 catch (IOException ioe)
					 {
						 logger.error("Exception thrown in drop(DropTargetDropEvent e)", ioe);
						 e.dropComplete(false);
						 return;
					 }
				 }
			 }
			 e.dropComplete(true);
		 }

		 // Helpers...
		 public boolean isDragAcceptable(DropTargetDragEvent e)
		 {
			 // Only accept COPY or MOVE gestures (ie LINK is not supported)
			 if ((e.getDropAction() & DnDConstants.ACTION_MOVE) == 0)
				 return false;

			 // Only accept this particular flavor
			 if (!e.isDataFlavorSupported(TREEPATH_FLAVOR))
				 return false;

			 // Do this if you want to prohibit dropping onto the drag source...
			 Point pt = e.getLocation();
			 TreePath path = getClosestPathForLocation(pt.x, pt.y);
			 if(path == null || path.equals(_pathSource))
				 return false;

			 return true;
		 }

		 public boolean isDropAcceptable(DropTargetDropEvent e)
		 {
			 // Only accept COPY or MOVE gestures (ie LINK is not supported)
			 if ((e.getDropAction() & DnDConstants.ACTION_MOVE) == 0)
				 return false;

			 // Only accept this particular flavor
			 if (!e.isDataFlavorSupported(TREEPATH_FLAVOR))
				 return false;

			 // Do this if you want to prohibit dropping onto the drag source...
			 Point pt = e.getLocation();
			 TreePath path = getClosestPathForLocation(pt.x, pt.y);
			 if( path == null || path.equals(_pathSource))
				 return false;

			 return true;
		 }
	}
	 
	/**
	* This represents a TreePath (a node in a JTree) that can be transferred between a drag source and a drop target.
	*/
	private class CTransferableTreePath implements Transferable
	{
		private TreePath        _path;
		/**
		* Constructs a transferrable tree path object for the specified path.
		*/
		public CTransferableTreePath(TreePath path)
		{
			_path = path;
		}

		// Transferable interface methods...
		public DataFlavor[] getTransferDataFlavors()
		{
			return _flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor)
		{
			return java.util.Arrays.asList(_flavors).contains(flavor);
		}

		public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
		{
			if (flavor.isMimeTypeEqual(TREEPATH_FLAVOR.getMimeType()))
				return _path;
			else
				throw new UnsupportedFlavorException(flavor);
		}
	} 

	private TreePath        _pathSource;                // The path being dragged
	private BufferedImage   _imgGhost;                  // The 'drag image'
	private Point           _ptOffset = new Point();    // Where, in the drag image, the mouse was clicked

	// The type of DnD object being dragged...
	public final static DataFlavor TREEPATH_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, "TreePath");
	private DataFlavor[]    _flavors = { TREEPATH_FLAVOR };

	public JDragTree(TreeNode root) {
		super(root);
		initialize();
	}
	
	public JDragTree(TreeModel model) {
		super(model);
		initialize();
	}
	
	private void initialize() {
		// install drag n drop support
		DragSource dragSource = DragSource.getDefaultDragSource();
		DragGestureRecognizer dgRecognizer = dragSource.createDefaultDragGestureRecognizer(this,
																	 DnDConstants.ACTION_MOVE,
																	 this);
		// don't act on right mouse button
		dgRecognizer.setSourceActions(
				dgRecognizer.getSourceActions() & ~InputEvent.BUTTON3_DOWN_MASK & ~InputEvent.BUTTON2_DOWN_MASK);
		new DropTarget(this, new CDropTargetListener());
	}
	
	private boolean isRootPath(TreePath path)
	{
		return isRootVisible() && getRowForPath(path) == 0;
	}

	// DragSourceListener interface methods
	public void dragDropEnd(DragSourceDropEvent e)
	{
		this.repaint();
	}

	public void dragEnter(DragSourceDragEvent e) {}

	public void dragExit(DragSourceEvent e) {}

	// DragGestureListener interface method
	public void dragGestureRecognized(DragGestureEvent e)
	{
        if( Core.frostSettings.getBoolValue(Settings.PREVENT_BOARDTREE_REORDERING) ) {
            // drag n drop not allowed
            return;
        }
        
		//we should make sure we aren't in edit mode
		InputEvent ievent=e.getTriggerEvent();
		if( ievent instanceof MouseEvent )
		{
			//even though I tell dgRecognizer to ignore the the right mouse button,
			// it thinks the RMB starts a drag event...argh
			if ((((MouseEvent) ievent).getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0) {
				return;
			}
		}

		// begin dnd
		Point ptDragOrigin = e.getDragOrigin();
		TreePath path = getPathForLocation(ptDragOrigin.x, ptDragOrigin.y);
		if (path == null)
			return;
		if (isRootPath(path))
			return; // Ignore user trying to drag the root node
        Object dragObj = path.getLastPathComponent();
        if( dragObj instanceof AbstractNode ) {
            AbstractNode n = (AbstractNode) dragObj; 
            if( !n.isBoard() && !n.isFolder() ) {
                return; // only boards and folder can be dragged
            }
        }

		// Work out the offset of the drag point from the TreePath bounding rectangle origin
		Rectangle raPath = getPathBounds(path);
		_ptOffset.setLocation(ptDragOrigin.x-raPath.x, ptDragOrigin.y-raPath.y);

		// Get the cell renderer (which is a JLabel) for the path being dragged
		JLabel lbl = (JLabel) getCellRenderer().getTreeCellRendererComponent
								(
									this,                                           // tree
									path.getLastPathComponent(),                    // value
									false,                                          // isSelected   (dont want a colored background)
									isExpanded(path),                               // isExpanded
									getModel().isLeaf(path.getLastPathComponent()), // isLeaf
									0,                                              // row          (not important for rendering)
									false                                           // hasFocus     (dont want a focus rectangle)
								);
		lbl.setSize((int)raPath.getWidth(), (int)raPath.getHeight()); // <-- The layout manager would normally do this

		// Get a buffered image of the selection for dragging a ghost image
		_imgGhost = new BufferedImage((int)raPath.getWidth(), (int)raPath.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
		Graphics2D g2 = _imgGhost.createGraphics();

		// Ask the cell renderer to paint itself into the BufferedImage
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));      // Make the image ghostlike
		lbl.paint(g2);

		// Now paint a gradient UNDER the ghosted JLabel text (but not under the icon if any)
		// Note: this will need tweaking if your icon is not positioned to the left of the text
		Icon icon = lbl.getIcon();
		int nStartOfText = (icon == null) ? 0 : icon.getIconWidth()+lbl.getIconTextGap();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER, 0.5f)); // Make the gradient ghostlike
		g2.setPaint(new GradientPaint(nStartOfText, 0, SystemColor.controlShadow,
									  getWidth(),   0, new Color(255,255,255,0)));
		g2.fillRect(nStartOfText, 0, getWidth(), _imgGhost.getHeight());
		g2.dispose();

		setSelectionPath(path); // Select this path in the tree

		// Wrap the path being transferred into a Transferable object
		Transferable transferable = new CTransferableTreePath(path);

		// Remember the path being dragged (because if it is being moved, we will have to delete it later)
		_pathSource = path;

		// We pass our drag image just in case it IS supported by the platform
		e.startDrag(null, _imgGhost, new Point(5,5), transferable, this);
	}

	public void dragOver(DragSourceDragEvent e) {}

	public void dropActionChanged(DragSourceDragEvent e) {}

}
