package org.openintents.shopping

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

interface SyncSupport {
    fun isAvailable(): Boolean
    fun pushListItem(listId: Long, cursor: Cursor?)
    fun updateListItem(listId: Long, itemUri: Uri?, values: ContentValues?)
    fun pushList(cursor: Cursor?)
    fun isSyncEnabled(): Boolean
    fun setSyncEnabled(enableSync: Boolean)
}
