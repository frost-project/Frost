/*
 SortedModelTable.java / Frost
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

import frost.util.gui.FrostSwingWorker;

public class SortedModelTable<T extends ModelItem<T>> extends ModelTable<T> {

	private static final long serialVersionUID = 1L;

	/**
	 * Index in the ModelTable of the column the model is 
	 * sorted by (or -1 if it is not currently sorted). 
	 */
	private int currentSortedColumnNumber = -1;
	private boolean ascending;
	
	/**
	 * @param newModel
	 * @param newTableFormat
	 */
	public SortedModelTable(SortedModel<T> newModel) {

        super(newModel.getTableFormat());
        
        newModel.setTable(this);
        
		setModel(newModel);
		initialize();
		
		getTable().setTableHeader(new SortedTableHeader(this));
	}
	/**
	 * @param columnNumber
	 */
	void columnClicked(int columnNumber) {
		if (columnNumber != currentSortedColumnNumber) {
			currentSortedColumnNumber = columnNumber;
			ascending = true;
		} else {
			ascending = !ascending;
		}
        
        resortTable();
	}
    
    public void setSortedColumn(final int col, final boolean asc) {
        currentSortedColumnNumber = col;
        ascending = asc;
        resortTable();
    }
    
    public void resortTable() {
        FrostSwingWorker worker = new FrostSwingWorker(table) {

            @Override
            protected void doNonUILogic() throws RuntimeException {
                int index = convertColumnIndexToFormat(currentSortedColumnNumber);
                model.sort(index, ascending);
            }

            @Override
            protected void doUIUpdateLogic() throws RuntimeException {
                table.revalidate();
                table.repaint();
            }

        };
        worker.start();
    }
    
    public void refreshView() {
        table.revalidate();
        table.repaint();
    }
	
	/**
	 * This method returns the number of the column the
	 * table is currently sorted by (or -1 if none).
	 * 
	 * @return the number of the column that is currently sorted. -1 if none.
	 */
	public int getSortedColumn() {
		return currentSortedColumnNumber;
	}

	/**
	 * @return  true if the sort order is ascending
	 */
	public boolean isSortedAscending() {
		return ascending;
	}
	
	/**
	 * This method returns the model item that is represented on a particular
	 * row of the table
	 * @param rowIndex the index of the row the model is represented on
	 * @return the model item (may be null)
	 */
	public T getItemAt(int rowIndex) {
		return model.getItemAt(rowIndex);
	}

	/* (non-Javadoc)
	 * @see frost.util.model.gui.ModelTable#setColumnVisible(int, boolean)
	 */
	@Override
    public void setColumnVisible(int index, boolean visible) {
		super.setColumnVisible(index, visible);
		if (!visible) {
			if (index == currentSortedColumnNumber) {
				currentSortedColumnNumber = -1;
			} else if (index < currentSortedColumnNumber) {
				currentSortedColumnNumber--;
			}
		}
	}
}
