package org.openintents.shopping;

import android.app.Activity;
import android.content.Context;

import org.openintents.shopping.sync.NoSyncSupport;
import org.openintents.shopping.ui.ToggleBoughtInputMethod;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

/**
 * This is the default implementation for all product flavors for any implementation
 * <p>
 * If the signature is changed make sure that the corresponding implementations are changed
 * because Android Studio does only show usage for the current build flavor.
 */
public class BaseOptionalDependencies {

    public void onResumeShoppingActivity(Activity context) {
        // do nothing;
    }

    public ToggleBoughtInputMethod getToggleBoughtInputMethod(Context context, ShoppingItemsView itemsView) {
        return null;
    }

    public SyncSupport getSyncSupport(final Context context) {
        return new NoSyncSupport();
    }
}
