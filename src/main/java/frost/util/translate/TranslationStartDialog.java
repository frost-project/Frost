/*
  TranslationStartDialog.java / Frost
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
package frost.util.translate;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import frost.Core;
import frost.Settings;
import frost.util.gui.translation.FrostResourceBundle;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageGuiSupport;
import frost.util.gui.translation.TranslateableFrostResourceBundle;

public class TranslationStartDialog extends JFrame {

	private static final long serialVersionUID = 1L;

    Language language = Language.getInstance();

    private JPanel jContentPane = null;
    private JLabel jLabel = null;
	private JComboBox<ComboBoxEntry> comboBoxTargetLanguage = null;
    private JLabel jLabel1 = null;
	private JComboBox<ComboBoxEntry> comboBoxSourceLanguage = null;
    private JPanel jPanel = null;
    private JButton Bok = null;
    private JButton Bcancel = null;
    private JLabel jLabel2 = null;

    /**
     * This is the default constructor
     */
    public TranslationStartDialog(final JFrame parent) {
        super();
        initialize();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * This method initializes this
     *
     * @return void
     */
    private void initialize() {
        this.setSize(495, 276);
        this.setTitle("Translate Frost");
        this.setContentPane(getJContentPane());
    }

    /**
     * This method initializes jContentPane
     *
     * @return JPanel
     */
    private JPanel getJContentPane() {
        if( jContentPane == null ) {
            final GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            gridBagConstraints11.gridx = 0;
            gridBagConstraints11.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints11.insets = new Insets(3,5,0,5);
            gridBagConstraints11.gridy = 3;
            jLabel2 = new JLabel();
            jLabel2.setText("(The fallback language for all missing keys is english.)");
            final GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.gridx = 0;
            gridBagConstraints4.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints4.weightx = 1.0;
            gridBagConstraints4.gridy = 6;
            final GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.fill = GridBagConstraints.NONE;
            gridBagConstraints3.gridy = 4;
            gridBagConstraints3.weightx = 0.0;
            gridBagConstraints3.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints3.insets = new Insets(5,25,10,5);
            gridBagConstraints3.gridx = 0;
            final GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.gridx = 0;
            gridBagConstraints2.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints2.insets = new Insets(15,5,0,5);
            gridBagConstraints2.gridy = 2;
            jLabel1 = new JLabel();
            jLabel1.setText("Choose the language used as source for the translation:");
            final GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.fill = GridBagConstraints.NONE;
            gridBagConstraints1.gridy = 1;
            gridBagConstraints1.weightx = 0.0;
            gridBagConstraints1.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints1.insets = new Insets(5,25,0,5);
            gridBagConstraints1.gridx = 0;
            final GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5,5,0,5);
            gridBagConstraints.gridy = 0;
            jLabel = new JLabel();
            jLabel.setText("Choose the target language to translate into:");
            jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            jContentPane.add(jLabel, gridBagConstraints);
            jContentPane.add(getCBoxTargetLanguage(), gridBagConstraints1);
            jContentPane.add(jLabel1, gridBagConstraints2);
            jContentPane.add(getCBoxSourceLanguage(), gridBagConstraints3);
            jContentPane.add(getJPanel(), gridBagConstraints4);
            jContentPane.add(jLabel2, gridBagConstraints11);
        }
        return jContentPane;
    }

    /**
     * This method initializes CBoxTargetLanguage
     *
     * @return JComboBox
     */
	private JComboBox<ComboBoxEntry> getCBoxTargetLanguage() {
        if( comboBoxTargetLanguage == null ) {
			comboBoxTargetLanguage = new JComboBox<>();
            
            final Locale availableLocales[] = Locale.getAvailableLocales();
            final List<Locale> buildIns = LanguageGuiSupport.getBuildInLocales();
            final List<Locale> externals = Language.getExternalLocales();
            
            final TreeMap<ComboBoxEntry,ComboBoxEntry> tm = new TreeMap<ComboBoxEntry,ComboBoxEntry>();
            
            ComboBoxEntry selectedEntry = null;
            String current_locale = Core.frostSettings.getString(Settings.LANGUAGE_LOCALE);
            if( current_locale.equals("default") ) {
            	current_locale = "en";
            }
            
            for( final Locale element : availableLocales ) {
                if( element.getCountry().length() > 0 ) {
                    // for now we concentrate on the main languages ;)
                    continue;
                }
                
                final String langCode = element.getLanguage();
                String newOrChangeStr;
                boolean isExternal = false;
                boolean isNew = false;
                if( externals.contains(element) ) {
                    newOrChangeStr = "external";
                    isExternal = true;
                } else  if( buildIns.contains(element) ) {
                    newOrChangeStr = "build-in";
                } else {
                    newOrChangeStr = "create";
                    isNew = true;
                }
                final String localeDesc = element.getDisplayName() + "  (" + newOrChangeStr+ ") ("+ langCode + ")";
                final ComboBoxEntry comboBoxEntry = new ComboBoxEntry(element, isExternal, isNew, localeDesc);
                tm.put(comboBoxEntry, comboBoxEntry);
                
                // check if local of item matches current local of frost
                if( langCode.equals(current_locale) ) {
                	selectedEntry = comboBoxEntry;
                }
            }
            // get sorted
			for (final ComboBoxEntry element : tm.keySet()) {
                comboBoxTargetLanguage.addItem(element);
            }
            
            // configure selected item
            if( selectedEntry != null ) {
            	comboBoxTargetLanguage.setSelectedItem(selectedEntry);
            }
        }
        return comboBoxTargetLanguage;
    }

    /**
     * This method initializes CBoxSourceLanguage
     *
     * @return JComboBox
     */
	private JComboBox<ComboBoxEntry> getCBoxSourceLanguage() {
        if( comboBoxSourceLanguage == null ) {
			comboBoxSourceLanguage = new JComboBox<>();

            ComboBoxEntry defaultEntry = null;

            final List<Locale> lst_buildin = LanguageGuiSupport.getBuildInLocales();
            final TreeMap<ComboBoxEntry,ComboBoxEntry> tm_buildin = new TreeMap<ComboBoxEntry,ComboBoxEntry>();
            for( final Locale locale : lst_buildin ) {
                final String localeDesc = locale.getDisplayName() + "  (" + locale.getLanguage() + ")";
                final ComboBoxEntry comboBoxEntry = new ComboBoxEntry(locale, false, false, localeDesc);
                tm_buildin.put(comboBoxEntry, comboBoxEntry);

                if( locale.getLanguage().equals("en") ) {
                    defaultEntry = comboBoxEntry;
                }
            }
            // get sorted
            for( final ComboBoxEntry comboBoxEntry : tm_buildin.keySet() ) {
                comboBoxSourceLanguage.addItem(comboBoxEntry);
            }

            final List<Locale> lst_external = Language.getExternalLocales();
            final TreeMap<ComboBoxEntry,ComboBoxEntry> tm_external = new TreeMap<ComboBoxEntry,ComboBoxEntry>();
            for( final Locale locale2 : lst_external ) {
                final Locale locale = locale2;
                final String localeDesc = locale.getDisplayName() + "  (external) (" + locale.getLanguage() + ")";
                final ComboBoxEntry cbe = new ComboBoxEntry(locale, true, false, localeDesc);
                tm_external.put(cbe, cbe);
            }
            // get sorted
			for (final ComboBoxEntry element : tm_external.keySet()) {
                comboBoxSourceLanguage.addItem(element);
            }

            // select english as source by default
            if( defaultEntry != null ) {
                comboBoxSourceLanguage.setSelectedItem(defaultEntry);
            }
        }
        return comboBoxSourceLanguage;
    }

    /**
     * This method initializes jPanel
     *
     * @return JPanel
     */
    private JPanel getJPanel() {
        if( jPanel == null ) {
            final FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(FlowLayout.RIGHT);
            jPanel = new JPanel();
            jPanel.setLayout(flowLayout);
            jPanel.add(getBok(), null);
            jPanel.add(getBcancel(), null);
        }
        return jPanel;
    }

    /**
     * This method initializes Bok
     *
     * @return JButton
     */
    private JButton getBok() {
        if( Bok == null ) {
            Bok = new JButton();
            Bok.setText("Ok");
            Bok.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    ComboBoxEntry cbe;
                    cbe = (ComboBoxEntry) getCBoxSourceLanguage().getSelectedItem();
                    final String sourceLanguageName = cbe.getLocale().getLanguage();
                    final boolean isSourceExternal = cbe.isExternal();

                    cbe = (ComboBoxEntry) getCBoxTargetLanguage().getSelectedItem();
                    final String targetLanguageName = cbe.getLocale().getLanguage();
                    final boolean isTargetExternal = cbe.isExternal();
                    final boolean isTargetNew = cbe.isNew();

                    startTranslation(
                            sourceLanguageName,
                            isSourceExternal,
                            targetLanguageName,
                            isTargetExternal,
                            isTargetNew);
                }
            });
        }
        return Bok;
    }

    /**
     * Start the translation dialog.
     */
    private void startTranslation(
            final String sourceLanguageName,
            final boolean isSourceExternal,
            final String targetLanguageName,
            final boolean isTargetExternal,
            final boolean isTargetNew)
    {
        setVisible(false);

        FrostResourceBundle sourceBundle;
        TranslateableFrostResourceBundle targetBundle;

        final FrostResourceBundle rootBundle = new FrostResourceBundle(); // english fallback for source language

        if( isSourceExternal ) {
            // load external properties file
            sourceBundle = new FrostResourceBundle(sourceLanguageName, rootBundle, true);
        } else {
            // build in source
            sourceBundle = new FrostResourceBundle(sourceLanguageName, rootBundle, false);
        }

        if( isTargetExternal ) {
            // load external properties file
            targetBundle = new TranslateableFrostResourceBundle(targetLanguageName, null, true);
        } else if( isTargetNew ) {
            // start a new translation, nothing to load
            targetBundle = new TranslateableFrostResourceBundle();
        } else {
            // target is build-in, enhance existing translation
            targetBundle = new TranslateableFrostResourceBundle(targetLanguageName, null, false);
        }

        final TranslationDialog td = new TranslationDialog();
        td.startDialog(rootBundle, sourceBundle, sourceLanguageName, targetBundle, targetLanguageName);

        dispose();
    }

    /**
     * This method initializes Bcancel
     */
    private JButton getBcancel() {
        if( Bcancel == null ) {
            Bcancel = new JButton();
            Bcancel.setText("Cancel");
            Bcancel.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    setVisible(false);
                    dispose();
                }
            });
        }
        return Bcancel;
    }

    private class ComboBoxEntry implements Comparable<Object> {
        boolean isExternal;
        boolean isNew;
        Locale locale;
        String displayString;
        public ComboBoxEntry(final Locale locale, final boolean isExternal, final boolean isNew, final String displayString) {
            this.locale = locale;
            this.isExternal = isExternal;
            this.displayString = displayString;
        }
        public Locale getLocale() {
            return locale;
        }
        public boolean isExternal() {
            return isExternal;
        }
        public boolean isNew() {
            return isNew;
        }
        @Override
        public String toString() {
            return displayString;
        }
        public int compareTo(final Object other) {
            return toString().compareTo(other.toString());
        }
    }
}  //  @jve:decl-index=0:visual-constraint="10,10"
