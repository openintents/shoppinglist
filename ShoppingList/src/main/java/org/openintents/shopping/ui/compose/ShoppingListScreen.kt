package org.openintents.shopping.ui.compose

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.openintents.shopping.R
import org.openintents.shopping.data.ItemEdit
import org.openintents.shopping.data.ListFilters
import org.openintents.shopping.data.ListMode
import org.openintents.shopping.data.ListTheme
import org.openintents.shopping.data.ListTotals
import org.openintents.shopping.data.ShoppingItem
import org.openintents.shopping.data.ShoppingListInfo
import org.openintents.shopping.data.StoreInfo
import org.openintents.shopping.data.lineCents
import org.openintents.shopping.library.provider.ShoppingContract.Status
import org.openintents.shopping.library.util.PriceConverter
import org.openintents.shopping.ui.compose.settings.SettingsActivity

/**
 * Stateful entry point: reads [ShoppingListViewModel] state and forwards events.
 * Kept thin so the stateless [ShoppingListScreen] below is previewable/testable.
 */
@Composable
fun ShoppingListRoute(viewModel: ShoppingListViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Data can change while we are in the background (widget, legacy UI,
    // automation, Settings), so reload whenever the screen is shown again.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    ShoppingListScreen(
        state = state,
        onSetMode = viewModel::setMode,
        onPickToggle = viewModel::pickToggle,
        onSelectList = viewModel::selectList,
        onCreateList = viewModel::createList,
        onAddItem = viewModel::addItem,
        onToggleItem = viewModel::toggle,
        onRestoreStatus = viewModel::restoreStatus,
        onUpdateItem = viewModel::updateItem,
        onRemoveItem = viewModel::removeItem,
        onSetSortOrder = viewModel::setSortOrder,
        onToggleHideChecked = viewModel::toggleHideChecked,
        onCleanup = viewModel::cleanup,
        onAddStore = viewModel::addStore,
        onRemoveStore = viewModel::removeStore,
        onLoadItemEditData = viewModel::loadItemEditData,
        onEndItemEdit = viewModel::endItemEdit,
        onSetStorePrice = viewModel::setStorePrice,
        onSelectStore = viewModel::selectStore,
        onExport = viewModel::exportTo,
        onImport = viewModel::importFrom,
        onConsumeMessage = viewModel::consumeMessage,
        onMarkAll = viewModel::markAll,
        onRenameList = viewModel::renameCurrentList,
        onDeleteList = viewModel::deleteCurrentList,
        onSetTheme = viewModel::setTheme,
        onConsumeScroll = viewModel::consumeScrollTarget,
        onSetStoreFilter = viewModel::setStoreFilter,
        onSetTagFilter = viewModel::setTagFilter,
        onUndoBulkChange = viewModel::undoBulkChange,
        onConsumeBulkChange = viewModel::consumeBulkChange,
        onMoveItem = viewModel::moveItem,
        onCopyItem = viewModel::copyItem,
        onDeleteItem = viewModel::deleteItem,
        onConsumeEditRequest = viewModel::consumeEditRequest,
        onSetThemeForAllLists = viewModel::setThemeForAllLists,
        onScannedBarcode = viewModel::addScannedBarcode,
        onNameUnknownBarcode = viewModel::nameUnknownBarcode,
        onDismissUnknownBarcode = viewModel::dismissUnknownBarcode,
        onConsumeAddedFromBarcode = viewModel::consumeAddedFromBarcode,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    state: ShoppingUiState,
    onSetMode: (ListMode) -> Unit,
    onPickToggle: (ShoppingItem) -> Unit,
    onSelectList: (Long) -> Unit,
    onCreateList: (String) -> Unit,
    onAddItem: (String) -> Unit,
    onToggleItem: (ShoppingItem) -> Unit,
    onRestoreStatus: (containsId: Long, status: Long) -> Unit,
    onUpdateItem: (ShoppingItem, ItemEdit) -> Unit,
    onRemoveItem: (ShoppingItem) -> Unit,
    onSetSortOrder: (Int) -> Unit,
    onToggleHideChecked: () -> Unit,
    onCleanup: () -> Unit,
    onAddStore: (String) -> Unit,
    onRemoveStore: (StoreInfo) -> Unit,
    onLoadItemEditData: (Long) -> Unit,
    onEndItemEdit: () -> Unit,
    onSetStorePrice: (itemId: Long, storeId: Long, priceCents: Long?) -> Unit,
    onSelectStore: (Long?) -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onConsumeMessage: () -> Unit,
    onMarkAll: (Boolean) -> Unit,
    onRenameList: (String) -> Unit,
    onDeleteList: () -> Unit,
    onSetTheme: (ListTheme) -> Unit,
    onConsumeScroll: () -> Unit,
    onSetStoreFilter: (Long?) -> Unit = {},
    onSetTagFilter: (String?) -> Unit = {},
    onUndoBulkChange: () -> Unit = {},
    onConsumeBulkChange: () -> Unit = {},
    onMoveItem: (ShoppingItem, Long) -> Unit = { _, _ -> },
    onCopyItem: (ShoppingItem) -> Unit = {},
    onDeleteItem: (ShoppingItem) -> Unit = {},
    onConsumeEditRequest: () -> Unit = {},
    onSetThemeForAllLists: (ListTheme) -> Unit = {},
    onScannedBarcode: (String) -> Unit = {},
    onNameUnknownBarcode: (String) -> Unit = {},
    onDismissUnknownBarcode: () -> Unit = {},
    onConsumeAddedFromBarcode: () -> Unit = {},
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // rememberSaveable: open dialogs survive rotation / activity recreation.
    var showNewListDialog by rememberSaveable { mutableStateOf(false) }
    var showStoresDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    // The item being edited, by relation-row id (saveable, unlike the item itself).
    var editingContainsId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingItem = editingContainsId?.let { id -> state.items.firstOrNull { it.containsId == id } }
    val snackbarHostState = remember { SnackbarHostState() }
    // Text of the add field. With the field in the top bar it also searches the list.
    var addText by rememberSaveable { mutableStateOf("") }
    val searchQuery = if (state.addBarOnTop) addText.trim() else ""
    val pickItemsSorted = remember(state.pickItems, searchQuery) {
        state.pickItems.filter { it.matches(searchQuery) } // sorted by the repository
    }
    val shownItems = remember(state.visibleItems, searchQuery) {
        state.visibleItems.filter { it.matches(searchQuery) }
    }
    val submitAdd = {
        if (addText.isNotBlank()) {
            onAddItem(addText)
            addText = ""
        }
    }

    val theme = state.theme
    val rowDetails = RowDetails(
        state.showQuantity, state.showUnits, state.showTags, state.showPriority, state.compact
    )
    val sendTitle = stringResource(R.string.send)
    val markedFormat = stringResource(R.string.undoable_marked_item)
    val unmarkedFormat = stringResource(R.string.undoable_unmarked_item)
    val undoLabel = stringResource(R.string.undo)
    val fontFamily: FontFamily? = remember(theme) {
        theme.fontAsset?.let { FontFamily(Font(it, context.assets)) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(onExport) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImport) }

    // Barcodes are scanned by a scanner app (ZXing intent, e.g. Binary Eye).
    var showNoScanner by rememberSaveable { mutableStateOf(false) }
    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.getStringExtra("SCAN_RESULT")?.takeIf { it.isNotBlank() }?.let(onScannedBarcode)
    }
    val scanBarcode = {
        try {
            scanLauncher.launch(
                Intent("com.google.zxing.client.android.SCAN").putExtra("SCAN_MODE", "PRODUCT_MODE")
            )
        } catch (e: android.content.ActivityNotFoundException) {
            showNoScanner = true
        }
    }
    val addedFormat = stringResource(R.string.undoable_added_item)
    LaunchedEffect(state.addedFromBarcode) {
        val name = state.addedFromBarcode ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, String.format(addedFormat, name), android.widget.Toast.LENGTH_SHORT).show()
        onConsumeAddedFromBarcode()
    }

    // Undo for mark all / unmark all / clean up.
    val resources = context.resources
    LaunchedEffect(state.bulkChange) {
        val change = state.bulkChange ?: return@LaunchedEffect
        onConsumeBulkChange()
        val count = when (change) {
            BulkChange.CLEANED_UP -> R.plurals.undoable_clean_list
            BulkChange.MARKED_ALL -> R.plurals.undoable_mark_all
            BulkChange.UNMARKED_ALL -> R.plurals.undoable_unmark_all
        }
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            message = resources.getQuantityString(count, state.bulkChangeCount, state.bulkChangeCount),
            actionLabel = undoLabel,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) onUndoBulkChange()
    }

    // A copied item opens in the editor.
    LaunchedEffect(state.editRequest, state.items) {
        val request = state.editRequest ?: return@LaunchedEffect
        if (state.items.any { it.containsId == request }) {
            editingContainsId = request
            onConsumeEditRequest()
        }
    }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            val text = when (it) {
                UserMessage.EXPORTED -> R.string.export_finished
                UserMessage.EXPORT_FAILED -> R.string.error_writing_file
                UserMessage.IMPORTED -> R.string.import_finished
                UserMessage.IMPORT_FAILED -> R.string.error_reading_file
            }
            android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
            onConsumeMessage()
        }
    }

    // After an add, scroll the list to where the new item landed (sort decides position).
    LaunchedEffect(state.scrollToContainsId) {
        val target = state.scrollToContainsId ?: return@LaunchedEffect
        val shown = if (state.mode == ListMode.PICK_ITEMS) pickItemsSorted else shownItems
        val idx = shown.indexOfFirst { it.containsId == target }
        if (idx >= 0) listState.animateScrollToItem(idx)
        onConsumeScroll()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ListDrawerContent(
                lists = state.lists,
                currentListId = state.currentListId,
                mode = state.mode,
                onSetMode = onSetMode,
                onSelectList = { id ->
                    onSelectList(id)
                    scope.launch { drawerState.close() }
                },
                onNewList = { showNewListDialog = true },
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        if (state.addBarOnTop) {
                            TopBarAddField(
                                text = addText,
                                onTextChange = { addText = it },
                                placeholder = state.currentListName.ifEmpty { stringResource(R.string.app_name) },
                                capitalization = state.capitalization,
                                onSubmit = submitAdd,
                                onScan = scanBarcode,
                            )
                        } else Column {
                            Text(
                                state.currentListName.ifEmpty { stringResource(R.string.app_name) },
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                stringResource(
                                    if (state.mode == ListMode.PICK_ITEMS) R.string.menu_pick_items
                                    else R.string.menu_start_shopping
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.compose_open_lists))
                        }
                    },
                    actions = {
                        ListOptionsMenu(
                            hideChecked = state.hideChecked,
                            onSort = { showSortDialog = true },
                            onToggleHideChecked = onToggleHideChecked,
                            onCleanup = onCleanup,
                            onMarkAll = onMarkAll,
                            onRenameList = { showRenameDialog = true },
                            onDeleteList = { showDeleteConfirm = true },
                            onSendList = {
                                shareList(context, state.currentListName, state.items, sendTitle)
                            },
                            onTheme = { showThemeDialog = true },
                            onManageStores = { showStoresDialog = true },
                            onImportCsv = {
                                importLauncher.launch(
                                    arrayOf("text/csv", "text/comma-separated-values", "text/plain")
                                )
                            },
                            onExportCsv = { exportLauncher.launch("shoppinglist.csv") },
                            onConvertCsv = {
                                context.startActivity(
                                    Intent(context, org.openintents.convertcsv.shoppinglist.ConvertCsvActivity::class.java)
                                        .setData(
                                            android.net.Uri.withAppendedPath(
                                                org.openintents.shopping.library.provider.ShoppingContract.Lists.CONTENT_URI,
                                                state.currentListId.toString()
                                            )
                                        )
                                )
                            },
                            onAbout = {
                                context.startActivity(
                                    Intent(context, org.openintents.distribution.about.About::class.java)
                                )
                            },
                        )
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    // The Scaffold padding already covers the navigation bar; don't add it twice.
                    .consumeWindowInsets(padding)
                    .imePadding() // lift the bottom add-bar above the soft keyboard
            ) {
                if (state.filters.isActive || (state.useFilters && (state.stores.isNotEmpty() || state.tags.isNotEmpty()))) {
                    ListFilterRow(
                        filters = state.filters,
                        stores = state.stores,
                        tags = state.tags,
                        useFilters = state.useFilters,
                        onSetStoreFilter = onSetStoreFilter,
                        onSetTagFilter = onSetTagFilter,
                    )
                    HorizontalDivider()
                }
                if (state.stores.isNotEmpty()) {
                    StoreFilterRow(
                        stores = state.stores,
                        selectedStoreId = state.selectedStoreId,
                        onSelectStore = onSelectStore,
                    )
                    HorizontalDivider()
                }
                val shownEmpty = if (state.mode == ListMode.PICK_ITEMS) pickItemsSorted.isEmpty()
                else shownItems.isEmpty()
                // Only the list itself wears the list theme (like the legacy UI);
                // app bar, totals and add bar keep the app's colors.
                ThemedListArea(theme = theme, modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (shownEmpty && !state.loading) {
                    Text(
                        text = stringResource(R.string.no_items_available),
                        color = Color(theme.checkedTextArgb),
                        fontFamily = fontFamily,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
                // Compact view: rows without the 48dp touch-target padding of the checkbox.
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides
                        if (state.compact) Dp.Unspecified else LocalMinimumInteractiveComponentSize.current
                ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.mode == ListMode.PICK_ITEMS) {
                        items(pickItemsSorted, key = { it.containsId }) { item ->
                            PickItemRow(
                                item = item,
                                theme = theme,
                                fontFamily = fontFamily,
                                fontSize = state.fontSize,
                                compact = state.compact,
                                onToggle = { onPickToggle(item) },
                            )
                            if (theme.showDivider) HorizontalDivider()
                        }
                        return@LazyColumn
                    }
                    items(shownItems, key = { it.containsId }) { item ->
                        ShoppingItemRow(
                            item = item,
                            theme = theme,
                            fontFamily = fontFamily,
                            fontSize = state.fontSize,
                            showPrice = state.showPrice,
                            details = rowDetails,
                            onToggle = {
                                val originalStatus = item.status
                                val wasBought = item.isBought
                                onToggleItem(item)
                                scope.launch {
                                    // Only the latest action is undoable; don't queue stale snackbars.
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = String.format(
                                            if (wasBought) unmarkedFormat else markedFormat, item.name
                                        ),
                                        actionLabel = undoLabel,
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onRestoreStatus(item.containsId, originalStatus)
                                    }
                                }
                            },
                            onEdit = { editingContainsId = item.containsId },
                        )
                        if (theme.showDivider) HorizontalDivider()
                    }
                }
                }
                if (state.fastScroll) {
                    FastScrollbar(
                        listState = listState,
                        itemCount = if (state.mode == ListMode.PICK_ITEMS) pickItemsSorted.size else shownItems.size,
                        color = Color(theme.checkedTextArgb),
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
                }
                if (state.totals.hasAny) {
                    HorizontalDivider()
                    TotalsBar(
                        totals = state.totals,
                        prioritySubtotal = state.prioritySubtotal,
                        priorityThreshold = state.prioritySubtotalThreshold,
                    )
                }
                if (state.addBarOnTop) {
                    // Suggestions for the add/search field in the top bar.
                    SuggestionRow(
                        query = addText,
                        suggestions = state.suggestions,
                        onPick = { onAddItem(it); addText = "" },
                    )
                } else {
                    AddItemRow(
                        text = addText,
                        onTextChange = { addText = it },
                        suggestions = state.suggestions,
                        capitalization = state.capitalization,
                        onAdd = { onAddItem(it); addText = "" },
                        onSubmit = submitAdd,
                        onScan = scanBarcode,
                    )
                }
            }
        }
    }

    if (showNewListDialog) {
        NewListDialog(
            onDismiss = { showNewListDialog = false },
            onConfirm = { name ->
                onCreateList(name)
                showNewListDialog = false
                scope.launch { drawerState.close() }
            }
        )
    }

    if (showRenameDialog) {
        TextEntryDialog(
            title = stringResource(R.string.rename_list),
            label = stringResource(R.string.compose_list_name),
            initial = state.currentListName,
            confirmLabel = stringResource(R.string.compose_rename),
            onDismiss = { showRenameDialog = false },
            onConfirm = { onRenameList(it); showRenameDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(state.currentListName.ifEmpty { stringResource(R.string.delete_list) }) },
            text = { Text(stringResource(R.string.confirm_delete_list)) },
            confirmButton = {
                TextButton(onClick = { onDeleteList(); showDeleteConfirm = false }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showNoScanner) {
        AlertDialog(
            onDismissRequest = { showNoScanner = false },
            text = { Text(stringResource(R.string.compose_no_scanner)) },
            confirmButton = {
                TextButton(onClick = {
                    showNoScanner = false
                    openStore(context, "de.markusfisch.android.binaryeye")
                }) { Text(stringResource(R.string.compose_install)) }
            },
            dismissButton = {
                TextButton(onClick = { showNoScanner = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    state.unknownBarcode?.let { code ->
        TextEntryDialog(
            title = stringResource(R.string.compose_unknown_product, code),
            label = stringResource(R.string.item),
            confirmLabel = stringResource(R.string.add),
            onDismiss = onDismissUnknownBarcode,
            onConfirm = onNameUnknownBarcode,
        )
    }

    if (showSortDialog) {
        ChoiceDialog(
            title = stringResource(R.string.menu_sort_list),
            labels = stringArrayResource(R.array.preference_sortorder_entries).toList(),
            values = stringArrayResource(R.array.preference_sortorder_entryvalues).toList(),
            selected = state.sortOrder.toString(),
            onDismiss = { showSortDialog = false },
            onSelect = { onSetSortOrder(it.toInt()); showSortDialog = false },
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            current = state.theme,
            fontSize = state.fontSize,
            onApplyToAllLists = { onSetThemeForAllLists(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false },
            onSelect = { onSetTheme(it); showThemeDialog = false },
        )
    }

    if (showStoresDialog) {
        ManageStoresDialog(
            stores = state.stores,
            onDismiss = { showStoresDialog = false },
            onAddStore = onAddStore,
            onRemoveStore = onRemoveStore,
        )
    }

    // The item vanished (e.g. removed elsewhere): close its editor.
    if (editingContainsId != null && editingItem == null && !state.loading) {
        LaunchedEffect(editingContainsId) {
            editingContainsId = null
            onEndItemEdit()
        }
    }
    editingItem?.let { item ->
        LaunchedEffect(item.itemId) { onLoadItemEditData(item.itemId) }
        val close = {
            editingContainsId = null
            onEndItemEdit()
        }
        EditItemDialog(
            item = item,
            stores = state.stores,
            storePrices = state.editingStorePrices,
            note = state.editingNote,
            loaded = state.editingLoaded,
            onSetStorePrice = { storeId, cents -> onSetStorePrice(item.itemId, storeId, cents) },
            onDismiss = close,
            onSave = { edit ->
                onUpdateItem(item, edit)
                close()
            },
            onDelete = {
                onRemoveItem(item)
                close()
            },
            otherLists = state.lists.filter { it.id != state.currentListId },
            onMove = { listId -> onMoveItem(item, listId); close() },
            onCopy = { onCopyItem(item); close() },
            onItemStores = {
                // Which stores carry the item, aisle and price per store.
                context.startActivity(
                    Intent(context, org.openintents.shopping.ui.ItemStoresActivity::class.java).setData(
                        org.openintents.shopping.library.provider.ShoppingContract.Lists.CONTENT_URI.buildUpon()
                            .appendPath(state.currentListId.toString())
                            .appendPath(item.itemId.toString()).build()
                    )
                )
            },
            onDeletePermanently = { onDeleteItem(item); close() },
        )
    }
}

@Composable
private fun ListDrawerContent(
    lists: List<ShoppingListInfo>,
    currentListId: Long,
    mode: ListMode,
    onSetMode: (ListMode) -> Unit,
    onSelectList: (Long) -> Unit,
    onNewList: () -> Unit,
) {
    ModalDrawerSheet {
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.menu_start_shopping)) },
            selected = mode == ListMode.SHOPPING,
            onClick = { onSetMode(ListMode.SHOPPING) },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.menu_pick_items)) },
            selected = mode == ListMode.PICK_ITEMS,
            onClick = { onSetMode(ListMode.PICK_ITEMS) },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = stringResource(R.string.compose_lists),
            modifier = Modifier.padding(16.dp),
        )
        lists.forEach { list ->
            NavigationDrawerItem(
                label = { Text(list.name) },
                selected = list.id == currentListId,
                onClick = { onSelectList(list.id) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.new_list)) },
            selected = false,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            onClick = onNewList,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreFilterRow(
    stores: List<StoreInfo>,
    selectedStoreId: Long?,
    onSelectStore: (Long?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedStoreId == null,
            onClick = { onSelectStore(null) },
            label = { Text(stringResource(R.string.compose_all_stores)) },
        )
        stores.forEach { store ->
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = selectedStoreId == store.id,
                onClick = { onSelectStore(store.id) },
                label = { Text(store.name) },
            )
        }
    }
}

/**
 * The store / tag filters of the list (the "use_filters" setting). Active
 * filters are always shown, so items are never hidden without a visible reason.
 */
@Composable
private fun ListFilterRow(
    filters: ListFilters,
    stores: List<StoreInfo>,
    tags: List<String>,
    useFilters: Boolean,
    onSetStoreFilter: (Long?) -> Unit,
    onSetTagFilter: (String?) -> Unit,
) {
    var storeMenu by remember { mutableStateOf(false) }
    var tagMenu by remember { mutableStateOf(false) }
    val unfiltered = stringResource(R.string.unfiltered)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (useFilters || filters.storeId != null) {
            Box {
                FilterChip(
                    selected = filters.storeId != null,
                    onClick = { storeMenu = true },
                    label = {
                        Text(stores.firstOrNull { it.id == filters.storeId }?.name ?: stringResource(R.string.stores))
                    },
                )
                DropdownMenu(expanded = storeMenu, onDismissRequest = { storeMenu = false }) {
                    DropdownMenuItem(text = { Text(unfiltered) }, onClick = { storeMenu = false; onSetStoreFilter(null) })
                    stores.forEach { store ->
                        DropdownMenuItem(
                            text = { Text(store.name) },
                            onClick = { storeMenu = false; onSetStoreFilter(store.id) },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Box {
            FilterChip(
                selected = filters.tag != null,
                onClick = { tagMenu = true },
                label = { Text(filters.tag ?: stringResource(R.string.tags)) },
            )
            DropdownMenu(expanded = tagMenu, onDismissRequest = { tagMenu = false }) {
                DropdownMenuItem(text = { Text(unfiltered) }, onClick = { tagMenu = false; onSetTagFilter(null) })
                tags.forEach { tag ->
                    DropdownMenuItem(text = { Text(tag) }, onClick = { tagMenu = false; onSetTagFilter(tag) })
                }
            }
        }
    }
}

@Composable
private fun ListOptionsMenu(
    hideChecked: Boolean,
    onSort: () -> Unit,
    onToggleHideChecked: () -> Unit,
    onCleanup: () -> Unit,
    onMarkAll: (Boolean) -> Unit,
    onRenameList: () -> Unit,
    onDeleteList: () -> Unit,
    onSendList: () -> Unit,
    onTheme: () -> Unit,
    onManageStores: () -> Unit,
    onImportCsv: () -> Unit,
    onExportCsv: () -> Unit,
    onConvertCsv: () -> Unit = {},
    onAbout: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.compose_more_options))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_sort_list)) },
            onClick = { onSort(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(if (hideChecked) R.string.compose_show_checked_items else R.string.preference_hidechecked_title)) },
            onClick = { onToggleHideChecked(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.clean_up_list)) },
            onClick = { onCleanup(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.mark_all_items)) },
            onClick = { onMarkAll(true); expanded = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.unmark_all_items)) },
            onClick = { onMarkAll(false); expanded = false },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.rename_list)) },
            onClick = { onRenameList(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.delete_list)) },
            onClick = { onDeleteList(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.send)) },
            onClick = { onSendList(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.theme)) },
            onClick = { onTheme(); expanded = false },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_item_stores)) },
            onClick = { onManageStores(); expanded = false },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.compose_import_csv)) },
            onClick = { onImportCsv(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.compose_export_csv)) },
            onClick = { onExportCsv(); expanded = false },
        )
        // The full CSV screen: HandyShopper format, encoding, import policy.
        DropdownMenuItem(
            text = { Text(stringResource(R.string.convert_csv)) },
            onClick = { onConvertCsv(); expanded = false },
        )
        val context = LocalContext.current
        DropdownMenuItem(
            text = { Text(stringResource(R.string.preferences)) },
            onClick = {
                context.startActivity(Intent(context, SettingsActivity::class.java))
                expanded = false
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(org.openintents.distribution.R.string.oi_distribution_about)) },
            onClick = { onAbout(); expanded = false },
        )
    }
}

