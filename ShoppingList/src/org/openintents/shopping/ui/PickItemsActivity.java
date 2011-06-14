package org.openintents.shopping.ui;

import org.openintents.shopping.R;
import org.openintents.shopping.R.id;
import org.openintents.shopping.R.layout;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class PickItemsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_pick_items);

		final ShoppingItemsView mListItems = (ShoppingItemsView) findViewById(R.id.list_items);
		mListItems.mMode = ShoppingActivity.MODE_ADD_ITEMS;

		String listId = getIntent().getData().getLastPathSegment();
		mListItems.fillItems(this, Long.parseLong(listId));
		//mListItems.setListTheme(ShoppingListView.MARK_CHECKBOX);
		mListItems.setListTheme("1");
		mListItems.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView parent, View v, int pos, long id) {
				mListItems.toggleItemRemovedFromList(pos);
				v.invalidate();
			}

		});

	}

}
