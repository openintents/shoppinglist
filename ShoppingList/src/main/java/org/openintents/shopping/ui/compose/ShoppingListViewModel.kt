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
import org.openintents.shopping.data.ListMode
import org.openintents.shopping.data.ListTheme
import org.openintents.shopping.data.ListTotals
import org.openintents.shopping.data.ProviderShoppingRepository
import org.openintents.shopping.data.ShoppingItem
import org.openintents.shopping.data.ShoppingListInfo
import org.openintents.shopping.data.SettingsRepository
import org.openintents.shopping.data.SharedPrefsSettingsRepository
import org.openintents.shopping.data.ShoppingRepository
import org.openintents.shopping.data.SortMode
import org.openintents.shopping.data.StoreInfo
import org.openintents.shopping.data.arrangeItems
import org.openintents.shopping.data.computeTotals

/** One-shot feedback shown to the user (the UI maps it to a translated string). */
enum class UserMessage { EXPORTED, EXPORT_FAILED, IMPORTED, IMPORT_FAILED }

/** Immutable UI state for the shopping screen. */
data class ShoppingUiState(
    val lists: List<ShoppingListInfo> = emptyList(),
    val currentListId: Long = -1L,
    val items: List<ShoppingItem> = emptyList(),
    val mode: ListMode = ListMode.SHOPPING,
    val pickItems: List<ShoppingItem> = emptyList(),
    val stores: List<StoreInfo> = emptyList(),
    val selectedStoreId: Long? = null,
    val storePricesForList: Map<Long, Long?> = emptyMap(),
    val editingStorePrices: Map<Long, Long?> = emptyMap(),
    val editingNote: String? = null,
    /** The item whose note/store prices are loaded into [editingNote]/[editingStorePrices]. */
    val editingItemId: Long? = null,
    val sortMode: SortMode = SortMode.UNCHECKED_FIRST,
    val hideChecked: Boolean = false,
    val theme: ListTheme = ListTheme.DEFAULT,
    /** "showprice" setting. */
    val showPrice: Boolean = true,
    /** "capitalization" setting: 0 = none, 1 = sentences, 2 = words. */
    val capitalization: Int = 1,
    /** Catalogue item names for the add-field auto-suggestions. */
    val suggestions: List<String> = emptyList(),
    val loading: Boolean = true,
    val userMessage: UserMessage? = null,
    /** Set after an add so the list can scroll to the new item; the UI consumes it. */
    val scrollToContainsId: Long? = null,
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
    private val settings: SettingsRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(ShoppingUiState())
    val state: StateFlow<ShoppingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // getDefaultListId() must run FIRST: on a fresh install it creates the
            // default list, which getLists() then needs to return for the drawer.
            val (defaultId, lists) = withContext(ioDispatcher) {
                val id = repository.getDefaultListId()
                id to repository.getLists()
            }
            _state.update { it.copy(lists = lists, currentListId = defaultId) }
            loadSettings()
            refresh()
        }
    }

    /** Re-reads the settings the Compose UI honors (they can change in Settings). */
    private suspend fun loadSettings() {
        val s = settings ?: return
        val (hideChecked, showPrice, capitalization) = withContext(ioDispatcher) {
            Triple(
                s.getBoolean(PREF_HIDE_CHECKED, false),
                s.getBoolean(PREF_SHOW_PRICE, true),
                s.getString(PREF_CAPITALIZATION, "1").toIntOrNull()?.takeIf { it in 0..2 } ?: 1,
            )
        }
        _state.update {
            it.copy(hideChecked = hideChecked, showPrice = showPrice, capitalization = capitalization)
        }
    }

    /**
     * Reloads everything when the screen comes back to the foreground: the data
     * may have been changed by the widget, the legacy UI, automation or Settings.
     */
    fun onResume() {
        if (_state.value.currentListId < 0) return // initial load still running
        viewModelScope.launch {
            loadSettings()
            val lists = withContext(ioDispatcher) { repository.getLists() }
            val current = _state.value.currentListId
            if (lists.none { it.id == current }) {
                // The current list was deleted elsewhere.
                val newId = withContext(ioDispatcher) { repository.getDefaultListId() }
                val newLists = withContext(ioDispatcher) { repository.getLists() }
                _state.update {
                    it.copy(
                        lists = newLists, currentListId = newId, loading = true,
                        selectedStoreId = null, storePricesForList = emptyMap()
                    )
                }
            } else {
                _state.update { it.copy(lists = lists) }
            }
            refresh()
        }
    }

    fun refresh() = viewModelScope.launch {
        val listId = _state.value.currentListId
        if (listId < 0) return@launch
        val mode = _state.value.mode
        val storeId = _state.value.selectedStoreId
        data class Loaded(
            val items: List<ShoppingItem>,
            val stores: List<StoreInfo>,
            val storePrices: Map<Long, Long?>,
            val theme: ListTheme,
            val suggestions: List<String>,
            val pickItems: List<ShoppingItem>,
        )
        val loaded = withContext(ioDispatcher) {
            Loaded(
                items = repository.getItems(listId),
                stores = repository.getStores(listId),
                storePrices = if (storeId != null) repository.getStorePricesForList(storeId) else emptyMap(),
                theme = repository.getListTheme(listId),
                suggestions = repository.getItemNameSuggestions(),
                pickItems = if (mode == ListMode.PICK_ITEMS) repository.getAllListItems(listId) else emptyList(),
            )
        }
        _state.update {
            // Drop the result if the user switched list/mode/store while loading:
            // a newer refresh() is on its way and must not be overwritten.
            if (it.currentListId != listId || it.mode != mode || it.selectedStoreId != storeId) it
            else it.copy(
                items = loaded.items, pickItems = loaded.pickItems, stores = loaded.stores,
                storePricesForList = loaded.storePrices, theme = loaded.theme,
                suggestions = loaded.suggestions, loading = false
            )
        }
    }

    fun setMode(mode: ListMode) {
        if (mode == _state.value.mode) return
        _state.update { it.copy(mode = mode) }
        refresh()
    }

    /** In pick mode: toggle an item on/off the current list. */
    fun pickToggle(item: ShoppingItem) = viewModelScope.launch {
        withContext(ioDispatcher) { repository.setItemOnList(item, !item.isOnList) }
        refresh()
    }

    fun setTheme(theme: ListTheme) = viewModelScope.launch {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.setListTheme(listId, theme) }
        _state.update { it.copy(theme = theme) }
    }

    fun selectList(listId: Long) {
        if (listId == _state.value.currentListId) return
        rememberActiveList(listId)
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
        if (newId < 0) return@launch
        val lists = withContext(ioDispatcher) {
            repository.setActiveList(newId)
            repository.getLists()
        }
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
        if (listId < 0) return@launch
        val itemId = withContext(ioDispatcher) { repository.addItem(listId, name) }
        refresh().join()
        if (itemId < 0 || _state.value.currentListId != listId) return@launch
        // Tell the UI to scroll to where the new item landed (sort decides the position).
        val containsId = _state.value.items.firstOrNull { it.itemId == itemId }?.containsId
        _state.update { it.copy(scrollToContainsId = containsId) }
    }

    fun consumeScrollTarget() = _state.update { it.copy(scrollToContainsId = null) }

    fun toggle(item: ShoppingItem) = viewModelScope.launch {
        withContext(ioDispatcher) { repository.toggleItemBought(item) }
        refresh()
    }

    /** Restores an item's status (used by the undo snackbar). */
    fun restoreStatus(containsId: Long, status: Long) = viewModelScope.launch {
        withContext(ioDispatcher) { repository.setItemStatus(containsId, status) }
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

    fun toggleHideChecked() {
        _state.update { it.copy(hideChecked = !it.hideChecked) }
        val hide = _state.value.hideChecked
        settings?.let { s -> viewModelScope.launch(ioDispatcher) { s.setBoolean(PREF_HIDE_CHECKED, hide) } }
    }

    fun cleanup() = viewModelScope.launch {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.cleanupList(listId) }
        refresh()
    }

    fun markAll(bought: Boolean) = viewModelScope.launch {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.markAllItems(listId, bought) }
        refresh()
    }

    fun renameCurrentList(newName: String) = viewModelScope.launch {
        if (newName.isBlank()) return@launch
        val listId = _state.value.currentListId
        val lists = withContext(ioDispatcher) {
            repository.renameList(listId, newName)
            repository.getLists()
        }
        _state.update { it.copy(lists = lists) }
    }

    fun deleteCurrentList() = viewModelScope.launch {
        val listId = _state.value.currentListId
        val (newId, lists) = withContext(ioDispatcher) {
            repository.deleteList(listId)
            // Switch to another list, or recreate the default if none remain.
            val pickId = repository.getLists().firstOrNull()?.id ?: repository.getDefaultListId()
            repository.setActiveList(pickId)
            pickId to repository.getLists()
        }
        _state.update {
            it.copy(
                lists = lists, currentListId = newId, loading = true,
                selectedStoreId = null, storePricesForList = emptyMap()
            )
        }
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

    /** Loads the per-store prices AND note for [itemId] (call when opening item edit). */
    fun loadItemEditData(itemId: Long) {
        // Already loaded (e.g. the dialog was recomposed after a rotation): keep it.
        if (_state.value.editingItemId == itemId) return
        // Clear the previous item's values so they never show (or get saved) for this one.
        _state.update { it.copy(editingItemId = itemId, editingStorePrices = emptyMap(), editingNote = null) }
        viewModelScope.launch {
            val (prices, note) = withContext(ioDispatcher) {
                repository.getItemStorePrices(itemId) to repository.getItemNote(itemId)
            }
            _state.update {
                if (it.editingItemId != itemId) it
                else it.copy(editingStorePrices = prices, editingNote = note)
            }
        }
    }

    /** The item editor was closed. */
    fun endItemEdit() = _state.update {
        it.copy(editingItemId = null, editingStorePrices = emptyMap(), editingNote = null)
    }

    fun setStorePrice(itemId: Long, storeId: Long, priceCents: Long?) = viewModelScope.launch {
        withContext(ioDispatcher) { repository.setItemStorePrice(itemId, storeId, priceCents) }
        val prices = withContext(ioDispatcher) { repository.getItemStorePrices(itemId) }
        _state.update { if (it.editingItemId != itemId) it else it.copy(editingStorePrices = prices) }
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
        _state.update { it.copy(userMessage = if (result.isSuccess) UserMessage.EXPORTED else UserMessage.EXPORT_FAILED) }
    }

    fun importFrom(uri: android.net.Uri) = viewModelScope.launch {
        val cr = contentResolver ?: return@launch
        val result = withContext(ioDispatcher) {
            runCatching {
                cr.openInputStream(uri)?.use { ins ->
                    java.io.InputStreamReader(ins).use { r ->
                        // KEEP (the legacy default): existing items keep their tags/prices.
                        repository.importCsv(
                            r,
                            org.openintents.convertcsv.common.ConvertCsvBaseActivity.IMPORT_POLICY_KEEP
                        )
                    }
                } ?: throw java.io.IOException("Cannot open input stream")
            }
        }
        refresh()
        _state.update { it.copy(userMessage = if (result.isSuccess) UserMessage.IMPORTED else UserMessage.IMPORT_FAILED) }
    }

    fun consumeMessage() = _state.update { it.copy(userMessage = null) }

    private fun rememberActiveList(listId: Long) {
        viewModelScope.launch(ioDispatcher) { repository.setActiveList(listId) }
    }

    companion object {
        private const val PREF_HIDE_CHECKED = "hidechecked"
        private const val PREF_SHOW_PRICE = "showprice"
        private const val PREF_CAPITALIZATION = "capitalization"

        /** Factory that wires the provider-backed repository from the Application context. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                ShoppingListViewModel(
                    ProviderShoppingRepository(app),
                    contentResolver = app.contentResolver,
                    settings = SharedPrefsSettingsRepository(app),
                )
            }
        }
    }
}
