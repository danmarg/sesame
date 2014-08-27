package net.af0.sesame;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.FileProvider;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import net.sqlcipher.database.SQLiteException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Common code used in multiple activities.
 */
class Common {
    /**
     * Export the keys database. The user chooses what to do with it.
     */
    static boolean ExportKeys(Context ctx) {
        // First, copy the database to a temporary file.
        File dst;
        try {
            String exportName = String.format(Constants.KEY_EXPORT_FILE,
                    new SimpleDateFormat(Constants.KEY_EXPORT_DATE_FORMAT).format(
                            Calendar.getInstance().getTime())
            );
            dst = new File(ctx.getCacheDir(), exportName);
            FileOutputStream dstStr = new FileOutputStream(dst);
            SQLCipherDatabase.ExportDatabase(ctx, dstStr);
        } catch (java.io.IOException e) {
            Log.e("EXPORT", e.toString());
            DisplayException(ctx, ctx.getString(R.string.export_keys_failure_title), e);
            return false;
        }
        // Now build an intent to share the file.
        Uri contentUri = FileProvider.getUriForFile(ctx, Constants.KEY_EXPORT_URI, dst);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.setType(Constants.KEY_EXPORT_MIME);
        ctx.startActivity(Intent.createChooser(shareIntent, ctx.getResources().getText(
                R.string.action_export)).setFlags(
                // Grant read permission to the receiver of the intent. The file should not be
                // readable by anyone else.
                Intent.FLAG_GRANT_READ_URI_PERMISSION));
        return true;
    }

    /**
     * Handle key database import after user has chosen a file. Prompts user for password, and
     * errors if the password is bad or the format is wrong.
     */
    static void onImportKeysResult(final Context ctx, Intent data, final Runnable importCallback) {
        // This is the URI of the imported data.
        final Uri uri = data.getData();
        final InputStream src;
        final OutputStream os;
        // The file to cache the imported data in while we parse it.
        final File dst;
        try {
            src = ctx.getContentResolver().openInputStream(uri);
            dst = File.createTempFile(Constants.KEY_IMPORT_TMPNAME, Constants.KEY_IMPORT_SUFFIX,
                    ctx.getCacheDir());
            os = new FileOutputStream(dst, false);
            int l;
            byte[] buf = new byte[1024];
            while ((l = src.read(buf)) != -1) {
                os.write(buf, 0, l);
            }
        } catch (IOException e) {
            Log.e("IMPORT", e.toString());
            DisplayException(ctx, ctx.getString(R.string.import_keys_failure_title), e);
            return;
        }

        // Set up a progress spinner for while we unlock and import, since that takes a while.
        final ProgressDialog progress = new ProgressDialog(ctx);
        progress.setTitle(R.string.unlock_progress_unlocking);
        // Build a dialog for the password prompt.
        final EditText passwordText = new EditText(ctx);
        passwordText.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
        alert.setTitle(ctx.getString(R.string.title_enter_password));
        alert.setMessage(ctx.getString(R.string.import_database_password_message));
        alert.setView(passwordText);
        alert.setPositiveButton(R.string.action_unlock, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Retrieve the password.
                final char[] password = new char[passwordText.length()];
                passwordText.getText().getChars(0, password.length, password, 0);
                // Show the dialog spinner.
                progress.show();
                AsyncTask<Void, Void, Boolean> importTask = new AsyncTask<Void, Void, Boolean>() {
                    Exception exception;

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        boolean success = false;
                        try {
                            SQLCipherDatabase.ImportDatabase(ctx, dst.getAbsolutePath(), password);
                            success = true;
                        } catch (SQLiteException e) {
                            Log.e("IMPORT", e.toString());
                            exception = e;
                        } catch (IOException e) {
                            Log.e("IMPORT", e.toString());
                            exception = e;
                        }
                        return success;
                    }

                    @Override
                    protected void onPostExecute(final Boolean success) {
                        progress.dismiss();
                        if (success) {
                            if (importCallback != null) {
                                importCallback.run();
                            }
                        } else {
                            Log.w("Importing database", exception.toString());
                            DisplayException(ctx, ctx.getString(R.string.import_keys_failure_title),
                                    exception);
                        }
                    }

                    @Override
                    protected void onCancelled() {
                    }
                };
                importTask.execute();
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Return...
            }
        });
        alert.show();
    }

    /**
     * Begin key import by prompting user for key file.
     */
    static void StartImportKeys(Activity ctx) {
        Intent importIntent = new Intent();
        importIntent.setAction(Intent.ACTION_GET_CONTENT);
        importIntent.setType(Constants.KEY_IMPORT_MIME);
        ctx.startActivityForResult(importIntent, Constants.IMPORT_DATABASE_RESULT);
    }

    /**
     * Common function to display an exception or error to the user.
     */
    static void DisplayException(Context ctx, String title, Exception e) {
        if (ctx == null) {
            return;
        }
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(e.getLocalizedMessage())
                .setNeutralButton(ctx.getString(R.string.dismiss), null)
                .create().show();
    }

    /**
     * An object implementing this must be passed to LoadRecordFromDatabase, to give a callback to
     * pass the loaded record to upon completion.
     */
    static interface DatabaseLoadCallbacks {
        void OnLoadRecord(SQLCipherDatabase.Record record);
    }

    /**
     * Load an item from the database in an AsyncTask and call callbacks.OnRecordLoad() when done.
     * Null will be passed to callbacks.OnLoadRecord if no record is found.
     * @param id
     * @param callbacks
     */
    static void LoadRecordFromDatabase(final long id, final DatabaseLoadCallbacks callbacks) {
        AsyncTask<Void, Void, Boolean> loadItemTask =
                new AsyncTask<Void, Void, Boolean>(){
                    SQLCipherDatabase.Record item;
                    @Override
                    public Boolean doInBackground(Void... param) {
                        item = SQLCipherDatabase.getRecord(id);
                        return item != null;
                    }

                    @Override
                    protected void onPostExecute(final Boolean success) {
                        callbacks.OnLoadRecord(item);
                    }

                    @Override
                    protected void onCancelled() {
                        callbacks.OnLoadRecord(null);
                    }
                };
        loadItemTask.execute();
    }
}
