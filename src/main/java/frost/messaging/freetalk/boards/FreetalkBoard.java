/*
  FreetalkBoard.java / Frost
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
package frost.messaging.freetalk.boards;

public class FreetalkBoard extends AbstractFreetalkNode {

	private static final long serialVersionUID = 1L;

	private int messageCount;
    private long firstSeenDate;
    private long latestMessageDate;

    public FreetalkBoard(final String name) {
        super(name);
        this.messageCount = 0;
        this.firstSeenDate = 0L;
        this.latestMessageDate = 0L;
    }

    public FreetalkBoard(final String name, final int messageCount, final long firstSeenDate, final long lastMessageDate) {
        super(name);
        this.messageCount = messageCount;
        this.firstSeenDate = firstSeenDate;
        this.latestMessageDate = lastMessageDate;
    }

    public int getMessageCount() {
        return messageCount;
    }
    public void setMessageCount(final int messageCount) {
        this.messageCount = messageCount;
    }

    public long getFirstSeenDate() {
        return firstSeenDate;
    }
    public void setFirstSeenDate(final long firstSeenDate) {
        this.firstSeenDate = firstSeenDate;
    }

    public long getLastMessageDate() {
        return latestMessageDate;
    }
    public void setLastMessageDate(final long lastMessageDate) {
        this.latestMessageDate = lastMessageDate;
    }

    @Override
    public boolean isBoard() {
        return true;
    }
}
