/*
 HelpHTMLEditorKit.java / Frost
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
package frost.gui.help;

import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * @author notitaccu
 */
public class HelpHTMLEditorKit extends HTMLEditorKit {

	private static final long serialVersionUID = 1L;

	private String url_prefix;
    
    private HelpHTMLDocument helpHTMLDocument = null;

    /**
     * Constructs an HTMLEditorKit, creates a StyleContext, and loads the style sheet.
     */
    public HelpHTMLEditorKit(String url_prefix) {
      super(); 
      this.url_prefix = url_prefix;
    }
    
    public HelpHTMLDocument getHelpHTMLDocument() {
        return helpHTMLDocument;
    }

//    /** 
//     * Shared factory for creating HTML Views. 
//     */
//    private static final ViewFactory helpFactory = new helpHTMLFactory();
//
//    public ViewFactory getViewFactory() {
//        return helpFactory;
//    }
//    
//    /**
//     * Factory which creates views for elements in the document.
//     */
//    public static class helpHTMLFactory extends HTMLFactory {
//
//        /**
//         * Creates a new factory.
//         * 
//         */
//        public helpHTMLFactory() {
//        }
//
//        /**
//         * Creates a view which can render the element.
//         * 
//         * @param elem
//         *            The element to create the view for.
//         * @return The newly created view.
//         */
//        public View create(Element elem) {
//            // hier koennen html tags ausgetauschtoder inzugefuegt werden
//            // wir sind hier nach dem parser und vor dem View
//            // notitaccu
//            return super.create(elem);
//        }
//    }
    
    /**
     * Create an uninitialized text storage model
     * that is appropriate for this type of editor.
     *
     * @return the model
     */
    @Override
    public Document createDefaultDocument() {
    	StyleSheet styles = getStyleSheet();
    	StyleSheet ss = new StyleSheet();
    
    	ss.addStyleSheet(styles);
    
    	helpHTMLDocument = new HelpHTMLDocument(url_prefix, ss);
        helpHTMLDocument.setParser(getParser());
        helpHTMLDocument.setAsynchronousLoadPriority(4); // -1 means syncron, else somthing auround 5? 
        helpHTMLDocument.setTokenThreshold(100);
    	return helpHTMLDocument;
    }
}
