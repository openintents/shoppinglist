package org.openintents.shopping.sync

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import org.openintents.shopping.SyncSupport

class NoSyncSupport : SyncSupport {

    override fun isAvailable(): Boolean = false

    override fun pushListItem(listId: Long, cursor: Cursor?) {
    }

    override fun pushList(cursor: Cursor?) {
    }

    override fun isSyncEnabled(): Boolean = false

    override fun setSyncEnabled(enableSync: Boolean) {
    }

    override fun updateListItem(listId: Long, itemUri: Uri?, values: ContentValues?) {
    }
}
