/*
 SortedModel.java / Frost
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a Model that stores ModelItems in a certain order. That does not
 * mean that it is sorted.
 *
 * Its implementation is thread-safe
 */
abstract public class SortedModel<T extends ModelItem<T>> {

	private static final Logger logger = LoggerFactory.getLogger(SortedModel.class);

	protected List<T> data;

	private SortedModelListenerSupport<T> listenerSupport;

    private boolean ascending;

    private int columnNumber = -1;

    private final SortedTableFormat<T> tableFormat;
    private SortedModelTable<T> table = null;

	public SortedModel(final SortedTableFormat<T> newFormat) {
		super();
		data = new ArrayList<T>();
        tableFormat = newFormat;
	}

    public void setTable(final SortedModelTable<T> t) {
        table = t;
    }
    public SortedModelTable<T> getTable() {
        return table;
    }

    public SortedTableFormat<T> getTableFormat() {
        return tableFormat;
    }

	protected void addItem(final T item) {

        if (columnNumber == -1) {
            synchronized(data) {
                data.add(item);
                fireItemAdded(item);
            }
            item.setModel(this);
        } else {
            addItem(item, getInsertionPoint(item));
        }
	}

	protected void addItem(final T item, final int position) {
		synchronized(data) {
			data.add(position, item);
			fireItemAdded(item);
		}
		item.setModel(this);
	}

	/**
	 * Adds an OrderedModelListener to the listener list.
	 * <p>
	 * If listener is null, no exception is thrown and no action is performed.
	 *
	 * @param    listener  the OrderedModelListener to be added
	 */
	public synchronized void addOrderedModelListener(final SortedModelListener<T> listener) {
		if (listener == null) {
			return;
		}
		if (listenerSupport == null) {
			listenerSupport = new SortedModelListenerSupport<T>();
		}
		listenerSupport.addModelListener(listener);
	}

	public synchronized void clear() {
		synchronized (data) {
			final Iterator<T> iterator = data.iterator();
			while (iterator.hasNext()) {
				final T item = iterator.next();
				item.setModel(null);
			}
			data.clear();

            getTable().fireTableDataChanged();

            if (listenerSupport == null) {
                return;
            }
            listenerSupport.fireModelCleared();
		}
	}

	protected void fireItemAdded(final T item) {

        final int position = data.indexOf(item);
        getTable().fireTableRowsInserted(position, position);

		if (listenerSupport == null) {
			return;
		}
		listenerSupport.fireItemAdded(item, position);
	}

	protected void fireItemChanged(final T item) {

        if (columnNumber == -1) {
            fireItemChanged(item);
        } else {
            // maybe reinsert, first check if position would change
            boolean reinsert = true;
            if( data.size() > 1 ) {
                final int p = data.indexOf(item);
                final Comparator<T> cmp = getComparator();
                if( p == 0 ) {
                    // first item, compare with second item
                    final T compItem = data.get(1);
                    if( cmp.compare(item, compItem) <= 0 ) {
                        // no need to resort
                        reinsert = false;
                    }
                } else if( p == data.size()-1 ){
                    // last item, compare with preceeding item
                    final T compItem = data.get(p-1);
                    if( cmp.compare(item, compItem) >= 0 ) {
                        // no need to resort
                        reinsert = false;
                    }
                } else {
                    // middle item, compare with preceeding and following item
                    final T compItem1 = data.get(p-1);
                    final T compItem2 = data.get(p+1);
                    if( cmp.compare(item, compItem1) >= 0 ) {
                        if( cmp.compare(item, compItem2) <= 0 ) {
                            // no need to resort
                            reinsert = false;
                        }
                    }
                }
            } else {
                // only 1 item in table, no need to reinsert
                reinsert = false;
            }

            if( reinsert ) {
                data.remove(item);
                data.add(getInsertionPoint(item), item);
                table.fireTableDataChanged();
            }
        }

        final int position = data.indexOf(item);
        getTable().fireTableRowsUpdated(position, position);

		if (listenerSupport == null) {
			return;
		}
		listenerSupport.fireItemChanged(item, position);
	}

    void itemChanged(final T item) {
        fireItemChanged(item);
    }

	private void fireItemsRemoved(final int[] positions, final ArrayList<T> items) {

        getTable().fireTableRowsDeleted(positions);

		if (listenerSupport == null) {
			return;
		}
		listenerSupport.fireItemsRemoved(positions, items);
	}

	public T getItemAt(final int position) {
        if( position >= data.size() ) {
            logger.error("SortedModel.getItemAt: position = {}, but size = {}", position, data.size());
            return null;
        }
		return data.get(position);
	}

	public int getItemCount() {
		return data.size();
	}

    /**
     * @return  a new List containing all items of this model, in unspecific order
     */
    public List<T> getItems() {
        return new ArrayList<T>(data);
    }

	/**
	 * Returns the index in this model of the first occurrence of the specified
     * item, or -1 if this model does not contain this element.
     *
     * @param item item to search for.
	 * @return the index in this model of the first occurrence of the specified
     * 	       item, or -1 if this model does not contain this element.
	 */
	public int indexOf(final T item) {
		return data.indexOf(item);
	}
	
	
	public boolean removeItem( final T item) {
		List<T> items = new ArrayList<T>();
		items.add(item);
		return removeItems(items);
	}

	public boolean removeItems(final List<T> items) {
		//We clear the link to the model of each item
		for( final T element : items ) {
			element.setModel(null);
		}
		//We remove the first occurrence of each item from the model
		final int[] removedPositions = new int[items.size()];
		final ArrayList<T> removedItems = new ArrayList<T>(); 
		int count = 0;
		synchronized (data) {
			for( final T element : items ) {
				final int position = data.indexOf(element);
				if (position != -1) {
					data.remove(position);
					removedItems.add(element);
					removedPositions[count] = position;
					count++;
				}
			}
		}
		//We send an items removed event. Only those items that actually
		//were in the model and thus were removed are included in the event.
		if (count != 0) {
			fireItemsRemoved(removedPositions, removedItems);
			return true;
		} else {
			return false;
		}
	}

    protected void sort(final int newColumnNumber, final boolean newAscending) {
        columnNumber = newColumnNumber;
        ascending = newAscending;
        Collections.sort(data, getComparator());
    }

    private Comparator<T> getComparator() {
        if (ascending) {
            return tableFormat.getComparator(columnNumber);
        } else {
            return tableFormat.getReverseComparator(columnNumber);
        }
    }

    private int getInsertionPoint(final T item) {
        int position = Collections.binarySearch(data, item, getComparator());
        if (position < 0) {
            // No similar item was in the list
            position = (position + 1) * -1;
        } else {
            // There was already a similar item (or more) in the list.
            // We find out the end position of that sublist and use it as insertion point.
            position = Collections.lastIndexOfSubList(data, Collections.singletonList(data.get(position)));
            position++;
        }
        return position;
    }
}
