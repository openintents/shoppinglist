package org.openintents.shopping;

import android.annotation.TargetApi;
import android.app.backup.*;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

import org.openintents.shopping.provider.ShoppingDatabase;

@TargetApi(Build.VERSION_CODES.FROYO)
public class ShoppingBackupAgent extends BackupAgentHelper {
    private static final String TAG = "ShoppingBackupAgent";
    private static final boolean debug = false || LogConstants.debug;

    // The name of the SharedPreferences file
    static final String PREFS = "org.openintents.shopping_preferences";

    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "prefs";

    static final String DB_BACKUP_KEY = "db";

    // Allocate a helper and add it to the backup agent
    public void onCreate() {
        if (debug) {
            Log.v(TAG, "onCreate");
        }
        SharedPreferencesBackupHelper prefsHelper = new SharedPreferencesBackupHelper(
                this.getApplicationContext(), PREFS);
        addHelper(PREFS_BACKUP_KEY, prefsHelper);

        FileBackupHelper helper = new FileBackupHelper(this, "../databases/"
                + ShoppingDatabase.DATABASE_NAME);
        addHelper(DB_BACKUP_KEY, helper);
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
        if (debug) {
            Log.v(TAG, "onRestore");
        }
        super.onRestore(data, appVersionCode, newState);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        if (debug) {
            Log.v(TAG, "onBackup");
        }
        super.onBackup(oldState, data, newState);
    }
}
