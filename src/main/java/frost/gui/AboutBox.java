/*
  AboutBox.java / About Box
  Copyright (C) 2001  Frost Project <jtcfrost.sourceforge.net>

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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.StringJoiner;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import frost.Settings;
import frost.util.gui.JDialogWithDetails;
import frost.util.gui.MiscToolkit;

public class AboutBox extends JDialogWithDetails {

	private static final long serialVersionUID = 1L;

	private final static String product = "Frost";

	// because a growing amount of users use CVS version:
	private String version = null;

	private final static String copyright = "Copyright 2001 - 2025 Frost development team";
	private final static String comments2 = "https://jtcfrost.sourceforge.net/";

    private final JPanel imagePanel = new JPanel();
    private final JPanel messagesPanel = new JPanel();

    private final JLabel imageLabel = new JLabel();
    private final JLabel productLabel = new JLabel();
    private final JLabel versionLabel = new JLabel();
    private final JLabel copyrightLabel = new JLabel();
    private final JLabel licenseLabel = new JLabel();
    private final JLabel websiteLabel = new JLabel();

    private static final ImageIcon frostImage = MiscToolkit.loadImageIcon("/data/jtc.jpg");

    public AboutBox(final Frame parent) {
        super(parent);
        initialize();
    }

    /**
     * Component initialization
     */
    private void initialize() {
        imageLabel.setIcon(frostImage);
        setTitle(language.getString("AboutBox.title"));
        setResizable(false);

        // Image panel
        imagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        imagePanel.add(imageLabel);

        // Messages panel
        final GridLayout gridLayout = new GridLayout(5, 1);
        messagesPanel.setLayout(gridLayout);
        messagesPanel.setBorder(new EmptyBorder(10, 50, 10, 10));
        productLabel.setText(product);
        versionLabel.setText(getVersion());
        copyrightLabel.setText(copyright);
        licenseLabel.setText(language.getString("AboutBox.label.openSourceProject"));
        websiteLabel.setText(comments2);
        messagesPanel.add(productLabel);
        messagesPanel.add(versionLabel);
        messagesPanel.add(copyrightLabel);
        messagesPanel.add(licenseLabel);
        messagesPanel.add(websiteLabel);

        // Putting everything together
        getUserPanel().setLayout(new BorderLayout());
        getUserPanel().add(imagePanel, BorderLayout.WEST);
        getUserPanel().add(messagesPanel, BorderLayout.CENTER);

        fillDetailsArea();
    }

	private void fillDetailsArea() {
		StringJoiner details = new StringJoiner("\n");
		details.add(language.getString("AboutBox.text.development"));
		details.add("");
		details.add(language.getString("AboutBox.text.active"));
		details.add("   Spider-Admin@Z+d9Knmjd3hQeeZU6BOWPpAAxxs");
		details.add("");
		details.add("   Jan Gerritsen");
		details.add("   (a*rtur@K7dLGJvoXF_QQeUhZq9bNp0lFx4)");
		details.add("");
		details.add("   José Manuel Arnesto");
		details.add("   (kevloral@0aGR0ur6QBN_+RSuU47Es4X7HVs)");
		details.add("");
		details.add("   Karsten Graul");
		details.add("   (bback@xgVRApPk+Yngy+jmtOeGzIbN_A0)");
		details.add("");
		details.add(language.getString("AboutBox.text.left"));
		details.add("   S. Amoako (quit)");
		details.add("   Roman Glebov (quit)");
		details.add("   Jan-Thomas Czornack (quit)");
		details.add("   Thomas Mueller (quit)");
		details.add("   Jim Hunziker (quit)");
		details.add("   Stefan Majewski (quit)");
		details.add("   Edward Louis Severson IV (quit)");
		details.add("   Ingo Franzki (old systray icon code)");
		details.add("   Frédéric Scheer (splashscreen logo)");
		setDetailsText(details.toString());
	}

	private String getVersion() {
		if (version == null) {
			version = language.getString("AboutBox.label.version") + ": " + Settings.getVersion();
		}
		return version;
	}
}
