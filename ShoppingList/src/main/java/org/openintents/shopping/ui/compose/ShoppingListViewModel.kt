package org.openintents.shopping.ui.compose

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openintents.shopping.data.ShoppingItem
import org.openintents.shopping.data.ShoppingRepository

/** Immutable UI state for the shopping-list screen. */
data class ShoppingUiState(
    val listId: Long = -1L,
    val items: List<ShoppingItem> = emptyList(),
    val loading: Boolean = true,
)

/**
 * Holds the screen state and the actions the UI can take, delegating all data
 * work to [ShoppingRepository] off the main thread. The Compose layer only ever
 * reads [state] and calls these methods — it never touches the provider.
 */
class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShoppingRepository(application)

    private val _state = MutableStateFlow(ShoppingUiState())
    val state: StateFlow<ShoppingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val listId = withContext(Dispatchers.IO) { repository.getDefaultListId() }
            _state.value = _state.value.copy(listId = listId)
            refresh()
        }
    }

    fun refresh() = viewModelScope.launch {
        val listId = _state.value.listId
        val items = withContext(Dispatchers.IO) { repository.getItems(listId) }
        _state.value = _state.value.copy(items = items, loading = false)
    }

    fun addItem(name: String) = viewModelScope.launch {
        val listId = _state.value.listId
        withContext(Dispatchers.IO) { repository.addItem(listId, name) }
        refresh()
    }

    fun toggle(item: ShoppingItem) = viewModelScope.launch {
        withContext(Dispatchers.IO) { repository.toggleItemBought(item) }
        refresh()
    }
}
