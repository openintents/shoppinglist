package org.openintents.shopping.ui;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.view.View;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

public class SnackbarUndoSingleItemStatusOperation extends SnackbarUndoOperation {

    /**
     *
     */
    private final ShoppingItemsView mShoppingItemsView;
    private Context mContext;
    private long mOldStatus;
    private long mNewStatus;
    private String mItemName;
    private String mContainsId;

    public SnackbarUndoSingleItemStatusOperation(ShoppingItemsView shoppingItemsView, Context context,
                                                 String containsId, String name,
                                                 long old_status, long new_status,
                                                 int type, boolean batch) {
        super(1, type, batch);
        mShoppingItemsView = shoppingItemsView;
        mContext = context;
        mContainsId = containsId;
        mItemName = name;
        mOldStatus = old_status;
        mNewStatus = new_status;
    }

    @Override
    public String getDescription(Context context) {
        return getSingularDescription(context);
    }

    @Override
    public String getSingularDescription(Context context) {
        int resId;

        if (mShoppingItemsView.inAddItemsMode()) {
            if (mNewStatus == ShoppingContract.Status.WANT_TO_BUY) {
                resId = R.string.undoable_added_item;
            } else {
                resId = R.string.undoable_removed_item;
            }
        } else {
            if (mNewStatus == ShoppingContract.Status.WANT_TO_BUY) {
                resId = R.string.undoable_unmarked_item;
            } else {
                resId = R.string.undoable_marked_item;
            }
        }
        return String.format(context.getResources().getString(resId), mItemName);
    }

    @Override
    public void onClick(View view) {
        ContentValues values = new ContentValues();
        values.put(ShoppingContract.Contains.STATUS, mOldStatus);
        mContext.getContentResolver().update(
                Uri.withAppendedPath(ShoppingContract.Contains.CONTENT_URI,
                        mContainsId), values, null, null
        );
        mShoppingItemsView.requery();
        mShoppingItemsView.invalidate();
    }
}