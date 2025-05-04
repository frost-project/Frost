/*
  SearchPanel.java / Frost
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
package frost.fileTransfer.search;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.Box.Filler;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import frost.Core;
import frost.Settings;
import frost.util.gui.CloseableTabbedPane;
import frost.util.gui.JSkinnablePopupMenu;
import frost.util.gui.MiscToolkit;
import frost.util.gui.TextComponentClipboardMenu;
import frost.util.gui.search.TableFindAction;
import frost.util.gui.translation.JTranslatableComboBox;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

public class SearchPanel extends JPanel implements LanguageListener {

	private static final long serialVersionUID = 1L;

	private final Language language = Language.getInstance();

    private SearchSimpleToolBar searchSimpleToolBar;
    private SearchAdvancedToolBar searchAdvancedToolBar;

    private final ImageIcon searchIcon = MiscToolkit.loadImageIcon("/data/toolbar/system-search.png");
    private final ImageIcon clearIcon = MiscToolkit.loadImageIcon("/data/toolbar/user-trash.png");

    TableFindAction tableFindAction = new TableFindAction();

    private boolean isInitialized = false;

    private CloseableTabbedPane searchTabs;

    private final String[] searchComboBoxKeys =
        { "SearchPane.fileTypes.allFiles",
          "SearchPane.fileTypes.audio",
          "SearchPane.fileTypes.video",
          "SearchPane.fileTypes.images",
          "SearchPane.fileTypes.documents",
          "SearchPane.fileTypes.executables",
          "SearchPane.fileTypes.archives" };

    public SearchPanel() {
        super();
        language.addLanguageListener(this);
    }

    public void initialize() {
        if (!isInitialized) {

            searchSimpleToolBar = new SearchSimpleToolBar();
            searchAdvancedToolBar = new SearchAdvancedToolBar();

            languageChanged(null);

            setLayout(new BorderLayout());

            searchTabs = new SearchCloseableTabbedPane();
            add(searchTabs, BorderLayout.CENTER);

            // adds simple top panel
            toggleMode(true); // start in simple mode

            isInitialized = true;
        }
    }

    private void toggleMode(final boolean toSimpleMode) {
        if( toSimpleMode ) {
            // switch to simple
            remove(searchAdvancedToolBar);
            add(searchSimpleToolBar, BorderLayout.NORTH);
        } else {
            // switch to extented
            remove(searchSimpleToolBar);
            add(searchAdvancedToolBar, BorderLayout.NORTH);
        }
        updateUI();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // promote change of look&feel to both toolbars,
        // one of them is currently not displayed
        if (searchSimpleToolBar != null) {
            searchSimpleToolBar.updateUI();
        }
        if (searchAdvancedToolBar != null) {
            searchAdvancedToolBar.updateUI();
        }
    }

    public void languageChanged(final LanguageEvent event) {
        searchSimpleToolBar.refreshLanguage();
        searchAdvancedToolBar.refreshLanguage();
    }

	public void startNewSearch(SearchParameters searchParams) {
		String tabText = searchParams.getTabText();

		SearchModel searchModel = new SearchModel(new SearchTableFormat());
		SearchTable searchTable = new SearchTable(searchModel, searchTabs, tabText);
		tableFindAction.install(searchTable.getTable());

		ProxyPanel pp = new ProxyPanel(searchTable, searchModel);

		searchTabs.addTab(tabText + " (...)", pp);

		SearchThread searchThread = new SearchThread(searchParams, searchTable, pp);
		searchThread.start();
	}

    private JSkinnablePopupMenu buildSearchOptionsMenu() {
        final JSkinnablePopupMenu searchOptionsMenu = new JSkinnablePopupMenu();
        final JMenuItem hideBadUserFilesCheckBox = new JCheckBoxMenuItem(language.getString("SearchPane.toolbar.searchOptions.hideFilesFromPeopleMarkedBad"));
        final JMenuItem hideCheckUserFilesCheckBox = new JCheckBoxMenuItem(language.getString("SearchPane.toolbar.searchOptions.hideFilesFromPeopleMarkedCheck"));
        final JMenuItem hideObserveUserFilesCheckBox = new JCheckBoxMenuItem(language.getString("SearchPane.toolbar.searchOptions.hideFilesFromPeopleMarkedObserve"));
        final JMenuItem hideFilesWithoutChkCheckBox = new JCheckBoxMenuItem(language.getString("SearchPane.toolbar.searchOptions.hideFilesWithoutChk"));

        hideBadUserFilesCheckBox.setSelected(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_BAD));
        hideCheckUserFilesCheckBox.setSelected(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_CHECK));
        hideObserveUserFilesCheckBox.setSelected(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_OBSERVE));
        hideFilesWithoutChkCheckBox.setSelected(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_FILES_WITHOUT_CHK));

        hideBadUserFilesCheckBox.addItemListener( new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                Core.frostSettings.setValue(Settings.SEARCH_HIDE_BAD, hideBadUserFilesCheckBox.isSelected());
            } });
        hideCheckUserFilesCheckBox.addItemListener( new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                Core.frostSettings.setValue(Settings.SEARCH_HIDE_CHECK, hideCheckUserFilesCheckBox.isSelected());
            } });
        hideObserveUserFilesCheckBox.addItemListener( new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                Core.frostSettings.setValue(Settings.SEARCH_HIDE_OBSERVE, hideObserveUserFilesCheckBox.isSelected());
            } });
        hideFilesWithoutChkCheckBox.addItemListener( new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                Core.frostSettings.setValue(Settings.SEARCH_HIDE_FILES_WITHOUT_CHK, hideFilesWithoutChkCheckBox.isSelected());
            } });

        searchOptionsMenu.add(hideBadUserFilesCheckBox);
        searchOptionsMenu.add(hideCheckUserFilesCheckBox);
        searchOptionsMenu.add(hideObserveUserFilesCheckBox);
        searchOptionsMenu.addSeparator();
        searchOptionsMenu.add(hideFilesWithoutChkCheckBox);

        return searchOptionsMenu;
    }

	private class SearchSimpleToolBar extends JToolBar implements ActionListener {

		private static final long serialVersionUID = 1L;

		private JTranslatableComboBox searchComboBox = null;
        private final JButton searchButton = new JButton(searchIcon);
        private final JTextField searchTextField = new JTextField(30);
        private final JButton toggleModeButtonToAdvanced = new JButton(">>");
        private final JButton searchOptionsButton = new JButton();

        public SearchSimpleToolBar() {
            super();
            initialize();
        }

        private void initialize() {

            setRollover(true);
            setFloatable(false);

            new TextComponentClipboardMenu(searchTextField, language);

            searchComboBox = new JTranslatableComboBox(language, searchComboBoxKeys);
            toggleModeButtonToAdvanced.setOpaque(false);
            searchOptionsButton.setOpaque(false);
            searchOptionsButton.setFocusPainted(false);

            MiscToolkit.configureButton(searchButton);
            searchComboBox.setMaximumSize(searchComboBox.getPreferredSize());
            searchTextField.setMaximumSize(searchTextField.getPreferredSize());

            toggleModeButtonToAdvanced.setMargin(new Insets(0, 0, 0, 0));
            toggleModeButtonToAdvanced.setBorderPainted(false);
            toggleModeButtonToAdvanced.setFocusPainted(false);
            toggleModeButtonToAdvanced.setRolloverEnabled(true);

            final Dimension blankSpace = new Dimension(8, 0);
            add(toggleModeButtonToAdvanced);
            add(Box.createRigidArea(blankSpace));
            add(searchTextField);
            add(Box.createRigidArea(blankSpace));
            add(searchComboBox);
            add(Box.createRigidArea(blankSpace));
            add(searchOptionsButton);
            add(Box.createRigidArea(blankSpace));
            add(searchButton);
            add(Box.createHorizontalGlue());

            searchTextField.addActionListener(this);
            searchButton.addActionListener(this);
            toggleModeButtonToAdvanced.addActionListener(this);
            searchOptionsButton.addActionListener(this);
        }

        public SearchParameters getSearchParameters() {
            final SearchParameters sp = new SearchParameters(true);
            sp.setExtensions(searchComboBox.getSelectedKey());
            sp.setSimpleSearchString(searchTextField.getText());
            sp.setHideBadUserFiles(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_BAD));
            sp.setHideCheckUserFiles(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_CHECK));
            sp.setHideObserveUserFiles(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_OBSERVE));
            sp.setHideFilesWithoutChkKey(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_FILES_WITHOUT_CHK));
            return sp;
        }

        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == searchButton || e.getSource() == searchTextField) {
                startNewSearch(getSearchParameters());
            } else if (e.getSource() == toggleModeButtonToAdvanced) {
                toggleMode(false);
            } else if (e.getSource() == searchOptionsButton) {
                buildSearchOptionsMenu().show(searchOptionsButton, 5, 5);
            }
        }

        public void refreshLanguage() {
            searchButton.setToolTipText(language.getString("SearchPane.toolbar.tooltip.search"));
            toggleModeButtonToAdvanced.setToolTipText(language.getString("SearchPane.toolbar.tooltip.toggleMode"));
            searchOptionsButton.setText(language.getString("SearchPane.toolbar.searchOptionsButton")+"...");
        }
    }

	private class SearchAdvancedToolBar extends JToolBar implements ActionListener {

		private static final long serialVersionUID = 1L;

		// own JTextField, needed to do the first layout when components are not yet
		// shown
		private class ChangedJtextfield extends JTextField {

			private static final long serialVersionUID = 1L;

			int ownPreferredWidth = -1;
            public ChangedJtextfield(final int i) {
                super(i);
            }
            /**
             * JTextField ignores
             */
            @Override
            public Dimension getPreferredSize() {
                if (ownPreferredWidth > -1) {
                    final Dimension dim = super.getPreferredSize();
                    dim.setSize(ownPreferredWidth, dim.getHeight());
                    return dim;
                } else {
                    return super.getPreferredSize();
                }
            }
            @Override
            public Dimension getMaximumSize() {
                if (ownPreferredWidth > -1) {
                    final Dimension dim = super.getMaximumSize();
                    dim.setSize(ownPreferredWidth, dim.getHeight());
                    return dim;
                } else {
                    return super.getMaximumSize();
                }
            }
            @Override
            public Dimension getMinimumSize() {
                if (ownPreferredWidth > -1) {
                    final Dimension dim = super.getMinimumSize();
                    dim.setSize(ownPreferredWidth, dim.getHeight());
                    return dim;
                } else {
                    return super.getMinimumSize();
                }
            }
            @Override
            public void setPreferredSize(final Dimension dim) {
                ownPreferredWidth = (int) dim.getWidth();
            }
        }

        private JTranslatableComboBox searchComboBox = null;
        private final JButton searchButton = new JButton(searchIcon);
        private final JButton clearButton = new JButton(clearIcon);
        private final JButton toggleModeButtonToSimple = new JButton("<<");

        private final JLabel searchNameLabel = new JLabel();
        private final JLabel searchCommentLabel = new JLabel();
        private final JLabel searchKeywordsLabel = new JLabel();
        private final JLabel searchOwnerLabel = new JLabel();

        private final ChangedJtextfield searchNameTextField = new ChangedJtextfield(18);
        private final ChangedJtextfield searchCommentTextField = new ChangedJtextfield(18);
        private final ChangedJtextfield searchKeywordsTextField = new ChangedJtextfield(18);
        private final ChangedJtextfield searchOwnerTextField = new ChangedJtextfield(18);

        private final JButton searchOptionsButton = new JButton();

        private final int JTEXTFIELD_MINIMUM_SIZE = 75;
        private final int JTEXTFIELD_MAXIMUM_SIZE = 175;

        public SearchAdvancedToolBar() {
            super();
            initialize();
        }

        /**
         * Overwritten to set the JTextField maxWidth to a value that fits for the
         * current container size. All JTextFields are set to the same size.
         */
        @Override
        public void doLayout() {
            // Note: BoxLayout uses the maximumSize!
            int nonJtextfieldComponentsWidth = 0;
            final List<Integer> jtextfieldIndices = new ArrayList<Integer>(); // yes we use autoboxing
            for (int x=0; x<getComponentCount(); x++) {
                final Component c = getComponent(x);
                if (c==null) {
                    continue;
                }
                if (c instanceof ChangedJtextfield) {
                    jtextfieldIndices.add(x);
                } else if (c instanceof Filler) {
                    // count width of all rigid areas, but not the width of the glue!
                    if (((Filler)c).getMaximumSize().getWidth() != Short.MAX_VALUE) {
						nonJtextfieldComponentsWidth += Double.valueOf(c.getMaximumSize().getWidth()).intValue();
                    }
                } else {
					// count width of all other components
					nonJtextfieldComponentsWidth += Double.valueOf(c.getMaximumSize().getWidth()).intValue();
                }
            }

            // don't ask me why, but l&fs other than windows need 20 pixel more space than the component indicate ...
            nonJtextfieldComponentsWidth += 20;

            // when 0 then we don't have any sizes yet.
            if (nonJtextfieldComponentsWidth > 0 && jtextfieldIndices.size() > 0) {
                final int remainingSpace = (int)getSize().getWidth() - nonJtextfieldComponentsWidth;
                int spacePerJtextfield = remainingSpace / jtextfieldIndices.size();
                spacePerJtextfield = Math.min(spacePerJtextfield, JTEXTFIELD_MAXIMUM_SIZE);
                spacePerJtextfield = Math.max(spacePerJtextfield, JTEXTFIELD_MINIMUM_SIZE);

                for (final int jtextfieldIndex : jtextfieldIndices) {
                    final Component c = getComponent(jtextfieldIndex);
                    if (c instanceof ChangedJtextfield) {
                        c.setMaximumSize(new Dimension(spacePerJtextfield, (int)c.getMaximumSize().getHeight()));
                        // again, don't ask why, but Nimbus look&feel uses the preferred size
                        c.setPreferredSize(new Dimension(spacePerJtextfield, (int)c.getPreferredSize().getHeight()));
                    }
                }
            }

            super.doLayout();
        }

        private void initialize() {

            setRollover(true);
            setFloatable(false);

            new TextComponentClipboardMenu(searchNameTextField, language);
            new TextComponentClipboardMenu(searchCommentTextField, language);
            new TextComponentClipboardMenu(searchKeywordsTextField, language);
            new TextComponentClipboardMenu(searchOwnerTextField, language);

            searchComboBox = new JTranslatableComboBox(language, searchComboBoxKeys);
            toggleModeButtonToSimple.setOpaque(false);
            searchOptionsButton.setOpaque(false);
            searchOptionsButton.setFocusPainted(false);

            MiscToolkit.configureButton(searchButton);
            MiscToolkit.configureButton(clearButton);
            searchComboBox.setMaximumSize(searchComboBox.getPreferredSize());
            searchNameTextField.setMaximumSize(searchNameTextField.getPreferredSize());
            searchCommentTextField.setMaximumSize(searchCommentTextField.getPreferredSize());
            searchKeywordsTextField.setMaximumSize(searchKeywordsTextField.getPreferredSize());
            searchOwnerTextField.setMaximumSize(searchOwnerTextField.getPreferredSize());

            toggleModeButtonToSimple.setMargin(new Insets(0, 0, 0, 0));
            toggleModeButtonToSimple.setBorderPainted(false);
            toggleModeButtonToSimple.setFocusPainted(false);
            toggleModeButtonToSimple.setRolloverEnabled(true);

            final Dimension blankSpace = new Dimension(8, 0);
            final Dimension smallSpace = new Dimension(3, 0);
            add(toggleModeButtonToSimple);
            add(Box.createRigidArea(blankSpace));
            add(searchNameLabel);
            add(Box.createRigidArea(smallSpace));
            add(searchNameTextField);
            add(Box.createRigidArea(blankSpace));
            add(searchCommentLabel);
            add(Box.createRigidArea(smallSpace));
            add(searchCommentTextField);
            add(Box.createRigidArea(blankSpace));
            add(searchKeywordsLabel);
            add(Box.createRigidArea(smallSpace));
            add(searchKeywordsTextField);
            add(Box.createRigidArea(blankSpace));
            add(searchOwnerLabel);
            add(Box.createRigidArea(smallSpace));
            add(searchOwnerTextField);
            add(Box.createRigidArea(blankSpace));
            add(searchComboBox);
            add(Box.createRigidArea(blankSpace));
            add(searchOptionsButton);
            add(Box.createRigidArea(smallSpace));
            add(clearButton);
            add(Box.createRigidArea(blankSpace));
            add(searchButton);
            add(Box.createHorizontalGlue());

            searchNameTextField.addActionListener(this);
            searchCommentTextField.addActionListener(this);
            searchKeywordsTextField.addActionListener(this);
            searchOwnerTextField.addActionListener(this);
            searchButton.addActionListener(this);
            clearButton.addActionListener(this);
            toggleModeButtonToSimple.addActionListener(this);
            searchOptionsButton.addActionListener(this);
        }

        public SearchParameters getSearchParameters() {
            final SearchParameters sp = new SearchParameters(false);
            sp.setExtensions(searchComboBox.getSelectedKey());
            sp.setNameString(searchNameTextField.getText());
            sp.setCommentString(searchCommentTextField.getText());
            sp.setKeywordString(searchKeywordsTextField.getText());
            sp.setOwnerString(searchOwnerTextField.getText());
            sp.setHideBadUserFiles(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_BAD));
            sp.setHideCheckUserFiles(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_CHECK));
            sp.setHideObserveUserFiles(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_OBSERVE));
            sp.setHideFilesWithoutChkKey(Core.frostSettings.getBoolean(Settings.SEARCH_HIDE_FILES_WITHOUT_CHK));
            return sp;
        }

        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == searchButton
                    || e.getSource() == searchNameTextField
                    || e.getSource() == searchCommentTextField
                    || e.getSource() == searchKeywordsTextField
                    || e.getSource() == searchOwnerTextField)
            {
                startNewSearch(getSearchParameters());
            } else if (e.getSource() == toggleModeButtonToSimple) {
                toggleMode(true);
            } else if (e.getSource() == clearButton) {
                clearTextfields();
            } else if (e.getSource() == searchOptionsButton) {
                buildSearchOptionsMenu().show(searchOptionsButton, 5, 5);
            }
        }

        private void clearTextfields() {
            searchNameTextField.setText("");
            searchCommentTextField.setText("");
            searchKeywordsTextField.setText("");
            searchOwnerTextField.setText("");
        }

        public void refreshLanguage() {
            searchButton.setToolTipText(language.getString("SearchPane.toolbar.tooltip.search"));
            clearButton.setToolTipText(language.getString("SearchPane.toolbar.tooltip.clear"));
            toggleModeButtonToSimple.setToolTipText(language.getString("SearchPane.toolbar.tooltip.toggleMode"));
            searchNameLabel.setText(language.getString("SearchPane.toolbar.name")+":");
            searchCommentLabel.setText(language.getString("SearchPane.toolbar.comment")+":");
            searchKeywordsLabel.setText(language.getString("SearchPane.toolbar.keywords")+":");
            searchOwnerLabel.setText(language.getString("SearchPane.toolbar.owner")+":");
            searchOptionsButton.setText(language.getString("SearchPane.toolbar.searchOptionsButton")+"...");
        }
    }

	/**
	 * Own closable tabbed pane to get notified if a tab was closed. Needed to
	 * explicitly clear the tablemodel.
	 */
	private class SearchCloseableTabbedPane extends CloseableTabbedPane {

		private static final long serialVersionUID = 1L;

		public SearchCloseableTabbedPane() {
			super();
		}

        @Override
        protected void tabWasClosed(final Component c) {
            if( c instanceof ProxyPanel ) {
                // explicitly clear the model after tab was closed to make life easier for the gc
                final ProxyPanel pp = (ProxyPanel)c;
                pp.tabWasClosed(); // stop search thread
                pp.getModel().clear();
            }
        }
    }

	/**
	 * Panel component that holds a SearchModel. Is added to a tabbed pane.
	 */
	public class ProxyPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private SearchTable searchTable;
		private SearchModel searchModel;
		private SearchThread thread;

		public ProxyPanel(SearchTable searchTable, SearchModel searchModel) {
			this.searchTable = searchTable;
			this.searchModel = searchModel;
			setLayout(new BorderLayout());
			add(searchTable.getScrollPane(), BorderLayout.CENTER);
		}

		public void setSearchThread(SearchThread t) {
			thread = t;
		}

		public void tabWasClosed() {
			if (thread != null) {
				thread.requestCancel();
			}
			tableFindAction.deinstall(searchTable.getTable());
			language.removeLanguageListener(searchTable);
			language.removeLanguageListener((LanguageListener) searchModel.getTableFormat());
		}

		public SearchModel getModel() {
			return searchModel;
		}
	}
}
