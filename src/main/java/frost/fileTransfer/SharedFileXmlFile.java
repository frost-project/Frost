/*
SharedFileXmlFile.java / Frost
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
package frost.fileTransfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import frost.util.XMLTools;
import frost.util.XMLizable;

public class SharedFileXmlFile implements XMLizable {

	private static final long serialVersionUID = 1L;

	public static final int MAX_COMMENT_LENGTH = 100;
    public static final int MAX_KEYWORDS_LENGTH = 100;

	private static final Logger logger = LoggerFactory.getLogger(SharedFileXmlFile.class);

    private final static char[] invalidChars = {'/', '\\', '?', '*', '<', '>', '\"', ':', '|'};

    // following fields must be unique in the FILELIST
    private String sha = null;  // SHA of the file
	private Long size = 0L; // Filesize
    private String key = null; // Name of this key

    // following fields can be different in a FrostSharedFileObject
    private String lastUploaded = null; // Last uploaded by sender
    private String filename = new String();

    private String comment = null;
    private String keywords = null;
    private int rating = 0;

    public SharedFileXmlFile() {
    }

    /**
     * Ensure that all fields are valid.
     */
    public void ensureValidity() {
        if( filename == null || filename.length() == 0 ) {
            filename = "filename";
        } else {
            // replace invalid characters
            for( final char element : invalidChars ) {
                filename = filename.replace(element, '_');
            }
        }
    }

    /**Tests if the filename is valid*/
    private boolean checkFilename() {
        if( filename == null || filename.length() == 0 ) {
            return false;
        }
        // rather than marking a file invalid if invalid chars are in the name we replace this chars
        ensureValidity();

        return true;
    }

    /**Tests if key is valid*/
    private boolean checkKey() {
        if (key == null) {
            return true;
        }
        if( key.startsWith("CHK@") ) {
            return true;
        }
        return false;
    }

    /**Returns true if key is valid*/
    public boolean isSharedFileValid() {
        if( size != null && sha != null && checkFilename() && checkKey() ) {
            return true;
        }
        return false;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }
    public String getFilename() {
        return filename.trim();
    }

    public String getSha() {
        return sha;
    }
    public void setSha(final String s) {
        sha = s;
    }

    /** Set key */
    public void setKey(final String key) {
        this.key = key;
    }

    /** Get key */
    public String getKey() {
        if( key == null ) {
            return key;
        }
        return key.trim();
    }

    /** Set date */
    public void setLastUploaded(String date) {
        if( date != null ) {
            date = date.trim();
        }
        lastUploaded = date;
    }

    /** Get date */
    public String getLastUploaded() {
        return lastUploaded;
    }

    /** Set size */
    public void setSize(final String size) {
        try {
			this.size = Long.parseLong(size);
        } catch (final NumberFormatException ex) {
            this.size = null;
        }
    }

    /** Set size */
    public void setSize(final long size) {
		this.size = size;
    }

    /** Get size */
    public Long getSize() {
        return size;
    }

    public Element getXMLElement(final Document doc) {

        // we do not add keys who are not signed by people we marked as GOOD!
        // but we add unsigned keys for now; this will probably change soon

        final Element fileelement = doc.createElement("File");

        Element element = doc.createElement("name");
        CDATASection cdata = doc.createCDATASection(getFilename());
        element.appendChild(cdata);
        fileelement.appendChild(element);

        element = doc.createElement("sha");
        cdata = doc.createCDATASection(getSha());
        element.appendChild(cdata);
        fileelement.appendChild(element);

        element = doc.createElement("size");
        Text textnode = doc.createTextNode("" + getSize());
        element.appendChild(textnode);
        fileelement.appendChild(element);

        if( getKey() != null && getKey().trim().length() > 0 ) {
            element = doc.createElement("key");
            textnode = doc.createTextNode(getKey());
            element.appendChild(textnode);
            fileelement.appendChild(element);
        }
        if( getLastUploaded() != null && getLastUploaded().trim().length() > 0 ) {
            element = doc.createElement("uploaded");
            textnode = doc.createTextNode(getLastUploaded());
            element.appendChild(textnode);
            fileelement.appendChild(element);
        }
        if( getComment() != null && getComment().trim().length() > 0 ) {
            element = doc.createElement("comment");
            cdata = doc.createCDATASection(getComment());
            element.appendChild(cdata);
            fileelement.appendChild(element);
        }
        if( getKeywords() != null && getKeywords().trim().length() > 0 ) {
            element = doc.createElement("keywords");
            cdata = doc.createCDATASection(getKeywords());
            element.appendChild(cdata);
            fileelement.appendChild(element);
        }
        if( getRating() != 0 ) {
            element = doc.createElement("rating");
            textnode = doc.createTextNode(Integer.toString(getRating()));
            element.appendChild(textnode);
            fileelement.appendChild(element);
        }
        return fileelement;
    }

    public void loadXMLElement(final Element current) throws SAXException {
        setFilename(XMLTools.getChildElementsCDATAValue(current, "name"));
        setSha(XMLTools.getChildElementsCDATAValue(current, "sha"));
        setKey(XMLTools.getChildElementsTextValue(current, "key"));
        setLastUploaded(XMLTools.getChildElementsTextValue(current, "uploaded"));
        setSize(XMLTools.getChildElementsTextValue(current, "size"));
        setComment(XMLTools.getChildElementsCDATAValue(current, "comment"));
        setKeywords(XMLTools.getChildElementsCDATAValue(current, "keywords"));
        final String rat = XMLTools.getChildElementsTextValue(current, "rating");
        if( rat != null ) {
            setRating( Integer.valueOf(rat).intValue() );
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        final SharedFileXmlFile other = (SharedFileXmlFile) obj;
        return sha.equals(other.getSha());
    }

    /**
     * factory method
     * @param e the element
     * @return the sharedFileObject created according to the element.
     */
    public static SharedFileXmlFile getInstance(final Element e){
        try {
            final SharedFileXmlFile result = new SharedFileXmlFile();
            result.loadXMLElement(e);
            return result;
        } catch(final SAXException ex) {
            logger.error("parsing file failed.", ex);
            return null;
        }
    }

    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        if( comment != null ) {
            comment = comment.trim();
            if( comment.length() == 0 ) {
                comment = null;
            } else {
                if( comment.length() > MAX_COMMENT_LENGTH ) {
                    comment = comment.substring(0, MAX_COMMENT_LENGTH);
                }
            }
        }
        this.comment = comment;
    }

    public String getKeywords() {
        return keywords;
    }
    public void setKeywords(String keywords) {
        if( keywords != null ) {
            keywords = keywords.trim();
            if( keywords.length() == 0 ) {
                keywords = null;
            } else {
                if( keywords.length() > MAX_KEYWORDS_LENGTH ) {
                    keywords = keywords.substring(0, MAX_KEYWORDS_LENGTH);
                }
            }
        }
        this.keywords = keywords;
    }

    public int getRating() {
        return rating;
    }
    public void setRating(final int rating) {
        this.rating = rating;
    }
}
