package net.af0.sesame;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
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

import com.github.amlcurran.showcaseview.ShowcaseView;

import java.security.SecureRandom;

/**
 * A fragment representing a single editable item screen.
 * This fragment is either contained in a {@link net.af0.sesame.ItemListActivity} in two-pane mode
 * (on tablets) or a {@link net.af0.sesame.EditItemActivity} on handsets.
 */
public class EditItemFragment extends Fragment
        implements SQLCipherDatabase.Callbacks2<Boolean, SQLCipherDatabase.Record>,
        SQLCipherDatabase.Callbacks<SQLCipherDatabase.Record> {
    // ShowcaseView for first run.
    ShowcaseView showcase_;
    // Progress spinner for saving changes
    private ProgressDialog progress_;
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
    public EditItemFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(Constants.ARG_TWO_PANE)) {
            twoPane_ = getArguments().getBoolean(Constants.ARG_TWO_PANE);
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
                    progress_.setCancelable(false);
                    progress_.setTitle(R.string.save_progress_saving);
                    progress_.show();
                    addRecord();
                }
            }
        });


        // Show help showcase.
        // TODO: Would be nice to set a target of the Generate button, but this seems not to work
        // from a fragment. Also, adjust the opacity here, since it's hard to read.
        showcase_ = new ShowcaseView.Builder(getActivity(), true)
                .setContentTitle(R.string.showcase_generate_title)
                .setContentText(R.string.showcase_generate_text)
                .setStyle(R.style.AddItemShowcase)
                .singleShot(Constants.SINGLE_SHOT_EDIT_ITEM)
                .build();

        // Load an existing item in the background, if specified.
        if (getArguments().containsKey(Constants.ARG_ITEM_ID)) {
            progress_ = new ProgressDialog(getActivity());
            progress_.setCancelable(false);
            progress_.setTitle(R.string.progress_loading);
            progress_.show();
            SQLCipherDatabase.getRecord(getArguments().getLong(Constants.ARG_ITEM_ID), this);
        }

        return addItemView_;
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
     * Generate a random password. This function successively iterates through
     *
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
     * Callback from async database load.
     *
     * @param record
     */
    public void OnFinish(SQLCipherDatabase.Record record) {
        progress_.dismiss();
        progress_ = null;
        existingRecord_ = record;
        if (existingRecord_ == null) {
            return;
        }
        usernameView_.setText(existingRecord_.getUsername());
        passwordView_.setText(existingRecord_.getPassword());
        domainView_.setText(existingRecord_.getDomain());
        remarksView_.setText(existingRecord_.getRemarks());

        // Set title to the current item's domain in single-pane mode, but only when editing an
        // existing item.
        if (!twoPane_) {
            getActivity().setTitle(existingRecord_.getDomain());
        }
    }

    /**
     * Callback from async database save.
     *
     * @param record
     */
    public void OnFinish(Boolean success, SQLCipherDatabase.Record record) {
        dismissProgress();
        if (success) {
            if (!twoPane_) {
                getActivity().setResult(Activity.RESULT_OK,
                        new Intent().putExtra(Constants.ARG_ITEM_ID, record.getId()));
                getActivity().finish();
            } else {
                ItemListActivity activity = (ItemListActivity) getActivity();
                if (activity != null) {
                    activity.refreshListFromDatabase();
                    activity.onItemSelected(String.valueOf(record.getId()));
                }
            }
        }
    }

    public void OnException(Exception exception) {
        Log.e("EDIT", exception.toString());
        dismissProgress();
        Common.DisplayException(getActivity(),
                getString(R.string.error_saving_item_title),
                exception);
        if (!twoPane_) {
            getActivity().setResult(Activity.RESULT_CANCELED);
        }
    }

    public void OnCancelled() {
        dismissProgress();
    }

    private void dismissProgress() {
        if (progress_ != null) {
            progress_.dismiss();
            progress_ = null;
        }
    }

    private void addRecord() {
        if (existingRecord_ != null) {
            existingRecord_.setUsername(username_);
            existingRecord_.setDomain(domain_);
            existingRecord_.setPassword(password_);
            existingRecord_.setRemarks(remarks_);
            SQLCipherDatabase.updateRecord(existingRecord_, this);
        } else {
            SQLCipherDatabase.createRecord(username_, domain_, password_, remarks_, this);
        }
    }
}
