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

/*
 * This code is based on Android's API demos.
 */

package org.openintents.shopping.share;

import android.content.ContentResolver;
import android.content.Context;

import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;

/**
 * Handles receiving information about changes in shared shopping lists.
 */
public class GTalkReceiver /* extends IntentReceiver */ {
    /**
     * Tag for log.
     */
    private static final String TAG = "GTalkReceiver";

    /**
     * Array of items for editing. This defines the projection for the table
     * Lists.
     */
    private static final String[] mProjectionLists = new String[]{
            ShoppingContract.Lists._ID, ShoppingContract.Lists.NAME,
            ShoppingContract.Lists.SHARE_NAME,
            ShoppingContract.Lists.SHARE_CONTACTS};

    /**
     * Index of ID in the Projection for Lists
     */
    private static final int mProjectionListsID = 0;
    private static final int mProjectionListsNAME = 1;
    private static final int mProjectionListsSHARENAME = 2;
    private static final int mProjectionListsSHARECONTACTS = 3;

    /**
     * Array of items for editing. This defines the projection for the table
     * ContainsFull.
     */
    private static final String[] mProjectionContainsFull = new String[]{
            ContainsFull._ID, ContainsFull.ITEM_NAME, ContainsFull.ITEM_IMAGE,
            ContainsFull.STATUS, ContainsFull.ITEM_ID,
            ContainsFull.SHARE_CREATED_BY, ContainsFull.SHARE_MODIFIED_BY};
    private static final int mProjectionContainsFullCONTAINSID = 0;
    private static final int mProjectionContainsFullITEMNAME = 1;
    private static final int mProjectionContainsFullITEMIMAGE = 2;
    private static final int mProjectionContainsFullSTATUS = 3;
    private static final int mProjectionContainsFullITEMID = 4;
    private static final int mProjectionContainsFullSHARECREATEDBY = 5;
    private static final int mProjectionContainsFullSHAREMODIFIEDBY = 6;

    private Context mContext;
    private ContentResolver mContentResolver;
    /*
     * public void onReceiveIntent(Context context, Intent intent) { mContext =
     * context; mContentResolver = mContext.getContentResolver(); String action
     * = intent.getAction(); Uri data = intent.getData(); Bundle bundle =
     * intent.getExtras();
     *
     * if (data == null) { // Here we have to work around an GTalk issue in
     * Android m5-rc14/15: // GTalk does not send data, so we send them in the
     * bundle: if (bundle != null) { data =
     * Uri.parse(bundle.getString(GTalkSender.DATA)); } else { Log.i(TAG,
     * "IntentReceiver: Received neither data nor bundle."); return; } }
     *
     * Log.i(TAG, "Received intent " + action + ", data " + data.toString());
     *
     * if (data.equals(Shopping.Lists.CONTENT_URI)) { if
     * (action.equals(OpenIntents.SHARE_UPDATE_ACTION)) { // Update on a
     * shopping list updateList(bundle); return; } } else if
     * (data.equals(Shopping.Items.CONTENT_URI)) { if
     * (action.equals(OpenIntents.SHARE_INSERT_ACTION)) { // Insert a new item
     * insertItem(bundle); return; } else if
     * (action.equals(OpenIntents.SHARE_UPDATE_ACTION)) { // Update an item
     * updateItem(bundle); return; } }
     *
     * }
     */
    /**
     * Updates shared list information or creates a new list.
     *
     * If the shared list does not exist yet, a new list is created.
     *
     * @param bundle
     */
    /*
     * void updateList(Bundle bundle) { // Update information about list: if
     * (bundle != null) { String shareListName =
     * bundle.getString(Shopping.Lists.SHARE_NAME); String shareContacts =
     * bundle.getString(Shopping.Lists.SHARE_CONTACTS);
     *
     * if (shareListName == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareListName is null."); return; } if
     * (shareContacts == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareContacts is null."); return; }
     *
     * // Get unique list identifier // Get a cursor for all items that are
     * contained // in currently selected shopping list. Cursor c =
     * mContentResolver.query( Shopping.Lists.CONTENT_URI, mProjectionLists,
     * Shopping.Lists.SHARE_NAME + " = '" + shareListName + "'", null,
     * Shopping.Lists.DEFAULT_SORT_ORDER);
     *
     * if (c == null || c.count() < 1) { // List does not exist yet: // Create
     * it first. // Add item to list: ContentValues values = new
     * ContentValues(2);
     *
     * // Let us use the share name as default name
     * values.put(Shopping.Lists.NAME, shareListName);
     * values.put(Shopping.Lists.SHARE_NAME, shareListName);
     *
     * // The sender is responsible that the contacts list // we receive // (1)
     * contains the sender's email, // (2) does not contain our email.
     * values.put(Shopping.Lists.SHARE_CONTACTS, shareContacts);
     *
     * try { Uri uri = mContentResolver.insert(Lists.CONTENT_URI, values);
     * Log.i(TAG, "Insert new list: " + uri);
     *
     * Toast.makeText(mContext, "Received new shopping list: " + shareListName,
     * Toast.LENGTH_LONG).show(); } catch (Exception e) { Log.i(TAG,
     * "insert list failed", e); } } else { // List exists, let us just update
     * email contacts c.first(); c.updateString(mProjectionListsSHARECONTACTS,
     * shareContacts);
     *
     * c.commitUpdates();
     *
     * c.requery(); }
     *
     * // Finally send notification that data changed:
     * mContext.broadcastIntent(new Intent(OpenIntents.REFRESH_ACTION));
     *
     * } else { Log.e(TAG, "Bundle received is null"); } }
     */

