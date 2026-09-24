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

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import org.openintents.convertcsv.common.ConvertCsvBaseActivity
import org.openintents.convertcsv.opencsv.CSVWriter
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull
import org.openintents.shopping.library.provider.ShoppingContract.Lists
import org.openintents.shopping.library.provider.ShoppingContract.Status
import java.io.IOException
import java.io.Writer

class ExportCsv(private val mContext: Context) {

    val handyShopperColumns =
        "Need,Priority,Description,CustomText,Quantity,Units,Price,Aisle,Date,Category,Stores,PerStoreInfo,EntryOrder,Coupon,Tax,Tax2,AutoDelete,Private,Note,Alarm,AlarmMidi,Icon,AutoOrder"

    companion object {
        @JvmField
        val PROJECTION_LISTS = arrayOf(
            Lists._ID,
            Lists.NAME,
            Lists.IMAGE,
            Lists.SHARE_NAME,
            Lists.SHARE_CONTACTS,
            Lists.SKIN_BACKGROUND
        )

        @JvmField
        val PROJECTION_CONTAINS_FULL = arrayOf(
            ContainsFull._ID,
            ContainsFull.ITEM_NAME,
            ContainsFull.ITEM_IMAGE,
            ContainsFull.STATUS,
            ContainsFull.ITEM_ID,
            ContainsFull.LIST_ID,
            ContainsFull.ITEM_TAGS,
            ContainsFull.SHARE_CREATED_BY,
            ContainsFull.SHARE_MODIFIED_BY
        )

        @JvmField
        val PROJECTION_CONTAINS_FULL_HANDY_SHOPPER = arrayOf(
            ContainsFull._ID,
            ContainsFull.ITEM_NAME,
            ContainsFull.ITEM_IMAGE,
            ContainsFull.QUANTITY,
            ContainsFull.PRIORITY,
            ContainsFull.STATUS,
            ContainsFull.ITEM_ID,
            ContainsFull.LIST_ID,
            ContainsFull.ITEM_TAGS,
            ContainsFull.ITEM_PRICE,
            ContainsFull.ITEM_UNITS,
            ContainsFull.SHARE_CREATED_BY,
            ContainsFull.SHARE_MODIFIED_BY
        )
    }

    /**
     * @param writer
     * @throws IOException
     */
    @Throws(IOException::class)
    fun exportCsv(writer: Writer) {
        val csvwriter = CSVWriter(writer)

        csvwriter.write(mContext.getString(R.string.header_subject))
        csvwriter.write(mContext.getString(R.string.header_percent_complete))
        csvwriter.write(mContext.getString(R.string.header_categories))
        csvwriter.write(mContext.getString(R.string.header_tags))
        csvwriter.writeNewline()

        val c = mContext.contentResolver.query(
            Lists.CONTENT_URI, PROJECTION_LISTS, null,
            null, Lists.DEFAULT_SORT_ORDER
        )

        if (c != null) {
            while (c.moveToNext()) {
                val listname = c.getString(c.getColumnIndexOrThrow(Lists.NAME))
                val id = c.getLong(c.getColumnIndexOrThrow(Lists._ID))

                // Log.i(ConvertCsvActivity.TAG, "List: " + listname)

                val ci = mContext.contentResolver.query(
                    ContainsFull.CONTENT_URI,
                    PROJECTION_CONTAINS_FULL,
                    ContainsFull.LIST_ID + " = ?",
                    arrayOf("" + id),
                    ContainsFull.DEFAULT_SORT_ORDER
                )

                if (ci != null) {
                    val itemcount = ci.count
                    ConvertCsvBaseActivity.dispatchSetMaxProgress(itemcount)
                    var progress = 0

                    while (ci.moveToNext()) {
                        ConvertCsvBaseActivity.dispatchConversionProgress(progress++)
                        val itemname = ci.getString(ci.getColumnIndexOrThrow(ContainsFull.ITEM_NAME))
                        val status = ci.getLong(ci.getColumnIndexOrThrow(ContainsFull.STATUS))
                        // 1 = bought, 0 = to buy; removed items as -1 so that an
                        // import (which maps anything else to REMOVED) keeps them off the list.
                        val percentage = when (status) {
                            Status.BOUGHT -> 1
                            Status.WANT_TO_BUY -> 0
                            else -> -1
                        }
                        val tags = ci.getString(ci.getColumnIndexOrThrow(ContainsFull.ITEM_TAGS))
                        csvwriter.write(itemname)
                        csvwriter.write(percentage)
                        csvwriter.write(listname)
                        csvwriter.write(tags)
                        csvwriter.writeNewline()
                    }
                    ci.close()
                }
            }
            c.close()
        }

        csvwriter.close()
    }

