package org.openintents.shopping.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openintents.shopping.data.ShoppingItem

/**
 * The shopping-list screen, driven entirely by [ShoppingListViewModel] state.
 * This is the first Compose screen; it will grow to replace the legacy
 * View-based ShoppingActivity incrementally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var newItem by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Shopping list") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(state.items, key = { it.containsId }) { item ->
                    ShoppingItemRow(item = item, onToggle = { viewModel.toggle(item) })
                    HorizontalDivider()
                }
            }
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
                            viewModel.addItem(newItem)
                            newItem = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(item: ShoppingItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.isBought, onCheckedChange = { onToggle() })
        Text(
            text = item.name,
            textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
