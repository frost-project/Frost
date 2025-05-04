/*
AltEdit.java / Frost
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
package frost.ext;

import java.awt.Frame;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.util.FileAccess;
import frost.util.gui.translation.Language;

/**
 * Class provides alternate editor functionality.
 *
 * @author bback
 */
public class AltEdit extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(AltEdit.class);

    private final Language language = Language.getInstance();

    private final Frame parentFrame;
    private final String linesep = System.getProperty("line.separator");

    private final String oldSubject;
    private final String oldText;

    private final String SUBJECT_MARKER = language.getString("AltEdit.markerLine.subject");
    private final String TEXT_MARKER = language.getString("AltEdit.markerLine.text");

    private final Object transferObject;
    private final AltEditCallbackInterface callbackTarget;

    public AltEdit(final String subject, final String text, final Frame parentFrame, final Object transferObject, final AltEditCallbackInterface callbackTarget) {
        this.parentFrame = parentFrame;
        this.oldSubject = subject;
        this.oldText = text;
        this.transferObject = transferObject;
        this.callbackTarget = callbackTarget;
    }

    private void callbackMessageFrame(final String newSubject, final String newText) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                callbackTarget.altEditCallback(transferObject, newSubject, newText);
            }
        });
    }

    @Override
    public void run() {

        // paranoia
        if( !Core.frostSettings.getBoolean(Settings.ALTERNATE_EDITOR_ENABLED) ) {
            callbackMessageFrame(null, null);
            return;
        }

        final String editor = Core.frostSettings.getString(Settings.ALTERNATE_EDITOR_COMMAND);

        if( editor == null || editor.length() == 0 ) {
            JOptionPane.showMessageDialog(parentFrame,
                    language.getString("AltEdit.errorDialog.noAlternateEditorConfigured"),
                    language.getString("AltEdit.errorDialogs.title"),
                    JOptionPane.ERROR_MESSAGE);
            callbackMessageFrame(null, null);
            return;
        }

        if( editor.indexOf("%f") == -1 ) {
            JOptionPane.showMessageDialog(parentFrame,
                    language.getString("AltEdit.errorDialog.missingPlaceholder"),
                    language.getString("AltEdit.errorDialogs.title"),
                    JOptionPane.ERROR_MESSAGE);
            callbackMessageFrame(null, null);
            return;
        }

        // part before and after %f
        final String editor_pre_file = editor.substring(0, editor.indexOf("%f"));
        final String editor_post_file = editor.substring(editor.indexOf("%f") + 2, editor.length());

        final File editFile = FileAccess.createTempFile("frostmsg", ".txt");
        editFile.deleteOnExit();

        StringBuilder sb = new StringBuilder();
        sb.append(language.getString("AltEdit.textFileMessage.1")).append(linesep);
        sb.append(language.getString("AltEdit.textFileMessage.2")).append(linesep);
        sb.append(language.getString("AltEdit.textFileMessage.3")).append(linesep).append(linesep);
        sb.append(SUBJECT_MARKER).append(linesep);
        sb.append(oldSubject).append(linesep).append(linesep);
        sb.append(oldText).append(linesep); // contains new from-header-line
        sb.append(TEXT_MARKER).append(linesep);

        if( FileAccess.writeFile(sb.toString(), editFile, "UTF-8") == false ) {
            JOptionPane.showMessageDialog(parentFrame,
                    language.getString("AltEdit.errorDialog.couldNotCreateMessageFile")+": "+editFile.getPath(),
                    language.getString("AltEdit.errorDialogs.title"),
                    JOptionPane.ERROR_MESSAGE);
            callbackMessageFrame(null, null);
            return;
        }
        sb = null;

        final String editorCmdLine = editor_pre_file + editFile.getPath() + editor_post_file;
        try {
          run_wait(editorCmdLine);
        } catch(final Throwable t) {
            JOptionPane.showMessageDialog(parentFrame,
                    language.getString("AltEdit.errorDialog.couldNotStartEditorUsingCommand")+": "+editorCmdLine+"\n"+t.toString(),
                    language.getString("AltEdit.errorDialogs.title"),
                    JOptionPane.ERROR_MESSAGE);
            editFile.delete();
            callbackMessageFrame(null, null);
            return;
        }

        final List<String> lines = FileAccess.readLines(editFile, "UTF-8");
        // adding the exec stdout/stderr output
        //lines.addAll(exec_output);

        if( lines.size() < 4 ) { // subject marker,subject,from line, text marker
            JOptionPane.showMessageDialog(parentFrame,
                    language.getString("AltEdit.errorDialog.invalidReturnedMessageFile"),
                    language.getString("AltEdit.errorDialogs.title"),
                    JOptionPane.ERROR_MESSAGE);
            editFile.delete();
            callbackMessageFrame(null, null);
            return;
        }

        String newSubject = null;
        final StringBuilder newTextSb = new StringBuilder();

        boolean inNewText = false;
        for( final Iterator<String> it=lines.iterator(); it.hasNext(); ) {
            String line = it.next();

            if( inNewText ) {
                newTextSb.append(line).append(linesep);
                continue;
            }

            if( line.equals(SUBJECT_MARKER) ) {
                // next line is the new subject
                if( it.hasNext() == false ) {
                    JOptionPane.showMessageDialog(parentFrame,
                            language.getString("AltEdit.errorDialog.invalidReturnedMessageFile"),
                            language.getString("AltEdit.errorDialogs.title"),
                            JOptionPane.ERROR_MESSAGE);
                    editFile.delete();
                    callbackMessageFrame(null, null);
                    return;
                }
                line = it.next();
                if( line.equals(TEXT_MARKER) ) {
                    JOptionPane.showMessageDialog(parentFrame,
                            language.getString("AltEdit.errorDialog.invalidReturnedMessageFile"),
                            language.getString("AltEdit.errorDialogs.title"),
                            JOptionPane.ERROR_MESSAGE);
                    editFile.delete();
                    callbackMessageFrame(null, null);
                    return;
                }
                newSubject = line.trim();
                continue;
            }

            if( line.equals(TEXT_MARKER) ) {
                // text begins
                inNewText = true;
            }
        }

        if( newSubject == null ) {
            JOptionPane.showMessageDialog(parentFrame,
                    language.getString("AltEdit.errorDialog.invalidReturnedMessageFile"),
                    language.getString("AltEdit.errorDialogs.title"),
                    JOptionPane.ERROR_MESSAGE);
            editFile.delete();
            callbackMessageFrame(null, null);
            return;
        }

        // finished, we have a newSubject and a newText now
        callbackMessageFrame(newSubject, newTextSb.toString());
    }

    /**
     * start an external program, and return their output
     * @param order the command to execute
     * @return the output generated by the program. Standard ouput and Error output are captured.
     */
    public static List<String> run_wait(final String order) throws Throwable {
        logger.info("-------------------------------------------------------------------");
        logger.info("Execute: {}", order);
        logger.info("-------------------------------------------------------------------");

        final ArrayList<String> result = new ArrayList<String>();

        final Process p = Runtime.getRuntime().exec(order);  // java 1.4 String Order
        //ProcessBuilder pb = new ProcessBuilder(order);   // java 1.5 List<String> order
        //Process p = pb.start();

        final InputStream stdOut = p.getInputStream();
        final InputStream stdErr = p.getErrorStream();

        List<String> tmpList;
        tmpList = FileAccess.readLines(stdOut, "UTF-8");
        if( tmpList != null ) {
            result.addAll(tmpList);
        }
        tmpList = FileAccess.readLines(stdErr, "UTF-8");
        if( tmpList != null ) {
            result.addAll(tmpList);
        }
        p.waitFor(); // wait really!
        return result;
    }
}
