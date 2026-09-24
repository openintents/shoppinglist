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

    /**
     * The sort order of [listId]: one of the legacy sort order values
     * (res/values/strings_not_for_translation.xml preference_sortorder_entryvalues).
     * Per list when "perListSort" is on, else the global "sortorder" setting.
     */
    fun getSortOrder(listId: Long): Int

    /** Sets the sort order (for this list with "perListSort", else globally). */
    fun setSortOrder(listId: Long, sortOrder: Int)

    /** All shopping lists, in the order chosen in the settings. */
    fun getLists(): List<ShoppingListInfo>

    /** The id of the list to show by default (creates the initial list if needed). */
    fun getDefaultListId(): Long

    /** Remembers [listId] as the list to open next time. */
    fun setActiveList(listId: Long)

    /** The items currently on [listId] (excludes items removed from the list), sorted per [getSortOrder]. */
    fun getItems(listId: Long): List<ShoppingItem>

    /**
     * Every item ever on [listId], INCLUDING ones removed from the list (status
     * REMOVED_FROM_LIST). Used by "Pick items" mode to re-add past items.
     */
    fun getAllListItems(listId: Long): List<ShoppingItem>

    /** Adds (or reuses) an item by name on [listId] as "want to buy". Returns item id, or -1 for blank. */
    fun addItem(listId: Long, name: String): Long

    /**
     * Adds items sent by another app (shared text, INSERT_FROM_EXTRAS). Quantity
     * and price are optional and may be null. Returns the number of items added.
     */
    fun addItems(listId: Long, items: List<NewItem>): Int

    /** The filters set on [listId]. */
    fun getListFilters(listId: Long): ListFilters

    /** Only show items stocked at [storeId] (null = all stores; needs "use_filters"). */
    fun setStoreFilter(listId: Long, storeId: Long?)

    /** Only show items tagged [tag] (null = all). */
    fun setTagFilter(listId: Long, tag: String?)

    /** The distinct tags of the items on [listId], sorted. */
    fun getListTags(listId: Long): List<String>

    /** Moves an item (its row, with quantity/priority/status) to another list. */
    fun moveItem(item: ShoppingItem, targetListId: Long)

    /** Copies an item (a new catalogue item on the same list). Returns the new row id, or null. */
    fun copyItem(item: ShoppingItem): Long?

    /**
     * Deletes an item from [listId] for good, and from the catalogue if no other
     * list has it (with its store prices).
     */
    fun deleteItem(listId: Long, item: ShoppingItem)

    /** Restores rows to a previous state (undo of mark all / clean up). */
    fun restore(snapshots: List<ItemSnapshot>) {
        snapshots.forEach { setItemStatus(it.containsId, it.status) }
    }

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

    /**
     * Flips an item between WANT_TO_BUY and BOUGHT, based on its current stored
     * status (so two quick taps flip it twice). Returns the new status.
     */
    fun toggleItemBought(item: ShoppingItem): Long {
        val current = getItemStatus(item.containsId) ?: item.status
        val newStatus = if (current == Status.BOUGHT) Status.WANT_TO_BUY else Status.BOUGHT
        setItemStatus(item.containsId, newStatus)
        return newStatus
    }

    /** The stored status of a relation row, or null if it does not exist. */
    fun getItemStatus(containsId: Long): Long?

    /** Removes every bought item from [listId]. Returns their previous state (for undo). */
    fun cleanupList(listId: Long): List<ItemSnapshot> {
        val bought = getItems(listId).filter { it.status == Status.BOUGHT }
        bought.forEach { removeItem(listId, it) }
        return bought.map { ItemSnapshot(it.containsId, it.status, it.quantity) }
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

    /**
     * Marks every item on [listId] as bought (true) or want-to-buy (false).
     * Returns the previous state of the changed items (for undo).
     */
    fun markAllItems(listId: Long, bought: Boolean): List<ItemSnapshot> {
        val target = if (bought) Status.BOUGHT else Status.WANT_TO_BUY
        val changed = getItems(listId).filter { it.status != target }
        changed.forEach { setItemStatus(it.containsId, target) }
        return changed.map { ItemSnapshot(it.containsId, it.status, it.quantity) }
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
