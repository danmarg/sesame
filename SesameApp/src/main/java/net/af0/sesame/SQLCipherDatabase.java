package net.af0.sesame;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class wrapping the database interactions.
 */
public final class SQLCipherDatabase {
    private static SQLiteDatabase database_;
    private static SQLHelper helper_;
    private static String[] allColumns_ = {
            SQLHelper.COLUMN_ID, SQLHelper.COLUMN_USERNAME, SQLHelper.COLUMN_DOMAIN,
            SQLHelper.COLUMN_PASSWORD, SQLHelper.COLUMN_REMARKS};

    public static Record createRecord(String username, String domain, String password,
                                      String remarks) {
        ContentValues values = new ContentValues();
        values.put(SQLHelper.COLUMN_USERNAME, username);
        values.put(SQLHelper.COLUMN_DOMAIN, domain);
        values.put(SQLHelper.COLUMN_PASSWORD, password);
        values.put(SQLHelper.COLUMN_REMARKS, remarks);
        long id = database_.insert(SQLHelper.TABLE_KEYS, null, values);
        Cursor crs = database_.query(SQLHelper.TABLE_KEYS, allColumns_,
                SQLHelper.COLUMN_ID + "=" + id, null, null, null, null);
        crs.moveToFirst();
        Record r = toRecord(crs);
        crs.close();
        return r;
    }

    public static void deleteRecord(Record r) {
        database_.delete(SQLHelper.TABLE_KEYS, SQLHelper.COLUMN_ID + "=" + r.getId(), null);
    }

    public static void updateRecord(Record r) {
        ContentValues values = new ContentValues();
        values.put(SQLHelper.COLUMN_USERNAME, r.getUsername());
        values.put(SQLHelper.COLUMN_DOMAIN, r.getDomain());
        values.put(SQLHelper.COLUMN_PASSWORD, r.getPassword());
        values.put(SQLHelper.COLUMN_REMARKS, r.getRemarks());
        database_.update(SQLHelper.TABLE_KEYS, values, SQLHelper.COLUMN_ID + "=" + r.getId(), null);
    }

    public static Record getRecord(long record_id) {
        Cursor crs = database_.query(SQLHelper.TABLE_KEYS, allColumns_,
                SQLHelper.COLUMN_ID + "=" + record_id,
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
        Cursor crs = database_.query(SQLHelper.TABLE_KEYS, allColumns_, null, null, null, null,
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
            helper_ = new SQLHelper(ctx);
        }
        database_ = helper_.getWritableDatabase(password);
    }

    public static void ImportDatabase(String path, char[] password) {
        if (isLocked()) {
            throw new SQLiteException("Database must be unlocked");
        }
        SQLiteDatabase imported = SQLiteDatabase.openDatabase(path, password, null,
                SQLiteDatabase.OPEN_READONLY);
        Cursor crs = imported.query(SQLHelper.TABLE_KEYS, allColumns_, null, null, null, null,
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
        // Open the database using openOrCreateDatabase, so we can set the KDF iter before keying.
        database_ = SQLiteDatabase.openOrCreateDatabase(getDatabaseFilePath(ctx).getPath(),
                password, null, new SQLiteDatabaseHook() {
                    @Override
                    public void preKey(SQLiteDatabase database) {
                        database.rawExecSQL(String.format("PRAGMA kdf_iter = %d",
                                Constants.KDF_ITERATIONS));
                    }
                    @Override
                    public void postKey(SQLiteDatabase database) {
                        database.execSQL(SQLHelper.DATABASE_CREATE);
                    }
                });
        // Reopen so we can use the SQLHelper to manage the database. This is a little hackish.
        database_.close();
        helper_ =  new SQLHelper(ctx);
        database_ = helper_.getWritableDatabase(password);
    }

    public static void DeleteDatabase(Context ctx) {
        ctx.deleteDatabase(SQLHelper.DATABASE_NAME);
    }

    public static void ChangePassword(String password) {
        // TODO: Switch this from a String to a char[] and just manually escape.
        database_.rawExecSQL("PRAGMA rekey = " + DatabaseUtils.sqlEscapeString(password) + ";");
    }

    public static boolean isLocked() {
        return database_ == null || !database_.isOpen();
    }

    public static void Lock() {
        if (database_ != null) {
            database_.close();
        }
        if (helper_ != null) {
            helper_.close();
        }
    }

    public static boolean Exists(Context ctx) {
        return ctx.getDatabasePath(SQLHelper.DATABASE_NAME).exists();
    }

    public static File getDatabaseFilePath(Context ctx) {
        return ctx.getDatabasePath(SQLHelper.DATABASE_NAME);
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

    public static class SQLHelper extends SQLiteOpenHelper {
        public static final String TABLE_KEYS = "keys";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_USERNAME = "username";
        public static final String COLUMN_DOMAIN = "domain";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_REMARKS = "remarks";
        public static final int DATABASE_VERSION = 3;
        public static final String DATABASE_NAME = "keys.db";
        static final String DATABASE_CREATE = "create table " +
                TABLE_KEYS + "(" +
                COLUMN_ID + " integer primary key autoincrement, " +
                COLUMN_USERNAME + " text, " +
                COLUMN_DOMAIN + " text," +
                COLUMN_PASSWORD + " text," +
                COLUMN_REMARKS + " text);";

        public SQLHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {}

        @Override
        public void onUpgrade(SQLiteDatabase database, int vOld, int vNew) {
            throw new UnsupportedOperationException("database upgrades not supported");
        }
    }
}