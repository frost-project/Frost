/*
  FreetalkMessage.java / Frost
  Copyright (C) 2009  Frost Project <jtcfrost.sourceforge.net>

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
package frost.messaging.freetalk;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.MainFrame;
import frost.messaging.freetalk.boards.FreetalkBoard;
import frost.messaging.freetalk.gui.messagetreetable.FreetalkMessageTreeTableSortStateBean;
import frost.util.DateFun;

/**
 * A Freetalk message.
 */
public class FreetalkMessage extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 1L;

	private static final Logger logger =  LoggerFactory.getLogger(FreetalkMessage.class);

    private FreetalkBoard board = null;
    private String msgId = null;
    private int msgIndex = 0;
    private String title = null;
    private String author = null;
    private long dateMillis = 0;
    private long fetchDateMillis = 0;
    private String parentMsgID = null;
    private String threadRootMsgID = null;
    private List<FreetalkFileAttachment> fileAttachments = null;

    private String content = "";

    private String dateAndTimeString = null;

    public static boolean sortThreadRootMsgsAscending;

    /**
     * Constructor for a dummy root node.
     */
    public FreetalkMessage(final boolean isRootnode) {
        board = new FreetalkBoard("(root)");
    }

    /**
     * Constructor used when a new message is received.
     */
    public FreetalkMessage(
            final FreetalkBoard board,
            final String msgId,
            final int msgIndex,
            final String title,
            final String author,
            final long dateMillis,
            final long fetchDateMillis,
            final String parentMsgID,
            final String threadRootMsgID,
            final List<FreetalkFileAttachment> fileAttachments)
    {
        super();
        this.board = board;
        this.msgId = msgId;
        this.msgIndex = msgIndex;
        this.title = title;
        this.author = author;
        this.dateMillis = dateMillis;
        this.fetchDateMillis = fetchDateMillis;
        this.parentMsgID = parentMsgID;
        this.threadRootMsgID = threadRootMsgID;
        this.fileAttachments = fileAttachments;
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public FreetalkBoard getBoard() {
        return board;
    }

    public String getMsgId() {
        return msgId;
    }

    public int getMsgIndex() {
        return msgIndex;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public long getFetchDateMillis() {
        return fetchDateMillis;
    }

    public boolean isThread() {
        return parentMsgID == null;
    }

    public String getParentMsgID() {
        return parentMsgID;
    }

    public String getThreadRootMsgID() {
        return threadRootMsgID;
    }

    public List<FreetalkFileAttachment> getFileAttachments() {
        return fileAttachments;
    }

    public String getContent() {
        return content;
    }
    public void setContent(final String c) {
        if (c == null) {
            logger.warn("!!!!!!!!!!!!! prevented null content");
            return;
        }
        content = c;
    }

    public String getDateAndTimeString() {
        if (dateAndTimeString == null) {
            // Build a String of format yyyy.mm.dd hh:mm:ssGMT
			final OffsetDateTime dateTime = DateFun.toOffsetDateTime(getDateMillis(), DateFun.getTimeZone());

			final String dateStr = DateFun.FORMAT_DATE_EXT.format(dateTime);
			final String timeStr = DateFun.FORMAT_TIME_EXT.format(dateTime);

            final StringBuilder sb = new StringBuilder(29);
            sb.append(dateStr).append(" ").append(timeStr);

            this.dateAndTimeString = sb.toString();
        }
        return dateAndTimeString;
    }

    public void resortChildren() {
        if( getChildren() == null || getChildren().size() <= 1 ) {
            return;
        }
        // choose a comparator based on settings in SortStateBean
        final Comparator<FreetalkMessage> comparator =
            FreetalkMessageTreeTableSortStateBean.getComparator(
                    FreetalkMessageTreeTableSortStateBean.getSortedColumn(), FreetalkMessageTreeTableSortStateBean.isAscending());
        if( comparator != null ) {
            Collections.sort(getChildren(), comparator);
        }
    }

    @Override
    public void add(final MutableTreeNode n) {
        add(n, true);
    }

    /**
     * Overwritten add to add new nodes sorted to a parent node
     */
    public void add(final MutableTreeNode nn, final boolean silent) {
        // add sorted
        final FreetalkMessage n = (FreetalkMessage)nn;
        int[] ixs;

        if( getChildren() == null ) {
            super.add(n);
            ixs = new int[] { 0 };
        } else {
            // If threaded:
            //   sort first msg of a thread (child of root) descending (newest first),
            //   but inside a thread sort siblings ascending (oldest first). (thunderbird/outlook do it this way)
            // If not threaded:
            //   sort as configured in SortStateBean
            int insertPoint;
            if( FreetalkMessageTreeTableSortStateBean.isThreaded() ) {
                if( isRoot() ) {
                    // child of root, sort descending
                    if( sortThreadRootMsgsAscending ) {
                        insertPoint = Collections.binarySearch(getChildren(), n, FreetalkMessageTreeTableSortStateBean.dateComparatorAscending);
                    } else {
                        insertPoint = Collections.binarySearch(getChildren(), n, FreetalkMessageTreeTableSortStateBean.dateComparatorDescending);
                    }
                } else {
                    // inside a thread, sort ascending
                    insertPoint = Collections.binarySearch(getChildren(), n, FreetalkMessageTreeTableSortStateBean.dateComparatorAscending);
                }
            } else {
                final Comparator<FreetalkMessage> comparator = FreetalkMessageTreeTableSortStateBean.getComparator(
                        FreetalkMessageTreeTableSortStateBean.getSortedColumn(), FreetalkMessageTreeTableSortStateBean.isAscending());
                if( comparator != null ) {
                    insertPoint = Collections.binarySearch(getChildren(), n, comparator);
                } else {
                    insertPoint = 0;
                }
            }

            if( insertPoint < 0 ) {
                insertPoint++;
                insertPoint *= -1;
            }
            if( insertPoint >= getChildren().size() ) {
                super.add(n);
                ixs = new int[] { getChildren().size() - 1 };
            } else {
                super.insert(n, insertPoint);
                ixs = new int[] { insertPoint };
            }
        }
        if( !silent ) {
            if( MainFrame.getInstance().getFreetalkMessageTab().getMessagePanel().getMessageTable().getTree().isExpanded(new TreePath(this.getPath())) ) {
                // if node is already expanded, notify new inserted row to the models
                MainFrame.getInstance().getFreetalkMessageTab().getMessagePanel().getMessageTreeModel().nodesWereInserted(this, ixs);
                if( n.getChildCount() > 0 ) {
                    // added node has childs, expand them all
                    MainFrame.getInstance().getFreetalkMessageTab().getMessagePanel().getMessageTable().expandNode(n);
                }
            } else {
                // if node is not expanded, expand it, this will notify the model of the new child as well as of the old childs
                MainFrame.getInstance().getFreetalkMessageTab().getMessagePanel().getMessageTable().expandNode(this);
            }
        }
    }

	@SuppressWarnings("unchecked")
	protected List<FreetalkMessage> getChildren() {
		Vector<? extends TreeNode> rawChildren = children;
		return (List<FreetalkMessage>) rawChildren;
	}
}
