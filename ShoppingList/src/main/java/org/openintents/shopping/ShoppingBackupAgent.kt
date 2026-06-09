package org.openintents.shopping

import android.app.backup.BackupAgentHelper
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FileBackupHelper
import android.app.backup.SharedPreferencesBackupHelper
import android.os.ParcelFileDescriptor
import android.util.Log
import org.openintents.shopping.provider.ShoppingDatabase
import java.io.IOException

class ShoppingBackupAgent : BackupAgentHelper() {

    // Allocate a helper and add it to the backup agent
    override fun onCreate() {
        if (debug) {
            Log.v(TAG, "onCreate")
        }
        val prefsHelper = SharedPreferencesBackupHelper(
            this.applicationContext, PREFS
        )
        addHelper(PREFS_BACKUP_KEY, prefsHelper)

        val helper = FileBackupHelper(this, "../databases/" + ShoppingDatabase.DATABASE_NAME)
        addHelper(DB_BACKUP_KEY, helper)
    }

    @Throws(IOException::class)
    override fun onRestore(
        data: BackupDataInput,
        appVersionCode: Int,
        newState: ParcelFileDescriptor
    ) {
        if (debug) {
            Log.v(TAG, "onRestore")
        }
        super.onRestore(data, appVersionCode, newState)
    }

    @Throws(IOException::class)
    override fun onBackup(
        oldState: ParcelFileDescriptor,
        data: BackupDataOutput,
        newState: ParcelFileDescriptor
    ) {
        if (debug) {
            Log.v(TAG, "onBackup")
        }
        super.onBackup(oldState, data, newState)
    }

    companion object {
        private const val TAG = "ShoppingBackupAgent"
        private val debug = false || LogConstants.debug

        // The name of the SharedPreferences file
        private const val PREFS = "org.openintents.shopping_preferences"

        // A key to uniquely identify the set of backup data
        private const val PREFS_BACKUP_KEY = "prefs"

        private const val DB_BACKUP_KEY = "db"
    }
}
