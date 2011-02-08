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

package org.openintents.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/**
 * Definition for content provider related to shopping.
 * 
 * Version corresponds to main version of OI Shopping List.
 * @version 1.2.7
 */
public abstract class Shopping {

	/**
	 * TAG for logging.
	 */
	private static final String TAG = "Shopping";
	public static final String ITEM_TYPE = "vnd.android.cursor.item/vnd.openintents.shopping.item";
	public static final String QUERY_ITEMS_WITH_STATE = "itemsWithState";

	/**
	 * Items that can be put into shopping lists.
	 */
	public static final class Items implements BaseColumns {
		/**
		 * The content:// style URL for this table.
		 */
		public static final Uri CONTENT_URI = Uri
				.parse("content://org.openintents.shopping/items");

		/**
		 * The default sort order for this table.
		 */
		public static final String DEFAULT_SORT_ORDER = "modified ASC";

		/**
		 * The name of the item.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String NAME = "name";

		/**
		 * An image of the item (uri).
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String IMAGE = "image";

		/**
		 * A price for the item (in cent)
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String PRICE = "price";

		/**
		 * Units for the item
		 * <P>
		 * Type: VARCHAR
		 * </P>
		 */
		public static final String UNITS = "units";
		
		/**
		 * Tags for the item
		 * <P>
		 * Type: VARCHAR
		 * </P>
		 */
		public static final String TAGS = "tags";
		
		/**
		 * A barcode (EAN or QR)
		 * <P>
		 * Type: VARCHAR
		 * </P>
		 */
		public static final String BARCODE = "barcode";

		/**
		 * a location where to find it, as geo:lat,long uri
		 * <P>
		 * Type: VARCHAR
		 * </P>
		 */
		public static final String LOCATION = "location";

		/**
		 * text of a note about the item
		 * <P>
		 * Type: VARCHAR
		 * </P>
		 */
		public static final String NOTE = "note";

		/**
		 * The timestamp for when the item was created.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String CREATED_DATE = "created";

		/**
		 * The timestamp for when the item was last modified.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String MODIFIED_DATE = "modified";

		/**
		 * The timestamp for when the item was last accessed.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String ACCESSED_DATE = "accessed";

		/**
		 * The timestamp for when the item is due.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String DUE_DATE = "due";

		/**
		 * Generic projection map.
		 */
		public static final String[] PROJECTION = { _ID, NAME, IMAGE, PRICE,
				CREATED_DATE, MODIFIED_DATE, ACCESSED_DATE, UNITS };

		/**
		 * Offset in PROJECTION array.
		 */
		public static final int PROJECTION_ID = 0;
		public static final int PROJECTION_NAME = 1;
		public static final int PROJECTION_IMAGE = 2;
		public static final int PROJECTION_PRICE = 3;
		public static final int PROJECTION_CREATED_DATE = 4;
		public static final int PROJECTION_MODIFIED_DATE = 5;
		public static final int PROJECTION_ACCESSED_DATE = 6;
		public static final int PROJECTION_UNITS = 7;

	}

	/**
	 * Shopping lists that can contain items.
	 */
	public static final class Lists implements BaseColumns {
		/**
		 * The content:// style URL for this table.
		 */
		public static final Uri CONTENT_URI = Uri
				.parse("content://org.openintents.shopping/lists");

		/**
		 * The default sort order for this table.
		 */
		public static final String DEFAULT_SORT_ORDER
		// = "modified DESC";
		= "modified ASC";

		/**
		 * The name of the list.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String NAME = "name";

		/**
		 * An image of the list (uri).
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String IMAGE = "image";

		/**
		 * The timestamp for when the item was created.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String CREATED_DATE = "created";

		/**
		 * The timestamp for when the item was last modified.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String MODIFIED_DATE = "modified";

		/**
		 * The timestamp for when the item was last accessed.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String ACCESSED_DATE = "accessed";

		/**
		 * The name of the shared shopping list that should be worldwide unique.
		 * 
		 * It is formed of the current user's email address and a unique suffix.
		 * 
		 * <P>
		 * Type: TEXT
		 * </P>
		 * Available since release 0.1.6.
		 */
		public static final String SHARE_NAME = "share_name";

		/**
		 * The comma separated list of contacts with whom this list is shared.
		 * 
		 * <P>
		 * Type: TEXT
		 * </P>
		 * Available since release 0.1.6.
		 */
		public static final String SHARE_CONTACTS = "share_contacts";

		/**
		 * Name of background image.
		 * 
		 * <P>
		 * Type: TEXT
		 * </P>
		 * Available since release 0.1.6.
		 */
		public static final String SKIN_BACKGROUND = "skin_background";

