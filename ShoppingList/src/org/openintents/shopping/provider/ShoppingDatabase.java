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

import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.ItemStores;
import org.openintents.shopping.library.provider.ShoppingContract.Items;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ShoppingDatabase extends SQLiteOpenHelper {

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
	 * 11: Release 1.4.0-beta
	 */
	static final int DATABASE_VERSION = 11;

	public static final String DATABASE_NAME = "shopping.db";
	
	ShoppingDatabase(Context context) {
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
		db.execSQL("CREATE INDEX itemstores_item_id on itemstores "
				+ " ( item_id asc, price asc );"); // V11
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		Log.w(ShoppingProvider.TAG, "Upgrading database from version " + oldVersion + " to "
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
					Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
					// If the error is "duplicate column name" then
					// everything is fine,
					// as this happens after upgrading 2->3, then
					// downgrading 3->2,
					// and then upgrading again 2->3.
				}
				// NO break; - fall through for further upgrades.
			case 3:
				try {
					db.execSQL("ALTER TABLE items ADD COLUMN "
							+ Items.BARCODE + " VARCHAR;");
					db.execSQL("ALTER TABLE items ADD COLUMN "
							+ Items.LOCATION + " VARCHAR;");
					db.execSQL("ALTER TABLE items ADD COLUMN "
							+ Items.DUE_DATE + " INTEGER;");
				} catch (SQLException e) {
					Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
					// If the error is "duplicate column name" then
					// everything is fine,
					// as this happens after upgrading 2->3, then
					// downgrading 3->2,
					// and then upgrading again 2->3.
				}
				// NO break; - fall through for further upgrades.
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
					Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
				}
				// NO break;
			case 5:
				try {
					db.execSQL("ALTER TABLE contains ADD COLUMN "
							+ Contains.PRIORITY + " INTEGER;");
				} catch (SQLException e) {
					Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
				}
				// NO break;
			case 6:
				try {
					db.execSQL("ALTER TABLE items ADD COLUMN " + Items.NOTE
							+ " VARCHAR;");
				} catch (SQLException e) {
					Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
				}
				// NO break;
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
					Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
				}
				// NO break;
			case 8:
				try {
					// There is no simple command in sqlite to change the type
					// of a field.
					// -> copy the whole table to change type of aisle
					// from INTEGER to VARCHAR.
					// (see http://www.sqlite.org/faq.html#q11 )
					// ("BEGIN TRANSACTION;" and "COMMIT;" are not valid
					// because we are already within a transaction.)
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
					Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
				}
				// NO break;
			case 9:
				try {
					db.execSQL("ALTER TABLE itemstores ADD COLUMN "
							+ ItemStores.STOCKS_ITEM + " INTEGER DEFAULT 1;");
					
				} catch (SQLException e) {
					Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
				}
				// NO break;
			case 10:
				db.execSQL("CREATE INDEX IF NOT EXISTS "
						+ " itemstores_item_id on itemstores "
						+ " ( item_id asc, price asc );"); // V11
				// NO break;

				// NO break UNTIL HERE!
				break;
			default:
				Log.w(ShoppingProvider.TAG, "Unknown version " + oldVersion
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
			Log.w(ShoppingProvider.TAG,
					"Don't know how to downgrade. Will not touch database and hope they are compatible.");
			// Do nothing.
		}

	}
}