package net.af0.sesame;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
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
                            try {  // Can throw exception on rotate, etc.
                                progress.dismiss();
                            } catch (IllegalArgumentException ex) {}
                        }

                        @Override
                        public void OnException(Exception exception) {
                            try {  // Can throw exception on rotate, etc.
                                progress.dismiss();
                            } catch (IllegalArgumentException ex) {}
                            Log.w("EXPORT", exception.toString());
                            DisplayException(ctx, ctx.getString(R.string.export_keys_failure_title),
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
