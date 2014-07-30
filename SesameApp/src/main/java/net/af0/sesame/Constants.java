package net.af0.sesame;

class Constants {
    public static final String KEY_IMPORT_TMPNAME = "import";
    public static final String KEY_IMPORT_SUFFIX = ".keys";
    public static final String KEY_EXPORT_FILE = "Sesame Key Export (%s).keys";
    public static final String KEY_EXPORT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String KEY_EXPORT_URI = "net.af0.sesame.keysexport";
    public static final String KEY_EXPORT_MIME = "application/octet-stream";
    static final String ARG_ITEM_ID = "item_id";
    static final String ARG_TWO_PANE = "two_pane";
    static final String[] PASSWORD_CHARS = {
            "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789!@#$%^&*()-_+=;:'\"[{]}\\|,<.>/?",
            "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789!@#$%^&*-_+,.?",
            "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789",
    };
    static final int MIN_RANDOM_PASSWORD_LENGTH = 8;
    static final int MAX_RANDOM_PASSWORD_LENGTH = 14;
    static final String PREFS_BACKUP = "backup_database";
    static final String PREFS_CHANGE_PASSWORD = "change_password";
    static final int IMPORT_DATABASE_RESULT = 13;
    static final String DB_METADATA_PREF = "db_metadata";
}
