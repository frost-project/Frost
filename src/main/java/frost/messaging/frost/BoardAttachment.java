/*
 BoardAttachment.java / Frost
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

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import frost.messaging.frost.boards.Board;
import frost.util.XMLTools;

public class BoardAttachment extends Attachment {

	private static final long serialVersionUID = 1L;

	private Board boardObj;

	@Override
    public int getType() {
		return Attachment.BOARD;
	}

	public Element getXMLElement(Document container) {

        Element el = container.createElement("Attachment");
        el.setAttribute("type", "board");

        CDATASection cdata;

        Element name = container.createElement("Name");
        cdata = container.createCDATASection(boardObj.getName());
        name.appendChild(cdata);
        el.appendChild(name);

        if( boardObj.getPublicKey() != null ) {
            Element pubkey = container.createElement("pubKey");
            cdata = container.createCDATASection(boardObj.getPublicKey());
            pubkey.appendChild(cdata);
            el.appendChild(pubkey);
        }

        if( boardObj.getPrivateKey() != null ) {
    		Element privkey = container.createElement("privKey");
    		cdata = container.createCDATASection(boardObj.getPrivateKey()); //null is ok
    		privkey.appendChild(cdata);
    		el.appendChild(privkey);
        }
		
        if( boardObj.getDescription() != null ) {
    		Element description = container.createElement("description");
    		cdata = container.createCDATASection(boardObj.getDescription());	//null is ok
    		description.appendChild(cdata);
    		el.appendChild(description);
        }

		return el;
	}

	public void loadXMLElement(Element e) throws SAXException {
		String name, privkey, pubkey, description;
		name = XMLTools.getChildElementsCDATAValue(e, "Name");
		privkey = XMLTools.getChildElementsCDATAValue(e, "privKey");
		pubkey = XMLTools.getChildElementsCDATAValue(e, "pubKey");
		description = XMLTools.getChildElementsCDATAValue(e, "description");

		boardObj = new Board(name, pubkey, privkey, description);
	}

	protected BoardAttachment(Element e) throws SAXException {
		loadXMLElement(e);
	}

	public BoardAttachment(Board obj) {
		boardObj = obj;
	}

	/**
	 * @return a FrostBoardObject
	 */
	public Board getBoardObj() {
		return boardObj;
	}

	/**
	 * A toString to enable the SortedSet to sort by boardname
	 */
	@Override
    public String toString() {
		if (getBoardObj() != null) {
			return getBoardObj().getName();
		}
		return "*ERR*";
	}

	public int compareTo(Attachment attachment) {
		return toString().compareTo(attachment.toString());
	}
}
