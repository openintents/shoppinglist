package org.openintents.shopping;

import android.database.Cursor;

public interface WearSupport {
    public boolean isAvailable();
    public void pushToWear(Cursor cursor);
}
