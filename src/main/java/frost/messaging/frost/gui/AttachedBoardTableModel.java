/*
  AttachedBoardTableModel.java / Frost
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
package frost.messaging.frost.gui;

import java.util.Iterator;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import frost.messaging.frost.BoardAttachment;
import frost.messaging.frost.boards.Board;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

public class AttachedBoardTableModel extends DefaultTableModel implements LanguageListener {

	private static final long serialVersionUID = 1L;

	private Language language = null;

    protected final static String columnNames[] = new String[3];

    protected final static Class<?> columnClasses[] = {
        String.class, //"Board Name",
        String.class, //"Access rights"
        String.class, //"Description"
    };

    public AttachedBoardTableModel() {
        super();
        language = Language.getInstance();
        language.addLanguageListener(this);
        refreshLanguage();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#isCellEditable(int, int)
     */
    @Override
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

    /* (non-Javadoc)
     * @see frost.gui.translation.LanguageListener#languageChanged(frost.gui.translation.LanguageEvent)
     */
    public void languageChanged(LanguageEvent event) {
        refreshLanguage();
    }

    private void refreshLanguage() {
        columnNames[0] = language.getString("MessagePane.boardAttachmentTable.boardName");
        columnNames[1] = language.getString("MessagePane.boardAttachmentTable.accessRights");
        columnNames[2] = language.getString("MessagePane.boardAttachmentTable.description");

        fireTableStructureChanged();
    }

    /**
     * This method fills the table model with the BoardAttachments
     * in the list passed as a parameter
     * @param boardAttachments list of BoardAttachments fo fill the model with
     */
    public void setData(List<BoardAttachment> boardAttachments) {
        setRowCount(0);
        Iterator<BoardAttachment> boards = boardAttachments.iterator();
        while (boards.hasNext()) {
            Board board = boards.next().getBoardObj();
            Object[] row = new Object[3];
            // There is no point in showing a board without name
            if (board.getName() != null) {
                row[0] = board.getName();
                if (board.getPublicKey() == null && board.getPrivateKey() == null) {
                    row[1] = "public";
                } else if (board.getPublicKey() != null && board.getPrivateKey() == null) {
                    row[1] = "read - only";
                } else {
                    row[1] = "read / write";
                }
                if (board.getDescription() == null) {
                    row[2] = "Not present";
                } else {
                    row[2] = board.getDescription();
                }
                addRow(row);
            }
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnName(int)
     */
    @Override
    public String getColumnName(int column)
    {
        if( column >= 0 && column < columnNames.length )
            return columnNames[column];
        return null;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    @Override
    public int getColumnCount()
    {
        return columnNames.length;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnClass(int)
     */
    @Override
    public Class<?> getColumnClass(int column) {
        if( column >= 0 && column < columnClasses.length )
            return columnClasses[column];
        return null;
    }
}
