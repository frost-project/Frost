/*
 LinkEditorKit.java / Frost
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
package frost.util.gui.textpane;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

/**
 * Use by JOptionPane for correct display Freenet's keys link.
 * @author ET
 */
public class LinkEditorKit extends StyledEditorKit {

	private static final long serialVersionUID = 1L;

	/**
	 *  attribute on inline elements
	 */
	public static final Object LINK = new String("link");
	
	/**
	 * can be static because it picks up the editor from the MouseEvent
	 */
	private static MouseInputListener linkHandler = new LinkHandler();
	
	public LinkEditorKit() {
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public void install(JEditorPane p) {
		super.install(p);
		p.addMouseListener(linkHandler);
		p.addMouseMotionListener(linkHandler);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
    public void deinstall(JEditorPane p) {
		p.removeMouseListener(linkHandler);
		super.deinstall(p);
	}
	
	private static class LinkHandler extends MouseInputAdapter {
		
		private Element activeElement;

		/**
		 * Find active Element in JEditorPane document
		 * @param e use for position of mouse
		 * @return Active element
		 */
		private Element characterElementAt(MouseEvent e) {
			JEditorPane p = (JEditorPane)e.getComponent();
			Position.Bias[] bias = new Position.Bias[1];
			int position = p.getUI().viewToModel2D(p, e.getPoint(), bias);
			if (bias[0] == Position.Bias.Backward && position != 0)
			--position;
			Element c = ((StyledDocument)p.getDocument()).getCharacterElement(position);
			// should test whether really inside
			return c;
		}
		
		/**
		 * Used for memory active element
		 */
		@Override
        public void mousePressed(MouseEvent e) {
//			if (!javax.swing.SwingUtilities.isLeftMouseButton(e))
//				return;
			JEditorPane p = (JEditorPane)e.getComponent();
			if (p.isEditable())
				return;
			Element c = characterElementAt(e);
			if (c != null && c.getAttributes().getAttribute(LINK) != null)
			activeElement = c;
		}
		
		/**
		 * Used for send HyperLinkEvent.
		 */
		@Override
        public void mouseReleased(MouseEvent e)	{
//			if (!javax.swing.SwingUtilities.isLeftMouseButton(e) || activeElement == null) {
//                return;
//            }
            if (activeElement == null) {
                return;
            }
			JEditorPane p = (JEditorPane)e.getComponent();
			Element c = characterElementAt(e);
			if (!p.isEditable() && c == activeElement) { //	too restrictive, should find attribute run
				activeElement = null;
				Object target = c.getAttributes().getAttribute(LINK);
                // use own event to provide MouseEvent to receiver
                MouseHyperlinkEvent evt = new MouseHyperlinkEvent(
                        p, 
                        HyperlinkEvent.EventType.ACTIVATED, 
                        null, 
                        target.toString(), 
                        c,
                        e);
				p.fireHyperlinkUpdate(evt);
			}
		}
		
		/**
		 * Used for display hand cursor
		 */
		@Override
        public void mouseMoved(MouseEvent e) {
			JEditorPane p = (JEditorPane)e.getComponent();
			if (p.isEditable())
				return;
			Element c = characterElementAt(e);
			if (c != null && c.getAttributes().getAttribute(LINK) != null) {
				p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			} else {
				p.setCursor(Cursor.getDefaultCursor());
			}
		}
	}
}
