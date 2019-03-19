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

package org.openintents.convertcsv.shoppinglist;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.openintents.convertcsv.common.ConvertCsvBaseActivity;
import org.openintents.convertcsv.opencsv.CSVWriter;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.library.provider.ShoppingContract.Status;

import java.io.IOException;
import java.io.Writer;

public class ExportCsv {

    public static final String[] PROJECTION_LISTS = new String[]{Lists._ID,
            Lists.NAME, Lists.IMAGE, Lists.SHARE_NAME, Lists.SHARE_CONTACTS,
            Lists.SKIN_BACKGROUND};

    public static final String[] PROJECTION_CONTAINS_FULL = new String[]{
            ContainsFull._ID, ContainsFull.ITEM_NAME, ContainsFull.ITEM_IMAGE,
            ContainsFull.STATUS, ContainsFull.ITEM_ID, ContainsFull.LIST_ID,
            ContainsFull.ITEM_TAGS,
            ContainsFull.SHARE_CREATED_BY, ContainsFull.SHARE_MODIFIED_BY};
    public static final String[] PROJECTION_CONTAINS_FULL_HANDY_SHOPPER = new String[]{
            ContainsFull._ID, ContainsFull.ITEM_NAME, ContainsFull.ITEM_IMAGE,
            ContainsFull.QUANTITY, ContainsFull.PRIORITY,
            ContainsFull.STATUS, ContainsFull.ITEM_ID, ContainsFull.LIST_ID,
            ContainsFull.ITEM_TAGS,
            ContainsFull.ITEM_PRICE, ContainsFull.ITEM_UNITS,
            ContainsFull.SHARE_CREATED_BY, ContainsFull.SHARE_MODIFIED_BY};
    Context mContext;
    String handyShopperColumns = "Need,Priority,Description,CustomText,Quantity,Units,Price,Aisle,Date,Category,Stores,PerStoreInfo,EntryOrder,Coupon,Tax,Tax2,AutoDelete,Private,Note,Alarm,AlarmMidi,Icon,AutoOrder";

    public ExportCsv(Context context) {
        mContext = context;
    }

    /**
     * @param dos
     * @throws IOException
     */
    public void exportCsv(Writer writer) throws IOException {

        CSVWriter csvwriter = new CSVWriter(writer);

        csvwriter.write(mContext.getString(R.string.header_subject));
        csvwriter.write(mContext.getString(R.string.header_percent_complete));
        csvwriter.write(mContext.getString(R.string.header_categories));
        csvwriter.write(mContext.getString(R.string.header_tags));
        csvwriter.writeNewline();

        Cursor c = mContext.getContentResolver().query(
                Lists.CONTENT_URI, PROJECTION_LISTS, null,
                null, Lists.DEFAULT_SORT_ORDER);

        if (c != null) {

            while (c.moveToNext()) {

                String listname = c.getString(c
                        .getColumnIndexOrThrow(Lists.NAME));
                long id = c
                        .getLong(c.getColumnIndexOrThrow(Lists._ID));

                // Log.i(ConvertCsvActivity.TAG, "List: " + listname);

                Cursor ci = mContext.getContentResolver().query(
                        ContainsFull.CONTENT_URI,
                        PROJECTION_CONTAINS_FULL,
                        ContainsFull.LIST_ID + " = ?",
                        new String[]{"" + id},
                        ContainsFull.DEFAULT_SORT_ORDER);

                if (ci != null) {
                    int itemcount = ci.getCount();
                    ConvertCsvBaseActivity.dispatchSetMaxProgress(itemcount);
                    int progress = 0;

                    while (ci.moveToNext()) {
                        ConvertCsvBaseActivity
                                .dispatchConversionProgress(progress++);
                        String itemname = ci
                                .getString(ci
                                        .getColumnIndexOrThrow(ContainsFull.ITEM_NAME));
                        int status = ci
                                .getInt(ci
                                        .getColumnIndexOrThrow(ContainsFull.STATUS));
                        int percentage = (status == Status.BOUGHT) ? 1
                                : 0;
                        String tags = ci
                                .getString(ci
                                        .getColumnIndexOrThrow(ContainsFull.ITEM_TAGS));
                        csvwriter.write(itemname);
                        csvwriter.write(percentage);
                        csvwriter.write(listname);
                        csvwriter.write(tags);
                        csvwriter.writeNewline();
                    }
                }
            }
        }

        csvwriter.close();
    }

