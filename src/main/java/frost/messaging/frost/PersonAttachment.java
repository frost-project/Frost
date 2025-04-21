/*
 PersonAttachment.java / Frost
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import frost.identities.Identity;
import frost.util.XMLTools;

public class PersonAttachment extends Attachment {

	private static final long serialVersionUID = 1L;

	private Identity identity;

	/**
	 * @param e
	 * @throws SAXException
	 */
	public PersonAttachment(final Element e) throws SAXException {
		loadXMLElement(e);
	}

	/**
	 * @param newIdentity
	 */
	public PersonAttachment(final Identity newIdentity) {
		identity = newIdentity;
	}

	/*
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Attachment attachment) {
		return toString().compareTo(attachment.toString());
	}


	/**
	 * @return
	 */
	public Identity getIdentity() {
		return identity;
	}

	/* (non-Javadoc)
	 * @see frost.messages.Attachment#getType()
	 */
	@Override
    public int getType() {
		return Attachment.PERSON;
	}

	/* (non-Javadoc)
	 * @see frost.XMLizable#getXMLElement(org.w3c.dom.Document)
	 */
	public Element getXMLElement(final Document container) {
		final Element el = container.createElement("Attachment");
		el.setAttribute("type", "person");
		el.appendChild(identity.getXMLElement(container));
		return el;
	}

	/* (non-Javadoc)
	 * @see frost.XMLizable#loadXMLElement(org.w3c.dom.Element)
	 */
	public void loadXMLElement(final Element e) throws SAXException {
		final Element _person =
			XMLTools.getChildElementsByTagName(e, "Identity").iterator().next();
		identity = Identity.createIdentityFromXmlElement(_person);
	}
}
