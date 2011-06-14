package org.openintents.shopping.ui;

import java.util.List;

import org.openintents.shopping.R;
import org.openintents.shopping.R.id;
import org.openintents.shopping.R.layout;
import org.openintents.shopping.R.string;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.shopping.ui.dialog.DialogActionListener;
import org.openintents.shopping.ui.dialog.NewListDialog;
import org.openintents.shopping.ui.dialog.RenameListDialog;
import org.openintents.shopping.ui.widget.StoreListView;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ItemStoresActivity extends Activity {

	private static final int DIALOG_NEW_STORE=1;
	
	long mListId = 0;
	StoreListView mItemStores = null;
	
	public class NewStoreDialog extends RenameListDialog {

		
		public NewStoreDialog(Context context) {
			super(context);
			
			setTitle(R.string.ask_new_store);
		}
		
		public NewStoreDialog(Context context, DialogActionListener listener) {
			super(context);
			
			setTitle(R.string.ask_new_store);
			setDialogActionListener(listener);
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_itemstores);

		mItemStores = (StoreListView) findViewById(R.id.list_stores);

		String listId;
		String itemId; 
		
		List<String> pathSegs = getIntent().getData().getPathSegments();
		int num = pathSegs.size();
		listId = pathSegs.get(num - 2);
		itemId = pathSegs.get(num - 1);
	
		mListId = Long.parseLong(listId);
		
		mItemStores.fillItems(this, Long.parseLong(listId), Long.parseLong(itemId));
		
		String itemname = ShoppingUtils.getItemName(this, Long.parseLong(itemId));
		setTitle(itemname + " @ ...");
		
		Button b = (Button) findViewById(R.id.button_ok);
		b.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				mItemStores.applyUpdate();
				finish();
			}});
		b = (Button) findViewById(R.id.button_cancel);
		b.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				mItemStores.undoChanges();
				finish();
			}});
		b = (Button) findViewById(R.id.button_add_store);
		b.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				showDialog(DIALOG_NEW_STORE);
			}});

	}

	
	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {

		case DIALOG_NEW_STORE:
			return new NewStoreDialog(this, new DialogActionListener() {

				public void onAction(String name) {
					ShoppingUtils.getStore(getApplicationContext(), name, mListId);
					mItemStores.requery();
				}
			});
		}
		return super.onCreateDialog(id);
	}
	
}
