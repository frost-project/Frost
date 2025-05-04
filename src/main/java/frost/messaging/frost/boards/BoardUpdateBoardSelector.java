/*
  BoardUpdateBoardSelector.java / Frost
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
package frost.messaging.frost.boards;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import frost.Core;
import frost.Settings;
import frost.messaging.frost.UnsentMessagesManager;

/**
 * Selects the next board eligible for updating.
 * Prefers boards with sendable messages.
 */
public class BoardUpdateBoardSelector {

    // TODO: update boards with most msgs in last X hours more often
    //  -> TOF calls Board.newMessageReceived. somehow count how many message were received during last X updates (?)
    //  -> OR check db table how many messages we received in last 24h?

    // -> HOLD item for later release, maybe its not longer critical because we do not request the full backload each time now

    private static Board lastSelectedBoard = null;

    /**
     * Used to sort FrostBoardObjects by lastUpdateStartMillis ascending.
     */
    private static final Comparator<Board> lastUpdateStartMillisCmp = new Comparator<Board>() {
        public int compare(Board value1, Board value2) {
            if (value1.getLastUpdateStartMillis() > value2.getLastUpdateStartMillis()) {
                return 1;
            } else if (value1.getLastUpdateStartMillis() < value2.getLastUpdateStartMillis()) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    /**
     * Chooses the next FrostBoard to update (automatic update).
     * First sorts by lastUpdateStarted time, then chooses first board
     * that is allowed to update.
     * Used only for automatic updating.
     * Returns NULL if no board to update is found.
     */
    public static Board selectNextBoard(final TofTreeModel tofTreeModel) {

        // prefer boards that have waiting sendable messages
        final List<Board> boardsWithSendableMsgs = UnsentMessagesManager.getBoardsWithSendableMessages();
        if( !boardsWithSendableMsgs.isEmpty() ) {
            Collections.sort(boardsWithSendableMsgs, lastUpdateStartMillisCmp);
            for( final Board board : boardsWithSendableMsgs ) {
                // choose first board that is not the last updated board
                // we compare pointers here
                if( board != lastSelectedBoard ) {
                    lastSelectedBoard = board;
                    return board;
                }
            }
        }

        final List<Board> allBoards = tofTreeModel.getAllBoards();
        if (allBoards.size() == 0) {
            lastSelectedBoard = null;
            return null;
        }

        Collections.sort(allBoards, lastUpdateStartMillisCmp);
        // now first board in list should be the one with latest update of all


        final long curTime = System.currentTimeMillis();
        // get in minutes
        final int minUpdateInterval = Core.frostSettings.getIntValue(Settings.BOARD_AUTOUPDATE_MIN_INTERVAL);
        // min -> ms
        final long minUpdateIntervalMillis = minUpdateInterval * 60L * 1000L;

        Board nextBoard = null;
        for( final Board lBoard : allBoards ) {
            if (nextBoard == null
                && lBoard.isAutomaticUpdateAllowed()
                && (curTime - minUpdateIntervalMillis) > lBoard.getLastUpdateStartMillis() // minInterval
                && ( (lBoard.isConfigured() && lBoard.getAutoUpdateEnabled())
                      || !lBoard.isConfigured()) )
            {
                nextBoard = lBoard;
                break;
            }
        }

        lastSelectedBoard = nextBoard;

        return nextBoard;
    }
}
