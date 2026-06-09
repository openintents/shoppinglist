/*
 * Copyright (C) 2008 OpenIntents.org
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

package org.openintents.convertcsv.shoppinglist

import android.content.Context
import android.text.TextUtils
import org.openintents.convertcsv.common.ConvertCsvBaseActivity
import org.openintents.convertcsv.common.WrongFormatException
import org.openintents.convertcsv.opencsv.CSVReader
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract.Status
import org.openintents.shopping.library.util.ShoppingUtils
import java.io.IOException
import java.io.Reader
import java.util.HashMap

class ImportCsv(private val mContext: Context, importPolicy: Int) {

    private var mDuplicate: Boolean = true
    private var mUpdate: Boolean = false

    init {
        when (importPolicy) {
            ConvertCsvBaseActivity.IMPORT_POLICY_KEEP -> {
                mDuplicate = false
                mUpdate = false
            }
            // IMPORT_POLICY_RESTORE not implemented, treat as overwrite for now.
            ConvertCsvBaseActivity.IMPORT_POLICY_RESTORE,
            ConvertCsvBaseActivity.IMPORT_POLICY_OVERWRITE -> {
                mDuplicate = false
                mUpdate = true
            }
            ConvertCsvBaseActivity.IMPORT_POLICY_DUPLICATE -> {
                mDuplicate = true
                mUpdate = false
            }
        }
    }

    /**
     * @param reader
     * @throws IOException
     */
    @Throws(IOException::class, WrongFormatException::class)
    fun importCsv(reader: Reader) {
        val csvreader = CSVReader(reader)
        var nextLine: Array<String>?
        while (csvreader.readNext().also { nextLine = it } != null) {
            if (nextLine!!.size != 4) {
                throw WrongFormatException()
            }
            // nextLine[] is an array of values from the line
            val statusstring = nextLine!![1]
            if (statusstring == mContext.getString(R.string.header_percent_complete)) {
                // First line is just subject, so let us skip it
                continue
            }
            val itemname = nextLine!![0]
            val status: Long = try {
                statusstring.toLong()
            } catch (e: NumberFormatException) {
                0L
            }
            val listname = nextLine!![2]
            val tags = nextLine!![3]

            // Add item to list
            val listId = ShoppingUtils.getList(mContext, listname)
            val itemId = ShoppingUtils.getItem(mContext, itemname, tags, null,
                null, null, mDuplicate, mUpdate)

            val resolvedStatus: Long = when (status) {
                1L -> Status.BOUGHT
                0L -> Status.WANT_TO_BUY
                else -> Status.REMOVED_FROM_LIST
            }

            ShoppingUtils.addItemToList(mContext, itemId, listId, resolvedStatus, null, null,
                false, mDuplicate, false)
        }
    }

    private fun convert_hs_price(hs_price: String): String {
        var price = hs_price
        try {
            var fprice = price.toDouble()
            fprice *= 100
            price = Math.round(fprice).toString()
        } catch (nfe: NumberFormatException) {
        }
        return price
    }

    @Throws(IOException::class, WrongFormatException::class)
    fun importHandyShopperCsv(reader: Reader, listId: Long, importStores: Boolean) {
        val csvreader = CSVReader(reader)
        var nextLine: Array<String>?
        val seen_stores = HashMap<String, Long>()
        val item_stores = HashMap<String, Long>()

        while (csvreader.readNext().also { nextLine = it } != null) {
            if (nextLine!!.size != 23) {
                throw WrongFormatException()
            }
            // nextLine[] is an array of values from the line
            val statusstring = nextLine!![0]
            if (statusstring == mContext.getString(R.string.header_need)) {
                // First line is just subject, so let us skip it
                continue
            }

            val status: Long = when {
                "x".equals(statusstring, ignoreCase = true) -> Status.WANT_TO_BUY
                "".equals(statusstring, ignoreCase = true) -> Status.BOUGHT
                "have".equals(statusstring, ignoreCase = true) -> Status.REMOVED_FROM_LIST
                else -> Status.REMOVED_FROM_LIST
            }

            val itemname = nextLine!![2]  // Description
            var tags = nextLine!![9]       // Category
            var price = nextLine!![6]      // Price
            val note = nextLine!![18]      // Note
            val units = nextLine!![5]

            if (nextLine!![3].isNotEmpty()) {
                tags = if (tags.isEmpty()) {
                    nextLine!![3]
                } else {
                    tags + "," + nextLine!![3]
                }
            }

            val quantity = nextLine!![4]   // Quantity
            val priority = nextLine!![1]   // Priority

            if (price.isNotEmpty()) {
                price = convert_hs_price(price)
            }

            // Add item to list
            //long listId = ShoppingUtils.getDefaultList(mContext)
            val itemId = ShoppingUtils.getItem(mContext, itemname, tags, price, units, note,
                mDuplicate, mUpdate)
            ShoppingUtils.addItemToList(mContext, itemId, listId, status, priority, quantity,
                false, mDuplicate, false)

            // Two columns contain per-store information. Column 10 lists
            // all stores which carry this item, delimited by semicolons. Column 11
            // lists aisles and prices for some subset of those stores.
            //
            // To save time, we first deal with the prices in column 11, then from
            // Column 10 we add only the ones not already added from Column 11.

            var stores: Array<String>
            item_stores.clear()

            // example value for column 11:    Big Y=/0.50;BJ's=11/0.42
            if (nextLine!![11].isNotEmpty() && importStores) {
                stores = nextLine!![11].split(";").toTypedArray()

                for (i_store in stores.indices) {
                    val key_vals = stores[i_store].split("=").toTypedArray()
                    val store_name = key_vals[0]
                    val aisle_price = key_vals[1].split("/").toTypedArray()
                    if (aisle_price.isEmpty()) continue
                    val aisle = aisle_price[0]
                    val store_price: String = if (aisle_price.size > 1) {
                        convert_hs_price(aisle_price[1])
                    } else {
                        ""
                    }

                    var storeId = seen_stores[store_name]
                    if (storeId == null) {
                        storeId = ShoppingUtils.getStore(mContext, store_name, listId)
                        seen_stores[store_name] = storeId
                    }
                    item_stores[store_name] = storeId
                    val item_store = ShoppingUtils.addItemToStore(mContext, itemId, storeId, aisle, store_price, mDuplicate)
                }
            }

            if (nextLine!![10].isNotEmpty()) {
                stores = nextLine!![10].split(";").toTypedArray()
                for (i_store in stores.indices) {
                    if (importStores) {    // real store import
                        var storeId = item_stores[stores[i_store]]
                        if (storeId != null)
                        // existence of item at store handled in price handling, no need to add it again.
                            continue
                        storeId = seen_stores[stores[i_store]]
                        if (storeId == null) {
                            storeId = ShoppingUtils.getStore(mContext, stores[i_store], listId)
                            seen_stores[stores[i_store]] = storeId
                        }
                        item_stores[stores[i_store]] = storeId // not strictly required, but...
                        val item_store = ShoppingUtils.addItemToStore(mContext, itemId, storeId, "", "", mDuplicate)
                    } else if (!TextUtils.isEmpty(stores[i_store])) {
                        // store names added as tags.
                        ShoppingUtils.addTagToItem(mContext, itemId, stores[i_store])
                    }
                }
            }
        }
    }
}
