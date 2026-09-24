package org.openintents.shopping.ui.compose

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.ui.compose.theme.OiShoppingTheme

/**
 * Host for the Compose UI. Registered in the manifest as
 * org.openintents.shopping.ShoppingActivity (the launcher entry).
 *
 * Opens the list given as intent data (content://org.openintents.shopping/lists/N),
 * as sent by list shortcuts, the widget and other apps.
 */
open class ComposeShoppingActivity : ComponentActivity() {

    private val viewModel: ShoppingListViewModel by viewModels { ShoppingListViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars and resize with the keyboard on every API level;
        // the Scaffold applies the insets.
        // The app is always dark: light system-bar icons regardless of the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) showListFrom(intent)
        setContent {
            OiShoppingTheme {
                ShoppingListRoute(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showListFrom(intent)
    }

    private fun showListFrom(intent: Intent?) {
        listIdFrom(intent?.data)?.let(viewModel::showList)
    }

    companion object {
        /** The list id of a content://org.openintents.shopping/lists/N URI, else null. */
        fun listIdFrom(uri: Uri?): Long? {
            if (uri == null || uri.authority != ShoppingContract.AUTHORITY) return null
            val segments = uri.pathSegments
            if (segments.size != 2 || segments[0] != "lists") return null
            return segments[1].toLongOrNull()
        }
    }
}
