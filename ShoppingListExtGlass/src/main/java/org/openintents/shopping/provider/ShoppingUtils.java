package org.openintents.shopping.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.openintents.shopping.Shopping;
import org.openintents.shopping.Shopping.Status;

public class ShoppingUtils {
    /**
     * TAG for logging.
     */
    private static final String TAG = "ShoppingUtils";

    /**
     * Obtain item id by name.
     *
     * @param context
     * @param name
     * @return Item ID or -1 if item does not exist.
     */
    private static long getItemId(Context context, String name) {
        long id = -1;

        Cursor existingItems = context.getContentResolver().query(Shopping.Items.CONTENT_URI,
                new String[]{Shopping.Items._ID}, "upper(name) = upper(?)",
                new String[]{name}, null);
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
        }
        existingItems.close();
        return id;
    }

    /**
     * Gets or creates a new item and returns its id. If the item exists
     * already, the existing id is returned. Otherwise a new item is created.
     *
     * @param name    New name of the item.
     * @param price
     * @param barcode
     * @return id of the new or existing item.
     */
    public static long updateOrCreateItem(Context context, String name, String tags, String price, String barcode) {
        long id = getItemId(context, name);

        if (id >= 0) {
            // Update existing item
            ContentValues values = getContentValues(
                    null, // Existing item: no need to change name.
                    tags, price, barcode);
            try {
                Uri uri = Uri.withAppendedPath(Shopping.Items.CONTENT_URI, String.valueOf(id));
                context.getContentResolver().update(uri, values, null, null);
                Log.i(TAG, "updated item: " + uri);
            } catch (Exception e) {
                Log.i(TAG, "Update item failed", e);
            }
        }

        if (id == -1) {
            // Add new item to list:
            ContentValues values = getContentValues(name, tags, price, barcode);
            try {
                Uri uri = context.getContentResolver().insert(Shopping.Items.CONTENT_URI, values);
                Log.i(TAG, "Insert new item: " + uri);
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.i(TAG, "Insert item failed", e);
                // return -1
            }
        }
        return id;

    }

    private static ContentValues getContentValues(String name, String tags, String price,
                                                  String barcode) {
        ContentValues values = new ContentValues(4);
        if (name != null) {
            values.put(Shopping.Items.NAME, name);
        }
        if (tags != null) {
            values.put(Shopping.Items.TAGS, tags);
        }
        if (price != null) {
            Long priceLong = PriceConverter.getCentPriceFromString(price);
            values.put(Shopping.Items.PRICE, priceLong);
        }
        if (barcode != null) {
            values.put(Shopping.Items.BARCODE, barcode);
        }
        return values;
    }

    /**
     * Gets or creates a new shopping list and returns its id. If the list
     * exists already, the existing id is returned. Otherwise a new list is
     * created.
     *
     * @param context
     * @param name    New name of the list.
     * @return id of the new or existing list.
     */
    public static long getList(Context context, final String name) {
        long id = -1;
        Cursor existingLists = context.getContentResolver().query(Shopping.Lists.CONTENT_URI,
                new String[]{Shopping.Lists._ID}, "upper(name) = upper(?)",
                new String[]{name}, null);
        if (existingLists.getCount() > 0) {
            existingLists.moveToFirst();
            id = existingLists.getLong(0);
        } else {
            // Add list to list:
            ContentValues values = new ContentValues(1);
            values.put(Shopping.Lists.NAME, name);
            try {
                Uri uri = context.getContentResolver().insert(Shopping.Lists.CONTENT_URI, values);
                Log.i(TAG, "Insert new list: " + uri);
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.i(TAG, "insert list failed", e);
                //return -1;
            }
        }
        existingLists.close();
        return id;
    }

    /**
     * Adds a new item to a specific list and returns its id. If the item exists
     * already, the existing id is returned.
     *
     * @param itemId   The id of the new item.
     * @param listId   The id of the shopping list the item is added.
     * @param quantity
     * @param itemType The type of the new item
     * @return id of the "contains" table entry, or -1 if insert failed.
     */
    public static long addItemToList(Context context, final long itemId, final long listId, String quantity) {
        long id = -1;
        long status = Status.WANT_TO_BUY;
        Cursor existingItems = context.getContentResolver()
                .query(Shopping.Contains.CONTENT_URI, new String[]{
                                Shopping.Contains._ID, Shopping.Contains.STATUS},
                        "list_id = ? AND item_id = ?",
                        new String[]{String.valueOf(listId),
                                String.valueOf(itemId)}, null
                );
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
            long oldstatus = existingItems.getLong(1);

            // Toggle status:
            if (oldstatus == Status.WANT_TO_BUY) {
                status = Status.BOUGHT;
            }

            // set status to want_to_buy:
            ContentValues values = new ContentValues(2);
            values.put(Shopping.Contains.STATUS, status);
            if (quantity != null) {
                // Only change quantity if an explicit value has been passed.
                // (see issue 286)
                values.put(Shopping.Contains.QUANTITY, quantity);
            }
            try {
                Uri uri = Uri.withAppendedPath(Shopping.Contains.CONTENT_URI, String.valueOf(id));
                context.getContentResolver().update(uri, values, null, null);
                Log.i(TAG, "updated item: " + uri);
            } catch (Exception e) {
                Log.i(TAG, "Insert item failed", e);
            }
        } else {
            // Add item to list:
            ContentValues values = new ContentValues(2);
            values.put(Shopping.Contains.ITEM_ID, itemId);
            values.put(Shopping.Contains.LIST_ID, listId);
            values.put(Shopping.Contains.STATUS, status);
            values.put(Shopping.Contains.QUANTITY, quantity);
            try {
                Uri uri = context.getContentResolver().insert(Shopping.Contains.CONTENT_URI, values);
                Log.i(TAG, "Insert new entry in 'contains': " + uri);
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.i(TAG, "insert into table 'contains' failed", e);
                //id = -1;
            }
        }
        existingItems.close();
        return id;
    }

    /**
     * Returns the id of the default shopping list. Currently this is always 1.
     *
     * @return The id of the default shopping list.
     */
    public static long getDefaultList(Context context) {
        long id = 1;
        Cursor c = context.getContentResolver().query(org.openintents.shopping.Shopping.ActiveList.CONTENT_URI,
                org.openintents.shopping.Shopping.ActiveList.PROJECTION, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            id = c.getLong(0);
            c.close();
        }
        return id;
    }

    public static Uri getListForItem(Context context, String itemId) {
        Cursor cursor = context.getContentResolver().query(Shopping.Contains.CONTENT_URI,
                new String[]{Shopping.Contains.LIST_ID}, Shopping.Contains.ITEM_ID + " = ?",
                new String[]{itemId}, Shopping.Contains.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            Uri uri;
            if (cursor.moveToFirst()) {

                uri = Uri.withAppendedPath(Shopping.Lists.CONTENT_URI, cursor
                        .getString(0));

            } else {
                uri = null;
            }
            cursor.close();
            return uri;
        } else {
            return null;
        }
    }

    /*
E/AndroidRuntime( 2469): 	at android.app.ActivityThread.handleServiceArgs(ActivityThread.java:3329)
E/AndroidRuntime( 2469): 	at android.app.ActivityThread.access$3600(ActivityThread.java:136)
E/AndroidRuntime( 2469): 	at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2252)
E/AndroidRuntime( 2469): 	at android.os.Handler.dispatchMessage(Handler.java:99)
E/AndroidRuntime( 2469): 	at android.os.Looper.loop(Looper.java:143)
E/AndroidRuntime( 2469): 	at android.app.ActivityThread.main(ActivityThread.java:5068)
E/AndroidRuntime( 2469): 	at java.lang.reflect.Method.invokeNative(Native Method)
E/AndroidRuntime( 2469): 	at java.lang.reflect.Method.invoke(Method.java:521)
E/AndroidRuntime( 2469): 	at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:858)
E/AndroidRuntime( 2469): 	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:616)
E/AndroidRuntime( 2469): 	at dalvik.system.NativeStart.main(Native Method)
E/AndroidRuntime( 2469): Caused by: java.lang.NullPointerException
E/AndroidRuntime( 2469): 	at org.openintents.shopping.provider.ShoppingUtils.getFirstList(ShoppingUtils.java:241)
E/AndroidRuntime( 2469): 	at org.openintents.extensions.liveviewshopping.LVShoppingService.startWork(LVShoppingService.java:100)
E/AndroidRuntime( 2469): 	at org.openintents.extensions.liveviewshopping.LVShoppingService.onStart(LVShoppingService.java:68)
E/AndroidRuntime( 2469): 	at android.app.Service.onStartCommand(Service.java:420)
E/AndroidRuntime( 2469): 	at android.app.ActivityThread.handleServiceArgs(ActivityThread.java:3315)
E/AndroidRuntime( 2469): 	... 10 more

     */
    public static long getFirstList(Context context) {
        Cursor existingLists = context.getContentResolver().query(Shopping.Lists.CONTENT_URI,
                new String[]{Shopping.Lists._ID}, null,
                null, null);
        long id;
        if (existingLists.getCount() > 0) {
            existingLists.moveToFirst();
            id = existingLists.getLong(0);
        } else {
            id = 1;
        }
        return id;
    }

    public static Cursor getListsIds(Context context) {

        Cursor existingLists = context.getContentResolver().query(Shopping.Lists.CONTENT_URI,
                new String[]{Shopping.Lists._ID, Shopping.Lists.SKIN_BACKGROUND, Shopping.Lists.NAME}, null,
                null, null);

        long[] ids = null;
        if (existingLists.getCount() > 0) {

            existingLists.moveToFirst();

            ids = new long[existingLists.getCount()];

            for (int i = 0; i < existingLists.getCount(); i++) {

                ids[i] = existingLists.getLong(0);

                existingLists.moveToNext();

            }

        } else {
            ids = new long[0];
        }
        return existingLists;
    }

}