    /**
     * Inserts an item into a list.
     *
     * @param bundle
     */
    /*
     * void insertItem(Bundle bundle) { // Update information about list: if
     * (bundle != null) { String shareListName =
     * bundle.getString(Shopping.Lists.SHARE_NAME); String itemName =
     * bundle.getString(Shopping.Items.NAME);
     *
     * if (shareListName == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareListName is null."); return; } if
     * (itemName == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareContacts is null."); return; }
     *
     * // Get unique list identifier // Get a cursor for all items that are
     * contained // in currently selected shopping list. Cursor c =
     * mContentResolver.query( Shopping.Lists.CONTENT_URI, mProjectionLists,
     * Shopping.Lists.SHARE_NAME + " = '" + shareListName + "'", null,
     * Shopping.Lists.DEFAULT_SORT_ORDER);
     *
     * if (c == null || c.count() < 1) { // List does not exist:
     *
     * Log.i(TAG, "insertItem: Received item for list that does not exist");
     *
     * // TODO: Ask user what to do about the item: // Either demand new list +
     * synchronization, or drop item // OR: Automatically create the list with
     * minimum entries } else { // List exists, let us insert item c.first();
     * long listId = c.getLong(mProjectionListsID); long itemId =
     * Shopping.getItem(itemName);
     *
     * Shopping.addItemToList(itemId, listId); }
     *
     *
     * // Finally send notification that data changed:
     * mContext.broadcastIntent(new Intent(OpenIntents.REFRESH_ACTION));
     *
     * } else { Log.e(TAG, "Bundle received is null"); } }
     */

