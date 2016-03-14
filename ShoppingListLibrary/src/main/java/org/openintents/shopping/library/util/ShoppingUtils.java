/* 
 * Copyright (C) 2007-2011 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.shopping.library.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.*;

public class ShoppingUtils {
    /**
     * TAG for logging.
     */
    private static final String TAG = "ShoppingUtils";
    private static final boolean debug = false;

    /**
     * Obtain item id by name.
     *
     * @param context
     * @param name
     * @return Item ID or -1 if item does not exist.
     */
    public static long getItemId(Context context, String name) {
        long id = -1;

        Cursor existingItems = context.getContentResolver().query(
                Items.CONTENT_URI, new String[]{Items._ID},
                "upper(name) = upper(?)", new String[]{name}, null);
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
        }

        existingItems.close();
        return id;
    }

    public static long getItemIdForList(Context context, String name, String list_id) {
        long id = -1;

        Cursor existingItems = context.getContentResolver().query(
                ContainsFull.CONTENT_URI, new String[]{Contains.ITEM_ID},
                "list_id = ? and upper(items.name) = upper(?)", new String[]{list_id, name}, null);
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
        }
        existingItems.close();
        return id;
    }

    public static String getItemName(Context context, long itemId) {
        String name = "";
        Cursor existingItems = context.getContentResolver().query(
                ShoppingContract.Items.CONTENT_URI,
                new String[]{ShoppingContract.Items.NAME}, "_id = ?",
                new String[]{String.valueOf(itemId)}, null);
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            name = existingItems.getString(0);
        }
        existingItems.close();
        return name;
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
    public static long updateOrCreateItem(Context context, String name,
                                          String tags, String price, String barcode, String list_id) {
        long id;

        if (list_id == null) {
            id = getItemId(context, name);
        } else {
            id = getItemIdForList(context, name, list_id);
        }

        if (id >= 0) {
            // Update existing item
            // (pass 'null' for name: Existing item: no need to change name.)
            ContentValues values = getContentValues(name, tags, price, barcode);
            try {
                Uri uri = Uri.withAppendedPath(
                        ShoppingContract.Items.CONTENT_URI, String.valueOf(id));
                context.getContentResolver().update(uri, values, null, null);
                if (debug) {
                    Log.d(TAG, "updated item: " + uri);
                }
            } catch (Exception e) {
                Log.e(TAG, "Update item failed", e);
            }
        }

        if (id == -1) {
            // Add new item to list.
            ContentValues values = getContentValues(name, tags, price, barcode);
            try {
                Uri uri = context.getContentResolver().insert(
                        ShoppingContract.Items.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new item: " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.e(TAG, "Insert item failed", e);
                // return -1
            }
        }
        return id;

    }

    private static ContentValues getContentValues(String name, String tags,
                                                  String price, String barcode) {
        ContentValues values = new ContentValues(4);
        if (name != null) {
            values.put(ShoppingContract.Items.NAME, name);
        }
        if (tags != null) {
            values.put(ShoppingContract.Items.TAGS, tags);
        }
        if (price != null) {
            Long priceLong = PriceConverter.getCentPriceFromString(price);
            values.put(ShoppingContract.Items.PRICE, priceLong);
        }
        if (barcode != null) {
            values.put(ShoppingContract.Items.BARCODE, barcode);
        }
        return values;
    }

    /**
     * Gets or creates a new item and returns its id. If the item exists
     * already, the existing id is returned. Otherwise a new item is created.
     *
     * @param name New name of the item.
     * @return id of the new or existing item.
     */
    public static long getItem(Context context, String name, String tags,
                               String price, String units, String note, Boolean duplicate,
                               Boolean update) {
        long id = -1;

        if (!duplicate) {
            if (id == -1) {
                id = getItemId(context, name);
            }

            if (id != -1 && !update) {
                return id;
            }
        }

        return getItem(context, id, name, tags, price, units, note);
    }

    /**
     * Gets or creates a new item and returns its id. If the item exists
     * already, the existing id is returned. Otherwise a new item is created.
     *
     * @param id   id of the item to update, or -1 to create a new item.
     * @param name New name of the item.
     * @return id of the new or existing item.
     */
    public static long getItem(Context context, long id, String name,
                               String tags, String price, String units, String note) {

        // now we are either updating or adding.
        // either way we need some content values.
        // Add item to list:
        ContentValues values = new ContentValues(1);
        if (id == -1) {
            values.put(Items.NAME, name);
        }

        values.put(Items.TAGS, tags);
        if (!TextUtils.isEmpty(note)) {
            values.put(Items.NOTE, note);
        }
        if (price != null) {
            values.put(Items.PRICE, price);
        }
        if (!TextUtils.isEmpty(units)) {
            // in the items table we store the string directly,
            // but we register the units in the units table for use in
            // completion.
            long unit_id = getUnits(context, units);
            values.put(Items.UNITS, units);
        }

        try {
            if (id == -1) {
                Uri uri = context.getContentResolver().insert(
                        Items.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new item: " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } else {
                context.getContentResolver().update(
                        Uri.withAppendedPath(Items.CONTENT_URI,
                                String.valueOf(id)), values, null, null
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Insert item failed", e);
            // return -1
        }

        return id;
    }

    public static long getUnits(Context context, String units) {
        long id = -1;
        Cursor existingUnits = context.getContentResolver().query(
                Units.CONTENT_URI, new String[]{Units._ID},
                "upper(name) = upper(?)", new String[]{units}, null);
        if (existingUnits.getCount() > 0) {
            existingUnits.moveToFirst();
            id = existingUnits.getLong(0);
            existingUnits.close();

        } else {
            existingUnits.close();
            // Add item to list:
            ContentValues values = new ContentValues(1);
            values.put(Units.NAME, units);
            try {
                Uri uri = context.getContentResolver().insert(
                        Units.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new units: " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.e(TAG, "Insert units failed", e);
                // return -1
            }
        }
        return id;
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
        Cursor existingItems = context.getContentResolver().query(
                Lists.CONTENT_URI, new String[]{Items._ID},
                "upper(name) = upper(?)", new String[]{name}, null);
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
            existingItems.close();
        } else {
            // Add list to list:
            ContentValues values = new ContentValues(1);
            values.put(Lists.NAME, name);
            try {
                Uri uri = context.getContentResolver().insert(
                        Lists.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new list: " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.e(TAG, "insert list failed", e);
                return -1;
            }
        }
        return id;
    }

    /**
     * Gets or creates a new store and returns its id. If the store exists
     * already, the existing id is returned. Otherwise a new store is created.
     *
     * @param context
     * @param name    New name of the list.
     * @return id of the new or existing list.
     */
    public static long getStore(Context context, final String name,
                                final long listId) {
        long id = -1;
        Cursor existingItems = context.getContentResolver().query(
                Stores.CONTENT_URI, new String[]{Stores._ID},
                "upper(name) = upper(?) AND list_id = ?",
                new String[]{name, String.valueOf(listId)}, null);
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
            existingItems.close();
        } else {
            // Add list to list:
            ContentValues values = new ContentValues(1);
            values.put(Stores.NAME, name);
            values.put(Stores.LIST_ID, listId);
            try {
                Uri uri = context.getContentResolver().insert(
                        Stores.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new store: " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.e(TAG, "insert store failed", e);
                return -1;
            }
        }
        return id;
    }

    /**
     * Adds a new item to a specific list and returns its id. If the item exists
     * already, the existing id is returned.
     *
     * @param itemId       The id of the new item.
     * @param listId       The id of the shopping list the item is added.
     * @param status       The status of the new item
     * @param priority     The priority of the new item
     * @param quantity     The quantity of the new item
     * @param togglestatus If true, then status is toggled between WANT_TO_BUY and BOUGHT
     * @return id of the "contains" table entry, or -1 if insert failed.
     */
    public static long addItemToList(Context context, final long itemId,
                                     final long listId, final long status, String priority,
                                     String quantity, final boolean togglestatus,
                                     final boolean known_new, final boolean resetQuantity) {
        long id = -1;
        Cursor existingItems = null;

        if (!known_new) {
            existingItems = context.getContentResolver()
                    .query(Contains.CONTENT_URI,
                            new String[]{Contains._ID, Contains.STATUS},
                            "list_id = ? AND item_id = ?",
                            new String[]{String.valueOf(listId),
                                    String.valueOf(itemId)}, null
                    );
        }
        if (existingItems != null && existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
            long oldstatus = existingItems.getLong(1);
            existingItems.close();

            long newstatus = Status.WANT_TO_BUY;
            // Toggle status:
            if (oldstatus == Status.WANT_TO_BUY) {
                newstatus = Status.BOUGHT;
            }

            // set status to want_to_buy:
            ContentValues values = new ContentValues(3);
            if (togglestatus) {
                values.put(Contains.STATUS, newstatus);
            } else {
                values.put(Contains.STATUS, status);
            }
            if (quantity != null) {
                // Only change quantity if an explicit value has been passed.
                // (see issue 286)
                values.put(ShoppingContract.Contains.QUANTITY, quantity);
            } else {
                if (resetQuantity) {
                    values.put(ShoppingContract.Contains.QUANTITY, "");
                }
            }
            if (priority != null) {
                values.put(ShoppingContract.Contains.PRIORITY, priority);
            }

            Uri uri = Uri.withAppendedPath(Contains.CONTENT_URI,
                    String.valueOf(id));
            try {
                context.getContentResolver().update(uri, values, null, null);
                if (debug) {
                    Log.d(TAG, "updated item: " + uri);
                }
            } catch (Exception e) {
                try {
                    // Maybe old version of OI Shopping List is installed:
                    values.remove(Contains.PRIORITY);
                    context.getContentResolver()
                            .update(uri, values, null, null);
                    if (debug) {
                        Log.d(TAG, "updated item: " + uri);
                    }
                } catch (Exception e2) {
                    Log.e(TAG, "insert into table 'contains' failed", e2);
                    id = -1;
                }
            }

        } else {
            if (existingItems != null) {
                existingItems.close();
            }
            // Add item to list:
            ContentValues values = new ContentValues(2);
            values.put(Contains.ITEM_ID, itemId);
            values.put(Contains.LIST_ID, listId);
            if (togglestatus) {
                values.put(Contains.STATUS, Status.WANT_TO_BUY);
            } else {
                values.put(Contains.STATUS, status);
            }
            if (quantity != null) {
                values.put(Contains.QUANTITY, quantity);
            }
            if (priority != null) {
                values.put(Contains.PRIORITY, priority);
            }

            try {
                Uri uri = context.getContentResolver().insert(
                        Contains.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new entry in 'contains': " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                try {
                    // Maybe old version of OI Shopping List is installed:
                    values.remove(Contains.PRIORITY);
                    Uri uri = context.getContentResolver().insert(
                            Contains.CONTENT_URI, values);
                    if (debug) {
                        Log.d(TAG, "Insert new entry in 'contains': " + uri);
                    }
                    id = Long.parseLong(uri.getPathSegments().get(1));
                } catch (Exception e2) {
                    Log.e(TAG, "insert into table 'contains' failed", e2);
                    id = -1;
                }
            }
        }
        return id;
    }

    /**
     * Adds an item to a specific store and returns its id. If the item exists
     * already, the existing id is returned.
     *
     * @param itemId     The id of the new item.
     * @param storeId    The id of the shopping list the item is added.
     * @param stocksItem The type of the new item
     * @return id of the "contains" table entry, or -1 if insert failed.
     */
    public static long addItemToStore(Context context, final long itemId,
                                      final long storeId, final boolean stocksItem, final String aisle,
                                      final String price, boolean known_new) {
        long id = -1;
        Cursor existingItems = null;

        if (!known_new) {
            existingItems = context.getContentResolver()
                    .query(ItemStores.CONTENT_URI,
                            new String[]{ItemStores._ID},
                            "store_id = ? AND item_id = ?",
                            new String[]{String.valueOf(storeId),
                                    String.valueOf(itemId)}, null
                    );
        }
        if (existingItems != null && existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
            existingItems.close();

            // update aisle and price:
            ContentValues values = new ContentValues(3);
            if (!TextUtils.isEmpty(price))
                values.put(ItemStores.PRICE, price);
            if (!TextUtils.isEmpty(aisle))
                values.put(ItemStores.AISLE, aisle);
            values.put(ItemStores.STOCKS_ITEM, stocksItem);
            try {
                Uri uri = Uri.withAppendedPath(ItemStores.CONTENT_URI,
                        String.valueOf(id));
                context.getContentResolver().update(uri, values, null, null);
                if (debug) {
                    Log.d(TAG, "updated itemstore: " + uri);
                }
            } catch (Exception e) {
                Log.e(TAG, "Update itemstore failed", e);
            }

        } else {
            if (existingItems != null) {
                existingItems.close();
            }
            // Add item to list:
            ContentValues values = new ContentValues(5);
            values.put(ItemStores.ITEM_ID, itemId);
            values.put(ItemStores.STORE_ID, storeId);
            values.put(ItemStores.PRICE, price);
            values.put(ItemStores.AISLE, aisle);
            values.put(ItemStores.STOCKS_ITEM, stocksItem);
            try {
                Uri uri = context.getContentResolver().insert(
                        ItemStores.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new entry in 'itemstores': " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.e(TAG, "insert into table 'itemstores' failed", e);
                id = -1;
            }
        }
        return id;
    }

    /**
     * Adds an item to a specific store and returns its id. If the item exists
     * already, the existing id is returned.
     *
     * @param itemId   The id of the new item.
     * @param storeId  The id of the store to which the item is added.
     * @param aisle    The aisle in which the item can be found at this store.
     *                 Can be null.
     * @param price    The price of the item at this store.
     *                 Can be null.
     * @param known_new true if the caller knows the item is not yet in the table for this store.
     * @return id of the "contains" table entry, or -1 if insert failed.
     */
    public static long addItemToStore(Context context, final long itemId,
                                      final long storeId, final String aisle, final String price, boolean known_new) {
        return addItemToStore(context, itemId, storeId, true, aisle, price, known_new);
    }

    /**
     * Returns the id of the default shopping list. Currently this is always 1.
     *
     * @return The id of the default shopping list.
     */
    public static long getDefaultList(Context context) {
        long id = 1;
        try {
            Cursor c = context.getContentResolver().query(
                    ActiveList.CONTENT_URI, ActiveList.PROJECTION, null, null,
                    null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                id = c.getLong(0);
                c.close();
            }
        } catch (IllegalArgumentException e) {
            // The URI has not been defined.
            // The URI requires OI Shopping List 1.3.0 or higher.
            // Most probably we want to access OI Shopping List 1.2.6 or
            // earlier.
            Log.e(TAG, "ActiveList URI not supported", e);
        }
        return id;
    }

    public static Uri getListForItem(Context context, String itemId) {
        Cursor cursor = context.getContentResolver().query(
                Contains.CONTENT_URI, new String[]{Contains.LIST_ID},
                Contains.ITEM_ID + " = ?", new String[]{itemId},
                Contains.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            Uri uri;
            if (cursor.moveToFirst()) {

                uri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                        cursor.getString(0));

            } else {
                uri = null;
            }
            cursor.close();
            return uri;
        } else {
            return null;
        }
    }

    public static void addTagToItem(Context context, long itemId, String newTag) {
        String allTags = "";
        Cursor existingTags = context.getContentResolver().query(
                Items.CONTENT_URI, new String[]{Items.TAGS}, "_id = ?",
                new String[]{String.valueOf(itemId)}, null);
        if (existingTags.getCount() > 0) {
            existingTags.moveToFirst();
            allTags = existingTags.getString(0);
            existingTags.close();
        }

        if (!TextUtils.isEmpty(allTags)) {
            if (allTags.equals(newTag))
                return;
            if (allTags.startsWith(newTag + ","))
                return;
            if (allTags.contains(", " + newTag))
                return;
            allTags = allTags + ", " + newTag;
        } else {
            allTags = newTag;
        }

        ContentValues values = new ContentValues(1);
        values.put(Items.TAGS, allTags);

        context.getContentResolver()
                .update(Uri.withAppendedPath(Items.CONTENT_URI,
                        String.valueOf(itemId)), values, null, null);
    }

    /**
     * Cleanly deletes an item from a list (from the Contains table), but the
     * item itself remains. Afterwards, either the item should be moved to
     * another list, or the item should be deleted. Deletion includes itemstores
     * and contains.
     *
     * @param context
     * @param itemId
     * @return 1 if the item got deleted, 0 otherwise.
     */
    public static int deleteItemFromList(Context context, String itemId,
                                         String listId) {
        // First delete all itemstores for item
        List<String> itemStoreIds = getItemStoreIdsForList(context, itemId,
                listId);
        for (String itemStoreId : itemStoreIds) {
            context.getContentResolver().delete(ItemStores.CONTENT_URI,
                    "itemstores._id = " + itemStoreId, null);
        }

        // Delete item from currentList by deleting contains row
        return context.getContentResolver().delete(
                Contains.CONTENT_URI, "item_id = ? and list_id = ?",
                new String[]{itemId, listId});
    }

    /**
     * Cleanly deletes an item from a particular list if it does not exist on
     * any other list. Deletion includes itemstores and the item itself.
     *
     * @param context
     * @param itemId
     * @return 1 if the item got deleted, 0 otherwise.
     */
    public static int deleteItem(Context context, String itemId, String listId) {
        deleteItemFromList(context, itemId, listId);

        int itemsDeleted = 0;
        if (!isItemContainedInOtherExistingList(context, itemId)) {
            // Delete the item itself if it is not contained in an existing list
            // anymore
            itemsDeleted = context.getContentResolver().delete(
                    Items.CONTENT_URI, "_id = ?", new String[]{itemId});
        }

        return itemsDeleted;
    }

    /**
     * Returns true if the item is contained in an existing list. Extra care is
     * taken because old contains could be left over from lists that do not
     * exist anymore.
     *
     * @param context
     * @param itemId
     * @return
     */
    private static boolean isItemContainedInOtherExistingList(Context context,
                                                              String itemId) {
        Cursor c = context.getContentResolver().query(Contains.CONTENT_URI,
                new String[]{Contains.LIST_ID}, Contains.ITEM_ID + " = ?",
                new String[]{itemId}, null);
        if (c != null) {
            while (c.moveToNext()) {
                // Item is contained in some list...
                String listId = c.getString(0);
                Cursor c2 = context.getContentResolver().query(
                        Lists.CONTENT_URI, new String[]{Lists._ID},
                        Lists._ID + " = ?", new String[]{listId}, null);
                if (c2 != null) {
                    if (c2.moveToNext()) {
                        // ... and that list exists
                        c2.close();
                        c.close();
                        return true;
                    }
                    c2.close();
                }

            }
            c.close();
        }
        return false;
    }

    /**
     * Cleanly deletes a store. Deletion includes itemstores and the store
     * itself.
     *
     * @param context
     * @param storeId
     * @return 1 if the store got deleted, 0 otherwise.
     */
    public static int deleteStore(Context context, String storeId) {
        // First delete all items for store
        context.getContentResolver().delete(ItemStores.CONTENT_URI,
                "store_id = " + storeId, null);

        // Then delete currently selected store
        return context.getContentResolver().delete(
                Stores.CONTENT_URI, "_id = " + storeId, null);
    }

    /**
     * Cleanly deletes a list. Deletion includes stores, itemstores, items, and
     * the list itself.
     *
     * @param context
     * @param listId
     * @return 1 if the list got deleted, 0 otherwise.
     */
    public static int deleteList(Context context, String listId) {
        // Delete all items
        List<String> itemIds = getItemIdsForList(context, listId);
        for (String itemId : itemIds) {
            deleteItem(context, itemId, listId);
        }

        // Delete all stores
        List<String> storeIds = getStoreIdsForList(context, listId);
        for (String storeId : storeIds) {
            deleteStore(context, storeId);
        }

        // Then delete currently selected list
        return context.getContentResolver().delete(
                Lists.CONTENT_URI, "_id = " + listId, null);
    }

    private static List<String> getItemStoreIdsForList(Context context,
                                                       String itemId, String listId) {
        // Get a cursor for all stores
        Cursor c = context.getContentResolver().query(
                ItemStores.CONTENT_URI.buildUpon().appendPath("item")
                        .appendPath(itemId).appendPath(listId).build(),
                new String[]{"itemstores._id"}, null, null, null
        );
        return getStringListAndCloseCursor(c, 0);
    }

    private static List<String> getItemIdsForList(Context context, String listId) {
        Cursor c = context.getContentResolver().query(Contains.CONTENT_URI,
                new String[]{Contains.ITEM_ID}, Contains.LIST_ID + " = ?",
                new String[]{listId}, null);
        return getStringListAndCloseCursor(c, 0);
    }

    private static List<String> getStoreIdsForList(Context context,
                                                   String listId) {
        Cursor c = context.getContentResolver().query(Stores.CONTENT_URI,
                new String[]{Stores._ID}, Stores.LIST_ID + " = ?",
                new String[]{listId}, null);
        return getStringListAndCloseCursor(c, 0);
    }

    private static List<String> getStringListAndCloseCursor(Cursor c, int index) {
        List<String> items = new LinkedList<String>();
        if (c != null) {
            while (c.moveToNext()) {
                String item = c.getString(index);
                items.add(item);
            }
            c.close();
        }
        return items;
    }

    private static String getListFilterStoreId(Context context, Uri list_uri) {
        String store_id = null;
        Cursor c = context.getContentResolver().query(list_uri,
                new String[]{Lists.STORE_FILTER}, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            store_id = c.getString(0);
            c.deactivate();
            c.close();
        }
        return store_id;
    }

    public static String getListFilterStoreName(Context context, Uri list_uri) {
        String filter = null;
        String store_id = getListFilterStoreId(context, list_uri);

        if (store_id != null && store_id.length() > 0) {
            Cursor c = context.getContentResolver().query(Stores.CONTENT_URI,
                    new String[]{Stores.NAME}, "_id = ?", new String[]{store_id}, null);
            if (c != null) {
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    filter = c.getString(0);
                }
                c.deactivate();
                c.close();
            }
        }

        return filter;
    }

    public static String getListTagsFilter(Context context, Uri list_uri) {
        String filter = null;
        Cursor c = context.getContentResolver().query(list_uri,
                new String[]{Lists.TAGS_FILTER}, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            filter = c.getString(0);
            c.deactivate();
            c.close();

            if (filter != null && filter.length() == 0) {
                filter = null;
            }
        }
        return filter;
    }

    public static void addDefaultsToAddedItem(Context context, long list_id, long item_id) {
        Uri list_uri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                Long.toString(list_id));
        String tagsFilter = getListTagsFilter(context, list_uri);
        String storeId = getListFilterStoreId(context, list_uri);
        boolean hasTagsFilter = !TextUtils.isEmpty(tagsFilter);
        boolean hasStoreIdFilter = !TextUtils.isEmpty(storeId);

        if (hasStoreIdFilter) {
            addItemToStore(context, item_id, Long.parseLong(storeId), true, null, null, false);
        }

        if (hasTagsFilter) {
            addTagToItem(context, item_id, tagsFilter);
        }
    }

    public static String getListSortOrder(Context context, long list_id) {
        String sort = null;
        Uri list_uri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                Long.toString(list_id));
        Cursor c = context.getContentResolver().query(list_uri,
                new String[]{Lists.ITEMS_SORT}, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            sort = c.getString(0);
            c.deactivate();
            c.close();

            if (sort != null && sort.length() == 0) {
                sort = null;
            }
        }
        return sort;
    }

}