    /**
     * @param dos
     * @throws IOException
     */
    public void exportHandyShopperCsv(Writer writer, long listId) throws IOException {

        CSVWriter csvwriter = new CSVWriter(writer);
        csvwriter.setLineEnd("\r\n");
        csvwriter.setQuoteCharacter(CSVWriter.NO_QUOTE_CHARACTER);

        csvwriter.write(handyShopperColumns);
        csvwriter.writeNewline();

        csvwriter.setQuoteCharacter(CSVWriter.DEFAULT_QUOTE_CHARACTER);


        Cursor ci = mContext.getContentResolver().query(
                ContainsFull.CONTENT_URI,
                PROJECTION_CONTAINS_FULL_HANDY_SHOPPER,
                ContainsFull.LIST_ID + " = ?",
                new String[]{"" + listId},
                ContainsFull.DEFAULT_SORT_ORDER);

        if (ci != null) {
            int itemcount = ci.getCount();
            ConvertCsvBaseActivity.dispatchSetMaxProgress(itemcount);
            int progress = 0;

            while (ci.moveToNext()) {
                ConvertCsvBaseActivity
                        .dispatchConversionProgress(progress++);
                String itemname = ci.getString(ci.getColumnIndexOrThrow(ContainsFull.ITEM_NAME));
                int status = ci.getInt(ci.getColumnIndexOrThrow(ContainsFull.STATUS));
                String tags = ci.getString(ci.getColumnIndexOrThrow(ContainsFull.ITEM_TAGS));
                String priority = ci.getString(ci.getColumnIndex(ContainsFull.PRIORITY));
                String quantity = ci.getString(ci.getColumnIndex(ContainsFull.QUANTITY));
                long price = ci.getLong(ci.getColumnIndex(ContainsFull.ITEM_PRICE));
                String pricestring = "";
                if (price != 0) {
                    pricestring += (double) price / 100.d;
                }
                String unit = ci.getString(ci.getColumnIndex(ContainsFull.ITEM_UNITS));
                long itemId = ci.getInt(ci.getColumnIndex(ContainsFull.ITEM_ID));

                String statusText = getHandyShopperStatusText(status);

                // Split off first tag.
                if (tags == null) {
                    tags = "";
                }
                int t = tags.indexOf(",");
                String firstTag = "";
                String otherTags = "";
                if (t >= 0) {
                    firstTag = tags.substring(0, t); // -> Category
                    otherTags = tags.substring(t + 1); // -> CustomText
                } else {
                    firstTag = tags; // -> Category
                    otherTags = ""; // -> CustomText
                }

                // Retrieve note:
                String note = getHandyShopperNote(itemId);
                if (note != null) {
                    // Replace LF by CR+LF
                    note = note.replace("\n", "\r\n");
                }

                String stores = getHandyShopperStores(itemId);
                String perStoreInfo = getHandyShopperPerStoreInfo(itemId);


                csvwriter.writeValue(statusText); // 0 Need
                csvwriter.writeValue(priority); // 1 Priority
                csvwriter.writeValue(itemname); // 2 Description
                csvwriter.writeValue(otherTags); // 3 CustomText
                csvwriter.writeValue(quantity); // 4 Quantity
                csvwriter.writeValue(unit); // 5 Units
                csvwriter.writeValue(pricestring); // 6 Price
                csvwriter.writeValue(""); // 7 Aisle
                csvwriter.writeValue(""); // 8 Date
                csvwriter.writeValue(firstTag); // 9 Category
                csvwriter.writeValue(stores); // 10 Stores
                csvwriter.writeValue(perStoreInfo); // 11 PerStoreInfo
                csvwriter.writeValue(""); // 12 EntryOrder
                csvwriter.writeValue(""); // 13 Coupon
                csvwriter.writeValue(""); // 14 Tax
                csvwriter.writeValue(""); // 15 Tax2
                csvwriter.writeValue(""); // 16 AutoDelete
                csvwriter.writeValue(""); // 17 Private
                csvwriter.write(note); // 18 Note (use quotes)
                csvwriter.writeValue(""); // 19 Alarm
                csvwriter.writeValue("0"); // 20 AlarmMidi
                csvwriter.writeValue("0"); // 21 Icon
                csvwriter.writeValue(""); // 22 AutoOrder

                csvwriter.writeNewline();
            }
            ci.close();
        }

        csvwriter.close();
    }

