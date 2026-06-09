package org.openintents.shopping.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Host for the new Compose UI. Kept separate from the legacy ShoppingActivity so
 * the app keeps working while screens are migrated to Compose one at a time.
 */
class ComposeShoppingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: ShoppingListViewModel = viewModel()
                ShoppingListScreen(vm)
            }
        }
    }
}
