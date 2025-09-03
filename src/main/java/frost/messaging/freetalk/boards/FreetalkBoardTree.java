/*
  FreetalkBoardTree.java / Frost
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
package frost.messaging.freetalk.boards;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.messaging.frost.gui.MessagePanel;
import frost.messaging.frost.threads.RunningBoardUpdateThreads;
import frost.storage.AutoSavable;
import frost.storage.ExitSavable;
import frost.storage.StorageException;
import frost.util.gui.JDragTree;
import frost.util.gui.MiscToolkit;
import frost.util.gui.search.TreeFindAction;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

public class FreetalkBoardTree extends JDragTree implements AutoSavable, ExitSavable, PropertyChangeListener {

	private static final long serialVersionUID = 1L;

    private boolean showBoardDescriptionToolTips;
    private boolean showFlaggedStarredIndicators;

    private final int MINIMUM_ROW_HEIGHT = 20;
    private final int ROW_HEIGHT_MARGIN = 4;

//    private class PopupMenuTofTree
//        extends JPopupMenu
//        implements LanguageListener, ActionListener {
//
//        private final JMenuItem addBoardItem = new JMenuItem();
//        private final JMenuItem addFolderItem = new JMenuItem();
//        private final JMenuItem configureBoardItem = new JMenuItem();
//        private final JMenuItem configureFolderItem = new JMenuItem();
//        private final JMenuItem cutNodeItem = new JMenuItem();
//
//        private final JMenuItem descriptionItem = new JMenuItem();
//        private final JMenuItem pasteNodeItem = new JMenuItem();
//        private final JMenuItem refreshItem = new JMenuItem();
//        private final JMenuItem removeNodeItem = new JMenuItem();
//        private final JMenuItem renameFolderItem = new JMenuItem();
//        private final JMenuItem searchMessagesItem = new JMenuItem();
//        private final JMenuItem sendMessageItem = new JMenuItem();
//
//        private final JMenuItem markAllReadItem = new JMenuItem();
//
//        private AbstractFreetalkNode selectedTreeNode = null;
//        private final JMenuItem sortFolderItem = new JMenuItem();
//
//        public PopupMenuTofTree() {
//            super();
//            initialize();
//        }
//
//        public void actionPerformed(final ActionEvent e) {
//            final Object source = e.getSource();
//
//            final frost.util.gui.FrostSwingWorker worker = new frost.util.gui.FrostSwingWorker(this) {
//
//                @Override
//                protected void doNonUILogic() throws RuntimeException {
//                    if (source == refreshItem) {
//                        refreshSelected();
//                    } else if (source == addBoardItem) {
//                        addBoardSelected();
//                    } else if (source == addFolderItem) {
//                        addFolderSelected();
//                    } else if (source == removeNodeItem) {
//                        removeNodeSelected();
//                    } else if (source == cutNodeItem) {
//                        cutNodeSelected();
//                    } else if (source == pasteNodeItem) {
//                        pasteNodeSelected();
//                    } else if (source == configureBoardItem || source == configureFolderItem) {
//                        configureBoardSelected();
//                    } else if (source == sortFolderItem) {
//                        sortFolderSelected();
//                    } else if( source == markAllReadItem ) {
//                        markAllReadSelected();
//                    } else if( source == renameFolderItem ) {
//                        renameFolderSelected();
//                    } else if( source == searchMessagesItem ) {
//                        searchMessagesSelected();
//                    } else if( source == sendMessageItem ) {
//                        sendMessageSelected();
//                    }
//                }
//
//                @Override
//                protected void doUIUpdateLogic() throws RuntimeException {
//                    //Nothing here
//                }
//
//            };
//            worker.start();
//        }
//
//        private void addBoardSelected() {
//            createNewBoard(mainFrame);
//        }
//
//        private void addFolderSelected() {
//            createNewFolder(mainFrame);
//        }
//
//        private void configureBoardSelected() {
//            configureBoard(selectedTreeNode);
//        }
//
//        private void cutNodeSelected() {
//            cutNode(selectedTreeNode);
//        }
//
//        private void initialize() {
//            refreshLanguage();
//
//            addBoardItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/internet-group-chat.png", 16, 16));
//            addFolderItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/folder-new.png", 16, 16));
//            configureBoardItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/document-properties.png", 16, 16));
//            configureFolderItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/document-properties.png", 16, 16));
//            cutNodeItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/edit-cut.png", 16, 16));
//            pasteNodeItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/edit-paste.png", 16, 16));
//            refreshItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/view-refresh.png", 16, 16));
//            removeNodeItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/user-trash.png", 16, 16));
//            sortFolderItem.setIcon(MiscToolkit.getScaledImage("/data/sort.gif", 16, 16));
//            renameFolderItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/edit-select-all.png", 16, 16));
//            searchMessagesItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/edit-find.png", 16, 16));
//            sendMessageItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/mail-message-new.png", 16, 16));
//
//            descriptionItem.setEnabled(false);
//
//            // add listeners
//            refreshItem.addActionListener(this);
//            addBoardItem.addActionListener(this);
//            addFolderItem.addActionListener(this);
//            removeNodeItem.addActionListener(this);
//            cutNodeItem.addActionListener(this);
//            pasteNodeItem.addActionListener(this);
//            configureBoardItem.addActionListener(this);
//            configureFolderItem.addActionListener(this);
//            sortFolderItem.addActionListener(this);
//            markAllReadItem.addActionListener(this);
//            renameFolderItem.addActionListener(this);
//            searchMessagesItem.addActionListener(this);
//            sendMessageItem.addActionListener(this);
//        }
//
//        public void languageChanged(final LanguageEvent event) {
//            refreshLanguage();
//        }
//
//        private void pasteNodeSelected() {
//            if (clipboard != null) {
//                pasteNode(selectedTreeNode);
//            }
//        }
//
//        private void refreshLanguage() {
//            addBoardItem.setText(language.getString("BoardTree.popupmenu.addNewBoard"));
//            addFolderItem.setText(language.getString("BoardTree.popupmenu.addNewFolder"));
//            configureBoardItem.setText(language.getString("BoardTree.popupmenu.configureSelectedBoard"));
//            configureFolderItem.setText(language.getString("BoardTree.popupmenu.configureSelectedFolder"));
//            sortFolderItem.setText(language.getString("BoardTree.popupmenu.sortFolder"));
//            markAllReadItem.setText(language.getString("BoardTree.popupmenu.markAllMessagesRead"));
//            renameFolderItem.setText(language.getString("BoardTree.popupmenu.renameFolder"));
//            searchMessagesItem.setText(language.getString("BoardTree.popupmenu.searchMessages")+"...");
//            sendMessageItem.setText(language.getString("BoardTree.popupmenu.sendMessage"));
//        }
//
//        private void refreshSelected() {
//            refreshNode(selectedTreeNode);
//        }
//
//        private void sendMessageSelected() {
//            if( selectedTreeNode == null || !selectedTreeNode.isBoard() ) {
//                return;
//            }
//            final Board board = (Board) selectedTreeNode;
//            MainFrame.getInstance().getMessagePanel().composeNewMessage(board);
//        }
//
//        private void markAllReadSelected() {
//            TOF.getInstance().markAllMessagesRead(selectedTreeNode); // folder or board
//        }
//
//        private void removeNodeSelected() {
//            removeNode(selectedTreeNode);
//        }
//
//        private void searchMessagesSelected() {
//            final List<Board> boardsToSearch = new ArrayList<Board>();
//            if( selectedTreeNode.isBoard() ) {
//                boardsToSearch.add((Board)selectedTreeNode);
//            } else {
//                // folder, add all child boards
//                final Enumeration<AbstractNode> e = selectedTreeNode.breadthFirstEnumeration();
//                while(e.hasMoreElements() ) {
//                    final AbstractNode n = e.nextElement();
//                    if( n.isBoard() ) {
//                        boardsToSearch.add((Board)n);
//                    }
//                }
//            }
//            if( !boardsToSearch.isEmpty() ) {
//                MainFrame.getInstance().startSearchMessagesDialog(boardsToSearch);
//            }
//        }
//
//        @Override
//        public void show(final Component invoker, final int x, final int y) {
//            final int selRow = getRowForLocation(x, y);
//
//            if (selRow != -1) { // only if a node is selected
//                removeAll();
//
//                final TreePath selPath = getPathForLocation(x, y);
//                selectedTreeNode = (AbstractNode) selPath.getLastPathComponent();
//
//                if( !selectedTreeNode.isFolder() && !selectedTreeNode.isBoard() ) {
//                    return; // no menu for sent/unsent
//                }
//
//                final String folderOrBoard1 =
//                    ((selectedTreeNode.isFolder())
//                        ? language.getString("BoardTree.popupmenu.Folder")
//                        : language.getString("BoardTree.popupmenu.Board"));
//                final String folderOrBoard2 =
//                    ((selectedTreeNode.isFolder())
//                        ? language.getString("BoardTree.popupmenu.folder")
//                        : language.getString("BoardTree.popupmenu.board"));
//
//                descriptionItem.setText(folderOrBoard1 + " : " + selectedTreeNode.getName());
//
//                refreshItem.setText(language.getString("BoardTree.popupmenu.refresh") + " " + folderOrBoard2);
//                removeNodeItem.setText(language.getString("BoardTree.popupmenu.remove") + " " + folderOrBoard2);
//                cutNodeItem.setText(language.getString("BoardTree.popupmenu.cut") + " " + folderOrBoard2);
//
//                add(descriptionItem);
//                addSeparator();
//                add(refreshItem);
//                if (selectedTreeNode.isFolder() == false && !((Board)selectedTreeNode).isReadAccessBoard() ) {
//                    add(sendMessageItem);
//                }
//                add(searchMessagesItem);
//                addSeparator();
//                add(markAllReadItem);
//                addSeparator();
//                if (selectedTreeNode.isFolder() == true) {
//                    add(renameFolderItem);
//                    add(configureFolderItem);
//                    add(sortFolderItem);
//                } else {
//                    add(configureBoardItem);
//                }
//                addSeparator();
//                add(addBoardItem);
//                add(addFolderItem);
//                if (selectedTreeNode.isRoot() == false) {
//                    add(removeNodeItem);
//                }
//                addSeparator();
//                if (selectedTreeNode.isRoot() == false) {
//                    add(cutNodeItem);
//                }
//                if (clipboard != null && selectedTreeNode.isFolder()) {
//                    final String folderOrBoard3 =
//                        ((clipboard.isFolder())
//                            ? language.getString("BoardTree.popupmenu.folder")
//                            : language.getString("BoardTree.popupmenu.board"));
//                    pasteNodeItem.setText(language.getString("BoardTree.popupmenu.paste")
//                            + " "
//                            + folderOrBoard3
//                            + " '"
//                            + clipboard.getName()
//                            + "'");
//                    add(pasteNodeItem);
//                }
//
//                super.show(invoker, x, y);
//            }
//        }
//
//        private void sortFolderSelected() {
//            ((Folder)selectedTreeNode).sortChildren();
//            model.nodeStructureChanged(selectedTreeNode);
//        }
//
//        private void renameFolderSelected() {
//            MainFrame.getInstance().getFrostMessageTab().renameFolder( (Folder)selectedTreeNode );
//        }
//    }

    private class Listener extends MouseAdapter implements LanguageListener, ActionListener, KeyListener
    {
        public void languageChanged(final LanguageEvent event) {
            refreshLanguage();
        }

        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == configBoardMenuItem) {
                configureBoard(model.getSelectedNode());
            }
        }

        public void keyPressed(final KeyEvent e) {
            final char key = e.getKeyChar();
            pressedKey(key);
        }

        public void keyTyped(final KeyEvent e) {
        }

        public void keyReleased(final KeyEvent e) {
        }

        @Override
        public void mousePressed(final MouseEvent e) {
//            if (e.isPopupTrigger()) {
//                if (e.getSource() == FreetalkBoardTree.this) {
//                    showTofTreePopupMenu(e);
//                }
//            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
//            if (e.isPopupTrigger()) {
//                if (e.getSource() == FreetalkBoardTree.this) {
//                    showTofTreePopupMenu(e);
//                }
//            }
        }
    }

	private class CellRenderer extends DefaultTreeCellRenderer {

		private static final long serialVersionUID = 1L;

		private transient Border borderFlaggedAndStarredMsgs = BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 2, 0, 0, Color.red), // outside
				BorderFactory.createMatteBorder(0, 2, 0, 0, Color.blue)); // inside
		private transient Border borderStarredMsgs = BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 2, 0, 0), // outside
				BorderFactory.createMatteBorder(0, 2, 0, 0, Color.blue)); // inside
		private transient Border borderFlaggedMsgs = BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 2, 0, 0, Color.red), // outside
				BorderFactory.createEmptyBorder(0, 2, 0, 0)); // inside
		private transient Border borderEmpty = BorderFactory.createEmptyBorder(0, 4, 0, 0);

        private final ImageIcon sentMessagesFolderIcon;
        private final ImageIcon unsentMessagesFolderIcon;

        private Font boldFont = null;
        private Font normalFont = null;

        public CellRenderer() {

            sentMessagesFolderIcon = MiscToolkit.loadImageIcon("/data/book_open.png");
            unsentMessagesFolderIcon = MiscToolkit.loadImageIcon("/data/book_next.png");

            this.setLeafIcon(MiscToolkit.loadImageIcon("/data/comments.png"));
            this.setClosedIcon(MiscToolkit.loadImageIcon("/data/folder.png"));
            this.setOpenIcon(MiscToolkit.loadImageIcon("/data/folder-open.png"));

			// provide startup font: paranoia - should be provided by initialize() of tree
			normalFont = Core.frostSettings.getFont(Settings.BOARD_TREE_FONT_NAME, Settings.BOARD_TREE_FONT_STYLE,
					Settings.BOARD_TREE_FONT_SIZE, null);
			boldFont = normalFont.deriveFont(Font.BOLD);
		}

        public void fontChanged(final Font font) {
            normalFont = font.deriveFont(Font.PLAIN);
            boldFont = font.deriveFont(Font.BOLD);
        }

        @Override
        public Component getTreeCellRendererComponent(
            final JTree tree,
            final Object value,
            final boolean isSelected,
            final boolean expanded,
            final boolean leaf,
            final int row,
            final boolean localHasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, localHasFocus);

            final AbstractFreetalkNode node = (AbstractFreetalkNode)value;

            final boolean containsUnreadMessage = node.containsUnreadMessages();

            FreetalkBoard board = null;

            if (node.isFolder()) {
                final FreetalkFolder folder = (FreetalkFolder) node;
                // if this is a folder, check board for new messages
                setText(folder.getName());
                if (containsUnreadMessage) {
                    setFont(boldFont);
                } else {
                    setFont(normalFont);
                }
                if( showFlaggedStarredIndicators ) {
                    setBorder(borderEmpty);
                } else {
                    setBorder(null);
                }

            } else if(node.isBoard()) {

                board = (FreetalkBoard) node;
                // set the special text (board name + if new msg. a ' (2)' is appended and bold)
//                if (containsUnreadMessage) {
//                    setFont(boldFont);
//                    final StringBuilder sb = new StringBuilder();
//                    sb.append(board.getName()).append(" (").append(board.getUnreadMessageCount()).append(")");
//                    setText(sb.toString());
//                } else {
                    setFont(normalFont);
                    setText(board.getName());
//                }

                // set the icon
//                setIcon( board.getStateIcon() );

                // for a board we set indicators if board contains flagged or starred messages
//                if( showFlaggedStarredIndicators ) {
//                    final boolean hasStarred = board.hasStarredMessages();
//                    final boolean hasFlagged = board.hasFlaggedMessages();
//                    if( hasStarred && !hasFlagged ) {
//                        // unread and no marked
//                        setBorder(borderStarredMsgs);
//                    } else if( !hasStarred && hasFlagged ) {
//                        // no unread and marked
//                        setBorder(borderFlaggedMsgs);
//                    } else if( !hasStarred && !hasFlagged ) {
//                        // nothing
//                        setBorder(borderEmpty);
//                    } else {
//                        // both
//                        setBorder(borderFlaggedAndStarredMsgs);
//                    }
//                } else {
                    setBorder(null);
//                }
            } else {
                // sent/unsent folder
                final AbstractFreetalkNode folder = node;

                setText(folder.getName());

                setFont(normalFont);
                if( showFlaggedStarredIndicators ) {
                    setBorder(borderEmpty);
                } else {
                    setBorder(null);
                }
            }

            setBackgroundNonSelectionColor(UIManager.getColor("Tree.textBackground"));
            setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));

            // set board description as tooltip
//            if( showBoardDescriptionToolTips
//                    && board != null
//                    && board.getDescription() != null
//                    && board.getDescription().length() > 0 )
//            {
//                final String newToolTipText;
//                newToolTipText = board.getDescription();
//                setToolTipText(newToolTipText);
//            } else {
//                final String newToolTipText;
//                newToolTipText = null;
//                setToolTipText(newToolTipText);
//            }

            return this;
        }
    }

	private transient Language language;
	private transient Settings settings;
    private MainFrame mainFrame;

    private final Listener listener = new Listener();

//    private PopupMenuTofTree popupMenuTofTree;

    private final CellRenderer cellRenderer = new CellRenderer();

    private final FreetalkBoardTreeModel model;

    private final JMenuItem configBoardMenuItem = new JMenuItem();

    private AbstractFreetalkNode clipboard = null;

    private RunningBoardUpdateThreads runningBoardUpdateThreads = null;

    public FreetalkBoardTree(final FreetalkBoardTreeModel model) {
        super(model);
        this.model = model;

        showBoardDescriptionToolTips = Core.frostSettings.getBoolean(Settings.SHOW_BOARDDESC_TOOLTIPS);
        showFlaggedStarredIndicators = Core.frostSettings.getBoolean(Settings.SHOW_BOARDTREE_FLAGGEDSTARRED_INDICATOR);

        setRowHeight(18); // we use 16x16 icons, keep a gap
    }

//    private PopupMenuTofTree getPopupMenuTofTree() {
//        if (popupMenuTofTree == null) {
//            popupMenuTofTree = new PopupMenuTofTree();
//            language.addLanguageListener(popupMenuTofTree);
//        }
//        return popupMenuTofTree;
//    }

    public void initialize() {

        language = Language.getInstance();
        language.addLanguageListener(listener);

        new TreeFindAction().install(this);

        Core.frostSettings.addPropertyChangeListener(Settings.SHOW_BOARDDESC_TOOLTIPS, this);
        Core.frostSettings.addPropertyChangeListener(Settings.SHOW_BOARDTREE_FLAGGEDSTARRED_INDICATOR, this);

        Core.frostSettings.addPropertyChangeListener(Settings.BOARD_TREE_FONT_NAME, this);
        Core.frostSettings.addPropertyChangeListener(Settings.BOARD_TREE_FONT_SIZE, this);
        Core.frostSettings.addPropertyChangeListener(Settings.BOARD_TREE_FONT_STYLE, this);

        configBoardMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/document-properties.png", 16, 16));
        refreshLanguage();

        putClientProperty("JTree.lineStyle", "Angled"); // I like this look

        setRootVisible(true);
        setCellRenderer(cellRenderer);
        setSelectionModel(model.getSelectionModel());
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Add listeners
        addKeyListener(listener);
        addMouseListener(listener);
        configBoardMenuItem.addActionListener(listener);

        // enable tooltips for this tree
        ToolTipManager.sharedInstance().registerComponent(this);

        // load nodes from disk
        loadTree();

        // only select folder if a board in this folder was selected before
        // (this is a fix for a problem that occurs only on linux: collapse a folder with a selected
        //  board inside and the selection jumps to the root node rather than to the collapsed node)
        addTreeExpansionListener(new TreeExpansionListener() {
            public void treeCollapsed(final TreeExpansionEvent event) {
                final TreePath selectedPath = getSelectionPath();
                final TreePath collapsedPath = event.getPath();
                if( collapsedPath.isDescendant(selectedPath) ) {
                    setSelectionPath(event.getPath());
                }
            }
            public void treeExpanded(final TreeExpansionEvent event) {
            }
        });

        // enable the machine ;)
        runningBoardUpdateThreads = new RunningBoardUpdateThreads();

		// provide startup font
		setFont(Core.frostSettings.getFont(Settings.BOARD_TREE_FONT_NAME, Settings.BOARD_TREE_FONT_STYLE,
				Settings.BOARD_TREE_FONT_SIZE, null));
	}

    private void cutNode(final AbstractFreetalkNode node) {
        if (node != null) {
            clipboard = node;
        }
    }

    private void pasteNode(final AbstractFreetalkNode position) {
        if (clipboard == null) {
            return;
        }
        if (position == null || !position.isFolder()) {
            return; // We only allow pasting under folders
        }

        model.removeNode(clipboard, false);

        position.add(clipboard);
        clipboard = null;

        final int insertedIndex[] = { position.getChildCount() - 1 }; // last in list is the newly added
        model.nodesWereInserted(position, insertedIndex);
    }

    private void refreshLanguage() {
        configBoardMenuItem.setText(language.getString("BoardTree.popupmenu.configureSelectedBoard"));
    }

    /**
     * Get keyTyped for tofTree
     */
    public void pressedKey(final char key) {
        if (!isEditing()) {
            if (key == 'x' || key == 'X') {
                cutNode(model.getSelectedNode());
            }
            if (key == 'v' || key == 'V') {
                pasteNode(model.getSelectedNode());
            }
        }
    }

    @Override
    protected void processKeyEvent(final KeyEvent e) {
        // hack to prevent the standard JTree idiom (selects nodes starting with pressed key)
        final MessagePanel msgPanel = MainFrame.getInstance().getMessagePanel();
        Action action = null;
        if( e.getID()==KeyEvent.KEY_TYPED && (e.getKeyChar() == 'n' || e.getKeyChar() == 'N') ) {
            action = msgPanel.getActionMap().get("NEXT_MSG");
        } else if( e.getID()==KeyEvent.KEY_TYPED && (e.getKeyChar() == 'b' || e.getKeyChar() == 'B') ) {
            action = msgPanel.getActionMap().get("SET_BAD");
        } else if( e.getID()==KeyEvent.KEY_TYPED && (e.getKeyChar() == 'c' || e.getKeyChar() == 'C') ) {
            action = msgPanel.getActionMap().get("SET_CHECK");
        } else if( e.getID()==KeyEvent.KEY_TYPED && (e.getKeyChar() == 'o' || e.getKeyChar() == 'O') ) {
            action = msgPanel.getActionMap().get("SET_OBSERVE");
        } else if( e.getID()==KeyEvent.KEY_TYPED && (e.getKeyChar() == 'g' || e.getKeyChar() == 'G') ) {
            action = msgPanel.getActionMap().get("SET_GOOD");
        } else if( e.getID()==KeyEvent.KEY_TYPED && (e.getKeyChar() == 'f' || e.getKeyChar() == 'F') ) {
            action = msgPanel.getActionMap().get("TOGGLE_FLAGGED");
        } else if( e.getID()==KeyEvent.KEY_TYPED && (e.getKeyChar() == 's' || e.getKeyChar() == 'S') ) {
            action = msgPanel.getActionMap().get("TOGGLE_STARRED");
        } else if( e.getID()==KeyEvent.KEY_TYPED && (e.getKeyChar() == 'j' || e.getKeyChar() == 'J') ) {
            action = msgPanel.getActionMap().get("TOGGLE_JUNK");
        } else if( Character.isLetter(e.getKeyChar()) || Character.isDigit(e.getKeyChar()) ) {
            // ignore
        } else {
            super.processKeyEvent(e);
        }

        if (action != null) {
            action.actionPerformed(null);
        }
    }

    /**
     * Loads a tree description file.
     */
    private boolean loadTree() {
        return true;
//        final TofTreeXmlIO xmlio = new TofTreeXmlIO();
//        String boardIniFilename = settings.getValue(SettingsClass.DIR_CONFIG) + "boards.xml";
//        // the call changes the toftree and loads nodes into it
//        final File iniFile = new File(boardIniFilename);
//        if( iniFile.exists() == false ) {
//            logger.warn("boards.xml file not found, reading default file (will be saved to boards.xml on exit).");
//            final String defaultBoardsFile = "boards.xml.default07";
//            boardIniFilename = settings.getValue(SettingsClass.DIR_CONFIG) + defaultBoardsFile;
//        }
//
//        final String unsentName = language.getString("UnsentMessages.folderName");
//        final String sentName = language.getString("SentMessages.folderName");
//        unsentMessagesFolder = new UnsentMessagesFolder("["+unsentName+"]");
//        sentMessagesFolder = new SentMessagesFolder("["+sentName+"]");
//
//        final boolean loadWasOk = xmlio.loadBoardTree( this, model, boardIniFilename, unsentMessagesFolder, sentMessagesFolder );
//        if( !loadWasOk ) {
//            return loadWasOk;
//        }
//
//        // check if the board 'frost-announce' is contained in the list, add it if not found
//        final String expectedPubkey = FREENET_07_FROST_ANNOUNCE_PUBKEY;
//
//        final List<Board> existingBoards = model.getAllBoards();
//        boolean boardFound = false;
//        for( final Board board : existingBoards ) {
//            if( board.getName().equals(FROST_ANNOUNCE_NAME) ) {
//                boardFound = true;
//                // check if pubkey is correct
//                if( board.getPublicKey() == null || board.getPublicKey().equals(expectedPubkey) == false ) {
//                    board.setPublicKey(expectedPubkey);
//                }
//                break;
//            }
//        }
//        if( !boardFound ) {
//            final Board newBoard = new Board(FROST_ANNOUNCE_NAME, "Announcement of new Frost versions");
//            newBoard.setPublicKey(expectedPubkey);
//            final Folder root = (Folder)model.getRoot();
//            model.addNodeToTree(newBoard, root);
//        }
//
//        // check all boards for obsolete 0.7 encryption key and warn user
//        final List<String> boardsWithObsoleteKeys = new ArrayList<String>();
//        for( final Board board : existingBoards ) {
//            if( board.getPublicKey() != null && board.getPublicKey().endsWith("AQABAAE") ) {
//                boardsWithObsoleteKeys.add( board.getName() );
//            }
//        }
//        if( boardsWithObsoleteKeys.size() > 0 ) {
//            final String title = language.getString("StartupMessage.uploadFile.boardsWithObsoleteKeysFound.title");
//            for( final String boardName : boardsWithObsoleteKeys ) {
//                final String text = language.formatMessage("StartupMessage.uploadFile.boardsWithObsoleteKeysFound.text", boardName);
//                final StartupMessage sm = new StartupMessage(
//                        StartupMessage.MessageType.BoardsWithObsoleteKeysFound,
//                        title,
//                        text,
//                        JOptionPane.ERROR_MESSAGE,
//                        true);
//                MainFrame.enqueueStartupMessage(sm);
//                logger.error("Board with obsolete public key found: {}", boardName);
//            }
//        }
//
//        return loadWasOk;
    }

    public void autoSave() throws StorageException {
        save();
    }
    public void exitSave() throws StorageException {
        save();
    }

    /**
     * Save TOF tree's content to a file
     */
    public void save() throws StorageException {

//        // board list is important, create bak files bak, bak2, bak3, bak4
//        final String configDir = settings.getValue(SettingsClass.DIR_CONFIG);
//        final File xmlFile = new File(configDir + "boards.xml");
//        final File bakFile = new File(configDir + "boards.xml.bak");
//        final File bak2File = new File(configDir + "boards.xml.bak2");
//        final File bak3File = new File(configDir + "boards.xml.bak3");
//        final File bak4File = new File(configDir + "boards.xml.bak4");
//        final File oldFile = new File(configDir + "boards.xml.old");
//        final File newFile = new File(configDir + "boards.new");
//
//        // save to new xml file
//        final TofTreeXmlIO xmlio = new TofTreeXmlIO();
//        // the method scans the toftree
//        if (!xmlio.saveBoardTree( this, model, newFile )) {
//            throw new StorageException("Error while saving the TofTree.");
//        }
//
//        oldFile.delete();
//
//        // check for bak4 and maybe delete
//        if( bak4File.isFile() ) {
//            bak3File.renameTo(oldFile);
//        }
//
//        // maybe rename bak3 to bak4
//        if( bak3File.isFile() ) {
//            bak3File.renameTo(bak4File);
//        }
//
//        // maybe rename bak2 to bak3
//        if( bak2File.isFile() ) {
//            bak2File.renameTo(bak3File);
//        }
//
//        // maybe rename bak to bak2
//        if( bakFile.isFile() ) {
//            bakFile.renameTo(bak2File);
//        }
//
//        // maybe rename xml to bak
//        if( xmlFile.isFile() ) {
//            xmlFile.renameTo(bakFile);
//        }
//
//        newFile.renameTo(xmlFile);
//
//        oldFile.delete();
    }

    /**
     * Opens dialog, gets new name for board, checks for double names, adds node to tree
     */
