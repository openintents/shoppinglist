package org.openintents.shopping.ui.widget;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/* This class exposes a subset of PopupMenu functionality, and chooses whether 
 * to use the platform PopupMenu (on Honeycomb or above) or a backported version.
 */
public class QuickSelectMenu {

    android.support.v7.widget.PopupMenu mImplPlatform = null;

    private OnItemSelectedListener mItemSelectedListener;

    public QuickSelectMenu(Context context, View anchor) {
        mImplPlatform = new android.support.v7.widget.PopupMenu(context, anchor);
        mImplPlatform
                .setOnMenuItemClickListener(new android.support.v7.widget.PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return onMenuItemClickImpl(item);
                    }
                });
    }

    // not sure if we want to expose this or just an add() method.
    public Menu getMenu() {
        Menu menu = mImplPlatform.getMenu();
        return menu;
    }

    /**
     * Interface responsible for receiving menu item click events if the items
     * themselves do not have individual item click listeners.
     */
    public interface OnItemSelectedListener {
        /**
         * This method will be invoked when an item is selected.
         *
         * @param item {@link CharSequence} that was selected
         * @param id
         */
        public void onItemSelected(CharSequence item, int id);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mItemSelectedListener = listener;
    }

    public void show() {
        mImplPlatform.show();
    }

    // popup.setOnMenuItemClickListener(new
    // android.widget.PopupMenu.OnMenuItemClickListener() {

    public boolean onMenuItemClickImpl(MenuItem item) {
        CharSequence name = item.getTitle();
        int id = item.getItemId();
        this.mItemSelectedListener.onItemSelected(name, id);
        return true;
    }
}
