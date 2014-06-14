package org.openintents.shopping.ui;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.openintents.shopping.R;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

public class PickItemsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pick_items);

        final ShoppingItemsView listItems = (ShoppingItemsView) findViewById(R.id.list_items);
        listItems.mMode = ShoppingActivity.MODE_PICK_ITEMS_DLG;

        String listId = getIntent().getData().getLastPathSegment();
        listItems.fillItems(this, Long.parseLong(listId));
        // mListItems.setListTheme(ShoppingListView.MARK_CHECKBOX);
        listItems.setListTheme("1");
        // mListItems.setOnItemClickListener(new OnItemClickListener() {

        //
        // public void onItemClick(AdapterView parent, View v, int pos, long id)
        // {
        // mListItems.toggleItemRemovedFromList(pos);
        // v.invalidate();
        // }

        //
        // });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.DONUT) {
            // setting android:onClick in activity_pick_items.xml does not work
            // yet for Android 1.5
            Button b = (Button) findViewById(R.id.button1);
            b.setOnClickListener(new Button.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onButton1Click(v);
                }
            });
        }
    }

    public void onButton1Click(View view) {
        finish();
    }
}
