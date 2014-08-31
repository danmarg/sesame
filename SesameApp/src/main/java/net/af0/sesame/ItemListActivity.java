package net.af0.sesame;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.SearchView;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionItemTarget;
import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.github.amlcurran.showcaseview.targets.Target;

import net.sqlcipher.CursorWrapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * An activity representing a list of Items. This activity has different presentations for handset
 * and tablet-size devices. On handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItemDetailActivity} representing item details. On tablets, the activity presents
 * the list of items and item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a {@link ItemListFragment} and
 * the item details (if present) is a {@link ItemDetailFragment}.
 * <p/>
 * This activity also implements the required {@link ItemListFragment.Callbacks} interface to listen
 * for item selections.
 */
public final class ItemListActivity extends FragmentActivity
        implements ItemListFragment.Callbacks, SearchView.OnQueryTextListener,
        LoaderManager.LoaderCallbacks<Cursor>, SQLCipherDatabase.Callbacks<Boolean> {

    private static final int ADD_RECORD_REQUEST = 0;
    private static final int EDIT_RECORD_REQUEST = 1;

    private static final int LOADER_ID = 1;
    // ShowcaseView for first run.
    ShowcaseView showcase_;
    // Progress spinner for database import.
    private ProgressDialog progress_;

    // Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
    private boolean twoPane_;
    private long selectedId_;
    private ItemListFragment itemListFragment_;
    private BlobCursorAdapter itemListAdapter_;

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

        // If we're locked, go to the unlock view.
        if (SQLCipherDatabase.isLocked()) {
            startActivity(new Intent(getBaseContext(), UnlockActivity.class).setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            refreshListFromDatabase();
        }
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_activity_actions, menu);
        // Set up search widget
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(this);

        // Show help showcase.
        Target t = new ActionItemTarget(this, R.id.add);
        int text;
        try {
            // This seems to be the only way to tell if the action bar item is visible?
            // See https://github.com/amlcurran/ShowcaseView/issues/195.
            t.getPoint();
            text = R.string.showcase_add_content_no_overflow;
        } catch (NullPointerException ex) {
            t = new ActionViewTarget(this, ActionViewTarget.Type.OVERFLOW);
            text = R.string.showcase_add_content_in_overflow;
        }
        showcase_ = new ShowcaseView.Builder(this, true)
                .setTarget(t)
                .setContentTitle(R.string.showcase_add_title)
                .setContentText(text)
                .setStyle(R.style.AppTheme)
                .singleShot(Constants.SINGLE_SHOT_ITEM_LIST)
                .build();

        if (twoPane_) {
            menu.add(0, R.id.action_edit, Menu.NONE, R.string.edit);
            menu.add(0, R.id.action_delete, Menu.NONE, R.string.delete);
        }

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Handle search queries.
     */
    public boolean onQueryTextSubmit(String query) {
        itemListAdapter_.getFilter().filter(query);
        return true;
    }

    /**
     * Handle search queries.
     */
    public boolean onQueryTextChange(String query) {
        itemListAdapter_.getFilter().filter(query);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_item_list);

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/values-large and res/values-sw600dp). If this view is present, then the activity
            // should be in two-pane mode.x
            twoPane_ = true;

            // In two-pane mode, list items should be given the 'activated' state when touched.
            ((ItemListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.item_list))
                    .setActivateOnItemClick();
        }

        itemListFragment_ = (ItemListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.item_list);

        itemListAdapter_ = new BlobCursorAdapter(
                this,
                null,
                0
        );
        itemListAdapter_.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return SQLCipherDatabase.getContaining(constraint.toString());
            }
        });
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

        itemListFragment_.setListAdapter(itemListAdapter_);

    }

    /**
     * Callback method for option item selection.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_lock:
                startActivity(new Intent(this, UnlockActivity.class).setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                Intent.FLAG_ACTIVITY_NO_ANIMATION
                ));
                return true;
            case R.id.action_export:
                return Common.ExportKeys(this);
            case R.id.action_import:
                startDatabaseImport();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.add:
                if (twoPane_) {
                    // In two-pane mode, show the add key view in this activity by adding or
                    // replacing the detail fragment using a fragment transaction.
                    Bundle arguments = new Bundle();
                    arguments.putBoolean(Constants.ARG_TWO_PANE, Boolean.TRUE);
                    EditItemFragment fragment = new EditItemFragment();
                    fragment.setArguments(arguments);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.item_detail_container, fragment)
                            .commit();

                } else {
                    // In single-pane mode, simply start the add key activity for the selected item
                    // ID.
                    Intent addIntent = new Intent(this, EditItemActivity.class);
                    addIntent.putExtra(Constants.ARG_TWO_PANE, Boolean.FALSE);
                    startActivityForResult(addIntent, ADD_RECORD_REQUEST);
                }
                return true;
            case R.id.action_edit:
                onEditItem(itemListAdapter_.getCursor().getPosition());
                return true;
            case R.id.action_delete:
                onDeleteItem(itemListAdapter_.getCursor().getPosition());
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // There's probably a more efficient way to do this on item add than rereading the database,
        // but at least this ensures consistency, I guess.
        switch (requestCode) {
            case ADD_RECORD_REQUEST:
                if (resultCode == RESULT_OK) {
                    refreshListFromDatabase();
                    onItemSelected(String.valueOf(data.getLongExtra(Constants.ARG_ITEM_ID, -1)));
                }
                break;
            case EDIT_RECORD_REQUEST:
                if (resultCode == RESULT_OK) {
                    refreshListFromDatabase();
                    onItemSelected(String.valueOf(selectedId_));
                }
                break;
            case Constants.IMPORT_DATABASE_RESULT:
                if (resultCode == RESULT_OK) {
                    finishDatabaseImport(data);
                }
                break;
        }
    }

    /**
     * /* Called from the child ItemListFragment on the Edit context menu.
     */
    public void onEditItem(int position) {
        long id = getRecordFromPosition(position);
        if (twoPane_) {
            Bundle arguments = new Bundle();
            arguments.putBoolean(Constants.ARG_TWO_PANE, true);
            arguments.putLong(Constants.ARG_ITEM_ID, id);
            EditItemFragment fragment = new EditItemFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.item_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity for the selected item ID.
            Intent editIntent = new Intent(this, EditItemActivity.class);
            editIntent.putExtra(Constants.ARG_ITEM_ID, id);
            startActivityForResult(editIntent, EDIT_RECORD_REQUEST);
        }
    }

    /**
     * Called from the child ItemListFragment on the Edit context menu.
     */
    public void onDeleteItem(int position) {
        long id = getRecordFromPosition(position);
        Bundle arguments = new Bundle();
        arguments.putLong(Constants.ARG_ITEM_ID, id);
        DeleteItemFragment fragment = new DeleteItemFragment();
        fragment.setArguments(arguments);
        fragment.show(getSupportFragmentManager(), "delete");
    }

    /**
     * Callback method from {@link ItemListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (id == null) {
            if (!twoPane_) {
                selectedId_ = -1;
                return;
            } else {  // In two-pane mode, select the 0th rather than leaving an empty pane.
                if (itemListAdapter_.getCount() == 0) {
                    // Unless we're empty, in which case show nothing.
                    getSupportFragmentManager().beginTransaction().remove(
                            getSupportFragmentManager().findFragmentById(R.id.item_detail_container));
                    return;
                } else {
                    selectedId_ = getRecordFromPosition(0);
                }
            }
        } else {
            selectedId_ = Long.parseLong(id);
        }
        if (twoPane_) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putLong(Constants.ARG_ITEM_ID, selectedId_);
            arguments.putBoolean(Constants.ARG_TWO_PANE, Boolean.TRUE);
            ItemDetailFragment fragment = new ItemDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.item_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ItemDetailActivity.class);
            detailIntent.putExtra(Constants.ARG_ITEM_ID, selectedId_);
            startActivity(detailIntent);
        }
    }

    // Implement Loader callbacks.

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new RecordCursorLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        itemListAdapter_.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        itemListAdapter_.swapCursor(null);
    }

    /**
     * Refresh the displayed list adapter from the open database.
     */
    void refreshListFromDatabase() {
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    /**
     * Get the record object for the specified offset in the list.
     */
    private long getRecordFromPosition(int position) {
        return ((CursorWrapper) itemListAdapter_.getItem(position)).getLong(0);
    }

    void startDatabaseImport() {
        Intent importIntent = new Intent();
        importIntent.setAction(Intent.ACTION_GET_CONTENT);
        importIntent.setType(Constants.KEY_IMPORT_MIME);
        startActivityForResult(importIntent, Constants.IMPORT_DATABASE_RESULT);
    }

    void finishDatabaseImport(Intent data) {
        // This is the URI of the imported data.
        final Uri uri = data.getData();
        final InputStream src;
        try {
            src = getContentResolver().openInputStream(uri);
        } catch (IOException exception) {
            Log.w("IMPORT", exception.toString());
            Common.DisplayException(this, getString(R.string.import_keys_failure_title), exception);
            return;
        }

        // Build a dialog for the password prompt.
        final EditText passwordText = new EditText(this);
        passwordText.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.title_enter_password));
        alert.setMessage(getString(R.string.import_database_password_message));
        alert.setView(passwordText);
        final ItemListActivity ctx = this;
        alert.setPositiveButton(R.string.action_unlock, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Retrieve the password.
                final char[] password = new char[passwordText.length()];
                passwordText.getText().getChars(0, password.length, password, 0);
                // Set up a progress spinner for while we unlock and import, since that takes a while.
                progress_ = new ProgressDialog(ctx);
                progress_.setTitle(R.string.progress_loading);
                progress_.setCancelable(false);
                progress_.show();
                SQLCipherDatabase.ImportDatabase(ctx, src, password, ctx);
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Return...
            }
        });
        alert.show();
    }

    private void dismissProgress() {
        if (progress_ != null) {
            progress_.dismiss();
            progress_ = null;
        }
    }

    // SQLCipherDatabase.Callbacks

    @Override
    public void OnFinish(Boolean success) {
        dismissProgress();
        refreshListFromDatabase();
    }

    @Override
    public void OnException(Exception exception) {
        dismissProgress();
        Log.w("Importing database", exception.toString());
        Common.DisplayException(this,
                getString(R.string.import_keys_failure_title), exception);
    }

    @Override
    public void OnCancelled() {
        dismissProgress();
        refreshListFromDatabase();
    }

    private static class RecordCursorLoader extends CursorLoader {
        public RecordCursorLoader(Context ctx) {
            super(ctx);
        }

        @Override
        public Cursor loadInBackground() {
            if (SQLCipherDatabase.isLocked()) {
                return null;
            }
            return SQLCipherDatabase.getAllCursor();
        }
    }

    static class BlobCursorAdapter extends CursorAdapter {
        private LayoutInflater inflater_;

        public BlobCursorAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            inflater_ = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            TextView text1 = (TextView) view.findViewById(R.id.text1);
            Common.ArrayToTextView(
                    Common.decode(cursor.getBlob(
                            cursor.getColumnIndex(SQLCipherDatabase.COLUMN_DOMAIN))),
                    text1
            );
            TextView text2 = (TextView) view.findViewById(R.id.text2);
            Common.ArrayToTextView(
                    Common.decode(cursor.getBlob(
                            cursor.getColumnIndex(SQLCipherDatabase.COLUMN_USERNAME))),
                    text2
            );
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater_.inflate(R.layout.two_line_list_item, parent, false);
        }
    }
}
