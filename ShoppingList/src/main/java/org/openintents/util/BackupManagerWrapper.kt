package org.openintents.util

import android.app.backup.BackupManager
import android.content.Context

class BackupManagerWrapper(ctx: Context) {

    /* class initialization fails when this throws an exception */
    companion object {
        init {
            try {
                Class.forName("android.app.backup.BackupManager")
            } catch (ex: Exception) {
                throw RuntimeException(ex)
            }
        }

        /* calling here forces class initialization */
        @JvmStatic
        fun checkAvailable() {
        }
    }

    private val mInstance: BackupManager = BackupManager(ctx)

    fun dataChanged() {
        mInstance.dataChanged()
    }
}