@Composable
private fun ManageStoresDialog(
    stores: List<StoreInfo>,
    onDismiss: () -> Unit,
    onAddStore: (String) -> Unit,
    onRemoveStore: (StoreInfo) -> Unit,
) {
    var newStore by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stores)) },
        text = {
            Column {
                if (stores.isEmpty()) {
                    Text(stringResource(R.string.no_stores_available))
                }
                stores.forEach { store ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(store.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRemoveStore(store) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.compose_remove_store, store.name))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newStore,
                        onValueChange = { newStore = it },
                        label = { Text(stringResource(R.string.compose_add_store)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            if (newStore.isNotBlank()) {
                                onAddStore(newStore)
                                newStore = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.compose_add_store))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_done)) }
        }
    )
}

/**
 * The list area in the list theme's colors; the Classic theme draws the notepad
 * paper (a 9-patch, whose padding keeps the text inside the paper's margins).
 */
@Composable
private fun ThemedListArea(
    theme: ListTheme,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val paper = remember(theme) {
        if (theme.paperBackground) {
            androidx.core.content.ContextCompat.getDrawable(context, R.drawable.shoppinglist01d)
        } else null
    }
    val paperPadding = remember(paper) {
        android.graphics.Rect().also { paper?.getPadding(it) }
    }
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .background(Color(theme.backgroundArgb))
            .drawBehind {
                paper?.let { d ->
                    d.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                    drawIntoCanvas { d.draw(it.nativeCanvas) }
                }
            }
            .padding(
                with(density) {
                    androidx.compose.foundation.layout.PaddingValues(
                        start = paperPadding.left.toDp(), top = paperPadding.top.toDp(),
                        end = paperPadding.right.toDp(), bottom = paperPadding.bottom.toDp(),
                    )
                }
            )
    ) {
        content()
    }
}

