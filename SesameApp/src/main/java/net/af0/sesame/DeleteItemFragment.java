package net.af0.sesame;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import net.sqlcipher.database.SQLiteException;

public final class DeleteItemFragment extends DialogFragment {
    private ProgressDialog progress_;
    private ItemListActivity listActivity_;
    private ItemDetailActivity detailActivity_;
    private long itemId_;
    // Async task for deleting an item
    private DeleteTask deleteTask_ = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        itemId_ = getArguments().getLong(Constants.ARG_ITEM_ID, -1);
        if (itemId_ == -1) {
            throw new IllegalStateException("Cannot delete an invalid item  ID");
        }
        Activity activity = getActivity();
        if (activity.getClass() == ItemListActivity.class) {
            listActivity_ = (ItemListActivity) activity;
        } else if (activity.getClass() == ItemDetailActivity.class) {
            detailActivity_ = (ItemDetailActivity) activity;
        } else {
            Log.e("DELETE FRAGMENT",
                    String.format("Unexpected parent activity %s", activity.getClass()));
        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.really_delete)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        progress_ = new ProgressDialog(getActivity());
                        progress_.setTitle(R.string.delete_progress_deleting);
                        progress_.show();
                        deleteTask_ = new DeleteTask();
                        deleteTask_.execute((Void) null);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onPause() {
        if (progress_ != null) {
            progress_.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (progress_ != null) {
            progress_.dismiss();
        }
        super.onStop();
    }

    @Override
    public void onResume() {
        if (progress_ != null) {
            progress_.show();
        }
        super.onResume();
    }

    /**
     * Represents an asynchronous database delete.
     */
    public class DeleteTask extends AsyncTask<Void, Void, Boolean> {
        private SQLiteException exception_;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                SQLCipherDatabase.deleteRecord(SQLCipherDatabase.getRecord(itemId_));
            } catch (SQLiteException e) {
                Log.w("Deleting record: %s", e.toString());
                exception_ = e;
                progress_.dismiss();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            deleteTask_ = null;
            progress_.dismiss();
            progress_ = null;
            if (success) {
                if (listActivity_ != null) {
                    listActivity_.refreshListFromDatabase();
                    listActivity_.onItemSelected(null);
                }
                if (detailActivity_ != null) {
                    detailActivity_.finish();
                }
            } else {
                Log.e("DELETE", exception_.toString());
                Common.DisplayException(getActivity(),
                        getString(R.string.error_deleting_item_title),
                        exception_);
            }
        }

        @Override
        protected void onCancelled() {
            deleteTask_ = null;
        }
    }
}