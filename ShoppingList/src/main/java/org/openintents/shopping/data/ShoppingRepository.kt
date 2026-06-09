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
     * Applies [edit] to [item]: name/price/units/tags on the item itself,
     * quantity/priority on its membership of the current list.
     */
    fun updateItem(item: ShoppingItem, edit: ItemEdit)

    /** Removes [item] from [listId] (the item stays in the catalogue / other lists). */
    fun removeItem(listId: Long, item: ShoppingItem)

    /** Sets the per-list status of a relation row to one of [Status]. */
    fun setItemStatus(containsId: Long, status: Long)

    /** Flips an item between WANT_TO_BUY and BOUGHT. */
    fun toggleItemBought(item: ShoppingItem) {
        val newStatus = if (item.status == Status.BOUGHT) Status.WANT_TO_BUY else Status.BOUGHT
        setItemStatus(item.containsId, newStatus)
    }

    /** Removes every bought item from [listId] (marks them removed-from-list). Returns the count. */
    fun cleanupList(listId: Long): Int {
        val bought = getItems(listId).filter { it.status == Status.BOUGHT }
        bought.forEach { setItemStatus(it.containsId, Status.REMOVED_FROM_LIST) }
        return bought.size
    }

    /** Creates a list by name, or returns the id of the existing list with that name. */
    fun createList(name: String): Long

    /** The stores defined for [listId]. */
    fun getStores(listId: Long): List<StoreInfo>

    /** Creates (or reuses) a store by name on [listId]. Returns store id, or -1 for blank. */
    fun addStore(listId: Long, name: String): Long

    /** Deletes a store (and its per-item entries). */
    fun removeStore(storeId: Long)

    /** Per-store prices for an item: storeId -> price in cents (absent/null = unset). */
    fun getItemStorePrices(itemId: Long): Map<Long, Long?>

    /** Prices (cents) at [storeId], keyed by itemId — used to show a store's prices on the list. */
    fun getStorePricesForList(storeId: Long): Map<Long, Long?>

    /** Sets the price (in cents) of an item at a store; null leaves it unchanged. */
    fun setItemStorePrice(itemId: Long, storeId: Long, priceCents: Long?)
}
