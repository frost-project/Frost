/*
 FontChooser.java / Frost
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
package frost.util.gui;

import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;
import frost.util.gui.translation.TranslatableListModel;

/**
 * @author $Author: artur8ur $
 * @version $Revision: 3244 $
 */
public class FontChooser extends JDialog {

	private static final long serialVersionUID = 1L;

	private class Listener implements ActionListener, ListSelectionListener, LanguageListener {

		public Listener() {
			super();
		}

		public void actionPerformed(final ActionEvent e) {
			if (e.getSource() == okButton) {
				okButtonPressed();
			}
			if (e.getSource() == cancelButton) {
				cancelButtonPressed();
			}
			if (e.getSource() == selectedSizeTextField) {
				sizeTyped();
			}
		}

		public void valueChanged(final ListSelectionEvent e) {
			if (!e.getValueIsAdjusting()) { // We ignore adjusting events
				if (e.getSource() == fontNamesList) {
					fontNameValueChanged();
				}
				if (e.getSource() == fontStylesList) {
					fontStyleValueChanged();
				}
				if (e.getSource() == fontSizesList) {
					fontSizeValueChanged();
				}
			}
		}

		public void languageChanged(final LanguageEvent event) {
			refreshLanguage();
		}
	}

	private final Listener listener = new Listener();

	private Language language = null;

	private final DefaultListModel<String> fontNamesModel = new DefaultListModel<>();
	private final JList<String> fontNamesList = new JList<>(fontNamesModel);
	private final JTextField selectedNameTextField = new JTextField();

	private TranslatableListModel fontStylesModel = null;
	private final JList<String> fontStylesList = new JList<>();
	private final JTextField selectedStyleTextField = new JTextField();

	private final DefaultListModel<String> fontSizesModel = new DefaultListModel<>();
	private final JList<String> fontSizesList = new JList<>(fontSizesModel);
	private final JTextField selectedSizeTextField = new JTextField();

	private final JLabel sampleLabel = new JLabel();
	private final JTextField sampleTextField = new JTextField();

	private final JButton cancelButton = new JButton();
	private final JButton okButton = new JButton();

	private Font selectedFont = null;
	private String selectedName = null;
	private Integer selectedSize = null;
	private Integer selectedStyle = null;

	private final HashMap<String,Integer> styles = new HashMap<String,Integer>();

	public FontChooser(final Frame owner, final Language language) {
		super(owner);
		this.language = language;
		language.addLanguageListener(listener);
		initialize();
	}

	public FontChooser(final Dialog owner, final Language language) {
		super(owner);
		this.language = language;
		language.addLanguageListener(listener);
		initialize();
	}

	private void initialize() {
		refreshLanguage();
		setSize(400, 350);
		setLocationRelativeTo(getOwner());

		new TextComponentClipboardMenu(selectedNameTextField, language);
		new TextComponentClipboardMenu(selectedStyleTextField, language);
		new TextComponentClipboardMenu(selectedSizeTextField, language);
		new TextComponentClipboardMenu(sampleTextField, language);

		final JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
		contentPane.setLayout(new GridBagLayout());
		setContentPane(contentPane);

		final GridBagConstraints constraints = new GridBagConstraints();
		final Insets insets4444 = new Insets(4, 4, 4, 4);
		constraints.insets = insets4444;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1;
		constraints.weighty = 1;

		//Adds the JLists
		constraints.gridx = 0;
		constraints.gridy = 0;
		fontNamesList.setBorder(new EmptyBorder(1, 1, 1, 4));
		JScrollPane scrollPane = new JScrollPane(fontNamesList);
		contentPane.add(scrollPane, constraints);
		constraints.gridx = 1;
		constraints.gridy = 0;
		scrollPane = new JScrollPane(fontStylesList);
		fontStylesList.setBorder(new EmptyBorder(1, 1, 1, 4));
		contentPane.add(scrollPane, constraints);
		constraints.gridx = 2;
		constraints.gridy = 0;
		scrollPane = new JScrollPane(fontSizesList);
		fontSizesList.setBorder(new EmptyBorder(1, 1, 1, 4));
		contentPane.add(scrollPane, constraints);

		//Adds the TextFields below
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.weighty = 0;
		selectedNameTextField.setEditable(false);
		contentPane.add(selectedNameTextField, constraints);
		constraints.gridx = 1;
		constraints.gridy = 1;
		selectedStyleTextField.setEditable(false);
		contentPane.add(selectedStyleTextField, constraints);
		constraints.gridx = 2;
		constraints.gridy = 1;
		contentPane.add(selectedSizeTextField, constraints);

		//Adds the sample label and textField
		constraints.gridx = 0;
		constraints.gridy = 3;
		contentPane.add(sampleLabel, constraints);

		constraints.gridx = 0;
		constraints.gridy = 4;
		constraints.gridwidth = 3;
		constraints.weighty = 1;
		sampleTextField.setText("aAbByYzZ");
		contentPane.add(sampleTextField, constraints);

		//Buttons panel - BEGIN
		final JPanel buttonsPanel = new JPanel(new GridBagLayout());
		final GridBagConstraints panelConstraints = new GridBagConstraints();
		panelConstraints.insets = insets4444;
		panelConstraints.weighty = 1;
		panelConstraints.anchor = GridBagConstraints.EAST;

		panelConstraints.gridx = 0;
		panelConstraints.gridy = 0;
		panelConstraints.weightx = 1;
		buttonsPanel.add(okButton, panelConstraints);

		panelConstraints.gridx = 1;
		panelConstraints.gridy = 0;
		panelConstraints.weightx = 0;
		buttonsPanel.add(cancelButton, panelConstraints);

		constraints.gridx = 0;
		constraints.gridy = 6;
		constraints.weighty = 0;
		contentPane.add(buttonsPanel, constraints);
		// Buttons panel - END

		//Adds listeners
		okButton.addActionListener(listener);
		cancelButton.addActionListener(listener);
		fontNamesList.addListSelectionListener(listener);
		fontStylesList.addListSelectionListener(listener);
		fontSizesList.addListSelectionListener(listener);
		selectedSizeTextField.addActionListener(listener);

		//Fills the JLists
		final GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		final String[] names = environment.getAvailableFontFamilyNames();
		final String[] sizes = { "8", "9", "10", "11", "12", "14", "16", "18", "22", "26", "30", "36", "48", "64" };
		styles.put(language.getString("Options.display.fontChooser.plain"), Font.PLAIN);
		styles.put(language.getString("Options.display.fontChooser.italic"), Font.ITALIC);
		styles.put(language.getString("Options.display.fontChooser.bold"), Font.BOLD);
		styles.put(language.getString("Options.display.fontChooser.boldItalic"), Font.ITALIC | Font.BOLD);

		for( final String element : names ) {
			fontNamesModel.addElement(element);
		}

		for( final String element : sizes ) {
			fontSizesModel.addElement(element);
		}

		fontStylesModel = new TranslatableListModel(language);
		fontStylesList.setModel(fontStylesModel);
		final ArrayList<String> styleKeysList = new ArrayList<String>(styles.keySet());
		Collections.reverse(styleKeysList);	//Because I want "Plain" to be first
		final Iterator<String> styleKeys = styleKeysList.iterator();
		while (styleKeys.hasNext()) {
			fontStylesModel.addElement(styleKeys.next());
		}

		fontNamesList.setSelectedIndex(0);
		fontSizesList.setSelectedIndex(0);
		fontStylesList.setSelectedIndex(0);
	}

