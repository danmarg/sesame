package net.af0.sesame;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ItemListActivity}
 * in two-pane mode (on tablets) or a {@link ItemDetailActivity}
 * on handsets.
 */
public class ItemDetailFragment extends Fragment {
    // The item we're showing details for.
    private SQLCipherDatabase.Record item_;

    private View rootView_;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() {
    }

    void loadFromDatabase() {
        item_ = SQLCipherDatabase.getRecord(getArguments().getLong(Constants.ARG_ITEM_ID));
        if (item_.getUsername() != null && !item_.getUsername().isEmpty()) {
            ((TextView) rootView_.findViewById(R.id.username)).setText(item_.getUsername());
        } else {
            rootView_.findViewById(R.id.username).setVisibility(View.INVISIBLE);
        }
        if (item_.getPassword() != null && !item_.getPassword().isEmpty()) {
            ((TextView) rootView_.findViewById(R.id.password)).setText(item_.getPassword());
        } else {
            rootView_.findViewById(R.id.password).setVisibility(View.INVISIBLE);
        }
        if (item_.getDomain() != null && !item_.getDomain().isEmpty()) {
            ((TextView) rootView_.findViewById(R.id.domain)).setText(item_.getDomain());
        } else {
            rootView_.findViewById(R.id.domain).setVisibility(View.INVISIBLE);
        }
        if (item_.getRemarks() != null && !item_.getRemarks().isEmpty()) {
            ((TextView) rootView_.findViewById(R.id.remarks)).setText(item_.getRemarks());
        } else {
            rootView_.findViewById(R.id.remarks).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView_ = inflater.inflate(R.layout.fragment_item_detail, container, false);

        loadFromDatabase();

        // Make the show/hide switch change the password field visibility.
        ((Switch) rootView_.findViewById(R.id.show_password)).setOnCheckedChangeListener(
                new
                        CompoundButton.OnCheckedChangeListener() {
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                ((TextView) rootView_.findViewById(R.id.password)).setInputType(
                                        isChecked ?
                                                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                                                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                                );
                            }
                        }
        );

        return rootView_;
    }
}
