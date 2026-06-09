package org.openintents.shopping.ui

import android.content.ContentProviderOperation
import android.content.Context
import android.content.OperationApplicationException
import android.net.Uri
import android.os.RemoteException
import android.view.View

import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.Contains
import org.openintents.shopping.ui.widget.ShoppingItemsView

import java.util.ArrayList

class SnackbarUndoMultipleItemStatusOperation(
    private val mShoppingItemsView: ShoppingItemsView,
    private var mContext: Context,
    type: Int,
    listId: Long,
    batch: Boolean
) : SnackbarUndoOperation(1, type, batch) {

    companion object {
        const val UNMARK_ALL: Int = 0
        const val MARK_ALL: Int = 1
        const val CLEAN_LIST: Int = 2
    }

    private val old_status = longArrayOf(
        ShoppingContract.Status.BOUGHT,
        ShoppingContract.Status.WANT_TO_BUY,
        ShoppingContract.Status.BOUGHT
    )
    private val resIds = intArrayOf(
        R.plurals.undoable_unmark_all,
        R.plurals.undoable_mark_all,
        R.plurals.undoable_clean_list
    )
    private val mItemList: ArrayList<String>

    init {
        // remember all contains ids for listId where status = old_status
        val selection = "list_id = ? AND " + ShoppingContract.Contains.STATUS +
                " == " + old_status[mType]
        val c = mContext.contentResolver.query(
            Contains.CONTENT_URI, arrayOf(Contains._ID),
            selection, arrayOf(listId.toString()), null
        )
        mItemList = ArrayList()
        if (c != null) {
            c.moveToFirst()
            while (!c.isAfterLast) {
                mItemList.add(c.getString(0))
                c.moveToNext()
            }
            c.close()
        }
    }

    override fun getDescription(context: Context): String {
        val count = mItemList.size
        return String.format(context.resources.getQuantityString(resIds[mType], count), count)
    }

    override fun getSingularDescription(context: Context): String {
        return getDescription(context)
    }

    override fun onClick(view: View) {
        // here is where we get to try batch provider operation
        val ops = ArrayList<ContentProviderOperation>()
        for (i in 0 until mItemList.size) {
            val containsId = mItemList[i]
            val uri = Uri.withAppendedPath(Contains.CONTENT_URI, containsId)
            ops.add(
                ContentProviderOperation.newUpdate(uri)
                    .withValue(ShoppingContract.Contains.STATUS, old_status[mType])
                    .build()
            )
        }
        try {
            mContext.contentResolver.applyBatch(ShoppingContract.AUTHORITY, ops)
        } catch (e: RemoteException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: OperationApplicationException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } finally {
        }
        mShoppingItemsView.requery()
        mShoppingItemsView.invalidate()
    }
}
