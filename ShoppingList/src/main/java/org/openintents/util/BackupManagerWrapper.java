package org.openintents.util;

import android.annotation.TargetApi;
import android.app.backup.BackupManager;
import android.content.Context;
import android.os.Build;

public class BackupManagerWrapper {
    /* class initialization fails when this throws an exception */
    static {
        try {
            Class.forName("android.app.backup.BackupManager");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private BackupManager mInstance;

    @TargetApi(Build.VERSION_CODES.FROYO)
    public BackupManagerWrapper(Context ctx) {
        mInstance = new BackupManager(ctx);

    }

    /* calling here forces class initialization */
    public static void checkAvailable() {
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public void dataChanged() {
        mInstance.dataChanged();
    }
}
