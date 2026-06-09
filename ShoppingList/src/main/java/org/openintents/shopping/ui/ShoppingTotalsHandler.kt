package org.openintents.shopping.ui

import android.app.Activity
import android.app.LoaderManager
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.ui.widget.ShoppingItemsView
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

open class ShoppingTotalsHandler(view: ShoppingItemsView) : LoaderManager.LoaderCallbacks<Cursor> {

    private val mItemsView: ShoppingItemsView = view
    private val mActivity: Activity = view.context as Activity
    private var mCursorLoader: CursorLoader? = null

    private var mListId: Long = 0

    private val mTotalTextView: TextView?
    private val mPriTotalTextView: TextView?
    private val mTotalCheckedTextView: TextView?
    private val mCountTextView: TextView?

    private val mPriceFormatter: NumberFormat = DecimalFormat.getNumberInstance(Locale.ENGLISH)

    init {
        mTotalCheckedTextView = mActivity.findViewById(R.id.total_1)
        mTotalTextView = mActivity.findViewById(R.id.total_2)
        mPriTotalTextView = mActivity.findViewById(R.id.total_3)
        mCountTextView = mActivity.findViewById(R.id.count)

        mPriceFormatter.maximumFractionDigits = 2
        mPriceFormatter.minimumFractionDigits = 2
    }

    fun update(manager: LoaderManager, listId: Long) {
        if (mCursorLoader == null) {
            mListId = listId
            mCursorLoader = manager.initLoader(ShoppingActivity.LOADER_TOTALS, null, this) as CursorLoader
        } else {
            if (mListId != listId) {
                mListId = listId
                mCursorLoader!!.setUri(
                    ShoppingContract.Subtotals.CONTENT_URI.buildUpon()
                        .appendPath(mListId.toString()).build()
                )
            }
            manager.restartLoader(ShoppingActivity.LOADER_TOTALS, null, this)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val loader = CursorLoader(mActivity)
        loader.projection = ShoppingContract.Subtotals.PROJECTION
        loader.setUri(
            ShoppingContract.Subtotals.CONTENT_URI.buildUpon()
                .appendPath(mListId.toString()).build()
        )
        return loader
    }

    override fun onLoadFinished(loader: Loader<Cursor>, total_cursor: Cursor) {
        var total: Long = 0
        var totalchecked: Long = 0
        var priority_total: Long = 0
        val priority_threshold = PreferenceActivity.getSubtotalByPriorityThreshold(mActivity)
        val prioIncludesChecked = PreferenceActivity.prioritySubtotalIncludesChecked(mActivity)
        var numChecked = 0
        var numUnchecked = 0

        total_cursor.moveToPosition(-1)
        while (total_cursor.moveToNext()) {
            val item_status = total_cursor.getLong(ShoppingContract.Subtotals.STATUS_INDEX)
            val isChecked = (item_status == ShoppingContract.Status.BOUGHT)

            if (item_status == ShoppingContract.Status.REMOVED_FROM_LIST) {
                continue
            }

            val price = total_cursor.getLong(ShoppingContract.Subtotals.SUBTOTAL_INDEX)
            total += price

            if (isChecked) {
                totalchecked += price
                numChecked += total_cursor.getLong(ShoppingContract.Subtotals.COUNT_INDEX).toInt()
            } else if (item_status == ShoppingContract.Status.WANT_TO_BUY) {
                numUnchecked += total_cursor.getLong(ShoppingContract.Subtotals.COUNT_INDEX).toInt()
            }

            if (priority_threshold != 0 && (prioIncludesChecked || !isChecked)) {
                val priority_str = total_cursor.getString(ShoppingContract.Subtotals.PRIORITY_INDEX)
                if (priority_str != null) {
                    var priority = 0
                    try {
                        priority = Integer.parseInt(priority_str)
                    } catch (e: NumberFormatException) {
                        // pretend it's a 0 then...
                    }
                    if (priority != 0 && priority <= priority_threshold) {
                        priority_total += price
                    }
                }
            }
        }

        if (debug) {
            Log.d(TAG, "Total: $total, Checked: $totalchecked(#$numChecked)")
        }
        mItemsView.updateNumChecked(numChecked.toLong(), numUnchecked.toLong())

        if (mTotalTextView == null || mTotalCheckedTextView == null) {
            // Most probably in "Add item" mode where no total is displayed
            return
        }

        if (mItemsView.mPriceVisibility != View.VISIBLE) {
            // If price is not displayed, do not display total
            mTotalTextView.visibility = View.GONE
            mPriTotalTextView!!.visibility = View.GONE
            mTotalCheckedTextView.visibility = View.GONE
            return
        }

        mTotalTextView.setTextColor(mItemsView.mTextColorPrice)
        mPriTotalTextView!!.setTextColor(mItemsView.mTextColorPrice)
        mTotalCheckedTextView.setTextColor(mItemsView.mTextColorPrice)
        mCountTextView!!.setTextColor(mItemsView.mTextColorPrice)

        if (total != 0L) {
            var s = mPriceFormatter.format(total * 0.01)
            s = mActivity.getString(R.string.total, s)
            mTotalTextView.text = s
            mTotalTextView.visibility = View.VISIBLE
        } else {
            mTotalTextView.visibility = View.GONE
        }

        if (priority_total != 0L) {
            val captions = intArrayOf(0, R.string.priority1_total, R.string.priority2_total,
                R.string.priority3_total, R.string.priority4_total)
            var s = mPriceFormatter.format(priority_total * 0.01)
            s = mActivity.getString(captions[priority_threshold], s)
            mPriTotalTextView.text = s
            mPriTotalTextView.visibility = View.VISIBLE
        } else {
            mPriTotalTextView.visibility = View.GONE
        }

        if (totalchecked != 0L) {
            var s = mPriceFormatter.format(totalchecked * 0.01)
            s = mActivity.getString(R.string.total_checked, s)
            mTotalCheckedTextView.text = s
            mTotalCheckedTextView.visibility = View.VISIBLE
            mCountTextView!!.visibility = View.VISIBLE
        } else {
            mTotalCheckedTextView.visibility = View.GONE
            mCountTextView!!.visibility = View.GONE
        }
        mCountTextView!!.text = "#$numChecked"
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
    }

    companion object {
        private const val TAG = "ShoppingTotalsHandler"
        private const val debug = false
    }
}
