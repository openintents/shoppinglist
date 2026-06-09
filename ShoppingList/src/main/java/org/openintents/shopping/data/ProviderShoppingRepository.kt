package org.openintents.shopping.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import org.openintents.shopping.library.provider.ShoppingContract.Contains
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull
import org.openintents.shopping.library.provider.ShoppingContract.Lists
import org.openintents.shopping.library.provider.ShoppingContract.Status
import org.openintents.shopping.library.util.ShoppingUtils

/**
 * [ShoppingRepository] backed by the app's ContentProvider + SQLite.
 *
 * All Cursor/Uri/ContentValues details live here so they never reach the UI.
 */
class ProviderShoppingRepository(private val context: Context) : ShoppingRepository {

    private val resolver get() = context.contentResolver

    override fun getLists(): List<ShoppingListInfo> {
        val out = ArrayList<ShoppingListInfo>()
        resolver.query(
            Lists.CONTENT_URI, arrayOf(Lists._ID, Lists.NAME),
            null, null, Lists.DEFAULT_SORT_ORDER
        )?.use { c ->
            while (c.moveToNext()) {
                out.add(ShoppingListInfo(c.getLong(0), c.getString(1) ?: ""))
            }
        }
        return out
    }

    override fun getDefaultListId(): Long = ShoppingUtils.getDefaultList(context)

    override fun getItems(listId: Long): List<ShoppingItem> {
        val out = ArrayList<ShoppingItem>()
        resolver.query(
            ContainsFull.CONTENT_URI,
            arrayOf(
                ContainsFull._ID, ContainsFull.ITEM_ID, ContainsFull.ITEM_NAME,
                ContainsFull.STATUS, ContainsFull.QUANTITY, ContainsFull.ITEM_PRICE,
                ContainsFull.PRIORITY, ContainsFull.ITEM_TAGS
            ),
            ContainsFull.LIST_ID + " = ?", arrayOf(listId.toString()),
            ContainsFull.DEFAULT_SORT_ORDER
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getLong(3) == Status.REMOVED_FROM_LIST) continue
                out.add(
                    ShoppingItem(
                        containsId = c.getLong(0),
                        itemId = c.getLong(1),
                        name = c.getString(2) ?: "",
                        status = c.getLong(3),
                        quantity = c.getString(4),
                        priceCents = if (c.isNull(5)) null else c.getLong(5),
                        priority = c.getString(6),
                        tags = c.getString(7),
                    )
                )
            }
        }
        return out
    }

    override fun addItem(listId: Long, name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        val itemId = ShoppingUtils.updateOrCreateItem(
            context, trimmed, null, null, null, listId.toString()
        )
        ShoppingUtils.addItemToList(
            context, itemId, listId, Status.WANT_TO_BUY,
            null, null, false, false, false
        )
        ShoppingUtils.addDefaultsToAddedItem(context, listId, itemId)
        return itemId
    }

    override fun setItemStatus(containsId: Long, status: Long) {
        val values = ContentValues().apply { put(Contains.STATUS, status) }
        resolver.update(
            Uri.withAppendedPath(Contains.CONTENT_URI, containsId.toString()),
            values, null, null
        )
    }

    override fun createList(name: String): Long = ShoppingUtils.getList(context, name)
}
