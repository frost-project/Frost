/*
 HelpBrowser.java / Frost
 Copyright (C) 2006  Frost Project <jtcfrost.sourceforge.net>

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
package frost.gui.help;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.util.ClipboardUtil;
import frost.util.gui.MiscToolkit;
import frost.util.gui.SimplePopupMenuListener;
import frost.util.gui.action.BaseAction;
import frost.util.gui.action.CancelAction;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

/**
 * Browser Component
 * @author Jantho
 * modified by notitaccu
 */
public class HelpBrowser extends JPanel implements LanguageListener, SimplePopupMenuListener {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(HelpBrowser.class);

    private static Language language = Language.getInstance();

    private final String url_prefix;

    private final String homePage;

	private CopyTextAction copyTextAction;
	private CancelAction cancelAction;
	private PopupMenuTofText popup;

	private transient BrowserHistory browserHistory;

    // Global Variables
    JFrame parent;
    // GUI Objects
    JButton backButton;
    JButton homeButton;
    JButton forwardButton;

    JTextField TFsearchTxt;
    JButton BfindNext;
    JButton BfindPrev;

    JEditorPane editorPane;

    HelpHTMLEditorKit helpHTMLEditorKit;

    int lastSearchPosStart = 0;
    int lastSearchPosEnd = 0;
    String lastSearchText = null;

	public HelpBrowser(final JFrame parent, final String locale, final String urlPrefix, final String homePage) {
		this.parent = parent;
		this.url_prefix = urlPrefix;
		this.homePage = homePage;
		setHelpLocale(locale);
		init();
	}

