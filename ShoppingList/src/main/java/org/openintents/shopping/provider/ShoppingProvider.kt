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

package org.openintents.shopping.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.UriMatcher
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.MatrixCursor
import android.database.SQLException
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import org.openintents.intents.ProviderIntents
import org.openintents.intents.ProviderUtils
import org.openintents.shopping.LogConstants
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.Contains
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull
import org.openintents.shopping.library.provider.ShoppingContract.ItemStores
import org.openintents.shopping.library.provider.ShoppingContract.Items
import org.openintents.shopping.library.provider.ShoppingContract.Lists
import org.openintents.shopping.library.provider.ShoppingContract.Status
import org.openintents.shopping.library.provider.ShoppingContract.Stores
import org.openintents.shopping.library.provider.ShoppingContract.Units
import org.openintents.shopping.ui.PreferenceActivity
import org.openintents.shopping.ui.widget.ShoppingItemsView

/**
 * Provides access to a database of shopping items and shopping lists.
 *
 *
 * ShoppingProvider maintains the following tables: * items: items you want to
 * buy * lists: shopping lists ("My shopping list", "Bob's shopping list") *
 * contains: which item/list/(recipe) is contained in which shopping list. *
 * stores: * itemstores: (which store carries which item)
 */
class ShoppingProvider : ContentProvider() {

    private var mOpenHelper: ShoppingDatabase? = null

    override fun onCreate(): Boolean {
        mOpenHelper = ShoppingDatabase(context!!)
        return true
    }

