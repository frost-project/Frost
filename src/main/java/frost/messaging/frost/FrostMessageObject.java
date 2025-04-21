/*
  FrostMessageObject.java / Frost
  Copyright (C) 2003  Frost Project <jtcfrost.sourceforge.net>

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
package frost.messaging.frost;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.MainFrame;
import frost.gui.model.TableMember;
import frost.identities.Identity;
import frost.messaging.frost.boards.Board;
import frost.messaging.frost.gui.messagetreetable.MessageTreeTableSortStateBean;
import frost.storage.perst.messages.MessageStorage;
import frost.storage.perst.messages.PerstFrostMessageObject;
import frost.util.DateFun;

/**
 * This class holds all informations that are shown in the GUI and stored to the database.
 * It adds more fields than a MessageObjectFile uses.
 */
public class FrostMessageObject extends AbstractMessageObject implements TableMember<FrostMessageObject> {

	private static final long serialVersionUID = 1L;

	private static final Logger logger =  LoggerFactory.getLogger(FrostMessageObject.class);

    transient private PerstFrostMessageObject perstFrostMessageObject = null;

    transient private final static ArrayList<String> EMPTY_STRINGLIST = new ArrayList<String>(0);

    // additional variables for use in GUI
    private boolean isValid = false;
    private String invalidReason = null;

    private int index = -1;
    private Board board = null;
	private OffsetDateTime dateAndTime = null;

    private boolean isDeleted = false;
    private boolean isNew = false;
    private boolean isReplied = false;
    private boolean isJunk = false;
    private boolean isFlagged = false; // !
    private boolean isStarred = false; // *

    private boolean hasFileAttachments = false;
    private boolean hasBoardAttachments = false;

    private ArrayList<String> inReplyToList = null;

    private String dateAndTimeString = null;

    private boolean isDummy = false;

    public static boolean sortThreadRootMsgsAscending;

    /**
     * Construct a new empty FrostMessageObject
     */
    public FrostMessageObject() {
    }

    public FrostMessageObject(final boolean isRootnode) {
        setDummy(true);
		setDateAndTime(DateFun.toOffsetDateTime(0, DateFun.getTimeZone()));
        setSubject("(root)");
        setNew(false);
        setFromName("");
    }

    /**
     * Construct a new FrostMessageObject with the data from a MessageXmlFile.
     */
    public FrostMessageObject(final MessageXmlFile mof, final Identity sender, final Board b, final int msgIndex) {
        setValid(true);
        setFromName(mof.getFromName());
        setBoard(b);
        setIndex(msgIndex);

        try {
            setDateAndTime(mof.getDateAndTime());
        } catch(final Throwable t) {
            // never happens, we already called this method
			setDateAndTime(DateFun.toOffsetDateTime(0, DateFun.getTimeZone()));
        }
        logger.debug("MSG TIME/DATE: time_in = {}, date_in = {}, out = {}", mof.getTimeStr(), mof.getDateStr(), getDateAndTime());
        // copy values from mof
        setAttachmentList(mof.getAttachmentList());
        setContent(mof.getContent());
        setInReplyTo(mof.getInReplyTo());
        setMessageId(mof.getMessageId());
        setPublicKey(mof.getPublicKey());
        setRecipientName(mof.getRecipientName());
        setSignatureV2(mof.getSignatureV2());
        setSignatureStatus(mof.getSignatureStatus());
        setSubject(mof.getSubject());
        setIdLinePos(mof.getIdLinePos());
        setIdLineLen(mof.getIdLineLen());
        setFromIdentity(sender);

        setHasBoardAttachments(mof.getAttachmentsOfType(Attachment.BOARD).size() > 0);
        setHasFileAttachments(mof.getAttachmentsOfType(Attachment.FILE).size() > 0);
    }

    /**
     * Construct a new FrostMessageObject for an invalid message (broken, encrypted for someone else, ...).
     */
	public FrostMessageObject(final Board b, final OffsetDateTime dt, final int msgIndex, final String reason) {
        setValid( false );
        setInvalidReason(reason);
        setBoard(b);
        setDateAndTime( dt );
        setIndex( msgIndex );
    }

    // create a dummy msg
    public FrostMessageObject(final String msgId, final Board b, final ArrayList<String> ll) {
        setMessageId(msgId);
        setBoard(b);
        setDummyInReplyToList(ll);

        setDummy(true);
		setDateAndTime(DateFun.toOffsetDateTime(0, DateFun.getTimeZone()));
        setSubject("");
        setNew(false);
        setFromName("");
    }

