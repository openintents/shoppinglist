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

package org.openintents.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Point
import android.net.Uri
import android.provider.BaseColumns

/**
 * Definition for content provider related to location.
 */
open class Location(private val mResolver: ContentResolver) {

    fun addLocation(location: android.location.Location): Uri? {
        val values = ContentValues(2)
        values.put(Locations.LATITUDE, location.latitude)
        values.put(Locations.LONGITUDE, location.longitude)
        return mResolver.insert(Locations.CONTENT_URI, values)
    }

    fun deleteLocation(id: Long): Int {
        return mResolver.delete(
            ContentUris.withAppendedId(Locations.CONTENT_URI, id),
            null,
            null
        )
    }

    fun getPoint(id: Long): Point? {
        var p: Point? = null
        val cursor = mResolver.query(
            ContentUris.withAppendedId(Locations.CONTENT_URI, id),
            arrayOf(Locations._ID, Locations.LATITUDE, Locations.LONGITUDE),
            null,
            null,
            Locations.DEFAULT_SORT_ORDER
        )!!
        if (cursor.moveToNext()) {
            val lat = (cursor.getDouble(1) * 1E6).toInt()
            val lon = (cursor.getDouble(2) * 1E6).toInt()
            p = Point(lat, lon)
        }
        cursor.close()
        return p
    }

    /**
     * @param locationId
     * @return
     * @deprecated !! Warning !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    @Deprecated("Cursor has to be closed by caller. Alternative API desired.")
    fun queryExtras(locationId: Long): android.database.Cursor? {
        val uri = Locations.CONTENT_URI.buildUpon()
            .appendPath(locationId.toString())
            .appendPath(Extras.URI_PATH_EXTRAS)
        return mResolver.query(
            uri.build(),
            arrayOf(Extras._ID, Extras.KEY, Extras.VALUE),
            null,
            null,
            Extras.KEY + "," + Extras.VALUE
        )
    }

    fun updateExtras(locationId: Long, extrasId: Long, values: ContentValues) {
        // empty body preserved from Java original
    }

    fun deleteExtra(extraId: Long): Int {
        return mResolver.delete(
            ContentUris.withAppendedId(Extras.CONTENT_URI, extraId),
            null,
            null
        )
    }

    fun addExtra(locationId: Long): Uri? {
        val uri = Locations.CONTENT_URI.buildUpon()
            .appendPath(locationId.toString())
            .appendPath(Extras.URI_PATH_EXTRAS)
        return mResolver.insert(uri.build(), null)
    }

    class Locations : BaseColumns {
        companion object : BaseColumns {
        const val _ID = "_id"
        const val _COUNT = "_count"
            /**
             * The content:// style URL for this table
             */
            @JvmField
            val CONTENT_URI: Uri = Uri.parse("content://org.openintents.locations/locations")

            /**
             * The default sort order for this table
             */
            const val DEFAULT_SORT_ORDER = "modified DESC"

            /**
             * The latitude of the location
             *
             * Type: TEXT
             */
            const val LATITUDE = "latitude"

            /**
             * The longitude of the location
             *
             * Type: TEXT
             */
            const val LONGITUDE = "longitude"

            /**
             * The timestamp for when the note was created
             *
             * Type: INTEGER (long)
             */
            const val CREATED_DATE = "created"

            /**
             * The timestamp for when the note was last modified
             *
             * Type: INTEGER (long)
             */
            const val MODIFIED_DATE = "modified"

            /**
             * bundle/extra key for pick action, containing location uri with scheme
             * geo:
             */
            const val EXTRA_GEO = "geo"
        }
    }

    class Extras : BaseColumns {
        companion object : BaseColumns {
        const val _ID = "_id"
        const val _COUNT = "_count"
            const val LOCATION_ID = "locationId"
            const val KEY = "key"
            const val VALUE = "value"
            const val URI_PATH_EXTRAS = "extras"

            @JvmField
            val CONTENT_URI: Uri = Uri.parse("content://org.openintents.locations/extras")
        }
    }
}
