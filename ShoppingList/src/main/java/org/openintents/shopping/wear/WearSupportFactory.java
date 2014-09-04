package org.openintents.shopping.wear;

import android.content.Context;
import android.database.Cursor;

import org.openintents.shopping.BuildConfig;
import org.openintents.shopping.WearSupport;

public class WearSupportFactory {
    private static final String PLAY = "play";

    public static WearSupport getDefault(Context context) {
        if (PLAY.equals(BuildConfig.FLAVOR)) {
            return new GooglePlayWearSupport(context);
        } else {
            return new NoWearSupport();
        }
    }

    private static class NoWearSupport implements WearSupport {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void pushToWear(Cursor cursor) {

        }
    }
}
