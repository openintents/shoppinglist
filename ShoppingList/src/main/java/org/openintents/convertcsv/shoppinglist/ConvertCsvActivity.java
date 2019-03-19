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

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Xml.Encoding;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import org.openintents.convertcsv.PreferenceActivity;
import org.openintents.convertcsv.common.ConvertCsvBaseActivity;
import org.openintents.convertcsv.common.WrongFormatException;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.util.ShoppingUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class ConvertCsvActivity extends ConvertCsvBaseActivity {

    public static final String TAG = "ConvertCsvActivity";

    final String HANDYSHOPPER_FORMAT = "handyshopper";

    public void setPreferencesUsed() {
        PREFERENCE_FILENAME = PreferenceActivity.PREFS_SHOPPINGLIST_FILENAME;
        DEFAULT_FILENAME = getString(R.string.default_shoppinglist_filename);
        PREFERENCE_FORMAT = PreferenceActivity.PREFS_SHOPPINGLIST_FORMAT;
        DEFAULT_FORMAT = "outlook tasks";
        PREFERENCE_ENCODING = PreferenceActivity.PREFS_SHOPPINGLIST_ENCODING;
        PREFERENCE_USE_CUSTOM_ENCODING = PreferenceActivity.PREFS_SHOPPINGLIST_USE_CUSTOM_ENCODING;
        RES_STRING_FILEMANAGER_TITLE = R.string.filemanager_title_shoppinglist;
        RES_ARRAY_CSV_FILE_FORMAT = R.array.shoppinglist_format;
        RES_ARRAY_CSV_FILE_FORMAT_VALUE = R.array.shoppinglist_format_value;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mConvertInfo != null) {
            mConvertInfo.setText(R.string.convert_all_shoppinglists);
        }

        if (mSpinner != null) {
            mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    updateInfo();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    updateInfo();
                }
            });
        }
    }

    public void updateInfo() {
        if (mConvertInfo == null) return;

        String format = getFormat();
        if (DEFAULT_FORMAT.equals(format)) {
            mConvertInfo.setText(R.string.convert_all_shoppinglists);
        } else if (HANDYSHOPPER_FORMAT.equals(format)) {
            long listId = getCurrentListId();
            String listname = getListName(listId);
            if (listname != null) {
                String text = getString(R.string.convert_list, listname);
                mConvertInfo.setText(text);
            }

        }
    }

    /**
     * @param reader
     * @throws IOException
     */
    @Override
    public void doImport(Reader reader) throws IOException,
            WrongFormatException {
        ImportCsv ic = new ImportCsv(this, getValidatedImportPolicy());
        String format = getFormat();
        if (DEFAULT_FORMAT.equals(format)) {
            ic.importCsv(reader);
        } else if (HANDYSHOPPER_FORMAT.equals(format)) {
            SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);
            Boolean importStores = pm.getBoolean("shoppinglist_import_stores", true);
            long listId = getCurrentListId();
            ic.importHandyShopperCsv(reader, listId, importStores);
        }
    }

    @Override
    public void onImportFinished() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        Uri uri = ShoppingContract.Lists.CONTENT_URI.buildUpon().appendPath(String.valueOf(getCurrentListId())).build();
        i.setData(uri);
        startActivity(i);
    }

    @Override
    protected Encoding getDefaultEncoding() {
        long id = mSpinner.getSelectedItemId();
        if (0 == id) {
            return Encoding.ISO_8859_1; // Default encoding for "MS Outlook Tasks".
        } else if (1 == id) {
            return Encoding.UTF_8; // Default encoding for "HandyShopper".
        } else {
            return super.getDefaultEncoding();
        }
    }

    /**
     * @param writer
     * @throws IOException
     */
    @Override
    public void doExport(Writer writer) throws IOException {
        ExportCsv ec = new ExportCsv(this);
        String format = getFormat();
        if (DEFAULT_FORMAT.equals(format)) {
            ec.exportCsv(writer);
        } else if (HANDYSHOPPER_FORMAT.equals(format)) {
            long listId = getCurrentListId();
            ec.exportHandyShopperCsv(writer, listId);
			/*runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(ConvertCsvActivity.this, R.string.error_not_yet_implemented,
							Toast.LENGTH_LONG).show();
				}
			});*/
        }
    }

    /**
     * @return The string that identifies the import policy for this importer.
     * null if this derived activity does not support import policies.
     */
    public String getImportPolicyPrefString() {
        return "shoppinglist_import_policy";
    }

    /**
     * @return The default import policy
     */
    public String getDefaultImportPolicy() {
        return "" + IMPORT_POLICY_KEEP;
    }

    public long getCurrentListId() {
        long listId = -1;

        // Try the URI with which Convert CSV has been called:
        Uri uri = getIntent().getData();
        Cursor c = getContentResolver().query(uri,
                new String[]{ShoppingContract.Lists._ID}, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                listId = c.getLong(0);
            }
            c.close();
        }

        // Use default list if URI is not valid.
        if (listId < 0) {
            listId = ShoppingUtils.getDefaultList(this);
        }
        return listId;
    }

    public String getListName(long listId) {
        String listname = null;
        Uri uri = getIntent().getData();
        Cursor c = getContentResolver().query(uri
                , new String[]{ShoppingContract.Lists.NAME}, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                listname = c.getString(0);
            }
            c.close();
        }
        return listname;
    }
}