//    public void createNewBoard(final Frame parent) {
//        boolean isDone = false;
//
//        while (!isDone) {
//            final NewBoardDialog dialog = new NewBoardDialog(parent);
//            dialog.setVisible(true);
//
//            if (dialog.getChoice() == NewBoardDialog.CHOICE_CANCEL) {
//                isDone = true; //cancelled
//            } else {
//                final String boardName = dialog.getBoardName();
//                final String boardDescription = dialog.getBoardDescription();
//
//                if (model.getBoardByName(boardName) != null) {
//                    JOptionPane.showMessageDialog(
//                        parent,
//                        language.formatMessage("BoardTree.duplicateNewBoardNameError.body", boardName),
//                        language.getString("BoardTree.duplicateNewBoardNameError.title"),
//                        JOptionPane.ERROR_MESSAGE);
//                } else {
//                    final Board newBoard = new Board(boardName, boardDescription);
//                    model.addNodeToTree(newBoard);
//                    // maybe this boardfolder already exists, scan for new messages
//                    TOF.getInstance().searchUnreadMessages(newBoard);
//                    isDone = true; //added
//                }
//            }
//        }
//    }

    /**
     * Checks if board is already existent, adds board to board tree.
     * @param fbobj
     */
    public void addNewBoard(final FreetalkBoard newBoard) {
        final FreetalkBoard existingBoard = model.getBoardByName(newBoard.getName());
        if (existingBoard == null) {
            model.addNodeToTree(newBoard);
            // maybe this boardfolder already exists, scan for new messages
//            TOF.getInstance().searchUnreadMessages(newBoard);
        }
    }

    /**
     * Opens dialog, gets new name for folder, checks for double names, adds node to tree
     * @param parent
     */
    public void createNewFolder(final Frame parent) {
        String nodeName = null;
        do {
            final Object nodeNameOb =
                JOptionPane.showInputDialog(
                    parent,
                    language.getString("BoardTree.newFolderDialog.body") + ":",
                    language.getString("BoardTree.newFolderDialog.title"),
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    language.getString("BoardTree.newFolderDialog.defaultName"));

            nodeName = ((nodeNameOb == null) ? null : nodeNameOb.toString());

            if (nodeName == null) {
                return; // cancelled
            }

        } while (nodeName.length() == 0);

        model.addNodeToTree(new FreetalkFolder(nodeName));
    }

    /**
     * Removes the given tree node, asks before deleting.
     * @return  true when node is removed
     */
    public boolean removeNode(final AbstractFreetalkNode node) {
        return removeNode(this, node);
    }

    /**
     * Removes the given tree node, asks before deleting.
     * @return  true when node is removed
     */
    public boolean removeNode(final Component parent, final AbstractFreetalkNode node) {
        int answer;
        if (node.isFolder()) {
            answer = JOptionPane.showConfirmDialog(
                    parent,
                    language.formatMessage("BoardTree.removeFolderConfirmation.body", node.getName()),
                    language.formatMessage("BoardTree.removeFolderConfirmation.title", node.getName()),
                    JOptionPane.YES_NO_OPTION);
        } else if(node.isBoard()) {
            answer = JOptionPane.showConfirmDialog(
                    parent,
                    language.formatMessage("BoardTree.removeBoardConfirmation.body", node.getName()),
                    language.formatMessage("BoardTree.removeBoardConfirmation.title", node.getName()),
                    JOptionPane.YES_NO_OPTION);
        } else {
            return false;
        }

        if (answer == JOptionPane.NO_OPTION) {
            return false;
        }

        // delete node from tree
        model.removeNode(node, true);

        return true;
    }

    public void setSettings(final Settings settings) {
        this.settings = settings;
    }

    public void setMainFrame(final MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

//    private void showTofTreePopupMenu(final MouseEvent e) {
//        getPopupMenuTofTree().show(e.getComponent(), e.getX(), e.getY());
//    }

    /**
     * starts update for the selected board, or for all childs (and their childs) of a folder
     */
    private void refreshNode(final AbstractFreetalkNode node) {
        if (node == null) {
            return;
        }

//        if (node.isBoard()) {
//            if (((FreetalkBoard)node).isManualUpdateAllowed()) {
//                updateBoard((FreetalkBoard)node);
//            }
//        } else if (node.isFolder()) {
//            // update all childs recursiv
//            final Enumeration<AbstractNode> leafs = node.children();
//            while (leafs.hasMoreElements()) {
//                refreshNode(leafs.nextElement());
//            }
//        }
    }

    public RunningBoardUpdateThreads getRunningBoardUpdateThreads() {
        return runningBoardUpdateThreads;
    }

    /**
     * News | Configure Board action performed
     * @param board
     */
    public void configureBoard(final AbstractFreetalkNode board) {
        if (board == null ) {
            return;
        }
//        final BoardSettingsFrame newFrame = new BoardSettingsFrame(mainFrame, board);
//        newFrame.runDialog(); // all needed updates of boards are done by the dialog before it closes
    }

    /**
     * Starts the board update threads, getRequest thread and update id thread.
     * Checks for each type of thread if its already running, and starts allowed
     * not-running threads for this board.
     * @param board
     */
    public void updateBoard(final FreetalkBoard board) {
        if (board == null || !board.isBoard()) {
            return;
        }

        // TODO: the gui buttons for boardupdate should be disabled instead
        if (!Core.isFreetalkTalkable()) {
        	return;
        }

        MainFrame.getInstance().getFreetalkMessageTab().updateBoard(board);

//        boolean threadStarted = false;
//
//        // download the messages of today
//        if (getRunningBoardUpdateThreads().isThreadOfTypeRunning(board, BoardUpdateThread.MSG_DNLOAD_TODAY) == false) {
//            getRunningBoardUpdateThreads().startMessageDownloadToday(
//                board,
//                settings,
//                listener);
//            logger.info("Starting update (MSG_TODAY) of {}", board.getName());
//            threadStarted = true;
//        }
//
//        final long now = System.currentTimeMillis();
//
//        if (getRunningBoardUpdateThreads().isThreadOfTypeRunning(board, BoardUpdateThread.MSG_DNLOAD_BACK) == false) {
//
//            // get the older messages, if configured start backload only after 12 hours
//            final long before12hours = now - (12L * 60L * 60L * 1000L); // 12 hours
//            boolean downloadCompleteBackload;
//            if( Core.frostSettings.getBoolean(SettingsClass.ALWAYS_DOWNLOAD_MESSAGES_BACKLOAD) == false
//                    && before12hours < board.getLastBackloadUpdateFinishedMillis() )
//            {
//                downloadCompleteBackload = false;
//            } else {
//                // we start a complete backload
//                downloadCompleteBackload = true;
//            }
//
//            getRunningBoardUpdateThreads().startMessageDownloadBack(board, settings, listener, downloadCompleteBackload);
//            logger.info("Starting update (MSG_BACKLOAD) of {}", board.getName());
//            threadStarted = true;
//        }
//
//        // if there was a new thread started, update the lastUpdateStartTimeMillis
//        if (threadStarted == true) {
//            board.setLastUpdateStartMillis(now);
//            board.incTimesUpdatedCount();
//        }
    }

    /**
     * Fires a nodeChanged (redraw) for all boards.
     * ONLY used to redraw tree after run of OptionsFrame.
     */
    public void updateTree() {
        // fire update for node
		final Enumeration<TreeNode> e = model.getRoot().depthFirstEnumeration();
        while (e.hasMoreElements()) {
            model.nodeChanged(e.nextElement());
        }
    }
    protected JMenuItem getConfigBoardMenuItem() {
        return configBoardMenuItem;
    }

    public void propertyChange(final PropertyChangeEvent evt) {
        if( evt.getPropertyName().equals(Settings.SHOW_BOARDDESC_TOOLTIPS) ) {
            showBoardDescriptionToolTips = Core.frostSettings.getBoolean(Settings.SHOW_BOARDDESC_TOOLTIPS);
        } else if( evt.getPropertyName().equals(Settings.SHOW_BOARDTREE_FLAGGEDSTARRED_INDICATOR) ) {
            showFlaggedStarredIndicators = Core.frostSettings.getBoolean(Settings.SHOW_BOARDTREE_FLAGGEDSTARRED_INDICATOR);
            updateTree(); // redraw tree nodes
        } else if( evt.getPropertyName().equals(Settings.BOARD_TREE_FONT_NAME) ) {
            fontChanged();
        } else if( evt.getPropertyName().equals(Settings.BOARD_TREE_FONT_SIZE) ) {
            fontChanged();
        } else if( evt.getPropertyName().equals(Settings.BOARD_TREE_FONT_STYLE) ) {
            fontChanged();
        }
    }

	private void fontChanged() {
		final int fontSize = Core.frostSettings.getInteger(Settings.BOARD_TREE_FONT_SIZE);
		setFont(Core.frostSettings.getFont(Settings.BOARD_TREE_FONT_NAME, Settings.BOARD_TREE_FONT_STYLE,
				Settings.BOARD_TREE_FONT_SIZE, "Tahoma"));

		// adjust row height to font size, add a margin
		setRowHeight(Math.max(fontSize + ROW_HEIGHT_MARGIN, MINIMUM_ROW_HEIGHT));
	}

    @Override
    public void setFont(final Font font) {
        super.setFont(font);

        if( cellRenderer != null ) {
            cellRenderer.fontChanged(font);
        }
        repaint();
    }

//    public UnsentMessagesFolder getUnsentMessagesFolder() {
//        return unsentMessagesFolder;
//    }
//
//    public SentMessagesFolder getSentMessagesFolder() {
//        return sentMessagesFolder;
//    }

    public FreetalkBoard getSelectedBoard() {
        final TreePath treePath = getSelectionPath();
        if( treePath == null ) {
            return null;
        }
        final AbstractFreetalkNode selectedNode = (AbstractFreetalkNode)treePath.getLastPathComponent();
        if( !selectedNode.isBoard() ) {
            return null;
        }
        final FreetalkBoard selectedBoard = (FreetalkBoard)selectedNode;
        return selectedBoard;
    }
}
