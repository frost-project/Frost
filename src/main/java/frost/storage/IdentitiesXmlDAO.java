/*
  IdentitiesXmlDAO.java / Frost
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
package frost.storage;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import frost.identities.Identity;
import frost.util.Mixed;
import frost.util.XMLTools;
import frost.util.XMLizable;

public class IdentitiesXmlDAO {

	private static final Logger logger = LoggerFactory.getLogger(IdentitiesXmlDAO.class);

    /**
     * Loads good, observe and bad identities from xml file.
     */
    public static List<Identity> loadIdentities(final File file) {

        final LinkedList<Identity> identities = new LinkedList<Identity>();

        final Document d = XMLTools.parseXmlFile(file);
        final Element rootEl = d.getDocumentElement();

        final List<Element> lists = XMLTools.getChildElementsByTagName(rootEl, "BuddyList");
        final Iterator<Element> it = lists.iterator();

        while (it.hasNext()) {
            final Element current = it.next();
            if (current.getAttribute("type").equals("friends")) {
                final BuddyList buddyList = new BuddyList();
                try {
                    buddyList.loadXMLElement(current);
                } catch (final SAXException e) {
                    logger.error("Error loading good identities", e);
                }
                for( final Object element : buddyList.getAllValues() ) {
                    final Identity id = (Identity)element;
                    id.setGOODWithoutUpdate();
                    identities.add(id);
                }
            } else if (current.getAttribute("type").equals("enemies")) {
                final BuddyList buddyList = new BuddyList();
                try {
                    buddyList.loadXMLElement(current);
                } catch (final SAXException e) {
                    logger.error("Error loading bad identities", e);
                }
                for( final Object element : buddyList.getAllValues() ) {
                    final Identity id = (Identity)element;
                    id.setBADWithoutUpdate();
                    identities.add(id);
                }
            } else if (current.getAttribute("type").equals("neutral")) {
                final BuddyList buddyList = new BuddyList();
                try {
                    buddyList.loadXMLElement(current);
                } catch (final SAXException e) {
                    logger.error("Error loading check identities", e);
                }
                for( final Object element : buddyList.getAllValues() ) {
                    final Identity id = (Identity)element;
                    id.setCHECKWithoutUpdate();
                    identities.add(id);
                }
            } else if (current.getAttribute("type").equals("observed")) {
                final BuddyList buddyList = new BuddyList();
                try {
                    buddyList.loadXMLElement(current);
                } catch (final SAXException e) {
                    logger.error("Error loading observe identities", e);
                }
                for( final Object element : buddyList.getAllValues() ) {
                    final Identity id = (Identity)element;
                    id.setOBSERVEWithoutUpdate();
                    identities.add(id);
                }
            }
        }

        return identities;
    }

    /**
     * Returns -1 on error, 0 if no identity is to export and no file was created,
     * or >0 for exported identity count.
     */
    public static int saveIdentities(final File file, final List<Identity> identities) {

        final BuddyList friends = new BuddyList();
        final BuddyList observed = new BuddyList();
        final BuddyList enemies = new BuddyList();

        int count = 0;

        for( final Object element : identities ) {
            final Identity id = (Identity) element;
            if( id.isGOOD() ) {
                friends.add(id);
                count++;
            } else if( id.isOBSERVE() ) {
                observed.add(id);
                count++;
            } else if( id.isBAD() ) {
                enemies.add(id);
                count++;
            }
            // we ignore CHECK ids here! this is the default
        }

        if( count == 0 ) {
            // dont create an empty file
            return count;
        }

        final Document d = XMLTools.createDomDocument();
        final Element rootElement = d.createElement("FrostIdentities");

        // then friends
        final Element friendsElement = friends.getXMLElement(d);
        friendsElement.setAttribute("type", "friends");
        rootElement.appendChild(friendsElement);
        // then enemies
        final Element enemiesElement = enemies.getXMLElement(d);
        enemiesElement.setAttribute("type", "enemies");
        rootElement.appendChild(enemiesElement);
        // then observed
        final Element observedElement = observed.getXMLElement(d);
        observedElement.setAttribute("type", "observed");
        rootElement.appendChild(observedElement);

        d.appendChild(rootElement);

        if( XMLTools.writeXmlFile(d, file) ) {
            return count;
        } else {
            return -1;
        }
    }

	private static class BuddyList implements XMLizable {

		private static final long serialVersionUID = 1L;

		private HashMap<String,Identity> hashMap = null;

        /**constructor*/
        public BuddyList() {
            hashMap = new HashMap<String,Identity>(100); //that sounds like a reasonable number
        }

        /**
         * adds a user to the list
         * returns false if the user exists
         */
        public synchronized boolean add(final Identity user) {
            final String str = user.getUniqueName();
            if (containsKey(str)) {
                return false;
            }
            hashMap.put(str, user);

            return true;
        }

        public boolean containsKey(final String key) {
            return hashMap.containsKey(Mixed.makeFilename(key));
        }

        public synchronized Element getXMLElement(final Document doc) {
            final Element main = doc.createElement("BuddyList");
            final Iterator<Identity> it = hashMap.values().iterator();
            while (it.hasNext()) {
                final Identity id = it.next();
                final Element el = id.getExportXMLElement(doc);
                main.appendChild(el);
            }
            return main;
        }

        public void loadXMLElement(final Element el) throws SAXException {
            if (el == null) {
                return;
            }
            for (final Element idEl : XMLTools.getChildElementsByTagName(el, "Identity") ) {
                final Identity id = Identity.createIdentityFromXmlElement( idEl );
                if( id != null ) {
                    add(id);
                }
            }
        }

        public Collection<Identity> getAllValues() {
            return hashMap.values();
        }
    }
}
