/*
  DisplayBoardTreePanel.java / Frost
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

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import frost.Settings;
import frost.util.gui.FontChooser;
import frost.util.gui.MiscToolkit;
import frost.util.gui.translation.Language;

public class DisplayBoardTreePanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private class Listener implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            if( e.getSource() == selectedColorButton ) {
                selectedColorPressed();
            } else if( e.getSource() == notSelectedColorButton ) {
                notSelectedColorPressed();
            } else if( e.getSource() == showBoardUpdateVisualizationCheckBox ) {
                refreshUpdateState();
            } else if( e.getSource() == boardTreeButton ) {
                boardTreeButtonPressed();
            }
        }
    }

    private JDialog owner = null;
    private Settings settings = null;
	private transient Language language;

    private final JCheckBox showBoardDescTooltipsCheckBox = new JCheckBox();
    private final JCheckBox showBoardUpdateCountCheckBox = new JCheckBox();
    private final JCheckBox preventBoardtreeReordering = new JCheckBox();
    private final JCheckBox showFlaggedStarredIndicators = new JCheckBox();

    private final JCheckBox showBoardUpdateVisualizationCheckBox = new JCheckBox();
    private JPanel colorPanel = null;
    private final JButton selectedColorButton = new JButton();
    private final JLabel selectedColorTextLabel = new JLabel();
    private final JLabel selectedColorLabel = new JLabel();
    private final JButton notSelectedColorButton = new JButton();
    private final JLabel notSelectedColorTextLabel = new JLabel();
    private final JLabel notSelectedColorLabel = new JLabel();
    private Color selectedColor = null;
    private Color notSelectedColor = null;

    private final Listener listener = new Listener();

    // fields for font panel
    private final JLabel boardTreeLabel = new JLabel();
    private final JButton boardTreeButton = new JButton();
    private final JLabel selectedBoardTreeFontLabel = new JLabel();
    private final JLabel fontsLabel = new JLabel();
    private Font selectedBodyFont = null;

    /**
     * @param owner the JDialog that will be used as owner of any dialog that is popped up from this panel
     * @param settings the SettingsClass instance that will be used to get and store the settings of the panel
     */
    protected DisplayBoardTreePanel(final JDialog owner, final Settings settings) {
        super();

        this.owner = owner;
        this.language = Language.getInstance();
        this.settings = settings;

        initialize();
        loadSettings();
    }

    public void cancel() {
    }

    private JPanel getColorPanel() {
        if (colorPanel == null) {

            colorPanel = new JPanel(new GridBagLayout());
            colorPanel.setBorder(new EmptyBorder(5, 30, 5, 5));
            final GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(0, 5, 5, 5);
            constraints.weighty = 1;
            constraints.weightx = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;

            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 0.5;
            colorPanel.add(selectedColorTextLabel, constraints);
            constraints.fill = GridBagConstraints.VERTICAL;
            constraints.gridx = 1;
            constraints.weightx = 0.2;
            colorPanel.add(selectedColorLabel, constraints);
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 2;
            constraints.weightx = 0.5;
            colorPanel.add(selectedColorButton, constraints);

            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 0.5;
            colorPanel.add(notSelectedColorTextLabel, constraints);
            constraints.fill = GridBagConstraints.VERTICAL;
            constraints.gridx = 1;
            constraints.weightx = 0.2;
            colorPanel.add(notSelectedColorLabel, constraints);
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridx = 2;
            constraints.weightx = 0.5;
            colorPanel.add(notSelectedColorButton, constraints);

            selectedColorLabel.setOpaque(true);
            notSelectedColorLabel.setOpaque(true);
            selectedColorLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
            notSelectedColorLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
            selectedColorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            notSelectedColorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        }
        return colorPanel;
    }

    /**
     * Initialize the class.
     */
    private void initialize() {
        setName("DisplayPanel");
        setLayout(new GridBagLayout());
        refreshLanguage();

        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        final Insets inset5511 = new Insets(5, 5, 1, 1);

        constraints.insets = inset5511;
        constraints.gridx = 0;
        constraints.gridy = 0;

        add(fontsLabel,constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridy++;
        add(getFontsPanel(),constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridy++;
        add(showBoardUpdateVisualizationCheckBox, constraints);

        constraints.gridy++;
        add(getColorPanel(), constraints);

        constraints.gridy++;
        add(showBoardUpdateCountCheckBox, constraints);

        constraints.gridy++;
        add(showBoardDescTooltipsCheckBox, constraints);

        constraints.gridy++;
        add(preventBoardtreeReordering, constraints);

        constraints.gridy++;
        add(showFlaggedStarredIndicators, constraints);

        constraints.gridy++;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        add(new JLabel(""), constraints);

        // Add listeners
        selectedColorButton.addActionListener(listener);
        notSelectedColorButton.addActionListener(listener);
        showBoardUpdateVisualizationCheckBox.addActionListener(listener);
        boardTreeButton.addActionListener(listener);
    }

	/**
	 * Load the settings of this panel
	 */
	private void loadSettings() {
		selectedBodyFont = settings.getFont(Settings.BOARD_TREE_FONT_NAME, Settings.BOARD_TREE_FONT_STYLE,
				Settings.BOARD_TREE_FONT_SIZE, null);
        selectedBoardTreeFontLabel.setText(getFontLabel(selectedBodyFont));

        showBoardUpdateCountCheckBox.setSelected(settings.getBoolean(Settings.SHOW_BOARD_UPDATED_COUNT));
        showBoardDescTooltipsCheckBox.setSelected(settings.getBoolean(Settings.SHOW_BOARDDESC_TOOLTIPS));
        preventBoardtreeReordering.setSelected(settings.getBoolean(Settings.PREVENT_BOARDTREE_REORDERING));
        showFlaggedStarredIndicators.setSelected(settings.getBoolean(Settings.SHOW_BOARDTREE_FLAGGEDSTARRED_INDICATOR));

        showBoardUpdateVisualizationCheckBox.setSelected(settings.getBoolean(Settings.BOARD_UPDATE_VISUALIZATION_ENABLED));
        refreshUpdateState();

        selectedColor = (Color) settings.getObjectValue(Settings.BOARD_UPDATE_VISUALIZATION_BGCOLOR_SELECTED);
        notSelectedColor = (Color) settings.getObjectValue(Settings.BOARD_UPDATE_VISUALIZATION_BGCOLOR_NOT_SELECTED);
        selectedColorLabel.setBackground(selectedColor);
        notSelectedColorLabel.setBackground(notSelectedColor);
    }

    public void ok() {
        saveSettings();
    }

    private void refreshLanguage() {
    	fontsLabel.setText(language.getString("Options.display.fonts"));
    	boardTreeLabel.setText(language.getString("Options.display.boardTree"));
        boardTreeButton.setText(language.getString("Options.display.choose"));
        selectedBoardTreeFontLabel.setText(getFontLabel(selectedBodyFont));

        showBoardUpdateCountCheckBox.setText(language.getString("Options.display.showBoardUpdateCount"));
        showBoardDescTooltipsCheckBox.setText(language.getString("Options.display.showTooltipWithBoardDescriptionInBoardTree"));
        preventBoardtreeReordering.setText(language.getString("Options.display.preventBoardtreeReordering"));
        showFlaggedStarredIndicators.setText(language.getString("Options.display.showBoardtreeFlaggedStarredIndicators"));

        final String on = language.getString("Options.common.on");
        final String color = language.getString("Options.news.3.color");
        final String choose = language.getString("Options.news.3.choose");
        showBoardUpdateVisualizationCheckBox.setText(language.getString("Options.news.3.showBoardUpdateVisualization") + " (" + on + ")");
        selectedColorTextLabel.setText(language.getString("Options.news.3.backgroundColorIfUpdatingBoardIsSelected"));
        selectedColorLabel.setText("    " + color + "    ");
        selectedColorButton.setText(choose);
        notSelectedColorTextLabel.setText(language.getString("Options.news.3.backgroundColorIfUpdatingBoardIsNotSelected"));
        notSelectedColorLabel.setText("    " + color + "    ");
        notSelectedColorButton.setText(choose);
    }

    /**
     * Save the settings of this panel
     */
    private void saveSettings() {
        if( selectedBodyFont != null ) {
            settings.setValue(Settings.BOARD_TREE_FONT_NAME, selectedBodyFont.getFamily());
            settings.setValue(Settings.BOARD_TREE_FONT_STYLE, selectedBodyFont.getStyle());
            settings.setValue(Settings.BOARD_TREE_FONT_SIZE, selectedBodyFont.getSize());
        }

        settings.setValue(Settings.SHOW_BOARD_UPDATED_COUNT, showBoardUpdateCountCheckBox.isSelected());
        settings.setValue(Settings.SHOW_BOARDDESC_TOOLTIPS, showBoardDescTooltipsCheckBox.isSelected());
        settings.setValue(Settings.PREVENT_BOARDTREE_REORDERING, preventBoardtreeReordering.isSelected());
        settings.setValue(Settings.SHOW_BOARDTREE_FLAGGEDSTARRED_INDICATOR, showFlaggedStarredIndicators.isSelected());

        settings.setValue(Settings.BOARD_UPDATE_VISUALIZATION_ENABLED, showBoardUpdateVisualizationCheckBox.isSelected());
        settings.setObjectValue(Settings.BOARD_UPDATE_VISUALIZATION_BGCOLOR_SELECTED, selectedColor);
        settings.setObjectValue(Settings.BOARD_UPDATE_VISUALIZATION_BGCOLOR_NOT_SELECTED, notSelectedColor);
    }

    private void selectedColorPressed() {
        final Color newCol =
            JColorChooser.showDialog(
                getTopLevelAncestor(),
                language.getString("Options.news.3.colorChooserDialog.title.chooseUpdatingColorOfSelectedBoards"),
                selectedColor);
        if (newCol != null) {
            selectedColor = newCol;
            selectedColorLabel.setBackground(selectedColor);
        }
    }

    private void notSelectedColorPressed() {
        final Color newCol =
            JColorChooser.showDialog(
                getTopLevelAncestor(),
                language.getString("Options.news.3.colorChooserDialog.title.chooseUpdatingColorOfUnselectedBoards"),
                notSelectedColor);
        if (newCol != null) {
            notSelectedColor = newCol;
            notSelectedColorLabel.setBackground(notSelectedColor);
        }
    }

    private void refreshUpdateState() {
        MiscToolkit.setContainerEnabled(getColorPanel(), showBoardUpdateVisualizationCheckBox.isSelected());
    }

    private JPanel getFontsPanel() {
        final JPanel fontsPanel = new JPanel(new GridBagLayout());
        fontsPanel.setBorder(new EmptyBorder(5, 20, 5, 5));
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        final Insets inset1515 = new Insets(1, 5, 1, 5);
        final Insets inset1519 = new Insets(1, 5, 1, 9);

        constraints.insets = inset1515;
        constraints.gridx = 0;
        constraints.gridy = 0;
        fontsPanel.add(boardTreeLabel, constraints);
        constraints.insets = inset1519;
        constraints.gridx = 1;
        constraints.gridy = 0;
        fontsPanel.add(boardTreeButton, constraints);
        constraints.insets = inset1515;
        constraints.gridx = 2;
        constraints.gridy = 0;
        fontsPanel.add(selectedBoardTreeFontLabel, constraints);

        return fontsPanel;
    }

    private String getFontLabel(final Font font) {
        if (font == null) {
            return "";
        } else {
            final StringBuilder returnValue = new StringBuilder();
            returnValue.append(font.getFamily());
            if (font.isBold()) {
                returnValue.append(" " + language.getString("Options.display.fontChooser.bold"));
            }
            if (font.isItalic()) {
                returnValue.append(" " + language.getString("Options.display.fontChooser.italic"));
            }
            returnValue.append(", " + font.getSize());
            return returnValue.toString();
        }
    }

    private void boardTreeButtonPressed() {
        final FontChooser fontChooser = new FontChooser(owner, language);
        fontChooser.setModal(true);
        fontChooser.setSelectedFont(selectedBodyFont);
        fontChooser.setVisible(true);
        final Font selectedFontTemp = fontChooser.getSelectedFont();
        if (selectedFontTemp != null) {
            selectedBodyFont = selectedFontTemp;
            selectedBoardTreeFontLabel.setText(getFontLabel(selectedBodyFont));
        }
    }
}
