/*
  FrostMessageManager.java / Frost
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
package frost.messaging.frost.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.gui.KnownBoardsFrame;
import frost.gui.SearchMessagesDialog;
import frost.messaging.frost.FrostMessageObject;
import frost.messaging.frost.boards.AbstractNode;
import frost.messaging.frost.boards.Board;
import frost.messaging.frost.boards.BoardInfoFrame;
import frost.messaging.frost.boards.BoardUpdateBoardSelector;
import frost.messaging.frost.boards.BoardUpdateInformationFrame;
import frost.messaging.frost.boards.Folder;
import frost.messaging.frost.boards.TOF;
import frost.messaging.frost.boards.TofTree;
import frost.messaging.frost.boards.TofTreeModel;
import frost.messaging.frost.gui.sentmessages.SentMessagesPanel;
import frost.messaging.frost.gui.unsentmessages.UnsentMessagesPanel;
import frost.messaging.frost.threads.RunningMessageThreadsInformation;
import frost.util.gui.MiscToolkit;
import frost.util.gui.SystraySupport;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

public class MessagingTab extends JPanel implements LanguageListener {

	private static final Logger logger = LoggerFactory.getLogger(MessagingTab.class);

	private static final long serialVersionUID = 1L;

    private JSplitPane treeAndTabbedPaneSplitpane = null;

    private MessagePanel messagePanel = null;

    private TofTree tofTree = null;
    private TofTreeModel tofTreeModel = null;

    private SentMessagesPanel sentMessagesPanel = null;
    private UnsentMessagesPanel unsentMessagesPanel = null;

//------------------------------------------------------------------------------------
    // toolbar and their items
    private JToolBar buttonToolBar;

    private JButton newBoardButton = null;
    private JButton newFolderButton = null;
    private JButton configBoardButton = null;
    private JButton renameFolderButton = null;
    private JButton removeBoardButton = null;
    private JButton boardInfoButton = null;
    private JButton knownBoardsButton = null;
    private JButton searchMessagesButton = null;
    private JButton systemTrayButton = null;
    private ImageIcon progressIconRunning = null;
    private ImageIcon progressIconIdle = null;
    private JLabel progressIconLabel = null;
    private JLabel disconnectedLabel = null;
//------------------------------------------------------------------------------------
    // menu items in main menu "News"
    private final JMenuItem tofDisplayBoardInfoMenuItem = new JMenuItem();
    private final JMenuItem tofDisplayBoardUpdateInformationMenuItem = new JMenuItem();
    private final JMenuItem tofDisplayKnownBoards = new JMenuItem();
    private final JMenuItem tofSearchMessages = new JMenuItem();
//------------------------------------------------------------------------------------

    private SearchMessagesDialog searchMessagesDialog = null;

    private final MainFrame mainFrame;
	private transient Language language;

    String forceSelectMessageId = null;

    public MessagingTab(final MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        language = Language.getInstance();
        language.addLanguageListener(this);
    }

    /**
     * Initializes the panel.
     */
    public void initialize() {

        sentMessagesPanel = new SentMessagesPanel();
        unsentMessagesPanel = new UnsentMessagesPanel();

        final JScrollPane tofTreeScrollPane = new JScrollPane(tofTree);
        tofTreeScrollPane.setWheelScrollingEnabled(true);

        tofTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(final TreeSelectionEvent e) {
                boardTree_actionPerformed();
            }
        });

        // Vertical Board Tree / MessagePane Divider
        final JPanel panel = new JPanel(new BorderLayout());
        treeAndTabbedPaneSplitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tofTreeScrollPane, panel);

        messagePanel = new MessagePanel(Core.frostSettings, mainFrame, this);
        messagePanel.setParentFrame(mainFrame);
        messagePanel.setIdentitiesManager(Core.getIdentitiesManager());
        messagePanel.initialize();

        panel.add(getMessagePanel(), BorderLayout.CENTER);

        int dividerLoc = Core.frostSettings.getInteger("MainFrame.treeAndTabbedPaneSplitpaneDividerLocation");
        if (dividerLoc < 10) {
            dividerLoc = 160;
        }
        treeAndTabbedPaneSplitpane.setDividerLocation(dividerLoc);

        setLayout(new BorderLayout());
        add(getButtonToolBar(), BorderLayout.NORTH);
        add(treeAndTabbedPaneSplitpane, BorderLayout.CENTER);

        // add menu items to News menu
        tofDisplayBoardInfoMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/information.png", 16, 16));
        tofDisplayKnownBoards.setIcon(MiscToolkit.getScaledImage("/data/toolbar/internet-web-browser.png", 16, 16));
        tofSearchMessages.setIcon(MiscToolkit.getScaledImage("/data/toolbar/edit-find.png", 16, 16));

        tofDisplayBoardInfoMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                tofDisplayBoardInfoMenuItem_actionPerformed(e);
            }
        });
        tofDisplayBoardUpdateInformationMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                tofDisplayBoardUpdateInformationMenuItem_actionPerformed(e);
            }
        });
        tofDisplayKnownBoards.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                tofDisplayKnownBoardsMenuItem_actionPerformed(e);
            }
        });
        tofSearchMessages.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                startSearchMessagesDialog();
            }
        });

        mainFrame.addMenuItem(tofDisplayBoardInfoMenuItem, "MainFrame.menu.news", 2, 1, true);
        mainFrame.addMenuItem(tofDisplayBoardUpdateInformationMenuItem, "MainFrame.menu.news", 2, 2, false);
        mainFrame.addMenuItem(tofDisplayKnownBoards, "MainFrame.menu.news", 2, 3, false);
        mainFrame.addMenuItem(tofSearchMessages, "MainFrame.menu.news", 2, 4, false);

        languageChanged(null);
    }

    public MessagePanel getMessagePanel() {
        return messagePanel;
    }
    public SentMessagesPanel getSentMessagesPanel() {
        return sentMessagesPanel;
    }
    public UnsentMessagesPanel getUnsentMessagesPanel() {
        return unsentMessagesPanel;
    }

    public TofTree getTofTree() {
        return tofTree;
    }
    public void setTofTree(final TofTree tofTree) {
        this.tofTree = tofTree;
    }

    public TofTreeModel getTofTreeModel() {
        return tofTreeModel;
    }
    public void setTofTreeModel(final TofTreeModel tofTreeModel) {
        this.tofTreeModel = tofTreeModel;
    }

    public void postInitialize() {
        // select saved board (NOTE: this loads the message list!)
        if (tofTree.getRowCount() > Core.frostSettings.getInteger(Settings.BOARDLIST_LAST_SELECTED_BOARD)) {
            tofTree.setSelectionRow(Core.frostSettings.getInteger(Settings.BOARDLIST_LAST_SELECTED_BOARD));
        }
    }

    public void setKeyActionForNewsTab(final Action action, final String actionName, final KeyStroke keyStroke) {
        treeAndTabbedPaneSplitpane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, actionName);
        treeAndTabbedPaneSplitpane.getActionMap().put(actionName, action);
    }

    public void showMessagePanelInSplitpane() {
        if( treeAndTabbedPaneSplitpane != null ) {
            final JPanel p = (JPanel)treeAndTabbedPaneSplitpane.getRightComponent();
            if( p.getComponent(0) == getMessagePanel() ) {
                return; // already shown
            }
            p.removeAll();
            p.add(getMessagePanel(), BorderLayout.CENTER);
            p.repaint();

            if( getSentMessagesPanel().isShown() ) {
                getSentMessagesPanel().cleanupAfterLeave();
            }
            if( getUnsentMessagesPanel().isShown() ) {
                getUnsentMessagesPanel().cleanupAfterLeave();
            }
        }
    }

    public void showSentMessagePanelInSplitpane() {
        if( treeAndTabbedPaneSplitpane != null ) {
            final JPanel p = (JPanel)treeAndTabbedPaneSplitpane.getRightComponent();
            if( p.getComponent(0) == getSentMessagesPanel() ) {
                return; // already shown
            }
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try { setPriority(getPriority() - 1); } catch(final Throwable lt) {}
                    getSentMessagesPanel().prepareForShow(); // load from db
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            p.removeAll();
                            p.add(getSentMessagesPanel(), BorderLayout.CENTER);
                            mainFrame.deactivateGlassPane(); // unblock gui
                            if( getUnsentMessagesPanel().isShown() ) {
                                getUnsentMessagesPanel().cleanupAfterLeave();
                            }
                        }
                    });
                }
            };
            mainFrame.activateGlassPane(); // block gui during load from database
            t.start();
        }
    }

    public void showUnsentMessagePanelInSplitpane() {
        if( treeAndTabbedPaneSplitpane != null ) {
            final JPanel p = (JPanel)treeAndTabbedPaneSplitpane.getRightComponent();
            if( p.getComponent(0) == getUnsentMessagesPanel() ) {
                return; // already shown
            }
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try { setPriority(getPriority() - 1); } catch(final Throwable lt) {}
                    getUnsentMessagesPanel().prepareForShow(); // load from db
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            p.removeAll();
                            p.add(getUnsentMessagesPanel(), BorderLayout.CENTER);
                            mainFrame.deactivateGlassPane(); // unblock gui
                            if( getSentMessagesPanel().isShown() ) {
                                getSentMessagesPanel().cleanupAfterLeave();
                            }
                        }
                    });
                }
            };
            mainFrame.activateGlassPane(); // block gui during load from database
            t.start();
        }
    }

    public void saveLayout() {
        Core.frostSettings.setValue("MainFrame.treeAndTabbedPaneSplitpaneDividerLocation",
                treeAndTabbedPaneSplitpane.getDividerLocation());
        getMessagePanel().saveLayout(Core.frostSettings);
        getSentMessagesPanel().saveTableFormat();
        getUnsentMessagesPanel().saveTableFormat();
    }

    public void showProgress() {
        progressIconLabel.setIcon(progressIconRunning);
    }
    public void hideProgress() {
        progressIconLabel.setIcon(progressIconIdle);
    }

    public void setDisconnected() {
        disconnectedLabel.setOpaque(true);
        disconnectedLabel.setBackground(Color.yellow);
        disconnectedLabel.setText(" DISCONNECTED ");
    }
    public void setConnected() {
        disconnectedLabel.setOpaque(false);
        disconnectedLabel.setBackground(progressIconLabel.getBackground());
        disconnectedLabel.setText("");
    }

    /**
     * Start the search dialog, all boards in board tree are selected.
     */
    public void startSearchMessagesDialog() {
        // show first time or bring to front
        getSearchMessagesDialog().startDialog();
    }

    /**
     * Start the search dialog with only the specified boards preselected as boards to search into.
     */
    public void startSearchMessagesDialog(final List<Board> l) {
        // show first time or bring to front
        getSearchMessagesDialog().startDialog(l);
    }

    public SearchMessagesDialog getSearchMessagesDialog() {
        if( searchMessagesDialog == null ) {
            try {
                searchMessagesDialog = new SearchMessagesDialog();
            } catch(Throwable t) {
                logger.error("SearchMessagesDialog could not be created.", t);
            }
        }
        return searchMessagesDialog;
    }

    private JToolBar getButtonToolBar() {
        if (buttonToolBar == null) {
            buttonToolBar = new JToolBar();

            // configure buttons
            newBoardButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/internet-group-chat.png"));
            newFolderButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/folder-new.png"));
            configBoardButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/document-properties.png"));
            renameFolderButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/edit-select-all.png"));
            removeBoardButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/user-trash.png"));
            boardInfoButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/information.png"));
            knownBoardsButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/internet-web-browser.png"));
            searchMessagesButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/edit-find.png"));

            systemTrayButton = new JButton(MiscToolkit.loadImageIcon("/data/tray.gif"));

            progressIconRunning = MiscToolkit.loadImageIcon("/data/progress_running.gif");
            progressIconIdle = MiscToolkit.loadImageIcon("/data/progress_idle.gif");
            progressIconLabel = new JLabel(progressIconIdle);
            disconnectedLabel = new JLabel("");

            MiscToolkit.configureButton(newBoardButton, "MainFrame.toolbar.tooltip.newBoard", language);
            MiscToolkit.configureButton(newFolderButton, "MainFrame.toolbar.tooltip.newFolder", language);
            MiscToolkit.configureButton(removeBoardButton, "MainFrame.toolbar.tooltip.removeBoard", language);
            MiscToolkit.configureButton(renameFolderButton, "MainFrame.toolbar.tooltip.renameFolder", language);
            MiscToolkit.configureButton(boardInfoButton, "MainFrame.toolbar.tooltip.boardInformationWindow", language);
            MiscToolkit.configureButton(systemTrayButton, "MainFrame.toolbar.tooltip.minimizeToSystemTray", language);
            MiscToolkit.configureButton(knownBoardsButton, "MainFrame.toolbar.tooltip.displayListOfKnownBoards", language);
            MiscToolkit.configureButton(searchMessagesButton, "MainFrame.toolbar.tooltip.searchMessages", language);
            MiscToolkit.configureButton(configBoardButton, "MainFrame.toolbar.tooltip.configureBoard", language);

            // add action listener
            knownBoardsButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    tofDisplayKnownBoardsMenuItem_actionPerformed(e);
                }
            });
            searchMessagesButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    startSearchMessagesDialog();
                }
            });
            newBoardButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    tofTree.createNewBoard(mainFrame);
                }
            });
            newFolderButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    tofTree.createNewFolder(mainFrame);
                }
            });
            renameFolderButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    renameFolder((Folder)tofTreeModel.getSelectedNode());
                }
            });
            removeBoardButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    tofTree.removeNode(tofTreeModel.getSelectedNode());
                }
            });

            configBoardButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    tofTree.configureBoard(tofTreeModel.getSelectedNode());
                }
            });

            systemTrayButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    if (SystraySupport.isInitialized()) {
                        SystraySupport.minimizeToTray();
                    }
                }
            });
            boardInfoButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    tofDisplayBoardInfoMenuItem_actionPerformed(e);
                }
            });

            // build panel
            buttonToolBar.setRollover(true);
            buttonToolBar.setFloatable(false);
            final Dimension blankSpace = new Dimension(3, 3);

            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.add(newBoardButton);
            buttonToolBar.add(newFolderButton);
            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.addSeparator();
            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.add(configBoardButton);
            buttonToolBar.add(renameFolderButton);
            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.addSeparator();
            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.add(removeBoardButton);
            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.addSeparator();
            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.add(boardInfoButton);
            buttonToolBar.add(knownBoardsButton);
            buttonToolBar.add(searchMessagesButton);
            if (SystraySupport.isInitialized()) {
                buttonToolBar.add(Box.createRigidArea(blankSpace));
                buttonToolBar.addSeparator();
                buttonToolBar.add(Box.createRigidArea(blankSpace));

                buttonToolBar.add(systemTrayButton);
            }
            buttonToolBar.add(Box.createHorizontalGlue());
            buttonToolBar.add(disconnectedLabel);
            buttonToolBar.add(Box.createRigidArea(blankSpace));
            buttonToolBar.add(progressIconLabel);
            buttonToolBar.add(Box.createRigidArea(blankSpace));
        }
        return buttonToolBar;
    }

    public void languageChanged(final LanguageEvent event) {
        // tool bar
        newBoardButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.newBoard"));
        newFolderButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.newFolder"));
        systemTrayButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.minimizeToSystemTray"));
        knownBoardsButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.displayListOfKnownBoards"));
        searchMessagesButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.searchMessages"));
        boardInfoButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.boardInformationWindow"));
        removeBoardButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.removeBoard"));
        renameFolderButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.renameFolder"));
        configBoardButton.setToolTipText(language.getString("MainFrame.toolbar.tooltip.configureBoard"));
        // menu items
        tofDisplayBoardInfoMenuItem.setText(language.getString("MainFrame.menu.news.displayBoardInformationWindow"));
        tofDisplayBoardUpdateInformationMenuItem.setText(language.getString("MainFrame.menu.news.displayBoardUpdateInformationMenuItem"));
        tofDisplayKnownBoards.setText(language.getString("MainFrame.menu.news.displayKnownBoards"));
        tofSearchMessages.setText(language.getString("MainFrame.menu.news.searchMessages"));
    }

    public void tofDisplayBoardInfoMenuItem_actionPerformed(final ActionEvent e) {
        if (BoardInfoFrame.isDialogShowing() == false) {
            final BoardInfoFrame boardInfo = new BoardInfoFrame(mainFrame, tofTree);
            boardInfo.startDialog();
        }
    }

    public void tofDisplayBoardUpdateInformationMenuItem_actionPerformed(final ActionEvent e) {
        if (BoardUpdateInformationFrame.isDialogShowing() == false) {
            final BoardUpdateInformationFrame boardInfo = new BoardUpdateInformationFrame(mainFrame, tofTree);
            boardInfo.startDialog();
        }
    }

    public void tofDisplayKnownBoardsMenuItem_actionPerformed(final ActionEvent e) {
        final KnownBoardsFrame knownBoards = new KnownBoardsFrame(mainFrame, tofTree);
        knownBoards.startDialog();
    }

    /**
     * Updates the whole tofTree.
     */
    public void boardTree_actionPerformed() {
        boardTree_actionPerformed(false);
    }

    /**
     * Updates the whole tofTree. Tries to
     */
    public void boardTree_actionPerformed(final boolean reload) {

        final int i[] = tofTree.getSelectionRows();
        if ((i != null) && (i.length > 0)) {
            Core.frostSettings.setValue(Settings.BOARDLIST_LAST_SELECTED_BOARD, i[0]);
        }

        final AbstractNode node = (AbstractNode) tofTree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }

        boolean showSentMessagesPanel = false;
        boolean showUnsentMessagesPanel = false;

        if (node.isBoard()) {
            // node is a board
            removeBoardButton.setEnabled(true);
            renameFolderButton.setEnabled(false);
            configBoardButton.setEnabled(true);

            // save the selected message for later re-select if we changed between threaded/flat view
            String previousMessageId = null;
            if( forceSelectMessageId != null ) {
                previousMessageId = forceSelectMessageId;
                forceSelectMessageId = null;
            } else if( reload ) {
                final int[] rows = getMessagePanel().getMessageTable().getSelectedRows();
                if( (rows != null) && (rows.length > 0) ) {
                    FrostMessageObject previousMessage = (FrostMessageObject) getMessagePanel().getMessageTableModel().getRow(rows[0]);
                    if( previousMessage != null ) {
                        previousMessageId = previousMessage.getMessageId();
                    }
                }
            }

            // remove previous msgs
            getMessagePanel().getMessageTable().setNewRootNode(new FrostMessageObject(true));
            getMessagePanel().updateMessageCountLabels(node);

            // read all messages for this board into message table (starts a thread)
            TOF.getInstance().updateTofTable((Board)node, previousMessageId);

            getMessagePanel().getMessageTable().clearSelection();
        } else if (node.isFolder()) {
            // node is a folder
            getMessagePanel().getMessageTable().setNewRootNode(new FrostMessageObject(true));
            getMessagePanel().updateMessageCountLabels(node);

            renameFolderButton.setEnabled(true);
            if (node.isRoot()) {
                removeBoardButton.setEnabled(false);
            } else {
                removeBoardButton.setEnabled(true);
            }
            configBoardButton.setEnabled(false);
        } else if (node.isUnsentMessagesFolder()) {
            // remove previous msgs to save memory
            getMessagePanel().getMessageTable().setNewRootNode(new FrostMessageObject(true));

            removeBoardButton.setEnabled(false);
            configBoardButton.setEnabled(false);

            showUnsentMessagesPanel = true;
        } else if (node.isSentMessagesFolder()) {
            // remove previous msgs to save memory
            getMessagePanel().getMessageTable().setNewRootNode(new FrostMessageObject(true));

            removeBoardButton.setEnabled(false);
            configBoardButton.setEnabled(false);

            showSentMessagesPanel = true;
        }

        if( showSentMessagesPanel ) {
            showSentMessagePanelInSplitpane();
        } else if( showUnsentMessagesPanel ) {
            showUnsentMessagePanelInSplitpane();
        } else {
            showMessagePanelInSplitpane();
        }
    }

    /**
     * Fires a nodeChanged (redraw) for this board and updates buttons.
     */
    public void updateTofTreeNode(final AbstractNode board) {
        if( board == null ) {
            return;
        }
        // fire update for node
        tofTreeModel.nodeChanged(board);
        // also update all parents
        TreeNode parentFolder = board.getParent();
        while (parentFolder != null) {
            tofTreeModel.nodeChanged(parentFolder);
            parentFolder = parentFolder.getParent();
        }
    }

    public void renameFolder(final Folder selected) {
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
        updateTofTreeNode(selected);
    }

    public void startNextBoardUpdate() {
        final Board nextBoard = BoardUpdateBoardSelector.selectNextBoard(getTofTreeModel());
        if (nextBoard != null) {
            getTofTree().updateBoard(nextBoard);
            logger.info("*** Automatic board update started for: {}", nextBoard.getName());
        } else {
            logger.info("*** Automatic board update - min update interval not reached.  waiting...");
        }
    }

    public RunningMessageThreadsInformation getRunningMessageThreadsInformation() {
        return getTofTree().getRunningBoardUpdateThreads().getRunningMessageThreadsInformation();
    }

    public void forceSelectMessageId(String messageId) {
        forceSelectMessageId = messageId;
    }
}
