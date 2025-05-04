/*
  SearchManager.java / Frost
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
package frost.fileTransfer.search;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import frost.Core;
import frost.MainFrame;
import frost.Settings;

public class SearchManager implements PropertyChangeListener {

    private SearchPanel panel;

    public SearchManager() {
        super();
    }

    public void initialize() {
        getPanel();
    }
    
    public void addPanelToMainFrame(MainFrame mainFrame) {
        mainFrame.addPanel("MainFrame.tabbedPane.search", getPanel());
        Core.frostSettings.addPropertyChangeListener(Settings.FILESHARING_DISABLE, this);
        updateDownloadStatus();
    }

    public SearchPanel getPanel() {
        if (panel == null) {
            panel = new SearchPanel();
            panel.initialize();
        }
        return panel;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Settings.FILESHARING_DISABLE)) {
            updateDownloadStatus();
        }
    }

    private void updateDownloadStatus() {
        boolean disableFileSharing = Core.frostSettings.getBoolValue(Settings.FILESHARING_DISABLE);
        MainFrame.getInstance().setPanelEnabled("MainFrame.tabbedPane.search", !disableFileSharing);
    }
}
