/*
 FileAttachment.java / Frost
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

import java.io.File;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import frost.util.CopyToClipboardItem;
import frost.util.XMLTools;

public class FileAttachment extends Attachment implements CopyToClipboardItem {

	private static final long serialVersionUID = 1L;

	private File file = null;

    private String key = null; // Name of this key
    private long size = 0; // Filesize
    private String filename = new String();

	/* (non-Javadoc)
	 * @see frost.messages.Attachment#getType()
	 */
	@Override
    public int getType() {
		return Attachment.FILE;
	}

	/* (non-Javadoc)
	 * @see frost.XMLizable#getXMLElement(org.w3c.dom.Document)
	 */
	public Element getXMLElement(final Document doc) {

        final Element fileelement = doc.createElement("File");

        Element element = doc.createElement("name");
        final CDATASection cdata = doc.createCDATASection(getFileName());
        element.appendChild(cdata);
        fileelement.appendChild(element);

        element = doc.createElement("size");
        Text textnode = doc.createTextNode("" + getFileSize());
        element.appendChild(textnode);
        fileelement.appendChild(element);

        element = doc.createElement("key");
        textnode = doc.createTextNode(getKey());
        element.appendChild(textnode);
        fileelement.appendChild(element);

        element = doc.createElement("Attachment");
        element.setAttribute("type", "file");
        element.appendChild(fileelement);

		return element;
	}

	/* (non-Javadoc)
	 * @see frost.XMLizable#loadXMLElement(org.w3c.dom.Element)
	 */
	public void loadXMLElement(final Element e) throws SAXException {
		final Element _file = XMLTools.getChildElementsByTagName(e, "File").iterator().next();

        filename = XMLTools.getChildElementsCDATAValue(_file, "name");
        key = XMLTools.getChildElementsTextValue(_file, "key");
		size = Long.parseLong(XMLTools.getChildElementsTextValue(_file, "size"));
	}

	/**
	 * @param e
	 * @throws SAXException
	 */
	public FileAttachment(final Element e) throws SAXException {
		loadXMLElement(e);
	}

	public FileAttachment(final String fname, final String k, final long s) {
        filename = fname;
        size = s;
        key = k;
	}

	/**
	 * Called for an unsend message, initializes internal file object.
	 */
    public FileAttachment(final File newFile, final String k, final long s) {
        file = newFile;
        filename = file.getName();
        size = s;
        key = k;
    }

    public FileAttachment(final File f) {
        file = f;
        filename = file.getName();
        size = file.length();
    }

    /*
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
    public int compareTo(Attachment attachment) {
		return toString().compareTo(attachment.toString());
	}

    public String getFileName() {
        return filename;
    }
    public String getKey() {
        return key;
    }
    public void setKey(final String k) {
        key = k;
    }
    public long getFileSize() {
        return size;
    }
    public File getInternalFile() {
        return file;
    }
}
