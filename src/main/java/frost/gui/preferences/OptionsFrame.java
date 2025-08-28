/*
  OptionsFrame.java / Frost
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
--------------------------------------------------------------------------
  DESCRIPTION:
  This file contains the whole 'Options' dialog. It first reads the
  actual config from properties file, and on 'OK' it saves all
  settings to the properties file and informs the caller to reload
  this file.
*/
package frost.gui.preferences;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.MainFrame;
import frost.Settings;
import frost.storage.StorageException;
import frost.util.gui.translation.Language;
import frost.util.gui.verify.VerifierUtil;

/**
 * Main options frame.
 */
public class OptionsFrame extends JDialog implements ListSelectionListener {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(OptionsFrame.class);

    /**
     * A simple helper class to store JPanels and their name into a JList.
     */
    class ListBoxData {
        String name;
        JPanel panel;
        public ListBoxData(final String n, final JPanel p) {
            panel = p;
            name = n;
        }
        public JPanel getPanel() {
            return panel;
        }
        @Override
        public String toString() {
            return name;
        }
    }

    private MainFrame mainFrame;
    private final Settings settings;
    private final Language language;

    private JPanel buttonPanel = null; // OK / Cancel
    private boolean checkBlock;
    private boolean checkBlockBody;

    // this vars hold some settings from start of dialog to the end.
    // then its checked if the settings are changed by user
    private boolean checkHideBadMessages;
    private boolean checkHideCheckMessages;
    private boolean checkHideObserveMessages;
    private boolean checkHideJunkMessages;
    private String checkMaxMessageDisplay;
    private String checkMaxMessageDownload;
    private boolean checkSignedOnly;
    private boolean checkRememberSharedFileDownloaded;

    private boolean checkShowDeletedMessages;
    private boolean showColoredRows;

    private boolean checkShowOwnMessagesAsMeDisabled;

    private boolean checkIndicateLowReceivedMessages;

    private JPanel contentAreaPanel = null;
    private DisplayPanel displayPanel = null;
    private DisplayBoardTreePanel displayBoardTreePanel = null;
    private DisplayMessagesPanel displayMessagesPanel = null;
    private DownloadPanel downloadPanel = null;

    private boolean exitState;

    private JPanel mainPanel = null;
    private MiscPanel miscPanel = null;
    private NewsPanel newsPanel = null;
    private News2Panel news2Panel = null;
    private JunkPanel junkPanel = null;
    private ExpirationPanel expirationPanel = null;
    private JList<ListBoxData> optionsGroupsList = null;
    private JPanel optionsGroupsPanel = null;
    private SearchPanel searchPanel = null;

    private boolean shouldReloadMessages = false;
    private boolean shouldResetLastBackloadUpdateFinishedMillis = false;
    private boolean shouldResetSharedFilesLastDownloaded = false;

    private UploadPanel uploadPanel = null;

    private static int lastSelectedPanelIndex = 0;

    /**
     * Constructor, reads init file and inits the gui.
     * @param mainFrame
     * @param settings
     */
    public OptionsFrame(final MainFrame mainFrame, final Settings settings) {
        super(mainFrame);
        setModal(true);
        this.mainFrame = mainFrame;
        this.settings = settings;

        language = Language.getInstance();

        setDataElements();

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            Init();
        } catch (final Exception e) {
            logger.error("Exception thrown in constructor", e);
        }
        // set initial selection (also sets panel)
        optionsGroupsList.setSelectedIndex(lastSelectedPanelIndex);

        // final layouting
        pack();