    public void fillFromOtherMessage(final FrostMessageObject mof) {

        setDummy(false);

        setNew(mof.isNew());
        setValid(mof.isValid());
        setBoard(mof.getBoard());
        setIndex(mof.getIndex());

        setDateAndTime( mof.getDateAndTime() );

        setAttachmentList(mof.getAttachmentList());
        setContent(mof.getContent());
        setFromName(mof.getFromName());
        setInReplyTo(mof.getInReplyTo());
        setMessageId(mof.getMessageId());
        setPublicKey(mof.getPublicKey());
        setRecipientName(mof.getRecipientName());
        setSignatureV2(mof.getSignatureV2());
        setSignatureStatus(mof.getSignatureStatus());
        setSubject(mof.getSubject());
        setIdLinePos(mof.getIdLinePos());
        setIdLineLen(mof.getIdLineLen());

        setHasBoardAttachments(mof.getAttachmentsOfTypeFile().size() > 0);
        setHasFileAttachments(mof.getAttachmentsOfTypeBoard().size() > 0);
    }
    
    

    /**
     * This is called from within a cell renderer and should finish as fast as
     * possible. So we check for UNREAD and MARKED here, and return an array with
     * the results.
     * This way we evaluate the childs only one time.
     * @return  boolean[2] where boolean[0] = hasUnread and boolean[1] = hasMarked
     */
    public boolean[] hasUnreadOrMarkedChilds() {
        final boolean[] result = new boolean[2];
        result[0] = result[1] = false;

        if( getChildCount() == 0 ) {
            return result;
        }
        breadthFirstEnumeration();
		final Enumeration<TreeNode> frostMessageObjectEnumeration = breadthFirstEnumeration();
        while(frostMessageObjectEnumeration.hasMoreElements()) {
			final FrostMessageObject frostMessageObject = (FrostMessageObject) frostMessageObjectEnumeration
					.nextElement();
            if( frostMessageObject.isNew() ) {
                result[0] = true;
                // both true? finished.
                if( result[1] == true ) {
                    return result;
                }
            }
            if( frostMessageObject.isStarred() || frostMessageObject.isFlagged() ) {
                result[1] = true;
                // both true? finished.
                if( result[0] == true ) {
                    return result;
                }
            }
        }
        return result;
    }

    public FrostMessageObject getThreadRootMessage() {
        if( ((FrostMessageObject)getParent()).isRoot() ) {
            return this;
        } else {
            return ((FrostMessageObject)getParent()).getThreadRootMessage();
        }
    }

    /**
     * Dynamically loads publicKey.
     */
    @Override
    public String getPublicKey() {
        if( super.getPublicKey() == null ) {
            MessageStorage.inst().retrievePublicKey(this);
        }
        return super.getPublicKey();
    }

    /**
     * Dynamically loads signature.
     */
//    public String getSignatureV2() {
//
//        if( getSignatureV2() == null ) {
//            try {
//                MessagesStorage.inst().retrieveSignature(this);
//            } catch (SQLException e) {
//                logger.error("SQLException", e);
//            }
//        }
//        return super.getSignatureV2();
//    }

    /**
     * Dynamically loads content.
     */
    @Override
    public String getContent() {

        if( content == null ) {
            MessageStorage.inst().retrieveMessageContent(this);
            if( content == null ) {
                content = "";
            }
        }
        return content;
    }
    
    
	/**
	 * Loads content together with attached boards and files, but represented as text
	 */
	public String getCompleteContent() {
		StringBuilder result = new StringBuilder(this.getContent());
		
		if (hasFileAttachments()) {
			result.append("\n\nAttached files:\n");
			
			for(FileAttachment fileAttachment : getAttachmentsOfTypeFile()) {
				result.append(fileAttachment.getKey()).append('\n');
			}
		}
		
		if (hasBoardAttachments()) {
			result.append("\n\nAttached boards:");
			
			for(BoardAttachment boardAttachment : getAttachmentsOfTypeBoard()) {
				final Board board = boardAttachment.getBoardObj();
				result.append("\nName: ").append(board.getName()).append('\n');
				if( board.getPublicKey() != null ) {
					result.append("PubKey: ").append(board.getPublicKey()).append('\n');
					if( board.getPrivateKey() != null ) {
						result.append("PrivKey: ").append(board.getPrivateKey()).append('\n');
					}
				}
			}
		}

		return result.toString();
	}
    
    private AttachmentList<Attachment> getAttachmentListInstance() {
    	if( !containsAttachments() ) {
            if (attachments == null) {
                attachments = new AttachmentList<Attachment>();
            }
            return attachments;
        }

        if (attachments == null) {
            MessageStorage.inst().retrieveAttachments(this);
            if (attachments == null) {
                attachments = new AttachmentList<Attachment>();
            }
        }
        return attachments;
    }

