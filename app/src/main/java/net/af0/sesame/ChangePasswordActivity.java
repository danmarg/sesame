package net.af0.sesame;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public final class ChangePasswordActivity extends Activity
        implements SQLCipherDatabase.Callbacks<Boolean> {
    // Show a progress spinner when we change the password in the background
    private ProgressDialog progress_;
    // Password value
    private String password_;
    // UI references
    private EditText passwordView_;
    private EditText password2View_;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.change_password, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Set up the creation form.
        passwordView_ = (EditText) findViewById(R.id.newPassword);
        password2View_ = (EditText) findViewById(R.id.newPassword2);
        password2View_.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                // On enter, do the password change.
                if (id == R.id.save_button || id == EditorInfo.IME_NULL) {
                    changePassword();
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Do the password change from the "save" click.
                changePassword();
            }
        });

        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
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
     * Common code to handle starting the password change.
     */
    private void changePassword() {
        // Clear errors
        passwordView_.setError(null);
        password2View_.setError(null);

        password_ = passwordView_.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check that password is filled in and matches the retyped password.
        if (TextUtils.isEmpty(password_)) {
            // Empty
            passwordView_.setError(getString(R.string.error_field_required));
            focusView = passwordView_;
            cancel = true;
        } else if (password_.length() < Constants.MIN_PASSWORD_LENGTH) {
            // Too short
            passwordView_.setError(getString(R.string.error_invalid_password));
            focusView = passwordView_;
            cancel = true;
        } else if (!password_.contentEquals(password2View_.getText().toString())) {
            // Mismatch
            password2View_.setError(getString(R.string.error_password2_mismatch));
            focusView = password2View_;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            if (progress_ != null) {
                return;  // Already running
            }

            // Show a progress spinner and begin unlocking in the background.
            progress_ = new ProgressDialog(this);
            progress_.setTitle(R.string.changing_password_progress);
            progress_.setCancelable(false);
            progress_.show();
            SQLCipherDatabase.ChangePassword(password_, this);
        }
    }

    public void OnFinish(Boolean success) {
        dismissProgress();
        if (success) {
            finish();
            startActivity(new Intent(getBaseContext(), ItemListActivity.class));
        }
    }

    public void OnException(Exception exception) {
        Log.w("CHANGE PASSWORD", exception);
        dismissProgress();
        Common.DisplayException(this, getString(R.string.action_change_error), exception);
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
