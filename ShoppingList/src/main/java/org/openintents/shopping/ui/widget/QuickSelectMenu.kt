package org.openintents.shopping.ui.widget

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.View

/* This class exposes a subset of PopupMenu functionality, and chooses whether
 * to use the platform PopupMenu (on Honeycomb or above) or a backported version.
 */
class QuickSelectMenu(context: Context, anchor: View) {

    private val mImplPlatform: androidx.appcompat.widget.PopupMenu =
        androidx.appcompat.widget.PopupMenu(context, anchor)

    private var mItemSelectedListener: OnItemSelectedListener? = null

    init {
        mImplPlatform.setOnMenuItemClickListener { item ->
            onMenuItemClickImpl(item)
        }
    }

    // not sure if we want to expose this or just an add() method.
    fun getMenu(): Menu = mImplPlatform.menu

    fun setOnItemSelectedListener(listener: OnItemSelectedListener) {
        mItemSelectedListener = listener
    }

    fun show() {
        mImplPlatform.show()
    }

    fun onMenuItemClickImpl(item: MenuItem): Boolean {
        val name = item.title
        val id = item.itemId
        mItemSelectedListener!!.onItemSelected(name, id)
        return true
    }

    // popup.setOnMenuItemClickListener(new
    // android.widget.PopupMenu.OnMenuItemClickListener() {

    /**
     * Interface responsible for receiving menu item click events if the items
     * themselves do not have individual item click listeners.
     */
    interface OnItemSelectedListener {
        /**
         * This method will be invoked when an item is selected.
         *
         * @param item [CharSequence] that was selected
         * @param id
         */
        fun onItemSelected(item: CharSequence?, id: Int)
    }
}