    override fun query(
        url: Uri, projection: Array<String?>?, selection: String?,
        selectionArgs: Array<String?>?, sort: String?
    ): Cursor? {
        val qb = SQLiteQueryBuilder()

        var list_id: Long = -1

        if (debug) {
            Log.d(TAG, "Query for URL: $url")
        }

        var defaultOrderBy: String? = null
        var groupBy: String? = null

        when (URL_MATCHER.match(url)) {

            ITEMS -> {
                qb.tables = "items"
                qb.setProjectionMap(ITEMS_PROJECTION_MAP)
                defaultOrderBy = Items.DEFAULT_SORT_ORDER
            }

            ITEM_ID -> {
                qb.tables = "items"
                qb.appendWhere("_id=" + url.pathSegments[1])
            }

            LISTS -> {
                qb.tables = "lists"
                qb.setProjectionMap(LISTS_PROJECTION_MAP)
                defaultOrderBy = Lists.DEFAULT_SORT_ORDER
            }

            LIST_ID -> {
                qb.tables = "lists"
                qb.appendWhere("_id=" + url.pathSegments[1])
            }

            CONTAINS -> {
                qb.tables = "contains"
                qb.setProjectionMap(CONTAINS_PROJECTION_MAP)
                defaultOrderBy = Contains.DEFAULT_SORT_ORDER
            }

            CONTAINS_ID -> {
                qb.tables = "contains"
                qb.appendWhere("_id=" + url.pathSegments[1])
            }

            CONTAINS_FULL -> {

                val inSearchMode = appIsInSearchMode()

                // all callers pass list id as selection_args[0]. perhaps not so
                // nice to depend on that, but... need to choose the projection map
                // based on the list's store filter.
                if (!inSearchMode
                    && PreferenceActivity.getUsingFiltersFromPrefs(context!!)
                    && listUsesStoreFilter(selectionArgs!![0]!!)
                ) {
                    // actually there are two ways we could do the query when
                    // filtering by stores. perhaps
                    // we should offer both. for now choose the first one...
                    if (true) {
                        // show only items which have corresponding records in
                        // itemstores
                        qb.tables = "contains, items, lists, itemstores"
                        qb.setProjectionMap(CONTAINS_FULL_STORE_PROJECTION_MAP)
                        qb.appendWhere(
                            "contains.item_id = items._id AND "
                                    + "contains.list_id = lists._id AND "
                                    + "items._id = itemstores.item_id AND "
                                    + "lists.store_filter = itemstores.store_id"
                        )
                    } else {
                        // this query is not quite right, but the idea is
                        // show all items, but only show prices from the selected
                        // store.
                        qb.tables = "contains, items, lists left outer join itemstores on (items._id = itemstores.item_id)"
                        qb.setProjectionMap(CONTAINS_FULL_STORE_PROJECTION_MAP)
                        qb.appendWhere(
                            "contains.item_id = items._id AND "
                                    + "contains.list_id = lists._id AND "
                                    + "items._id = itemstores.item_id AND "
                                    + "lists.store_filter = itemstores.store_id"
                        )
                    }
                } else if (PreferenceActivity
                        .getUsingPerStorePricesFromPrefs(context!!)
                ) {
                    qb.tables = "contains, items, lists left outer join itemstores on (items._id = itemstores.item_id)"
                    qb.setProjectionMap(CONTAINS_FULL_CHEAPEST_PROJECTION_MAP)
                    qb.appendWhere(
                        "contains.item_id = items._id AND "
                                + "contains.list_id = lists._id"
                    )
                    groupBy = "items._id"
                } else {
                    qb.tables = "contains, items, lists"
                    qb.setProjectionMap(CONTAINS_FULL_PROJECTION_MAP)
                    qb.appendWhere(
                        "contains.item_id = items._id AND "
                                + "contains.list_id = lists._id"
                    )
                }
                defaultOrderBy = ContainsFull.DEFAULT_SORT_ORDER
                val tagFilter = getListTagsFilter(selectionArgs!![0]!!)
                if (!inSearchMode && !TextUtils.isEmpty(tagFilter)) {
                    qb.appendWhere(" AND items.tags like '%" + escapeSQLChars(tagFilter!!) + "%' ESCAPE '`'")
                }
            }

            CONTAINS_FULL_ID -> {
                qb.tables = "contains, items, lists"
                qb.appendWhere("_id=" + url.pathSegments[1])
                qb.appendWhere(
                    "contains.item_id = items._id AND "
                            + "contains.list_id = lists._id"
                )
            }

            CONTAINS_FULL_LISTID -> {
                list_id = url.pathSegments[2].toLong()
                qb.tables = "contains, items, lists"
                qb.appendWhere(
                    "contains.item_id = items._id AND " +
                            "contains.list_id = lists._id AND " +
                            "lists._id = " + list_id
                )
            }

            STORES -> {
                qb.tables = "stores"
                qb.setProjectionMap(STORES_PROJECTION_MAP)
            }

            STORES_ID -> {
                qb.tables = "stores"
                qb.appendWhere("_id=" + url.pathSegments[1])
            }

            STORES_LISTID -> {
                qb.tables = "stores"
                qb.setProjectionMap(STORES_PROJECTION_MAP)
                qb.appendWhere("list_id=" + url.pathSegments[1])
            }

            TAGS_LISTID -> {
                // this is for querying tags regardless of filters.
                // might want to restrict the projection map.
                qb.tables = "contains, items, lists"
                qb.setProjectionMap(CONTAINS_FULL_PROJECTION_MAP)
                qb.appendWhere(
                    "contains.item_id = items._id AND "
                            + "contains.list_id = lists._id AND " + "contains.list_id="
                            + url.pathSegments[1]
                )
                groupBy = "items.tags"
            }

            ITEMSTORES -> {
                qb.tables = "itemstores, items, stores"
                qb.setProjectionMap(ITEMSTORES_PROJECTION_MAP)
                qb.appendWhere("itemstores.item_id = items._id AND itemstores.store_id = stores._id")
            }

            ITEMSTORES_ID -> {
                qb.tables = "itemstores, items, stores"
                qb.appendWhere("_id=" + url.pathSegments[1])
                qb.appendWhere("itemstores.item_id = items._id AND itemstores.store_id = stores._id")
            }

            ITEMSTORES_ITEMID -> {
                // path segment 1 is "item", path segment 2 is item id, path segment
                // 3 is list id.
                qb.tables = ("stores left outer join itemstores on (stores._id = itemstores.store_id and "
                        + "itemstores.item_id = "
                        + url.pathSegments[2]
                        + ")")
                qb.appendWhere("stores.list_id = " + url.pathSegments[3])
            }

            NOTES -> {
                qb.tables = "items"
                qb.setProjectionMap(NOTES_PROJECTION_MAP)
            }

            NOTE_ID -> {
                qb.tables = "items"
                qb.setProjectionMap(NOTES_PROJECTION_MAP)
                qb.appendWhere("_id=" + url.pathSegments[1])
            }

            UNITS -> {
                qb.tables = "units"
                qb.setProjectionMap(UNITS_PROJECTION_MAP)
            }

            UNITS_ID -> {
                qb.tables = "units"
                qb.setProjectionMap(UNITS_PROJECTION_MAP)
                qb.appendWhere("_id=" + url.pathSegments[1])
            }

            ACTIVELIST -> {
                val m = MatrixCursor(projection)
                // assumes only one projection will ever be used,
                // asking only for the id of the active list.
                val sp = context!!.getSharedPreferences(
                    "org.openintents.shopping_preferences",
                    Context.MODE_PRIVATE
                )
                list_id = sp.getInt("lastused", 1).toLong()
                m.addRow(arrayOf<Any>(list_id.toString()))
                return m
            }

            PREFS -> {
                val m = MatrixCursor(projection)
                // assumes only one projection will ever be used,
                // asking only for the id of the active list.
                val sortOrder = PreferenceActivity.getSortOrderFromPrefs(
                    context!!, ShoppingItemsView.MODE_IN_SHOP
                )
                m.addRow(arrayOf<Any>(sortOrder))
                return m
            }

            SUBTOTALS_LISTID -> {
                list_id = url.pathSegments[1].toLong()
                // FALLTHROUGH
                if (list_id == -1L) {
                    // this gets the wrong answer if user has switched lists in this
                    // session.
                    val sp = context!!.getSharedPreferences(
                        "org.openintents.shopping_preferences",
                        Context.MODE_PRIVATE
                    )
                    list_id = sp.getInt("lastused", 1).toLong()
                }
                qb.setProjectionMap(SUBTOTALS_PROJECTION_MAP)
                groupBy = "priority, status"
                if (PreferenceActivity
                        .getUsingPerStorePricesFromPrefs(context!!)
                ) {
                    // status added to "group by" to cover the case where there are
                    // no store prices
                    // for any checked items. still need to count them separately so
                    // Clean List
                    // can be ungreyed.
                    qb.tables = ("(SELECT (min(itemstores.price) * case when ((contains.quantity is null) or (length(contains.quantity) = 0)) then 1 else contains.quantity end) as qty_price, "
                            + "contains.status as status, contains.priority as priority FROM contains, items left outer join itemstores on (items._id = itemstores.item_id) "
                            + "WHERE (contains.item_id = items._id AND contains.list_id = "
                            + list_id
                            + " ) AND contains.status != 3 GROUP BY itemstores.item_id, status) ")
                } else {
                    qb.tables = ("(SELECT (items.price * case when ((contains.quantity is null) or (length(contains.quantity) = 0)) then 1 else contains.quantity end) as qty_price, "
                            + "contains.status as status, contains.priority as priority FROM contains, items "
                            + "WHERE (contains.item_id = items._id AND contains.list_id = "
                            + list_id + " ) AND contains.status != 3) ")
                }
            }

            SUBTOTALS -> {
                if (list_id == -1L) {
                    // this gets the wrong answer if user has switched lists in this
                    // session.
                    val sp = context!!.getSharedPreferences(
                        "org.openintents.shopping_preferences",
                        Context.MODE_PRIVATE
                    )
                    list_id = sp.getInt("lastused", 1).toLong()
                }
                qb.setProjectionMap(SUBTOTALS_PROJECTION_MAP)
                groupBy = "priority, status"
                if (PreferenceActivity
                        .getUsingPerStorePricesFromPrefs(context!!)
                ) {
                    // status added to "group by" to cover the case where there are
                    // no store prices
                    // for any checked items. still need to count them separately so
                    // Clean List
                    // can be ungreyed.
                    qb.tables = ("(SELECT (min(itemstores.price) * case when ((contains.quantity is null) or (length(contains.quantity) = 0)) then 1 else contains.quantity end) as qty_price, "
                            + "contains.status as status, contains.priority as priority FROM contains, items left outer join itemstores on (items._id = itemstores.item_id) "
                            + "WHERE (contains.item_id = items._id AND contains.list_id = "
                            + list_id
                            + " ) AND contains.status != 3 GROUP BY itemstores.item_id, status) ")
                } else {
                    qb.tables = ("(SELECT (items.price * case when ((contains.quantity is null) or (length(contains.quantity) = 0)) then 1 else contains.quantity end) as qty_price, "
                            + "contains.status as status, contains.priority as priority FROM contains, items "
                            + "WHERE (contains.item_id = items._id AND contains.list_id = "
                            + list_id + " ) AND contains.status != 3) ")
                }
            }

            CONTAINS_COPYOFID -> {
                val oldContainsId = url.pathSegments[2].toLong()
                return copyItemAndContains(projection, oldContainsId)
            }

            else -> throw IllegalArgumentException("Unknown URL $url")
        }

        // If no sort order is specified use the default

        val orderBy: String?
        if (TextUtils.isEmpty(sort)) {
            orderBy = defaultOrderBy
        } else {
            orderBy = sort
        }

        val db = mOpenHelper!!.readableDatabase
        if (debug) {
            val qs = qb.buildQuery(
                projection, selection, null, groupBy,
                null, orderBy, null
            )
            Log.d(TAG, "Query : $qs")
        }

        val c = qb.query(
            db, projection, selection, selectionArgs, groupBy,
            null, orderBy
        )
        c.setNotificationUri(context!!.contentResolver, url)
        return c
    }

