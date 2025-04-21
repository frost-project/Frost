/*
  TargetFolderChooser.java / Frost
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
package frost.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import frost.messaging.frost.boards.AbstractNode;
import frost.messaging.frost.boards.Folder;
import frost.messaging.frost.boards.TofTreeModel;
import frost.util.gui.MiscToolkit;
import frost.util.gui.translation.Language;

/**
 * This class let the user choose a folder from the folders in tofTree.
 */
public class TargetFolderChooser extends JDialog {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;
    private JPanel buttonsPanel = null;
    private JTree folderTree = null;
    private JButton okButton = null;
    private JButton cancelButton = null;

    private final DefaultTreeModel treeModel;

    private Folder choosedFolder = null;
    private JScrollPane jScrollPane = null;

    private final Language language;

    /**
     * This is the default constructor
     */
    public TargetFolderChooser(final TofTreeModel origModel) {
        super();
        final MyTreeNode rootNode = buildTree(origModel);
        treeModel = new DefaultTreeModel(rootNode);
        language = Language.getInstance();
        initialize();
    }

    /**
     * Build a new tree which contains all folders of the TofTree.
     */
    private MyTreeNode buildTree(final TofTreeModel origModel) {
        final MyTreeNode rootNode = new MyTreeNode((Folder)origModel.getRoot());

        addNodesRecursiv(rootNode, origModel.getRoot());

        return rootNode;
    }

    private void addNodesRecursiv(final MyTreeNode addNode, final DefaultMutableTreeNode origNode) {

        for(int x=0; x < origNode.getChildCount(); x++) {
            final AbstractNode b = (AbstractNode)origNode.getChildAt(x);
            if( b.isFolder() ) {
                final MyTreeNode newNode = new MyTreeNode((Folder)b);
                addNode.add(newNode);
                addNodesRecursiv(newNode, b);
            }
        }
    }

    /**
     * This method initializes this
     *
     * @return void
     */
    private void initialize() {
        final int dlgSizeX = 350;
        final int dlgSizeY = 400;
        final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screen.width-dlgSizeX)/2;
        final int y = (screen.height-dlgSizeY)/2;
        setBounds(x,y,dlgSizeX,dlgSizeY);

        this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setModal(true);
        this.setTitle(language.getString("TargetFolderChooser.title"));
        this.setContentPane(getJContentPane());
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(final java.awt.event.WindowEvent e) {
                cancelButtonPressed();
            }
        });
    }

    /**
     * This method initializes jContentPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if( jContentPane == null ) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
            jContentPane.add(getButtonsPanel(), java.awt.BorderLayout.SOUTH);
        }
        return jContentPane;
    }

    /**
     * This method initializes buttonsPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getButtonsPanel() {
        if( buttonsPanel == null ) {
            final FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(java.awt.FlowLayout.RIGHT);
            buttonsPanel = new JPanel();
            buttonsPanel.setLayout(flowLayout);
            buttonsPanel.add(getOkButton(), null);
            buttonsPanel.add(getCancelButton(), null);
        }
        return buttonsPanel;
    }

    /**
     * This method initializes folderTree
     *
     * @return javax.swing.JTree
     */
    private JTree getFolderTree() {
        if( folderTree == null ) {
            folderTree = new JTree(treeModel);
            folderTree.setCellRenderer(new CellRenderer());
            folderTree.setSelectionRow(0);
        }
        return folderTree;
    }

    /**
     * This method initializes okButton
     *
     * @return javax.swing.JButton
     */
    private JButton getOkButton() {
        if( okButton == null ) {
            okButton = new JButton();
            okButton.setText(language.getString("Common.ok"));
            okButton.setSelected(false);
            okButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(final java.awt.event.ActionEvent e) {
                    okButtonPressed();
                }
            });
        }
        return okButton;
    }

    /**
     * This method initializes cancelButton
     *
     * @return javax.swing.JButton
     */
    private JButton getCancelButton() {
        if( cancelButton == null ) {
            cancelButton = new JButton();
            cancelButton.setText(language.getString("Common.cancel"));
            cancelButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(final java.awt.event.ActionEvent e) {
                    cancelButtonPressed();
                }
            });
        }
        return cancelButton;
    }

    private void okButtonPressed() {
        choosedFolder = ((MyTreeNode)getFolderTree().getSelectionPath().getLastPathComponent()).getFolder();
        setVisible(false);
    }

    private void cancelButtonPressed() {
        choosedFolder = null;
        setVisible(false);
    }

    public Folder startDialog() {

        setVisible(true);
        return choosedFolder;
    }

    /**
     * This method initializes jScrollPane
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getJScrollPane() {
        if( jScrollPane == null ) {
            jScrollPane = new JScrollPane();
            jScrollPane.setBackground(java.awt.Color.white);
            jScrollPane.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createEmptyBorder(2,2,2,2),
                    javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED)));
            jScrollPane.setViewportView(getFolderTree());
        }
        return jScrollPane;
    }

    /**
     * Simple renderer to set a nice icon for each folder.
     */
	private class CellRenderer extends DefaultTreeCellRenderer {

		private static final long serialVersionUID = 1L;

		private ImageIcon boardIcon;

        public CellRenderer() {
            boardIcon = MiscToolkit.loadImageIcon("/data/folder-open.png");
        }

        @Override
        public Component getTreeCellRendererComponent(
            final JTree tree,
            final Object value,
            final boolean sel,
            final boolean expanded,
            final boolean leaf,
            final int row,
            final boolean lHasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, lHasFocus);
            setIcon(boardIcon);
            return this;
        }
    }

    /**
     * A simple treenode implementation that holds a Board and returns its name as toString()
     */
	private class MyTreeNode extends DefaultMutableTreeNode {

		private static final long serialVersionUID = 1L;

		private Folder folder;

        public MyTreeNode(final Folder usrObj) {
            super(usrObj);
            folder = usrObj;
        }
        @Override
        public String toString() {
            return folder.getName();
        }
        public Folder getFolder() {
            return folder;
        }
    }
}  //  @jve:decl-index=0:visual-constraint="10,10"
