package org.openintents.shopping;

import android.content.Context;

import org.openintents.shopping.ui.MyoToggleBoughtInputMethod;
import org.openintents.shopping.ui.ToggleBoughtInputMethod;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

public class OptionalDependencies extends BaseOptionalDependencies {
    @Override
    public ToggleBoughtInputMethod getToggleBoughtInputMethod(Context context, ShoppingItemsView itemsView) {
        return new MyoToggleBoughtInputMethod(context, itemsView);
    }
}