	private void cancelButtonPressed() {
		selectedFont = null;
		dispose();
	}
	private void okButtonPressed() {
		dispose();
	}

	private void fontNameValueChanged() {
		if (fontNamesList.getSelectedIndex() != -1) {
			selectedName = fontNamesList.getSelectedValue().toString();
			selectedNameTextField.setText(selectedName);
			refreshFont();
		}
	}

	private void refreshFont() {
		if ((selectedName != null) && (selectedSize != null) && (selectedStyle != null)) {
			selectedFont =
				new Font(selectedName, selectedStyle.intValue(), selectedSize.intValue());
			sampleTextField.setFont(selectedFont);
		}
	}

	private void fontStyleValueChanged() {
		final int selectedIndex = fontStylesList.getSelectedIndex();
		if (selectedIndex != -1) {
			final String styleString = fontStylesModel.getElementAt(selectedIndex).toString();
			final String styleKey = fontStylesModel.getKeyAt(selectedIndex);

			selectedStyleTextField.setText(styleString);
			selectedStyle = styles.get(styleKey);
			refreshFont();
		}
	}

	private void fontSizeValueChanged() {
		if (fontSizesList.getSelectedIndex() != -1) {
			selectedSize = Integer.parseInt(fontSizesList.getSelectedValue());
			selectedSizeTextField.setText(selectedSize.toString());
			refreshFont();
		}
	}

    public Font getSelectedFont() {
		return selectedFont;
	}

	public void setSelectedFont(final Font font) {
		selectedFont = font;
		//Name
		final String familyName = font.getFamily();
		if (fontNamesModel.contains(familyName)) {
			fontNamesList.setSelectedValue(familyName, true);
		}
		//Size
		selectedSize = font.getSize();
		if (fontSizesModel.contains(selectedSize)) {
			fontSizesList.setSelectedValue(selectedSize, true);
		} else {
			selectedSizeTextField.setText(selectedSize.toString());
			//SetText doesn't launch an event, so we simulate it from here:
			sizeTyped();
		}
		//Style
		int stylePos = -1;
		final Integer style = font.getStyle();
		final Iterator<Map.Entry<String,Integer>> styleEntries = styles.entrySet().iterator();
		while (styleEntries.hasNext() && stylePos == -1) {
			final Map.Entry<String,Integer> entry = styleEntries.next();
			if (entry.getValue().equals(style)) {
				stylePos = fontStylesModel.indexOfKey(entry.getKey());
			}
		}
		fontStylesList.setSelectedIndex(stylePos);
	}

	private void sizeTyped() {
		final String size = selectedSizeTextField.getText();
		try {
			selectedSize = Integer.parseInt(size);
			if (fontSizesModel.contains(size)) {
				fontSizesList.setSelectedValue(size, true);
			} else {
				fontSizesList.clearSelection();
			}
			refreshFont();
		} catch (final NumberFormatException exception) {
			//Nothing, just ignore the typed value
		}
	}

	private void refreshLanguage() {
		setTitle(language.getString("Options.display.fontChooser.title"));
		sampleLabel.setText(language.getString("Options.display.fontChooser.sample"));
		okButton.setText(language.getString("Common.ok"));
		cancelButton.setText(language.getString("Common.cancel"));
	}
}
