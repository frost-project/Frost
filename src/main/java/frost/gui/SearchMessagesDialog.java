/*
  SearchMessagesDialog.java / Frost
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
package frost.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import com.toedter.calendar.JDateChooser;

import frost.Core;
import frost.MainFrame;
import frost.gui.model.SearchMessagesTableModel;
import frost.messaging.frost.FrostMessageObject;
import frost.messaging.frost.FrostSearchResultMessageObject;
import frost.messaging.frost.boards.Board;
import frost.messaging.frost.gui.MessageWindow;
import frost.messaging.frost.threads.SearchMessagesThread;
import frost.util.gui.MiscToolkit;
import frost.util.gui.TextComponentClipboardMenu;
import frost.util.gui.translation.JTranslatableTabbedPane;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;
import frost.util.gui.tristatecheckbox.TristateCheckBox;

public class SearchMessagesDialog extends JFrame implements LanguageListener {

	private static final long serialVersionUID = 1L;

	private final transient Language language = Language.getInstance();

	private transient SearchMessagesConfig searchMessagesConfig;

    private HashSet<Component> previouslyEnabledComponents = new HashSet<Component>();

	private String resultCountPrefix;
	private String startSearchStr;
	private String stopSearchStr;

	private transient List<Board> chosedBoardsList = new ArrayList<>();
	private transient SearchMessagesThread runningSearchThread;
	private int resultCount;

    private JLabel LresultCount = null;
    private JRadioButton date_RBall = null;
    private JPanel PbuttonsRight = null;
    private JPanel PbuttonsLeft = null;
    private JButton BopenMsg = null;
    private JPanel Pattachments = null;
    private JCheckBox attachment_CBmustContainBoards = null;
    private JCheckBox attachment_CBmustContainFiles = null;
    private JButton Bhelp = null;

    private JPanel jContentPane = null;
    private JPanel contentPanel = null;
    private JPanel Pbuttons = null;
    private JButton Bsearch = null;
    private JButton Bcancel = null;
    private JTabbedPane jTabbedPane = null;
    private JPanel Psearch = null;
    private JPanel PsearchResult = null;
    private JLabel Lsender = null;
    private JLabel Lcontent = null;
    private JTextField search_TFsender = null;
    private JTextField search_TFcontent = null;
    private JPanel Pdate = null;
    private JRadioButton date_RBdisplayed = null;
    private JRadioButton date_RBbetweenDates = null;
    private JDateChooser date_TFstartDate = null;
    private JLabel date_Lto = null;
    private JDateChooser date_TFendDate = null;
    private JRadioButton date_RBdaysBackward = null;
    private JTextField date_TFdaysBackward = null;
    private JPanel PtrustState = null;
    private JRadioButton truststate_RBdisplayed = null;
    private JRadioButton truststate_RBall = null;
    private JRadioButton truststate_RBchosed = null;
    private JPanel truststate_PtrustStates = null;
    private JCheckBox truststate_CBgood = null;
    private JCheckBox truststate_CBobserve = null;
    private JCheckBox truststate_CBcheck = null;
    private JCheckBox truststate_CBbad = null;
    private JCheckBox truststate_CBnone = null;
    private JCheckBox truststate_CBtampered = null;
    private JPanel Parchive = null;
    private JRadioButton archive_RBkeypoolAndArchive = null;
    private JRadioButton archive_RBkeypoolOnly = null;
    private JRadioButton archive_RBarchiveOnly = null;
    private JPanel Pboards = null;
    private JRadioButton boards_RBdisplayed = null;
//    private JRadioButton boards_RBallExisting = null;
    private JRadioButton boards_RBchosed = null;
    private JButton boards_Bchoose = null;
    private JTextField boards_TFchosedBoards = null;
    private TristateCheckBox search_CBprivateMsgsOnly = null;
    private TristateCheckBox search_CBflaggedMsgsOnly = null;
    private TristateCheckBox search_CBstarredMsgsOnly = null;
    private TristateCheckBox search_CBrepliedMsgsOnly = null;
    private JLabel LsearchResult = null;
    private JScrollPane jScrollPane = null;
    private SearchMessagesResultTable searchResultTable = null;
    private SearchMessagesTableModel searchMessagesTableModel = null;  //  @jve:decl-index=0:visual-constraint="735,15"
    private ButtonGroup boards_buttonGroup = null;  //  @jve:decl-index=0:visual-constraint="755,213"
    private ButtonGroup date_buttonGroup = null;  //  @jve:decl-index=0:visual-constraint="765,261"
    private ButtonGroup truststate_buttonGroup = null;  //  @jve:decl-index=0:visual-constraint="752,302"
    private ButtonGroup archive_buttonGroup = null;  //  @jve:decl-index=0:visual-constraint="760,342"
    private JLabel Lsubject = null;
    private JTextField search_TFsubject = null;

    private final JCheckBox senderCaseCheckBox = new JCheckBox("");
    private final JCheckBox subjectCaseCheckBox = new JCheckBox("");
    private final JCheckBox contentCaseCheckBox = new JCheckBox("");

    public SearchMessagesDialog() {
        super();
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        initialize();
        languageChanged(null);
        loadWindowState();
        initializeWithDefaults();

        language.addLanguageListener(this);
    }

    /**
     * Start the search dialog, all boards in board tree are selected.
     */
    public void startDialog() {
        getBoards_RBdisplayed().doClick();
        updateBoardTextField(null);
        setVisible(true);
    }

    /**
     * Start the search dialog with only the specified boards preselected as boards to search into.
     */
    public void startDialog(final List<Board> l) {
        getBoards_RBchosed().doClick();
        updateBoardTextField(l);
        clearSearchResultTable();
        setVisible(true);
    }

    /**
     * This method initializes search_TFsubject
     *
     * @return JTextField
     */
    private JTextField getSearch_TFsubject() {
        if( search_TFsubject == null ) {
            search_TFsubject = new JTextField();
            new TextComponentClipboardMenu(search_TFsubject, language);
        }
        return search_TFsubject;
    }

    /**
     * This method initializes this
     *
     * @return void
     */
    private void initialize() {
        this.setTitle(language.getString("SearchMessages.title"));
        this.setIconImage(MiscToolkit.loadImageIcon("/data/toolbar/edit-find.png").getImage());
        this.setSize(new Dimension(700,550));
        this.setContentPane(getJContentPane());
        // create button groups
        this.getDate_buttonGroup();
        this.getBoards_buttonGroup();
        this.getTruststate_buttonGroup();
        this.getArchive_buttonGroup();
    }

    /**
     * This method initializes jContentPane
     *
     * @return JPanel
     */
    private JPanel getJContentPane() {
        if( jContentPane == null ) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(getContentPanel(), BorderLayout.CENTER);
            jContentPane.add(getPbuttons(), BorderLayout.SOUTH);
        }
        return jContentPane;
    }

    /**
     * This method initializes contentPanel
     *
     * @return JPanel
     */
    private JPanel getContentPanel() {
        if( contentPanel == null ) {
            contentPanel = new JPanel();
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(getJTabbedPane(), BorderLayout.NORTH);
            contentPanel.add(getPsearchResult(), BorderLayout.CENTER);
        }
        return contentPanel;
    }

    /**
     * This method initializes buttonPanel
     *
     * @return JPanel
     */
    private JPanel getPbuttons() {
        if( Pbuttons == null ) {
            Pbuttons = new JPanel();
            Pbuttons.setLayout(new BorderLayout());
            Pbuttons.add(getPbuttonsRight(), BorderLayout.WEST);
            Pbuttons.add(getJPanel(), BorderLayout.EAST);
        }
        return Pbuttons;
    }

    /**
     * This method initializes Bsearch
     *
     * @return JButton
     */
    private JButton getBsearch() {
        if( Bsearch == null ) {
            Bsearch = new JButton();
            // enter key anywhere in dialog (except in table where it opens a msg) starts or stops searching
            Bsearch.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "startStopSearch");
			final Action action = new AbstractAction() {

				private static final long serialVersionUID = 1L;

				public void actionPerformed(final ActionEvent arg0) {
                    if( getBsearch().isEnabled() ) {
                        startOrStopSearching();
                    }
                }
            };
            Bsearch.getActionMap().put("startStopSearch", action);
            Bsearch.addActionListener(action);
        }
        return Bsearch;
    }

    /**
     * This method initializes Bcancel
     *
     * @return JButton
     */
    private JButton getBcancel() {
        if( Bcancel == null ) {
            Bcancel = new JButton();
            Bcancel.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    closePressed();
                }
            });
        }
        return Bcancel;
    }

    /**
     * This method initializes jTabbedPane
     *
     * @return JTabbedPane
     */
    private JTabbedPane getJTabbedPane() {
        if( jTabbedPane == null ) {
            jTabbedPane = new JTranslatableTabbedPane(language);
            jTabbedPane.addTab("SearchMessages.search", null, getPsearch(), null);
            jTabbedPane.addTab("SearchMessages.boards", null, getPboards(), null);
            jTabbedPane.addTab("SearchMessages.date", null, getPdate(), null);
            jTabbedPane.addTab("SearchMessages.trustState", null, getPtrustState(), null);
            jTabbedPane.addTab("SearchMessages.archive", null, getParchive(), null);
            jTabbedPane.addTab("SearchMessages.attachments", null, getPattachments(), null);
        }
        return jTabbedPane;
    }

    /**
     * This method initializes jPanel
     *
     * @return JPanel
     */
    private JPanel getPsearch() {
        if( Psearch == null ) {
            final GridBagConstraints gridBagConstraints29 = new GridBagConstraints();
            gridBagConstraints29.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints29.gridy = 1;
            gridBagConstraints29.weightx = 1.0;
            gridBagConstraints29.insets = new Insets(1,5,1,5);
            gridBagConstraints29.gridx = 1;
            gridBagConstraints29.gridwidth = 4;
            final GridBagConstraints gridBagConstraints110 = new GridBagConstraints();
            gridBagConstraints110.gridx = 0;
            gridBagConstraints110.insets = new Insets(1,5,1,0);
            gridBagConstraints110.anchor = GridBagConstraints.WEST;
            gridBagConstraints110.gridy = 1;
            Lsubject = new JLabel();

            final GridBagConstraints gridBagConstraints101 = new GridBagConstraints();
            gridBagConstraints101.gridx = 1;
            gridBagConstraints101.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints101.insets = new Insets(1,1,1,5);
            gridBagConstraints101.fill = GridBagConstraints.NONE;
            gridBagConstraints101.weighty = 1.0;
            gridBagConstraints101.gridy = 3;

            final GridBagConstraints gridBagConstraints102 = new GridBagConstraints();
            gridBagConstraints102.gridx = 2;
            gridBagConstraints102.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints102.insets = new Insets(1,1,1,5);
            gridBagConstraints102.fill = GridBagConstraints.NONE;
            gridBagConstraints102.weighty = 1.0;
            gridBagConstraints102.gridy = 3;

            final GridBagConstraints gridBagConstraints103 = new GridBagConstraints();
            gridBagConstraints103.gridx = 3;
            gridBagConstraints103.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints103.insets = new Insets(1,1,1,5);
            gridBagConstraints103.fill = GridBagConstraints.NONE;
            gridBagConstraints103.weighty = 1.0;
            gridBagConstraints103.gridy = 3;

            final GridBagConstraints gridBagConstraints104 = new GridBagConstraints();
            gridBagConstraints104.gridx = 4;
            gridBagConstraints104.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints104.insets = new Insets(1,1,1,5);
            gridBagConstraints104.fill = GridBagConstraints.NONE;
            gridBagConstraints104.weighty = 1.0;
            gridBagConstraints104.gridy = 3;

            final GridBagConstraints gridBagConstraints91 = new GridBagConstraints();
            gridBagConstraints91.gridx = -1;
            gridBagConstraints91.gridy = -1;
            final GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints2.gridy = 2;
            gridBagConstraints2.weightx = 1.0;
            gridBagConstraints2.gridwidth = 1;
            gridBagConstraints2.insets = new Insets(1,5,1,5);
            gridBagConstraints2.gridx = 1;
            gridBagConstraints2.gridwidth = 4;
            final GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            gridBagConstraints11.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints11.gridy = 0;
            gridBagConstraints11.weightx = 1.0;
            gridBagConstraints11.gridwidth = 1;
            gridBagConstraints11.insets = new Insets(1,5,1,5);
            gridBagConstraints11.gridx = 1;
            gridBagConstraints11.gridwidth = 4;
            final GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.gridx = 0;
            gridBagConstraints1.insets = new Insets(1,5,1,0);
            gridBagConstraints1.anchor = GridBagConstraints.WEST;
            gridBagConstraints1.gridy = 2;
            Lcontent = new JLabel();
            final GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.insets = new Insets(1,5,1,0);
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.gridy = 0;
            Lsender = new JLabel();
            Psearch = new JPanel();
            Psearch.setLayout(new GridBagLayout());
            Psearch.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3,3,3,3), BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));
            Psearch.add(Lsender, gridBagConstraints);
            Psearch.add(Lcontent, gridBagConstraints1);
            Psearch.add(getSearch_CBprivateMsgsOnly(), gridBagConstraints101);
            Psearch.add(getSearch_CBflaggedMsgsOnly(), gridBagConstraints102);
            Psearch.add(getSearch_CBstarredMsgsOnly(), gridBagConstraints103);
            Psearch.add(getSearch_CBrepliedMsgsOnly(), gridBagConstraints104);
            Psearch.add(Lsubject, gridBagConstraints110);

            JPanel dummyPanel;

            dummyPanel = new JPanel();
            dummyPanel.setLayout(new BoxLayout(dummyPanel, BoxLayout.X_AXIS));
            dummyPanel.add(getSearch_TFsender());
            dummyPanel.add(senderCaseCheckBox);
            Psearch.add(dummyPanel, gridBagConstraints11);

            dummyPanel = new JPanel();
            dummyPanel.setLayout(new BoxLayout(dummyPanel, BoxLayout.X_AXIS));
            dummyPanel.add(getSearch_TFcontent());
            dummyPanel.add(contentCaseCheckBox);
            Psearch.add(dummyPanel, gridBagConstraints2);

            dummyPanel = new JPanel();
            dummyPanel.setLayout(new BoxLayout(dummyPanel, BoxLayout.X_AXIS));
            dummyPanel.add(getSearch_TFsubject());
            dummyPanel.add(subjectCaseCheckBox);
            Psearch.add(dummyPanel, gridBagConstraints29);
        }
        return Psearch;
    }

    /**
     * This method initializes jPanel2
     *
     * @return JPanel
     */
    private JPanel getPsearchResult() {
        if( PsearchResult == null ) {
            final GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            gridBagConstraints6.gridx = 1;
            gridBagConstraints6.anchor = GridBagConstraints.EAST;
            gridBagConstraints6.insets = new Insets(1,5,1,5);
            gridBagConstraints6.gridy = 0;
            LresultCount = new JLabel("");
            final GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.fill = GridBagConstraints.BOTH;
            gridBagConstraints4.gridy = 1;
            gridBagConstraints4.ipadx = 239;
            gridBagConstraints4.ipady = 0;
            gridBagConstraints4.weightx = 1.0;
            gridBagConstraints4.weighty = 1.0;
            gridBagConstraints4.insets = new Insets(1,5,1,5);
            gridBagConstraints4.gridwidth = 2;
            gridBagConstraints4.gridx = 0;
            final GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.ipadx = 0;
            gridBagConstraints3.insets = new Insets(1,5,1,5);
            gridBagConstraints3.anchor = GridBagConstraints.WEST;
            gridBagConstraints3.gridy = 0;
            LsearchResult = new JLabel();
            PsearchResult = new JPanel();
            PsearchResult.setLayout(new GridBagLayout());
            PsearchResult.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3,3,3,3), BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));
            PsearchResult.add(LsearchResult, gridBagConstraints3);
            PsearchResult.add(getJScrollPane(), gridBagConstraints4);
            PsearchResult.add(LresultCount, gridBagConstraints6);
        }
        return PsearchResult;
    }

    /**
     * This method initializes jTextField
     *
     * @return JTextField
     */
    private JTextField getSearch_TFsender() {
        if( search_TFsender == null ) {
            search_TFsender = new JTextField();
            new TextComponentClipboardMenu(search_TFsender, language);
        }
        return search_TFsender;
    }

    /**
     * This method initializes jTextField1
     *
     * @return JTextField
     */
    private JTextField getSearch_TFcontent() {
        if( search_TFcontent == null ) {
            search_TFcontent = new JTextField();
            new TextComponentClipboardMenu(search_TFcontent, language);
        }
        return search_TFcontent;
    }

    /**
     * This method initializes jPanel1
     *
     * @return JPanel
     */
    private JPanel getPdate() {
        if( Pdate == null ) {
            final GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
            gridBagConstraints7.gridx = 0;
            gridBagConstraints7.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints7.insets = new Insets(1,5,0,5);
            gridBagConstraints7.gridy = 1;
            final GridBagConstraints gridBagConstraints15 = new GridBagConstraints();
            gridBagConstraints15.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints15.insets = new Insets(1,5,1,5);
            gridBagConstraints15.gridwidth = 3;
            gridBagConstraints15.gridx = 1;
            gridBagConstraints15.gridy = 3;
            gridBagConstraints15.weightx = 1.0;
            gridBagConstraints15.fill = GridBagConstraints.NONE;
            final GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
            gridBagConstraints14.insets = new Insets(1,5,1,5);
            gridBagConstraints14.gridy = 3;
            gridBagConstraints14.weighty = 1.0;
            gridBagConstraints14.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints14.gridx = 0;
            final GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
            gridBagConstraints13.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints13.insets = new Insets(1,5,0,5);
            gridBagConstraints13.gridx = 3;
            gridBagConstraints13.gridy = 2;
            gridBagConstraints13.weightx = 0.0;
            gridBagConstraints13.fill = GridBagConstraints.NONE;
            final GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
            gridBagConstraints12.anchor = GridBagConstraints.WEST;
            gridBagConstraints12.gridx = 2;
            gridBagConstraints12.gridy = 2;
            gridBagConstraints12.insets = new Insets(1,2,0,2);
            date_Lto = new JLabel();
            final GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
            gridBagConstraints10.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints10.insets = new Insets(1,5,0,5);
            gridBagConstraints10.gridx = 1;
            gridBagConstraints10.gridy = 2;
            gridBagConstraints10.weightx = 0.0;
            gridBagConstraints10.fill = GridBagConstraints.NONE;
            final GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
            gridBagConstraints9.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints9.gridx = 0;
            gridBagConstraints9.gridy = 2;
            gridBagConstraints9.insets = new Insets(1,5,0,5);
            final GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            gridBagConstraints5.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints5.gridwidth = 4;
            gridBagConstraints5.gridx = 0;
            gridBagConstraints5.gridy = 0;
            gridBagConstraints5.insets = new Insets(1,5,0,5);
            Pdate = new JPanel();
            Pdate.setLayout(new GridBagLayout());
            Pdate.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3,3,3,3), BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));
            Pdate.add(getDate_RBdisplayed(), gridBagConstraints5);
            Pdate.add(getDate_RBbetweenDates(), gridBagConstraints9);
            Pdate.add(getDate_TFstartDate(), gridBagConstraints10);
            Pdate.add(date_Lto, gridBagConstraints12);
            Pdate.add(getDate_TFendDate(), gridBagConstraints13);
            Pdate.add(getDate_RBdaysBackward(), gridBagConstraints14);
            Pdate.add(getDate_TFdaysBackward(), gridBagConstraints15);
            Pdate.add(getDate_RBall(), gridBagConstraints7);
        }
        return Pdate;
    }

    /**
     * This method initializes jRadioButton
     *
     * @return JRadioButton
     */
    private JRadioButton getDate_RBdisplayed() {
        if( date_RBdisplayed == null ) {
            date_RBdisplayed = new JRadioButton();
            date_RBdisplayed.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    date_RBitemStateChanged();
                }
            });
        }
        return date_RBdisplayed;
    }

    /**
     * This method initializes jRadioButton1
     *
     * @return JRadioButton
     */
    private JRadioButton getDate_RBbetweenDates() {
        if( date_RBbetweenDates == null ) {
            date_RBbetweenDates = new JRadioButton();
            date_RBbetweenDates.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    date_RBitemStateChanged();
                }
            });
        }
        return date_RBbetweenDates;
    }

    /**
     * This method initializes jTextField3
     *
     * @return JTextField
     */
    private JDateChooser getDate_TFstartDate() {
        if( date_TFstartDate == null ) {
            date_TFstartDate = new JDateChooser();
        }
        return date_TFstartDate;
    }

    /**
     * This method initializes jTextField4
     *
     * @return JTextField
     */
    private JDateChooser getDate_TFendDate() {
        if( date_TFendDate == null ) {
            date_TFendDate = new JDateChooser();
        }
        return date_TFendDate;
    }

    /**
     * This method initializes jRadioButton2
     *
     * @return JRadioButton
     */
    private JRadioButton getDate_RBdaysBackward() {
        if( date_RBdaysBackward == null ) {
            date_RBdaysBackward = new JRadioButton();
            date_RBdaysBackward.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    date_RBitemStateChanged();
                }
            });
        }
        return date_RBdaysBackward;
    }

    private void date_RBitemStateChanged() {
        if( getDate_RBdisplayed().isSelected() ) {
            getDate_TFdaysBackward().setEnabled(false);
            getDate_TFendDate().setEnabled(false);
            getDate_TFstartDate().setEnabled(false);
        } else if( getDate_RBbetweenDates().isSelected() ) {
            getDate_TFdaysBackward().setEnabled(false);
            getDate_TFendDate().setEnabled(true);
            getDate_TFstartDate().setEnabled(true);
        } else if( getDate_RBdaysBackward().isSelected() ) {
            getDate_TFdaysBackward().setEnabled(true);
            getDate_TFendDate().setEnabled(false);
            getDate_TFstartDate().setEnabled(false);
        }
    }

    /**
     * This method initializes jTextField5
     *
     * @return JTextField
     */
    private JTextField getDate_TFdaysBackward() {
        if( date_TFdaysBackward == null ) {
            date_TFdaysBackward = new JTextField();
            date_TFdaysBackward.setColumns(6);
            date_TFdaysBackward.setDocument(new WholeNumberDocument());
        }
        return date_TFdaysBackward;
    }

    /**
     * This method initializes jPanel4
     *
     * @return JPanel
     */
    private JPanel getPtrustState() {
        if( PtrustState == null ) {
            final GridBagConstraints gridBagConstraints25 = new GridBagConstraints();
            gridBagConstraints25.anchor = GridBagConstraints.WEST;
            gridBagConstraints25.insets = new Insets(0,25,0,0);
            gridBagConstraints25.gridwidth = 3;
            gridBagConstraints25.gridx = 0;
            gridBagConstraints25.gridy = 3;
            gridBagConstraints25.weightx = 1.0;
            gridBagConstraints25.weighty = 1.0;
            gridBagConstraints25.fill = GridBagConstraints.NONE;
            final GridBagConstraints gridBagConstraints18 = new GridBagConstraints();
            gridBagConstraints18.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints18.gridx = 0;
            gridBagConstraints18.gridy = 2;
            gridBagConstraints18.insets = new Insets(1,5,0,5);
            final GridBagConstraints gridBagConstraints17 = new GridBagConstraints();
            gridBagConstraints17.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints17.gridx = 0;
            gridBagConstraints17.gridy = 1;
            gridBagConstraints17.insets = new Insets(1,5,0,5);
            final GridBagConstraints gridBagConstraints16 = new GridBagConstraints();
            gridBagConstraints16.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints16.insets = new Insets(1,5,0,5);
            gridBagConstraints16.gridx = 0;
            gridBagConstraints16.gridy = 0;
            gridBagConstraints16.fill = GridBagConstraints.NONE;
            PtrustState = new JPanel();
            PtrustState.setLayout(new GridBagLayout());
            PtrustState.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3,3,3,3), BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));
            PtrustState.add(getTruststate_RBdisplayed(), gridBagConstraints16);
            PtrustState.add(getTruststate_RBall(), gridBagConstraints17);
            PtrustState.add(getTruststate_RBchosed(), gridBagConstraints18);
            PtrustState.add(getTruststate_PtrustStates(), gridBagConstraints25);
        }
        return PtrustState;
    }

    /**
     * This method initializes jRadioButton3
     *
     * @return JRadioButton
     */
    private JRadioButton getTruststate_RBdisplayed() {
        if( truststate_RBdisplayed == null ) {
            truststate_RBdisplayed = new JRadioButton();
            truststate_RBdisplayed.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    trustState_RBitemStateChanged();
                }
            });
        }
        return truststate_RBdisplayed;
    }

    private void trustState_RBitemStateChanged() {
        boolean enableTtrustStatesPanel;
        if( getTruststate_RBchosed().isSelected() ) {
            enableTtrustStatesPanel = true;
        } else {
            enableTtrustStatesPanel = false;
        }
        final Component[] comps = getTruststate_PtrustStates().getComponents();
        for( final Component element : comps ) {
            element.setEnabled(enableTtrustStatesPanel);
        }
    }

    /**
     * This method initializes jRadioButton4
     *
     * @return JRadioButton
     */
    private JRadioButton getTruststate_RBall() {
        if( truststate_RBall == null ) {
            truststate_RBall = new JRadioButton();
            truststate_RBall.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    trustState_RBitemStateChanged();
                }
            });
        }
        return truststate_RBall;
    }

    /**
     * This method initializes jRadioButton5
     *
     * @return JRadioButton
     */
    private JRadioButton getTruststate_RBchosed() {
        if( truststate_RBchosed == null ) {
            truststate_RBchosed = new JRadioButton();
            truststate_RBchosed.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    trustState_RBitemStateChanged();
                }
            });
        }
        return truststate_RBchosed;
    }

    /**
     * This method initializes jPanel5
     *
     * @return JPanel
     */
    private JPanel getTruststate_PtrustStates() {
        if( truststate_PtrustStates == null ) {
            final GridBagConstraints gridBagConstraints24 = new GridBagConstraints();
            gridBagConstraints24.fill = GridBagConstraints.NONE;
            gridBagConstraints24.gridx = 5;
            gridBagConstraints24.gridy = 0;
            gridBagConstraints24.weightx = 0.0;
            gridBagConstraints24.insets = new Insets(1,5,1,5);
            final GridBagConstraints gridBagConstraints23 = new GridBagConstraints();
            gridBagConstraints23.insets = new Insets(1,5,1,5);
            gridBagConstraints23.gridy = 0;
            gridBagConstraints23.gridx = 4;
            final GridBagConstraints gridBagConstraints22 = new GridBagConstraints();
            gridBagConstraints22.insets = new Insets(1,5,1,5);
            gridBagConstraints22.gridy = 0;
            gridBagConstraints22.gridx = 3;
            final GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
            gridBagConstraints21.insets = new Insets(1,5,1,5);
            gridBagConstraints21.gridy = 0;
            gridBagConstraints21.gridx = 2;
            final GridBagConstraints gridBagConstraints20 = new GridBagConstraints();
            gridBagConstraints20.insets = new Insets(1,5,1,5);
            gridBagConstraints20.gridy = 0;
            gridBagConstraints20.gridx = 1;
            final GridBagConstraints gridBagConstraints19 = new GridBagConstraints();
            gridBagConstraints19.anchor = GridBagConstraints.CENTER;
            gridBagConstraints19.gridx = 0;
            gridBagConstraints19.gridy = 0;
            gridBagConstraints19.insets = new Insets(1,0,1,5);
            truststate_PtrustStates = new JPanel();
            truststate_PtrustStates.setLayout(new GridBagLayout());
            truststate_PtrustStates.add(getTruststate_CBgood(), gridBagConstraints19);
            truststate_PtrustStates.add(getTruststate_CBobserve(), gridBagConstraints20);
            truststate_PtrustStates.add(getTruststate_CBcheck(), gridBagConstraints21);
            truststate_PtrustStates.add(getTruststate_CBbad(), gridBagConstraints22);
            truststate_PtrustStates.add(getTruststate_CBnone(), gridBagConstraints23);
            truststate_PtrustStates.add(getTruststate_CBtampered(), gridBagConstraints24);
        }
        return truststate_PtrustStates;
    }

    /**
     * This method initializes jCheckBox
     *
     * @return JCheckBox
     */
    private JCheckBox getTruststate_CBgood() {
        if( truststate_CBgood == null ) {
            truststate_CBgood = new JCheckBox();
        }
        return truststate_CBgood;
    }

    /**
     * This method initializes jCheckBox1
     *
     * @return JCheckBox
     */
    private JCheckBox getTruststate_CBobserve() {
        if( truststate_CBobserve == null ) {
            truststate_CBobserve = new JCheckBox();
        }
        return truststate_CBobserve;
    }

    /**
     * This method initializes jCheckBox2
     *
     * @return JCheckBox
     */
    private JCheckBox getTruststate_CBcheck() {
        if( truststate_CBcheck == null ) {
            truststate_CBcheck = new JCheckBox();
        }
        return truststate_CBcheck;
    }

    /**
     * This method initializes jCheckBox3
     *
     * @return JCheckBox
     */
    private JCheckBox getTruststate_CBbad() {
        if( truststate_CBbad == null ) {
            truststate_CBbad = new JCheckBox();
        }
        return truststate_CBbad;
    }

    /**
     * This method initializes jCheckBox4
     *
     * @return JCheckBox
     */
    private JCheckBox getTruststate_CBnone() {
        if( truststate_CBnone == null ) {
            truststate_CBnone = new JCheckBox();
        }
        return truststate_CBnone;
    }

    /**
     * This method initializes jCheckBox5
     *
     * @return JCheckBox
     */
    private JCheckBox getTruststate_CBtampered() {
        if( truststate_CBtampered == null ) {
            truststate_CBtampered = new JCheckBox();
        }
        return truststate_CBtampered;
    }

    /**
     * This method initializes jPanel3
     *
     * @return JPanel
     */
    private JPanel getParchive() {
        if( Parchive == null ) {
            final GridBagConstraints gridBagConstraints28 = new GridBagConstraints();
            gridBagConstraints28.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints28.gridx = 0;
            gridBagConstraints28.gridy = 2;
            gridBagConstraints28.fill = GridBagConstraints.NONE;
            gridBagConstraints28.weighty = 1.0;
            gridBagConstraints28.insets = new Insets(3,5,1,5);
            final GridBagConstraints gridBagConstraints27 = new GridBagConstraints();
            gridBagConstraints27.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints27.gridx = 0;
            gridBagConstraints27.gridy = 1;
            gridBagConstraints27.fill = GridBagConstraints.NONE;
            gridBagConstraints27.weightx = 1.0;
            gridBagConstraints27.insets = new Insets(3,5,1,5);
            final GridBagConstraints gridBagConstraints26 = new GridBagConstraints();
            gridBagConstraints26.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints26.insets = new Insets(3,5,1,5);
            gridBagConstraints26.gridx = 0;
            gridBagConstraints26.gridy = 0;
            gridBagConstraints26.weighty = 0.0;
            gridBagConstraints26.fill = GridBagConstraints.NONE;
            Parchive = new JPanel();
            Parchive.setLayout(new GridBagLayout());
            Parchive.setPreferredSize(new Dimension(517,25));
            Parchive.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3,3,3,3), BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));
            Parchive.add(getArchive_RBkeypoolOnly(), gridBagConstraints27);
            Parchive.add(getArchive_RBarchiveOnly(), gridBagConstraints28);
            Parchive.add(getArchive_RBkeypoolAndArchive(), gridBagConstraints26);
        }
        return Parchive;
    }

    /**
     * This method initializes jRadioButton6
     *
     * @return JRadioButton
     */
    private JRadioButton getArchive_RBkeypoolAndArchive() {
        if( archive_RBkeypoolAndArchive == null ) {
            archive_RBkeypoolAndArchive = new JRadioButton();
            archive_RBkeypoolAndArchive.setPreferredSize(new Dimension(195,20));
        }
        return archive_RBkeypoolAndArchive;
    }

    /**
     * This method initializes jRadioButton7
     *
     * @return JRadioButton
     */
    private JRadioButton getArchive_RBkeypoolOnly() {
        if( archive_RBkeypoolOnly == null ) {
            archive_RBkeypoolOnly = new JRadioButton();
            archive_RBkeypoolOnly.setPreferredSize(new Dimension(152,20));
        }
        return archive_RBkeypoolOnly;
    }

    /**
     * This method initializes jRadioButton8
     *
     * @return JRadioButton
     */
    private JRadioButton getArchive_RBarchiveOnly() {
        if( archive_RBarchiveOnly == null ) {
            archive_RBarchiveOnly = new JRadioButton();
            archive_RBarchiveOnly.setPreferredSize(new Dimension(150,20));
        }
        return archive_RBarchiveOnly;
    }

    /**
     * This method initializes jPanel6
     *
     * @return JPanel
     */
    private JPanel getPboards() {
        if( Pboards == null ) {
            final GridBagConstraints gridBagConstraints35 = new GridBagConstraints();
            gridBagConstraints35.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints35.insets = new Insets(1,25,1,5);
            gridBagConstraints35.gridwidth = 2;
            gridBagConstraints35.gridx = 0;
            gridBagConstraints35.gridy = 3;
            gridBagConstraints35.weightx = 1.0;
            gridBagConstraints35.weighty = 1.0;
            gridBagConstraints35.fill = GridBagConstraints.HORIZONTAL;
            final GridBagConstraints gridBagConstraints34 = new GridBagConstraints();
            gridBagConstraints34.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints34.gridx = 1;
            gridBagConstraints34.gridy = 2;
            gridBagConstraints34.insets = new Insets(1,5,0,5);
            final GridBagConstraints gridBagConstraints33 = new GridBagConstraints();
            gridBagConstraints33.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints33.gridx = 0;
            gridBagConstraints33.gridy = 2;
            gridBagConstraints33.insets = new Insets(1,5,0,5);
            final GridBagConstraints gridBagConstraints32 = new GridBagConstraints();
            gridBagConstraints32.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints32.gridwidth = 2;
            gridBagConstraints32.gridx = 0;
            gridBagConstraints32.gridy = 1;
            gridBagConstraints32.insets = new Insets(1,5,0,5);
            final GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
            gridBagConstraints31.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints31.gridwidth = 2;
            gridBagConstraints31.gridx = 0;
            gridBagConstraints31.gridy = 0;
            gridBagConstraints31.insets = new Insets(1,5,0,5);
            Pboards = new JPanel();
            Pboards.setLayout(new GridBagLayout());
            Pboards.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3,3,3,3), BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));
            Pboards.add(getBoards_RBdisplayed(), gridBagConstraints31);
