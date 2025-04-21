/*
 StatisticsDialog.java / Frost
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
package frost.gui;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import frost.util.gui.translation.Language;

public class StatisticsDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;
    private JLabel LmsgCountLabel = null;
    private JLabel LmsgCount = null;
    private JLabel LarcMsgCountLabel = null;
    private JLabel LarcMsgCount = null;
    private JLabel LidCountLabel = null;
    private JLabel LidCount = null;
    private JLabel LfileCountLabel = null;
    private JLabel LfileCount = null;
    private JButton Bclose = null;
    private JLabel glueLabel = null;
    private JLabel LfileSizeLabel = null;
    private JLabel LfileSize = null;
    private JLabel LsharerCountLabel = null;
    private JLabel LsharerCount = null;

    Frame owner;

    /**
     * @param owner
     */
    public StatisticsDialog(final Frame owner) {
        super(owner);
        this.owner = owner;
        initialize();
    }

    /**
     * This method initializes this
     *
     * @return void
     */
    private void initialize() {
        this.setSize(423, 313);
        this.setTitle("StatisticsDialog.title");
        this.setContentPane(getJContentPane());
    }

    /**
     * This method initializes jContentPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if( jContentPane == null ) {
            final GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
            gridBagConstraints10.gridx = 1;
            gridBagConstraints10.insets = new Insets(8, 5, 0, 10);
            gridBagConstraints10.anchor = GridBagConstraints.EAST;
            gridBagConstraints10.gridy = 3;
            LsharerCount = new JLabel();
            LsharerCount.setText("JLabel");
            final GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
            gridBagConstraints9.gridx = 0;
            gridBagConstraints9.insets = new Insets(8, 10, 0, 5);
            gridBagConstraints9.anchor = GridBagConstraints.WEST;
            gridBagConstraints9.gridy = 3;
            LsharerCountLabel = new JLabel();
            LsharerCountLabel.setText("Sharers");
            final GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
            gridBagConstraints8.gridx = 1;
            gridBagConstraints8.anchor = GridBagConstraints.EAST;
            gridBagConstraints8.insets = new Insets(3, 5, 0, 10);
            gridBagConstraints8.gridy = 5;
            LfileSize = new JLabel();
            LfileSize.setText("JLabel");
            final GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
            gridBagConstraints7.gridx = 0;
            gridBagConstraints7.anchor = GridBagConstraints.WEST;
            gridBagConstraints7.insets = new Insets(3, 10, 0, 5);
            gridBagConstraints7.gridy = 5;
            LfileSizeLabel = new JLabel();
            LfileSizeLabel.setText("File sizes");
            final GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            gridBagConstraints6.gridx = 0;
            gridBagConstraints6.weighty = 1.0;
            gridBagConstraints6.weightx = 1.0;
            gridBagConstraints6.gridwidth = 2;
            gridBagConstraints6.gridy = 7;
            glueLabel = new JLabel();
            glueLabel.setText("");
            final GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            gridBagConstraints5.gridx = 0;
            gridBagConstraints5.gridwidth = 2;
            gridBagConstraints5.anchor = GridBagConstraints.CENTER;
            gridBagConstraints5.insets = new Insets(10, 5, 10, 5);
            gridBagConstraints5.gridy = 6;
            final GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.gridx = 1;
            gridBagConstraints4.anchor = GridBagConstraints.EAST;
            gridBagConstraints4.insets = new Insets(3, 5, 0, 10);
            gridBagConstraints4.gridy = 4;
            LfileCount = new JLabel();
            LfileCount.setText("JLabel");
            final GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
            gridBagConstraints31.gridx = 0;
            gridBagConstraints31.anchor = GridBagConstraints.WEST;
            gridBagConstraints31.insets = new Insets(3, 10, 0, 5);
            gridBagConstraints31.gridy = 4;
            LfileCountLabel = new JLabel();
            LfileCountLabel.setText("Files");
            final GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
            gridBagConstraints21.gridx = 1;
            gridBagConstraints21.anchor = GridBagConstraints.EAST;
            gridBagConstraints21.insets = new Insets(8, 5, 0, 10);
            gridBagConstraints21.gridy = 2;
            LidCount = new JLabel();
            LidCount.setText("JLabel");
            final GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            gridBagConstraints11.gridx = 0;
            gridBagConstraints11.anchor = GridBagConstraints.WEST;
            gridBagConstraints11.insets = new Insets(8, 10, 0, 5);
            gridBagConstraints11.gridy = 2;
            LidCountLabel = new JLabel();
            LidCountLabel.setText("Identities");
            final GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.gridx = 1;
            gridBagConstraints3.anchor = GridBagConstraints.EAST;
            gridBagConstraints3.insets = new Insets(3, 5, 0, 10);
            gridBagConstraints3.gridy = 1;
            LarcMsgCount = new JLabel();
            LarcMsgCount.setText("JLabel");
            final GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.gridx = 0;
            gridBagConstraints2.anchor = GridBagConstraints.WEST;
            gridBagConstraints2.insets = new Insets(3, 10, 0, 5);
            gridBagConstraints2.gridy = 1;
            LarcMsgCountLabel = new JLabel();
            LarcMsgCountLabel.setText("Archived messages");
            final GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.gridx = 1;
            gridBagConstraints1.anchor = GridBagConstraints.EAST;
            gridBagConstraints1.insets = new Insets(10, 5, 0, 10);
            gridBagConstraints1.gridy = 0;
            LmsgCount = new JLabel();
            LmsgCount.setText("JLabel");
            final GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(10, 10, 0, 5);
            gridBagConstraints.gridy = 0;
            LmsgCountLabel = new JLabel();
            LmsgCountLabel.setText("Messages");
            jContentPane = new JPanel();
            jContentPane.setLayout(new GridBagLayout());
            jContentPane.add(LmsgCountLabel, gridBagConstraints);
            jContentPane.add(LmsgCount, gridBagConstraints1);
            jContentPane.add(LarcMsgCountLabel, gridBagConstraints2);
            jContentPane.add(LarcMsgCount, gridBagConstraints3);
            jContentPane.add(LidCountLabel, gridBagConstraints11);
            jContentPane.add(LidCount, gridBagConstraints21);
            jContentPane.add(LfileCountLabel, gridBagConstraints31);
            jContentPane.add(LfileCount, gridBagConstraints4);
            jContentPane.add(getBclose(), gridBagConstraints5);
            jContentPane.add(glueLabel, gridBagConstraints6);
//            jContentPane.add(LfileSizeLabel, gridBagConstraints7);
//            jContentPane.add(LfileSize, gridBagConstraints8);
            jContentPane.add(LsharerCountLabel, gridBagConstraints9);
            jContentPane.add(LsharerCount, gridBagConstraints10);
        }
        return jContentPane;
    }

    /**
     * This method initializes Bclose
     *
     * @return javax.swing.JButton
     */
    private JButton getBclose() {
        if( Bclose == null ) {
            Bclose = new JButton();
            Bclose.setText("Close");
            Bclose.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(final java.awt.event.ActionEvent e) {
                    setVisible(false);
                }
            });
        }
        return Bclose;
    }

    private void refreshLanguage() {
        final Language language = Language.getInstance();

        setTitle(language.getString("StatisticsDialog.title"));
        Bclose.setText(language.getString("StatisticsDialog.button.close"));

        LmsgCountLabel.setText(language.getString("StatisticsDialog.label.messages")+":");
        LarcMsgCountLabel.setText(language.getString("StatisticsDialog.label.archivedMessages")+":");
        LidCountLabel.setText(language.getString("StatisticsDialog.label.identities")+":");
        LsharerCountLabel.setText(language.getString("StatisticsDialog.label.sharers")+":");
        LfileCountLabel.setText(language.getString("StatisticsDialog.label.files")+":");
        LfileSizeLabel.setText(language.getString("StatisticsDialog.label.fileSizes")+":");
    }

    public void startDialog(final int msgCount, final int arcMsgCount, final int idCount, final int sharerCount, final int fileCount, final long fileSize) {
        setModal(true);
        refreshLanguage();

        final NumberFormat nf = NumberFormat.getInstance();

        LmsgCount.setText(nf.format(msgCount));
        LarcMsgCount.setText(nf.format(arcMsgCount));
        LidCount.setText(nf.format(idCount));
        LsharerCount.setText(nf.format(sharerCount));
        LfileCount.setText(nf.format(fileCount));
        LfileSize.setText(nf.format(fileSize));

        pack();
        setLocationRelativeTo(owner);
        setVisible(true);
    }

}  //  @jve:decl-index=0:visual-constraint="10,10"
