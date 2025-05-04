/*
  SearchModel.java / Frost
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.fileTransfer.FileTransferManager;
import frost.fileTransfer.FrostFileListFileObject;
import frost.fileTransfer.download.DownloadModel;
import frost.fileTransfer.download.FrostDownloadItem;
import frost.util.model.SortedModel;
import frost.util.model.SortedTableFormat;

public class SearchModel extends SortedModel<FrostSearchItem> {

	private static final Logger logger = LoggerFactory.getLogger(SearchModel.class);

    public SearchModel(final SortedTableFormat<FrostSearchItem> f) {
        super(f);
    }

    public void addSearchItem(final FrostSearchItem searchItem) {
        addItem(searchItem);
    }

    public void addItemsToDownloadTable(final List<FrostSearchItem> selectedItems) {

        if( selectedItems == null ) {
            return;
        }

        final DownloadModel downloadModel = FileTransferManager.inst().getDownloadManager().getModel();

        for (int i = selectedItems.size() - 1; i >= 0; i--) {
            final FrostFileListFileObject flf = selectedItems.get(i).getFrostFileListFileObject();
            String filename = flf.getDisplayName();
            // maybe convert html codes (e.g. %2c -> , )
            if( filename.indexOf("%") > 0 ) {
                try {
                    filename = URLDecoder.decode(filename, "UTF-8");
                } catch (final UnsupportedEncodingException ex) {
                    logger.error("Decode of HTML code failed", ex);
                }
            }
            final FrostDownloadItem dlItem = new FrostDownloadItem(flf, filename);
            downloadModel.addDownloadItem(dlItem);
            selectedItems.get(i).updateState();
        }
    }
}