    /**
     * @param writer
     * @throws IOException
     */
    @Throws(IOException::class)
    fun exportHandyShopperCsv(writer: Writer, listId: Long) {
        val csvwriter = CSVWriter(writer)
        csvwriter.setLineEnd("\r\n")
        csvwriter.setQuoteCharacter(CSVWriter.NO_QUOTE_CHARACTER)

        csvwriter.write(handyShopperColumns)
        csvwriter.writeNewline()

        csvwriter.setQuoteCharacter(CSVWriter.DEFAULT_QUOTE_CHARACTER)

        val ci = mContext.contentResolver.query(
            ContainsFull.CONTENT_URI,
            PROJECTION_CONTAINS_FULL_HANDY_SHOPPER,
            ContainsFull.LIST_ID + " = ?",
            arrayOf("" + listId),
            ContainsFull.DEFAULT_SORT_ORDER
        )

        if (ci != null) {
            val itemcount = ci.count
            ConvertCsvBaseActivity.dispatchSetMaxProgress(itemcount)
            var progress = 0

            while (ci.moveToNext()) {
                ConvertCsvBaseActivity.dispatchConversionProgress(progress++)
                val itemname = ci.getString(ci.getColumnIndexOrThrow(ContainsFull.ITEM_NAME))
                val status = ci.getLong(ci.getColumnIndexOrThrow(ContainsFull.STATUS))
                val tags = ci.getString(ci.getColumnIndexOrThrow(ContainsFull.ITEM_TAGS))
                val priority = ci.getString(ci.getColumnIndexOrThrow(ContainsFull.PRIORITY))
                val quantity = ci.getString(ci.getColumnIndexOrThrow(ContainsFull.QUANTITY))
                val price = ci.getLong(ci.getColumnIndexOrThrow(ContainsFull.ITEM_PRICE))
                var pricestring = ""
                if (price != 0L) {
                    pricestring += price.toDouble() / 100.0
                }
                val unit = ci.getString(ci.getColumnIndexOrThrow(ContainsFull.ITEM_UNITS))
                val itemId = ci.getInt(ci.getColumnIndexOrThrow(ContainsFull.ITEM_ID)).toLong()

                val statusText = getHandyShopperStatusText(status)

                // Split off first tag.
                var tagsNonNull = tags ?: ""
                val t = tagsNonNull.indexOf(",")
                val firstTag: String
                val otherTags: String
                if (t >= 0) {
                    firstTag = tagsNonNull.substring(0, t) // -> Category
                    otherTags = tagsNonNull.substring(t + 1) // -> CustomText
                } else {
                    firstTag = tagsNonNull // -> Category
                    otherTags = "" // -> CustomText
                }

                // Retrieve note:
                var note: String? = getHandyShopperNote(itemId)
                if (note != null) {
                    // Replace LF by CR+LF
                    note = note.replace("\n", "\r\n")
                }

                val stores = getHandyShopperStores(itemId)
                val perStoreInfo = getHandyShopperPerStoreInfo(itemId)

                csvwriter.writeValue(statusText)  // 0 Need
                csvwriter.writeValue(priority)    // 1 Priority
                csvwriter.writeValue(itemname)    // 2 Description
                csvwriter.writeValue(otherTags)   // 3 CustomText
                csvwriter.writeValue(quantity)    // 4 Quantity
                csvwriter.writeValue(unit)        // 5 Units
                csvwriter.writeValue(pricestring) // 6 Price
                csvwriter.writeValue("")          // 7 Aisle
                csvwriter.writeValue("")          // 8 Date
                csvwriter.writeValue(firstTag)    // 9 Category
                csvwriter.writeValue(stores)      // 10 Stores
                csvwriter.writeValue(perStoreInfo)// 11 PerStoreInfo
                csvwriter.writeValue("")          // 12 EntryOrder
                csvwriter.writeValue("")          // 13 Coupon
                csvwriter.writeValue("")          // 14 Tax
                csvwriter.writeValue("")          // 15 Tax2
                csvwriter.writeValue("")          // 16 AutoDelete
                csvwriter.writeValue("")          // 17 Private
                csvwriter.write(note)             // 18 Note (use quotes)
                csvwriter.writeValue("")          // 19 Alarm
                csvwriter.writeValue("0")         // 20 AlarmMidi
                csvwriter.writeValue("0")         // 21 Icon
                csvwriter.writeValue("")          // 22 AutoOrder

                csvwriter.writeNewline()
            }
            ci.close()
        }

        csvwriter.close()
    }

