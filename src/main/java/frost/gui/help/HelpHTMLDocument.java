/*
 HelpHTMLDocument.java / Frost
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

import java.net.URL;

import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author notitaccu
 */
public class HelpHTMLDocument extends HTMLDocument {

	private static final long serialVersionUID = 1L;

	private static final Logger logger =  LoggerFactory.getLogger(HelpHTMLDocument.class);

//    private String url_prefix;

    public HelpHTMLDocument(String url_prefix, StyleSheet styles) {
        super(styles);
//        this.url_prefix = url_prefix;
    }

    /**
     * Fetches the reader for the parser to use when loading the document with HTML. This is implemented to return an
     * instance of <code>HTMLDocument.HTMLReader</code>. Subclasses can reimplement this method to change how the
     * document gets structured if desired. (For example, to handle custom tags, or structurally represent character
     * style elements.)
     * 
     * @param pos
     *            the starting position
     * @return the reader used by the parser to load the document
     */
    @Override
    public HelpHTMLEditorKit.ParserCallback getReader(int pos) {
        Object desc = getProperty(Document.StreamDescriptionProperty);
        if( desc instanceof URL ) {
            setBase((URL) desc);
        }
        HelpHTMLReader reader = new HelpHTMLReader(pos);
        return reader;
    }

    /**
     * Returns the reader for the parser to use to load the document with HTML. This is implemented to return an
     * instance of <code>HTMLDocument.HTMLReader</code>. Subclasses can reimplement this method to change how the
     * document gets structured if desired. (For example, to handle custom tags, or structurally represent character
     * style elements.)
     * <p>
     * This is a convenience method for <code>getReader(int, int, int, HTML.Tag, TRUE)</code>.
     * 
     * @param popDepth
     *            the number of <code>ElementSpec.EndTagTypes</code> to generate before inserting
     * @param pushDepth
     *            the number of <code>ElementSpec.StartTagTypes</code> with a direction of
     *            <code>ElementSpec.JoinNextDirection</code> that should be generated before inserting, but after the
     *            end tags have been generated
     * @param insertTag
     *            the first tag to start inserting into document
     * @return the reader used by the parser to load the document
     */
    // public HelpHTMLEditorKit.ParserCallback getReader(int pos, int popDepth,
    // int pushDepth,
    // HTML.Tag insertTag) {
    // return getReader(pos, popDepth, pushDepth, insertTag, true);
    // return getReader(pos, popDepth, pushDepth, insertTag);
    // }
    /**
     * Fetches the reader for the parser to use to load the document with HTML. This is implemented to return an
     * instance of HTMLDocument.HTMLReader. Subclasses can reimplement this method to change how the document get
     * structured if desired (e.g. to handle custom tags, structurally represent character style elements, etc.).
     * 
     * @param popDepth
     *            the number of <code>ElementSpec.EndTagTypes</code> to generate before inserting
     * @param pushDepth
     *            the number of <code>ElementSpec.StartTagTypes</code> with a direction of
     *            <code>ElementSpec.JoinNextDirection</code> that should be generated before inserting, but after the
     *            end tags have been generated
     * @param insertTag
     *            the first tag to start inserting into document
     * @param insertInsertTag
     *            false if all the Elements after insertTag should be inserted; otherwise insertTag will be inserted
     * @return the reader used by the parser to load the document
     */
    /*
     * HelpHTMLEditorKit.ParserCallback getReader(int pos, int popDepth, int pushDepth, HTML.Tag insertTag, boolean
     * insertInsertTag) { Object desc = getProperty(Document.StreamDescriptionProperty); if (desc instanceof URL) {
     * setBase((URL)desc); } HelpHTMLReader reader = new HelpHTMLReader(pos, popDepth, pushDepth, insertTag,
     * insertInsertTag, false, true); return reader; }
     */

	public class HelpHTMLReader extends HTMLReader {

        public HelpHTMLReader(int offset) {
            super(offset, 0, 0, null);
        }

        /**
         * Callback from the parser. Route to the appropriate handler for the tag.
         */
        @Override
        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {

//            for(Enumeration e = a.getAttributeNames(); e.hasMoreElements(); ) {
//                Object attName = e.nextElement();
//                String attVal = (String)a.getAttribute(attName);
//                if( attVal.indexOf("http://") > -1 ) {
//                    a.removeAttribute(attName);
//                    a.addAttribute(attName, attVal.substring("http://".length()));
//                }
//            }

            logger.debug("noti's parserhook touched");
            if( t == HTML.Tag.IMG ) {

                String src = (String) a.getAttribute(HTML.Attribute.SRC);

                // da versucht doch einer was zu laden, ah!, ein bild
                // boeses blind abschnippeln, zur Strafe für den Versuch ;-)
                // boese sind: ':', '//', '!'

                // das geht moeglicherweise schoener

                int i = src.lastIndexOf(":");
                i++;
                if( i > 0 ) {
                    src = src.substring(i);
                }
                i = src.lastIndexOf("!");
                i++;
                if( i > 0 ) {
                    src = src.substring(i);
                }
                i = src.lastIndexOf("//");
                i++;
                if( i > 0 ) {
                    src = src.substring(++i);
                }

                a.removeAttribute(HTML.Attribute.SRC);

                // the url_prefix will be done by the doc's loader itselfs
                a.addAttribute(HTML.Attribute.SRC, src);

                // a.addAttribute(HTML.Attribute.SRC, "die_url_ist_wirklich_ungueltig_mfg_notitaccu.gif");
                logger.debug("BrumBrumIMG-2 fertig: {}", a);
                // return;
            }

            logger.debug("BummiGummi {}", a);
            super.handleSimpleTag(t, a, pos);

            //super(t, a, pos); 
        }
    }
}
