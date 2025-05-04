/*
 ModelTable.java / Frost
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
package frost.util.model;

import java.awt.Font;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This subclass of AbstractTableModel is passed an OrderedModel and a
 * TableFormat in the constructor. It then creates a JTable and displays
 * the content of the OrderedModel on it according to the rules specified
 * in the TableFormat.
 *
 * It also listens for changes in the OrderedModel  and updates the JTable
 * as necessary.
 *
 * Besides, the user can choose which columns will be shown via a menu
 * that pops up when he right clicks on the header.
 */
abstract public class ModelTable<T extends ModelItem<T>> extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(ModelTable.class);

	/**
	 * Helper class to be able to safely get the selection fron any thread
	 */
	protected class SelectionGetter implements Runnable {

		private final int MODE_SINGLE = 0;
		private final int MODE_MULTIPLE = 1;

		int mode = 0;

		List<T> selectedItems;
		T selectedItem;

		/**
		 * This method returns an array of all the Ts that are
		 * selected in the JTable.
		 *  @return an array containing the Ts that are selected
		 */
		public List<T> getSelectedItems() {
			mode = MODE_MULTIPLE;
			if (SwingUtilities.isEventDispatchThread()) {
				run();
			} else {
				try {
					SwingUtilities.invokeAndWait(this);
				} catch (final InterruptedException e) {
					logger.error("Exception thrown in SelectionGetter.run()", e);
				} catch (final InvocationTargetException e) {
					logger.error("Exception thrown in SelectionGetter.run()", e);
				}
			}
			return selectedItems;
		}

		/**
		 * This method returns the T that is selected in
		 * the JTable (or the first one if there are several). If there is
		 * none, it returns null.
		 *  @return the T that is selected in the JTable, or
		 * 			 the first one if there are several. null if there is
		 * 			 none.
		 */
		public T getSelectedItem() {
			mode = MODE_SINGLE;
			if (SwingUtilities.isEventDispatchThread()) {
				run();
			} else {
				try {
					SwingUtilities.invokeAndWait(this);
				} catch (final InterruptedException e) {
					logger.error("Exception thrown in SelectionGetter.run()", e);
				} catch (final InvocationTargetException e) {
					logger.error("Exception thrown in SelectionGetter.run()", e);
				}
			}
			return selectedItem;
		}

		/**
		 * This method is executed in the Swing event thread. It gets the selected items
		 * and places them in an attribute so that the methods getSelectedItem and getSelectedItems
		 * can return them
		 * @see Runnable#run()
		 */
		public void run() {
			synchronized (model) {
				switch (mode) {
					case MODE_MULTIPLE :
						selectedItems = new ArrayList<T>();
						final int[] selectedRows = table.getSelectedRows();
						for (int i = 0; i < selectedRows.length; i++) {
							selectedItems.add( model.getItemAt(selectedRows[i]));
						}
						break;
					case MODE_SINGLE :
						final int selectedRow = table.getSelectedRow();
						if (selectedRow != -1) {
							selectedItem = model.getItemAt(selectedRow);
						}
						break;
				}
			}
		}

	}

	protected ModelTableFormat<T> tableFormat;
	protected SortedModel<T> model;

	protected JTable table;
	private JScrollPane scrollPane;

	/**
	 * This ArrayList contains the model indexes of the columns that are being shown
	 */
	private final ArrayList<Integer> visibleColumns = new ArrayList<Integer>();

	/**
	 * This ArrayList contains all of the TableColumns that this ModelTable may show.
	 */
	private final ArrayList<TableColumn> columns = new ArrayList<TableColumn>();

	/**
	 * This method creates an instance of Model table with the given ModelTableFormat
	 * but without an OrderedModel. The method setModel should be called before
	 * initialization (this constructor does not perform that initialization).
	 * @param newTableFormat the ModelTableFormat that defines the visual representation
	 * 						  of the data in the OrderedModel.
	 */
	protected ModelTable(final ModelTableFormat<T> newTableFormat) {
		super();

		tableFormat = newTableFormat;
	}

	/**
	 * This method creates an instance of Model table with the given ModelTableFormat
	 * and OrderedModel and initializes it.
	 * @param newModel the OrderedModel that contains the data to be shown on the JTable
	 * @param newTableFormat the ModelTableFormat that defines the visual representation
	 * 						  of the data in the OrderedModel.
	 */
	protected ModelTable(final SortedModel<T> newModel, final ModelTableFormat<T> newTableFormat) {
		super();

		model = newModel;
		tableFormat = newTableFormat;

		initialize();
	}

	/**
	 * This method initializes the ModelTable. It creates the default TableColumns,
	 * customizes the JTable and sets up the listener.
	 */
	protected void initialize() {
		final int columnCount = tableFormat.getColumnCount();
		for (int i = 0; i < columnCount; i++) {
			visibleColumns.add(i);
		}

		table = new JTable(this);
		scrollPane = new JScrollPane(table);

        scrollPane.setWheelScrollingEnabled(true);
		tableFormat.addTable(table);

		tableFormat.customizeTable(this);

		final TableColumnModel columnModel = table.getColumnModel();
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			columns.add(columnModel.getColumn(i));
		}
	}

	public int getColumnCount() {
		return visibleColumns.size();
	}

	public int getRowCount() {
		return model.getItemCount();
	}

	public Object getValueAt(final int rowIndex, final int columnIndex) {
		final int index = convertColumnIndexToFormat(columnIndex);
		final T mi = model.getItemAt(rowIndex);
		if( mi == null ) {
		    return "ModelTable.getValueAt(): index "+rowIndex+" is null";
		} else {
		    return tableFormat.getCellValue(mi, index);
		}
	}


	/**
	 * This method is called whenever an event is received from the
	 * OrderedModel indicating that several items have been removed from it.
	 * @param positions the positions of the ModelItems that have
	 * been removed from the OrderedModel.
	 */
	protected void fireTableRowsDeleted(final int[] positions) {
		for( final int position : positions ) {
			fireTableRowsDeleted(position, position);
		}
	}

	/**
	 * This method returns an array of all the ModelItems that are
	 * selected in the JTable.
	 * @return an array containing the ModelItems that are selected
	 */
	public List<T> getSelectedItems() {
		return new SelectionGetter().getSelectedItems();
	}

	/**
	 * This method returns the selected ModelItem, or the first one
	 * if there was several of them. It returns null if there was none.
	 * @return the selected ModelItem, or the first one if there was
	 * 			several of them. null if there was none.
	 */
	public T getSelectedItem() {
		return new SelectionGetter().getSelectedItem();
	}

	/**
	 * This method returns the number of rows that are selected.
	 * @return the number of rows that are selected.
	 */
	public int getSelectedCount() {
		return table.getSelectedRowCount();
	}

	/**
	 * This method returns the JScrollPane the JTable is created into.
	 * @return the JScrollPane the JTable is created into.
	 */
	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	@Override
    public String getColumnName(final int column) {
		final int index = convertColumnIndexToFormat(column);
		return tableFormat.getColumnName(index);
	}

	/**
	 * This method returns the JTable that is used by this ModelTable
	 * to show the contents of its OrderedModel.
	 * @return the JTable that is used by this ModelTable to show the
	 * 			contents of its OrderedModel.
	 */
	public JTable getTable() {
		return table;
	}

	/**
	 * This method changes the Font of the JTable in this ModelTable
	 * @param font the new font for the JTable in this ModelTable.
	 */
	public void setFont(final Font font) {
		table.setFont(font);
		table.setRowHeight(font.getSize() + 5);
	}

	@Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
		final int index = convertColumnIndexToFormat(columnIndex);
		return tableFormat.isColumnEditable(index);
	}

	/**
	 * This method is used to find out if the column with the given model index
	 * is currently being shown or not.
	 * @param columnIndex the model index of the column to find out if it is
	 * 					   being shown or not.
	 * @return true if the column is being shown. false otherwise.
	 */
	public boolean isColumnVisible(final int columnIndex) {
		final int position = convertColumnIndexToModel(columnIndex);
		if (position != -1) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * This method shows or hides a particular column. In case it tries to
	 * show a column that is already being shown or to hide a column that is
	 * already hidden, the command is simply ignored.
	 * @param index the model index of the column to hide or show.
	 * @param visible if true, the column will be shown. If false, the column
	 * 		   will be hidden.
	 */
	public void setColumnVisible(final int index, final boolean visible) {
		final TableColumnModel columnModel = getTable().getColumnModel();
		final int position = convertColumnIndexToModel(index);

		if (visible) {
			if (position == -1) {
				visibleColumns.add(index);
				final TableColumn column = columns.get(index);
				column.setModelIndex(visibleColumns.size() - 1);
				columnModel.addColumn(column);
			}
		} else {
			if (position != -1) {
				visibleColumns.remove(Integer.valueOf(index));
				columnModel.removeColumn(columns.get(index));
				//Here we have to decrease the model index of all the columns
				//that were to the right of the one we have removed.
				for (int i = 0; i < columnModel.getColumnCount(); i++) {
					final TableColumn column = columnModel.getColumn(i);
					final int modelIndex = column.getModelIndex();
					if (modelIndex >= position) {
						column.setModelIndex(modelIndex - 1);
					}
				}
			}
		}
	}

	@Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
		final int index = convertColumnIndexToFormat(columnIndex);
		tableFormat.setCellValue(aValue, model.getItemAt(rowIndex), index);
	}

	/**
	 * This method sets a new OrderedModel for the ModelTable (mainly to be
	 * used in conjunction with the constructor that is only passed a ModelTableFormat)
	 * @param model the OrderedModel this ModelTable will get the data from
	 */
	protected void setModel(final SortedModel<T> newModel) {
		model = newModel;
	}

	/**
	 * This method returns an Iterator of all the TableColumns that this
	 * ModelTable may show.
	 * @return an Iterator of all the TableColumns that this
	 * 			ModelTable may show.
	 */
	public Iterator<TableColumn> getColumns() {
		return columns.iterator();
	}

    public List<TableColumn> getColumnsList() {
        return columns;
    }

	/**
	 * This method maps the index that a column has in the associated TableFormat to
	 * the index that column has in this ModelTable.
	 *
	 * @param formatColumnIndex the index a column has in the associated TableFormat
	 * @return the index that column has in this ModelTable
	 */
	protected int convertColumnIndexToModel(final int formatColumnIndex) {
		return visibleColumns.indexOf(formatColumnIndex);
	}

	/**
	 * This method maps the index that a column has in this ModelTable to
	 * the index that column has in the associated TableFormat.
	 *
	 * @param formatColumnIndex the index a column has in this ModelTable
	 * @return the index that column has in the associated TableFormat
	 */
	protected int convertColumnIndexToFormat(final int modelColumnIndex) {
		final Integer index = visibleColumns.get(modelColumnIndex);
		return index.intValue();
	}

    public ModelTableFormat<T> getTableFormat() {
        return tableFormat;
    }
}