		/**
		 * Name of font in list.
		 * 
		 * <P>
		 * Type: TEXT
		 * </P>
		 * Available since release 0.1.6.
		 */
		public static final String SKIN_FONT = "skin_font";

		/**
		 * Color of text in list.
		 * 
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 * Available since release 0.1.6.
		 */
		public static final String SKIN_COLOR = "skin_color";

		/**
		 * Color of strikethrough text in list.
		 * 
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 * Available since release 0.1.6.
		 */
		public static final String SKIN_COLOR_STRIKETHROUGH = "skin_color_strikethrough";
	}

	/**
	 * Information which list contains which items/lists/(recipes)
	 */
	public static final class Contains implements BaseColumns {
		/**
		 * The content:// style URL for this table.
		 */
		public static final Uri CONTENT_URI = Uri
				.parse("content://org.openintents.shopping/contains");

		/**
		 * The default sort order for this table.
		 */
		public static final String DEFAULT_SORT_ORDER = "modified DESC";

		/**
		 * The id of the item.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String ITEM_ID = "item_id";

		/**
		 * The id of the list that contains item_id.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String LIST_ID = "list_id";

		/**
		 * Quantity specifier.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String QUANTITY = "quantity";

		/**
		 * Priority specifier.
		 * <P>
		 * Type: INTEGER (long) 1-5
		 * </P>
		 */
		public static final String PRIORITY = "priority";
		
		/**
		 * Status: WANT_TO_BUY or BOUGHT.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String STATUS = "status";

		/**
		 * The timestamp for when the item was created.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String CREATED_DATE = "created";

		/**
		 * The timestamp for when the item was last modified.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String MODIFIED_DATE = "modified";

		/**
		 * The timestamp for when the item was last accessed.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String ACCESSED_DATE = "accessed";

		/**
		 * Name of person who inserted the item.
		 * 
		 * <P>
		 * Type: TEXT
		 * </P>
		 * Available since release 0.1.6.
		 */
		public static final String SHARE_CREATED_BY = "share_created_by";

		/**
		 * Name of person who changed status of the item, for example mark it as
		 * bought.
		 * 
		 * <P>
		 * Type: TEXT
		 * </P>
		 * Available since release 0.1.6.
		 */
		public static final String SHARE_MODIFIED_BY = "share_modified_by";

		
		/**
		 * sort key with in the list
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String SORT_KEY = "sort_key";
		/**
		 * Support sort orders. The "sort order" in the preferences is an index
		 * into this array.
		 */
		public static final String[] SORT_ORDERS = {
				"contains.status ASC, items.name ASC", // unchecked first, alphabetical
				"items.name ASC",
				"contains.modified DESC", 
				"contains.modified ASC",
				"(items.tags IS NULL or items.tags = '') ASC, items.tags ASC, items.name ASC", // sort by tags, but put empty tags last.
				"items.price DESC, items.name ASC",
				"contains.status ASC, (items.tags IS NULL or items.tags = '') ASC, items.tags ASC, items.name ASC", // unchecked first, tags alphabetical, but put empty tags last.
				"contains.status ASC, contains.priority ASC, items.name ASC", // unchecked first, priority, alphabetical
				};
		
	}

	/**
	 * Combined table of contents, items, and lists.
	 */
	public static final class ContainsFull implements BaseColumns {

		/**
		 * The content:// style URL for this table.
		 */
		public static final Uri CONTENT_URI = Uri
				.parse("content://org.openintents.shopping/containsfull");

		/**
		 * The default sort order for this table.
		 */
		public static final String DEFAULT_SORT_ORDER
		// = "contains.modified DESC";
		= "contains.modified ASC";

		// Elements from Contains

		/**
		 * The id of the item.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String ITEM_ID = "item_id";

		/**
		 * The id of the list that contains item_id.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String LIST_ID = "list_id";

		/**
		 * Quantity specifier.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String QUANTITY = "quantity";

		/**
		 * Priority specifier.
		 * <P>
		 * Type: INTEGER (long) 1-5
		 * </P>
		 */
		public static final String PRIORITY = "priority";
		
		/**
		 * Status: WANT_TO_BUY or BOUGHT.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String STATUS = "status";

		/**
		 * The timestamp for when the item was created.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String CREATED_DATE = "created";

		/**
		 * The timestamp for when the item was last modified.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String MODIFIED_DATE = "modified";

		/**
		 * The timestamp for when the item was last accessed.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String ACCESSED_DATE = "accessed";

		/**
		 * Name of person who inserted the item.
		 * 
		 * <P>
		 * Type: TEXT
		 * </P>
		 * Available since release 0.1.6.
		 */
		public static final String SHARE_CREATED_BY = "share_created_by";

