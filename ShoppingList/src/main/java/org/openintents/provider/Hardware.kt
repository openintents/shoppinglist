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

package org.openintents.provider

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log

/**
 * Definition for content provider related to hardware. Stores hardware
 * abstraction and hardware simulator related data.
 */
abstract class Hardware {

    companion object {
        @JvmField
        val mProjectionPreferencesFilter: Array<String> =
            arrayOf(Preferences._ID, Preferences.NAME, Preferences.VALUE)

        const val mProjectionPreferencesID = 0

        // //////////////////////////////////////////////////////
        // Convenience functions:
        const val mProjectionPreferencesNAME = 1
        const val mProjectionPreferencesVALUE = 2

        // Some default preference values
        const val IPADDRESS = "IP address"
        const val SOCKET = "Socket"
        const val DEFAULT_SOCKET = "8010"

        /**
         * TAG for logging.
         */
        private const val TAG = "Hardware"

        /**
         * The content resolver has to be set before accessing any of these
         * functions.
         */
        @JvmField
        var mContentResolver: ContentResolver? = null

        /**
         * Obtains the 'value' for preferenceID, or returns "" if not existent.
         *
         * @param name The name of the preference.
         * @return The value for preference 'name'.
         */
        @JvmStatic
        fun getPreference(name: String): String {
            var s = ""
            try {
                Log.i(TAG, "getPreference()")
                val c = mContentResolver!!.query(
                    Preferences.CONTENT_URI,
                    mProjectionPreferencesFilter,
                    Preferences.NAME + "= '" + name + "'",
                    null,
                    Preferences.DEFAULT_SORT_ORDER
                )!!
                if (c.count >= 1) {
                    c.moveToFirst()
                    return c.getString(mProjectionPreferencesVALUE)
                } else if (c.count == 0) {
                    // This value does not exist yet!
                } else {
                    Log.e(TAG, "table 'preferences' corrupt. Multiple NAME!")
                }
                c.close()
            } catch (e: Exception) {
                Log.e(TAG, "insert into table 'contains' failed", e)
                s = "Preferences table corrupt!"
            }
            return s
        }

        /**
         * Updates the 'value' for the preferenceID.
         *
         * @param name  The name of the preference.
         * @param value The value to set.
         */
        @JvmStatic
        fun setPreference(name: String, value: String) {
            /*
             * // This value does not exist yet. Let's insert it: ContentValues
             * values2 = new ContentValues(2); values2.put(Preferences.NAME, name);
             * values2.put(Preferences.VALUE, value);
             * mContentResolver.insert(Preferences.CONTENT_URI, values2);
             */

            Log.i(TAG, "setPreference")
            try {
                Log.i(TAG, "get Cursor.")
                if (mContentResolver == null) {
                    Log.i(TAG, "Panic!.")
                }
                val c = mContentResolver!!.query(
                    Preferences.CONTENT_URI,
                    mProjectionPreferencesFilter,
                    Preferences.NAME + "= '" + name + "'",
                    null,
                    Preferences.DEFAULT_SORT_ORDER
                )
                Log.i(TAG, "got Cursor.")
                // Log.i(TAG, "Cursor: " + c.toString());

                if (c == null) {
                    Log.e(TAG, "missing hardware provider")
                    return
                }

                if (c.count < 1) {
                    Log.i(TAG, "Insert")

                    // This value does not exist yet. Let's insert it:
                    val values = ContentValues(2)
                    values.put(Preferences.NAME, name)
                    values.put(Preferences.VALUE, value)
                    mContentResolver!!.insert(Preferences.CONTENT_URI, values)
                } else if (c.count >= 1) {
                    Log.i(TAG, "Update")

                    // This is the key, so we can update it:
                    c.moveToFirst()
                    val id = c.getString(mProjectionPreferencesID)
                    val cv = ContentValues()
                    cv.put(Preferences.VALUE, value)
                    mContentResolver!!.update(
                        Uri.withAppendedPath(Preferences.CONTENT_URI, id),
                        cv,
                        null,
                        null
                    )

                    // c.requery();
                    c.getString(mProjectionPreferencesVALUE)
                } else {
                    Log.e(TAG, "table 'preferences' corrupt. Multiple NAME!")
                }
                c.close()
            } catch (e: Exception) {
                Log.i(TAG, "setPreference() failed", e)
            }
        }
    }

    /**
     * Hardware preferences. Simple table to store name-value pairs.
     */
    class Preferences : BaseColumns {
        companion object : BaseColumns {
        const val _ID = "_id"
        const val _COUNT = "_count"
            /**
             * The content:// style URL for this table.
             */
            @JvmField
            val CONTENT_URI: Uri = Uri.parse("content://org.openintents.hardware/preferences")

            /**
             * The default sort order for this table.
             */
            const val DEFAULT_SORT_ORDER = "_id ASC"

            /**
             * The name of the item.
             *
             * Type: TEXT
             */
            const val NAME = "name"

            /**
             * An image of the item (uri).
             *
             * Type: TEXT
             */
            const val VALUE = "value"
        }
    }
}
