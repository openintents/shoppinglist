package org.openintents.provider

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

import android.app.AlarmManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.UriMatcher
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log

/**
 * Provider for the Alert Framework, Make sure you call init(Context c) before
 * using any of the convenience functions. Location.Position is an Uri of the
 * format geo:long,lat example: geo:3.1472,567890 Location.Distance is distance
 * in meters as long.
 *
 * @author Ronan 'Zero' Schwarz
 */
open class Alert {

    companion object {
        const val _TAG = "Alert"

        const val TYPE_LOCATION = "location"
        const val TYPE_SENSOR = "sensor"
        const val TYPE_GENERIC = "generic"
        const val TYPE_COMBINED = "combined"
        const val TYPE_DATE_TIME = "datetime"

        const val NATURE_USER = "user"
        const val NATURE_SYSTEM = "system"

        // ugly hack to make the mock provider work,
        // see [..]
        const val LOCATION_EXPIRES = 1000000L
        const val EXTRA_URI = "URI"

        private val URL_MATCHER: UriMatcher
        private const val ALERT_GENERIC = 100
        private const val ALERT_GENERIC_ID = 101
        private const val ALERT_LOCATION = 102
        private const val ALERT_LOCATION_ID = 103
        private const val ALERT_COMBINED = 104
        private const val ALERT_COMBINED_ID = 105
        private const val ALERT_SENSOR = 106
        private const val ALERT_SENSOR_ID = 106
        private const val ALERT_DATE_TIME = 107
        private const val ALERT_DATE_TIME_ID = 108

        @JvmField
        var mContentResolver: ContentResolver? = null

        @JvmField
        var locationManager: LocationManager? = null

        @JvmField
        var alarmManager: AlarmManager? = null

        @JvmField
        var context: Context? = null

        init {
            URL_MATCHER = UriMatcher(UriMatcher.NO_MATCH)

            URL_MATCHER.addURI("org.openintents.alert", "generic/", ALERT_GENERIC)
            URL_MATCHER.addURI("org.openintents.alert", "generic/#", ALERT_GENERIC_ID)
            URL_MATCHER.addURI("org.openintents.alert", "location", ALERT_LOCATION)
            URL_MATCHER.addURI("org.openintents.alert", "location/#", ALERT_LOCATION_ID)
            URL_MATCHER.addURI("org.openintents.alert", "combined", ALERT_COMBINED)
            URL_MATCHER.addURI("org.openintents.alert", "combined/#", ALERT_COMBINED_ID)
            URL_MATCHER.addURI("org.openintents.alert", "datetime", ALERT_DATE_TIME)
            URL_MATCHER.addURI("org.openintents.alert", "datetime/#", ALERT_DATE_TIME_ID)
            URL_MATCHER.addURI("org.openintents.alert", "", 6000)
            URL_MATCHER.addURI("org.openintents.alert", "/", 6001)
        }

        @JvmStatic
        fun registerManagedService(
            serviceClassName: String,
            timeIntervall: Long,
            useWhileRoaming: Boolean
        ) {
            var minTime: Long
            var cv: ContentValues

            var c: android.database.Cursor? = mContentResolver!!.query(
                ManagedService.CONTENT_URI,
                ManagedService.PROJECTION,
                ManagedService.SERVICE_CLASS + " like '" + serviceClassName + "'",
                null,
                null
            )

            if (c != null && c.count > 0) { // update
                c.moveToFirst()
                val id = c.getString(c.getColumnIndexOrThrow(ManagedService._ID))
                val values = ContentValues()
                values.put(ManagedService.TIME_INTERVALL, java.lang.Long.toString(timeIntervall))
                values.put(ManagedService.DO_ROAMING, java.lang.Boolean.toString(useWhileRoaming))
                mContentResolver!!.update(
                    Uri.withAppendedPath(ManagedService.CONTENT_URI, id),
                    values,
                    null,
                    null
                )
            } else {
                // insert
                cv = ContentValues()
                cv.put(ManagedService.SERVICE_CLASS, serviceClassName)
                cv.put(ManagedService.TIME_INTERVALL, timeIntervall)
                cv.put(ManagedService.DO_ROAMING, useWhileRoaming)
                insert(ManagedService.CONTENT_URI, cv)
            }
            c?.close()

            // get all entry && compute new minimum time intervall
            // TODO: make this in sql.
            c = mContentResolver!!.query(
                ManagedService.CONTENT_URI,
                ManagedService.PROJECTION,
                null,
                null,
                null
            )!!

            c.moveToFirst()
            minTime = c.getLong(c.getColumnIndexOrThrow(ManagedService.TIME_INTERVALL))
            while (!c.isAfterLast) {
                val l = c.getLong(c.getColumnIndexOrThrow(ManagedService.TIME_INTERVALL))
                if (l < minTime) {
                    minTime = l
                }
                c.moveToNext()
            }
            c.close()

            c = mContentResolver!!.query(
                DateTime.CONTENT_URI,
                DateTime.PROJECTION,
                DateTime.INTENT + " like '" + org.openintents.OpenIntents.SERVICE_MANAGER + "'",
                null,
                null
            )
            val now = "time:epoch," + System.currentTimeMillis()
            cv = ContentValues()
            cv.put(DateTime.TIME, now)
            cv.put(DateTime.REOCCURENCE, minTime)
            cv.put(DateTime.INTENT, org.openintents.OpenIntents.SERVICE_MANAGER)
            cv.put(DateTime.NATURE, NATURE_SYSTEM)
            cv.put(DateTime.ACTIVATE_ON_BOOT, true)
            cv.put(DateTime.ACTIVE, true)
            cv.put(DateTime.TYPE, TYPE_DATE_TIME)
            if (c != null && c.count > 0) {
                update(
                    DateTime.CONTENT_URI,
                    cv,
                    DateTime.INTENT + " like '" + org.openintents.OpenIntents.SERVICE_MANAGER + "'",
                    null
                )

                // TODO new SDK cancle PendingIntent
                // alarmManager.cancel(new
                // Intent().setAction(org.openintents.OpenIntents.SERVICE_MANAGER));

                registerDateTimeAlert(cv)
                // registerDateTimeAlert
            } else {
                insert(DateTime.CONTENT_URI, cv)
            }
            c?.close()
            Log.d(_TAG, "registerManagedService: finished")
        }

        @JvmStatic
        fun unregisterManagedService(serviceClassName: String) {
            val cv = ContentValues()
            var minTime: Long

            delete(
                ManagedService.CONTENT_URI,
                ManagedService.SERVICE_CLASS + " like '" + serviceClassName + "'",
                null
            )

            // get all entry && compute new minimum time intervall
            // TODO: make this in sql.
            var c: android.database.Cursor = mContentResolver!!.query(
                ManagedService.CONTENT_URI,
                ManagedService.PROJECTION,
                null,
                null,
                null
            )!!

            c.moveToFirst()
            minTime = c.getLong(c.getColumnIndexOrThrow(ManagedService.TIME_INTERVALL))
            while (!c.isAfterLast) {
                val l = c.getLong(c.getColumnIndexOrThrow(ManagedService.TIME_INTERVALL))
                if (l < minTime) {
                    minTime = l
                }
                c.moveToNext()
            }
            c.close()

            c = mContentResolver!!.query(
                DateTime.CONTENT_URI,
                DateTime.PROJECTION,
                DateTime.INTENT + " like '" + org.openintents.OpenIntents.SERVICE_MANAGER + "'",
                null,
                null
            )!!
            val now = "time:epoch," + System.currentTimeMillis()
            cv.put(DateTime.TIME, now)
            cv.put(DateTime.REOCCURENCE, minTime)
            cv.put(DateTime.INTENT, org.openintents.OpenIntents.SERVICE_MANAGER)
            cv.put(DateTime.NATURE, NATURE_SYSTEM)
            cv.put(DateTime.ACTIVATE_ON_BOOT, true)
            cv.put(DateTime.ACTIVE, true)
            cv.put(DateTime.TYPE, TYPE_DATE_TIME)
            if (c.count > 0) {
                update(
                    DateTime.CONTENT_URI,
                    cv,
                    DateTime.INTENT + " like '" + org.openintents.OpenIntents.SERVICE_MANAGER + "'",
                    null
                )

                // TODO new SDK cancle PendingIntent
                // alarmManager.cancel(new
                // Intent().setAction(org.openintents.OpenIntents.SERVICE_MANAGER));
                registerDateTimeAlert(cv)
                // registerDateTimeAlert
            }
        }

        /**
         * @param uri the content uri to insert to
         * @param cv  the ContentValues that will be inserted to
         */
        @JvmStatic
        fun insert(uri: Uri, cv: ContentValues): Uri? {
            val type = URL_MATCHER.match(uri)

            val res = mContentResolver!!.insert(uri, cv)
            Log.d(_TAG, " insert, result>>" + res + "<<")
            if (res != null) { // register alert
                Log.d(_TAG, "uri>>" + uri + "<< matched>>" + type + "<<")
                when (type) {
                    ALERT_LOCATION -> registerLocationAlert(cv)
                    ALERT_DATE_TIME -> registerDateTimeAlert(cv)
                    else -> {
                    }
                }
            }
            return res
        }

        /**
         * @param uri           the content uri to delete
         * @param selection     the selection to check against
         * @param selectionArgs the arguments applied to selection string (optional)
         * @return number of deleted rows
         */
        @JvmStatic
        fun delete(uri: Uri, selection: String?, selectionArgs: Array<String?>?): Int {
            return mContentResolver!!.delete(uri, selection, selectionArgs)
        }

        /**
         * @param uri           the content uri to update
         * @param cv            the ContentValues that will be update in selected rows.
         * @param selection     the selection to check against
         * @param selectionArgs the arguments applied to selection string (optional)
         * @return number of updated rows
         */
        @JvmStatic
        fun update(
            uri: Uri,
            values: ContentValues,
            selection: String?,
            selectionArgs: Array<String?>?
        ): Int {
            return mContentResolver!!.update(uri, values, selection, selectionArgs)
        }

        @JvmStatic
        fun registerLocationAlert(cv: ContentValues) {
            var gUri: Uri? = null
            var distStr = ""

            var geo = ""

            try {
                gUri = Uri.parse(cv.getAsString(Location.POSITION))
                // do this for easier debugging.
                distStr = cv.getAsString(Location.DISTANCE)
                // float dist=cv.getAsFloat(Location.DISTANCE);
                geo = gUri.schemeSpecificPart
                @Suppress("UNUSED_VARIABLE")
                val loc = geo.split(",".toRegex()).toTypedArray()

                // TODO: find out how to handle this now
                /*
                 * PendingIntent i= new PendingIntent();
                 * //i.setClassName("org.openintents.alert"
                 * ,"LocationAlertDispatcher");
                 * i.setAction("org.openintents.action.LOCATION_ALERT_DISPATCH");
                 * //i.setData(gUri); i.putExtra(Location.POSITION,
                 * cv.getAsString(Location.POSITION));
                 *
                 * locationManager.addProximityAlert( latitude, longitude, dist,
                 * LOCATION_EXPIRES, i );
                 * Log.d(_TAG,"Registerd alert geo:"+geo+" dist:"+dist);
                 * Log.d(_TAG,"Registered alert intent:" + i);
                 */
            } catch (aioe: ArrayIndexOutOfBoundsException) {
                Log.e(_TAG, "Error parsing geo uri. not in format geo:lat,long")
            } catch (nfe: NumberFormatException) {
                Log.e(
                    _TAG,
                    "Error parsing longitude/latitude. Not A Number (NAN)\n uri>>" +
                            gUri + "<< \n dist>>" + distStr + "<<"
                )
            } catch (npe: NullPointerException) {
                Log.e(_TAG, "Nullpointer occured. did you call init(context) ?")
                npe.printStackTrace()
            }

            // registerReceiver(org.openintents.alert.LocationAlertDispatcher,);
        }

        @JvmStatic
        fun init(c: Context) {
            context = c
            locationManager =
                context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            mContentResolver = context!!.contentResolver
            alarmManager = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }

        @JvmStatic
        fun registerDateTimeAlert(cv: ContentValues) {
            val myDate = cv.getAsString(DateTime.TIME)
            val s = myDate.split(",".toRegex()).toTypedArray()
            Log.d(_TAG, "registerDateTimeAlert: s[0]>>" + s[0] + "<< s[1]>>+" + s[1] + "<<")
            var time = 0L
            val myReoccurence = cv.getAsLong(DateTime.REOCCURENCE)
            val i = Intent()
            val b = Bundle()
            b.putString(DateTime.TIME, myDate)
            b.putLong(DateTime.REOCCURENCE, myReoccurence)
            i.action = org.openintents.OpenIntents.DATE_TIME_ALERT_DISPATCH
            try {
                time = s[1].toLong()
            } catch (nfe: NumberFormatException) {
                Log.e(
                    _TAG,
                    "registerDateTimeAlert: Date/Time couldn't be parsed, check time format of >" +
                            myDate + "<"
                )
                return
            }

            if (myReoccurence == 0L) {
                // TODO new SDK cancle PendingIntent
                // alarmManager.set(AlarmManager.RTC,time,i);
                Log.d(_TAG, "registerDateTimeAlert: registerd single @>>" + time + "<<")
            } else {
                // TODO new SDK cancle PendingIntent
                // alarmManager.setRepeating(AlarmManager.RTC,time,myReoccurence,i);
                Log.d(
                    _TAG,
                    "registerDateTimeAlert: registerd reoccuirng @>>" + time +
                            "<< intervall>>" + myReoccurence + "<<"
                )
            }
        }

        @JvmStatic
        fun unregisterDateTimeAlert(@Suppress("UNUSED_PARAMETER") cv: ContentValues) {
            /*
             * String myDate=cv.getAsString(DateTime.TIME); String
             * s[]=myDate.split(",");
             * Log.d(_TAG,"registerDateTimeAlert: s[0]>>"+s[0]
             * +"<< s[1]>>+"+s[1]+"<<"); long time=0; long
             * myReoccurence=cv.getAsLong(DateTime.REOCCURENCE);
             *
             * Cursor c=mContentResolver.query( DateTime.CONTENT_URI,
             * DateTime.PROJECTION_MAP, DateTime.TIME+" like '"+myDate+"'", null
             * null );
             *
             * if (c==null||c.count()==0) {//alert has been deleted
             *
             * }else if (c!=null&&c.count()==1) {//exactly out alert alarmManager. }
             * //TODO: check if there are now other alerts at this time.
             */

            // atm it would oly be possible to delete all dateTimeDispatch alerts
            // and register the need a new. so we just leave them be, means some
            // emtpy lookups, but hey! :/
        }
    }

