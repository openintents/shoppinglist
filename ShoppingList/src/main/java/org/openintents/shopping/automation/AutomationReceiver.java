package org.openintents.shopping.automation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.openintents.intents.ShoppingListIntents;
import org.openintents.shopping.LogConstants;

public class AutomationReceiver extends BroadcastReceiver {

    private final static String TAG = "AutomationReceiver";
    private final static boolean debug = false || LogConstants.debug;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (debug) {
            Log.i(TAG, "Receive intent: " + intent.toString());
        }

        final String action = intent
                .getStringExtra(ShoppingListIntents.EXTRA_ACTION);
        final String dataString = intent
                .getStringExtra(ShoppingListIntents.EXTRA_DATA);
        Uri data = null;
        if (dataString != null) {
            data = Uri.parse(dataString);
        }
        if (debug) {
            Log.i(TAG, "action: " + action + ", data: " + dataString);
        }

        if (ShoppingListIntents.TASK_CLEAN_UP_LIST.equals(action)) {
            // Clean up list.
            if (data != null) {
                if (debug) {
                    Log.i(TAG, "Clean up list " + data);
                }
                AutomationActions.cleanUpList(context, data);
            }
        }

    }

}
