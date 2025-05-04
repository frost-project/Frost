/*
  BoardInfoFrame.java / Frost
  Copyright (C) 2001  Frost Project <jtcfrost.sourceforge.net>

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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.time.OffsetDateTime;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import frost.Core;
import frost.MainFrame;
import frost.Settings;
import frost.fileTransfer.common.TableBackgroundColors;
import frost.gui.SortedTable;
import frost.gui.model.SortedTableModel;
import frost.gui.model.TableMember;
import frost.storage.perst.messages.MessageStorage;
import frost.util.ClipboardUtil;
import frost.util.DateFun;
import frost.util.gui.MiscToolkit;
import frost.util.gui.action.BaseAction;
import frost.util.gui.translation.Language;
import frost.util.gui.translation.LanguageEvent;
import frost.util.gui.translation.LanguageListener;

public class BoardInfoFrame extends JFrame implements BoardUpdateThreadListener, LanguageListener {

	private static final long serialVersionUID = 1L;

    private final boolean showColoredLines;

	private class Listener extends MouseAdapter {

		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				updateSelectedBoardAction.actionPerformed(null);
			}
		}
	}

    private MainFrame mainFrame;
	private TofTree tofTree;
    private static boolean isShowing = false; // flag, is true if frame is showing, used by frame1
	private transient Language language;
	private transient Listener listener = new Listener();

    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private final JPanel boardTablePanel = new JPanel(new BorderLayout());

    private final JLabel summaryLabel = new JLabel();

    private final JButton updateButton = new JButton();
    private final JButton updateSelectedBoardButton = new JButton();
    private final JButton updateAllBoardsButton = new JButton();
    private final JButton removeSelectedBoardsButton = new JButton();
    private final JButton Bclose = new JButton();

	private CopyInfoToClipboardAction copyInfoToClipboardAction;
	private UpdateSelectedBoardAction updateSelectedBoardAction;
	private UpdateAllBoardsAction updateAllBoardsAction;
	private RemoveSelectedBoardsAction removeSelectedBoardsAction;
	private UpdateAction updateAction;
	private PopupMenu popupMenu;

    private BoardInfoTableModel boardTableModel = null;
    private SortedTable<BoardInfoTableMember> boardTable = null;

    public BoardInfoFrame(final MainFrame mainFrame, final TofTree tofTree) {
        super();
        this.mainFrame = mainFrame;
        this.tofTree = tofTree;

		copyInfoToClipboardAction = new CopyInfoToClipboardAction();
		updateSelectedBoardAction = new UpdateSelectedBoardAction();
		updateAllBoardsAction = new UpdateAllBoardsAction();
		removeSelectedBoardsAction = new RemoveSelectedBoardsAction();
		updateAction = new UpdateAction();
		popupMenu = new PopupMenu();

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		Init();

        int width = (int) (mainFrame.getWidth() * 0.75);
        int height = (int) (mainFrame.getHeight() * 0.75);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if( width < 1000 ) {
        	if( screenSize.width > 1300 ) {
        		width = 1200;
        	} else if( screenSize.width > 1000 ) {
        		width = (int) (mainFrame.getWidth() * 0.99);
        	}
        }

        if( height < 500 ) {
        	if( screenSize.width > 900 ) {
        		height = 800;
        	} else {
        		height = (int) (screenSize.width * 0.85);
        	}
        }

        setSize(width, height);
        setLocationRelativeTo(mainFrame);

        showColoredLines = Core.frostSettings.getBoolean(Settings.SHOW_COLORED_ROWS);

		language = Language.getInstance();
		languageChanged(null);
	}

	private void Init() {
        boardTableModel = new BoardInfoTableModel();
        boardTable = new SortedTable<BoardInfoTableMember>(boardTableModel);

        //------------------------------------------------------------------------
        // Configure objects
        //------------------------------------------------------------------------

        final ImageIcon frameIcon = MiscToolkit.loadImageIcon("/data/jtc.jpg");
        setIconImage(frameIcon.getImage());
        setSize(new Dimension(350, 200));
        setResizable(true);

        boardTable.setRowSelectionAllowed(true);
        boardTable.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        boardTable.setRowHeight(18); // we use 16x16 icons, keep a gap

        boardTable.setDefaultRenderer( Object.class, new BoardInfoTableCellRenderer(SwingConstants.LEFT) );
        boardTable.setDefaultRenderer( Number.class, new BoardInfoTableCellRenderer(SwingConstants.RIGHT) );

        updateSelectedBoardButton.setEnabled(false);

        //------------------------------------------------------------------------
        // Actionlistener
        //------------------------------------------------------------------------
		boardTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				checkActionsEnabled();
			}
		});

		updateSelectedBoardButton.setAction(updateSelectedBoardAction);
		updateAllBoardsButton.setAction(updateAllBoardsAction);
		removeSelectedBoardsButton.setAction(removeSelectedBoardsAction);
		updateButton.setAction(updateAction);

		// Bclose
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeDialog();
			}
		};
		Bclose.addActionListener(al);

        //------------------------------------------------------------------------
        // Append objects
        //------------------------------------------------------------------------
        this.getContentPane().add(mainPanel, null); // add Main panel

        mainPanel.add(boardTablePanel, BorderLayout.CENTER);
        boardTablePanel.add(updateSelectedBoardButton, BorderLayout.NORTH);
        boardTablePanel.add(new JScrollPane(boardTable), BorderLayout.CENTER);
        final JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.X_AXIS));
        summaryPanel.setBorder(new EmptyBorder(5,0,0,0));
        summaryPanel.add(summaryLabel);
        summaryPanel.add(Box.createRigidArea(new Dimension(15,3))); // ensure minimum glue size
        summaryPanel.add(Box.createHorizontalGlue());
        summaryPanel.add(updateButton);

        boardTablePanel.add(summaryPanel, BorderLayout.SOUTH);
        boardTablePanel.setBorder( new CompoundBorder(
                                                     new EtchedBorder(),
                                                     new EmptyBorder(7,7,7,7)
                                                     ));
        boardTablePanel.setBorder( new CompoundBorder(
                                                     new EmptyBorder(7,7,7,7),
                                                     boardTablePanel.getBorder()
                                                     ));

        final JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5,7,7,7));
        buttonsPanel.add(updateSelectedBoardButton);
        buttonsPanel.add(Box.createRigidArea(new Dimension(15,3)));
        buttonsPanel.add(updateAllBoardsButton);
        buttonsPanel.add(Box.createRigidArea(new Dimension(15,3))); // ensure minimum glue size
        buttonsPanel.add(removeSelectedBoardsButton);
        buttonsPanel.add(Box.createRigidArea(new Dimension(15,3))); // ensure minimum glue size
        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.add(Bclose);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

		boardTable.addMouseListener(listener);
		boardTable.setComponentPopupMenu(popupMenu);

		updateAction.actionPerformed(null);

        // set table column sizes
        final int[] newWidths = { 150,30,20,20,20,20,20,40 };

        for (int i = 0; i < newWidths.length; i++) {
            boardTable.getColumnModel().getColumn(i).setPreferredWidth(newWidths[i]);
        }
    }

	private void checkActionsEnabled() {
		Boolean isEnabled = boardTable.getSelectedRowCount() > 0;
		updateSelectedBoardAction.setEnabled(isEnabled);
		removeSelectedBoardsAction.setEnabled(isEnabled);
		copyInfoToClipboardAction.setEnabled(isEnabled);
	}

    private static UpdateBoardInfoTableThread updateBoardInfoTableThread = null;

    private class UpdateBoardInfoTableThread extends Thread
    {
        @Override
        public void run()
        {
            int messageCount = 0;
            int boardCount = 0;
            final List<Board> boards = ((TofTreeModel) tofTree.getModel()).getAllBoards();

            for( final Board board : boards ) {
                final BoardInfoTableMember newRow = new BoardInfoTableMember(board);
                fillInBoardCounts(board, newRow);

                // count statistics
                messageCount += newRow.getAllMessageCount().intValue();
                boardCount++;

                final BoardInfoTableMember finalRow = newRow;
                final int finalBoardCount = boardCount;
                final int finalMessageCount = messageCount;
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            boardTableModel.addRow(finalRow);
                            summaryLabel.setText(language.getString("BoardInfoFrame.label.boards") +": "+
                                                 finalBoardCount +"    "+
                                                 language.getString("BoardInfoFrame.label.messages") +": "+
                                                 finalMessageCount);
                        }});
            }
            updateBoardInfoTableThread = null;
        }
    }

    /**
     * Gets number of new+all messages and files of a board
     *
     * @param board name of the board
     * @return Integer value
     */
    public BoardInfoTableMember fillInBoardCounts(final Board board, final BoardInfoTableMember row) {

        final int countTodaysMessages  = MessageStorage.inst().getMessageCount(board, 0);
        final int countAllMessages     = MessageStorage.inst().getMessageCount(board, -1);
        final int countFlaggedMessages = MessageStorage.inst().getFlaggedMessageCount(board);
        final int countStarredMessages = MessageStorage.inst().getStarredMessageCount(board);
        final int countUnreadMessages  = MessageStorage.inst().getUnreadMessageCount(board);
		final OffsetDateTime dateTime = MessageStorage.inst().getDateTimeOfLatestMessage(board);
        final String dateStr;
        if (dateTime != null) {
			final OffsetDateTime date = DateFun.toStartOfDay(dateTime);
			dateStr = DateFun.FORMAT_DATE_EXT.format(date);
        } else {
            dateStr = "---";
        }

        row.setAllMessageCount(countAllMessages);
        row.setTodaysMessageCount(countTodaysMessages);
        row.setFlaggedMessageCount(countFlaggedMessages);
        row.setStarredMessageCount(countStarredMessages);
        row.setUnreadMessageCount(countUnreadMessages);
        row.setDateOfLastMsg(dateStr);

        return row;
    }

    public void startDialog() {
        tofTree.getRunningBoardUpdateThreads().addBoardUpdateThreadListener(this);
		language.addLanguageListener(this);
        language.addLanguageListener(boardTableModel);
        setDialogShowing(true);
        setVisible(true);
    }

    protected void closeDialog() {
        tofTree.getRunningBoardUpdateThreads().removeBoardUpdateThreadListener(this);
		language.removeLanguageListener(this);
        language.removeLanguageListener(boardTableModel);
        setDialogShowing(false);
        dispose();
    }

    @Override
    protected void processWindowEvent(final WindowEvent e) {
        if( e.getID() == WindowEvent.WINDOW_CLOSING ) {
            // setDialogShowing( false ); // also done in closeDialog()
            closeDialog();
        }
        super.processWindowEvent(e);
    }

    /**
     * The class is a table row, holding the board and its file/message counts.
     */
    class BoardInfoTableMember extends TableMember.BaseTableMember<BoardInfoTableMember> {
        Board board;
        Integer allMsgCount;
        Integer todaysMsgCount;
        Integer flaggedMsgCount;
        Integer starredMsgCount;
        Integer unreadMsgCount;
        String dateOfLastMsg;

        public BoardInfoTableMember(final Board board) {
            this.board = board;
            this.allMsgCount = null;
            this.todaysMsgCount = null;
            this.flaggedMsgCount = null;
            this.starredMsgCount = null;
            this.unreadMsgCount = null;
            this.dateOfLastMsg = null;
        }

		public Comparable<?> getValueAt(final int column) {
            switch( column ) {
            case 0:
                return board.getName();
            case 1:
                return board.getStateString();
            case 2:
                return allMsgCount;
            case 3:
                return todaysMsgCount;
            case 4: // flagged
                return flaggedMsgCount;
            case 5: // starred
                return starredMsgCount;
            case 6: // unread
                return unreadMsgCount;
            case 7: // date of last msg
                return dateOfLastMsg;
            }
            return "*ERR*";
        }

        public Board getBoard() {
            return board;
        }

        public Integer getAllMessageCount() {
            return allMsgCount;
        }

        public void setAllMessageCount(final int i) {
			allMsgCount = i;
        }

        public void setTodaysMessageCount(final int i) {
			todaysMsgCount = i;
        }

        public void setFlaggedMessageCount(final int i) {
			flaggedMsgCount = i;
        }
        public void setStarredMessageCount(final int i) {
			starredMsgCount = i;
        }

        public void setUnreadMessageCount(final int i) {
			unreadMsgCount = i;
        }

        public void setDateOfLastMsg(final String s) {
            dateOfLastMsg = s;
        }
    }

	private class BoardInfoTableModel extends SortedTableModel<BoardInfoTableMember> implements LanguageListener {

		private static final long serialVersionUID = 1L;

		private transient Language language;

        protected final static String columnNames[] = new String[8];

        protected final static Class<?> columnClasses[] =  {
            String.class,   // board name
            String.class,   // board state
            Integer.class,  // message count
            Integer.class,  // today
            Integer.class,  // flagged
            Integer.class,  // starred
            Integer.class,  // unread
            String.class,   // date of last valid msg
        };

        public BoardInfoTableModel() {
            super();
            language = Language.getInstance();
			languageChanged(null);
        }

		public void languageChanged(LanguageEvent event) {
			columnNames[0] = language.getString("BoardInfoFrame.table.board");
			columnNames[1] = language.getString("BoardInfoFrame.table.state");
			columnNames[2] = language.getString("BoardInfoFrame.table.messages");
			columnNames[3] = language.getString("BoardInfoFrame.table.messagesToday");
			columnNames[4] = language.getString("BoardInfoFrame.table.messagesFlagged");
			columnNames[5] = language.getString("BoardInfoFrame.table.messagesStarred");
			columnNames[6] = language.getString("BoardInfoFrame.table.messagesUnread");
			columnNames[7] = language.getString("BoardInfoFrame.table.lastMsgDate");

			fireTableStructureChanged();
		}

        @Override
        public boolean isCellEditable(final int row, final int col) {
            return false;
        }

        @Override
        public String getColumnName(final int column) {
            if( (column >= 0) && (column < columnNames.length) ) {
                return columnNames[column];
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            if( (column >= 0) && (column < columnClasses.length) ) {
                return columnClasses[column];
            }
            return null;
        }
    }

	private class BoardInfoTableCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		private Font boldFont;
		private Font origFont;
		private transient Border border;

        public BoardInfoTableCellRenderer(final int horizontalAlignment) {
            super();
            origFont = boardTable.getFont();
            boldFont = origFont.deriveFont(Font.BOLD);
            border = BorderFactory.createEmptyBorder(0, 3, 0, 3);
            setVerticalAlignment(SwingConstants.CENTER);
            setHorizontalAlignment(horizontalAlignment);
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                final boolean hasFocus, final int row, final int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            final BoardInfoTableMember tblrow = boardTableModel.getRow(row);

            if( tblrow.getBoard().isUpdating() ) {
                setFont(boldFont);
            } else {
                setFont(origFont);
            }
            setBorder(border);

            // get the original model column index (maybe columns were reordered by user)
            final TableColumn tableColumn = table.getColumnModel().getColumn(column);
            final int modelColumn = tableColumn.getModelIndex();

            if( modelColumn == 0 ) {
                setIcon(tblrow.getBoard().getStateIcon());
            } else {
                setIcon(null);
            }

            if (!isSelected) {
                final Color newBackground = TableBackgroundColors.getBackgroundColor(table, row, showColoredLines);
                setBackground(newBackground);
            }
            return this;
        }
    }

    // Implementing the BoardUpdateThreadListener ...

     /**
      * Is called if a Thread is finished.
      */
     public void boardUpdateThreadFinished(final BoardUpdateThread thread) {
        boardTableModel.tableEntriesChanged();
    }

    /**
     * Is called if a Thread is started.
     *
     * @see frost.messaging.frost.boards.BoardUpdateThreadListener#boardUpdateThreadStarted(frost.messaging.frost.boards.BoardUpdateThread)
     */
    public void boardUpdateThreadStarted(final BoardUpdateThread thread) {
        boardTableModel.tableEntriesChanged();
    }

    public void boardUpdateInformationChanged(final BoardUpdateThread thread, final BoardUpdateInformation bui) {
    }

    public static boolean isDialogShowing() {
        return isShowing;
    }

    public static void setDialogShowing(final boolean val) {
        isShowing = val;
    }

	@Override
	public void languageChanged(LanguageEvent event) {
		setTitle(language.getString("BoardInfoFrame.title"));

		Bclose.setText(language.getString("BoardInfoFrame.button.close"));

		copyInfoToClipboardAction.setText(language.getString("BoardInfoFrame.popupMenu.copyInfoToClipboard"));
		updateSelectedBoardAction.setText(language.getString("BoardInfoFrame.button.updateSelectedBoard"));
		updateAllBoardsAction.setText(language.getString("BoardInfoFrame.button.updateAllBoards"));
		removeSelectedBoardsAction.setText(language.getString("BoardInfoFrame.button.removeSelectedBoards"));
		updateAction.setText(language.getString("BoardInfoFrame.button.update"));

		updateAction.actionPerformed(null); // update labels
	}

	private class CopyInfoToClipboardAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			int[] selectedRows = boardTable.getSelectedRows();

			if (selectedRows.length > 0) {
				StringBuilder sb = new StringBuilder();
				for (int rowIx : selectedRows) {
					BoardInfoTableMember row = (boardTableModel).getRow(rowIx);
					sb.append(row.getBoard().getName());
					sb.append("  (");
					sb.append(row.getBoard().getStateString());
					sb.append(")  ");
					sb.append(row.getAllMessageCount());
					sb.append("\n");
				}
				ClipboardUtil.copyText(sb.toString());
			}
		}
	}

	private class UpdateSelectedBoardAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			int[] selectedRows = boardTable.getSelectedRows();

			if (selectedRows.length > 0) {
				for (int rowIx : selectedRows) {
					BoardInfoTableMember row = boardTableModel.getRow(rowIx);

					if (row.getBoard().isManualUpdateAllowed()) {
						tofTree.updateBoard(row.getBoard());
					}
					boardTableModel.fireTableCellUpdated(rowIx, 0);
				}
				boardTable.clearSelection();
			}
		}
	}

	/**
	 * Tries to start update for all allowed boards. Gets list of board from
	 * tofTree, because the board table could be not yet finished to load.
	 */
	private class UpdateAllBoardsAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			List<Board> boards = ((TofTreeModel) tofTree.getModel()).getAllBoards();
			for (Board board : boards) {
				if (board.isManualUpdateAllowed()) {
					tofTree.updateBoard(board);
				}
				boardTableModel.fireTableDataChanged();
			}
		}
	}

	private class RemoveSelectedBoardsAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			int[] selectedRows = boardTable.getSelectedRows();

			for (int rowIx : selectedRows) {
				BoardInfoTableMember row = boardTableModel.getRow(rowIx);
				mainFrame.getMessagingTab().getTofTree().removeNode(BoardInfoFrame.this, row.getBoard());
			}

			updateAction.actionPerformed(null);
		}
	}

	private class UpdateAction extends BaseAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (updateBoardInfoTableThread != null) {
				return;
			}

			boardTable.getModel().clearDataModel();

			updateBoardInfoTableThread = new UpdateBoardInfoTableThread();
			updateBoardInfoTableThread.start();

			checkActionsEnabled();
		}
	}

	private class PopupMenu extends JPopupMenu {

		private static final long serialVersionUID = 1L;

		public PopupMenu() {
			add(copyInfoToClipboardAction);
			addSeparator();
			add(updateSelectedBoardAction);
			add(updateAllBoardsAction);
			addSeparator();
			add(removeSelectedBoardsAction);
			addSeparator();
			add(updateAction);
		}
	}
}
