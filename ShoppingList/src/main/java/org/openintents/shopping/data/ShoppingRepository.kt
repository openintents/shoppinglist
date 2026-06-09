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
 * Business-logic boundary over the shopping ContentProvider.
 *
 * This is the "extracted business logic": it hides Cursors / Uris / ContentValues
 * behind plain [ShoppingListInfo] / [ShoppingItem] domain models, so the UI layer
 * (Compose / ViewModel) and unit tests never touch the Android provider API
 * directly. All provider/SQL details live here and are covered by ShoppingRepositoryTest.
 */
open class ShoppingRepository(private val context: Context) {

    private val resolver get() = context.contentResolver

    /** All shopping lists, in the provider's default order. */
    open fun getLists(): List<ShoppingListInfo> {
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

    /** The id of the list to show by default (creates the initial list if needed). */
    open fun getDefaultListId(): Long = ShoppingUtils.getDefaultList(context)

    /** The items currently on [listId] (excludes items removed from the list). */
    open fun getItems(listId: Long): List<ShoppingItem> {
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

    /**
     * Adds (or reuses) an item by name and puts it on [listId] as "want to buy".
     * Mirrors ShoppingItemsView.insertNewItem. Returns the item id, or -1 for a blank name.
     */
    open fun addItem(listId: Long, name: String): Long {
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

    /** Sets the per-list status of a relation row to one of [Status]. */
    open fun setItemStatus(containsId: Long, status: Long) {
        val values = ContentValues().apply { put(Contains.STATUS, status) }
        resolver.update(
            Uri.withAppendedPath(Contains.CONTENT_URI, containsId.toString()),
            values, null, null
        )
    }

    /** Flips an item between WANT_TO_BUY and BOUGHT. */
    open fun toggleItemBought(item: ShoppingItem) {
        val newStatus = if (item.status == Status.BOUGHT) Status.WANT_TO_BUY else Status.BOUGHT
        setItemStatus(item.containsId, newStatus)
    }

    /** Creates a list by name, or returns the id of the existing list with that name. */
    open fun createList(name: String): Long = ShoppingUtils.getList(context, name)
}