		/**
		 * Name of person who crossed out the item.
		 * 
		 * <P>
		 * Type: TEXT
		 * </P>
		 * Available since release 0.1.6.
		 */
		public static final String SHARE_MODIFIED_BY = "share_modified_by";

		// Elements from Items

		/**
		 * The name of the item.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String ITEM_NAME = "item_name";

		/**
		 * An image of the item (uri).
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String ITEM_IMAGE = "item_image";

		/**
		 * A price of the item (in cent).
		 * <P>
		 * Type: INTEGER
		 * </P>
		 */
		public static final String ITEM_PRICE = "item_price";

		/**
		 * Units of the item.
		 * <P>
		 * Type: VARCHAR
		 * </P>
		 */
		public static final String ITEM_UNITS = "item_units";
		
		/**
		 * tags of the item.
		 * <P>
		 * Type: VARCHAR
		 * </P>
		 */
		public static final String ITEM_TAGS = "item_tags";
		
		// Elements from Lists

		/**
		 * The name of the list.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String LIST_NAME = "list_name";

		/**
		 * An image of the list (uri).
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String LIST_IMAGE = "list_image";

		/**
		 * A barcode (EAN or QR)
		 * <P>
		 * Type: VARCHAR
		 * </P>
		 */
		public static final String BARCODE = "barcode";

		/**
		 * a location where to find it, as geo:lat,long uri
		 * <P>
		 * Type: VARCHAR
		 * </P>
		 */
		public static final String LOCATION = "location";
		
		/**
		 * The timestamp for when the item is due.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String DUE_DATE = "due";
		
		/**
		 * Whether the item has a note.
		 * <P>
		 * Type: INTEGER 
		 * </P>
		 */
		public static final String ITEM_HAS_NOTE = "item_has_note";
	
	}

	/**
	 * Status of "contains" element.
	 */
	public static final class Status {

		/**
		 * Want to buy this item.
		 */
		public static final long WANT_TO_BUY = 1;

		/**
		 * Have bought this item.
		 */
		public static final long BOUGHT = 2;
		
		/**
		 * Have removed it from the list.
		 * Won't be deleted, in oder to keep reference for later suggestions.
		 */
		public static final long REMOVED_FROM_LIST = 3;

		/**
		 * Checks whether a status is a valid possibility.
		 * 
		 * @param s
		 *            status to be checked.
		 * @return true if status is a valid possibility.
		 */
		public static boolean isValid(final long s) {
			return s == WANT_TO_BUY || s == BOUGHT || s == REMOVED_FROM_LIST;
		}

	}

	/**
	 * Stores which might be able to sell items.
	 */
	public static final class Stores implements BaseColumns {
		/**
		 * The content:// style URL for this table.
		 */
		public static final Uri CONTENT_URI = Uri
				.parse("content://org.openintents.shopping/stores");

		/**
		 * The default sort order for this table.
		 */
		public static final String DEFAULT_SORT_ORDER = "name ASC";

		/**
		 * The name of the item.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String NAME = "name";
		
		/**
		 * The id of the list associated with this store.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String LIST_ID = "list_id";

		/**
		 * The timestamp for when the store was created.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String CREATED_DATE = "created";

		/**
		 * The timestamp for when the store was last modified.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String MODIFIED_DATE = "modified";

	}
	
	/**
	 * Items that can be put into shopping lists.
	 */
	public static final class ItemStores implements BaseColumns {
		/**
		 * The content:// style URL for this table.
		 */
		public static final Uri CONTENT_URI = Uri
				.parse("content://org.openintents.shopping/itemstores");

		/**
		 * The default sort order for this table.
		 */
		public static final String DEFAULT_SORT_ORDER = "item_id ASC";


		/**
		 * The timestamp for when the itemstore record was created.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String CREATED_DATE = "created";

		/**
		 * The timestamp for when the itemstore record was last modified.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String MODIFIED_DATE = "modified";

		/**
		 * The id of the item.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String ITEM_ID = "item_id";
		
		/**
		 * The id of one store that contains item.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String STORE_ID = "store_id";

		/**
		 * The aisle which contains item item_id at store store_id.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String AISLE = "aisle";
		
		/**
		 * The price of item item_id at store store_id.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String PRICE = "price";
	}


	/**
	 * Completion table for the Units field of Items.
	 */
	public static final class Units implements BaseColumns {
		/**
		 * The content:// style URL for this table.
		 */
		public static final Uri CONTENT_URI = Uri
				.parse("content://org.openintents.shopping/units");

