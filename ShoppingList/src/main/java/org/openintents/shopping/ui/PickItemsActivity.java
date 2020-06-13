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
        listItems.setPickItemsDlgMode();

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

    }

    public void onButton1Click(View view) {
        finish();
    }
}