    private fun listUsesStoreFilter(listId: String): Boolean {
        val db = mOpenHelper!!.readableDatabase
        val qb = SQLiteQueryBuilder()
        qb.tables = "lists"
        qb.appendWhere("_id=$listId")
        val c = qb.query(
            db, arrayOf(Lists.STORE_FILTER), null,
            null, null, null, null
        )
        if (c.count != 1) {
            return false
        }

        c.moveToFirst()
        val storeId = c.getLong(0)
        c.deactivate()
        c.close()

        return (storeId != -1L)
    }

    private fun getListTagsFilter(listId: String): String? {
        val db = mOpenHelper!!.readableDatabase
        val qb = SQLiteQueryBuilder()
        qb.tables = "lists"
        qb.appendWhere("_id=$listId")
        val c = qb.query(
            db, arrayOf(Lists.TAGS_FILTER), null, null,
            null, null, null
        )
        if (c.count != 1) {
            return null
        }

        c.moveToFirst()
        val tag = c.getString(0)
        c.deactivate()
        c.close()

        return (tag)
    }

    private fun appIsInSearchMode(): Boolean {
        val sp = context!!.getSharedPreferences(
            "org.openintents.shopping_preferences",
            Context.MODE_PRIVATE
        )
        return sp.getBoolean("_searching", false)
    }

    // caller wants us to copy the item and the contains record.
    // only supported projection is item_id, contains_id of the copy.
    private fun copyItemAndContains(projection: Array<String?>?, oldContainsId: Long): Cursor? {
        val oldItemId: Long
        val containsCopyId: Long
        val itemCopyId: Long
        val db = mOpenHelper!!.writableDatabase

        // find the item id from the contains record
        var qb = SQLiteQueryBuilder()
        qb.tables = "contains"
        qb.appendWhere("_id=$oldContainsId")
        var c = qb.query(
            db, arrayOf(Contains.ITEM_ID), null, null,
            null, null, null
        )
        if (c.count != 1) {
            return null
        }

        c.moveToFirst()
        oldItemId = c.getLong(0)
        c.deactivate()
        c.close()

        // read the item
        qb = SQLiteQueryBuilder()
        qb.tables = "items"
        qb.appendWhere("_id=$oldItemId")
        c = qb.query(db, Items.PROJECTION_TO_COPY, null, null, null, null, null)
        if (c.count != 1) {
            return null
        }
        c.moveToFirst()
        val itemValues = ContentValues()
        DatabaseUtils.cursorRowToContentValues(c, itemValues)
        c.deactivate()
        c.close()

        // read the contains record
        qb = SQLiteQueryBuilder()
        qb.tables = "contains"
        qb.appendWhere("_id=$oldContainsId")
        c = qb.query(
            db, Contains.PROJECTION_TO_COPY, null, null, null, null,
            null
        )
        if (c.count != 1) {
            return null
        }
        c.moveToFirst()
        val containsValues = ContentValues()
        DatabaseUtils.cursorRowToContentValues(c, containsValues)
        c.deactivate()
        c.close()

        // insert the item copy
        validateItemValues(itemValues)
        itemCopyId = db.insert("items", "items", itemValues)

        // insert the contains record copy
        containsValues.put(Contains.ITEM_ID, itemCopyId)
        validateContainsValues(containsValues)
        containsCopyId = db.insert("contains", "contains", containsValues)

        // not sure, should we also copy ItemStores records?

        val m = MatrixCursor(projection)
        m.addRow(
            arrayOf<Any>(
                itemCopyId.toString(),
                containsCopyId.toString()
            )
        )
        return m
    }

    override fun insert(url: Uri, initialValues: ContentValues?): Uri? {
        val values: ContentValues
        if (initialValues != null) {
            values = ContentValues(initialValues)
        } else {
            values = ContentValues()
        }

        // insert is supported for items or lists
        when (URL_MATCHER.match(url)) {
            ITEMS, NOTES -> return insertItem(url, values)

            LISTS -> return insertList(url, values)

            CONTAINS -> return insertContains(url, values)

            CONTAINS_FULL -> throw IllegalArgumentException(
                "Insert not supported for "
                        + url + ", use CONTAINS instead of CONTAINS_FULL."
            )

            STORES -> return insertStore(url, values)

            ITEMSTORES -> return insertItemStore(url, values)

            UNITS -> return insertUnits(url, values)

            else -> throw IllegalArgumentException("Unknown URL $url")
        }
    }

    private fun insertItem(url: Uri, values: ContentValues): Uri {
        val db = mOpenHelper!!.writableDatabase
        val rowID: Long

        validateItemValues(values)

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)
        // insert the item.
        rowID = db.insert("items", "items", values)
        if (rowID > 0) {
            val uri = ContentUris.withAppendedId(Items.CONTENT_URI, rowID)
            context!!.contentResolver.notifyChange(uri, null)

            val intent = Intent(ProviderIntents.ACTION_INSERTED)
            intent.data = uri
            context!!.sendBroadcast(intent)

            return uri
        }

