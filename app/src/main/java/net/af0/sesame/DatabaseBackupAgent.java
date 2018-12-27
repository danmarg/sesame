package net.af0.sesame;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;

/**
 * BackupAgent to handle automatic database backups.
 */
public class DatabaseBackupAgent extends BackupAgentHelper {
    @Override
    public void onCreate() {
        // Backup database, if requested.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean(Constants.PREFS_BACKUP, true)) {
            return;  // No backup requested
        }
        FileBackupHelper file_helper = new FileBackupHelper(
                this, SQLCipherDatabase.Instance().getDatabaseFilePath(this).getName());
        addHelper("db_file_helper", file_helper);
        // Backup prefs, too.
        SharedPreferencesBackupHelper prefs_helper =
                new SharedPreferencesBackupHelper(this, Constants.PREFS_BACKUP,
                        Constants.DB_METADATA_PREF);
        addHelper("prefs_helper", prefs_helper);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        // Begin a transaction to prevent database changes while backing up.
        SQLCipherDatabase.Instance().beginTransaction();
        try {
            super.onBackup(oldState, data, newState);
        } finally {
            SQLCipherDatabase.Instance().endTransaction();
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
            throws IOException {
        // Begin a transaction to ensure the database has no other writes while we import.
        // TODO: Investigate if this is actually safe if the database is already unlocked. We could
        // end up overwriting an unlocked database with one with a different password; god knows
        // what would happen then. The alternatives are poor, however; if the password for the
        // backup is different than the current password, we have no way to import it without
        // prompting the user.
        SQLCipherDatabase.Instance().beginTransaction();
        try {
            super.onRestore(data, appVersionCode, newState);
        } finally {
            SQLCipherDatabase.Instance().endTransaction();
        }
    }

    @Override
    public File getFilesDir() {
        File path = SQLCipherDatabase.Instance().getDatabaseFilePath(this);
        return path.getParentFile();
    }
}
