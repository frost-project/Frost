/*
 TranslatableListModel.java / Frost
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
package frost.util.gui.translation;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;

/**
 * A translatable list model contains keys to a Language. It shows the localized values on screen.
 * Its getElementAt method returns the localized value, while the mehtod getKeyAt returns the key.
 * 
 * @author $Author: artur8ur $
 * @version $Revision: 3244 $
 */
public class TranslatableListModel extends DefaultListModel<String> implements LanguageListener {

	private static final long serialVersionUID = 1L;

	private Language language = null;

	public TranslatableListModel(Language language) {
		super();
		this.language = language;
		language.addLanguageListener(this);
	}

	public void languageChanged(LanguageEvent event) {
		fireContentsChanged(this, 0, getSize() - 1);
	}

	/** 
	 * This method returns the internationalized value at a given position
	 * @see ListModel#getElementAt(int)
	 */
	@Override
	public String getElementAt(int index) {
		String key = super.getElementAt(index).toString();
		return language.getString(key);
	}

	/**
	 * This method returns the key at a given position
	 * @param selectedIndex
	 * @return the key 
	 */
	public String getKeyAt(int selectedIndex) {
		return super.getElementAt(selectedIndex).toString();
	}

	/**
	 * This method returns the position of the key in the model
	 * @param key
	 * @return the position
	 */
	public int indexOfKey(Object key) {
		return super.indexOf(key);
	}

	/**
	 * This method returns the position of the first occurrence of the
	 * internationalized element in the model
	 * @param elem internationalized item
	 * @return the position
	 */
	@Override
    public int indexOf(Object elem) {
		int position = -1;
		for (int i = 0; (i < getSize() - 1) || (position != -1);i++) {
			String localizedValue = language.getString(getKeyAt(i));
			if (elem.equals(localizedValue)) {
				position = i;	
			}
		} 
		return super.indexOf(elem);
	}

}
