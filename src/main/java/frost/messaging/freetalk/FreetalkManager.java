/*
  FreetalkManager.java / Frost
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.fcp.FcpHandler;
import frost.fcp.NodeAddress;
import frost.fcp.fcp07.freetalk.FcpFreetalkConnection;
import frost.messaging.freetalk.identities.FreetalkOwnIdentity;

public class FreetalkManager {

	private static final Logger logger =  LoggerFactory.getLogger(FreetalkManager.class);

    private FcpFreetalkConnection fcpFreetalkConnection = null;

    private static FreetalkManager instance = null;

    private final List<FreetalkOwnIdentity> ownIdentityList = new ArrayList<FreetalkOwnIdentity>();

    private FreetalkManager() {
        try {
            if (FcpHandler.inst().getFreenetNode() == null) {
                throw new Exception("No freenet nodes defined");
            }
            final NodeAddress na = FcpHandler.inst().getFreenetNode();
            fcpFreetalkConnection = new FcpFreetalkConnection(na);
        } catch(final Exception ex) {
            fcpFreetalkConnection = null;
        }
    }

    public static FreetalkManager getInstance() {
        return instance;
    }

    public String getLoginUserId() {
        final String uid = Core.frostSettings.getValue(Settings.FREETALK_LOGIN_USERID);
        if (uid == null) {
            return "";
        }
        return uid;
    }

    public synchronized static void initialize() {
        if (instance == null) {
            instance = new FreetalkManager();
        }
    }

    /**
     * Connection is null when Freetalk plugin is not Talkable.
     */
    public FcpFreetalkConnection getConnection() {
        return fcpFreetalkConnection;
    }

    public List<FreetalkOwnIdentity> getOwnIdentities() {
        return ownIdentityList;
    }

    public boolean isOwnIdentity(final String freetalkAddress) {
        for (final FreetalkOwnIdentity oid : ownIdentityList) {
            if (oid.getFreetalkAddress().equals(freetalkAddress)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds new identities when not already in list.
     * Removes identites that are no longer in list.
     */
    public void applyOwnIdentities(final List<FreetalkOwnIdentity> newOwnIds) {

        final HashMap<String, FreetalkOwnIdentity> oldIdMap = new HashMap<String, FreetalkOwnIdentity>();
        final HashMap<String, FreetalkOwnIdentity> newIdMap = new HashMap<String, FreetalkOwnIdentity>();

        for (final FreetalkOwnIdentity oid : ownIdentityList) {
            oldIdMap.put(oid.getUid(), oid);
        }
        for (final FreetalkOwnIdentity oid : newOwnIds) {
            newIdMap.put(oid.getUid(), oid);
        }

        // add new ids to list
        for (final FreetalkOwnIdentity oid : newOwnIds) {
            if (!oldIdMap.containsKey(oid.getUid())) {
                // add new id to list
                ownIdentityList.add(oid);
            }
        }

        // find and remove non-existing ids
        final Iterator<FreetalkOwnIdentity> i = ownIdentityList.iterator();
        while (i.hasNext()) {
            final FreetalkOwnIdentity oid = i.next();
            if (!newIdMap.containsKey(oid.getUid())) {
                // remove from list
                i.remove();
            }
        }

        logger.debug("~~~~~ OwnIdentity count = {}", ownIdentityList.size());
    }
}
