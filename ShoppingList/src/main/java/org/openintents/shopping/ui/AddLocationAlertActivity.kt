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

package org.openintents.shopping.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import org.openintents.provider.Alert
import org.openintents.provider.Location.Locations
import org.openintents.provider.Tag
import org.openintents.shopping.R

/**
 * Allows to edit the share settings for a shopping list.
 */
open class AddLocationAlertActivity : Activity(), OnClickListener {

    /**
     * TAG for logging.
     */
    private val mAlertAdded: TextView by lazy { findViewById(R.id.alert_added_text) }
    private val mTags: TextView by lazy { findViewById(R.id.tags) }
    private val mLocation: TextView by lazy { findViewById(R.id.location) }

    private var mShoppingListUri: Uri? = null

    private var mTag: Tag? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        mTag = Tag(this)
        Alert.init(applicationContext)

        setContentView(R.layout.activity_add_location_alert)

        // Get the uri of the list
        mShoppingListUri = intent.data

        // Set up click handlers for the text field and button
        // (views are lazily initialized above)

        val picklocation = findViewById<Button>(R.id.picklocation)
        picklocation.setOnClickListener(this)

        /*
         * Button addlocationalert = (Button)
         * this.findViewById(R.id.addlocationalert);
         * addlocationalert.setOnClickListener(this);
         */

        val viewalerts = findViewById<Button>(R.id.viewalerts)
        viewalerts.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.picklocation) {
            pickLocation()
        } else if (id == R.id.viewalerts) {
            viewAlerts()
        } else {
            // Don't know what to do - do nothing.
            Log.e(TAG, "AddLocationAlertActivity: Unexpedted view id clicked.")
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

        // TODO Here we should store temporary information
    }

    fun pickLocation() {
        // Call the pick location activity
        val intent = Intent(Intent.ACTION_PICK, Locations.CONTENT_URI)
        try {
            startActivityForResult(intent, REQUEST_PICK_LOC)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.locations_not_installed, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Locations not installed", e)
        }
    }

    fun addLocationAlert() {
        // Add location into database
        addAlert(mLocation.text.toString(), null, Intent.ACTION_VIEW, null, mShoppingListUri.toString())
    }

    fun viewAlerts() {
        // View list of alerts
        val intent = Intent(Intent.ACTION_VIEW, Alert.Generic.CONTENT_URI)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.alerts_not_installed, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Alerts not installed", e)
        }
    }

    // / TODO: Simply copied this routine from LocationsView.java.
    // This should be a convenience function in the alerts provider!
    private fun addAlert(locationUri: String?, data: String?, actionName: String?, type: String?, uri: String?) {
        val values = ContentValues()
        values.put(Alert.Location.ACTIVE, java.lang.Boolean.TRUE)
        values.put(Alert.Location.ACTIVATE_ON_BOOT, java.lang.Boolean.TRUE)
        values.put(Alert.Location.DISTANCE, 100L)
        values.put(Alert.Location.POSITION, locationUri)
        values.put(Alert.Location.INTENT, actionName)
        values.put(Alert.Location.INTENT_URI, uri)
        // TODO convert type to uri (?) or add INTENT_MIME_TYPE column
        // getContentResolver().insert(Alert.Location.CONTENT_URI, values);
        // using alert.insert will register alerts automatically.
        Alert.insert(Alert.Location.CONTENT_URI, values)
        val textId: Int
        if (uri != null) {
            textId = R.string.alert_added
            mAlertAdded.text = getString(R.string.location_alert_added)
        } else {
            textId = R.string.alert_not_added
            mAlertAdded.text = getString(R.string.alert_not_added)
        }
        Toast.makeText(this, textId, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_PICK_LOC -> {
                    /*
                     * mLocation.setText(bundle.getString(Locations.EXTRA_GEO));
                     * mTags.setText(mTag.findTags(data, ", "));
                     */
                    val geo = data!!.getStringExtra(Locations.EXTRA_GEO)
                    mLocation.text = geo
                    mTags.text = mTag!!.findTags(data.dataString!!, ", ")
                    addLocationAlert()
                }
                else -> {
                }
            }
        }
    }

    companion object {
        /**
         * TAG for logging.
         */
        private const val TAG = "AddLocationAlert"

        private const val REQUEST_PICK_LOC = 1
    }
}
