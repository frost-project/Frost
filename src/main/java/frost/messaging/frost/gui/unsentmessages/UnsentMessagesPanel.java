/*
  UnsentMessagesPanel.java / Frost
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
package frost.messaging.frost.gui.unsentmessages;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import frost.Core;
import frost.Settings;
import frost.messaging.frost.FrostUnsentMessageObject;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

@SuppressWarnings("serial")
public class UnsentMessagesPanel extends JPanel implements LanguageListener {

    Language language = Language.getInstance();

    private UnsentMessagesTable unsentMessagesTable;
    private JLabel unsentMsgsLabel;
    private JCheckBox disableMessageUpload; 

    private boolean isShown = false;

    public UnsentMessagesPanel() {
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

    public synchronized void addUnsentMessage(FrostUnsentMessageObject i) {
        if( isShown ) {
            unsentMessagesTable.addUnsentMessage(i);
        }
    }

    public synchronized void removeUnsentMessage(FrostUnsentMessageObject i) {
        if( isShown ) {
            unsentMessagesTable.removeUnsentMessage(i);
        }
    }

    public synchronized void updateUnsentMessage(FrostUnsentMessageObject i) {
        if( isShown ) {
            unsentMessagesTable.updateUnsentMessage(i);
        }
    }

    public void updateUnsentMessagesCount() {
        refreshLanguage();
    }

    public void clearTableModel() {
        unsentMessagesTable.clearTableModel();
    }

    public void loadTableModel() {
        unsentMessagesTable.loadTableModel();
    }

    public void saveTableFormat() {
        unsentMessagesTable.saveTableFormat();
    }

    public void refreshLanguage() {
        unsentMsgsLabel.setText( language.getString("UnsentMessages.label") + " ("+unsentMessagesTable.getRowCount()+")");
        disableMessageUpload.setText( language.getString("UnsentMessages.disableMessageUpload") );
    }

    public void languageChanged(LanguageEvent event) {
        refreshLanguage();
    }

    private void initialize() {

        this.setLayout(new BorderLayout());
        unsentMsgsLabel = new JLabel();
        unsentMsgsLabel.setBorder(BorderFactory.createEmptyBorder(2,4,2,2));
        disableMessageUpload = new JCheckBox();
        disableMessageUpload.setBorder(BorderFactory.createEmptyBorder(2,16,2,2));
        disableMessageUpload.setSelected(Core.frostSettings.getBoolValue(Settings.MESSAGE_UPLOAD_DISABLED));
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(unsentMsgsLabel);
        p.add(disableMessageUpload);
        this.add(p, BorderLayout.NORTH);

        unsentMessagesTable = new UnsentMessagesTable();
        unsentMessagesTable.getScrollPane().setWheelScrollingEnabled(true);
        this.add(unsentMessagesTable.getScrollPane(), BorderLayout.CENTER);

        // apply a bold font to labels
        Font font = unsentMsgsLabel.getFont();
        font = font.deriveFont(Font.BOLD);
        unsentMsgsLabel.setFont(font);
        
        // listen to changes on disableMessageUpload checkbox
        disableMessageUpload.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Core.frostSettings.setValue(Settings.MESSAGE_UPLOAD_DISABLED, disableMessageUpload.isSelected());
            }
        });
    }
}
