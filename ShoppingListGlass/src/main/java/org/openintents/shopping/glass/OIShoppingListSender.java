package org.openintents.shopping.glass;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.util.Log;

import org.openintents.shopping.Shopping;
import org.openintents.shopping.provider.ShoppingUtils;
import org.openintents.shopping.provider.ThemeUtils2;

public class OIShoppingListSender {
    private static final String LOG_TAG = "OIShoppingListSender";
    private boolean mInvalideShoppingVersion;
    private Cursor mShoppingListIds;
    private Activity context;
    private int mShoppingListPos;
    private int mPos;
    private ContentObserver mContentObserver;
    private long mShoppingListId;
    private String mShoppingListName;
    private Cursor mExistingItems;


    public OIShoppingListSender(Activity activity) {
        this.context = activity;

        try {
            PackageInfo info = context.getPackageManager().getPackageInfo("org.openintents.shopping", 0);
            if (info.versionCode < 10024) {
                mInvalideShoppingVersion = true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            mInvalideShoppingVersion = true;
        }

        initShoppingLists(false);

    }

    private void initShoppingLists(boolean setDefault) {

        if (mInvalideShoppingVersion) {
            return;
        }

        mShoppingListIds = ShoppingUtils.getListsIds(context);

        if (setDefault) {
            long activeListId = ShoppingUtils.getDefaultList(context);
            Log.d(LOG_TAG, "active list " + activeListId);

            mShoppingListPos = 0;
            int count = 0;
            if (mShoppingListIds.moveToFirst()) {
                do {

                    long id = mShoppingListIds.getLong(0);
                    if (id == activeListId) {
                        mShoppingListPos = count;
                        Log.d(LOG_TAG, "active list pos " + count);
                        break;
                    }
                    count++;
                } while (mShoppingListIds.moveToNext());
            }
        }

        setCurrentShoppingListId(mShoppingListPos);

        refreshCursor();

        /*
        context.getContentResolver().registerContentObserver(Shopping.Contains.CONTENT_URI,

                true, mContentObserver) ;
        context.getContentResolver().registerContentObserver(Shopping.ContainsFull.CONTENT_URI,
                true, mContentObserver);
        context.getContentResolver().registerContentObserver(
                Shopping.Items.CONTENT_URI, true, mContentObserver);
        */
        mPos = 0;

    }

    private void setCurrentShoppingListId(int mShoppingListPos2) {
        mShoppingListIds.moveToPosition(mShoppingListPos);
        if (mShoppingListIds.getCount() > 0) {
            mShoppingListId = mShoppingListIds.getLong(0);
        }
        Log.d(LOG_TAG, "mShoppingListId: " + mShoppingListId);
        ThemeUtils2.setRemoteStyle(context, mShoppingListIds.getString(1), 14,
                true);
        mShoppingListName = mShoppingListIds.getString(2);

    }

    private void refreshCursor() {

        Log.d(LOG_TAG, "refreshCursor() called");
        try {
            if (mExistingItems != null) {
                mExistingItems.close();
            }
            String sortOrder = "contains.modified_date"; //

            mExistingItems = context.getContentResolver()
                    .query(

                            Shopping.ContainsFull.CONTENT_URI,
                            new String[]{Shopping.ContainsFull._ID,
                                    Shopping.ContainsFull.ITEM_NAME,
                                    Shopping.ContainsFull.ITEM_TAGS,
                                    Shopping.ContainsFull.QUANTITY,
                                    Shopping.ContainsFull.MODIFIED_DATE,
                                    Shopping.ContainsFull.STATUS},
                            Shopping.ContainsFull.LIST_ID + " = ?  AND  "
                                    + Shopping.ContainsFull.STATUS + "<= ?",

                            new String[]{
                                    String.valueOf(mShoppingListId),
                                    String.valueOf(Shopping.Status.WANT_TO_BUY)},
                            null);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Item getItem(int pos) {

        Item result = null;
        if (mExistingItems != null) {
            mExistingItems.requery();
            mExistingItems.moveToPosition(pos);
            if (mExistingItems.getCount() > 0) {

                result = new Item(

                        mExistingItems.getString(0), mExistingItems.getString(1),
                        mExistingItems.getString(2), mExistingItems.getInt(3),
                        // mExistingItems.getInt(4),
                        mExistingItems.getInt(5)

                );
            }
        }
        return result;
    }

    public String getShoppingListName() {
        return mShoppingListName;
    }

    public int getCount() {
        return mExistingItems.getCount();
    }

    public static class Item {

        private Item(String id, String item, String tags, int quantity,
                     int bought) {
            this.item = item;
            this.tags = tags;
            this.quantity = quantity;
            if (bought == 1) {

                this.bought = false;
            } else {

                this.bought = true;

            }
        }

        public String id;
        public String item;
        public String tags;
        public int quantity;
        public boolean bought;
    }

}