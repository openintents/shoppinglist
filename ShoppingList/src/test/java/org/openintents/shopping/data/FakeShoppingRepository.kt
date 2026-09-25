package org.openintents.shopping.data

import org.openintents.shopping.library.provider.ShoppingContract.Status

/**
 * In-memory [ShoppingRepository] for fast, framework-free ViewModel tests.
 * Mirrors the observable behavior of ProviderShoppingRepository.
 */
class FakeShoppingRepository : ShoppingRepository {

    private val lists = mutableListOf<ShoppingListInfo>()
    private val itemsByList = mutableMapOf<Long, MutableList<ShoppingItem>>()
    private val storesByList = mutableMapOf<Long, MutableList<StoreInfo>>()
    private var nextId = 1L

    var activeListId: Long? = null

    override fun getDefaultListId(): Long {
        if (lists.isEmpty()) createList("My list")
        return activeListId?.takeIf { id -> lists.any { it.id == id } } ?: lists.first().id
    }

    override fun setActiveList(listId: Long) {
        activeListId = listId
    }

    override fun getLists(): List<ShoppingListInfo> = lists.toList()

    private val sortByList = mutableMapOf<Long, Int>()

    override fun getSortOrder(listId: Long): Int = sortByList[listId] ?: 0

    override fun setSortOrder(listId: Long, sortOrder: Int) { sortByList[listId] = sortOrder }

    /** Implements the two basic legacy sort orders: 0 unchecked first + name, 1 name. */
    override fun getItems(listId: Long): List<ShoppingItem> {
        val items = itemsByList[listId].orEmpty().filter { it.status != Status.REMOVED_FROM_LIST }
        return when (getSortOrder(listId)) {
            1 -> items.sortedBy { it.name.lowercase() }
            else -> items.sortedWith(compareBy({ it.status }, { it.name.lowercase() }))
        }
    }

    override fun getAllListItems(listId: Long): List<ShoppingItem> =
        itemsByList[listId].orEmpty().toList()

    override fun createList(name: String): Long {
        lists.firstOrNull { it.name == name }?.let { return it.id }
        val id = nextId++
        lists.add(ShoppingListInfo(id, name))
        itemsByList[id] = mutableListOf()
        return id
    }

    override fun addItem(listId: Long, name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        val id = nextId++
        itemsByList.getOrPut(listId) { mutableListOf() }
            .add(ShoppingItem(id, id, trimmed, Status.WANT_TO_BUY, null, null, null, null))
        return id
    }

    private val barcodes = mutableMapOf<String, String>()

    override fun addItems(listId: Long, items: List<NewItem>): Int =
        items.count { item ->
            item.barcode?.let { barcodes[it] = item.name.trim() }
            addItem(listId, item.name) >= 0
        }

    override fun getItemNameForBarcode(barcode: String): String? = barcodes[barcode]

    override fun getItemStatus(containsId: Long): Long? =
        itemsByList.values.flatten().firstOrNull { it.containsId == containsId }?.status

