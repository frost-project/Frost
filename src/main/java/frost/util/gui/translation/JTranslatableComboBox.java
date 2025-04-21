/*
 JTranslatableComboBox.java / Frost
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

import javax.swing.JComboBox;

/**
 * @author $Author: artur8ur $
 * @version $Revision: 3244 $
 */
public class JTranslatableComboBox extends JComboBox<CheckBoxItem> implements LanguageListener {

	private static final long serialVersionUID = 1L;

	private Language language = null;
	private String[] keys;

	/**
	 * @param language
	 * @param keys
	 */
	public JTranslatableComboBox(Language language, String[] keys) {
		super();
		this.language = language;
		this.keys = keys;
		language.addLanguageListener(this);
		for (int i = 0; i < keys.length; i++) {
			String value = language.getString(keys[i]);
			CheckBoxItem item = new CheckBoxItem(keys[i], value);
			addItem(item);
		}
	}

	/* (non-Javadoc)
	 * @see frost.gui.translation.LanguageListener#languageChanged(frost.gui.translation.LanguageEvent)
	 */
	public void languageChanged(LanguageEvent event) {
		refreshLanguage();		
	}

	/**
	 * 
	 */
	private void refreshLanguage() {
		for (int i = 0; i < keys.length; i++) {
			CheckBoxItem item = getItemAt(i);
			String newValue = language.getString(item.getKey());
			item.setValue(newValue);
		}
		//This is done to refresh the horizontal size
		CheckBoxItem dummy = new CheckBoxItem("","");	
		addItem(dummy);
		removeItem(dummy);	
	}
	
	/**
	 * @return
	 */
	public String getSelectedKey() { 
		Object selectedItem = getSelectedItem();
		if ((selectedItem != null) && (selectedItem instanceof CheckBoxItem)) {
			return ((CheckBoxItem) selectedItem).getKey();
		} else {
			return null;
		}
	}
	
	/**
	 * If no item contains that key, the selection remains unchanged.
	 * @param aKey the key of the item to select
	 */
	public void setSelectedKey(String aKey) {
		boolean found = false;
		for (int i = 0;(i < getItemCount()) && !found; i++) {
			Object item = getItemAt(i);
			if (item instanceof CheckBoxItem) {
				if (((CheckBoxItem) item).getKey().equals(aKey)) {
					setSelectedIndex(i);
					found = true;
				}
			}
		}
	}

}
