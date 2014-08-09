package net.af0.sesame;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * An activity representing a single Item detail screen. This activity is only used on handset
 * devices. On tablet-size devices, item details are presented side-by-side with a list of items
 * in a {@link ItemListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing more than a
 * {@link ItemDetailFragment}.
 */
public final class ItemDetailActivity extends FragmentActivity {
    private static final int EDIT_RECORD_REQUEST = 0;

    private ItemDetailFragment detailFragment_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        // Show the Up button in the action bar.
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putLong(Constants.ARG_ITEM_ID,
                    getIntent().getLongExtra(Constants.ARG_ITEM_ID, -1));
            detailFragment_ = new ItemDetailFragment();
            detailFragment_.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.item_detail_container, detailFragment_)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item_detail_actions, menu);
        return true;
    }

    @Override
    protected void onResume() {
        // If we're locked, go to the unlock view.
        if (SQLCipherDatabase.isLocked()) {
            startActivity(new Intent(getBaseContext(), UnlockActivity.class).setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                onEditItem();
                break;
            case R.id.action_delete:
                onDeleteItem();
                break;
            case android.R.id.home:
                NavUtils.navigateUpTo(this, new Intent(this, ItemListActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // There's probably a more efficient way to do this on item add than rereading the database,
        // but at least this ensures consistency.
        switch (requestCode) {
            case EDIT_RECORD_REQUEST:
                if (resultCode == RESULT_OK) {
                    detailFragment_.loadFromDatabase();
                }
                break;
        }
    }

    // Called from the child ItemListFragment on the Edit context menu.
    void onEditItem() {
        long id = getIntent().getLongExtra(Constants.ARG_ITEM_ID, -1);
        // In single-pane mode, simply start the detail activity
        // for the selected item ID.
        Intent editIntent = new Intent(this, EditItemActivity.class);
        editIntent.putExtra(Constants.ARG_ITEM_ID, id);
        startActivityForResult(editIntent, EDIT_RECORD_REQUEST);
    }

    // Called from the child ItemListFragment on the Edit context menu.
    void onDeleteItem() {
        long id = getIntent().getLongExtra(Constants.ARG_ITEM_ID, -1);
        Bundle arguments = new Bundle();
        arguments.putLong(Constants.ARG_ITEM_ID, id);
        DeleteItemFragment fragment = new DeleteItemFragment();
        fragment.setArguments(arguments);
        fragment.show(getSupportFragmentManager(), "delete");
    }
}
