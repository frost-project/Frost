/*
MessageFrame.java / Frost
Copyright (C) 2001  Frost Project <jtcfrost.sourceforge.net>

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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.ComboBoxEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.ext.AltEdit;
import frost.ext.AltEditCallbackInterface;
import frost.gui.BoardsChooser;
import frost.gui.ScrollableBar;
import frost.gui.SmileyChooserDialog;
import frost.gui.SortedTable;
import frost.gui.model.SortedTableModel;
import frost.gui.model.TableMember;
import frost.gui.model.TableMember.BaseTableMember;
import frost.identities.Identity;
import frost.identities.LocalIdentity;
import frost.messaging.frost.BoardAttachment;
import frost.messaging.frost.FileAttachment;
import frost.messaging.frost.FrostMessageObject;
import frost.messaging.frost.FrostUnsentMessageObject;
import frost.messaging.frost.UnsentMessagesManager;
import frost.messaging.frost.boards.Board;
import frost.storage.perst.messages.MessageStorage;
import frost.util.ClipboardUtil;
import frost.util.DateFun;
import frost.util.FileAccess;
import frost.util.Mixed;
import frost.util.gui.ImmutableArea;
import frost.util.gui.ImmutableAreasDocument;
import frost.util.gui.MiscToolkit;
import frost.util.gui.SimplePopupMenuListener;
import frost.util.gui.TextComponentClipboardMenu;
import frost.util.gui.action.BaseAction;
import frost.util.gui.action.CancelAction;
import frost.util.gui.textpane.AntialiasedTextArea;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

public class MessageFrame extends JFrame
		implements AltEditCallbackInterface, SimplePopupMenuListener, LanguageListener {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(MessageFrame.class);

	private transient Language language;

    private boolean initialized = false;

    private final Window parentWindow;

    private Board board;
    private String repliedMsgId;
	private transient Settings frostSettings;

    private MFAttachedBoardsTable boardsTable;
    private MFAttachedFilesTable filesTable;
    private MFAttachedBoardsTableModel boardsTableModel;
    private MFAttachedFilesTableModel filesTableModel;

    private JSplitPane messageSplitPane = null;
    private JSplitPane attachmentsSplitPane = null;
    private JScrollPane filesTableScrollPane;
    private JScrollPane boardsTableScrollPane;

	private RemoveFilesAction removeFilesAction;
	private AttFilesPopupMenu attFilesPopupMenu;

	private RemoveBoardsAction removeBoardsAction;
	private AttBoardsPopupMenu attBoardsPopupMenu;

	private CutAction cutAction;
	private CopyAction copyAction;
	private PasteAction pasteAction;
	private CancelAction cancelAction;
	private MessageBodyPopupMenu messageBodyPopupMenu;

    private final JButton Bsend = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/mail-forward.png"));
    private final JButton Bcancel = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/user-trash.png"));
    private final JButton BattachFile = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/mail-attachment.png"));
    private final JButton BattachBoard = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/internet-group-chat.png"));

    private final JCheckBox sign = new JCheckBox();
    private final JCheckBox encrypt = new JCheckBox();
	private JComboBox<Identity> buddies;

    private final JLabel Lboard = new JLabel();
    private final JLabel Lfrom = new JLabel();
    private final JLabel Lsubject = new JLabel();
    private final JTextField TFboard = new JTextField(); // Board (To)
    private final JTextField subjectTextField = new JTextField(); // Subject
    private final JButton BchooseSmiley = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/face-smile.png"));

    private final AntialiasedTextArea messageTextArea = new AntialiasedTextArea(); // Text
	private transient ImmutableArea headerArea;
//    private TextHighlighter textHighlighter = null;
    private String oldSender = null;
    private String currentSignature = null;

    private FrostMessageObject repliedMessage = null;

	private JComboBox<Identity> ownIdentitiesComboBox = null;

    private static int openInstanceCount = 0;

    public MessageFrame(final Settings newSettings, final Window tparentWindow) {
        super();
        parentWindow = tparentWindow;
        this.language = Language.getInstance();
        frostSettings = newSettings;

        incOpenInstanceCount();

		messageTextArea.setFont(Core.frostSettings.getFont(Settings.MESSAGE_BODY_FONT_NAME,
				Settings.MESSAGE_BODY_FONT_STYLE, Settings.MESSAGE_BODY_FONT_SIZE, "Monospaced"));
        messageTextArea.setAntiAliasEnabled(frostSettings.getBoolean(Settings.MESSAGE_BODY_ANTIALIAS));
        final ImmutableAreasDocument messageDocument = new ImmutableAreasDocument();
        headerArea = new ImmutableArea(messageDocument);
        messageDocument.addImmutableArea(headerArea); // user must not change the header of the message
        messageTextArea.setDocument(messageDocument);
//        textHighlighter = new TextHighlighter(Color.LIGHT_GRAY);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                windowIsClosing();
            }
            @Override
            public void windowClosed(final WindowEvent e) {
                windowWasClosed();
            }
        });
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    private void windowIsClosing() {
        final String title = language.getString("MessageFrame.discardMessage.title");
        final String text = language.getString("MessageFrame.discardMessage.text");
        final int answer = JOptionPane.showConfirmDialog(
                this,
                text,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if( answer == JOptionPane.YES_OPTION ) {
            dispose();
        }
    }

    private void windowWasClosed() {
        decOpenInstanceCount();
    }

    private void attachBoards_actionPerformed(final ActionEvent e) {

        // get and sort all boards
        final List<Board> allBoards = MainFrame.getInstance().getMessagingTab().getTofTreeModel().getAllBoards();
        if (allBoards.size() == 0) {
            return;
        }
        Collections.sort(allBoards);

        final BoardsChooser chooser = new BoardsChooser(this, allBoards);
        chooser.setLocationRelativeTo(this);
        final List<Board> chosenBoards = chooser.runDialog();
        if ((chosenBoards == null) || (chosenBoards.size() == 0)) { // nothing chosed or cancelled
            return;
        }

        for (int i = 0; i < chosenBoards.size(); i++) {
            final Board chosedBoard = chosenBoards.get(i);

            String privKey = chosedBoard.getPrivateKey();

            if (privKey != null) {
                final int answer =
                    JOptionPane.showConfirmDialog(this,
                        language.formatMessage("MessageFrame.attachBoard.sendPrivateKeyConfirmationDialog.body", chosedBoard.getName()),
                        language.getString("MessageFrame.attachBoard.sendPrivateKeyConfirmationDialog.title"),
                        JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.NO_OPTION) {
                    privKey = null; // don't provide privkey
                }
            }
            // build a new board because maybe privKey shouldn't be uploaded
            final Board aNewBoard =
                new Board(chosedBoard.getName(), chosedBoard.getPublicKey(), privKey, chosedBoard.getDescription());
            final MFAttachedBoard ab = new MFAttachedBoard(aNewBoard);
            boardsTableModel.addRow(ab);
        }
        positionDividers();
    }

    private void attachFile_actionPerformed(final ActionEvent e) {
        final String lastUsedDirectory = frostSettings.getString(Settings.DIR_LAST_USED);
        final JFileChooser fc = new JFileChooser(lastUsedDirectory);
        fc.setDialogTitle(language.getString("MessageFrame.fileChooser.title"));
        fc.setFileHidingEnabled(false);
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);

        final int returnVal = fc.showOpenDialog(MessageFrame.this);
        if( returnVal == JFileChooser.APPROVE_OPTION ) {
            final File[] selectedFiles = fc.getSelectedFiles();
            for( final File element : selectedFiles ) {
                // for convenience remember last used directory
                frostSettings.setValue(Settings.DIR_LAST_USED, element.getPath());

                // collect all chosen files + files in all chosen directories
                final List<File> allFiles = FileAccess.getAllEntries(element);
                for (int j = 0; j < allFiles.size(); j++) {
                    final File aFile = allFiles.get(j);
                    final MFAttachedFile af = new MFAttachedFile( aFile );
                    filesTableModel.addRow( af );
                }
            }
        }
        positionDividers();
    }

    /**
     * Finally called to start composing a message. Uses alternate editor if configured.
     *
     * @param newBoard
     * @param newSubject
     * @param inReplyTo
     * @param newText
     * @param isReply
     * @param toIds
     *            a list of those identities the new message may be sent to
     * @param senderId
     * @param msg
     */
    private void composeMessage(
            final Board newBoard,
            final String newSubject,
            final String inReplyTo,
            String newText,
            final boolean isReply,
            final List<Identity> toIds,
            final LocalIdentity senderId,   // if given compose encrypted reply
            final FrostMessageObject msg) {

        repliedMessage = msg;

        if (isReply) {
            newText += "\n\n";
        }

        if (frostSettings.getBoolean("useAltEdit")) {
            // build our transfer object that the parser will provide us in its callback
            final TransferObject to = new TransferObject();
            to.newBoard = newBoard;
            to.newSubject = newSubject;
            to.inReplyTo = inReplyTo;
            to.newText = newText;
            to.isReply = isReply;
            to.toIds = toIds;
            to.senderId = senderId;
            // create a temporary editText that is show in alternate editor
            // the editor will return only new text to us
			final OffsetDateTime now = OffsetDateTime.now(DateFun.getTimeZone());
			final String date = DateFun.FORMAT_DATE_EXT.format(now) + " - " + DateFun.FORMAT_TIME_EXT.format(now);
            final String fromLine = "----- (sender) ----- " + date + " -----";
            final String editText = newText + fromLine + "\n\n";

            final AltEdit ae = new AltEdit(newSubject, editText, MainFrame.getInstance(), to, this);
            ae.start();
        } else {
            // invoke frame directly, no alternate editor
            composeMessageContinued(newBoard, newSubject, inReplyTo, newText, null, isReply, toIds, senderId);
        }
    }

    public void altEditCallback(final Object toObj, String newAltSubject, final String newAltText) {
        final TransferObject to = (TransferObject)toObj;
        if( newAltSubject == null ) {
            newAltSubject = to.newSubject; // use original subject
        }
        composeMessageContinued(
                to.newBoard,
                newAltSubject,
                to.inReplyTo,
                to.newText,
                newAltText,
                to.isReply,
                to.toIds,
                to.senderId);
    }

    /**
     * This method is either invoked by ComposeMessage OR by the callback of the AltEdit
     * class.
     *
     * @param newBoard
     * @param newSubject
     * @param inReplyTo
     * @param newText
     * @param altEditText
     * @param isReply
     * @param toIds
     *            a list of those identities the new message may be sent to or null if the
     *            message to be sent is not a crypted reply
     * @param senderId
     */
    private void composeMessageContinued(
        final Board newBoard,
        final String newSubject,
        final String inReplyTo,
        String newText,
        final String altEditText,
        final boolean isReply,
        final List<Identity> toIds,
        final LocalIdentity senderId)   // if given compose encrypted reply
    {
        headerArea.setEnabled(false);
        board = newBoard;
        repliedMsgId = inReplyTo; // maybe null

        String from;
        boolean isInitializedSigned;
        if( senderId != null ) {
            // encrypted reply!
            from = senderId.getUniqueName();
            isInitializedSigned = true;
        } else {
            // use remembered sender name, maybe per board
            String userName = Core.frostSettings.getString("userName."+board.getBoardFilename());
            if( (userName == null) || (userName.length() == 0) ) {
                userName = Core.frostSettings.getString(Settings.LAST_USED_FROMNAME);
            }
            if( Core.getIdentitiesManager().isMySelf(userName) ) {
                // isSigned
                from = userName;
                isInitializedSigned = true;
            } else if( userName.indexOf("@") > 0 ) {
                // invalid, only LocalIdentities are allowed to contain an @
                from = "Anonymous";
                isInitializedSigned = false;
            } else {
                from = userName;
                isInitializedSigned = false;
            }
        }
        oldSender = from;

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		initialize(newBoard, newSubject);

        sign.setEnabled(false);

        final ImageIcon signedIcon = MiscToolkit.loadImageIcon("/data/signed.gif");
        final ImageIcon unsignedIcon = MiscToolkit.loadImageIcon("/data/unsigned.gif");
        sign.setDisabledSelectedIcon(signedIcon);
        sign.setDisabledIcon(unsignedIcon);
        sign.setSelectedIcon(signedIcon);
        sign.setIcon(unsignedIcon);

        sign.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                updateSignToolTip();
            }
        });

        // maybe prepare to reply to an encrypted message
        if (toIds != null) {
            // set correct sender identity
            for(int x=0; x < getOwnIdentitiesComboBox().getItemCount(); x++) {
                final Object obj = getOwnIdentitiesComboBox().getItemAt(x);
                if( obj instanceof LocalIdentity ) {
                    final LocalIdentity li = (LocalIdentity)obj;
                    if( senderId.getUniqueName().equals(li.getUniqueName()) ) {
                        getOwnIdentitiesComboBox().setSelectedIndex(x);
                        break;
                    }
                }
            }
            getOwnIdentitiesComboBox().setEnabled(false);
            // set and lock controls (after we set the identity, the itemlistener would reset the controls!)
            sign.setSelected(true);
            encrypt.setSelected(true);
            // dont allow to disable signing/encryption
            encrypt.setEnabled(false);
            buddies.removeAllItems();
            for (int i = 0; i < toIds.size(); i++) {
                buddies.addItem(toIds.get(i));
            }
            buddies.setSelectedItem(toIds.get(0));
            if (toIds.size() > 1) {
                // We only let the user choose if there is more than one possible destination
                buddies.setEnabled(true);
            } else {
                buddies.setEnabled(false);
            }
        } else {
            if( isInitializedSigned ) {
                // set saved sender identity
                for(int x=0; x < getOwnIdentitiesComboBox().getItemCount(); x++) {
                    final Object obj = getOwnIdentitiesComboBox().getItemAt(x);
                    if( obj instanceof LocalIdentity ) {
                        final LocalIdentity li = (LocalIdentity)obj;
                        if( from.equals(li.getUniqueName()) ) {
                            getOwnIdentitiesComboBox().setSelectedIndex(x);
                            sign.setSelected(true);
                            getOwnIdentitiesComboBox().setEditable(false);
                            break;
                        }
                    }
                }
            } else {
                // initialized unsigned/anonymous
                getOwnIdentitiesComboBox().setSelectedIndex(0);
                getOwnIdentitiesComboBox().getEditor().setItem(from);
                sign.setSelected(false);
                getOwnIdentitiesComboBox().setEditable(true);
            }

            if( sign.isSelected() && (buddies.getItemCount() > 0) ) {
                encrypt.setEnabled(true);
            } else {
                encrypt.setEnabled(false);
            }
            encrypt.setSelected(false);
            buddies.setEnabled(false);
        }

        updateSignToolTip();

        // prepare message text
		final OffsetDateTime now = OffsetDateTime.now(DateFun.getTimeZone());
		final String date = DateFun.FORMAT_DATE_EXT.format(now) + " - " + DateFun.FORMAT_TIME_EXT.format(now);
        final String fromLine = "----- " + from + " ----- " + date + " -----";

        final int headerAreaStart = newText.length();// begin of non-modifiable area
        newText += fromLine + "\n\n";
        final int headerAreaEnd = newText.length() - 2; // end of non-modifiable area

        if( altEditText != null ) {
            newText += altEditText; // maybe append text entered in alternate editor
        }

        // later set cursor to this position in text
        final int caretPos = newText.length();

        // set sig if msg is marked as signed
        currentSignature = null;
        if( sign.isSelected()  ) {
            // maybe append a signature
            final LocalIdentity li = (LocalIdentity)getOwnIdentitiesComboBox().getSelectedItem();
            if( li.getSignature() != null ) {
                currentSignature = "\n-- \n" + li.getSignature();
                newText += currentSignature;
            }
        }

        messageTextArea.setText(newText);
        headerArea.setStartPos(headerAreaStart);
        headerArea.setEndPos(headerAreaEnd);
        headerArea.setEnabled(true);

