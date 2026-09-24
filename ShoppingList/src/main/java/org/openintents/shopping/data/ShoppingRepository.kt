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

    /** Remembers [listId] as the list to open next time. */
    fun setActiveList(listId: Long)

    /** The items currently on [listId] (excludes items removed from the list). */
    fun getItems(listId: Long): List<ShoppingItem>

    /**
     * Every item ever on [listId], INCLUDING ones removed from the list (status
     * REMOVED_FROM_LIST). Used by "Pick items" mode to re-add past items.
     */
    fun getAllListItems(listId: Long): List<ShoppingItem>

    /** Adds (or reuses) an item by name on [listId] as "want to buy". Returns item id, or -1 for blank. */
    fun addItem(listId: Long, name: String): Long

    /**
     * Distinct item names from the whole catalogue (every list), sorted, for the
     * add-field auto-suggestions. Items on the current list are a subset of these.
     */
    fun getItemNameSuggestions(): List<String>

    /**
     * Applies [edit] to [item]: name/price/units/tags on the item itself,
     * quantity/priority on its membership of the current list.
     */
    fun updateItem(item: ShoppingItem, edit: ItemEdit)

    /** Removes [item] from [listId] (the item stays in the catalogue / other lists). */
    fun removeItem(listId: Long, item: ShoppingItem)

    /** Sets the per-list status of a relation row to one of [Status]. */
    fun setItemStatus(containsId: Long, status: Long)

    /** Puts an item on the list (WANT_TO_BUY) or removes it (REMOVED_FROM_LIST). */
    fun setItemOnList(item: ShoppingItem, onList: Boolean) {
        setItemStatus(item.containsId, if (onList) Status.WANT_TO_BUY else Status.REMOVED_FROM_LIST)
    }

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

    /** The theme selected for a list (stored in Lists.SKIN_BACKGROUND). */
    fun getListTheme(listId: Long): ListTheme

    /** Sets the theme for a list. */
    fun setListTheme(listId: Long, theme: ListTheme)

    /** Renames a list. */
    fun renameList(listId: Long, newName: String)

    /** Deletes a list (and its items' membership). */
    fun deleteList(listId: Long)

    /** Marks every item on [listId] as bought (true) or want-to-buy (false). */
    fun markAllItems(listId: Long, bought: Boolean) {
        val target = if (bought) Status.BOUGHT else Status.WANT_TO_BUY
        getItems(listId).forEach { if (it.status != target) setItemStatus(it.containsId, target) }
    }

    /** The stores defined for [listId]. */
    fun getStores(listId: Long): List<StoreInfo>

    /** Creates (or reuses) a store by name on [listId]. Returns store id, or -1 for blank. */
    fun addStore(listId: Long, name: String): Long

    /** Deletes a store (and its per-item entries). */
    fun removeStore(storeId: Long)

    /** Exports all lists as OpenIntents CSV to [writer]. */
    fun exportCsv(writer: java.io.Writer)

    /** Imports OpenIntents CSV from [reader]; [importPolicy] is a ConvertCsvBaseActivity.IMPORT_POLICY_*. */
    fun importCsv(reader: java.io.Reader, importPolicy: Int)

    /** The free-text note attached to an item (Items.NOTE), or null. */
    fun getItemNote(itemId: Long): String?

    /** Per-store prices for an item: storeId -> price in cents (absent/null = unset). */
    fun getItemStorePrices(itemId: Long): Map<Long, Long?>

    /** Prices (cents) at [storeId], keyed by itemId — used to show a store's prices on the list. */
    fun getStorePricesForList(storeId: Long): Map<Long, Long?>

    /** Sets the price (in cents) of an item at a store; null leaves it unchanged. */
    fun setItemStorePrice(itemId: Long, storeId: Long, priceCents: Long?)
}
