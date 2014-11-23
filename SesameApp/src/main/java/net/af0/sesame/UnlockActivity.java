package net.af0.sesame;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * This is the first activity the user sees when opening the app. It allows unlocking the database
 * (or forwards the user to the CreateDatabaseActivity if none exists).
 */
public final class UnlockActivity extends Activity implements SQLCipherDatabase.Callbacks<Boolean> {
    // Value of password at the time of the login attempt
    private char[] password_;

    // UI references
    private EditText passwordView_;
    private ProgressDialog progress_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_unlock);

        // If the database does not yet exist, redirect to the CreateDatabaseActivity.
        if (!SQLCipherDatabase.Exists(this)) {
            startActivity(new Intent(getBaseContext(), CreateDatabaseActivity.class).setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
            ));
        } else if (!SQLCipherDatabase.isLocked()) {
            startActivity(new Intent(getBaseContext(), ItemListActivity.class).setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
            ));
        }

        // Set up the login form.
        passwordView_ = (EditText) findViewById(R.id.password);
        passwordView_.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.unlock_button || id == EditorInfo.IME_NULL) {
                    attemptUnlock();
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.unlock_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptUnlock();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class).setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            case R.id.action_export:
                return Common.ExportKeys(this);
        }
        return super.onOptionsItemSelected(item);
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

        // Lock (if we're coming from the "lock" button.
        if (!SQLCipherDatabase.isLocked()) {
            SQLCipherDatabase.Lock();
        }

        super.onResume();
    }

    /**
     * Retrieve the password set in the UI and attempt to unlock the database in a background task.
     */
    void attemptUnlock() {
        if (progress_ != null) {
            return;  // Already running
        }
        // Reset errors.
        passwordView_.setError(null);

        // Store values at the time of the login attempt.
        password_ = new char[passwordView_.length()];
        passwordView_.getText().getChars(0, password_.length, password_, 0);

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (password_.length == 0) {
            passwordView_.setError(getString(R.string.error_field_required));
            focusView = passwordView_;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner and begin unlocking in the background.
            progress_ = new ProgressDialog(this);
            progress_.setTitle(R.string.unlock_progress_unlocking);
            progress_.setCancelable(false);
            progress_.show();
            SQLCipherDatabase.OpenDatabase(getBaseContext(), password_, this);
        }
    }

    public void OnException(Exception exception) {
        Log.w("Unlocking database", exception.toString());
        dismissProgress();
        if (exception instanceof UnsupportedOperationException) {
            passwordView_.setError(getString(R.string.error_opening_unknown));
        } else {
            passwordView_.setError(getString(R.string.error_incorrect_password));
        }
        passwordView_.requestFocus();
    }

    public void OnFinish(Boolean success) {
        dismissProgress();
        if (success) {
            // Don't finish() here--we want the "lock" button to take us back here in the stack.
            startActivity(new Intent(getBaseContext(), ItemListActivity.class));
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
        for (int i = 0; i < password_.length; i++) {
            password_[i] = 0;
        }
    }
}