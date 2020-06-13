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

package org.openintents.shopping.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.openintents.provider.Alert;
import org.openintents.provider.Location.Locations;
import org.openintents.provider.Tag;
import org.openintents.shopping.R;

/**
 * Allows to edit the share settings for a shopping list.
 */
public class AddLocationAlertActivity extends Activity implements
        OnClickListener {
    /**
     * TAG for logging.
     */
    private static final String TAG = "AddLocationAlert";

    private static final int REQUEST_PICK_LOC = 1;

    private TextView mAlertAdded;
    private TextView mTags;
    private TextView mLocation;

    private Uri mShoppingListUri;

    private Tag mTag;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mTag = new Tag(this);

        setContentView(R.layout.activity_add_location_alert);

        // Get the uri of the list
        mShoppingListUri = getIntent().getData();

        // Set up click handlers for the text field and button
        mAlertAdded = (TextView) this.findViewById(R.id.alert_added_text);
        mTags = (TextView) this.findViewById(R.id.tags);
        mLocation = (TextView) this.findViewById(R.id.location);

        Button picklocation = (Button) this.findViewById(R.id.picklocation);
        picklocation.setOnClickListener(this);

        /*
         * Button addlocationalert = (Button)
         * this.findViewById(R.id.addlocationalert);
         * addlocationalert.setOnClickListener(this);
         */

        Button viewalerts = (Button) this.findViewById(R.id.viewalerts);
        viewalerts.setOnClickListener(this);

    }

    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.picklocation:
                pickLocation();
                break;
            // case R.id.addlocationalert:
            // addLocationAlert();
            // break;
            case R.id.viewalerts:
                viewAlerts();
                break;
            default:
                // Don't know what to do - do nothing.
                Log.e(TAG, "AddLocationAlertActivity: Unexpedted view id clicked.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

        // TODO Here we should store temporary information

    }

    public void pickLocation() {
        // Call the pick location activity
        Intent intent;

        intent = new Intent(Intent.ACTION_PICK, Locations.CONTENT_URI);
        try {
            startActivityForResult(intent, REQUEST_PICK_LOC);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.locations_not_installed,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Locations not installed", e);
        }
    }

    public void addLocationAlert() {
        // Add location into database
        addAlert(mLocation.getText().toString(), null, Intent.ACTION_VIEW,
                null, mShoppingListUri.toString());
    }

    public void viewAlerts() {
        // View list of alerts

        Intent intent = new Intent(Intent.ACTION_VIEW,
                Alert.Generic.CONTENT_URI);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.alerts_not_installed,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Alerts not installed", e);
        }
    }

    // / TODO: Simply copied this routine from LocationsView.java.
    // This should be a convenience function in the alerts provider!
    private void addAlert(String locationUri, String data, String actionName,
                          String type, String uri) {

        ContentValues values = new ContentValues();
        values.put(Alert.Location.ACTIVE, Boolean.TRUE);
        values.put(Alert.Location.ACTIVATE_ON_BOOT, Boolean.TRUE);
        values.put(Alert.Location.DISTANCE, 100L);
        values.put(Alert.Location.POSITION, locationUri);
        values.put(Alert.Location.INTENT, actionName);
        values.put(Alert.Location.INTENT_URI, uri);
        // TODO convert type to uri (?) or add INTENT_MIME_TYPE column
        // getContentResolver().insert(Alert.Location.CONTENT_URI, values);
        // using alert.insert will register alerts automatically.
        Alert.insert(Alert.Location.CONTENT_URI, values);
        int textId;
        if (uri != null) {
            textId = R.string.alert_added;

            mAlertAdded.setText(getString(R.string.location_alert_added));
        } else {
            textId = R.string.alert_not_added;

            mAlertAdded.setText(getString(R.string.alert_not_added));
        }
        Toast.makeText(this, textId, Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {

                case REQUEST_PICK_LOC:
                    /*
                     * mLocation.setText(bundle.getString(Locations.EXTRA_GEO));
                     * mTags.setText(mTag.findTags(data, ", "));
                     */

                    String geo = data.getStringExtra(Locations.EXTRA_GEO);
                    mLocation.setText(geo);
                    mTags.setText(mTag.findTags(data.getDataString(), ", "));
                    addLocationAlert();
                    break;
                default:
                    break;
            }
        }
    }

}
