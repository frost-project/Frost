/*
  BoardsChooser.java / Frost
  Copyright (C) 2006  Frost Project <jtcfrost.sourceforge.net>
  This file is contributed by Stefan Majewski <feuerblume@users.sourceforge.net>

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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import frost.messaging.frost.boards.Board;
import frost.util.gui.translation.Language;

public class BoardsChooser extends JDialog {

	private static final long serialVersionUID = 1L;

	private final Language language = Language.getInstance();

    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    JTable boardsTable;
    BoardsTableModel boardsTableModel;

    JButton Bcancel;
    List<BoardTableEntry> boardList;
    JButton Bok;
    boolean okPressed = false;

    public BoardsChooser(final Frame parent, final List<Board> boards, final List<Board> preselectedBoards) {
        super(parent);
        setModal(true);
        setTitle(language.getString("BoardsChooser.title"));

        // fill given board into our list as BoardListEntries
        boardList = new ArrayList<BoardTableEntry>();
        for( final Board b : boards ) {
            final BoardTableEntry e = new BoardTableEntry();
            e.board = b;
            e.isSelected = Boolean.FALSE;

            if( preselectedBoards != null ) {
                // check if this board should be selected
                for( final Board sb : preselectedBoards ) {
                    if( b.getName().equals(sb.getName()) ) {
                        e.isSelected = Boolean.TRUE;
                    }
                }
            }
            boardList.add(e);
        }

        initGui();

        setLocationRelativeTo(parent);
    }

    public BoardsChooser(final Frame parent, final List<Board> boards) {
        this(parent, boards, null);
    }

    private void initGui() {
        Bok = new JButton(language.getString("Common.ok"));
        Bok.addActionListener( new ActionListener() {
               public void actionPerformed(final ActionEvent e) {
                    okPressed = true;
                    setVisible(false);
               } });
        Bcancel = new JButton(language.getString("Common.cancel"));
        Bcancel.addActionListener( new ActionListener() {
               public void actionPerformed(final ActionEvent e) {
                    okPressed = false;
                    setVisible(false);
               } });
        final JPanel buttonsPanel = new JPanel( new FlowLayout(FlowLayout.RIGHT, 8, 8) );
        buttonsPanel.add( Bok );
        buttonsPanel.add( Bcancel );

        boardsTableModel = new BoardsTableModel(boardList);
        boardsTable = new JTable(boardsTableModel);
        boardsTable.setShowGrid(false);
        boardsTable.setTableHeader(null);

        boardsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        boardsTable.setRowSelectionAllowed(true);

        boardsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                // on double click toggle the board selection state (checkbox)
                if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    final int row = boardsTable.getSelectedRow();
                    if (row < 0) {
                        return;
                    }
                    final BoardTableEntry en = boardList.get(row);
                    if( en == null ) {
                        return;
                    }
                    en.isSelected = Boolean.valueOf(!en.isSelected.booleanValue());
                    boardsTableModel.fireTableCellUpdated(row, 0);
                }
            }
        });

        final TableColumn column = boardsTable.getColumnModel().getColumn(0);
        column.setPreferredWidth(30);
        column.setMaxWidth(30);

        final JScrollPane listScroller = new JScrollPane();
        listScroller.setBorder( new CompoundBorder( new EmptyBorder(5,5,5,5),
                                                    new CompoundBorder( new EtchedBorder(),
                                                                        new EmptyBorder(5,5,5,5) )
                                                  ) );
        listScroller.setWheelScrollingEnabled(true);
        listScroller.setViewportView(boardsTable);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(listScroller, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
        setSize(300, 400);
    }

    public List<Board> runDialog()
    {
        setVisible(true);
        if( okPressed == false ) {
            return null;
        }

        final ArrayList<Board> chosed = new ArrayList<Board>();
        for( final BoardTableEntry boardTableEntry : boardList ) {
            if( boardTableEntry.isSelected.booleanValue() ) {
                chosed.add(boardTableEntry.board);
            }
        }
        return chosed;
    }

    class BoardTableEntry {
        public Boolean isSelected;
        public Board board;
    }

	private class BoardsTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private List<BoardTableEntry> boardsList = new ArrayList<BoardTableEntry>();

        public BoardsTableModel(final List<BoardTableEntry> l) {
            super();
            boardsList = l;
        }
        @Override
        public String getColumnName(final int col) {
            return "";
        }
        @Override
        public Class<?> getColumnClass(final int c) {
            return getValueAt(0, c).getClass();
        }
        public int getRowCount() {
            if( boardsList == null ) {
                return 0;
            }
            return boardsList.size();
        }
        public int getColumnCount() {
            return 2;
        }
        public Object getValueAt(final int row, final int col) {
            final BoardTableEntry e = boardsList.get(row);
            if( col==0 ) {
                return e.isSelected;
            } else {
                return e.board.getName();
            }
        }
        @Override
        public boolean isCellEditable(final int row, final int col) {
            if( col == 0 ) {
                return true;
            }
            return false;
        }
        @Override
        public void setValueAt(final Object value, final int row, final int col) {
            final BoardTableEntry e = boardsList.get(row);
            e.isSelected = (Boolean)value;
            fireTableCellUpdated(row, col);
        }
    }
}
