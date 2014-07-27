package net.af0.sesame;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;

public class DatabaseBackupAgent extends BackupAgentHelper {

    @Override
    public void onCreate() {
        // Backup database, if requested.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean(Constants.PREFS_BACKUP, true)) {
            return;  // No backup requested
        }
        FileBackupHelper file_helper = new FileBackupHelper(
                this, SQLCipherDatabase.SQLHelper.DATABASE_NAME);
        addHelper("db_file_helper", file_helper);
        // Backup prefs, too.
        SharedPreferencesBackupHelper prefs_helper =
                new SharedPreferencesBackupHelper(this, Constants.PREFS_BACKUP);
        addHelper("prefs_helper", prefs_helper);
    }

    @Override
    public File getFilesDir() {
        File path = getDatabasePath(SQLCipherDatabase.SQLHelper.DATABASE_NAME);
        return path.getParentFile();
    }
}