package org.openintents.shopping.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.openintents.intents.ShoppingListIntents
import org.openintents.shopping.LogConstants

class AutomationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (debug) {
            Log.i(TAG, "Receive intent: " + intent.toString())
        }

        val action = intent.getStringExtra(ShoppingListIntents.EXTRA_ACTION)
        val dataString = intent.getStringExtra(ShoppingListIntents.EXTRA_DATA)
        var data: Uri? = null
        if (dataString != null) {
            data = Uri.parse(dataString)
        }
        if (debug) {
            Log.i(TAG, "action: $action, data: $dataString")
        }

        if (ShoppingListIntents.TASK_CLEAN_UP_LIST == action) {
            // Clean up list.
            if (data != null) {
                if (debug) {
                    Log.i(TAG, "Clean up list $data")
                }
                AutomationActions.cleanUpList(context, data)
            }
        }
    }

    companion object {
        private const val TAG = "AutomationReceiver"
        private val debug = false || LogConstants.debug
    }
}
