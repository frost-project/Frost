/*
  SearchThread.java / Frost
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.Core;
import frost.Settings;
import frost.fileTransfer.FrostFileListFileObject;
import frost.fileTransfer.FrostFileListFileObjectOwner;
import frost.identities.Identity;
import frost.storage.FileListCallback;
import frost.storage.perst.filelist.FileListStorage;
import frost.util.TextSearchFun;

class SearchThread extends Thread implements FileListCallback {

	private static final Logger logger = LoggerFactory.getLogger(SearchThread.class);

    private SearchParameters searchParams;

	private List<String> audioExtension;
	private List<String> videoExtension;
	private List<String> imageExtension;
	private List<String> documentExtension;
	private List<String> executableExtension;
	private List<String> archiveExtension;

    private int allFileCount;
    private int maxSearchResults;

    private SearchTable searchTable;

    private boolean isCancelRequested = false;
    private boolean isMaximumSearchResultsReached = false;

    private SearchPanel.ProxyPanel tabComponent;

    private boolean isCancelRequested() {
        return isCancelRequested;
    }
    public void requestCancel() {
        isCancelRequested = true;
    }

    private boolean isMaximumSearchResultsReached() {
        return isMaximumSearchResultsReached;
    }
    public void maximumSearchResultsReached() {
        isMaximumSearchResultsReached = true;
    }

    private String lowerCase(String s) {
        if( s == null ) {
            return "";
        } else {
            return s.toLowerCase();
        }
    }

    /**
     * Check search options, step 1.
     */
    private boolean searchFile1(FrostFileListFileObject fo) {

        // check if file has a key
        if( searchParams.isHideFilesWithoutChkKey() ) {
            if( fo.getKey() == null || fo.getKey().length() == 0 ) {
                return false;
            }
        }

        // hideFiles: show file if no bad/check/observe owner; or if at least 1 owner is good/observe
        if( searchParams.isHideBadUserFiles()
                || searchParams.isHideCheckUserFiles()
                || searchParams.isHideObserveUserFiles())
        {
            boolean accept = true;
            for( Iterator<FrostFileListFileObjectOwner> i = fo.getFrostFileListFileObjectOwnerIterator(); i.hasNext(); ) {
                FrostFileListFileObjectOwner ob = i.next();
                if( ob.getOwner() != null ) {
                    Identity id = Core.getIdentitiesManager().getIdentity(ob.getOwner());
                    if (id != null ) {

                        if( id.isBAD() && searchParams.isHideBadUserFiles() ) {
                            accept = false; // dont break, maybe there is a good
                        } else if( id.isCHECK() && searchParams.isHideCheckUserFiles() ) {
                            accept = false; // dont break, maybe there is a good
                        } else if( id.isOBSERVE() && searchParams.isHideObserveUserFiles() ) {
                            accept = false; // dont break, maybe there is a good
                        }

                        if( id.isGOOD() || (id.isOBSERVE() && !searchParams.isHideObserveUserFiles()) ) {
                            accept = true;
                            break; // break, one GOOD is enough to accept
                        }
                    }
                }
            }
            if( !accept ) {
                return false;
            }
        }

                // check file extension. if extension of ONE file is ok the file matches
        if( searchParams.getExtensions() != SearchParameters.EXTENSIONS_ALL ) {
            boolean accept = false;
            for( Iterator<FrostFileListFileObjectOwner> i = fo.getFrostFileListFileObjectOwnerIterator(); i.hasNext(); ) {
                FrostFileListFileObjectOwner ob = i.next();
                String name = lowerCase(ob.getName());
                // check for search type
                if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_AUDIO ) {
                    accept = checkType(audioExtension, name);
                } else if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_VIDEO ) {
                    accept = checkType(videoExtension, name);
                } else if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_IMAGES ) {
                    accept = checkType(imageExtension, name);
                } else if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_DOCUMENTS ) {
                    accept = checkType(documentExtension, name);
                } else if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_ARCHIVES ) {
                    accept = checkType(archiveExtension, name);
                } else if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_EXECUTABLES ) {
                    accept = checkType(executableExtension, name);
                }
                if( accept == true ) {
                    break; // break, one correct extension is enough to accept
                }
            }
            if( !accept ) {
                return false;
            }
        }

        return true; // accepted
    }

    /**
     * Check search options, step 2.
     */
    private boolean searchFile2(FrostFileListFileObject fo) {
        if( searchParams.isSimpleSearch() ) {
            return (searchFile2NotStringsSimple(fo) && searchFile2StringsSimple(fo));
        } else {
            return (searchFile2NotStringsAdvanced(fo) && searchFile2StringsAdvanced(fo));
        }
    }

    /**
     * Check all NOT strings, if a not string occurs for a file it is not accepted.
     */
    private boolean searchFile2NotStringsAdvanced(FrostFileListFileObject fo) {

        if( searchParams.getNotName().isEmpty()
                && searchParams.getNotComment().isEmpty()
                && searchParams.getNotKeyword().isEmpty()
                && searchParams.getNotOwner().isEmpty() )
        {
            return true; // no not strings given
        }

        for( Iterator<FrostFileListFileObjectOwner> i = fo.getFrostFileListFileObjectOwnerIterator(); i.hasNext(); ) {
            FrostFileListFileObjectOwner ob = i.next();

            // check notName
            if( TextSearchFun.containsAnyString(lowerCase(ob.getName()), searchParams.getNotName()) ) {
                return false;
            }
            // check notComment
            if( TextSearchFun.containsAnyString(lowerCase(ob.getComment()), searchParams.getNotComment()) ) {
                return false;
            }
            // check notKeyword
            if( TextSearchFun.containsAnyString(lowerCase(ob.getKeywords()), searchParams.getNotKeyword()) ) {
                return false;
            }
            // check notOwner
            if( TextSearchFun.containsAnyString(lowerCase(ob.getOwner()), searchParams.getNotOwner()) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * If a NOT string occurs inside name, comment, keyword then do not accept this file.
     */
    private boolean searchFile2NotStringsSimple(FrostFileListFileObject fo) {

        if( searchParams.getSimpleSearchNotStrings().isEmpty() ) {
            return true;
        }

        for( Iterator<FrostFileListFileObjectOwner> i = fo.getFrostFileListFileObjectOwnerIterator(); i.hasNext(); ) {
            FrostFileListFileObjectOwner ob = i.next();

            String name = lowerCase(ob.getName());
            String comment = lowerCase(ob.getComment());
            String keyword = lowerCase(ob.getKeywords());

            for(int x=0; x < searchParams.getSimpleSearchNotStrings().size(); x++ ) {
                String notString = searchParams.getSimpleSearchNotStrings().get(x);
                if( name.indexOf(notString) > -1 ) {
                    return false;
                }
                if( comment.indexOf(notString) > -1 ) {
                    return false;
                }
                if( keyword.indexOf(notString) > -1 ) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Advanced string search: if the given string does not occur in the field (e.g. comment)
     * then the file is not accepted.
     */
    private boolean searchFile2StringsAdvanced(FrostFileListFileObject fo) {

        boolean nameFound = false;
        boolean commentFound = false;
        boolean keywordFound = false;
        boolean ownerFound = false;

        if( searchParams.getName().isEmpty() ) {
            nameFound = true;
        }
        if( searchParams.getComment().isEmpty() ) {
            commentFound = true;
        }
        if( searchParams.getKeyword().isEmpty() ) {
            keywordFound = true;
        }
        if( searchParams.getOwner().isEmpty() ) {
            ownerFound = true;
        }

        if (nameFound && commentFound && keywordFound && ownerFound) {
            return true; // find all
        }

        for( Iterator<FrostFileListFileObjectOwner> i = fo.getFrostFileListFileObjectOwnerIterator(); i.hasNext(); ) {
            FrostFileListFileObjectOwner ob = i.next();

            // then check for strings, if strings are given they must match

            // check name
            String name = lowerCase(ob.getName());
            if( !nameFound && name.length() > 0 ) {
                if( TextSearchFun.containsEachString(name, searchParams.getName()) ) {
                    nameFound = true;
                }
            }
            // check comment
            String comment = lowerCase(ob.getComment());
            if( !commentFound && comment.length() > 0 ) {
                if( TextSearchFun.containsEachString(comment, searchParams.getComment()) ) {
                    commentFound = true;
                }
            }
            // check keyword
            String keyword = lowerCase(ob.getKeywords());
            if( !keywordFound && keyword.length() > 0 ) {
                if( TextSearchFun.containsEachString(keyword, searchParams.getKeyword()) ) {
                    keywordFound = true;
                }
            }
            // check owner
            String owner = lowerCase(ob.getOwner());
            if( !ownerFound && owner.length() > 0 ) {
                if( TextSearchFun.containsEachString(owner, searchParams.getOwner()) ) {
                    ownerFound = true;
                }
            }

            // stop if all were found
            if (nameFound && commentFound && keywordFound && ownerFound) {
                return true;
            }
        }

        return (nameFound && commentFound && keywordFound && ownerFound);
    }

    /**
     * Simple string search: each given string must be found at least once in name, comment or keyword
     * of any owner.
     */
    private boolean searchFile2StringsSimple(FrostFileListFileObject fo) {

        if( searchParams.getSimpleSearchStrings().isEmpty() ) {
            return true; // find all
        }

        // mark all strings not found
        Map<String,Boolean> searchStrings = new HashMap<String,Boolean>();
        for(int x=0; x < searchParams.getSimpleSearchStrings().size(); x++) {
            String string = searchParams.getSimpleSearchStrings().get(x);
            searchStrings.put(string, Boolean.FALSE);
        }

        // success if all was found
        for( Iterator<FrostFileListFileObjectOwner> i=fo.getFrostFileListFileObjectOwnerIterator(); i.hasNext(); ) {
            FrostFileListFileObjectOwner ob = i.next();

            String name = lowerCase(ob.getName());
            String comment = lowerCase(ob.getComment());
            String keyword = lowerCase(ob.getKeywords());

            for(int x=0; x < searchParams.getSimpleSearchStrings().size(); x++) {
                String string = searchParams.getSimpleSearchStrings().get(x);
                if( name.indexOf(string) > -1 ) {
                    searchStrings.put(string, Boolean.TRUE);
                }
                if( comment.indexOf(string) > -1 ) {
                    searchStrings.put(string, Boolean.TRUE);
                }
                if( keyword.indexOf(string) > -1 ) {
                    searchStrings.put(string, Boolean.TRUE);
                }
            }
        }

        // finally check if all words were found
        boolean allFound = true;
        for( Boolean b : searchStrings.values() ) {
            if( b.booleanValue() == false ) {
                allFound = false;
                break;
            }
        }
        return allFound;
    }

    /**
     * Checks extension types
     * @param extension Array with acceptable extensions
     * @param filename Filename to be checked
     * @return True if file gets accepted, else false
     */
	private boolean checkType(List<String> extension, String filename) {
        boolean accepted = false;

		for (int i = 0; i < extension.size(); i++) {
			if (filename.endsWith(extension.get(i)))
                accepted = true;
        }

        return accepted;
    }

//    private boolean filterSearchResults(FrostFileListFileObject fo) {
//        if (request.indexOf("*age") != -1) {
//            int agePos = request.indexOf("*age");
//            int nextSpacePos = request.indexOf(" ", agePos);
//            if (nextSpacePos == -1) {
//                nextSpacePos = request.length();
//            }
//
//            int age = 1;
//            try {
//                age = Integer.parseInt(request.substring(agePos + 4, nextSpacePos));
//            } catch (NumberFormatException e) {
//                logger.warn("Did not recognice age, using default 1.");
//            }
//
//            long diffMillis = (long)age * 24L * 60L * 60L * 1000L;
//            long minDateMillis = currentDate.getTime() - diffMillis;
//
//            if( fo.getLastReceived() < minDateMillis ) {
//                return false; // older than age
//            }
//        }
//    }

    /**
     * Displays search results in search table
     */
    private void addSearchResult(FrostFileListFileObject fo) {

        allFileCount++;

        if (allFileCount > this.maxSearchResults) {
            logger.info("maxSearchResults reached ()!", maxSearchResults);
            maximumSearchResultsReached();
            return;
        }

        FrostSearchItem searchItem = new FrostSearchItem(fo);
        searchTable.addSearchItem(searchItem);
    }

    public boolean fileRetrieved(FrostFileListFileObject fo) {
        if( isMaximumSearchResultsReached() ) {
            return isMaximumSearchResultsReached();
        }
        if( searchFile1(fo) && searchFile2(fo) ) {
            addSearchResult(fo);
        }
        return isCancelRequested();
    }

    @Override
    public void run() {

        allFileCount = 0;
        logger.info(">>> Filesearch started...");

        // check file extension. if extension of ONE file is ok the file matches
		List<String> searchForExtensions = null;
        if( searchParams.getExtensions() != SearchParameters.EXTENSIONS_ALL ) {
            if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_AUDIO ) {
                searchForExtensions = audioExtension;
            } else if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_VIDEO ) {
                searchForExtensions = videoExtension;
            } else if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_IMAGES ) {
                searchForExtensions = imageExtension;
            } else if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_DOCUMENTS ) {
                searchForExtensions = documentExtension;
            } else if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_ARCHIVES ) {
                searchForExtensions = archiveExtension;
            } else if( searchParams.getExtensions() == SearchParameters.EXTENSIONS_EXECUTABLES ) {
                searchForExtensions = executableExtension;
            }
        }

        if( searchParams.isSimpleSearch() ) {
            FileListStorage.inst().retrieveFiles(
                    this,
                    searchParams.getSimpleSearchStrings(),
                    searchParams.getSimpleSearchStrings(),
                    searchParams.getSimpleSearchStrings(),
                    null,
                    searchForExtensions); // no owner search
        } else {
            FileListStorage.inst().retrieveFiles(
                    this,
                    searchParams.getName(),
                    searchParams.getComment(),
                    searchParams.getKeyword(),
                    searchParams.getOwner(),
                    searchForExtensions);
        }
        logger.info("<<< Filesearch finished");

        if( isCancelRequested() ) {
            searchTable.searchCancelled();
        } else {
            searchTable.searchFinished(tabComponent);
        }
    }

    /**Constructor*/
    public SearchThread(SearchParameters searchParams, SearchTable searchTable, SearchPanel.ProxyPanel tabComponent) {

        this.searchParams = searchParams;
        this.tabComponent = tabComponent;
        tabComponent.setSearchThread(this); // will notify this thread to stop if tab was closed
        this.searchTable = searchTable;

        audioExtension      = Core.frostSettings.getStringList(Settings.FILEEXTENSION_AUDIO);
        videoExtension      = Core.frostSettings.getStringList(Settings.FILEEXTENSION_VIDEO);
        documentExtension   = Core.frostSettings.getStringList(Settings.FILEEXTENSION_DOCUMENT);
        executableExtension = Core.frostSettings.getStringList(Settings.FILEEXTENSION_EXECUTABLE);
        archiveExtension    = Core.frostSettings.getStringList(Settings.FILEEXTENSION_ARCHIVE);
        imageExtension      = Core.frostSettings.getStringList(Settings.FILEEXTENSION_IMAGE);
        maxSearchResults    = Core.frostSettings.getInteger(Settings.SEARCH_MAX_RESULTS);

        if( maxSearchResults <= 0 ) {
            maxSearchResults = 10000; // default
        }
    }
}
