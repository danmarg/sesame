package net.af0.sesame;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import net.sqlcipher.database.SQLiteException;

public final class ChangePasswordActivity extends Activity {
    private ProgressDialog progress_;
    // Async task for database creation
    private ChangePasswordTask changeTask_ = null;
    // Password value
    private String password_;
    // UI references
    private EditText passwordView_;
    private EditText password2View_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Set up the creation form.
        passwordView_ = (EditText) findViewById(R.id.newPassword);
        password2View_ = (EditText) findViewById(R.id.newPassword2);
        passwordView_.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.change_password, menu);
        return true;
    }

    private void changePassword() {
        if (changeTask_ != null) {
            return;
        }

        // Clear errors
        passwordView_.setError(null);
        password2View_.setError(null);

        password_ = passwordView_.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check that password is filled in and matches the retyped password.
        if (TextUtils.isEmpty(password_)) {
            passwordView_.setError(getString(R.string.error_field_required));
            focusView = passwordView_;
            cancel = true;
        } else if (password_.length() < 4) {
            passwordView_.setError(getString(R.string.error_invalid_password));
            focusView = passwordView_;
            cancel = true;
        } else if (!password_.contentEquals(password2View_.getText().toString())) {
            password2View_.setError(getString(R.string.error_password2_mismatch));
            focusView = password2View_;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            // Show a progress spinner and begin unlocking in the background.
            progress_ = new ProgressDialog(this);
            progress_.setTitle(R.string.changing_password_progress);
            progress_.show();
            changeTask_ = new ChangePasswordTask(this);
            changeTask_.execute((Void) null);
        }
    }

    /**
     * An async database creation task.
     */
    public class ChangePasswordTask extends AsyncTask<Void, Void, Boolean> {
        private SQLiteException exception_;
        private Activity parent_;

        public ChangePasswordTask(Activity parent) {
            parent_ = parent;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                SQLCipherDatabase.ChangePassword(password_);
            } catch (SQLiteException e) {
                Log.w("Change password", e);
                exception_ = e;
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            changeTask_ = null;
            progress_.dismiss();
            if (success) {
                finish();
                startActivity(new Intent(getBaseContext(), ItemListActivity.class));
            } else {
                AlertDialog.Builder b = new AlertDialog.Builder(parent_);
                b.setTitle(R.string.action_change_error)
                        .setMessage(exception_.getLocalizedMessage())
                        .setNeutralButton("OK", null);
                AlertDialog d = b.create();
                d.show();
            }
        }

        @Override
        protected void onCancelled() {
            changeTask_ = null;
            progress_.dismiss();
        }
    }
}
