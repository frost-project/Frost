/*
  JunkPanel.java / Frost
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.InputVerifier;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import frost.Settings;
import frost.util.gui.translation.Language;
import frost.util.gui.verify.PositiveIntegerVerifier;

public class JunkPanel extends JPanel {

	private static final long serialVersionUID = 1L;

    private Settings settings = null;
    private Language language = null;

    private final JCheckBox hideJunkMessagesCheckBox = new JCheckBox();
    private final JCheckBox markJunkIdentityBadCheckBox = new JCheckBox();

    private final JCheckBox stopBoardUpdatesWhenDosedCheckBox = new JCheckBox();
    private final JLabel LinvalidSubsequentMessagesThreshold = new JLabel();
    private final JTextField TfInvalidSubsequentMessagesThreshold = new JTextField(8);

    private final Listener listener = new Listener();

    private class Listener implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == stopBoardUpdatesWhenDosedCheckBox) {
                refreshStopOnDosState();
            }
        }
    }

    /**
     * @param settings the SettingsClass instance that will be used to get and store the settings of the panel
     */
    protected JunkPanel(final Settings settings) {
        super();

        this.language = Language.getInstance();
        this.settings = settings;

        initialize();
        loadSettings();
    }

    private void initialize() {
        setName("JunkPanel");
        setLayout(new GridBagLayout());
        refreshLanguage();

		InputVerifier integerVerifier = new PositiveIntegerVerifier();
		TfInvalidSubsequentMessagesThreshold.setInputVerifier(integerVerifier);

        // Adds all of the components
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        final Insets insets5555 = new Insets(5, 5, 5, 5);
        constraints.insets = insets5555;
        constraints.weightx = 1.0;
        constraints.gridwidth = 1;
        constraints.gridy=0;

        constraints.insets = insets5555;
        constraints.gridx = 0;

        add(hideJunkMessagesCheckBox, constraints);

        constraints.gridy++;

        add(markJunkIdentityBadCheckBox, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        {
            final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
            add(separator, constraints);
        }
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridy++;

        add(stopBoardUpdatesWhenDosedCheckBox, constraints);

        constraints.gridy++;

        {
            final JPanel subPanel = new JPanel(new GridBagLayout());
            final GridBagConstraints subConstraints = new GridBagConstraints();
            subConstraints.insets = new Insets(0,10,0,10);
            subConstraints.anchor = GridBagConstraints.WEST;
            subConstraints.gridx = 0;
            subPanel.add(LinvalidSubsequentMessagesThreshold, subConstraints);
            subConstraints.gridx = 1;
            subPanel.add(TfInvalidSubsequentMessagesThreshold, subConstraints);

            add(subPanel, constraints);
        }

        // glue
        constraints.gridy++;
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        add(new JLabel(""), constraints);

        // Add listeners
        stopBoardUpdatesWhenDosedCheckBox.addActionListener(listener);
    }

    private void refreshStopOnDosState() {
        final boolean stopOnDos = stopBoardUpdatesWhenDosedCheckBox.isSelected();
        LinvalidSubsequentMessagesThreshold.setEnabled(stopOnDos);
        TfInvalidSubsequentMessagesThreshold.setEnabled(stopOnDos);
    }

    /**
     * Load the settings of this panel
     */
    private void loadSettings() {
        hideJunkMessagesCheckBox.setSelected(settings.getBoolean(Settings.JUNK_HIDE_JUNK_MESSAGES));
        markJunkIdentityBadCheckBox.setSelected(settings.getBoolean(Settings.JUNK_MARK_JUNK_IDENTITY_BAD));
        stopBoardUpdatesWhenDosedCheckBox.setSelected(settings.getBoolean(Settings.DOS_STOP_BOARD_UPDATES_WHEN_DOSED));
        TfInvalidSubsequentMessagesThreshold.setText(""+settings.getInteger(Settings.DOS_INVALID_SUBSEQUENT_MSGS_THRESHOLD));
    }

    public void ok() {
        saveSettings();
    }

    private void refreshLanguage() {
        hideJunkMessagesCheckBox.setText(language.getString("Options.junk.hideJunkMessages"));
        markJunkIdentityBadCheckBox.setText(language.getString("Options.junk.markJunkIdentityBad"));

        stopBoardUpdatesWhenDosedCheckBox.setText(language.getString("Options.junk.stopBoardUpdatesWhenDosed"));
        LinvalidSubsequentMessagesThreshold.setText(language.getString("Options.junk.invalidSubsequentMessagesThreshold"));
    }

    /**
     * Save the settings of this panel
     */
    private void saveSettings() {
        settings.setValue(Settings.JUNK_HIDE_JUNK_MESSAGES, hideJunkMessagesCheckBox.isSelected());
        settings.setValue(Settings.JUNK_MARK_JUNK_IDENTITY_BAD, markJunkIdentityBadCheckBox.isSelected());
        settings.setValue(Settings.DOS_STOP_BOARD_UPDATES_WHEN_DOSED, stopBoardUpdatesWhenDosedCheckBox.isSelected());
        settings.setValue(Settings.DOS_INVALID_SUBSEQUENT_MSGS_THRESHOLD, TfInvalidSubsequentMessagesThreshold.getText());
    }
}
