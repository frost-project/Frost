/*
  Settings.java / Frost
  Copyright (C) 2001  Frost Project <jtcfrost.sourceforge.net>
  This file contributed by Stefan Majewski <e9926279@stud3.tuwien.ac.at>

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

package frost;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import frost.fileTransfer.FreenetPriority;
import frost.storage.ExitSavable;
import frost.storage.StorageException;
import frost.util.Convert;

/**
 * Read settings from frost.ini and store them.
 */
public class Settings implements ExitSavable {

	private static final Logger logger = LoggerFactory.getLogger(Settings.class);

	private static final Path appHome = Path.of(System.getProperty(AppHome.APP_HOME));

    private final File settingsFile;
    private final Hashtable<String,Object> settingsHash;
    private Hashtable<String,Object> defaults = null;
    private final String fs = System.getProperty("file.separator");
    private PropertyChangeSupport changeSupport = null;
    private Vector<SettingsUpdater> updaters = null;

	public static final String DOS_STOP_BOARD_UPDATES_WHEN_DOSED = "dos.stopBoardUpdatesWhenDosed";
	public static final String DOS_INVALID_SUBSEQUENT_MSGS_THRESHOLD = "dos.invalidSubsequentMessagesThreshold";

	public static final String MIGRATE_VERSION = "migrate.version";
	public static final String SETTINGS_VERSION = "settings.version";

	public static final String DB_CLEANUP_INTERVAL = "database.cleanup.interval";
	public static final String DB_CLEANUP_LASTRUN = "database.cleanup.lastRun";

	public static final String DB_CLEANUP_REMOVEOFFLINEFILEWITHKEY = "database.cleanup.removeOfflineFilesWithKey";
	public static final String DB_CLEANUP_OFFLINEFILESMAXDAYSOLD = "database.cleanup.offlineFilesMaxDaysOld";

	public static final String PERST_COMPACT_STORAGES = "perst.compactStorages";
	public static final String PERST_EXPORT_STORAGES = "perst.exportStorages";

	public static final String DIR_CONFIG = "config.dir";
	public static final String DIR_DOWNLOAD = "downloadDirectory";
	public static final String DOWNLOAD_MANAGER_RECENT_DOWNLOAD_DIR_PREFIX = "DownloadManager.recentDownloadDir.";
	public static final String EXEC_ON_DOWNLOAD = "downloadExec";
	public static final String EXEC_ON_UPLOAD = "uploadExec";
	public static final String DIR_LAST_USED = "lastUsedDirectory";
	public static final String DIR_TEMP = "temp.dir";
	public static final String DIR_LOCALDATA = "localdata.dir";
	public static final String DIR_STORE = "store.dir";
	public static final String DIR_HELP = "help.dir";

	public static final String FREENET_VERSION = "freenetVersion";

	public static final String LANGUAGE_LOCALE = "locale";
	public static final String LANGUAGE_LOCALE_EXTERNAL = "localeExternal";

	public static final String CONFIRM_MARK_ALL_MSGS_READ = "confirm.markAllMessagesRead";
	public static final String CONFIRM_FILESHARING_IS_ENABLED = "confirm.fileSharingIsEnabled";

	public static final String LOOK_AND_FEEL = "ChoosenLookAndFeel";

	public static final String STORAGE_STORE_INVALID_MESSAGES = "storage.storeInvalidMessages";
	public static final String STORAGE_STORE_SENT_MESSAGES = "storage.storeSentMessages";

//    public static final String COMPACT_DBTABLES = "compactDatabaseTables";

	// not yet available in options dialog; all sizes in KiB
	public static final String PERST_PAGEPOOLSIZE_FILES = "perst.pagepoolsizeKiB.files";
	public static final String PERST_PAGEPOOLSIZE_INDEXSLOTS = "perst.pagepoolsizeKiB.indexslots";
	public static final String PERST_PAGEPOOLSIZE_SHAREDFILESCHKKEYS = "perst.pagepoolsizeKiB.sharedfilechkkeys";
	public static final String PERST_PAGEPOOLSIZE_FILELIST = "perst.pagepoolsizeKiB.filelist";
	public static final String PERST_PAGEPOOLSIZE_IDENTITIES = "perst.pagepoolsizeKiB.identities";
	public static final String PERST_PAGEPOOLSIZE_MESSAGEARCHIVE = "perst.pagepoolsizeKiB.messagearchive";
	public static final String PERST_PAGEPOOLSIZE_MESSAGES = "perst.pagepoolsizeKiB.messages";
	public static final String PERST_PAGEPOOLSIZE_MESSAGECONTENTS = "perst.pagepoolsizeKiB.messagecontents";
	public static final String PERST_PAGEPOOLSIZE_TRACKDOWNLOADKEYS = "perst.pagepoolsizeKiB.trackdownloadkeys";

	public static final String FREENET_FCP_ADDRESS = "availableNodes";
	public static final String FCP2_USE_DDA = "fcp2.useDDA";
	public static final String FCP2_USE_PERSISTENCE = "fcp2.usePersistence";
	public static final String FCP2_USE_ONE_CONNECTION_FOR_MESSAGES = "fcp2.useOneConnectionForMessages";

	public static final String FCP2_DEFAULT_PRIO_MESSAGE_UPLOAD = "fcp2.defaultPriorityMessageUpload";
	public static final String FCP2_DEFAULT_PRIO_MESSAGE_DOWNLOAD = "fcp2.defaultPriorityMessageDownload";
	public static final String FCP2_DEFAULT_PRIO_FILE_UPLOAD = "fcp2.defaultPriorityFileUpload";
	public static final String FCP2_DEFAULT_PRIO_FILE_DOWNLOAD = "fcp2.defaultPriorityFileDownload";

	// not in gui dialog!
	public static final String FCP2_SET_TARGET_FILENAME_FOR_MANUAL_PUT = "fcp2.setTargetfilenameForManualPut";

	// if true, Frost enforces its priority setting, external changes are reversed
	public static final String FCP2_ENFORCE_FROST_PRIO_FILE_UPLOAD = "fcp2.enforceFrostPriorityFileUpload";
	public static final String FCP2_ENFORCE_FROST_PRIO_FILE_DOWNLOAD = "fcp2.enforceFrostPriorityFileDownload";

	// If true, we start the requests with a FCP2:MaxRetries of 2 and never try them
	// again. (use during DoS attacks)
	// If false, we start the requests with a FCP2:MaxRetries of 1 and try them
	// again during each board update. (default)
	public static final String FCP2_QUICKLY_FAIL_ON_ADNF = "fcp2.quicklyFailOnAdnf"; // not in gui dialog!

	// If true, Frost will use early encode when uploading new messages
	public static final String FCP2_USE_EARLY_ENCODE = "fcp2.useEarlyEncode";

	public static final String AUTO_SAVE_INTERVAL = "autoSaveInterval";
	public static final String AUTO_SAVE_LOCAL_IDENTITIES = "autoSaveLocalIdentities";
	public static final String FILESHARING_DISABLE = "disableFilesharing";
	public static final String FILESHARING_IGNORE_CHECK_AND_BELOW = "filesharing.ignoreCheckAndBelow";
	public static final String REMEMBER_SHAREDFILE_DOWNLOADED = "rememberSharedFileDownloaded";
	public static final String DOWNLOADING_ACTIVATED = "downloadingActivated";