//        textHighlighter.highlight(messageTextArea, headerAreaStart, headerAreaEnd-headerAreaStart, true);

        setVisible(true);

        // reset the splitpanes
        positionDividers();

        // Properly positions the caret (AKA cursor)
        messageTextArea.requestFocusInWindow();
        messageTextArea.getCaret().setDot(caretPos);
        messageTextArea.getCaret().setVisible(true);
    }

    public void composeNewMessage(final Board newBoard, final String newSubject, final String newText) {
        composeMessage(newBoard, newSubject, null, newText, false, null, null, null);
    }

    public void composeReply(
            final Board newBoard,
            final String newSubject,
            final String inReplyTo,
            final String newText,
            final FrostMessageObject msg) {
        composeMessage(newBoard, newSubject, inReplyTo, newText, true, null, null, msg);
    }

    /**
     * @param newBoard
     * @param newSubject
     * @param inReplyTo
     * @param newText
     * @param toIds
     *            a list of those identities the new message may be sent to
     * @param fromId
     *            the identity the new message will be sent from
     * @param msg
     */
    public void composeEncryptedReply(
            final Board newBoard,
            final String newSubject,
            final String inReplyTo,
            final String newText,
            final List<Identity> toIds,
            final LocalIdentity fromId,
            final FrostMessageObject msg) {
        composeMessage(newBoard, newSubject, inReplyTo, newText, true, toIds, fromId, msg);
    }

	@Override
	public void dispose() {
		if (initialized) {
			language.removeLanguageListener(this);
			initialized = false;
		}
		super.dispose();
	}

	private void initialize(Board targetBoard, String subject) {
        if (!initialized) {
            final ImageIcon frameIcon = MiscToolkit.loadImageIcon("/data/toolbar/mail-message-new.png");
            setIconImage(frameIcon.getImage());
            setResizable(true);

            boardsTableModel = new MFAttachedBoardsTableModel();
            boardsTable = new MFAttachedBoardsTable(boardsTableModel);
            boardsTableScrollPane = new JScrollPane(boardsTable);
            boardsTableScrollPane.setWheelScrollingEnabled(true);

            filesTableModel = new MFAttachedFilesTableModel();
            filesTable = new MFAttachedFilesTable(filesTableModel);
            filesTableScrollPane = new JScrollPane(filesTable);
            filesTableScrollPane.setWheelScrollingEnabled(true);

            // FIXME: option to show own identities in list, or to hide them
            final List<Identity> budList = Core.getIdentitiesManager().getAllGOODIdentities();
            Identity id = null;
            if( repliedMessage != null ) {
                id = repliedMessage.getFromIdentity();
            }
            if( (budList.size() > 0) || (id != null) ) {
                Collections.sort( budList, new BuddyComparator() );
                if( id != null ) {
                    if( id.isGOOD() == true ) {
                        budList.remove(id); // remove before put to top of list
                    }
                    // add id to top of list in case the user enables 'encrypt'
                    budList.add(0, id);
                }
				buddies = new JComboBox<>(new Vector<Identity>(budList));
                buddies.setSelectedItem(budList.get(0));
            } else {
				buddies = new JComboBox<>();
            }
            buddies.setMaximumSize(new Dimension(300, 25)); // dirty fix for overlength combobox on linux

            MiscToolkit.configureButton(Bsend, "MessageFrame.toolbar.tooltip.sendMessage", language);
            MiscToolkit.configureButton(Bcancel, "Common.cancel", language);
            MiscToolkit.configureButton(BattachFile, "MessageFrame.toolbar.tooltip.addFileAttachments", language);
            MiscToolkit.configureButton(BattachBoard, "MessageFrame.toolbar.tooltip.addBoardAttachments", language);
            MiscToolkit.configureButton(BchooseSmiley, "MessageFrame.toolbar.tooltip.chooseSmiley", language);
            BchooseSmiley.setFocusable(false);

            TFboard.setEditable(false);
            TFboard.setText(targetBoard.getName());

            new TextComponentClipboardMenu(TFboard, language);
            new TextComponentClipboardMenu((TextComboBoxEditor)getOwnIdentitiesComboBox().getEditor(), language);
            new TextComponentClipboardMenu(subjectTextField, language);
            subjectTextField.setText(subject);
            messageTextArea.setLineWrap(true);
            messageTextArea.setWrapStyleWord(true);

            sign.setOpaque(false);
            encrypt.setOpaque(false);

            //------------------------------------------------------------------------
            // Actionlistener
            //------------------------------------------------------------------------
			Bsend.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    send_actionPerformed(e);
                }
            });
			Bcancel.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    windowIsClosing();
                }
            });
			BattachFile.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    attachFile_actionPerformed(e);
                }
            });
			BattachBoard.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    attachBoards_actionPerformed(e);
                }
            });
			encrypt.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    encrypt_actionPerformed(e);
                }
            });
			BchooseSmiley.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    chooseSmiley_actionPerformed(e);
                }
            });

            //------------------------------------------------------------------------
            // Append objects
            //------------------------------------------------------------------------
            final JPanel panelMain = new JPanel(new BorderLayout()); // Main Panel

            final JPanel panelHeader = new JPanel(new BorderLayout()); // header (toolbar and textfields)
            final JPanel panelTextfields = new JPanel(new GridBagLayout());

            final JToolBar panelToolbar = new JToolBar(); // toolbar
            panelToolbar.setRollover(true);
            panelToolbar.setFloatable(false);

            final JScrollPane bodyScrollPane = new JScrollPane(messageTextArea); // Textscrollpane
            bodyScrollPane.setWheelScrollingEnabled(true);
            bodyScrollPane.setMinimumSize(new Dimension(100, 50));

            // FIXME: add a smiley chooser right beside the subject textfield!

            // text fields
            final GridBagConstraints constraints = new GridBagConstraints();
            final Insets insets = new Insets(0, 3, 0, 3);
            final Insets insets0 = new Insets(0, 0, 0, 0);
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.weighty = 0.0;
            constraints.weightx = 0.0;

            constraints.insets = insets;

            constraints.gridx = 0;
            constraints.gridy = 0;

            constraints.fill = GridBagConstraints.NONE;
            constraints.gridwidth = 1;
            constraints.insets = insets;
            constraints.weightx = 0.0;
            panelTextfields.add(Lboard, constraints);

            constraints.gridx = 1;

            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridwidth = 2;
            constraints.insets = insets0;
            constraints.weightx = 1.0;
            panelTextfields.add(TFboard, constraints);

            constraints.gridx = 0;
            constraints.gridy++;

            constraints.fill = GridBagConstraints.NONE;
            constraints.gridwidth = 1;
            constraints.insets = insets;
            constraints.weightx = 0.0;
            panelTextfields.add(Lfrom, constraints);

            constraints.gridx = 1;

            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridwidth = 2;
            constraints.insets = insets0;
            constraints.weightx = 1.0;
            panelTextfields.add(getOwnIdentitiesComboBox(), constraints);

            constraints.gridx = 0;
            constraints.gridy++;

            constraints.fill = GridBagConstraints.NONE;
            constraints.gridwidth = 1;
            constraints.insets = insets;
            constraints.weightx = 0.0;
            panelTextfields.add(Lsubject, constraints);

            constraints.gridx = 1;

            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridwidth = 1;
            constraints.insets = insets0;
            constraints.weightx = 1.0;
            panelTextfields.add(subjectTextField, constraints);

            constraints.gridx = 2;

            constraints.fill = GridBagConstraints.NONE;
            constraints.gridwidth = 1;
            constraints.insets = insets;
            constraints.weightx = 0.0;
            panelTextfields.add(BchooseSmiley, constraints);

            // toolbar
            panelToolbar.add(Bsend);
            panelToolbar.add(Bcancel);
            panelToolbar.addSeparator();
            panelToolbar.add(BattachFile);
            panelToolbar.add(BattachBoard);
            panelToolbar.addSeparator();
            panelToolbar.add(sign);
            panelToolbar.addSeparator();
            panelToolbar.add(encrypt);
            panelToolbar.add(buddies);