    // -------------------------------------------------------------------------
    // Nested companion-as-static-class pattern:
    // Each inner class implements BaseColumns so Java code can access
    // Alert.Generic.CONTENT_URI, Alert.Location.ACTIVE, etc.
    // CONTENT_URI and PROJECTION use @JvmField (runtime object, not compile-time const).
    // All String column name constants use `const val`.
    // -------------------------------------------------------------------------

    class Generic : BaseColumns {
        companion object : BaseColumns {
        const val _ID = "_id"
        const val _COUNT = "_count"
            @JvmField
            val CONTENT_URI: Uri = Uri.parse("content://org.openintents.alert/generic")

            const val CONDITION1 = "condition1"
            const val CONDITION2 = "condition2"
            const val TYPE = "alert_type"
            const val RULE = "rule"
            const val NATURE = "nature"
            const val ACTIVE = "active"
            const val ACTIVATE_ON_BOOT = "activate_on_boot"
            const val INTENT = "intent"
            const val INTENT_CATEGORY = "intent_category"
            const val INTENT_URI = "intent_uri"
            const val INTENT_MIME_TYPE = "intent_mime_type"
            const val DEFAULT_SORT_ORDER = ""

            @JvmField
            val PROJECTION: Array<String> = arrayOf(
                _ID, _COUNT, CONDITION1, CONDITION2, TYPE, RULE, NATURE,
                ACTIVE, ACTIVATE_ON_BOOT, INTENT, INTENT_CATEGORY, INTENT_URI, INTENT_MIME_TYPE
            )
        }
    }

