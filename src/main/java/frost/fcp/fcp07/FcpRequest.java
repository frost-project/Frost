/*
  FcpRequest.java / Frost
  Copyright (C) 2003  Jan-Thomas Czornack <jantho@users.sourceforge.net>

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
package frost.fcp.fcp07;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.fcp.DataNotFoundException;
import frost.fcp.FcpResultGet;
import frost.fcp.FcpToolsException;
import frost.fileTransfer.download.FrostDownloadItem;
import frost.util.FileAccess;

/**
 * Requests a key from freenet
 */
// while requesting / inserting, show chunks left to try (incl. trying chunks) -> Warte (9) / 18% (9)
public class FcpRequest {

	private static final Logger logger = LoggerFactory.getLogger(FcpRequest.class);

	final static boolean DEBUG = true;

    /**
     * getFile retrieves a file from Freenet. It does detect if this file is a redirect, a splitfile or
     * just a simple file. It checks the size for the file and returns false if sizes do not match.
     * Size is ignored if it is NULL
     *
     * @param key The key to retrieve. All to Freenet known key formats are allowed (passed to node via FCP).
     * @param size Size of the file in bytes. Is ignored if not an integer value or -1 (splitfiles do not need this setting).
     * @param target Target path
     * @param htl request htl
     * @param doRedirect If true, getFile redirects if possible and downloads the file it was redirected to.
     * @return True if download was successful, else false.
     */
    public static FcpResultGet getFile(
            final int type,
            final String key,
            final Long size,
            final File target,
            final int maxSize,
            final int maxRetries,
            final boolean createTempFile,
            final FrostDownloadItem dlItem)
    {
        File tempFile = null;
        if( createTempFile ) {
            tempFile = FileAccess.createTempFile("getFile_", ".tmp");
        } else {
            tempFile = new File( target.getPath() + ".tmp" );
        }

        // First we just download the file, not knowing what lies ahead
        final FcpResultGet results = getKey(type, key, tempFile, maxSize, maxRetries, dlItem);

        if( results.isSuccess() ) {

            // If the target file exists, we remove it
            if( target.isFile() ) {
                target.delete();
            }

            final boolean wasOK = tempFile.renameTo(target);
            if( wasOK == false ) {
               logger.error("Could not move file '{}' to '{}'.", tempFile.getPath(), target.getPath());
               logger.error("Maybe the locations are on different filesystems where a move is not allowed.");
               logger.error("Please try change the location of 'temp.dir' in the frost.ini file, and copy the file to a save location by yourself.");
               return FcpResultGet.RESULT_FAILED;
            }
        } else {
            // if we reach here, the download was NOT successful in any way
            tempFile.delete();
        }
        return results;
    }

    // used by getFile
    private static FcpResultGet getKey(
            final int type,
            final String key,
            final File target,
            final int maxSize,
            final int maxRetries,
            final FrostDownloadItem dlItem)
    {
        if( key == null || key.length() == 0 || key.startsWith("null") ) {
            logger.error("FcpRequest(07).getKey(): KEY IS NULL!");
            return FcpResultGet.RESULT_FAILED;
        }

        FcpConnection connection;
        try {
            connection = FcpFactory.getFcpConnectionInstance();
        } catch (final ConnectException e1) {
            connection = null;
        }

        FcpResultGet results = null;

        if( connection != null ) {
            int tries = 0;
            final int maxtries = 3;
            while( tries < maxtries ) {
                try {
                    results = connection.getKeyToFile(type, key, target, maxSize, maxRetries, dlItem);
                    break;
                } catch( final ConnectException e ) {
                    tries++;
                    continue;
                } catch( final DataNotFoundException ex ) { // frost.FcpTools.DataNotFoundException
                    // do nothing, data not found is usual ...
					logger.info("FcpRequest.getKey(1): DataNotFoundException (usual if not found)", ex);
                    break;
                } catch( final FcpToolsException e ) {
					logger.error("FcpRequest.getKey(1): FcpToolsException", e);
                    break;
                } catch( final IOException e ) {
					logger.error("FcpRequest.getKey(1): IOException", e);
                    break;
                }
            }
        }

        String printableKey = null;
        if( DEBUG ) {
            String keyPrefix = "";
            if( key.indexOf("@") > -1 ) {
                keyPrefix = key.substring(0, key.indexOf("@")+1);
            }
            String keyUrl = "";
            if( key.indexOf("/") > -1 ) {
                keyUrl = key.substring(key.indexOf("/"));
            }
            printableKey = new StringBuilder().append(keyPrefix)
                                             .append("...")
                                             .append(keyUrl).toString();
        }

        logger.debug("getKey: file = '{}' ; len = {}", target.getPath(), target.length());

        if( results == null ) {
            // paranoia
            results = FcpResultGet.RESULT_FAILED;
            logger.debug("getKey - Failed, result=null");
        } else if( results.isSuccess() && target.length() > 0 ) {
            logger.debug("getKey - Success: {}", printableKey);
        } else {
            target.delete();
            logger.debug("getKey - Failed: {}; rc = {}; isFatal = {}", printableKey, results.getReturnCode(), results.isFatal());
        }
        return results;
    }
}
