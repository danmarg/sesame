package net.af0.sesame;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import net.sqlcipher.database.SQLiteException;

public final class CreateDatabaseActivity extends Activity {
    private ProgressDialog progress_;
    // Async task for database creation
    private DatabaseCreationTask creationTask_ = null;
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
        passwordView_ = (EditText) findViewById(R.id.newPassword);
        password2View_ = (EditText) findViewById(R.id.newPassword2);
        passwordView_.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

    private void createDatabase() {
        if (creationTask_ != null) {
            return;
        }

        // Clear errors
        passwordView_.setError(null);
        password2View_.setError(null);

        password_ = new char[passwordView_.length()];
        passwordView_.getText().getChars(0, password_.length, password_, 0);
        char[] password2 = new char[password2View_.length()];
        passwordView_.getText().getChars(0, password2.length, password2, 0);

        boolean cancel = false;
        View focusView = null;

        // Check that password is filled in and matches the retyped password.
        if (password_.length == 0) {
            passwordView_.setError(getString(R.string.error_field_required));
            focusView = passwordView_;
            cancel = true;
        } else if (password_.length < 4) {
            passwordView_.setError(getString(R.string.error_invalid_password));
            focusView = passwordView_;
            cancel = true;
        } else {
            boolean neq = false;
            if (password_.length != password2.length) {
                neq = true;
            } else {
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
            progress_.show();
            creationTask_ = new DatabaseCreationTask();
            creationTask_.execute((Void) null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.create_database, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * An async database creation task.
     */
    public class DatabaseCreationTask extends AsyncTask<Void, Void, Boolean> {
        private SQLiteException exception_;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                SQLCipherDatabase.CreateDatabase(getBaseContext(), password_);
            } catch (SQLiteException e) {
                exception_ = e;
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            creationTask_ = null;
            progress_.dismiss();
            if (success) {
                finish();
                startActivity(new Intent(getBaseContext(), ItemListActivity.class));
            } else {
                Log.e("CREATE", exception_.toString());
                Common.DisplayException(getParent(),
                        getString(R.string.error_creating_database),
                        exception_);
            }
        }

        @Override
        protected void onCancelled() {
            creationTask_ = null;
            progress_.dismiss();
        }
    }

}
