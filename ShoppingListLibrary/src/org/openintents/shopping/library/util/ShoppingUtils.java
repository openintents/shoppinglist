package org.openintents.shopping.library.util;


import org.openintents.shopping.library.provider.Shopping;
import org.openintents.shopping.library.provider.Shopping.ActiveList;
import org.openintents.shopping.library.provider.Shopping.Contains;
import org.openintents.shopping.library.provider.Shopping.ItemStores;
import org.openintents.shopping.library.provider.Shopping.Items;
import org.openintents.shopping.library.provider.Shopping.Lists;
import org.openintents.shopping.library.provider.Shopping.Status;
import org.openintents.shopping.library.provider.Shopping.Stores;
import org.openintents.shopping.library.provider.Shopping.Units;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class ShoppingUtils {
	/**
	 * TAG for logging.
	 */
	private static final String TAG = "ShoppingUtils";

	/**
	 * Obtain item id by name.
	 * @param context
	 * @param name
	 * @return Item ID or -1 if item does not exist.
	 */
	private static long getItemId(Context context, String name) {
		long id = -1;
		
		Cursor existingItems = context.getContentResolver().query(Shopping.Items.CONTENT_URI,
				new String[] { Shopping.Items._ID }, "upper(name) = upper(?)",
				new String[] { name }, null);
		if (existingItems.getCount() > 0) {
			existingItems.moveToFirst();
			id = existingItems.getLong(0);
		};
		existingItems.close();
		return id;
	}
	
	/**
	 * Gets or creates a new item and returns its id. If the item exists
	 * already, the existing id is returned. Otherwise a new item is created.
	 * @param name New name of the item.
	 * @param price
	 * @param barcode
	 * 
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

	// Some convenience functions follow	

	/**
	 * Gets or creates a new item and returns its id. If the item exists
	 * already, the existing id is returned. Otherwise a new item is created.
	 * 
	 * @param name
	 *            New name of the item.
	 * @return id of the new or existing item.
	 */
	public static long getItem(Context context, String name, String tags, 
			String price, String units, String note, Boolean duplicate, Boolean update) {
		long id = -1;
		
		if (!duplicate) {
			Cursor existingItems = context.getContentResolver().query(Items.CONTENT_URI,
				new String[] { Items._ID }, "upper(name) = ?",
				new String[] { name.toUpperCase() }, null);
			if (existingItems.getCount() > 0) {
				existingItems.moveToFirst();
				id = existingItems.getLong(0);
			}
			existingItems.close();
			
			if (id != -1 && !update) {
				return id;
			}
		} 
		
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
		if (price != null){
			values.put(Items.PRICE, price);
		}
		if (!TextUtils.isEmpty(units)){
			// in the items table we store the string directly,
			// but we register the units in the units table for use in 
			// completion.
			long unit_id = getUnits(context, units);
			values.put(Items.UNITS, units);
		}
		
		try {
			if (id == -1) {
				Uri uri = context.getContentResolver().insert(Items.CONTENT_URI, values);
				Log.i(TAG, "Insert new item: " + uri);
				id = Long.parseLong(uri.getPathSegments().get(1));
			} else {
				context.getContentResolver().update(Uri.withAppendedPath(Items.CONTENT_URI, 
						String.valueOf(id)), values, null, null);
			}
		} catch (Exception e) {
			Log.i(TAG, "Insert item failed", e);
			// return -1
		}
		
		return id;
	}

	public static long getUnits(Context context, String units) {
		long id = -1;
		Cursor existingUnits = context.getContentResolver().query(Units.CONTENT_URI,
				new String[] { Units._ID }, "upper(name) = ?",
				new String[] { units.toUpperCase() }, null);
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
				Uri uri = context.getContentResolver().insert(Units.CONTENT_URI, values);
				Log.i(TAG, "Insert new units: " + uri);
				id = Long.parseLong(uri.getPathSegments().get(1));
			} catch (Exception e) {
				Log.i(TAG, "Insert units failed", e);
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
	 * @param name
	 *            New name of the list.
	 * @return id of the new or existing list.
	 */
	public static long getList(Context context, final String name) {
		long id = -1;
		Cursor existingItems = context.getContentResolver().query(Lists.CONTENT_URI,
				new String[] { Items._ID }, "upper(name) = ?",
				new String[] { name.toUpperCase() }, null);
		if (existingItems.getCount() > 0) {
			existingItems.moveToFirst();
			id = existingItems.getLong(0);
			existingItems.close();
		} else {
			// Add list to list:
			ContentValues values = new ContentValues(1);
			values.put(Lists.NAME, name);
			try {
				Uri uri = context.getContentResolver().insert(Lists.CONTENT_URI, values);
				Log.i(TAG, "Insert new list: " + uri);
				id = Long.parseLong(uri.getPathSegments().get(1));
			} catch (Exception e) {
				Log.i(TAG, "insert list failed", e);
				return -1;
			}
		}
		return id;
	}

	/**
	 * Gets or creates a new store and returns its id. If the store
	 * exists already, the existing id is returned. Otherwise a new store is
	 * created.
	 * 
	 * @param context 
	 * @param name
	 *            New name of the list.
	 * @return id of the new or existing list.
	 */
	public static long getStore(Context context, final String name, final long listId) {
		long id = -1;
		Cursor existingItems = context.getContentResolver().query(Stores.CONTENT_URI,
				new String[] { Stores._ID }, "upper(name) = ? AND list_id = ?",
				new String[] { name.toUpperCase(), String.valueOf(listId) }, null);
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
				Uri uri = context.getContentResolver().insert(Stores.CONTENT_URI, values);
				Log.i(TAG, "Insert new store: " + uri);
				id = Long.parseLong(uri.getPathSegments().get(1));
			} catch (Exception e) {
				Log.i(TAG, "insert store failed", e);
				return -1;
			}
		}
		return id;
	}
	
	
	/**
	 * Adds a new item to a specific list and returns its id. If the item exists
	 * already, the existing id is returned.
	 * 
	 * @param itemId
	 *            The id of the new item.
	 * @param listId
	 *            The id of the shopping list the item is added.
	 * @param itemType
	 *            The type of the new item
	 * @param togglestatus
	 *            If true, then status is toggled between WANT_TO_BUY and BOUGHT
	 * @return id of the "contains" table entry, or -1 if insert failed.
	 */
	public static long addItemToList(Context context, final long itemId,
			final long listId,	final long status, String priority, 
			String quantity, final boolean togglestatus) {
		long id = -1;
		Cursor existingItems = context.getContentResolver()
				.query(Contains.CONTENT_URI, new String[] { Contains._ID, Contains.STATUS },
						"list_id = ? AND item_id = ?",
						new String[] { String.valueOf(listId),
								String.valueOf(itemId) }, null);
		if (existingItems.getCount() > 0) {
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
				values.put(Shopping.Contains.QUANTITY, quantity);
			}
			if (priority != null) {
				values.put(Shopping.Contains.PRIORITY, priority);
			}
			
			Uri uri = Uri.withAppendedPath(Contains.CONTENT_URI, String.valueOf(id));
			try {
				context.getContentResolver().update(uri, values, null, null);
				Log.i(TAG, "updated item: " + uri);
			} catch (Exception e) {
				try {
					// Maybe old version of OI Shopping List is installed:
					values.remove(Contains.PRIORITY);
					context.getContentResolver().update(uri, values, null, null);
					Log.i(TAG, "updated item: " + uri);
				} catch (Exception e2) {
					Log.i(TAG, "insert into table 'contains' failed", e2);
					id = -1;
				}
			}
			
		} else {
			existingItems.close();
			// Add item to list:
			ContentValues values = new ContentValues(2);
			values.put(Contains.ITEM_ID, itemId);
			values.put(Contains.LIST_ID, listId);
			if (togglestatus) {
				values.put(Contains.STATUS, Status.WANT_TO_BUY);
			} else {
				values.put(Contains.STATUS, status);
			}
			values.put(Contains.QUANTITY, quantity);
			values.put(Contains.PRIORITY, priority);

			try {
				Uri uri = context.getContentResolver().insert(Contains.CONTENT_URI, values);
				Log.i(TAG, "Insert new entry in 'contains': " + uri);
				id = Long.parseLong(uri.getPathSegments().get(1));
			} catch (Exception e) {
				try {
					// Maybe old version of OI Shopping List is installed:
					values.remove(Contains.PRIORITY);
					Uri uri = context.getContentResolver().insert(Contains.CONTENT_URI, values);
					Log.i(TAG, "Insert new entry in 'contains': " + uri);
					id = Long.parseLong(uri.getPathSegments().get(1));
				} catch (Exception e2) {
					Log.i(TAG, "insert into table 'contains' failed", e2);
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
	 * @param itemId
	 *            The id of the new item.
	 * @param listId
	 *            The id of the shopping list the item is added.
	 * @param itemType
	 *            The type of the new item
	 * @return id of the "contains" table entry, or -1 if insert failed.
	 */
	public static long addItemToStore(Context context, final long itemId, final long storeId, final long aisle, final String price) {
		long id = -1;
		Cursor existingItems = context.getContentResolver()
				.query(ItemStores.CONTENT_URI, new String[] { ItemStores._ID },
						"store_id = ? AND item_id = ?",
						new String[] { String.valueOf(storeId),
								String.valueOf(itemId) }, null);
		if (existingItems.getCount() > 0) {
			existingItems.moveToFirst();
			id = existingItems.getLong(0);
			existingItems.close();
			
			// update aisle and price:
			ContentValues values = new ContentValues(1);
			values.put(ItemStores.PRICE, price);
			values.put(ItemStores.AISLE, aisle);
			try {
				Uri uri = Uri.withAppendedPath(ItemStores.CONTENT_URI, String.valueOf(id));
				context.getContentResolver().update(uri, values, null, null);
				Log.i(TAG, "updated itemstore: " + uri);				
			} catch (Exception e) {
				Log.i(TAG, "Update itemstore failed", e);				
			}
			
		} else {
			existingItems.close();
			// Add item to list:
			ContentValues values = new ContentValues(2);
			values.put(ItemStores.ITEM_ID, itemId);
			values.put(ItemStores.STORE_ID, storeId);
			values.put(ItemStores.PRICE, price);
			values.put(ItemStores.AISLE, aisle);
			try {
				Uri uri = context.getContentResolver().insert(ItemStores.CONTENT_URI, values);
				Log.i(TAG, "Insert new entry in 'itemstores': " + uri);
				id = Long.parseLong(uri.getPathSegments().get(1));
			} catch (Exception e) {
				Log.i(TAG, "insert into table 'itemstores' failed", e);
				id = -1;
			}
		}
		return id;
	}

	
	/**
	 * Returns the id of the default shopping list. Currently this is always 1.
	 * 
	 * @return The id of the default shopping list.
	 */
	public static long getDefaultList(Context context) {
		long id = 1;
		Cursor c = context.getContentResolver().query(ActiveList.CONTENT_URI,
				ActiveList.PROJECTION, null, null, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			id = c.getLong(0);
			c.close();
		}
		return id;
	}	

	public static Uri getListForItem(Context context, String itemId) {
		Cursor cursor = context.getContentResolver().query(Contains.CONTENT_URI,
				new String[] { Contains.LIST_ID }, Contains.ITEM_ID + " = ?",
				new String[] { itemId }, Contains.DEFAULT_SORT_ORDER);
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

	public static void addTagToItem(Context context, long itemId, String store) {
		String allTags = "";
		Cursor existingTags = context.getContentResolver().query(Items.CONTENT_URI,
				new String[] { Items.TAGS }, "_id = ?",
				new String[] { String.valueOf(itemId) }, null);
		if (existingTags.getCount() > 0) {
			existingTags.moveToFirst();
			allTags = existingTags.getString(0);
			existingTags.close();
		} 

		if (!TextUtils.isEmpty(allTags)) {
			allTags = allTags + ", " + store;
		} else {
			allTags = store;
		}
		
		ContentValues values = new ContentValues(1);
		values.put(Items.TAGS, allTags);
		
		context.getContentResolver().update(Uri.withAppendedPath(Items.CONTENT_URI, 
				String.valueOf(itemId)), values, null, null);
	}
}
