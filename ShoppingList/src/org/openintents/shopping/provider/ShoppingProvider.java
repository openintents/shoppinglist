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

package org.openintents.shopping.provider;

import java.util.HashMap;

import org.openintents.intents.ProviderIntents;
import org.openintents.intents.ProviderUtils;
import org.openintents.shopping.LogConstants;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.ItemStores;
import org.openintents.shopping.library.provider.ShoppingContract.Items;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.library.provider.ShoppingContract.Stores;
import org.openintents.shopping.library.provider.ShoppingContract.Units;
import org.openintents.shopping.ui.PreferenceActivity;
import org.openintents.shopping.ui.ShoppingActivity;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * Provides access to a database of shopping items and shopping lists.
 * 
 * ShoppingProvider maintains the following tables: 
 * * items: items you want to buy 
 * * lists: shopping lists ("My shopping list",	"Bob's shopping list") 
 * * contains: which item/list/(recipe) is contained in which shopping list. 
 * * stores: 
 * * itemstores: (which store carries which item) 
 */
public class ShoppingProvider extends ContentProvider {

	private ShoppingDatabase mOpenHelper;

	static final String TAG = "ShoppingProvider";
	private static final boolean debug = false || LogConstants.debug;
	

	
	private static HashMap<String, String> ITEMS_PROJECTION_MAP;
	private static HashMap<String, String> LISTS_PROJECTION_MAP;
	private static HashMap<String, String> CONTAINS_PROJECTION_MAP;
	private static HashMap<String, String> CONTAINS_FULL_PROJECTION_MAP;
	private static HashMap<String, String> CONTAINS_FULL_CHEAPEST_PROJECTION_MAP;
	private static HashMap<String, String> STORES_PROJECTION_MAP;
	private static HashMap<String, String> ITEMSTORES_PROJECTION_MAP;
	private static HashMap<String, String> NOTES_PROJECTION_MAP;
	private static HashMap<String, String> UNITS_PROJECTION_MAP;
	private static HashMap<String, String> SUBTOTALS_PROJECTION_MAP;

	// Basic tables
	private static final int ITEMS = 1;
	private static final int ITEM_ID = 2;
	private static final int LISTS = 3;
	private static final int LIST_ID = 4;
	private static final int CONTAINS = 5;
	private static final int CONTAINS_ID = 6;
	private static final int STORES = 7;
	private static final int STORES_ID = 8;
	private static final int STORES_LISTID = 9;
	private static final int ITEMSTORES = 10;
	private static final int ITEMSTORES_ID = 11;
	private static final int NOTES = 12;
	private static final int NOTE_ID = 13;
	private static final int UNITS = 14;
	private static final int UNITS_ID = 15;
	private static final int PREFS = 16;
	private static final int ITEMSTORES_ITEMID = 17;
	private static final int SUBTOTALS = 18;
	private static final int SUBTOTALS_LISTID = 19;

	// Derived tables
	private static final int CONTAINS_FULL = 101; // combined with items and
	// lists
	private static final int CONTAINS_FULL_ID = 102;
	private static final int ACTIVELIST = 103;
	// duplicate specified contains record and its item, return ids
	private static final int CONTAINS_COPYOFID = 104;

	private static final UriMatcher URL_MATCHER;

