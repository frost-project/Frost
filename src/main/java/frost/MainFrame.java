/*
  MainFrame.java / Frost
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
package frost;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultTreeModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.fileTransfer.FileTransferInformation;
import frost.gui.AboutBox;
import frost.gui.IdentitiesBrowser;
import frost.gui.MainFrameStatusBar;
import frost.gui.ManageLocalIdentitiesDialog;
import frost.gui.ManageTrackedDownloads;
import frost.gui.StatisticsDialog;
import frost.gui.help.HelpBrowserFrame;
import frost.gui.preferences.OptionsFrame;
import frost.messaging.freetalk.gui.FreetalkMessageFrame;
import frost.messaging.freetalk.gui.FreetalkMessageTab;
import frost.messaging.frost.UnsentMessagesManager;
import frost.messaging.frost.boards.AbstractNode;
import frost.messaging.frost.boards.Board;
import frost.messaging.frost.boards.TOF;
import frost.messaging.frost.boards.TofTree;
import frost.messaging.frost.boards.TofTreeModel;
import frost.messaging.frost.gui.MessageFrame;
import frost.messaging.frost.gui.MessagePanel;
import frost.messaging.frost.gui.MessagingTab;
import frost.messaging.frost.gui.messagetreetable.MessageTreeTable;
import frost.messaging.frost.gui.messagetreetable.TreeTableModelAdapter;
import frost.messaging.frost.threads.RunningMessageThreadsInformation;
import frost.storage.StorageException;
import frost.storage.perst.filelist.FileListStorage;
import frost.storage.perst.identities.IdentitiesStorage;
import frost.storage.perst.messagearchive.ArchiveMessageStorage;
import frost.storage.perst.messages.MessageStorage;
import frost.util.DateFun;
import frost.util.Mixed;
import frost.util.gui.GlassPane;
import frost.util.gui.JSkinnablePopupMenu;
import frost.util.gui.MemoryMonitor;
import frost.util.gui.MiscToolkit;
import frost.util.gui.StartupMessage;
import frost.util.gui.SystraySupport;
import frost.util.gui.translation.JTranslatableTabbedPane;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageGuiSupport;
import frost.util.gui.translation.LanguageListener;
import frost.util.translate.TranslationStartDialog;

public class MainFrame extends JFrame implements SettingsUpdater, LanguageListener {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);

    private final ImageIcon frameIconDefault = MiscToolkit.loadImageIcon("/data/jtc.jpg");
    private final ImageIcon frameIconNewMessage = MiscToolkit.loadImageIcon("/data/newmessage.gif");

	private MessagingTab messagingTab;
	private transient FreetalkMessageTab freetalkMessageTab;

	private HelpBrowserFrame helpBrowser;
	private MemoryMonitor memoryMonitor;

    private long todaysDateMillis = 0;

    private static Settings frostSettings = null;

    private static MainFrame instance = null; // set in constructor

    private long counter = 55;

	private static List<StartupMessage> queuedStartupMessages = new LinkedList<>();

    //File Menu
    private final JMenu fileMenu = new JMenu();
    private final JMenuItem fileExitMenuItem = new JMenuItem();
    private final JMenuItem fileStatisticsMenuItem = new JMenuItem();

    private final JMenuItem helpAboutMenuItem = new JMenuItem();
    private final JMenuItem helpHelpMenuItem = new JMenuItem();
    private final JMenuItem helpMemMonMenuItem = new JMenuItem();

    //Help Menu
    private final JMenu helpMenu = new JMenu();

    //Language Menu
    private final JMenu languageMenu = new JMenu();

	private final transient Language language;

    // The main menu
    private JMenuBar menuBar;

    private MainFrameStatusBar statusBar;

    //Options Menu
    private final JMenu optionsMenu = new JMenu();
    private final JMenuItem optionsPreferencesMenuItem = new JMenuItem();
    private final JMenuItem optionsManageLocalIdentitiesMenuItem = new JMenuItem();
    private final JMenuItem optionsManageIdentitiesMenuItem = new JMenuItem();
    private final JMenuItem optionsManageTrackedDownloadsMenuItem = new JMenuItem();

    //Plugin Menu
    private final JMenu pluginMenu = new JMenu();
    private final JMenuItem pluginTranslateMenuItem = new JMenuItem();


    private JTranslatableTabbedPane tabbedPane;
    private final JLabel timeLabel = new JLabel("");

    private final JCheckBoxMenuItem tofAutomaticUpdateMenuItem = new JCheckBoxMenuItem();

    private final JMenu tofMenu = new JMenu();

	private GlassPane glassPane;

	private final transient List<JRadioButtonMenuItem> lookAndFeels = new ArrayList<>();

    public MainFrame(final Settings settings, final String title) {

        instance = this;
        Core.getInstance();
        frostSettings = settings;
        language = Language.getInstance();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frostSettings.addUpdater(this);

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        setIconImage(frameIconDefault.getImage());
        setResizable(true);

        setTitle(title);

        // we don't want all of our tooltips to hide after 4 seconds, they should be shown forever
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        addWindowListener(new WindowClosingListener());
        addWindowStateListener(new WindowStateListener());
    }

    public static MainFrame getInstance() {
        return instance;
    }

    public void addPanel(final String title, final JPanel panel) {
        getTabbedPane().add(title, panel);
    }

    /**
     * This method adds a menu item to one of the menus of the menu bar of the frame.
     * It will insert it into an existing menu or into a new one. It will insert it
     * into an existing block or into a new one (where a block is a group of menu items
     * delimited by separators) at the given position.
     * If the position number exceeds the number of items in that block, the item is
     * added at the end of that block.
     * @param item the menu item to add
     * @param menuNameKey the text (as a language key) of the menu to insert the item into.
     *          If there is no menu with that text, a new one will be created at the end
     *          of the menu bar and the item will be put inside.
     * @param block the number of the block to insert the item into. If newBlock is true
     *          we will create a new block at that position. If it is false, we will use
     *          the existing one. If the block number exceeds the number of blocks in the
     *          menu, a new block is created at the end of the menu and the item is
     *          inserted there, no matter what the value of the newBlock parameter is.
     * @param position the position inside the block to insert the item at. If the position
     *          number exceeds the number of items in the block, the item is added at the
     *          end of the block.
     * @param newBlock true to insert the item in a new block. False to use an existing one.
     */
    public void addMenuItem(final JMenuItem item, final String menuNameKey, final int block, final int position, final boolean newBlock) {
        final String menuName = language.getString(menuNameKey);
        int index = 0;
        JMenu menu = null;
        while ((index < getMainMenuBar().getMenuCount()) &&
                (menu == null)) {
            final JMenu aMenu = getMainMenuBar().getMenu(index);
            if ((aMenu != null) &&
                (menuName.equals(aMenu.getText()))) {
                menu = aMenu;
            }
            index++;
        }
        if (menu == null) {
            //There isn't any menu with that name, so we create a new one.
            menu = new JMenu(menuName);
            getMainMenuBar().add(menu);
            menu.add(item);
            return;
        }
        index = 0;
        int blockCount = 0;
        while ((index < menu.getItemCount()) &&
               (blockCount < block)) {
            final Component component = menu.getItem(index);
            if (component == null) {
                blockCount++;
            }
            index++;
        }
        if (blockCount < block) {
            // Block number exceeds the number of blocks in the menu or newBlock is true.
            menu.addSeparator();
            menu.add(item);
            return;
        }
        if (newBlock) {
            // New block created and item put in there.
            menu.insertSeparator(index);
            menu.insert(item, index);
            return;
        }
        int posCount = 0;
        Component component = menu.getItem(index);
        while ((index < menu.getComponentCount()) &&
               (component != null) &&
               (posCount < position)) {
                index++;
                posCount++;
                component = menu.getItem(index);
        }
        menu.add(item, index);
    }

    private JTabbedPane getTabbedPane() {
        if (tabbedPane == null) {
            tabbedPane = new JTranslatableTabbedPane(language);
        }
        return tabbedPane;
    }

    public void selectTabbedPaneTab(final String title) {
        final int position = getTabbedPane().indexOfTab(title);
        if (position != -1) {
            getTabbedPane().setSelectedIndex(position);
        }
    }

    /**
     *
     */
    public void setDisconnected() {
        getMessagingTab().setDisconnected();
    }
    /**
     *
     */
    public void setConnected() {
        getMessagingTab().setConnected();
    }

    /**
     * Build the menu bar.
     */
    private JMenuBar getMainMenuBar() {
        if (menuBar == null) {
            menuBar = new JMenuBar();

            final JMenu lookAndFeelMenu = getLookAndFeelMenu();

            tofAutomaticUpdateMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/mail-send-receive.png", 16, 16));

            fileExitMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/system-log-out.png", 16, 16));
            fileStatisticsMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/x-office-spreadsheet.png", 16, 16));
            lookAndFeelMenu.setIcon(MiscToolkit.getScaledImage("/data/toolbar/preferences-desktop-theme.png", 16, 16));
            optionsManageIdentitiesMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/group.png", 16, 16));
            //optionsManageTrackedDownloadsMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/filelist.png", 16, 16));
            optionsManageLocalIdentitiesMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/user.png", 16, 16));
            optionsPreferencesMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/preferences-system.png", 16, 16));
            helpAboutMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/award_star_silver_3.png", 16, 16));
            pluginTranslateMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/arrow_switch.png", 16, 16));

            // add action listener
            fileExitMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    fileExitMenuItem_actionPerformed();
                }
            });
            fileStatisticsMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    fileStatisticsMenuItem_actionPerformed(e);
                }
            });
            optionsPreferencesMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    optionsPreferencesMenuItem_actionPerformed();
                }
            });
            optionsManageLocalIdentitiesMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    optionsManageLocalIdentitiesMenuItem_actionPerformed(e);
                }
            });
            optionsManageIdentitiesMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    optionsManageIdentitiesMenuItem_actionPerformed(e);
                }
            });
            optionsManageTrackedDownloadsMenuItem.addActionListener(new ActionListener() {
            	public void actionPerformed(final ActionEvent e) {
            		optionsManageTrackedDownloadsMenuItem_actionPerformed(e);
            	}
            });
            pluginTranslateMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                	getTranslationDialog().setVisible(true);
                }
            });
            helpHelpMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/help-browser.png", 16, 16));
            helpHelpMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    showHtmlHelp("index.html");
                    //HelpFrame dlg = new HelpFrame(MainFrame.this);
                    //dlg.setVisible(true);
                }
            });

            if( Core.isHelpHtmlSecure() == false ) {
                helpHelpMenuItem.setEnabled(false);
            }

            helpAboutMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    helpAboutMenuItem_actionPerformed();
                }
            });

            helpMemMonMenuItem.setIcon(MiscToolkit.getScaledImage("/data/toolbar/utilities-system-monitor.png", 16, 16));
            helpMemMonMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    getMemoryMonitor().showDialog();
                }
            });

            // construct menu

            // File Menu
            fileMenu.add(fileStatisticsMenuItem);
            fileMenu.addSeparator();
            fileMenu.add(fileExitMenuItem);
            // News Menu
            tofMenu.add(tofAutomaticUpdateMenuItem);
            // Options Menu
            optionsMenu.add(optionsManageLocalIdentitiesMenuItem);
            optionsMenu.add(optionsManageIdentitiesMenuItem);
            optionsMenu.add(optionsManageTrackedDownloadsMenuItem);
            optionsMenu.addSeparator();
            optionsMenu.add( lookAndFeelMenu );
            optionsMenu.addSeparator();
            optionsMenu.add(optionsPreferencesMenuItem);
            // Plugin Menu
            pluginMenu.add(pluginTranslateMenuItem);
            // Language Menu
            LanguageGuiSupport.getInstance().buildInitialLanguageMenu(languageMenu);
            // Help Menu
            helpMenu.add(helpMemMonMenuItem);
            helpMenu.add(helpHelpMenuItem);
            helpMenu.addSeparator();
            helpMenu.add(helpAboutMenuItem);

            // add all to bar
            menuBar.add(fileMenu);
            menuBar.add(tofMenu);
            menuBar.add(optionsMenu);
            menuBar.add(pluginMenu);
            menuBar.add(languageMenu);
            menuBar.add(helpMenu);

            // add time label
            menuBar.add(Box.createHorizontalGlue());
            menuBar.add(timeLabel);
            menuBar.add(Box.createRigidArea(new Dimension(3,3)));

            translateMainMenu();

            language.addLanguageListener(this);
        }
        return menuBar;
    }

	private JMenu getLookAndFeelMenu() {
		// init look and feel menu
		final UIManager.LookAndFeelInfo[] lookAndFeelList = UIManager.getInstalledLookAndFeels();
		final JMenu lfMenu = new JMenu("Look and feel");
		final ButtonGroup group = new ButtonGroup();

		final ActionListener al = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				final String lfName = event.getActionCommand();
				MiscToolkit.setLookAndFeel(lfName);
				updateComponentTreesUI();
			}
		};

		for (final LookAndFeelInfo lookAndFeel : lookAndFeelList) {
			final String className = lookAndFeel.getClassName();
			final JRadioButtonMenuItem rmItem = new JRadioButtonMenuItem(
					lookAndFeel.getName() + "  [" + className + "]");
			rmItem.setActionCommand(className);
			rmItem.setSelected(UIManager.getLookAndFeel().getClass().getName().equals(className));
			rmItem.addActionListener(al);
			group.add(rmItem);
			lfMenu.add(rmItem);
			lookAndFeels.add(rmItem);
		}
		return lfMenu;
	}

    private MainFrameStatusBar getStatusBar() {
        if( statusBar == null ) {
            statusBar = new MainFrameStatusBar();
        }
        return statusBar;
    }

    /**
     * @return
     */
    private JTabbedPane buildMainPanel() {
    	getMessagingTab().initialize();
        getTabbedPane().insertTab("MainFrame.tabbedPane.news", null, getMessagingTab(), null, 0);

        // optionally show Freetalk tab
        if (frostSettings.getBoolean(Settings.FREETALK_SHOW_TAB)) {
            getFreetalkMessageTab().initialize();
            getTabbedPane().insertTab("MainFrame.tabbedPane.freetalk", null, getFreetalkMessageTab().getTabPanel(), null, 1);
        }

        getTabbedPane().setSelectedIndex(0);

        return getTabbedPane();
    }

    /**
     * save size,location and state of window
     * let save message panel layouts
     */
    public void saveLayout() {
        final Rectangle bounds = getBounds();
        final boolean isMaximized = ((getExtendedState() & Frame.MAXIMIZED_BOTH) != 0);

        frostSettings.setValue(Settings.MAINFRAME_LAST_MAXIMIZED, isMaximized);

        if (!isMaximized) { // Only save the current dimension if frame is not maximized
            frostSettings.setValue(Settings.MAINFRAME_LAST_HEIGHT, bounds.height);
            frostSettings.setValue(Settings.MAINFRAME_LAST_WIDTH, bounds.width);
            frostSettings.setValue(Settings.MAINFRAME_LAST_X, bounds.x);
            frostSettings.setValue(Settings.MAINFRAME_LAST_Y, bounds.y);
        }

        for( final JRadioButtonMenuItem rbmi : lookAndFeels ) {
            if( rbmi.isSelected() ) {
                frostSettings.setValue(Settings.LOOK_AND_FEEL, rbmi.getActionCommand());
            }
        }

        getMessagingTab().saveLayout();
        if (frostSettings.getBoolean(Settings.FREETALK_SHOW_TAB)) {
            getFreetalkMessageTab().saveLayout();
        }
    }

    /**
     * File | Exit action performed
     */
    public void fileExitMenuItem_actionPerformed() {

        // warn if create message windows are open
        if ((MessageFrame.getOpenInstanceCount() > 0) || (FreetalkMessageFrame.getOpenInstanceCount() > 0)) {
            final int result = JOptionPane.showConfirmDialog(
                    this,
                    language.getString("MainFrame.openCreateMessageWindows.body"),
                    language.getString("MainFrame.openCreateMessageWindows.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                return;
            }
        }

        // warn if messages are currently uploading
        if (UnsentMessagesManager.getRunningMessageUploads() > 0 ) {
            final int result = JOptionPane.showConfirmDialog(
                    this,
                    language.getString("MainFrame.runningUploadsWarning.body"),
                    language.getString("MainFrame.runningUploadsWarning.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                return;
            }
        }

        saveLayout();

        System.exit(0);
    }

    /**
     * File | Statistics action performed
     */
    private void fileStatisticsMenuItem_actionPerformed(final ActionEvent evt) {

        activateGlassPane(); // lock gui

        new Thread() {
            @Override
            public void run() {
                try {
                    final int msgCount = MessageStorage.inst().getMessageCount();
                    final int arcMsgCount = ArchiveMessageStorage.inst().getMessageCount();
                    final int idCount = IdentitiesStorage.inst().getIdentityCount();
                    final int fileCount = FileListStorage.inst().getFileCount();
                    final int sharerCount = FileListStorage.inst().getSharerCount();
//                    final long fileSizes = FileListStorage.inst().getFileSizes();
// NOTE: file size computation scans all file list files, takes a long time, disabled for now.
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            deactivateGlassPane();
                            final StatisticsDialog dlg = new StatisticsDialog(MainFrame.this);
//                            dlg.startDialog(msgCount, arcMsgCount, idCount, sharerCount, fileCount, fileSizes);
                            dlg.startDialog(msgCount, arcMsgCount, idCount, sharerCount, fileCount, 0L);
                        }
                    });
                } finally {
                    // paranoia, don't left gui locked
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            deactivateGlassPane();
                        }
                    });
                }
            }
        }.start();
    }

    public MessagePanel getMessagePanel() {
        return getMessagingTab().getMessagePanel();
    }

    /**
     * Help | About action performed
     */
    private void helpAboutMenuItem_actionPerformed() {
        final AboutBox dlg = new AboutBox(this);
        dlg.setVisible(true);
    }

    public void postInitialize() {
        getMessagingTab().postInitialize();
    }

    public void initialize() {

        // Add components
        final JPanel contentPanel = (JPanel) getContentPane();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(buildMainPanel(), BorderLayout.CENTER);
        contentPanel.add(getStatusBar(), BorderLayout.SOUTH);
        setJMenuBar(getMainMenuBar());

        // step through all messages on disk up to maxMessageDisplay and check if there are new messages
        TOF.getInstance().searchAllUnreadMessages(false);

        tofAutomaticUpdateMenuItem.setSelected(frostSettings.getBoolean(Settings.BOARD_AUTOUPDATE_ENABLED));

        // make sure the font size isn't too small to see
        if (frostSettings.getInteger(Settings.MESSAGE_BODY_FONT_SIZE) < 6) {
            frostSettings.setValue(Settings.MESSAGE_BODY_FONT_SIZE, 6);
        }

        // load size, location and state of window
        int lastHeight = frostSettings.getInteger(Settings.MAINFRAME_LAST_HEIGHT);
        int lastWidth = frostSettings.getInteger(Settings.MAINFRAME_LAST_WIDTH);
        int lastPosX = frostSettings.getInteger(Settings.MAINFRAME_LAST_X);
        int lastPosY = frostSettings.getInteger(Settings.MAINFRAME_LAST_Y);
        final boolean lastMaximized = frostSettings.getBoolean(Settings.MAINFRAME_LAST_MAXIMIZED);
        final Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();

        if (lastWidth < 100) {
            lastWidth = 700;
        }
        if (lastWidth > scrSize.width) {
            lastWidth = scrSize.width;
        }

        if (lastHeight < 100) {
            lastHeight = 500;
        }
        if (lastHeight > scrSize.height) {
            lastWidth = scrSize.height;
        }

        if (lastPosX < 0) {
            lastPosX = 0;
        }
        if (lastPosY < 0) {
            lastPosY = 0;
        }

        if ((lastPosX + lastWidth) > scrSize.width) {
            lastPosX = scrSize.width / 10;
            lastWidth = (int) ((scrSize.getWidth() / 10.0) * 8.0);
        }

        if ((lastPosY + lastHeight) > scrSize.height) {
            lastPosY = scrSize.height / 10;
            lastHeight = (int) ((scrSize.getHeight() / 10.0) * 8.0);
        }

        setBounds(lastPosX, lastPosY, lastWidth, lastHeight);

        if (lastMaximized) {
            setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
        }

        validate();
    }

    /**
     * Start the ticker that makes the mainframe waggle and starts board updates.
     */
    public void startTickerThread() {
        final Thread tickerThread = new Thread("tick tack") {
            @Override
            public void run() {
                while (true) {
                    Mixed.wait(1000);
                    // TODO: Refactor this method in Core. lots of work :)
                    timer_actionPerformed();
                }
            }
        };
        tickerThread.start();
    }

    /**
     * Options | Preferences action performed
     */
    private void optionsPreferencesMenuItem_actionPerformed() {
        try {
            frostSettings.exitSave();
        } catch (final StorageException se) {
            logger.error("Error while saving the settings.", se);
        }

        final OptionsFrame optionsDlg = new OptionsFrame(this, frostSettings);
        final boolean okPressed = optionsDlg.runDialog();
        if (okPressed) {
            // check if signed only+hideCheck+hideBad or blocking words settings changed
            if (optionsDlg.shouldReloadMessages()) {
                // update the new msg. count for all boards
                TOF.getInstance().searchAllUnreadMessages(true);
                // reload all messages
                tofTree_actionPerformed(null);
            }
            if( optionsDlg.shouldResetLastBackloadUpdateFinishedMillis() ) {
                // reset lastBackloadUpdatedMillis for all boards
                getMessagingTab().getTofTreeModel().resetLastBackloadUpdateFinishedMillis();
            }
            if( optionsDlg.shouldResetSharedFilesLastDownloaded() ) {
                // reset lastDownloaded of all shared files
                final Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            FileListStorage.inst().resetLastDownloaded();
                        } catch(final Throwable tt) {
                            logger.error("Exception during resetLastDownloaded", tt);
                        }
                    }
                };
                t.start();
            }

            // repaint whole tree, in case the update visualization was enabled or disabled (or others)
            getMessagingTab().getTofTree().updateTree();
        }
    }

    private void optionsManageLocalIdentitiesMenuItem_actionPerformed(final ActionEvent e) {
        final ManageLocalIdentitiesDialog dlg = new ManageLocalIdentitiesDialog();
        dlg.setVisible(true); // modal
        if( dlg.isIdentitiesImported() ) {
            // identities were imported, reload message table to show 'ME' for imported local identities
            tofTree_actionPerformed(null);
        }
    }

    private void optionsManageIdentitiesMenuItem_actionPerformed(final ActionEvent e) {
        new IdentitiesBrowser(this).startDialog();
    }

    private void optionsManageTrackedDownloadsMenuItem_actionPerformed(final ActionEvent e) {
    	new ManageTrackedDownloads(this).startDialog(this);
    }

    /**
     * Refresh the texts in MainFrame with new language.
     */
    public void languageChanged(final LanguageEvent e) {
        translateMainMenu();
        LanguageGuiSupport.getInstance().translateLanguageMenu();
    }

    public void setPanelEnabled(final String title, final boolean enabled) {
        final int position = getTabbedPane().indexOfTab(title);
        if (position != -1) {
            getTabbedPane().setEnabledAt(position, enabled);
        }
    }

    public void setTofTree(final TofTree tofTree) {
        getMessagingTab().setTofTree(tofTree);
    }

    public void setTofTreeModel(final TofTreeModel tofTreeModel) {
        getMessagingTab().setTofTreeModel(tofTreeModel);
    }

    /**
     * timer Action Listener (automatic download), gui updates
     */
    public void timer_actionPerformed() {
        // this method is called by a timer each second, so this counter counts seconds
        counter++;

        final RunningMessageThreadsInformation msgInfo = getMessagingTab().getRunningMessageThreadsInformation();
        final FileTransferInformation fileInfo = Core.getInstance().getFileTransferManager().getFileTransferInformation();

        //////////////////////////////////////////////////
        //   Automatic TOF update
        //////////////////////////////////////////////////
        if (Core.isFreenetOnline() &&
            ((counter % 15) == 0) && // check all 15 seconds if a board update could be started
            isAutomaticBoardUpdateEnabled() &&
            (msgInfo.getDownloadingBoardCount() < frostSettings.getInteger(Settings.BOARD_AUTOUPDATE_CONCURRENT_UPDATES)))
        {
            getMessagingTab().startNextBoardUpdate();
        }

        //////////////////////////////////////////////////
        //   Display time in button bar
        //////////////////////////////////////////////////
		final OffsetDateTime now = OffsetDateTime.now(DateFun.getTimeZone());

        // check all 60 seconds if the day changed
        if( (getTodaysDateMillis() == 0) || ((counter % 60) == 0) ) {
			final long millis = DateFun.toStartOfDayInMilli(now);
            if( getTodaysDateMillis() != millis ) {
                setTodaysDateMillis(millis);
            }
        }

        timeLabel.setText(
            new StringBuilder()
						.append(DateFun.FORMAT_DATE_VISIBLE.format(now)).append(" - ")
						.append(DateFun.FORMAT_TIME_VISIBLE.format(now))
                .toString());

        /////////////////////////////////////////////////
        //   Update status bar and file count in panels
        /////////////////////////////////////////////////
        getStatusBar().setStatusBarInformations(fileInfo, msgInfo, getMessagingTab().getTofTreeModel().getSelectedNode().getName());

        Core.getInstance().getFileTransferManager().updateWaitingCountInPanels(fileInfo);
    }

    public long getTodaysDateMillis() {
        return todaysDateMillis;
    }

    private void setTodaysDateMillis(final long v) {
        todaysDateMillis = v;
    }

    /** TOF Board selected
     * Core.getOut()
     * if e == NULL, the method is called by truster or by the reloader after options were changed
     * in this cases we usually should left select the actual message (if one) while reloading the table
     * @param e
     */
    public void tofTree_actionPerformed(final TreeSelectionEvent e) {
        getMessagingTab().boardTree_actionPerformed();
    }

    public void tofTree_actionPerformed(final TreeSelectionEvent e, final boolean reload) {
        getMessagingTab().boardTree_actionPerformed(reload);
    }

    private void translateMainMenu() {
        fileMenu.setText(language.getString("MainFrame.menu.file"));
        fileExitMenuItem.setText(language.getString("Common.exit"));
        fileStatisticsMenuItem.setText(language.getString("MainFrame.menu.file.statistics"));
        tofMenu.setText(language.getString("MainFrame.menu.news"));
        tofAutomaticUpdateMenuItem.setText(language.getString("MainFrame.menu.news.automaticBoardUpdate"));
        optionsMenu.setText(language.getString("MainFrame.menu.options"));
        optionsPreferencesMenuItem.setText(language.getString("MainFrame.menu.options.preferences"));
        optionsManageLocalIdentitiesMenuItem.setText(language.getString("MainFrame.menu.options.manageLocalIdentities"));
        optionsManageIdentitiesMenuItem.setText(language.getString("MainFrame.menu.options.manageIdentities"));
        optionsManageTrackedDownloadsMenuItem.setText(language.getString("MainFrame.menu.options.manageTrackedDownloads"));
        pluginMenu.setText(language.getString("MainFrame.menu.plugins"));
        pluginTranslateMenuItem.setText(language.getString("MainFrame.menu.plugins.translateFrost"));
        languageMenu.setText(language.getString("MainFrame.menu.language"));
        helpMenu.setText(language.getString("MainFrame.menu.help"));
        helpMemMonMenuItem.setText(language.getString("MainFrame.menu.help.showMemoryMonitor"));
        helpHelpMenuItem.setText(language.getString("MainFrame.menu.help.help"));
        helpAboutMenuItem.setText(language.getString("MainFrame.menu.help.aboutFrost"));
    }

    public void updateSettings() {
        frostSettings.setValue(Settings.BOARD_AUTOUPDATE_ENABLED, tofAutomaticUpdateMenuItem.isSelected());
    }

    /**
     * Selects message icon in lower right corner
     */
    public void displayNewMessageIcon(final boolean showNewMessageIcon) {

        getStatusBar().showNewMessageIcon(showNewMessageIcon);

        if( SystraySupport.isInitialized() ) {
            if( showNewMessageIcon ) {
                SystraySupport.setIconNewMessage();
            } else {
                SystraySupport.setIconNormal();
            }
        }

        final ImageIcon iconToSet;
        if (showNewMessageIcon) {
            iconToSet = frameIconNewMessage;
        } else {
            iconToSet = frameIconDefault;
        }
        setIconImage(iconToSet.getImage());
    }

    /**
     * Fires a nodeChanged (redraw) for this board and updates buttons.
     */
    public void updateTofTree(final AbstractNode board) {
        getMessagingTab().updateTofTreeNode(board);
    }

    public void setAutomaticBoardUpdateEnabled(final boolean state) {
        tofAutomaticUpdateMenuItem.setSelected(state);
    }

    public boolean isAutomaticBoardUpdateEnabled() {
        return tofAutomaticUpdateMenuItem.isSelected();
    }

    private MemoryMonitor getMemoryMonitor() {
        if( memoryMonitor == null ) {
            memoryMonitor = new MemoryMonitor();
        }
        return memoryMonitor;
    }

    private TranslationStartDialog getTranslationDialog () {
        return new TranslationStartDialog(this);
    }

	public void showHtmlHelp(final String item) {
		if (Core.isHelpHtmlSecure() == false) {
			return;
		}
		if (helpBrowser == null) {
			helpBrowser = new HelpBrowserFrame(frostSettings.getString(Settings.LANGUAGE_LOCALE));
		}
		// show first time or bring to front
		helpBrowser.setVisible(true);
		helpBrowser.showHelpPage(item);
	}

    /**
     * Start the search dialog with only the specified boards preselected as boards to search into.
     */
    public void startSearchMessagesDialog(final List<Board> l) {
        // show first time or bring to front
        getMessagingTab().getSearchMessagesDialog().startDialog(l);
    }

    public void updateMessageCountLabels(final Board board) {
        // forward to MessagePanel
        getMessagingTab().getMessagePanel().updateMessageCountLabels(board);
    }
    public TreeTableModelAdapter getMessageTableModel() {
        // forward to MessagePanel
        return getMessagingTab().getMessagePanel().getMessageTableModel();
    }
    public DefaultTreeModel getMessageTreeModel() {
        // forward to MessagePanel
        return getMessagingTab().getMessagePanel().getMessageTreeModel();
    }
    public MessageTreeTable getMessageTreeTable() {
        // forward to MessagePanel
        return getMessagingTab().getMessagePanel().getMessageTable();
    }

    private class WindowClosingListener extends WindowAdapter {
        @Override
        public void windowClosing(final WindowEvent e) {
            fileExitMenuItem_actionPerformed();
        }
    }

    private class WindowStateListener extends WindowAdapter {
        @Override
        public void windowStateChanged(final WindowEvent e) {
            if( (e.getNewState() & Frame.ICONIFIED) != 0 ) {
                // Frost window was minimized by user, minimize to tray if configured
                if ( Core.frostSettings.getBoolean(Settings.MINIMIZE_TO_SYSTRAY)
                        && SystraySupport.isInitialized())
                {
                    final boolean wasMaximized = ((e.getOldState() & Frame.MAXIMIZED_BOTH) != 0);

                    // frame is minimized right now, de-minimize so it shows up next time
                    if (!wasMaximized) {
                        setExtendedState(Frame.NORMAL);
                    } else {
                        setExtendedState(Frame.MAXIMIZED_BOTH);
                    }

                    SystraySupport.minimizeToTray();
                }
            }
        }
    }

    public void activateGlassPane() {
        getMessagingTab().showProgress();

        // Mount the glasspane on the component window
        final GlassPane aPane = GlassPane.mount(this, true);

        // keep track of the glasspane as an instance variable
        glassPane = aPane;

        if (glassPane != null) {
            // Start interception UI interactions
            glassPane.setVisible(true);
        }
    }

    public void deactivateGlassPane() {
        if (glassPane != null) {
            // Stop UI interception
            glassPane.setVisible(false);
            glassPane = null;
        }
        getMessagingTab().hideProgress();
    }

    /**
     *  Updates the component tree UI of all the frames and dialogs of the application
     */
    public void updateComponentTreesUI() {
        final Frame[] appFrames = Frame.getFrames();
        final JSkinnablePopupMenu[] appPopups = JSkinnablePopupMenu.getSkinnablePopupMenus();
        for( final Frame element : appFrames ) { //Loop to update all the frames
            SwingUtilities.updateComponentTreeUI(element);
            final Window[] ownedWindows = element.getOwnedWindows();
            for( final Window element2 : ownedWindows ) { //Loop to update the dialogs
                if (element2 instanceof Dialog) {
                    SwingUtilities.updateComponentTreeUI(element2);
                }
            }
        }
        for( final JSkinnablePopupMenu element : appPopups ) { //Loop to update all the popups
            SwingUtilities.updateComponentTreeUI(element);
        }
        // the panels are not all in the component tree, update them manually
        SwingUtilities.updateComponentTreeUI(getMessagePanel());
        SwingUtilities.updateComponentTreeUI(getMessagingTab().getSentMessagesPanel());
        SwingUtilities.updateComponentTreeUI(getMessagingTab().getUnsentMessagesPanel());
        repaint();
    }

    /**
     * Enqueue a message that is shown after the mainframe became visible.
     * Used to show messages about problems occured during loading (e.g. missing shared files).
     */
    public static void enqueueStartupMessage(final StartupMessage sm) {
        queuedStartupMessages.add( sm );
    }

    /**
     * Show the enqueued messages and finally clear the messages queue.
     */
    public void showStartupMessages() {
        for( final StartupMessage sm : queuedStartupMessages ) {
            sm.display(this);
        }
        // cleanup
        StartupMessage.cleanup();
        queuedStartupMessages.clear();
        queuedStartupMessages = null;
    }

    public MessagingTab getMessagingTab() {
        if (messagingTab == null) {
            messagingTab = new MessagingTab(this);
        }
        return messagingTab;
    }

    public FreetalkMessageTab getFreetalkMessageTab() {
        if (freetalkMessageTab == null) {
            freetalkMessageTab = new FreetalkMessageTab(this);
        }
        return freetalkMessageTab;
    }
}
