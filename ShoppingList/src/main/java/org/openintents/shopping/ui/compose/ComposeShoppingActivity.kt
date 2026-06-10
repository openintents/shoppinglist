package org.openintents.shopping.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openintents.shopping.ui.compose.theme.OiShoppingTheme

/**
 * Host for the new Compose UI. Kept separate from the legacy ShoppingActivity so
 * the app keeps working while screens are migrated to Compose one at a time.
 */
class ComposeShoppingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OiShoppingTheme {
                val vm: ShoppingListViewModel = viewModel(factory = ShoppingListViewModel.Factory)
                ShoppingListRoute(vm)
            }
        }
    }
}
