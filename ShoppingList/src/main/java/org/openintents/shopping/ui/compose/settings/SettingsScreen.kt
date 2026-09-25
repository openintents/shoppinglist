package org.openintents.shopping.ui.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openintents.shopping.R

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        bools = state.bools,
        choices = state.choices,
        onSetBool = viewModel::setBool,
        onSetChoice = viewModel::setChoice,
        onResetAll = viewModel::resetAll,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bools: Map<String, Boolean>,
    choices: Map<String, String>,
    onSetBool: (String, Boolean) -> Unit,
    onSetChoice: (String, String) -> Unit,
    onBack: () -> Unit,
    onResetAll: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preferences)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.compose_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            AppSettingsCatalog.sections.forEach { section ->
                Text(
                    stringResource(section.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                )
                section.settings.forEach { setting ->
                    when (setting) {
                        is ChoiceSetting -> ChoiceRow(
                            title = stringResource(setting.titleRes),
                            entries = stringArrayResource(setting.entriesRes),
                            values = stringArrayResource(setting.valuesRes),
                            selectedValue = choices[setting.key] ?: setting.default,
                            onSelect = { onSetChoice(setting.key, it) },
                        )
                        is BoolSetting -> SwitchRow(
                            title = stringResource(setting.titleRes),
                            checked = bools[setting.key] ?: setting.default,
                            onCheckedChange = { onSetBool(setting.key, it) },
                        )
                    }
                    HorizontalDivider()
                }
            }
            var confirmReset by remember { mutableStateOf(false) }
            TextButton(
                onClick = { confirmReset = true },
                modifier = Modifier.padding(16.dp),
            ) { Text(stringResource(R.string.preference_reset_all_settings)) }
            if (confirmReset) {
                val context = LocalContext.current
                AlertDialog(
                    onDismissRequest = { confirmReset = false },
                    title = { Text(stringResource(R.string.preference_reset_all_settings)) },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmReset = false
                            onResetAll()
                            android.widget.Toast.makeText(
                                context, R.string.preference_reset_all_settings_done, android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }) { Text(stringResource(R.string.ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) }
                    },
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    entries: Array<String>,
    values: Array<String>,
    selectedValue: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = values.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
    val selectedLabel = entries.getOrNull(selectedIndex) ?: ""

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(selectedLabel, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.compose_change)) }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        values.getOrNull(index)?.let(onSelect)
                        expanded = false
                    }
                )
            }
        }
    }
}
