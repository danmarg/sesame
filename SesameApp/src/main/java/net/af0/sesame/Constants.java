package net.af0.sesame;

/**
 * Fixed constants.
 */
class Constants {
    /**
     * Minimum password length on new and changed passwords.
     */
    public static final int MIN_PASSWORD_LENGTH = 4;
    /**
     * Filename for database import tempfile.
     */
    public static final String KEY_IMPORT_TMPNAME = "import";
    /**
     * File extension for database import tempfile.
     */
    public static final String KEY_IMPORT_SUFFIX = ".keys";
    /**
     * Export filename (with format string for date).
     */
    public static final String KEY_EXPORT_FILE = "Sesame Key Export (%s).keys";
    /**
     * Format for date substring in export filename.
     */
    public static final String KEY_EXPORT_DATE_FORMAT = "yyyy-MM-dd";
    /**
     * Export URI.
     */
    public static final String KEY_EXPORT_URI = "net.af0.sesame.keysexport";
    /**
     * Export mime type.
     */
    public static final String KEY_EXPORT_MIME = "application/octet-stream";
    /**
     * Import mime type.
     */
    public static final String KEY_IMPORT_MIME = "*/*";
    /**
     * Intent argument for item ID. This is always the database "id" field, not the id of the UI
     * element holding it.
     */
    static final String ARG_ITEM_ID = "item_id";
    /**
     * Intent argument to indicate two-pane mode.
     */
    static final String ARG_TWO_PANE = "two_pane";
    /**
     * Eligible characters for use in a password. Each element in this array represents one possible
     * set of characters; upon generation, we will iterate through all possible sets sequentially.
     */
    static final String[] PASSWORD_CHARS = {
            "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789",
            "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789!@#$%^&*-_+,.?",
            "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789!@#$%^&*()-_+=;:'\"[{]}\\|,<.>/?",

    };
    /**
     * Min length for password generation.
     */
    static final int MIN_RANDOM_PASSWORD_LENGTH = 8;
    /**
     * Max length for password generation.
     */
    static final int MAX_RANDOM_PASSWORD_LENGTH = 14;
    /**
     * Preference for whether to do cloud backups. Must remain in sync with preferences.xml.
     */
    static final String PREFS_BACKUP = "backup_database";
    /**
     * Preference for "password change" field.
     */
    static final String PREFS_CHANGE_PASSWORD = "change_password";
    /**
     * Activity result ID from database import.
     */
    static final int IMPORT_DATABASE_RESULT = 13;
    /**
     * Default number of PBKDF iterations for key derivation. Note that changing this will only
     * change databases created henceforth; existing databases will not be upgraded in-place. To
     * achieve this, one can export the database, delete it, create a new one, and import the
     * export.
     */
    static final int KDF_ITER = 256000;
    /**
     * Preference key for storing the serialized DatabaseMetadata proto for the current database.
     */
    static final String DB_METADATA_PREF = "db_metadata";
    /**
     * Single-shot key for Item List showcase view.
     */
    static final int SINGLE_SHOT_ITEM_LIST = 1;
    /**
     * Single-shot key for Edit Item showcase view.
     */
    static final int SINGLE_SHOT_EDIT_ITEM = 2;
}
