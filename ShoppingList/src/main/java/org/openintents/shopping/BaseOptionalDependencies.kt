package org.openintents.shopping

import android.app.Activity
import android.content.Context
import org.openintents.shopping.sync.NoSyncSupport
import org.openintents.shopping.ui.ToggleBoughtInputMethod
import org.openintents.shopping.ui.widget.ShoppingItemsView

/**
 * This is the default implementation for all product flavors for any implementation
 *
 * If the signature is changed make sure that the corresponding implementations are changed
 * because Android Studio does only show usage for the current build flavor.
 */
open class BaseOptionalDependencies {

    open fun onResumeShoppingActivity(context: Activity) {
        // do nothing
    }

    open fun getToggleBoughtInputMethod(context: Context, itemsView: ShoppingItemsView): ToggleBoughtInputMethod? {
        return null
    }

    open fun getSyncSupport(context: Context): SyncSupport {
        return NoSyncSupport()
    }
}
