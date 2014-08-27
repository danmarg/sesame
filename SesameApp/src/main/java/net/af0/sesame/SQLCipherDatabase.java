package net.af0.sesame;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import net.sqlcipher.Cursor;
import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Singleton class wrapping the database interactions.
 */
public final class SQLCipherDatabase {
    static final String COLUMN_USERNAME = "username";
    static final String COLUMN_DOMAIN = "domain";
    private static final String TABLE_KEYS = "keys";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_REMARKS = "remarks";
    static final String DATABASE_CREATE = "create table " +
            TABLE_KEYS + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_USERNAME + " text, " +
            COLUMN_DOMAIN + " text," +
            COLUMN_PASSWORD + " text," +
            COLUMN_REMARKS + " text);";
    private static final String[] allColumns_ = {
            // Stupid. I named the field "id" and SimpleCursorFactory expects "id". Rather than
            // rename and break compatibility to gracefully handle database upgrades, let's just
            // alias it. I hope nobody is reading this...
            COLUMN_ID + " AS _id", COLUMN_USERNAME, COLUMN_DOMAIN,
            COLUMN_PASSWORD, COLUMN_REMARKS};
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "keys.db";
    private static net.sqlcipher.database.SQLiteOpenHelper helper_;
    private static SQLiteDatabase database_;


    /**
     * An object implementing this must be passed to getRecord or createRecord, to give a callback
     * to pass the resulting record to upon completion.
     */
    static interface Callbacks {
        void OnLoadRecord(Record record);
        void OnException(SQLException exception);
        void OnSaveRecord(boolean success, Record record);
        void OnCancelled();
    }