    /**
     * location based alerts. you must at least specify a position
     */
    class Location : BaseColumns {
        companion object : BaseColumns {
        const val _ID = "_id"
        const val _COUNT = "_count"
            @JvmField
            val CONTENT_URI: Uri = Uri.parse("content://org.openintents.alert/location")

            /**
             * Location.Position is an Uri of the format geo:long,lat example:
             * geo:3.1472,567890
             */
            const val POSITION = Generic.CONDITION1

            /**
             * Location.Distance is distance in meters as long.
             */
            const val DISTANCE = Generic.CONDITION2

            /**
             * Type must always be of Alert.TYPE_LOCATION any other values will
             * result in your alert not being processed.
             */
            const val TYPE = Generic.TYPE

            const val RULE = Generic.RULE
            const val NATURE = Generic.NATURE
            const val ACTIVE = Generic.ACTIVE
            const val ACTIVATE_ON_BOOT = Generic.ACTIVATE_ON_BOOT
            const val INTENT = Generic.INTENT
            const val INTENT_CATEGORY = Generic.INTENT_CATEGORY
            const val INTENT_URI = Generic.INTENT_URI
            const val INTENT_MIME_TYPE = Generic.INTENT_MIME_TYPE
            const val DEFAULT_SORT_ORDER = ""

            @JvmField
            val PROJECTION: Array<String> = arrayOf(
                _ID, _COUNT, POSITION, DISTANCE, TYPE, RULE, NATURE,
                ACTIVE, ACTIVATE_ON_BOOT, INTENT, INTENT_CATEGORY, INTENT_URI, INTENT_MIME_TYPE
            )
        }
    }

