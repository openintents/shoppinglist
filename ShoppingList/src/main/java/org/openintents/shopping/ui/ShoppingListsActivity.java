package org.openintents.shopping.ui;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import org.openintents.intents.GeneralIntents;
import org.openintents.intents.ShoppingListIntents;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Activity to show list of shopping lists Used for INSERT_FROM_EXTRAS
 */
public class ShoppingListsActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Cursor cursor = managedQuery(ShoppingContract.Lists.CONTENT_URI,
                new String[]{Lists._ID, Lists.NAME}, null, null,
                PreferenceActivity.getShoppingListSortOrderFromPrefs(this));
        setListAdapter(new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, cursor,
                new String[]{Lists.NAME}, new int[]{android.R.id.text1}));

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (action.equals(Intent.ACTION_CREATE_SHORTCUT)) {
            setTitle(R.string.pick_list_for_shortcut);
        }
        if (action.equals(GeneralIntents.ACTION_INSERT_FROM_EXTRAS)) {
            setTitle(R.string.pick_list_to_insert_items);
        }
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type) && sharedText != null) {
            setTitle(R.string.pick_list_to_insert_items);
            // from now on handle this as an ACTION_INSERT_FROM_EXTRAS
            // for each line in the shared text, an item will be added
            intent.setAction(GeneralIntents.ACTION_INSERT_FROM_EXTRAS);
            intent.setType(ShoppingListIntents.TYPE_STRING_ARRAYLIST_SHOPPING);
            ArrayList<String> data = readSharedText(intent, sharedText);
            intent.putStringArrayListExtra("org.openintents.extra.STRING_ARRAYLIST_SHOPPING", data);
        }

    }

    private ArrayList<String> readSharedText(Intent intent, String sharedText) {
        ArrayList<String> data = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new StringReader(sharedText));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                data.add(line);
            }

            reader.close();
        } catch (IOException e) {
        }
        return data;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String action = getIntent().getAction();

        // if (getCallingActivity() != null) {
        if (Intent.ACTION_PICK.equals(action)) {
            Intent data = new Intent();
            data.setData(Uri.withAppendedPath(Lists.CONTENT_URI,
                    String.valueOf(id)));
            setResult(RESULT_OK, data);
            finish();
        } else if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            Intent data = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.withAppendedPath(Lists.CONTENT_URI,
                    String.valueOf(id));
            data.setData(uri);

            String title = getTitle(uri);

            Intent shortcut = new Intent(Intent.ACTION_CREATE_SHORTCUT);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, data);
            Intent.ShortcutIconResource sir = Intent.ShortcutIconResource
                    .fromContext(this, R.drawable.ic_launcher_shoppinglist);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, sir);

            setResult(RESULT_OK, shortcut);
            finish();
        } else if (GeneralIntents.ACTION_INSERT_FROM_EXTRAS.equals(action)) {
            // Forward the intent to the shopping activity
            Intent intent = new Intent(getIntent());

            // Add the selected list
            intent.setClass(this,
                    org.openintents.shopping.ShoppingActivity.class);
            Uri uri = Uri.withAppendedPath(Lists.CONTENT_URI,
                    String.valueOf(id));
            intent.setData(uri);

            // After the user had a chance to look at the list, return to the
            // calling activity.
            intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

            startActivity(intent);

            finish();
        }
        // }
    }

    private String getTitle(Uri uri) {
        Cursor c = getContentResolver().query(uri,
                new String[]{ShoppingContract.Lists.NAME}, null, null, null);
        if (c != null && c.moveToFirst()) {
            return c.getString(0);
        }
        if (c != null) {
            c.close();
        }

        // If there was a problem retrieving the list title
        // simply use the application name
        return getString(R.string.app_name);
    }
}
