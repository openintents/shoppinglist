package org.openintents.util;

import android.annotation.TargetApi;
import android.app.backup.BackupManager;
import android.content.Context;
import android.os.Build;

public class BackupManagerWrapper {
    private BackupManager mInstance;

    /* class initialization fails when this throws an exception */
    static {
        try {
            Class.forName("android.app.backup.BackupManager");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /* calling here forces class initialization */
    public static void checkAvailable() {
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public BackupManagerWrapper(Context ctx) {
        mInstance = new BackupManager(ctx);

    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public void dataChanged() {
        mInstance.dataChanged();
    }
}