//            Pboards.add(getBoards_RBallExisting(), gridBagConstraints32);
            Pboards.add(getBoards_RBchosed(), gridBagConstraints33);
            Pboards.add(getBoards_Bchoose(), gridBagConstraints34);
            Pboards.add(getBoards_TFchosedBoards(), gridBagConstraints35);
        }
        return Pboards;
    }

    /**
     * This method initializes jRadioButton9
     *
     * @return JRadioButton
     */
    private JRadioButton getBoards_RBdisplayed() {
        if( boards_RBdisplayed == null ) {
            boards_RBdisplayed = new JRadioButton();
            boards_RBdisplayed.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    boards_RBitemStateChanged();
                }
            });
        }
        return boards_RBdisplayed;
    }

    /**
     * This method initializes jRadioButton10
     *
     * @return JRadioButton
     */
//    private JRadioButton getBoards_RBallExisting() {
//        if( boards_RBallExisting == null ) {
//            boards_RBallExisting = new JRadioButton();
//            boards_RBallExisting.setText("Search in all existing board directories");
//            boards_RBallExisting.addItemListener(new ItemListener() {
//                public void itemStateChanged(ItemEvent e) {
//                    boards_RBitemStateChanged();
//                }
//            });
//        }
//        return boards_RBallExisting;
//    }

    /**
     * This method initializes jRadioButton11
     *
     * @return JRadioButton
     */
    private JRadioButton getBoards_RBchosed() {
        if( boards_RBchosed == null ) {
            boards_RBchosed = new JRadioButton();
            boards_RBchosed.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    boards_RBitemStateChanged();
                }
            });
        }
        return boards_RBchosed;
    }

    private void boards_RBitemStateChanged() {
        boolean enableChooseControls;
        if( getBoards_RBchosed().isSelected() ) {
            enableChooseControls = true;
        } else {
            enableChooseControls = false;
        }
        getBoards_Bchoose().setEnabled(enableChooseControls);
        getBoards_TFchosedBoards().setEnabled(enableChooseControls);
    }

    /**
     * This method initializes jButton1
     *
     * @return JButton
     */
    private JButton getBoards_Bchoose() {
        if( boards_Bchoose == null ) {
            boards_Bchoose = new JButton();
            boards_Bchoose.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    chooseBoards();
                }
            });
        }
        return boards_Bchoose;
    }

    /**
     * This method initializes jTextField6
     *
     * @return JTextField
     */
    private JTextField getBoards_TFchosedBoards() {
        if( boards_TFchosedBoards == null ) {
            boards_TFchosedBoards = new JTextField();
            boards_TFchosedBoards.setText("");
            boards_TFchosedBoards.setEditable(false);
        }
        return boards_TFchosedBoards;
    }

    /**
     * This method initializes jCheckBox6
     *
     * @return JCheckBox
     */
    private TristateCheckBox getSearch_CBprivateMsgsOnly() {
        if( search_CBprivateMsgsOnly == null ) {
            search_CBprivateMsgsOnly = new TristateCheckBox();
        }
        return search_CBprivateMsgsOnly;
    }

    private TristateCheckBox getSearch_CBflaggedMsgsOnly() {
        if( search_CBflaggedMsgsOnly == null ) {
            search_CBflaggedMsgsOnly = new TristateCheckBox();
        }
        return search_CBflaggedMsgsOnly;
    }

    private TristateCheckBox getSearch_CBstarredMsgsOnly() {
        if( search_CBstarredMsgsOnly == null ) {
            search_CBstarredMsgsOnly = new TristateCheckBox();
        }
        return search_CBstarredMsgsOnly;
    }

    private TristateCheckBox getSearch_CBrepliedMsgsOnly() {
        if( search_CBrepliedMsgsOnly == null ) {
            search_CBrepliedMsgsOnly = new TristateCheckBox();
        }
        return search_CBrepliedMsgsOnly;
    }

    /**
     * This method initializes jScrollPane
     *
     * @return JScrollPane
     */
    private JScrollPane getJScrollPane() {
        if( jScrollPane == null ) {
            jScrollPane = new JScrollPane();
            jScrollPane.setWheelScrollingEnabled(true);
            jScrollPane.setForeground(new Color(51,51,51));
            jScrollPane.setViewportView(getSearchResultTable());
        }
        return jScrollPane;
    }

    /**
     * This method initializes jTable
     *
     * @return JTable
     */
    private SearchMessagesResultTable getSearchResultTable() {
        if( searchResultTable == null ) {
            searchResultTable = new SearchMessagesResultTable(getSearchMessagesTableModel());
            searchResultTable.setAutoCreateColumnsFromModel(true);
            searchResultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            searchResultTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if(SwingUtilities.isLeftMouseButton(e) && (e.getClickCount() == 2)) {
                        openSelectedMessage();
                    }
                }
            });

            // enter should not jump to next message, but open the selected msg (if any)
            searchResultTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "openMessage");
			final Action action = new AbstractAction() {

				private static final long serialVersionUID = 1L;

				public void actionPerformed(final ActionEvent arg0) {
                    if( getBopenMsg().isEnabled() ) {
                        openSelectedMessage();
                    }
                }
            };
            searchResultTable.getActionMap().put("openMessage", action);
        }
        return searchResultTable;
    }

    /**
     * This method initializes searchMessagesTableModel
     *
     * @return frost.gui.model.SearchMessagesTableModel
     */
    private SearchMessagesTableModel getSearchMessagesTableModel() {
        if( searchMessagesTableModel == null ) {
            searchMessagesTableModel = new SearchMessagesTableModel();
        }
        return searchMessagesTableModel;
    }

    /**
     * This method initializes buttonGroup
     *
     * @return ButtonGroup
     */
    private ButtonGroup getBoards_buttonGroup() {
        if( boards_buttonGroup == null ) {
            boards_buttonGroup = new ButtonGroup();
            boards_buttonGroup.add(getBoards_RBdisplayed());
            boards_buttonGroup.add(getBoards_RBchosed());
//            boards_buttonGroup.add(getBoards_RBallExisting());
        }
        return boards_buttonGroup;
    }

    /**
     * This method initializes date_buttonGroup
     *
     * @return ButtonGroup
     */
    private ButtonGroup getDate_buttonGroup() {
        if( date_buttonGroup == null ) {
            date_buttonGroup = new ButtonGroup();
            date_buttonGroup.add(getDate_RBbetweenDates());
            date_buttonGroup.add(getDate_RBdaysBackward());
            date_buttonGroup.add(getDate_RBdisplayed());
            date_buttonGroup.add(getDate_RBall());
        }
        return date_buttonGroup;
    }

    /**
     * This method initializes truststate_buttonGroup
     *
     * @return ButtonGroup
     */
    private ButtonGroup getTruststate_buttonGroup() {
        if( truststate_buttonGroup == null ) {
            truststate_buttonGroup = new ButtonGroup();
            truststate_buttonGroup.add(getTruststate_RBdisplayed());
            truststate_buttonGroup.add(getTruststate_RBall());
            truststate_buttonGroup.add(getTruststate_RBchosed());
        }
        return truststate_buttonGroup;
    }

    /**
     * This method initializes archive_buttonGroup
     *
     * @return ButtonGroup
     */
    private ButtonGroup getArchive_buttonGroup() {
        if( archive_buttonGroup == null ) {
            archive_buttonGroup = new ButtonGroup();
            archive_buttonGroup.add(getArchive_RBkeypoolOnly());
            archive_buttonGroup.add(getArchive_RBarchiveOnly());
            archive_buttonGroup.add(getArchive_RBkeypoolAndArchive());
        }
        return archive_buttonGroup;
    }

    private void chooseBoards() {

        // get and sort all boards
        final List<Board> allBoards = MainFrame.getInstance().getMessagingTab().getTofTreeModel().getAllBoards();
        if (allBoards.size() == 0) {
            JOptionPane.showMessageDialog(this,
                    language.getString("SearchMessages.errorDialogs.noBoardsToChoose"),
                    language.getString("SearchMessages.errorDialogs.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        Collections.sort(allBoards);

        final BoardsChooser bc = new BoardsChooser(this, allBoards, chosedBoardsList);

        final List<Board> resultBoards = bc.runDialog();
        if( resultBoards != null ) {
            chosedBoardsList = resultBoards;
            updateBoardTextField(chosedBoardsList);
        }
    }

    private void updateBoardTextField(final List<Board> boards) {
        final StringBuilder txt = new StringBuilder();
        if( boards != null ) {
            for(final Iterator<Board> i=boards.iterator(); i.hasNext(); ) {
                final Board b = i.next();
                txt.append(b.getName());
                if( i.hasNext() ) {
                    txt.append("; ");
                }
            }
            chosedBoardsList = boards;
        } else {
            chosedBoardsList = Collections.emptyList();
        }
        getBoards_TFchosedBoards().setText(txt.toString());
    }

    private void initializeWithDefaults() {

        getBoards_RBdisplayed().doClick();
        getDate_RBdisplayed().doClick();
        getTruststate_RBdisplayed().doClick();
        getArchive_RBkeypoolAndArchive().doClick();

        getDate_TFdaysBackward().setText("0");
    }

    private SearchMessagesConfig getSearchConfig() {

        final SearchMessagesConfig scfg = new SearchMessagesConfig();

        scfg.setSenderString(getSearch_TFsender().getText().trim(), !senderCaseCheckBox.isSelected());
        scfg.setSubjectString(getSearch_TFsubject().getText().trim(), !subjectCaseCheckBox.isSelected());
        scfg.setContentString(getSearch_TFcontent().getText().trim(), !contentCaseCheckBox.isSelected());

        scfg.searchPrivateMsgsOnly = getSearch_CBprivateMsgsOnly().getBooleanState();
        scfg.searchFlaggedMsgsOnly = getSearch_CBflaggedMsgsOnly().getBooleanState();
        scfg.searchStarredMsgsOnly = getSearch_CBstarredMsgsOnly().getBooleanState();
        scfg.searchRepliedMsgsOnly = getSearch_CBrepliedMsgsOnly().getBooleanState();

        if( getBoards_RBdisplayed().isSelected() ) {
            scfg.searchBoards = SearchMessagesConfig.BOARDS_DISPLAYED;
//        } else if( getBoards_RBallExisting().isSelected() ) {
//            scfg.searchBoards = SearchConfig.BOARDS_EXISTING_DIRS;
        } else if( getBoards_RBchosed().isSelected() ) {
            if( chosedBoardsList.size() == 0 ) {
                JOptionPane.showMessageDialog(this,
                        language.getString("SearchMessages.errorDialogs.noBoardsChosen"),
                        language.getString("SearchMessages.errorDialogs.title"),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
            scfg.searchBoards = SearchMessagesConfig.BOARDS_CHOSED;
            scfg.chosedBoards = chosedBoardsList;
        }

        if( getDate_RBdisplayed().isSelected() ) {
            scfg.searchDates = SearchMessagesConfig.DATE_DISPLAYED;
        } else if( getDate_RBall().isSelected() ) {
            scfg.searchDates = SearchMessagesConfig.DATE_ALL;
        } else if( getDate_RBbetweenDates().isSelected() ) {
            scfg.searchDates = SearchMessagesConfig.DATE_BETWEEN_DATES;
            try {
                // NOTE: getDate() can be null when field is empty. NPE is catched below.
                scfg.startDate = getDate_TFstartDate().getDate().getTime();
                scfg.endDate = getDate_TFendDate().getDate().getTime();

                // check start before end
                if( scfg.startDate > scfg.endDate ) {
                    JOptionPane.showMessageDialog(this,
                            language.getString("SearchMessages.errorDialogs.startDateIsAfterEndDate"),
                            language.getString("SearchMessages.errorDialogs.title"),
                            JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            } catch(final Exception ex) {
                JOptionPane.showMessageDialog(this,
                        language.getString("SearchMessages.errorDialogs.invalidStartOrEndDate"),
                        language.getString("SearchMessages.errorDialogs.title"),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
        } else if( getDate_RBdaysBackward().isSelected() ) {
            scfg.searchDates = SearchMessagesConfig.DATE_DAYS_BACKWARD;
            try {
                scfg.daysBackward = Integer.parseInt(getDate_TFdaysBackward().getText());
            } catch(final NumberFormatException ex) { } // never happens, we allow only digits in textfield!
        }

        if( getTruststate_RBdisplayed().isSelected() ) {
            scfg.searchTruststates = SearchMessagesConfig.TRUST_DISPLAYED;
        } else if( getTruststate_RBall().isSelected() ) {
            scfg.searchTruststates = SearchMessagesConfig.TRUST_ALL;
        } else if( getTruststate_RBchosed().isSelected() ) {
            scfg.searchTruststates = SearchMessagesConfig.TRUST_CHOSED;
            scfg.trust_good = getTruststate_CBgood().isSelected();
            scfg.trust_observe = getTruststate_CBobserve().isSelected();
            scfg.trust_check = getTruststate_CBcheck().isSelected();
            scfg.trust_bad = getTruststate_CBbad().isSelected();
            scfg.trust_none = getTruststate_CBnone().isSelected();
            scfg.trust_tampered = getTruststate_CBtampered().isSelected();

            if( !scfg.trust_good && !scfg.trust_observe && !scfg.trust_check &&
                !scfg.trust_bad && !scfg.trust_none && !scfg.trust_tampered )
            {
                JOptionPane.showMessageDialog(this,
                        language.getString("SearchMessages.errorDialogs.noTrustStateSelected"),
                        language.getString("SearchMessages.errorDialogs.title"),
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }

        if( getArchive_RBkeypoolOnly().isSelected() ) {
            scfg.searchInKeypool = true;
            scfg.searchInArchive = false;
        } else if( getArchive_RBarchiveOnly().isSelected() ) {
            scfg.searchInKeypool = false;
            scfg.searchInArchive = true;
        } else if( getArchive_RBkeypoolAndArchive().isSelected() ) {
            scfg.searchInKeypool = true;
            scfg.searchInArchive = true;
        }

        scfg.msgMustContainBoards = getAttachment_CBmustContainBoards().isSelected();
        scfg.msgMustContainFiles = getAttachment_CBmustContainFiles().isSelected();

        return scfg;
    }

    /**
     * When window is about to close, do same as if CANCEL was pressed.
     */
    @Override
    protected void processWindowEvent(final WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            closePressed();
        } else {
            super.processWindowEvent(e);
        }
    }

    private SearchMessagesThread getRunningSearchThread() {
        return runningSearchThread;
    }

    private void setRunningSearchThread(final SearchMessagesThread t) {
        runningSearchThread = t;
    }

    /**
     * Disables all input panels during run of search, remembers disabled
     * components for later re-enabling.
     *
     */
    private void disableInputPanels() {
        previouslyEnabledComponents.clear();
        for(int x=0; x < getJTabbedPane().getTabCount(); x++ ) {
            final JPanel c = (JPanel)getJTabbedPane().getComponentAt(x);
            disableInputPanels(c);
        }
    }
    private void disableInputPanels(final Container c) {
        final Component[] cs = c.getComponents();
        for( final Component element : cs ) {
            if( element instanceof Container ) {
                disableInputPanels((Container)element);
            }
            if( element.isEnabled() ) {
                previouslyEnabledComponents.add(element);
                element.setEnabled(false);
            }
        }
    }

    /**
     * Re-enables the disabled input panels.
     */
    private void enableInputPanels() {
        for(int x=0; x < getJTabbedPane().getTabCount(); x++ ) {
            final JPanel c = (JPanel)getJTabbedPane().getComponentAt(x);
            enableInputPanels(c);
        }
        previouslyEnabledComponents.clear();
    }

    private void enableInputPanels(final Container c) {
        final Component[] cs = c.getComponents();
        for( final Component element : cs ) {
            if( element instanceof Container ) {
                enableInputPanels((Container)element);
            }
            if( previouslyEnabledComponents.contains(element) ) {
                element.setEnabled(true);
            }
        }
    }

    public void notifySearchThreadFinished() {
        setRunningSearchThread(null);

        enableInputPanels();

        // reset buttons
        getBcancel().setEnabled(true);
        getBsearch().setText(startSearchStr);
    }

    // stop searching or close window
    private void closePressed() {
        if( getRunningSearchThread() != null ) {
            // close not allowed, search must be stopped
            JOptionPane.showMessageDialog(this,
                    language.getString("SearchMessages.errorDialog.stopSearchBeforeClose"),
                    language.getString("SearchMessages.errorDialogs.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        saveWindowState();
        language.removeLanguageListener(this);
        ((JTranslatableTabbedPane)getJTabbedPane()).close();
        setVisible(false);
    }

    private void startOrStopSearching() {

        if( getRunningSearchThread() != null ) {
            // stop search thread, final handling is done in notifySearchThreadFinished()
            getRunningSearchThread().requestStop();
            return;
        }

        searchMessagesConfig = getSearchConfig();
        if( searchMessagesConfig == null ) {
            // invalid cfg
            return;
        }

        clearSearchResultTable();

        // disable all input panels
        disableInputPanels();

        // set button states
        getBcancel().setEnabled(false);
        getBsearch().setText(stopSearchStr);

        getBopenMsg().setEnabled(false);

        setRunningSearchThread(new SearchMessagesThread(this, searchMessagesConfig));
        getRunningSearchThread().setPriority(Thread.MIN_PRIORITY); // low prio
        getRunningSearchThread().start();
    }

    private void clearSearchResultTable() {
        getSearchMessagesTableModel().clearDataModel();
        resultCount = 0;
        updateResultCountLabel(resultCount);
    }

    private void updateResultCountLabel(final int rs) {
        LresultCount.setText(resultCountPrefix + rs);
    }

    /**
     * Called by SearchMessagesThread to add a found message.
     */
    public void addFoundMessage(final FrostSearchResultMessageObject msg) {
        // we were called from io thread
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // add msg to table
                getSearchMessagesTableModel().addRow(msg);
                resultCount++;
                updateResultCountLabel(resultCount);
                if( !getBopenMsg().isEnabled() ) {
                    getBopenMsg().setEnabled(true);
                }
            }
        });
    }

	// TODO: Add 'jump to message in board'. Not for archived messages.
    private void openSelectedMessage() {
        final int row = getSearchResultTable().getSelectedRow();
        if (row < 0) {
            return;
        }
        final FrostSearchResultMessageObject msg = getSearchMessagesTableModel().getRow(row);
        if( msg == null ) {
            return;
        }
        final FrostMessageObject mo = msg.getMessageObject();
        final MessageWindow messageWindow = new MessageWindow( this, mo, this.getSize(), searchMessagesConfig );
        messageWindow.setVisible(true);
    }

	/**
	 * This Document ensures that only digits can be entered into a text field.
	 */
	protected class WholeNumberDocument extends PlainDocument {

		private static final long serialVersionUID = 1L;

		@Override
        public void insertString(final int offs, final String str, final AttributeSet a) throws BadLocationException {
            final char[] source = str.toCharArray();
            final char[] result = new char[source.length];
            int j = 0;

            for( int i = 0; i < result.length; i++ ) {
                if( Character.isDigit(source[i]) ) {
                    result[j++] = source[i];
                }
            }
            super.insertString(offs, new String(result, 0, j), a);
        }
    }

    /**
     * This method initializes date_RBall
     *
     * @return JRadioButton
     */
    private JRadioButton getDate_RBall() {
        if( date_RBall == null ) {
            date_RBall = new JRadioButton();
        }
        return date_RBall;
    }

    private void saveWindowState() {
        final Rectangle bounds = getBounds();
        final boolean isMaximized = ((getExtendedState() & Frame.MAXIMIZED_BOTH) != 0);

        Core.frostSettings.setValue("searchMessagesDialog.lastFrameMaximized", isMaximized);

        if (!isMaximized) { // Only save the current dimension if frame is not maximized
            Core.frostSettings.setValue("searchMessagesDialog.lastFrameHeight", bounds.height);
            Core.frostSettings.setValue("searchMessagesDialog.lastFrameWidth", bounds.width);
            Core.frostSettings.setValue("searchMessagesDialog.lastFramePosX", bounds.x);
            Core.frostSettings.setValue("searchMessagesDialog.lastFramePosY", bounds.y);
        }
    }

    private void loadWindowState() {
        // load size, location and state of window
        int lastHeight = Core.frostSettings.getInteger("searchMessagesDialog.lastFrameHeight");
        int lastWidth = Core.frostSettings.getInteger("searchMessagesDialog.lastFrameWidth");
        final int lastPosX = Core.frostSettings.getInteger("searchMessagesDialog.lastFramePosX");
        final int lastPosY = Core.frostSettings.getInteger("searchMessagesDialog.lastFramePosY");
        final boolean lastMaximized = Core.frostSettings.getBoolean("searchMessagesDialog.lastFrameMaximized");

        if( (lastHeight <= 0) || (lastWidth <= 0) ) {
            // first call
            setSize(700,550);
            setLocationRelativeTo(MainFrame.getInstance());
            return;
        }

        final Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();

        if (lastWidth < 100) {
            lastWidth = 700;
        }
        if (lastHeight < 100) {
            lastHeight = 550;
        }

        if ((lastPosX + lastWidth) > scrSize.width) {
            setSize(700,550);
            setLocationRelativeTo(MainFrame.getInstance());
            return;
        }

        if ((lastPosY + lastHeight) > scrSize.height) {
            setSize(700,550);
            setLocationRelativeTo(MainFrame.getInstance());
            return;
        }

        setBounds(lastPosX, lastPosY, lastWidth, lastHeight);

        if (lastMaximized) {
            setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
        }
    }

    /**
     * This method initializes jPanel
     *
     * @return JPanel
     */
    private JPanel getJPanel() {
        if( PbuttonsRight == null ) {
            PbuttonsRight = new JPanel();
            PbuttonsRight.add(getBsearch(), null);
            PbuttonsRight.add(getBcancel(), null);
        }
        return PbuttonsRight;
    }

    /**
     * This method initializes PbuttonsRight
     *
     * @return JPanel
     */
    private JPanel getPbuttonsRight() {
        if( PbuttonsLeft == null ) {
            PbuttonsLeft = new JPanel();
            PbuttonsLeft.add(getBhelp(), null);
            PbuttonsLeft.add(getBopenMsg(), null);
        }
        return PbuttonsLeft;
    }

    /**
     * This method initializes Bfocus
     *
     * @return JButton
     */
    private JButton getBopenMsg() {
        if( BopenMsg == null ) {
            BopenMsg = new JButton();
            BopenMsg.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    openSelectedMessage();
                }
            });
            BopenMsg.setEnabled(false);
        }
        return BopenMsg;
    }

    public void languageChanged(final LanguageEvent e) {

        resultCountPrefix = language.getString("SearchMessages.label.results") + ": ";
        startSearchStr = language.getString("SearchMessages.button.search");
        stopSearchStr = language.getString("SearchMessages.button.stopSearch");

        if( getRunningSearchThread() != null ) {
            getBsearch().setText(stopSearchStr);
        } else {
            getBsearch().setText(startSearchStr);
        }

        getBopenMsg().setText(language.getString("SearchMessages.button.openMessage"));
        getBhelp().setText(language.getString("SearchMessages.button.help"));
        getBcancel().setText(language.getString("SearchMessages.button.close"));

        Lsender.setText(language.getString("SearchMessages.search.sender"));
        Lcontent.setText(language.getString("SearchMessages.search.content"));
        Lsubject.setText(language.getString("SearchMessages.search.subject"));

        senderCaseCheckBox.setToolTipText(language.getString("SearchMessages.search.tooltip.caseSensitiv"));
        subjectCaseCheckBox.setToolTipText(language.getString("SearchMessages.search.tooltip.caseSensitiv"));
        contentCaseCheckBox.setToolTipText(language.getString("SearchMessages.search.tooltip.caseSensitiv"));

        LsearchResult.setText(language.getString("SearchMessages.label.searchResult"));
        date_Lto.setText(language.getString("SearchMessages.date.to"));

        getDate_RBbetweenDates().setText(language.getString("SearchMessages.date.searchBetweenDates"));
        getDate_RBdisplayed().setText(language.getString("SearchMessages.date.searchInMessagesThatWouldBeDisplayed"));
        getDate_RBdaysBackward().setText(language.getString("SearchMessages.date.searchNumberOfDaysBackward"));
        getDate_RBall().setText(language.getString("SearchMessages.date.searchAllDates"));

        getTruststate_RBall().setText(language.getString("SearchMessages.trustState.searchAllMessages"));
        getTruststate_RBdisplayed().setText(language.getString("SearchMessages.trustState.searchInMessagesThatWouldBeDisplayed"));
        getTruststate_RBchosed().setText(language.getString("SearchMessages.trustState.searchOnlyInMessagesWithFollowingTrustState"));

        getTruststate_CBtampered().setText(language.getString("SearchMessages.trustState.tampered"));
        getTruststate_CBnone().setText(language.getString("SearchMessages.trustState.none"));
        getTruststate_CBbad().setText(language.getString("SearchMessages.trustState.bad"));
        getTruststate_CBcheck().setText(language.getString("SearchMessages.trustState.check"));
        getTruststate_CBobserve().setText(language.getString("SearchMessages.trustState.observe"));
        getTruststate_CBgood().setText(language.getString("SearchMessages.trustState.good"));

        getArchive_RBarchiveOnly().setText(language.getString("SearchMessages.archive.searchOnlyInArchive"));
        getArchive_RBkeypoolOnly().setText(language.getString("SearchMessages.archive.searchOnlyInKeypool"));
        getArchive_RBkeypoolAndArchive().setText(language.getString("SearchMessages.archive.searchInKeypoolAndArchive"));

        getBoards_RBchosed().setText(language.getString("SearchMessages.boards.searchFollowingBoards"));
        getBoards_RBdisplayed().setText(language.getString("SearchMessages.boards.searchInDisplayedBoards"));

        getSearch_CBprivateMsgsOnly().setText(language.getString("SearchMessages.search.searchPrivateMessagesOnly"));
        getSearch_CBflaggedMsgsOnly().setText(language.getString("SearchMessages.search.searchFlaggedMessagesOnly"));
        getSearch_CBstarredMsgsOnly().setText(language.getString("SearchMessages.search.searchStarredMessagesOnly"));
        getSearch_CBrepliedMsgsOnly().setText(language.getString("SearchMessages.search.searchRepliedMessagesOnly"));
        getBoards_Bchoose().setText(language.getString("SearchMessages.boards.chooseBoards")+"...");

        getAttachment_CBmustContainBoards().setText(language.getString("SearchMessages.attachments.messageMustContainBoardAttachments"));
        getAttachment_CBmustContainFiles().setText(language.getString("SearchMessages.attachments.messageMustContainFileAttachments"));
    }

    /**
     * This method initializes Pattachments
     *
     * @return JPanel
     */
    private JPanel getPattachments() {
        if( Pattachments == null ) {
            final GridBagConstraints gridBagConstraints30 = new GridBagConstraints();
            gridBagConstraints30.gridx = 0;
            gridBagConstraints30.insets = new Insets(3,5,1,5);
            gridBagConstraints30.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints30.weighty = 1.0;
            gridBagConstraints30.weightx = 1.0;
            gridBagConstraints30.gridy = 1;
            final GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
            gridBagConstraints8.gridx = 0;
            gridBagConstraints8.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints8.insets = new Insets(3,5,1,5);
            gridBagConstraints8.gridy = 0;
            Pattachments = new JPanel();
            Pattachments.setLayout(new GridBagLayout());
            Pattachments.add(getAttachment_CBmustContainBoards(), gridBagConstraints8);
            Pattachments.add(getAttachment_CBmustContainFiles(), gridBagConstraints30);
        }
        return Pattachments;
    }

    /**
     * This method initializes attachment_CBmustContainBoards
     *
     * @return JCheckBox
     */
    private JCheckBox getAttachment_CBmustContainBoards() {
        if( attachment_CBmustContainBoards == null ) {
            attachment_CBmustContainBoards = new JCheckBox();
        }
        return attachment_CBmustContainBoards;
    }

    /**
     * This method initializes attachment_CBmustContainFiles
     *
     * @return JCheckBox
     */
    private JCheckBox getAttachment_CBmustContainFiles() {
        if( attachment_CBmustContainFiles == null ) {
            attachment_CBmustContainFiles = new JCheckBox();
        }
        return attachment_CBmustContainFiles;
    }

    /**
     * This method initializes Bhelp
     *
     * @return JButton
     */
    private JButton getBhelp() {
        if( Bhelp == null ) {
            Bhelp = new JButton();
            if( Core.isHelpHtmlSecure() == false ) {
                Bhelp.setEnabled(false);
            } else {
                Bhelp.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        MainFrame.getInstance().showHtmlHelp("searchDialog.html");
                    }
                });
            }
        }
        return Bhelp;
    }


}  //  @jve:decl-index=0:visual-constraint="10,10"