/**
 * Fast scroll ("fastscroll" setting): a thumb at the right edge that shows the
 * position in the list; dragging or tapping the track jumps through the list.
 */
@Composable
private fun FastScrollbar(
    listState: LazyListState,
    itemCount: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val visibleCount = listState.layoutInfo.visibleItemsInfo.size
    if (itemCount == 0 || visibleCount >= itemCount) return
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxHeight().width(28.dp)) {
        val trackPx = constraints.maxHeight.toFloat()
        val thumbPx = maxOf(trackPx * visibleCount / itemCount, with(density) { 40.dp.toPx() })
        val scrollable = (itemCount - visibleCount).coerceAtLeast(1)
        val fraction = (listState.firstVisibleItemIndex.toFloat() / scrollable).coerceIn(0f, 1f)
        val jumpTo = { y: Float ->
            val target = ((y / trackPx) * itemCount).toInt().coerceIn(0, itemCount - 1)
            scope.launch { listState.scrollToItem(target) }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(itemCount, trackPx) {
                    detectVerticalDragGestures(
                        onDragStart = { jumpTo(it.y) },
                        onVerticalDrag = { change, _ -> jumpTo(change.position.y) },
                    )
                }
                .pointerInput(itemCount, trackPx) { detectTapGestures { jumpTo(it.y) } }
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, ((trackPx - thumbPx) * fraction).roundToInt()) }
                .padding(end = 4.dp)
                .width(6.dp)
                .height(with(density) { thumbPx.toDp() })
                .background(color, RoundedCornerShape(3.dp))
        )
    }
}