    public static void createRecord(final String username, final String domain,
                                      final String password, final String remarks,
                                      final Callbacks callbacks) {
        new AsyncTask<Void, Void, Boolean>(){
            Record r;
            SQLException exception;
            @Override
            public Boolean doInBackground(Void... param) {
                try {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_USERNAME, username);
                    values.put(COLUMN_DOMAIN, domain);
                    values.put(COLUMN_PASSWORD, password);
                    values.put(COLUMN_REMARKS, remarks);
                    long id = database_.insert(TABLE_KEYS, null, values);
                    Cursor crs = database_.query(TABLE_KEYS, allColumns_,
                            COLUMN_ID + "=" + id, null, null, null, null);
                    crs.moveToFirst();
                    r = toRecord(crs);
                    crs.close();
                    return r != null;
                } catch(SQLException e) {
                    exception = e;
                    return false;
                }
            }

            @Override
            protected void onPostExecute(final Boolean success) {
                if (callbacks != null) {
                    if (exception != null) {
                        callbacks.OnException(exception);
                    } else {
                        callbacks.OnLoadRecord(r);
                    }
                }
            }
            @Override
            protected void onCancelled() {
                if (callbacks != null) {
                    callbacks.OnCancelled();
                }
            }
        }.execute();
    }

    public static void deleteRecord(final long record_id, final Callbacks callbacks) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                database_.delete(TABLE_KEYS, COLUMN_ID + "=" + record_id, null);
                return true;
            }

            @Override
            protected void onPostExecute(final Boolean result) {
                if (callbacks != null) {
                    callbacks.OnSaveRecord(result, null);
                }
            }
            @Override
            protected void onCancelled() {
                if (callbacks != null) {
                    callbacks.OnCancelled();
                }
            }
        }.execute();
    }

    public static void updateRecord(final Record r, final Callbacks callbacks) {
        new AsyncTask<Void, Void, Boolean>() {
            SQLException exception;
            @Override
            protected Boolean doInBackground(Void... params) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_USERNAME, r.getUsername());
                values.put(COLUMN_DOMAIN, r.getDomain());
                values.put(COLUMN_PASSWORD, r.getPassword());
                values.put(COLUMN_REMARKS, r.getRemarks());
                try {
                    database_.update(TABLE_KEYS, values, COLUMN_ID + "=" + r.getId(), null);
                } catch (SQLException e) {
                    exception = e;
                }
                return true;
            }

            @Override
            protected void onPostExecute(final Boolean result) {
                if (callbacks != null) {
                    if (exception != null) {
                        callbacks.OnException(exception);
                    } else {
                        callbacks.OnSaveRecord(result, r);
                    }
                }
            }
            @Override
            protected void onCancelled() {
                if (callbacks != null) {
                    callbacks.OnCancelled();
                }
            }
        }.execute();
    }

    public static void getRecord(final long record_id,  final Callbacks callbacks) {
        new AsyncTask<Void, Void, Boolean>(){
            Record r;
            @Override
            public Boolean doInBackground(Void... param) {
                Cursor crs = database_.query(TABLE_KEYS, allColumns_,
                        COLUMN_ID + "=" + record_id,
                        null, null, null, null);
                if (crs.getCount() == 0) {
                    crs.close();
                    return false;
                }
                crs.moveToFirst();
                r = toRecord(crs);
                crs.close();
                return r != null;
            }

            @Override
            protected void onPostExecute(final Boolean success) {
                callbacks.OnLoadRecord(r);
            }
            @Override
            protected void onCancelled() {
                if (callbacks != null) {
                    callbacks.OnCancelled();
                }
            }
        }.execute();
    }

    public static Cursor getAllCursor() {
        return database_.query(TABLE_KEYS, allColumns_, null, null, null, null,
                COLUMN_DOMAIN + " COLLATE NOCASE ASC, " + COLUMN_USERNAME + " COLLATE NOCASE ASC");
    }

    public static Cursor getContaining(String substr) {
        String s = DatabaseUtils.sqlEscapeString("%" + substr + "%");
        return database_.query(TABLE_KEYS, allColumns_,
                String.format("%s LIKE %s OR %s LIKE %s", COLUMN_DOMAIN, s, COLUMN_USERNAME, s),
                null, null, null, COLUMN_DOMAIN + " COLLATE NOCASE ASC, " + COLUMN_USERNAME +
                        " COLLATE NOCASE ASC"
        );
    }

    public static Record toRecord(Cursor crs) {
        Record r = new Record();
        r.setId(crs.getLong(0));
        r.setUsername(crs.getString(1));
        r.setDomain(crs.getString(2));
        r.setPassword(crs.getString(3));
        r.setRemarks(crs.getString(4));

        return r;
    }

    private static DatabaseMetadata.Database getMetadataFromPrefs(Context ctx) {
        DatabaseMetadata.Database metadata = DatabaseMetadata.Database.newBuilder()
                .setVersion(DATABASE_VERSION)
                .setKdfIter(Constants.KDF_ITER)
                .build();
        SharedPreferences prefs = ctx.getSharedPreferences(Constants.DB_METADATA_PREF,
                Context.MODE_PRIVATE);
        try {
            metadata =
                    DatabaseMetadata.Database.newBuilder(metadata).mergeFrom(
                            DatabaseMetadata.Database.parseFrom(Base64.decode(prefs.getString(
                                            Constants.DB_METADATA_PREF, ""),
                                    Base64.DEFAULT
                            ))
                    ).build();
        } catch (InvalidProtocolBufferException ex) {
            Log.e("IMPORT", ex.toString());
            // Go with defaults anyway. Uh oh...
        }
        return metadata;
    }

    public static synchronized void OpenDatabase(Context ctx, char[] password) {
        if (helper_ == null) {
            SQLiteDatabase.loadLibs(ctx);
            helper_ = new OpenHelper(ctx, getMetadataFromPrefs(ctx));
        }
        database_ = helper_.getWritableDatabase(password);
    }

    // The import/export format for the database is
    //   # bytes: DatabaseMetadata, serialized with writeDelimitedTo() (i.e. preceded by a size
    //            varint). Parse with parseDelimitedFrom().
    //      rest: SQLCipher DB
    // So we have to strip the first n bytes before passing the file off to SQLCipher.
    public static void ImportDatabase(Context ctx, String path, char[] password)
            throws IOException {
        if (isLocked()) {
            throw new SQLiteException("Database must be unlocked");
        }
        // Get temporary path to write database minus metadata to.
        File tmpDb = File.createTempFile(Constants.KEY_IMPORT_TMPNAME, Constants.KEY_IMPORT_SUFFIX,
                ctx.getCacheDir());
        DatabaseMetadata.Database metadata = DatabaseMetadata.Database.newBuilder()
                .setVersion(DATABASE_VERSION)
                .setKdfIter(Constants.KDF_ITER)
                .build();
        OutputStream tmpDbStr = new FileOutputStream(tmpDb);
        InputStream rawInput = new FileInputStream(path);
        try {
            // Parse the DatabaseMetadata.
            metadata = DatabaseMetadata.Database.parseDelimitedFrom(rawInput);
            // Now get the remaining buffer.
            byte[] buf = new byte[1024];
            int b;
            while ((b = rawInput.read(buf)) != -1) {
                tmpDbStr.write(buf, 0, b);
            }
        } finally {
            tmpDbStr.close();
        }
        // Now everything is hunky dory.
        SQLiteDatabase imported = SQLiteDatabase.openDatabase(tmpDb.getPath(), password, null,
                SQLiteDatabase.OPEN_READONLY, new DatabaseHook(metadata));
        if (imported.getVersion() != DATABASE_VERSION) {
            // Because we're not using OpenHelper here, we have to handle version mismatches
            // ourselves. For now, versioning is unsupported!
            throw new UnsupportedOperationException("Upgrade not supported!");

        }
        Cursor crs = imported.query(TABLE_KEYS, allColumns_, null, null, null, null,
                null);
        for (crs.moveToFirst(); !crs.isAfterLast(); crs.moveToNext()) {
            Record r = toRecord(crs);
            createRecord(r.getUsername(), r.getDomain(), r.getPassword(), r.getRemarks(), null);
        }
        crs.close();
    }

    public static void ExportDatabase(Context ctx, OutputStream output) throws IOException {
        getMetadataFromPrefs(ctx).writeDelimitedTo(output);
        InputStream rawInput = new FileInputStream(getDatabaseFilePath(ctx));
        byte[] buf = new byte[1024];
        int b;
        BeginTransaction();
        try {
            while ((b = rawInput.read(buf)) != -1) {
                output.write(buf, 0, b);
            }
        } finally {
            rawInput.close();
            EndTransaction();
        }
    }

    public static void CreateDatabase(Context ctx, char[] password) {
        if (Exists(ctx)) {
            throw new SQLiteException("file already exists");
        }
        // Store a DatabaseMetadata object with our creation defaults.
        DatabaseMetadata.Database metadata = DatabaseMetadata.Database.newBuilder().setVersion(
                DATABASE_VERSION).setKdfIter(Constants.KDF_ITER).build();
        SharedPreferences.Editor preferencesEditor = ctx.getSharedPreferences(
                Constants.DB_METADATA_PREF, Context.MODE_PRIVATE).edit();
        preferencesEditor.putString(Constants.DB_METADATA_PREF,
                Base64.encodeToString(metadata.toByteArray(), Base64.DEFAULT));
        preferencesEditor.commit();
        // Open normally.
        OpenDatabase(ctx, password);
    }

    public static synchronized void DeleteDatabase(Context ctx) {
        Lock();  // Throw away the open database handle.
        ctx.deleteDatabase(DATABASE_NAME);
    }

    public static void ChangePassword(String password) {
        // TODO: Switch this from a String to a char[] and just manually escape.
        database_.rawExecSQL("PRAGMA rekey = " + DatabaseUtils.sqlEscapeString(password) + ";");
    }

    public static synchronized boolean isLocked() {
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

    // Begin a transaction on the open database. Useful for preventing writes during, say, a file
    // backup.
    public static synchronized void BeginTransaction() {
        if (!isLocked()) {
            database_.beginTransaction();
        }
    }

    public static synchronized void EndTransaction() {
        if (!isLocked()) {
            database_.endTransaction();
        }
    }

    private static class OpenHelper extends net.sqlcipher.database.SQLiteOpenHelper {
        public OpenHelper(Context ctx, DatabaseMetadata.Database metadata) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION, new DatabaseHook(metadata));
        }

        public void onCreate(SQLiteDatabase database) {
            database.execSQL(DATABASE_CREATE);
        }

        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            throw new UnsupportedOperationException("Upgrade not supported");
        }
    }

    private static class DatabaseHook implements SQLiteDatabaseHook {
        final DatabaseMetadata.Database metadata_;

        public DatabaseHook(DatabaseMetadata.Database metadata) {
            metadata_ = metadata;
        }

        @Override
        public void preKey(SQLiteDatabase database) {
        }

        @Override
        public void postKey(SQLiteDatabase database) {
            database.rawExecSQL(String.format("PRAGMA kdf_iter = %d",
                    metadata_.getKdfIter()));
            database.rawExecSQL(String.format("PRAGMA cipher = '%s'",
                    metadata_.getCipher()));
        }
    }

    // Database model.
    public static class Record {
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
    }
}