package org.openintents.shopping.ui.compose.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openintents.shopping.ui.compose.theme.OiShoppingTheme

/** Host for the Compose settings screen. */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OiShoppingTheme {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
                SettingsRoute(vm, onBack = { finish() })
            }
        }
    }
}
