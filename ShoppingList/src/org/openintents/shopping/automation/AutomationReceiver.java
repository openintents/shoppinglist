package org.openintents.shopping.automation;

import org.openintents.intents.ShoppingListIntents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class AutomationReceiver extends BroadcastReceiver {

	private final static String TAG = "AutomationReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "Receive intent: " + intent.toString());
		
		final String action = intent.getStringExtra(ShoppingListIntents.EXTRA_ACTION);
		final String dataString = intent.getStringExtra(ShoppingListIntents.EXTRA_DATA);
		Uri data = null;
		if (dataString != null) {
			data = Uri.parse(dataString);
		}
		Log.i(TAG, "action: " + action + ", data: " + dataString);
		
		if (ShoppingListIntents.TASK_CLEAN_UP_LIST.equals(action)) {
			// Start countdown.
			if (data != null) {
				// Launch that countdown:
				Log.i(TAG, "Launch countdown " + data);
				AutomationActions.cleanUpList(context, data);
			}
		}
		
	}

}
