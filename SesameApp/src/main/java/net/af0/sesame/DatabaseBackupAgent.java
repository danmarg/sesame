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

public class DatabaseBackupAgent extends BackupAgentHelper {

    @Override
    public void onCreate() {
        // Backup database, if requested.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean(Constants.PREFS_BACKUP, true)) {
            return;  // No backup requested
        }
        FileBackupHelper file_helper = new FileBackupHelper(
                this, SQLCipherDatabase.getDatabaseFilePath(this).getName());
        addHelper("db_file_helper", file_helper);
        // Backup prefs, too.
        SharedPreferencesBackupHelper prefs_helper =
                new SharedPreferencesBackupHelper(this, Constants.PREFS_BACKUP);
        addHelper("prefs_helper", prefs_helper);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor  newState) throws IOException {
        SQLCipherDatabase.BeginTransaction();
        super.onBackup(oldState, data, newState);
        SQLCipherDatabase.EndTransaction();
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
        throws IOException {
        SQLCipherDatabase.BeginTransaction();
        super.onRestore(data, appVersionCode, newState);
        SQLCipherDatabase.EndTransaction();
    }

    @Override
    public File getFilesDir() {
        File path = SQLCipherDatabase.getDatabaseFilePath(this);
        return path.getParentFile();
    }
}