	@Override
	public boolean onCreate() {
		mOpenHelper = new ShoppingDatabase(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri url, String[] projection, String selection,
			String[] selectionArgs, String sort) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		long list_id = -1;
		
		if (debug) Log.d(TAG, "Query for URL: " + url);

		String defaultOrderBy = null;
		String groupBy = null;

		switch (URL_MATCHER.match(url)) {
		
		case ITEMS:
			qb.setTables("items");
			qb.setProjectionMap(ITEMS_PROJECTION_MAP);
			defaultOrderBy = Items.DEFAULT_SORT_ORDER;
			break;

		case ITEM_ID:
			qb.setTables("items");
			qb.appendWhere("_id=" + url.getPathSegments().get(1));
			break;

		case LISTS:
			qb.setTables("lists");
			qb.setProjectionMap(LISTS_PROJECTION_MAP);
			defaultOrderBy = Lists.DEFAULT_SORT_ORDER;
			break;

		case LIST_ID:
			qb.setTables("lists");
			qb.appendWhere("_id=" + url.getPathSegments().get(1));
			break;

		case CONTAINS:
			qb.setTables("contains");
			qb.setProjectionMap(CONTAINS_PROJECTION_MAP);
			defaultOrderBy = Contains.DEFAULT_SORT_ORDER;
			break;

		case CONTAINS_ID:
			qb.setTables("contains");
			qb.appendWhere("_id=" + url.getPathSegments().get(1));
			break;

		case CONTAINS_FULL:
			
			if (PreferenceActivity.getUsingPerStorePricesFromPrefs(getContext())) {
				qb.setTables("contains, items, lists left outer join itemstores on (items._id = itemstores.item_id)");
				qb.setProjectionMap(CONTAINS_FULL_CHEAPEST_PROJECTION_MAP);
				qb.appendWhere("contains.item_id = items._id AND " +
					 		   "contains.list_id = lists._id");
				groupBy = "items._id";
				
			} else {
				qb.setTables("contains, items, lists");
				qb.setProjectionMap(CONTAINS_FULL_PROJECTION_MAP);
				qb.appendWhere("contains.item_id = items._id AND " +
					 		   "contains.list_id = lists._id");
				
			}
			defaultOrderBy = ContainsFull.DEFAULT_SORT_ORDER;
			break;

		case CONTAINS_FULL_ID:
			qb.setTables("contains, items, lists");
			qb.appendWhere("_id=" + url.getPathSegments().get(1));
			qb.appendWhere("contains.item_id = items._id AND " +
			 		   "contains.list_id = lists._id");
			break;
			
		case STORES:
			qb.setTables("stores");
			qb.setProjectionMap(STORES_PROJECTION_MAP);
			break;

		case STORES_ID:
			qb.setTables("stores");
			qb.appendWhere("_id=" + url.getPathSegments().get(1));
			break;

		case STORES_LISTID:
			qb.setTables("stores");
			qb.setProjectionMap(STORES_PROJECTION_MAP);
			qb.appendWhere("list_id=" + url.getPathSegments().get(1));
			break;
			
		case ITEMSTORES:
			qb.setTables("itemstores, items, stores");
			qb.setProjectionMap(ITEMSTORES_PROJECTION_MAP);
			qb.appendWhere("itemstores.item_id = items._id AND itemstores.store_id = stores._id");
			break;
			
		case ITEMSTORES_ID:
			qb.setTables("itemstores, items, stores");
			qb.appendWhere("_id=" + url.getPathSegments().get(1));
			qb.appendWhere("itemstores.item_id = items._id AND itemstores.store_id = stores._id");
			break;
		
		case ITEMSTORES_ITEMID:
			// path segment 1 is "item", path segment 2 is item id, path segment 3 is list id.
			qb.setTables("stores left outer join itemstores on (stores._id = itemstores.store_id and " +
					"itemstores.item_id = " + url.getPathSegments().get(2) + ")");
			qb.appendWhere("stores.list_id = " + url.getPathSegments().get(3) );
			break;
			
		case NOTES:
			qb.setTables("items");
			qb.setProjectionMap(NOTES_PROJECTION_MAP);
			break;

		case NOTE_ID:
			qb.setTables("items");
			qb.setProjectionMap(NOTES_PROJECTION_MAP);
			qb.appendWhere("_id=" + url.getPathSegments().get(1));
			break;

		case UNITS:
			qb.setTables("units");
			qb.setProjectionMap(UNITS_PROJECTION_MAP);
			break;

		case UNITS_ID:
			qb.setTables("units");
			qb.setProjectionMap(UNITS_PROJECTION_MAP);
			qb.appendWhere("_id=" + url.getPathSegments().get(1));
			break;
		
		case ACTIVELIST:
			MatrixCursor m = new MatrixCursor(projection);
			// assumes only one projection will ever be used, 
			// asking only for the id of the active list.
			SharedPreferences sp = getContext().getSharedPreferences(
					"org.openintents.shopping_preferences", Context.MODE_PRIVATE);
			list_id = sp.getInt("lastused", 1);
			m.addRow(new Object [] {Long.toString(list_id)});
			return (Cursor)m;
		case PREFS:
			m = new MatrixCursor(projection);
			// assumes only one projection will ever be used, 
			// asking only for the id of the active list.
			String sortOrder = PreferenceActivity.getSortOrderFromPrefs(getContext(), 
					ShoppingActivity.MODE_IN_SHOP);
			m.addRow(new Object [] {sortOrder});
			return (Cursor)m;
			
		case SUBTOTALS_LISTID:
			list_id = Long.parseLong(url.getPathSegments().get(1));
			// FALLTHROUGH
		case SUBTOTALS:
			if (list_id == -1) {
				// this gets the wrong answer if user has switched lists in this session.
				sp = getContext().getSharedPreferences(
						"org.openintents.shopping_preferences", Context.MODE_PRIVATE);
				list_id = sp.getInt("lastused", 1);	
			}
			qb.setProjectionMap(SUBTOTALS_PROJECTION_MAP);
			groupBy = "priority, status";
			if (PreferenceActivity.getUsingPerStorePricesFromPrefs(getContext())) {
				// status added to "group by" to cover the case where there are no store prices 
				// for any checked items. still need to count them separately so Clean List 
				// can be ungreyed.
				qb.setTables("(SELECT (min(itemstores.price) * case when ((contains.quantity is null) or (length(contains.quantity) = 0)) then 1 else contains.quantity end) as qty_price, " + 
							 "contains.status as status, contains.priority as priority FROM contains, items left outer join itemstores on (items._id = itemstores.item_id) " + 
							 "WHERE (contains.item_id = items._id AND contains.list_id = " + list_id + " ) AND contains.status != 3 GROUP BY itemstores.item_id, status) ");
				
			} else {
				qb.setTables("(SELECT (items.price * case when ((contains.quantity is null) or (length(contains.quantity) = 0)) then 1 else contains.quantity end) as qty_price, " + 
						     "contains.status as status, contains.priority as priority FROM contains, items " + 
						     "WHERE (contains.item_id = items._id AND contains.list_id = " + list_id + " ) AND contains.status != 3) ");

			}
			break;
			
		case CONTAINS_COPYOFID:
			long oldContainsId = Long.parseLong(url.getPathSegments().get(2));
			return copyItemAndContains(projection, oldContainsId);
			
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}

		// If no sort order is specified use the default

		String orderBy;
		if (TextUtils.isEmpty(sort)) {
			orderBy = defaultOrderBy;
		} else {
			orderBy = sort;
		}

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		if (debug) {
			String qs = qb.buildQuery(projection, selection, null, groupBy, null, orderBy, null);
			Log.d(TAG, "Query : " + qs);
		}

		Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy,
				null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), url);
		return c;
	}

	// caller wants us to copy the item and the contains record.
	// only supported projection is item_id, contains_id of the copy.
	private Cursor copyItemAndContains(String[] projection, long oldContainsId) {
		long oldItemId, containsCopyId, itemCopyId;
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		// find the item id from the contains record
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables("contains");
		qb.appendWhere("_id=" + oldContainsId);
		Cursor c = qb.query(db, new String[] {Contains.ITEM_ID}, null, null, null, null, null);
		if (c.getCount() != 1) {
			return null;
		}
		
		c.moveToFirst();
		oldItemId = c.getLong(0);
		c.deactivate();
		c.close();
		
		// read the item
		qb = new SQLiteQueryBuilder();
		qb.setTables("items");
		qb.appendWhere("_id=" + oldItemId);
		c = qb.query(db, Items.PROJECTION_TO_COPY, null, null, null, null, null);
		if (c.getCount() != 1) {
			return null;
		}
		c.moveToFirst();
		ContentValues itemValues = new ContentValues();
		DatabaseUtils.cursorRowToContentValues(c, itemValues);
		c.deactivate();
		c.close();
		
		// read the contains record
		qb = new SQLiteQueryBuilder();
		qb.setTables("contains");
		qb.appendWhere("_id=" + oldContainsId);
		c = qb.query(db, Contains.PROJECTION_TO_COPY, null, null, null, null, null);
		if (c.getCount() != 1) {
			return null;
		}
		c.moveToFirst();
		ContentValues containsValues = new ContentValues();
		DatabaseUtils.cursorRowToContentValues(c, containsValues);
		c.deactivate();
		c.close();
		
		// insert the item copy
		validateItemValues(itemValues);
		itemCopyId = db.insert("items", "items", itemValues);

		// insert the contains record copy
		containsValues.put(Contains.ITEM_ID, itemCopyId);
		validateContainsValues(containsValues);
		containsCopyId = db.insert("contains", "contains", containsValues);
		
		// not sure, should we also copy ItemStores records?
		
		MatrixCursor m = new MatrixCursor(projection);
		m.addRow(new Object [] {Long.toString(itemCopyId), Long.toString(containsCopyId)});
		return (Cursor)m;
	}

	@Override
	public Uri insert(Uri url, ContentValues initialValues) {
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		// insert is supported for items or lists
		switch (URL_MATCHER.match(url)) {
		case ITEMS:
		case NOTES:
			return insertItem(url, values);

		case LISTS:
			return insertList(url, values);

		case CONTAINS:
			return insertContains(url, values);

		case CONTAINS_FULL:
			throw new IllegalArgumentException("Insert not supported for "
					+ url + ", use CONTAINS instead of CONTAINS_FULL.");

		case STORES:
			return insertStore(url, values);
			
		case ITEMSTORES:
			return insertItemStore(url, values);

		case UNITS:
			return insertUnits(url, values);
			
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
	}

	private Uri insertItem(Uri url, ContentValues values) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowID;

		validateItemValues(values);

		// TODO: Here we should check, whether item exists already.
		// (see TagsProvider)
		// insert the item.
		rowID = db.insert("items", "items", values);
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Items.CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uri, null);

			Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
			intent.setData(uri);
			getContext().sendBroadcast(intent);

			return uri;
		}

		// If everything works, we should not reach the following line:
		throw new SQLException("Failed to insert row into " + url);
	}

	private void validateItemValues(ContentValues values) {
		Long now = Long.valueOf(System.currentTimeMillis());
		// Make sure that the fields are all set
		if (!values.containsKey(Items.NAME)) {
			Resources r = getContext().getResources();
			values.put(Items.NAME, r.getString(R.string.new_item));
		}

		if (!values.containsKey(Items.IMAGE)) {
			values.put(Items.IMAGE, "");
		}

		if (!values.containsKey(Items.CREATED_DATE)) {
			values.put(Items.CREATED_DATE, now);
		}

		if (!values.containsKey(Items.MODIFIED_DATE)) {
			values.put(Items.MODIFIED_DATE, now);
		}

		if (!values.containsKey(Items.ACCESSED_DATE)) {
			values.put(Items.ACCESSED_DATE, now);
		}
	}

	private Uri insertList(Uri url, ContentValues values) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowID;

		Long now = Long.valueOf(System.currentTimeMillis());
		Resources r = Resources.getSystem();

		// Make sure that the fields are all set
		if (!values.containsKey(Lists.NAME)) {
			values.put(Lists.NAME, r.getString(R.string.new_list));
		}

		if (!values.containsKey(Lists.IMAGE)) {
			values.put(Lists.IMAGE, "");
		}

		if (!values.containsKey(Lists.CREATED_DATE)) {
			values.put(Lists.CREATED_DATE, now);
		}

		if (!values.containsKey(Lists.MODIFIED_DATE)) {
			values.put(Lists.MODIFIED_DATE, now);
		}

		if (!values.containsKey(Lists.ACCESSED_DATE)) {
			values.put(Lists.ACCESSED_DATE, now);
		}

		if (!values.containsKey(Lists.SHARE_CONTACTS)) {
			values.put(Lists.SHARE_CONTACTS, "");
		}

		if (!values.containsKey(Lists.SKIN_BACKGROUND)) {
			values.put(Lists.SKIN_BACKGROUND, "");
		}

		if (!values.containsKey(Lists.SKIN_FONT)) {
			values.put(Lists.SKIN_FONT, "");
		}

		if (!values.containsKey(Lists.SKIN_COLOR)) {
			values.put(Lists.SKIN_COLOR, 0);
		}

		if (!values.containsKey(Lists.SKIN_COLOR_STRIKETHROUGH)) {
			values.put(Lists.SKIN_COLOR_STRIKETHROUGH, 0xFF006600);
		}

		// TODO: Here we should check, whether item exists already.
		// (see TagsProvider)

		// insert the tag.
		rowID = db.insert("lists", "lists", values);
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Lists.CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uri, null);

			Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
			intent.setData(uri);
			getContext().sendBroadcast(intent);

			return uri;
		}

		// If everything works, we should not reach the following line:
		throw new SQLException("Failed to insert row into " + url);

	}

	private Uri insertContains(Uri url, ContentValues values) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Resources r = Resources.getSystem();

		// Make sure that the fields are all set
		if (!(values.containsKey(Contains.ITEM_ID) && values
				.containsKey(Contains.LIST_ID))) {
			// At least these values should exist.
			throw new SQLException("Failed to insert row into " + url
					+ ": ITEM_ID and LIST_ID must be given.");
		}

		// TODO: Check here that ITEM_ID and LIST_ID
		// actually exist in the tables.
		if (!values.containsKey(Contains.STATUS)) {
			values.put(Contains.STATUS, Status.WANT_TO_BUY);
		} else {
			// Check here that STATUS is valid.
			long s = values.getAsInteger(Contains.STATUS);
			if (!Status.isValid(s)) {
				throw new SQLException("Failed to insert row into " + url
						+ ": Status " + s + " is not valid.");
			}
		}
					
		validateContainsValues(values);
		
		// TODO: Here we should check, whether item exists already.
		// (see TagsProvider)

		// insert the item.
		long rowId = db.insert("contains", "contains", values);
		if (rowId > 0) {
			Uri uri = ContentUris.withAppendedId(Contains.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(uri, null);

			Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
			intent.setData(uri);
			getContext().sendBroadcast(intent);

			return uri;
		}

		// If everything works, we should not reach the following line:
		throw new SQLException("Failed to insert row into " + url);
	}

	private void validateContainsValues(ContentValues values) {
		Long now = Long.valueOf(System.currentTimeMillis());

		if (!values.containsKey(Contains.CREATED_DATE)) {
			values.put(Contains.CREATED_DATE, now);
		}
		if (!values.containsKey(Contains.MODIFIED_DATE)) {
			values.put(Contains.MODIFIED_DATE, now);
		}

		if (!values.containsKey(Contains.ACCESSED_DATE)) {
			values.put(Contains.ACCESSED_DATE, now);
		}

		if (!values.containsKey(Contains.SHARE_CREATED_BY)) {
			values.put(Contains.SHARE_CREATED_BY, "");
		}

		if (!values.containsKey(Contains.SHARE_MODIFIED_BY)) {
			values.put(Contains.SHARE_MODIFIED_BY, "");
		}

	}

	private Uri insertStore(Uri url, ContentValues values) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowID;

		Long now = Long.valueOf(System.currentTimeMillis());
		Resources r = Resources.getSystem();

		// Make sure that the fields are all set
		if (!values.containsKey(Stores.NAME)) {
			throw new SQLException("Failed to insert row into " + url
					+ ": Store NAME must be given.");
		}

		if (!values.containsKey(Stores.CREATED_DATE)) {
			values.put(Stores.CREATED_DATE, now);
		}

		if (!values.containsKey(Stores.MODIFIED_DATE)) {
			values.put(Stores.MODIFIED_DATE, now);
		}

		// TODO: Here we should check, whether item exists already.
		// (see TagsProvider)

		// insert the tag.
		rowID = db.insert("stores", "stores", values);
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Stores.CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uri, null);

			Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
			intent.setData(uri);
			getContext().sendBroadcast(intent);

			return uri;
		}

		// If everything works, we should not reach the following line:
		throw new SQLException("Failed to insert row into " + url);

	}

	private Uri insertItemStore(Uri url, ContentValues values) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Long now = Long.valueOf(System.currentTimeMillis());
		Resources r = Resources.getSystem();

		// Make sure that the fields are all set
		if (!(values.containsKey(ItemStores.ITEM_ID) && values
				.containsKey(ItemStores.STORE_ID))) {
			// At least these values should exist.
			throw new SQLException("Failed to insert row into " + url
					+ ": ITEM_ID and STORE_ID must be given.");
		}

		// TODO: Check here that ITEM_ID and STORE_ID
		// actually exist in the tables.

		if (!values.containsKey(ItemStores.PRICE)) {
			values.put(ItemStores.PRICE, -1);
		} 
		if (!values.containsKey(ItemStores.AISLE)) {
			values.putNull(ItemStores.AISLE);
		}

		if (!values.containsKey(ItemStores.CREATED_DATE)) {
			values.put(ItemStores.CREATED_DATE, now);
		}

		if (!values.containsKey(ItemStores.MODIFIED_DATE)) {
			values.put(ItemStores.MODIFIED_DATE, now);
		}


		// TODO: Here we should check, whether item exists already.
		// (see TagsProvider)

		// insert the item.
		long rowId = db.insert("itemstores", "itemstores", values);
		if (rowId > 0) {
			Uri uri = ContentUris.withAppendedId(ItemStores.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(uri, null);

			Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
			intent.setData(uri);
			getContext().sendBroadcast(intent);

			return uri;
		}

		// If everything works, we should not reach the following line:
		throw new SQLException("Failed to insert row into " + url);
	}

	private Uri insertUnits(Uri url, ContentValues values) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowID;

		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set
		if (!values.containsKey(Units.NAME)) {
			throw new SQLException("Failed to insert row into " + url
					+ ": Units NAME must be given.");
		}

		if (!values.containsKey(Units.CREATED_DATE)) {
			values.put(Units.CREATED_DATE, now);
		}

		if (!values.containsKey(Stores.MODIFIED_DATE)) {
			values.put(Units.MODIFIED_DATE, now);
		}

		// TODO: Here we should check, whether item exists already.
		// (see TagsProvider)

		// insert the units.
		rowID = db.insert("units", "units", values);
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(Units.CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(uri, null);

			Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
			intent.setData(uri);
			getContext().sendBroadcast(intent);

			return uri;
		}

		// If everything works, we should not reach the following line:
		throw new SQLException("Failed to insert row into " + url);

	}

	@Override
	public int delete(Uri url, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		long[] affectedRows = null;
		// long rowId;
		switch (URL_MATCHER.match(url)) {
		case ITEMS:
			affectedRows = ProviderUtils.getAffectedRows(db, "items", where,
					whereArgs);
			count = db.delete("items", where, whereArgs);
			break;

		case ITEM_ID:
			String segment = url.getPathSegments().get(1); // contains rowId
			// rowId = Long.parseLong(segment);
			String whereString;
			if (!TextUtils.isEmpty(where)) {
				whereString = " AND (" + where + ')';
			} else {
				whereString = "";
			}

			affectedRows = ProviderUtils.getAffectedRows(db, "items", "_id="
					+ segment + whereString, whereArgs);
			count = db.delete("items", "_id=" + segment + whereString,
					whereArgs);
			break;

		case LISTS:
			affectedRows = ProviderUtils.getAffectedRows(db, "lists", where,
					whereArgs);
			count = db.delete("lists", where, whereArgs);
			break;

		case LIST_ID:
			segment = url.getPathSegments().get(1); // contains rowId
			// rowId = Long.parseLong(segment);
			if (!TextUtils.isEmpty(where)) {
				whereString = " AND (" + where + ')';
			} else {
				whereString = "";
			}

			affectedRows = ProviderUtils.getAffectedRows(db, "lists", "_id="
					+ segment + whereString, whereArgs);
			count = db.delete("lists", "_id=" + segment + whereString,
					whereArgs);
			break;

		case CONTAINS:
			affectedRows = ProviderUtils.getAffectedRows(db, "contains", where,
					whereArgs);
			count = db.delete("contains", where, whereArgs);
			break;

		case CONTAINS_ID:
			segment = url.getPathSegments().get(1); // contains rowId
			// rowId = Long.parseLong(segment);
			if (!TextUtils.isEmpty(where)) {
				whereString = " AND (" + where + ')';
			} else {
				whereString = "";
			}

			affectedRows = ProviderUtils.getAffectedRows(db, "contains", "_id="
					+ segment + whereString, whereArgs);
			count = db.delete("contains", "_id=" + segment + whereString,
					whereArgs);
			break;

		case NOTE_ID:
			// don't delete the row, just the note.
			ContentValues values = new ContentValues();
			values.putNull("note");
			count = update(url, values, null, null);
			break;

		case STORES:
			affectedRows = ProviderUtils.getAffectedRows(db, "stores", where,
					whereArgs);
			count = db.delete("stores", where, whereArgs);
			break;
			
		case ITEMSTORES:
			affectedRows = ProviderUtils.getAffectedRows(db, "itemstores", where,
					whereArgs);
			count = db.delete("itemstores", where, whereArgs);
			break;

		case ITEMSTORES_ID:
			segment = url.getPathSegments().get(1); // contains rowId
			// rowId = Long.parseLong(segment);
			if (!TextUtils.isEmpty(where)) {
				whereString = " AND (" + where + ')';
			} else {
				whereString = "";
			}

			affectedRows = ProviderUtils.getAffectedRows(db, "itemstores", "_id="
					+ segment + whereString, whereArgs);
			count = db.delete("itemstores", "_id=" + segment + whereString,
					whereArgs);
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}

		getContext().getContentResolver().notifyChange(url, null);

		Intent intent = new Intent(ProviderIntents.ACTION_DELETED);
		intent.setData(url);
		intent.putExtra(ProviderIntents.EXTRA_AFFECTED_ROWS, affectedRows);
		getContext().sendBroadcast(intent);

		return count;
	}

	@Override
	public int update(Uri url, ContentValues values, String where,
			String[] whereArgs) {
		if (debug) Log.d(TAG, "update called for: " + url);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		Uri secondUri = null;
		
		// long rowId;
		switch (URL_MATCHER.match(url)) {
		case ITEMS:
		case NOTES:
			count = db.update("items", values, where, whereArgs);
			break;

		case NOTE_ID:
			// drop some OI Notepad fields on the floor.
			values.remove("title");
			values.remove("encrypted");
			values.remove("theme");
			values.remove("nothing_To_see_here");
			// fall through...
		case ITEM_ID:
			String segment = url.getPathSegments().get(1); // contains rowId
			// rowId = Long.parseLong(segment);
			String whereString;
			if (!TextUtils.isEmpty(where)) {
				whereString = " AND (" + where + ')';
			} else {
				whereString = "";
			}

			count = db.update("items", values, "_id=" + segment + whereString,
					whereArgs);
			secondUri  = ShoppingContract.Items.CONTENT_URI;
			break;

		case LISTS:
			count = db.update("lists", values, where, whereArgs);
			break;

		case LIST_ID:
			segment = url.getPathSegments().get(1); // contains rowId
			// rowId = Long.parseLong(segment);
			if (!TextUtils.isEmpty(where)) {
				whereString = " AND (" + where + ')';
			} else {
				whereString = "";
			}

			count = db.update("lists", values, "_id=" + segment + whereString,
					whereArgs);
			break;

		case CONTAINS:
			count = db.update("contains", values, where, whereArgs);
			break;

		case CONTAINS_ID:
			segment = url.getPathSegments().get(1); // contains rowId
			// rowId = Long.parseLong(segment);
			if (!TextUtils.isEmpty(where)) {
				whereString = " AND (" + where + ')';
			} else {
				whereString = "";
			}

			count = db.update("contains", values, "_id=" + segment
					+ whereString, whereArgs);
			break;

		case STORES:
			count = db.update("stores", values, where, whereArgs);
			break;
			
		case STORES_ID:
			segment = url.getPathSegments().get(1); // contains rowId
			// rowId = Long.parseLong(segment);
			if (!TextUtils.isEmpty(where)) {
				whereString = " AND (" + where + ')';
			} else {
				whereString = "";
			}
			count = db.update("stores", values, "_id=" + segment
					+ whereString, whereArgs);
			break;

		case ITEMSTORES:
			count = db.update("itemstores", values, where, whereArgs);
			break;
			
		case ITEMSTORES_ID:
			segment = url.getPathSegments().get(1); // contains rowId
			// rowId = Long.parseLong(segment);
			if (!TextUtils.isEmpty(where)) {
				whereString = " AND (" + where + ')';
			} else {
				whereString = "";
			}
			count = db.update("itemstores", values, "_id=" + segment
					+ whereString, whereArgs);
			break;
	
		case UNITS:
			count = db.update("units", values, where, whereArgs);
			break;

		case UNITS_ID:
			segment = url.getPathSegments().get(1); // contains rowId
			// rowId = Long.parseLong(segment);
			if (!TextUtils.isEmpty(where)) {
				whereString = " AND (" + where + ')';
			} else {
				whereString = "";
			}
			count = db.update("units", values, "_id=" + segment
					+ whereString, whereArgs);
			break;

		default:
			Log.e(TAG, "Update received unknown URL: " + url);
			throw new IllegalArgumentException("Unknown URL " + url);
		}

		getContext().getContentResolver().notifyChange(url, null);
		if (secondUri != null){
			getContext().getContentResolver().notifyChange(secondUri, null);
		}
		
		Intent intent = new Intent(ProviderIntents.ACTION_MODIFIED);
		intent.setData(url);
		getContext().sendBroadcast(intent);

		return count;
	}

	@Override
	public String getType(Uri url) {
		switch (URL_MATCHER.match(url)) {
		case ITEMS:
			return "vnd.android.cursor.dir/vnd.openintents.shopping.item";

		case ITEM_ID:
			return ShoppingContract.ITEM_TYPE;

		case LISTS:
			return "vnd.android.cursor.dir/vnd.openintents.shopping.list";

		case LIST_ID:
			return "vnd.android.cursor.item/vnd.openintents.shopping.list";

		case CONTAINS:
			return "vnd.android.cursor.dir/vnd.openintents.shopping.contains";

		case CONTAINS_ID:
			return "vnd.android.cursor.item/vnd.openintents.shopping.contains";

		case CONTAINS_FULL:
			return "vnd.android.cursor.dir/vnd.openintents.shopping.containsfull";

		case CONTAINS_FULL_ID:
			return "vnd.android.cursor.item/vnd.openintents.shopping.containsfull";

		case STORES:
			return "vnd.android.cursor.dir/vnd.openintents.shopping.stores";

		case STORES_ID:
		case STORES_LISTID:
			return "vnd.android.cursor.item/vnd.openintents.shopping.stores";
			
		case NOTES:
			return ShoppingContract.Notes.CONTENT_TYPE; 
		case NOTE_ID: 
			return ShoppingContract.Notes.CONTENT_ITEM_TYPE;
			
		case ITEMSTORES:
			return "vnd.android.cursor.dir/vnd.openintents.shopping.itemstores";
		case ITEMSTORES_ID:
			return "vnd.android.cursor.item/vnd.openintents.shopping.itemstores";
		case ITEMSTORES_ITEMID:
			return "vnd.android.cursor.dir/vnd.openintents.shopping.itemstores";

		case UNITS:
			return "vnd.android.cursor.dir/vnd.openintents.shopping.units";
		case UNITS_ID:
			return "vnd.android.cursor.item/vnd.openintents.shopping.units";
		
		case ACTIVELIST:
			// not sure this is quite right
			return "vnd.android.cursor.item/vnd.openintents.shopping.list";

			
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
	}

	static {
		URL_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URL_MATCHER.addURI("org.openintents.shopping", "items", ITEMS);
		URL_MATCHER.addURI("org.openintents.shopping", "items/#", ITEM_ID);
		URL_MATCHER.addURI("org.openintents.shopping", "lists", LISTS);
		URL_MATCHER.addURI("org.openintents.shopping", "lists/active", ACTIVELIST);
		URL_MATCHER.addURI("org.openintents.shopping", "lists/#", LIST_ID);
		URL_MATCHER.addURI("org.openintents.shopping", "contains", CONTAINS);
		URL_MATCHER.addURI("org.openintents.shopping", "contains/#",
				CONTAINS_ID);
		URL_MATCHER.addURI("org.openintents.shopping", "contains/copyof/#",
				CONTAINS_COPYOFID);
		URL_MATCHER.addURI("org.openintents.shopping", "containsfull",
				CONTAINS_FULL);
		URL_MATCHER.addURI("org.openintents.shopping", "containsfull/#",
				CONTAINS_FULL_ID);
		URL_MATCHER.addURI("org.openintents.shopping", "stores", STORES);
		URL_MATCHER.addURI("org.openintents.shopping", "stores/#", STORES_ID);
		URL_MATCHER.addURI("org.openintents.shopping", "itemstores", ITEMSTORES);
		URL_MATCHER.addURI("org.openintents.shopping", "itemstores/#", 
				ITEMSTORES_ID);
		URL_MATCHER.addURI("org.openintents.shopping", "itemstores/item/#/#", 
				ITEMSTORES_ITEMID);
		URL_MATCHER.addURI("org.openintents.shopping", "liststores/#", 
				STORES_LISTID);
		URL_MATCHER.addURI("org.openintents.shopping", "notes", NOTES);
		URL_MATCHER.addURI("org.openintents.shopping", "notes/#", NOTE_ID);
		URL_MATCHER.addURI("org.openintents.shopping", "units", UNITS);
		URL_MATCHER.addURI("org.openintents.shopping", "units/#", UNITS_ID);
		
		URL_MATCHER.addURI("org.openintents.shopping", "prefs", PREFS);
		// subtotals for the specified list id, or active list if not specified
		URL_MATCHER.addURI("org.openintents.shopping", "subtotals/#", SUBTOTALS_LISTID);
		URL_MATCHER.addURI("org.openintents.shopping", "subtotals", SUBTOTALS);


		ITEMS_PROJECTION_MAP = new HashMap<String, String>();
		ITEMS_PROJECTION_MAP.put(Items._ID, "items._id");
		ITEMS_PROJECTION_MAP.put(Items.NAME, "items.name");
		ITEMS_PROJECTION_MAP.put(Items.IMAGE, "items.image");
		ITEMS_PROJECTION_MAP.put(Items.PRICE, "items.price");
		ITEMS_PROJECTION_MAP.put(Items.UNITS, "items.units");		
		ITEMS_PROJECTION_MAP.put(Items.TAGS, "items.tags");
		ITEMS_PROJECTION_MAP.put(Items.BARCODE, "items.barcode");
		ITEMS_PROJECTION_MAP.put(Items.LOCATION, "items.location");
		ITEMS_PROJECTION_MAP.put(Items.DUE_DATE, "items.due");
		ITEMS_PROJECTION_MAP.put(Items.CREATED_DATE, "items.created");
		ITEMS_PROJECTION_MAP.put(Items.MODIFIED_DATE, "items.modified");
		ITEMS_PROJECTION_MAP.put(Items.ACCESSED_DATE, "items.accessed");

		LISTS_PROJECTION_MAP = new HashMap<String, String>();
		LISTS_PROJECTION_MAP.put(Lists._ID, "lists._id");
		LISTS_PROJECTION_MAP.put(Lists.NAME, "lists.name");
		LISTS_PROJECTION_MAP.put(Lists.IMAGE, "lists.image");
		LISTS_PROJECTION_MAP.put(Lists.CREATED_DATE, "lists.created");
		LISTS_PROJECTION_MAP.put(Lists.MODIFIED_DATE, "lists.modified");
		LISTS_PROJECTION_MAP.put(Lists.ACCESSED_DATE, "lists.accessed");
		LISTS_PROJECTION_MAP.put(Lists.SHARE_NAME, "lists.share_name");
		LISTS_PROJECTION_MAP.put(Lists.SHARE_CONTACTS, "lists.share_contacts");
		LISTS_PROJECTION_MAP
				.put(Lists.SKIN_BACKGROUND, "lists.skin_background");
		LISTS_PROJECTION_MAP.put(Lists.SKIN_FONT, "lists.skin_font");
		LISTS_PROJECTION_MAP.put(Lists.SKIN_COLOR, "lists.skin_color");
		LISTS_PROJECTION_MAP.put(Lists.SKIN_COLOR_STRIKETHROUGH,
				"lists.skin_color_strikethrough");

		CONTAINS_PROJECTION_MAP = new HashMap<String, String>();
		CONTAINS_PROJECTION_MAP.put(Contains._ID, "contains._id");
		CONTAINS_PROJECTION_MAP.put(Contains.ITEM_ID, "contains.item_id");
		CONTAINS_PROJECTION_MAP.put(Contains.LIST_ID, "contains.list_id");
		CONTAINS_PROJECTION_MAP.put(Contains.QUANTITY, "contains.quantity");
		CONTAINS_PROJECTION_MAP.put(Contains.PRIORITY, "contains.priority");

		CONTAINS_PROJECTION_MAP.put(Contains.STATUS, "contains.status");
		CONTAINS_PROJECTION_MAP.put(Contains.CREATED_DATE, "contains.created");
		CONTAINS_PROJECTION_MAP
				.put(Contains.MODIFIED_DATE, "contains.modified");
		CONTAINS_PROJECTION_MAP
				.put(Contains.ACCESSED_DATE, "contains.accessed");
		CONTAINS_PROJECTION_MAP.put(Contains.SHARE_CREATED_BY,
				"contains.share_created_by");
		CONTAINS_PROJECTION_MAP.put(Contains.SHARE_MODIFIED_BY,
				"contains.share_modified_by");

		CONTAINS_FULL_PROJECTION_MAP = new HashMap<String, String>();
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull._ID, "contains._id as _id");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_ID,
				"contains.item_id");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.LIST_ID,
				"contains.list_id");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.QUANTITY,
				"contains.quantity as quantity");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.PRIORITY,
		"contains.priority as priority");
		CONTAINS_FULL_PROJECTION_MAP
				.put(ContainsFull.STATUS, "contains.status");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.CREATED_DATE,
				"contains.created");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.MODIFIED_DATE,
				"contains.modified");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ACCESSED_DATE,
				"contains.accessed");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.SHARE_CREATED_BY,
				"contains.share_created_by");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.SHARE_MODIFIED_BY,
				"contains.share_modified_by");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_NAME,
				"items.name as item_name");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_IMAGE,
				"items.image as item_image");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_PRICE,
				"items.price as item_price");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_UNITS,
		        "items.units as item_units");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_TAGS,
		        "items.tags as item_tags");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.LIST_NAME,
				"lists.name as list_name");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.LIST_IMAGE,
				"lists.image as list_image");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_HAS_NOTE,
		        "items.note is not NULL and items.note <> '' as item_has_note");

		CONTAINS_FULL_CHEAPEST_PROJECTION_MAP = 
			new HashMap<String, String>(CONTAINS_FULL_PROJECTION_MAP);
		CONTAINS_FULL_CHEAPEST_PROJECTION_MAP.put(ContainsFull.ITEM_PRICE, 
				"min(itemstores.price) as item_price");

		UNITS_PROJECTION_MAP = new HashMap<String, String>();
		UNITS_PROJECTION_MAP.put(Units._ID, "units._id");
		UNITS_PROJECTION_MAP.put(Units.CREATED_DATE, "units.created");
		UNITS_PROJECTION_MAP.put(Units.MODIFIED_DATE, "units.modified");
		UNITS_PROJECTION_MAP.put(Units.NAME, "units.name");
		UNITS_PROJECTION_MAP.put(Units.SINGULAR, "units.singular");

		STORES_PROJECTION_MAP = new HashMap<String, String>();
		STORES_PROJECTION_MAP.put(Stores._ID, "stores._id");
		STORES_PROJECTION_MAP.put(Stores.CREATED_DATE, "stores.created");
		STORES_PROJECTION_MAP.put(Stores.MODIFIED_DATE, "stores.modified");
		STORES_PROJECTION_MAP.put(Stores.NAME, "stores.name");
		STORES_PROJECTION_MAP.put(Stores.LIST_ID, "stores.list_id");

		ITEMSTORES_PROJECTION_MAP = new HashMap<String, String>();
		ITEMSTORES_PROJECTION_MAP.put(ItemStores._ID, "itemstores._id");
		ITEMSTORES_PROJECTION_MAP.put(ItemStores.CREATED_DATE, 
				"itemstores.created");
		ITEMSTORES_PROJECTION_MAP.put(ItemStores.MODIFIED_DATE, 
				"itemstores.modified");
		ITEMSTORES_PROJECTION_MAP.put(ItemStores.ITEM_ID, "itemstores.item_id");
		ITEMSTORES_PROJECTION_MAP.put(ItemStores.STORE_ID, "itemstores.store_id");
		ITEMSTORES_PROJECTION_MAP.put(Stores.NAME, "stores.name");
		ITEMSTORES_PROJECTION_MAP.put(ItemStores.AISLE, "itemstores.aisle");
		ITEMSTORES_PROJECTION_MAP.put(ItemStores.PRICE, "itemstores.price");
		ITEMSTORES_PROJECTION_MAP.put(ItemStores.STOCKS_ITEM, "itemstores.stocks_item");

		NOTES_PROJECTION_MAP = new HashMap<String, String>();
		NOTES_PROJECTION_MAP.put(ShoppingContract.Notes._ID, "items._id");
		NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.NOTE, "items.note");
		NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.TITLE, "null as title");
		NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.TAGS, "null as tags");
		NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.ENCRYPTED, "null as encrypted");
		NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.THEME, "null as theme");
		
		SUBTOTALS_PROJECTION_MAP = new HashMap<String, String>();
		SUBTOTALS_PROJECTION_MAP.put(ShoppingContract.Subtotals.COUNT, "count() as count");
		SUBTOTALS_PROJECTION_MAP.put(ShoppingContract.Subtotals.PRIORITY, "priority");
		SUBTOTALS_PROJECTION_MAP.put(ShoppingContract.Subtotals.SUBTOTAL, "sum(qty_price) as subtotal");
		SUBTOTALS_PROJECTION_MAP.put(ShoppingContract.Subtotals.STATUS, "status");		
	}
}
