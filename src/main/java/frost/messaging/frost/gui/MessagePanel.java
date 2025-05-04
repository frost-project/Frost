/*
 MessagePanel.java / Frost
  Copyright (C) 2006  Frost Project <jtcfrost.sourceforge.net>
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
package frost.messaging.frost.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.gui.SetIdentityCommentDialog;
import frost.identities.IdentitiesManager;
import frost.identities.Identity;
import frost.identities.LocalIdentity;
import frost.messaging.frost.FrostMessageObject;
import frost.messaging.frost.boards.AbstractNode;
import frost.messaging.frost.boards.Board;
import frost.messaging.frost.boards.TOF;
import frost.messaging.frost.boards.TofTree;
import frost.messaging.frost.gui.messagetreetable.MessageTreeTable;
import frost.messaging.frost.gui.messagetreetable.MessageTreeTableModel;
import frost.messaging.frost.gui.messagetreetable.TreeTableModelAdapter;
import frost.storage.perst.messages.MessageStorage;
import frost.util.ClipboardUtil;
import frost.util.gui.JSkinnablePopupMenu;
import frost.util.gui.MiscToolkit;
import frost.util.gui.SelectRowOnRightClick;
import frost.util.gui.search.TableFindAction;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

@SuppressWarnings("serial")
public class MessagePanel extends JPanel implements PropertyChangeListener {

	private static final Logger logger = LoggerFactory.getLogger(MessagePanel.class);

    private MessageTreeTable messageTable = null;
    private MessageTextPane messageTextPane = null;
    private JScrollPane messageListScrollPane = null;
    private JSplitPane msgTableAndMsgTextSplitpane = null;
    private final JLabel subjectLabel = new JLabel();
    private final JLabel subjectTextLabel = new JLabel();

    private boolean indicateLowReceivedMessages;
    private int indicateLowReceivedMessagesCountRed;
    private int indicateLowReceivedMessagesCountLightRed;

    private final MainFrame mainFrame;
    private final MessagingTab frostMessageTab;

    public static enum IdentityState { GOOD, CHECK, OBSERVE, BAD };
    public static enum BooleanState { FLAGGED, STARRED, JUNK };

    private class Listener
    extends MouseAdapter
    implements
        ActionListener,
        ListSelectionListener,
        TreeSelectionListener,
        LanguageListener
    {

        public Listener() {
            super();
        }

        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == updateBoardButton) {
                updateButton_actionPerformed(e);
            } else if (e.getSource() == newMessageButton) {
                newMessageButton_actionPerformed();
            } else if (e.getSource() == replyButton) {
                replyButton_actionPerformed(e);
            } else if (e.getSource() == saveMessageButton) {
                getMessageTextPane().saveMessageButton_actionPerformed();
            } else if (e.getSource() == nextUnreadMessageButton) {
                selectNextUnreadMessage();
            } else if (e.getSource() == setGoodButton) {
                setTrustState_actionPerformed(IdentityState.GOOD);
            } else if (e.getSource() == setBadButton) {
                setTrustState_actionPerformed(IdentityState.BAD);
            } else if (e.getSource() == setCheckButton) {
                setTrustState_actionPerformed(IdentityState.CHECK);
            } else if (e.getSource() == setObserveButton) {
                setTrustState_actionPerformed(IdentityState.OBSERVE);
            } else if (e.getSource() == toggleShowUnreadOnly) {
                toggleShowOnly_actionPerformed(e);
            } else if (e.getSource() == toggleShowFlaggedOnly) {
                toggleShowOnly_actionPerformed(e);
            } else if (e.getSource() == toggleShowStarredOnly) {
                toggleShowOnly_actionPerformed(e);
            } else if (e.getSource() == toggleShowThreads) {
                toggleShowThreads_actionPerformed(e);
            } else if (e.getSource() == toggleShowSmileys) {
                toggleShowSmileys_actionPerformed(e);
            } else if (e.getSource() == toggleShowHyperlinks) {
                toggleShowHyperlinks_actionPerformed(e);
            }
        }

        private void maybeShowPopup(final MouseEvent e) {
            if (e.isPopupTrigger()) {
                if (e.getComponent() == messageTable) {
                    showMessageTablePopupMenu(e);
                } else if( e.getComponent() == subjectTextLabel ) {
                    getPopupMenuSubjectText().show(e.getComponent(), e.getX(), e.getY());
                }
                // if leftbtn double click on message show this message in a new window
            } else if(SwingUtilities.isLeftMouseButton(e)) {
                // accepting only mouse pressed event as double click, otherwise it will be triggered twice
                if(e.getID() == MouseEvent.MOUSE_PRESSED ) {
                    if((e.getClickCount() == 2) && (e.getComponent() == messageTable) ) {
                        if( Core.frostSettings.getBoolean(Settings.MSGTABLE_DOUBLE_CLICK_SHOWS_MESSAGE) ) {
                        showCurrentMessagePopupWindow();
                        }
                    } else if((e.getClickCount() == 1) && (e.getComponent() == messageTable) ) {
                        // 'edit' the icon columns, toggle state flagged/starred
                        final int row = messageTable.rowAtPoint(e.getPoint());
                        final int col = messageTable.columnAtPoint(e.getPoint());
                        if( (row > -1) && (col > -1) ) {
                            final int modelCol = messageTable.getColumnModel().getColumn(col).getModelIndex();
                            editIconColumn(row, modelCol);
                        }
                    }
                }
            }
        }

        /**
         * Left click onto a row/col occurred. Check if the click was over an icon column and maybe toggle
         * its state (starred/flagged).
         */
        protected void editIconColumn(final int row, final int modelCol) {
            final BooleanState state;
            switch(modelCol) {
                case MessageTreeTableModel.COLUMN_INDEX_FLAGGED: state = BooleanState.FLAGGED; break;
                case MessageTreeTableModel.COLUMN_INDEX_STARRED: state = BooleanState.STARRED; break;
                case MessageTreeTableModel.COLUMN_INDEX_JUNK: state = BooleanState.JUNK; break;
                default: return;
            }

            final FrostMessageObject message = (FrostMessageObject)getMessageTableModel().getRow(row);
            if( (message == null) || message.isDummy() ) {
                return;
            }

            updateBooleanState(state, Collections.singletonList(message));
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            maybeShowPopup(e);
        }

        public void valueChanged(final ListSelectionEvent e) {
            messageTable_itemSelected(e);
        }

        public void valueChanged(final TreeSelectionEvent e) {
            boardsTree_actionPerformed(e);
        }

        public void languageChanged(final LanguageEvent event) {
            refreshLanguage();
        }
    }

	private class PopupMenuSubjectText
        extends JSkinnablePopupMenu
        implements ActionListener, LanguageListener
    {
        JMenuItem copySubjectText = new JMenuItem();

        public PopupMenuSubjectText() {
            super();
            initialize();
        }

        private void initialize() {
            refreshLanguage();
            copySubjectText.addActionListener(this);
        }

        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == copySubjectText) {
                ClipboardUtil.copyText(subjectTextLabel.getText());
            }
        }

        @Override
        public void show(final Component invoker, final int x, final int y) {
            removeAll();
            add(copySubjectText);
            super.show(invoker, x, y);
        }

        private void refreshLanguage() {
            copySubjectText.setText(language.getString("MessagePane.subjectText.popupmenu.copySubjectText"));
        }
        public void languageChanged(final LanguageEvent event) {
            refreshLanguage();
        }
    }

	private class PopupMenuMessageTable
        extends JSkinnablePopupMenu
        implements ActionListener, LanguageListener {

        private final JMenuItem markAllMessagesReadItem = new JMenuItem();
        private final JMenuItem markSelectedMessagesReadItem = new JMenuItem();
        private final JMenuItem markSelectedMessagesUnreadItem = new JMenuItem();
        private final JMenuItem markThreadReadItem = new JMenuItem();
        private final JMenuItem markMessageUnreadItem = new JMenuItem();
        private final JMenuItem setBadItem = new JMenuItem();
        private final JMenuItem setCheckItem = new JMenuItem();
        private final JMenuItem setGoodItem = new JMenuItem();
        private final JMenuItem setObserveItem = new JMenuItem();
        private final JMenuItem setCommentItem = new JMenuItem();

        private final JMenuItem deleteItem = new JMenuItem();
        private final JMenuItem undeleteItem = new JMenuItem();

        private final JMenuItem showItem = new JMenuItem();

        private final JMenuItem expandAllItem = new JMenuItem();
        private final JMenuItem collapseAllItem = new JMenuItem();

        private final JMenuItem expandThreadItem = new JMenuItem();
        private final JMenuItem collapseThreadItem = new JMenuItem();

        public PopupMenuMessageTable() {
            super();
            initialize();
        }

        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == markMessageUnreadItem) {
//                markSelectedMessageUnread();
                markSelectedMessagesReadOrUnread(false);
            } else if (e.getSource() == markAllMessagesReadItem) {
                TOF.getInstance().markAllMessagesRead(frostMessageTab.getTofTreeModel().getSelectedNode());
            } else if (e.getSource() == markSelectedMessagesReadItem) {
                markSelectedMessagesReadOrUnread(true);
            } else if (e.getSource() == markSelectedMessagesUnreadItem) {
                markSelectedMessagesReadOrUnread(false);
            } else if (e.getSource() == markThreadReadItem) {
                markThreadRead();
            } else if (e.getSource() == deleteItem) {
                deleteSelectedMessage();
            } else if (e.getSource() == undeleteItem) {
                undeleteSelectedMessage();
            } else if (e.getSource() == showItem) {
                showCurrentMessagePopupWindow();
            } else if (e.getSource() == expandAllItem) {
                getMessageTable().expandAll(true);
            } else if (e.getSource() == collapseAllItem) {
                getMessageTable().expandAll(false);
            } else if (e.getSource() == expandThreadItem) {
                getMessageTable().expandThread(true, selectedMessage);
            } else if (e.getSource() == collapseThreadItem) {
                getMessageTable().expandThread(false, selectedMessage);
            } else if (e.getSource() == setGoodItem) {
                setTrustState_actionPerformed(IdentityState.GOOD);
            } else if (e.getSource() == setBadItem) {
                setTrustState_actionPerformed(IdentityState.BAD);
            } else if (e.getSource() == setCheckItem) {
                setTrustState_actionPerformed(IdentityState.CHECK);
            } else if (e.getSource() == setObserveItem) {
                setTrustState_actionPerformed(IdentityState.OBSERVE);
            } else if (e.getSource() == setCommentItem) {
            	setComment_actionPerformed();
            }
        }

        private void initialize() {
            refreshLanguage();

            markMessageUnreadItem.addActionListener(this);
            markAllMessagesReadItem.addActionListener(this);
            markSelectedMessagesReadItem.addActionListener(this);
            markSelectedMessagesUnreadItem.addActionListener(this);
            markThreadReadItem.addActionListener(this);
            setGoodItem.addActionListener(this);
            setBadItem.addActionListener(this);
            setCheckItem.addActionListener(this);
            setObserveItem.addActionListener(this);
            setCommentItem.addActionListener(this);
            deleteItem.addActionListener(this);
            undeleteItem.addActionListener(this);
            showItem.addActionListener(this);
            expandAllItem.addActionListener(this);
            collapseAllItem.addActionListener(this);
            expandThreadItem.addActionListener(this);
            collapseThreadItem.addActionListener(this);
        }

        public void languageChanged(final LanguageEvent event) {
            refreshLanguage();
        }

        private void refreshLanguage() {
            markMessageUnreadItem.setText(language.getString("MessagePane.messageTable.popupmenu.markMessageUnread"));
            markAllMessagesReadItem.setText(language.getString("MessagePane.messageTable.popupmenu.markAllMessagesRead"));
            markSelectedMessagesReadItem.setText(language.getString("MessagePane.messageTable.popupmenu.markSelectedMessagesReadItem"));
            markSelectedMessagesUnreadItem.setText(language.getString("MessagePane.messageTable.popupmenu.markSelectedMessagesUnreadItem"));
            markThreadReadItem.setText(language.getString("MessagePane.messageTable.popupmenu.markThreadRead"));
            setGoodItem.setText(language.getString("MessagePane.messageTable.popupmenu.setToGood"));
            setBadItem.setText(language.getString("MessagePane.messageTable.popupmenu.setToBad"));
            setCheckItem.setText(language.getString("MessagePane.messageTable.popupmenu.setToCheck"));
            setObserveItem.setText(language.getString("MessagePane.messageTable.popupmenu.setToObserve"));
            setCommentItem.setText(language.getString("MessagePane.messageTable.popupmenu.setComment"));
            deleteItem.setText(language.getString("MessagePane.messageTable.popupmenu.deleteMessage"));
            undeleteItem.setText(language.getString("MessagePane.messageTable.popupmenu.undeleteMessage"));
            showItem.setText(language.getString("MessagePane.messageTable.popupmenu.showMessage"));
            expandAllItem.setText(language.getString("MessagePane.messageTable.popupmenu.expandAll"));
            collapseAllItem.setText(language.getString("MessagePane.messageTable.popupmenu.collapseAll"));
            expandThreadItem.setText(language.getString("MessagePane.messageTable.popupmenu.expandThread"));
            collapseThreadItem.setText(language.getString("MessagePane.messageTable.popupmenu.collapseThread"));
        }

        @Override
        public void show(final Component invoker, final int x, final int y) {

            if( messageTable.getSelectedRowCount() < 1 ) {
                return;
            }

            if (frostMessageTab.getTofTreeModel().getSelectedNode().isBoard()) {

                removeAll();

                // menu shown if multiple rows are selected
                if( messageTable.getSelectedRowCount() > 1 ) {

                    add(markSelectedMessagesReadItem);
                    add(markSelectedMessagesUnreadItem);
                    addSeparator();
                    add(deleteItem);
                    add(undeleteItem);
                    addSeparator();
                    add(setGoodItem);
                    add(setObserveItem);
                    add(setCheckItem);
                    add(setBadItem);
                    addSeparator();
                    add(setCommentItem);

                    deleteItem.setEnabled(true);
                    undeleteItem.setEnabled(true);

                    setGoodItem.setEnabled(true);
                    setObserveItem.setEnabled(true);
                    setCheckItem.setEnabled(true);
                    setBadItem.setEnabled(true);

                    setCommentItem.setEnabled(true);

                    super.show(invoker, x, y);
                    return;
                }

                if( Core.frostSettings.getBoolean(Settings.SHOW_THREADS) ) {
                    if( messageTable.getSelectedRowCount() == 1 ) {
                        add(expandThreadItem);
                        add(collapseThreadItem);
                    }
                    add(expandAllItem);
                    add(collapseAllItem);
                    addSeparator();
                }

                boolean itemAdded = false;
                if (messageTable.getSelectedRow() > -1) {
                    add(markMessageUnreadItem);
                    itemAdded = true;
                }
                if( (selectedMessage != null) && (selectedMessage.getBoard().getUnreadMessageCount() > 0) ) {
                    add(markAllMessagesReadItem);
                    add(markThreadReadItem);
                    itemAdded = true;
                }
                if( itemAdded ) {
                    addSeparator();
                }
                add(setGoodItem);
                add(setObserveItem);
                add(setCheckItem);
                add(setBadItem);
                setGoodItem.setEnabled(false);
                setObserveItem.setEnabled(false);
                setCheckItem.setEnabled(false);
                setBadItem.setEnabled(false);

                add(setCommentItem);
                setCommentItem.setEnabled(false);

                if ((messageTable.getSelectedRow() > -1) && (selectedMessage != null)) {
                    if( identitiesManager.isMySelf(selectedMessage.getFromName()) ) {
                        // keep all off
                    } else if (selectedMessage.isMessageStatusGOOD()) {
                        setObserveItem.setEnabled(true);
                        setCheckItem.setEnabled(true);
                        setBadItem.setEnabled(true);
                        setCommentItem.setEnabled(true);
                    } else if (selectedMessage.isMessageStatusCHECK()) {
                        setObserveItem.setEnabled(true);
                        setGoodItem.setEnabled(true);
                        setBadItem.setEnabled(true);
                        setCommentItem.setEnabled(true);
                    } else if (selectedMessage.isMessageStatusBAD()) {
                        setObserveItem.setEnabled(true);
                        setGoodItem.setEnabled(true);
                        setCheckItem.setEnabled(true);
                        setCommentItem.setEnabled(true);
                    } else if (selectedMessage.isMessageStatusOBSERVE()) {
                        setGoodItem.setEnabled(true);
                        setCheckItem.setEnabled(true);
                        setBadItem.setEnabled(true);
                        setCommentItem.setEnabled(true);
                    } else if (selectedMessage.isMessageStatusOLD()) {
                        // keep all buttons disabled
                    } else if (selectedMessage.isMessageStatusTAMPERED()) {
                        // keep all buttons disabled
                    } else {
                        logger.warn("invalid message state");
                    }
                }

                if (selectedMessage != null) {
                    addSeparator();
                    add(deleteItem);
                    add(undeleteItem);
                    deleteItem.setEnabled(false);
                    undeleteItem.setEnabled(false);
                    if(selectedMessage.isDeleted()) {
                        undeleteItem.setEnabled(true);
                    } else {
                        deleteItem.setEnabled(true);
                    }
					addSeparator();
					showItem.setEnabled(true);
                }

                super.show(invoker, x, y);
            }
        }
    }

    private final Settings settings;
    private final Language language  = Language.getInstance();
    private IdentitiesManager identitiesManager;
    private JFrame parentFrame;

    private boolean initialized = false;

    private final Listener listener = new Listener();

    private FrostMessageObject selectedMessage;

    private PopupMenuMessageTable popupMenuMessageTable = null;
    private PopupMenuSubjectText popupMenuSubjectText = null;

    //private JButton downloadAttachmentsButton =
    //  new JButton(Mixed.loadImageIcon("/data/attachment.gif"));
    //private JButton downloadBoardsButton =
    //  new JButton(Mixed.loadImageIcon("/data/attachmentBoard.gif"));
    private final JButton newMessageButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/mail-message-new.png"));
    private final JButton replyButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/mail-reply-sender.png"));
    private final JButton saveMessageButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/document-save-as.png"));
    protected JButton nextUnreadMessageButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/go-next.png"));
    private final JButton updateBoardButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/view-refresh.png"));

    private final JButton setGoodButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/weather-clear.png"));
    private final JButton setObserveButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/weather-few-clouds.png"));
    private final JButton setCheckButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/weather-overcast.png"));
    private final JButton setBadButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/weather-storm.png"));

    private final JToggleButton toggleShowUnreadOnly = new JToggleButton("");
    private final JToggleButton toggleShowFlaggedOnly = new JToggleButton("");
    private final JToggleButton toggleShowStarredOnly = new JToggleButton("");

    private final JToggleButton toggleShowThreads = new JToggleButton("");
    private final JToggleButton toggleShowSmileys = new JToggleButton("");
    private final JToggleButton toggleShowHyperlinks = new JToggleButton("");

    private String allMessagesCountPrefix = "";
    private final JLabel allMessagesCountLabel = new JLabel("");

    private String unreadMessagesCountPrefix = "";
    private final JLabel unreadMessagesCountLabel = new JLabel("");

    public MessagePanel(final Settings settings, final MainFrame mf, final MessagingTab fmt) {
        super();
        this.settings = settings;
        mainFrame = mf;
        frostMessageTab = fmt;
    }

    private JToolBar getButtonsToolbar() {
        // configure buttons
        MiscToolkit.configureButton(newMessageButton, "MessagePane.toolbar.tooltip.newMessage", language);
        MiscToolkit.configureButton(updateBoardButton, "MessagePane.toolbar.tooltip.update", language);
        MiscToolkit.configureButton(replyButton, "MessagePane.toolbar.tooltip.reply", language);
        MiscToolkit.configureButton(saveMessageButton, "MessagePane.toolbar.tooltip.saveMessage", language);
        MiscToolkit.configureButton(nextUnreadMessageButton, "MessagePane.toolbar.tooltip.nextUnreadMessage", language);
        MiscToolkit.configureButton(setGoodButton, "MessagePane.toolbar.tooltip.setToGood", language);
        MiscToolkit.configureButton(setBadButton, "MessagePane.toolbar.tooltip.setToBad", language);
        MiscToolkit.configureButton(setCheckButton, "MessagePane.toolbar.tooltip.setToCheck", language);
        MiscToolkit.configureButton(setObserveButton, "MessagePane.toolbar.tooltip.setToObserve", language);
        // MiscToolkit.configureButton(downloadAttachmentsButton,"Download attachment(s)","/data/attachment_rollover.gif",language);
        // MiscToolkit.configureButton(downloadBoardsButton,"Add Board(s)","/data/attachmentBoard_rollover.gif",language);

        replyButton.setEnabled(false);
        saveMessageButton.setEnabled(false);
        setGoodButton.setEnabled(false);
        setCheckButton.setEnabled(false);
        setBadButton.setEnabled(false);
        setObserveButton.setEnabled(false);

        ImageIcon icon;

        toggleShowUnreadOnly.setSelected(Core.frostSettings.getBoolean(Settings.SHOW_UNREAD_ONLY));
        icon = MiscToolkit.loadImageIcon("/data/toolbar/software-update-available.png");
        toggleShowUnreadOnly.setIcon(icon);
        toggleShowUnreadOnly.setRolloverEnabled(true);
        toggleShowUnreadOnly.setRolloverIcon(MiscToolkit.createRolloverIcon(icon));
        toggleShowUnreadOnly.setMargin(new Insets(0, 0, 0, 0));
        toggleShowUnreadOnly.setPreferredSize(new Dimension(24,24));
        toggleShowUnreadOnly.setFocusPainted(false);
        toggleShowUnreadOnly.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowUnreadOnly"));

        toggleShowFlaggedOnly.setSelected(Core.frostSettings.getBoolean(Settings.SHOW_FLAGGED_ONLY));
        icon = MiscToolkit.loadImageIcon("/data/flagged.gif");
        toggleShowFlaggedOnly.setIcon(icon);
        toggleShowFlaggedOnly.setRolloverEnabled(true);
        toggleShowFlaggedOnly.setRolloverIcon(MiscToolkit.createRolloverIcon(icon));
        toggleShowFlaggedOnly.setMargin(new Insets(0, 0, 0, 0));
        toggleShowFlaggedOnly.setPreferredSize(new Dimension(24,24));
        toggleShowFlaggedOnly.setFocusPainted(false);
        toggleShowFlaggedOnly.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowFlaggedOnly"));

        toggleShowStarredOnly.setSelected(Core.frostSettings.getBoolean(Settings.SHOW_STARRED_ONLY));
        icon = MiscToolkit.loadImageIcon("/data/starred.gif");
        toggleShowStarredOnly.setIcon(icon);
        toggleShowStarredOnly.setRolloverEnabled(true);
        toggleShowStarredOnly.setRolloverIcon(MiscToolkit.createRolloverIcon(icon));
        toggleShowStarredOnly.setMargin(new Insets(0, 0, 0, 0));
        toggleShowStarredOnly.setPreferredSize(new Dimension(24,24));
        toggleShowStarredOnly.setFocusPainted(false);
        toggleShowStarredOnly.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowStarredOnly"));

        toggleShowThreads.setSelected(Core.frostSettings.getBoolean(Settings.SHOW_THREADS));
        icon = MiscToolkit.loadImageIcon("/data/toolbar/toggle-treeview.png");
        toggleShowThreads.setIcon(icon);
        toggleShowThreads.setRolloverEnabled(true);
        toggleShowThreads.setRolloverIcon(MiscToolkit.createRolloverIcon(icon));
        toggleShowThreads.setMargin(new Insets(0, 0, 0, 0));
        toggleShowThreads.setPreferredSize(new Dimension(24,24));
        toggleShowThreads.setFocusPainted(false);
        toggleShowThreads.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowThreads"));

        toggleShowSmileys.setSelected(Core.frostSettings.getBoolean(Settings.SHOW_SMILEYS));
        icon = MiscToolkit.loadImageIcon("/data/toolbar/face-smile.png");
        toggleShowSmileys.setIcon(icon);
        toggleShowSmileys.setRolloverEnabled(true);
        toggleShowSmileys.setRolloverIcon(MiscToolkit.createRolloverIcon(icon));
        toggleShowSmileys.setMargin(new Insets(0, 0, 0, 0));
        toggleShowSmileys.setPreferredSize(new Dimension(24,24));
        toggleShowSmileys.setFocusPainted(false);
        toggleShowSmileys.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowSmileys"));

        toggleShowHyperlinks.setSelected(Core.frostSettings.getBoolean(Settings.SHOW_KEYS_AS_HYPERLINKS));
        icon = MiscToolkit.loadImageIcon("/data/togglehyperlinks.gif");
        toggleShowHyperlinks.setIcon(icon);
        toggleShowHyperlinks.setRolloverEnabled(true);
        toggleShowHyperlinks.setRolloverIcon(MiscToolkit.createRolloverIcon(icon));
        toggleShowHyperlinks.setMargin(new Insets(0, 0, 0, 0));
        toggleShowHyperlinks.setPreferredSize(new Dimension(24,24));
        toggleShowHyperlinks.setFocusPainted(false);
        toggleShowHyperlinks.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowHyperlinks"));

        // build buttons panel
        final JToolBar buttonsToolbar = new JToolBar();
        buttonsToolbar.setRollover(true);
        buttonsToolbar.setFloatable(false);
        final Dimension blankSpace = new Dimension(3, 3);

        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.add(nextUnreadMessageButton);
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.addSeparator();
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.add(saveMessageButton);
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.addSeparator();
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.add(newMessageButton);
        buttonsToolbar.add(replyButton);
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.addSeparator();
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.add(updateBoardButton);
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.addSeparator();
    //  buttonsToolbar.add(Box.createRigidArea(blankSpace));
    //  buttonsToolbar.add(downloadAttachmentsButton);
    //  buttonsToolbar.add(downloadBoardsButton);
    //  buttonsToolbar.add(Box.createRigidArea(blankSpace));
    //  buttonsToolbar.addSeparator();
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.add(setGoodButton);
        buttonsToolbar.add(setObserveButton);
        buttonsToolbar.add(setCheckButton);
        buttonsToolbar.add(setBadButton);
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.addSeparator();
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.add(toggleShowUnreadOnly);
        buttonsToolbar.add(toggleShowFlaggedOnly);
        buttonsToolbar.add(toggleShowStarredOnly);
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.addSeparator();
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.add(toggleShowThreads);
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.add(toggleShowSmileys);
        buttonsToolbar.add(Box.createRigidArea(blankSpace));
        buttonsToolbar.add(toggleShowHyperlinks);

        buttonsToolbar.add(Box.createRigidArea(new Dimension(8, 0)));
        buttonsToolbar.add(Box.createHorizontalGlue());
        buttonsToolbar.add(allMessagesCountLabel);
        buttonsToolbar.add(Box.createRigidArea(new Dimension(8, 0)));
        buttonsToolbar.add(unreadMessagesCountLabel);
        buttonsToolbar.add(Box.createRigidArea(blankSpace));

        // listeners
        newMessageButton.addActionListener(listener);
        updateBoardButton.addActionListener(listener);
        replyButton.addActionListener(listener);
    //  downloadAttachmentsButton.addActionListener(listener);
    //  downloadBoardsButton.addActionListener(listener);
        saveMessageButton.addActionListener(listener);
        nextUnreadMessageButton.addActionListener(listener);
        setGoodButton.addActionListener(listener);
        setCheckButton.addActionListener(listener);
        setBadButton.addActionListener(listener);
        setObserveButton.addActionListener(listener);
        toggleShowUnreadOnly.addActionListener(listener);
        toggleShowFlaggedOnly.addActionListener(listener);
        toggleShowStarredOnly.addActionListener(listener);
        toggleShowThreads.addActionListener(listener);
        toggleShowSmileys.addActionListener(listener);
        toggleShowHyperlinks.addActionListener(listener);

        return buttonsToolbar;
    }

    private void updateLabelSize(final JLabel label) {
        final String labelText = label.getText();
        final JLabel dummyLabel = new JLabel(labelText + "00000");
        dummyLabel.doLayout();
        final Dimension labelSize = dummyLabel.getPreferredSize();
        label.setPreferredSize(labelSize);
        label.setMinimumSize(labelSize);
    }

    private PopupMenuMessageTable getPopupMenuMessageTable() {
        if (popupMenuMessageTable == null) {
            popupMenuMessageTable = new PopupMenuMessageTable();
            language.addLanguageListener(popupMenuMessageTable);
        }
        return popupMenuMessageTable;
    }

    private PopupMenuSubjectText getPopupMenuSubjectText() {
        if (popupMenuSubjectText == null) {
            popupMenuSubjectText = new PopupMenuSubjectText();
            language.addLanguageListener(popupMenuSubjectText);
        }
        return popupMenuSubjectText;
    }

    public void initialize() {
        if (!initialized) {
            refreshLanguage();
            language.addLanguageListener(listener);

            FrostMessageObject.sortThreadRootMsgsAscending = settings.getBoolean(Settings.SORT_THREADROOTMSGS_ASCENDING);

            indicateLowReceivedMessages = Core.frostSettings.getBoolean(Settings.INDICATE_LOW_RECEIVED_MESSAGES);
            indicateLowReceivedMessagesCountRed = Core.frostSettings.getInteger(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_RED);
            indicateLowReceivedMessagesCountLightRed = Core.frostSettings.getInteger(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_LIGHTRED);

            Core.frostSettings.addPropertyChangeListener(Settings.SORT_THREADROOTMSGS_ASCENDING, this);
            Core.frostSettings.addPropertyChangeListener(Settings.MSGTABLE_MULTILINE_SELECT, this);
            Core.frostSettings.addPropertyChangeListener(Settings.MSGTABLE_SCROLL_HORIZONTAL, this);
            Core.frostSettings.addPropertyChangeListener(Settings.INDICATE_LOW_RECEIVED_MESSAGES, this);
            Core.frostSettings.addPropertyChangeListener(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_RED, this);
            Core.frostSettings.addPropertyChangeListener(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_LIGHTRED, this);

            // build messages list scroll pane
            final MessageTreeTableModel messageTableModel = new MessageTreeTableModel(new DefaultMutableTreeNode());
            language.addLanguageListener(messageTableModel);
            messageTable = new MessageTreeTable(messageTableModel);
            new TableFindAction().install(messageTable);
            updateMsgTableResizeMode();
            updateMsgTableMultilineSelect();
            messageTable.getSelectionModel().addListSelectionListener(listener);
            messageListScrollPane = new JScrollPane(messageTable);
            messageListScrollPane.setWheelScrollingEnabled(true);
            messageListScrollPane.getViewport().setBackground(messageTable.getBackground());

            messageTextPane = new MessageTextPane(mainFrame);

            final JPanel subjectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,3,0));
            subjectPanel.add(subjectLabel);
            subjectPanel.add(subjectTextLabel);
            subjectPanel.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

            subjectTextLabel.addMouseListener(listener);

            // load message table layout
            messageTable.loadLayout(settings);

            fontChanged();

            final JPanel dummyPanel = new JPanel(new BorderLayout());
            dummyPanel.add(subjectPanel, BorderLayout.NORTH);
            dummyPanel.add(messageTextPane, BorderLayout.CENTER);

            msgTableAndMsgTextSplitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, messageListScrollPane, dummyPanel);
            msgTableAndMsgTextSplitpane.setDividerSize(10);
            msgTableAndMsgTextSplitpane.setResizeWeight(0.5d);
            msgTableAndMsgTextSplitpane.setMinimumSize(new Dimension(50, 20));
            int dividerLoc = Core.frostSettings.getInteger(Settings.MSGTABLE_MSGTEXT_DIVIDER_LOCATION);
            if( dividerLoc < 10 ) {
                dividerLoc = 160;
            }
            msgTableAndMsgTextSplitpane.setDividerLocation(dividerLoc);

            // build main panel
            setLayout(new BorderLayout());
            add(getButtonsToolbar(), BorderLayout.NORTH);
            add(msgTableAndMsgTextSplitpane, BorderLayout.CENTER);

            // listeners
            messageTable.addMouseListener(listener);
			messageTable.addMouseListener(new SelectRowOnRightClick(messageTable));

            frostMessageTab.getTofTree().addTreeSelectionListener(listener);

            assignHotkeys();

            // display welcome message if no boards are available
            boardsTree_actionPerformed(null); // set initial states

            initialized = true;
        }
    }

    private void assignHotkeys() {

        // TODO: also check TofTree.processKeyEvent() which forwards the hotkeys!

    // assign F5 key - start board update
        final Action boardUpdateAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
                final TofTree tofTree = frostMessageTab.getTofTree();
                final Board selectedBoard = tofTree.getSelectedBoard();
                if( selectedBoard == null ) {
                    return;
                }
                tofTree.updateBoard(selectedBoard);
            }
        };
        frostMessageTab.setKeyActionForNewsTab(boardUpdateAction, "UPDATE_BOARD", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));


    // assign DELETE key - delete message
        final Action deleteMessageAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
                deleteSelectedMessage();
            }
        };
        frostMessageTab.setKeyActionForNewsTab(deleteMessageAction, "DEL_MSG", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        frostMessageTab.setKeyActionForNewsTab(deleteMessageAction, "DEL_MSG", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));

    // remove ENTER assignment from table
        messageTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).getParent().remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0));

    // assign ENTER key - open message viewer
        final Action openMessageAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
                showCurrentMessagePopupWindow();
            }
        };
        frostMessageTab.setKeyActionForNewsTab(openMessageAction, "OPEN_MSG", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));

    // assign N key - next unread (to whole news panel, including tree)
        this.getActionMap().put("NEXT_MSG", new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
                selectNextUnreadMessage();
            }
        });
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), "NEXT_MSG");

    // assign B key - set BAD
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "SET_BAD");
        this.getActionMap().put("SET_BAD", new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
                setTrustState_actionPerformed(IdentityState.BAD);
            }
        });

    // assign G key - set GOOD
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_G, 0), "SET_GOOD");
        this.getActionMap().put("SET_GOOD", new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
                setTrustState_actionPerformed(IdentityState.GOOD);
            }
        });

    // assign C key - set CHECK
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "SET_CHECK");
        this.getActionMap().put("SET_CHECK", new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
                setTrustState_actionPerformed(IdentityState.CHECK);
            }
        });

    // assign O key - set OBSERVE
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_O, 0), "SET_OBSERVE");
        this.getActionMap().put("SET_OBSERVE", new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
                setTrustState_actionPerformed(IdentityState.OBSERVE);
            }
        });

    // assign F key - toggle FLAGGED
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "TOGGLE_FLAGGED");
        this.getActionMap().put("TOGGLE_FLAGGED", new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
                updateBooleanState(BooleanState.FLAGGED);
            }
        });

    // assign S key - toggle STARRED
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "TOGGLE_STARRED");
        this.getActionMap().put("TOGGLE_STARRED", new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
                updateBooleanState(BooleanState.STARRED);
            }
        });

    // assign J key - toggle JUNK
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0), "TOGGLE_JUNK");
        this.getActionMap().put("TOGGLE_JUNK", new AbstractAction() {
            public void actionPerformed(final ActionEvent event) {
                updateBooleanState(BooleanState.JUNK);
            }
        });
    }

    public void saveLayout(final Settings frostSettings) {
        frostSettings.setValue(Settings.MSGTABLE_MSGTEXT_DIVIDER_LOCATION,
                msgTableAndMsgTextSplitpane.getDividerLocation());

        getMessageTable().saveLayout(frostSettings);
    }

    private void fontChanged() {
        final String fontName = settings.getString(Settings.MESSAGE_LIST_FONT_NAME);
        final int fontStyle = settings.getInteger(Settings.MESSAGE_LIST_FONT_STYLE);
        final int fontSize = settings.getInteger(Settings.MESSAGE_LIST_FONT_SIZE);
        Font font = new Font(fontName, fontStyle, fontSize);
        if (!font.getFamily().equals(fontName)) {
            logger.error("The selected font was not found in your system");
            logger.error("That selection will be changed to \"SansSerif\".");
            settings.setValue(Settings.MESSAGE_LIST_FONT_NAME, "SansSerif");
            font = new Font("SansSerif", fontStyle, fontSize);
        }
        messageTable.setFont(font);
    }

    /**
     * Gets the content of the message selected in the tofTable.
     * @param e This selectionEv ent is needed to determine if the Table is just being edited
     * @param table The tofTable
     * @param messages A Vector containing all MessageObjects that are just displayed by the table
     * @return The content of the message
     */
    private FrostMessageObject evalSelection(final ListSelectionEvent e, final JTable table, final Board board) {
//        if( (!e.getValueIsAdjusting() && !table.isEditing()) ) {
        if( !table.isEditing() ) {

            // more than 1 selected row is handled specially, only used to delete/undelete messages
            if( table.getSelectedRowCount() > 1 ) {
                return null;
            }
            final int row = table.getSelectedRow();
            if( (row != -1) && (row < getMessageTableModel().getRowCount()) ) {

                final FrostMessageObject message = (FrostMessageObject)getMessageTableModel().getRow(row);

                if( message != null ) {

                    if( message.isNew() == false ) {
                        // its a read message, nothing more to do here ...
                        return message;
                    }

                    // this is a new message
                    message.setNew(false); // mark as read

                    getMessageTableModel().fireTableRowsUpdated(row, row);

                    // determine thread root msg of this msg
                    final FrostMessageObject threadRootMsg = message.getThreadRootMessage();

                    // update thread root to reset unread msg childs marker
                    if( (threadRootMsg != message) && (threadRootMsg != null) ) {
                        getMessageTreeModel().nodeChanged(threadRootMsg);
                    }

                    board.decUnreadMessageCount();

                    MainFrame.getInstance().updateMessageCountLabels(board);
                    MainFrame.getInstance().updateTofTree(board);

                    final Thread saver = new Thread() {
                        @Override
                        public void run() {
                            // save the changed isnew state into the database
                            MessageStorage.inst().updateMessage(message);
                        }
                    };
                    saver.start();

                    return message;
                }
            }
        }
        return null;
    }

    private void messageTable_itemSelected(final ListSelectionEvent e) {

        final AbstractNode selectedNode = frostMessageTab.getTofTreeModel().getSelectedNode();
        if (selectedNode.isFolder()) {
            setGoodButton.setEnabled(false);
            setCheckButton.setEnabled(false);
            setBadButton.setEnabled(false);
            setObserveButton.setEnabled(false);
            replyButton.setEnabled(false);
            saveMessageButton.setEnabled(false);
            return;
        } else if(!selectedNode.isBoard()) {
            return;
        }

        final Board selectedBoard = (Board) selectedNode;

        // board selected
        final FrostMessageObject newSelectedMessage = evalSelection(e, messageTable, selectedBoard);
        if( newSelectedMessage == selectedMessage ) {
            return; // user is reading a message, selection did NOT change
        } else {
            selectedMessage = newSelectedMessage;
        }

        if (selectedMessage != null) {

            if( selectedMessage.isDummy() ) {
                getMessageTextPane().update_boardSelected();
                clearSubjectTextLabel();
                setGoodButton.setEnabled(false);
                setCheckButton.setEnabled(false);
                setBadButton.setEnabled(false);
                setObserveButton.setEnabled(false);
                replyButton.setEnabled(false);
                saveMessageButton.setEnabled(false);
                return;
            }

            MainFrame.getInstance().displayNewMessageIcon(false);

            if (selectedBoard.isReadAccessBoard() == false) {
                replyButton.setEnabled(true);
            } else {
                replyButton.setEnabled(false);
            }

            if( identitiesManager.isMySelf(selectedMessage.getFromName()) ) {
                setGoodButton.setEnabled(false);
                setCheckButton.setEnabled(false);
                setBadButton.setEnabled(false);
                setObserveButton.setEnabled(false);
            } else if (selectedMessage.isMessageStatusCHECK()) {
                setCheckButton.setEnabled(false);
                setGoodButton.setEnabled(true);
                setBadButton.setEnabled(true);
                setObserveButton.setEnabled(true);
            } else if (selectedMessage.isMessageStatusGOOD()) {
                setGoodButton.setEnabled(false);
                setCheckButton.setEnabled(true);
                setBadButton.setEnabled(true);
                setObserveButton.setEnabled(true);
            } else if (selectedMessage.isMessageStatusBAD()) {
                setBadButton.setEnabled(false);
                setGoodButton.setEnabled(true);
                setCheckButton.setEnabled(true);
                setObserveButton.setEnabled(true);
            } else if (selectedMessage.isMessageStatusOBSERVE()) {
                setObserveButton.setEnabled(false);
                setGoodButton.setEnabled(true);
                setCheckButton.setEnabled(true);
                setBadButton.setEnabled(true);
            } else {
                setGoodButton.setEnabled(false);
                setCheckButton.setEnabled(false);
                setBadButton.setEnabled(false);
                setObserveButton.setEnabled(false);
            }

            getMessageTextPane().update_messageSelected(selectedMessage);
            updateSubjectTextLabel(selectedMessage.getSubject(), selectedMessage.getFromIdentity());

            if (selectedMessage.getContent().length() > 0) {
                saveMessageButton.setEnabled(true);
            } else {
                saveMessageButton.setEnabled(false);
            }

        } else {
            // no msg selected
            getMessageTextPane().update_boardSelected();
            clearSubjectTextLabel();
            replyButton.setEnabled(false);
            saveMessageButton.setEnabled(false);

            setGoodButton.setEnabled(false);
            setCheckButton.setEnabled(false);
            setBadButton.setEnabled(false);
            setObserveButton.setEnabled(false);
        }
    }

    private void newMessageButton_actionPerformed() {
        final AbstractNode node = frostMessageTab.getTofTreeModel().getSelectedNode();
        if( (node == null) || !node.isBoard() ) {
            return;
        }
        final Board board = (Board) node;
        composeNewMessage(board);
    }

    public void composeNewMessage(final Board targetBoard) {
        if( (targetBoard == null) || targetBoard.isReadAccessBoard() ) {
            return;
        }
        final MessageFrame newMessageFrame = new MessageFrame(settings, mainFrame);
        newMessageFrame.composeNewMessage(targetBoard, "No subject", "");
    }

    public void setTrustState_actionPerformed(final IdentityState idState) {

        final List<FrostMessageObject> selectedMessages = getSelectedMessages();
        if( selectedMessages.size() == 0 ) {
            return;
        }

        boolean idChanged = false;
        for(final FrostMessageObject targetMessage  : selectedMessages ) {
            final Identity id = getSelectedMessageFromIdentity(targetMessage);
            if( id == null ) {
                continue;
            }
            if( (idState == IdentityState.GOOD) && !id.isGOOD() ) {
                id.setGOOD();
                idChanged = true;
            } else if( (idState == IdentityState.OBSERVE) && !id.isOBSERVE() ) {
                id.setOBSERVE();
                idChanged = true;
            } else if( (idState == IdentityState.CHECK) && !id.isCHECK() ) {
                id.setCHECK();
                idChanged = true;
            } else if( (idState == IdentityState.BAD) && !id.isBAD() ) {
                id.setBAD();
                idChanged = true;
            }
        }
        // any id changed, gui update needed?
        if( idChanged ) {
            updateTableAfterChangeOfIdentityState();
            if( messageTable.getSelectedRows().length == 1 ) {
                // keep msg selected, change toolbar buttons
                setGoodButton.setEnabled( !(idState == IdentityState.GOOD) );
                setCheckButton.setEnabled( !(idState == IdentityState.CHECK) );
                setBadButton.setEnabled( !(idState == IdentityState.BAD) );
                setObserveButton.setEnabled( !(idState == IdentityState.OBSERVE) );
            }
        }
    }


    public void setComment_actionPerformed() {

        final List<FrostMessageObject> selectedMessages = getSelectedMessages();
        if( selectedMessages.size() == 0 ) {
            return;
        }

        for(final FrostMessageObject targetMessage  : selectedMessages ) {
            final Identity identity = getSelectedMessageFromIdentity(targetMessage);
            if( identity == null ) {
                continue;
            }
            ( new SetIdentityCommentDialog(identity) ).setVisible(true);
        }
    }

    private void toggleShowOnly_actionPerformed(final ActionEvent e) {

        if (e.getSource() == toggleShowUnreadOnly) {
            toggleShowStarredOnly.setSelected(false);
            toggleShowFlaggedOnly.setSelected(false);
        } else if (e.getSource() == toggleShowStarredOnly) {
            toggleShowUnreadOnly.setSelected(false);
            toggleShowFlaggedOnly.setSelected(false);
        } else {
            toggleShowUnreadOnly.setSelected(false);
            toggleShowStarredOnly.setSelected(false);
        }

    	Core.frostSettings.setValue(Settings.SHOW_UNREAD_ONLY, toggleShowUnreadOnly.isSelected());
    	Core.frostSettings.setValue(Settings.SHOW_STARRED_ONLY, toggleShowStarredOnly.isSelected());
    	Core.frostSettings.setValue(Settings.SHOW_FLAGGED_ONLY, toggleShowFlaggedOnly.isSelected());

        // reload messages
        MainFrame.getInstance().tofTree_actionPerformed(null, true);
    }

    private void toggleShowThreads_actionPerformed(final ActionEvent e) {
        final boolean oldValue = Core.frostSettings.getBoolean(Settings.SHOW_THREADS);
        final boolean newValue = !oldValue;
        Core.frostSettings.setValue(Settings.SHOW_THREADS, newValue);
        // reload messages
        MainFrame.getInstance().tofTree_actionPerformed(null, true);
    }

    private void toggleShowSmileys_actionPerformed(final ActionEvent e) {
        final boolean oldValue = Core.frostSettings.getBoolean(Settings.SHOW_SMILEYS);
        final boolean newValue = !oldValue;
        Core.frostSettings.setValue(Settings.SHOW_SMILEYS, newValue);
        // redraw is done in textpane by propertychangelistener!
    }

    private void toggleShowHyperlinks_actionPerformed(final ActionEvent e) {
        final boolean oldValue = Core.frostSettings.getBoolean(Settings.SHOW_KEYS_AS_HYPERLINKS);
        final boolean newValue = !oldValue;
        Core.frostSettings.setValue(Settings.SHOW_KEYS_AS_HYPERLINKS, newValue);
        // redraw is done in textpane by propertychangelistener!
    }

    private void refreshLanguage() {
        newMessageButton.setToolTipText(language.getString("MessagePane.toolbar.tooltip.newMessage"));
        replyButton.setToolTipText(language.getString("MessagePane.toolbar.tooltip.reply"));
        saveMessageButton.setToolTipText(language.getString("MessagePane.toolbar.tooltip.saveMessage"));
        nextUnreadMessageButton.setToolTipText(language.getString("MessagePane.toolbar.tooltip.nextUnreadMessage"));
        setGoodButton.setToolTipText(language.getString("MessagePane.toolbar.tooltip.setToGood"));
        setBadButton.setToolTipText(language.getString("MessagePane.toolbar.tooltip.setToBad"));
        setCheckButton.setToolTipText(language.getString("MessagePane.toolbar.tooltip.setToCheck"));
        setObserveButton.setToolTipText(language.getString("MessagePane.toolbar.tooltip.setToObserve"));
        updateBoardButton.setToolTipText(language.getString("MessagePane.toolbar.tooltip.update"));
        toggleShowUnreadOnly.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowUnreadOnly"));
        toggleShowFlaggedOnly.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowFlaggedOnly"));
        toggleShowStarredOnly.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowStarredOnly"));
        toggleShowThreads.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowThreads"));
        toggleShowSmileys.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowSmileys"));
        toggleShowHyperlinks.setToolTipText(language.getString("MessagePane.toolbar.tooltip.toggleShowHyperlinks"));
        subjectLabel.setText(language.getString("MessageWindow.subject")+": ");

        allMessagesCountPrefix = language.getString("MessagePane.toolbar.labelAllMessageCount")+": ";
        allMessagesCountLabel.setText(allMessagesCountPrefix);
        unreadMessagesCountPrefix = language.getString("MessagePane.toolbar.labelNewMessageCount")+": ";
        unreadMessagesCountLabel.setText(unreadMessagesCountPrefix);

        updateLabelSize(allMessagesCountLabel);
        updateLabelSize(unreadMessagesCountLabel);
    }

    private void replyButton_actionPerformed(final ActionEvent e) {
        final FrostMessageObject origMessage = selectedMessage;
        composeReply(origMessage, parentFrame);
    }

    /**
     * Creates a new frame for composing a reply to the message recieved as a parameter.
     *
     * @param origMessage
     *            the message that is going to be replied to in the new frame
     * @param parent
     *            the parent Window of the new frame to be created
     */
    public void composeReply(final FrostMessageObject origMessage, final Window parent) {

        final Board targetBoard = frostMessageTab.getTofTreeModel().getBoardByName(origMessage.getBoard().getName());
        if( targetBoard == null ) {
            final String title = language.getString("MessagePane.missingBoardError.title");
            final String txt = language.formatMessage("MessagePane.missingBoardError.text", origMessage.getBoard().getName());
            JOptionPane.showMessageDialog(parent, txt, title, JOptionPane.ERROR);
            return;
        }

        String subject = origMessage.getSubject();
        if (subject.startsWith("Re:") == false) {
            subject = "Re: " + subject;
        }

        // add msgId we answer to the inReplyTo list
        String inReplyTo = null;
        if( origMessage.getMessageId() != null ) {
            inReplyTo = origMessage.getInReplyTo();
            if( inReplyTo == null ) {
                inReplyTo = origMessage.getMessageId();
            } else {
                inReplyTo += ","+origMessage.getMessageId();
            }
        }

        if (origMessage.getRecipientName() != null) {
            // This message was encripted for me, so reply encrypted

            Identity senderId = origMessage.getFromIdentity();
            if (senderId == null) {
                // The identity that sent the message is missing, so the message cannot be replied to
                final String title = language.getString("MessagePane.missingSenderIdentityError.title");
                final String txt = language.formatMessage("MessagePane.missingSenderIdentityError.text", origMessage.getFromName());
                JOptionPane.showMessageDialog(parent, txt, title, JOptionPane.ERROR_MESSAGE);
                return;
            }

            Identity recipientId = identitiesManager.getIdentity(origMessage.getRecipientName());
            if (recipientId == null) {
                // The identity that received the message is missing, so the message cannot be replied to
                final String title = language.getString("MessagePane.missingRecipientIdentityError.title");
                final String txt = language.formatMessage("MessagePane.missingRecipientIdentityError.text", origMessage.getRecipientName());
                JOptionPane.showMessageDialog(parent, txt, title, JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalIdentity fromId;
            List<Identity> toIds = new ArrayList<Identity>();
            if (senderId instanceof LocalIdentity) {
                // We want to reply to our own message. Therefore, we (senderId) will be the destination of the new message by default, while
                // also letting the user easily specify as destination the recipient (recipientId) of the original message.
                fromId = (LocalIdentity) senderId;
                toIds.add(senderId);
                if (!senderId.equals(recipientId)) {
                    // The recipient is actually different from the sender. Therefore, we add it.
                    toIds.add(recipientId);
                }
            } else {
                // The message was not sent by me, so find the identity of mine that was the recipient of this message
                if (recipientId instanceof LocalIdentity) {
                    // The recipient is a local identity of us
                    fromId = (LocalIdentity) recipientId;
                    toIds.add(senderId);
                } else {
                    // The recipient is not a local identity, so the message cannot be replied to
                    final String title = language.getString("MessagePane.missingLocalIdentityError.title");
                    final String txt = language.formatMessage("MessagePane.missingLocalIdentityError.text", origMessage.getRecipientName());
                    JOptionPane.showMessageDialog(parent, txt, title, JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            final MessageFrame newMessageFrame = new MessageFrame(settings, parent);
            newMessageFrame.composeEncryptedReply(
                    targetBoard,
                    subject,
                    inReplyTo,
                    origMessage.getCompleteContent(),
                    toIds,
                    fromId,
                    origMessage);
        } else {
            final MessageFrame newMessageFrame = new MessageFrame(settings, parent);
            newMessageFrame.composeReply(
                    targetBoard,
                    subject,
                    inReplyTo,
                    origMessage.getCompleteContent(),
                    origMessage);
        }
    }

    private void showMessageTablePopupMenu(final MouseEvent e) {
        // show popup menu
        getPopupMenuMessageTable().show(e.getComponent(), e.getX(), e.getY());
    }

    private void showCurrentMessagePopupWindow() {
        if( !isCorrectlySelectedMessage() ) {
            return;
        }
        final MessageWindow messageWindow = new MessageWindow( mainFrame, selectedMessage, this.getSize() );
        messageWindow.setVisible(true);
    }

    private void updateButton_actionPerformed(final ActionEvent e) {
        // restarts all finished threads if there are some long running threads
        final AbstractNode node = frostMessageTab.getTofTreeModel().getSelectedNode();
        if ((node != null) && node.isBoard() ) {
            final Board b = (Board) node;
            if( b.isManualUpdateAllowed() ) {
                frostMessageTab.getTofTree().updateBoard(b);
            }
        }
    }

    private void boardsTree_actionPerformed(final TreeSelectionEvent e) {

        if (((TreeNode) frostMessageTab.getTofTreeModel().getRoot()).getChildCount() == 0) {
            // There are no boards
            getMessageTextPane().update_noBoardsFound();
            clearSubjectTextLabel();
        } else {
            // There are boards
            final AbstractNode node = (AbstractNode)frostMessageTab .getTofTree().getLastSelectedPathComponent();
            if (node != null) {
                if (node.isBoard()) {
                    // node is a board
                    // FIXME: reset message history!
                    getMessageTextPane().update_boardSelected();
                    clearSubjectTextLabel();
                    updateBoardButton.setEnabled(true);
                    saveMessageButton.setEnabled(false);
                    replyButton.setEnabled(false);
                    if (((Board)node).isReadAccessBoard()) {
                        newMessageButton.setEnabled(false);
                    } else {
                        newMessageButton.setEnabled(true);
                    }
                } else if(node.isFolder()) {
                    // node is a folder
                    newMessageButton.setEnabled(false);
                    saveMessageButton.setEnabled(false);
                    updateBoardButton.setEnabled(false);
                    getMessageTextPane().update_folderSelected();
                    clearSubjectTextLabel();
                }
            }
        }
    }

    /**
     * returns true if message was correctly selected
     * @return
     */
    private boolean isCorrectlySelectedMessage() {
        final int row = messageTable.getSelectedRow();
        if ((row < 0)
            || (selectedMessage == null)
            || (frostMessageTab.getTofTreeModel().getSelectedNode() == null)
            || !frostMessageTab.getTofTreeModel().getSelectedNode().isBoard()
            || selectedMessage.isDummy() )
        {
            return false;
        }
        return true;
    }

    private void markSelectedMessagesReadOrUnread(final boolean markRead) {
        final AbstractNode node = frostMessageTab.getTofTreeModel().getSelectedNode();
        if( (node == null) || !node.isBoard() ) {
            return;
        }
        final Board board = (Board) node;

        final List<FrostMessageObject> selectedMessages = getSelectedMessages();
        if( selectedMessages.size() == 0 ) {
            return;
        }

        // set all selected messages unread
        final ArrayList<FrostMessageObject> saveMessages = new ArrayList<FrostMessageObject>();
        final DefaultTreeModel model = (DefaultTreeModel)MainFrame.getInstance().getMessagePanel().getMessageTable().getTree().getModel();
        for(final FrostMessageObject targetMessage : selectedMessages ) {
            if( markRead ) {
                // mark read
                if( targetMessage.isNew() ) {
                    targetMessage.setNew(false);
                    board.decUnreadMessageCount();
                }
            } else {
                // mark unread
                if( !targetMessage.isNew() ) {
                    targetMessage.setNew(true);
                    board.incUnreadMessageCount();
                }
            }
            model.nodeChanged(targetMessage);
            saveMessages.add(targetMessage);
        }

        if( !markRead ) {
            messageTable.removeRowSelectionInterval(0, messageTable.getRowCount() - 1);
        }

        // update new and shown message count
        updateMessageCountLabels(board);
        mainFrame.updateTofTree(board);

        final Thread saver = new Thread() {
            @Override
            public void run() {
                // save message, we must save the changed deleted state
                for( final Object element : saveMessages ) {
                    final FrostMessageObject targetMessage = (FrostMessageObject)element;
                    MessageStorage.inst().updateMessage(targetMessage);
                }
            }
        };
        saver.start();
    }

    private void markThreadRead() {
        if( selectedMessage == null ) {
            return;
        }

        final TreeNode[] rootPath = selectedMessage.getPath();
        if( rootPath.length < 2 ) {
            return;
        }

        final FrostMessageObject levelOneMsg = (FrostMessageObject)rootPath[1];

        final DefaultTreeModel model = MainFrame.getInstance().getMessagePanel().getMessageTreeModel();
        final AbstractNode node = frostMessageTab.getTofTreeModel().getSelectedNode();
        if( (node == null) || !node.isBoard() ) {
            return;
        }
        final Board board = (Board) node;
        final LinkedList<FrostMessageObject> msgList = new LinkedList<FrostMessageObject>();

		final Enumeration<TreeNode> frostMessageObjectEnumeration = levelOneMsg.depthFirstEnumeration();
        while( frostMessageObjectEnumeration.hasMoreElements() ) {
			final FrostMessageObject frostMessageObject = (FrostMessageObject) frostMessageObjectEnumeration
					.nextElement();

            if( frostMessageObject.isNew() ) {
                msgList.add(frostMessageObject);
                frostMessageObject.setNew(false);
                // don't update row when row is not shown, this corrupts treetable layout
                if( MainFrame.getInstance().getMessagePanel().getMessageTable().getTree().isVisible(new TreePath(frostMessageObject.getPath())) ) {
                    model.nodeChanged(frostMessageObject);
                }
                board.decUnreadMessageCount();
            }
        }

        updateMessageCountLabels(board);
        mainFrame.updateTofTree(board);

        final Thread saver = new Thread() {
            @Override
            public void run() {
                // save message, we must save the changed deleted state into the database
                for( final Object element : msgList ) {
                    final FrostMessageObject mo = (FrostMessageObject) element;
                    MessageStorage.inst().updateMessage(mo);
                }
            }
        };
        saver.start();
    }

    public void deleteSelectedMessage() {

        final AbstractNode node = frostMessageTab.getTofTreeModel().getSelectedNode();
        if( (node == null) || !node.isBoard() ) {
            return;
        }
        final Board board = (Board) node;

        final List<FrostMessageObject> selectedMessages = getSelectedMessages();
        if( selectedMessages.size() == 0 ) {
            return;
        }

        // set all selected messages deleted
        final ArrayList<FrostMessageObject> saveMessages = new ArrayList<FrostMessageObject>();
        final DefaultTreeModel model = (DefaultTreeModel)MainFrame.getInstance().getMessagePanel().getMessageTable().getTree().getModel();
        for( final FrostMessageObject targetMessage : selectedMessages ) {
            targetMessage.setDeleted(true);
            if( targetMessage.isNew() ) {
                targetMessage.setNew(false);
                board.decUnreadMessageCount();
            }
            // we don't remove the message immediately, they are not loaded during next change to this board
            // needs repaint or the line which crosses the message isn't completely seen
            model.nodeChanged(targetMessage);
            saveMessages.add(targetMessage);
        }

        // update new and shown message count
        updateMessageCountLabels(board);
        mainFrame.updateTofTree(board);

        final Thread saver = new Thread() {
            @Override
            public void run() {
                // save message, we must save the changed deleted state
                for( final Object element : saveMessages ) {
                    final FrostMessageObject targetMessage = (FrostMessageObject)element;
                    MessageStorage.inst().updateMessage(targetMessage);
                }
            }
        };
        saver.start();
    }

    private void undeleteSelectedMessage() {
        final List<FrostMessageObject> selectedMessages = getSelectedMessages();
        if( selectedMessages.size() == 0 ) {
            return;
        }

        // set all selected messages deleted
        final ArrayList<FrostMessageObject> saveMessages = new ArrayList<FrostMessageObject>();
        final DefaultTreeModel model = (DefaultTreeModel)MainFrame.getInstance().getMessagePanel().getMessageTable().getTree().getModel();
        for( final FrostMessageObject targetMessage : selectedMessages ) {
            if( !targetMessage.isDeleted() ) {
                continue;
            }
            targetMessage.setDeleted(false);

            // needs repaint or the line which crosses the message isn't completely seen
            model.nodeChanged(targetMessage);

            saveMessages.add(targetMessage);
        }

        final Thread saver = new Thread() {
            @Override
            public void run() {
                // save message, we must save the changed deleted state
                for( final Object element : saveMessages ) {
                    final FrostMessageObject targetMessage = (FrostMessageObject)element;
                    MessageStorage.inst().updateMessage(targetMessage);
                }
            }
        };
        saver.start();
    }

    /**
     * @return a list of all selected, non dummy messages; or an empty list
     */
    private java.util.List<FrostMessageObject> getSelectedMessages() {
        if( (messageTable.getSelectedRowCount() <= 1) && !isCorrectlySelectedMessage() ) {
            return Collections.emptyList();
        }

        final int[] rows = messageTable.getSelectedRows();

        if( (rows == null) || (rows.length == 0) ) {
            return Collections.emptyList();
        }

        final java.util.List<FrostMessageObject> msgs = new ArrayList<FrostMessageObject>(rows.length);
        for( final int ix : rows ) {
            final FrostMessageObject targetMessage = (FrostMessageObject)getMessageTableModel().getRow(ix);
            if( (targetMessage != null) && !targetMessage.isDummy() ) {
                msgs.add(targetMessage);
            }
        }
        return msgs;
    }

    /**
     * @param identitiesManager
     */
    public void setIdentitiesManager(final IdentitiesManager identitiesManager) {
        this.identitiesManager = identitiesManager;
    }

    public void setParentFrame(final JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    /**
     * Method that update the Msg and New counts for tof table
     * Expects that the boards messages are shown in table
     * @param board
     */
    public void updateMessageCountLabels(final AbstractNode node) {
        if (node.isFolder()) {
            allMessagesCountLabel.setText("");
            unreadMessagesCountLabel.setText("");
            nextUnreadMessageButton.setEnabled(false);
        } else if (node.isBoard()) {
            int allMessages = 0;
            final FrostMessageObject rootNode = (FrostMessageObject)MainFrame.getInstance().getMessageTreeModel().getRoot();

			final Enumeration<TreeNode> frostMessageObjectEnumeration = rootNode.depthFirstEnumeration();
            while( frostMessageObjectEnumeration.hasMoreElements() ) {
				FrostMessageObject frostMessageObject = (FrostMessageObject) frostMessageObjectEnumeration
						.nextElement();
				if (!frostMessageObject.isDummy()) {
                    allMessages++;
                }
            }
            allMessagesCountLabel.setText(allMessagesCountPrefix + allMessages);

            final int unreadMessages = ((Board)node).getUnreadMessageCount();
            unreadMessagesCountLabel.setText(unreadMessagesCountPrefix + unreadMessages);
            if( unreadMessages > 0 ) {
                nextUnreadMessageButton.setEnabled(true);
            } else {
                nextUnreadMessageButton.setEnabled(false);
            }
        }
    }

    private Identity getSelectedMessageFromIdentity(final FrostMessageObject msg) {
        if( msg == null ) {
            return null;
        }
        if( !msg.isSignatureStatusVERIFIED() ) {
            return null;
        }
        final Identity ident = msg.getFromIdentity();
        if(ident == null ) {
            logger.error("no identity in list for from: {}", msg.getFromName());
            return null;
        }
        if( ident instanceof LocalIdentity ) {
            logger.info("Ignored request to change my own ID state");
            return null;
        }
        return ident;
    }

    public FrostMessageObject getSelectedMessage() {
        if( !isCorrectlySelectedMessage() ) {
            return null;
        }
        return selectedMessage;
    }

    public void updateTableAfterChangeOfIdentityState() {
        // walk through shown messages and remove unneeded (e.g. if hideBad)
        // remember selected msg and select next
        final AbstractNode node = frostMessageTab.getTofTreeModel().getSelectedNode();
        if( (node == null) || !node.isBoard() ) {
            return;
        }
        final Board board = (Board) node;
        // a board is selected and shown
        final DefaultTreeModel model = getMessageTreeModel();
        final DefaultMutableTreeNode rootnode = (DefaultMutableTreeNode)model.getRoot();

		for (final Enumeration<TreeNode> e = rootnode.depthFirstEnumeration(); e.hasMoreElements();) {
			final FrostMessageObject frostMessageObject = (FrostMessageObject) e.nextElement();
            if( !(frostMessageObject instanceof FrostMessageObject) ) {
                logger.error("frostMessageObject not of type FrostMessageObject");
                continue;
            }
            final int row = MainFrame.getInstance().getMessageTreeTable().getRowForNode(frostMessageObject);
            if( row >= 0 ) {
                getMessageTableModel().fireTableRowsUpdated(row, row);
            }
        }
        MainFrame.getInstance().updateMessageCountLabels(board);
    }

    /**
     * Search through all messages, find next unread message by date (earliest message in table).
     */
    public void selectNextUnreadMessage() {

        FrostMessageObject nextMessage = null;

        final DefaultTreeModel tableModel = getMessageTreeModel();

        // use a different method based on threaded or not threaded view
        if( Core.frostSettings.getBoolean(Settings.SHOW_THREADS) ) {

            final FrostMessageObject initial = getSelectedMessage();

            if (initial != null) {

            	final TreeNode[] path = initial.getPath();
            	final java.util.List<TreeNode> path_list = java.util.Arrays.asList(path);

            	for( int idx = initial.getLevel(); (idx > 0) && (nextMessage == null); idx-- ) {
            		final FrostMessageObject parent = (FrostMessageObject) path[idx];
            		final LinkedList<FrostMessageObject> queue = new LinkedList<FrostMessageObject>();
            		for( queue.add(parent); !queue.isEmpty() && (nextMessage == null); ) {
            			final FrostMessageObject message = queue.removeFirst();
            			if( message.isNew() ) {
            				nextMessage = message;
            				break;
            			}

						final Enumeration<TreeNode> children = message.children();
            			while( children.hasMoreElements() ) {
							final FrostMessageObject frostMessageObject = (FrostMessageObject) children.nextElement();
            				if( !path_list.contains(frostMessageObject) ) {
            					queue.add(frostMessageObject);
            				}
            			}
            		}
            	}
            }
        }
        if( nextMessage == null ) {
			final Enumeration<TreeNode> frostMessageObjectEnumeration = ((DefaultMutableTreeNode) tableModel.getRoot())
					.depthFirstEnumeration();
            while( frostMessageObjectEnumeration.hasMoreElements() ) {
				final FrostMessageObject frostMessageObject = (FrostMessageObject) frostMessageObjectEnumeration
						.nextElement();
                if( frostMessageObject.isNew() ) {
                    if( nextMessage == null ) {
                        nextMessage = frostMessageObject;
                    } else {
                        if( nextMessage.getDateAndTimeString().compareTo(frostMessageObject.getDateAndTimeString()) > 0 ) {
                            nextMessage = frostMessageObject;
                        }
                    }
                }
            }
        }

        if( nextMessage == null ) {
            // code to move to next board???
        } else {
            messageTable.removeRowSelectionInterval(0, getMessageTableModel().getRowCount() - 1);
            messageTable.getTree().makeVisible(new TreePath(nextMessage.getPath()));
            final int row = messageTable.getRowForNode(nextMessage);
            if( row >= 0 ) {
                messageTable.addRowSelectionInterval(row, row);
                messageListScrollPane.getVerticalScrollBar().setValue(
                        (row == 0 ? row : row - 1) * messageTable.getRowHeight());
            }
        }
    }

    /**
     * Update one of flagged, starred, junk in all currently selected messages.
     */
    public void updateBooleanState(final BooleanState state) {
        updateBooleanState(state, getSelectedMessages());
    }

    /**
     * Update one of flagged, starred, junk in all messages in msgs.
     * The current state of the first message in list is toggled.
     */
    private void updateBooleanState(final BooleanState state, final List<FrostMessageObject> msgs) {
        if( msgs.isEmpty() ) {
            return;
        }

        final boolean doEnable;
        final FrostMessageObject firstMessage = msgs.get(0);
        switch(state) {
            case FLAGGED: doEnable = !firstMessage.isFlagged(); break;
            case STARRED: doEnable = !firstMessage.isStarred(); break;
            case JUNK:    doEnable = !firstMessage.isJunk(); break;
            default: return;
        }

        final List<Identity> identitiesToMarkBad;
        if( (state == BooleanState.JUNK)
                && Core.frostSettings.getBoolean(Settings.JUNK_MARK_JUNK_IDENTITY_BAD)
                && doEnable )
        {
            // we set junk to true and we want to set all junk senders to bad
            identitiesToMarkBad = new ArrayList<Identity>();
        } else {
            identitiesToMarkBad = null;
        }

        for( final FrostMessageObject message : msgs ) {
            switch(state) {
                case FLAGGED: message.setFlagged(doEnable); break;
                case STARRED: message.setStarred(doEnable); break;
                case JUNK:    message.setJunk(doEnable); break;
            }

            final int row = MainFrame.getInstance().getMessageTreeTable().getRowForNode(message);
            if( row >= 0 ) {
                getMessageTableModel().fireTableRowsUpdated(row, row);
            }

            if( identitiesToMarkBad != null ) {
                final Identity id = message.getFromIdentity();
                if( (id != null)
                        && id.isCHECK() )
                {
                    identitiesToMarkBad.add(id);
                }
            }

            // for flagged/starred, update marker on thread root message
            if( state != BooleanState.JUNK ) {
                // determine thread root msg of this msg
                final FrostMessageObject threadRootMsg = message.getThreadRootMessage();

                // update thread root to update the marker border
                if( (threadRootMsg != message) && (threadRootMsg != null) ) {
                    getMessageTreeModel().nodeChanged(threadRootMsg);
                }
            }
        }

        // if flagged or starred, update board markers in board tree
        if( state != BooleanState.JUNK ) {
            // update flagged/starred indicators in board tree
            boolean hasStarredWork = false;
            boolean hasFlaggedWork = false;

            final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)firstMessage.getRoot();
			final Enumeration<TreeNode> frostMessageObjectEnumeration = rootNode.depthFirstEnumeration();
            while( frostMessageObjectEnumeration.hasMoreElements()) {

				final FrostMessageObject frostMessageObject = (FrostMessageObject) frostMessageObjectEnumeration
						.nextElement();
                if( !hasStarredWork && frostMessageObject.isStarred() ) {
                    hasStarredWork = true;
                }
                if( !hasFlaggedWork && frostMessageObject.isFlagged() ) {
                    hasFlaggedWork = true;
                }
                if( hasFlaggedWork && hasStarredWork ) {
                    break; // finished
                }
            }
            final Board board = firstMessage.getBoard();
            board.setFlaggedMessages(hasFlaggedWork);
            board.setStarredMessages(hasStarredWork);
            MainFrame.getInstance().updateTofTree(board);
        }

        // maybe set identities to bad
        if( (identitiesToMarkBad != null)
                && !identitiesToMarkBad.isEmpty() )
        {
            for( final Identity id : identitiesToMarkBad ) {
                id.setBAD();
            }

            updateTableAfterChangeOfIdentityState();
            if( msgs.size() == 1 ) {
                // keep msg selected, change toolbar buttons
                setGoodButton.setEnabled( true );
                setCheckButton.setEnabled( true );
                setBadButton.setEnabled( false );
                setObserveButton.setEnabled( true );
            }
        }

        // save all changed messages
        final Thread saver = new Thread() {
            @Override
            public void run() {
                if( !MessageStorage.inst().beginExclusiveThreadTransaction() ) {
                    logger.error("Failed to start EXCLUSIVE transaction in MessageStore!");
                    return;
                }
                try {
                    for( final FrostMessageObject message : msgs ) {
                        MessageStorage.inst().updateMessage(message, false);
                    }
                } finally {
                    MessageStorage.inst().endThreadTransaction();
                }
            }
        };
        saver.start();
    }

    private void updateSubjectTextLabel(final String newText, final Identity fromId) {
        if( newText == null ) {
            // clear
            subjectTextLabel.setText("");
        } else {
            subjectTextLabel.setText(newText);
        }
        ImageIcon iconToSet = null;
        if( fromId != null ) {
            if( indicateLowReceivedMessages ) {
                final int receivedMsgCount = fromId.getReceivedMessageCount();
                if( receivedMsgCount <= indicateLowReceivedMessagesCountRed ) {
                    iconToSet = MainFrame.getInstance().getMessageTreeTable().receivedOneMessage;
                } else if( receivedMsgCount <= indicateLowReceivedMessagesCountLightRed ) {
                    iconToSet = MainFrame.getInstance().getMessageTreeTable().receivedFiveMessages;
                }
            }
        }
        subjectLabel.setIcon(iconToSet);
    }

    private void clearSubjectTextLabel() {
        subjectTextLabel.setText("");
        subjectLabel.setIcon(null);
    }

    public TreeTableModelAdapter getMessageTableModel() {
        return (TreeTableModelAdapter)getMessageTable().getModel();
    }
    public DefaultTreeModel getMessageTreeModel() {
        return (DefaultTreeModel)getMessageTable().getTree().getModel();
    }
    public MessageTreeTable getMessageTable() {
        return messageTable;
    }
    public MessageTextPane getMessageTextPane() {
        return messageTextPane;
    }

    private void updateMsgTableMultilineSelect() {
        if( Core.frostSettings.getBoolean(Settings.MSGTABLE_MULTILINE_SELECT) ) {
            messageTable.setSelectionMode(DefaultListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        } else {
            messageTable.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        }
    }

    private void updateMsgTableResizeMode() {
        if( Core.frostSettings.getBoolean(Settings.MSGTABLE_SCROLL_HORIZONTAL) ) {
            // show horizontal scrollbar if needed
            getMessageTable().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        } else {
            // auto-resize columns, no horizontal scrollbar
            getMessageTable().setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        }
    }

    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Settings.MSGTABLE_MULTILINE_SELECT)) {
            updateMsgTableMultilineSelect();
        } else if (evt.getPropertyName().equals(Settings.MSGTABLE_SCROLL_HORIZONTAL)) {
            updateMsgTableResizeMode();
        } else if (evt.getPropertyName().equals(Settings.SORT_THREADROOTMSGS_ASCENDING)) {
            FrostMessageObject.sortThreadRootMsgsAscending = settings.getBoolean(Settings.SORT_THREADROOTMSGS_ASCENDING);
        } else if (evt.getPropertyName().equals(Settings.INDICATE_LOW_RECEIVED_MESSAGES)) {
            indicateLowReceivedMessages = Core.frostSettings.getBoolean(Settings.INDICATE_LOW_RECEIVED_MESSAGES);
        } else if (evt.getPropertyName().equals(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_RED)) {
            indicateLowReceivedMessagesCountRed = Core.frostSettings.getInteger(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_RED);
        } else if (evt.getPropertyName().equals(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_LIGHTRED)) {
            indicateLowReceivedMessagesCountLightRed = Core.frostSettings.getInteger(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_LIGHTRED);
        }
    }
}
