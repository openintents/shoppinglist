package org.openintents.shopping.ui;

import org.openintents.shopping.R;
import org.openintents.shopping.R.id;
import org.openintents.shopping.R.layout;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v2.os.Build;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.AdapterView.OnItemClickListener;

public class PickItemsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_pick_items);

		final ShoppingItemsView mListItems = (ShoppingItemsView) findViewById(R.id.list_items);
		mListItems.mMode = ShoppingActivity.MODE_PICK_ITEMS_DLG;

		String listId = getIntent().getData().getLastPathSegment();
		mListItems.fillItems(this, Long.parseLong(listId));
		//mListItems.setListTheme(ShoppingListView.MARK_CHECKBOX);
		mListItems.setListTheme("1");
		// mListItems.setOnItemClickListener(new OnItemClickListener() {
              // 
		// 	public void onItemClick(AdapterView parent, View v, int pos, long id) {
		// 		mListItems.toggleItemRemovedFromList(pos);
		// 		v.invalidate();
		// 	}
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
