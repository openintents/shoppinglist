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

package org.openintents

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException

/**
 * Provides OpenIntents action and category specifiers.
 *
 * These specifiers extend the standard Android specifiers.
 */
abstract class OpenIntents {

    companion object {
        // !! CAREFUL !!
        // If you change any of the string definitions, you have
        // to change it in all Manifests that use them as well!

        // -----------------------------------------------
        // Tags
        // -----------------------------------------------
        /** Identifier for tag action. */
        const val TAG_ACTION = "org.openintents.action.TAG"

        // -----------------------------------------------
        // Shopping
        // -----------------------------------------------
        /**
         * Change share settings for an item.
         *
         * Currently implemented for shopping list.
         */
        const val SET_SHARE_SETTINGS_ACTION = "org.openintents.action.SET_SHARE_SETTINGS"

        /**
         * Change theme settings or appearance for an item.
         *
         * Currently implemented for shopping list.
         */
        const val SET_THEME_SETTINGS_ACTION = "org.openintents.action.SET_THEME_SETTINGS"

        /**
         * Broadcasts updated information about an item or a list.
         *
         * If the list does not exist on one of the recipients, it is created. If
         * the item does not exist, it is created. This action is intended to be
         * received through GTalk or XMPP.
         */
        const val SHARE_UPDATE_ACTION = "org.openintents.action.SHARE_UPDATE"

        /**
         * Inserts an item into a shared shopping list.
         *
         * This action is intended to be received through GTalk or XMPP.
         */
        const val SHARE_INSERT_ACTION = "org.openintents.action.SHARE_INSERT"

        /** Notifies a list that the content changed. */
        const val REFRESH_ACTION = "org.openintents.action.REFRESH"

        /**
         * Adds a location alert to a specific item.
         *
         * Currently implemented for shopping list.
         */
        const val ADD_LOCATION_ALERT_ACTION = "org.openintents.action.ADD_LOCATION_ALERT"

        const val LOCATION_ALERT_DISPATCH = "org.openintents.action.LOCATION_ALERT_DISPATCH"
        const val DATE_TIME_ALERT_DISPATCH = "org.openintents.action.DATE_TIME_ALERT_DISPATCH"
        const val SERVICE_MANAGER = "org.openintents.action.SERVICE_MANAGER"

        // -----------------------------------------------
        // Categories
        // -----------------------------------------------
        /**
         * Main category specifier.
         *
         * Applications placed into this category in the AndroidManifest.xml file
         * are displayed in the main view of OpenIntents.
         */
        const val MAIN_CATEGORY = "org.openintents.category.MAIN"

        /**
         * Settings category specifier.
         *
         * Applications placed into this category in the AndroidManifest.xml file
         * are displayed in the settings tab of OpenIntents.
         */
        const val SETTINGS_CATEGORY = "org.openintents.category.SETTINGS"

        /**
         * identifier for adding generic alerts action.
         *
         * @deprecated will be removed by 0.2.1 latest
         */
        const val ADD_GENERIC_ALERT = "org.openintents.action.ADD_GENERIC_ALERT"

        /**
         * identifier for adding generic alerts action.
         *
         * @deprecated will be removed by 0.2.1 latest
         */
        const val EDIT_GENERIC_ALERT = "org.openintents.action.EDIT_GENERIC_ALERT"

        const val PREFERENCES_INIT_DEFAULT_VALUES = "InitView"
        const val PREFERENCES_DONT_SHOW_INIT_DEFAULT_VALUES = "dontShowInitDefaultValues"

        /**
         * shows an English message if open intents is not installed, finishes the
         * activity after user clicked "ok".
         */
        @JvmStatic
        fun requiresOpenIntents(activity: Activity) {
            try {
                activity.packageManager.getPackageInfo("org.openintents", 0)
            } catch (e: NameNotFoundException) {
                AlertDialog.Builder(activity)
                    .setTitle("Warning")
                    .setMessage(
                        "Requires OpenIntents! Please install the open intents application from www.openintents.org first."
                    )
                    .setPositiveButton("ok") { _, _ ->
                        activity.finish()
                    }
                    .show()
            }
        }

        /**
         * calls the InitDefaultValues activity (unless unchecked).
         */
        @JvmStatic
        fun suggestInitDefaultValues(activity: Activity) {
            val prefs = activity.getSharedPreferences(PREFERENCES_INIT_DEFAULT_VALUES, 0)
            val b = prefs.getBoolean(PREFERENCES_DONT_SHOW_INIT_DEFAULT_VALUES, false)
            if (b == false) {
                // User does not want to see intro screen again.
                val intent = Intent()
                intent.setClassName("org.openintents", "org.openintents.main.InitView")
                activity.startActivity(intent)
            }
        }
    }
}
