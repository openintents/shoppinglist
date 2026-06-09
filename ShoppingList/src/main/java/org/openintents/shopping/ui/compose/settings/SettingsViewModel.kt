package org.openintents.shopping.ui.compose.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openintents.shopping.data.SettingsRepository
import org.openintents.shopping.data.SharedPrefsSettingsRepository

data class SettingsUiState(
    val bools: Map<String, Boolean> = emptyMap(),
    val choices: Map<String, String> = emptyMap(),
)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun setBool(key: String, value: Boolean) {
        repository.setBoolean(key, value)
        load()
    }

    fun setChoice(key: String, value: String) {
        repository.setString(key, value)
        load()
    }

    private fun load() {
        _state.value = SettingsUiState(
            bools = AppSettingsCatalog.toggles.associate { it.key to repository.getBoolean(it.key, it.default) },
            choices = AppSettingsCatalog.choices.associate { it.key to repository.getString(it.key, it.default) },
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                SettingsViewModel(SharedPrefsSettingsRepository(app))
            }
        }
    }
}
