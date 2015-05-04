package org.openintents.shopping.ui;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.openintents.intents.ShoppingListIntents;
import org.openintents.shopping.LogConstants;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

import java.util.List;

public class ItemsFromExtras {
    /**
     * The items to add to the shopping list.
     * <p/>
     * Received as a string array list in the intent extras.
     */
    private List<String> mExtraItems;

    /**
     * The quantities for items to add to the shopping list.
     * <p/>
     * Received as a string array list in the intent extras.
     */
    private List<String> mExtraQuantities;

    /**
     * The prices for items to add to the shopping list.
     * <p/>
     * Received as a string array list in the intent extras.
     */
    private List<String> mExtraPrices;

    /**
     * The barcodes for items to add to the shopping list.
     * <p/>
     * Received as a string array list in the intent extras.
     */
    private List<String> mExtraBarcodes;

    /**
     * The list URI received together with intent extras.
     */
    private Uri mExtraListUri;
    private static final boolean debug = LogConstants.debug;
    private static final String TAG = "ItemsFromExtras";

    /**
     * Inserts new item from string array received in intent extras.
     */
    public void insertInto(ShoppingActivity activity, ShoppingItemsView itemsView) {
        if (mExtraItems != null) {
            // Make sure we are in the correct list:
            if (mExtraListUri != null) {
                long listId = Long
                        .parseLong(mExtraListUri.getLastPathSegment());
                if (debug) {
                    Log.d(TAG, "insert items into list " + listId);
                }
                if (listId != activity.getSelectedListId()) {
                    if (debug) {
                        Log.d(TAG, "set new list: " + listId);
                    }
                    activity.setSelectedListId((int) listId);
                }
                itemsView.fillItems(activity, listId);
            }

            int max = mExtraItems.size();
            int maxQuantity = (mExtraQuantities != null) ? mExtraQuantities
                    .size() : -1;
            int maxPrice = (mExtraPrices != null) ? mExtraPrices.size() : -1;
            int maxBarcode = (mExtraBarcodes != null) ? mExtraBarcodes.size()
                    : -1;
            for (int i = 0; i < max; i++) {
                String item = mExtraItems.get(i);
                String quantity = (i < maxQuantity) ? mExtraQuantities.get(i)
                        : null;
                String price = (i < maxPrice) ? mExtraPrices.get(i) : null;
                String barcode = (i < maxBarcode) ? mExtraBarcodes.get(i)
                        : null;
                if (debug) {
                    Log.d(TAG, "Add item: " + item + ", quantity: " + quantity
                            + ", price: " + price + ", barcode: " + barcode);
                }
                itemsView.insertNewItem(activity, item, quantity, null, price,
                        barcode);
            }
            // delete the string array list of extra items so it can't be
            // inserted twice
            mExtraItems = null;
            mExtraQuantities = null;
            mExtraPrices = null;
            mExtraBarcodes = null;
            mExtraListUri = null;
        } else {
            Toast.makeText(activity, R.string.no_items_available,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void getShoppingExtras(final Intent intent) {
        mExtraItems = intent.getExtras().getStringArrayList(
                ShoppingListIntents.EXTRA_STRING_ARRAYLIST_SHOPPING);
        mExtraQuantities = intent.getExtras().getStringArrayList(
                ShoppingListIntents.EXTRA_STRING_ARRAYLIST_QUANTITY);
        mExtraPrices = intent.getExtras().getStringArrayList(
                ShoppingListIntents.EXTRA_STRING_ARRAYLIST_PRICE);
        mExtraBarcodes = intent.getExtras().getStringArrayList(
                ShoppingListIntents.EXTRA_STRING_ARRAYLIST_BARCODE);

        mExtraListUri = null;
        if ((intent.getDataString() != null)
                && (intent.getDataString()
                .startsWith(ShoppingContract.Lists.CONTENT_URI
                        .toString()))) {
            // We received a valid shopping list URI.

            // Set current list to received list:
            mExtraListUri = intent.getData();
            if (debug) {
                Log.d(TAG, "Received extras for " + mExtraListUri.toString());
            }
        }
    }

    public boolean hasBeenInserted() {
        return mExtraItems == null;
    }

    public boolean hasItems() {
        return mExtraItems != null;
    }
}