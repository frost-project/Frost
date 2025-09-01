/*
  SentMessagesPanel.java / Frost
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
package frost.messaging.frost.gui.sentmessages;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import frost.messaging.frost.FrostMessageObject;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

public class SentMessagesPanel extends JPanel implements LanguageListener {

	private static final long serialVersionUID = 1L;

	private transient Language language = Language.getInstance();

    private JLabel sentMsgsLabel;
    private SentMessagesTable sentMessagesTable;

    private boolean isShown = false;

    public SentMessagesPanel() {
        super();
        language.addLanguageListener(this);
        initialize();
        refreshLanguage();
    }

    public synchronized void prepareForShow() {
        loadTableModel();
        isShown = true;
    }

    public boolean isShown() {
        return isShown;
    }

    public synchronized void cleanupAfterLeave() {
        clearTableModel();
        isShown = false;
    }

    /**
     * Adds a new sent message to the sent messages table, but only when this table is currently shown.
     * @param i   msg to add
     * @return    true if msg was added (sent msgs table was currently shown)
     */
    public synchronized boolean addSentMessage(final FrostMessageObject i) {
        if( isShown ) {
            sentMessagesTable.addSentMessage(i);
            return true;
        } else {
            return false;
        }
    }

    public void updateSentMessagesCount() {
        refreshLanguage();
    }

    public void clearTableModel() {
        sentMessagesTable.clearTableModel();
    }

    public void loadTableModel() {
        sentMessagesTable.loadTableModel();
        refreshLanguage();
    }

    public void saveTableFormat() {
        sentMessagesTable.saveTableFormat();
    }

    public void refreshLanguage() {
        sentMsgsLabel.setText( language.getString("SentMessages.label") + " ("+sentMessagesTable.getRowCount()+")");
    }

    public void languageChanged(final LanguageEvent event) {
        refreshLanguage();
    }

    private void initialize() {

        this.setLayout(new BorderLayout());
        sentMsgsLabel = new JLabel();
        sentMsgsLabel.setBorder(BorderFactory.createEmptyBorder(2,4,2,2));
        this.add(sentMsgsLabel, BorderLayout.NORTH);

        sentMessagesTable = new SentMessagesTable();
        sentMessagesTable.getScrollPane().setWheelScrollingEnabled(true);
        this.add(sentMessagesTable.getScrollPane(), BorderLayout.CENTER);

        Font font = sentMsgsLabel.getFont();
        font = font.deriveFont(Font.BOLD);
        sentMsgsLabel.setFont(font);
    }
}