	public static final String BOARD_TREE_FONT_NAME = "boardTreeFontName";
	public static final String BOARD_TREE_FONT_SIZE = "boardTreeFontSize";
	public static final String BOARD_TREE_FONT_STYLE = "boardTreeFontStyle";
	public static final String FILE_LIST_FONT_NAME = "fileListFontName";
	public static final String FILE_LIST_FONT_SIZE = "fileListFontSize";
	public static final String FILE_LIST_FONT_STYLE = "fileListFontStyle";
	public static final String MESSAGE_BODY_FONT_NAME = "messageBodyFontName";
	public static final String MESSAGE_BODY_FONT_SIZE = "messageBodyFontSize";
	public static final String MESSAGE_BODY_FONT_STYLE = "messageBodyFontStyle";
	public static final String MESSAGE_LIST_FONT_NAME = "messageListFontName";
	public static final String MESSAGE_LIST_FONT_SIZE = "messageListFontSize";
	public static final String MESSAGE_LIST_FONT_STYLE = "messageListFontStyle";
	public static final String SHOW_DELETED_MESSAGES = "showDeletedMessages";
	public static final String SILENTLY_RETRY_MESSAGES = "silentlyRetryMessages";
	public static final String HANDLE_OWN_MESSAGES_AS_NEW_DISABLED = "handleOwnMessagesAsNewDisabled";
	public static final String SHOW_OWN_MESSAGES_AS_ME_DISABLED = "showOwnMessagesAsMeDisabled";
	public static final String SORT_THREADROOTMSGS_ASCENDING = "sortThreadRootMessagesAscending";

	public static final String ALWAYS_DOWNLOAD_MESSAGES_BACKLOAD = "alwaysDownloadMessagesBackload";

	public static final String UPLOAD_MAX_RETRIES = "uploadMaxRetries";
	public static final String UPLOAD_WAITTIME = "uploadRetriesWaitTime";
	public static final String UPLOAD_MAX_THREADS = "uploadThreads";
	public static final String UPLOAD_REMOVE_NOT_EXISTING_FILES = "uploadRemoveNotExistingFiles";

	public static final String DOWNLOAD_MAX_THREADS = "downloadThreads";
	public static final String DOWNLOAD_MAX_RETRIES = "downloadMaxRetries";
	public static final String DOWNLOAD_WAITTIME = "downloadWaittime";
	public static final String DOWNLOAD_REMOVE_FINISHED = "removeFinishedDownloads";
	public static final String UPLOAD_REMOVE_FINISHED = "removeFinishedUploads";

	public static final String GQ_SHOW_EXTERNAL_ITEMS_DOWNLOAD = "showExternalGlobalQueueDownloads";
	public static final String GQ_SHOW_EXTERNAL_ITEMS_UPLOAD = "showExternalGlobalQueueUploads";

	public static final String COMPRESS_UPLOADS = "compressUploads";

	public static final String SAVE_SORT_STATES = "saveSortStates";
	public static final String MSGTABLE_MULTILINE_SELECT = "messageTableMultilineSelect";
	public static final String MSGTABLE_SCROLL_HORIZONTAL = "messageTableScrollHorizontal";
	public static final String MSGTABLE_SHOW_COLLAPSED_THREADS = "messageTableShowCollapsedThreads";
	public static final String MSGTABLE_EXPAND_ROOT_CHILDREN = "messageTableExpandRootChildren";
	public static final String MSGTABLE_EXPAND_UNREAD_THREADS = "messageTableExpandUnreadThreads";
	public static final String MSGTABLE_DOUBLE_CLICK_SHOWS_MESSAGE = "MessagePanel.doubleClickShowsMessage";
	public static final String SHOW_BOARDDESC_TOOLTIPS = "showBoardDescriptionTooltips";
	public static final String SHOW_BOARD_UPDATED_COUNT = "showBoardUpdatedCount";
	public static final String PREVENT_BOARDTREE_REORDERING = "preventBoardTreeReordering";
	public static final String SHOW_BOARDTREE_FLAGGEDSTARRED_INDICATOR = "showBoardtreeFlaggedStarredIndicators";
	public static final String SHOW_BOARD_UPDATE_VISUALIZATION = Settings.BOARD_UPDATE_VISUALIZATION_ENABLED;
	public static final String DISABLE_SPLASHSCREEN = "disableSplashScreen";
	public static final String SHOW_SYSTRAY_ICON = "showSystrayIcon";
	public static final String MINIMIZE_TO_SYSTRAY = "minimizeToSystray";

	public static final String MAX_MESSAGE_DISPLAY = "maxMessageDisplay";
	public static final String MAX_MESSAGE_DOWNLOAD = "maxMessageDownload";
	public static final String MESSAGE_UPLOAD_DISABLED = "messageUploadDisabled";

	public static final String SEARCH_MAX_RESULTS = "maxSearchResults";
	public static final String SEARCH_HIDE_BAD = "hideBadFiles";
	public static final String SEARCH_HIDE_CHECK = "hideCheckFiles";
	public static final String SEARCH_HIDE_OBSERVE = "hideObserveFiles";
	public static final String SEARCH_HIDE_FILES_WITHOUT_CHK = "hideFilesWithoutChk";

	public static final String BOARDLIST_LAST_SELECTED_BOARD = "tofTreeSelectedRow";

	public static final String MESSAGE_BLOCK_SUBJECT = "blockMessage";
	public static final String MESSAGE_BLOCK_SUBJECT_ENABLED = "blockMessageChecked";
	public static final String MESSAGE_BLOCK_BODY = "blockMessageBody";
	public static final String MESSAGE_BLOCK_BODY_ENABLED = "blockMessageBodyChecked";
	public static final String MESSAGE_BLOCK_BOARDNAME = "blockMessageBoard";
	public static final String MESSAGE_BLOCK_BOARDNAME_ENABLED = "blockMessageBoardChecked";

	public static final String JUNK_HIDE_JUNK_MESSAGES = "junk.hideJunkMessages";
	public static final String JUNK_MARK_JUNK_IDENTITY_BAD = "junk.markJunkIdentityBad";

	public static final String MESSAGE_HIDE_OBSERVE = "hideObserveMessages";
	public static final String MESSAGE_HIDE_CHECK = "hideCheckMessages";
	public static final String MESSAGE_HIDE_BAD = "hideBadMessages";
	public static final String MESSAGE_HIDE_UNSIGNED = "signedOnly";
	public static final String MESSAGE_HIDE_COUNT = "hideMessageCount";
	public static final String MESSAGE_HIDE_COUNT_EXCLUDE_PRIVATE = "hideMessageCountExcludePrivate";

	public static final String KNOWNBOARDS_BLOCK_FROM_OBSERVE = "blockBoardsFromObserve";
	public static final String KNOWNBOARDS_BLOCK_FROM_CHECK = "blockBoardsFromCheck";
	public static final String KNOWNBOARDS_BLOCK_FROM_BAD = "blockBoardsFromBad";
	public static final String KNOWNBOARDS_BLOCK_FROM_UNSIGNED = "blockBoardsFromUnsigned";

