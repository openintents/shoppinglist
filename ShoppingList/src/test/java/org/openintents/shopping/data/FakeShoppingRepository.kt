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

    override fun getDefaultListId(): Long {
        if (lists.isEmpty()) createList("My list")
        return lists.first().id
    }

    override fun getLists(): List<ShoppingListInfo> = lists.toList()

    override fun getItems(listId: Long): List<ShoppingItem> =
        itemsByList[listId].orEmpty().filter { it.status != Status.REMOVED_FROM_LIST }

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

    override fun updateItem(item: ShoppingItem, edit: ItemEdit) {
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

    override fun removeItem(listId: Long, item: ShoppingItem) {
        itemsByList[listId]?.removeAll { it.containsId == item.containsId }
    }

    override fun setItemStatus(containsId: Long, status: Long) {
        itemsByList.values.forEach { items ->
            val idx = items.indexOfFirst { it.containsId == containsId }
            if (idx >= 0) items[idx] = items[idx].copy(status = status)
        }
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

    override fun setItemStorePrice(itemId: Long, storeId: Long, priceCents: Long?) {
        if (priceCents == null) return // matches provider impl: null leaves it unchanged
        storePrices[itemId to storeId] = priceCents
    }
}
