package org.openintents.shopping.ui.widget;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.openintents.distribution.DownloadAppDialog;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.ItemStores;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.library.provider.ShoppingContract.Stores;
import org.openintents.shopping.R.id;
import org.openintents.shopping.R.layout;
import org.openintents.shopping.theme.ThemeAttributes;
import org.openintents.shopping.theme.ThemeShoppingList;
import org.openintents.shopping.theme.ThemeUtils;
import org.openintents.shopping.ui.PreferenceActivity;
import org.openintents.shopping.ui.dialog.EditItemDialog;
import org.openintents.shopping.library.util.PriceConverter;
import org.openintents.shopping.library.util.ShoppingUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StrikethroughSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView.OnEditorActionListener;
import android.widget.TextView;

/**
 * View to show a shopping list with its items
 * 
 */
public class StoreListView extends ListView {
	private final static String TAG = "StoreListView";
	private final static boolean debug = true;

	Typeface mCurrentTypeface = null;

	public int mPriceVisibility;
	public String mTextTypeface;
	public float mTextSize;
	public boolean mTextUpperCaseFont;
	public int mTextColor;
	public int mTextColorPrice;
	public int mTextColorChecked;
	public boolean mShowCheckBox;
	public boolean mInTextInput;
	
	public boolean mBinding = false;
	
	private boolean mTextChanged = false;
		
	final String[] mStringItems = new String[] { 
			"itemstores." + ItemStores._ID, Stores.NAME, ItemStores.STOCKS_ITEM,
			ItemStores.PRICE, ItemStores.AISLE, "stores._id as store_id"
		};
	final static int cursorColumnPRICE = 3;
	final static int cursorColumnAISLE = 4;
	
	private Cursor mCursorItemstores;
	private long mItemId;
	private long mListId;
	private ContentValues [] mBackup = null;
	private boolean mDirty = false;
	
	EditText m_lastView = null;
	int m_lastCol = 0;

	public void applyUpdate () {
		if (m_lastView == null)
			return;
		String val = m_lastView.getText().toString();
		if (m_lastCol == cursorColumnPRICE) {
			val = Long.toString(PriceConverter.getCentPriceFromString(val));
		}
		Integer row = (Integer) m_lastView.getTag();
		if (row != null) {
			Log.d(TAG, "Text changed to " + val + " @ pos " + row + ", col " + m_lastCol);
			maybeUpdate(row, m_lastCol, val);
		}
		m_lastView = null;
	}
	
	/**
	 * Extend the SimpleCursorAdapter to handle updates to the data
	 */
	public class mSimpleCursorAdapter extends SimpleCursorAdapter implements
			ViewBinder {
				
		private class EditTextWatcher implements TextWatcher, OnFocusChangeListener {

			int mCol;
			EditText mView;
			
			public EditTextWatcher (EditText v, int col) {
				Log.d(TAG, "New EditTextWatcher for " + v.toString() + 
						 " col " + col );
				mView = v;
				mCol = col;
			}
				
			@Override
			public void afterTextChanged(Editable s) {
				
				if (mBinding) 
					return; // for update purposes, doesn't count as change 
				
				if (mView != m_lastView) {
					mView.setOnFocusChangeListener(this);
					//applyUpdate();
				}
				
				m_lastView = mView;
				m_lastCol = mCol;
				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start,
					int count, int after) {
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start,
					int before, int count) {
				
			}

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (v == m_lastView && hasFocus == false) {
					mInTextInput = true;
					applyUpdate();
					mInTextInput = false;
				}
			}
	
		}
		
		/**
		 * Constructor simply calls super class.
		 * 
		 * @param context
		 *            Context.
		 * @param layout
		 *            Layout.
		 * @param c
		 *            Cursor.
		 * @param from
		 *            Projection from.
		 * @param to
		 *            Projection to.
		 */
		mSimpleCursorAdapter(final Context context, final int layout,
				final Cursor c, final String[] from, final int[] to) {
			super(context, layout, c, from, to);
			super.setViewBinder(this);

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = super.newView(context, cursor, parent);
			
			EditText v;
			v = (EditText) view.findViewById(R.id.price);
			v.addTextChangedListener(new EditTextWatcher(v, cursorColumnPRICE));
			v.setVisibility(mPriceVisibility);
			
			v = (EditText) view.findViewById(R.id.aisle);
			v.addTextChangedListener(new EditTextWatcher(v, cursorColumnAISLE));
			v.setVisibility(mPriceVisibility);
			
			return view;
		}