	public static final String BOARD_AUTOUPDATE_ENABLED = "automaticUpdate";
	public static final String BOARD_AUTOUPDATE_CONCURRENT_UPDATES = "automaticUpdate.concurrentBoardUpdates";
	public static final String BOARD_AUTOUPDATE_MIN_INTERVAL = "automaticUpdate.boardsMinimumUpdateInterval";

	public static final String BOARD_UPDATE_VISUALIZATION_ENABLED = "boardUpdateVisualization";
	public static final String BOARD_UPDATE_VISUALIZATION_BGCOLOR_SELECTED = "boardUpdatingSelectedBackgroundColor";
	public static final String BOARD_UPDATE_VISUALIZATION_BGCOLOR_NOT_SELECTED = "boardUpdatingNonSelectedBackgroundColor";

	public static final String SHOW_THREADS = "MessagePanel.showThreads";

	public static final String INDICATE_LOW_RECEIVED_MESSAGES = "MessagePanel.indicateLowReceivedMessages";
	public static final String INDICATE_LOW_RECEIVED_MESSAGES_COUNT_RED = "MessagePanel.indicateLowReceivedMessages.redCount";
	public static final String INDICATE_LOW_RECEIVED_MESSAGES_COUNT_LIGHTRED = "MessagePanel.indicateLowReceivedMessages.lightRedCount";

	public static final String SHOW_UNREAD_ONLY = "MessagePanel.showUnreadOnly";
	public static final String SHOW_FLAGGED_ONLY = "MessagePanel.showFlaggedOnly";
	public static final String SHOW_STARRED_ONLY = "MessagePanel.showStarredOnly";

	public static final String MSGTABLE_MSGTEXT_DIVIDER_LOCATION = "MessagePanel.msgTableAndMsgTextSplitpaneDividerLocation";

	public static final String BOARD_LAST_USER_PREFIX = "userName.";

	public static final String SEARCH_MESSAGES_DIALOG_LAST_FRAME_MAXIMIZED = "searchMessagesDialog.lastFrameMaximized";
	public static final String SEARCH_MESSAGES_DIALOG_LAST_FRAME_HEIGHT = "searchMessagesDialog.lastFrameHeight";
	public static final String SEARCH_MESSAGES_DIALOG_LAST_FRAME_WIDTH = "searchMessagesDialog.lastFrameWidth";
	public static final String SEARCH_MESSAGES_DIALOG_LAST_FRAME_POS_X = "searchMessagesDialog.lastFramePosX";
	public static final String SEARCH_MESSAGES_DIALOG_LAST_FRAME_POS_Y = "searchMessagesDialog.lastFramePosY";

	public static final String HELP_BROWSER_DIALOG_LAST_FRAME_MAXIMIZED = "helpBrowser.lastFrameMaximized";
	public static final String HELP_BROWSER_DIALOG_LAST_FRAME_HEIGHT = "helpBrowser.lastFrameHeight";
	public static final String HELP_BROWSER_DIALOG_LAST_FRAME_WIDTH = "helpBrowser.lastFrameWidth";
	public static final String HELP_BROWSER_DIALOG_LAST_FRAME_POS_X = "helpBrowser.lastFramePosX";
	public static final String HELP_BROWSER_DIALOG_LAST_FRAME_POS_Y = "helpBrowser.lastFramePosY";

	public static final String MESSAGE_TREE_TABLE_TABLE_INDEX_MODEL_COLUMN_PREFIX = "MessageTreeTable.tableindex.modelcolumn.";
	public static final String MESSAGE_TREE_TABLE_COLUMN_WIDTH_MODEL_COLUMN_PREFIX = "MessageTreeTable.columnwidth.modelcolumn.";

	public static final String SEARCH_MESSAGE_TABLE_TABLE_INDEX_MODEL_COLUMN_PREFIX = "messagetable.tableindex.modelcolumn.";
	public static final String SEARCH_MESSAGE_TABLE_COLUMN_WIDTH_MODEL_COLUMN_PREFIX = "messagetable.columnwidth.modelcolumn.";

	public static final String FREETALK_SHOW_TAB = "Freetalk.showTab";
	public static final String FREETALK_LOGIN_USERID = "Freetalk.loginUserId";
	public static final String FREETALK_BOARD_LAST_USER_PREFIX = "freetalkAddress.";
	public static final String FREETALK_TAB_TREE_AND_TABBED_PANE_SPLITPANE_DIVIDER_LOCATION = "FreetalkTab.treeAndTabbedPaneSplitpaneDividerLocation";

	public static final String FREETALK_SHOW_KEYS_AS_HYPERLINKS = "FreetalkMessagePanel.showKeysAsHyperlinks";
	public static final String FREETALK_SHOW_SMILEYS = "FreetalkMessagePanel.showSmileys";
	public static final String FREETALK_SHOW_THREADS = "FreetalkMessagePanel.showThreads";
	public static final String FREETALK_SHOW_UNREAD_ONLY = "FreetalkMessagePanel.showUnreadOnly";
	public static final String FREETALK_MSGTABLE_MSGTEXT_DIVIDER_LOCATION = "FreetalkMessagePanel.msgTableAndMsgTextSplitpaneDividerLocation";

	public static final String SHOW_COLORED_ROWS = "showColoredRows";
	public static final String SHOW_SMILEYS = "showSmileys";
	public static final String SHOW_KEYS_AS_HYPERLINKS = "showKeysAsHyperlinks";
	public static final String MESSAGE_BODY_ANTIALIAS = "messageBodyAA";

	public static final String ALTERNATE_EDITOR_ENABLED = "useAltEdit";
	public static final String ALTERNATE_EDITOR_COMMAND = "altEdit";

	public static final String FILE_BASE = "fileBase";
	public static final String MESSAGE_BASE = "messageBase";

	public static final String MESSAGE_EXPIRE_DAYS = "messageExpireDays";
	public static final String MESSAGE_EXPIRATION_MODE = "messageExpirationMode";
	public static final String ARCHIVE_KEEP_FLAGGED_AND_STARRED = "archiveKeepFlaggedOrStarredMessages";
	public static final String ARCHIVE_KEEP_UNREAD = "archiveKeepUnreadMessages";

	public static final String MIN_DAYS_BEFORE_FILE_RESHARE = "minDaysBeforeFileReshare";
	public static final String MAX_FILELIST_DOWNLOAD_DAYS = "fileListDownloadDays";

	public static final String FILE_LIST_FILE_DETAILS_DIALOG_HEIGHT = "FileListFileDetailsDialog.height";
	public static final String FILE_LIST_FILE_DETAILS_DIALOG_WIDTH = "FileListFileDetailsDialog.width";

	public static final String FILEEXTENSION_AUDIO = "audioExtension";
	public static final String FILEEXTENSION_VIDEO = "videoExtension";
	public static final String FILEEXTENSION_DOCUMENT = "documentExtension";
	public static final String FILEEXTENSION_EXECUTABLE = "executableExtension";
	public static final String FILEEXTENSION_ARCHIVE = "archiveExtension";
	public static final String FILEEXTENSION_IMAGE = "imageExtension";

	public static final String LAST_USED_FROMNAME = "userName";

