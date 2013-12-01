package org.openintents.shopping.glass;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.util.Log;

import org.openintents.shopping.Shopping;
import org.openintents.shopping.provider.ShoppingUtils;
import org.openintents.shopping.provider.ThemeUtils2;

public class OIShoppingListSender {
    private static final String LOG_TAG = "OIShoppingListSender";
    private static final boolean debug = true;
    private boolean mInvalideShoppingVersion;
    private Cursor mShoppingListIds;
    private Context context;
    private int mShoppingListPos;
    private int mPos;
    private ContentObserver mContentObserver;
    private long mShoppingListId;
    private String mShoppingListName;
    private Cursor mExistingItems;

    public void initSender(Context context) {
        if (debug) Log.d(LOG_TAG, "initSender("+context+")");
        this.context = context;
        initShoppingLists(true);
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


/*        mContentObserver = new ShoppingObserver(null);
        context.getContentResolver().registerContentObserver(Shopping.Contains.CONTENT_URI,
                true, mContentObserver);
        context.getContentResolver().registerContentObserver(Shopping.ContainsFull.CONTENT_URI,
                true, mContentObserver);
        context.getContentResolver().registerContentObserver(
                Shopping.Items.CONTENT_URI, true, mContentObserver);
*/
        mPos = 0;
        refreshCursor();
        if (mExistingItems != null) {
            int count=mExistingItems.getCount();
            if (debug) Log.d(LOG_TAG, "count="+count);
            while(mPos<count){
                Item item=getItem(mPos);
                if (debug) Log.d(LOG_TAG, "item="+item.item);
                mPos++;
            }
        }
    }
/*
    class ShoppingObserver extends ContentObserver {

        public ShoppingObserver(Handler handler) {

            super(handler);

        }
        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }
    };
*/
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
                if (debug) Log.d(LOG_TAG, "item="+item.item);
                items[position]=item.item;
                position++;
            }

        }

        return items;
    }
    /*
    public String buildShoppingCard() {
        String card="";
        card+="<article>";
        card+="<section>";
        if (mExistingItems != null) {
            int count=mExistingItems.getCount();
            if (debug) Log.d(LOG_TAG, "count="+count);
            if (count==0) {
                return "Nothing in shopping list.";
            }
            int position=0;
            card+="<li>";
            while(position<count){
                Item item=getItem(position);
                if (debug) Log.d(LOG_TAG, "item="+item.item);
                card += "<ul>"+item.item+"</ul>\n";
                position++;
            }
            card+="</li>";
        }
        card+="</section>";
        card+="</article>";
        return card;
    }*/

    private void setCurrentShoppingListId(int mShoppingListPos2) {
        mShoppingListIds.moveToPosition(mShoppingListPos);
        if (mShoppingListIds.getCount() > 0) {
            mShoppingListId = mShoppingListIds.getLong(0);
        }
        Log.d(LOG_TAG, "mShoppingListId: " + mShoppingListId);
        ThemeUtils2.setRemoteStyle(context, mShoppingListIds.getString(1), 14,
                true);
        mShoppingListName = mShoppingListIds.getString(2);
        if (debug) Log.d(LOG_TAG, "mShoppingListName: " + mShoppingListName);

    }

    public void refreshCursor() {

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

            if (debug) Log.d(LOG_TAG, "mExistingItems=" + mExistingItems);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Item getItem(int pos) {

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

    private class Item {

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
