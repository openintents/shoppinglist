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

package org.openintents.shopping.share

import android.app.Activity
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.Lists

/**
 * Allows to edit the share settings for a shopping list.
 */
class ListShareSettingsActivity : Activity() {
    /**
     * Cursor for access to the list.
     */
    private var mCursor: Cursor? = null

    /**
     * The EditText containing the unique shared list name.
     */
    private lateinit var mShareName: EditText

    /**
     * The EditText containing the contacts.
     */
    private lateinit var mContacts: EditText

    private lateinit var mUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list_share_settings)

        // Get the uri of the list
        mUri = intent.data!!

        // Get a cursor to access the note
        mCursor = managedQuery(mUri, mProjectionLists, null, null, null)

        // Set up click handlers for the text field and button
        mContacts = this.findViewById(R.id.contacts)
        // mText.setOnClickListener(this);
        mShareName = this.findViewById(R.id.share_name)

        // Button b = (Button) findViewById(R.id.ok);
        // b.setOnClickListener(this);

        val bOk = this.findViewById<Button>(R.id.ok)
        bOk.setOnClickListener {
            pressOK()
        }

        val bCancel = this.findViewById<Button>(R.id.cancel)
        bCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // Initialize the text with the title column from the cursor
        if (mCursor != null) {
            mCursor!!.moveToFirst()
            val sn = mCursor!!.getString(mProjectionListsSHARENAME)
            mShareName.setText(sn)
            val contacts = mCursor!!.getString(mProjectionListsSHARECONTACTS)
            mContacts.setText(contacts)
        }
    }

    override fun onPause() {
        super.onPause()

        // TODO Here we should store temporary information
    }

    fun pressOK() {
        val sharename = mShareName.text.toString()
        val contacts = mContacts.text.toString()

        if (!contacts.equals("") && sharename.equals("")) {
            mShareName.requestFocus()
            Toast.makeText(this, getString(R.string.please_enter_description),
                    Toast.LENGTH_SHORT).show()
            return
        }

        // Write the text back into the cursor
        if (mCursor != null) {
            val values = ContentValues()
            values.put(Lists.SHARE_NAME, sharename)
            values.put(Lists.SHARE_CONTACTS, contacts)
            contentResolver.update(mUri, values, "_id = ?",
                    arrayOf(mCursor!!.getString(0)))
        }

        // Broadcast the information to peers:
        // Should be done in the calling activity.
        val bundle = Bundle()
        bundle.putString(ShoppingContract.Lists.SHARE_NAME, sharename)
        bundle.putString(ShoppingContract.Lists.SHARE_CONTACTS, contacts)

        /*
         * setResult(RESULT_OK, mUri.toString(), bundle);
         */
        // TODO ??? OK???
        setResult(RESULT_OK)

        // Log.i(TAG, "call finish()");
        finish()
        // Log.i(TAG, "called finish()");

        // setResult(RESULT_OK, mUri.toString());
        Log.i(TAG, "Sending bundle: sharename: $sharename, contacts: $contacts")

        // Log.i(TAG, "Return RESULT_OK");
    }

    /*
     * public void onClick(View v) { // When the user clicks, just finish this
     * activity. // onPause will be called, and we save our data there.
     * finish(); }
     */

    companion object {
        /**
         * TAG for logging.
         */
        private const val TAG = "ListShareSettings"

        /**
         * Array of items we need to edit. This defines the projection for the table
         * Lists.
         */
        private val mProjectionLists = arrayOf(
            ShoppingContract.Lists._ID, ShoppingContract.Lists.NAME,
            ShoppingContract.Lists.SHARE_NAME,
            ShoppingContract.Lists.SHARE_CONTACTS
        )

        /**
         * Index of ID in the Projection for Lists
         */
        private const val mProjectionListsID = 0
        private const val mProjectionListsNAME = 1
        private const val mProjectionListsSHARENAME = 2
        private const val mProjectionListsSHARECONTACTS = 3
    }
}
