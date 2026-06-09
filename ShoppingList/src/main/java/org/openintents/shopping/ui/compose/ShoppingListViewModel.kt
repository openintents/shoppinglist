package org.openintents.shopping.ui.compose

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openintents.shopping.data.ItemEdit
import org.openintents.shopping.data.ListTotals
import org.openintents.shopping.data.ProviderShoppingRepository
import org.openintents.shopping.data.ShoppingItem
import org.openintents.shopping.data.ShoppingListInfo
import org.openintents.shopping.data.ShoppingRepository
import org.openintents.shopping.data.SortMode
import org.openintents.shopping.data.StoreInfo
import org.openintents.shopping.data.arrangeItems
import org.openintents.shopping.data.computeTotals

/** Immutable UI state for the shopping screen. */
data class ShoppingUiState(
    val lists: List<ShoppingListInfo> = emptyList(),
    val currentListId: Long = -1L,
    val items: List<ShoppingItem> = emptyList(),
    val stores: List<StoreInfo> = emptyList(),
    val editingStorePrices: Map<Long, Long?> = emptyMap(),
    val sortMode: SortMode = SortMode.UNCHECKED_FIRST,
    val hideChecked: Boolean = false,
    val loading: Boolean = true,
) {
    val currentListName: String
        get() = lists.firstOrNull { it.id == currentListId }?.name ?: ""

    /** The items to render, after the user's sort + filter (derived). */
    val visibleItems: List<ShoppingItem>
        get() = arrangeItems(items, sortMode, hideChecked)

    /** Money totals derived from the full [items] (independent of the view filter). */
    val totals: ListTotals
        get() = computeTotals(items)
}

/**
 * Owns the screen state and the actions the UI can take. Depends on the
 * [ShoppingRepository] interface (constructor-injected) and a [CoroutineDispatcher]
 * for off-main work, so it can be unit-tested with a fake repository and a test
 * dispatcher — no Android framework needed (see ShoppingListViewModelTest).
 */
class ShoppingListViewModel(
    private val repository: ShoppingRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(ShoppingUiState())
    val state: StateFlow<ShoppingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val (lists, defaultId) = withContext(ioDispatcher) {
                repository.getLists() to repository.getDefaultListId()
            }
            _state.update { it.copy(lists = lists, currentListId = defaultId) }
            refresh()
        }
    }

    fun refresh() = viewModelScope.launch {
        val listId = _state.value.currentListId
        val (items, stores) = withContext(ioDispatcher) {
            repository.getItems(listId) to repository.getStores(listId)
        }
        _state.update { it.copy(items = items, stores = stores, loading = false) }
    }

    fun selectList(listId: Long) {
        if (listId == _state.value.currentListId) return
        _state.update { it.copy(currentListId = listId, loading = true) }
        refresh()
    }

    fun createList(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val newId = withContext(ioDispatcher) {
            val id = repository.createList(name.trim())
            id
        }
        val lists = withContext(ioDispatcher) { repository.getLists() }
        _state.update { it.copy(lists = lists, currentListId = newId, loading = true) }
        refresh()
    }

    fun addItem(name: String) = viewModelScope.launch {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.addItem(listId, name) }
        refresh()
    }

    fun toggle(item: ShoppingItem) = viewModelScope.launch {
        withContext(ioDispatcher) { repository.toggleItemBought(item) }
        refresh()
    }

    fun updateItem(item: ShoppingItem, edit: ItemEdit) = viewModelScope.launch {
        withContext(ioDispatcher) { repository.updateItem(item, edit) }
        refresh()
    }

    fun removeItem(item: ShoppingItem) = viewModelScope.launch {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.removeItem(listId, item) }
        refresh()
    }

    fun setSortMode(mode: SortMode) = _state.update { it.copy(sortMode = mode) }

    fun toggleHideChecked() = _state.update { it.copy(hideChecked = !it.hideChecked) }

    fun cleanup() = viewModelScope.launch {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.cleanupList(listId) }
        refresh()
    }

    fun addStore(name: String) = viewModelScope.launch {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.addStore(listId, name) }
        refresh()
    }

    fun removeStore(store: StoreInfo) = viewModelScope.launch {
        withContext(ioDispatcher) { repository.removeStore(store.id) }
        refresh()
    }

    /** Loads the per-store prices for [itemId] into state (call when opening item edit). */
    fun loadStorePrices(itemId: Long) = viewModelScope.launch {
        val prices = withContext(ioDispatcher) { repository.getItemStorePrices(itemId) }
        _state.update { it.copy(editingStorePrices = prices) }
    }

    fun setStorePrice(itemId: Long, storeId: Long, priceCents: Long?) = viewModelScope.launch {
        withContext(ioDispatcher) { repository.setItemStorePrice(itemId, storeId, priceCents) }
        val prices = withContext(ioDispatcher) { repository.getItemStorePrices(itemId) }
        _state.update { it.copy(editingStorePrices = prices) }
    }

    companion object {
        /** Factory that wires the provider-backed repository from the Application context. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                ShoppingListViewModel(ProviderShoppingRepository(app))
            }
        }
    }
}