        // If everything works, we should not reach the following line:
        throw SQLException("Failed to insert row into $url")
    }

    private fun validateItemValues(values: ContentValues) {
        val now = java.lang.Long.valueOf(System.currentTimeMillis())
        // Make sure that the fields are all set
        if (!values.containsKey(Items.NAME)) {
            val r = context!!.resources
            values.put(Items.NAME, r.getString(R.string.new_item))
        }

        if (!values.containsKey(Items.IMAGE)) {
            values.put(Items.IMAGE, "")
        }

        if (!values.containsKey(Items.CREATED_DATE)) {
            values.put(Items.CREATED_DATE, now)
        }

        if (!values.containsKey(Items.MODIFIED_DATE)) {
            values.put(Items.MODIFIED_DATE, now)
        }

        if (!values.containsKey(Items.ACCESSED_DATE)) {
            values.put(Items.ACCESSED_DATE, now)
        }
    }

    private fun insertList(url: Uri, values: ContentValues): Uri {
        val db = mOpenHelper!!.writableDatabase
        val rowID: Long

        val now = java.lang.Long.valueOf(System.currentTimeMillis())
        val r = android.content.res.Resources.getSystem()

        // Make sure that the fields are all set
        if (!values.containsKey(Lists.NAME)) {
            values.put(Lists.NAME, r.getString(R.string.new_list))
        }

        if (!values.containsKey(Lists.IMAGE)) {
            values.put(Lists.IMAGE, "")
        }

        if (!values.containsKey(Lists.CREATED_DATE)) {
            values.put(Lists.CREATED_DATE, now)
        }

        if (!values.containsKey(Lists.MODIFIED_DATE)) {
            values.put(Lists.MODIFIED_DATE, now)
        }

        if (!values.containsKey(Lists.ACCESSED_DATE)) {
            values.put(Lists.ACCESSED_DATE, now)
        }

        if (!values.containsKey(Lists.SHARE_CONTACTS)) {
            values.put(Lists.SHARE_CONTACTS, "")
        }

        if (!values.containsKey(Lists.SKIN_BACKGROUND)) {
            values.put(Lists.SKIN_BACKGROUND, "")
        }

        if (!values.containsKey(Lists.SKIN_FONT)) {
            values.put(Lists.SKIN_FONT, "")
        }

        if (!values.containsKey(Lists.SKIN_COLOR)) {
            values.put(Lists.SKIN_COLOR, 0)
        }

        if (!values.containsKey(Lists.SKIN_COLOR_STRIKETHROUGH)) {
            values.put(Lists.SKIN_COLOR_STRIKETHROUGH, 0xFF006600.toInt())
        }

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)

        // insert the tag.
        rowID = db.insert("lists", "lists", values)
        if (rowID > 0) {
            val uri = ContentUris.withAppendedId(Lists.CONTENT_URI, rowID)
            context!!.contentResolver.notifyChange(uri, null)

            val intent = Intent(ProviderIntents.ACTION_INSERTED)
            intent.data = uri
            context!!.sendBroadcast(intent)

            return uri
        }

        // If everything works, we should not reach the following line:
        throw SQLException("Failed to insert row into $url")
    }

    private fun insertContains(url: Uri, values: ContentValues): Uri {
        val db = mOpenHelper!!.writableDatabase

        // Make sure that the fields are all set
        if (!(values.containsKey(Contains.ITEM_ID) && values
                .containsKey(Contains.LIST_ID))
        ) {
            // At least these values should exist.
            throw SQLException(
                "Failed to insert row into " + url
                        + ": ITEM_ID and LIST_ID must be given."
            )
        }

        // TODO: Check here that ITEM_ID and LIST_ID
        // actually exist in the tables.
        if (!values.containsKey(Contains.STATUS)) {
            values.put(Contains.STATUS, Status.WANT_TO_BUY)
        } else {
            // Check here that STATUS is valid.
            val s = values.getAsInteger(Contains.STATUS).toLong()
            if (!Status.isValid(s)) {
                throw SQLException(
                    "Failed to insert row into " + url
                            + ": Status " + s + " is not valid."
                )
            }
        }

        validateContainsValues(values)

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)

        // insert the item.
        val rowId = db.insert("contains", "contains", values)
        if (rowId > 0) {
            val uri = ContentUris.withAppendedId(Contains.CONTENT_URI, rowId)
            context!!.contentResolver.notifyChange(uri, null)

            val intent = Intent(ProviderIntents.ACTION_INSERTED)
            intent.data = uri
            context!!.sendBroadcast(intent)

            return uri
        }

        // If everything works, we should not reach the following line:
        throw SQLException("Failed to insert row into $url")
    }

    private fun validateContainsValues(values: ContentValues) {
        val now = java.lang.Long.valueOf(System.currentTimeMillis())

        if (!values.containsKey(Contains.CREATED_DATE)) {
            values.put(Contains.CREATED_DATE, now)
        }
        if (!values.containsKey(Contains.MODIFIED_DATE)) {
            values.put(Contains.MODIFIED_DATE, now)
        }

        if (!values.containsKey(Contains.ACCESSED_DATE)) {
            values.put(Contains.ACCESSED_DATE, now)
        }

        if (!values.containsKey(Contains.SHARE_CREATED_BY)) {
            values.put(Contains.SHARE_CREATED_BY, "")
        }

        if (!values.containsKey(Contains.SHARE_MODIFIED_BY)) {
            values.put(Contains.SHARE_MODIFIED_BY, "")
        }
    }

    private fun insertStore(url: Uri, values: ContentValues): Uri {
        val db = mOpenHelper!!.writableDatabase
        val rowID: Long

        val now = java.lang.Long.valueOf(System.currentTimeMillis())

        // Make sure that the fields are all set
        if (!values.containsKey(Stores.NAME)) {
            throw SQLException(
                "Failed to insert row into " + url
                        + ": Store NAME must be given."
            )
        }

        if (!values.containsKey(Stores.CREATED_DATE)) {
            values.put(Stores.CREATED_DATE, now)
        }

        if (!values.containsKey(Stores.MODIFIED_DATE)) {
            values.put(Stores.MODIFIED_DATE, now)
        }

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)

        // insert the tag.
        rowID = db.insert("stores", "stores", values)
        if (rowID > 0) {
            val uri = ContentUris.withAppendedId(Stores.CONTENT_URI, rowID)
            context!!.contentResolver.notifyChange(uri, null)

            val intent = Intent(ProviderIntents.ACTION_INSERTED)
            intent.data = uri
            context!!.sendBroadcast(intent)

            return uri
        }

        // If everything works, we should not reach the following line:
        throw SQLException("Failed to insert row into $url")
    }

    private fun insertItemStore(url: Uri, values: ContentValues): Uri {
        val db = mOpenHelper!!.writableDatabase
        val now = java.lang.Long.valueOf(System.currentTimeMillis())

        // Make sure that the fields are all set
        if (!(values.containsKey(ItemStores.ITEM_ID) && values
                .containsKey(ItemStores.STORE_ID))
        ) {
            // At least these values should exist.
            throw SQLException(
                "Failed to insert row into " + url
                        + ": ITEM_ID and STORE_ID must be given."
            )
        }

        // TODO: Check here that ITEM_ID and STORE_ID
        // actually exist in the tables.

        if (!values.containsKey(ItemStores.PRICE)) {
            values.put(ItemStores.PRICE, -1)
        }
        if (!values.containsKey(ItemStores.AISLE)) {
            values.putNull(ItemStores.AISLE)
        }

        if (!values.containsKey(ItemStores.CREATED_DATE)) {
            values.put(ItemStores.CREATED_DATE, now)
        }

        if (!values.containsKey(ItemStores.MODIFIED_DATE)) {
            values.put(ItemStores.MODIFIED_DATE, now)
        }

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)

        // insert the item.
        val rowId = db.insert("itemstores", "itemstores", values)
        if (rowId > 0) {
            val uri = ContentUris.withAppendedId(ItemStores.CONTENT_URI, rowId)
            context!!.contentResolver.notifyChange(uri, null)

            val intent = Intent(ProviderIntents.ACTION_INSERTED)
            intent.data = uri
            context!!.sendBroadcast(intent)

            return uri
        }

        // If everything works, we should not reach the following line:
        throw SQLException("Failed to insert row into $url")
    }

    private fun insertUnits(url: Uri, values: ContentValues): Uri {
        val db = mOpenHelper!!.writableDatabase
        val rowID: Long

        val now = java.lang.Long.valueOf(System.currentTimeMillis())

        // Make sure that the fields are all set
        if (!values.containsKey(Units.NAME)) {
            throw SQLException(
                "Failed to insert row into " + url
                        + ": Units NAME must be given."
            )
        }

        if (!values.containsKey(Units.CREATED_DATE)) {
            values.put(Units.CREATED_DATE, now)
        }

        if (!values.containsKey(Stores.MODIFIED_DATE)) {
            values.put(Units.MODIFIED_DATE, now)
        }

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)

        // insert the units.
        rowID = db.insert("units", "units", values)
        if (rowID > 0) {
            val uri = ContentUris.withAppendedId(Units.CONTENT_URI, rowID)
            context!!.contentResolver.notifyChange(uri, null)

            val intent = Intent(ProviderIntents.ACTION_INSERTED)
            intent.data = uri
            context!!.sendBroadcast(intent)

            return uri
        }

        // If everything works, we should not reach the following line:
        throw SQLException("Failed to insert row into $url")
    }

    override fun delete(url: Uri, where: String?, whereArgs: Array<String?>?): Int {
        val db = mOpenHelper!!.writableDatabase
        val count: Int
        var affectedRows: LongArray? = null
        // long rowId;
        when (URL_MATCHER.match(url)) {
            ITEMS -> {
                affectedRows = ProviderUtils.getAffectedRows(
                    db, "items", where,
                    whereArgs
                )
                count = db.delete("items", where, whereArgs)
            }

            ITEM_ID -> {
                val segment = url.pathSegments[1] // contains rowId
                // rowId = Long.parseLong(segment);
                val whereString: String
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND ($where)"
                } else {
                    whereString = ""
                }

                affectedRows = ProviderUtils.getAffectedRows(
                    db, "items", "_id="
                            + segment + whereString, whereArgs
                )
                count = db.delete(
                    "items", "_id=" + segment + whereString,
                    whereArgs
                )
            }

            LISTS -> {
                affectedRows = ProviderUtils.getAffectedRows(
                    db, "lists", where,
                    whereArgs
                )
                count = db.delete("lists", where, whereArgs)
            }

            LIST_ID -> {
                val segment = url.pathSegments[1] // contains rowId
                // rowId = Long.parseLong(segment);
                val whereString: String
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND ($where)"
                } else {
                    whereString = ""
                }

                affectedRows = ProviderUtils.getAffectedRows(
                    db, "lists", "_id="
                            + segment + whereString, whereArgs
                )
                count = db.delete(
                    "lists", "_id=" + segment + whereString,
                    whereArgs
                )
            }

            CONTAINS -> {
                affectedRows = ProviderUtils.getAffectedRows(
                    db, "contains", where,
                    whereArgs
                )
                count = db.delete("contains", where, whereArgs)
            }

            CONTAINS_ID -> {
                val segment = url.pathSegments[1] // contains rowId
                // rowId = Long.parseLong(segment);
                val whereString: String
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND ($where)"
                } else {
                    whereString = ""
                }

                affectedRows = ProviderUtils.getAffectedRows(
                    db, "contains", "_id="
                            + segment + whereString, whereArgs
                )
                count = db.delete(
                    "contains", "_id=" + segment + whereString,
                    whereArgs
                )
            }

            NOTE_ID -> {
                // don't delete the row, just the note.
                val values = ContentValues()
                values.putNull("note")
                count = update(url, values, null, null)
            }

            STORES -> {
                affectedRows = ProviderUtils.getAffectedRows(
                    db, "stores", where,
                    whereArgs
                )
                count = db.delete("stores", where, whereArgs)
            }

            ITEMSTORES -> {
                affectedRows = ProviderUtils.getAffectedRows(
                    db, "itemstores",
                    where, whereArgs
                )
                count = db.delete("itemstores", where, whereArgs)
            }

            ITEMSTORES_ID -> {
                val segment = url.pathSegments[1] // contains rowId
                // rowId = Long.parseLong(segment);
                val whereString: String
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND ($where)"
                } else {
                    whereString = ""
                }

                affectedRows = ProviderUtils.getAffectedRows(
                    db, "itemstores",
                    "_id=" + segment + whereString, whereArgs
                )
                count = db.delete(
                    "itemstores", "_id=" + segment + whereString,
                    whereArgs
                )
            }

            else -> throw IllegalArgumentException("Unknown URL $url")
        }

        context!!.contentResolver.notifyChange(url, null)

        val intent = Intent(ProviderIntents.ACTION_DELETED)
        intent.data = url
        intent.putExtra(ProviderIntents.EXTRA_AFFECTED_ROWS, affectedRows)
        context!!.sendBroadcast(intent)

        return count
    }

    override fun update(
        url: Uri, values: ContentValues?, where: String?,
        whereArgs: Array<String?>?
    ): Int {
        if (debug) {
            Log.d(TAG, "update called for: $url")
        }
        val db = mOpenHelper!!.writableDatabase
        val count: Int
        var secondUri: Uri? = null

        // long rowId;
        when (URL_MATCHER.match(url)) {
            ITEMS, NOTES -> {
                count = db.update("items", values, where, whereArgs)
            }

            NOTE_ID -> {
                // drop some OI Notepad fields on the floor.
                values!!.remove("title")
                values.remove("encrypted")
                values.remove("theme")
                values.remove("nothing_To_see_here")
                // fall through...
                val segment = url.pathSegments[1] // contains rowId
                // rowId = Long.parseLong(segment);
                val whereString: String
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND ($where)"
                } else {
                    whereString = ""
                }

                count = db.update(
                    "items", values, "_id=" + segment + whereString,
                    whereArgs
                )
                secondUri = ShoppingContract.Items.CONTENT_URI
            }

            ITEM_ID -> {
                val segment = url.pathSegments[1] // contains rowId
                // rowId = Long.parseLong(segment);
                val whereString: String
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND ($where)"
                } else {
                    whereString = ""
                }

                count = db.update(
                    "items", values, "_id=" + segment + whereString,
                    whereArgs
                )
                secondUri = ShoppingContract.Items.CONTENT_URI
            }

            LISTS -> {
                count = db.update("lists", values, where, whereArgs)
            }

            LIST_ID -> {
                val segment = url.pathSegments[1] // contains rowId
                // rowId = Long.parseLong(segment);
                val whereString: String
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND ($where)"
                } else {
                    whereString = ""
                }

                count = db.update(
                    "lists", values, "_id=" + segment + whereString,
                    whereArgs
                )
            }

            CONTAINS -> {
                count = db.update("contains", values, where, whereArgs)
            }

            CONTAINS_ID -> {
                val segment = url.pathSegments[1] // contains rowId
                // rowId = Long.parseLong(segment);
                val whereString: String
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND ($where)"
                } else {
                    whereString = ""
                }

                count = db.update(
                    "contains", values, "_id=" + segment
                            + whereString, whereArgs
                )
            }

            STORES -> {
                count = db.update("stores", values, where, whereArgs)
            }

            STORES_ID -> {
                val segment = url.pathSegments[1] // contains rowId
                // rowId = Long.parseLong(segment);
                val whereString: String
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND ($where)"
                } else {
                    whereString = ""
                }
                count = db.update(
                    "stores", values, "_id=" + segment + whereString,
                    whereArgs
                )
            }

            ITEMSTORES -> {
                count = db.update("itemstores", values, where, whereArgs)
            }

            ITEMSTORES_ID -> {
                val segment = url.pathSegments[1] // contains rowId
                // rowId = Long.parseLong(segment);
                val whereString: String
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND ($where)"
                } else {
                    whereString = ""
                }
                count = db.update(
                    "itemstores", values, "_id=" + segment
                            + whereString, whereArgs
                )
            }

            UNITS -> {
                count = db.update("units", values, where, whereArgs)
            }

            UNITS_ID -> {
                val segment = url.pathSegments[1] // contains rowId
                // rowId = Long.parseLong(segment);
                val whereString: String
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND ($where)"
                } else {
                    whereString = ""
                }
                count = db.update(
                    "units", values, "_id=" + segment + whereString,
                    whereArgs
                )
            }

            else -> {
                Log.e(TAG, "Update received unknown URL: $url")
                throw IllegalArgumentException("Unknown URL $url")
            }
        }

        context!!.contentResolver.notifyChange(url, null)
        if (secondUri != null) {
            context!!.contentResolver.notifyChange(secondUri, null)
        }

        val intent = Intent(ProviderIntents.ACTION_MODIFIED)
        intent.data = url
        context!!.sendBroadcast(intent)

        return count
    }

    override fun getType(url: Uri): String? {
        when (URL_MATCHER.match(url)) {
            ITEMS -> return "vnd.android.cursor.dir/vnd.openintents.shopping.item"

            ITEM_ID -> return ShoppingContract.ITEM_TYPE

            LISTS -> return "vnd.android.cursor.dir/vnd.openintents.shopping.list"

            LIST_ID -> return "vnd.android.cursor.item/vnd.openintents.shopping.list"

            CONTAINS -> return "vnd.android.cursor.dir/vnd.openintents.shopping.contains"

            CONTAINS_ID -> return "vnd.android.cursor.item/vnd.openintents.shopping.contains"

            CONTAINS_FULL -> return "vnd.android.cursor.dir/vnd.openintents.shopping.containsfull"

            CONTAINS_FULL_ID -> return "vnd.android.cursor.item/vnd.openintents.shopping.containsfull"

            CONTAINS_FULL_LISTID -> return "vnd.android.cursor.dir/vnd.openintents.shopping.containsfull"

            STORES -> return "vnd.android.cursor.dir/vnd.openintents.shopping.stores"

            STORES_ID, STORES_LISTID -> return "vnd.android.cursor.item/vnd.openintents.shopping.stores"

            NOTES -> return ShoppingContract.Notes.CONTENT_TYPE
            NOTE_ID -> return ShoppingContract.Notes.CONTENT_ITEM_TYPE

            ITEMSTORES -> return "vnd.android.cursor.dir/vnd.openintents.shopping.itemstores"
            ITEMSTORES_ID -> return "vnd.android.cursor.item/vnd.openintents.shopping.itemstores"
            ITEMSTORES_ITEMID -> return "vnd.android.cursor.dir/vnd.openintents.shopping.itemstores"

            UNITS -> return "vnd.android.cursor.dir/vnd.openintents.shopping.units"
            UNITS_ID -> return "vnd.android.cursor.item/vnd.openintents.shopping.units"

            ACTIVELIST ->                 // not sure this is quite right
                return "vnd.android.cursor.item/vnd.openintents.shopping.list"

            else -> throw IllegalArgumentException("Unknown URL $url")
        }
    }

    companion object {
        const val TAG: String = "ShoppingProvider"
        private val debug = false || LogConstants.debug

        // Basic tables
        private const val ITEMS = 1
        private const val ITEM_ID = 2
        private const val LISTS = 3
        private const val LIST_ID = 4
        private const val CONTAINS = 5
        private const val CONTAINS_ID = 6
        private const val STORES = 7
        private const val STORES_ID = 8
        private const val STORES_LISTID = 9
        private const val ITEMSTORES = 10
        private const val ITEMSTORES_ID = 11
        private const val NOTES = 12
        private const val NOTE_ID = 13
        private const val UNITS = 14
        private const val UNITS_ID = 15
        private const val PREFS = 16
        private const val ITEMSTORES_ITEMID = 17
        private const val SUBTOTALS = 18
        private const val SUBTOTALS_LISTID = 19
        private const val CONTAINS_FULL_LISTID = 20

        // Derived tables
        private const val CONTAINS_FULL = 101 // combined with items and

        // lists
        private const val CONTAINS_FULL_ID = 102
        private const val ACTIVELIST = 103

        // duplicate specified contains record and its item, return ids
        private const val CONTAINS_COPYOFID = 104
        private const val TAGS_LISTID = 105

        private val URL_MATCHER: UriMatcher
        private var ITEMS_PROJECTION_MAP: HashMap<String, String>
        private var LISTS_PROJECTION_MAP: HashMap<String, String>
        private var CONTAINS_PROJECTION_MAP: HashMap<String, String>
        private var CONTAINS_FULL_PROJECTION_MAP: HashMap<String, String>
        private var CONTAINS_FULL_CHEAPEST_PROJECTION_MAP: HashMap<String, String>
        private var CONTAINS_FULL_STORE_PROJECTION_MAP: HashMap<String, String>
        private var STORES_PROJECTION_MAP: HashMap<String, String>
        private var ITEMSTORES_PROJECTION_MAP: HashMap<String, String>
        private var NOTES_PROJECTION_MAP: HashMap<String, String>
        private var UNITS_PROJECTION_MAP: HashMap<String, String>
        private var SUBTOTALS_PROJECTION_MAP: HashMap<String, String>

        init {
            URL_MATCHER = UriMatcher(UriMatcher.NO_MATCH)
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "items", ITEMS)
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "items/#", ITEM_ID)
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "lists", LISTS)
            URL_MATCHER.addURI(
                "org.openintents.shopping.ishy", "lists/active",
                ACTIVELIST
            )
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "lists/#", LIST_ID)
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "contains", CONTAINS)
            URL_MATCHER.addURI(
                "org.openintents.shopping.ishy", "contains/#",
                CONTAINS_ID
            )
            URL_MATCHER.addURI(
                "org.openintents.shopping.ishy", "contains/copyof/#",
                CONTAINS_COPYOFID
            )
            URL_MATCHER.addURI(
                "org.openintents.shopping.ishy", "containsfull",
                CONTAINS_FULL
            )
            URL_MATCHER.addURI(
                "org.openintents.shopping.ishy", "containsfull/#",
                CONTAINS_FULL_ID
            )
            URL_MATCHER.addURI(
                "org.openintents.shopping.ishy", "containsfull/list/#",
                CONTAINS_FULL_LISTID
            )
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "stores", STORES)
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "stores/#", STORES_ID)
            URL_MATCHER
                .addURI("org.openintents.shopping.ishy", "itemstores", ITEMSTORES)
            URL_MATCHER.addURI(
                "org.openintents.shopping.ishy", "itemstores/#",
                ITEMSTORES_ID
            )
            URL_MATCHER.addURI(
                "org.openintents.shopping.ishy", "itemstores/item/#/#",
                ITEMSTORES_ITEMID
            )
            URL_MATCHER.addURI(
                "org.openintents.shopping.ishy", "liststores/#",
                STORES_LISTID
            )
            URL_MATCHER.addURI(
                "org.openintents.shopping.ishy", "listtags/#",
                TAGS_LISTID
            )
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "notes", NOTES)
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "notes/#", NOTE_ID)
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "units", UNITS)
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "units/#", UNITS_ID)

            URL_MATCHER.addURI("org.openintents.shopping.ishy", "prefs", PREFS)
            // subtotals for the specified list id, or active list if not specified
            URL_MATCHER.addURI(
                "org.openintents.shopping.ishy", "subtotals/#",
                SUBTOTALS_LISTID
            )
            URL_MATCHER.addURI("org.openintents.shopping.ishy", "subtotals", SUBTOTALS)

            ITEMS_PROJECTION_MAP = HashMap()
            ITEMS_PROJECTION_MAP.put(Items._ID, "items._id")
            ITEMS_PROJECTION_MAP.put(Items.NAME, "items.name")
            ITEMS_PROJECTION_MAP.put(Items.IMAGE, "items.image")
            ITEMS_PROJECTION_MAP.put(Items.PRICE, "items.price")
            ITEMS_PROJECTION_MAP.put(Items.UNITS, "items.units")
            ITEMS_PROJECTION_MAP.put(Items.TAGS, "items.tags")
            ITEMS_PROJECTION_MAP.put(Items.BARCODE, "items.barcode")
            ITEMS_PROJECTION_MAP.put(Items.LOCATION, "items.location")
            ITEMS_PROJECTION_MAP.put(Items.DUE_DATE, "items.due")
            ITEMS_PROJECTION_MAP.put(Items.CREATED_DATE, "items.created")
            ITEMS_PROJECTION_MAP.put(Items.MODIFIED_DATE, "items.modified")
            ITEMS_PROJECTION_MAP.put(Items.ACCESSED_DATE, "items.accessed")

            LISTS_PROJECTION_MAP = HashMap()
            LISTS_PROJECTION_MAP.put(Lists._ID, "lists._id")
            LISTS_PROJECTION_MAP.put(Lists.NAME, "lists.name")
            LISTS_PROJECTION_MAP.put(Lists.IMAGE, "lists.image")
            LISTS_PROJECTION_MAP.put(Lists.CREATED_DATE, "lists.created")
            LISTS_PROJECTION_MAP.put(Lists.MODIFIED_DATE, "lists.modified")
            LISTS_PROJECTION_MAP.put(Lists.ACCESSED_DATE, "lists.accessed")
            LISTS_PROJECTION_MAP.put(Lists.SHARE_NAME, "lists.share_name")
            LISTS_PROJECTION_MAP.put(Lists.SHARE_CONTACTS, "lists.share_contacts")
            LISTS_PROJECTION_MAP
                .put(Lists.SKIN_BACKGROUND, "lists.skin_background")
            LISTS_PROJECTION_MAP.put(Lists.SKIN_FONT, "lists.skin_font")
            LISTS_PROJECTION_MAP.put(Lists.SKIN_COLOR, "lists.skin_color")
            LISTS_PROJECTION_MAP.put(
                Lists.SKIN_COLOR_STRIKETHROUGH,
                "lists.skin_color_strikethrough"
            )
            LISTS_PROJECTION_MAP.put(Lists.ITEMS_SORT, "lists.items_sort")

            CONTAINS_PROJECTION_MAP = HashMap()
            CONTAINS_PROJECTION_MAP.put(Contains._ID, "contains._id")
            CONTAINS_PROJECTION_MAP.put(Contains.ITEM_ID, "contains.item_id")
            CONTAINS_PROJECTION_MAP.put(Contains.LIST_ID, "contains.list_id")
            CONTAINS_PROJECTION_MAP.put(Contains.QUANTITY, "contains.quantity")
            CONTAINS_PROJECTION_MAP.put(Contains.PRIORITY, "contains.priority")

            CONTAINS_PROJECTION_MAP.put(Contains.STATUS, "contains.status")
            CONTAINS_PROJECTION_MAP.put(Contains.CREATED_DATE, "contains.created")
            CONTAINS_PROJECTION_MAP
                .put(Contains.MODIFIED_DATE, "contains.modified")
            CONTAINS_PROJECTION_MAP
                .put(Contains.ACCESSED_DATE, "contains.accessed")
            CONTAINS_PROJECTION_MAP.put(
                Contains.SHARE_CREATED_BY,
                "contains.share_created_by"
            )
            CONTAINS_PROJECTION_MAP.put(
                Contains.SHARE_MODIFIED_BY,
                "contains.share_modified_by"
            )

            CONTAINS_FULL_PROJECTION_MAP = HashMap()
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull._ID,
                "contains._id as _id"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.ITEM_ID,
                "contains.item_id"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.LIST_ID,
                "contains.list_id"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.QUANTITY,
                "contains.quantity as quantity"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.PRIORITY,
                "contains.priority as priority"
            )
            CONTAINS_FULL_PROJECTION_MAP
                .put(ContainsFull.STATUS, "contains.status")
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.CREATED_DATE,
                "contains.created"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.MODIFIED_DATE,
                "contains.modified"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.ACCESSED_DATE,
                "contains.accessed"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.SHARE_CREATED_BY,
                "contains.share_created_by"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.SHARE_MODIFIED_BY,
                "contains.share_modified_by"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.ITEM_NAME,
                "items.name as item_name"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.ITEM_IMAGE,
                "items.image as item_image"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.ITEM_PRICE,
                "items.price as item_price"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.ITEM_UNITS,
                "items.units as item_units"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.ITEM_TAGS,
                "items.tags as item_tags"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.LIST_NAME,
                "lists.name as list_name"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.LIST_IMAGE,
                "lists.image as list_image"
            )
            CONTAINS_FULL_PROJECTION_MAP.put(
                ContainsFull.ITEM_HAS_NOTE,
                "items.note is not NULL and items.note <> '' as item_has_note"
            )

            CONTAINS_FULL_CHEAPEST_PROJECTION_MAP = HashMap(
                CONTAINS_FULL_PROJECTION_MAP
            )
            CONTAINS_FULL_CHEAPEST_PROJECTION_MAP.put(
                ContainsFull.ITEM_PRICE,
                "min(itemstores.price) as item_price"
            )

            CONTAINS_FULL_STORE_PROJECTION_MAP = HashMap(
                CONTAINS_FULL_PROJECTION_MAP
            )
            CONTAINS_FULL_STORE_PROJECTION_MAP.put(
                ContainsFull.ITEM_PRICE,
                "itemstores.price as item_price"
            )

            UNITS_PROJECTION_MAP = HashMap()
            UNITS_PROJECTION_MAP.put(Units._ID, "units._id")
            UNITS_PROJECTION_MAP.put(Units.CREATED_DATE, "units.created")
            UNITS_PROJECTION_MAP.put(Units.MODIFIED_DATE, "units.modified")
            UNITS_PROJECTION_MAP.put(Units.NAME, "units.name")
            UNITS_PROJECTION_MAP.put(Units.SINGULAR, "units.singular")

            STORES_PROJECTION_MAP = HashMap()
            STORES_PROJECTION_MAP.put(Stores._ID, "stores._id")
            STORES_PROJECTION_MAP.put(Stores.CREATED_DATE, "stores.created")
            STORES_PROJECTION_MAP.put(Stores.MODIFIED_DATE, "stores.modified")
            STORES_PROJECTION_MAP.put(Stores.NAME, "stores.name")
            STORES_PROJECTION_MAP.put(Stores.LIST_ID, "stores.list_id")

            ITEMSTORES_PROJECTION_MAP = HashMap()
            ITEMSTORES_PROJECTION_MAP.put(ItemStores._ID, "itemstores._id")
            ITEMSTORES_PROJECTION_MAP.put(
                ItemStores.CREATED_DATE,
                "itemstores.created"
            )
            ITEMSTORES_PROJECTION_MAP.put(
                ItemStores.MODIFIED_DATE,
                "itemstores.modified"
            )
            ITEMSTORES_PROJECTION_MAP.put(ItemStores.ITEM_ID, "itemstores.item_id")
            ITEMSTORES_PROJECTION_MAP.put(
                ItemStores.STORE_ID,
                "itemstores.store_id"
            )
            ITEMSTORES_PROJECTION_MAP.put(Stores.NAME, "stores.name")
            ITEMSTORES_PROJECTION_MAP.put(ItemStores.AISLE, "itemstores.aisle")
            ITEMSTORES_PROJECTION_MAP.put(ItemStores.PRICE, "itemstores.price")
            ITEMSTORES_PROJECTION_MAP.put(
                ItemStores.STOCKS_ITEM,
                "itemstores.stocks_item"
            )

            NOTES_PROJECTION_MAP = HashMap()
            NOTES_PROJECTION_MAP.put(ShoppingContract.Notes._ID, "items._id")
            NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.NOTE, "items.note")
            NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.TITLE, "null as title")
            NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.TAGS, "null as tags")
            NOTES_PROJECTION_MAP.put(
                ShoppingContract.Notes.ENCRYPTED,
                "null as encrypted"
            )
            NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.THEME, "null as theme")

            SUBTOTALS_PROJECTION_MAP = HashMap()
            SUBTOTALS_PROJECTION_MAP.put(
                ShoppingContract.Subtotals.COUNT,
                "count() as count"
            )
            SUBTOTALS_PROJECTION_MAP.put(
                ShoppingContract.Subtotals.PRIORITY,
                "priority"
            )
            SUBTOTALS_PROJECTION_MAP.put(
                ShoppingContract.Subtotals.SUBTOTAL,
                "sum(qty_price) as subtotal"
            )
            SUBTOTALS_PROJECTION_MAP.put(
                ShoppingContract.Subtotals.STATUS,
                "status"
            )
        }

        @JvmStatic
        fun escapeSQLChars(trapped: String): String {
            /*
        In order for this method to work properly, the query using the result must use '`' as its
        Escape character.
        */
            return trapped.replace("'", "''").replace("`", "``").replace("%", "`%").replace("_", "`_")
        }
    }
}
