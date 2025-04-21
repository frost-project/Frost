/*
  TextComponentFindAction.java / Frost
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
package frost.util.gui.search;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

/**
  * press ctrl-i to start case insensitive search or
  * ctrl-shift-i for case sensitive search. While searching press
  * up/down key to select next/previous match in text. Press ESC
  * to end the search.
 */

//@author Santhosh Kumar T - santhosh@in.fiorano.com
// source: http://jroller.com/page/santhosh?entry=incremental_search_jtextcomponent
public class TextComponentFindAction extends FindAction implements FocusListener {

	private static final long serialVersionUID = 1L;

	// 1. inits searchField with selected text
    // 2. adds focus listener so that textselection gets painted
    //    even if the textcomponent has no focus
    @Override
    protected void initSearch(ActionEvent ae){
        super.initSearch(ae);
        JTextComponent textComp = (JTextComponent)ae.getSource();
        String selectedText = textComp.getSelectedText();
        if(selectedText!=null)
            searchField.setText(selectedText);
        searchField.removeFocusListener(this);
        searchField.addFocusListener(this);
    }

    @Override
    protected boolean changed(JComponent comp2, String str, Position.Bias bias){
        JTextComponent textComp = (JTextComponent)comp2;
        int offset = bias==Position.Bias.Forward ? textComp.getCaretPosition() : textComp.getCaret().getMark() - 1;

        int index = getNextMatch(textComp, str, offset, bias);
        if(index!=-1){
            textComp.select(index, index + str.length());
            return true;
        }else{
            offset = bias==null || bias==Position.Bias.Forward ? 0 : textComp.getDocument().getLength();
            index = getNextMatch(textComp, str, offset, bias);
            if(index!=-1){
                textComp.select(index, index + str.length());
                return true;
            }else
                return false;
        }
    }

    protected int getNextMatch(JTextComponent textComp, String str, int startingOffset, Position.Bias bias){
        String text = null;

        // get text from document, otherwize it won't work with JEditorPane with html
        try{
            text = textComp.getDocument().getText(0, textComp.getDocument().getLength());
        } catch(BadLocationException e){
            throw new RuntimeException("This should never happen!");
        }

        if(ignoreCase){
            str = str.toUpperCase();
            text = text.toUpperCase();
        }

        return bias==null || bias==Position.Bias.Forward
                ? text.indexOf(str, startingOffset)
                : text.lastIndexOf(str, startingOffset);
    }

    /*-------------------------------------------------[ FocusListener ]---------------------------------------------------*/

    // ensures that the selection is visible
    // because textcomponent doesn't show selection
    // when they don't have focus
    public void focusGained(FocusEvent e){
        Caret caret = ((JTextComponent)comp).getCaret();
        caret.setVisible(true);
        caret.setSelectionVisible(true);
    }

    public void focusLost(FocusEvent e){}
}