    private void init() {

        // history init
        browserHistory = new BrowserHistory();
        browserHistory.resetToHomepage(homePage);

        editorPane = new JEditorPane();
        editorPane.setCaret(new SelectionPreservingCaret());
        editorPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if( e.getEventType() == HyperlinkEvent.EventType.ENTERED ) {
                    ((JEditorPane) e.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                }
                if( e.getEventType() == HyperlinkEvent.EventType.EXITED ) {
                    ((JEditorPane) e.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    return;
                }
                if( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
                    ((JEditorPane) e.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    browserHistory.setCurrentPage(e.getURL().toString());
                    setHelpPage(e.getURL().toString());
                }
            }
        });

		copyTextAction = new CopyTextAction();
		cancelAction = new CancelAction();
		popup = new PopupMenuTofText();
		popup.addPopupMenuListener(this);
		editorPane.setComponentPopupMenu(popup);

        final JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setWheelScrollingEnabled(true);

        backButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/go-previous.png"));
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if( browserHistory.isBackwardPossible() ) {
                    setHelpPage(browserHistory.backwardPage());
                }
            }
        });

        forwardButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/go-next.png"));
        forwardButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if( browserHistory.isForwardPossible() ) {
                    setHelpPage(browserHistory.forwardPage());
                }
            }
        });

        homeButton = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/go-home.png"));
        homeButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                browserHistory.resetToHomepage(homePage);
                setHelpPage(homePage);
            }
        });

        final JLabel Lsearch = new JLabel(MiscToolkit.loadImageIcon("/data/toolbar/system-search.png"));
        TFsearchTxt = new JTextField(15);

        BfindNext = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/go-down.png"));
        BfindNext.setDefaultCapable(true);
        BfindNext.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                searchText(true); // search forward
            }
        });

        BfindPrev = new JButton(MiscToolkit.loadImageIcon("/data/toolbar/go-up.png"));
        BfindPrev.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                searchText(false); // search backward
            }
        });

        final JPanel contentPanel = this;
        contentPanel.setLayout(new BorderLayout());

        final JPanel buttonPanelLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanelLeft.add(backButton);
        buttonPanelLeft.add(homeButton);
        buttonPanelLeft.add(forwardButton);

        final JPanel buttonPanelRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanelRight.add(Lsearch);
        buttonPanelRight.add(TFsearchTxt);
        buttonPanelRight.add(BfindNext);
        buttonPanelRight.add(BfindPrev);

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(buttonPanelLeft, BorderLayout.WEST);
        buttonPanel.add(buttonPanelRight, BorderLayout.EAST);

        editorPane.setEditable(false);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.NORTH);

        helpHTMLEditorKit = new HelpHTMLEditorKit(url_prefix);
        editorPane.setEditorKit(helpHTMLEditorKit);

		language.addLanguageListener(this);
		languageChanged(null);

        setHelpPage(homePage);
    }

    private void searchText(final boolean forward) {

        String searchTxt = TFsearchTxt.getText();
        if( searchTxt == null ) {
            return;
        }
        searchTxt = searchTxt.trim();
        if( searchTxt.length() == 0 ) {
            return;
        }

        searchTxt = searchTxt.toLowerCase();

        if( lastSearchText == null ) {
            lastSearchText = searchTxt;
        } else if( lastSearchText != null && searchTxt.equals(lastSearchText) == false ) {
            // search from the beginning
            lastSearchPosStart=0;
            lastSearchPosEnd=0;
            lastSearchText=searchTxt;
        }

        String docTxt = null;
        try {
            docTxt = helpHTMLEditorKit.getHelpHTMLDocument().getText(0, helpHTMLEditorKit.getHelpHTMLDocument().getLength());
            docTxt = docTxt.toLowerCase();
        } catch (final BadLocationException e1) {
            logger.error("Could not get text from document.", e1);
            return;
        }

        int pos;
        if( forward ) {
            pos = docTxt.indexOf(searchTxt, lastSearchPosEnd); // search after last found endPos
        } else {
            // search before last found startPos
            if( lastSearchPosStart > 0 ) {
                final String tmpStr = docTxt.substring(0, lastSearchPosStart);
                pos = tmpStr.lastIndexOf(searchTxt);
            } else {
                // we are already at the begin
                return;
            }
        }
        if( pos > -1 ) {
            // scroll to text and select
            final int endPos = pos + searchTxt.length();
            editorPane.setCaretPosition(pos);
            editorPane.moveCaretPosition(endPos);

            lastSearchPosStart = pos;
            lastSearchPosEnd = endPos;
        } else {
            editorPane.setCaretPosition(0);
            lastSearchPosStart = 0;
            lastSearchPosEnd = 0;
        }
    }

    void setHelpPage(String url) {
        if( url == null ) {
            url = homePage;
        }

		try {
			URI uri = new URI(url);
			if (!uri.isAbsolute()) {
				uri = Path.of(url_prefix).resolve(url).toUri();
			}

			logger.debug("Show help page {}", uri);
			editorPane.setPage(uri.toString());

            lastSearchPosStart = 0; // reset pos
            lastSearchPosEnd = 0; // reset pos
            lastSearchText = null;

            editorPane.requestFocus();
		} catch (IOException e) {
			logger.error("Missing file: {}", url);
		} catch (URISyntaxException e) {
			logger.error("Invalid URI: {}", url);
		}

        updateBrowserButtons();
    }

    private void updateBrowserButtons() {
        forwardButton.setEnabled( browserHistory.isForwardPossible() );
        backButton.setEnabled( browserHistory.isBackwardPossible() );
    }

    void setHelpLocale(final String newLocale) {
        // Hier ist ne schoene stelle zum pruefen.
//        if( newLocale.equals("default") ) {
//            url_locale = "";
//        } else {
//            url_locale = newLocale;
//        }
    }

    private class BrowserHistory {

        private final ArrayList<String> history = new ArrayList<String>();
        private int historypos = -1; // this means history ist invalid

        public boolean isForwardPossible() {
            if( historypos < history.size()-1 ) {
                return true;
            }
            return false;
        }
        public String forwardPage() {
            if( !isForwardPossible() ) {
                return null;
            }
            historypos++;
            return history.get(historypos);
        }
        public boolean isBackwardPossible() {
            if( historypos > 0 ) {
                return true;
            }
            return false;
        }
        public String backwardPage() {
            if( !isBackwardPossible() ) {
                return null;
            }
            historypos--;
            return history.get(historypos);
        }
        public void setCurrentPage(final String page) {
            // a link was clicked, add this new page after current historypos and clear alll forward pages
            // this is the behaviour of Mozilla too
            if( historypos < history.size()-1 ) {
                history.subList(historypos+1, history.size()).clear();
            }
            history.add(page);
            historypos++;
        }
        public void resetToHomepage(final String homepage) {
            history.clear();
            history.add(homepage);
            historypos = 0; // current page is page at index 0
        }
    }

    /**
     * Caret implementation that doesn't blow away the selection when
     * we lose focus.
     */
    public class SelectionPreservingCaret extends DefaultCaret {

		private static final long serialVersionUID = 1L;

        /*
         * The last SelectionPreservingCaret that lost focus
         */
        private SelectionPreservingCaret last = null;

        /**
         * The last event that indicated loss of focus
         */
        private FocusEvent lastFocusEvent = null;

        public SelectionPreservingCaret() {
            // The blink rate is set by BasicTextUI when the text component
            // is created, and is not (re-) set when a new Caret is installed.
            // This implementation attempts to pull a value from the UIManager,
            // and defaults to a 500ms blink rate. This assumes that the
            // look and feel uses the same blink rate for all text components
            // (and hence we just pull the value for TextArea). If you are
            // using a look and feel for which this is not the case, you may
            // need to set the blink rate after creating the Caret.
            int blinkRate = 500;
            final Object o = UIManager.get("TextArea.caretBlinkRate");
            if ((o != null) && (o instanceof Integer)) {
                final Integer rate = (Integer) o;
                blinkRate = rate.intValue();
            }
            setBlinkRate(blinkRate);
        }

        /**
         * Called when the component containing the caret gains focus.
         * DefaultCaret does most of the work, while the subclass checks
         * to see if another instance of SelectionPreservingCaret previously
         * had focus.
         *
         * @param e the focus event
         * @see FocusListener#focusGained
         */
        @Override
        public void focusGained(final FocusEvent evt) {
            super.focusGained(evt);

            // If another instance of SelectionPreservingCaret had focus and
            // we defered a focusLost event, deliver that event now.
            if ((last != null) && (last != this)) {
                last.hide();
            }
        }

        /**
         * Called when the component containing the caret loses focus. Instead
         * of hiding both the caret and the selection, the subclass only
         * hides the caret and saves a (static) reference to the event and this
         * specific caret instance so that the event can be delivered later
         * if appropriate.
         *
         * @param e the focus event
         * @see FocusListener#focusLost
         */
        @Override
        public void focusLost(final FocusEvent evt) {
            setVisible(false);
            last = this;
            lastFocusEvent = evt;
        }

        /**
         * Delivers a defered focusLost event to this caret.
         */
        protected void hide() {
            if (last == this) {
                super.focusLost(lastFocusEvent);
                last = null;
                lastFocusEvent = null;
            }
        }
    }

	@Override
	public void languageChanged(LanguageEvent event) {
		copyTextAction.setText(language.getString("Common.copy"));
		cancelAction.setText(language.getString("Common.cancel"));
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		copyTextAction.setEnabled(editorPane.getSelectedText() != null);
	}

	private class CopyTextAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			ClipboardUtil.copyText(editorPane.getSelectedText());
		}
	}

	private class PopupMenuTofText extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		public PopupMenuTofText() {
			add(copyTextAction);
			addSeparator();
			add(cancelAction);
		}
	}
}
