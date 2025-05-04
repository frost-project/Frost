/*
  TableBackgroundColors.java / Frost
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
package frost.fileTransfer.common;

import java.awt.Color;

import javax.swing.JTable;

public class TableBackgroundColors {

    private static Color firstBackgroundColor = Color.white;
    private static Color secondBackgroundColor = new Color(0xEE,0xEE,0xEE);

    private static Color greenColor = new Color(0x99,0xFF,0x99);
    private static Color lightGreenColor = new Color(0xCC,0xFF,0xCC);

    private static Color redColor = new Color(0xFF,0x99,0x99);
    private static Color lightRedColor = new Color(0xFF,0xCC,0xCC);

    public static Color getBackgroundColor(final JTable table, final int row, final boolean showColoredLines) {
        if( showColoredLines ) {
            if ((row & 0x0001) == 0) {
                return firstBackgroundColor;
            } else {
                return secondBackgroundColor;
            }
        } else {
            return table.getBackground();
        }
    }

    public static Color getBackgroundColorDone(final JTable table, final int row, final boolean showColoredLines) {
        if( showColoredLines ) {
            if ((row & 0x0001) == 0) {
                return lightGreenColor;
            } else {
                return greenColor;
            }
        } else {
            return greenColor;
        }
    }

    public static Color getBackgroundColorFailed(final JTable table, final int row, final boolean showColoredLines) {
        if( showColoredLines ) {
            if ((row & 0x0001) == 0) {
                return lightRedColor;
            } else {
                return redColor;
            }
        } else {
            return redColor;
        }
    }
}
