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

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Xml.Encoding
import android.view.View
import android.widget.AdapterView
import org.openintents.convertcsv.PreferenceActivity
import org.openintents.convertcsv.common.ConvertCsvBaseActivity
import org.openintents.convertcsv.common.WrongFormatException
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.util.ShoppingUtils
import java.io.IOException
import java.io.Reader
import java.io.Writer

open class ConvertCsvActivity : ConvertCsvBaseActivity() {

    companion object {
        @JvmField
        val TAG = "ConvertCsvActivity"
    }

    val HANDYSHOPPER_FORMAT = "handyshopper"

    override fun setPreferencesUsed() {
        PREFERENCE_FILENAME = PreferenceActivity.PREFS_SHOPPINGLIST_FILENAME
        DEFAULT_FILENAME = getString(R.string.default_shoppinglist_filename)
        PREFERENCE_FORMAT = PreferenceActivity.PREFS_SHOPPINGLIST_FORMAT
        DEFAULT_FORMAT = "outlook tasks"
        PREFERENCE_ENCODING = PreferenceActivity.PREFS_SHOPPINGLIST_ENCODING
        PREFERENCE_USE_CUSTOM_ENCODING = PreferenceActivity.PREFS_SHOPPINGLIST_USE_CUSTOM_ENCODING
        RES_STRING_FILEMANAGER_TITLE = R.string.filemanager_title_shoppinglist
        RES_ARRAY_CSV_FILE_FORMAT = R.array.shoppinglist_format
        RES_ARRAY_CSV_FILE_FORMAT_VALUE = R.array.shoppinglist_format_value
    }

    /**
     * Called when the activity is first created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (mConvertInfo != null) {
            mConvertInfo!!.setText(R.string.convert_all_shoppinglists)
        }

        if (mSpinner != null) {
            mSpinner!!.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>, selectedItemView: View, position: Int, id: Long
                ) {
                    updateInfo()
                }

                override fun onNothingSelected(parentView: AdapterView<*>) {
                    updateInfo()
                }
            })
        }
    }

    open fun updateInfo() {
        if (mConvertInfo == null) return

        val format = getFormat()
        if (DEFAULT_FORMAT == format) {
            mConvertInfo!!.setText(R.string.convert_all_shoppinglists)
        } else if (HANDYSHOPPER_FORMAT == format) {
            val listId = getCurrentListId()
            val listname = getListName(listId)
            if (listname != null) {
                val text = getString(R.string.convert_list, listname)
                mConvertInfo!!.setText(text)
            }
        }
    }

    /**
     * @param reader
     * @throws IOException
     */
    @Throws(IOException::class, WrongFormatException::class)
    override fun doImport(reader: Reader) {
        val ic = ImportCsv(this, getValidatedImportPolicy())
        val format = getFormat()
        if (DEFAULT_FORMAT == format) {
            ic.importCsv(reader)
        } else if (HANDYSHOPPER_FORMAT == format) {
            val pm = PreferenceManager.getDefaultSharedPreferences(this)
            val importStores = pm.getBoolean("shoppinglist_import_stores", true)
            val listId = getCurrentListId()
            ic.importHandyShopperCsv(reader, listId, importStores)
        }
    }

    override fun onImportFinished() {
        val i = Intent(Intent.ACTION_VIEW)
        val uri = ShoppingContract.Lists.CONTENT_URI.buildUpon()
            .appendPath(getCurrentListId().toString()).build()
        i.data = uri
        startActivity(i)
    }

    override fun getDefaultEncoding(): Encoding {
        val id = mSpinner!!.selectedItemId
        return when (id) {
            0L -> Encoding.ISO_8859_1 // Default encoding for "MS Outlook Tasks".
            1L -> Encoding.UTF_8      // Default encoding for "HandyShopper".
            else -> super.getDefaultEncoding()
        }
    }

    /**
     * @param writer
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun doExport(writer: Writer) {
        val ec = ExportCsv(this)
        val format = getFormat()
        if (DEFAULT_FORMAT == format) {
            ec.exportCsv(writer)
        } else if (HANDYSHOPPER_FORMAT == format) {
            val listId = getCurrentListId()
            ec.exportHandyShopperCsv(writer, listId)
            /*runOnUiThread {
                Toast.makeText(this@ConvertCsvActivity, R.string.error_not_yet_implemented,
                    Toast.LENGTH_LONG).show()
            }*/
        }
    }

    /**
     * @return The string that identifies the import policy for this importer.
     * null if this derived activity does not support import policies.
     */
    override fun getImportPolicyPrefString(): String {
        return "shoppinglist_import_policy"
    }

    /**
     * @return The default import policy
     */
    override fun getDefaultImportPolicy(): String {
        return "" + IMPORT_POLICY_KEEP
    }

    open fun getCurrentListId(): Long {
        var listId: Long = -1

        // Try the URI with which Convert CSV has been called:
        val uri: Uri? = intent.data
        val c: Cursor? = contentResolver.query(
            uri!!,
            arrayOf(ShoppingContract.Lists._ID), null, null, null
        )
        if (c != null) {
            if (c.moveToFirst()) {
                listId = c.getLong(0)
            }
            c.close()
        }

        // Use default list if URI is not valid.
        if (listId < 0) {
            listId = ShoppingUtils.getDefaultList(this)
        }
        return listId
    }

    open fun getListName(listId: Long): String? {
        var listname: String? = null
        val uri: Uri? = intent.data
        val c: Cursor? = contentResolver.query(
            uri!!,
            arrayOf(ShoppingContract.Lists.NAME), null, null, null
        )
        if (c != null) {
            if (c.moveToFirst()) {
                listname = c.getString(0)
            }
            c.close()
        }
        return listname
    }
}
