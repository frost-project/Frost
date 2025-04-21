/*
  SmileyChooserPanel.java / Frost
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
package frost.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.util.gui.SmileyCache;
import frost.util.gui.translation.Language;

public class SmileyChooserDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final Logger logger =  LoggerFactory.getLogger(SmileyChooserDialog.class);

    Language language = Language.getInstance();

    String returnValue = null;

    public SmileyChooserDialog(final JDialog parent) {
        super(parent, true);
        initialize();
    }

    public SmileyChooserDialog(final JFrame parent) {
        super(parent, true);
        initialize();
    }

	protected class SmileyImage extends JLabel {

		private static final long serialVersionUID = 1L;

		private String smileyText;

        public SmileyImage(final ImageIcon i, final String s) {
            super(i);
            smileyText = s;
        }
        public String getSmileyText() {
            return smileyText;
        }
    }

    protected class Listener implements MouseListener {
        public void mouseClicked(final MouseEvent e) {
            if( javax.swing.SwingUtilities.isLeftMouseButton(e) ) {
                if( e.getSource() instanceof SmileyImage ) {
                    final SmileyImage si = (SmileyImage)e.getSource();
                    iconChoosed(si.getSmileyText());
                }
            }
        }

        public void mouseEntered(final MouseEvent e) {
        }
        public void mouseExited(final MouseEvent e) {
        }
        public void mousePressed(final MouseEvent e) {
        }
        public void mouseReleased(final MouseEvent e) {
        }
    }

    public void initialize() {

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(language.getString("SmileyChooserDialog.title"));

        final JPanel p = new JPanel();

        final int count = SmileyCache.getSmileyCount();
        final int maxCols = 5;
        final int numRows = (count / 5) +1; // use +1 to give extra row for the remainder

        p.setLayout(new GridLayout(numRows, maxCols, 6, 6));

        final Listener l = new Listener();

        for(int i = 1; i < count; i++){
            final SmileyImage si = new SmileyImage(
                    SmileyCache.getCachedSmiley(i),
                    SmileyCache.getSmileyText(i));
            si.addMouseListener(l);
            p.add(si);
            //Add your click selection listener here...
        }

        p.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLoweredBevelBorder(),
                        BorderFactory.createEmptyBorder(5,5,5,5))
                );
        getContentPane().add(p, BorderLayout.CENTER);
        pack();
    }

    private void iconChoosed(final String iconText) {
        returnValue = iconText;
        dispose();
    }

    public String startDialog(final int x, final int y) {
        setLocation(x-getWidth(), y);
        setVisible(true);
        return returnValue;
    }

	public static void main(final String[] args) {
		final String s = new SmileyChooserDialog((JDialog) null).startDialog(0, 0);
		logger.info("choosed = {}", s);
	}

}