        // center dialog on parent
        setLocationRelativeTo(mainFrame);
    }

    /**
     * Close window and do not save settings
     */
    private void cancel() {
        exitState = false;
        dispose();
    }

    /**
     * cancelButton Action Listener (Cancel)
     * @param e
     */
    private void cancelButton_actionPerformed(final ActionEvent e) {
        cancel();
    }

    /**
     * Computes the maximum width and height of the various options panels.
     * Returns Dimension with max. x and y that is needed.
     * Gets all panels from the ListModel of the option groups list.
     * @param m
     * @return
     */
    protected Dimension computeMaxSize(final ListModel<ListBoxData> m) {
        if ((m == null) || (m.getSize() == 0)) {
            return null;
        }
        int maxX = -1;
        int maxY = -1;
        // misuse a JDialog to determine the panel size before showing
        JDialog dlgdummy = new JDialog();
        for (int x = 0; x < m.getSize(); x++) {
            final ListBoxData lbdata = m.getElementAt(x);
            final JPanel aPanel = lbdata.getPanel();

            contentAreaPanel.removeAll();
            contentAreaPanel.add(aPanel, BorderLayout.CENTER);
            dlgdummy.setContentPane(contentAreaPanel);
            dlgdummy.pack();
            // get size (including bordersize from contentAreaPane)
            final int tmpX = contentAreaPanel.getWidth();
            final int tmpY = contentAreaPanel.getHeight();
            maxX = Math.max(maxX, tmpX);
            maxY = Math.max(maxY, tmpY);
        }
        dlgdummy = null; // give some hint to gc() , in case its needed
        contentAreaPanel.removeAll();
        return new Dimension(maxX, maxY);
    }

    /**
     * Build the button panel.
     * @return
     */
    protected JPanel getButtonPanel() {
        if (buttonPanel == null) {
            buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            // OK / Cancel
            final JButton okButton = new JButton(language.getString("Common.ok"));
            final JButton cancelButton = new JButton(language.getString("Common.cancel"));

            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    okButton_actionPerformed(e);
                }
            });
            cancelButton
                .addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    cancelButton_actionPerformed(e);
                }
            });
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
        }
        return buttonPanel;
    }

    /**
     * @return
     */
    private DisplayPanel getDisplayPanel() {
        if (displayPanel == null) {
            displayPanel = new DisplayPanel(this, settings);
        }
        return displayPanel;
    }

    /**
     * @return
     */
    private DisplayBoardTreePanel getDisplayBoardTreePanel() {
        if (displayBoardTreePanel == null) {
            displayBoardTreePanel = new DisplayBoardTreePanel(this, settings);
        }
        return displayBoardTreePanel;
    }

    /**
     * @return
     */
    private DisplayMessagesPanel getDisplayMessagesPanel() {
        if (displayMessagesPanel == null) {
            displayMessagesPanel = new DisplayMessagesPanel(this, settings);
        }
        return displayMessagesPanel;
    }

    /**
     * @return
     */
    private DownloadPanel getDownloadPanel() {
        if (downloadPanel == null) {
            downloadPanel = new DownloadPanel(this, settings);
        }
        return downloadPanel;
    }

    /**
     * @return
     */
    private MiscPanel getMiscPanel() {
        if (miscPanel == null) {
            miscPanel = new MiscPanel(settings);
        }
        return miscPanel;
    }

    /**
     * @return
     */
    private News2Panel getNews2Panel() {
        if (news2Panel == null) {
            news2Panel = new News2Panel(settings);
        }
        return news2Panel;
    }

    /**
     * @return
     */
    private JunkPanel getJunkPanel() {
        if (junkPanel == null) {
            junkPanel = new JunkPanel(settings);
        }
        return junkPanel;
    }

    /**
     * @return
     */
    private ExpirationPanel getExpirationPanel() {
        if (expirationPanel == null) {
            expirationPanel = new ExpirationPanel(this, settings);
        }
        return expirationPanel;
    }

    /**
     * @return
     */
    private NewsPanel getNewsPanel() {
        if (newsPanel == null) {
            newsPanel = new NewsPanel(settings);
        }
        return newsPanel;
    }

    /**
     * Build the panel containing the list of option groups.
     * @return
     */
    protected JPanel getOptionsGroupsPanel() {
        if (optionsGroupsPanel == null) {
            // init the list
            final Vector<ListBoxData> listData = new Vector<ListBoxData>();
            listData.add( new ListBoxData(" "+language.getString("Options.downloads")+" ", getDownloadPanel()));
            listData.add( new ListBoxData(" "+language.getString("Options.uploads")+" ", getUploadPanel()));
            listData.add( new ListBoxData(" "+language.getString("Options.news")+" (1) ", getNewsPanel()));
            listData.add( new ListBoxData(" "+language.getString("Options.news")+" (2) ", getNews2Panel()));
            listData.add( new ListBoxData(" "+language.getString("Options.junk")+" ", getJunkPanel()));
            listData.add( new ListBoxData(" "+language.getString("Options.display")+" ", getDisplayPanel()));

            listData.add( new ListBoxData("    "+language.getString("Options.boardTree")+" ", getDisplayBoardTreePanel()));
            listData.add( new ListBoxData("    "+language.getString("Options.messages")+" ", getDisplayMessagesPanel()));
            listData.add( new ListBoxData(" "+language.getString("Options.expiration")+" ", getExpirationPanel()));
            listData.add( new ListBoxData(" "+language.getString("Options.search")+" ", getSearchPanel()));
            listData.add( new ListBoxData(" "+language.getString("Options.miscellaneous")+" ", getMiscPanel()));
            optionsGroupsList = new JList<ListBoxData>(listData);
            optionsGroupsList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            optionsGroupsList.addListSelectionListener(this);

            optionsGroupsPanel = new JPanel(new GridBagLayout());
            final GridBagConstraints constr = new GridBagConstraints();
            constr.anchor = GridBagConstraints.NORTHWEST;
            constr.fill = GridBagConstraints.BOTH;
            constr.weightx = 0.7;
            constr.weighty = 0.7;
            constr.insets = new Insets(5, 5, 5, 5);
            constr.gridx = 0;
            constr.gridy = 0;
            optionsGroupsPanel.add(optionsGroupsList, constr);
            optionsGroupsPanel.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(5, 5, 5, 5),
                    BorderFactory.createEtchedBorder()));
        }
        return optionsGroupsPanel;
    }

    /**
     * @return
     */
    private SearchPanel getSearchPanel() {
        if (searchPanel == null) {
            searchPanel = new SearchPanel(mainFrame, settings);
        }
        return searchPanel;
    }

    /**
     * @return
     */
    private UploadPanel getUploadPanel() {
        if (uploadPanel == null) {
            uploadPanel = new UploadPanel(settings);
        }
        return uploadPanel;
    }
    /**
     * Build up the whole GUI.
     * @throws Exception
     */
    private void Init() throws Exception {
        //------------------------------------------------------------------------
        // Configure objects
        //------------------------------------------------------------------------
        this.setTitle(language.getString("Options.title"));
        // a program should always give users a chance to change the dialog size if needed
        this.setResizable(true);

        mainPanel = new JPanel(new BorderLayout());
        this.getContentPane().add(mainPanel, null); // add Main panel

        // prepare content area panel
        contentAreaPanel = new JPanel(new BorderLayout());
        contentAreaPanel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        contentAreaPanel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 0, 5, 5),
                contentAreaPanel.getBorder()));

        mainPanel.add(getButtonPanel(), BorderLayout.SOUTH);
        mainPanel.add(getOptionsGroupsPanel(), BorderLayout.WEST);

        // compute and set size of contentAreaPanel
        final Dimension neededSize = computeMaxSize(optionsGroupsList.getModel());
        contentAreaPanel.setMinimumSize(neededSize);
        contentAreaPanel.setPreferredSize(neededSize);

        mainPanel.add(contentAreaPanel, BorderLayout.CENTER);
    }

    /**
     * Close window and save settings
     */
    private void ok() {
		if (!VerifierUtil.verifyAll(contentAreaPanel)) {
			return;
		}

        exitState = true;

        if (displayPanel != null) {
            displayPanel.ok();
        }
        if (displayBoardTreePanel != null) {
            displayBoardTreePanel.ok();
        }
        if (displayMessagesPanel != null) {
            displayMessagesPanel.ok();
        }
        if (downloadPanel != null) {
            downloadPanel.ok();
        }
        if (searchPanel != null) {
            searchPanel.ok();
        }
        if (uploadPanel != null) {
            uploadPanel.ok();
        }
        if (miscPanel != null) {
            miscPanel.ok();
        }
        if (newsPanel != null) {
            newsPanel.ok();
        }
        if (news2Panel != null) {
            news2Panel.ok();
        }
        if (junkPanel != null) {
            junkPanel.ok();
        }
        if (expirationPanel != null) {
            expirationPanel.ok();
        }

        saveSettings();

        dispose();
    }

    /**
     * okButton Action Listener (OK)
     */
    private void okButton_actionPerformed(final ActionEvent e) {
        ok();
    }

    /**
     * When window is about to close, do same as if CANCEL was pressed.
     * @see Window#processWindowEvent(WindowEvent)
     */
    @Override
    protected void processWindowEvent(final WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            cancel();
        }
        super.processWindowEvent(e);
    }

    /**
     * Can be called to run dialog and get its answer (true=OK, false=CANCEL)
     * @return
     */
    public boolean runDialog() {
        exitState = false;
        setVisible(true); // run dialog
        return exitState;
    }

    /**
     * Save settings
     */
    private void saveSettings() {
        try {
            settings.exitSave();
        } catch (final StorageException se) {
            logger.error("Error while saving the settings.", se);
        }

        // now check if some settings changed
        if( !checkMaxMessageDisplay.equals(settings.getString(Settings.MAX_MESSAGE_DISPLAY))
            || (checkSignedOnly != settings.getBoolean(Settings.MESSAGE_HIDE_UNSIGNED))
            || (checkHideBadMessages != settings.getBoolean(Settings.MESSAGE_HIDE_BAD))
            || (checkHideCheckMessages != settings.getBoolean(Settings.MESSAGE_HIDE_CHECK))
            || (checkHideObserveMessages != settings.getBoolean(Settings.MESSAGE_HIDE_OBSERVE))
            || (checkHideJunkMessages != settings.getBoolean(Settings.JUNK_HIDE_JUNK_MESSAGES))
            || (checkBlock != settings.getBoolean(Settings.MESSAGE_BLOCK_SUBJECT_ENABLED))
            || (checkBlockBody != settings.getBoolean(Settings.MESSAGE_BLOCK_BODY_ENABLED))
            || (checkShowDeletedMessages != settings.getBoolean(Settings.SHOW_DELETED_MESSAGES))
            || (showColoredRows != settings.getBoolean(Settings.SHOW_COLORED_ROWS))
            || (checkShowOwnMessagesAsMeDisabled != settings.getBoolean(Settings.SHOW_OWN_MESSAGES_AS_ME_DISABLED))
            || (checkIndicateLowReceivedMessages != settings.getBoolean(Settings.INDICATE_LOW_RECEIVED_MESSAGES))
          )
        {
            // at least one setting changed, reload messages
            shouldReloadMessages = true;
        }

        if( !checkMaxMessageDownload.equals(settings.getString(Settings.MAX_MESSAGE_DOWNLOAD)) ) {
            shouldResetLastBackloadUpdateFinishedMillis = true;
        }

        // if settings was true before and now its disabled
        if( (checkRememberSharedFileDownloaded == true)
                && (settings.getBoolean(Settings.REMEMBER_SHAREDFILE_DOWNLOADED) == false) )
        {
            shouldResetSharedFilesLastDownloaded = true;
        }
    }

    //------------------------------------------------------------------------

    /**
     * Load settings
     */
    private void setDataElements() {
        // first set some settings to check later if they are changed by user
        checkMaxMessageDisplay = settings.getString(Settings.MAX_MESSAGE_DISPLAY);
        checkMaxMessageDownload = settings.getString(Settings.MAX_MESSAGE_DOWNLOAD);
        checkSignedOnly = settings.getBoolean(Settings.MESSAGE_HIDE_UNSIGNED);
        checkHideBadMessages = settings.getBoolean(Settings.MESSAGE_HIDE_BAD);
        checkHideCheckMessages = settings.getBoolean(Settings.MESSAGE_HIDE_CHECK);
        checkHideObserveMessages = settings.getBoolean(Settings.MESSAGE_HIDE_OBSERVE);
        checkHideJunkMessages = settings.getBoolean(Settings.JUNK_HIDE_JUNK_MESSAGES);
        checkBlock = settings.getBoolean(Settings.MESSAGE_BLOCK_SUBJECT_ENABLED);
        checkBlockBody = settings.getBoolean(Settings.MESSAGE_BLOCK_BODY_ENABLED);
        checkShowDeletedMessages = settings.getBoolean(Settings.SHOW_DELETED_MESSAGES);
        checkShowOwnMessagesAsMeDisabled = settings.getBoolean(Settings.SHOW_OWN_MESSAGES_AS_ME_DISABLED);

        checkIndicateLowReceivedMessages = settings.getBoolean(Settings.INDICATE_LOW_RECEIVED_MESSAGES);

        checkRememberSharedFileDownloaded = settings.getBoolean(Settings.REMEMBER_SHAREDFILE_DOWNLOADED);

        showColoredRows = settings.getBoolean(Settings.SHOW_COLORED_ROWS);
    }

    /**
     * Is called after the dialog is hidden.
     * This method should return true if:
     *  - signedOnly, hideCheck or hideBad where changed by user
     *  - a block settings was changed by user
     * If it returns true, the messages table should be reloaded.
     * @return
     */
    public boolean shouldReloadMessages() {
        return shouldReloadMessages;
    }

    /**
     * @return  true if the LastBackloadUpdateFinishedMillis for all boards should be resetted (user changed maxDownloadDays)
     */
    public boolean shouldResetLastBackloadUpdateFinishedMillis() {
        return shouldResetLastBackloadUpdateFinishedMillis;
    }

    /**
     * @return
     */
    public boolean shouldResetSharedFilesLastDownloaded() {
        return shouldResetSharedFilesLastDownloaded;
    }

    /**
     * Implementing the ListSelectionListener.
     * Must change the content of contentAreaPanel to the selected
     * panel.
     * @see ListSelectionListener#valueChanged(ListSelectionEvent)
     */
    public void valueChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

		final JList<?> theList = (JList<?>) e.getSource();
		final ListBoxData Olbdata = (ListBoxData) theList.getSelectedValue();

		if (!VerifierUtil.verifyAll(contentAreaPanel)) {
			SwingUtilities.invokeLater(() -> optionsGroupsList.setSelectedIndex(lastSelectedPanelIndex));
			return;
		} else {
			lastSelectedPanelIndex = theList.getSelectedIndex();
		}

        contentAreaPanel.removeAll();

        if (Olbdata instanceof ListBoxData) {
			final ListBoxData lbdata = Olbdata;
            final JPanel newPanel = lbdata.getPanel();
            contentAreaPanel.add(newPanel, BorderLayout.CENTER);
            newPanel.revalidate();
            newPanel.repaint();
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                contentAreaPanel.revalidate();
            }
        });
    }
}