    fun getHandyShopperStatusText(status: Long): String {
        return when (status) {
            Status.WANT_TO_BUY -> "x"
            Status.REMOVED_FROM_LIST -> "have"
            Status.BOUGHT -> ""
            else -> ""
        }
    }

    private fun getHandyShopperNote(itemId: Long): String? {
        val uri: Uri = ContentUris.withAppendedId(ShoppingContract.Items.CONTENT_URI, itemId)

        var note: String? = null
        val c1 = mContext.contentResolver.query(
            uri,
            arrayOf(ShoppingContract.Items.NOTE), null, null, null
        )
        if (c1 != null) {
            if (c1.moveToFirst()) {
                note = c1.getString(0)
            }
            c1.close()
        }
        return note
    }

    private fun getHandyShopperStores(itemId: Long): String {
        var stores = ""

        val c1 = mContext.contentResolver.query(
            ShoppingContract.ItemStores.CONTENT_URI,
            arrayOf(
                ShoppingContract.ItemStores.ITEM_ID,
                ShoppingContract.ItemStores.STORE_ID
            ),
            ShoppingContract.ItemStores.ITEM_ID + " = ?",
            arrayOf("" + itemId), null
        )
        if (c1 != null) {
            while (c1.moveToNext()) {
                val storeId = c1.getLong(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.STORE_ID))
                val uri2: Uri = ContentUris.withAppendedId(ShoppingContract.Stores.CONTENT_URI, storeId)
                val c2 = mContext.contentResolver.query(
                    uri2,
                    arrayOf(ShoppingContract.Stores.NAME), null, null, null
                )
                if (c2 != null) {
                    if (c2.moveToFirst()) {
                        val storeName = c2.getString(c2.getColumnIndexOrThrow(ShoppingContract.Stores.NAME))
                        stores = if (stores == "") {
                            storeName
                        } else {
                            stores + ";" + storeName
                        }
                    }
                    c2.close()
                }
            }
            c1.close()
        }
        return stores
    }

    // Deal with per-store aisles and prices from column 11.
    // example value for column 11:    Big Y=/0.50;BJ's=11/0.42
    private fun getHandyShopperPerStoreInfo(itemId: Long): String {
        var perStoreInfo = ""

        val c1 = mContext.contentResolver.query(
            ShoppingContract.ItemStores.CONTENT_URI,
            arrayOf(
                ShoppingContract.ItemStores.ITEM_ID,
                ShoppingContract.ItemStores.STORE_ID,
                ShoppingContract.ItemStores.AISLE,
                ShoppingContract.ItemStores.PRICE
            ),
            ShoppingContract.ItemStores.ITEM_ID + " = ?",
            arrayOf("" + itemId), null
        )
        if (c1 != null) {
            while (c1.moveToNext()) {
                val storeId = c1.getLong(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.STORE_ID))
                val aisle = c1.getString(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.AISLE))
                val price = c1.getLong(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.PRICE))
                val pricestring = "" + price.toDouble() / 100.0

                val uri2: Uri = ContentUris.withAppendedId(ShoppingContract.Stores.CONTENT_URI, storeId)
                val c2 = mContext.contentResolver.query(
                    uri2,
                    arrayOf(ShoppingContract.Stores.NAME), null, null, null
                )

                if (c2 != null) {
                    if (c2.moveToFirst()) {
                        val storeName = c2.getString(c2.getColumnIndexOrThrow(ShoppingContract.Stores.NAME))

                        if (price != 0L) {
                            val info = "$storeName=${aisle ?: ""}/$pricestring"
                            perStoreInfo = if (perStoreInfo == "") {
                                info
                            } else {
                                perStoreInfo + ";" + info
                            }
                        }
                    }
                    c2.close()
                }
            }
            c1.close()
        }
        return perStoreInfo
    }
}
