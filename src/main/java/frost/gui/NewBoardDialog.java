/*
  NewBoardDialog.java / Frost
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
package frost.gui;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import frost.util.gui.TextComponentClipboardMenu;
import frost.util.gui.translation.Language;

public class NewBoardDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private class Listener implements DocumentListener, ActionListener {

        public void changedUpdate(DocumentEvent e) {
            if (e.getDocument() == nameTextField.getDocument()) {
                updateAddButtonState();
            }
        }

        public void insertUpdate(DocumentEvent e) {
            if (e.getDocument() == nameTextField.getDocument()) {
                updateAddButtonState();
            }
        }

        public void removeUpdate(DocumentEvent e) {
            if (e.getDocument() == nameTextField.getDocument()) {
                updateAddButtonState();
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == addButton) {
                addButton_actionPerformed();
            } else if (e.getSource() == cancelButton) {
                cancelButton_actionPerformed();
            }
        }
    }

    public static int CHOICE_ADD = 1;
    public static int CHOICE_CANCEL = 2;

    private Listener listener = new Listener();

	private transient Language language;

    private JPanel contentPanel = new JPanel();

    private JLabel detailsLabel = new JLabel();
    private JLabel descriptionLabel = new JLabel();
    private JLabel nameLabel = new JLabel();
    private JTextField nameTextField = new JTextField(40);
    private JButton cancelButton = new JButton();
    private JButton addButton = new JButton();
    private JTextArea descriptionTextArea = new JTextArea(3, 40);
    private JScrollPane descriptionScrollPane;

    private int choice = CHOICE_CANCEL;
    private String boardName;
    private String boardDescription;

    public NewBoardDialog(Frame owner) {
        super(owner);
        this.language = Language.getInstance();
        initialize();
        pack();
        setLocationRelativeTo(owner);
        setModal(true);
    }

    private void initialize() {
        contentPanel.setBorder(new EmptyBorder(15,15,15,15));
        setContentPane(contentPanel);
        contentPanel.setLayout(new GridBagLayout());
        refreshLanguage();

        new TextComponentClipboardMenu(nameTextField, language);
        new TextComponentClipboardMenu(descriptionTextArea, language);

        // Adds all of the components
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        Insets insets5555 = new Insets(5,5,5,5);
        Insets insets10_555 = new Insets(10,5,5,5);
        constraints.insets = insets5555;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.gridwidth = 3;

        contentPanel.add(detailsLabel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        contentPanel.add(nameLabel, constraints);
        constraints.gridy = 2;
        contentPanel.add(nameTextField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        contentPanel.add(descriptionLabel, constraints);
        constraints.gridy = 4;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        descriptionScrollPane = new JScrollPane(descriptionTextArea);
        contentPanel.add(descriptionScrollPane, constraints);

        constraints.gridwidth = 1;
        constraints.weightx = 2;
        constraints.weighty = 0;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = insets10_555;
        constraints.gridx = 1;
        constraints.gridy = 5;
        contentPanel.add(addButton, constraints);
        constraints.weightx = 0;
        constraints.gridx = 2;
        contentPanel.add(cancelButton, constraints);

        addButton.setEnabled(false);

        nameTextField.getDocument().addDocumentListener(listener);
        addButton.addActionListener(listener);
        cancelButton.addActionListener(listener);
    }

    private void refreshLanguage() {
        setTitle(" " + language.getString("NewBoardDialog.title"));
        detailsLabel.setText(language.getString("NewBoardDialog.details"));
        nameLabel.setText(language.getString("NewBoardDialog.name"));
        descriptionLabel.setText(language.getString("NewBoardDialog.description"));
        addButton.setText(language.getString("NewBoardDialog.add"));
        cancelButton.setText(language.getString("Common.cancel"));
    }

    private void updateAddButtonState() {
        if (nameTextField.getText().trim().length() == 0) {
            addButton.setEnabled(false);
        } else {
            addButton.setEnabled(true);
        }
    }

    private void addButton_actionPerformed() {
        
        boardName = nameTextField.getText().trim();
        boardDescription = descriptionTextArea.getText().trim();
        
        choice = CHOICE_ADD;
        dispose();
    }
    
    private void cancelButton_actionPerformed() {
        dispose();
    }

    public int getChoice() {
        return choice;
    }

    public String getBoardDescription() {
        return boardDescription;
    }

    public String getBoardName() {
        return boardName;
    }
}
