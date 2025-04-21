package frost.util.gui;

import java.awt.Color;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.JTextComponent;

// Derived from "The Java Developers Almanac 1.4"

/**
 * This is a Highlighter for JtextComponents. It can be used to highlight
 * text in the component with a colored background.
 */
public class TextHighlighter {

    private final Color color;
    private final boolean matchAnyCase;
    // An instance of the private subclass of the default highlight painter
    Highlighter.HighlightPainter myHighlightPainter;

    /**
     * @param col  Background color for highlighted text
     */
    public TextHighlighter(final Color col) {
       color = col;
       myHighlightPainter = new MyHighlightPainter(color);
       this.matchAnyCase = true;
    }

    /**
     * @param col  Background color for highlighted text
     * @param matchAnyCase  Should HUGO be highlighted if the highlight word is hugo? true means yes
     */
    public TextHighlighter(final Color col, final boolean matchAnyCase) {
       color = col;
       myHighlightPainter = new MyHighlightPainter(color);
       this.matchAnyCase = matchAnyCase;
    }

    public void highlight(final JTextComponent textComp, final List<String> patterns, boolean removeOldHighlights) {
        for( final String p : patterns ) {
            highlight(textComp, p, removeOldHighlights);
            if( removeOldHighlights ) {
                removeOldHighlights = false; // call with remove only first time
            }
        }
    }

//  Creates highlights around all occurrences of pattern in textComp
    public void highlight(final JTextComponent textComp, final String pattern, final boolean removeOldHighlights) {
        if( removeOldHighlights ) {
            // First remove all old highlights
            removeHighlights(textComp);
        }

        try {
            final Highlighter hilite = textComp.getHighlighter();
            final Document doc = textComp.getDocument();
            String text = doc.getText(0, doc.getLength());
            if( matchAnyCase ) {
                text = text.toLowerCase();
            }
            int pos = 0;

            // Search for pattern
            while ((pos = text.indexOf(pattern, pos)) >= 0) {
                // Create highlighter using private painter and apply around pattern
                hilite.addHighlight(pos, pos+pattern.length(), myHighlightPainter);
                pos += pattern.length();
            }
        } catch (final BadLocationException e) {
        }
    }

    public void highlight(final JTextComponent textComp, final int pos, final int len, final boolean removeOldHighlights) {
        if( removeOldHighlights ) {
            // First remove all old highlights
            removeHighlights(textComp);
        }

        try {
            final Highlighter hilite = textComp.getHighlighter();
            hilite.addHighlight(pos, pos+len, myHighlightPainter);
        } catch (final BadLocationException e) {
        }
    }

    // Removes only our private highlights
    public void removeHighlights(final JTextComponent textComp) {
        final Highlighter hilite = textComp.getHighlighter();
        final Highlighter.Highlight[] hilites = hilite.getHighlights();

        for( final Highlight element : hilites ) {
            if (element.getPainter() instanceof MyHighlightPainter) {
                hilite.removeHighlight(element);
            }
        }
    }

    // A private subclass of the default highlight painter
	private class MyHighlightPainter extends DefaultHighlightPainter {

        public MyHighlightPainter(final Color color) {
            super(color);
        }
    }
}
