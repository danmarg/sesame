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
 * Create a new database.
 */
public final class CreateDatabaseActivity extends Activity
        implements SQLCipherDatabase.Callbacks<Boolean> {
    // Progress spinner for background task of creating database
    private ProgressDialog progress_;
    // Password value
    private char[] password_;
    // UI references
    private EditText passwordView_;
    private EditText password2View_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_database);

        // Set up the creation form.
        passwordView_ = findViewById(R.id.newPassword);
        password2View_ = findViewById(R.id.newPassword2);
        password2View_.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.create_database_button || id == EditorInfo.IME_NULL) {
                    createDatabase();
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.create_database_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createDatabase();
            }
        });
    }

    /**
     * Handle actual database creation. Validate passwords, and start a background task.
     */
    private void createDatabase() {
        if (progress_ != null) {
            return;  // Already running
        }
        // Clear errors
        passwordView_.setError(null);
        password2View_.setError(null);

        password_ = new char[passwordView_.length()];
        passwordView_.getText().getChars(0, password_.length, password_, 0);
        char[] password2 = new char[password2View_.length()];
        password2View_.getText().getChars(0, password2.length, password2, 0);

        boolean cancel = false;
        View focusView = null;

        // Check that password is filled in and matches the retyped password.
        if (password_.length == 0) {
            // Empty
            passwordView_.setError(getString(R.string.error_field_required));
            focusView = passwordView_;
            cancel = true;
        } else if (password_.length < Constants.MIN_PASSWORD_LENGTH) {
            // Too short
            passwordView_.setError(getString(R.string.error_invalid_password));
            focusView = passwordView_;
            cancel = true;
        } else {
            boolean neq = false;
            if (password_.length != password2.length) {
                neq = true;
            } else {
                // Mismatch
                for (int i = 0; i < password_.length; i++) {
                    if (password_[i] != password2[i]) {
                        neq = true;
                        break;
                    }
                }
            }
            if (neq) {
                password2View_.setError(getString(R.string.error_password2_mismatch));
                focusView = password2View_;
                cancel = true;
            }
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            // Show a progress spinner and begin creating in the background.
            progress_ = new ProgressDialog(this);
            progress_.setTitle(R.string.creating_database_progress);
            progress_.setCancelable(false);
            progress_.show();
            SQLCipherDatabase.Instance().createDatabase(getBaseContext(), password_, this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_database, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
                // Launch the settings activity for this menu item.
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
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
        super.onResume();
    }

    public void OnFinish(Boolean success) {
        dismissProgress();
        if (success) {
            finish();
            startActivity(new Intent(getBaseContext(), ItemListActivity.class));
        }
    }

    public void OnException(Exception exception) {
        dismissProgress();
        Log.e("CREATE", exception.toString());
        Common.DisplayException(getParent(), getString(R.string.error_creating_database), exception);
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
}