	public static final String MAINFRAME_LAST_WIDTH = "lastFrameWidth";
	public static final String MAINFRAME_LAST_HEIGHT = "lastFrameHeight";
	public static final String MAINFRAME_LAST_X = "lastFramePosX";
	public static final String MAINFRAME_LAST_Y = "lastFramePosY";
	public static final String MAINFRAME_LAST_MAXIMIZED = "lastFrameMaximized";
	public static final String MAINFRAME_TREE_AND_TABBED_PANE_SPLIT_PANE_DIVIDER_LOCATION = "MainFrame.treeAndTabbedPaneSplitpaneDividerLocation";

	public static final String LOG_DOWNLOADS_ENABLED = "logDownloads";
	public static final String LOG_UPLOADS_ENABLED = "logUploads";

	public static final String TRACK_DOWNLOADS_ENABLED = "trackDownloads";

	public static final String USE_BOARDNAME_DOWNLOAD_SUBFOLDER_ENABLED = "useBoardnameDownloadSubfolder";

	public static final String BROWSER_ADDRESS = "browserAddress";

	public static final String FILE_LIST_FILE_DETAILS_DIALOG_SORT_STATE_SORTED_COLUMN = "FileListFileDetailsDialog.sortState.sortedColumn";
	public static final String FILE_LIST_FILE_DETAILS_DIALOG_SORT_STATE_SORTED_ASCENDING = "FileListFileDetailsDialog.sortState.sortedAscending";
	public static final String FILE_LIST_FILE_DETAILS_DIALOG_COLUMN_TABLE_INDEX_PREFIX = "FileListFileDetailsDialog.tableindex.modelcolumn.";
	public static final String FILE_LIST_FILE_DETAILS_DIALOG_COLUMN_WIDTH_PREFIX = "FileListFileDetailsDialog.columnwidth.modelcolumn.";

	public static final String DOWNLOAD_TABLE_SORT_STATE_SORTED_COLUMN = "DownloadTable.sortState.sortedColumn";
	public static final String DOWNLOAD_TABLE_SORT_STATE_SORTED_ASCENDING = "DownloadTable.sortState.sortedAscending";
	public static final String DOWNLOAD_TABLE_COLUMN_TABLE_INDEX_PREFIX = "DownloadTable.tableindex.modelcolumn.";
	public static final String DOWNLOAD_TABLE_COLUMN_WIDTH_PREFIX = "DownloadTable.columnwidth.modelcolumn.";

	public static final String SEARCH_FILES_TABLE_SORT_STATE_SORTED_COLUMN = "SearchFilesTable.sortState.sortedColumn";
	public static final String SEARCH_FILES_TABLE_SORT_STATE_SORTED_ASCENDING = "SearchFilesTable.sortState.sortedAscending";
	public static final String SEARCH_FILES_TABLE_COLUMN_TABLE_INDEX_PREFIX = "SearchFilesTable.tableindex.modelcolumn.";
	public static final String SEARCH_FILES_TABLE_COLUMN_WIDTH_PREFIX = "SearchFilesTable.columnwidth.modelcolumn.";

	public static final String UNSENT_MESSAGES_TABLE_SORT_STATE_SORTED_COLUMN = "UnsentMessagesTable.sortState.sortedColumn";
	public static final String UNSENT_MESSAGES_SORT_STATE_SORTED_ASCENDING = "UnsentMessagesTable.sortState.sortedAscending";
	public static final String UNSENT_MESSAGES_COLUMN_TABLE_INDEX_PREFIX = "UnsentMessagesTable.tableindex.modelcolumn.";
	public static final String UNSENT_MESSAGES_COLUMN_WIDTH_PREFIX = "UnsentMessagesTable.columnwidth.modelcolumn.";

	public static final String SHARED_FILES_TABLE_SORT_STATE_SORTED_COLUMN = "SharedFilesTable.sortState.sortedColumn";
	public static final String SHARED_FILES_TABLE_SORT_STATE_SORTED_ASCENDING = "SharedFilesTable.sortState.sortedAscending";
	public static final String SHARED_FILES_TABLE_COLUMN_TABLE_INDEX_PREFIX = "SharedFilesTable.tableindex.modelcolumn.";
	public static final String SHARED_FILES_TABLE_COLUMN_WIDTH_PREFIX = "SharedFilesTable.columnwidth.modelcolumn.";

	public static final String UPLOAD_TABLE_SORT_STATE_SORTED_COLUMN = "UploadTable.sortState.sortedColumn";
	public static final String UPLOAD_TABLE_SORT_STATE_SORTED_ASCENDING = "UploadTable.sortState.sortedAscending";
	public static final String UPLOAD_TABLE_COLUMN_TABLE_INDEX_PREFIX = "UploadTable.tableindex.modelcolumn.";
	public static final String UPLOAD_TABLE_COLUMN_WIDTH_PREFIX = "UploadTable.columnwidth.modelcolumn.";

	public static final String SENT_MESSAGES_TABLE_SORT_STATE_SORTED_COLUMN = "SentMessagesTable.sortState.sortedColumn";
	public static final String SENT_MESSAGES_TABLE_SORT_STATE_SORTED_ASCENDING = "SentMessagesTable.sortState.sortedAscending";
	public static final String SENT_MESSAGES_TABLE_COLUMN_TABLE_INDEX_PREFIX = "SentMessagesTable.tableindex.modelcolumn.";
	public static final String SENT_MESSAGES_TABLE_COLUMN_WIDTH_PREFIX = "SentMessagesTable.columnwidth.modelcolumn.";

    public Settings() {
        settingsHash = new Hashtable<String,Object>();
        // the FIX config.dir
        settingsHash.put(DIR_CONFIG, "config" + fs);
        final String configFilename = "config" + fs + "frost.ini";
        settingsFile = new File(configFilename);
        loadDefaults();
        if (!readSettingsFile()) {
            writeSettingsFile();
        }
        settingsHash.put(FILE_BASE, "testfiles1");
    }

    /**
     * Creates a new SettingsClass to read a frost.ini in directory config, relative
     * to the provided base directory.
     * The configuration is not read immediately, call readSettingsFile.
     * @param baseDirectory  the base directory of the config/frost.ini file
     */
    public Settings(final File baseDirectory) {
        settingsHash = new Hashtable<String,Object>();
        // the FIX config.dir
        settingsHash.put(DIR_CONFIG, baseDirectory.getPath() + fs + "config" + fs);
        final String configFilename = baseDirectory.getPath() + fs + "config" + fs + "frost.ini";
        settingsFile = new File(configFilename);
    }

    /**
     * Takes a path name, replaces all separator chars with the separator chars of the system.
     * Ensures that the path ends with a separator char.
     * @param path  input path
     * @return  changed path
     */
    public String setSystemFileSeparator(String path) {
        if (fs.equals("\\")) {
            path = path.replace('/', File.separatorChar);
        } else if (fs.equals("/")) {
            path = path.replace('\\', File.separatorChar);
        }

        // append fileseparator to end if needed
        if (path.endsWith(fs) == false) {
            path = path + fs;
        }
        return path;
    }

    public String getDefaultValue(final String key) {
        String val = (String) defaults.get(key);
        if (val == null) {
            val = "";
        }
        return val;
    }

