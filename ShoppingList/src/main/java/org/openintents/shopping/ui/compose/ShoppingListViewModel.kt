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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openintents.shopping.data.ItemEdit
import org.openintents.shopping.data.ListMode
import org.openintents.shopping.data.ListTheme
import org.openintents.shopping.data.ItemSnapshot
import org.openintents.shopping.data.ListFilters
import org.openintents.shopping.data.ListTotals
import org.openintents.shopping.BuildConfig
import org.openintents.shopping.data.NewItem
import org.openintents.shopping.data.OpenFoodFactsLookup
import org.openintents.shopping.data.ProductLookup
import org.openintents.shopping.data.ProviderShoppingRepository
import org.openintents.shopping.data.ShoppingItem
import org.openintents.shopping.data.ShoppingListInfo
import org.openintents.shopping.data.SettingsRepository
import org.openintents.shopping.data.SharedPrefsSettingsRepository
import org.openintents.shopping.data.ShoppingRepository
import org.openintents.shopping.data.StoreInfo
import org.openintents.shopping.data.arrangeItems
import org.openintents.shopping.data.computeTotals
import org.openintents.shopping.data.prioritySubtotal

/** A bulk change that can be undone. */
enum class BulkChange { MARKED_ALL, UNMARKED_ALL, CLEANED_UP }

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
    /** True once the note / store prices of [editingItemId] are loaded (Save waits for it). */
    val editingLoaded: Boolean = false,
    /** The list's sort order (a legacy sort order value, see ShoppingRepository.getSortOrder). */
    val sortOrder: Int = 0,
    val hideChecked: Boolean = false,
    val theme: ListTheme = ListTheme.DEFAULT,
    /** "showprice" setting. */
    val showPrice: Boolean = true,
    /** "capitalization" setting: 0 = none, 1 = sentences, 2 = words. */
    val capitalization: Int = 1,
    /** "fontsize" setting: 0 tiny, 1 small, 2 medium, 3 large (see ListTheme.textSizeSp). */
    val fontSize: Int = 2,
    /** "holosearch" setting: the search/add field is in the top bar instead of at the bottom. */
    val addBarOnTop: Boolean = false,
    /** Which item details the rows show ("showquantity", "showunits", "showtags", "showpriority"). */
    val showQuantity: Boolean = true,
    val showUnits: Boolean = true,
    val showTags: Boolean = true,
    val showPriority: Boolean = true,
    /** "priority_subtotal_threshold" (0 = off) and "priosubtotal_includes_checked". */
    val prioritySubtotalThreshold: Int = 0,
    val prioritySubtotalIncludesChecked: Boolean = true,
    /** A scanned barcode no product name was found for: the UI asks for a name. */
    val unknownBarcode: String? = null,
    /** Name of an item just added from a barcode (the UI confirms it, then consumes it). */
    val addedFromBarcode: String? = null,
    /** "barcode_button" setting: show the scan button next to the add field. */
    val showScanButton: Boolean = true,
    /** "barcode_lookup" setting: look up scanned barcodes on Open Food Facts. */
    val barcodeLookup: Boolean = true,
    /** "compact" setting: denser rows, more items on the screen. */
    val compact: Boolean = false,
    /** "fastscroll" setting: a draggable scroll thumb for long lists. */
    val fastScroll: Boolean = false,
    /** "use_filters" setting: offer the store / tag filter. */
    val useFilters: Boolean = false,
    /** The list's filters and the tags that can be filtered by. */
    val filters: ListFilters = ListFilters(),
    val tags: List<String> = emptyList(),
    /** Set after mark all / unmark all / clean up: the UI offers an undo, then consumes it. */
    val bulkChange: BulkChange? = null,
    /** How many items [bulkChange] changed. */
    val bulkChangeCount: Int = 0,
    /** Set after a copy: the UI opens this row in the editor, then consumes it. */
    val editRequest: Long? = null,
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
        get() = arrangeItems(effectiveItems, hideChecked)

    /** Money totals (using the selected store's prices when a store is selected). */
    val totals: ListTotals
        get() = computeTotals(effectiveItems)

    /** The priority subtotal (0 when off). */
    val prioritySubtotal: Long
        get() = prioritySubtotal(effectiveItems, prioritySubtotalThreshold, prioritySubtotalIncludesChecked)
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
    private val productLookup: ProductLookup? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(ShoppingUiState())

    /** State before the last mark all / unmark all / clean up, for its undo. */
    private var lastBulkChange: List<ItemSnapshot> = emptyList()

    /** A list requested (e.g. by a shortcut) before the initial load finished. */
    private var pendingListId: Long? = null
    val state: StateFlow<ShoppingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // getDefaultListId() must run FIRST: on a fresh install it creates the
            // default list, which getLists() then needs to return for the drawer.
            val (defaultId, lists) = withContext(ioDispatcher) {
                val id = repository.getDefaultListId()
                id to repository.getLists()
            }
            // Read on the main thread, where showList() writes it.
            val requested = pendingListId?.takeIf { p -> lists.any { it.id == p } }
            pendingListId = null
            if (requested != null) rememberActiveList(requested)
            _state.update { it.copy(lists = lists, currentListId = requested ?: defaultId) }
            loadSettings()
            refresh()
        }
    }

    /** Re-reads the settings the Compose UI honors (they can change in Settings). */
    private suspend fun loadSettings() {
        val s = settings ?: return
        val loaded = withContext(ioDispatcher) {
            _state.value.copy(
                hideChecked = s.getBoolean(PREF_HIDE_CHECKED, false),
                showPrice = s.getBoolean(PREF_SHOW_PRICE, true),
                capitalization = s.getString(PREF_CAPITALIZATION, "1").toIntOrNull()
                    ?.takeIf { it in 0..2 } ?: 1,
                fontSize = s.getString(PREF_FONT_SIZE, "2").toIntOrNull()?.takeIf { it in 0..3 } ?: 2,
                addBarOnTop = s.getBoolean(PREF_ADD_BAR_ON_TOP, false),
                showQuantity = s.getBoolean("showquantity", true),
                showUnits = s.getBoolean("showunits", true),
                showTags = s.getBoolean("showtags", true),
                showPriority = s.getBoolean("showpriority", true),
                prioritySubtotalThreshold = s.getString("priority_subtotal_threshold", "0")
                    .toIntOrNull()?.takeIf { it in 0..4 } ?: 0,
                prioritySubtotalIncludesChecked = s.getBoolean("priosubtotal_includes_checked", true),
                useFilters = s.getBoolean("use_filters", false),
                compact = s.getBoolean("compact", false),
                barcodeLookup = s.getBoolean(PREF_BARCODE_LOOKUP, true),
                showScanButton = s.getBoolean("barcode_button", true),
                fastScroll = s.getBoolean("fastscroll", false),
            )
        }
        _state.update {
            it.copy(
                hideChecked = loaded.hideChecked, showPrice = loaded.showPrice,
                capitalization = loaded.capitalization, fontSize = loaded.fontSize,
                addBarOnTop = loaded.addBarOnTop,
                showQuantity = loaded.showQuantity, showUnits = loaded.showUnits,
                showTags = loaded.showTags, showPriority = loaded.showPriority,
                prioritySubtotalThreshold = loaded.prioritySubtotalThreshold,
                prioritySubtotalIncludesChecked = loaded.prioritySubtotalIncludesChecked,
                useFilters = loaded.useFilters,
                compact = loaded.compact, fastScroll = loaded.fastScroll,
                barcodeLookup = loaded.barcodeLookup, showScanButton = loaded.showScanButton,
            )
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
            val sortOrder: Int,
            val filters: ListFilters,
            val tags: List<String>,
        )
        val loaded = withContext(ioDispatcher) {
            Loaded(
                items = repository.getItems(listId),
                stores = repository.getStores(listId),
                storePrices = if (storeId != null) repository.getStorePricesForList(storeId) else emptyMap(),
                theme = repository.getListTheme(listId),
                suggestions = repository.getItemNameSuggestions(),
                pickItems = if (mode == ListMode.PICK_ITEMS) repository.getAllListItems(listId) else emptyList(),
                sortOrder = repository.getSortOrder(listId),
                filters = repository.getListFilters(listId),
                tags = repository.getListTags(listId),
            )
        }
        _state.update {
            // Drop the result if the user switched list/mode/store while loading:
            // a newer refresh() is on its way and must not be overwritten.
            if (it.currentListId != listId || it.mode != mode || it.selectedStoreId != storeId) it
            else it.copy(
                items = loaded.items, pickItems = loaded.pickItems, stores = loaded.stores,
                storePricesForList = loaded.storePrices, theme = loaded.theme,
                suggestions = loaded.suggestions, sortOrder = loaded.sortOrder,
                filters = loaded.filters, tags = loaded.tags, loading = false
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

    /** "Use this theme for all lists". */
    fun setThemeForAllLists(theme: ListTheme) = viewModelScope.launch {
        withContext(ioDispatcher) { repository.getLists().forEach { repository.setListTheme(it.id, theme) } }
        _state.update { it.copy(theme = theme) }
    }

    fun setTheme(theme: ListTheme) = viewModelScope.launch {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.setListTheme(listId, theme) }
        _state.update { it.copy(theme = theme) }
    }

    /** Shows [listId] (from an intent); ignored if there is no such list. */
    fun showList(listId: Long) {
        if (_state.value.currentListId < 0) {
            pendingListId = listId // initial load still running; it picks this up
            return
        }
        viewModelScope.launch {
            // Re-read: the list may have been created while we were in the background.
            val lists = withContext(ioDispatcher) { repository.getLists() }
            _state.update { it.copy(lists = lists) }
            if (lists.any { it.id == listId }) selectList(listId)
        }
    }

    /**
     * Adds items sent by another app (shared text, INSERT_FROM_EXTRAS) to
     * [listId], or to the current list when null.
     */
    fun addItemsFromIntent(listId: Long?, items: List<NewItem>) {
        if (items.none { it.name.isNotBlank() }) return
        viewModelScope.launch {
            // Wait for the initial load, which decides the current list.
            _state.first { it.currentListId >= 0 }
            val lists = withContext(ioDispatcher) { repository.getLists() }
            val target = listId?.takeIf { id -> lists.any { it.id == id } } ?: _state.value.currentListId
            withContext(ioDispatcher) { repository.addItems(target, items) }
            _state.update { it.copy(lists = lists) }
            if (target != _state.value.currentListId) selectList(target) else refresh()
        }
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
        if (storeId == _state.value.selectedStoreId) return
        _state.update { it.copy(selectedStoreId = storeId, storePricesForList = emptyMap()) }
        // refresh() loads the selected store's prices together with the items.
        if (storeId != null) refresh()
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

    /**
     * Adds the product with this barcode: an item that already has the barcode,
     * else the name from Open Food Facts (if enabled); if neither is known the UI
     * asks for a name ([ShoppingUiState.unknownBarcode]).
     */
    fun addScannedBarcode(barcode: String) = viewModelScope.launch {
        val code = barcode.trim()
        if (code.isEmpty() || _state.value.currentListId < 0) return@launch
        val lookup = productLookup?.takeIf { _state.value.barcodeLookup }
        val name = withContext(ioDispatcher) {
            repository.getItemNameForBarcode(code) ?: lookup?.productName(code)
        }
        if (name == null) {
            _state.update { it.copy(unknownBarcode = code) }
        } else {
            addWithBarcode(name, code)
        }
    }

    /** The user named a product whose barcode was unknown. */
    fun nameUnknownBarcode(name: String) {
        val code = _state.value.unknownBarcode ?: return
        _state.update { it.copy(unknownBarcode = null) }
        if (name.isNotBlank()) viewModelScope.launch { addWithBarcode(name.trim(), code) }
    }

    fun dismissUnknownBarcode() = _state.update { it.copy(unknownBarcode = null) }

    fun consumeAddedFromBarcode() = _state.update { it.copy(addedFromBarcode = null) }

    private suspend fun addWithBarcode(name: String, barcode: String) {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.addItems(listId, listOf(NewItem(name, barcode = barcode))) }
        refresh().join()
        val added = _state.value.items.firstOrNull { it.name.equals(name, ignoreCase = true) }
        _state.update { it.copy(addedFromBarcode = name, scrollToContainsId = added?.containsId) }
    }

    fun toggle(item: ShoppingItem) = viewModelScope.launch {
        // The repository flips the stored status, so quick double taps work.
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

    fun setSortOrder(sortOrder: Int) = viewModelScope.launch {
        val listId = _state.value.currentListId
        if (listId < 0) return@launch
        withContext(ioDispatcher) { repository.setSortOrder(listId, sortOrder) }
        refresh()
    }

    fun toggleHideChecked() {
        _state.update { it.copy(hideChecked = !it.hideChecked) }
        val hide = _state.value.hideChecked
        settings?.let { s -> viewModelScope.launch(ioDispatcher) { s.setBoolean(PREF_HIDE_CHECKED, hide) } }
    }

    fun cleanup() = viewModelScope.launch {
        val listId = _state.value.currentListId
        val changed = withContext(ioDispatcher) { repository.cleanupList(listId) }
        offerUndo(changed, BulkChange.CLEANED_UP)
        refresh()
    }

    fun markAll(bought: Boolean) = viewModelScope.launch {
        val listId = _state.value.currentListId
        val changed = withContext(ioDispatcher) { repository.markAllItems(listId, bought) }
        offerUndo(changed, if (bought) BulkChange.MARKED_ALL else BulkChange.UNMARKED_ALL)
        refresh()
    }

    private fun offerUndo(changed: List<ItemSnapshot>, change: BulkChange) {
        if (changed.isEmpty()) return
        lastBulkChange = changed
        _state.update { it.copy(bulkChange = change, bulkChangeCount = changed.size) }
    }

    /** The UI showed the undo snackbar for [ShoppingUiState.bulkChange]. */
    fun consumeBulkChange() = _state.update { it.copy(bulkChange = null) }

    /** Undoes the last mark all / unmark all / clean up. */
    fun undoBulkChange() = viewModelScope.launch {
        val snapshots = lastBulkChange
        lastBulkChange = emptyList()
        if (snapshots.isEmpty()) return@launch
        withContext(ioDispatcher) { repository.restore(snapshots) }
        refresh()
    }

    /** Only show items at [storeId] (null = all); needs the "use_filters" setting. */
    fun setStoreFilter(storeId: Long?) = viewModelScope.launch {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.setStoreFilter(listId, storeId) }
        refresh()
    }

    /** Only show items with [tag] (null = all). */
    fun setTagFilter(tag: String?) = viewModelScope.launch {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.setTagFilter(listId, tag) }
        refresh()
    }

    fun moveItem(item: ShoppingItem, targetListId: Long) = viewModelScope.launch {
        if (targetListId == _state.value.currentListId) return@launch
        withContext(ioDispatcher) { repository.moveItem(item, targetListId) }
        refresh()
    }

    /** Copies an item; the UI then opens the copy in the editor ([ShoppingUiState.editRequest]). */
    fun copyItem(item: ShoppingItem) = viewModelScope.launch {
        val newContainsId = withContext(ioDispatcher) { repository.copyItem(item) }
        refresh().join()
        if (newContainsId != null) _state.update { it.copy(editRequest = newContainsId) }
    }

    fun consumeEditRequest() = _state.update { it.copy(editRequest = null) }

    /** Deletes an item for good (and from the catalogue if no other list has it). */
    fun deleteItem(item: ShoppingItem) = viewModelScope.launch {
        val listId = _state.value.currentListId
        withContext(ioDispatcher) { repository.deleteItem(listId, item) }
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
        if (_state.value.selectedStoreId == store.id) {
            _state.update { it.copy(selectedStoreId = null, storePricesForList = emptyMap()) }
        }
        refresh()
    }

    /** Loads the per-store prices AND note for [itemId] (call when opening item edit). */
    fun loadItemEditData(itemId: Long) {
        // Already loaded (e.g. the dialog was recomposed after a rotation): keep it.
        if (_state.value.editingItemId == itemId) return
        // Clear the previous item's values so they never show (or get saved) for this one.
        _state.update {
            it.copy(
                editingItemId = itemId, editingLoaded = false,
                editingStorePrices = emptyMap(), editingNote = null,
            )
        }
        viewModelScope.launch {
            val (prices, note) = withContext(ioDispatcher) {
                repository.getItemStorePrices(itemId) to repository.getItemNote(itemId)
            }
            _state.update {
                if (it.editingItemId != itemId) it
                else it.copy(editingStorePrices = prices, editingNote = note, editingLoaded = true)
            }
        }
    }

    /** The item editor was closed. */
    fun endItemEdit() = _state.update {
        it.copy(editingItemId = null, editingLoaded = false, editingStorePrices = emptyMap(), editingNote = null)
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
        // An import can create lists: reload them for the drawer.
        val lists = withContext(ioDispatcher) { repository.getLists() }
        _state.update {
            it.copy(
                lists = lists,
                userMessage = if (result.isSuccess) UserMessage.IMPORTED else UserMessage.IMPORT_FAILED,
            )
        }
        refresh()
    }

    fun consumeMessage() = _state.update { it.copy(userMessage = null) }

    private fun rememberActiveList(listId: Long) {
        viewModelScope.launch(ioDispatcher) { repository.setActiveList(listId) }
    }

    companion object {
        private const val PREF_HIDE_CHECKED = "hidechecked"
        private const val PREF_SHOW_PRICE = "showprice"
        private const val PREF_CAPITALIZATION = "capitalization"
        private const val PREF_FONT_SIZE = "fontsize"
        private const val PREF_BARCODE_LOOKUP = "barcode_lookup"
        /** Same key as the legacy "search/add items in action bar" layout choice. */
        private const val PREF_ADD_BAR_ON_TOP = "holosearch"

        /** Factory that wires the provider-backed repository from the Application context. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                ShoppingListViewModel(
                    ProviderShoppingRepository(app),
                    contentResolver = app.contentResolver,
                    settings = SharedPrefsSettingsRepository(app),
                    productLookup = OpenFoodFactsLookup(
                        "OI Shopping List/${BuildConfig.VERSION_NAME} (https://github.com/openintents/shoppinglist)"
                    ),
                )
            }
        }
    }
}