		/**
		 * Additionally to the standard bindView, we also check for STATUS, and
		 * strike the item through if BOUGHT.
		 */
		@Override
		public void bindView(final View view, final Context context,
				final Cursor cursor) {
			
			// set tags to null during binding, to help avoid extra db updates while binding
			EditText v;
			v = (EditText) view.findViewById(R.id.price);
			v.setTag(null);
			v = (EditText) view.findViewById(R.id.aisle);
			v.setTag(null);
			
			mBinding = true;
			super.bindView(view, context, cursor);
			mBinding = false;
			
			boolean status = cursor.getInt(2) != 0;
			final int cursorpos = cursor.getPosition();
				
			CheckBox c = (CheckBox) view.findViewById(R.id.check);

			Log.i(TAG, "bindview: pos = " + cursor.getPosition());

			// set style for check box
			c.setTag(new Integer(cursor.getPosition()));

			c.setVisibility(CheckBox.VISIBLE);
			c.setChecked(status);
			
			// The parent view knows how to deal with clicks.
			// We just pass the click through.
			// c.setClickable(false);

			c.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Log.d(TAG, "Click: ");
					toggleItemstore(cursorpos);
				}

			});
			
			v = (EditText) view.findViewById(R.id.price);
			v.setTag(new Integer(cursor.getPosition()));
			v = (EditText) view.findViewById(R.id.aisle);
			v.setTag(new Integer(cursor.getPosition()));

		}

		public boolean setViewValue(View view, Cursor cursor, int i) {
			int id = view.getId();
			if (id == R.id.price) {
				long price = cursor.getLong(cursorColumnPRICE);
				if (price != 0) {
					String text = PriceConverter.getStringFromCentPrice(price);
					((TextView)view).setText(text);
					return true;
				}
			}
			// let SimpleCursorAdapter handle the binding.
			return false;
		}

		@Override
		public void setViewBinder(ViewBinder viewBinder) {
			throw new RuntimeException("this adapter implements setViewValue");
		}

	}
	
	ContentObserver mContentObserver = new ContentObserver(new Handler()) {

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mDirty = true;
			if (mCursorItemstores != null && !mInTextInput) {
				try {
					requery();
				} catch (IllegalStateException e) {
					Log.e(TAG, "IllegalStateException ", e);
					// Somehow the logic is not completely right yet...
					mCursorItemstores = null;
				}
			}

		}

	};
	
	public StoreListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public StoreListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public StoreListView(Context context) {
		super(context);
		init();
	}

	private void init() {

	}

	public void onResume() {

		// Content observer registered at fillItems()
		// registerContentObserver();
	}

	public void onPause() {
		unregisterContentObserver();
	}

	private void backupValues() {
		int nRows = mCursorItemstores.getCount();
		if (mBackup != null)
			return;
		mBackup = new ContentValues[nRows];
		int i = 0;
		while (mCursorItemstores.moveToNext()) {
			mBackup[i] = new ContentValues();
			DatabaseUtils.cursorRowToContentValues(mCursorItemstores, mBackup[i++]);			
		}
	}
	
	public void undoChanges() {
		for (int i = 0; i < mBackup.length; i ++) {
			ContentValues cv = mBackup[i];
			String storeId = cv.getAsString("store_id");
		    
			if (cv.getAsString("_id") == null) {
				// dummy record. delete any itemstores for this item id and store id.
				// (may have been created during editing)
				ContentResolver cr = getContext().getContentResolver();
				Cursor existingItems = cr.query(ItemStores.CONTENT_URI,
						new String[] { ItemStores._ID },
						"store_id = ? AND item_id = ?",
						new String[] { storeId,	String.valueOf(mItemId) }, null);
				if (existingItems.getCount() > 0) {
					existingItems.moveToFirst();
					long id = existingItems.getLong(0);
					cr.delete(ItemStores.CONTENT_URI.buildUpon().
							appendPath(String.valueOf(id)).build(), null, null);
					existingItems.close();
				}
			} else {
				// real record, restore its values.
				// long itemstore_id = cv.getAsLong("_id");
				boolean has_item = cv.getAsBoolean("stocks_item");
				String price = cv.getAsString("price");
				String aisle = cv.getAsString("aisle");
				ShoppingUtils.addItemToStore(getContext(), mItemId, Long.parseLong(storeId), 
						has_item, aisle, price);
			}
		}
	}
	
	/**
	 * 
	 * @param activity
	 *            Activity to manage the cursor.
	 * @param listId
	 * @return
	 */
	public Cursor fillItems(Activity activity, long listId, long itemId) {

		mListId = listId;
		mItemId = itemId;
		String sortOrder = "stores.name";
		
		if (mCursorItemstores != null && !mCursorItemstores.isClosed()) {
			mCursorItemstores.close();
		}

		// Get a cursor for all stores
		mCursorItemstores = getContext().getContentResolver().query(
				ItemStores.CONTENT_URI.buildUpon().appendPath("item").appendPath(String.valueOf(mItemId)).build(), 
				mStringItems, null, null, sortOrder);
		activity.startManagingCursor(mCursorItemstores);

		registerContentObserver();

		if (mCursorItemstores == null) {
			Log.e(TAG, "missing shopping provider");
			setAdapter(new ArrayAdapter<String>(this.getContext(),
					android.R.layout.simple_list_item_1,
					new String[] { "no shopping provider" }));
			return mCursorItemstores;
		}

		backupValues();

		
		int layout_row = R.layout.list_item_store;
		mPriceVisibility = PreferenceActivity.getUsingPerStorePricesFromPrefs(getContext()) ?
				View.VISIBLE : View.INVISIBLE;

		mSimpleCursorAdapter adapter = new mSimpleCursorAdapter(this
				.getContext(),
		// Use a template that displays a text view
				layout_row,
				// Give the cursor to the list adapter
				mCursorItemstores,
				// Map the IMAGE and NAME to...
				new String[] { Stores.NAME, 
				ItemStores.PRICE, ItemStores.AISLE		
				},
				// the view defined in the XML template
				new int[] { R.id.name, R.id.price, R.id.aisle });
		setAdapter(adapter);

		return mCursorItemstores;
	}

	/**
	 * 
	 */
	private void registerContentObserver() {
		getContext().getContentResolver().registerContentObserver(
				ShoppingContract.ItemStores.CONTENT_URI, true, mContentObserver);
	}

	private void unregisterContentObserver() {
		getContext().getContentResolver().unregisterContentObserver(
				mContentObserver);
	}

	public void toggleItemstore(int position) {
		if (mCursorItemstores.getCount() <= position) {
			Log.e(TAG, "toggle inexistent item. Probably clicked too quickly?");
			return;
		}

		mCursorItemstores.moveToPosition(position);

		long oldstatus = 0; 

		// should first check if the itemstore record exists...
		String itemstore_id = null;
		
		if (mCursorItemstores.isNull(0)) {
			long storeId = mCursorItemstores.getLong(5);
			long isid = ShoppingUtils.addItemToStore(getContext(), mItemId, storeId, "", "");
			itemstore_id = Long.toString(isid);
		} else {
			itemstore_id = mCursorItemstores.getString(0);
			oldstatus = mCursorItemstores.getLong(2);
		}
		
		// Toggle status:
		long newstatus = 1 - oldstatus;
		
		ContentValues values = new ContentValues();
		values.put(ItemStores.STOCKS_ITEM, newstatus);
		Log.d(TAG, "update row " + itemstore_id + ", newstatus "
				+ newstatus);
		getContext().getContentResolver().update(
				Uri.withAppendedPath(ShoppingContract.ItemStores.CONTENT_URI,
						itemstore_id), values, null, null);

		requery();
		invalidate();
	}

	public void maybeUpdate(int position, int column, String new_val) {
		if (mCursorItemstores.getCount() <= position) {
			Log.e(TAG, "edit nonexistent item.");
			return;
		}

		mCursorItemstores.moveToPosition(position);
		String old_val = mCursorItemstores.getString(column);
		if (new_val.equals(old_val)) {
			return;
		}
		
		if (mCursorItemstores.isNull(0)) {
			long storeId = mCursorItemstores.getLong(5);
			String aisle = "";
			String price = "";
			
			if (column == 3) {
				price = new_val;
			}
			if (column == 4) {
				aisle = new_val;
			}
			ShoppingUtils.addItemToStore(getContext(), mItemId, storeId, aisle, price);
			
			/* At the corresponding points in the item view, we would requery and invalidate. However that is mainly because the editing
			 * happens in widgets outside the list view itself, where here it happens in EditTexts directly in the list. So we probably
			 * don't need to invalidate() here. Do we really need to requery()? Probably somewhere, perhaps not here. */
			//requery();
			//invalidate();
			// need to do those somewhere else.
			mDirty = true;
			return;
		}
			
		String itemstore_id = mCursorItemstores.getString(0);
		Uri uri = Uri.withAppendedPath(ItemStores.CONTENT_URI, itemstore_id);
		ContentValues cv = new ContentValues();
		cv.put(mStringItems[column], new_val);
		getContext().getContentResolver().update(uri, cv, null, null);

		// see comment above
		//requery();
		//invalidate();
		mDirty = true;

	}

	public void requery() {
		if (debug)
			Log.d(TAG, "requery()");
		mCursorItemstores.requery();
		mDirty = false;
	}
}
