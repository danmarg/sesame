package net.af0.sesame;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import net.sqlcipher.database.SQLiteException;

import java.security.SecureRandom;

/**
 * A fragment representing a single editable item screen.
 * This fragment is either contained in a {@link net.af0.sesame.ItemListActivity} in two-pane mode
 * (on tablets) or a {@link net.af0.sesame.EditItemActivity} on handsets.
 */
public class EditItemFragment extends Fragment {
    // Progress spinner for saving changes
    private ProgressDialog progress_;
    // Async task for adding an item
    private AddTask addTask_ = null;
    // Whether we're in two-pane mode, which dictates how we complete after adding.
    private boolean twoPane_;
    // ID of the record being edited, if it's not a new one.
    private SQLCipherDatabase.Record existingRecord_;

    // Value of fields at time of add
    private String username_;
    private String domain_;
    private String password_;
    private String remarks_;

    private View addItemView_;
    private View addItemFormView_;
    private EditText usernameView_;
    private EditText domainView_;
    private EditText passwordView_;
    private EditText remarksView_;
    private Switch passwordSwitch_;

    // Used for password generation
    private SecureRandom rand_;
    // Offset into {@link net.af0.sesame.Constants.PASSWORD_CHARS} for which chars to use next.
    private int generateCount_ = 0;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
     * screen orientation changes).
     */
    public EditItemFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(Constants.ARG_TWO_PANE)) {
            twoPane_ = getArguments().getBoolean(Constants.ARG_TWO_PANE);
        }
        if (getArguments().containsKey(Constants.ARG_ITEM_ID)) {
            existingRecord_ = SQLCipherDatabase.getRecord(
                    getArguments().getLong(Constants.ARG_ITEM_ID, -1));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        addItemView_ = inflater.inflate(R.layout.fragment_edit_item, container, false);
        addItemFormView_ = addItemView_.findViewById(R.id.add_item_form);

        usernameView_ = (EditText) addItemFormView_.findViewById(R.id.username);
        domainView_ = (EditText) addItemFormView_.findViewById(R.id.domain);
        passwordView_ = (EditText) addItemFormView_.findViewById(R.id.password);
        remarksView_ = (EditText) addItemFormView_.findViewById(R.id.remarks);
        passwordSwitch_ = ((Switch) addItemView_.findViewById(R.id.show_password));

        if (existingRecord_ != null) {
            usernameView_.setText(existingRecord_.getUsername());
            passwordView_.setText(existingRecord_.getPassword());
            domainView_.setText(existingRecord_.getDomain());
            remarksView_.setText(existingRecord_.getRemarks());
        }

        // Make the show/hide switch change the password field visibility.
        passwordSwitch_.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                ((TextView) addItemView_.findViewById(R.id.password)).setInputType(
                                        isChecked ?
                                                InputType.TYPE_CLASS_TEXT |
                                                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                                                InputType.TYPE_CLASS_TEXT |
                                                        InputType.TYPE_TEXT_VARIATION_PASSWORD
                                );
                            }
                        }
        );

        Button generateButton = (Button) addItemView_.findViewById(R.id.generate_password_button);
        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generatePassword();
            }
        });

        Button addButton = (Button) addItemView_.findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (addTask_ != null) {
                    return;
                }
                username_ = usernameView_.getText().toString();
                domain_ = domainView_.getText().toString();
                password_ = passwordView_.getText().toString();
                remarks_ = remarksView_.getText().toString();

                boolean cancel = false;
                View focusView = null;

                // Check for required fields.
                if (TextUtils.isEmpty(domain_) && TextUtils.isEmpty(username_)) {
                    usernameView_.setError(getString(R.string.error_field_required));
                    focusView = usernameView_;
                    cancel = true;
                } else if (TextUtils.isEmpty(password_)) {
                    passwordView_.setError(getString(R.string.error_field_required));
                    focusView = passwordView_;
                    cancel = true;
                }

                if (cancel) {
                    // There was an error.
                    focusView.requestFocus();
                } else {
                    // Show a progress spinner and begin unlocking in the background.
                    progress_ = new ProgressDialog(getActivity());
                    progress_.setTitle(R.string.save_progress_saving);
                    progress_.show();
                    addTask_ = new AddTask();
                    addTask_.execute((Void) null);
                }
            }
        });

        return addItemView_;
    }

    /**
     * Generate a random password. This function successively iterates through
     * @{link net.af0.sesame.Constants.PASSWORD_CHARS} in order to generate passwords with different
     * character sets. Passwords are random-length, random-value from the current set.
     */
    private void generatePassword() {
        if (rand_ == null) {
            rand_ = new SecureRandom();
        }

        StringBuilder s = new StringBuilder();
        int l = Constants.MIN_RANDOM_PASSWORD_LENGTH +
                rand_.nextInt(Constants.MAX_RANDOM_PASSWORD_LENGTH -
                        Constants.MIN_RANDOM_PASSWORD_LENGTH + 1);
        String pwdChars = Constants.PASSWORD_CHARS[generateCount_++ %
                Constants.PASSWORD_CHARS.length];
        for (int i = 0; i < l; i++) {
            s.append(pwdChars.charAt(rand_.nextInt(pwdChars.length())));
        }
        // Always show new passwords.
        passwordSwitch_.setChecked(true);
        passwordView_.setText(s);
    }

    /**
     * Represents an asynchronous database add.
     */
    public class AddTask extends AsyncTask<Void, Void, Boolean> {
        private SQLiteException exception;
        private long newRecordId_;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (existingRecord_ != null) {
                    newRecordId_ = existingRecord_.getId();
                    existingRecord_.setUsername(username_);
                    existingRecord_.setDomain(domain_);
                    existingRecord_.setPassword(password_);
                    existingRecord_.setRemarks(remarks_);
                    SQLCipherDatabase.updateRecord(existingRecord_);
                } else {
                    newRecordId_ = SQLCipherDatabase.createRecord(
                            username_, domain_, password_, remarks_).getId();
                }
            } catch (SQLiteException e) {
                Log.w("EDITING", e.toString());
                exception = e;
                if (!twoPane_) {
                    getActivity().setResult(Activity.RESULT_CANCELED);
                }
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            addTask_ = null;
            progress_.dismiss();
            if (success) {
                if (!twoPane_) {
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                } else {
                    ItemListActivity activity = (ItemListActivity) getActivity();
                    if (activity != null) {
                        activity.refreshListFromDatabase();
                        activity.onItemSelected(String.valueOf(newRecordId_));
                    }
                }
            } else {
                Log.e("EDIT", exception.toString());
                Common.DisplayException(getActivity(),
                        getString(R.string.error_saving_item_title),
                        exception);
                if (!twoPane_) {
                    getActivity().setResult(Activity.RESULT_CANCELED);
                }
            }
        }

        @Override
        protected void onCancelled() {
            if (!twoPane_) {
                getActivity().setResult(Activity.RESULT_CANCELED);
            }
            addTask_ = null;
            progress_.dismiss();
        }
    }
}