    /**
     * Updates information about an item in a list.
     *
     * If the item does not exist yet, it is created. Update could include to
     * strike an item through.
     *
     * @param bundle
     */
    /*
     * void updateItem(Bundle bundle) { // Update information about list: if
     * (bundle != null) { String shareListName =
     * bundle.getString(Shopping.Lists.SHARE_NAME); String itemNameOld =
     * bundle.getString(Shopping.Items.NAME + GTalkSender.OLD); String itemName
     * = bundle.getString(Shopping.Items.NAME); // TODO: In m5, Android only
     * supports Strings in bundles for GTalk String itemStatusOld =
     * bundle.getString(Shopping.Contains.STATUS + GTalkSender.OLD); String
     * itemStatus = bundle.getString(Shopping.Contains.STATUS); String
     * itemSender = bundle.getString(GTalkSender.SENDER);
     *
     * if (shareListName == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareListName is null."); return; } if
     * (itemName == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareContacts is null."); return; } if
     * (itemNameOld == null || itemStatusOld == null || itemStatus == null ||
     * itemSender == null) { Log.e(TAG,
     * "Bundle received is incomplete: at least one item missing."); return; }
     *
     * // Get a cursor for shared list. Cursor c = mContentResolver.query(
     * Shopping.Lists.CONTENT_URI, mProjectionLists, Shopping.Lists.SHARE_NAME +
     * " = '" + shareListName + "'", null, Shopping.Lists.DEFAULT_SORT_ORDER);
     *
     * if (c == null || c.count() < 1) { // List does not exist:
     *
     * Log.i(TAG, "insertItem: Received item for list that does not exist");
     *
     * // TODO: Ask user what to do about the item: // Either demand new list +
     * synchronization, or drop item // OR: Automatically create the list with
     * minimum entries } else { // List exists, let us update item c.first();
     * long listId = c.getLong(mProjectionListsID);
     *
     * Log.i(TAG, "Query: item name = " + itemNameOld + ", status = " +
     * itemStatusOld);
     *
     * // Now look for item with old name and old status: Cursor citem =
     * mContentResolver.query( Shopping.ContainsFull.CONTENT_URI,
     * mProjectionContainsFull, Shopping.ContainsFull.LIST_ID + " = '" + listId
     * + "'" + " AND " + Shopping.ContainsFull.ITEM_NAME + " = '" + itemNameOld
     * + "'" + " AND " + Shopping.ContainsFull.STATUS + " = '" + itemStatusOld +
     * "'", null, null);
     *
     * if (citem == null || citem.count() < 1) { // Item with same name and same
     * status does not exist. // Let us see if there exists one with same name
     * at least. // The assumption is that probably "status" is out of sync, //
     * and we want to avoid having duplicates.
     *
     * // (Later, there should really be a hidden Item ID that is // unique so
     * that these issues of duplicates can not arise. Log.i(TAG,
     * "Re-Query: item name = " + itemNameOld);
     *
     * citem = mContentResolver.query( Shopping.ContainsFull.CONTENT_URI,
     * mProjectionContainsFull, Shopping.ContainsFull.LIST_ID + " = '" + listId
     * + "'" + " AND " + Shopping.ContainsFull.ITEM_NAME + " = '" + itemNameOld
     * + "'", null, null); }
     *
     * if (citem == null || citem.count() < 1) { // Item does not exist - we
     * will insert (create) it:
     *
     * Log.i(TAG,
     * "insertItem: Received update for item that does not exist - inserting item"
     * );
     *
     * long itemId = Shopping.getItem(itemName);
     *
     * // Add item to list: ContentValues values = new ContentValues(2);
     * values.put(Contains.ITEM_ID, itemId); values.put(Contains.LIST_ID,
     * listId); values.put(Contains.STATUS, Long.parseLong(itemStatus));
     * values.put(Contains.SHARE_MODIFIED_BY, itemSender); try { Uri uri =
     * mContentResolver.insert(Contains.CONTENT_URI, values); Log.i(TAG,
     * "Insert new entry in 'contains': " + uri); // return
     * Long.parseLong(uri.getPathSegments().get(1)); } catch (Exception e) {
     * Log.i(TAG, "insert into table 'contains' failed", e); // return -1; } }
     * else { // Item exists, let us update citem.first();
     *
     * long itemId = citem.getLong(mProjectionContainsFullITEMID); long
     * containsId = citem.getLong(mProjectionContainsFullCONTAINSID);
     *
     * Uri itemUri = Uri.withAppendedPath(Shopping.Items.CONTENT_URI, "" +
     * itemId); ContentValues values = new ContentValues(1);
     * values.put(Shopping.Items.NAME, itemName);
     * mContentResolver.update(itemUri, values, null, null);
     *
     * Uri containsUri = Uri.withAppendedPath(Shopping.Contains.CONTENT_URI, ""
     * + containsId); values = new ContentValues(2);
     * values.put(Shopping.Contains.STATUS, Long.parseLong(itemStatus));
     * values.put(Shopping.Contains.SHARE_MODIFIED_BY, itemSender);
     * mContentResolver.update(containsUri, values, null, null);
     *
     * } }
     *
     * // Finally send notification that data changed:
     * mContext.broadcastIntent(new Intent(OpenIntents.REFRESH_ACTION));
     *
     * } else { Log.e(TAG, "Bundle received is null"); } }
     */

}
