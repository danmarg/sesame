package net.af0.sesame;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import net.sqlcipher.database.SQLiteException;

public final class UnlockActivity extends Activity {
    // Async task for database unlocking
    private UnlockTask unlockTask_ = null;

    // Value of password at the time of the login attempt
    private char[] password_;

    // UI references
    private EditText passwordView_;
    private ProgressDialog progress_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_unlock);

        // Lock (if we're coming from the "lock" button.
        if (!SQLCipherDatabase.isLocked()) {
            SQLCipherDatabase.Lock();
        }

        // If the database does not yet exist, redirect to the CreateDatabaseActivity.
        if (!SQLCipherDatabase.Exists(this)) {
            startActivity(new Intent(getBaseContext(), CreateDatabaseActivity.class).setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (!SQLCipherDatabase.isLocked()) {
            startActivity(new Intent(getBaseContext(), ItemListActivity.class).setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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

    void attemptUnlock() {
        if (unlockTask_ != null) {
            return;
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
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner and begin unlocking in the background.
            progress_ = new ProgressDialog(this);
            progress_.setTitle(R.string.unlock_progress_unlocking);
            progress_.show();
            unlockTask_ = new UnlockTask();
            unlockTask_.execute((Void) null);
        }
    }

    /**
     * Represents an asynchronous database unlock.
     */
    public class UnlockTask extends AsyncTask<Void, Void, Boolean> {
        private SQLiteException exception_;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                SQLCipherDatabase.OpenDatabase(getBaseContext(), password_);
            } catch (SQLiteException e) {
                exception_ = e;
                return false;
            } finally {
                for (int i = 0; i < password_.length; i++) {
                    password_[i] = 0;
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            unlockTask_ = null;
            progress_.dismiss();

            if (success) {
                // Don't finish() here--we want the "lock" button to take us back here in the stack.
                startActivity(new Intent(getBaseContext(), ItemListActivity.class));
            } else {
                Log.w("Unlocking database", exception_.toString());
                passwordView_.setError(getString(R.string.error_incorrect_password));
                passwordView_.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            unlockTask_ = null;
            progress_.dismiss();
        }
    }
}
