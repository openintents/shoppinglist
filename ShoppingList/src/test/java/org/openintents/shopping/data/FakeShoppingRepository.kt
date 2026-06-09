package org.openintents.shopping.data

import org.openintents.shopping.library.provider.ShoppingContract.Status

/**
 * In-memory [ShoppingRepository] for fast, framework-free ViewModel tests.
 * Mirrors the observable behavior of ProviderShoppingRepository.
 */
class FakeShoppingRepository : ShoppingRepository {

    private val lists = mutableListOf<ShoppingListInfo>()
    private val itemsByList = mutableMapOf<Long, MutableList<ShoppingItem>>()
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

    override fun setItemStatus(containsId: Long, status: Long) {
        itemsByList.values.forEach { items ->
            val idx = items.indexOfFirst { it.containsId == containsId }
            if (idx >= 0) items[idx] = items[idx].copy(status = status)
        }
    }
}
