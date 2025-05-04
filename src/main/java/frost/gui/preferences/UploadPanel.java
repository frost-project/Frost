/*
  UploadPanel.java / Frost
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import frost.Settings;
import frost.util.gui.TextComponentClipboardMenu;
import frost.util.gui.translation.Language;

@SuppressWarnings("serial")
class UploadPanel extends JPanel {

    public class Listener implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == browseExecButton) {
                browseExecPressed();
            }
        }
    }

    private JDialog owner = null;
    private Settings settings = null;
    private Language language = null;

    private final JLabel priorityLabel = new JLabel();
    private final JTextField priorityTextField = new JTextField(6);
    private final JCheckBox enforceFrostPriorityFileUpload = new JCheckBox();

    private final JLabel maxRetriesLabel = new JLabel();
    private final JTextField maxRetriesTextField = new JTextField(6);
    private final JLabel waitTimeLabel = new JLabel();
    private final JTextField waitTimeTextField = new JTextField(6);

    private final JLabel threadsLabel = new JLabel();
    private final JTextField threadsTextField = new JTextField(6);

    private final JCheckBox logUploadsCheckBox = new JCheckBox();

    private final JCheckBox removeNotExistingfiles = new JCheckBox();

    private final Listener listener = new Listener();

    private final JButton browseExecButton = new JButton();
    private final JLabel execLabel = new JLabel();
    private final JTextField execTextField = new JTextField(20);

    /**
     * @param settings the SettingsClass instance that will be used to get and store the settings of the panel
     */
    protected UploadPanel(final Settings settings) {
        super();

        this.language = Language.getInstance();
        this.settings = settings;

        initialize();
        loadSettings();
    }

    private void browseExecPressed() {
        final JFileChooser fc = new JFileChooser(settings.getValue(Settings.DIR_LAST_USED));
        fc.setDialogTitle(language.getString("Options.downloads.filechooser.title"));
        fc.setFileHidingEnabled(true);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);

        final int returnVal = fc.showOpenDialog(owner);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = fc.getSelectedFile();
            settings.setValue(Settings.DIR_LAST_USED, file.getParent());
            execTextField.setText(file.getPath());
        }
    }

    private void initialize() {
        setName("UploadPanel");
        setLayout(new GridBagLayout());
        refreshLanguage();

        // We create the components
        new TextComponentClipboardMenu(threadsTextField, language);
        new TextComponentClipboardMenu(maxRetriesTextField, language);
        new TextComponentClipboardMenu(waitTimeTextField, language);
        new TextComponentClipboardMenu(execTextField, language);

        // Adds all of the components
        final GridBagConstraints constraints = new GridBagConstraints();

        final Insets insets0555 = new Insets(0, 5, 5, 5);

        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.insets = insets0555;
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        add(maxRetriesLabel, constraints);
        constraints.gridx = 1;
        add(maxRetriesTextField, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        add(waitTimeLabel, constraints);
        constraints.gridx = 1;
        add(waitTimeTextField, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        add(threadsLabel, constraints);
        constraints.gridx = 1;
        add(threadsTextField, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        add(priorityLabel, constraints);
        constraints.gridx = 1;
        add(priorityTextField, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 3;
        add(logUploadsCheckBox, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 3;
        add(enforceFrostPriorityFileUpload, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 3;
        add(removeNotExistingfiles, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        add(execLabel, constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        add(execTextField, constraints);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 2;
        constraints.weightx = 0.0;
        add(browseExecButton, constraints);

        // glue
        constraints.gridy++;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        add(new JLabel(""), constraints);

        browseExecButton.addActionListener(listener);
    }

    /**
     * Load the settings of this panel
     */
    private void loadSettings() {
        priorityTextField.setText(settings.getValue(Settings.FCP2_DEFAULT_PRIO_FILE_UPLOAD));
        enforceFrostPriorityFileUpload.setSelected(settings.getBoolValue(Settings.FCP2_ENFORCE_FROST_PRIO_FILE_UPLOAD));
        threadsTextField.setText(settings.getValue(Settings.UPLOAD_MAX_THREADS));
        maxRetriesTextField.setText("" + settings.getIntValue(Settings.UPLOAD_MAX_RETRIES));
        waitTimeTextField.setText("" + settings.getIntValue(Settings.UPLOAD_WAITTIME));
        logUploadsCheckBox.setSelected(settings.getBoolValue(Settings.LOG_UPLOADS_ENABLED));
        removeNotExistingfiles.setSelected(settings.getBoolValue(Settings.UPLOAD_REMOVE_NOT_EXISTING_FILES));
        execTextField.setText(settings.getValue(Settings.EXEC_ON_UPLOAD));
    }

    public void ok() {
        saveSettings();
    }

    private void refreshLanguage() {
        final String minutes = language.getString("Options.common.minutes");

        waitTimeLabel.setText(language.getString("Options.uploads.waittimeAfterEachTry") + " (" + minutes + ")");
        maxRetriesLabel.setText(language.getString("Options.uploads.maximumNumberOfRetries"));
        threadsLabel.setText(language.getString("Options.uploads.numberOfSimultaneousUploads") + " (3)");
        logUploadsCheckBox.setText(language.getString("Options.uploads.logUploads"));
        priorityLabel.setText(language.getString("Options.uploads.uploadPriority") + " (3)");
        enforceFrostPriorityFileUpload.setText(language.getString("Options.uploads.enforceFrostPriorityFileUpload"));
        removeNotExistingfiles.setText(language.getString("Options.uploads.removeNotExistingFiles"));
        execLabel.setText(language.getString("Options.uploads.uploadExec"));
        browseExecButton.setText(language.getString("Common.browse") + "...");
    }

    /**
     * Save the settings of this panel
     */
    private void saveSettings() {
        settings.setValue(Settings.FCP2_DEFAULT_PRIO_FILE_UPLOAD, priorityTextField.getText());
        settings.setValue(Settings.FCP2_ENFORCE_FROST_PRIO_FILE_UPLOAD, enforceFrostPriorityFileUpload.isSelected());
        settings.setValue(Settings.UPLOAD_MAX_THREADS, threadsTextField.getText());
        settings.setValue(Settings.UPLOAD_MAX_RETRIES, maxRetriesTextField.getText());
        settings.setValue(Settings.UPLOAD_WAITTIME, waitTimeTextField.getText());
        settings.setValue(Settings.LOG_UPLOADS_ENABLED, logUploadsCheckBox.isSelected());
        settings.setValue(Settings.UPLOAD_REMOVE_NOT_EXISTING_FILES, removeNotExistingfiles.isSelected());
        settings.setValue(Settings.EXEC_ON_UPLOAD, execTextField.getText());
    }
}
