package net.af0.sesame;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

/**
 * An activity that allows one to edit an item.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link EditItemFragment}.
 */
public final class EditItemActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);

        // Show the Up button in the action bar.
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            // Create the edit item fragment and add it to the activity using a fragment
            // transaction.
            Bundle arguments = new Bundle();
            arguments.putBoolean(Constants.ARG_TWO_PANE, Boolean.FALSE);
            arguments.putLong(Constants.ARG_ITEM_ID,
                    getIntent().getLongExtra(Constants.ARG_ITEM_ID, -1));
            EditItemFragment fragment = new EditItemFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.add_item_container, fragment)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        // If we're locked, go to the unlock view.
        if (SQLCipherDatabase.Instance().isLocked()) {
            startActivity(new Intent(getBaseContext(), UnlockActivity.class).setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
            ));
        }
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpTo(this, new Intent(this, ItemListActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