    public AttachmentList<FileAttachment> getAttachmentsOfTypeFile() {
        return getAttachmentListInstance().getAllOfTypeFile();
    }
    
    public AttachmentList<BoardAttachment> getAttachmentsOfTypeBoard() {
        return getAttachmentListInstance().getAllOfTypeBoard();
    }
    
    public AttachmentList<PersonAttachment> getAttachmentsOfTypePerson() {
        return getAttachmentListInstance().getAllOfTypePerson();
    }
    
    
    /*
     * @see frost.gui.model.TableMember#compareTo(frost.gui.model.TableMember, int)
     */
    public int compareTo(final FrostMessageObject another, final int tableColumnIndex) {
        String c1 = (String) getValueAt(tableColumnIndex);
        String c2 = (String) another.getValueAt(tableColumnIndex);
        if (tableColumnIndex == 4) {
            return c1.compareTo(c2);
        } else {
            // If we are sorting by anything but date...
            if (tableColumnIndex == 2) {
                //If we are sorting by subject...
                if (c1.indexOf("Re: ") == 0) {
                    c1 = c1.substring(4);
                }
                if (c2.indexOf("Re: ") == 0) {
                    c2 = c2.substring(4);
                }
            }
            final int result = c1.compareToIgnoreCase(c2);
            if (result == 0) { // Items are the same. Date and time decides
                final String d1 = (String) getValueAt(4);
                final String d2 = (String) another.getValueAt(4);
                return d1.compareTo(d2);
            } else {
                return result;
            }
        }
    }

    /*
     * @see frost.gui.model.TableMember#getValueAt(int)
     */
	public Comparable<?> getValueAt(final int column) {
        switch(column) {
            case 0: return Integer.toString(getIndex());
            case 1: return getFromName();
            case 2: return getSubject();
            case 3: return getMessageStatusString();
            case 4: return getDateAndTimeString();
            default: return "*ERR*";
        }
    }

