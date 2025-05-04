/*
  FilePointersThread.java / Frost
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
package frost.fileTransfer.filelist;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.fcp.FcpHandler;
import frost.fileTransfer.SharedFilesCHKKeyManager;
import frost.storage.perst.IndexSlot;
import frost.storage.perst.IndexSlotsStorage;
import frost.storage.perst.SharedFilesCHKKey;
import frost.transferlayer.GlobalFileDownloader;
import frost.transferlayer.GlobalFileDownloaderResult;
import frost.transferlayer.GlobalFileUploader;
import frost.util.DateFun;
import frost.util.FileAccess;
import frost.util.Mixed;

/**
 * This thread downloads the KSK pointer files for file sharing from the public indices.
 * Received KSK files contain CHK keys of the filelists, this CHK keys are inserted into
 * the database (another thread downloads them).
 * When an update is finished, the thread checks if there are pending CHK keys that must be
 * send, and sends them.
 * Finally the thread sleeps for some time and restarts to retrieve the pointer files.
 */
public class FilePointersThread extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(FilePointersThread.class);

    private static final int baseSleepTime = 15 * 60 * 1000;

    private final String keyPrefix;

    // one and only instance
    private static FilePointersThread instance = new FilePointersThread();

    private FilePointersThread() {
        final String fileBase = Core.frostSettings.getValue(Settings.FILE_BASE);
        keyPrefix = "KSK@frost/filepointers/" + fileBase + "-";
    }

    public static FilePointersThread getInstance() {
        return instance;
    }

    public boolean cancelThread() {
        return false;
    }

    /**
     * Returns true if no error occured.
     */
    private boolean uploadIndexFile(final String dateStr, final IndexSlot gis) throws Throwable {

        // get a list of CHK keys to send
        final List<SharedFilesCHKKey> sharedFileCHKkeys = SharedFilesCHKKeyManager.getCHKKeysToSend();
        if( sharedFileCHKkeys == null || sharedFileCHKkeys.size() == 0 ) {
            logger.info("FILEDN: No CHK keys to send.");
            return true;
        }

        // write a pointerfile to a tempfile
        List<String> tmpChkStringKeys = new ArrayList<String>(sharedFileCHKkeys.size());
        for( final SharedFilesCHKKey ck : sharedFileCHKkeys ) {
            tmpChkStringKeys.add( ck.getChkKey() );
        }

        final FilePointerFileContent content = new FilePointerFileContent(System.currentTimeMillis(), tmpChkStringKeys);

        final File tmpPointerFile = FileAccess.createTempFile("kskptr_", ".xml");
        tmpPointerFile.deleteOnExit();
        if( !FilePointerFile.writePointerFile(content, tmpPointerFile) ) {
            logger.error("FILEDN: Error writing the KSK pointer file.");
            return false;
        }

        tmpChkStringKeys.clear();
        tmpChkStringKeys = null;

        // Wait some random time to not to flood the node
        Mixed.waitRandom(2000);

        logger.info("FILEDN: Starting upload of pointer file containing {} CHK keys", sharedFileCHKkeys.size());

        final String insertKey = keyPrefix + dateStr + "-";
        logger.info("uploadIndexFile: Starting upload of pointer file containing {} CHK keys to {}...", sharedFileCHKkeys.size(), insertKey);
        final boolean wasOk = GlobalFileUploader.uploadFile(gis, tmpPointerFile, insertKey, ".xml", true);
        logger.info("uploadIndexFile: upload finished, wasOk = {}", wasOk);
        tmpPointerFile.delete();
        if( wasOk ) {
            SharedFilesCHKKeyManager.updateCHKKeysWereSuccessfullySent(sharedFileCHKkeys);
        }

        IndexSlotsStorage.inst().storeSlot(gis);

        return wasOk;
    }

    private void downloadDate(final String dateStr, final IndexSlot gis, final boolean isForToday) throws Throwable {

        // "KSK@frost/filelistpointer/2006.11.1-<index>.xml"
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
            Mixed.waitRandom(3000);

            logger.info("FILEDN: Requesting index {} for date {}", index, dateStr);

            final String downKey = requestKey + index + ".xml";
            logger.debug("FilePointersThread.downloadDate: requesting: {}", downKey);

            final boolean quicklyFailOnAdnf;
            final int maxRetries;
            if( Core.frostSettings.getBoolValue(Settings.FCP2_QUICKLY_FAIL_ON_ADNF) ) {
                quicklyFailOnAdnf = true;
                maxRetries = 2;
            } else {
                // default
                quicklyFailOnAdnf = false;
                maxRetries = -1;
            }

            final GlobalFileDownloaderResult result = GlobalFileDownloader.downloadFile(downKey, FcpHandler.MAX_MESSAGE_SIZE_07, maxRetries);

            if(  result == null ) {
                logger.warn("FilePointersThread.downloadDate: failure");
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
                    logger.warn("FilePointersThread.downloadDate: Index {} got ADNF, will never try index again.", index);
                } else {
                    logger.warn("FilePointersThread.downloadDate: Skipping index {} for now, will try again later.", index);
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
                logger.error("FilePointersThread.downloadDate: Dropping index , FILE_TOO_BIG.", index);
            } else {
                // process received data
                logger.debug("FilePointersThread.downloadDate: success");

                final File downloadedFile = result.getResultFile();

                FilePointerFileContent content = null;
                try {
                    content = FilePointerFile.readPointerFile(downloadedFile);
                } catch (final Exception e) {
                    logger.error("Invalid XML content: ", e);
                }

                logger.debug("readPointerFile: result: {}", content);
                downloadedFile.delete();
                SharedFilesCHKKeyManager.processReceivedCHKKeys(content);
            }

            IndexSlotsStorage.inst().storeSlot(gis); // remember each progress
        }
        logger.info("FilePointersThread.downloadDate: finished");
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
                downloadBack = 1 + Core.frostSettings.getIntValue(Settings.MAX_FILELIST_DOWNLOAD_DAYS);
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
                            IndexSlotsStorage.FILELISTS, date);

                    logger.debug("FilePointersThread: download for {}", dateStr);
                    // download file pointer files for this date
                    if( !isInterrupted() ) {
                        downloadDate(dateStr, gis, isForToday);
                    }

                    // for today, maybe upload a file pointer file
                    if( !isInterrupted() && isForToday ) {
                        try {
                            logger.debug("FilePointersThread: upload for {}", dateStr);
                            uploadIndexFile(dateStr, gis);
                        } catch(final Throwable t) {
                            logger.error("Exception during uploadIndexFile()", t);
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
                logger.error("Stopping FilePointersThread because of too much exceptions");
                break;
            }
            if( isInterrupted() ) {
                break;
            }

            // random sleeptime to anonymize our uploaded pointer files
            Mixed.waitRandom(baseSleepTime);
        }
    }
}
