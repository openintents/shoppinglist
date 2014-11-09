package org.openintents.shopping.wear;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

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
        public void pushListItem(long listId, Cursor cursor) {

        }

        @Override
        public void pushList(Cursor cursor) {

        }

        @Override
        public boolean isSyncEnabled() {
            return false;
        }

        @Override
        public void setSyncEnabled(boolean enableSync) {

        }

        @Override
        public void updateListItem(long listId, Uri itemUri, ContentValues values) {

        }

    }
}
