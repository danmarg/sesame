package net.af0.sesame;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FilterQueryProvider;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;


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
        implements ItemListFragment.Callbacks, SearchView.OnQueryTextListener {

    private static final int ADD_RECORD_REQUEST = 0;
    private static final int EDIT_RECORD_REQUEST = 1;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean twoPane_;
    private long selectedId_;

    private ItemListFragment itemListFragment_;
    private SimpleCursorAdapter itemListAdapter_;

    @Override
    protected void onResume() {
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
                                Intent.FLAG_ACTIVITY_NEW_TASK
                ));
                return true;
            case R.id.action_export:
                return Common.ExportKeys(this);
            case R.id.action_import:
                Common.StartImportKeys(this);
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
                    // TODO: set selection to added item
                }
                break;
            case EDIT_RECORD_REQUEST:
                if (resultCode == RESULT_OK) {
                    refreshListFromDatabase();
                }
                onItemSelected(String.valueOf(selectedId_));
                break;
            case Constants.IMPORT_DATABASE_RESULT:
                if (resultCode == RESULT_OK) {
                    Common.onImportKeysResult(this, data,
                            new Runnable() {
                                @Override
                                public void run() {
                                    refreshListFromDatabase();
                                }
                            }
                    );
                }
                break;
        }
    }

    /**
    /* Called from the child ItemListFragment on the Edit context menu.
     */
    public void onEditItem(int position) {
        long id = getRecordFromPosition(position).getId();
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
        long id = getRecordFromPosition(position).getId();
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
            // Clear the fragment.
            if (twoPane_) {
                getSupportFragmentManager().beginTransaction().remove(
                        getSupportFragmentManager().findFragmentById(R.id.item_detail_container));
            }
            return;
        }
        selectedId_ = Long.valueOf(id);
        if (SQLCipherDatabase.getRecord(selectedId_) == null) {
            // Item was deleted. We should find a more elegant way to do this.
            return;
        }
        if (twoPane_) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putLong(Constants.ARG_ITEM_ID, selectedId_);
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

    /**
     * Refresh the displayed list adapter from the open database.
     */
    void refreshListFromDatabase() {
        itemListAdapter_ = new SimpleCursorAdapter(
                this,
                R.layout.two_line_list_item,
                SQLCipherDatabase.getAllCursor(),
                new String[]{SQLCipherDatabase.COLUMN_DOMAIN,
                        SQLCipherDatabase.COLUMN_USERNAME},
                new int[]{R.id.text1, R.id.text2},
                0
        );
        itemListAdapter_.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return SQLCipherDatabase.getContaining(constraint.toString());
            }
        });
        if (itemListFragment_ == null) {
            itemListFragment_ = (ItemListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.item_list);
        }
        itemListFragment_.setListAdapter(itemListAdapter_);
    }

    /**
     * Get the record object for the specified offset in the list.
     */
    private SQLCipherDatabase.Record getRecordFromPosition(int position) {
        return (SQLCipherDatabase.Record) itemListAdapter_.getItem(position);
    }
}
