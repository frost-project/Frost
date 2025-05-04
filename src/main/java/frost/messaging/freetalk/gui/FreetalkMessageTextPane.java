/*
  MessageTextPane.java / Frost
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
package frost.messaging.freetalk.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.download.DownloadModel;
import frost.fileTransfer.download.FrostDownloadItem;
import frost.gui.SearchMessagesConfig;
import frost.messaging.freetalk.FreetalkFileAttachment;
import frost.messaging.freetalk.FreetalkMessage;
import frost.util.ClipboardUtil;
import frost.util.FileAccess;
import frost.util.gui.SmileyCache;
import frost.util.gui.TextHighlighter;
import frost.util.gui.search.FindAction;
import frost.util.gui.search.TextComponentFindAction;
import frost.util.gui.textpane.AntialiasedTextPane;
import frost.util.gui.textpane.MessageDecoder;
import frost.util.gui.textpane.MouseHyperlinkEvent;
import frost.util.gui.textpane.TextPane;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

public class FreetalkMessageTextPane extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(FreetalkMessageTextPane.class);

	private transient Language language = Language.getInstance();

    private AntialiasedTextPane messageTextArea = null;
    private JSplitPane messageSplitPane = null;

    private FreetalkAttachedFilesTableModel attachedFilesModel;
    private JTable filesTable = null;
    private JScrollPane filesTableScrollPane;
    private JScrollPane messageBodyScrollPane;

    private PopupMenuAttachmentFile popupMenuAttachmentTable = null;
    private PopupMenuHyperLink popupMenuHyperLink = null;
    private PopupMenuTofText popupMenuTofText = null;

    private FreetalkMessage selectedMessage;

    private final MainFrame mainFrame = MainFrame.getInstance();

    private final Component parentFrame;

	private transient PropertyChangeListener propertyChangeListener;

	private transient SearchMessagesConfig searchMessagesConfig;
	private transient TextHighlighter textHighlighter;
    private static Color highlightColor = new Color(0x20, 0xFF, 0x20); // light green
    private static Color idLineHighlightColor = Color.LIGHT_GRAY;
	private transient TextHighlighter idLineTextHighlighter = new TextHighlighter(idLineHighlightColor);

    public FreetalkMessageTextPane(final Component parentFrame) {
        this(parentFrame, null);
    }

    public FreetalkMessageTextPane(final Component parentFrame, final SearchMessagesConfig smc) {
        super();
        this.parentFrame = parentFrame;
        this.searchMessagesConfig = smc;
        initialize();
    }

    /**
     * Called if there are no boards in the board list.
     */
    public void update_noBoardsFound() {
        messageSplitPane.setBottomComponent(null);
        messageSplitPane.setDividerSize(0);
        setMessageText(language.getString("MessagePane.defaultText.welcomeMessage"));
    }

    /**
     * Called if a board is selected, but no message in message table.
     */
    public void update_boardSelected() {
        messageSplitPane.setBottomComponent(null);
        messageSplitPane.setDividerSize(0);
        setMessageText(language.getString("MessagePane.defaultText.noMessageSelected"));
    }

    /**
     * Called if a folder is selected.
     */
    public void update_folderSelected() {
        messageSplitPane.setBottomComponent(null);
        messageSplitPane.setDividerSize(0);
        setMessageText(language.getString("MessagePane.defaultText.noBoardSelected"));
    }

    private void setMessageText(final String txt) {
        idLineTextHighlighter.removeHighlights(messageTextArea);
        SmileyCache.clearCachedSmileys();
        messageTextArea.setText(txt);
    }

    public TextPane getTextArea() {
        return messageTextArea;
    }

    /**
     * Find the offset in text where the caret must be positioned to
     * show the line at 'offset' on top of visible text.
     * Scans through the visible text and counts 'linesDown' visible lines (maybe wrapped!).
     */
    private int calculateCaretPosition(final JTextComponent c, int offset, int linesDown) {
        final int len = c.getDocument().getLength();
        try {
            while (offset < len) {
                int end = Utilities.getRowEnd(c, offset);
                if (end < 0) {
                    break;
                }

                // Include the last character on the line
                end = Math.min(end+1, len);

                offset = end;
                linesDown--;
                if( linesDown == 0 ) {
                    return offset;
                }
            }
        } catch (final BadLocationException e) {
        }
        return len;
    }

    /**
     * Called if a message is selected.
     */
    public void update_messageSelected(final FreetalkMessage msg) {

        selectedMessage = msg;

        if( textHighlighter != null ) {
            textHighlighter.removeHighlights(messageTextArea);
        }

        final List<FreetalkFileAttachment> fileAttachments = selectedMessage.getFileAttachments();
        attachedFilesModel.setData(fileAttachments);

        positionDividers(fileAttachments);

        setMessageText(selectedMessage.getContent());

        messageBodyScrollPane.getVerticalScrollBar().setValueIsAdjusting(true);
        messageBodyScrollPane.getVerticalScrollBar().setValue(0);

        // for search messages don't scroll down to begin of text
//        if( searchMessagesConfig == null ) {
//            int pos = selectedMessage.getIdLinePos();
//            final int len = selectedMessage.getIdLineLen();
//            if( pos > -1 && len > 10 ) {
//                // highlite id line if there are valid infos abpout the idline in message
//                idLineTextHighlighter.highlight(messageTextArea, pos, len, true);
//            } else {
//                // fallback
//                pos = selectedMessage.getContent().lastIndexOf("----- "+selectedMessage.getFromName()+" ----- ");
//            }
//
//            if( pos >= 0 ) {
//                // scroll to begin of reply
//                final int h = messageTextArea.getFontMetrics(messageTextArea.getFont()).getHeight();
//                final int s = textViewHeight; // messageBodyScrollPane.getViewport().getHeight();
//                final int v = s/h - 1; // how many lines are visible?
//
//                pos = calculateCaretPosition(messageTextArea, pos, v);
//
//                messageTextArea.getCaret().setDot(pos);
//            } else {
//                // scroll to end of message
//                pos = selectedMessage.getContent().length();
//                messageTextArea.getCaret().setDot(pos);
//            }
//        }

        messageBodyScrollPane.getVerticalScrollBar().setValueIsAdjusting(false);

        if( searchMessagesConfig != null &&
            searchMessagesConfig.content != null &&
            searchMessagesConfig.content.size() > 0 )
        {
            // highlight words in content that the user searched for
            if( textHighlighter == null ) {
                textHighlighter = new TextHighlighter(highlightColor, true);
            }
            textHighlighter.highlight(messageTextArea, searchMessagesConfig.content, false);
        }
    }

    private void initialize() {

        setLayout(new BorderLayout());

        final MessageDecoder decoder = new MessageDecoder();
        decoder.setSmileyDecode(Core.frostSettings.getBoolValue(Settings.FREETALK_SHOW_SMILEYS));
        decoder.setFreenetKeysDecode(Core.frostSettings.getBoolValue(Settings.FREETALK_SHOW_KEYS_AS_HYPERLINKS));
        messageTextArea = new AntialiasedTextPane(decoder);
        messageTextArea.setEditable(false);
        messageTextArea.setDoubleBuffered(true);
        messageTextArea.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
//        messageTextArea.setLineWrap(true);
//        messageTextArea.setWrapStyleWord(true);

        messageTextArea.setAntiAliasEnabled(Core.frostSettings.getBoolValue(Settings.MESSAGE_BODY_ANTIALIAS));

        messageBodyScrollPane = new JScrollPane(messageTextArea);
        messageBodyScrollPane.setWheelScrollingEnabled(true);

        // build attached files scroll pane
        attachedFilesModel = new FreetalkAttachedFilesTableModel();
        filesTable = new JTable(attachedFilesModel);
        attachedFilesModel.configureTable(filesTable);
        filesTableScrollPane = new JScrollPane(filesTable);
        filesTableScrollPane.setWheelScrollingEnabled(true);

        fontChanged();

        //Put everything together
        messageSplitPane =
            new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                messageBodyScrollPane,
                null);
        messageSplitPane.setDividerSize(0);
        messageSplitPane.setDividerLocation(1.0);
        messageSplitPane.setResizeWeight(1.0);

        add(messageSplitPane, BorderLayout.CENTER);

        messageTextArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showTofTextAreaPopupMenu(e);
                }
            }
            @Override
            public void mouseReleased(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showTofTextAreaPopupMenu(e);
                }
            }
        });
        messageTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(final KeyEvent e) {
                if( e == null ) {
                    return;
                } else if(e.getKeyChar() == KeyEvent.VK_DELETE && parentFrame == mainFrame ) {
                    mainFrame.getMessagePanel().deleteSelectedMessage();
                }
            }
        });

        final FindAction findAction = new TextComponentFindAction();
        findAction.install(messageTextArea);

        messageTextArea.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(final HyperlinkEvent evt) {
                if( !(evt instanceof MouseHyperlinkEvent) ) {
                    logger.error("hyperlinkevent is wrong object!");
                    return;
                }
                final MouseHyperlinkEvent e = (MouseHyperlinkEvent) evt;
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    // user clicked on 'clickedKey', List 'allKeys' contains all keys
                    final List<String> allKeys = ((MessageDecoder)messageTextArea.getDecoder()).getHyperlinkedKeys();
                    final String clickedKey = e.getDescription();
                    // show menu to download this/all keys and copy this/all to clipboard
                    showHyperLinkPopupMenu(
                            e,
                            clickedKey,
                            allKeys,
                            e.getMouseEvent().getX(),
                            e.getMouseEvent().getY());
                }
            }
        });
        filesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showAttachedFilesPopupMenu(e);
                }
            }
            @Override
            public void mouseReleased(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showAttachedFilesPopupMenu(e);
                }
            }
        });

        propertyChangeListener = new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(Settings.MESSAGE_BODY_ANTIALIAS)) {
                    messageTextArea.setAntiAliasEnabled(Core.frostSettings.getBoolValue(Settings.MESSAGE_BODY_ANTIALIAS));
                } else if (evt.getPropertyName().equals(Settings.MESSAGE_BODY_FONT_NAME)) {
                    fontChanged();
                } else if (evt.getPropertyName().equals(Settings.MESSAGE_BODY_FONT_SIZE)) {
                    fontChanged();
                } else if (evt.getPropertyName().equals(Settings.MESSAGE_BODY_FONT_STYLE)) {
                    fontChanged();
                } else if (evt.getPropertyName().equals(Settings.FREETALK_SHOW_SMILEYS)) {
                    ((MessageDecoder)messageTextArea.getDecoder()).setSmileyDecode(Core.frostSettings.getBoolValue(Settings.FREETALK_SHOW_SMILEYS));
                    if( selectedMessage != null ) {
                        update_messageSelected(selectedMessage);
                    } else {
                        setMessageText(messageTextArea.getText());
                    }
                } else if (evt.getPropertyName().equals(Settings.FREETALK_SHOW_KEYS_AS_HYPERLINKS)) {
                    ((MessageDecoder)messageTextArea.getDecoder()).setFreenetKeysDecode(Core.frostSettings.getBoolValue(Settings.FREETALK_SHOW_KEYS_AS_HYPERLINKS));
                    if( selectedMessage != null ) {
                        update_messageSelected(selectedMessage);
                    } else {
                        setMessageText(messageTextArea.getText());
                    }
                }
            }
        };

        Core.frostSettings.addPropertyChangeListener(Settings.MESSAGE_BODY_FONT_NAME, propertyChangeListener);
        Core.frostSettings.addPropertyChangeListener(Settings.MESSAGE_BODY_FONT_SIZE, propertyChangeListener);
        Core.frostSettings.addPropertyChangeListener(Settings.MESSAGE_BODY_FONT_STYLE, propertyChangeListener);
        Core.frostSettings.addPropertyChangeListener(Settings.MESSAGE_BODY_ANTIALIAS, propertyChangeListener);
        Core.frostSettings.addPropertyChangeListener(Settings.FREETALK_SHOW_SMILEYS, propertyChangeListener);
        Core.frostSettings.addPropertyChangeListener(Settings.FREETALK_SHOW_KEYS_AS_HYPERLINKS, propertyChangeListener);
    }

    private void fontChanged() {
        final String fontName = Core.frostSettings.getValue(Settings.MESSAGE_BODY_FONT_NAME);
        final int fontStyle = Core.frostSettings.getIntValue(Settings.MESSAGE_BODY_FONT_STYLE);
        final int fontSize = Core.frostSettings.getIntValue(Settings.MESSAGE_BODY_FONT_SIZE);
        Font font = new Font(fontName, fontStyle, fontSize);
        if (!font.getFamily().equals(fontName)) {
            logger.error("The selected font was not found in your system");
            logger.error("That selection will be changed to \"Monospaced\".");
            Core.frostSettings.setValue(Settings.MESSAGE_BODY_FONT_NAME, "Monospaced");
            font = new Font("Monospaced", fontStyle, fontSize);
        }
        messageTextArea.setFont(font);
    }

    private void positionDividers(final List<FreetalkFileAttachment> attachedFiles) {

        if (attachedFiles == null || attachedFiles.size() == 0) {
            messageSplitPane.setBottomComponent(null);
            messageSplitPane.setDividerSize(0);
            messageSplitPane.setDividerLocation(1.0);
        } else {
            messageSplitPane.setBottomComponent(filesTableScrollPane);
            messageSplitPane.setDividerSize(3);
            messageSplitPane.setDividerLocation(0.75);
        }
    }

    public void saveMessageButton_actionPerformed() {
        FileAccess.saveDialog(
            MainFrame.getInstance(),
            messageTextArea.getText(),
            Core.frostSettings.getValue(Settings.DIR_LAST_USED),
            language.getString("MessagePane.messageText.saveDialog.title"));
    }

    private void showAttachedFilesPopupMenu(final MouseEvent e) {
        if (popupMenuAttachmentTable == null) {
            popupMenuAttachmentTable = new PopupMenuAttachmentFile();
            language.addLanguageListener(popupMenuAttachmentTable);
        }
        popupMenuAttachmentTable.show(e.getComponent(), e.getX(), e.getY());
    }

    private void showHyperLinkPopupMenu(final HyperlinkEvent e, final String clickedKey, final List<String> allKeys, final int x, final int y) {
        if (popupMenuHyperLink == null) {
            popupMenuHyperLink = new PopupMenuHyperLink();
            language.addLanguageListener(popupMenuHyperLink);
        }
        popupMenuHyperLink.setClickedKey(clickedKey);
        popupMenuHyperLink.setAllKeys(allKeys);

        popupMenuHyperLink.show(messageTextArea, x, y);
    }

    private void showTofTextAreaPopupMenu(final MouseEvent e) {
        if (popupMenuTofText == null) {
            popupMenuTofText = new PopupMenuTofText(messageTextArea);
            language.addLanguageListener(popupMenuTofText);
        }
        popupMenuTofText.show(e.getComponent(), e.getX(), e.getY());
    }

    private class PopupMenuAttachmentFile extends JPopupMenu implements ActionListener, LanguageListener {

		private static final long serialVersionUID = 1L;

//        private JMenuItem cancelItem = new JMenuItem();
        private final JMenuItem saveAttachmentItem = new JMenuItem();
        private final JMenuItem saveAttachmentsItem = new JMenuItem();

        private final JMenu copyToClipboardMenu = new JMenu();
        private final JMenuItem copyKeysAndNamesItem = new JMenuItem();
        private final JMenuItem copyExtendedInfoItem = new JMenuItem();

        public PopupMenuAttachmentFile() {
            super();
            initialize();
        }

        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == saveAttachmentsItem || e.getSource() == saveAttachmentItem) {
                downloadAttachments();
            } else if (e.getSource() == copyKeysAndNamesItem) {
				ClipboardUtil.copyKeysAndFilenames(getItems());
            } else if (e.getSource() == copyExtendedInfoItem) {
				ClipboardUtil.copyExtendedInfo(getItems());
            }
        }

        private void initialize() {
            languageChanged(null);

            copyToClipboardMenu.add(copyKeysAndNamesItem);
            copyToClipboardMenu.add(copyExtendedInfoItem);

            copyKeysAndNamesItem.addActionListener(this);
            copyExtendedInfoItem.addActionListener(this);

            saveAttachmentsItem.addActionListener(this);
            saveAttachmentItem.addActionListener(this);
        }

        public void languageChanged(final LanguageEvent event) {
            copyKeysAndNamesItem.setText(language.getString("Common.copyToClipBoard.copyKeysWithFilenames"));
            copyExtendedInfoItem.setText(language.getString("Common.copyToClipBoard.copyExtendedInfo"));
            copyToClipboardMenu.setText(language.getString("Common.copyToClipBoard") + "...");

            saveAttachmentsItem.setText(language.getString("MessagePane.fileAttachmentTable.popupmenu.downloadAttachments"));
            saveAttachmentItem.setText(language.getString("MessagePane.fileAttachmentTable.popupmenu.downloadSelectedAttachment"));
        }

        @Override
        public void show(final Component invoker, final int x, final int y) {
            removeAll();

            add(copyToClipboardMenu);
            addSeparator();

            if (filesTable.getSelectedRow() == -1) {
                add(saveAttachmentsItem);
            } else {
                add(saveAttachmentItem);
            }

            super.show(invoker, x, y);
        }

        /**
         * Adds either the selected or all files from the attachmentTable to downloads table.
         */
        private void downloadAttachments() {
            final Iterator<FreetalkFileAttachment> it = getItems().iterator();
            while (it.hasNext()) {
                final FreetalkFileAttachment fa = it.next();
                String filename = fa.getFileName();
                // maybe convert html codes (e.g. %2c -> , )
                if( filename.indexOf("%") > 0 ) {
                    try {
                        filename = java.net.URLDecoder.decode(filename, "UTF-8");
                    } catch (final java.io.UnsupportedEncodingException ex) {
                        logger.error("Decode of HTML code failed", ex);
                    }
                }
                final FrostDownloadItem dlItem = new FrostDownloadItem(
                        filename,
                        fa.getKey(),
                        fa.getFileSize());
                getDownloadModel().addDownloadItem(dlItem);
            }
        }

        /**
         * Returns a list of all items to process, either selected ones or all.
         */
        private List<FreetalkFileAttachment> getItems() {
            List<FreetalkFileAttachment> items = null;
            final int[] selectedRows = filesTable.getSelectedRows();
            if (selectedRows.length == 0) {
                // If no rows are selected, add all attachments to download table
                items = selectedMessage.getFileAttachments();
            } else {
                final List<FreetalkFileAttachment> attachments = selectedMessage.getFileAttachments();
                items = new LinkedList<FreetalkFileAttachment>();
                for( final int element : selectedRows ) {
                    final FreetalkFileAttachment fo = attachments.get(element);
                    items.add(fo);
                }
            }
            return items;
        }
    }

    private class PopupMenuHyperLink extends JPopupMenu implements ActionListener, LanguageListener {

		private static final long serialVersionUID = 1L;

        private final JMenuItem cancelItem = new JMenuItem();

        private final JMenuItem copyKeyOnlyToClipboard = new JMenuItem();

        private final JMenuItem copyFreesiteLinkToClipboard = new JMenuItem();

        private final JMenuItem copyFileLinkToClipboard = new JMenuItem();
        private final JMenuItem copyAllFileLinksToClipboard = new JMenuItem();

        private final JMenuItem downloadFile = new JMenuItem();
        private final JMenuItem downloadAllFiles = new JMenuItem();

        private String clickedKey = null;
		private transient List<String> allKeys;

        public PopupMenuHyperLink() {
            super();
            initialize();
        }

        public void setClickedKey(final String s) {
            clickedKey = s;
        }
        public void setAllKeys(final List<String> l) {
            allKeys = l;
        }

        public void actionPerformed(final ActionEvent e) {
            if( e.getSource() == copyKeyOnlyToClipboard ) {
                copyToClipboard(false);
            } else if( e.getSource() == copyFreesiteLinkToClipboard ) {
                copyToClipboard(false);
            } else if( e.getSource() == copyFileLinkToClipboard ) {
                copyToClipboard(false);
            } else if( e.getSource() == copyAllFileLinksToClipboard ) {
                copyToClipboard(true);
            } else if( e.getSource() == downloadFile ) {
                downloadItems(false);
            } else if( e.getSource() == downloadAllFiles ) {
                downloadItems(true);
            }
        }

        private void initialize() {
            languageChanged(null);

            copyKeyOnlyToClipboard.addActionListener(this);
            copyFreesiteLinkToClipboard.addActionListener(this);
            copyFileLinkToClipboard.addActionListener(this);
            copyAllFileLinksToClipboard.addActionListener(this);
            downloadFile.addActionListener(this);
            downloadAllFiles.addActionListener(this);
        }

        public void languageChanged(final LanguageEvent event) {
            copyKeyOnlyToClipboard.setText(language.getString("MessagePane.hyperlink.popupmenu.copyKeyToClipboard"));
            copyFreesiteLinkToClipboard.setText(language.getString("MessagePane.hyperlink.popupmenu.copyFreesiteLinkToClipboard"));
            copyFileLinkToClipboard.setText(language.getString("MessagePane.hyperlink.popupmenu.copyFileKeyToClipboard"));
            copyAllFileLinksToClipboard.setText(language.getString("MessagePane.hyperlink.popupmenu.copyAllFileKeysToClipboard"));
            downloadFile.setText(language.getString("MessagePane.hyperlink.popupmenu.downloadFileKey"));
            downloadAllFiles.setText(language.getString("MessagePane.hyperlink.popupmenu.downloadAllFileKeys"));

            cancelItem.setText(language.getString("Common.cancel"));
        }

        @Override
        public void show(final Component invoker, final int x, final int y) {
            removeAll();

            // if clickedLink conatins no '/', its only a key without file, allow to copy this to clipboard only
            // if clickedLink ends with a '/' its a freesite link, allow to copy this to clipboard only
            // else the clickedLink is a filelink, allow to copy/download this link or ALL filelinks

            if( clickedKey.indexOf("/") < 0 ||
                !Character.isLetterOrDigit(clickedKey.charAt(clickedKey.length()-1)) )
            {
                // key only
                add(copyKeyOnlyToClipboard);
            } else if( clickedKey.endsWith("/") ) {
                // freesite link
                add(copyFreesiteLinkToClipboard);
            } else {
                // file key
                add(copyFileLinkToClipboard);
                if( allKeys.size() > 1 ) {
                    add(copyAllFileLinksToClipboard);
                }
                addSeparator();
                add(downloadFile);
                if( allKeys.size() > 1 ) {
                    add(downloadAllFiles);
                }
            }

            addSeparator();
            add(cancelItem);

            super.show(invoker, x, y);
        }

        /**
         * Adds either the selected or all files from the attachmentTable to downloads table.
         */
        private void downloadItems(final boolean getAll) {

            final List<String> items = getItems(getAll);
            if( items == null ) {
                return;
            }

            for( final String item : items ) {
                String key;
                // 0.7: use key/filename
                key = item;
                String name = item.substring(item.lastIndexOf("/")+1);
                // maybe convert html codes (e.g. %2c -> , )
                if( name.indexOf("%") > 0 ) {
                    try {
                        name = java.net.URLDecoder.decode(name, "UTF-8");
                    } catch (final java.io.UnsupportedEncodingException ex) {
                        logger.error("Decode of HTML code failed", ex);
                    }
                }
                final FrostDownloadItem dlItem = new FrostDownloadItem(name, key);
                getDownloadModel().addDownloadItem(dlItem);
            }
        }

        private List<String> getItems(final boolean getAll) {
            List<String> items;
            if( getAll ) {
                items = allKeys;
            } else {
                items = Collections.singletonList(clickedKey);
            }
            if( items == null || items.size() == 0 ) {
                return null;
            }
            return items;
        }

        /**
         * This method copies the CHK keys and file names of the selected or all items to the clipboard.
         */
        private void copyToClipboard(final boolean getAll) {

            final List<String> items = getItems(getAll);
            if( items == null ) {
                return;
            }

            String text;
            if( items.size() > 1 ) {
                final StringBuilder textToCopy = new StringBuilder();
            	for( final String key : items ) {
            		textToCopy.append(key).append("\n");
            	}
            	text = textToCopy.toString();
            } else {
            	// don't include a trailing \n if we only have one item
            	text = items.get(0);
            }
            ClipboardUtil.copyText(text);
        }
    }

    private class PopupMenuTofText extends JPopupMenu implements ActionListener, LanguageListener {

		private static final long serialVersionUID = 1L;

        private final JTextComponent sourceTextComponent;

        private final JMenuItem copyItem = new JMenuItem();
        private final JMenuItem cancelItem = new JMenuItem();
        private final JMenuItem saveMessageItem = new JMenuItem();

        public PopupMenuTofText(final JTextComponent sourceTextComponent) {
            super();
            this.sourceTextComponent = sourceTextComponent;
            initialize();
        }

        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == saveMessageItem) {
                saveMessageButton_actionPerformed();
            } else if (e.getSource() == copyItem) {
                // copy selected text
                final String text = sourceTextComponent.getSelectedText();
                ClipboardUtil.copyText(text);
            }
        }

        private void initialize() {
            languageChanged(null);

            copyItem.addActionListener(this);
            saveMessageItem.addActionListener(this);

            add(copyItem);
            addSeparator();
            add(saveMessageItem);
            addSeparator();
            add(cancelItem);
        }

        public void languageChanged(final LanguageEvent event) {
            copyItem.setText(language.getString("MessagePane.messageText.popupmenu.copy"));
            saveMessageItem.setText(language.getString("MessagePane.messageText.popupmenu.saveMessageToDisk"));
            cancelItem.setText(language.getString("Common.cancel"));
        }

        @Override
        public void show(final Component invoker, final int x, final int y) {
            if ((selectedMessage != null) && (selectedMessage.getContent() != null)) {
                if (sourceTextComponent.getSelectedText() != null) {
                    copyItem.setEnabled(true);
                } else {
                    copyItem.setEnabled(false);
                }
                super.show(invoker, x, y);
            }
        }
    }

    private DownloadModel getDownloadModel() {
        return FileTransferManager.inst().getDownloadManager().getModel();
    }

    public void close() {
        Core.frostSettings.removePropertyChangeListener(Settings.MESSAGE_BODY_FONT_NAME, propertyChangeListener);
        Core.frostSettings.removePropertyChangeListener(Settings.MESSAGE_BODY_FONT_SIZE, propertyChangeListener);
        Core.frostSettings.removePropertyChangeListener(Settings.MESSAGE_BODY_FONT_STYLE, propertyChangeListener);
        Core.frostSettings.removePropertyChangeListener(Settings.MESSAGE_BODY_ANTIALIAS, propertyChangeListener);

        if (popupMenuAttachmentTable != null) {
            language.removeLanguageListener(popupMenuAttachmentTable);
        }
        if (popupMenuTofText != null) {
            language.removeLanguageListener(popupMenuTofText);
        }
    }

    /**
     * Used by MessageWindow to attach a KeyListener for ESC.
     */
    @Override
    public void addKeyListener(final KeyListener l) {
        super.addKeyListener(l);
        messageTextArea.addKeyListener(l);
        filesTable.addKeyListener(l);
    }
    /**
     * Used by MessageWindow to detach a KeyListener for ESC.
     */
    @Override
    public void removeKeyListener(final KeyListener l) {
        super.removeKeyListener(l);
        messageTextArea.removeKeyListener(l);
        filesTable.removeKeyListener(l);
    }
}
