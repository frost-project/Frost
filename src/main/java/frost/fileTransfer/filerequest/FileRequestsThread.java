/*
  FileRequestsThread.java / Frost
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
package frost.fileTransfer.filerequest;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.fcp.FcpHandler;
import frost.storage.perst.IndexSlot;
import frost.storage.perst.IndexSlotsStorage;
import frost.transferlayer.GlobalFileDownloader;
import frost.transferlayer.GlobalFileDownloaderResult;
import frost.transferlayer.GlobalFileUploader;
import frost.util.DateFun;
import frost.util.FileAccess;
import frost.util.Mixed;

/**
 * Thread receives and sends file requests periodically.
 * Runs forever and sleeps between loops.
 */
public class FileRequestsThread extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(FileRequestsThread.class);

    // sleeptime between request loops
    private static final int sleepTime = 10 * 60 * 1000;

    private final String keyPrefix;

    // one and only instance
    private static FileRequestsThread instance = new FileRequestsThread();

    private FileRequestsThread() {
        final String fileBase = Core.frostSettings.getString(Settings.FILE_BASE);
        keyPrefix = "KSK@frost/filerequests/" + fileBase + "-";
    }

    public static FileRequestsThread getInstance() {
        return instance;
    }

    public boolean cancelThread() {
        return false;
    }

    /**
     * Returns true if no error occured.
     */
    private boolean uploadRequestFile(final String dateStr, final IndexSlot gis) throws Throwable {

        // get a list of CHK keys to send
        final List<String> fileRequests = FileRequestsManager.getRequestsToSend();
        if( fileRequests == null || fileRequests.size() == 0 ) {
            logger.info("No requests to send.");
            return true;
        }
        logger.debug("uploadRequestFile: fileRequests to send: {}", fileRequests.size());

        final FileRequestFileContent content = new FileRequestFileContent(System.currentTimeMillis(), fileRequests);

        // write a file with requests to a tempfile
        final File tmpRequestFile = FileAccess.createTempFile("filereq_", ".xml");
        if( !FileRequestFile.writeRequestFile(content, tmpRequestFile) ) {
            logger.error("Error writing the file requests file.");
            return false;
        }

        // Wait some random time to not to flood the node
        Mixed.waitRandom(2000);

        logger.info("Starting upload of request file containing {} SHAs", fileRequests.size());

        final String insertKey = keyPrefix + dateStr + "-";
        final boolean wasOk = GlobalFileUploader.uploadFile(gis, tmpRequestFile, insertKey, ".xml", true);
        tmpRequestFile.delete();
        logger.debug("uploadRequestFile: upload finished, wasOk = {}", wasOk);
        if( wasOk ) {
            FileRequestsManager.updateRequestsWereSuccessfullySent(fileRequests);
        }

        IndexSlotsStorage.inst().storeSlot(gis);

        return wasOk;
    }

    private void downloadDate(final String dateStr, final IndexSlot gis, final boolean isForToday) throws Throwable {

        // "KSK@frost/filerequests/2006.11.1-<index>.xml"
        final String requestKey = keyPrefix + dateStr + "-";

        int maxFailures;
        if (isForToday) {
            maxFailures = 3; // skip a maximum of 2 empty slots for today
        } else {
            maxFailures = 2; // skip a maximum of 1 empty slot for backload
        }
        int index = gis.findFirstDownloadSlot();
        int failures = 0;

        while (failures < maxFailures && index >= 0 ) {

            // Wait some random time to not to flood the node
            Mixed.waitRandom(2500);

            logger.info("Requesting index {} for date {}", index, dateStr);

            final boolean quicklyFailOnAdnf;
            final int maxRetries;
            if( Core.frostSettings.getBoolean(Settings.FCP2_QUICKLY_FAIL_ON_ADNF) ) {
                quicklyFailOnAdnf = true;
                maxRetries = 2;
            } else {
                // default
                quicklyFailOnAdnf = false;
                maxRetries = -1;
            }

            final String downKey = requestKey + index + ".xml";
            final GlobalFileDownloaderResult result = GlobalFileDownloader.downloadFile(downKey, FcpHandler.MAX_MESSAGE_SIZE_07, maxRetries);

            if( result == null ) {
                // download failed.
                if( gis.isDownloadIndexBehindLastSetIndex(index) ) {
                    // we stop if we tried maxFailures indices behind the last known index
                    failures++;
                }
                // next loop we try next index
                index = gis.findNextDownloadSlot(index);
                continue;
            }

            failures = 0;

            if( result.getErrorCode() == GlobalFileDownloaderResult.ERROR_EMPTY_REDIRECT ) {
                if( quicklyFailOnAdnf ) {
                    logger.debug("FileRequestsThread.downloadDate: Index {} got ADNF, will never try index again.", index);
                } else {
                    logger.debug("FileRequestsThread.downloadDate: Skipping index {} for now, will try again later.", index);
                }
                if( quicklyFailOnAdnf ) {
                    // don't try again
                    gis.setDownloadSlotUsed(index);
                    IndexSlotsStorage.inst().storeSlot(gis); // remember each progress
                }
                // next loop we try next index
                index = gis.findNextDownloadSlot(index);
                continue;
            }

            // downloaded something, mark it
            gis.setDownloadSlotUsed(index);
            // next loop we try next index
            index = gis.findNextDownloadSlot(index);

            if( result.getErrorCode() == GlobalFileDownloaderResult.ERROR_FILE_TOO_BIG ) {
                logger.error("FileRequestsThread.downloadDate: Dropping index {}, FILE_TOO_BIG.", index);
            } else {
                // process results
                final File downloadedFile = result.getResultFile();

                final FileRequestFileContent content = FileRequestFile.readRequestFile(downloadedFile);
                downloadedFile.delete();
                FileRequestsManager.processReceivedRequests(content);
            }

            // downloaded file was processed, store slot
            IndexSlotsStorage.inst().storeSlot(gis);
        }
    }

    @Override
    public void run() {

        final int maxAllowedExceptions = 5;
        int occuredExceptions = 0;

        // 2 times after startup we download full backload, then only 1 day backward
        int downloadFullBackloadCount = 2;

        while( true ) {

            // +1 for today
            int downloadBack;
            if( downloadFullBackloadCount > 0 ) {
                downloadBack = 1 + Core.frostSettings.getInteger(Settings.MAX_FILELIST_DOWNLOAD_DAYS);
                downloadFullBackloadCount--;
            } else {
                downloadBack = 2; // today and yesterday only
            }

            try {
				final OffsetDateTime nowDate = OffsetDateTime.now(DateFun.getTimeZone());
                for (int i=0; i < downloadBack; i++) {
                    boolean isForToday;
                    if( i == 0 ) {
                        isForToday = true; // upload own keys today only
                    } else {
                        isForToday = false;
                    }

					final OffsetDateTime localDate = nowDate.minusDays(i);
					final String dateStr = DateFun.FORMAT_DATE.format(localDate);
					final long date = DateFun.toStartOfDayInMilli(localDate);

                    final IndexSlot gis = IndexSlotsStorage.inst().getSlotForDate(
                            IndexSlotsStorage.REQUESTS, date);

                    logger.debug("FileRequestsThread: starting download for {}", dateStr);
                    // download file pointer files for this date
                    if( !isInterrupted() ) {
                        downloadDate(dateStr, gis, isForToday);
                    }

                    // for today, maybe upload a file pointer file
                    if( !isInterrupted() && isForToday ) {
                        try {
                            logger.debug("FileRequestsThread: starting upload for {}", dateStr);
                            uploadRequestFile(dateStr, gis);
                        } catch(final Throwable t) {
                            logger.error("Exception catched during uploadRequestFile()", t);
                        }
                    }

                    if( isInterrupted() ) {
                        break;
                    }
                }
            } catch (final Throwable e) {
                logger.error("Exception catched", e);
                occuredExceptions++;
            }

            if( occuredExceptions > maxAllowedExceptions ) {
                logger.error("Stopping FileRequestsThread because of too much exceptions");
                break;
            }
            if( isInterrupted() ) {
                break;
            }

            logger.debug("FileRequestsThread: sleeping 10 minutes");
            Mixed.wait(sleepTime); // sleep 10 minutes
        }
    }
}
