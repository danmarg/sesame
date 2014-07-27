package net.af0.sesame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.FileProvider;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.google.common.io.Files;

import net.sqlcipher.database.SQLiteException;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

class Common {
    static boolean ExportKeys(Context ctx) {
        File src, dst;
        try {
            src = SQLCipherDatabase.getDatabaseFilePath(ctx);
            String exportName = String.format(Constants.KEY_EXPORT_FILE,
                    new SimpleDateFormat(Constants.KEY_EXPORT_DATE_FORMAT).format(
                            Calendar.getInstance().getTime())
            );
            dst = new File(ctx.getCacheDir(), exportName);
            Files.copy(src, dst);
        } catch (java.io.IOException e) {
            Log.e("EXPORT", e.toString());
            DisplayException(ctx, ctx.getString(R.string.export_keys_failure_title), e);
            return false;
        }
        Uri contentUri = FileProvider.getUriForFile(ctx, Constants.KEY_EXPORT_URI, dst);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.setType(Constants.KEY_EXPORT_MIME);
        ctx.startActivity(Intent.createChooser(shareIntent, ctx.getResources().getText(
                R.string.action_export)).setFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION));
        return true;
    }

    static void onImportKeysResult(final Context ctx, Intent data, final Runnable importCallback) {
        final Uri uri = data.getData();
        Log.e("ASDF", uri.getPath());
        final InputStream src;
        final OutputStream os;
        final File dst;
        try {
           src = ctx.getContentResolver().openInputStream(uri);
           dst = File.createTempFile(Constants.KEY_IMPORT_TMPNAME, "", ctx.getCacheDir());
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

        // Get password.
        final EditText passwordText = new EditText(ctx);
        passwordText.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
        alert.setTitle(ctx.getString(R.string.title_enter_password));
        alert.setMessage(ctx.getString(R.string.import_database_password_message));
        alert.setView(passwordText);
        alert.setPositiveButton(R.string.action_unlock, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    char[] password = new char[passwordText.length()];
                    passwordText.getText().getChars(0, password.length, password, 0);
                    SQLCipherDatabase.ImportDatabase(dst.getAbsolutePath(), password);
                    if (importCallback != null) {
                        importCallback.run();
                    }
                } catch (SQLiteException e) {
                    Log.e("IMPORT", e.toString());
                    DisplayException(ctx, ctx.getString(R.string.import_keys_failure_title), e);
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Return...
            }
        });
        alert.show();
    }

    static void StartImportKeys(Activity ctx) {
        Intent importIntent = new Intent();
        importIntent.setAction(Intent.ACTION_GET_CONTENT);
        importIntent.setType("*/*");
        ctx.startActivityForResult(importIntent, Constants.IMPORT_DATABASE_RESULT);
    }

    static void DisplayException(Context ctx, String title, Exception e) {
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(e.getLocalizedMessage())
                .setNeutralButton(ctx.getString(R.string.dismiss), null)
                .create().show();
    }
}
