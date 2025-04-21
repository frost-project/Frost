/*
  KnownBoard.java / Frost
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
package frost.gui;

import frost.messaging.frost.boards.Board;

public class KnownBoard extends Board {

	private static final long serialVersionUID = 1L;

	private boolean isHidden = false;

    public KnownBoard(String name, String pubKey, String privKey, String description) {
        super(name, pubKey, privKey, description);
    }
    
    public KnownBoard(Board newb) {
        this(newb.getName(), newb.getPublicKey(), newb.getPrivateKey(), newb.getDescription());
    }
    
    public void setHidden(boolean h) {
        isHidden = h;
    }
    
    public boolean isHidden() {
        return isHidden;
    }
}
