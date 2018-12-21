package net.af0.sesame;

import android.content.Context;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Preference that pops a dialog and (upon confirmation) deletes the database.
 */
public final class DeleteDatabasePreference extends DialogPreference
        implements SQLCipherDatabase.Callbacks<Boolean> {
    public DeleteDatabasePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            SQLCipherDatabase.DeleteDatabase(getContext(), this);
            getContext().startActivity(new Intent(getContext(), CreateDatabaseActivity.class));
        }
    }

    public void OnFinish(Boolean success) {
    }

    public void OnException(Exception exception) {
        Log.e("DELETING", Log.getStackTraceString(exception));
    }

    public void OnCancelled() {
    }
}
