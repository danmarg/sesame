package net.af0.sesame;

import android.content.Context;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.AttributeSet;

/**
 * Preference that pops a dialog and (upon confirmation) deletes the database.
 */
public final class DeleteDatabasePreference extends DialogPreference {
    public DeleteDatabasePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            SQLCipherDatabase.DeleteDatabase(getContext());
            getContext().startActivity(new Intent(getContext(), CreateDatabaseActivity.class));
        }
    }
}
