package org.openintents.shopping.data

import org.openintents.shopping.library.provider.ShoppingContract.Status

/**
 * Business-logic boundary for shopping lists and their items.
 *
 * The UI/ViewModel depend on this interface, not on the Android ContentProvider,
 * so they can be unit-tested with a fake. [ProviderShoppingRepository] is the
 * real implementation; it is covered end-to-end by ShoppingRepositoryTest.
 */
interface ShoppingRepository {

    /** All shopping lists, in the provider's default order. */
    fun getLists(): List<ShoppingListInfo>

    /** The id of the list to show by default (creates the initial list if needed). */
    fun getDefaultListId(): Long

    /** The items currently on [listId] (excludes items removed from the list). */
    fun getItems(listId: Long): List<ShoppingItem>

    /** Adds (or reuses) an item by name on [listId] as "want to buy". Returns item id, or -1 for blank. */
    fun addItem(listId: Long, name: String): Long

    /**
     * Updates an item's name and price, and its per-list quantity.
     * [priceCents] null clears the price; [quantity] null/blank clears the quantity.
     */
    fun updateItem(item: ShoppingItem, name: String, quantity: String?, priceCents: Long?)

    /** Removes [item] from [listId] (the item stays in the catalogue / other lists). */
    fun removeItem(listId: Long, item: ShoppingItem)

    /** Sets the per-list status of a relation row to one of [Status]. */
    fun setItemStatus(containsId: Long, status: Long)

    /** Flips an item between WANT_TO_BUY and BOUGHT. */
    fun toggleItemBought(item: ShoppingItem) {
        val newStatus = if (item.status == Status.BOUGHT) Status.WANT_TO_BUY else Status.BOUGHT
        setItemStatus(item.containsId, newStatus)
    }

    /** Creates a list by name, or returns the id of the existing list with that name. */
    fun createList(name: String): Long
}
