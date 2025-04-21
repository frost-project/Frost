/*
  MessageUploadFailedDialog.java / Frost
  Copyright (C) 2001  Frost Project <jtcfrost.sourceforge.net>
  This file is contributed by Stefan Majewski <feuerblume@users.sourceforge.net>

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
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import frost.messaging.frost.Attachment;
import frost.messaging.frost.MessageXmlFile;
import frost.util.gui.translation.Language;

public class MessageUploadFailedDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private class ButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == retryButton) {
                userAnswer = RETRY_VALUE;
            } else if (e.getSource() == tryOnNextStartupButton) {
                userAnswer = RETRY_NEXT_STARTUP_VALUE;
            } else if (e.getSource() == discardButton) {
                userAnswer = DISCARD_VALUE;
            }
            dispose();
        }
    }

    private class RetryButtonTimer extends Timer {

        private int secs;

        public RetryButtonTimer(int secs) {
            this.secs = secs;
        }

        public void start() {
            scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    timerTriggered();
                }
            }, 1000, 1000);
        }

        private void timerTriggered() {
            secs--;
            retryButton.setText(retryButtonText + secs);
            if (secs == 0) {
                userAnswer = RETRY_VALUE;
                dispose();
            }
        }
    }

    private static final int SECONDS_TO_WAIT = 30;

    public static final int NO_VALUE = 0;
    public static final int RETRY_VALUE = 1;
    public static final int RETRY_NEXT_STARTUP_VALUE = 2;
    public static final int DISCARD_VALUE = 3;

    private JButton retryButton, discardButton, tryOnNextStartupButton;
    private String retryButtonText;
    private RetryButtonTimer timer;
    private int userAnswer = NO_VALUE;
    
    private MessageXmlFile failedMessage;
    private String errorString;

    public MessageUploadFailedDialog(Frame owner, MessageXmlFile fm, String error) {
        super(owner, true);
        
        failedMessage = fm;
        errorString = error;

        Language language = Language.getInstance();

        retryButtonText = language.getString("MessageUploadFailedDialog.option.retry") + " - ";

        setTitle(language.getString("MessageUploadFailedDialog.title"));

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        getContentPane().add(mainPanel);

        BorderLayout layout = new BorderLayout(0, 15);
        mainPanel.setLayout(layout);

        timer = new RetryButtonTimer(SECONDS_TO_WAIT);

        Icon warningIcon = UIManager.getIcon("OptionPane.warningIcon");
        String warningText = "  " + language.getString("MessageUploadFailedDialog.body");
        if( error != null && error.length() > 0 ) {
            warningText += "\n   Error  : "+errorString;
        }
        warningText += "\n   Sender : "+failedMessage.getFromName();
        warningText += "\n   Subject: "+failedMessage.getSubject();
        warningText += "\n   Boards : "+failedMessage.getAttachmentsOfType(Attachment.BOARD).size();
        warningText += "\n   Files  : "+failedMessage.getAttachmentsOfType(Attachment.FILE).size();
        mainPanel.add(new JLabel(warningText, warningIcon, SwingConstants.LEFT), BorderLayout.NORTH);

        mainPanel.add(getButtonPanel(language), BorderLayout.SOUTH);

        ButtonListener bl = new ButtonListener();
        retryButton.addActionListener(bl);
        tryOnNextStartupButton.addActionListener(bl);
        discardButton.addActionListener(bl);

        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        pack();
        setLocationRelativeTo(owner);
    }

    public int startDialog() {
        retryButton.requestFocus();
        timer.start();
        setModal(true); // paranoia
        setVisible(true);
        return userAnswer;
    }

    private JPanel getButtonPanel(Language language) {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));

        retryButton = new JButton(retryButtonText + SECONDS_TO_WAIT);
        discardButton = new JButton(language.getString("MessageUploadFailedDialog.option.discardMessage"));
        tryOnNextStartupButton = new JButton(language.getString("MessageUploadFailedDialog.option.retryOnNextStartup"));

        buttonsPanel.add(retryButton);
        buttonsPanel.add(tryOnNextStartupButton);
        buttonsPanel.add(discardButton);

        return buttonsPanel;
    }

    @Override
    public void dispose() {
        timer.cancel();
        super.dispose();
    }
}
