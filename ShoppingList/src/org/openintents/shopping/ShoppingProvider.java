/* 
 * Copyright (C) 2007-2008 OpenIntents.org
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

package org.openintents.shopping;

import java.util.HashMap;

import org.openintents.intents.ProviderIntents;
import org.openintents.intents.ProviderUtils;
import org.openintents.shopping.library.provider.Shopping;
import org.openintents.shopping.library.provider.Shopping.Contains;
import org.openintents.shopping.library.provider.Shopping.ContainsFull;
import org.openintents.shopping.library.provider.Shopping.ItemStores;
import org.openintents.shopping.library.provider.Shopping.Items;
import org.openintents.shopping.library.provider.Shopping.Lists;
import org.openintents.shopping.library.provider.Shopping.Status;
import org.openintents.shopping.library.provider.Shopping.Stores;
import org.openintents.shopping.library.provider.Shopping.Units;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * Provides access to a database of shopping items and shopping lists.
 * 
 */
public class ShoppingProvider extends ContentProvider {

	// private SQLiteOpenHelper mOpenHelper;
	private DatabaseHelper mOpenHelper;

	private static final String TAG = "ShoppingProvider";
	private static final boolean debug = false;
	static final String DATABASE_NAME = "shopping.db";

	/**
	 * Version of database.
	 * 
	 * The various versions were introduced in the following releases:
	 * 
	 * 1: Release 0.1.1
	 * 2: Release 0.1.6
	 * 3: Release 1.0.4-beta
	 * 4: Release 1.0.4-beta
	 * 5: Release 1.2.7-beta 
	 * 6: Release 1.2.7-beta 
	 * 7: Release 1.2.7-beta 
	 * 8: Release 1.2.7-beta
	 * 9: Release 1.3.0
     * 10: Release 1.3.1-beta
	 */
	private static final int DATABASE_VERSION = 10;

	private static HashMap<String, String> ITEMS_PROJECTION_MAP;
	private static HashMap<String, String> LISTS_PROJECTION_MAP;
	private static HashMap<String, String> CONTAINS_PROJECTION_MAP;
	private static HashMap<String, String> CONTAINS_FULL_PROJECTION_MAP;
	private static HashMap<String, String> CONTAINS_FULL_CHEAPEST_PROJECTION_MAP;
	private static HashMap<String, String> STORES_PROJECTION_MAP;
	private static HashMap<String, String> ITEMSTORES_PROJECTION_MAP;
	private static HashMap<String, String> NOTES_PROJECTION_MAP;
	private static HashMap<String, String> UNITS_PROJECTION_MAP;


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

	// Derived tables
	private static final int CONTAINS_FULL = 101; // combined with items and
	// lists
	private static final int CONTAINS_FULL_ID = 102;
	private static final int ACTIVELIST = 103;

	private static final UriMatcher URL_MATCHER;