	public boolean readSettingsFile() {
		// maybe restore a .bak of the .ini file
		if ((settingsFile.isFile() == false) || (settingsFile.length() == 0)) {
			// try to restore .bak file
			final String configDirStr = getString(Settings.DIR_CONFIG);
			final File bakFile = new File(configDirStr + "frost.ini.bak");
			if (bakFile.isFile() && (bakFile.length() > 0)) {
				bakFile.renameTo(settingsFile);
			} else {
				return false;
			}
		}

		String line = null;
		try (LineNumberReader settingsReader = new LineNumberReader(new FileReader(settingsFile));) {
			while ((line = settingsReader.readLine()) != null) {
				line = line.trim();
				if ((line.length() != 0) && (line.startsWith("#") == false)) {
					final StringTokenizer strtok = new StringTokenizer(line, "=");
					String key = "";
					String value = "";
					Object objValue = value;
					if (strtok.countTokens() >= 2) {
						key = strtok.nextToken().trim();
						value = strtok.nextToken().trim();
						// to allow '=' in values
						while (strtok.hasMoreElements()) {
							value += "=" + strtok.nextToken();
						}
						if (value.startsWith("type.color(") && value.endsWith(")")) {
							// this is a color
							final String rgbPart = value.substring(11, value.length() - 1);
							final StringTokenizer strtok2 = new StringTokenizer(rgbPart, ",");

							if (strtok2.countTokens() == 3) {
								try {
									int red, green, blue;
									red = Integer.parseInt(strtok2.nextToken().trim());
									green = Integer.parseInt(strtok2.nextToken().trim());
									blue = Integer.parseInt(strtok2.nextToken().trim());
									final Color c = new Color(red, green, blue);
									objValue = c;
								} catch (NumberFormatException e) {
									objValue = null;
								}
							} else {
								objValue = null; // dont insert in settings, use default instead
							}
						}
						// scan all path config values and set correct system file separator
						else if (key.equals(Settings.DIR_TEMP) || key.equals(DIR_LOCALDATA)
								|| key.equals(DIR_STORE) || key.equals(DIR_DOWNLOAD) || key.equals(DIR_LAST_USED)
								|| key.equals(DIR_HELP)) {
							value = setSystemFileSeparator(value);
							objValue = value;
						} else {
							// 'old' behaviour
							objValue = value;
						}
						if (objValue != null) {
							settingsHash.put(key, objValue);
						}
					}
				}
			}
		} catch (IOException e) {
			logger.error("Exception thrown in readSettingsFile()", e);
			return false;
		}

		doCleanup();
		doChanges();

		logger.info("Read user configuration");
		return true;
	}

    /**
     * Adjust values as needed.
     */
    private void doChanges() {
        if (this.getString(Settings.MESSAGE_BASE).length() == 0) {
            this.setValue(Settings.MESSAGE_BASE, "news");
        }

        // adjust reshare interval, old default was 3
        if( this.getInteger(MIN_DAYS_BEFORE_FILE_RESHARE) == 3 ) {
            this.setValue(MIN_DAYS_BEFORE_FILE_RESHARE, 5);
        }

        // maybe enable for a later release
//        if (this.getValue(FILE_BASE).length() == 0) {
//            this.setValue(FILE_BASE, "files");
//        }

        // dynamically add archiveExtension .7z
        final String tmp = this.getString(Settings.FILEEXTENSION_ARCHIVE);
        if( (tmp != null) && (tmp.indexOf(".7z") < 0) ) {
            // add .7z
            this.setValue(Settings.FILEEXTENSION_ARCHIVE, tmp + ";.7z");
        }
    }

    /**
     * Remove obsolete keys.
     */
    private void doCleanup() {
        settingsHash.remove("fcp2.defaultPriorityFile");
        settingsHash.remove("fcp2.defaultPriorityMessage");
        settingsHash.remove("spamTreshold");
        settingsHash.remove("skinsEnabled");
        settingsHash.remove("selectedSkin");
        settingsHash.remove("doBoardBackoff");
        settingsHash.remove("compactDatabaseTables");
    }

	private boolean writeSettingsFile() {
		// ensure the config directory exists
		final File configDir = new File("config");
		if (!configDir.isDirectory()) {
			configDir.mkdirs(); // if the config dir doesn't exist, we create it
		}

		// write to new file
		final String configDirStr = getString(Settings.DIR_CONFIG);
		final File newFile = new File(configDirStr + "frost.ini.new");
		final File oldFile = new File(configDirStr + "frost.ini.old");
		final File bakFile = new File(configDirStr + "frost.ini.bak");

		try (PrintWriter settingsWriter = new PrintWriter(new FileWriter(newFile));) {
			final TreeMap<String, Object> sortedSettings = new TreeMap<String, Object>(settingsHash); // sort the lines
			final Iterator<String> i = sortedSettings.keySet().iterator();
			while (i.hasNext()) {
				final String key = i.next();
				if (key.equals(DIR_CONFIG)) {
					continue; // do not save the config dir, its unchangeable
				}

				String val = null;
				if (sortedSettings.get(key) instanceof Color) {
					final Color c = (Color) sortedSettings.get(key);
					val = new StringBuilder().append("type.color(").append(c.getRed()).append(",").append(c.getGreen())
							.append(",").append(c.getBlue()).append(")").toString();
				} else {
					val = sortedSettings.get(key).toString();
				}

				settingsWriter.println(key + "=" + val);
			}
		} catch (final IOException e) {
			logger.error("Exception thrown in writeSettingsFile", e);
			return false;
		}

        oldFile.delete();

        if( bakFile.isFile() ) {
            bakFile.renameTo(oldFile);
        }

        if( settingsFile.isFile() && (settingsFile.length() > 0) ) {
            settingsFile.renameTo(bakFile); // settingsFile keeps old name!
        }

        newFile.renameTo(settingsFile);

        oldFile.delete();

        logger.info("Wrote configuration");
        return true;
    }

