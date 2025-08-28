/*
  ExpirationPanel.java / Frost
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

import javax.swing.ButtonGroup;
import javax.swing.InputVerifier;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import frost.Settings;
import frost.util.gui.TextComponentClipboardMenu;
import frost.util.gui.translation.Language;
import frost.util.gui.verify.PositiveIntegerVerifier;

public class ExpirationPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private transient Settings settings;
	private transient Language language;

    private final JLabel LcleanupIntervalDays = new JLabel();
    private final JTextField TfCleanupIntervalDays = new JTextField(8);
    private final JCheckBox CbCleanupNextStartup = new JCheckBox();

    private final JRadioButton RbKeepExpiredMessages = new JRadioButton();
    private final JRadioButton RbArchiveExpiredMessages = new JRadioButton();
    private final JRadioButton RbDeleteExpiredMessages = new JRadioButton();
    private final ButtonGroup BgExpiredMessages = new ButtonGroup();

    private final JLabel LmessageExpireDays = new JLabel();
    private final JTextField TfMessageExpireDays = new JTextField(8);

    private final JCheckBox CbKeepFlaggedAndStarred = new JCheckBox();
    private final JCheckBox CbKeepUnread = new JCheckBox();

    private final JCheckBox CbRemoveOfflineFilesWithKey = new JCheckBox();
    private final JLabel LofflineFilesMaxDaysOld = new JLabel();
    private final JTextField TfOfflineFilesMaxDaysOld = new JTextField(8);

    /**
     * @param settings the SettingsClass instance that will be used to get and store the settings of the panel
     */
    protected ExpirationPanel(final JDialog owner, final Settings settings) {
        super();
        this.language = Language.getInstance();
        this.settings = settings;

        initialize();
        loadSettings();
    }

    private void initialize() {
        setName("ExpirationPanel");
        setLayout(new GridBagLayout());
        refreshLanguage();

        // We create the components
        new TextComponentClipboardMenu(TfMessageExpireDays, language);

		InputVerifier integerVerifier = new PositiveIntegerVerifier();
		TfCleanupIntervalDays.setInputVerifier(integerVerifier);
		TfMessageExpireDays.setInputVerifier(integerVerifier);
		TfOfflineFilesMaxDaysOld.setInputVerifier(integerVerifier);

        // Adds all of the components
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        final Insets insets0555 = new Insets(0, 5, 5, 5);

        constraints.insets = insets0555;
        constraints.gridy = 0;

        {
            final JPanel subPanel = new JPanel(new GridBagLayout());
            final GridBagConstraints subConstraints = new GridBagConstraints();
            subConstraints.insets = new Insets(0,0,0,10);
            subConstraints.gridx = 0;
            subPanel.add(LcleanupIntervalDays, subConstraints);
            subConstraints.gridx = 1;
            subPanel.add(TfCleanupIntervalDays, subConstraints);

            add(subPanel, constraints);
        }

        constraints.gridy++;

        constraints.gridx = 0;
        add(CbCleanupNextStartup, constraints);

        constraints.gridy++;

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;

        {
            final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
            add(separator, constraints);
        }

        constraints.gridy++;

        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;

        {
            final JPanel subPanel = new JPanel(new GridBagLayout());
            final GridBagConstraints subConstraints = new GridBagConstraints();
            subConstraints.insets = new Insets(0,0,0,10);
            subConstraints.gridx = 0;
            subPanel.add(LmessageExpireDays, subConstraints);
            subConstraints.gridx = 1;
            subPanel.add(TfMessageExpireDays, subConstraints);

            add(subPanel, constraints);
        }

        constraints.gridy++;

        constraints.gridx = 0;
        add(RbKeepExpiredMessages, constraints);

        constraints.gridy++;

        constraints.gridx = 0;
        add(RbArchiveExpiredMessages, constraints);

        constraints.gridy++;

        constraints.gridx = 0;
        add(RbDeleteExpiredMessages, constraints);

        constraints.gridy++;

        constraints.gridx = 0;
        add(CbKeepUnread, constraints);

        constraints.gridy++;

        constraints.gridx = 0;
        add(CbKeepFlaggedAndStarred, constraints);

        constraints.gridy++;

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;

        {
            final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
            add(separator, constraints);
        }

        constraints.gridy++;

        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;

        {
            final JPanel subPanel = new JPanel(new GridBagLayout());
            final GridBagConstraints subConstraints = new GridBagConstraints();
            subConstraints.insets = new Insets(0,0,0,10);
            subConstraints.gridx = 0;
            subPanel.add(LofflineFilesMaxDaysOld, subConstraints);
            subConstraints.gridx = 1;
            subPanel.add(TfOfflineFilesMaxDaysOld, subConstraints);

            add(subPanel, constraints);
        }

        constraints.gridy++;

        add(CbRemoveOfflineFilesWithKey, constraints);

        // glue
        constraints.gridy++;
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        add(new JLabel(""), constraints);

        // add radiobuttons to buttongroup
        BgExpiredMessages.add( RbKeepExpiredMessages );
        BgExpiredMessages.add( RbArchiveExpiredMessages );
        BgExpiredMessages.add( RbDeleteExpiredMessages );
    }

    /**
     * Load the settings of this panel
     */
    private void loadSettings() {

        TfMessageExpireDays.setText(settings.getString(Settings.MESSAGE_EXPIRE_DAYS));

        final String mode = settings.getString(Settings.MESSAGE_EXPIRATION_MODE);
        if( mode.toUpperCase().equals("KEEP") ) {
            RbKeepExpiredMessages.doClick();
        } else if( mode.toUpperCase().equals("ARCHIVE") ) {
            RbArchiveExpiredMessages.doClick();
        } else if( mode.toUpperCase().equals("DELETE") ) {
            RbDeleteExpiredMessages.doClick();
        } else {
            RbKeepExpiredMessages.doClick(); // // unknown value, use default
        }

        CbKeepFlaggedAndStarred.setSelected(settings.getBoolean(Settings.ARCHIVE_KEEP_FLAGGED_AND_STARRED));
        CbKeepUnread.setSelected(settings.getBoolean(Settings.ARCHIVE_KEEP_UNREAD));

        CbRemoveOfflineFilesWithKey.setSelected(settings.getBoolean(Settings.DB_CLEANUP_REMOVEOFFLINEFILEWITHKEY));
        TfOfflineFilesMaxDaysOld.setText(settings.getString(Settings.DB_CLEANUP_OFFLINEFILESMAXDAYSOLD));

        TfCleanupIntervalDays.setText(settings.getString(Settings.DB_CLEANUP_INTERVAL));
        if( settings.getLong(Settings.DB_CLEANUP_LASTRUN) == 0 ) {
            CbCleanupNextStartup.setSelected(true);
        } else {
            CbCleanupNextStartup.setSelected(false);
        }
    }

    /**
     * Save the settings of this panel
     */
    private void saveSettings() {

        settings.setValue(Settings.MESSAGE_EXPIRE_DAYS, TfMessageExpireDays.getText());

        if( RbKeepExpiredMessages.isSelected() ) {
            settings.setValue(Settings.MESSAGE_EXPIRATION_MODE, "KEEP");
        } else if( RbArchiveExpiredMessages.isSelected() ) {
            settings.setValue(Settings.MESSAGE_EXPIRATION_MODE, "ARCHIVE");
        } else if( RbDeleteExpiredMessages.isSelected() ) {
            settings.setValue(Settings.MESSAGE_EXPIRATION_MODE, "DELETE");
        } else {
            settings.setValue(Settings.MESSAGE_EXPIRATION_MODE, "KEEP");
        }

        settings.setValue(Settings.ARCHIVE_KEEP_FLAGGED_AND_STARRED, CbKeepFlaggedAndStarred.isSelected());
        settings.setValue(Settings.ARCHIVE_KEEP_UNREAD, CbKeepUnread.isSelected());

        settings.setValue(Settings.DB_CLEANUP_REMOVEOFFLINEFILEWITHKEY, CbRemoveOfflineFilesWithKey.isSelected());
        settings.setValue(Settings.DB_CLEANUP_OFFLINEFILESMAXDAYSOLD, TfOfflineFilesMaxDaysOld.getText());

        settings.setValue(Settings.DB_CLEANUP_INTERVAL, TfCleanupIntervalDays.getText());
        if( CbCleanupNextStartup.isSelected() ) {
            settings.setValue(Settings.DB_CLEANUP_LASTRUN, 0L);
        }
    }

    public void ok() {
        saveSettings();
    }

    private void refreshLanguage() {
        RbKeepExpiredMessages.setText(language.getString("Options.expiration.keepExpiredMessages"));
        RbArchiveExpiredMessages.setText(language.getString("Options.expiration.archiveExpiredMessages"));
        RbDeleteExpiredMessages.setText(language.getString("Options.expiration.deleteExpiredMessages"));

        LmessageExpireDays.setText(language.getString("Options.expiration.numberOfDaysBeforeMessageExpires") + " (90)");

        CbKeepFlaggedAndStarred.setText(language.getString("Options.expiration.keepFlaggedAndStarredMessages"));
        CbKeepUnread.setText(language.getString("Options.expiration.keepUnreadMessages"));

        CbRemoveOfflineFilesWithKey.setText(language.getString("Options.expiration.removeOfflineFilesWithKey"));
        LofflineFilesMaxDaysOld.setText(language.getString("Options.expiration.offlineFilesMaxDaysOld")+" (30)");

        LcleanupIntervalDays.setText(language.getString("Options.expiration.cleanupIntervalDays") +
                " (5 "+language.getString("Options.common.days")+")");
        CbCleanupNextStartup.setText(language.getString("Options.expiration.cleanupNextStartup"));
    }
}
