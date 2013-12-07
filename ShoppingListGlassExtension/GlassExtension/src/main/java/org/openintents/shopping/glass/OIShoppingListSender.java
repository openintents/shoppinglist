package org.openintents.shopping.glass;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import org.openintents.shopping.Shopping;
import org.openintents.shopping.provider.ShoppingUtils;
import org.openintents.shopping.provider.ThemeUtils2;

public class OIShoppingListSender {
    private static final String TAG = "OIShoppingListSender";
    private static final boolean debug = true;
//    private boolean mInvalideShoppingVersion;
    private Cursor mShoppingListIds;
    private Context context;
    private ContentObserver mContentObserver;
    private ContentCallback contentCallback;
    private long mShoppingListId;
    private Cursor mExistingItems;

    public void initSender(Context context) {
        if (debug) Log.d(TAG, "initSender("+context+")");
        this.context = context;
        initShoppingLists(true);
    }

    public String[] getLists() {
        Cursor listsIds = ShoppingUtils.getListsIds(context);
        int count=listsIds.getCount();
        if (debug) Log.d(TAG, "count="+count);
        String[] lists=new String[count];
        int pos=0;
        if (listsIds.moveToFirst()) {
            do {
                long listId = listsIds.getLong(0);
                String listName = listsIds.getString(2);
                if (debug) Log.d(TAG, "list: " + listId + " " + listName);
                lists[pos]=listName;
                pos++;
            } while (listsIds.moveToNext());
        }
        return lists;
    }

    public void setActiveListId(long id) {
        if (debug) Log.d(TAG, "setActiveListId("+id+")");
        if (id>mShoppingListIds.getCount()) {
            if (debug) Log.d(TAG,"trying to set to bad id");
            return;
        }
        setCurrentShoppingListId((int) id);
    }

    private void initShoppingLists(boolean setDefault) {

//        if (mInvalideShoppingVersion) {
//            return;
//        }

        mShoppingListIds = ShoppingUtils.getListsIds(context);

        if (setDefault) {
            long activeListId = ShoppingUtils.getDefaultList(context);
            if (debug) Log.d(TAG, "active list " + activeListId);

            int count = 0;
            if (mShoppingListIds.moveToFirst()) {
                do {

                    long id = mShoppingListIds.getLong(0);
                    if (id == activeListId) {
                        if (debug) Log.d(TAG, "active list pos " + count);
                        break;
                    }
                    count++;
                } while (mShoppingListIds.moveToNext());
            }
        }

        // default to the first list
        setCurrentShoppingListId(0);

        int mPos;
        mPos = 0;
        refreshCursor();
        if (mExistingItems != null) {
            int count=mExistingItems.getCount();
            if (debug) Log.d(TAG, "count="+count);
            while(mPos<count){
                Item item=getItem(mPos);
                if (debug) Log.d(TAG, "item="+item.item);
                mPos++;
            }
        }
    }

    public void registerObserver(ContentCallback callback) {
        contentCallback = callback;
        mContentObserver = new ShoppingObserver(null);
        context.getContentResolver().registerContentObserver(Shopping.Contains.CONTENT_URI,
                true, mContentObserver);
        context.getContentResolver().registerContentObserver(Shopping.ContainsFull.CONTENT_URI,
                true, mContentObserver);
        context.getContentResolver().registerContentObserver(
                Shopping.Items.CONTENT_URI, true, mContentObserver);
    }

    public void unregisterObserver() {
        if (mContentObserver!=null) {
            context.getContentResolver().
                unregisterContentObserver(mContentObserver);
        }
    }

    class ShoppingObserver extends ContentObserver {

        public ShoppingObserver(Handler handler) {
            super(handler);
        }
        @Override
        public boolean deliverSelfNotifications() {
            if (debug) Log.d(TAG, "deliverSelfNotifications()");
            return super.deliverSelfNotifications();
        }
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (debug) Log.d(TAG, "onChange("+selfChange+")");
        }
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (debug) Log.d(TAG, "onChange("+selfChange+","+uri+")");
            contentCallback.onChange();
        }
    }

    public static interface ContentCallback {
        public void onChange();
    }

    public String[] getItems() {

        if (mExistingItems==null) {
            return new String[0];
        }

        int count=mExistingItems.getCount();
        String[] items=new String[count];
        if (count>0) {
            int position=0;
            while(position<count){
                Item item=getItem(position);
                if (debug) Log.d(TAG, "items["+position+"]="+item.item);
                items[position]=item.item;
                position++;
            }

        }

        return items;
    }

    private void setCurrentShoppingListId(int mShoppingListPos2) {
        mShoppingListIds.moveToPosition(mShoppingListPos2);
        if (mShoppingListIds.getCount() > 0) {
            mShoppingListId = mShoppingListIds.getLong(0);
        }
        Log.d(TAG, "mShoppingListId: " + mShoppingListId);
        ThemeUtils2.setRemoteStyle(context, mShoppingListIds.getString(1), 14,
                true);
        String mShoppingListName;
        mShoppingListName = mShoppingListIds.getString(2);
        if (debug) Log.d(TAG, "mShoppingListName: " + mShoppingListName);
    }

    public void refreshCursor() {

        Log.d(TAG, "refreshCursor() called");
        try {
            if (mExistingItems != null) {
                mExistingItems.close();
            }
//            String sortOrder = "contains.modified_date"; //

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

            if (debug) Log.d(TAG, "mExistingItems=" + mExistingItems);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Item getItem(int pos) {

        Item result = null;
        if (mExistingItems != null) {
//            refreshCursor();
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

    private class Item {

        private Item(String id, String item, String tags, int quantity,
                     int bought) {
            this.id = id;
            this.item = item;
            this.tags = tags;
            this.quantity = quantity;
            this.bought = bought != 1;
        }

        public String id;
        public String item;
        public String tags;
        public int quantity;
        public boolean bought;
    }

}