    public String getDateAndTimeString() {
        if( dateAndTimeString == null ) {
            // Build a String of format yyyy.mm.dd hh:mm:ssGMT
			OffsetDateTime dateTime = getDateAndTime();

			if (dateTime == null) {
				dateTime = OffsetDateTime.now(DateFun.getTimeZone());
			}

			final String dateStr = DateFun.FORMAT_DATE_EXT.format(dateTime);
			final String timeStr = DateFun.FORMAT_TIME_EXT.format(dateTime);

            final StringBuilder sb = new StringBuilder(29);
            sb.append(dateStr).append(" ").append(timeStr);

            this.dateAndTimeString = sb.toString();
        }
        return this.dateAndTimeString;
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(final Board board) {
        this.board = board;
    }

    public boolean hasBoardAttachments() {
        return hasBoardAttachments;
    }

    public void setHasBoardAttachments(final boolean hasBoardAttachments) {
        this.hasBoardAttachments = hasBoardAttachments;
    }

    public boolean hasFileAttachments() {
        return hasFileAttachments;
    }

    public void setHasFileAttachments(final boolean hasFileAttachments) {
        this.hasFileAttachments = hasFileAttachments;
    }

    @Override
    public boolean containsAttachments() {
        if( hasFileAttachments() || hasBoardAttachments() ) {
            return true;
        }
        return false;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(final int index) {
        this.index = index;
    }

    public String getInvalidReason() {
        return invalidReason;
    }

    public void setInvalidReason(final String invalidReason) {
        this.invalidReason = invalidReason;
    }

    public boolean isReplied() {
        return isReplied;
    }

    public void setReplied(final boolean isReplied) {
        this.isReplied = isReplied;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(final boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public boolean isJunk() {
        return isJunk;
    }

    public void setJunk(final boolean isJunk) {
        this.isJunk = isJunk;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    public void setFlagged(final boolean isFlagged) {
        this.isFlagged = isFlagged;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(final boolean isNew) {
        this.isNew = isNew;
    }

    public boolean isStarred() {
        return isStarred;
    }

    public void setStarred(final boolean isStarred) {
        this.isStarred = isStarred;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(final boolean isValid) {
        this.isValid = isValid;
    }

	public void setDateAndTime(final OffsetDateTime dt) {
        dateAndTime = dt;
    }

	public OffsetDateTime getDateAndTime() {
        return dateAndTime;
    }

    public void setDummy(final boolean v) {
        isDummy = v;
    }

    public boolean isDummy() {
        return isDummy;
    }

    private void setDummyInReplyToList(final ArrayList<String> l) {
        inReplyToList = l;
    }

    public ArrayList<String> getInReplyToList() {
        if( inReplyToList == null ) {
            if( getInReplyTo() == null ) {
                inReplyToList = EMPTY_STRINGLIST;
            } else {
                inReplyToList = new ArrayList<String>();
                final String s = getInReplyTo();
                final StringTokenizer st = new StringTokenizer(s, ",");
                while( st.hasMoreTokens() ) {
                    final String r = st.nextToken().trim();
                    if(r.length() > 0) {
                        inReplyToList.add(r);
                    }
                }
            }
        }
        return inReplyToList;
    }

    public void resortChildren() {
        if( getChildren() == null || getChildren().size() <= 1 ) {
            return;
        }
        // choose a comparator based on settings in SortStateBean
        final Comparator<FrostMessageObject> comparator = MessageTreeTableSortStateBean.getComparator(MessageTreeTableSortStateBean.getSortedColumn(), MessageTreeTableSortStateBean.isAscending());
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
    public void add(final MutableTreeNode mutableTreeNode, final boolean silent) {
        // add sorted
        final FrostMessageObject frostMessageObject = (FrostMessageObject)mutableTreeNode;

		if (isNodeAncestor(frostMessageObject)) {
			logger.warn("Invalid message \"{}\" is ignored!", frostMessageObject.getContent());
			return;
		}

        int[] ixs;

        if( getChildren() == null ) {
            super.add(frostMessageObject);
            ixs = new int[] { 0 };
        } else {
            // If threaded:
            //   sort first msg of a thread (child of root) descending (newest first),
            //   but inside a thread sort siblings ascending (oldest first). (thunderbird/outlook do it this way)
            // If not threaded:
            //   sort as configured in SortStateBean
            int insertPoint;
            if( MessageTreeTableSortStateBean.isThreaded() ) {
                if( isRoot() ) {
                    // child of root, sort descending
                    if( sortThreadRootMsgsAscending ) {
                        insertPoint = Collections.binarySearch(getChildren(), frostMessageObject, MessageTreeTableSortStateBean.dateComparatorAscending);
                    } else {
                        insertPoint = Collections.binarySearch(getChildren(), frostMessageObject, MessageTreeTableSortStateBean.dateComparatorDescending);
                    }
                } else {
                    // inside a thread, sort ascending
                    insertPoint = Collections.binarySearch(getChildren(), frostMessageObject, MessageTreeTableSortStateBean.dateComparatorAscending);
                }
            } else {
                final Comparator<FrostMessageObject> comparator = MessageTreeTableSortStateBean.getComparator(MessageTreeTableSortStateBean.getSortedColumn(), MessageTreeTableSortStateBean.isAscending());
                if( comparator != null ) {
                    insertPoint = Collections.binarySearch(getChildren(), frostMessageObject, comparator);
                } else {
                    insertPoint = 0;
                }
            }

            if( insertPoint < 0 ) {
                insertPoint++;
                insertPoint *= -1;
            }
            if( insertPoint >= getChildren().size() ) {
                super.add(frostMessageObject);
                ixs = new int[] { getChildren().size() - 1 };
            } else {
                super.insert(frostMessageObject, insertPoint);
                ixs = new int[] { insertPoint };
            }
        }
        if( !silent ) {
            if( MainFrame.getInstance().getMessageTreeTable().getTree().isExpanded(new TreePath(this.getPath())) ) {
                // if node is already expanded, notify new inserted row to the models
                MainFrame.getInstance().getMessageTreeModel().nodesWereInserted(this, ixs);
                if( frostMessageObject.getChildCount() > 0 ) {
                    // added node has childs, expand them all
                    MainFrame.getInstance().getMessageTreeTable().expandNode(frostMessageObject);
                }
            } else {
                // if node is not expanded, expand it, this will notify the model of the new child as well as of the old childs
                MainFrame.getInstance().getMessageTreeTable().expandNode(this);
            }
        }
    }

	@SuppressWarnings("unchecked")
	protected List<FrostMessageObject> getChildren() {
		Vector<? extends TreeNode> rawChildren = children;
		return (List<FrostMessageObject>) rawChildren;
	}

    @Override
    public String toString() {
        return getSubject();
    }

    public PerstFrostMessageObject getPerstFrostMessageObject() {
        return perstFrostMessageObject;
    }
    public void setPerstFrostMessageObject(final PerstFrostMessageObject perstFrostMessageObject) {
        this.perstFrostMessageObject = perstFrostMessageObject;
    }
}