    class DateTime : BaseColumns {
        companion object : BaseColumns {
        const val _ID = "_id"
        const val _COUNT = "_count"
            @JvmField
            val CONTENT_URI: Uri = Uri.parse("content://org.openintents.alert/datetime")

            /**
             * The point in time for the alarm, in format time:epoch1234456 number
             * is time in millisecond sice 1970, like you get from
             * System.getCurrentMillis
             */
            const val TIME = Generic.CONDITION1

            /**
             * the alert reocurs every n milliseconds, or not at all if set to 0.
             * reouccreny should be at least 1 minute.
             */
            const val REOCCURENCE = Generic.CONDITION2

            const val TYPE = Generic.TYPE
            const val RULE = Generic.RULE
            const val NATURE = Generic.NATURE
            const val ACTIVE = Generic.ACTIVE
            const val ACTIVATE_ON_BOOT = Generic.ACTIVATE_ON_BOOT
            const val INTENT = Generic.INTENT
            const val INTENT_CATEGORY = Generic.INTENT_CATEGORY
            const val INTENT_URI = Generic.INTENT_URI
            const val INTENT_MIME_TYPE = Generic.INTENT_MIME_TYPE
            const val DEFAULT_SORT_ORDER = ""

            @JvmField
            val PROJECTION: Array<String> = arrayOf(
                _ID, _COUNT, TIME, REOCCURENCE, TYPE, RULE, NATURE,
                ACTIVE, ACTIVATE_ON_BOOT, INTENT, INTENT_CATEGORY, INTENT_URI, INTENT_MIME_TYPE
            )
        }
    }

    class ManagedService : BaseColumns {
        companion object : BaseColumns {
        const val _ID = "_id"
        const val _COUNT = "_count"
            @JvmField
            val CONTENT_URI: Uri = Uri.parse("content://org.openintents.alert/managedservice")

            const val SERVICE_CLASS = "service_class"
            const val TIME_INTERVALL = "time_intervall"
            const val DO_ROAMING = "do_roaming"
            const val LAST_TIME = "last_time"

            @JvmField
            val PROJECTION: Array<String> = arrayOf(
                _ID, _COUNT, SERVICE_CLASS, TIME_INTERVALL, DO_ROAMING, LAST_TIME
            )
        }
    }
} /* eoc */
