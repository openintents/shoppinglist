package org.openintents.shopping.ui;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class ShoppingTotalsHandler implements LoaderManager.LoaderCallbacks<Cursor> {
    private final static String TAG = "ShoppingTotalsHandler";
    private final static boolean debug = false;

    private Activity mActivity;
    private ShoppingItemsView mItemsView;
    private CursorLoader mCursorLoader;

    private long mListId;

    private TextView mTotalTextView;
    private TextView mPriTotalTextView;
    private TextView mTotalCheckedTextView;
    private TextView mCountTextView;

    private NumberFormat mPriceFormatter = DecimalFormat.getNumberInstance(Locale.ENGLISH);

    public ShoppingTotalsHandler(ShoppingItemsView view) {
        mItemsView = view;
        mActivity = (Activity) view.getContext();

        mTotalCheckedTextView = (TextView) mActivity.findViewById(R.id.total_1);
        mTotalTextView = (TextView) mActivity.findViewById(R.id.total_2);
        mPriTotalTextView = (TextView) mActivity.findViewById(R.id.total_3);
        mCountTextView = (TextView) mActivity.findViewById(R.id.count);

        mPriceFormatter.setMaximumFractionDigits(2);
        mPriceFormatter.setMinimumFractionDigits(2);
    }

    public void update(LoaderManager manager, long listId) {
        if (mCursorLoader == null) {
            mListId = listId;
            mCursorLoader = (CursorLoader) manager.initLoader(ShoppingActivity.LOADER_TOTALS, null, this);
        } else {
            if (mListId != listId) {
                mListId = listId;
                mCursorLoader.setUri(ShoppingContract.Subtotals.CONTENT_URI.buildUpon().appendPath(Long.toString(mListId)).build());
            }
            manager.restartLoader(ShoppingActivity.LOADER_TOTALS, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(mActivity);
        loader.setProjection(ShoppingContract.Subtotals.PROJECTION);
        loader.setUri(ShoppingContract.Subtotals.CONTENT_URI.buildUpon().appendPath(Long.toString(mListId)).build());
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor total_cursor) {
        long total = 0;
        long totalchecked = 0;
        long priority_total = 0;
        int priority_threshold = PreferenceActivity.getSubtotalByPriorityThreshold(mActivity);
        boolean prioIncludesChecked =
                PreferenceActivity.prioritySubtotalIncludesChecked(mActivity);
        int numChecked = 0, numUnchecked = 0;

        total_cursor.moveToPosition(-1);
        while (total_cursor.moveToNext()) {
            long item_status = total_cursor.getLong(ShoppingContract.Subtotals.STATUS_INDEX);
            boolean isChecked = (item_status == ShoppingContract.Status.BOUGHT);

            if (item_status == ShoppingContract.Status.REMOVED_FROM_LIST) {
                continue;
            }

            long price = total_cursor.getLong(ShoppingContract.Subtotals.SUBTOTAL_INDEX);
            total += price;

            if (isChecked) {
                totalchecked += price;
                numChecked += total_cursor.getLong(ShoppingContract.Subtotals.COUNT_INDEX);
            } else if (item_status == ShoppingContract.Status.WANT_TO_BUY) {
                numUnchecked += total_cursor.getLong(ShoppingContract.Subtotals.COUNT_INDEX);
            }

            if (priority_threshold != 0 && (prioIncludesChecked || !isChecked)) {
                String priority_str = total_cursor.getString(ShoppingContract.Subtotals.PRIORITY_INDEX);
                if (priority_str != null) {
                    int priority = 0;
                    try {
                        priority = Integer.parseInt(priority_str);
                    } catch (NumberFormatException e) {
                        // pretend it's a 0 then...
                    }
                    if (priority != 0 && priority <= priority_threshold) {
                        priority_total += price;
                    }
                }
            }
        }

        if (debug) {
            Log.d(TAG, "Total: " + total + ", Checked: " + totalchecked + "(#" + numChecked + ")");
        }
        mItemsView.updateNumChecked(numChecked, numUnchecked);

        if (mTotalTextView == null || mTotalCheckedTextView == null) {
            // Most probably in "Add item" mode where no total is displayed
            return;
        }

        if (mItemsView.mPriceVisibility != View.VISIBLE) {
            // If price is not displayed, do not display total
            mTotalTextView.setVisibility(View.GONE);
            mPriTotalTextView.setVisibility(View.GONE);
            mTotalCheckedTextView.setVisibility(View.GONE);
            return;
        }


        mTotalTextView.setTextColor(mItemsView.mTextColorPrice);
        mPriTotalTextView.setTextColor(mItemsView.mTextColorPrice);
        mTotalCheckedTextView.setTextColor(mItemsView.mTextColorPrice);
        mCountTextView.setTextColor(mItemsView.mTextColorPrice);

        if (total != 0) {
            String s = mPriceFormatter.format(total * 0.01d);
            s = mActivity.getString(R.string.total, s);
            mTotalTextView.setText(s);
            mTotalTextView.setVisibility(View.VISIBLE);
        } else {
            mTotalTextView.setVisibility(View.GONE);
        }

        if (priority_total != 0) {
            final int[] captions = {0, R.string.priority1_total, R.string.priority2_total,
                    R.string.priority3_total, R.string.priority4_total};
            String s = mPriceFormatter.format(priority_total * 0.01d);
            s = mActivity.getString(captions[priority_threshold], s);
            mPriTotalTextView.setText(s);
            mPriTotalTextView.setVisibility(View.VISIBLE);
        } else {
            mPriTotalTextView.setVisibility(View.GONE);
        }

        if (totalchecked != 0) {
            String s = mPriceFormatter.format(totalchecked * 0.01d);
            s = mActivity.getString(R.string.total_checked, s);
            mTotalCheckedTextView.setText(s);
            mTotalCheckedTextView.setVisibility(View.VISIBLE);
            mCountTextView.setVisibility(View.VISIBLE);
        } else {
            mTotalCheckedTextView.setVisibility(View.GONE);
            mCountTextView.setVisibility(View.GONE);
        }
        mCountTextView.setText("#" + numChecked);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}