	/**
	 * ShoppingProvider maintains the following tables: 
	 * * items: items you want to buy 
	 * * lists: shopping lists ("My shopping list",	"Bob's shopping list") 
	 * * contains: which item/list/(recipe) is contained in which shopping list. 
	 * * stores: 
	 * * itemstores: (which store carries which item) 
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/**
		 * Creates tables "items", "lists", and "contains".
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE items (" + "_id INTEGER PRIMARY KEY," // V1
					+ "name VARCHAR," // V1
					+ "image VARCHAR," // V1
					+ "price INTEGER," // V3
					+ "units VARCHAR," // V8
					+ "tags VARCHAR," // V3
					+ "barcode VARCHAR," // V4
					+ "location VARCHAR," // V4
					+ "note VARCHAR" // V7
					+ "due INTEGER," // V4
					+ "created INTEGER," // V1
					+ "modified INTEGER," // V1
					+ "accessed INTEGER" // V1					
					+ ");");
			db.execSQL("CREATE TABLE lists (" + "_id INTEGER PRIMARY KEY," // V1
					+ "name VARCHAR," // V1
					+ "image VARCHAR," // V1
					+ "created INTEGER," // V1
					+ "modified INTEGER," // V1
					+ "accessed INTEGER," // V1
					+ "share_name VARCHAR," // V2
					+ "share_contacts VARCHAR," // V2
					+ "skin_background VARCHAR," // V2
					+ "skin_font VARCHAR," // V2
					+ "skin_color INTEGER," // V2
					+ "skin_color_strikethrough INTEGER" // V2					
					+ ");");
			db.execSQL("CREATE TABLE contains (" + "_id INTEGER PRIMARY KEY," // V1
					+ "item_id INTEGER," // V1
					+ "list_id INTEGER," // V1
					+ "quantity VARCHAR," // V1
					+ "status INTEGER," // V1
					+ "created INTEGER," // V1
					+ "modified INTEGER," // V1
					+ "accessed INTEGER," // V1
					+ "share_created_by VARCHAR," // V2
					+ "share_modified_by VARCHAR," // V2
					+ "sort_key INTEGER," // V3
					+ "priority INTEGER" // V6
					+ ");");
			db.execSQL("CREATE TABLE stores (" + "_id INTEGER PRIMARY KEY," // V5
					+ "name VARCHAR, " // V5
					+ "list_id INTEGER," // V5
					+ "created INTEGER," // V5
					+ "modified INTEGER" // V5
					+ ");");
			db.execSQL("CREATE TABLE itemstores(" + "_id INTEGER PRIMARY KEY," // V5
					+ "item_id INTEGER," // V5
					+ "store_id INTEGER," // V5
					+ "stocks_item INTEGER DEFAULT 1," //V10
					+ "aisle INTEGER," // V5
					+ "price INTEGER," // V5
					+ "created INTEGER," // V5
					+ "modified INTEGER" // V5
					+ ");");
			db.execSQL("CREATE TABLE units (" + "_id INTEGER PRIMARY KEY," // V8
					+ "name VARCHAR, " // V8
					+ "singular VARCHAR, " // V8
					+ "created INTEGER," // V8
					+ "modified INTEGER" // V8
					+ ");");
			
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ".");
			if (newVersion > oldVersion) {
				// Upgrade
				switch (oldVersion) {
				case 2:
					// Upgrade from version 2
					// It seems SQLite3 only allows to add one column at a time,
					// so we need three SQL statements:
					try {
						db.execSQL("ALTER TABLE items ADD COLUMN "
								+ Items.PRICE + " INTEGER;");
						db.execSQL("ALTER TABLE items ADD COLUMN "
								+ Items.TAGS + " VARCHAR;");
						db.execSQL("ALTER TABLE contains ADD COLUMN "
								+ Contains.SORT_KEY + " INTEGER;");
					} catch (SQLException e) {
						Log.e(TAG, "Error executing SQL: ", e);
						// If the error is "duplicate column name" then
						// everything is fine,
						// as this happens after upgrading 2->3, then
						// downgrading 3->2,
						// and then upgrading again 2->3.
					}
					// fall through for further upgrades.
				case 3:
					try {
						db.execSQL("ALTER TABLE items ADD COLUMN "
								+ Items.BARCODE + " VARCHAR;");
						db.execSQL("ALTER TABLE items ADD COLUMN "
								+ Items.LOCATION + " VARCHAR;");
						db.execSQL("ALTER TABLE items ADD COLUMN "
								+ Items.DUE_DATE + " INTEGER;");

					} catch (SQLException e) {
						Log.e(TAG, "Error executing SQL: ", e);
						// If the error is "duplicate column name" then
						// everything is fine,
						// as this happens after upgrading 2->3, then
						// downgrading 3->2,
						// and then upgrading again 2->3.
					}
					// fall through for further upgrades.
				case 4:
					try {						
						db.execSQL("CREATE TABLE stores (" + "_id INTEGER PRIMARY KEY," // V5
								+ "name VARCHAR, " // V5
								+ "list_id INTEGER," // V5
								+ "created INTEGER," // V5
								+ "modified INTEGER" // V5
								+ ");");
						db.execSQL("CREATE TABLE itemstores(" + "_id INTEGER PRIMARY KEY," // V5
								+ "item_id INTEGER," // V5
								+ "store_id INTEGER," // V5
								+ "aisle INTEGER," // V5
								+ "price INTEGER," // V5
								+ "created INTEGER," // V5
								+ "modified INTEGER" // V5
								+ ");");
					} catch (SQLException e) {
						Log.e(TAG, "Error executing SQL: ", e);
					}
				case 5:
					try {				
						db.execSQL("ALTER TABLE contains ADD COLUMN "
								+ Contains.PRIORITY + " INTEGER;");
					} catch (SQLException e) {
						Log.e(TAG, "Error executing SQL: ", e);
					}
					
			    case 6:
			    	try {				
						db.execSQL("ALTER TABLE items ADD COLUMN "
								+ Items.NOTE + " VARCHAR;");
					} catch (SQLException e) {
						Log.e(TAG, "Error executing SQL: ", e);
					}

			    case 7:
			    	try {				
						db.execSQL("ALTER TABLE items ADD COLUMN "
								+ Items.UNITS + " VARCHAR;");
						db.execSQL("CREATE TABLE units (" + "_id INTEGER PRIMARY KEY," // V8
								+ "name VARCHAR, " // V8
								+ "singular VARCHAR, " // V8	
								+ "created INTEGER," // V8
								+ "modified INTEGER" // V8
								+ ");");
						
					} catch (SQLException e) {
						Log.e(TAG, "Error executing SQL: ", e);
					}
			    case 8:
			    	try {
			    		// There is no simple command in sqlite to change the type of a
			    		// field.
			    		// -> copy the whole table to change type of aisle
			    		//    from INTEGER to VARCHAR.
			    		// (see http://www.sqlite.org/faq.html#q11 )
			    		// ("BEGIN TRANSACTION;" and "COMMIT;" are not valid
			    		//  because we are already within a transaction.)
						//db.execSQL("CREATE TEMPORARY TABLE itemstores_backup("
						//			+ "_id INTEGER PRIMARY KEY," // V5
						//			+ "item_id INTEGER," // V5
						//			+ "store_id INTEGER," // V5
						//			+ "aisle INTEGER," // V5:INTEGER, (V9:VARCHAR)
						//			+ "price INTEGER," // V5
						//			+ "created INTEGER," // V5
						//			+ "modified INTEGER" // V5
						//			+ ");");
						//db.execSQL("INSERT INTO itemstores_backup SELECT "
						//			+ "_id,item_id,store_id,aisle,price,created,modified"
						//			+ " FROM itemstores;");
						//db.execSQL("DROP TABLE itemstores;");
						//db.execSQL("CREATE TABLE itemstores("
						//			+ "_id INTEGER PRIMARY KEY," // V5
						//			+ "item_id INTEGER," // V5
						//			+ "store_id INTEGER," // V5
						//			+ "aisle VARCHAR," // (V5:INTEGER), V9
						//			+ "price INTEGER," // V5
						//			+ "created INTEGER," // V5
						//			+ "modified INTEGER" // V5
						//			+ ");");
						//db.execSQL("INSERT INTO itemstores SELECT "
						//			+ "_id,item_id,store_id,aisle,price,created,modified"
						//			+ " FROM itemstores_backup;");
						//db.execSQL("DROP TABLE itemstores_backup;");

						// Replace "-1" values by "".
		            	ContentValues values = new ContentValues();
		            	values.put(ItemStores.AISLE, "");
		                db.update("itemstores", values, "aisle = '-1'", null);
						
					} catch (SQLException e) {
						Log.e(TAG, "Error executing SQL: ", e);
					}

		        case 9:
			    	try {				
						db.execSQL("ALTER TABLE itemstores ADD COLUMN "
								+ ItemStores.STOCKS_ITEM + " INTEGER DEFAULT 1;");
						
					} catch (SQLException e) {
						Log.e(TAG, "Error executing SQL: ", e);
					}

					/**
					* case 10:
					*/
					break;
				default:
					Log.w(TAG, "Unknown version " + oldVersion
							+ ". Creating new database.");
					db.execSQL("DROP TABLE IF EXISTS items");
					db.execSQL("DROP TABLE IF EXISTS lists");
					db.execSQL("DROP TABLE IF EXISTS contains");
					db.execSQL("DROP TABLE IF EXISTS stores");
					db.execSQL("DROP TABLE IF EXISTS itemstores");
					db.execSQL("DROP TABLE IF EXISTS units");
					onCreate(db);
				}
			} else { // newVersion <= oldVersion
				// Downgrade
				Log
						.w(
								TAG,
								"Don't know how to downgrade. Will not touch database and hope they are compatible.");
				// Do nothing.
			}

		}
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri url, String[] projection, String selection,
			String[] selectionArgs, String sort) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

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
				groupBy = "itemstores.item_id";
				
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
			// path segment 1 is "item", path segment 2 is item id.
			qb.setTables("stores left outer join itemstores on (stores._id = itemstores.store_id and " +
					"itemstores.item_id = " + url.getPathSegments().get(2) + ")");
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
			long list_id = sp.getInt("lastused", 1);
			m.addRow(new Object [] {Long.toString(list_id)});
			return (Cursor)m;
		case PREFS:
			m = new MatrixCursor(projection);
			// assumes only one projection will ever be used, 
			// asking only for the id of the active list.
			String sortOrder = PreferenceActivity.getSortOrderFromPrefs(getContext());
			m.addRow(new Object [] {sortOrder});
			return (Cursor)m;
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
		Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy,
				null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), url);
		return c;
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
		Long now = Long.valueOf(System.currentTimeMillis());
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
			secondUri  = Shopping.Items.CONTENT_URI;
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
			return Shopping.ITEM_TYPE;

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
			return Shopping.Notes.CONTENT_TYPE; 
		case NOTE_ID: 
			return Shopping.Notes.CONTENT_ITEM_TYPE;
			
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
		URL_MATCHER.addURI("org.openintents.shopping", "containsfull",
				CONTAINS_FULL);
		URL_MATCHER.addURI("org.openintents.shopping", "containsfull/#",
				CONTAINS_FULL_ID);
		URL_MATCHER.addURI("org.openintents.shopping", "stores", STORES);
		URL_MATCHER.addURI("org.openintents.shopping", "stores/#", STORES_ID);
		URL_MATCHER.addURI("org.openintents.shopping", "itemstores", ITEMSTORES);
		URL_MATCHER.addURI("org.openintents.shopping", "itemstores/#", 
				ITEMSTORES_ID);
		URL_MATCHER.addURI("org.openintents.shopping", "itemstores/item/#", 
				ITEMSTORES_ITEMID);
		URL_MATCHER.addURI("org.openintents.shopping", "liststores/#", 
				STORES_LISTID);
		URL_MATCHER.addURI("org.openintents.shopping", "notes", NOTES);
		URL_MATCHER.addURI("org.openintents.shopping", "notes/#", NOTE_ID);
		URL_MATCHER.addURI("org.openintents.shopping", "units", UNITS);
		URL_MATCHER.addURI("org.openintents.shopping", "units/#", UNITS_ID);
		
		URL_MATCHER.addURI("org.openintents.shopping", "prefs", PREFS);


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
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull._ID, "contains._id");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_ID,
				"contains.item_id");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.LIST_ID,
				"contains.list_id");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.QUANTITY,
				"contains.quantity");
		CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.PRIORITY,
		"contains.priority");
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
		        "items.note is not NULL as item_has_note");

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

		NOTES_PROJECTION_MAP = new HashMap<String, String>();
		NOTES_PROJECTION_MAP.put(Shopping.Notes._ID, "items._id");
		NOTES_PROJECTION_MAP.put(Shopping.Notes.NOTE, "items.note");
		NOTES_PROJECTION_MAP.put(Shopping.Notes.TITLE, "null as title");
		NOTES_PROJECTION_MAP.put(Shopping.Notes.TAGS, "null as tags");
		NOTES_PROJECTION_MAP.put(Shopping.Notes.ENCRYPTED, "null as encrypted");
		NOTES_PROJECTION_MAP.put(Shopping.Notes.THEME, "null as theme");
	}
}