    String getHandyShopperStatusText(int status) {
        String statusText = "";
        if (status == Status.WANT_TO_BUY) {
            statusText = "x";
        } else if (status == Status.REMOVED_FROM_LIST) {
            statusText = "have";
        } else if (status == Status.BOUGHT) {
            statusText = "";
        }
        return statusText;
    }

    private String getHandyShopperNote(long itemId) {
        Uri uri = ContentUris.withAppendedId(ShoppingContract.Items.CONTENT_URI, itemId);

        String note = "";
        Cursor c1 = mContext.getContentResolver().query(uri,
                new String[]{ShoppingContract.Items.NOTE}, null, null, null);
        if (c1 != null) {
            if (c1.moveToFirst()) {
                note = c1.getString(0);
            }
            c1.close();
        }
        return note;
    }

    private String getHandyShopperStores(long itemId) {
        String stores = "";

        Cursor c1 = mContext.getContentResolver().query(
                ShoppingContract.ItemStores.CONTENT_URI,
                new String[]{ShoppingContract.ItemStores.ITEM_ID,
                        ShoppingContract.ItemStores.STORE_ID},
                ShoppingContract.ItemStores.ITEM_ID + " = ?",
                new String[]{"" + itemId}, null);
        if (c1 != null) {
            while (c1.moveToNext()) {
                long storeId = c1.getLong(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.STORE_ID));
                Uri uri2 = ContentUris.withAppendedId(ShoppingContract.Stores.CONTENT_URI, storeId);
                Cursor c2 = mContext.getContentResolver().query(uri2,
                        new String[]{ShoppingContract.Stores.NAME}, null, null, null);
                if (c2 != null) {
                    if (c2.moveToFirst()) {
                        String storeName = c2.getString(c2.getColumnIndexOrThrow(ShoppingContract.Stores.NAME));
                        if (stores.equals("")) {
                            stores = storeName;
                        } else {
                            stores += ";" + storeName;
                        }
                    }
                    c2.close();
                }
            }
            c1.close();
        }
        return stores;
    }

    // Deal with per-store aisles and prices from column 11.
    // example value for column 11:    Big Y=/0.50;BJ's=11/0.42
    private String getHandyShopperPerStoreInfo(long itemId) {
        String perStoreInfo = "";

        Cursor c1 = mContext.getContentResolver().query(
                ShoppingContract.ItemStores.CONTENT_URI,
                new String[]{ShoppingContract.ItemStores.ITEM_ID,
                        ShoppingContract.ItemStores.STORE_ID,
                        ShoppingContract.ItemStores.AISLE,
                        ShoppingContract.ItemStores.PRICE},
                ShoppingContract.ItemStores.ITEM_ID + " = ?",
                new String[]{"" + itemId}, null);
        if (c1 != null) {
            while (c1.moveToNext()) {
                long storeId = c1.getLong(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.STORE_ID));
                String aisle = c1.getString(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.AISLE));
                long price = c1.getLong(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.PRICE));
                String pricestring = "" + (double) price / 100.d;

                Uri uri2 = ContentUris.withAppendedId(ShoppingContract.Stores.CONTENT_URI, storeId);
                Cursor c2 = mContext.getContentResolver().query(uri2,
                        new String[]{ShoppingContract.Stores.NAME}, null, null, null);

                if (c2 != null) {
                    if (c2.moveToFirst()) {
                        String storeName = c2.getString(c2.getColumnIndexOrThrow(ShoppingContract.Stores.NAME));

                        if (price != 0) {
                            String info = storeName + "=" + aisle + "/" + pricestring;

                            if (perStoreInfo.equals("")) {
                                perStoreInfo = info;
                            } else {
                                perStoreInfo += ";" + info;
                            }
                        }
                    }
                    c2.close();
                }
            }
            c1.close();
        }
        return perStoreInfo;
    }

}
