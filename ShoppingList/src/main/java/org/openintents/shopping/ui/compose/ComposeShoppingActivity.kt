package org.openintents.shopping.ui.compose

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.view.WindowManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.openintents.intents.ShoppingListIntents
import org.openintents.shopping.data.ListMode
import org.openintents.shopping.data.NewItem
import org.openintents.util.ShakeSensorListener
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.widgets.CheckItemsWidget
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

    private val shakeListener = object : ShakeSensorListener() {
        override fun onShake() {
            // "Shake to clean up" (only while shopping, like the legacy UI).
            if (viewModel.state.value.mode == ListMode.SHOPPING) viewModel.cleanup()
        }
    }

    override fun onResume() {
        super.onResume()
        // Settings that act on the window (they can change in Settings).
        @Suppress("DEPRECATION")
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
        requestedOrientation = prefs.getString("orientation", "-1")?.toIntOrNull()
            ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (prefs.getBoolean("screenlock", false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        if (prefs.getBoolean("shake", false)) {
            val sensors = getSystemService(SENSOR_SERVICE) as SensorManager
            sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sensors.registerListener(shakeListener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onPause() {
        (getSystemService(SENSOR_SERVICE) as SensorManager).unregisterListener(shakeListener)
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        updateWidgets()
    }

    private fun showListFrom(intent: Intent?) {
        val listId = listIdFrom(intent?.data)
        val items = itemsFrom(intent)
        if (items.isNotEmpty()) {
            // Shared text / INSERT_FROM_EXTRAS (forwarded by ShoppingListsActivity).
            viewModel.addItemsFromIntent(listId, items)
            // Don't add them again if the same intent is delivered once more.
            intent?.removeExtra(ShoppingListIntents.EXTRA_STRING_ARRAYLIST_SHOPPING)
        } else {
            listId?.let(viewModel::showList)
        }
    }

    /** Home-screen widgets show list items; refresh them when leaving the app. */
    private fun updateWidgets() {
        val context = applicationContext
        Thread {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CheckItemsWidget::class.java))
            if (ids.isNotEmpty()) CheckItemsWidget().onUpdate(context, manager, ids)
        }.start()
    }

    companion object {
        /** Items sent as string array lists in the extras (see ShoppingListIntents). */
        fun itemsFrom(intent: Intent?): List<NewItem> {
            val extras = intent?.extras ?: return emptyList()
            val names = extras.getStringArrayList(ShoppingListIntents.EXTRA_STRING_ARRAYLIST_SHOPPING)
                ?: return emptyList()
            val quantities = extras.getStringArrayList(ShoppingListIntents.EXTRA_STRING_ARRAYLIST_QUANTITY)
            val prices = extras.getStringArrayList(ShoppingListIntents.EXTRA_STRING_ARRAYLIST_PRICE)
            val barcodes = extras.getStringArrayList(ShoppingListIntents.EXTRA_STRING_ARRAYLIST_BARCODE)
            return names.mapIndexedNotNull { i, name ->
                if (name.isNullOrBlank()) null
                else NewItem(name, quantities?.getOrNull(i), prices?.getOrNull(i), barcodes?.getOrNull(i))
            }
        }

        /** The list id of a content://org.openintents.shopping/lists/N URI, else null. */
        fun listIdFrom(uri: Uri?): Long? {
            if (uri == null || uri.authority != ShoppingContract.AUTHORITY) return null
            val segments = uri.pathSegments
            if (segments.size != 2 || segments[0] != "lists") return null
            return segments[1].toLongOrNull()
        }
    }
}
