package org.openintents.shopping.ui;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.view.View;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

import java.util.ArrayList;

public class SnackbarUndoMultipleItemStatusOperation extends SnackbarUndoOperation {

    /**
     *
     */
    public static final int UNMARK_ALL = 0;
    public static final int MARK_ALL = 1;
    public static final int CLEAN_LIST = 2;
    private final ShoppingItemsView mShoppingItemsView;
    private long[] old_status = {ShoppingContract.Status.BOUGHT,
            ShoppingContract.Status.WANT_TO_BUY, ShoppingContract.Status.BOUGHT};
    private int[] resIds = {R.plurals.undoable_unmark_all, R.plurals.undoable_mark_all,
            R.plurals.undoable_clean_list};
    private Context mContext;
    private ArrayList<String> mItemList;

    public SnackbarUndoMultipleItemStatusOperation(ShoppingItemsView shoppingItemsView, Context context,
                                                   int type, long listId, boolean batch) {
        super(1, type, batch);
        mShoppingItemsView = shoppingItemsView;
        mContext = context;

        // remember all contains ids for listId where status = old_status
        String selection = "list_id = ? AND " + ShoppingContract.Contains.STATUS
                + " == " + old_status[mType];
        Cursor c = context.getContentResolver().query(
                Contains.CONTENT_URI, new String[]{Contains._ID},
                selection, new String[]{String.valueOf(listId)}, null);
        int numItems = c.getCount();
        mItemList = new ArrayList<String>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            mItemList.add(c.getString(0));
        }
    }

    @Override
    public String getDescription(Context context) {
        int count = mItemList.size();
        return String.format(context.getResources().getQuantityString(resIds[mType], count), count);
    }

    @Override
    public String getSingularDescription(Context context) {
        return getDescription(context);
    }

    @Override
    public void onClick(View view) {
        // here is where we get to try batch provider operation
        ArrayList<ContentProviderOperation> ops =
                new ArrayList<ContentProviderOperation>();
        for (int i = 0; i < mItemList.size(); i++) {
            String containsId = mItemList.get(i);
            Uri uri = Uri.withAppendedPath(Contains.CONTENT_URI, containsId);
            ops.add(ContentProviderOperation.newUpdate(uri).
                    withValue(ShoppingContract.Contains.STATUS, old_status[mType]).build());
        }
        try {
            mContext.getContentResolver().applyBatch(ShoppingContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {

        }
        mShoppingItemsView.requery();
        mShoppingItemsView.invalidate();
    }
}