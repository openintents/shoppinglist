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

import android.content.Context;
import android.text.TextUtils;

import org.openintents.convertcsv.common.ConvertCsvBaseActivity;
import org.openintents.convertcsv.common.WrongFormatException;
import org.openintents.convertcsv.opencsv.CSVReader;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.library.util.ShoppingUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

public class ImportCsv {

    Context mContext;
    Boolean mDuplicate = true;
    Boolean mUpdate = false;

    public ImportCsv(Context context, int importPolicy) {
        mContext = context;
        switch (importPolicy) {
            case ConvertCsvBaseActivity.IMPORT_POLICY_KEEP:
                mDuplicate = false;
                mUpdate = false;
                break;
            case ConvertCsvBaseActivity.IMPORT_POLICY_RESTORE:
                // not implemented, treat as overwrite for now.
            case ConvertCsvBaseActivity.IMPORT_POLICY_OVERWRITE:
                mDuplicate = false;
                mUpdate = true;
                break;
            case ConvertCsvBaseActivity.IMPORT_POLICY_DUPLICATE:
                mDuplicate = true;
                mUpdate = false;
                break;
        }
    }

    /**
     * @param dis
     * @throws IOException
     */
    public void importCsv(Reader reader) throws IOException,
            WrongFormatException {
        CSVReader csvreader = new CSVReader(reader);
        String[] nextLine;
        while ((nextLine = csvreader.readNext()) != null) {
            if (nextLine.length != 4) {
                throw new WrongFormatException();
            }
            // nextLine[] is an array of values from the line
            String statusstring = nextLine[1];
            if (statusstring.equals(mContext.getString(R.string.header_percent_complete))) {
                // First line is just subject, so let us skip it
                continue;
            }
            String itemname = nextLine[0];
            long status;
            try {
                status = Long.parseLong(statusstring);
            } catch (NumberFormatException e) {
                status = 0;
            }
            String listname = nextLine[2];
            String tags = nextLine[3];

            // Add item to list
            long listId = ShoppingUtils.getList(mContext, listname);
            long itemId = ShoppingUtils.getItem(mContext, itemname, tags, null,
                    null, null, mDuplicate, mUpdate);

            if (status == 1) {
                status = Status.BOUGHT;
            } else if (status == 0) {
                status = Status.WANT_TO_BUY;
            } else {
                status = Status.REMOVED_FROM_LIST;
            }


            ShoppingUtils.addItemToList(mContext, itemId, listId, status, null, null,
                    false, mDuplicate, false);
        }

    }

    private String convert_hs_price(String hs_price) {
        String price = hs_price;
        try {
            Double fprice = Double.parseDouble(price);
            fprice *= 100;
            price = ((Long) Math.round(fprice)).toString();
        } catch (NumberFormatException nfe) {
        }
        return price;
    }

    public void importHandyShopperCsv(Reader reader, long listId, Boolean importStores) throws IOException, WrongFormatException {
        CSVReader csvreader = new CSVReader(reader);
        String[] nextLine;
        HashMap<String, Long> seen_stores = new HashMap<String, Long>();
        HashMap<String, Long> item_stores = new HashMap<String, Long>();

        while ((nextLine = csvreader.readNext()) != null) {
            if (nextLine.length != 23) {
                throw new WrongFormatException();
            }
            // nextLine[] is an array of values from the line
            String statusstring = nextLine[0];
            if (statusstring.equals(mContext.getString(R.string.header_need))) {
                // First line is just subject, so let us skip it
                continue;
            }

            long status;
            if ("x".equalsIgnoreCase(statusstring)) {
                status = Status.WANT_TO_BUY;
            } else if ("".equalsIgnoreCase(statusstring)) {
                status = Status.BOUGHT;
            } else if ("have".equalsIgnoreCase(statusstring)) {
                status = Status.REMOVED_FROM_LIST;
            } else {
                status = Status.REMOVED_FROM_LIST;
            }

            String itemname = nextLine[2]; // Description
            String tags = nextLine[9]; // Category
            String price = nextLine[6]; // Price
            String note = nextLine[18]; // Note
            String units = nextLine[5];

            if (nextLine[3].length() > 0) {
                if (tags.length() == 0) {
                    tags = nextLine[3];
                } else {
                    tags += "," + nextLine[3];
                }
            }

            String quantity = nextLine[4]; // Quantity
            String priority = nextLine[1]; // Priority

            if (price.length() > 0) {
                price = convert_hs_price(price);
            }

            // Add item to list
            //long listId = ShoppingUtils.getDefaultList(mContext);
            long itemId = ShoppingUtils.getItem(mContext, itemname, tags, price, units, note,
                    mDuplicate, mUpdate);
            ShoppingUtils.addItemToList(mContext, itemId, listId, status, priority, quantity,
                    false, mDuplicate, false);

            // Two columns contain per-store information. Column 10 lists
            // all stores which carry this item, delimited by semicolons. Column 11
            // lists aisles and prices for some subset of those stores.
            //
            // To save time, we first deal with the prices in column 11, then from
            // Column 10 we add only the ones not already added from Column 11.

            String[] stores;
            item_stores.clear();

            // example value for column 11:    Big Y=/0.50;BJ's=11/0.42
            if (nextLine[11].length() > 0 && importStores) {
                stores = nextLine[11].split(";");

                for (int i_store = 0; i_store < stores.length; i_store++) {
                    String[] key_vals = stores[i_store].split("=");
                    String store_name = key_vals[0];
                    String[] aisle_price = key_vals[1].split("/");
                    if (aisle_price.length == 0)
                        continue;
                    String aisle = aisle_price[0];
                    String store_price = "";
                    if (aisle_price.length > 1) {
                        store_price = convert_hs_price(aisle_price[1]);
                    }

                    Long storeId = seen_stores.get(store_name);
                    if (storeId == null) {
                        storeId = ShoppingUtils.getStore(mContext, store_name, listId);
                        seen_stores.put(store_name, storeId);
                    }
                    item_stores.put(store_name, storeId);
                    long item_store = ShoppingUtils.addItemToStore(mContext, itemId, storeId, aisle, store_price, mDuplicate);
                }
            }

            if (nextLine[10].length() > 0) {
                stores = nextLine[10].split(";");
                for (int i_store = 0; i_store < stores.length; i_store++) {
                    if (importStores) {    // real store import
                        Long storeId = item_stores.get(stores[i_store]);
                        if (storeId != null)
                            // existence of item at store handled in price handling, no need to add it again.
                            continue;
                        storeId = seen_stores.get(stores[i_store]);
                        if (storeId == null) {
                            storeId = ShoppingUtils.getStore(mContext, stores[i_store], listId);
                            seen_stores.put(stores[i_store], storeId);
                        }
                        item_stores.put(stores[i_store], storeId); // not strictly required, but...
                        long item_store = ShoppingUtils.addItemToStore(mContext, itemId, storeId, "", "", mDuplicate);
                    } else if (!TextUtils.isEmpty(stores[i_store])) {
                        // store names added as tags.
                        ShoppingUtils.addTagToItem(mContext, itemId, stores[i_store]);
                    }
                }
            }
        }
    }
}
