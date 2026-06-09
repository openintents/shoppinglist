package org.openintents.shopping.ui.compose.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel

/** Host for the Compose settings screen. */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
                SettingsRoute(vm, onBack = { finish() })
            }
        }
    }
}
