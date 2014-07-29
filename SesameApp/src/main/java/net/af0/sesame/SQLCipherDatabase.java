package net.af0.sesame;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class wrapping the database interactions.
 */
public final class SQLCipherDatabase {
    private static final String TABLE_KEYS = "keys";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_DOMAIN = "domain";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_REMARKS = "remarks";
    static final String DATABASE_CREATE = "create table " +
            TABLE_KEYS + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_USERNAME + " text, " +
            COLUMN_DOMAIN + " text," +
            COLUMN_PASSWORD + " text," +
            COLUMN_REMARKS + " text);";
    private static String[] allColumns_ = {
            COLUMN_ID, COLUMN_USERNAME, COLUMN_DOMAIN,
            COLUMN_PASSWORD, COLUMN_REMARKS};
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "keys.db";
    private static net.sqlcipher.database.SQLiteOpenHelper helper_;
    private static SQLiteDatabase database_;

    public static Record createRecord(String username, String domain, String password,
                                      String remarks) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_DOMAIN, domain);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_REMARKS, remarks);
        long id = database_.insert(TABLE_KEYS, null, values);
        Cursor crs = database_.query(TABLE_KEYS, allColumns_,
                COLUMN_ID + "=" + id, null, null, null, null);
        crs.moveToFirst();
        Record r = toRecord(crs);
        crs.close();
        return r;
    }

    public static void deleteRecord(Record r) {
        database_.delete(TABLE_KEYS, COLUMN_ID + "=" + r.getId(), null);
    }

    public static void updateRecord(Record r) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, r.getUsername());
        values.put(COLUMN_DOMAIN, r.getDomain());
        values.put(COLUMN_PASSWORD, r.getPassword());
        values.put(COLUMN_REMARKS, r.getRemarks());
        database_.update(TABLE_KEYS, values, COLUMN_ID + "=" + r.getId(), null);
    }

    public static Record getRecord(long record_id) {
        Cursor crs = database_.query(TABLE_KEYS, allColumns_,
                COLUMN_ID + "=" + record_id,
                null, null, null, null);
        if (crs.getCount() == 0) {
            return null;
        }
        crs.moveToFirst();
        Record r = toRecord(crs);
        crs.close();
        return r;
    }

    public static List<Record> getAll() {
        Cursor crs = database_.query(TABLE_KEYS, allColumns_, null, null, null, null,
                null);
        List<Record> rs = new ArrayList<Record>(crs.getCount());
        for (crs.moveToFirst(); !crs.isAfterLast(); crs.moveToNext()) {
            rs.add(toRecord(crs));
        }
        crs.close();
        return rs;
    }

    private static Record toRecord(Cursor crs) {
        Record r = new Record();
        r.setId(crs.getLong(0));
        r.setUsername(crs.getString(1));
        r.setDomain(crs.getString(2));
        r.setPassword(crs.getString(3));
        r.setRemarks(crs.getString(4));

        return r;
    }

    public static void OpenDatabase(Context ctx, char[] password) {
        if (helper_ == null) {
            SQLiteDatabase.loadLibs(ctx);
            helper_ = new OpenHelper(ctx);
        }
        database_ = helper_.getWritableDatabase(password);
    }

    public static void ImportDatabase(String path, char[] password) {
        if (isLocked()) {
            throw new SQLiteException("Database must be unlocked");
        }
        SQLiteDatabase imported = SQLiteDatabase.openDatabase(
                path,
                password, null, SQLiteDatabase.OPEN_READONLY, new DatabaseHook());
        if (imported.getVersion() != DATABASE_VERSION) {
            // Because we're not using OpenHelper here, we have to handle version mismatches
            // ourselves. For now, versioning is unsupported!
            throw new UnsupportedOperationException("Upgrade not supported!");

        }
        Cursor crs = imported.query(TABLE_KEYS, allColumns_, null, null, null, null,
                null);
        for (crs.moveToFirst(); !crs.isAfterLast(); crs.moveToNext()) {
            Record r = toRecord(crs);
            createRecord(r.getUsername(), r.getDomain(), r.getPassword(), r.getRemarks());
        }
        crs.close();
    }

    public static void CreateDatabase(Context ctx, char[] password) {
        SQLiteDatabase.loadLibs(ctx);
        if (Exists(ctx)) {
            throw new SQLiteException("file already exists");
        }
        OpenDatabase(ctx, password);
    }

    public static void DeleteDatabase(Context ctx) {
        ctx.deleteDatabase(DATABASE_NAME);
    }

    public static void ChangePassword(String password) {
        // TODO: Switch this from a String to a char[] and just manually escape.
        database_.rawExecSQL("PRAGMA rekey = " + DatabaseUtils.sqlEscapeString(password) + ";");
    }

    public static boolean isLocked() {
        return database_ == null || !database_.isOpen();
    }

    public static void Lock() {
        if (helper_ != null) {
            helper_.close();
        }
        if (database_ != null) {
            database_.close();
        }
    }

    public static boolean Exists(Context ctx) {
        return ctx.getDatabasePath(DATABASE_NAME).exists();
    }

    public static File getDatabaseFilePath(Context ctx) {
        return ctx.getDatabasePath(DATABASE_NAME);
    }

    private static class OpenHelper extends net.sqlcipher.database.SQLiteOpenHelper {
        public OpenHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION, new DatabaseHook());
        }

        public void onCreate(SQLiteDatabase database) {
            database.execSQL(DATABASE_CREATE);
        }

        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            throw new UnsupportedOperationException("Upgrade not supported");
        }
    }

    private static class DatabaseHook implements SQLiteDatabaseHook {
        @Override
        public void preKey(SQLiteDatabase database) {
        }

        @Override
        public void postKey(SQLiteDatabase database) {
            database.rawExecSQL(String.format("PRAGMA kdf_iter = %d",
                    Constants.KDF_ITERATIONS));
        }
    }

    // Database model.
    public static class Record implements Comparable<Record> {
        private long id_;
        private String username_;
        private String domain_;
        private String password_;  // TODO: switch these all to char[]?
        private String remarks_;

        public long getId() {
            return id_;
        }

        void setId(long id) {
            id_ = id;
        }

        public String getUsername() {
            return username_;
        }

        public void setUsername(String username) {
            username_ = username;
        }

        public String getDomain() {
            return domain_;
        }

        public void setDomain(String domain) {
            domain_ = domain;
        }

        public String getPassword() {
            return password_;
        }

        public void setPassword(String password) {
            password_ = password;
        }

        public String getRemarks() {
            return remarks_;
        }

        public void setRemarks(String remarks) {
            remarks_ = remarks;
        }

        @Override
        public String toString() {
            if (getUsername() != null && getDomain() != null &&
                    !getUsername().isEmpty() && !getDomain().isEmpty()) {
                return getUsername() + " / " + getDomain();
            } else if (getUsername() != null && !getUsername().isEmpty()) {
                return getUsername();
            } else if (getDomain() != null && !getDomain().isEmpty()) {
                return getDomain();
            }
            return "";
        }

        @Override
        public int compareTo(Record another) {
            if (domain_ != null) {
                if (domain_.compareTo(another.getDomain()) != 0) {
                    return domain_.compareToIgnoreCase(another.getDomain());
                }
            }
            if (username_ != null) {
                if (username_.compareTo(another.getUsername()) != 0) {
                    return username_.compareToIgnoreCase(another.getUsername());
                }
            }
            return -1;
        }
    }
}