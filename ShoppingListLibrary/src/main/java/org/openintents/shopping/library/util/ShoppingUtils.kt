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

package org.openintents.shopping.library.util

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.text.TextUtils
import android.util.Log

import java.util.LinkedList

import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.ActiveList
import org.openintents.shopping.library.provider.ShoppingContract.Contains
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull
import org.openintents.shopping.library.provider.ShoppingContract.ItemStores
import org.openintents.shopping.library.provider.ShoppingContract.Items
import org.openintents.shopping.library.provider.ShoppingContract.Lists
import org.openintents.shopping.library.provider.ShoppingContract.Status
import org.openintents.shopping.library.provider.ShoppingContract.Stores
import org.openintents.shopping.library.provider.ShoppingContract.Units

object ShoppingUtils {
    /**
     * TAG for logging.
     */
    private const val TAG = "ShoppingUtils"
    private const val debug = false

    /**
     * Obtain item id by name.
     *
     * @param context
     * @param name
     * @return Item ID or -1 if item does not exist.
     */
    @JvmStatic
    fun getItemId(context: Context, name: String): Long {
        var id: Long = -1

        val existingItems = context.contentResolver.query(
                Items.CONTENT_URI, arrayOf(Items._ID),
                "upper(name) = upper(?)", arrayOf(name), null)
        if (existingItems!!.count > 0) {
            existingItems.moveToFirst()
            id = existingItems.getLong(0)
        }

        existingItems.close()
        return id
    }

    @JvmStatic
    fun getItemIdForList(context: Context, name: String, list_id: String): Long {
        var id: Long = -1

        val existingItems = context.contentResolver.query(
                ContainsFull.CONTENT_URI, arrayOf(Contains.ITEM_ID),
                "list_id = ? and upper(items.name) = upper(?)", arrayOf(list_id, name), null)
        if (existingItems!!.count > 0) {
            existingItems.moveToFirst()
            id = existingItems.getLong(0)
        }
        existingItems.close()
        return id
    }