    /**
     * Adds a PropertyChangeListener to the listener list.
     * <p>
     * If listener is null, no exception is thrown and no action is performed.
     *
     * @param    listener  the PropertyChangeListener to be added
     *
     * @see #removePropertyChangeListener
     * @see #getPropertyChangeListeners
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public synchronized void addPropertyChangeListener(final PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Adds a PropertyChangeListener to the listener list for a specific
     * property.
     * <p>
     * If listener is null, no exception is thrown and no action is performed.
     *
     * @param propertyName one of the property names listed above
     * @param listener the PropertyChangeListener to be added
     *
     * @see #removePropertyChangeListener(String, PropertyChangeListener)
     * @see #getPropertyChangeListeners(String)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public synchronized void addPropertyChangeListener(
        final String propertyName,
        final PropertyChangeListener listener) {

        if (listener == null) {
            return;
        }
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Adds a SettingsUpdater to the updaters list.
     * <p>
     * If updater is null, no exception is thrown and no action is performed.
     *
     * @param    updater  the SettingsUpdater to be added
     *
     * @see #removeUpdater
     */
    public synchronized void addUpdater(final SettingsUpdater updater) {
        if (updater == null) {
            return;
        }
        if (updaters == null) {
            updaters = new Vector<SettingsUpdater>();
        }
        updaters.addElement(updater);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * <p>
     * If listener is null, no exception is thrown and no action is performed.
     *
     * @param listener the PropertyChangeListener to be removed
     *
     * @see #addPropertyChangeListener
     * @see #getPropertyChangeListeners
     * @see #removePropertyChangeListener(String,PropertyChangeListener)
     */
    public synchronized void removePropertyChangeListener(final PropertyChangeListener listener) {
        if ((listener == null) || (changeSupport == null)) {
            return;
        }
        changeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener from the listener list for a specific
     * property.
     * <p>
     * If listener is null, no exception is thrown and no action is performed.
     *
     * @param propertyName a valid property name
     * @param listener the PropertyChangeListener to be removed
     *
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     * @see #getPropertyChangeListeners(String)
     * @see #removePropertyChangeListener(PropertyChangeListener)
     */
    public synchronized void removePropertyChangeListener(
        final String propertyName,
        final PropertyChangeListener listener) {

        if ((listener == null) || (changeSupport == null)) {
            return;
        }
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a SettingsUpdater from the updaters list.
     * <p>
     * If updaters is null, no exception is thrown and no action is performed.
     *
     * @param updater the SettingsUpdater to be removed
     *
     * @see #addUpdater
     */
    public synchronized void removeUpdater(final SettingsUpdater updater) {
        if ((updater == null) || (updaters == null)) {
            return;
        }
        updaters.removeElement(updater);
    }

    /**
     * Returns an array of all the property change listeners
     * registered on this component.
     *
     * @return all of this component's <code>PropertyChangeListener</code>s
     *         or an empty array if no property change
     *         listeners are currently registered
     *
     * @see      #addPropertyChangeListener
     * @see      #removePropertyChangeListener
     * @see      #getPropertyChangeListeners(String)
     * @see      PropertyChangeSupport#getPropertyChangeListeners
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return changeSupport.getPropertyChangeListeners();
    }

    /**
     * Returns an array of all the listeners which have been associated
     * with the named property.
     *
     * @return all of the <code>PropertyChangeListeners</code> associated with
     *         the named property or an empty array if no listeners have
     *         been added
     *
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     * @see #removePropertyChangeListener(String, PropertyChangeListener)
     * @see #getPropertyChangeListeners
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners(final String propertyName) {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return changeSupport.getPropertyChangeListeners(propertyName);
    }

    /**
     * Support for reporting bound property changes for Object properties.
     * This method can be called when a bound property has changed and it will
     * send the appropriate PropertyChangeEvent to any registered
     * PropertyChangeListeners.
     *
     * @param propertyName the property whose value has changed
     * @param oldValue the property's previous value
     * @param newValue the property's new value
     */
    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        if (changeSupport == null) {
            return;
        }
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /* Get the values from the Hash
     * Functions will return null if nothing appropriate
     * is found or the settings are wrongly formatted or
     * any other conceivable exception.
     */
    public String getString(final String key) {
        return (String) settingsHash.get(key);
    }

    public Object getObjectValue(final String key) {
        return settingsHash.get(key);
    }

	public List<String> getStringList(final String key) {
		return Convert.toStringList(getString(key));
	}

	public Boolean getBoolean(final String key) {
        final String str = (String) settingsHash.get(key);
        if (str == null) {
            return false;
        }
        try {
            if (str.toLowerCase().equals("false")) {
                return false;
            }
            if (str.toLowerCase().equals("true")) {
                return true;
            }
        } catch (final NullPointerException e) {
            return false;
        }
        return getBoolean(getDefaultValue(key));
    }

	public Integer getInteger(final String key) {
        final String str = (String) settingsHash.get(key);
        if (str == null) {
            return 0;
        }
        int val = 0;
        try {
            val = Integer.parseInt(str);
        } catch (final NumberFormatException e) {
            return getInteger(getDefaultValue(key));
        } catch (final Exception e) {
            return 0;
        }
        return val;
    }

	public Long getLong(final String key) {
        final String str = (String) settingsHash.get(key);
        if (str == null) {
            return 0L;
        }
        long val = 0L;
        try {
            val = Long.parseLong(str);
        } catch (final NumberFormatException e) {
            return getLong(getDefaultValue(key));
        } catch (final Exception e) {
            return 0L;
        }
        return val;
    }

	public FreenetPriority getPriority(String key) {
		return FreenetPriority.getPriority(getInteger(key));
	}

	public Font getFont(String fontNameKey, String fontStyleKey, String fontSizeKey, String fallBackFontName) {
		String fontName = getString(fontNameKey);
		Integer fontStyle = getInteger(fontStyleKey);
		Integer fontSize = getInteger(fontSizeKey);
		Font font = new Font(fontName, fontStyle, fontSize);
		if (!font.getFamily().equals(fontName)) {
			logger.error("The selected font \"{}\" was not found in your system.", fontName);
			if (fallBackFontName != null) {
				logger.error("That selection will be changed to \"{}\".", fallBackFontName);
				setValue(fontNameKey, fallBackFontName);
				font = new Font(fallBackFontName, fontStyle, fontSize);
			}
		}
		return font;
	}

	public void setValue(final String key, String value) {
        // for all dirs ensure correct separator chars and a separator checr at end of name
        if( key.endsWith(".dir") ) {
            value = setSystemFileSeparator(value);
        }
        final Object oldValue = settingsHash.get(key);
        settingsHash.put(key, value);
        // Report the change to any registered listeners.
        firePropertyChange(key, oldValue, value);
    }
    public void setValue(final String key, final Integer value) {
        setValue(key, String.valueOf(value));
    }
    public void setValue(final String key, final int value) {
        setValue(key, String.valueOf(value));
    }
    public void setValue(final String key, final long value) {
        setValue(key, String.valueOf(value));
    }
    public void setValue(final String key, final Boolean value) {
        setValue(key, String.valueOf(value));
    }
    public void setValue(final String key, final boolean value) {
        setValue(key, String.valueOf(value));
    }

    public void setObjectValue(final String key, final Object value) {
        final Object oldValue = settingsHash.get(key);
        settingsHash.put(key, value);
        // Report the change to any registered listeners.
        firePropertyChange(key, oldValue, value);
    }

    /**
     * Contains all default values that are used if no value is found in .ini file.
     */
    public void loadDefaults() {
        defaults = new Hashtable<String,Object>();
        final File fn = File.listRoots()[0];

        defaults.put(MIGRATE_VERSION, "0");

        defaults.put(FREETALK_SHOW_TAB, "false");
        defaults.put(FREETALK_LOGIN_USERID, "");

        defaults.put(DOS_STOP_BOARD_UPDATES_WHEN_DOSED, "true");
        defaults.put(DOS_INVALID_SUBSEQUENT_MSGS_THRESHOLD, "30");

        defaults.put(DB_CLEANUP_INTERVAL, "5");
        defaults.put(DB_CLEANUP_LASTRUN, "0");

        defaults.put(DB_CLEANUP_REMOVEOFFLINEFILEWITHKEY, "true");
        defaults.put(DB_CLEANUP_OFFLINEFILESMAXDAYSOLD, "30");

        // DIRECTORIES
        defaults.put(DIR_TEMP, "localdata" + fs + "temp" + fs);
        defaults.put(DIR_LOCALDATA, "localdata" + fs);
        defaults.put(DIR_STORE, "store" + fs);
		defaults.put(DIR_HELP, "help" + fs);

        defaults.put(DIR_DOWNLOAD, "downloads" + fs);
        defaults.put(DIR_LAST_USED, "." + fs);

        defaults.put(FILESHARING_DISABLE, "true");
        defaults.put(FILESHARING_IGNORE_CHECK_AND_BELOW, "true");
        defaults.put(DISABLE_SPLASHSCREEN, "false");

        defaults.put(STORAGE_STORE_INVALID_MESSAGES, "false");
        defaults.put(STORAGE_STORE_SENT_MESSAGES, "true");

        defaults.put(REMEMBER_SHAREDFILE_DOWNLOADED, "true");

		defaults.put(FREENET_FCP_ADDRESS, "127.0.0.1:9481");
        defaults.put(FCP2_USE_DDA, "false");
        defaults.put(FCP2_USE_PERSISTENCE, "true");
        defaults.put(FCP2_USE_ONE_CONNECTION_FOR_MESSAGES, "true");

        defaults.put(FCP2_DEFAULT_PRIO_MESSAGE_UPLOAD, "2");
        defaults.put(FCP2_DEFAULT_PRIO_MESSAGE_DOWNLOAD, "2");
        defaults.put(FCP2_DEFAULT_PRIO_FILE_UPLOAD, "3");
        defaults.put(FCP2_DEFAULT_PRIO_FILE_DOWNLOAD, "3");

        defaults.put(FCP2_ENFORCE_FROST_PRIO_FILE_DOWNLOAD, "false");
        defaults.put(FCP2_ENFORCE_FROST_PRIO_FILE_UPLOAD, "false");

        defaults.put(FCP2_SET_TARGET_FILENAME_FOR_MANUAL_PUT, "true");
        defaults.put(FCP2_QUICKLY_FAIL_ON_ADNF, "false");

        defaults.put(FCP2_USE_EARLY_ENCODE, "true");

        defaults.put(ALTERNATE_EDITOR_COMMAND, fn + "path" + fs + "to" + fs + "editor" + " %f");
        defaults.put(BOARD_AUTOUPDATE_ENABLED, "true");
        defaults.put(BOARD_AUTOUPDATE_CONCURRENT_UPDATES, "6"); // no. of concurrent updating boards in auto update
        defaults.put(BOARD_AUTOUPDATE_MIN_INTERVAL, "45"); // time in min to wait between start of updates for 1 board

        defaults.put(BOARD_UPDATE_VISUALIZATION_ENABLED, "true");
        defaults.put(BOARD_UPDATE_VISUALIZATION_BGCOLOR_NOT_SELECTED, new Color(233, 233, 233)); // "type.color(233,233,233)"
        defaults.put(BOARD_UPDATE_VISUALIZATION_BGCOLOR_SELECTED, new Color(137, 137, 191)); // "type.color(137,137,191)"

        defaults.put(MESSAGE_BLOCK_SUBJECT, "");
        defaults.put(MESSAGE_BLOCK_SUBJECT_ENABLED, "false");
        defaults.put(MESSAGE_BLOCK_BODY, "");
        defaults.put(MESSAGE_BLOCK_BODY_ENABLED, "false");
        defaults.put(MESSAGE_BLOCK_BOARDNAME, "");
        defaults.put(MESSAGE_BLOCK_BOARDNAME_ENABLED, "false");

        defaults.put(MESSAGE_HIDE_UNSIGNED, "false");
        defaults.put(MESSAGE_HIDE_BAD, "false");
        defaults.put(MESSAGE_HIDE_CHECK, "false");
        defaults.put(MESSAGE_HIDE_OBSERVE, "false");
        defaults.put(MESSAGE_HIDE_COUNT, "0");
        defaults.put(MESSAGE_HIDE_COUNT_EXCLUDE_PRIVATE, "true");

        defaults.put(JUNK_HIDE_JUNK_MESSAGES, "false");

        defaults.put(KNOWNBOARDS_BLOCK_FROM_UNSIGNED, "false");
        defaults.put(KNOWNBOARDS_BLOCK_FROM_BAD, "true");
        defaults.put(KNOWNBOARDS_BLOCK_FROM_CHECK, "false");
        defaults.put(KNOWNBOARDS_BLOCK_FROM_OBSERVE, "false");

        defaults.put(DOWNLOAD_MAX_THREADS, "3");
        defaults.put(DOWNLOADING_ACTIVATED, "true");

        defaults.put(DOWNLOAD_MAX_RETRIES, "25");
        defaults.put(DOWNLOAD_WAITTIME, "5");

        defaults.put(MAX_MESSAGE_DISPLAY, "15");
        defaults.put(MAX_MESSAGE_DOWNLOAD, "5");
        defaults.put(ALWAYS_DOWNLOAD_MESSAGES_BACKLOAD, "false");

        defaults.put(MIN_DAYS_BEFORE_FILE_RESHARE, "5"); // reshare all 5 days
        defaults.put(MAX_FILELIST_DOWNLOAD_DAYS, "5"); // download backward 5 days

        defaults.put(MESSAGE_BASE, "news");
        defaults.put(FILE_BASE, "files");

        defaults.put(SHOW_SYSTRAY_ICON, "true");
        defaults.put(MINIMIZE_TO_SYSTRAY, "false");

        defaults.put(DOWNLOAD_REMOVE_FINISHED, "false");
        defaults.put(UPLOAD_REMOVE_FINISHED, "false");

        defaults.put(SEARCH_MAX_RESULTS, "10000");
        defaults.put(SEARCH_HIDE_BAD, "true");
        defaults.put(SEARCH_HIDE_CHECK, "false");
        defaults.put(SEARCH_HIDE_OBSERVE, "false");
        defaults.put(SEARCH_HIDE_FILES_WITHOUT_CHK, "false");

        defaults.put(GQ_SHOW_EXTERNAL_ITEMS_DOWNLOAD, "false");
        defaults.put(GQ_SHOW_EXTERNAL_ITEMS_UPLOAD, "false");

        defaults.put(COMPRESS_UPLOADS, "true");

        defaults.put(BOARDLIST_LAST_SELECTED_BOARD, "0");
        defaults.put(UPLOAD_MAX_THREADS, "3");
        defaults.put(ALTERNATE_EDITOR_ENABLED, "false");
        defaults.put(LAST_USED_FROMNAME, "Anonymous");
        defaults.put(FILEEXTENSION_AUDIO, ".mp3;.ogg;.wav;.mid;.mod;.flac;.sid");
        defaults.put(FILEEXTENSION_VIDEO, ".mpeg;.mpg;.avi;.divx;.asf;.wmv;.rm;.ogm;.mov");
        defaults.put(FILEEXTENSION_DOCUMENT, ".doc;.txt;.tex;.pdf;.dvi;.ps;.odt;.sxw;.sdw;.rtf;.pdb;.psw");
        defaults.put(FILEEXTENSION_EXECUTABLE, ".exe;.vbs;.jar;.sh;.bat;.bin");
        defaults.put(FILEEXTENSION_ARCHIVE, ".zip;.rar;.jar;.gz;.arj;.ace;.bz;.tar;.tgz;.tbz");
        defaults.put(FILEEXTENSION_IMAGE, ".jpeg;.jpg;.jfif;.gif;.png;.tif;.tiff;.bmp;.xpm");
        defaults.put(AUTO_SAVE_INTERVAL, "60");
        defaults.put(AUTO_SAVE_LOCAL_IDENTITIES, "true");

        defaults.put(MESSAGE_UPLOAD_DISABLED, "false");

        defaults.put(MESSAGE_EXPIRE_DAYS, "90");
        defaults.put(MESSAGE_EXPIRATION_MODE, "KEEP"); // KEEP or ARCHIVE or DELETE, default KEEP
        defaults.put(ARCHIVE_KEEP_FLAGGED_AND_STARRED, "true");
        defaults.put(ARCHIVE_KEEP_UNREAD, "false");

        defaults.put(LANGUAGE_LOCALE, "default");

        defaults.put(MAINFRAME_LAST_WIDTH, "700"); // "lastFrameWidth"
        defaults.put(MAINFRAME_LAST_HEIGHT, "500"); // "lastFrameHeight"
        defaults.put(MAINFRAME_LAST_X, "50"); // "lastFramePosX"
        defaults.put(MAINFRAME_LAST_Y, "50"); // "lastFramePosY"
        defaults.put(MAINFRAME_LAST_MAXIMIZED, "false"); // "lastFrameMaximized"

        defaults.put(BOARD_TREE_FONT_NAME, "Tahoma");
		defaults.put(BOARD_TREE_FONT_STYLE, Integer.valueOf(Font.PLAIN).toString());
        defaults.put(BOARD_TREE_FONT_SIZE, "11");
        defaults.put(MESSAGE_BODY_FONT_NAME, "Monospaced");
		defaults.put(MESSAGE_BODY_FONT_STYLE, Integer.valueOf(Font.PLAIN).toString());
        defaults.put(MESSAGE_BODY_FONT_SIZE, "12");
        defaults.put(MESSAGE_LIST_FONT_NAME, "SansSerif");
		defaults.put(MESSAGE_LIST_FONT_STYLE, Integer.valueOf(Font.PLAIN).toString());
        defaults.put(MESSAGE_LIST_FONT_SIZE, "11");
        defaults.put(FILE_LIST_FONT_NAME, "SansSerif");
		defaults.put(FILE_LIST_FONT_STYLE, Integer.valueOf(Font.PLAIN).toString());
        defaults.put(FILE_LIST_FONT_SIZE, "11");

        defaults.put(MESSAGE_BODY_ANTIALIAS, "false");
        defaults.put(MSGTABLE_MULTILINE_SELECT, "false");
        defaults.put(MSGTABLE_SCROLL_HORIZONTAL, "false");
        defaults.put(MSGTABLE_SHOW_COLLAPSED_THREADS, "false");
        defaults.put(MSGTABLE_DOUBLE_CLICK_SHOWS_MESSAGE, "true");

        defaults.put(SAVE_SORT_STATES, "false");

        defaults.put(SHOW_BOARDDESC_TOOLTIPS, "true");
        defaults.put(PREVENT_BOARDTREE_REORDERING, "false");
        defaults.put(SHOW_BOARDTREE_FLAGGEDSTARRED_INDICATOR, "true");

        defaults.put(SILENTLY_RETRY_MESSAGES, "false");
        defaults.put(SHOW_DELETED_MESSAGES, "false");

        defaults.put(UPLOAD_MAX_RETRIES, "5");
        defaults.put(UPLOAD_WAITTIME, "5");
        defaults.put(UPLOAD_REMOVE_NOT_EXISTING_FILES, "true");

        defaults.put(SHOW_THREADS, "true");
        defaults.put(HANDLE_OWN_MESSAGES_AS_NEW_DISABLED, "false");
        defaults.put(SHOW_OWN_MESSAGES_AS_ME_DISABLED, "false");
        defaults.put(SORT_THREADROOTMSGS_ASCENDING, "false");

        defaults.put(INDICATE_LOW_RECEIVED_MESSAGES, "true");
        defaults.put(INDICATE_LOW_RECEIVED_MESSAGES_COUNT_RED, "1");
        defaults.put(INDICATE_LOW_RECEIVED_MESSAGES_COUNT_LIGHTRED, "5");

        defaults.put(SHOW_COLORED_ROWS, "true");
        defaults.put(SHOW_SMILEYS, "true");
        defaults.put(SHOW_KEYS_AS_HYPERLINKS, "true");

        defaults.put(LOG_DOWNLOADS_ENABLED, "false");
        defaults.put(LOG_UPLOADS_ENABLED, "false");
        defaults.put(TRACK_DOWNLOADS_ENABLED, "false");
        defaults.put(USE_BOARDNAME_DOWNLOAD_SUBFOLDER_ENABLED, "false");


        defaults.put(CONFIRM_MARK_ALL_MSGS_READ, "true");
        defaults.put(CONFIRM_FILESHARING_IS_ENABLED, "true");

        defaults.put(PERST_PAGEPOOLSIZE_FILES,              "2048");
        defaults.put(PERST_PAGEPOOLSIZE_INDEXSLOTS,         "1024");
        defaults.put(PERST_PAGEPOOLSIZE_SHAREDFILESCHKKEYS, "1024");
        defaults.put(PERST_PAGEPOOLSIZE_FILELIST,           "1024");
        defaults.put(PERST_PAGEPOOLSIZE_IDENTITIES,         "2048");
        defaults.put(PERST_PAGEPOOLSIZE_MESSAGEARCHIVE,     "2048");
        defaults.put(PERST_PAGEPOOLSIZE_MESSAGES,           "6144");
        defaults.put(PERST_PAGEPOOLSIZE_MESSAGECONTENTS,    "4096");
        defaults.put(PERST_PAGEPOOLSIZE_TRACKDOWNLOADKEYS,  "1024");

        defaults.put(PERST_COMPACT_STORAGES, "false");
        defaults.put(PERST_EXPORT_STORAGES,  "false");

		defaults.put(BROWSER_ADDRESS, "http://127.0.0.1:8888/");

        settingsHash.putAll(defaults);
    }

    /**
     * This method asks all of the updaters to update the settings values
     * they have knowledge about and saves all of the settings values to disk.
     *
     * (Not thread-safe with addUpdater/removeUpdater)
     */
    public void exitSave() throws StorageException {
        if (updaters != null) {
            final Enumeration<SettingsUpdater> enumeration = updaters.elements();
            while (enumeration.hasMoreElements()) {
                enumeration.nextElement().updateSettings();
            }
        }
        if (!writeSettingsFile()) {
            throw new StorageException("Error while saving the settings.");
        }
    }

	public final String resolve(String filename) {
		return appHome.resolve(filename).normalize().toString();
	}

	public String getFullHelpPath() {
		return resolve(getString(DIR_HELP)) + File.separator;
	}

	public static String getVersion() {
		return Settings.class.getPackage().getImplementationVersion();
	}

	public static String generateFproxyAddress(String fcpAddress) {
		String[] parts = fcpAddress.split(":");
		if (parts.length == 2) {
			return "http://" + parts[0] + ":8888/";
		} else {
			return "";
		}
	}
}
