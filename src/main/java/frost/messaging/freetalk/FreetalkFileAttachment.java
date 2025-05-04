/*
 FileAttachment.java / Frost
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
package frost.messaging.freetalk;

import java.io.File;

import frost.util.CopyToClipboardItem;

public class FreetalkFileAttachment implements CopyToClipboardItem {

    private File file = null;

    private String uri = null;
    private long size = 0; // Filesize

	public FreetalkFileAttachment(final String u, final long s) {
        size = s;
        uri = u;
	}

	/**
	 * Called for an unsend message, initializes internal file object.
	 */
    public FreetalkFileAttachment(final File newFile, final String k, final long s) {
        file = newFile;
        size = s;
        uri = k;
    }

    public FreetalkFileAttachment(final File f) {
        file = f;
        size = file.length();
    }

	public int compareTo(final Object o) {
		final String myName = getFileName();
		final String otherName = ((FreetalkFileAttachment) o).getFileName();
		return myName.compareTo(otherName);
	}

    public String getUri() {
        return uri;
    }
    public void setUri(final String k) {
        uri = k;
    }
    public long getFileSize() {
        return size;
    }
    public File getInternalFile() {
        return file;
    }

    public String getFileName() {
        return uri;
    }

    public String getKey() {
        return "";
    }
}