    override fun getItemNameSuggestions(): List<String> =
        itemsByList.values.flatten()
            .map { it.name }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }

    private val noteByItem = mutableMapOf<Long, String?>()

    override fun updateItem(item: ShoppingItem, edit: ItemEdit) {
        noteByItem[item.itemId] = edit.note
        itemsByList.values.forEach { items ->
            val idx = items.indexOfFirst { it.containsId == item.containsId }
            if (idx >= 0) {
                items[idx] = items[idx].copy(
                    name = edit.name.trim(),
                    quantity = edit.quantity,
                    priceCents = edit.priceCents,
                    units = edit.units,
                    priority = edit.priority,
                    tags = edit.tags,
                )
            }
        }
    }

    override fun getItemNote(itemId: Long): String? = noteByItem[itemId]

    override fun removeItem(listId: Long, item: ShoppingItem) {
        // Soft-remove (status REMOVED_FROM_LIST), keeping the row for Pick-items mode.
        setItemStatus(item.containsId, Status.REMOVED_FROM_LIST)
    }

    override fun setItemStatus(containsId: Long, status: Long) {
        itemsByList.values.forEach { items ->
            val idx = items.indexOfFirst { it.containsId == containsId }
            if (idx >= 0) items[idx] = items[idx].copy(status = status)
        }
    }

    private val filtersByList = mutableMapOf<Long, ListFilters>()

    override fun getListFilters(listId: Long): ListFilters = filtersByList[listId] ?: ListFilters()

    override fun setStoreFilter(listId: Long, storeId: Long?) {
        filtersByList[listId] = getListFilters(listId).copy(storeId = storeId)
    }

    override fun setTagFilter(listId: Long, tag: String?) {
        filtersByList[listId] = getListFilters(listId).copy(tag = tag)
    }

    override fun getListTags(listId: Long): List<String> =
        itemsByList[listId].orEmpty().flatMap { it.tags.orEmpty().split(',') }
            .map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()

    override fun moveItem(item: ShoppingItem, targetListId: Long) {
        itemsByList.values.forEach { items -> items.removeAll { it.containsId == item.containsId } }
        itemsByList.getOrPut(targetListId) { mutableListOf() }.add(item)
    }

    override fun copyItem(item: ShoppingItem): Long? {
        val id = nextId++
        itemsByList.values.firstOrNull { items -> items.any { it.containsId == item.containsId } }
            ?.add(item.copy(containsId = id, itemId = id))
        return id
    }

    override fun deleteItem(listId: Long, item: ShoppingItem) {
        itemsByList[listId]?.removeAll { it.containsId == item.containsId }
    }

    private val themeByList = mutableMapOf<Long, ListTheme>()

    override fun getListTheme(listId: Long): ListTheme = themeByList[listId] ?: ListTheme.DEFAULT

    override fun setListTheme(listId: Long, theme: ListTheme) { themeByList[listId] = theme }

    override fun renameList(listId: Long, newName: String) {
        val idx = lists.indexOfFirst { it.id == listId }
        if (idx >= 0) lists[idx] = lists[idx].copy(name = newName.trim())
    }

    override fun deleteList(listId: Long) {
        lists.removeAll { it.id == listId }
        itemsByList.remove(listId)
    }

    override fun getStores(listId: Long): List<StoreInfo> =
        storesByList[listId].orEmpty().sortedBy { it.name.lowercase() }

    override fun addStore(listId: Long, name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        val existing = storesByList[listId]?.firstOrNull { it.name == trimmed }
        if (existing != null) return existing.id
        val id = nextId++
        storesByList.getOrPut(listId) { mutableListOf() }.add(StoreInfo(id, trimmed))
        return id
    }

    override fun removeStore(storeId: Long) {
        storesByList.values.forEach { stores -> stores.removeAll { it.id == storeId } }
        storePrices.keys.filter { it.second == storeId }.forEach { storePrices.remove(it) }
    }

    private val storePrices = mutableMapOf<Pair<Long, Long>, Long?>()

    override fun getItemStorePrices(itemId: Long): Map<Long, Long?> =
        storePrices.filterKeys { it.first == itemId }.mapKeys { it.key.second }

    override fun getStorePricesForList(storeId: Long): Map<Long, Long?> =
        storePrices.filterKeys { it.second == storeId }.mapKeys { it.key.first }

    override fun exportCsv(writer: java.io.Writer) { /* not used in fake-based tests */ }
    override fun importCsv(reader: java.io.Reader, importPolicy: Int) { /* not used in fake-based tests */ }

    override fun setItemStorePrice(itemId: Long, storeId: Long, priceCents: Long?) {
        if (priceCents == null) return // matches provider impl: null leaves it unchanged
        storePrices[itemId to storeId] = priceCents
    }
}
