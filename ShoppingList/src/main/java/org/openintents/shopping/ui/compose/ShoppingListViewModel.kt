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
    val selectedStoreId: Long? = null,
    val storePricesForList: Map<Long, Long?> = emptyMap(),
    val editingStorePrices: Map<Long, Long?> = emptyMap(),
    val sortMode: SortMode = SortMode.UNCHECKED_FIRST,
    val hideChecked: Boolean = false,
    val loading: Boolean = true,
    val userMessage: String? = null,
) {
    val currentListName: String
        get() = lists.firstOrNull { it.id == currentListId }?.name ?: ""

    /**
     * Items with the selected store's price substituted in (when a store is
     * selected). Both the displayed list and the totals derive from these.
     */
    val effectiveItems: List<ShoppingItem>
        get() = if (selectedStoreId == null) items
        else items.map { it.copy(priceCents = storePricesForList[it.itemId] ?: it.priceCents) }

    /** The items to render, after the user's sort + filter (derived). */
    val visibleItems: List<ShoppingItem>
        get() = arrangeItems(effectiveItems, sortMode, hideChecked)

    /** Money totals (using the selected store's prices when a store is selected). */
    val totals: ListTotals
        get() = computeTotals(effectiveItems)
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
    private val contentResolver: android.content.ContentResolver? = null,
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
        val storeId = _state.value.selectedStoreId
        val (items, stores) = withContext(ioDispatcher) {
            repository.getItems(listId) to repository.getStores(listId)
        }
        val storePrices = if (storeId != null) {
            withContext(ioDispatcher) { repository.getStorePricesForList(storeId) }
        } else emptyMap()
        _state.update {
            it.copy(items = items, stores = stores, storePricesForList = storePrices, loading = false)
        }
    }

    fun selectList(listId: Long) {
        if (listId == _state.value.currentListId) return
        _state.update {
            it.copy(
                currentListId = listId, loading = true,
                selectedStoreId = null, storePricesForList = emptyMap()
            )
        }
        refresh()
    }

    fun selectStore(storeId: Long?) {
        if (storeId == null) {
            _state.update { it.copy(selectedStoreId = null, storePricesForList = emptyMap()) }
            return
        }
        _state.update { it.copy(selectedStoreId = storeId) }
        viewModelScope.launch {
            val prices = withContext(ioDispatcher) { repository.getStorePricesForList(storeId) }
            _state.update { it.copy(storePricesForList = prices) }
        }
    }

    fun createList(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val newId = withContext(ioDispatcher) {
            val id = repository.createList(name.trim())
            id
        }
        val lists = withContext(ioDispatcher) { repository.getLists() }
        _state.update {
            it.copy(
                lists = lists, currentListId = newId, loading = true,
                selectedStoreId = null, storePricesForList = emptyMap()
            )
        }
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

    fun exportTo(uri: android.net.Uri) = viewModelScope.launch {
        val cr = contentResolver ?: return@launch
        val result = withContext(ioDispatcher) {
            runCatching {
                cr.openOutputStream(uri)?.use { os ->
                    java.io.OutputStreamWriter(os).use { w -> repository.exportCsv(w) }
                } ?: throw java.io.IOException("Cannot open output stream")
            }
        }
        _state.update { it.copy(userMessage = if (result.isSuccess) "Exported" else "Export failed") }
    }

    fun importFrom(uri: android.net.Uri) = viewModelScope.launch {
        val cr = contentResolver ?: return@launch
        val result = withContext(ioDispatcher) {
            runCatching {
                cr.openInputStream(uri)?.use { ins ->
                    java.io.InputStreamReader(ins).use { r ->
                        repository.importCsv(
                            r,
                            org.openintents.convertcsv.common.ConvertCsvBaseActivity.IMPORT_POLICY_OVERWRITE
                        )
                    }
                } ?: throw java.io.IOException("Cannot open input stream")
            }
        }
        refresh()
        _state.update { it.copy(userMessage = if (result.isSuccess) "Imported" else "Import failed") }
    }

    fun consumeMessage() = _state.update { it.copy(userMessage = null) }

    companion object {
        /** Factory that wires the provider-backed repository from the Application context. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                ShoppingListViewModel(
                    ProviderShoppingRepository(app),
                    contentResolver = app.contentResolver,
                )
            }
        }
    }
}
