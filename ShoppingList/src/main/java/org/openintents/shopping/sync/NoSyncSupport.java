package org.openintents.shopping.sync;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.openintents.shopping.SyncSupport;

public class NoSyncSupport implements SyncSupport {
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
