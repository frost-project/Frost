/*
  DisplayMessagesPanel.java / Frost
  Copyright (C) 2007  Frost Project <jtcfrost.sourceforge.net>

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
package frost.gui.preferences;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.InputVerifier;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import frost.Settings;
import frost.util.gui.MiscToolkit;
import frost.util.gui.translation.Language;
import frost.util.gui.verify.PositiveIntegerVerifier;

public class DisplayMessagesPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private class Listener implements ChangeListener {
        public void stateChanged(final ChangeEvent e) {
            if (e.getSource() == indicateLowReceivedMessagesCheckBox) {
                indicateLowReceivedMessagesChanged();
            } else if (e.getSource() == showCollapsedThreadsCheckBox) {
				collapsedThreadsOptionsChanged();
            }
        }
    }

	private transient Settings settings;
	private transient Language language;

    private final JCheckBox messageBodyAACheckBox = new JCheckBox();
    private final JCheckBox msgTableMultilineSelectCheckBox = new JCheckBox();
    private final JCheckBox msgTableScrollHorizontalCheckBox = new JCheckBox();
    private final JCheckBox sortThreadRootMsgsAscendingCheckBox = new JCheckBox();

    private final JCheckBox showCollapsedThreadsCheckBox = new JCheckBox();
    private JPanel collapsedThreadsOptionsPanel = null;
    private final JCheckBox expandRootChildrenCheckBox = new JCheckBox();
    private final JCheckBox expandUnreadThreadsCheckBox = new JCheckBox();
    private final JCheckBox doubleClickShowsMessageCheckBox = new JCheckBox();

    private final JCheckBox showDeletedMessagesCheckBox = new JCheckBox();
    private final JCheckBox dontShowOwnMessagesAsNewCheckBox = new JCheckBox();
    private final JCheckBox dontShowOwnMessagesAsMECheckBox = new JCheckBox();

    private JPanel indicateLowReceivedMessagesPanel = null;
    private final JCheckBox indicateLowReceivedMessagesCheckBox = new JCheckBox();
    private final JLabel LindicateLowReceivedMessagesCountRed = new JLabel();
    private final JLabel LindicateLowReceivedMessagesCountLightRed = new JLabel();
    private final JTextField TFindicateLowReceivedMessagesCountRed = new JTextField(8);
    private final JTextField TFindicateLowReceivedMessagesCountLightRed = new JTextField(8);

    private final Listener listener = new Listener();

    /**
     * @param owner the JDialog that will be used as owner of any dialog that is popped up from this panel
     * @param settings the SettingsClass instance that will be used to get and store the settings of the panel
     */
    protected DisplayMessagesPanel(final JDialog owner, final Settings settings) {
        super();

        this.language = Language.getInstance();
        this.settings = settings;

        initialize();
        loadSettings();
    }

    public void cancel() {
    }

    private JPanel getIndicateLowReceivedMessagesPanel() {
        if( indicateLowReceivedMessagesPanel == null ) {
            indicateLowReceivedMessagesPanel = new JPanel(new GridBagLayout());
            indicateLowReceivedMessagesPanel.setBorder(new EmptyBorder(5, 30, 5, 5));
            final GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(0, 5, 5, 5);
            constraints.weighty = 0;
            constraints.weightx = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.gridy = 0;

            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridx = 0;
            constraints.weightx = 0.5;
            indicateLowReceivedMessagesPanel.add(LindicateLowReceivedMessagesCountRed, constraints);
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 1;
            constraints.weightx = 1;
            indicateLowReceivedMessagesPanel.add(TFindicateLowReceivedMessagesCountRed, constraints);

            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridx = 0;
            constraints.gridy++;
            constraints.weightx = 0.5;
            indicateLowReceivedMessagesPanel.add(LindicateLowReceivedMessagesCountLightRed, constraints);
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 1;
            constraints.weightx = 1;
            indicateLowReceivedMessagesPanel.add(TFindicateLowReceivedMessagesCountLightRed, constraints);
        }
        return indicateLowReceivedMessagesPanel;
    }

    private JPanel getCollapsedThreadsOptionsPanel() {
        if( collapsedThreadsOptionsPanel == null ) {
            collapsedThreadsOptionsPanel = new JPanel(new GridBagLayout());
            collapsedThreadsOptionsPanel.setBorder(new EmptyBorder(5, 30, 5, 5));
            final GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(0, 5, 5, 5);
            constraints.weighty = 0;
            constraints.weightx = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.gridy = 0;

            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridx = 0;
            constraints.weightx = 0.5;
            collapsedThreadsOptionsPanel.add(expandRootChildrenCheckBox, constraints);

            constraints.gridy++;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridx = 0;
            constraints.weightx = 0.5;
            collapsedThreadsOptionsPanel.add(expandUnreadThreadsCheckBox, constraints);
        }
        return collapsedThreadsOptionsPanel;
    }

    /**
     * Initialize the class.
     */
    private void initialize() {
        setName("DisplayPanel");
        setLayout(new GridBagLayout());
        refreshLanguage();

		InputVerifier integerVerifier = new PositiveIntegerVerifier();
		TFindicateLowReceivedMessagesCountRed.setInputVerifier(integerVerifier);
		TFindicateLowReceivedMessagesCountLightRed.setInputVerifier(integerVerifier);

        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        final Insets inset5511 = new Insets(5, 5, 1, 1);

        constraints.insets = inset5511;
        constraints.gridx = 0;
        constraints.gridy = 0;
        add(messageBodyAACheckBox, constraints);

        constraints.insets = inset5511;
        constraints.gridy++;
        add(msgTableMultilineSelectCheckBox, constraints);

        constraints.gridy++;
        add(msgTableScrollHorizontalCheckBox, constraints);

        constraints.gridy++;
        add(sortThreadRootMsgsAscendingCheckBox, constraints);

        constraints.gridy++;
        add(showCollapsedThreadsCheckBox, constraints);

        constraints.gridy++;
        add(getCollapsedThreadsOptionsPanel(), constraints);

        constraints.gridy++;
		add(doubleClickShowsMessageCheckBox, constraints);

        constraints.gridy++;
        add(showDeletedMessagesCheckBox, constraints);

        constraints.gridy++;
        add(dontShowOwnMessagesAsNewCheckBox, constraints);

        constraints.gridy++;
        add(dontShowOwnMessagesAsMECheckBox, constraints);

        constraints.gridy++;
        add(indicateLowReceivedMessagesCheckBox, constraints);

        constraints.gridy++;
        add(getIndicateLowReceivedMessagesPanel(), constraints);

        constraints.gridy++;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        add(new JLabel(""), constraints);

        // add listener
        indicateLowReceivedMessagesCheckBox.addChangeListener(listener);
        showCollapsedThreadsCheckBox.addChangeListener(listener);
    }

    /**
     * Load the settings of this panel
     */
    private void loadSettings() {
        messageBodyAACheckBox.setSelected(settings.getBoolean(Settings.MESSAGE_BODY_ANTIALIAS));
        msgTableMultilineSelectCheckBox.setSelected(settings.getBoolean(Settings.MSGTABLE_MULTILINE_SELECT));
        msgTableScrollHorizontalCheckBox.setSelected(settings.getBoolean(Settings.MSGTABLE_SCROLL_HORIZONTAL));
        sortThreadRootMsgsAscendingCheckBox.setSelected(settings.getBoolean(Settings.SORT_THREADROOTMSGS_ASCENDING));
        showCollapsedThreadsCheckBox.setSelected(settings.getBoolean(Settings.MSGTABLE_SHOW_COLLAPSED_THREADS));
        expandRootChildrenCheckBox.setSelected(settings.getBoolean(Settings.MSGTABLE_EXPAND_ROOT_CHILDREN));
        expandUnreadThreadsCheckBox.setSelected(settings.getBoolean(Settings.MSGTABLE_EXPAND_UNREAD_THREADS));
        doubleClickShowsMessageCheckBox.setSelected(settings.getBoolean(Settings.MSGTABLE_DOUBLE_CLICK_SHOWS_MESSAGE));
        showDeletedMessagesCheckBox.setSelected(settings.getBoolean(Settings.SHOW_DELETED_MESSAGES));
        dontShowOwnMessagesAsNewCheckBox.setSelected(settings.getBoolean(Settings.HANDLE_OWN_MESSAGES_AS_NEW_DISABLED));
        dontShowOwnMessagesAsMECheckBox.setSelected(settings.getBoolean(Settings.SHOW_OWN_MESSAGES_AS_ME_DISABLED));

        TFindicateLowReceivedMessagesCountRed.setText(settings.getString(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_RED));
        TFindicateLowReceivedMessagesCountLightRed.setText(settings.getString(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_LIGHTRED));

        indicateLowReceivedMessagesCheckBox.setSelected(settings.getBoolean(Settings.INDICATE_LOW_RECEIVED_MESSAGES));
        indicateLowReceivedMessagesChanged();
        collapsedThreadsOptionsChanged();
    }

    public void ok() {
        saveSettings();
    }

    private void refreshLanguage() {
        messageBodyAACheckBox.setText(language.getString("Options.display.enableAntialiasingForMessageBody"));
        msgTableScrollHorizontalCheckBox.setText(language.getString("Options.display.showHorizontalScrollbarInMessageTable"));
        msgTableMultilineSelectCheckBox.setText(language.getString("Options.display.enableMultilineSelectionsInMessageTable"));
        sortThreadRootMsgsAscendingCheckBox.setText(language.getString("Options.display.sortThreadRootMsgsAscending"));
        showCollapsedThreadsCheckBox.setText(language.getString("Options.display.showCollapsedThreads"));
        expandRootChildrenCheckBox.setText(language.getString("Options.display.expandRootChildren"));
        expandUnreadThreadsCheckBox.setText(language.getString("Options.display.expandUnreadThreads"));
        doubleClickShowsMessageCheckBox.setText(language.getString("Options.display.doubleClickShowsMessage"));
        showDeletedMessagesCheckBox.setText(language.getString("Options.news.3.showDeletedMessages"));
        dontShowOwnMessagesAsNewCheckBox.setText(language.getString("Options.news.3.dontHandleOwnMessagesAsNew"));
        dontShowOwnMessagesAsMECheckBox.setText(language.getString("Options.news.3.dontShowOwnMessagesAsMe"));

        indicateLowReceivedMessagesCheckBox.setText(language.getString("Options.display.indicateLowReceivedMessages"));
        LindicateLowReceivedMessagesCountRed.setText(language.getString("Options.display.indicateLowReceivedMessagesCountRed"));
        LindicateLowReceivedMessagesCountLightRed.setText(language.getString("Options.display.indicateLowReceivedMessagesCountLightRed"));
    }

    /**
     * Save the settings of this panel
     */
    private void saveSettings() {
        settings.setValue(Settings.MESSAGE_BODY_ANTIALIAS, messageBodyAACheckBox.isSelected());
        settings.setValue(Settings.MSGTABLE_MULTILINE_SELECT, msgTableMultilineSelectCheckBox.isSelected());
        settings.setValue(Settings.MSGTABLE_SCROLL_HORIZONTAL, msgTableScrollHorizontalCheckBox.isSelected());
        settings.setValue(Settings.SORT_THREADROOTMSGS_ASCENDING, sortThreadRootMsgsAscendingCheckBox.isSelected());
        settings.setValue(Settings.MSGTABLE_SHOW_COLLAPSED_THREADS, showCollapsedThreadsCheckBox.isSelected());
        settings.setValue(Settings.MSGTABLE_EXPAND_ROOT_CHILDREN, expandRootChildrenCheckBox.isSelected());
        settings.setValue(Settings.MSGTABLE_EXPAND_UNREAD_THREADS, expandUnreadThreadsCheckBox.isSelected());
        settings.setValue(Settings.MSGTABLE_DOUBLE_CLICK_SHOWS_MESSAGE, doubleClickShowsMessageCheckBox.isSelected());
        settings.setValue(Settings.SHOW_DELETED_MESSAGES, showDeletedMessagesCheckBox.isSelected());
        settings.setValue(Settings.HANDLE_OWN_MESSAGES_AS_NEW_DISABLED, dontShowOwnMessagesAsNewCheckBox.isSelected());
        settings.setValue(Settings.SHOW_OWN_MESSAGES_AS_ME_DISABLED, dontShowOwnMessagesAsMECheckBox.isSelected());

        settings.setValue(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_RED, TFindicateLowReceivedMessagesCountRed.getText());
        settings.setValue(Settings.INDICATE_LOW_RECEIVED_MESSAGES_COUNT_LIGHTRED, TFindicateLowReceivedMessagesCountLightRed.getText());
        settings.setValue(Settings.INDICATE_LOW_RECEIVED_MESSAGES, indicateLowReceivedMessagesCheckBox.isSelected());
    }

    private void indicateLowReceivedMessagesChanged() {
        MiscToolkit.setContainerEnabled(getIndicateLowReceivedMessagesPanel(), indicateLowReceivedMessagesCheckBox.isSelected());
    }

    private void collapsedThreadsOptionsChanged() {
        MiscToolkit.setContainerEnabled(getCollapsedThreadsOptionsPanel(), showCollapsedThreadsCheckBox.isSelected());
    }
}
