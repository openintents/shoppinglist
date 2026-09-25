package org.openintents.shopping.ui

import android.app.ListActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import org.openintents.intents.GeneralIntents
import org.openintents.intents.ShoppingListIntents
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.Lists
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.ArrayList

/**
 * Activity to show list of shopping lists Used for INSERT_FROM_EXTRAS
 */
open class ShoppingListsActivity : ListActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursor = managedQuery(
            ShoppingContract.Lists.CONTENT_URI,
            arrayOf(Lists._ID, Lists.NAME), null, null,
            PreferenceActivity.getShoppingListSortOrderFromPrefs(this)
        )
        listAdapter = SimpleCursorAdapter(
            this,
            android.R.layout.simple_list_item_1, cursor,
            arrayOf(Lists.NAME), intArrayOf(android.R.id.text1)
        )

        val intent = intent
        val action = intent.action
        val type = intent.type
        if (action == Intent.ACTION_CREATE_SHORTCUT) {
            setTitle(R.string.pick_list_for_shortcut)
        }
        if (action == GeneralIntents.ACTION_INSERT_FROM_EXTRAS) {
            setTitle(R.string.pick_list_to_insert_items)
        }
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (Intent.ACTION_SEND == action && "text/plain" == type && sharedText != null) {
            setTitle(R.string.pick_list_to_insert_items)
            // from now on handle this as an ACTION_INSERT_FROM_EXTRAS
            // for each line in the shared text, an item will be added
            intent.action = GeneralIntents.ACTION_INSERT_FROM_EXTRAS
            intent.type = ShoppingListIntents.TYPE_STRING_ARRAYLIST_SHOPPING
            val data = readSharedText(intent, sharedText)
            intent.putStringArrayListExtra("org.openintents.extra.STRING_ARRAYLIST_SHOPPING", data)
        }
    }

    private fun readSharedText(intent: Intent, sharedText: String): ArrayList<String> {
        val data = ArrayList<String>()
        val reader = BufferedReader(StringReader(sharedText))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                data.add(line!!)
            }
            reader.close()
        } catch (e: IOException) {
        }
        return data
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val action = intent.action

        // if (getCallingActivity() != null) {
        if (Intent.ACTION_PICK == action) {
            val data = Intent()
            data.data = Uri.withAppendedPath(Lists.CONTENT_URI, id.toString())
            setResult(RESULT_OK, data)
            finish()
        } else if (Intent.ACTION_CREATE_SHORTCUT == action) {
            val data = Intent(Intent.ACTION_VIEW)
            val uri = Uri.withAppendedPath(Lists.CONTENT_URI, id.toString())
            data.data = uri

            val title = getTitle(uri)

            val shortcut = Intent(Intent.ACTION_CREATE_SHORTCUT)
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title)
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, data)
            val sir = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_shoppinglist)
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, sir)

            setResult(RESULT_OK, shortcut)
            finish()
        } else if (GeneralIntents.ACTION_INSERT_FROM_EXTRAS == action) {
            // Forward the intent to the shopping activity
            val intent = Intent(intent)

            // Add the selected list
            intent.setClass(this, org.openintents.shopping.ShoppingActivity::class.java)
            val uri = Uri.withAppendedPath(Lists.CONTENT_URI, id.toString())
            intent.data = uri

            // After the user had a chance to look at the list, return to the
            // calling activity.
            intent.flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT

            startActivity(intent)

            finish()
        }
        // }
    }

    private fun getTitle(uri: Uri): String {
        val c = contentResolver.query(
            uri,
            arrayOf(ShoppingContract.Lists.NAME), null, null, null
        )
        if (c != null && c.moveToFirst()) {
            val title = c.getString(0)
            c.close()
            return title ?: getString(R.string.app_name)
        }
        c?.close()

        // If there was a problem retrieving the list title
        // simply use the application name
        return getString(R.string.app_name)
    }
}
