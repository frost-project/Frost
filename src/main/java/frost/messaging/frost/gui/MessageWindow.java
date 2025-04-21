/*
  MessageWindow.java / Frost
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
package frost.messaging.frost.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import frost.MainFrame;
import frost.gui.SearchMessagesConfig;
import frost.messaging.frost.FrostMessageObject;
import frost.util.gui.MiscToolkit;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

public class MessageWindow extends JFrame {

	private static final long serialVersionUID = 1L;

	private final FrostMessageObject message;
    private final Window parentWindow;

    private MessageTextPane messageTextPane;
    private MessageWindowTopPanel topPanel;

    private Listener listener;

    private final Language language = Language.getInstance();

    private SearchMessagesConfig searchMessagesConfig = null;

    private final boolean showReplyButton;

    private static final ImageIcon frameIcon = MiscToolkit.loadImageIcon("/data/messagebright.gif");

    public MessageWindow(final Window parentWindow, final FrostMessageObject message, final Dimension size) {
        this(parentWindow, message, size, null, true);
    }

    public MessageWindow(final Window parentWindow, final FrostMessageObject message, final Dimension size, final boolean showReplyButton) {
        this(parentWindow, message, size, null, showReplyButton);
    }

    public MessageWindow(final Window parentWindow, final FrostMessageObject message, final Dimension size, final SearchMessagesConfig smc) {
        this(parentWindow, message, size, smc, true);
    }

    public MessageWindow(
            final Window parentWindow,
            final FrostMessageObject message,
            final Dimension size,
            final SearchMessagesConfig smc,
            final boolean showReplyButton)
    {
        super();
        this.message = message;
        this.parentWindow = parentWindow;
        this.setSize(size);
        this.searchMessagesConfig = smc;
        this.showReplyButton = showReplyButton;

        initialize();

        // set visible BEFORE updating the textpane to allow correct positioning of dividers
        setVisible(true);

        messageTextPane.update_messageSelected(message);
    }

    private void initialize(){
        listener = new Listener();

        this.setTitle(message.getSubject());

        this.getContentPane().setLayout(new BorderLayout());

        topPanel = new MessageWindowTopPanel(message);
        this.getContentPane().add(topPanel, BorderLayout.NORTH);

        messageTextPane = new MessageTextPane(this, searchMessagesConfig);
        this.getContentPane().add(messageTextPane, BorderLayout.CENTER);

        this.addKeyListener(listener);
        messageTextPane.addKeyListener(listener);
        topPanel.addKeyListener(listener);
        this.addWindowListener(listener);

        this.setIconImage(frameIcon.getImage());
        this.setLocationRelativeTo(parentWindow);
    }

    private void close() {
        messageTextPane.removeKeyListener(listener);
        messageTextPane.close();
        topPanel.removeKeyListener(listener);
        topPanel.close();
        dispose();
    }

    private void replyButtonPressed() {
        MainFrame.getInstance().getMessagePanel().composeReply(message, MainFrame.getInstance());
    }

    private class Listener extends WindowAdapter implements KeyListener, WindowListener {
        public void keyPressed(final KeyEvent e) {
            maybeDoSomething(e);
        }
        public void keyReleased(final KeyEvent e) {
        }
        public void keyTyped(final KeyEvent e) {
        }
        @Override
        public void windowClosing(final WindowEvent e) {
            close();
        }
        public void maybeDoSomething(final KeyEvent e){
            if( e.getKeyChar() == KeyEvent.VK_ESCAPE ) {
                close();
            }
        }
    }

	class MessageWindowTopPanel extends JPanel implements LanguageListener {

		private static final long serialVersionUID = 1L;

		private JLabel Lsubject = null;
        private JLabel Lfrom = null;
        private JLabel Lto = null;
        private JLabel Ldate = null;
        private JTextField TFsubject = null;
        private JTextField TFfrom = null;
        private JTextField TFto = null;
        private JTextField TFdate = null;
        private JLabel Lboard = null;
        private JTextField TFboard = null;

        private final FrostMessageObject innerMessage;
        private JButton Breply = null;

        public MessageWindowTopPanel(final FrostMessageObject msg) {
            super();
            innerMessage = msg;

            initialize();
            languageChanged(null);
            language.addLanguageListener(this);
        }

        @Override
        public void addKeyListener(final KeyListener l) {
            super.addKeyListener(l);
            final Component[] c = getComponents();
            for( final Component element : c ) {
                element.addKeyListener(l);
            }
        }

        @Override
        public void removeKeyListener(final KeyListener l) {
            super.removeKeyListener(l);
            final Component[] c = getComponents();
            for( final Component element : c ) {
                element.removeKeyListener(l);
            }
        }

        public void close() {
            language.removeLanguageListener(this);
        }

        // subject, from, (to), date/board
        private void initialize() {
            Lboard = new JLabel();
            Lboard.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
            Ldate = new JLabel();
            Ldate.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
            Lfrom = new JLabel();
            Lfrom.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
            Lsubject = new JLabel();
            Lsubject.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
            Lto = new JLabel();
            Lto.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));

            final GridBagConstraints BreplyConstraints = new GridBagConstraints(); // Breply
            BreplyConstraints.gridx = 5;
            BreplyConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
            BreplyConstraints.gridheight = 3;
            BreplyConstraints.insets = new java.awt.Insets(5,5,5,5);
            BreplyConstraints.gridy = 1;

            final GridBagConstraints LsubjectConstraints = new GridBagConstraints();  // Lsubject
            LsubjectConstraints.gridx = 0;
            LsubjectConstraints.insets = new java.awt.Insets(1,5,1,2);
            LsubjectConstraints.anchor = java.awt.GridBagConstraints.WEST;
            LsubjectConstraints.gridy = 1;

            final GridBagConstraints TFsubjectConstraints = new GridBagConstraints(); // TFsubject
            TFsubjectConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            TFsubjectConstraints.gridy = 1;
            TFsubjectConstraints.weightx = 1.0;
            TFsubjectConstraints.gridwidth = 4;
            TFsubjectConstraints.insets = new java.awt.Insets(1,1,1,5);
            TFsubjectConstraints.anchor = java.awt.GridBagConstraints.WEST;
            TFsubjectConstraints.gridx = 1;

            final GridBagConstraints LfromConstraints = new GridBagConstraints(); // Lfrom
            LfromConstraints.gridx = 0;
            LfromConstraints.insets = new java.awt.Insets(1,5,1,2);
            LfromConstraints.anchor = java.awt.GridBagConstraints.WEST;
            LfromConstraints.gridy = 2;

            final GridBagConstraints TFfromConstraints = new GridBagConstraints(); // TFfrom
            TFfromConstraints.fill = java.awt.GridBagConstraints.NONE;
            TFfromConstraints.gridy = 2;
            TFfromConstraints.weightx = 1.0;
            TFfromConstraints.gridwidth = 4;
            TFfromConstraints.insets = new java.awt.Insets(1,1,1,5);
            TFfromConstraints.anchor = java.awt.GridBagConstraints.WEST;
            TFfromConstraints.gridx = 1;

            final GridBagConstraints LtoConstraints = new GridBagConstraints(); // Lto
            LtoConstraints.gridx = 0;
            LtoConstraints.insets = new java.awt.Insets(1,5,1,2);
            LtoConstraints.anchor = java.awt.GridBagConstraints.WEST;
            LtoConstraints.gridy = 3;

            final GridBagConstraints TFtoConstraints = new GridBagConstraints(); // TFto
            TFtoConstraints.fill = java.awt.GridBagConstraints.NONE;
            TFtoConstraints.gridy = 3;
            TFtoConstraints.weightx = 1.0;
            TFtoConstraints.gridwidth = 4;
            TFtoConstraints.insets = new java.awt.Insets(1,1,1,5);
            TFtoConstraints.anchor = java.awt.GridBagConstraints.WEST;
            TFtoConstraints.gridx = 1;

            final GridBagConstraints LdateConstraints = new GridBagConstraints(); // Ldate
            LdateConstraints.gridx = 0;
            LdateConstraints.insets = new java.awt.Insets(1,5,1,2);
            LdateConstraints.anchor = java.awt.GridBagConstraints.WEST;
            LdateConstraints.gridy = 4;

            final GridBagConstraints TFdateConstraints = new GridBagConstraints(); // TFdate
            TFdateConstraints.fill = java.awt.GridBagConstraints.NONE;
            TFdateConstraints.gridy = 4;
            TFdateConstraints.weightx = 0.0;
            TFdateConstraints.insets = new java.awt.Insets(1,1,1,5);
            TFdateConstraints.anchor = java.awt.GridBagConstraints.WEST;
            TFdateConstraints.gridx = 1;

            final GridBagConstraints LboardConstraints = new GridBagConstraints(); // Lboard
            LboardConstraints.gridx = 2;
            LboardConstraints.insets = new java.awt.Insets(1,8,1,2);
            LboardConstraints.anchor = java.awt.GridBagConstraints.WEST;
            LboardConstraints.gridy = 4;

            final GridBagConstraints TFboardConstraints = new GridBagConstraints(); // TFboard
            TFboardConstraints.fill = java.awt.GridBagConstraints.NONE;
            TFboardConstraints.gridy = 4;
            TFboardConstraints.weightx = 0.0;
            TFboardConstraints.anchor = java.awt.GridBagConstraints.WEST;
            TFboardConstraints.insets = new java.awt.Insets(1,1,1,5);
            TFboardConstraints.gridx = 4;

            this.setLayout(new GridBagLayout());
            this.setSize(new java.awt.Dimension(496,254));
            this.add(Lsubject, LsubjectConstraints);
            this.add(Lfrom, LfromConstraints);
            this.add(Ldate, LdateConstraints);
            this.add(getTFsubject(), TFsubjectConstraints);
            this.add(getTFfrom(), TFfromConstraints);
            if( innerMessage.getRecipientName() != null ) {
                this.add(Lto, LtoConstraints);
                this.add(getTFto(), TFtoConstraints);
            }
            this.add(getTFdate(), TFdateConstraints);
            this.add(Lboard, LboardConstraints);
            this.add(getTFboard(), TFboardConstraints);
            if( showReplyButton ) {
                this.add(getBreply(), BreplyConstraints);
            }
        }

        public void languageChanged(final LanguageEvent event) {
            Lsubject.setText(language.getString("MessageWindow.subject")+":");
            Lfrom.setText(language.getString("MessageWindow.from")+":");
            Lto.setText(language.getString("MessageWindow.to")+":");
            Ldate.setText(language.getString("MessageWindow.date")+":");
            Lboard.setText(language.getString("MessageWindow.board")+":");
        }

        private JTextField getTFsubject() {
            if( TFsubject == null ) {
                TFsubject = new JTextField();
                TFsubject.setText(" "+innerMessage.getSubject());
                TFsubject.setBorder(javax.swing.BorderFactory.createEmptyBorder(2,2,2,2));
                TFsubject.setEditable(false);
            }
            return TFsubject;
        }

        private JTextField getTFfrom() {
            if( TFfrom == null ) {
                TFfrom = new JTextField();
                TFfrom.setText(" "+innerMessage.getFromName());
                TFfrom.setBorder(javax.swing.BorderFactory.createEmptyBorder(2,2,2,2));
                TFfrom.setEditable(false);
            }
            return TFfrom;
        }

        private JTextField getTFto() {
            if( TFto == null ) {
                TFto = new JTextField();
                TFto.setText(" "+innerMessage.getRecipientName());
                TFto.setBorder(javax.swing.BorderFactory.createEmptyBorder(2,2,2,2));
                TFto.setEditable(false);
            }
            return TFto;
        }

        private JTextField getTFdate() {
            if( TFdate == null ) {
                TFdate = new JTextField();
                TFdate.setText(" "+innerMessage.getDateAndTimeString());
                TFdate.setBorder(javax.swing.BorderFactory.createEmptyBorder(2,2,2,2));
                TFdate.setEditable(false);
            }
            return TFdate;
        }

        private JTextField getTFboard() {
            if( TFboard == null ) {
                TFboard = new JTextField();
                TFboard.setText(" "+innerMessage.getBoard().getName());
                TFboard.setBorder(javax.swing.BorderFactory.createEmptyBorder(2,2,2,2));
                TFboard.setEditable(false);
            }
            return TFboard;
        }

        private JButton getBreply() {
            if( Breply == null ) {
                Breply = new JButton();
                Breply.setIcon(MiscToolkit.loadImageIcon("/data/toolbar/mail-reply-sender.png"));
                Breply.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent arg0) {
                        replyButtonPressed();
                    }
                });
                MiscToolkit.configureButton(Breply, "MessageWindow.tooltip.reply", language);
            }
            return Breply;
        }
    }
}
