package org.openintents.shopping.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.view.View

import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.ui.widget.ShoppingItemsView

class SnackbarUndoSingleItemStatusOperation(
    private val mShoppingItemsView: ShoppingItemsView,
    private var mContext: Context,
    private var mContainsId: String,
    private var mItemName: String,
    private var mOldStatus: Long,
    private var mNewStatus: Long,
    type: Int,
    batch: Boolean
) : SnackbarUndoOperation(1, type, batch) {

    override fun getDescription(context: Context): String {
        return getSingularDescription(context)
    }

    override fun getSingularDescription(context: Context): String {
        val resId: Int
        if (mShoppingItemsView.inAddItemsMode()) {
            resId = if (mNewStatus == ShoppingContract.Status.WANT_TO_BUY) {
                R.string.undoable_added_item
            } else {
                R.string.undoable_removed_item
            }
        } else {
            resId = if (mNewStatus == ShoppingContract.Status.WANT_TO_BUY) {
                R.string.undoable_unmarked_item
            } else {
                R.string.undoable_marked_item
            }
        }
        return String.format(context.resources.getString(resId), mItemName)
    }

    override fun onClick(view: View) {
        val values = ContentValues()
        values.put(ShoppingContract.Contains.STATUS, mOldStatus)
        mContext.contentResolver.update(
            Uri.withAppendedPath(ShoppingContract.Contains.CONTENT_URI, mContainsId),
            values, null, null
        )
        mShoppingItemsView.requery()
        mShoppingItemsView.invalidate()
    }
}
