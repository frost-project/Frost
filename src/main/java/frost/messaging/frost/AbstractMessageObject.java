/*
 AbstractMessageObject.java / Frost
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

package frost.messaging.frost;

/**
 * This class holds the basic variables of a message.
 * It is used by the MessageObjectFile (XML file representation)
 * and the FrostMessageObject (database representation).
 */
public abstract class AbstractMessageObject extends AbstractMessageStatusProvider {

	private static final long serialVersionUID = 1L;

	protected AttachmentList<Attachment> attachments = null;
    protected AttachmentList<FileAttachment> fileAttachments = null;
    protected AttachmentList<BoardAttachment> boardAttachments = null;
    
    protected String content = null;
    private String subject = "";
    private String recipientName = ""; // set if msg was encrypted
    private String signatureV2 = ""; // set if message is signed with V2 format
    private String messageId = null;
    private String inReplyTo = null;
    private int idLinePos = -1;
    private int idLineLen = -1;

    public String getContent() {
        return content;
    }
    public String getRecipientName() {
        return recipientName;
    }
    public String getSignatureV2() {
        return signatureV2;
    }
    public String getSubject() {
        return subject;
    }
    public String getMessageId() {
        return messageId;
    }
    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setContent(final String content) {
        this.content = content;
    }
    public void setSubject(final String subject) {
        this.subject = subject;
    }
    public void setRecipientName(final String rec) {
        recipientName = rec;
    }
    public void setSignatureV2(final String sig) {
        signatureV2 = sig;
    }
    public void setInReplyTo(final String s) {
        this.inReplyTo = s;
    }
    public void setMessageId(final String s) {
        this.messageId = s;
    }

    /**
     * This method adds a new Attachment to the attachments list.
     * @param attachment the new Attachment to add to the attachments list.
     */
    public void addAttachment(final Attachment attachment) {
        getAttachmentList().add(attachment);
    }

    /**
     * This method returns the AttachmentList. If no one exists, it creates a new one.
     * @return the AttachmentList
     */
    public AttachmentList<Attachment> getAttachmentList() {
        if (attachments == null) {
            attachments = new AttachmentList<Attachment>();
        }
        return attachments;
    }

    public void setAttachmentList(final AttachmentList<Attachment> al) {
        this.attachments = al;
    }

    /**
     * This method returns an AttachmentList containing all of the
     * attachments of the given type. The type can be one of those:
     *  Attachment.FILE
     *  Attachment.BOARD
     *  Attachment.PERSON (currently unused)
     * @param type the type of attachments to return in the AttachmentList
     * @return an AttachmentList containing all of the attachments of the given type.
     */
    public AttachmentList<Attachment> getAttachmentsOfType(final int type) {
        if (attachments == null) {
            attachments = new AttachmentList<Attachment>();
        }
        return attachments.getAllOfType(type);
    }
    
    public AttachmentList<FileAttachment> getAttachmentsOfTypeFile() {
        if (attachments == null) {
            attachments = new AttachmentList<Attachment>();
        }
        return attachments.getAllOfTypeFile();
    }
    
    public AttachmentList<BoardAttachment> getAttachmentsOfTypeBoard() {
        if (attachments == null) {
            attachments = new AttachmentList<Attachment>();
        }
        return attachments.getAllOfTypeBoard();
    }
    
    public AttachmentList<PersonAttachment> getAttachmentsOfTypePerson() {
        if (attachments == null) {
            attachments = new AttachmentList<Attachment>();
        }
        return attachments.getAllOfTypePerson();
    }

    public boolean containsAttachments() {
        if ((getAttachmentsOfTypeBoard().size() > 0) ||
            (getAttachmentsOfTypeFile().size() > 0)) {
            return true;
        }
        return false;
    }

    public int getIdLineLen() {
        return idLineLen;
    }
    public void setIdLineLen(final int idLineLen) {
        this.idLineLen = idLineLen;
    }

    public int getIdLinePos() {
        return idLinePos;
    }
    public void setIdLinePos(final int idLinePos) {
        this.idLinePos = idLinePos;
    }
}
