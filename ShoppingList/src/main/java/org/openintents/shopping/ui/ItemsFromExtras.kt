package org.openintents.shopping.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import org.openintents.intents.ShoppingListIntents
import org.openintents.shopping.LogConstants
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.ui.widget.ShoppingItemsView

open class ItemsFromExtras {

    /**
     * The items to add to the shopping list.
     *
     * Received as a string array list in the intent extras.
     */
    private var mExtraItems: List<String>? = null

    /**
     * The quantities for items to add to the shopping list.
     *
     * Received as a string array list in the intent extras.
     */
    private var mExtraQuantities: List<String>? = null

    /**
     * The prices for items to add to the shopping list.
     *
     * Received as a string array list in the intent extras.
     */
    private var mExtraPrices: List<String>? = null

    /**
     * The barcodes for items to add to the shopping list.
     *
     * Received as a string array list in the intent extras.
     */
    private var mExtraBarcodes: List<String>? = null

    /**
     * The list URI received together with intent extras.
     */
    private var mExtraListUri: Uri? = null

    /**
     * Inserts new item from string array received in intent extras.
     */
    fun insertInto(activity: ShoppingActivity, itemsView: ShoppingItemsView) {
        if (mExtraItems != null) {
            // Make sure we are in the correct list:
            if (mExtraListUri != null) {
                val listId = java.lang.Long.parseLong(mExtraListUri!!.lastPathSegment)
                if (debug) {
                    Log.d(TAG, "insert items into list $listId")
                }
                if (listId != activity.getSelectedListId()) {
                    if (debug) {
                        Log.d(TAG, "set new list: $listId")
                    }
                    activity.setSelectedListId(listId.toInt())
                }
                itemsView.fillItems(activity, listId)
            }

            val max = mExtraItems!!.size
            val maxQuantity = if (mExtraQuantities != null) mExtraQuantities!!.size else -1
            val maxPrice = if (mExtraPrices != null) mExtraPrices!!.size else -1
            val maxBarcode = if (mExtraBarcodes != null) mExtraBarcodes!!.size else -1
            for (i in 0 until max) {
                val item = mExtraItems!![i]
                val quantity = if (i < maxQuantity) mExtraQuantities!![i] else null
                val price = if (i < maxPrice) mExtraPrices!![i] else null
                val barcode = if (i < maxBarcode) mExtraBarcodes!![i] else null
                if (debug) {
                    Log.d(TAG, "Add item: $item, quantity: $quantity, price: $price, barcode: $barcode")
                }
                itemsView.insertNewItem(activity, item, quantity, null, price, barcode)
            }
            // delete the string array list of extra items so it can't be
            // inserted twice
            mExtraItems = null
            mExtraQuantities = null
            mExtraPrices = null
            mExtraBarcodes = null
            mExtraListUri = null
        } else {
            Toast.makeText(activity, R.string.no_items_available, Toast.LENGTH_SHORT).show()
        }
    }

    fun getShoppingExtras(intent: Intent) {
        mExtraItems = intent.extras!!.getStringArrayList(
            ShoppingListIntents.EXTRA_STRING_ARRAYLIST_SHOPPING
        )
        mExtraQuantities = intent.extras!!.getStringArrayList(
            ShoppingListIntents.EXTRA_STRING_ARRAYLIST_QUANTITY
        )
        mExtraPrices = intent.extras!!.getStringArrayList(
            ShoppingListIntents.EXTRA_STRING_ARRAYLIST_PRICE
        )
        mExtraBarcodes = intent.extras!!.getStringArrayList(
            ShoppingListIntents.EXTRA_STRING_ARRAYLIST_BARCODE
        )

        mExtraListUri = null
        if ((intent.dataString != null) &&
            intent.dataString!!.startsWith(ShoppingContract.Lists.CONTENT_URI.toString())
        ) {
            // We received a valid shopping list URI.

            // Set current list to received list:
            mExtraListUri = intent.data
            if (debug) {
                Log.d(TAG, "Received extras for ${mExtraListUri.toString()}")
            }
        }
    }

    fun hasBeenInserted(): Boolean {
        return mExtraItems == null
    }

    fun hasItems(): Boolean {
        return mExtraItems != null
    }

    companion object {
        private val debug = LogConstants.debug
        private const val TAG = "ItemsFromExtras"
    }
}
