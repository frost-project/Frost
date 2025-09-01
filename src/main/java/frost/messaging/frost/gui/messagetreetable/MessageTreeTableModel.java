/*
 * Copyright 1997-1999 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT OF OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THIS SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

package frost.messaging.frost.gui.messagetreetable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import frost.MainFrame;
import frost.messaging.frost.FrostMessageObject;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

/**
 * An implementation of TreeTableModel that uses reflection to answer
 * TableModel methods. This works off a handful
 * of values. A TreeNode is used to answer all the TreeModel related
 * methods (similiar to AbstractTreeTableModel and DefaultTreeModel).
 * The column names are specified in the constructor. The values for
 * the columns are dynamically obtained via reflection, you simply
 * provide the method names. The methods used to set a particular value are
 * also specified as an array of method names, a null method name, or
 * null array indicates the column isn't editable. And the class types,
 * used for the TableModel method getColumnClass are specified in the
 * constructor.
 *
 * @author Scott Violet
 */
public class MessageTreeTableModel extends DefaultTreeModel implements TreeTableModel, LanguageListener {

	private static final long serialVersionUID = 1L;

	private transient Language language;

    public final static int COLUMN_INDEX_FLAGGED = 0;
    public final static int COLUMN_INDEX_STARRED = 1;
    public final static int COLUMN_INDEX_SUBJECT = 2;
    public final static int COLUMN_INDEX_FROM    = 3;
    public final static int COLUMN_INDEX_INDEX   = 4;
    public final static int COLUMN_INDEX_JUNK    = 5;
    public final static int COLUMN_INDEX_SIG     = 6;
    public final static int COLUMN_INDEX_DATE    = 7;

    public final static int MAX_COLUMN_INDEX     = 7;

    protected final static String columnNames[] = new String[MAX_COLUMN_INDEX + 1];

    /**
     * Constructor for creating a DynamicTreeTableModel.
     */
    public MessageTreeTableModel(final TreeNode root) {
        super(root);
        language = Language.getInstance();
        language.addLanguageListener(this);
        refreshLanguage();
    }

    public void languageChanged(final LanguageEvent event) {
        refreshLanguage();
    }

    private void refreshLanguage() {

        columnNames[COLUMN_INDEX_FLAGGED] = "!";
        columnNames[COLUMN_INDEX_STARRED] = "*";
        columnNames[COLUMN_INDEX_SUBJECT] = language.getString("MessagePane.messageTable.subject");
        columnNames[COLUMN_INDEX_FROM]    = language.getString("MessagePane.messageTable.from");
        columnNames[COLUMN_INDEX_INDEX]   = language.getString("MessagePane.messageTable.index");
        columnNames[COLUMN_INDEX_JUNK]    = "J";
        columnNames[COLUMN_INDEX_SIG]     = language.getString("MessagePane.messageTable.sig");
        columnNames[COLUMN_INDEX_DATE]    = language.getString("MessagePane.messageTable.date");

        try {
            final TableColumnModel tcm = MainFrame.getInstance().getMessagePanel().getMessageTable().getTableHeader().getColumnModel();
            for(int x=0; x<tcm.getColumnCount(); x++) {
                final TableColumn tc = tcm.getColumn(x);
                tc.setHeaderValue(columnNames[tc.getModelIndex()]);
            }
        } catch(final NullPointerException e) {
            // could occur during startup, ignore
            // this code is intended to manually change the header strings if the language was changed
        }
    }

    //
    // TreeModel interface
    //

    /**
     * TreeModel method to return the number of children of a particular
     * node. Since <code>node</code> is a TreeNode, this can be answered
     * via the TreeNode method <code>getChildCount</code>.
     */
    @Override
    public int getChildCount(final Object node) {
        return ((TreeNode)node).getChildCount();
    }

    /**
     * TreeModel method to locate a particular child of the specified
     * node. Since <code>node</code> is a TreeNode, this can be answered
     * via the TreeNode method <code>getChild</code>.
     */
    @Override
    public Object getChild(final Object node, final int i) {
        return ((TreeNode)node).getChildAt(i);
    }

    //
    //  The TreeTable interface.
    //

    /**
     * Returns the number of column names passed into the constructor.
     */
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * Returns the column name passed into the constructor.
     */
    public String getColumnName(final int column) {
    	if (columnNames == null || column < 0 || column >= columnNames.length) {
    	    return null;
    	}
    	return columnNames[column];
    }

    /**
     * Returns the column class for column <code>column</code>. This
     * is set in the constructor.
     */
    public Class<?> getColumnClass(final int column) {
        if( column == COLUMN_INDEX_SUBJECT ) {
            return TreeTableModel.class;
        }
        if( column == COLUMN_INDEX_FLAGGED || column == COLUMN_INDEX_STARRED || column == COLUMN_INDEX_JUNK ) {
            return Boolean.class;
        }
        return String.class;
    }

    /**
     * Returns the value for the column <code>column</code> and object
     * <code>node</code>. The return value is determined by invoking
     * the method specified in constructor for the passed in column.
     */
    public Object getValueAt(final Object node, final int column) {

        if( node instanceof FrostMessageObject ) {
            final FrostMessageObject mo = (FrostMessageObject)node;
            if( mo.isDummy() ) {
                // show no text for dummy msgs
                switch(column) {
                    case COLUMN_INDEX_FLAGGED: return Boolean.FALSE;
                    case COLUMN_INDEX_STARRED: return Boolean.FALSE;
                    // 2 is tree+subject column
                    case COLUMN_INDEX_FROM: return "";
                    case COLUMN_INDEX_INDEX: return "";
                    case COLUMN_INDEX_JUNK: return Boolean.FALSE;
                    case COLUMN_INDEX_SIG: return "";
                    case COLUMN_INDEX_DATE: return "";
                    default: return "*ERR*";
                }
            } else {
                switch(column) {
                    case COLUMN_INDEX_FLAGGED: return Boolean.valueOf(mo.isFlagged());
                    case COLUMN_INDEX_STARRED: return Boolean.valueOf(mo.isStarred());
                    // 2 is tree+subject column
                    case COLUMN_INDEX_FROM: return mo.getFromName();
                    case COLUMN_INDEX_INDEX: return Integer.toString(mo.getIndex());
                    case COLUMN_INDEX_JUNK: return Boolean.valueOf(mo.isJunk());
                    case COLUMN_INDEX_SIG: return mo.getMessageStatusString();
                    case COLUMN_INDEX_DATE: return mo.getDateAndTimeString();
                    default: return "*ERR*";
                }
            }
        } else {
            return "*ERR*";
        }
    }

    /**
     * <code>isCellEditable</code> is invoked by the JTreeTable to determine
     * if a particular entry can be added. This is overridden to return true
     * for the first column, assuming the node isn't the root, as well as
     * returning two for the second column if the node is a BookmarkEntry.
     * For all other columns this returns false.
     */
    public boolean isCellEditable(final Object node, final int column) {
        if( column == COLUMN_INDEX_SUBJECT ) {
            return true; // tree column
        }
        return false;
    }

    public void setValueAt(final Object aValue, final Object node, final int column) {
    }
}
