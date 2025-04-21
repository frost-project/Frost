/*
  MessageObjectFile.java / Frost
  Copyright (C) 2001  Frost Project <jtcfrost.sourceforge.net>

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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import frost.Core;
import frost.identities.IdentitiesManager;
import frost.identities.Identity;
import frost.identities.LocalIdentity;
import frost.util.DateFun;
import frost.util.FileAccess;
import frost.util.XMLTools;
import frost.util.XMLizable;

public class MessageXmlFile extends AbstractMessageObject implements XMLizable {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(MessageXmlFile.class);

//    private static final char[] evilChars = {'/', '\\', '*', '=', '|', '&', '#', '\"', '<', '>'}; // will be converted to _

    private String boardName = "";
    private String dateStr = "";
    private String timeStr = "";

	private OffsetDateTime dateAndTime = null;

    protected File file;

    /**
     * Constructor.
     * Used to construct an instance for a new message.
     */
    public MessageXmlFile(final FrostMessageObject mo) {

        // dateAndTime is set by the uploadthread

        setMessageId(mo.getMessageId()); // messageid
        setInReplyTo(mo.getInReplyTo()); // inreplyto
        setBoardName(mo.getBoard().getName()); // board
        setFromName(mo.getFromName()); // from

        final Identity id = getFromIdentity();
        if( id != null ) {
            setPublicKey(id.getPublicKey());
        }
        setSubject(mo.getSubject()); // subject
        setRecipientName(mo.getRecipientName()); // recipient
        setContent(mo.getContent()); // msgcontent
        setIdLinePos(mo.getIdLinePos());
        setIdLineLen(mo.getIdLineLen());

        setAttachmentList(mo.getAttachmentList());
    }

    /**
     * Constructor.
     * Used to construct an instance for an existing messagefile.
     * @param file
     * @throws MessageCreationException
     */
    public MessageXmlFile(final File file) throws MessageCreationException, Throwable {

        if (file == null) {
        	throw new MessageCreationException("Invalid input file for MessageObject. File is null.");
        } else if (!file.exists()) {
            throw new MessageCreationException(
        					"Invalid input file '" + file.getName() + "' for MessageObject. File doesn't exist.");
        } else if (file.length() < 20) { // prolog+needed tags are always > 20, but we need to filter
                                         // out the messages containing "Empty", "Invalid", "Double", "Broken"
                                         // (encrypted for someone else OR completely invalid)
            throw new MessageCreationException(
                            "Empty input file '" + file.getName() + "' for MessageObject (size < 20).", true);
        }
        this.file = file;

        loadFile();
        // ensure basic contents and formats
        if( !isValid() ) {
            throw new MessageCreationException("Message has invalid or missing fields.", MessageCreationException.INVALID_FORMAT);
        }
    }

    public File getFile() {
        return file;
    }

    /**
     * Signs message and sets signature.
     * @param privateKey
     */
    public void signMessageV2(final String privateKey) {
        final String sig = Core.getCrypto().detachedSign(getSignableContentV2(), privateKey);
        setSignatureV2(sig);
    }

    /**
     * Returns true if the message signature is valid.
     * @param pubKey
     * @return
     */
    public boolean verifyMessageSignatureV2(final String pubKey) {
        final boolean sigIsValid = Core.getCrypto().detachedVerify(getSignableContentV2(), pubKey, getSignatureV2());
        return sigIsValid;
    }

    /**
     * Returns a String containing all content that is used to sign/verify the message.
     * @return
     */
    private String getSignableContentV2() {

        final String escapeChar = "|";
        final StringBuilder allContent = new StringBuilder();

        allContent.append(getDateStr()).append(escapeChar);
        allContent.append(getTimeStr()).append(escapeChar);

        // enhanced V2 stuff
        allContent.append(getBoardName()).append(escapeChar);
        allContent.append(getFromName()).append(escapeChar);
        allContent.append(getMessageId()).append(escapeChar);
        if( getInReplyTo() != null && getInReplyTo().length() > 0 ) {
            allContent.append(getInReplyTo()).append(escapeChar);
        }
        if( getRecipientName() != null && getRecipientName().length() > 0 ) {
            allContent.append(getRecipientName()).append(escapeChar);
        }
        allContent.append(getIdLinePos()).append(escapeChar);
        allContent.append(getIdLineLen()).append(escapeChar);
        // eov2

        allContent.append(getSubject()).append(escapeChar);
        allContent.append(getContent()).append(escapeChar);
        // attachments
        for(final Iterator<Attachment> attachmentIterator = getAttachmentList().iterator(); attachmentIterator.hasNext(); ) {
            final Attachment attachment = attachmentIterator.next();

            if( attachment.getType() == Attachment.BOARD ) {
                final BoardAttachment boardAttachment = (BoardAttachment)attachment;

                allContent.append( boardAttachment.getBoardObj().getBoardFilename() ).append(escapeChar);

                if( boardAttachment.getBoardObj().getPublicKey() != null ) {
                    allContent.append( boardAttachment.getBoardObj().getPublicKey() ).append(escapeChar);
                }

                if( boardAttachment.getBoardObj().getPrivateKey() != null ) {
                    allContent.append( boardAttachment.getBoardObj().getPrivateKey() ).append(escapeChar);
                }

            } else if( attachment.getType() == Attachment.FILE ) {
                final FileAttachment fileAttachment = (FileAttachment)attachment;
                allContent.append( fileAttachment.getFileName() ).append(escapeChar);
                allContent.append( fileAttachment.getKey() ).append(escapeChar);
            }
        }
        return allContent.toString();
    }

    /**
     * @see frost.util.XMLizable#getXMLElement(org.w3c.dom.Document)
     */
    public Element getXMLElement(final Document d) {
        final Element el = d.createElement("FrostMessage");

        CDATASection cdata;
        Element current;

        if( getMessageId() != null ) {
            current = d.createElement("MessageId");
            cdata = d.createCDATASection(getMessageId());
            current.appendChild(cdata);
            el.appendChild(current);
        }

        if( getInReplyTo() != null ) {
            current = d.createElement("InReplyTo");
            cdata = d.createCDATASection(getInReplyTo());
            current.appendChild(cdata);
            el.appendChild(current);
        }

        if( getIdLinePos() > -1 && getIdLineLen() > -1 ) {
            Text txt;
            current = d.createElement("IdLinePos");
            txt = d.createTextNode(Integer.toString(getIdLinePos()));
            current.appendChild(txt);
            el.appendChild(current);
            current = d.createElement("IdLineLen");
            txt = d.createTextNode(Integer.toString(getIdLineLen()));
            current.appendChild(txt);
            el.appendChild(current);
        }

        //from
        current = d.createElement("From");
        cdata = d.createCDATASection(getFromName());
        current.appendChild(cdata);
        el.appendChild(current);

        //subject
        current = d.createElement("Subject");
        cdata = d.createCDATASection(getSubject());
        current.appendChild(cdata);
        el.appendChild(current);

        //date
        current = d.createElement("Date");
        cdata = d.createCDATASection(getDateStr());
        current.appendChild(cdata);
        el.appendChild(current);

        //time
        current = d.createElement("Time");
        cdata = d.createCDATASection(getTimeStr());
        current.appendChild(cdata);
        el.appendChild(current);

        //body
        current = d.createElement("Body");
        cdata = d.createCDATASection(getContent());
        current.appendChild(cdata);
        el.appendChild(current);

        //board
        current = d.createElement("Board");
        cdata = d.createCDATASection(getBoardName());
        current.appendChild(cdata);
        el.appendChild(current);

        //public Key
        if (getPublicKey() != null && getPublicKey().length() > 0) {
            current = d.createElement("pubKey");
            cdata = d.createCDATASection(getPublicKey());
            current.appendChild(cdata);
            el.appendChild(current);
        }

        // recipient
        if (getRecipientName() != null && getRecipientName().length() > 0) {
            current = d.createElement("recipient");
            cdata = d.createCDATASection(getRecipientName());
            current.appendChild(cdata);
            el.appendChild(current);
        }

        // signature V2
        if (getSignatureV2() != null && getSignatureV2().length() > 0) {
            current = d.createElement("SignatureV2");
            cdata = d.createCDATASection(getSignatureV2());
            current.appendChild(cdata);
            el.appendChild(current);
        }

//        // signature status
//        if( getSignatureStatus() != SIGNATURESTATUS_UNSET ) {
//            current = d.createElement("signatureStatus");
//            if( getSignatureStatus() == SIGNATURESTATUS_TAMPERED ) {
//                cdata = d.createCDATASection(SIGNATURESTATUS_TAMPERED_STR);
//            } else if( signatureStatus == SIGNATURESTATUS_OLD ) {
//                cdata = d.createCDATASection(SIGNATURESTATUS_OLD_STR);
//            } else if( signatureStatus == SIGNATURESTATUS_VERIFIED ) {
//                cdata = d.createCDATASection(SIGNATURESTATUS_VERIFIED_STR);
//            }
//            current.appendChild(cdata);
//            el.appendChild(current);
//        }

        //attachments
        if (getAttachmentList().size() > 0) {
            el.appendChild(getAttachmentList().getXMLElement(d));
        }

        return el;
    }

    /**
     * @return true if the message is basically valid
     */
    private boolean isValid() {

        if (getDateStr() == null || getDateStr().length() == 0 || getDateStr().length() > 22 ) {
            logger.error("Date validation failed.");
            return false;
        }
        if (getTimeStr() == null || getTimeStr().length() == 0) {
            logger.error("Time validation failed.");
            return false;
        }
        if (getBoardName() == null || getBoardName().length() == 0 || getBoardName().length() > 256 ) {
            logger.error("Board name validation failed.");
            return false;
        }
        if (getFromName() == null || getFromName().length() == 0 || getFromName().length() > 256 ) {
            logger.error("From name validation failed.");
            return false;
        }

        if (getSubject() == null) {
            setSubject(""); // we accept empty subjects
        } else if ( getSubject().length() > 256 ) {
            logger.error("Subject validation failed.");
            return false;
        }

        if (getContent() == null) {
            logger.error("Content validation failed, no content.");
            return false;
        }
        if (getContent().length() > (64 * 1024)) { // 64k or whatever fits in zipped data
            logger.error("Content validation failed, overlength content.");
            return false;
        }
        // don't accept messages with only an id header line, or empty messages
        final String trimmedContent = getContent().trim();
        if( trimmedContent.length() == 0 ) {
            logger.error("Content validation failed, empty.");
            return false;
        }
        if( trimmedContent.indexOf("\n") < 0 ) {
            // only one line
            if( trimmedContent.startsWith("-----") && trimmedContent.endsWith("-----") ) {
                // only id header line
                logger.error("Content validation failed, only id header line.");
                return false;
            }
        }

        return true;
    }

    /**
     * Parses the XML file and passes the FrostMessage element to XMLize load method.
     */
    protected void loadFile() throws Exception {
        Document doc = null;
        try {
            doc = XMLTools.parseXmlFile(this.file);
        } catch(final Exception ex) {  // xml format error
            final File badMessage = new File("badmessage.xml");
            if (file.renameTo(badMessage)) {
                logger.error("send the file badmessage.xml to a dev for analysis, more details below:", ex);
            }
        }

        if( doc == null ) {
            throw new Exception("MessageObject.loadFile: couldn't parse XML Document - " +
                                "File name: '" + file.getName() + "'");
        }

        Element rootNode = doc.getDocumentElement();

        // transparently decrypt and continue load on success
        if( rootNode.getTagName().equals("EncryptedFrostMessage") ) {
            // get recipient (must be I to continue)
            setRecipientName(XMLTools.getChildElementsCDATAValue(rootNode, "recipient"));
            if( getRecipientName() == null ) {
                // no recipient
                throw new Exception("encrypted message contains no 'recipient' section.");
            }
            final IdentitiesManager identitiesManager = Core.getIdentitiesManager();
            if( !identitiesManager.isMySelf(getRecipientName()) ) {
                // not for me
                throw new MessageCreationException("Encrypted message is not for me.",
                        MessageCreationException.MSG_NOT_FOR_ME);
            }
            final String base64enc = XMLTools.getChildElementsCDATAValue(rootNode, "content");
            if( base64enc == null ) {
                // no content
                throw new Exception("encrypted message contains no 'content' section.");
            }
            final byte[] base64bytes = base64enc.getBytes("ISO-8859-1");
            final byte[] encBytes = Base64.decode(base64bytes);

            // decrypt content
            final LocalIdentity receiverId = identitiesManager.getLocalIdentity(getRecipientName());
            final byte[] decContent = Core.getCrypto().decrypt(encBytes, receiverId.getPrivateKey());
            if( decContent == null ) {
                logger.error("TOFDN: Encrypted message could not be decrypted!");
                throw new MessageCreationException("Encrypted message could not be decrypted.",
                        MessageCreationException.DECRYPT_FAILED);
            }
            // decContent is an complete XML file, save it to file and load it
            FileAccess.writeFile(decContent, this.file);
            // try to load again
            try {
                doc = XMLTools.parseXmlFile(this.file);
            } catch(final Exception ex) {  // xml format error
                final File badMessage = new File("badmessage.xml");
                if (file.renameTo(badMessage)) {
                    logger.error("send the file badmessage.xml to a dev for analysis, more details below:", ex);
                }
            }

            if( doc == null ) {
                throw new Exception("MessageObject.loadFile: couldn't parse XML Document - " +
                                    "File name: '" + file.getName() + "'");
            }
            rootNode = doc.getDocumentElement();
        }
        if( rootNode.getTagName().equals("FrostMessage") == false ) {
            final File badMessage = new File("badmessage.xml");
            if (file.renameTo(badMessage)) {
                logger.error("send the file badmessage.xml to a dev for analysis.");
            }
            throw new Exception("invalid message: does not contain the root tag 'FrostMessage'");
        }
        // load the message itself
        loadXMLElement(rootNode);
    }

    /**
     * @see frost.util.XMLizable#loadXMLElement(org.w3c.dom.Element)
     */
    public void loadXMLElement(final Element e) throws SAXException {
        setMessageId(XMLTools.getChildElementsCDATAValue(e, "MessageId"));
        setInReplyTo(XMLTools.getChildElementsCDATAValue(e, "InReplyTo"));
        setFromName(XMLTools.getChildElementsCDATAValue(e, "From"));
        setSubject(XMLTools.getChildElementsCDATAValue(e, "Subject"));
        dateStr = XMLTools.getChildElementsCDATAValue(e, "Date");
        timeStr = XMLTools.getChildElementsCDATAValue(e, "Time");
        setPublicKey(XMLTools.getChildElementsCDATAValue(e, "pubKey"));
        setRecipientName(XMLTools.getChildElementsCDATAValue(e, "recipient"));
        setBoardName(XMLTools.getChildElementsCDATAValue(e, "Board"));
        setContent(XMLTools.getChildElementsCDATAValue(e, "Body"));

        setSignatureV2(XMLTools.getChildElementsCDATAValue(e, "SignatureV2"));

        final String idLinePosStr = XMLTools.getChildElementsTextValue(e, "IdLinePos");
        final String idLineLenStr = XMLTools.getChildElementsTextValue(e, "IdLineLen");
        setIdLinePosLen(idLinePosStr, idLineLenStr, ((getContent() != null )?getContent().length():0));

        // this parameter is contained in local XML messages only
        String sigstat = XMLTools.getChildElementsCDATAValue(e, "signatureStatus");
        if( sigstat != null && (sigstat=sigstat.trim()).length() > 0 ) {
            setSignatureStatusFromString(sigstat);
        }

        final List<Element> l = XMLTools.getChildElementsByTagName(e, "AttachmentList");
        if (l.size() > 0) {
            final Element attachmentsElement = l.get(0);
            getAttachmentList().loadXMLElement(attachmentsElement);
        }
    }

    private void setIdLinePosLen(final String pos, final String len, final int contentLen) {
        if( pos == null || len == null || pos.length() == 0 || len.length() == 0 ) {
            return;
        }
        int p = -1, l = -1;
        try {
            p = Integer.parseInt(pos);
            l = Integer.parseInt(len);
        } catch(final Throwable t) {
            return;
        }
        if( p < 0 || l < 0 ) {
            return;
        }
        if( p+l > contentLen ) {
            return;
        }
        setIdLinePos(p);
        setIdLineLen(l);
    }

    public void setFile(final File f) {
        file = f;
    }

    /**
     * Save the message.
     */
    public boolean save() {
        if( file == null ) {
            logger.error("internal File pointer is not set");
            return false;
        }
        return saveToFile(file);
    }

    /**
     * Save the message to the specified file.
     * Does not change the internal File pointer.
     */
    public boolean saveToFile(final File f) {
        final File tmpFile = new File(f.getPath() + "sav.tmp");
        boolean success = false;
        try {
            final Document doc = XMLTools.createDomDocument();
            doc.appendChild(getXMLElement(doc));
            success = XMLTools.writeXmlFile(doc, tmpFile.getPath());
        } catch (final Exception e) {
            logger.error("Error while saving message.", e);
        }
        if (success && tmpFile.length() > 0) {
            if( f.isFile() && f.delete() == false ) {
                logger.error("Error while saving message, delete failed.");
            }
            if( tmpFile.renameTo(f) == false ) {
                logger.error("Error while saving message, renameTo failed.");
                return false;
            }
        } else {
            tmpFile.delete();
        }
        return success;
    }

    /**
     * Encrypt the complete XML message file with public key of recipient and
     * create an EncryptedFrostMessage XML file that contains the encryted content
     * in base64 format. Saves the resulting XML file into targetFile.
     *
     * @param msgFile  the message xml file that is the input for encryption
     * @param recipientPublicKey  recipients public key
     * @param targetFile  the target xml file for the encrypted message
     * @return
     */
    public static boolean encryptForRecipientAndSaveCopy(final File msgFile, final Identity recipient, final File targetFile) {
        final byte[] xmlContent = FileAccess.readByteArray(msgFile);
        final byte[] encContent = Core.getCrypto().encrypt(xmlContent, recipient.getPublicKey());
        String base64enc;
        try {
            base64enc = new String(Base64.encode(encContent), "ISO-8859-1");
        } catch (final UnsupportedEncodingException ex) {
            logger.error("ISO-8859-1 encoding is not supported.", ex);
            return false;
        }

        final Document doc = XMLTools.createDomDocument();
        final Element el = doc.createElement("EncryptedFrostMessage");

        CDATASection cdata;
        Element current;

        // recipient
        current = doc.createElement("recipient");
        cdata = doc.createCDATASection(recipient.getUniqueName());
        current.appendChild(cdata);
        el.appendChild(current);

        // base64 content
        current = doc.createElement("content");
        cdata = doc.createCDATASection(base64enc);
        current.appendChild(cdata);
        el.appendChild(current);

        doc.appendChild(el);
        return XMLTools.writeXmlFile(doc, targetFile.getPath());
    }

    /**
     * Compares the given message in otherMsgFile with this message.
     * Compares content (body), subject, from and attachments.
     */
    public boolean equals(final File otherMsgFile) {
        try {
            final MessageXmlFile otherMessage = new MessageXmlFile(otherMsgFile);
            return equals(otherMessage);
        } catch(final Throwable t) {
            logger.error("Handled Exception in compareTo(File otherMsgFile)", t);
            return false; // We assume that the other message is different (it may be corrupted)
        }
    }

    /**
     * Compares the given otherMsg with this message.
     * Compares content (body), subject, from and attachments.
     */
    public boolean equals(final MessageXmlFile otherMsg) {
        try {
            // We compare the messages by content (body), subject, from and attachments
            if (!getContent().equals(otherMsg.getContent())) {
                return false;
            }
            if (!getSubject().equals(otherMsg.getSubject())) {
                return false;
            }
            if (!getFromName().equals(otherMsg.getFromName())) {
                return false;
            }

            final AttachmentList<Attachment> attachments1 = otherMsg.getAttachmentList();
            final AttachmentList<Attachment> attachments2 = getAttachmentList();
            if (attachments1.size() != attachments2.size()) {
                return false;
            }

            final Iterator<Attachment> iterator1 = attachments1.iterator();
            final Iterator<Attachment> iterator2 = attachments2.iterator();
            while (iterator1.hasNext()) {
                final Attachment attachment1 = iterator1.next();
                final Attachment attachment2 = iterator2.next();
                if (attachment1.compareTo(attachment2) != 0) {
                    return false;
                }
            }
            return true;
        } catch (final Throwable t) {
            logger.error("Handled Exception in compareTo(MessageObject otherMsg)", t);
            return false; // We assume that the local message is different (it may be corrupted)
        }
    }

    public String getBoardName() {
        return boardName;
    }
    public String getDateStr() {
        return dateStr;
    }
    public String getTimeStr() {
        return timeStr;
    }

    public void setBoardName(final String board) {
        this.boardName = board;
    }

	public OffsetDateTime getDateAndTime() throws Throwable {
        if( dateAndTime == null ) {
			final long millis = DateFun
					.toMilli(DateFun.parseDate(getDateStr(), DateFun.FORMAT_DATE, DateFun.getTimeZone()))
					+ DateFun.toMilli(DateFun.parseTime(getTimeStr(), DateFun.FORMAT_TIME, DateFun.getTimeZone()));
			dateAndTime = DateFun.toOffsetDateTime(millis, DateFun.getTimeZone());
        }
        return dateAndTime;
    }

	public void setDateAndTime(final OffsetDateTime dt) {
		dateAndTime = dt;
		dateStr = DateFun.FORMAT_DATE.format(dt);
		timeStr = DateFun.FORMAT_TIME_EXT.format(dt);
	}
}
