package org.openintents.shopping.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.openintents.shopping.data.ShoppingItem
import org.openintents.shopping.data.ShoppingListInfo
import org.openintents.shopping.library.util.PriceConverter

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
        onUpdateItem = viewModel::updateItem,
        onRemoveItem = viewModel::removeItem,
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
    onUpdateItem: (ShoppingItem, String, String?, Long?) -> Unit,
    onRemoveItem: (ShoppingItem) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showNewListDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }

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
            topBar = {
                TopAppBar(
                    title = { Text(state.currentListName.ifEmpty { "Shopping list" }) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open lists")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(state.items, key = { it.containsId }) { item ->
                        ShoppingItemRow(
                            item = item,
                            onToggle = { onToggleItem(item) },
                            onClick = { editingItem = item },
                        )
                        HorizontalDivider()
                    }
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

    editingItem?.let { item ->
        EditItemDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { name, quantity, priceCents ->
                onUpdateItem(item, name, quantity, priceCents)
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

@Composable
private fun ShoppingItemRow(item: ShoppingItem, onToggle: () -> Unit, onClick: () -> Unit) {
    val decoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None
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
            textDecoration = decoration,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
        item.priceCents?.let { cents ->
            Text(
                text = PriceConverter.getStringFromCentPrice(cents),
                textDecoration = decoration,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun EditItemDialog(
    item: ShoppingItem,
    onDismiss: () -> Unit,
    onSave: (name: String, quantity: String?, priceCents: Long?) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(item.name) }
    var quantity by remember { mutableStateOf(item.quantity.orEmpty()) }
    var price by remember {
        mutableStateOf(item.priceCents?.let { PriceConverter.getStringFromCentPrice(it) } ?: "")
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
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDelete) { Text("Remove from list") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val cents = if (price.isBlank()) null else PriceConverter.getCentPriceFromString(price)
                        onSave(name, quantity.ifBlank { null }, cents)
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
