package net.af0.sesame;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ItemListActivity} in two-pane mode (on tablets) or
 * a {@link ItemDetailActivity} on handsets.
 */
public class ItemDetailFragment extends Fragment
        implements SQLCipherDatabase.Callbacks<SQLCipherDatabase.Record> {
    // The item we're showing details for.
    SQLCipherDatabase.Record item_;
    ProgressDialog progress_;
    private View rootView_;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() { }

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
                                                InputType.TYPE_CLASS_TEXT |
                                                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                                                InputType.TYPE_CLASS_TEXT |
                                                        InputType.TYPE_TEXT_VARIATION_PASSWORD
                                );
                            }
                        }
        );

        return rootView_;
    }

    /**
     * Load (or reload) the list from the open database.
     */
    void loadFromDatabase() {
        progress_ = new ProgressDialog(getActivity());
        progress_.setTitle(R.string.progress_loading);
        progress_.setCancelable(false);
        progress_.show();
        SQLCipherDatabase.getRecord(getArguments().getLong(Constants.ARG_ITEM_ID, -1), this);
    }

    /**
     * Callback from async database load.
     *
     * @param item
     */
    public void OnFinish(SQLCipherDatabase.Record item) {
        progress_.dismiss();
        item_ = item;
        if (item_ == null) {
            return;
        }
        TextView usernameView_ = ((TextView) rootView_.findViewById(R.id.username));
        TextView passwordView_ = ((TextView) rootView_.findViewById(R.id.password));
        TextView domainView_ = ((TextView) rootView_.findViewById(R.id.domain));
        TextView remarksView_ = ((TextView) rootView_.findViewById(R.id.remarks));

        // For each field, either set the value and show the field (since it may be hidden) or, if
        // empty, hide the field.
        if (item_.getUsername() != null && !item_.getUsername().isEmpty()) {
            usernameView_.setText(item_.getUsername());
            usernameView_.setVisibility(View.VISIBLE);
        } else {
            usernameView_.setVisibility(View.INVISIBLE);
        }
        if (item_.getPassword() != null && !item_.getPassword().isEmpty()) {
            passwordView_.setText(item_.getPassword());
            passwordView_.setVisibility(View.VISIBLE);
        } else {
            passwordView_.setVisibility(View.INVISIBLE);
        }
        if (item_.getDomain() != null && !item_.getDomain().isEmpty()) {
            domainView_.setText(item_.getDomain());
            domainView_.setVisibility(View.VISIBLE);
        } else {
            domainView_.setVisibility(View.INVISIBLE);
        }
        if (item_.getRemarks() != null && !item_.getRemarks().isEmpty()) {
            remarksView_.setText(item_.getRemarks());
            remarksView_.setVisibility(View.VISIBLE);
        } else {
            remarksView_.setVisibility(View.INVISIBLE);
        }

        // Set title to the current item's domain, if unset. In two-pane mode the parent activity is
        // the ItemList, and the title is already set.
        if (getActivity().getTitle().length() == 0) {
            getActivity().setTitle(item_.getDomain());
        }
    }

    public void OnCancelled() {
        progress_.dismiss();
    }

    public void OnException(Exception exception) {
        Log.e("DETAIL", exception.toString());
    }
}