    @JvmStatic
    fun getItemName(context: Context, itemId: Long): String {
        var name = ""
        val existingItems = context.contentResolver.query(
                ShoppingContract.Items.CONTENT_URI,
                arrayOf(ShoppingContract.Items.NAME), "_id = ?",
                arrayOf(itemId.toString()), null)
        if (existingItems!!.count > 0) {
            existingItems.moveToFirst()
            name = existingItems.getString(0) ?: ""
        }
        existingItems.close()
        return name
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
    @JvmStatic
    fun updateOrCreateItem(context: Context, name: String,
                           tags: String?, price: String?, barcode: String?, list_id: String?): Long {
        var id: Long

        if (list_id == null) {
            id = getItemId(context, name)
        } else {
            id = getItemIdForList(context, name, list_id)
        }

        if (id >= 0) {
            // Update existing item
            // (pass 'null' for name: Existing item: no need to change name.)
            val values = getContentValues(name, tags, price, barcode)
            try {
                val uri = Uri.withAppendedPath(
                        ShoppingContract.Items.CONTENT_URI, id.toString())
                context.contentResolver.update(uri, values, null, null)
                if (debug) {
                    Log.d(TAG, "updated item: " + uri)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update item failed", e)
            }
        }

        if (id == -1L) {
            // Add new item to list.
            val values = getContentValues(name, tags, price, barcode)
            try {
                val uri = context.contentResolver.insert(
                        ShoppingContract.Items.CONTENT_URI, values)
                if (debug) {
                    Log.d(TAG, "Insert new item: " + uri)
                }
                id = java.lang.Long.parseLong(uri!!.pathSegments[1])
            } catch (e: Exception) {
                Log.e(TAG, "Insert item failed", e)
                // return -1
            }
        }
        return id

    }

    private fun getContentValues(name: String?, tags: String?,
                                 price: String?, barcode: String?): ContentValues {
        val values = ContentValues(4)
        if (name != null) {
            values.put(ShoppingContract.Items.NAME, name)
        }
        if (tags != null) {
            values.put(ShoppingContract.Items.TAGS, tags)
        }
        if (price != null) {
            val priceLong = PriceConverter.getCentPriceFromString(price)
            values.put(ShoppingContract.Items.PRICE, priceLong)
        }
        if (barcode != null) {
            values.put(ShoppingContract.Items.BARCODE, barcode)
        }
        return values
    }

    /**
     * Gets or creates a new item and returns its id. If the item exists
     * already, the existing id is returned. Otherwise a new item is created.
     *
     * @param name New name of the item.
     * @return id of the new or existing item.
     */
    @JvmStatic
    fun getItem(context: Context, name: String, tags: String?,
                price: String?, units: String?, note: String?, duplicate: Boolean,
                update: Boolean): Long {
        var id: Long = -1

        if (!duplicate) {
            if (id == -1L) {
                id = getItemId(context, name)
            }

            if (id != -1L && !update) {
                return id
            }
        }

        return getItem(context, id, name, tags, price, units, note)
    }

    /**
     * Gets or creates a new item and returns its id. If the item exists
     * already, the existing id is returned. Otherwise a new item is created.
     *
     * @param id   id of the item to update, or -1 to create a new item.
     * @param name New name of the item.
     * @return id of the new or existing item.
     */
    @JvmStatic
    fun getItem(context: Context, id: Long, name: String?,
                tags: String?, price: String?, units: String?, note: String?): Long {
        var id = id

        // now we are either updating or adding.
        // either way we need some content values.
        // Add item to list:
        val values = ContentValues(1)
        if (id == -1L) {
            values.put(Items.NAME, name)
        }

        values.put(Items.TAGS, tags)
        if (!TextUtils.isEmpty(note)) {
            values.put(Items.NOTE, note)
        }
        if (price != null) {
            values.put(Items.PRICE, price)
        }
        if (!TextUtils.isEmpty(units)) {
            // in the items table we store the string directly,
            // but we register the units in the units table for use in
            // completion.
            val unit_id = getUnits(context, units)
            values.put(Items.UNITS, units)
        }

        try {
            if (id == -1L) {
                val uri = context.contentResolver.insert(
                        Items.CONTENT_URI, values)
                if (debug) {
                    Log.d(TAG, "Insert new item: " + uri)
                }
                id = java.lang.Long.parseLong(uri!!.pathSegments[1])
            } else {
                context.contentResolver.update(
                        Uri.withAppendedPath(Items.CONTENT_URI,
                                id.toString()), values, null, null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Insert item failed", e)
            // return -1
        }

        return id
    }

    @JvmStatic
    fun getUnits(context: Context, units: String?): Long {
        var id: Long = -1
        val existingUnits = context.contentResolver.query(
                Units.CONTENT_URI, arrayOf(Units._ID),
                "upper(name) = upper(?)", arrayOf(units), null)
        if (existingUnits!!.count > 0) {
            existingUnits.moveToFirst()
            id = existingUnits.getLong(0)
            existingUnits.close()

        } else {
            existingUnits.close()
            // Add item to list:
            val values = ContentValues(1)
            values.put(Units.NAME, units)
            try {
                val uri = context.contentResolver.insert(
                        Units.CONTENT_URI, values)
                if (debug) {
                    Log.d(TAG, "Insert new units: " + uri)
                }
                id = java.lang.Long.parseLong(uri!!.pathSegments[1])
            } catch (e: Exception) {
                Log.e(TAG, "Insert units failed", e)
                // return -1
            }
        }
        return id
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
    @JvmStatic
    fun getList(context: Context, name: String): Long {
        var id: Long = -1
        val existingItems = context.contentResolver.query(
                Lists.CONTENT_URI, arrayOf(Items._ID),
                "upper(name) = upper(?)", arrayOf(name), null)
        if (existingItems!!.count > 0) {
            existingItems.moveToFirst()
            id = existingItems.getLong(0)
            existingItems.close()
        } else {
            // Add list to list:
            val values = ContentValues(1)
            values.put(Lists.NAME, name)
            try {
                val uri = context.contentResolver.insert(
                        Lists.CONTENT_URI, values)
                if (debug) {
                    Log.d(TAG, "Insert new list: " + uri)
                }
                id = java.lang.Long.parseLong(uri!!.pathSegments[1])
            } catch (e: Exception) {
                Log.e(TAG, "insert list failed", e)
                return -1
            }
        }
        return id
    }

    /**
     * Gets or creates a new store and returns its id. If the store exists
     * already, the existing id is returned. Otherwise a new store is created.
     *
     * @param context
     * @param name    New name of the list.
     * @return id of the new or existing list.
     */
    @JvmStatic
    fun getStore(context: Context, name: String,
                 listId: Long): Long {
        var id: Long = -1
        val existingItems = context.contentResolver.query(
                Stores.CONTENT_URI, arrayOf(Stores._ID),
                "upper(name) = upper(?) AND list_id = ?",
                arrayOf(name, listId.toString()), null)
        if (existingItems!!.count > 0) {
            existingItems.moveToFirst()
            id = existingItems.getLong(0)
            existingItems.close()
        } else {
            // Add list to list:
            val values = ContentValues(1)
            values.put(Stores.NAME, name)
            values.put(Stores.LIST_ID, listId)
            try {
                val uri = context.contentResolver.insert(
                        Stores.CONTENT_URI, values)
                if (debug) {
                    Log.d(TAG, "Insert new store: " + uri)
                }
                id = java.lang.Long.parseLong(uri!!.pathSegments[1])
            } catch (e: Exception) {
                Log.e(TAG, "insert store failed", e)
                return -1
            }
        }
        return id
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
    @JvmStatic
    fun addItemToList(context: Context, itemId: Long,
                      listId: Long, status: Long, priority: String?,
                      quantity: String?, togglestatus: Boolean,
                      known_new: Boolean, resetQuantity: Boolean): Long {
        var id: Long = -1
        var existingItems: Cursor? = null

        if (!known_new) {
            existingItems = context.contentResolver
                    .query(Contains.CONTENT_URI,
                            arrayOf(Contains._ID, Contains.STATUS),
                            "list_id = ? AND item_id = ?",
                            arrayOf(listId.toString(),
                                    itemId.toString()), null
                    )
        }
        if (existingItems != null && existingItems.count > 0) {
            existingItems.moveToFirst()
            id = existingItems.getLong(0)
            val oldstatus = existingItems.getLong(1)
            existingItems.close()

            var newstatus = Status.WANT_TO_BUY
            // Toggle status:
            if (oldstatus == Status.WANT_TO_BUY) {
                newstatus = Status.BOUGHT
            }

            // set status to want_to_buy:
            val values = ContentValues(3)
            if (togglestatus) {
                values.put(Contains.STATUS, newstatus)
            } else {
                values.put(Contains.STATUS, status)
            }
            if (quantity != null) {
                // Only change quantity if an explicit value has been passed.
                // (see issue 286)
                values.put(ShoppingContract.Contains.QUANTITY, quantity)
            } else {
                if (resetQuantity) {
                    values.put(ShoppingContract.Contains.QUANTITY, "")
                }
            }
            if (priority != null) {
                values.put(ShoppingContract.Contains.PRIORITY, priority)
            }

            val uri = Uri.withAppendedPath(Contains.CONTENT_URI,
                    id.toString())
            try {
                context.contentResolver.update(uri, values, null, null)
                if (debug) {
                    Log.d(TAG, "updated item: " + uri)
                }
            } catch (e: Exception) {
                try {
                    // Maybe old version of OI Shopping List is installed:
                    values.remove(Contains.PRIORITY)
                    context.contentResolver
                            .update(uri, values, null, null)
                    if (debug) {
                        Log.d(TAG, "updated item: " + uri)
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "insert into table 'contains' failed", e2)
                    id = -1
                }
            }

        } else {
            if (existingItems != null) {
                existingItems.close()
            }
            // Add item to list:
            val values = ContentValues(2)
            values.put(Contains.ITEM_ID, itemId)
            values.put(Contains.LIST_ID, listId)
            if (togglestatus) {
                values.put(Contains.STATUS, Status.WANT_TO_BUY)
            } else {
                values.put(Contains.STATUS, status)
            }
            if (quantity != null) {
                values.put(Contains.QUANTITY, quantity)
            }
            if (priority != null) {
                values.put(Contains.PRIORITY, priority)
            }

            try {
                val uri = context.contentResolver.insert(
                        Contains.CONTENT_URI, values)
                if (debug) {
                    Log.d(TAG, "Insert new entry in 'contains': " + uri)
                }
                id = java.lang.Long.parseLong(uri!!.pathSegments[1])
            } catch (e: Exception) {
                try {
                    // Maybe old version of OI Shopping List is installed:
                    values.remove(Contains.PRIORITY)
                    val uri = context.contentResolver.insert(
                            Contains.CONTENT_URI, values)
                    if (debug) {
                        Log.d(TAG, "Insert new entry in 'contains': " + uri)
                    }
                    id = java.lang.Long.parseLong(uri!!.pathSegments[1])
                } catch (e2: Exception) {
                    Log.e(TAG, "insert into table 'contains' failed", e2)
                    id = -1
                }
            }
        }
        return id
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
    @JvmStatic
    fun addItemToStore(context: Context, itemId: Long,
                       storeId: Long, stocksItem: Boolean, aisle: String?,
                       price: String?, known_new: Boolean): Long {
        var id: Long = -1
        var existingItems: Cursor? = null

        if (!known_new) {
            existingItems = context.contentResolver
                    .query(ItemStores.CONTENT_URI,
                            arrayOf(ItemStores._ID),
                            "store_id = ? AND item_id = ?",
                            arrayOf(storeId.toString(),
                                    itemId.toString()), null
                    )
        }
        if (existingItems != null && existingItems.count > 0) {
            existingItems.moveToFirst()
            id = existingItems.getLong(0)
            existingItems.close()

            // update aisle and price:
            val values = ContentValues(3)
            if (!TextUtils.isEmpty(price))
                values.put(ItemStores.PRICE, price)
            if (!TextUtils.isEmpty(aisle))
                values.put(ItemStores.AISLE, aisle)
            values.put(ItemStores.STOCKS_ITEM, stocksItem)
            try {
                val uri = Uri.withAppendedPath(ItemStores.CONTENT_URI,
                        id.toString())
                context.contentResolver.update(uri, values, null, null)
                if (debug) {
                    Log.d(TAG, "updated itemstore: " + uri)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update itemstore failed", e)
            }

        } else {
            if (existingItems != null) {
                existingItems.close()
            }
            // Add item to list:
            val values = ContentValues(5)
            values.put(ItemStores.ITEM_ID, itemId)
            values.put(ItemStores.STORE_ID, storeId)
            values.put(ItemStores.PRICE, price)
            values.put(ItemStores.AISLE, aisle)
            values.put(ItemStores.STOCKS_ITEM, stocksItem)
            try {
                val uri = context.contentResolver.insert(
                        ItemStores.CONTENT_URI, values)
                if (debug) {
                    Log.d(TAG, "Insert new entry in 'itemstores': " + uri)
                }
                id = java.lang.Long.parseLong(uri!!.pathSegments[1])
            } catch (e: Exception) {
                Log.e(TAG, "insert into table 'itemstores' failed", e)
                id = -1
            }
        }
        return id
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
    @JvmStatic
    fun addItemToStore(context: Context, itemId: Long,
                       storeId: Long, aisle: String?, price: String?, known_new: Boolean): Long {
        return addItemToStore(context, itemId, storeId, true, aisle, price, known_new)
    }

    /**
     * Returns the id of the default shopping list. Currently this is always 1.
     *
     * @return The id of the default shopping list.
     */
    @JvmStatic
    fun getDefaultList(context: Context): Long {
        var id: Long = 1
        try {
            val c = context.contentResolver.query(
                    ActiveList.CONTENT_URI, ActiveList.PROJECTION, null, null,
                    null)
            if (c!!.count > 0) {
                c.moveToFirst()
                id = c.getLong(0)
                c.close()
            }
        } catch (e: IllegalArgumentException) {
            // The URI has not been defined.
            // The URI requires OI Shopping List 1.3.0 or higher.
            // Most probably we want to access OI Shopping List 1.2.6 or
            // earlier.
            Log.e(TAG, "ActiveList URI not supported", e)
        }
        return id
    }

    @JvmStatic
    fun getListForItem(context: Context, itemId: String): Uri? {
        val cursor = context.contentResolver.query(
                Contains.CONTENT_URI, arrayOf(Contains.LIST_ID),
                Contains.ITEM_ID + " = ?", arrayOf(itemId),
                Contains.DEFAULT_SORT_ORDER)
        if (cursor != null) {
            val uri: Uri?
            if (cursor.moveToFirst()) {

                uri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                        cursor.getString(0))

            } else {
                uri = null
            }
            cursor.close()
            return uri
        } else {
            return null
        }
    }

    @JvmStatic
    fun addTagToItem(context: Context, itemId: Long, newTag: String) {
        var allTags: String? = ""
        val existingTags = context.contentResolver.query(
                Items.CONTENT_URI, arrayOf(Items.TAGS), "_id = ?",
                arrayOf(itemId.toString()), null)
        if (existingTags!!.count > 0) {
            existingTags.moveToFirst()
            allTags = existingTags.getString(0)
            existingTags.close()
        }

        if (!TextUtils.isEmpty(allTags)) {
            if (allTags == newTag)
                return
            if (allTags!!.startsWith(newTag + ","))
                return
            if (allTags.contains(", " + newTag))
                return
            allTags = allTags + ", " + newTag
        } else {
            allTags = newTag
        }

        val values = ContentValues(1)
        values.put(Items.TAGS, allTags)

        context.contentResolver
                .update(Uri.withAppendedPath(Items.CONTENT_URI,
                        itemId.toString()), values, null, null)
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
    @JvmStatic
    fun deleteItemFromList(context: Context, itemId: String,
                           listId: String): Int {
        // First delete all itemstores for item
        val itemStoreIds = getItemStoreIdsForList(context, itemId,
                listId)
        for (itemStoreId in itemStoreIds) {
            context.contentResolver.delete(ItemStores.CONTENT_URI,
                    "itemstores._id = " + itemStoreId, null)
        }

        // Delete item from currentList by deleting contains row
        return context.contentResolver.delete(
                Contains.CONTENT_URI, "item_id = ? and list_id = ?",
                arrayOf(itemId, listId))
    }

    /**
     * Cleanly deletes an item from a particular list if it does not exist on
     * any other list. Deletion includes itemstores and the item itself.
     *
     * @param context
     * @param itemId
     * @return 1 if the item got deleted, 0 otherwise.
     */
    @JvmStatic
    fun deleteItem(context: Context, itemId: String, listId: String): Int {
        deleteItemFromList(context, itemId, listId)

        var itemsDeleted = 0
        if (!isItemContainedInOtherExistingList(context, itemId)) {
            // Delete the item itself if it is not contained in an existing list
            // anymore
            itemsDeleted = context.contentResolver.delete(
                    Items.CONTENT_URI, "_id = ?", arrayOf(itemId))
        }

        return itemsDeleted
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
    private fun isItemContainedInOtherExistingList(context: Context,
                                                   itemId: String): Boolean {
        val c = context.contentResolver.query(Contains.CONTENT_URI,
                arrayOf(Contains.LIST_ID), Contains.ITEM_ID + " = ?",
                arrayOf(itemId), null)
        if (c != null) {
            while (c.moveToNext()) {
                // Item is contained in some list...
                val listId = c.getString(0)
                val c2 = context.contentResolver.query(
                        Lists.CONTENT_URI, arrayOf(Lists._ID),
                        Lists._ID + " = ?", arrayOf(listId), null)
                if (c2 != null) {
                    if (c2.moveToNext()) {
                        // ... and that list exists
                        c2.close()
                        c.close()
                        return true
                    }
                    c2.close()
                }

            }
            c.close()
        }
        return false
    }

    /**
     * Cleanly deletes a store. Deletion includes itemstores and the store
     * itself.
     *
     * @param context
     * @param storeId
     * @return 1 if the store got deleted, 0 otherwise.
     */
    @JvmStatic
    fun deleteStore(context: Context, storeId: String): Int {
        // First delete all items for store
        context.contentResolver.delete(ItemStores.CONTENT_URI,
                "store_id = " + storeId, null)

        // Then delete currently selected store
        return context.contentResolver.delete(
                Stores.CONTENT_URI, "_id = " + storeId, null)
    }

    /**
     * Cleanly deletes a list. Deletion includes stores, itemstores, items, and
     * the list itself.
     *
     * @param context
     * @param listId
     * @return 1 if the list got deleted, 0 otherwise.
     */
    @JvmStatic
    fun deleteList(context: Context, listId: String): Int {
        // Delete all items
        val itemIds = getItemIdsForList(context, listId)
        for (itemId in itemIds) {
            deleteItem(context, itemId, listId)
        }

        // Delete all stores
        val storeIds = getStoreIdsForList(context, listId)
        for (storeId in storeIds) {
            deleteStore(context, storeId)
        }

        // Then delete currently selected list
        return context.contentResolver.delete(
                Lists.CONTENT_URI, "_id = " + listId, null)
    }

    private fun getItemStoreIdsForList(context: Context,
                                       itemId: String, listId: String): List<String> {
        // Get a cursor for all stores
        val c = context.contentResolver.query(
                ItemStores.CONTENT_URI.buildUpon().appendPath("item")
                        .appendPath(itemId).appendPath(listId).build(),
                arrayOf("itemstores._id"), null, null, null
        )
        return getStringListAndCloseCursor(c, 0)
    }

    private fun getItemIdsForList(context: Context, listId: String): List<String> {
        val c = context.contentResolver.query(Contains.CONTENT_URI,
                arrayOf(Contains.ITEM_ID), Contains.LIST_ID + " = ?",
                arrayOf(listId), null)
        return getStringListAndCloseCursor(c, 0)
    }

    private fun getStoreIdsForList(context: Context,
                                   listId: String): List<String> {
        val c = context.contentResolver.query(Stores.CONTENT_URI,
                arrayOf(Stores._ID), Stores.LIST_ID + " = ?",
                arrayOf(listId), null)
        return getStringListAndCloseCursor(c, 0)
    }

    private fun getStringListAndCloseCursor(c: Cursor?, index: Int): List<String> {
        val items = LinkedList<String>()
        if (c != null) {
            while (c.moveToNext()) {
                val item = c.getString(index)
                items.add(item)
            }
            c.close()
        }
        return items
    }

    private fun getListFilterStoreId(context: Context, list_uri: Uri): String? {
        var store_id: String? = null
        val c = context.contentResolver.query(list_uri,
                arrayOf(Lists.STORE_FILTER), null, null, null)
        if (c!!.count > 0) {
            c.moveToFirst()
            store_id = c.getString(0)
            c.deactivate()
            c.close()
        }
        return store_id
    }

    @JvmStatic
    fun getListFilterStoreName(context: Context, list_uri: Uri): String? {
        var filter: String? = null
        val store_id = getListFilterStoreId(context, list_uri)

        if (store_id != null && store_id.length > 0) {
            val c = context.contentResolver.query(Stores.CONTENT_URI,
                    arrayOf(Stores.NAME), "_id = ?", arrayOf(store_id), null)
            if (c != null) {
                if (c.count > 0) {
                    c.moveToFirst()
                    filter = c.getString(0)
                }
                c.deactivate()
                c.close()
            }
        }

        return filter
    }

    @JvmStatic
    fun getListTagsFilter(context: Context, list_uri: Uri): String? {
        var filter: String? = null
        val c = context.contentResolver.query(list_uri,
                arrayOf(Lists.TAGS_FILTER), null, null, null)
        if (c!!.count > 0) {
            c.moveToFirst()
            filter = c.getString(0)
            c.deactivate()
            c.close()

            if (filter != null && filter.length == 0) {
                filter = null
            }
        }
        return filter
    }

    @JvmStatic
    fun addDefaultsToAddedItem(context: Context, list_id: Long, item_id: Long) {
        val list_uri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                java.lang.Long.toString(list_id))
        val tagsFilter = getListTagsFilter(context, list_uri)
        val storeId = getListFilterStoreId(context, list_uri)
        val hasTagsFilter = !TextUtils.isEmpty(tagsFilter)
        val hasStoreIdFilter = !TextUtils.isEmpty(storeId)

        if (hasStoreIdFilter) {
            addItemToStore(context, item_id, java.lang.Long.parseLong(storeId), true, null, null, false)
        }

        if (hasTagsFilter) {
            addTagToItem(context, item_id, tagsFilter!!)
        }
    }

    @JvmStatic
    fun getListSortOrder(context: Context, list_id: Long): String? {
        var sort: String? = null
        val list_uri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                java.lang.Long.toString(list_id))
        val c = context.contentResolver.query(list_uri,
                arrayOf(Lists.ITEMS_SORT), null, null, null)
        if (c!!.count > 0) {
            c.moveToFirst()
            sort = c.getString(0)
            c.deactivate()
            c.close()

            if (sort != null && sort.length == 0) {
                sort = null
            }
        }
        return sort
    }

}
