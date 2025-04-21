/*
  FcpPersistentRequest.java / Frost
  Copyright (C) 2011  Frost Project <jtcfrost.sourceforge.net>

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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.time.Instant;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import frost.identities.Identity;
import frost.util.DateFun;
import frost.util.gui.textpane.AntialiasedTextArea;
import frost.util.gui.translation.Language;

/**
 * @author Jan Gerritsen
 *
 */
public class SetIdentityCommentDialog extends JFrame {

	private static final long serialVersionUID = 1L;

	protected  Language language = null;

	private Identity identity;

	private JButton okButton;
	private JButton cancelButton;

	private AntialiasedTextArea commentTextArea;

	public SetIdentityCommentDialog(Identity identity) {
		this.language = Language.getInstance();
		this.identity = identity;

		initialize();
	}

	/**
	 * Component initialization
	 */
	private void initialize() {
		setTitle(language.getString("SetIdentityCommentDialog.title"));

		// Set window size
		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		
		int width, height;
		
		if( screenSize.width > 800 ) {
			width = 650;
		} else {
			width = (int) (screenSize.width * 0.90);
		}

		if( screenSize.height > 650 ) {
			height = 500;
		} else {
			height = (int) (screenSize.height * 0.85);
		}
		
		setSize(width, height);
		this.setResizable(true);

		// OK Button
		okButton = new JButton(language.getString("Common.ok"));
		okButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				identity.setComment(commentTextArea.getText());
				dispose();
			}
		});

		// Cancel Button
		cancelButton = new JButton(language.getString("Common.cancel"));
		cancelButton.addActionListener( new java.awt.event.ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				dispose();
			}
		});

		// Button row
		final JPanel buttonsPanel = new JPanel(new BorderLayout());
		buttonsPanel.setLayout( new BoxLayout( buttonsPanel, BoxLayout.X_AXIS ));
		buttonsPanel.add( Box.createHorizontalGlue() );
		buttonsPanel.add( cancelButton );
		buttonsPanel.add(Box.createRigidArea(new Dimension(10,3)));
		buttonsPanel.add( okButton );
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));

		// Header row
		final JPanel headerPanel = new JPanel(new GridBagLayout());
		final GridBagConstraints constraints = new GridBagConstraints();
		final Insets insets = new Insets(3, 3, 3, 3);
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.weighty = 0.0;
		constraints.weightx = 1.0;
		constraints.insets = insets;
		constraints.gridwidth = 1;
		constraints.gridy = 0;

		// uniqeName
		headerPanel.add(new JLabel("Name: " + identity.getUniqueName()), constraints);

		constraints.gridy++;

		// labelLastSeenTimestamp
		headerPanel.add(new JLabel("Last seen: "
				+ DateFun.FORMAT_DATE_TIME_VISIBLE.format(Instant.ofEpochMilli(identity.getLastSeenTimestamp()))),
				constraints);

		constraints.gridy++;

		// ReceivedMessageCount
		headerPanel.add(new JLabel("Received messages: " + identity.getReceivedMessageCount()), constraints);

		constraints.gridy++;

		// Comment
		headerPanel.add(new JLabel("Comment:"), constraints);
		commentTextArea = new AntialiasedTextArea();
		commentTextArea.setText(identity.getComment());
		final JScrollPane commentScrollPane = new JScrollPane(commentTextArea);
		commentScrollPane.setWheelScrollingEnabled(true);
		commentScrollPane.setMinimumSize(new Dimension(100, 50));

		
		// main panel
		final JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add( headerPanel, BorderLayout.NORTH);
		mainPanel.add( commentScrollPane, BorderLayout.CENTER );
		mainPanel.add( buttonsPanel, BorderLayout.SOUTH );
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5,7,7,7));

		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(mainPanel, null);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

}
