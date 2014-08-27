package net.af0.sesame;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Common code used in multiple activities.
 */
class Common {
    /**
     * Export the keys database. The user chooses what to do with it.
     */
    static boolean ExportKeys(final Context ctx) {
        final ProgressDialog progress = new ProgressDialog(ctx);
        progress.setTitle(R.string.progress_loading);
        // First, copy the database to a temporary file.
        File dst;
        try {
            String exportName = String.format(Constants.KEY_EXPORT_FILE,
                    new SimpleDateFormat(Constants.KEY_EXPORT_DATE_FORMAT).format(
                            Calendar.getInstance().getTime())
            );
            dst = new File(ctx.getCacheDir(), exportName);
            FileOutputStream dstStr = new FileOutputStream(dst);
            SQLCipherDatabase.Callbacks<Boolean> callbacks = new
                    SQLCipherDatabase.Callbacks<Boolean>() {
                        @Override
                        public void OnFinish(Boolean success) {
                        }

                        @Override
                        public void OnException(Exception exception) {
                            progress.dismiss();
                            Log.w("Importing database", exception.toString());
                            DisplayException(ctx, ctx.getString(R.string.import_keys_failure_title),
                                    exception);
                        }

                        @Override
                        public void OnCancelled() {
                        }
                    };
            SQLCipherDatabase.ExportDatabase(ctx, dstStr, callbacks);
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
        try {
            src = ctx.getContentResolver().openInputStream(uri);
        } catch (IOException exception) {
            Log.w("Importing database", exception.toString());
            DisplayException(ctx, ctx.getString(R.string.import_keys_failure_title),
                    exception);
            return;
        }

        // Set up a progress spinner for while we unlock and import, since that takes a while.
        final ProgressDialog progress = new ProgressDialog(ctx);
        progress.setTitle(R.string.progress_loading);
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
                SQLCipherDatabase.Callbacks<Boolean> callbacks = new
                        SQLCipherDatabase.Callbacks<Boolean>() {
                            @Override
                            public void OnFinish(Boolean success) {
                                progress.dismiss();
                                if (importCallback != null) {
                                    importCallback.run();
                                }
                            }

                            @Override
                            public void OnException(Exception exception) {
                                progress.dismiss();
                                Log.w("Importing database", exception.toString());
                                DisplayException(ctx, ctx.getString(R.string.import_keys_failure_title),
                                        exception);
                            }

                            @Override
                            public void OnCancelled() {
                                progress.dismiss();
                                if (importCallback != null) {
                                    importCallback.run();
                                }
                            }
                        };
                SQLCipherDatabase.ImportDatabase(ctx, src, password, callbacks);
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

}
