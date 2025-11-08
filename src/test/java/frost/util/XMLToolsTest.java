/*
  XMLToolsTest.java / Frost
  Copyright (C) 2023  Frost Project <jtcfrost.sourceforge.net>

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
package frost.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import frost.junit.InitExtension;

@ExtendWith(InitExtension.class)
public class XMLToolsTest {

	@Test
	public void createAndValidateXML() {
		String messageID = "<![CDATA[\\\\</MessageId>]]> <helpme />";
		String xmlFilename = Paths.get(System.getProperty("java.io.tmpdir"), "AAAAA.xml", "").toString();

		Document d = XMLTools.createDomDocument();
		Element el = d.createElement("FrostMessage");

		Element current = d.createElement("MessageId");
		CDATASection cdata = d.createCDATASection(messageID);

		current.appendChild(cdata);
		el.appendChild(current);
		d.appendChild(el);

		assertTrue(XMLTools.writeXmlFile(d, xmlFilename));

		Document dd = XMLTools.parseXmlFile(xmlFilename);
		Element root = dd.getDocumentElement();
		assertEquals(messageID, XMLTools.getChildElementsCDATAValue(root, "MessageId"));
	}
}