		/**
		 * The default sort order for this table.
		 */
		public static final String DEFAULT_SORT_ORDER = "name ASC";

		/**
		 * The name of the units.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String NAME = "name";
		
		/**
		 * The name of the units when quantity == 1, 
		 * if different from general/plural unit name.
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String SINGULAR = "singular";
		
		/**
		 * The timestamp for when the unit was created.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String CREATED_DATE = "created";

		/**
		 * The timestamp for when the unit was last modified.
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String MODIFIED_DATE = "modified";

	}

	
	public static final class Notes implements BaseColumns {
		
	    // unlike other tables, this one does not correspond
		// to its own sql table... it just defines a projection of the items table.
		
	    // This class cannot be instantiated
	    private Notes() {}

	        /**
	         * The content:// style URL for this table
	         */
	        public static final Uri CONTENT_URI = Uri.parse("content://org.openintents.shopping/notes");
	        
	        /**
	         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
	         */
	        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.openintents.notepad.note";

	        /**
	         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
	         */
	        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.openintents.notepad.note";

	        /**
	         * The title of the note
	         * <P>Type: TEXT</P>
	         */
	        public static final String TITLE = "title";

	        /**
	         * The note itself
	         * <P>Type: TEXT</P>
	         */
	        public static final String NOTE = "note";

	        /**
	         * The timestamp for when the note was created
	         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
	         */
	        public static final String CREATED_DATE = "created";

	        /**
	         * The timestamp for when the note was last modified
	         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
	         */
	        public static final String MODIFIED_DATE = "modified";
	     
	        /**
	         * Tags associated with a note.
	         * Multiple tags are separated by commas.
	         * <P>Type: TEXT</P>
	         * @since 1.1.0
	         */
	        public static final String TAGS = "tags";

	        /**
	         * Whether the note is encrypted.
	         * 0 = not encrypted. 1 = encrypted.
	         * <P>Type: INTEGER</P>
	         * @since 1.1.0
	         */
	        public static final String ENCRYPTED = "encrypted";

	        /**
	         * A theme URI.
	         * <P>Type: TEXT</P>
	         * @since 1.1.0
	         */
	        public static final String THEME = "theme";
	        
	}
	

	/**
	 * Virtual table containing the id of the active list.
	 */
	public static final class ActiveList implements BaseColumns {
		/**
		 * The content:// style URL for this table.
		 */
		public static final Uri CONTENT_URI = Uri
				.parse("content://org.openintents.shopping/lists/active");

		/**
		 * Generic projection map.
		 */
		public static final String[] PROJECTION = { _ID };

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
	 * @return id of the "contains" table entry, or -1 if insert failed.
	 */
	public static long addItemToList(Context context, final long itemId,
			final long listId,	final long status, final long priority, 
			final double quantity) {
		long id = -1;
		Cursor existingItems = context.getContentResolver()
				.query(Contains.CONTENT_URI, new String[] { Contains._ID },
						"list_id = ? AND item_id = ?",
						new String[] { String.valueOf(listId),
								String.valueOf(itemId) }, null);
		if (existingItems.getCount() > 0) {
			existingItems.moveToFirst();
			id = existingItems.getLong(0);
			existingItems.close();
			
			// set status to want_to_buy:
			ContentValues values = new ContentValues(1);
			values.put(Contains.STATUS, status);
			try {
				Uri uri = Uri.withAppendedPath(Contains.CONTENT_URI, String.valueOf(id));
				context.getContentResolver().update(uri, values, null, null);
				Log.i(TAG, "updated item: " + uri);				
			} catch (Exception e) {
				Log.i(TAG, "Insert item failed", e);				
			}
			
		} else {
			existingItems.close();
			// Add item to list:
			ContentValues values = new ContentValues(2);
			values.put(Contains.ITEM_ID, itemId);
			values.put(Contains.LIST_ID, listId);
			values.put(Contains.STATUS, status);
			values.put(Contains.QUANTITY, quantity);
			values.put(Contains.PRIORITY, priority);

			try {
				Uri uri = context.getContentResolver().insert(Contains.CONTENT_URI, values);
				Log.i(TAG, "Insert new entry in 'contains': " + uri);
				id = Long.parseLong(uri.getPathSegments().get(1));
			} catch (Exception e) {
				Log.i(TAG, "insert into table 'contains' failed", e);
				id = -1;
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
