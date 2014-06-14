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

package org.openintents.shopping.library.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Definition for content provider related to shopping.
 */
public abstract class ShoppingContract {

    /**
     * TAG for logging.
     */
    private static final String TAG = "Shopping";
    public static final String ITEM_TYPE = "vnd.android.cursor.item/vnd.openintents.shopping.item";
    public static final String QUERY_ITEMS_WITH_STATE = "itemsWithState";
    public static final String AUTHORITY = "org.openintents.shopping";

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
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String NAME = "name";

        /**
         * An image of the item (uri).
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String IMAGE = "image";

        /**
         * A price for the item (in cent)
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String PRICE = "price";

        /**
         * Units for the item
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String UNITS = "units";

        /**
         * Tags for the item
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String TAGS = "tags";

        /**
         * A barcode (EAN or QR)
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String BARCODE = "barcode";

        /**
         * a location where to find it, as geo:lat,long uri
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String LOCATION = "location";

        /**
         * text of a note about the item
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String NOTE = "note";

        /**
         * The timestamp for when the item was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the item was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * The timestamp for when the item was last accessed.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ACCESSED_DATE = "accessed";

        /**
         * The timestamp for when the item is due.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String DUE_DATE = "due";

        /**
         * Generic projection map.
         */
        public static final String[] PROJECTION = {_ID, NAME, IMAGE, PRICE,
                CREATED_DATE, MODIFIED_DATE, ACCESSED_DATE, UNITS};

        public static final String[] PROJECTION_TO_COPY = {
                NAME, IMAGE, PRICE, UNITS, TAGS, BARCODE, LOCATION, NOTE
        };

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
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String NAME = "name";

        /**
         * An image of the list (uri).
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String IMAGE = "image";

        /**
         * The timestamp for when the item was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the item was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * The timestamp for when the item was last accessed.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ACCESSED_DATE = "accessed";

        /**
         * The name of the shared shopping list that should be worldwide unique.
         * <p/>
         * It is formed of the current user's email address and a unique suffix.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_NAME = "share_name";

        /**
         * The comma separated list of contacts with whom this list is shared.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_CONTACTS = "share_contacts";

        /**
         * Name of background image.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SKIN_BACKGROUND = "skin_background";

        /**
         * Name of font in list.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SKIN_FONT = "skin_font";

        /**
         * Color of text in list.
         * <p/>
         * <p/>
         * Type: INTEGER (long)
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SKIN_COLOR = "skin_color";

        /**
         * Color of strikethrough text in list.
         * <p/>
         * <p/>
         * Type: INTEGER (long)
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SKIN_COLOR_STRIKETHROUGH = "skin_color_strikethrough";

        /**
         * ID of store to filter in list, -1 to show all stores.
         * <p/>
         * <p/>
         * Type: INTEGER (long)
         * </P>
         * Available since release 1.6.
         */
        public static final String STORE_FILTER = "store_filter";

        /**
         * Tag text to filter in list.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 1.6.
         */
        public static final String TAGS_FILTER = "tags_filter";

        public static final String[] SORT_ORDERS = new String[]{
                "UPPER(" + NAME + ") ASC",
                "UPPER(" + NAME + ") DESC",
                CREATED_DATE + " DESC",
                CREATED_DATE + " ASC"
        };

        /**
         * ID of sort order to use for this list, null to follow prefs.
         * <p/>
         * <p/>
         * Type: INTEGER (long)
         * </P>
         * Available since release 2.0.
         */
        public static final String ITEMS_SORT = "items_sort";

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
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ITEM_ID = "item_id";

        /**
         * The id of the list that contains item_id.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String LIST_ID = "list_id";

        /**
         * Quantity specifier.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String QUANTITY = "quantity";

        /**
         * Priority specifier.
         * <p/>
         * Type: INTEGER (long) 1-5
         * </P>
         */
        public static final String PRIORITY = "priority";

        /**
         * Status: WANT_TO_BUY or BOUGHT.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String STATUS = "status";

        /**
         * The timestamp for when the item was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the item was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * The timestamp for when the item was last accessed.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ACCESSED_DATE = "accessed";

        /**
         * Name of person who inserted the item.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_CREATED_BY = "share_created_by";

        /**
         * Name of person who changed status of the item, for example mark it as
         * bought.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_MODIFIED_BY = "share_modified_by";

        /**
         * sort key with in the list
         * <p/>
         * Type: INTEGER
         * </P>
         */
        public static final String SORT_KEY = "sort_key";
        /**
         * Support sort orders. The "sort order" in the preferences is an index
         * into this array.
         */
        public static final String[] SORT_ORDERS = {
                // unchecked first, alphabetical
                "contains.status ASC, items.name COLLATE NOCASE ASC",

                "items.name COLLATE NOCASE ASC",

                "contains.modified DESC",

                "contains.modified ASC",

                // sort by tags, but put empty tags last.
                "(items.tags IS NULL or items.tags = '') ASC, items.tags COLLATE NOCASE ASC, items.name COLLATE NOCASE ASC",

                "items.price DESC, items.name COLLATE NOCASE ASC",

                // unchecked first, tags alphabetical, but put empty tags last.
                "contains.status ASC, (items.tags IS NULL or items.tags = '') ASC, items.tags COLLATE NOCASE ASC, items.name COLLATE NOCASE ASC",

                // unchecked first, priority, alphabetical
                "contains.status ASC, contains.priority ASC, items.name COLLATE NOCASE ASC",

                // unchecked first, priority, tags alphabetical, but put empty
                // tags last.
                "contains.status ASC, contains.priority ASC, (items.tags IS NULL or items.tags = '') ASC, items.tags COLLATE NOCASE ASC, items.name COLLATE NOCASE ASC",

                // priority, tags alphabetical, but put empty tags last.
                "contains.priority ASC, (items.tags IS NULL or items.tags = '') ASC, items.tags COLLATE NOCASE ASC, items.name COLLATE NOCASE ASC",
        };

