/*
  FrostUnsentMessageObject.java / Frost
  Copyright (C) 2006  Frost Project <jtcfrost.sourceforge.net>

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
package frost.messaging.frost;

import java.time.Instant;

import frost.MainFrame;
import frost.messaging.frost.threads.MessageThread;
import frost.storage.perst.messages.PerstFrostUnsentMessageObject;
import frost.util.DateFun;

/**
 * Same as FrostMessageObject, but adds some stuff needed only for unsend messages.
 */
public class FrostUnsentMessageObject extends FrostMessageObject {

	private static final long serialVersionUID = 1L;

	private PerstFrostUnsentMessageObject perstFrostUnsentMessageObject = null;

    private long timeAdded = 0;
    private String timeAddedString = null;

    private long sendAfterTime = 0;

    private MessageThread currentUploadThread = null; // is set during upload of message

    public FrostUnsentMessageObject() {
        super();
    }

    public void setTimeAdded(final long ta) {
        timeAdded = ta;
    }
    public long getTimeAdded() {
        return timeAdded;
    }
    public String getTimeAddedString() {
        if( timeAddedString == null ) {
			timeAddedString = DateFun.FORMAT_DATE_EXT.format(Instant.ofEpochMilli(timeAdded)) + " "
					+ DateFun.FORMAT_TIME_EXT.format(Instant.ofEpochMilli(timeAdded));
        }
        return timeAddedString;
    }

    public AttachmentList<FileAttachment> getUnsentFileAttachments() {
        final AttachmentList<FileAttachment> result = new AttachmentList<FileAttachment>();
        final AttachmentList<FileAttachment> fileAttachments =  getAttachmentsOfTypeFile();
        if( (fileAttachments == null) || (fileAttachments.size() == 0) ) {
            return result;
        }
        for( final FileAttachment fa : fileAttachments ) {
            if( (fa.getKey() == null) || (fa.getKey().length() == 0) ) {
                result.add(fa);
            }
        }
        return result;
    }

    public MessageThread getCurrentUploadThread() {
        return currentUploadThread;
    }

    public void setCurrentUploadThread(final MessageThread currentUploadThread) {
        this.currentUploadThread = currentUploadThread;
        // update unsend message in unsend messages table
        MainFrame.getInstance().getMessagingTab().getUnsentMessagesPanel().updateUnsentMessage(this);
    }

    public long getSendAfterTime() {
        return sendAfterTime;
    }
    public void setSendAfterTime(final long sendAfter) {
        this.sendAfterTime = sendAfter;
    }

    public PerstFrostUnsentMessageObject getPerstFrostUnsentMessageObject() {
        return perstFrostUnsentMessageObject;
    }

    public void setPerstFrostUnsentMessageObject(final PerstFrostUnsentMessageObject perstFrostUnsentMessageObject) {
        this.perstFrostUnsentMessageObject = perstFrostUnsentMessageObject;
    }
}
