/*
  MiscPanel.java / Frost
  Copyright (C) 2003  Frost Project <jtcfrost.sourceforge.net>

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

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Settings;
import frost.util.gui.MiscToolkit;
import frost.util.gui.TextComponentClipboardMenu;
import frost.util.gui.translation.Language;

@SuppressWarnings("serial")
class MiscPanel extends JPanel {

	private static final Logger logger =  LoggerFactory.getLogger(MiscPanel.class);

    private Settings settings = null;
    private Language language = null;

    private final JLabel autoSaveIntervalLabel = new JLabel();
    private final JTextField autoSaveIntervalTextField = new JTextField(8);
    private final JCheckBox autoSaveLocalIdentitiesCheckBox = new JCheckBox();
    private final JLabel freenetNodeLabel = new JLabel();
    private final JTextField freenetNodeTextField = new JTextField();

    private final JLabel browserAddressLabel = new JLabel();
    private final JTextField browserAddressTextField = new JTextField();

    private final JCheckBox useDDACheckBox = new JCheckBox();
    private final JCheckBox usePersistenceCheckBox = new JCheckBox();

    private final JCheckBox showSystrayIconCheckBox = new JCheckBox();
    private final JCheckBox minimizeToSystrayCheckBox = new JCheckBox();
    private final JCheckBox splashScreenCheckBox = new JCheckBox();
    private final JCheckBox compactDatabaseAtNextStartupCheckBox = new JCheckBox();

    /**
     * @param settings the SettingsClass instance that will be used to get and store the settings of the panel
     */
    protected MiscPanel(final Settings settings) {
        super();

        this.language = Language.getInstance();
        this.settings = settings;

        initialize();
        loadSettings();
    }

    private void initialize() {
        setName("MiscPanel");
        setLayout(new GridBagLayout());
        refreshLanguage();

        // We create the components
        new TextComponentClipboardMenu(autoSaveIntervalTextField, language);
        new TextComponentClipboardMenu(freenetNodeTextField, language);
        new TextComponentClipboardMenu(browserAddressTextField, language);

        // Adds all of the components
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        final Insets insets5555 = new Insets(5, 5, 5, 5);
        final Insets insets0555 = new Insets(0, 5, 5, 5);

        constraints.weightx = 0;
        constraints.insets = insets5555;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        add(freenetNodeLabel, constraints);
        constraints.gridy++;
        constraints.weightx = 1;
        add(freenetNodeTextField, constraints);
        constraints.weightx = 0;

        constraints.insets = insets0555;

        constraints.gridwidth = 2;
        constraints.gridx = 0;
        constraints.gridy++;
        add(useDDACheckBox, constraints);

        constraints.gridy++;
        add(usePersistenceCheckBox, constraints);

        constraints.insets = insets5555;

        constraints.weightx = 0;
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy++;
        add(autoSaveIntervalLabel, constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.NONE;
        add(autoSaveIntervalTextField, constraints);

        constraints.weightx = 0;
        constraints.gridwidth = 2;
        constraints.gridx = 0;

        constraints.gridy++;
        add(autoSaveLocalIdentitiesCheckBox, constraints);

        constraints.insets = insets0555;

        constraints.gridy++;
        add(splashScreenCheckBox, constraints);
        constraints.gridy++;
        add(showSystrayIconCheckBox, constraints);
        constraints.gridy++;
        add(minimizeToSystrayCheckBox, constraints);
        constraints.gridy++;
        add(compactDatabaseAtNextStartupCheckBox, constraints);

        constraints.insets = insets0555;

        // browserAddress
        constraints.gridy++;
        constraints.gridwidth = 2;
        add(browserAddressLabel, constraints);
        constraints.gridy++;
        constraints.weightx = 3;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        add(browserAddressTextField, constraints);
        constraints.weightx = 0;

        // glue
        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        add(new JLabel(""), constraints);
    }

    /**
     * Load the settings of this panel
     */
    private void loadSettings() {
        showSystrayIconCheckBox.setSelected(settings.getBoolValue(Settings.SHOW_SYSTRAY_ICON));
        minimizeToSystrayCheckBox.setSelected(settings.getBoolValue(Settings.MINIMIZE_TO_SYSTRAY));
        compactDatabaseAtNextStartupCheckBox.setSelected(settings.getBoolValue(Settings.PERST_COMPACT_STORAGES));
        freenetNodeTextField.setText(settings.getValue(Settings.FREENET_FCP_ADDRESS));
        autoSaveIntervalTextField.setText(Integer.toString(settings.getIntValue(Settings.AUTO_SAVE_INTERVAL)));
        autoSaveLocalIdentitiesCheckBox.setSelected(settings.getBoolValue(Settings.AUTO_SAVE_LOCAL_IDENTITIES));

        splashScreenCheckBox.setSelected(settings.getBoolValue(Settings.DISABLE_SPLASHSCREEN));
        useDDACheckBox.setSelected(settings.getBoolValue(Settings.FCP2_USE_DDA));
        usePersistenceCheckBox.setSelected(settings.getBoolValue(Settings.FCP2_USE_PERSISTENCE));
        
        browserAddressTextField.setText(settings.getValue(Settings.BROWSER_ADDRESS));
    }

    public void ok() {

        final String nodes = freenetNodeTextField.getText();
        if( nodes.indexOf(",") > -1 ) {
            if( usePersistenceCheckBox.isSelected() ) {
                MiscToolkit.showMessage(
                        "Persistence is not possible with more than 1 node. Persistence disabled.",
                        JOptionPane.ERROR_MESSAGE,
                        "Warning: Persistence is not possible");
                usePersistenceCheckBox.setSelected(false);
            }
        }

		final String browserAddress = browserAddressTextField.getText();
		if (browserAddress.length() > 0) {
			if (!browserAddress.matches("^https?://.+")) {
				logger.warn("Unrecognized URI format: {}", browserAddress);
				browserAddressTextField.setText("");
			} else if (!browserAddress.endsWith("/")) {
				browserAddressTextField.setText(browserAddress + "/");
			}
		}

        saveSettings();
    }

    private void refreshLanguage() {
        freenetNodeLabel.setText(language.getString("Options.miscellaneous.freenetFcpNode"));
        useDDACheckBox.setText(language.getString("Options.miscellaneous.useDDA"));
        useDDACheckBox.setToolTipText(language.getString("Options.miscellaneous.useDDA.tooltip"));

        usePersistenceCheckBox.setText(language.getString("Options.miscellaneous.usePersistence"));

        autoSaveIntervalLabel.setText(language.getString("Options.miscellaneous.automaticSavingInterval") +
                " (60 "+language.getString("Options.common.minutes")+")");
        autoSaveLocalIdentitiesCheckBox.setText(language.getString("Options.miscellaneous.autoSaveLocalIdentities"));
        splashScreenCheckBox.setText(language.getString("Options.miscellaneous.disableSplashscreen"));
        showSystrayIconCheckBox.setText(language.getString("Options.miscellaneous.showSysTrayIcon"));
        minimizeToSystrayCheckBox.setText(language.getString("Options.miscellaneous.minimizeToSystray"));
        compactDatabaseAtNextStartupCheckBox.setText(language.getString("Options.miscellaneous.compactStoragesDuringNextStartup"));

        browserAddressLabel.setText(language.getString("Options.miscellaneous.browserAddress"));
    }

    /**
     * Save the settings of this panel
     */
    private void saveSettings() {
        settings.setValue(Settings.FREENET_FCP_ADDRESS, freenetNodeTextField.getText());
        settings.setValue(Settings.SHOW_SYSTRAY_ICON, showSystrayIconCheckBox.isSelected());
        settings.setValue(Settings.MINIMIZE_TO_SYSTRAY, minimizeToSystrayCheckBox.isSelected());
        settings.setValue(Settings.PERST_COMPACT_STORAGES, compactDatabaseAtNextStartupCheckBox.isSelected());
        settings.setValue(Settings.AUTO_SAVE_INTERVAL, autoSaveIntervalTextField.getText());
        settings.setValue(Settings.AUTO_SAVE_LOCAL_IDENTITIES, autoSaveLocalIdentitiesCheckBox.isSelected());
        settings.setValue(Settings.DISABLE_SPLASHSCREEN, splashScreenCheckBox.isSelected());
        settings.setValue(Settings.FCP2_USE_DDA, useDDACheckBox.isSelected());
        settings.setValue(Settings.FCP2_USE_PERSISTENCE, usePersistenceCheckBox.isSelected());
        settings.setValue(Settings.BROWSER_ADDRESS, browserAddressTextField.getText());
    }
}