        /**
         * For each of the above sort orders, does it depend on status?
         */
        public static final boolean[] StatusAffectsSortOrder = {
                true, false, false, false, false, false, true, true, true, false
        };

        public static final String[] PROJECTION_TO_COPY = {
                LIST_ID, QUANTITY, PRIORITY, STATUS
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
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ITEM_ID = "item_id";

        /**
         * The id of the list that contains item_id.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String LIST_ID = "list_id";

        /**
         * Quantity specifier.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String QUANTITY = "quantity";

        /**
         * Priority specifier.
         * <p/>
         * Type: INTEGER (long) 1-5
         * </P>
         */
        public static final String PRIORITY = "priority";

        /**
         * Status: WANT_TO_BUY or BOUGHT.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String STATUS = "status";

        /**
         * The timestamp for when the item was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the item was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * The timestamp for when the item was last accessed.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ACCESSED_DATE = "accessed";

        /**
         * Name of person who inserted the item.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_CREATED_BY = "share_created_by";

        /**
         * Name of person who crossed out the item.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_MODIFIED_BY = "share_modified_by";

        // Elements from Items

        /**
         * The name of the item.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String ITEM_NAME = "item_name";

        /**
         * An image of the item (uri).
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String ITEM_IMAGE = "item_image";

        /**
         * A price of the item (in cent).
         * <p/>
         * Type: INTEGER
         * </P>
         */
        public static final String ITEM_PRICE = "item_price";

        /**
         * Units of the item.
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String ITEM_UNITS = "item_units";

        /**
         * tags of the item.
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String ITEM_TAGS = "item_tags";

        // Elements from Lists

        /**
         * The name of the list.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String LIST_NAME = "list_name";

        /**
         * An image of the list (uri).
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String LIST_IMAGE = "list_image";

        /**
         * A barcode (EAN or QR)
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String BARCODE = "barcode";

        /**
         * a location where to find it, as geo:lat,long uri
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String LOCATION = "location";

        /**
         * The timestamp for when the item is due.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String DUE_DATE = "due";

        /**
         * Whether the item has a note.
         * <p/>
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
         * Have removed it from the list. Won't be deleted, in oder to keep
         * reference for later suggestions.
         */
        public static final long REMOVED_FROM_LIST = 3;

        /**
         * Checks whether a status is a valid possibility.
         *
         * @param s status to be checked.
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
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String NAME = "name";

        /**
         * The id of the list associated with this store.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String LIST_ID = "list_id";

        /**
         * The timestamp for when the store was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the store was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        public static final Uri QUERY_BY_LIST_URI = Uri
                .parse("content://org.openintents.shopping/liststores");

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
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the itemstore record was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * The id of the item.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ITEM_ID = "item_id";

        /**
         * The id of one store that contains item.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String STORE_ID = "store_id";

        /**
         * The aisle which contains item item_id at store store_id.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String AISLE = "aisle";

        /**
         * The price of item item_id at store store_id.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String PRICE = "price";

        /**
         * Whether we expect to find item item_id at store store_id.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String STOCKS_ITEM = "stocks_item";
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
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String NAME = "name";

        /**
         * The name of the units when quantity == 1, if different from
         * general/plural unit name.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String SINGULAR = "singular";

        /**
         * The timestamp for when the unit was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the unit was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";
    }

    public static final class Notes implements BaseColumns {

        // unlike other tables, this one does not correspond
        // to its own sql table... it just defines a projection of the items
        // table.

        // This class cannot be instantiated
        private Notes() {
        }

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/notes");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.openintents.notepad.note";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.openintents.notepad.note";

        /**
         * The title of the note
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String TITLE = "title";

        /**
         * The note itself
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String NOTE = "note";

        /**
         * The timestamp for when the note was created
         * <p/>
         * Type: INTEGER (long from System.curentTimeMillis())
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the note was last modified
         * <p/>
         * Type: INTEGER (long from System.curentTimeMillis())
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * Tags associated with a note. Multiple tags are separated by commas.
         * <p/>
         * Type: TEXT
         * </P>
         *
         * @since 1.1.0
         */
        public static final String TAGS = "tags";

        /**
         * Whether the note is encrypted. 0 = not encrypted. 1 = encrypted.
         * <p/>
         * Type: INTEGER
         * </P>
         *
         * @since 1.1.0
         */
        public static final String ENCRYPTED = "encrypted";

        /**
         * A theme URI.
         * <p/>
         * Type: TEXT
         * </P>
         *
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
        public static final String[] PROJECTION = {_ID};
    }

    /**
     * Virtual table containing subtotals of items by status and priority.
     */
    public static final class Subtotals {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/subtotals");

        /**
         * Priority
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String PRIORITY = "priority";

        /**
         * Status
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String STATUS = "status";

        /**
         * Number of items subtotaled in this cell.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String COUNT = "count";

        /**
         * Subtotal.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String SUBTOTAL = "subtotal";

        /**
         * Generic projection map.
         */
        public static final String[] PROJECTION = {PRIORITY, STATUS, COUNT,
                SUBTOTAL};
        // index values for use with cursors using the default projection
        public static final int PRIORITY_INDEX = 0;
        public static final int STATUS_INDEX = 1;
        public static final int COUNT_INDEX = 2;
        public static final int SUBTOTAL_INDEX = 3;
    }
}
