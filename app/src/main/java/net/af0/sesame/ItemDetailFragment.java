package net.af0.sesame;

import android.app.ProgressDialog;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.nio.CharBuffer;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ItemListActivity} in two-pane mode (on tablets) or
 * a {@link ItemDetailActivity} on handsets.
 */
public class ItemDetailFragment extends Fragment
        implements SQLCipherDatabase.Callbacks<SQLCipherDatabase.Record> {
    // The item we're showing details for.
    SQLCipherDatabase.Record record_;
    ProgressDialog progress_;
    private View rootView_;

    private TextView usernameView_;
    private TextView passwordView_;
    private TextView domainView_;
    private TextView remarksView_;

    // Whether we're in two-pane mode, which dictates whether we update the activity title or not.
    private boolean twoPane_;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(Constants.ARG_TWO_PANE)) {
            twoPane_ = getArguments().getBoolean(Constants.ARG_TWO_PANE);
        }
    }

    @Override
    public void onDestroyView() {
        if (progress_ != null) {
            progress_.dismiss();
        }
        if (record_ != null) {
            record_.forget();
        }
        usernameView_.setText("");
        passwordView_.setText("");
        domainView_.setText("");
        remarksView_.setText("");

        if (!twoPane_) {
            getActivity().setTitle("");
        }

        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView_ = inflater.inflate(R.layout.fragment_item_detail, container, false);

        usernameView_ = rootView_.findViewById(R.id.username);
        passwordView_ = rootView_.findViewById(R.id.password);
        domainView_ = rootView_.findViewById(R.id.domain);
        remarksView_ = rootView_.findViewById(R.id.remarks);

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
        SQLCipherDatabase.Instance().getRecord(getArguments().getLong(Constants.ARG_ITEM_ID, -1), this);
    }

    /**
     * Callback from async database load.
     *
     * @param item
     */
    public void OnFinish(SQLCipherDatabase.Record item) {
        progress_.dismiss();
        record_ = item;
        if (record_ == null) {
            return;
        }

        // For each field, either set the value and show the field (since it may be hidden) or, if
        // empty, hide the field.
        if (record_.getUsername() != null && record_.getUsername().length > 0) {
            Common.ArrayToTextView(record_.getUsername(), usernameView_);
            usernameView_.setVisibility(View.VISIBLE);
        } else {
            usernameView_.setVisibility(View.INVISIBLE);
        }
        if (record_.getPassword() != null && record_.getPassword().length > 0) {
            Common.ArrayToTextView(record_.getPassword(), passwordView_);
            passwordView_.setVisibility(View.VISIBLE);
        } else {
            passwordView_.setVisibility(View.INVISIBLE);
        }
        if (record_.getDomain() != null && record_.getDomain().length > 0) {
            Common.ArrayToTextView(record_.getDomain(), domainView_);
            domainView_.setVisibility(View.VISIBLE);
        } else {
            domainView_.setVisibility(View.INVISIBLE);
        }
        if (record_.getRemarks() != null && record_.getRemarks().length > 0) {
            Common.ArrayToTextView(record_.getRemarks(), remarksView_);
            remarksView_.setVisibility(View.VISIBLE);
        } else {
            remarksView_.setVisibility(View.INVISIBLE);
        }

        // Set title to the current item's domain, if unset. In two-pane mode the parent activity is
        // the ItemListActivity, and the title is already set.
        if (!twoPane_) {
            getActivity().setTitle(CharBuffer.wrap(record_.getDomain()));
        }
    }

    public void OnCancelled() {
        progress_.dismiss();
    }

    public void OnException(Exception exception) {
        Log.e("DETAIL", exception.toString());
    }
}
