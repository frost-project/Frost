/*
  SearchMessagesTableModel.java / Frost
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
package frost.gui.model;

import frost.messaging.frost.FrostSearchResultMessageObject;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

public class SearchMessagesTableModel extends SortedTableModel<FrostSearchResultMessageObject>
		implements LanguageListener {

	private static final long serialVersionUID = 1L;

	private Language language = null;

    protected final String columnNames[] = new String[8];

    protected final Class<?> columnClasses[] = {
        Boolean.class, // flagged
        Boolean.class, // starred
        String.class, //LangRes.getString("Index"),
        String.class, //LangRes.getString("From"),
        String.class, //LangRes.getString("Board"),
        String.class, //LangRes.getString("Subject"),
        String.class, //"Sig",
        String.class //LangRes.getString("Date")
    };

    public SearchMessagesTableModel() {
        super();
        language = Language.getInstance();
        refreshLanguage();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public String getColumnName(int column) {
        if( column >= 0 && column < columnNames.length )
            return columnNames[column];
        return null;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Class<?> getColumnClass(int column) {
        if( column >= 0 && column < columnClasses.length )
            return columnClasses[column];
        return null;
    }

    public void languageChanged(LanguageEvent event) {
        refreshLanguage();
    }

    private void refreshLanguage() {
        columnNames[0] = "";
        columnNames[1] = "";
        columnNames[2] = language.getString("SearchMessages.resultTable.index");
        columnNames[3] = language.getString("SearchMessages.resultTable.from");
        columnNames[4] = language.getString("SearchMessages.resultTable.board");
        columnNames[5] = language.getString("SearchMessages.resultTable.subject");
        columnNames[6] = language.getString("SearchMessages.resultTable.sig");
        columnNames[7] = language.getString("SearchMessages.resultTable.date");

        fireTableStructureChanged();
    }
}
