/*
  FreetalkMessageTab.java / Frost
  Copyright (C) 2009  Frost Project <jtcfrost.sourceforge.net>

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
package frost.messaging.freetalk.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fcp.fcp07.freetalk.FcpFreetalkConnection;
import frost.gui.NewBoardDialog;
import frost.messaging.freetalk.FreetalkManager;
import frost.messaging.freetalk.FreetalkMessage;
import frost.messaging.freetalk.FreetalkUnsentMessage;
import frost.messaging.freetalk.boards.AbstractFreetalkNode;
import frost.messaging.freetalk.boards.FreetalkBoard;
import frost.messaging.freetalk.boards.FreetalkBoardTree;
import frost.messaging.freetalk.boards.FreetalkBoardTreeModel;
import frost.messaging.freetalk.boards.FreetalkFolder;
import frost.messaging.freetalk.transfer.CreateBoardCallback;
import frost.messaging.freetalk.transfer.ListMessagesCallback;
import frost.messaging.freetalk.transfer.ListOwnIdentitiesCallback;
import frost.messaging.freetalk.transfer.ListSubscribedBoardsCallback;
import frost.messaging.freetalk.transfer.PutMessageCallback;
import frost.util.gui.MiscToolkit;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

public class FreetalkMessageTab implements LanguageListener {

	private static final Logger logger = LoggerFactory.getLogger(FreetalkMessageTab.class);

    private JSplitPane treeAndTabbedPaneSplitpane = null;
    private JPanel tabPanel = null;

    private FreetalkMessagePanel messagePanel = null;

    private JToolBar buttonToolBar;
    private JButton newBoardButton = null;
    private JButton updateBoardButton = null;
    private JButton newFolderButton = null;
    private JButton renameFolderButton = null;
    private JButton removeBoardButton = null;

    private final MainFrame mainFrame;
    private final Language language;
    private FreetalkManager ftManager = null;

    private FreetalkBoardTree ftBoardTree;
    private FreetalkBoardTreeModel ftBoardTreeModel;

    public FreetalkMessageTab(final MainFrame localMainFrame) {

        language = Language.getInstance();
        language.addLanguageListener(this);

        mainFrame = localMainFrame;
    }

    public void initialize() {
        if (tabPanel == null) {

            ftManager = FreetalkManager.getInstance();

            getBoardTree().initialize();

            final JScrollPane tofTreeScrollPane = new JScrollPane(getBoardTree());
            tofTreeScrollPane.setWheelScrollingEnabled(true);

            getBoardTree().addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(final TreeSelectionEvent e) {
                    boardTree_actionPerformed();
                }
            });

            // Vertical Board Tree / MessagePane Divider
            final JPanel panel = new JPanel(new BorderLayout());
            treeAndTabbedPaneSplitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tofTreeScrollPane, panel);

            messagePanel = new FreetalkMessagePanel(Core.frostSettings, mainFrame, this);
            messagePanel.setParentFrame(mainFrame);
            messagePanel.initialize();

            panel.add(getMessagePanel(), BorderLayout.CENTER);

            int dividerLoc = Core.frostSettings.getInteger("FreetalkTab.treeAndTabbedPaneSplitpaneDividerLocation");
            if( dividerLoc < 10 ) {
                dividerLoc = 160;
            }
            treeAndTabbedPaneSplitpane.setDividerLocation(dividerLoc);

            final JPanel p = new JPanel(new BorderLayout());
            p.add(getButtonToolBar(), BorderLayout.NORTH);
            p.add(treeAndTabbedPaneSplitpane, BorderLayout.CENTER);

            tabPanel = p;
        }
    }

    public FreetalkBoardTree getBoardTree() {
        if (ftBoardTree == null) {
            ftBoardTree = new FreetalkBoardTree(getTreeModel());
            ftBoardTree.setSettings(Core.frostSettings);
            ftBoardTree.setMainFrame(MainFrame.getInstance());
        }
        return ftBoardTree;
    }

    public FreetalkBoardTreeModel getTreeModel() {
        if (ftBoardTreeModel == null) {
            // this rootnode is discarded later, but if we create the tree without parameters,
            // a new Model is created wich contains some sample data by default (swing)
            // this confuses our renderer wich only expects FrostBoardObjects in the tree
            final FreetalkFolder dummyRootNode = new FreetalkFolder("Frost Message System");
            ftBoardTreeModel = new FreetalkBoardTreeModel(dummyRootNode);
        }
        return ftBoardTreeModel;
    }

    public void boardTree_actionPerformed() {

//        final int i[] = ftManager.getBoardTree().getSelectionRows();
//        if (i != null && i.length > 0) {
//            Core.frostSettings.setValue(SettingsClass.BOARDLIST_LAST_SELECTED_BOARD, i[0]);
//        }

        final AbstractFreetalkNode node = (AbstractFreetalkNode) getBoardTree().getLastSelectedPathComponent();
        if (node == null) {
            return;
        }

        if (node.isBoard()) {
            // node is a board
            removeBoardButton.setEnabled(true);
            renameFolderButton.setEnabled(false);

            // save the selected message for later re-select if we changed between threaded/flat view
//            FrostMessageObject previousMessage = null;
//            if( reload ) {
//                final int[] rows = getMessagePanel().getMessageTable().getSelectedRows();
//                if( rows != null && rows.length > 0 ) {
//                    previousMessage = (FrostMessageObject) getMessagePanel().getMessageTableModel().getRow(rows[0]);
//                }
//            }

            updateBoard((FreetalkBoard)node);

        } else if (node.isFolder()) {
            // node is a folder
            getMessagePanel().getMessageTable().setNewRootNode(new FreetalkMessage(true));
            getMessagePanel().updateMessageCountLabels(node);

            renameFolderButton.setEnabled(true);
            if (node.isRoot()) {
                removeBoardButton.setEnabled(false);
            } else {
                removeBoardButton.setEnabled(true);
            }
//            configBoardButton.setEnabled(false);
        }
    }

    public void updateBoard(final FreetalkBoard board) {
        // remove previous msgs
        getMessagePanel().getMessageTable().setNewRootNode(new FreetalkMessage(true));
        getMessagePanel().updateMessageCountLabels(board);

        getMessagePanel().getMessageTable().clearSelection();

        // FIXME: load msgs for selected board, build threads
        sendFreetalkCommandListMessages(board, true);
    }

    /**
     * Fires a nodeChanged (redraw) for this board and updates buttons.
     */
    public void updateTreeNode(final AbstractFreetalkNode board) {
        if( board == null ) {
            return;
        }
        // fire update for node
        getTreeModel().nodeChanged(board);
        // also update all parents
        TreeNode parentFolder = board.getParent();
        while (parentFolder != null) {
            getTreeModel().nodeChanged(parentFolder);
            parentFolder = parentFolder.getParent();
        }
    }

    public void saveLayout() {
        if (treeAndTabbedPaneSplitpane != null) {
            Core.frostSettings.setValue("FreetalkTab.treeAndTabbedPaneSplitpaneDividerLocation",
                    treeAndTabbedPaneSplitpane.getDividerLocation());
        }
    }

    public FreetalkMessagePanel getMessagePanel() {
        return messagePanel;
    }

    public JToolBar getButtonToolBar() {
        if (buttonToolBar == null) {
            buttonToolBar = new JToolBar();

            newBoardButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/internet-group-chat.png"));
            updateBoardButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/view-refresh.png"));
            newFolderButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/folder-new.png"));
            renameFolderButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/edit-select-all.png"));
            removeBoardButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/user-trash.png"));

            MiscToolkit.configureButton(newBoardButton, "MainFrame.toolbar.tooltip.newBoard", language);
            MiscToolkit.configureButton(updateBoardButton, "MessagePane.toolbar.tooltip.update", language);
            MiscToolkit.configureButton(newFolderButton, "MainFrame.toolbar.tooltip.newFolder", language);
            MiscToolkit.configureButton(renameFolderButton, "MainFrame.toolbar.tooltip.renameFolder", language);
            MiscToolkit.configureButton(removeBoardButton, "MainFrame.toolbar.tooltip.removeBoard", language);

            buttonToolBar.setRollover(true);
            buttonToolBar.setFloatable(false);
            final Dimension blankSpace = new Dimension(3, 3);

            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.add(updateBoardButton);
            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.addSeparator();
            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.add(newBoardButton);
            buttonToolBar.add(newFolderButton);
            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.add(renameFolderButton);
            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.add(removeBoardButton);
            buttonToolBar.add(Box.createRigidArea(blankSpace));

            newBoardButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    createNewBoard();
                }
            });
            updateBoardButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    sendFreetalkCommandListOwnIdentities();
                    sendFreetalkCommandListSubscribedBoards(FreetalkManager.getInstance().getLoginUserId());
                }
            });
            newFolderButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    getBoardTree().createNewFolder(mainFrame);
                }
            });
            renameFolderButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    renameFolder((FreetalkFolder)getTreeModel().getSelectedNode());
                }
            });
            removeBoardButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    getBoardTree().removeNode(getTreeModel().getSelectedNode());
                }
            });

            if (!Core.isFreetalkTalkable()) {
                updateBoardButton.setEnabled(false);
            }
        }
        return buttonToolBar;
    }

    public void createNewBoard() {

        final NewBoardDialog dialog = new NewBoardDialog(mainFrame);
        dialog.setVisible(true);

        if (dialog.getChoice() == NewBoardDialog.CHOICE_CANCEL) {
            return; //cancelled
        } else {
            final String boardName = dialog.getBoardName();
//            final String boardDescription = dialog.getBoardDescription();
            sendFreetalkCommandCreateBoard(boardName);
        }
    }

    public void renameFolder(final FreetalkFolder selected) {
        if (selected == null) {
            return;
        }
        String newname = null;
        do {
            newname = JOptionPane.showInputDialog(
                    mainFrame,
                    language.getString("MainFrame.dialog.renameFolder")+":\n",
                    selected.getName());
            if (newname == null) {
                return; // cancel
            }
        } while (newname.length() == 0);

        selected.setName(newname);
        updateTreeNode(selected);
    }

    public JPanel getTabPanel() {
        return tabPanel;
    }

    public void sendFreetalkCommandListBoards() {

        final String id = FcpFreetalkConnection.getNextFcpidentifier();
        ftManager.getConnection().registerCallback(id, new ListSubscribedBoardsCallback(mainFrame));

        mainFrame.activateGlassPane();

        try {
            ftManager.getConnection().sendCommandListBoards(id);
        } catch(final Exception ex) {
            logger.error("Error sending command ListBoards", ex);
            mainFrame.deactivateGlassPane();
            return;
        }
    }

    public void sendFreetalkCommandListSubscribedBoards(final String ownId) {

        final String id = FcpFreetalkConnection.getNextFcpidentifier();
        ftManager.getConnection().registerCallback(id, new ListSubscribedBoardsCallback(mainFrame));

        mainFrame.activateGlassPane();

        try {
            ftManager.getConnection().sendCommandListSubscribedBoards(id, ownId);
        } catch(final Exception ex) {
            logger.error("Error sending command ListBoards", ex);
            mainFrame.deactivateGlassPane();
            return;
        }
    }

    public void sendFreetalkCommandListMessages(final FreetalkBoard board, final boolean withContent) {

        final String id = FcpFreetalkConnection.getNextFcpidentifier();

        ftManager.getConnection().registerCallback(
                id, new ListMessagesCallback(mainFrame, board, Core.frostSettings.getBoolean(Settings.FREETALK_SHOW_THREADS)));

        mainFrame.activateGlassPane();

        try {
            ftManager.getConnection().sendCommandListMessages(id, board.getName(), FreetalkManager.getInstance().getLoginUserId(), withContent);
        } catch(final Exception ex) {
            logger.error("Error sending command ListMessages", ex);
            mainFrame.deactivateGlassPane();
            return;
        }
    }

    public void sendFreetalkCommandCreateBoard(final String newBoardName) {

        final String id = FcpFreetalkConnection.getNextFcpidentifier();

        ftManager.getConnection().registerCallback(id, new CreateBoardCallback(mainFrame));

        try {
            ftManager.getConnection().sendCommandCreateBoard(id, newBoardName);
        } catch(final Exception ex) {
            logger.error("Error sending command CreateBoard", ex);
            return;
        }
    }

    public void sendFreetalkCommandListOwnIdentities() {

        final String id = FcpFreetalkConnection.getNextFcpidentifier();

        ftManager.getConnection().registerCallback(id, new ListOwnIdentitiesCallback());

        try {
            ftManager.getConnection().sendCommandListOwnIdentities(id);
        } catch(final Exception ex) {
            logger.error("Error sending command ListOwnIdentities", ex);
            return;
        }
    }

    public void sendFreetalkCommandPutMessage(final FreetalkUnsentMessage msg) {

        final String id = FcpFreetalkConnection.getNextFcpidentifier();

        ftManager.getConnection().registerCallback(id, new PutMessageCallback(mainFrame));

        final java.util.List<String> targetBoardNames = new ArrayList<String>();
        for (final FreetalkBoard fb : msg.getTargetBoards()) {
            targetBoardNames.add(fb.getName());
        }

        try {
            logger.debug("{}", msg.getOwnIdentity().getUid());
            ftManager.getConnection().sendCommandPutMessage(
                    id,
                    msg.getParentId(),
                    msg.getOwnIdentity().getUid(),
                    msg.getReplyToBoard().getName(),
                    targetBoardNames,
                    msg.getTitle(),
                    msg.getContent(),
                    msg.getFileAttachments());
        } catch(final Exception ex) {
            logger.error("Error sending command ListOwnIdentities", ex);
            return;
        }
    }

    public void languageChanged(final LanguageEvent event) {
        if (newBoardButton == null) {
            // freetalk is not enabled and not visible ...
            return;
        }

        // tool bar
        newBoardButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.newBoard"));
        newBoardButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.newBoard"));
        newFolderButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.newFolder"));
        removeBoardButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.removeBoard"));
        renameFolderButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.renameFolder"));
    }
}