/** An item's name as the theme shows it (upper-case fonts, "... OK" suffix). */
@Composable
private fun themedName(theme: ListTheme, text: String, checked: Boolean): String {
    val base = if (theme.upperCase) text.uppercase() else text
    return if (checked && theme.checkedSuffix) base + stringResource(R.string.suffix_checked) else base
}

/** Which item details a row shows (the "show..." settings). */
private data class RowDetails(
    val quantity: Boolean = true,
    val units: Boolean = true,
    val tags: Boolean = true,
    val priority: Boolean = true,
    /** Compact view: less padding, so more items fit on the screen. */
    val compact: Boolean = false,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShoppingItemRow(
    item: ShoppingItem,
    theme: ListTheme,
    fontFamily: FontFamily?,
    fontSize: Int,
    showPrice: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    details: RowDetails = RowDetails(),
) {
    val struck = item.isBought && theme.strikethroughChecked
    val decoration = if (struck) TextDecoration.LineThrough else TextDecoration.None
    val color = Color(if (item.isBought) theme.checkedTextArgb else theme.textArgb)
    val textSize = theme.textSizeSp(fontSize).sp
    val editLabel = stringResource(R.string.menu_edit_item)
    // Like the legacy UI: tap marks the item, long-press edits it. Themes without
    // a checkbox show the state through color / strike-through / suffix only.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onToggle,
                onLongClick = onEdit,
                onLongClickLabel = editLabel,
            )
            .padding(horizontal = 8.dp, vertical = if (details.compact) 0.dp else 4.dp)
            .heightIn(min = if (details.compact) 28.dp else 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (theme.showCheckBox) {
            Checkbox(
                checked = item.isBought,
                onCheckedChange = { onToggle() },
                modifier = Modifier.semantics { contentDescription = item.name },
            )
        }
        val label = buildString {
            if (details.quantity && !item.quantity.isNullOrBlank()) append(item.quantity).append(' ')
            if (details.units && !item.units.isNullOrBlank()) append(item.units).append(' ')
            append(item.name)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text = themedName(theme, label, item.isBought),
                color = color,
                fontFamily = fontFamily,
                fontSize = textSize,
                textDecoration = decoration,
            )
            if (details.tags && !item.tags.isNullOrBlank()) {
                Text(
                    text = if (theme.upperCase) item.tags.uppercase() else item.tags,
                    color = Color(theme.priceArgb),
                    fontFamily = fontFamily,
                    fontSize = textSize * 0.6f,
                )
            }
        }
        if (details.priority && !item.priority.isNullOrBlank()) {
            // A small badge, so it is not mistaken for part of the price.
            Text(
                text = item.priority,
                color = Color(theme.priorityArgb),
                fontFamily = fontFamily,
                fontSize = textSize * 0.6f,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .border(1.dp, Color(theme.priorityArgb), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp)
            )
        }
        // Line cost (price * quantity), like the legacy UI and the totals.
        lineCents(item)?.takeIf { showPrice }?.let { cents ->
            Text(
                text = PriceConverter.getStringFromCentPrice(cents),
                color = Color(if (item.isBought) theme.checkedTextArgb else theme.priceArgb),
                fontFamily = fontFamily,
                fontSize = textSize * 0.7f,
                textDecoration = decoration,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun PickItemRow(
    item: ShoppingItem,
    theme: ListTheme,
    fontFamily: FontFamily?,
    fontSize: Int,
    onToggle: () -> Unit,
    compact: Boolean = false,
) {
    // In pick mode the checkbox means "on this list"; off-list items are dimmed.
    val color = Color(if (item.isOnList) theme.textArgb else theme.checkedTextArgb)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = if (compact) 0.dp else 4.dp)
            .heightIn(min = if (compact) 28.dp else 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (theme.showCheckBox) {
            Checkbox(
                checked = item.isOnList,
                onCheckedChange = { onToggle() },
                modifier = Modifier.semantics { contentDescription = item.name },
            )
        }
        Text(
            text = themedName(theme, item.name, checked = false),
            color = color,
            fontFamily = fontFamily,
            fontSize = theme.textSizeSp(fontSize).sp,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
    }
}

/** A single-choice dialog (radio buttons), e.g. the sort order. */
@Composable
private fun ChoiceDialog(
    title: String,
    labels: List<String>,
    values: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                labels.zip(values).forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

private fun ListTheme.labelRes(): Int = when (this) {
    ListTheme.DEFAULT -> R.string.theme_default
    ListTheme.CLASSIC -> R.string.theme_classic
    ListTheme.ANDROID -> R.string.theme_bugdroid
}

/** Theme picker: each option is a small live preview of a list in that theme. */
@Composable
private fun ThemeDialog(
    current: ListTheme,
    fontSize: Int,
    onApplyToAllLists: (ListTheme) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (ListTheme) -> Unit,
) {
    val context = LocalContext.current
    val sample = listOf(
        ShoppingItem(-1, -1, stringResource(R.string.theme_preview_item_1), Status.WANT_TO_BUY, null, null, null, null),
        ShoppingItem(-2, -2, stringResource(R.string.theme_preview_item_2), Status.BOUGHT, null, null, null, null),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ListTheme.entries.forEach { t ->
                    val font = remember(t) { t.fontAsset?.let { FontFamily(Font(it, context.assets)) } }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(t) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = t == current, onClick = { onSelect(t) })
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(stringResource(t.labelRes()))
                            ThemedListArea(theme = t, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                Column {
                                    sample.forEach { item ->
                                        ShoppingItemRow(
                                            item = item, theme = t, fontFamily = font,
                                            fontSize = fontSize.coerceAtMost(1), showPrice = false,
                                            onToggle = { onSelect(t) }, onEdit = { onSelect(t) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_done)) } },
        dismissButton = {
            TextButton(onClick = { onApplyToAllLists(current) }) {
                Text(stringResource(R.string.use_theme_for_all_lists))
            }
        },
    )
}

@Composable
private fun EditItemDialog(
    item: ShoppingItem,
    stores: List<StoreInfo>,
    storePrices: Map<Long, Long?>,
    note: String?,
    /** Note and store prices are loaded; saving earlier would erase them. */
    loaded: Boolean,
    onSetStorePrice: (storeId: Long, priceCents: Long?) -> Unit,
    onDismiss: () -> Unit,
    onSave: (ItemEdit) -> Unit,
    onDelete: () -> Unit,
    otherLists: List<ShoppingListInfo> = emptyList(),
    onMove: (Long) -> Unit = {},
    onCopy: () -> Unit = {},
    onDeletePermanently: () -> Unit = {},
    onItemStores: () -> Unit = {},
) {
    var showMove by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(item.name) }
    var quantity by rememberSaveable { mutableStateOf(item.quantity.orEmpty()) }
    var price by rememberSaveable {
        mutableStateOf(item.priceCents?.let { PriceConverter.getStringFromCentPrice(it) } ?: "")
    }
    var units by rememberSaveable { mutableStateOf(item.units.orEmpty()) }
    var priority by rememberSaveable { mutableStateOf(item.priority.orEmpty()) }
    var tags by rememberSaveable { mutableStateOf(item.tags.orEmpty()) }
    // Note loads asynchronously after the dialog opens; seed when it arrives.
    var noteText by rememberSaveable(note) { mutableStateOf(note.orEmpty()) }
    // Per-store price text, re-seeded when the loaded prices arrive.
    val storePriceText = remember(stores, storePrices) {
        mutableStateMapOf<Long, String>().apply {
            stores.forEach { s ->
                put(s.id, storePrices[s.id]?.let { PriceConverter.getStringFromCentPrice(it) } ?: "")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_edit_item)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.item)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(stringResource(R.string.quantity)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = units,
                    onValueChange = { units = it },
                    label = { Text(stringResource(R.string.units)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text(stringResource(R.string.price)) },
                    singleLine = true,
                    isError = !isValidPrice(price),
                    supportingText = if (isValidPrice(price)) null
                    else ({ Text(stringResource(R.string.compose_invalid_price)) }),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it },
                    label = { Text(stringResource(R.string.priority)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.tags)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.note)) },
                )
                if (stores.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.compose_per_store_prices))
                    stores.forEach { store ->
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = storePriceText[store.id] ?: "",
                            onValueChange = { storePriceText[store.id] = it },
                            label = { Text(store.name) },
                            singleLine = true,
                            isError = !isValidPrice(storePriceText[store.id].orEmpty()),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDelete) { Text(stringResource(R.string.menu_remove_item)) }
                if (otherLists.isNotEmpty()) {
                    TextButton(onClick = { showMove = true }) { Text(stringResource(R.string.menu_move_item)) }
                }
                TextButton(onClick = onCopy) { Text(stringResource(R.string.menu_copy_item)) }
                if (stores.isNotEmpty()) {
                    TextButton(onClick = onItemStores) { Text(stringResource(R.string.menu_item_stores)) }
                }
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.menu_delete_item)) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = loaded,
                onClick = {
                    // Never save a typo'd price: that would silently erase the stored one.
                    val pricesValid = isValidPrice(price) &&
                        stores.all { isValidPrice(storePriceText[it.id].orEmpty()) }
                    if (name.isNotBlank() && pricesValid) {
                        val cents = if (price.isBlank()) null else PriceConverter.getCentPriceFromString(price)
                        onSave(
                            ItemEdit(
                                name = name,
                                quantity = quantity.ifBlank { null },
                                priceCents = cents,
                                units = units.ifBlank { null },
                                priority = priority.ifBlank { null },
                                tags = tags.ifBlank { null },
                                note = noteText.ifBlank { null },
                            )
                        )
                        stores.forEach { store ->
                            val txt = storePriceText[store.id].orEmpty()
                            if (txt.isNotBlank()) {
                                onSetStorePrice(store.id, PriceConverter.getCentPriceFromString(txt))
                            }
                        }
                    }
                }
            ) { Text(stringResource(R.string.compose_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )

    if (showMove) {
        AlertDialog(
            onDismissRequest = { showMove = false },
            title = { Text(stringResource(R.string.menu_move_item)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    otherLists.forEach { list ->
                        Text(
                            list.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMove = false; onMove(list.id) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMove = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.menu_delete_item)) },
            text = { Text(stringResource(R.string.delete_item_confirm, item.name)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeletePermanently() }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

private fun isValidPrice(text: String): Boolean =
    text.isBlank() || PriceConverter.getCentPriceFromString(text) != null

@Composable
private fun TotalsBar(totals: ListTotals, prioritySubtotal: Long, priorityThreshold: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.total, formatTotal(totals.toBuyCents)),
            modifier = Modifier.weight(1f)
        )
        if (prioritySubtotal != 0L && priorityThreshold in 1..4) {
            val caption = when (priorityThreshold) {
                1 -> R.string.priority1_total
                2 -> R.string.priority2_total
                3 -> R.string.priority3_total
                else -> R.string.priority4_total
            }
            Text(
                text = stringResource(caption, formatTotal(prioritySubtotal)),
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        if (totals.boughtCents > 0) {
            Text(text = stringResource(R.string.total_checked, formatTotal(totals.boughtCents)))
        }
    }
}

/** Like PriceConverter but shows "0.00" for a zero total instead of an empty string. */
private fun formatTotal(cents: Long): String =
    if (cents == 0L) "0.00" else PriceConverter.getStringFromCentPrice(cents)

/** Case-insensitive search of the list (empty query matches everything). */
private fun ShoppingItem.matches(query: String): Boolean =
    query.isEmpty() || name.contains(query, ignoreCase = true)

private fun keyboardCapitalization(capitalization: Int) = when (capitalization) {
    0 -> KeyboardCapitalization.None
    2 -> KeyboardCapitalization.Words
    else -> KeyboardCapitalization.Sentences
}

/** Catalogue names matching what's typed (case-insensitive); prefix matches first. */
@Composable
private fun SuggestionRow(query: String, suggestions: List<String>, onPick: (String) -> Unit) {
    val matches = remember(query, suggestions) {
        val q = query.trim()
        if (q.isBlank()) emptyList()
        else suggestions.asSequence()
            .filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }
            .sortedByDescending { it.startsWith(q, ignoreCase = true) }
            .take(8)
            .toList()
    }
    if (matches.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        matches.forEach { name ->
            // Tapping a suggestion adds it straight away (fast re-add).
            SuggestionChip(onClick = { onPick(name) }, label = { Text(name) })
            Spacer(Modifier.width(8.dp))
        }
    }
}

/** The add field at the bottom of the screen. */
@Composable
private fun AddItemRow(
    text: String,
    onTextChange: (String) -> Unit,
    suggestions: List<String>,
    capitalization: Int,
    onAdd: (String) -> Unit,
    onSubmit: () -> Unit,
    onScan: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SuggestionRow(query = text, suggestions = suggestions, onPick = onAdd)
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.new_item)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = keyboardCapitalization(capitalization),
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.weight(1f)
            )
            if (text.isEmpty()) {
                IconButton(onClick = onScan) {
                    Icon(
                        painterResource(R.drawable.ic_barcode),
                        contentDescription = stringResource(R.string.compose_scan_barcode),
                    )
                }
            }
            IconButton(onClick = onSubmit) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add))
            }
        }
    }
}