//            panelButtons.add(addAttachedFilesToUploadTable);

            final ScrollableBar panelButtonsScrollable = new ScrollableBar(panelToolbar);

            panelHeader.add(panelButtonsScrollable, BorderLayout.PAGE_START);
//            panelToolbar.add(panelButtons, BorderLayout.PAGE_START);
            panelHeader.add(panelTextfields, BorderLayout.CENTER);

            //Put everything together
            attachmentsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, filesTableScrollPane,
                    boardsTableScrollPane);
            attachmentsSplitPane.setResizeWeight(0.5);
            attachmentsSplitPane.setDividerSize(3);
            attachmentsSplitPane.setDividerLocation(0.5);

            messageSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, bodyScrollPane,
                    attachmentsSplitPane);
            messageSplitPane.setDividerSize(0);
            messageSplitPane.setDividerLocation(1.0);
            messageSplitPane.setResizeWeight(1.0);

            panelMain.add(panelHeader, BorderLayout.NORTH);
            panelMain.add(messageSplitPane, BorderLayout.CENTER);

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(panelMain, BorderLayout.CENTER);

			removeFilesAction = new RemoveFilesAction();
			attFilesPopupMenu = new AttFilesPopupMenu();
			attFilesPopupMenu.addPopupMenuListener(this);
			filesTable.setComponentPopupMenu(attFilesPopupMenu);

			removeBoardsAction = new RemoveBoardsAction();
			attBoardsPopupMenu = new AttBoardsPopupMenu();
			attBoardsPopupMenu.addPopupMenuListener(this);
			boardsTable.setComponentPopupMenu(attBoardsPopupMenu);

			cutAction = new CutAction();
			copyAction = new CopyAction();
			pasteAction = new PasteAction();
			cancelAction = new CancelAction();
			messageBodyPopupMenu = new MessageBodyPopupMenu();
			messageBodyPopupMenu.addPopupMenuListener(this);
			messageTextArea.setComponentPopupMenu(messageBodyPopupMenu);

            pack();

            // window is now packed to needed size. Check if packed width is smaller than
            // 75% of the parent frame and use the larger size.
            // pack is needed to ensure that all dialog elements are shown (was problem on linux).
            int width = getWidth();
            if( width < (int)(parentWindow.getWidth() * 0.75) ) {
                width = (int)(parentWindow.getWidth() * 0.75);
            }

            setSize( width, (int)(parentWindow.getHeight() * 0.75) ); // always set height to 75% of parent
            setLocationRelativeTo(parentWindow);

			language.addLanguageListener(this);
			languageChanged(null);

            initialized = true;
        }
    }

    private void positionDividers() {
        final int attachedFiles = filesTableModel.getRowCount();
        final int attachedBoards = boardsTableModel.getRowCount();
        if ((attachedFiles == 0) && (attachedBoards == 0)) {
            // Neither files nor boards
            messageSplitPane.setBottomComponent(null);
            messageSplitPane.setDividerSize(0);
            return;
        }
        messageSplitPane.setDividerSize(3);
        messageSplitPane.setDividerLocation(0.75);
        if ((attachedFiles != 0) && (attachedBoards == 0)) {
            //Only files
            messageSplitPane.setBottomComponent(filesTableScrollPane);
            return;
        }
        if ((attachedFiles == 0) && (attachedBoards != 0)) {
            //Only boards
            messageSplitPane.setBottomComponent(boardsTableScrollPane);
            return;
        }
        if ((attachedFiles != 0) && (attachedBoards != 0)) {
            //Both files and boards
            messageSplitPane.setBottomComponent(attachmentsSplitPane);
            attachmentsSplitPane.setTopComponent(filesTableScrollPane);
            attachmentsSplitPane.setBottomComponent(boardsTableScrollPane);
        }
    }

    private void updateSignToolTip() {
        final boolean isSelected = sign.isSelected();
        if( isSelected ) {
            sign.setToolTipText(language.getString("MessagePane.toolbar.tooltip.isSigned"));
        } else {
            sign.setToolTipText(language.getString("MessagePane.toolbar.tooltip.isUnsigned"));
        }
    }

    protected void removeSelectedItemsFromTable( final JTable tbl ) {
		@SuppressWarnings("unchecked")
		final SortedTableModel<? extends TableMember<?>> m = (SortedTableModel<? extends TableMember<?>>) tbl
				.getModel();

        final int[] sel = tbl.getSelectedRows();
        for(int x=sel.length-1; x>=0; x--)
        {
            m.removeRow(sel[x]);
        }
        positionDividers();
    }

    private void chooseSmiley_actionPerformed(final ActionEvent e) {
        final SmileyChooserDialog dlg = new SmileyChooserDialog(this);
        final int x = this.getX() + BchooseSmiley.getX();
        final int y = this.getY() + BchooseSmiley.getY();
        String chosedSmileyText = dlg.startDialog(x, y);
        if( (chosedSmileyText != null) && (chosedSmileyText.length() > 0) ) {
            chosedSmileyText += " ";
            // paste into document
            try {
                final Caret caret = messageTextArea.getCaret();
                final int p0 = Math.min(caret.getDot(), caret.getMark());
                final int p1 = Math.max(caret.getDot(), caret.getMark());

                final Document document = messageTextArea.getDocument();

                // FIXME: maybe check for a blank before insert of smiley text???
                if (document instanceof PlainDocument) {
                    ((PlainDocument) document).replace(p0, p1 - p0, chosedSmileyText, null);
                } else {
                    if (p0 != p1) {
                        document.remove(p0, p1 - p0);
                    }
                    document.insertString(p0, chosedSmileyText, null);
                }
            } catch (final Throwable ble) {
                logger.error("Problem while pasting text.", ble);
            }
        }
        // finally set focus back to message window
        messageTextArea.requestFocusInWindow();
    }

    private void send_actionPerformed(final ActionEvent e) {

        LocalIdentity senderId = null;
        String from;
        if( getOwnIdentitiesComboBox().getSelectedItem() instanceof LocalIdentity ) {
            senderId = (LocalIdentity)getOwnIdentitiesComboBox().getSelectedItem();
            from = senderId.getUniqueName();
        } else {
            from = getOwnIdentitiesComboBox().getEditor().getItem().toString();
        }

        final String subject = subjectTextField.getText().trim();
        subjectTextField.setText(subject); // if a pbl occurs show the subject we checked
        final String text = messageTextArea.getText().trim();

        if( subject.equals("No subject") ) {
            final int n = JOptionPane.showConfirmDialog( this,
                                language.getString("MessageFrame.defaultSubjectWarning.text"),
                                language.getString("MessageFrame.defaultSubjectWarning.title"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
            if( n == JOptionPane.YES_OPTION ) {
                return;
            }
        }

        if( subject.length() == 0) {
            JOptionPane.showMessageDialog( this,
                                language.getString("MessageFrame.noSubjectError.text"),
                                language.getString("MessageFrame.noSubjectError.title"),
                                JOptionPane.ERROR);
            return;
        }
        if( from.length() == 0) {
            JOptionPane.showMessageDialog( this,
                                language.getString("MessageFrame.noSenderError.text"),
                                language.getString("MessageFrame.noSenderError.title"),
                                JOptionPane.ERROR);
            return;
        }
        final int maxTextLength = (60*1024);
        final int msgSize = text.length() + subject.length() + from.length() + ((repliedMsgId!=null)?repliedMsgId.length():0);
        if( msgSize > maxTextLength ) {
            JOptionPane.showMessageDialog( this,
                    language.formatMessage("MessageFrame.textTooLargeError.text",
                            Integer.toString(text.length()),
                            Integer.toString(maxTextLength)),
                    language.getString("MessageFrame.textTooLargeError.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        final int idLinePos = headerArea.getStartPos();
        final int idLineLen = headerArea.getEndPos() - headerArea.getStartPos();
        if( text.length() == headerArea.getEndPos() ) {
            JOptionPane.showMessageDialog( this,
                    language.getString("MessageFrame.noContentError.text"),
                    language.getString("MessageFrame.noContentError.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // for convenience set last used user
        if( from.indexOf("@") < 0 ) {
            // only save anonymous usernames
            frostSettings.setValue(Settings.LAST_USED_FROMNAME, from);
        }
        frostSettings.setValue("userName."+board.getBoardFilename(), from);

        final FrostUnsentMessageObject newMessage = new FrostUnsentMessageObject();
        newMessage.setMessageId(Mixed.createUniqueId()); // new message, create a new unique msg id
        newMessage.setInReplyTo(repliedMsgId);
        newMessage.setBoard(board);
        newMessage.setFromName(from);
        newMessage.setSubject(subject);
        newMessage.setContent(text);
        newMessage.setIdLinePos(idLinePos);
        newMessage.setIdLineLen(idLineLen);

        // MessageUploadThread will set date + time !

        // attach all files and boards the user chosed
        if( filesTableModel.getRowCount() > 0 ) {
            for(int x=0; x < filesTableModel.getRowCount(); x++) {
                final MFAttachedFile af = filesTableModel.getRow(x);
                final File aChosedFile = af.getFile();
                final FileAttachment fa = new FileAttachment(aChosedFile);
                newMessage.addAttachment(fa);
            }
            newMessage.setHasFileAttachments(true);
        }
        if( boardsTableModel.getRowCount() > 0 ) {
            for(int x=0; x < boardsTableModel.getRowCount(); x++) {
                final MFAttachedBoard ab = boardsTableModel.getRow(x);
                final Board aChosedBoard = ab.getBoardObject();
                final BoardAttachment ba = new BoardAttachment(aChosedBoard);
                newMessage.addAttachment(ba);
            }
            newMessage.setHasBoardAttachments(true);
        }

        Identity recipient = null;
        if( encrypt.isSelected() ) {
            recipient = (Identity)buddies.getSelectedItem();
            if( recipient == null ) {
                JOptionPane.showMessageDialog( this,
                        language.getString("MessageFrame.encryptErrorNoRecipient.body"),
                        language.getString("MessageFrame.encryptErrorNoRecipient.title"),
                        JOptionPane.ERROR);
                return;
            }
            newMessage.setRecipientName(recipient.getUniqueName());
        }

        UnsentMessagesManager.addNewUnsentMessage(newMessage);

//        // zip the xml file and check for maximum size
//        File tmpFile = FileAccess.createTempFile("msgframe_", "_tmp");
//        tmpFile.deleteOnExit();
//        if( mo.saveToFile(tmpFile) == true ) {
//            File zipFile = new File(tmpFile.getPath() + ".zipped");
//            zipFile.delete(); // just in case it already exists
//            zipFile.deleteOnExit(); // so that it is deleted when Frost exits
//            FileAccess.writeZipFile(FileAccess.readByteArray(tmpFile), "entry", zipFile);
//            long zipLen = zipFile.length();
//            tmpFile.delete();
//            zipFile.delete();
//            if( zipLen > 30000 ) { // 30000 because data+metadata must be smaller than 32k
//                JOptionPane.showMessageDialog( this,
//                        "The zipped message is too large ("+zipLen+" bytes, "+30000+" allowed)! Remove some text.",
//                        "Message text too large!",
//                        JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//        } else {
//            JOptionPane.showMessageDialog( this,
//                    "Error verifying the resulting message size.",
//                    "Error",
//                    JOptionPane.ERROR_MESSAGE);
//            return;
//        }

        // TODO: if user deletes the unsent msg then the replied state keeps (see below)
        //  We would have to set the replied state after the msg was successfully sent, because
        //  we can't remove the state later, maybe the msg was replied twice and we would remove
        //  the replied state from first reply...

        // set isReplied to replied message
        if( repliedMessage != null ) {
            if( repliedMessage.isReplied() == false ) {
                repliedMessage.setReplied(true);
                final FrostMessageObject saveMsg = repliedMessage;
                final Thread saver = new Thread() {
                    @Override
                    public void run() {
                        // save the changed isreplied state into the database
                        MessageStorage.inst().updateMessage(saveMsg);
                    }
                };
                saver.start();
            }
        }

        setVisible(false);
        dispose();
    }

    private void senderChanged(final LocalIdentity selectedId) {

        boolean isSigned;
        if( selectedId == null ) {
            isSigned = false;
        } else {
            isSigned = true;
        }

        sign.setSelected(isSigned);

        if (isSigned) {
            if( buddies.getItemCount() > 0 ) {
                encrypt.setEnabled(true);
                if( encrypt.isSelected() ) {
                    buddies.setEnabled(true);
                } else {
                    buddies.setEnabled(false);
                }
            }

            removeSignatureFromText(currentSignature); // remove signature if existing
            currentSignature = addSignatureToText(selectedId.getSignature()); // add new signature if not existing
        } else {
            encrypt.setSelected(false);
            encrypt.setEnabled(false);
            buddies.setEnabled(false);
            removeSignatureFromText(currentSignature); // remove signature if existing
            currentSignature = null;
        }
    }

    private String addSignatureToText(final String sig) {
        if( sig == null ) {
            return null;
        }
        final String newSig = "\n-- \n" + sig;
        if (!messageTextArea.getText().endsWith(newSig)) {
            try {
                messageTextArea.getDocument().insertString(messageTextArea.getText().length(), newSig, null);
            } catch (final BadLocationException e1) {
                logger.error("Error while updating the signature ", e1);
            }
        }
        return newSig;
    }

    private void removeSignatureFromText(final String sig) {
        if( sig == null ) {
            return;
        }
        if (messageTextArea.getText().endsWith(sig)) {
            try {
                messageTextArea.getDocument().remove(messageTextArea.getText().length()-sig.length(), sig.length());
            } catch (final BadLocationException e1) {
                logger.error("Error while updating the signature ", e1);
            }
        }
    }

    private void encrypt_actionPerformed(final ActionEvent e) {
        if( encrypt.isSelected() ) {
            buddies.setEnabled(true);
        } else {
            buddies.setEnabled(false);
        }
    }

    protected void updateHeaderArea(final String sender) {
        if( !headerArea.isEnabled() ) {
            return; // ignore updates
        }
        if( (sender == null) || (oldSender == null) || oldSender.equals(sender) ) {
            return;
        }
        try {
            // TODO: add grey background! highlighter mit headerArea um pos zu finden
            headerArea.setEnabled(false);
            messageTextArea.getDocument().remove(headerArea.getStartPos() + 6, oldSender.length());
            messageTextArea.getDocument().insertString(headerArea.getStartPos() + 6, sender, null);
            oldSender = sender;
            headerArea.setEnabled(true);
//            textHighlighter.highlight(messageTextArea, headerArea.getStartPos(), headerArea.getEndPos()-headerArea.getStartPos(), true);
        } catch (final BadLocationException exception) {
            logger.error("Error while updating the message header", exception);
        }
        logger.debug("{} ; {}: '{}'", headerArea.getStartPos(), headerArea.getEndPos(), messageTextArea.getText().substring(headerArea.getStartPos(), headerArea.getEndPos()));

// DBG: 0 ; 77: '----- blubb2@xpDZ5ZfXK9wYiHB_hkVGRCwJl54 ----- 2006.10.13 - 18:20:12GMT -----'
// DBG: 39 ; 119: '----- wegdami t@plewLcBTHKmPwpWakJNpUdvWSR8 ----- 2006.10.13 - 18:20:12GMT -----'
    }

	private class TextComboBoxEditor extends JTextField implements ComboBoxEditor {

		private static final long serialVersionUID = 1L;

		private boolean isSigned;

        public TextComboBoxEditor() {
            super();
        }

        public Component getEditorComponent() {
            return this;
        }

        public void setItem(final Object arg0) {
            if( arg0 instanceof LocalIdentity ) {
                isSigned = true;
            } else {
                isSigned = false;
            }
            setText(arg0.toString());
        }

        public Object getItem() {
            return getText();
        }

        public boolean isSigned() {
            return isSigned;
        }
    }

	private JComboBox<Identity> getOwnIdentitiesComboBox() {
        if( ownIdentitiesComboBox == null ) {
			ownIdentitiesComboBox = new JComboBox<>();
			ownIdentitiesComboBox.addItem(Identity.createIdentityFromExactStrings("Anonymous", ""));
            // sort own unique names
            final TreeMap<String,LocalIdentity> sortedIds = new TreeMap<String,LocalIdentity>();
			for (final LocalIdentity li : Core.getIdentitiesManager().getLocalIdentities()) {
                sortedIds.put(li.getUniqueName(), li);
            }
			for (final LocalIdentity element : sortedIds.values()) {
                ownIdentitiesComboBox.addItem(element);
            }

            final TextComboBoxEditor editor = new TextComboBoxEditor();

            editor.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(final DocumentEvent e) {
                    updateHeaderArea2();
                }
                public void insertUpdate(final DocumentEvent e) {
                    updateHeaderArea2();
                }
                public void removeUpdate(final DocumentEvent e) {
                    updateHeaderArea2();
                }
                private void updateHeaderArea2() {
                    final String sender = getOwnIdentitiesComboBox().getEditor().getItem().toString();
                    updateHeaderArea(sender);
                }
            });

            final AbstractDocument doc = (AbstractDocument) editor.getDocument();
            doc.setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(final DocumentFilter.FilterBypass fb, final int offset, String string,
                        final AttributeSet attr) throws BadLocationException
                {
                    if (((TextComboBoxEditor)getOwnIdentitiesComboBox().getEditor()).isSigned() == false ) {
                        string = string.replaceAll("@","");
                    }
                    super.insertString(fb, offset, string, attr);
                }
                @Override
                public void replace(final DocumentFilter.FilterBypass fb, final int offset, final int length, String string,
                        final AttributeSet attrs) throws BadLocationException
                {
                    if (((TextComboBoxEditor)getOwnIdentitiesComboBox().getEditor()).isSigned() == false ) {
                        string = string.replaceAll("@","");
                    }
                    super.replace(fb, offset, length, string, attrs);
                }
            });

            ownIdentitiesComboBox.setEditor(editor);

            ownIdentitiesComboBox.setEditable(true);

//            ownIdentitiesComboBox.getEditor().selectAll();
			ownIdentitiesComboBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
                    if( e.getStateChange() == ItemEvent.DESELECTED ) {
                        return;
                    }
                    LocalIdentity selectedId = null;
                    if( ownIdentitiesComboBox.getSelectedIndex() == 0 ) {
                        ownIdentitiesComboBox.setEditable(true); // original anonymous
//                        ownIdentitiesComboBox.getEditor().selectAll();
                    } else if( ownIdentitiesComboBox.getSelectedIndex() < 0 ) {
                        ownIdentitiesComboBox.setEditable(true); // own value, anonymous
//                        ownIdentitiesComboBox.getEditor().selectAll();
                    } else {
                        ownIdentitiesComboBox.setEditable(false);
                        selectedId = (LocalIdentity) ownIdentitiesComboBox.getSelectedItem();
                    }
                    final String sender = getOwnIdentitiesComboBox().getSelectedItem().toString();
                    updateHeaderArea(sender);
                    senderChanged(selectedId);
                }
            });
        }
        return ownIdentitiesComboBox;
    }

	private class BuddyComparator implements Comparator<Identity> {
        public int compare(final Identity id1, final Identity id2) {
            final String s1 = id1.getUniqueName();
            final String s2 = id2.getUniqueName();
            return s1.toLowerCase().compareTo( s2.toLowerCase() );
        }
    }

	private class MFAttachedBoard extends BaseTableMember<MFAttachedBoard> {

		private Board aBoard;

        public MFAttachedBoard(final Board ab) {
            aBoard = ab;
        }

        public Board getBoardObject() {
            return aBoard;
        }

		public Comparable<?> getValueAt(final int column) {
            switch (column) {
                case 0 : return aBoard.getName();
                case 1 : return (aBoard.getPublicKey() == null) ? "N/A" : aBoard.getPublicKey();
                case 2 : return (aBoard.getPrivateKey() == null) ? "N/A" : aBoard.getPrivateKey();
                case 3 : return (aBoard.getDescription() == null) ? "N/A" : aBoard.getDescription();
            }
            return "*ERR*";
        }
    }

	private class MFAttachedBoardsTable extends SortedTable<MFAttachedBoard> {

		private static final long serialVersionUID = 1L;

        public MFAttachedBoardsTable(final MFAttachedBoardsTableModel m) {
            super(m);
            // set column sizes
            final int[] widths = {250, 80, 80};
            for (int i = 0; i < widths.length; i++) {
                getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }
            // default for sort: sort by name ascending ?
            sortedColumnIndex = 0;
            sortedColumnAscending = true;
            resortTable();
        }
    }

	private class MFAttachedBoardsTableModel extends SortedTableModel<MFAttachedBoard> {

		private static final long serialVersionUID = 1L;

        protected final Class<?> columnClasses[] = {
            String.class,
            String.class,
            String.class,
            String.class
        };

        protected final String columnNames[] = {
            language.getString("MessageFrame.boardAttachmentTable.boardname"),
            language.getString("MessageFrame.boardAttachmentTable.publicKey"),
            language.getString("MessageFrame.boardAttachmentTable.privateKey"),
            language.getString("MessageFrame.boardAttachmentTable.description")
        };

        public MFAttachedBoardsTableModel() {
            super();
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            if( (column >= 0) && (column < columnClasses.length) ) {
                return columnClasses[column];
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(final int column) {
            if( (column >= 0) && (column < columnNames.length) ) {
                return columnNames[column];
            }
            return null;
        }

        @Override
        public boolean isCellEditable(final int row, final int col) {
            return false;
        }

        @Override
        public void setValueAt(final Object aValue, final int row, final int column) {}
    }

	private class MFAttachedFile extends BaseTableMember<MFAttachedFile> {

		private File aFile;

        public MFAttachedFile(final File af) {
            aFile = af;
        }

        public File getFile() {
            return aFile;
        }

		public Comparable<?> getValueAt(final int column)  {
            switch(column) {
                case 0: return aFile.getName();
                case 1: return Long.toString(aFile.length());
            }
            throw new IndexOutOfBoundsException("Nu such column: " + Integer.toString(column));
        }
    }

	private class MFAttachedFilesTable extends SortedTable<MFAttachedFile> {

		private static final long serialVersionUID = 1L;

        public MFAttachedFilesTable(final MFAttachedFilesTableModel m) {
            super(m);

            // set column sizes
            final int[] widths = {250, 80};
            for (int i = 0; i < widths.length; i++) {
                getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }

            // default for sort: sort by name ascending ?
            sortedColumnIndex = 0;
            sortedColumnAscending = true;
            resortTable();
        }
    }

	private class MFAttachedFilesTableModel extends SortedTableModel<MFAttachedFile> {

		private static final long serialVersionUID = 1L;

        protected final Class<?> columnClasses[] = {
            String.class,
            String.class
        };

        protected final String columnNames[] = {
            language.getString("MessageFrame.fileAttachmentTable.filename"),
            language.getString("MessageFrame.fileAttachmentTable.size")
        };

        public MFAttachedFilesTableModel() {
            super();
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            if( (column >= 0) && (column < columnClasses.length) ) {
                return columnClasses[column];
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(final int column) {
            if( (column >= 0) && (column < columnNames.length) ) {
                return columnNames[column];
            }
            return null;
        }

        @Override
        public boolean isCellEditable(final int row, final int col) {
            return false;
        }
    }

    private class TransferObject {
        public Board newBoard;
        public String newSubject;
        public String inReplyTo;
        public String newText;
        public boolean isReply;
        public List<Identity> toIds = null;;
        public LocalIdentity senderId = null;
    }

    public static synchronized int getOpenInstanceCount() {
        return openInstanceCount;
    }

    private static synchronized void incOpenInstanceCount() {
        openInstanceCount++;
    }

    private static synchronized void decOpenInstanceCount() {
        if( openInstanceCount > 0 ) { // paranoia
            openInstanceCount--;
        }
    }

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		Boolean canCopyOrCut = messageTextArea.getSelectedText() != null;

		removeFilesAction.setEnabled(filesTable.getSelectedRowCount() > 0);
		removeBoardsAction.setEnabled(boardsTable.getSelectedRowCount() > 0);
		cutAction.setEnabled(canCopyOrCut);
		copyAction.setEnabled(canCopyOrCut);
		pasteAction.setEnabled(ClipboardUtil.hasText());
	}

	@Override
	public void languageChanged(LanguageEvent event) {
		setTitle(language.getString("MessageFrame.createMessage.title"));

		Bsend.setToolTipText(language.getString("MessageFrame.toolbar.tooltip.sendMessage"));
		Bcancel.setToolTipText(language.getString("Common.cancel"));
		BattachFile.setToolTipText(language.getString("MessageFrame.toolbar.tooltip.addFileAttachments"));
		BattachBoard.setToolTipText(language.getString("MessageFrame.toolbar.tooltip.addBoardAttachments"));

		encrypt.setText(language.getString("MessageFrame.toolbar.encryptFor"));

		Lboard.setText(language.getString("MessageFrame.board") + ": ");
		Lfrom.setText(language.getString("MessageFrame.from") + ": ");
		Lsubject.setText(language.getString("MessageFrame.subject") + ": ");

		removeFilesAction.setText(language.getString("MessageFrame.attachmentTables.popupmenu.remove"));
		removeBoardsAction.setText(language.getString("MessageFrame.attachmentTables.popupmenu.remove"));
		cutAction.setText(language.getString("Common.cut"));
		copyAction.setText(language.getString("Common.copy"));
		pasteAction.setText(language.getString("Common.paste"));
		cancelAction.setText(language.getString("Common.cancel"));

		updateSignToolTip();
	}

	private class RemoveFilesAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			removeSelectedItemsFromTable(filesTable);
		}
	}

	private class AttFilesPopupMenu extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		public AttFilesPopupMenu() {
			add(removeFilesAction);
		}
	}

	private class RemoveBoardsAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			removeSelectedItemsFromTable(boardsTable);
		}
	}

	private class AttBoardsPopupMenu extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		public AttBoardsPopupMenu() {
			add(removeBoardsAction);
		}
	}

	private class CutAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			ClipboardUtil.copyText(messageTextArea.getSelectedText());

			int start = messageTextArea.getSelectionStart();
			int end = messageTextArea.getSelectionEnd();
			try {
				messageTextArea.getDocument().remove(start, end - start);
			} catch (BadLocationException ex) {
				logger.error("Problem while cutting text.", ex);
			}
		}
	}

	private class CopyAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			ClipboardUtil.copyText(messageTextArea.getSelectedText());
		}
	}

	private class PasteAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			String text = ClipboardUtil.pasteText();

			Caret caret = messageTextArea.getCaret();
			int p0 = Math.min(caret.getDot(), caret.getMark());
			int p1 = Math.max(caret.getDot(), caret.getMark());

			Document document = messageTextArea.getDocument();
			try {
				if (document instanceof PlainDocument) {
					((PlainDocument) document).replace(p0, p1 - p0, text, null);
				} else {
					if (p0 != p1) {
						document.remove(p0, p1 - p0);
					}
					document.insertString(p0, text, null);
				}
			} catch (BadLocationException ex) {
				logger.error("Problem while pasting text.", e);
			}
		}
	}

	private class MessageBodyPopupMenu extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		public MessageBodyPopupMenu() {
			add(cutAction);
			add(copyAction);
			add(pasteAction);
			addSeparator();
			add(cancelAction);
		}
	}
}
