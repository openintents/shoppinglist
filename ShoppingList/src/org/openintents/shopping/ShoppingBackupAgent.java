package org.openintents.shopping;

import java.io.FileOutputStream;
import java.io.IOException;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;

public class ShoppingBackupAgent extends BackupAgentHelper {

	// The name of the SharedPreferences file
	static final String PREFS = "org.openintents.shopping_preferences";

	// A key to uniquely identify the set of backup data
	static final String PREFS_BACKUP_KEY = "prefs";

	static final String DB_BACKUP_KEY = "db";

	// Allocate a helper and add it to the backup agent
	public void onCreate() {
		Log.v("shopping backup", "onCreate");
		SharedPreferencesBackupHelper prefsHelper = new SharedPreferencesBackupHelper(
				this.getApplicationContext(), PREFS);
		addHelper(PREFS_BACKUP_KEY, prefsHelper);

		FileBackupHelper helper = new FileBackupHelper(this, "../databases/"
				+ ShoppingProvider.DATABASE_NAME);
		addHelper(DB_BACKUP_KEY, helper);
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
			ParcelFileDescriptor newState) throws IOException {
		Log.v("shopping backup", "onRestore");
		super.onRestore(data, appVersionCode, newState);
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) throws IOException {
		Log.v("shopping backup", "onBackup");
		super.onBackup(oldState, data, newState);
	}
}
