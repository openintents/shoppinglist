package org.openintents.shopping;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public interface WearSupport {
    public boolean isAvailable();
    public void pushListItem(long listId, Cursor cursor);
    void updateListItem(long listId, Uri itemUri, ContentValues values);
    void pushList(Cursor cursor);

    boolean isSyncEnabled();
    void setSyncEnabled(boolean enableSync);
}
