package org.openintents.shopping.ui.compose

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.openintents.shopping.data.ItemEdit
import org.openintents.shopping.data.ListTheme
import org.openintents.shopping.data.ListTotals
import org.openintents.shopping.data.ShoppingItem
import org.openintents.shopping.data.ShoppingListInfo
import org.openintents.shopping.data.SortMode
import org.openintents.shopping.data.StoreInfo
import org.openintents.shopping.library.util.PriceConverter
import org.openintents.shopping.ui.compose.settings.SettingsActivity

/**
 * Stateful entry point: reads [ShoppingListViewModel] state and forwards events.
 * Kept thin so the stateless [ShoppingListScreen] below is previewable/testable.
 */
@Composable
fun ShoppingListRoute(viewModel: ShoppingListViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShoppingListScreen(
        state = state,
        onSelectList = viewModel::selectList,
        onCreateList = viewModel::createList,
        onAddItem = viewModel::addItem,
        onToggleItem = viewModel::toggle,
        onRestoreStatus = viewModel::restoreStatus,
        onUpdateItem = viewModel::updateItem,
        onRemoveItem = viewModel::removeItem,
        onSetSortMode = viewModel::setSortMode,
        onToggleHideChecked = viewModel::toggleHideChecked,
        onCleanup = viewModel::cleanup,
        onAddStore = viewModel::addStore,
        onRemoveStore = viewModel::removeStore,
        onLoadItemEditData = viewModel::loadItemEditData,
        onSetStorePrice = viewModel::setStorePrice,
        onSelectStore = viewModel::selectStore,
        onExport = viewModel::exportTo,
        onImport = viewModel::importFrom,
        onConsumeMessage = viewModel::consumeMessage,
        onMarkAll = viewModel::markAll,
        onRenameList = viewModel::renameCurrentList,
        onDeleteList = viewModel::deleteCurrentList,
        onSetTheme = viewModel::setTheme,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    state: ShoppingUiState,
    onSelectList: (Long) -> Unit,
    onCreateList: (String) -> Unit,
    onAddItem: (String) -> Unit,
    onToggleItem: (ShoppingItem) -> Unit,
    onRestoreStatus: (containsId: Long, status: Long) -> Unit,
    onUpdateItem: (ShoppingItem, ItemEdit) -> Unit,
    onRemoveItem: (ShoppingItem) -> Unit,
    onSetSortMode: (SortMode) -> Unit,
    onToggleHideChecked: () -> Unit,
    onCleanup: () -> Unit,
    onAddStore: (String) -> Unit,
    onRemoveStore: (StoreInfo) -> Unit,
    onLoadItemEditData: (Long) -> Unit,
    onSetStorePrice: (itemId: Long, storeId: Long, priceCents: Long?) -> Unit,
    onSelectStore: (Long?) -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onConsumeMessage: () -> Unit,
    onMarkAll: (Boolean) -> Unit,
    onRenameList: (String) -> Unit,
    onDeleteList: () -> Unit,
    onSetTheme: (ListTheme) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showNewListDialog by remember { mutableStateOf(false) }
    var showStoresDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val theme = state.theme
    val fontFamily: FontFamily? = remember(theme) {
        theme.fontAsset?.let { FontFamily(Font(it, context.assets)) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(onExport) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImport) }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            onConsumeMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ListDrawerContent(
                lists = state.lists,
                currentListId = state.currentListId,
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
                    title = { Text(state.currentListName.ifEmpty { "Shopping list" }) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open lists")
                        }
                    },
                    actions = {
                        ListOptionsMenu(
                            sortMode = state.sortMode,
                            hideChecked = state.hideChecked,
                            onSetSortMode = onSetSortMode,
                            onToggleHideChecked = onToggleHideChecked,
                            onCleanup = onCleanup,
                            onMarkAll = onMarkAll,
                            onRenameList = { showRenameDialog = true },
                            onDeleteList = { showDeleteConfirm = true },
                            onSendList = { shareList(context, state.currentListName, state.items) },
                            onTheme = { showThemeDialog = true },
                            onManageStores = { showStoresDialog = true },
                            onImportCsv = {
                                importLauncher.launch(
                                    arrayOf("text/csv", "text/comma-separated-values", "text/plain")
                                )
                            },
                            onExportCsv = { exportLauncher.launch("shoppinglist.csv") },
                        )
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(theme.backgroundArgb))
            ) {
                if (state.stores.isNotEmpty()) {
                    StoreFilterRow(
                        stores = state.stores,
                        selectedStoreId = state.selectedStoreId,
                        onSelectStore = onSelectStore,
                    )
                    HorizontalDivider()
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(state.visibleItems, key = { it.containsId }) { item ->
                        ShoppingItemRow(
                            item = item,
                            theme = theme,
                            fontFamily = fontFamily,
                            onToggle = {
                                val originalStatus = item.status
                                val wasBought = item.isBought
                                onToggleItem(item)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = if (wasBought) "Unmarked ${item.name}" else "Marked ${item.name}",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onRestoreStatus(item.containsId, originalStatus)
                                    }
                                }
                            },
                            onClick = { editingItem = item },
                        )
                        HorizontalDivider()
                    }
                }
                if (state.totals.hasAny) {
                    HorizontalDivider()
                    TotalsBar(totals = state.totals)
                }
                AddItemRow(onAdd = onAddItem)
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
            title = "Rename list",
            label = "List name",
            initial = state.currentListName,
            confirmLabel = "Rename",
            onDismiss = { showRenameDialog = false },
            onConfirm = { onRenameList(it); showRenameDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete list") },
            text = { Text("Delete \"${state.currentListName}\" and its items?") },
            confirmButton = {
                TextButton(onClick = { onDeleteList(); showDeleteConfirm = false }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            current = state.theme,
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

    editingItem?.let { item ->
        LaunchedEffect(item.itemId) { onLoadItemEditData(item.itemId) }
        EditItemDialog(
            item = item,
            stores = state.stores,
            storePrices = state.editingStorePrices,
            note = state.editingNote,
            onSetStorePrice = { storeId, cents -> onSetStorePrice(item.itemId, storeId, cents) },
            onDismiss = { editingItem = null },
            onSave = { edit ->
                onUpdateItem(item, edit)
                editingItem = null
            },
            onDelete = {
                onRemoveItem(item)
                editingItem = null
            },
        )
    }
}

@Composable
private fun ListDrawerContent(
    lists: List<ShoppingListInfo>,
    currentListId: Long,
    onSelectList: (Long) -> Unit,
    onNewList: () -> Unit,
) {
    ModalDrawerSheet {
        Text(
            text = "Lists",
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
            label = { Text("New list…") },
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
            label = { Text("All") },
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

@Composable
private fun ListOptionsMenu(
    sortMode: SortMode,
    hideChecked: Boolean,
    onSetSortMode: (SortMode) -> Unit,
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
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Sort: unchecked first") },
            leadingIcon = { if (sortMode == SortMode.UNCHECKED_FIRST) Icon(Icons.Filled.Check, null) },
            onClick = { onSetSortMode(SortMode.UNCHECKED_FIRST); expanded = false },
        )
        DropdownMenuItem(
            text = { Text("Sort: alphabetical") },
            leadingIcon = { if (sortMode == SortMode.ALPHABETICAL) Icon(Icons.Filled.Check, null) },
            onClick = { onSetSortMode(SortMode.ALPHABETICAL); expanded = false },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(if (hideChecked) "Show checked items" else "Hide checked items") },
            onClick = { onToggleHideChecked(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text("Clean up (remove checked)") },
            onClick = { onCleanup(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text("Mark all items") },
            onClick = { onMarkAll(true); expanded = false },
        )
        DropdownMenuItem(
            text = { Text("Unmark all items") },
            onClick = { onMarkAll(false); expanded = false },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Rename list") },
            onClick = { onRenameList(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text("Delete list") },
            onClick = { onDeleteList(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text("Send list") },
            onClick = { onSendList(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text("Theme") },
            onClick = { onTheme(); expanded = false },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Stores…") },
            onClick = { onManageStores(); expanded = false },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Import CSV…") },
            onClick = { onImportCsv(); expanded = false },
        )
        DropdownMenuItem(
            text = { Text("Export CSV…") },
            onClick = { onExportCsv(); expanded = false },
        )
        val context = LocalContext.current
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = {
                context.startActivity(Intent(context, SettingsActivity::class.java))
                expanded = false
            },
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
        title = { Text("Stores") },
        text = {
            Column {
                if (stores.isEmpty()) {
                    Text("No stores yet.")
                }
                stores.forEach { store ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(store.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRemoveStore(store) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove ${store.name}")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newStore,
                        onValueChange = { newStore = it },
                        label = { Text("Add store") },
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
                        Icon(Icons.Filled.Add, contentDescription = "Add store")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItem,
    theme: ListTheme,
    fontFamily: FontFamily?,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val struck = item.isBought && theme.strikethroughChecked
    val decoration = if (struck) TextDecoration.LineThrough else TextDecoration.None
    val color = Color(if (item.isBought) theme.checkedTextArgb else theme.textArgb)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.isBought, onCheckedChange = { onToggle() })
        val label = buildString {
            if (!item.quantity.isNullOrBlank()) append(item.quantity).append("  ")
            append(item.name)
        }
        Text(
            text = label,
            color = color,
            fontFamily = fontFamily,
            textDecoration = decoration,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
        item.priceCents?.let { cents ->
            Text(
                text = PriceConverter.getStringFromCentPrice(cents),
                color = color,
                fontFamily = fontFamily,
                textDecoration = decoration,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun ThemeDialog(current: ListTheme, onDismiss: () -> Unit, onSelect: (ListTheme) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column {
                ListTheme.entries.forEach { t ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(t) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = t == current, onClick = { onSelect(t) })
                        Text(t.displayName, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun EditItemDialog(
    item: ShoppingItem,
    stores: List<StoreInfo>,
    storePrices: Map<Long, Long?>,
    note: String?,
    onSetStorePrice: (storeId: Long, priceCents: Long?) -> Unit,
    onDismiss: () -> Unit,
    onSave: (ItemEdit) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(item.name) }
    var quantity by remember { mutableStateOf(item.quantity.orEmpty()) }
    var price by remember {
        mutableStateOf(item.priceCents?.let { PriceConverter.getStringFromCentPrice(it) } ?: "")
    }
    var units by remember { mutableStateOf(item.units.orEmpty()) }
    var priority by remember { mutableStateOf(item.priority.orEmpty()) }
    var tags by remember { mutableStateOf(item.tags.orEmpty()) }
    // Note loads asynchronously after the dialog opens; seed when it arrives.
    var noteText by remember(note) { mutableStateOf(note.orEmpty()) }
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
        title = { Text("Edit item") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = units,
                    onValueChange = { units = it },
                    label = { Text("Units") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it },
                    label = { Text("Priority") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note") },
                )
                if (stores.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Per-store prices")
                    stores.forEach { store ->
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = storePriceText[store.id] ?: "",
                            onValueChange = { storePriceText[store.id] = it },
                            label = { Text(store.name) },
                            singleLine = true,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDelete) { Text("Remove from list") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
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
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun TotalsBar(totals: ListTotals) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "To buy: ${formatTotal(totals.toBuyCents)}", modifier = Modifier.weight(1f))
        if (totals.boughtCents > 0) {
            Text(text = "Bought: ${formatTotal(totals.boughtCents)}")
        }
    }
}

/** Like PriceConverter but shows "0.00" for a zero total instead of an empty string. */
private fun formatTotal(cents: Long): String =
    if (cents == 0L) "0.00" else PriceConverter.getStringFromCentPrice(cents)

@Composable
private fun AddItemRow(onAdd: (String) -> Unit) {
    var newItem by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = newItem,
            onValueChange = { newItem = it },
            label = { Text("Add item") },
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                if (newItem.isNotBlank()) {
                    onAdd(newItem)
                    newItem = ""
                }
            }
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add")
        }
    }
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
    var text by remember { mutableStateOf(initial) }
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun shareList(context: android.content.Context, listName: String, items: List<ShoppingItem>) {
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
    context.startActivity(Intent.createChooser(intent, "Send list"))
}

@Composable
private fun NewListDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New list") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("List name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