/**
 * The search/add field in the top bar ("holosearch" setting): typing filters
 * the list, Enter or + adds the text as a new item.
 */
@Composable
private fun TopBarAddField(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    capitalization: Int,
    onSubmit: () -> Unit,
    onScan: () -> Unit,
) {
    TextField(
        value = text,
        onValueChange = onTextChange,
        placeholder = { Text(placeholder, maxLines = 1) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (text.isEmpty()) {
                IconButton(onClick = onScan) {
                    Icon(
                        painterResource(R.drawable.ic_barcode),
                        contentDescription = stringResource(R.string.compose_scan_barcode),
                    )
                }
            } else {
                Row {
                    IconButton(onClick = { onTextChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.compose_clear))
                    }
                    IconButton(onClick = onSubmit) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add))
                    }
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = keyboardCapitalization(capitalization),
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = placeholder
        },
    )
}

@Composable
private fun TextEntryDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Opens an app's page in the installed app store (F-Droid, Play...), else on F-Droid's website. */
private fun openStore(context: android.content.Context, packageName: String) {
    val market = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName"))
    try {
        context.startActivity(market)
    } catch (e: android.content.ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://f-droid.org/packages/$packageName/"))
        )
    }
}

private fun shareList(
    context: android.content.Context,
    listName: String,
    items: List<ShoppingItem>,
    chooserTitle: String,
) {
    val body = buildString {
        append(listName).append('\n')
        items.forEach { item ->
            append(if (item.isBought) "[x] " else "[ ] ")
            if (!item.quantity.isNullOrBlank()) append(item.quantity).append(' ')
            append(item.name).append('\n')
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, listName)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

@Composable
private fun NewListDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_list)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.compose_list_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
            ) { Text(stringResource(R.string.compose_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
