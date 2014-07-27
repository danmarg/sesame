package net.af0.sesame;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ItemListFragment} and the item details
 * (if present) is a {@link ItemDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link ItemListFragment.Callbacks} interface
 * to listen for item selections.
 */
public final class ItemListActivity extends FragmentActivity
        implements ItemListFragment.Callbacks, SearchView.OnQueryTextListener {

    private static final int ADD_RECORD_REQUEST = 0;
    private static final int EDIT_RECORD_REQUEST = 1;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean twoPane_;
    private long selectedId_;

    private ItemListFragment itemListFragment_;
    private RecordArrayAdapter itemListAdapter_;

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

    public boolean onQueryTextSubmit(String query) {
        itemListAdapter_.getFilter().filter(query);
        return true;
    }

    public boolean onQueryTextChange(String query) {
        itemListAdapter_.getFilter().filter(query);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_item_list);

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.x
            twoPane_ = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
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
                    // In two-pane mode, show the add key view in this activity by
                    // adding or replacing the detail fragment using a
                    // fragment transaction.
                    Bundle arguments = new Bundle();
                    arguments.putBoolean(Constants.ARG_TWO_PANE, Boolean.TRUE);
                    EditItemFragment fragment = new EditItemFragment();
                    fragment.setArguments(arguments);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.item_detail_container, fragment)
                            .commit();

                } else {
                    // In single-pane mode, simply start the add key activity
                    // for the selected item ID.
                    Intent addIntent = new Intent(this, EditItemActivity.class);
                    addIntent.putExtra(Constants.ARG_TWO_PANE, Boolean.FALSE);
                    startActivityForResult(addIntent, ADD_RECORD_REQUEST);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void refreshListFromDatabase() {
        List<SQLCipherDatabase.Record> objects = SQLCipherDatabase.getAll();
        Collections.sort(objects);
        itemListAdapter_ = new RecordArrayAdapter(
                this,
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                objects);
        if (itemListFragment_ == null) {
            itemListFragment_ = (ItemListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.item_list);
        }
        itemListFragment_.setListAdapter(itemListAdapter_);
    }

    private SQLCipherDatabase.Record getRecordFromPosition(int position) {
        return itemListAdapter_.getItem(position);
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
                            public void run() { refreshListFromDatabase(); }
                        });
                }
                break;
        }
    }

    // Called from the child ItemListFragment on the Edit context menu.
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
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent editIntent = new Intent(this, EditItemActivity.class);
            editIntent.putExtra(Constants.ARG_ITEM_ID, id);
            startActivityForResult(editIntent, EDIT_RECORD_REQUEST);
        }
    }

    // Called from the child ItemListFragment on the Edit context menu.
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

    // Extend ArrayAdapter in order to provide customer filtering.
    final class RecordArrayAdapter extends ArrayAdapter<SQLCipherDatabase.Record> {
        private List<SQLCipherDatabase.Record> objects_;
        private final Filter filter_;

        public RecordArrayAdapter(Context context, int resource, int textViewResourceId,
                                  final List<SQLCipherDatabase.Record> objects) {
            super(context, resource, textViewResourceId, objects);
            // Keep a clone so we can refresh after a search without rereading from the database.
            this.objects_ = new ArrayList<SQLCipherDatabase.Record>(objects);
            // Filter displayed items by case insensitive key contains.
            this.filter_ = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults res = new FilterResults();
                    if (constraint == null || constraint.length() == 0) {
                        res.values = new ArrayList<SQLCipherDatabase.Record>(objects_);
                        res.count = objects_.size();
                        return res;
                    }
                    ArrayList<SQLCipherDatabase.Record> t = new
                            ArrayList<SQLCipherDatabase.Record>();
                    String c = constraint.toString().toLowerCase();
                    for (SQLCipherDatabase.Record r : objects_) {
                        if (r.getRemarks().toLowerCase().contains(c) ||
                                r.getDomain().toLowerCase().contains(c) ||
                                r.getUsername().toLowerCase().contains(c)) {
                            t.add(r);
                        }
                    }
                    res.values = t;
                    res.count = t.size();
                    return res;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    addAll((ArrayList<SQLCipherDatabase.Record>) results.values);
                    if (results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
        }

        @Override
        public Filter getFilter() {
            return filter_;
        }
    }
}
