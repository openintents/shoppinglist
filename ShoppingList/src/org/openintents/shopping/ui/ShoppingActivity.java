/* 
 * Copyright (C) 2007-2010 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.shopping.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.openintents.OpenIntents;
import org.openintents.distribution.DistributionLibraryFragmentActivity;
import org.openintents.intents.GeneralIntents;
import org.openintents.intents.ShoppingListIntents;
import org.openintents.provider.Alert;
import org.openintents.provider.Location.Locations;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.Items;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.library.util.PriceConverter;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.shopping.ui.dialog.DialogActionListener;
import org.openintents.shopping.ui.dialog.EditItemDialog;
import org.openintents.shopping.ui.dialog.NewListDialog;
import org.openintents.shopping.ui.dialog.RenameListDialog;
import org.openintents.shopping.ui.dialog.ThemeDialog;
import org.openintents.shopping.ui.dialog.ThemeDialog.ThemeDialogListener;
import org.openintents.shopping.ui.tablet.ShoppingListFilterFragment;
import org.openintents.shopping.ui.widget.ShoppingItemsView;
import org.openintents.shopping.ui.widget.ShoppingItemsView.DragListener;
import org.openintents.shopping.ui.widget.ShoppingItemsView.DropListener;
import org.openintents.shopping.ui.widget.ShoppingItemsView.OnCustomClickListener;
import org.openintents.util.MenuIntentOptionsWithIcons;
import org.openintents.util.ShakeSensorListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.support.v2.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v2.view.MenuCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * Displays a shopping list.
 * 
 */
public class ShoppingActivity extends DistributionLibraryFragmentActivity implements
		ThemeDialogListener, OnCustomClickListener { // implements
	// AdapterView.OnItemClickListener
	// {
	
	/**
	 * TAG for logging.
	 */
	private static final String TAG = "ShoppingActivity";
	private static final boolean debug = false;

	public class MyGestureDetector extends SimpleOnGestureListener {
        private static final float DISTANCE_DIP = 16.0f;
        private static final float PATH_DIP = 40.0f;
        // convert dip measurements to pixels
        final float scale = getResources().getDisplayMetrics().density;
        int scaledDistance = (int) (DISTANCE_DIP * scale + 0.5f);
        int scaledPath = (int) (PATH_DIP * scale + 0.5f);
        // For more information about touch gestures and screens support, see:
        // http://developer.android.com/resources/articles/gestures.html
        // http://developer.android.com/reference/android/gesture/package-summary.html
        // http://developer.android.com/guide/practices/screens_support.html try {

        @Override
	        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        	if (e1 == null || e2 == null) return false;

        	 try {
        		 DisplayMetrics dm = getResources().getDisplayMetrics();

        		 int REL_SWIPE_MIN_DISTANCE = (int)(SWIPE_MIN_DISTANCE * dm.densityDpi / 160.0f);
        		 int REL_SWIPE_MAX_OFF_PATH = (int)(SWIPE_MAX_OFF_PATH * dm.densityDpi / 160.0f);
        		 int REL_SWIPE_THRESHOLD_VELOCITY = (int)(SWIPE_THRESHOLD_VELOCITY * dm.densityDpi / 160.0f);


	                if (Math.abs(e1.getY() - e2.getY()) > REL_SWIPE_MAX_OFF_PATH)
	                    return false;
	                // right to left swipe
	                if(e1.getX() - e2.getX() > REL_SWIPE_MIN_DISTANCE && Math.abs(velocityX) > REL_SWIPE_THRESHOLD_VELOCITY) {
	                    Toast.makeText(ShoppingActivity.this, "Left Swipe", Toast.LENGTH_SHORT).show();
	                    changeList(-1);
	                }  else if (e2.getX() - e1.getX() > REL_SWIPE_MIN_DISTANCE && Math.abs(velocityX) > REL_SWIPE_THRESHOLD_VELOCITY) {
	                    Toast.makeText(ShoppingActivity.this, "Right Swipe", Toast.LENGTH_SHORT).show();
	                    changeList(1);
	                }
	            } catch (Exception e) {
	                // nothing
	            }
	            return false;
	        }

	}


	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	private static final int MENU_NEW_LIST = Menu.FIRST;
	private static final int MENU_CLEAN_UP_LIST = Menu.FIRST + 1;
	private static final int MENU_DELETE_LIST = Menu.FIRST + 2;

	private static final int MENU_SHARE = Menu.FIRST + 3;
	private static final int MENU_THEME = Menu.FIRST + 4;

	private static final int MENU_ADD_LOCATION_ALERT = Menu.FIRST + 5;

	private static final int MENU_RENAME_LIST = Menu.FIRST + 6;

	private static final int MENU_MARK_ITEM = Menu.FIRST + 7;
	private static final int MENU_EDIT_ITEM = Menu.FIRST + 8; // includes rename
	private static final int MENU_DELETE_ITEM = Menu.FIRST + 9;

	private static final int MENU_INSERT_FROM_EXTRAS = Menu.FIRST + 10; // insert
																		// from
																		// string
																		// array
																		// in
																		// intent
																		// extras

	// TODO: Implement the following menu items
	// private static final int MENU_EDIT_LIST = Menu.FIRST + 12; // includes
	// rename
	// private static final int MENU_SORT = Menu.FIRST + 13; // sort
	// alphabetically
	// or modified
	private static final int MENU_PICK_ITEMS = Menu.FIRST + 14; // pick from
	// previously
	// used items

	// TODO: Implement "select list" action
	// that can be called by other programs.
	// private static final int MENU_SELECT_LIST = Menu.FIRST + 15; // select a
	// shopping list
	private static final int MENU_PREFERENCES = Menu.FIRST + 17;
	private static final int MENU_SEND = Menu.FIRST + 18;
	private static final int MENU_REMOVE_ITEM_FROM_LIST = Menu.FIRST + 19;
	private static final int MENU_MOVE_ITEM = Menu.FIRST + 20;
	private static final int MENU_MARK_ALL_ITEMS = Menu.FIRST + 21;
	private static final int MENU_ITEM_STORES = Menu.FIRST + 22;

	private static final int MENU_DISTRIBUTION_START = Menu.FIRST + 100; // MUST
																			// BE
																			// LAST

	private static final int DIALOG_ABOUT = 1;
	// private static final int DIALOG_TEXT_ENTRY = 2;
	private static final int DIALOG_NEW_LIST = 2;
	private static final int DIALOG_RENAME_LIST = 3;
	private static final int DIALOG_EDIT_ITEM = 4;
	private static final int DIALOG_DELETE_ITEM = 5;
	private static final int DIALOG_THEME = 6;

	private static final int DIALOG_DISTRIBUTION_START = 100; // MUST BE LAST

	private static final int REQUEST_CODE_CATEGORY_ALTERNATIVE = 1;
	private static final int REQUEST_PICK_LIST = 2;

	/**
	 * The main activity.
	 * 
	 * Displays the shopping list that was used last time.
	 */
	private static final int STATE_MAIN = 0;

	/**
	 * VIEW action on a item/list URI.
	 */
	private static final int STATE_VIEW_LIST = 1;

	/**
	 * PICK action on an dir/item URI.
	 */
	private static final int STATE_PICK_ITEM = 2;

	/**
	 * GET_CONTENT action on an item/item URI.
	 */
	private static final int STATE_GET_CONTENT_ITEM = 3;

	/**
	 * Current state
	 */
	private int mState;

	/**
	 * mode: separate dialog to add items from existing list
	 */
	public static final int MODE_PICK_ITEMS_DLG = 3;

	
	/**
	 * mode: add items from existing list
	 */
	public static final int MODE_ADD_ITEMS = 2;

	/**
	 * mode: I am in the shop
	 */
	public static final int MODE_IN_SHOP = 1;

	/**
	 * URI of current list
	 */
	private Uri mListUri;

	/**
	 * URI of selected item
	 */
	private Uri mItemUri;

	/**
	 * Definition of the requestCode for the subactivity.
	 */
	static final private int SUBACTIVITY_LIST_SHARE_SETTINGS = 0;

	/**
	 * Definition for message handler:
	 */
	static final private int MESSAGE_UPDATE_CURSORS = 1;

	/**
	 * Update interval for automatic requires.
	 * 
	 * (Workaround since ContentObserver does not work.)
	 */
	private int mUpdateInterval;

	private boolean mUpdating;

	/**
	 * The items to add to the shopping list.
	 * 
	 * Received as a string array list in the intent extras.
	 */
	private List<String> mExtraItems;

	/**
	 * The quantities for items to add to the shopping list.
	 * 
	 * Received as a string array list in the intent extras.
	 */
	private List<String> mExtraQuantities;

	/**
	 * The prices for items to add to the shopping list.
	 * 
	 * Received as a string array list in the intent extras.
	 */
	private List<String> mExtraPrices;

	/**
	 * The barcodes for items to add to the shopping list.
	 * 
	 * Received as a string array list in the intent extras.
	 */
	private List<String> mExtraBarcodes;

	/**
	 * The list URI received together with intent extras.
	 */
	private Uri mExtraListUri;

	/**
	 * Private members connected to list of shopping lists
	 */
	//	Temp - making it generic for tablet compatibility
	private AdapterView mShoppingListsView;
	private Cursor mCursorShoppingLists;
	private static final String[] mStringListFilter = new String[] { Lists._ID,
			Lists.NAME, Lists.IMAGE, Lists.SHARE_NAME, Lists.SHARE_CONTACTS,
			Lists.SKIN_BACKGROUND };
	private static final int mStringListFilterID = 0;
	private static final int mStringListFilterNAME = 1;
	private static final int mStringListFilterIMAGE = 2;
	private static final int mStringListFilterSHARENAME = 3;
	private static final int mStringListFilterSHARECONTACTS = 4;
	private static final int mStringListFilterSKINBACKGROUND = 5;

	private ShoppingItemsView mItemsView;
	// private Cursor mCursorItems;

	public static final String[] mStringItems = new String[] { ContainsFull._ID,
			ContainsFull.ITEM_NAME, ContainsFull.ITEM_IMAGE,
			ContainsFull.ITEM_TAGS, ContainsFull.ITEM_PRICE,
			ContainsFull.QUANTITY, ContainsFull.STATUS, ContainsFull.ITEM_ID,
			ContainsFull.SHARE_CREATED_BY, ContainsFull.SHARE_MODIFIED_BY,
			ContainsFull.PRIORITY, ContainsFull.ITEM_HAS_NOTE,
			ContainsFull.ITEM_UNITS };
	static final int mStringItemsCONTAINSID = 0;
	public static final int mStringItemsITEMNAME = 1;
	static final int mStringItemsITEMIMAGE = 2;
	public static final int mStringItemsITEMTAGS = 3;
	public static final int mStringItemsITEMPRICE = 4;
	public static final int mStringItemsQUANTITY = 5;
	public static final int mStringItemsSTATUS = 6;
	public static final int mStringItemsITEMID = 7;
	private static final int mStringItemsSHARECREATEDBY = 8;
	private static final int mStringItemsSHAREMODIFIEDBY = 9;
	public static final int mStringItemsPRIORITY = 10;
	public static final int mStringItemsITEMHASNOTE = 11;
	public static final int mStringItemsITEMUNITS = 12;

	private LinearLayout.LayoutParams mLayoutParamsItems;
	private int mAllowedListHeight; // Height for the list allowed in this view.

	private AutoCompleteTextView mEditText;

	protected Context mDialogContext;

	// TODO: Set up state information for onFreeze(), ...
	// State data to be stored when freezing:
	private final String ORIGINAL_ITEM = "original item";

	// private static final String BUNDLE_TEXT_ENTRY_MENU = "text entry menu";
	// private static final String BUNDLE_CURSOR_ITEMS_POSITION =
	// "cursor items position";
	private static final String BUNDLE_ITEM_URI = "item uri";
	private static final String BUNDLE_RELATION_URI = "relation_uri";

	// Skins --------------------------

	// private int mTextEntryMenu;
	/*
	 * NOTE: mItemsCursor is used for autocomplete Textview, mCursorItems is for
	 * items in list
	 */
	private Cursor mItemsCursor;

	/**
	 * Remember position for screen orientation change.
	 */
	// int mEditItemPosition = -1;

	// public int mPriceVisibility;
	// private int mTagsVisibility;
	private SensorManager mSensorManager;
	private SensorListener mMySensorListener = new ShakeSensorListener() {

		@Override
		public void onShake() {
			// Provide some visual feedback.
			Animation shake = AnimationUtils.loadAnimation(
					ShoppingActivity.this, R.anim.shake);
			findViewById(R.id.background).startAnimation(shake);

			cleanupList();
		}

	};

	/**
	 * isActive is true only after onResume() and before onPause().
	 */
	private boolean mIsActive = false;

	/**
	 * Whether to use the sensor for shake.
	 */
	private boolean mUseSensor = false;
	private Uri mRelationUri;
	private int mMoveItemPosition;

	private EditItemDialog.FieldType mEditItemFocusField = EditItemDialog.FieldType.ITEMNAME;
	private GestureDetector mGestureDetector;
	private View.OnTouchListener mGestureListener;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (debug)
			Log.d(TAG, "Shopping list onCreate()");

		mDistribution.setFirst(MENU_DISTRIBUTION_START,
				DIALOG_DISTRIBUTION_START);

		// Check whether EULA has been accepted
		// or information about new version can be presented.
		if (mDistribution.showEulaOrNewVersion()) {
			return;
		}

		setContentView(R.layout.activity_shopping);

		// mEditItemPosition = -1;

		// Automatic requeries (once a second)
		mUpdateInterval = 2000;
		mUpdating = false;

		// General Uris:
		mListUri = ShoppingContract.Lists.CONTENT_URI;
		mItemUri = ShoppingContract.Items.CONTENT_URI;

		int defaultShoppingList = initFromPreferences();

		// Handle the calling intent
		final Intent intent = getIntent();
		final String type = intent.resolveType(this);
		final String action = intent.getAction();

		if (action == null) {
			// Main action
			mState = STATE_MAIN;

			mListUri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI, ""
					+ defaultShoppingList);

			intent.setData(mListUri);
		} else if (Intent.ACTION_MAIN.equals(action)) {
			// Main action
			mState = STATE_MAIN;

			mListUri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI, ""
					+ defaultShoppingList);

			intent.setData(mListUri);

		} else if (Intent.ACTION_VIEW.equals(action)) {
			mState = STATE_VIEW_LIST;

			if (ShoppingContract.ITEM_TYPE.equals(type)) {
				mListUri = ShoppingUtils.getListForItem(this, intent.getData()
						.getLastPathSegment());
			} else if (intent.getData() != null) {
				mListUri = intent.getData();
			}
		} else if (Intent.ACTION_INSERT.equals(action)) {
			// TODO: insert items from extras ????
			mState = STATE_VIEW_LIST;

			if (ShoppingContract.ITEM_TYPE.equals(type)) {
				mListUri = ShoppingUtils.getListForItem(
						getApplicationContext(), intent.getData()
								.getLastPathSegment());
			} else if (intent.getData() != null) {
				mListUri = intent.getData();
			}

		} else if (Intent.ACTION_PICK.equals(action)) {
			mState = STATE_PICK_ITEM;

			mListUri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI, ""
					+ defaultShoppingList);
		} else if (Intent.ACTION_GET_CONTENT.equals(action)) {
			mState = STATE_GET_CONTENT_ITEM;

			mListUri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI, ""
					+ defaultShoppingList);
		} else if (GeneralIntents.ACTION_INSERT_FROM_EXTRAS.equals(action)) {
			if (ShoppingListIntents.TYPE_STRING_ARRAYLIST_SHOPPING.equals(type)) {
				/*
				 * Need to insert new items from a string array in the intent
				 * extras Use main action but add an item to the options menu
				 * for adding extra items
				 */
				getShoppingExtras(intent);
				mState = STATE_MAIN;
				mListUri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI, ""
						+ defaultShoppingList);
				intent.setData(mListUri);
			} else if (intent.getDataString().startsWith(
					ShoppingContract.Lists.CONTENT_URI.toString())) {
				// Somewhat quick fix to pass data from ShoppingListsActivity to
				// this activity.

				// We received a valid shopping list URI:
				mListUri = intent.getData();

				getShoppingExtras(intent);
				mState = STATE_MAIN;
				intent.setData(mListUri);
			}
		} else {
			// Unknown action.
			Log.e(TAG, "Shopping: Unknown action, exiting");
			finish();
			return;
		}

		// hook up all buttons, lists, edit text:
		createView();

		// populate the lists
		fillListFilter();

		// Get last part of URI:
		int selectList;
		try {
			selectList = Integer.parseInt(mListUri.getLastPathSegment());
		} catch (NumberFormatException e) {
			selectList = defaultShoppingList;
		}

		// select the default shopping list at the beginning:
		setSelectedListId(selectList);

		if (icicle != null) {
			String prevText = icicle.getString(ORIGINAL_ITEM);
			if (prevText != null) {
				mEditText.setTextKeepState(prevText);
			}
			// mTextEntryMenu = icicle.getInt(BUNDLE_TEXT_ENTRY_MENU);
			// mEditItemPosition = icicle.getInt(BUNDLE_CURSOR_ITEMS_POSITION);
			mItemUri = Uri.parse(icicle.getString(BUNDLE_ITEM_URI));
			if (icicle.containsKey(BUNDLE_RELATION_URI)) {
				mRelationUri = Uri.parse(icicle.getString(BUNDLE_RELATION_URI));
			}
		}

		// set focus to the edit line:
		mEditText.requestFocus();

		// TODO remove initFromPreferences from onCreate
		// we need it in resume to update after settings have changed
		initFromPreferences();
		// now update title and fill all items
		onModeChanged();
	}

	private int initFromPreferences() {
		// if set to "last used", override the default list.
		SharedPreferences sp = getSharedPreferences(
				"org.openintents.shopping_preferences", MODE_PRIVATE);
		final boolean loadLastUsed = sp.getBoolean(
				PreferenceActivity.PREFS_LOADLASTUSED,
				PreferenceActivity.PREFS_LOADLASTUSED_DEFAULT);

		Log.e(TAG, "load last used ?" + loadLastUsed);
		int defaultShoppingList = 1;
		if (loadLastUsed) {
			defaultShoppingList = sp.getInt(PreferenceActivity.PREFS_LASTUSED,
					1);
			if (mItemsView != null) {
				// UGLY WORKAROUND:
				// On screen orientation changes, fillItems() is called twice.
				// That is why we have to set the list position twice.
				mItemsView.mUpdateLastListPosition = 2;
				
				mItemsView.mLastListPosition = sp.getInt(PreferenceActivity.PREFS_LASTLIST_POSITION, 0);
				mItemsView.mLastListTop = sp.getInt(PreferenceActivity.PREFS_LASTLIST_TOP, 0);

				if (debug) Log.d(TAG, "Load list position: pos: " + mItemsView.mLastListPosition
						+ ", top: " + mItemsView.mLastListTop);
			}
		} else {
			defaultShoppingList = (int) ShoppingUtils.getDefaultList(this);
		}

		if (mItemsView != null) {
			if (sp.getBoolean(PreferenceActivity.PREFS_SHOW_PRICE,
					PreferenceActivity.PREFS_SHOW_PRICE_DEFAULT)) {
				mItemsView.mPriceVisibility = View.VISIBLE;
			} else {
				mItemsView.mPriceVisibility = View.GONE;
			}

			if (sp.getBoolean(PreferenceActivity.PREFS_SHOW_TAGS,
					PreferenceActivity.PREFS_SHOW_TAGS_DEFAULT)) {
				mItemsView.mTagsVisibility = View.VISIBLE;
			} else {
				mItemsView.mTagsVisibility = View.GONE;
			}
			if (sp.getBoolean(PreferenceActivity.PREFS_SHOW_QUANTITY,
					PreferenceActivity.PREFS_SHOW_QUANTITY_DEFAULT)) {
				mItemsView.mQuantityVisibility = View.VISIBLE;
			} else {
				mItemsView.mQuantityVisibility = View.GONE;
			}
			if (sp.getBoolean(PreferenceActivity.PREFS_SHOW_UNITS,
					PreferenceActivity.PREFS_SHOW_UNITS_DEFAULT)) {
				mItemsView.mUnitsVisibility = View.VISIBLE;
			} else {
				mItemsView.mUnitsVisibility = View.GONE;
			}
			if (sp.getBoolean(PreferenceActivity.PREFS_SHOW_PRIORITY,
					PreferenceActivity.PREFS_SHOW_PRIORITY_DEFAULT)) {
				mItemsView.mPriorityVisibility = View.VISIBLE;
			} else {
				mItemsView.mPriorityVisibility = View.GONE;
			}
		}

		mUseSensor = sp.getBoolean(PreferenceActivity.PREFS_SHAKE,
				PreferenceActivity.PREFS_SHAKE_DEFAULT);

		return defaultShoppingList;
	}

	private void registerSensor() {
		if (!mUseSensor) {
			// Don't use sensors
			return;
		}

		if (mItemsView.mMode == MODE_IN_SHOP) {
			if (mSensorManager == null) {
				mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			}
			mSensorManager.registerListener(mMySensorListener,
					SensorManager.SENSOR_ACCELEROMETER,
					SensorManager.SENSOR_DELAY_UI);
		}

	}

	private void unregisterSensor() {
		if (mSensorManager != null) {
			mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			mSensorManager.unregisterListener(mMySensorListener);
		}
	}

	@Override
	protected void onResume() {
		if (debug)
			Log.i(TAG, "Shopping list onResume() 1");
		super.onResume();
		if (debug)
			Log.i(TAG, "Shopping list onResume() 2");

		// Reload preferences, in case something changed
		initFromPreferences();
		
		mIsActive = true;

		// Modify our overall title depending on the mode we are running in.
		if (mState == STATE_MAIN || mState == STATE_VIEW_LIST) {
			// App name is default
			// setTitle(getText(R.string.app_name));
		} else if ((mState == STATE_PICK_ITEM)
				|| (mState == STATE_GET_CONTENT_ITEM)) {
			setTitle(getText(R.string.pick_item));
			setTitleColor(0xFFAAAAFF);
		}

		mItemsView.setListTheme(loadListTheme());
		mItemsView.onResume();

		// TODO fling disabled for release 1.3.0
//		mGestureDetector = new GestureDetector(new MyGestureDetector());
//		mGestureListener = new OnTouchListener() {
//			public boolean onTouch(View view, MotionEvent e) {
//				if (mGestureDetector.onTouchEvent(e)) {
//                    return true;
//                }
//				return false;
//			}
//		};
//		mListItemsView.setOnTouchListener(mGestureListener);
				
		mEditText
				.setKeyListener(PreferenceActivity
						.getCapitalizationKeyListenerFromPrefs(getApplicationContext()));

		if (!mUpdating) {
			mUpdating = true;
			// mHandler.sendMessageDelayed(mHandler.obtainMessage(
			// MESSAGE_UPDATE_CURSORS), mUpdateInterval);
		}

		// fillItems();

		// TODO ???
		/*
		 * // Register intent receiver for refresh intents: IntentFilter
		 * intentfilter = new IntentFilter(OpenIntents.REFRESH_ACTION);
		 * registerReceiver(mIntentReceiver, intentfilter);
		 */

		// Items received through intents are added in
		// fillItems().

		registerSensor();

		if (debug)
			Log.i(TAG, "Shopping list onResume() finished");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (debug)
			Log.i(TAG, "Shopping list onPause()");
		if (debug)
			Log.i(TAG, "Spinner: onPause: " + mIsActive);
		mIsActive = false;
		if (debug)
			Log.i(TAG, "Spinner: onPause: " + mIsActive);

		unregisterSensor();

		// Save position and pixel position of first visible item
		// of current shopping list
		int listposition = mItemsView.getFirstVisiblePosition();
		View v = mItemsView.getChildAt(0);
		int listtop = (v == null) ? 0 : v.getTop();
		if (debug) Log.d(TAG, "Save list position: pos: " + listposition
				+ ", top: " + listtop);
		
		SharedPreferences sp = getSharedPreferences(
				"org.openintents.shopping_preferences", MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putInt(PreferenceActivity.PREFS_LASTUSED, new Long(
				getSelectedListId()).intValue());
		editor.putInt(PreferenceActivity.PREFS_LASTLIST_POSITION, listposition);
		editor.putInt(PreferenceActivity.PREFS_LASTLIST_TOP, listtop);
		editor.commit();
		// TODO ???
		/*
		 * // Unregister refresh intent receiver
		 * unregisterReceiver(mIntentReceiver);
		 * 
		 */

		mItemsView.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (debug)
			Log.i(TAG, "Shopping list onSaveInstanceState()");

		// Save original text from edit box
		String s = mEditText.getText().toString();
		outState.putString(ORIGINAL_ITEM, s);

		outState.putString(BUNDLE_ITEM_URI, mItemUri.toString());
		if (mRelationUri != null) {
			outState.putString(BUNDLE_RELATION_URI, mRelationUri.toString());
		}

		mUpdating = false;

		// after items have been added through an "insert from extras" the
		// action name should be different to avoid duplicate inserts e.g. on
		// rotation.
		if (mExtraItems == null
				&& GeneralIntents.ACTION_INSERT_FROM_EXTRAS.equals(getIntent()
						.getAction())) {
			setIntent(getIntent().setAction(Intent.ACTION_VIEW));
		}
	}

	/**
	 * Hook up buttons, lists, and edittext with functionality.
	 */
	private void createView() {

		//Temp-create either Spinner or List based upon the Display
		createList();

		mEditText = (AutoCompleteTextView) findViewById(R.id.autocomplete_add_item);
		if (mItemsCursor != null) {
			if (debug)
				Log.d(TAG, "mItemsCursor managedQuery 1");
			stopManagingCursor(mItemsCursor);
			mItemsCursor.close();
			mItemsCursor = null;
		}
		mItemsCursor = managedQuery(Items.CONTENT_URI, new String[] {
				Items._ID, Items.NAME }, null, null, "name desc");
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_dropdown_item_1line, mItemsCursor,
				new String[] { Items.NAME }, new int[] { android.R.id.text1 });
		adapter.setStringConversionColumn(1);
		adapter.setFilterQueryProvider(new FilterQueryProvider() {
			public Cursor runQuery(CharSequence constraint) {
				if (mItemsCursor != null) {
					if (debug)
						Log.d(TAG, "mItemsCursor managedQuery 2");
					stopManagingCursor(mItemsCursor);

					// For some reason, closing the cursor seems to post
					// an invalidation on the background thread and a crash...
					// so we keep it open.
					// mItemsCursor.close();
					// mItemsCursor = null;
				}
				mItemsCursor = managedQuery(Items.CONTENT_URI, new String[] {
						Items._ID, Items.NAME }, "upper(name) like ?",
						new String[] { "%"
								+ (constraint == null ? "" : constraint
										.toString().toUpperCase()) + "%" },
						"name desc");
				return mItemsCursor;
			}

		});
		mEditText.setAdapter(adapter);
		mEditText.setOnKeyListener(new OnKeyListener() {

			private int mLastKeyAction = KeyEvent.ACTION_UP;

			public boolean onKey(View v, int keyCode, KeyEvent key) {
				// Log.i(TAG, "KeyCode: " + keyCode
				// + " =?= "
				// +Integer.parseInt(getString(R.string.key_return)) );

				// Shortcut: Instead of pressing the button,
				// one can also press the "Enter" key.
				if (debug)
					Log.i(TAG, "Key action: " + key.getAction());
				if (debug)
					Log.i(TAG, "Key code: " + keyCode);
				if (keyCode == KeyEvent.KEYCODE_ENTER) {

					if (mEditText.isPopupShowing()) {
						mEditText.performCompletion();
					}

					// long key press might cause call of duplicate onKey events
					// with ACTION_DOWN
					// this would result in inserting an item and showing the
					// pick list

					if (key.getAction() == KeyEvent.ACTION_DOWN
							&& mLastKeyAction == KeyEvent.ACTION_UP) {
						insertNewItem();
					}

					mLastKeyAction = key.getAction();
					return true;
				}
				;
				return false;
			}
		});

		Button button = (Button) findViewById(R.id.button_add_item);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				insertNewItem();
			}
		});
		button.setOnLongClickListener(new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				Intent intent = new Intent();				
				intent.setData(mListUri);
				intent.setClassName("org.openintents.barcodescanner", "org.openintents.barcodescanner.BarcodeScanner");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
				try {
					startActivity(intent);
				} catch (ActivityNotFoundException e) {
					Log.v(TAG, "barcode scanner not found");
					return false;
				}
				return true;
			}
		});

		mLayoutParamsItems = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);

		mItemsView = (ShoppingItemsView) findViewById(R.id.list_items);
		mItemsView.setThemedBackground(findViewById(R.id.background));
		mItemsView.setCustomClickListener(this);

		mItemsView.setItemsCanFocus(true);
		mItemsView.setDragListener(new DragListener() {

			@Override
			public void drag(int from, int to) {
				Log.v("DRAG", "" + from + "/" + to);

			}
		});
		mItemsView.setDropListener(new DropListener() {

			@Override
			public void drop(int from, int to) {
				Log.v("DRAG", "" + from + "/" + to);

			}
		});

		TextView tv = (TextView) findViewById(R.id.total_1);
		mItemsView.setTotalCheckedTextView(tv);

		tv = (TextView) findViewById(R.id.total_2);
		mItemsView.setTotalTextView(tv);

		tv = (TextView) findViewById(R.id.total_3);
		mItemsView.setPrioritySubtotalTextView(tv);
		
		tv = (TextView) findViewById(R.id.count);
		mItemsView.setCountTextView(tv);

		mItemsView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView parent, View v, int pos, long id) {
				Cursor c = (Cursor) parent.getItemAtPosition(pos);
				onCustomClick(c, pos, EditItemDialog.FieldType.ITEMNAME);
				// DO NOT CLOSE THIS CURSOR - IT IS A MANAGED ONE.
				// ---- c.close();
			}

		});

		mItemsView
				.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

					public void onCreateContextMenu(ContextMenu contextmenu,
							View view, ContextMenuInfo info) {
						contextmenu.add(0, MENU_EDIT_ITEM, 0,
								R.string.menu_edit_item).setShortcut('1', 'e');
						contextmenu.add(0, MENU_MARK_ITEM, 0,
								R.string.menu_mark_item).setShortcut('2', 'm');
						contextmenu.add(0, MENU_ITEM_STORES, 0,
								R.string.menu_item_stores).setShortcut('3', 's');
						contextmenu.add(0, MENU_REMOVE_ITEM_FROM_LIST, 0,
								R.string.menu_remove_item)
								.setShortcut('3', 'r');
						contextmenu.add(0, MENU_DELETE_ITEM, 0,
								R.string.menu_delete_item)
								.setShortcut('4', 'd');
						contextmenu.add(0, MENU_MOVE_ITEM, 0,
								R.string.menu_move_item).setShortcut('5', 'l');
					}

				});
	}

	private void createList() {

		// TODO switch layout on screen size, not sdk versions
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
			
			mShoppingListsView = (ListView) findViewById(android.R.id.list);
			((ListView)mShoppingListsView)
			.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected(AdapterView parent, View v,
						int position, long id) {
					if (debug)
						Log.d(TAG, "ListView: onItemSelected");
					if (id != mItemsView.getListId()) {
						// No need to requery... same list we had before. 
						fillItems();
						// Now set the theme based on the selected list:
						mItemsView.setListTheme(loadListTheme());
					}

					((ListView) mShoppingListsView).setItemChecked(position,true);
					
				}

				public void onNothingSelected(AdapterView arg0) {
					if (debug)
						Log.d(TAG, "Listview: onNothingSelected: "
								+ mIsActive);
					if (mIsActive) {
						fillItems();
					}
				}
			});

			
			
			


		}else{
			mShoppingListsView = (Spinner) findViewById(R.id.spinner_listfilter);
			((Spinner)mShoppingListsView)
			.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected(AdapterView parent, View v,
						int position, long id) {
					if (debug)
						Log.d(TAG, "Spinner: onItemSelected");
					if (id != mItemsView.getListId()) {
						// No need to requery... same list we had before. 
						fillItems();
						// Now set the theme based on the selected list:
						mItemsView.setListTheme(loadListTheme());
					}					
				}

				public void onNothingSelected(AdapterView arg0) {
					if (debug)
						Log.d(TAG, "Spinner: onNothingSelected: "
								+ mIsActive);
					if (mIsActive) {
						fillItems();
					}
				}
			});
		}
	}

	public void onCustomClick(Cursor c, int pos, EditItemDialog.FieldType field) {
		if (mState == STATE_PICK_ITEM) {
			pickItem(c);
		} else {
			if (mItemsView.mShowCheckBox) {
				// In default theme, there is an extra check box,
				// so clicking on anywhere else means to edit the
				// item.
				
				if (field == EditItemDialog.FieldType.PRICE &&
					PreferenceActivity.getUsingPerStorePricesFromPrefs(this))
					// should really be a per-list preference
				{
					editItemStores(pos);
				}
				else
					editItem(pos, field);
			} else {
				// For themes without a checkbox, clicking anywhere means
				// to toggle the item.
				mItemsView.toggleItemBought(pos);
			}
		}
	}

	/**
	 * Inserts new item from edit box into currently selected shopping list.
	 */
	private void insertNewItem() {
		String newItem = mEditText.getText().toString();

		// Only add if there is something to add:
		if (newItem.compareTo("") != 0) {
			long listId = getSelectedListId();
			if (listId < 0) {
				// No valid list - probably view is not active
				// and no item is selected.
				return;
			}

			mItemsView.insertNewItem(this, newItem, null, null, null, null);
			mEditText.setText("");
		} else {
			// Open list to select item from
			pickItems();
		}
	}

	/**
	 * Obtain items from extras.
	 */
	private void getShoppingExtras(final Intent intent) {
		mExtraItems = intent.getExtras().getStringArrayList(
				ShoppingListIntents.EXTRA_STRING_ARRAYLIST_SHOPPING);
		mExtraQuantities = intent.getExtras().getStringArrayList(
				ShoppingListIntents.EXTRA_STRING_ARRAYLIST_QUANTITY);
		mExtraPrices = intent.getExtras().getStringArrayList(
				ShoppingListIntents.EXTRA_STRING_ARRAYLIST_PRICE);
		mExtraBarcodes = intent.getExtras().getStringArrayList(
				ShoppingListIntents.EXTRA_STRING_ARRAYLIST_BARCODE);

		mExtraListUri = null;
		if ((intent.getDataString() != null)
				&& (intent.getDataString()
						.startsWith(ShoppingContract.Lists.CONTENT_URI.toString()))) {
			// We received a valid shopping list URI.

			// Set current list to received list:
			mExtraListUri = intent.getData();
			if (debug)
				Log.d(TAG, "Received extras for " + mExtraListUri.toString());
		}
	}

	/**
	 * Inserts new item from string array received in intent extras.
	 */
	private void insertItemsFromExtras() {
		if (mExtraItems != null) {
			// Make sure we are in the correct list:
			if (mExtraListUri != null) {
				long listId = Long
						.parseLong(mExtraListUri.getLastPathSegment());
				if (debug)
					Log.d(TAG, "insert items into list " + listId);
				if (listId != getSelectedListId()) {
					if (debug)
						Log.d(TAG, "set new list: " + listId);
					setSelectedListId((int) listId);
					mItemsView.fillItems(this, listId);
				}
			}

			int max = mExtraItems.size();
			int maxQuantity = (mExtraQuantities != null) ? mExtraQuantities
					.size() : -1;
			int maxPrice = (mExtraPrices != null) ? mExtraPrices.size() : -1;
			int maxBarcode = (mExtraBarcodes != null) ? mExtraBarcodes.size()
					: -1;
			for (int i = 0; i < max; i++) {
				String item = mExtraItems.get(i);
				String quantity = (i < maxQuantity) ? mExtraQuantities.get(i)
						: null;
				String price = (i < maxPrice) ? mExtraPrices.get(i) : null;
				String barcode = (i < maxBarcode) ? mExtraBarcodes.get(i)
						: null;
				if (debug)
					Log.d(TAG, "Add item: " + item + ", quantity: " + quantity
							+ ", price: " + price + ", barcode: " + barcode);
				mItemsView.insertNewItem(this, item, quantity, null, price,
						barcode);
			}
			// delete the string array list of extra items so it can't be
			// inserted twice
			mExtraItems = null;
			mExtraQuantities = null;
			mExtraPrices = null;
			mExtraBarcodes = null;
			mExtraListUri = null;
		} else {
			Toast.makeText(this, R.string.no_items_available,
					Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Picks an item and returns to calling activity.
	 */
	private void pickItem(Cursor c) {
		long itemId = c.getLong(mStringItemsITEMID);
		Uri url = ContentUris
				.withAppendedId(ShoppingContract.Items.CONTENT_URI, itemId);

		Intent intent = new Intent();
		intent.setData(url);
		setResult(RESULT_OK, intent);
		finish();
	}

	// Menu

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		int MENU_ACTION_WITH_TEXT=0;
		
		//Temp- for backward compatibility with OS 3 features 
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			try{
				//setting the value equivalent to desired expression
				//MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT
				java.lang.reflect.Field field=MenuItem.class.getDeclaredField("SHOW_AS_ACTION_IF_ROOM");
				MENU_ACTION_WITH_TEXT=field.getInt(MenuItem.class);
				field=MenuItem.class.getDeclaredField("SHOW_AS_ACTION_WITH_TEXT");
				MENU_ACTION_WITH_TEXT|=field.getInt(MenuItem.class);				
			}catch(Exception e){
				//reset value irrespective of cause
				MENU_ACTION_WITH_TEXT=0;
			}
			
		}
		
		// Add menu option for auto adding items from string array in intent
		// extra if they exist		
		if (mExtraItems != null) {
			menu.add(0, MENU_INSERT_FROM_EXTRAS, 0, R.string.menu_auto_add)
					.setIcon(android.R.drawable.ic_menu_upload);
		}

		//Temp - Temporary item holder for compatibility framework
		MenuItem item=null;
		// Standard menu
		item=menu.add(0, MENU_NEW_LIST, 0, R.string.new_list)
				.setIcon(R.drawable.ic_menu_add_list).setShortcut('0', 'n');
		MenuCompat.setShowAsAction(item, MENU_ACTION_WITH_TEXT);
		item=menu.add(0, MENU_CLEAN_UP_LIST, 0, R.string.clean_up_list)
				.setIcon(R.drawable.ic_menu_clean_up).setShortcut('1', 'c');
		MenuCompat.setShowAsAction(item, MENU_ACTION_WITH_TEXT);
		

		menu.add(0, MENU_PICK_ITEMS, 0, R.string.menu_pick_items)
				.setIcon(android.R.drawable.ic_menu_add).setShortcut('2', 'p');
		

		/*
		 * menu.add(0, MENU_SHARE, 0, R.string.share)
		 * .setIcon(R.drawable.contact_share001a) .setShortcut('4', 's');
		 */

		menu.add(0, MENU_THEME, 0, R.string.theme)
				.setIcon(android.R.drawable.ic_menu_manage)
				.setShortcut('3', 't');

		item=menu.add(0, MENU_PREFERENCES, 0, R.string.preferences)
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setShortcut('4', 'p');
		MenuCompat.setShowAsAction(item, MENU_ACTION_WITH_TEXT);

		item=menu.add(0, MENU_RENAME_LIST, 0, R.string.rename_list)
				.setIcon(android.R.drawable.ic_menu_edit).setShortcut('5', 'r');
		MenuCompat.setShowAsAction(item, MENU_ACTION_WITH_TEXT);

		menu.add(0, MENU_DELETE_LIST, 0, R.string.delete_list)
				.setIcon(android.R.drawable.ic_menu_delete)
				.setShortcut('6', 'd');
		MenuCompat.setShowAsAction(item, MENU_ACTION_WITH_TEXT);

		menu.add(0, MENU_SEND, 0, R.string.send)
				.setIcon(android.R.drawable.ic_menu_send).setShortcut('7', 's');

		if (addLocationAlertPossible()) {
			menu.add(0, MENU_ADD_LOCATION_ALERT, 0, R.string.shopping_add_alert)
					.setIcon(android.R.drawable.ic_menu_mylocation)
					.setShortcut('8', 'l');
		}
		
		menu.add(0, MENU_MARK_ALL_ITEMS, 0, R.string.mark_all_items)
				.setIcon(android.R.drawable.ic_menu_agenda)
				.setShortcut('9', 'm');
		MenuCompat.setShowAsAction(item, MENU_ACTION_WITH_TEXT);
		

		// Add distribution menu items last.
		mDistribution.onCreateOptionsMenu(menu);

		// NOTE:
		// Dynamically added menu items are included in onPrepareOptionsMenu()
		// instead of here!
		// (Explanation see there.)

		return true;
	}

	/**
	 * Check whether an application exists that handles the pick activity.
	 * 
	 * @return
	 */
	private boolean addLocationAlertPossible() {

		// Test whether intent exists for picking a location:
		PackageManager pm = getPackageManager();
		Intent intent = new Intent(Intent.ACTION_PICK, Locations.CONTENT_URI);
		List<ResolveInfo> resolve_pick_location = pm.queryIntentActivities(
				intent, PackageManager.MATCH_DEFAULT_ONLY);
		/*
		 * for (int i = 0; i < resolve_pick_location.size(); i++) { Log.d(TAG,
		 * "Activity name: " + resolve_pick_location.get(i).activityInfo.name);
		 * }
		 */

		// Check whether adding alerts is possible.
		intent = new Intent(Intent.ACTION_VIEW, Alert.Generic.CONTENT_URI);
		List<ResolveInfo> resolve_view_alerts = pm.queryIntentActivities(
				intent, PackageManager.MATCH_DEFAULT_ONLY);

		boolean pick_location_possible = (resolve_pick_location.size() > 0);
		boolean view_alerts_possible = (resolve_view_alerts.size() > 0);
		if (debug)
			Log.d(TAG, "Pick location possible: " + pick_location_possible);
		if (debug)
			Log.d(TAG, "View alerts possible: " + view_alerts_possible);
		if (pick_location_possible && view_alerts_possible) {
			return true;
		}

		return false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		// TODO: Add item-specific menu items (see NotesList.java example)
		// like edit, strike-through, delete.

		// Add menu option for auto adding items from string array in intent
		// extra if they exist
		if (mExtraItems == null) {
			menu.removeItem(MENU_INSERT_FROM_EXTRAS);
		}

		// Selected list:
		long listId = getSelectedListId();

		// set menu title for change mode
		MenuItem menuItem = menu.findItem(MENU_PICK_ITEMS);
		
		if (mItemsView.mMode == MODE_ADD_ITEMS) {
			menuItem.setTitle(R.string.menu_start_shopping);
			menuItem.setIcon(android.R.drawable.ic_menu_myplaces); 
		} else {
			menu.findItem(MENU_PICK_ITEMS).setTitle(R.string.menu_pick_items);
			menuItem.setIcon(android.R.drawable.ic_menu_add);
		}

		// set menu title for change mode
		menuItem = menu.findItem(MENU_CLEAN_UP_LIST).setVisible(
				mItemsView.mMode == MODE_IN_SHOP);

		// Delete list is possible, if we have more than one list:
		// AND
		// the current list is not the default list (listId == 0) - issue #105
		// TODO: Later, the default list should be user-selectable,
		// and not deletable.

		// TODO ???
		/*
		 * menu.setItemShown(MENU_DELETE_LIST, mCursorListFilter.count() > 1 &&
		 * listId != 1); // 1 is hardcoded number of default first list.
		 */

		// The following code is put from onCreateOptionsMenu to
		// onPrepareOptionsMenu,
		// because the URI of the shopping list can change if the user switches
		// to another list.
		// Generate any additional actions that can be performed on the
		// overall list. This allows other applications to extend
		// our menu with their own actions.
		Intent intent = new Intent(null, getIntent().getData());
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		// menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
		// new ComponentName(this, NoteEditor.class), null, intent, 0, null);

		// Workaround to add icons:
		MenuIntentOptionsWithIcons menu2 = new MenuIntentOptionsWithIcons(this,
				menu);
		menu2.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
				new ComponentName(this, ShoppingActivity.class), null, intent,
				0, null);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (debug)
			Log.d(TAG, "onOptionsItemSelected getItemId: " + item.getItemId());
		Intent intent;
		switch (item.getItemId()) {
		case MENU_NEW_LIST:
			showDialog(DIALOG_NEW_LIST);
			return true;

		case MENU_CLEAN_UP_LIST:
			cleanupList();
			return true;

		case MENU_RENAME_LIST:
			showDialog(DIALOG_RENAME_LIST);
			return true;

		case MENU_DELETE_LIST:
			deleteListConfirm();
			return true;

		case MENU_PICK_ITEMS:
			if (PreferenceActivity.getUsingPickItemsDlgFromPrefs(getApplicationContext())) {
				pickItems();
			} else {
			  if (mItemsView.mMode == MODE_IN_SHOP) {
			    mItemsView.mMode = MODE_ADD_ITEMS;
			  } else {
			    mItemsView.mMode = MODE_IN_SHOP;
			  }
			  onModeChanged();
			}
			return true;

		case MENU_SHARE:
			setShareSettings();
			return true;

		case MENU_THEME:
			setThemeSettings();
			return true;

		case MENU_ADD_LOCATION_ALERT:
			addLocationAlert();
			return true;

		case MENU_PREFERENCES:
			intent = new Intent(this, PreferenceActivity.class);
			startActivity(intent);
			return true;
		case MENU_SEND:
			sendList();
			return true;
		case MENU_INSERT_FROM_EXTRAS:
			insertItemsFromExtras();
			return true;
		case MENU_MARK_ALL_ITEMS:
			mItemsView.toggleOnAllItems();
			return true;
		}
		if (debug)
			Log.d(TAG, "Start intent group id : " + item.getGroupId());
		if (Menu.CATEGORY_ALTERNATIVE == item.getGroupId()) {
			// Start alternative cateogory intents with option to return a
			// result.
			if (debug)
				Log.d(TAG, "Start alternative intent for : "
						+ item.getIntent().getDataString());
			startActivityForResult(item.getIntent(),
					REQUEST_CODE_CATEGORY_ALTERNATIVE);
			return true;
		}
		return super.onOptionsItemSelected(item);

	}

	/**
	 * 
	 */
	private void pickItems() {
		Intent intent;
		intent = new Intent(this, PickItemsActivity.class);
		intent.setData(mListUri);
		startActivity(intent);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case MENU_MARK_ITEM:
			markItem(menuInfo.position);
			break;
		case MENU_EDIT_ITEM:
			editItem(menuInfo.position, EditItemDialog.FieldType.ITEMNAME);
			break;
		case MENU_REMOVE_ITEM_FROM_LIST:
			removeItemFromList(menuInfo.position);
			break;
		case MENU_DELETE_ITEM:
			deleteItemDialog(menuInfo.position);
			break;
		case MENU_MOVE_ITEM:
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_PICK);
			intent.setData(ShoppingContract.Lists.CONTENT_URI);
			startActivityForResult(intent, REQUEST_PICK_LIST);
			mMoveItemPosition = menuInfo.position;
			break;
		case MENU_ITEM_STORES:
			editItemStores(menuInfo.position);
			break;
		}

		return true;
	}

	// /////////////////////////////////////////////////////
	//
	// Menu functions
	//

	/**
	 * Creates a new list from dialog.
	 * 
	 * @return true if new list was created. False if new list was not created,
	 *         because user has not given any name.
	 */
	private boolean createNewList(String name) {

		if (name.equals("")) {
			// User has not provided any name
			Toast.makeText(this, getString(R.string.please_enter_name),
					Toast.LENGTH_SHORT).show();
			return false;
		}

		String previousTheme = loadListTheme();

		int newId = (int) ShoppingUtils.getList(this, name);
		fillListFilter();

		setSelectedListId(newId);

		// Now set the theme based on the selected list:
		saveListTheme(previousTheme);
		mItemsView.setListTheme(previousTheme);

		return true;
	}

	/**
	 * Rename list from dialog.
	 * 
	 * @return true if new list was renamed. False if new list was not renamed,
	 *         because user has not given any name.
	 */
	private boolean renameList(String newName) {

		if (newName.equals("")) {
			// User has not provided any name
			Toast.makeText(this, getString(R.string.please_enter_name),
					Toast.LENGTH_SHORT).show();
			return false;
		}

		// Rename currently selected list:
		ContentValues values = new ContentValues();
		values.put(Lists.NAME, "" + newName);
		getContentResolver().update(
				Uri.withAppendedPath(Lists.CONTENT_URI,
						mCursorShoppingLists.getString(0)), values, null, null);

		mCursorShoppingLists.requery();
		return true;
	}

	private void sendList() {
		if (mItemsView.getAdapter() instanceof CursorAdapter) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < mItemsView.getAdapter().getCount(); i++) {
				Cursor item = (Cursor) mItemsView.getAdapter().getItem(i);
				if (item.getLong(mStringItemsSTATUS) == ShoppingContract.Status.BOUGHT) {
					sb.append("[X] ");
				} else {
					sb.append("[ ] ");
				}
				String quantity = item.getString(mStringItemsQUANTITY);
				long pricecent = item.getLong(mStringItemsITEMPRICE);
				String price = PriceConverter.getStringFromCentPrice(pricecent);
				String tags = item.getString(mStringItemsITEMTAGS);
				if (!TextUtils.isEmpty(quantity)) {
					sb.append(quantity);
					sb.append(" ");
				}
				String units = item.getString(mStringItemsITEMUNITS);
				if (!TextUtils.isEmpty(units)) {
					sb.append(units);
					sb.append(" ");
				}
				sb.append(item.getString(mStringItemsITEMNAME));
				// Put additional info (price, tags) in brackets
				boolean p = !TextUtils.isEmpty(price);
				boolean t = !TextUtils.isEmpty(tags);
				if (p || t) {
					sb.append(" (");
					if (p) {
						sb.append(price);
					}
					if (p && t) {
						sb.append(", ");
					}
					if (t) {
						sb.append(tags);
					}
					sb.append(")");
				}
				sb.append("\n");
			}

			Intent i = new Intent();
			i.setAction(Intent.ACTION_SEND);
			i.setType("text/plain");
			i.putExtra(Intent.EXTRA_SUBJECT, getCurrentListName());
			i.putExtra(Intent.EXTRA_TEXT, sb.toString());

			try {
				startActivity(Intent.createChooser(i, getString(R.string.send)));
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this, R.string.email_not_available,
						Toast.LENGTH_SHORT).show();
				Log.e(TAG, "Email client not installed");
			}
		} else {
			Toast.makeText(this, R.string.empty_list_not_sent,
					Toast.LENGTH_SHORT);
		}

	}

	/**
	 * Clean up the currently visible shopping list by removing items from list
	 * that are marked BOUGHT.
	 */
	private void cleanupList() {
		// Remove all items from current list
		// which have STATUS = Status.BOUGHT

		if (!mItemsView.cleanupList()) {
			// Show toast
			Toast.makeText(this, R.string.no_items_marked, Toast.LENGTH_SHORT)
					.show();
		}
	}

	/**
	 * Confirm 'delete list' command by AlertDialog.
	 */
	private void deleteListConfirm() {
		new AlertDialog.Builder(this)
				// .setIcon(R.drawable.alert_dialog_icon)
				.setTitle(R.string.delete_list)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// click Ok
								deleteList();
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// click Cancel
							}
						})
				// .create()
				.show();
	}

	/**
	 * Deletes currently selected shopping list.
	 */
	private void deleteList() {
		String listId = mCursorShoppingLists.getString(0);
		// First delete all items in list
		getContentResolver().delete(Contains.CONTENT_URI,
				"list_id = " + listId, null);

		// Then delete currently selected list
		getContentResolver().delete(Lists.CONTENT_URI, "_id = " + listId, null);

		// Update view
		fillListFilter();
		fillItems();

		// Now set the theme based on the selected list:
		mItemsView.setListTheme(loadListTheme());

	}

	/** Mark item */
	void markItem(int position) {
		mItemsView.toggleItemBought(position);
	}

	/**
	 * Edit item
	 * 
	 * @param field
	 */
	void editItem(int position, EditItemDialog.FieldType field) {
		if (debug)
			Log.d(TAG, "EditItems: Position: " + position);
		mItemsView.mCursorItems.moveToPosition(position);
		// mEditItemPosition = position;

		long itemId = mItemsView.mCursorItems.getLong(mStringItemsITEMID);
		long containsId = mItemsView.mCursorItems
				.getLong(mStringItemsCONTAINSID);

		mItemUri = Uri
				.withAppendedPath(ShoppingContract.Items.CONTENT_URI, "" + itemId);
		mRelationUri = Uri.withAppendedPath(ShoppingContract.Contains.CONTENT_URI, ""
				+ containsId);
		mEditItemFocusField = field;

		showDialog(DIALOG_EDIT_ITEM);
	}

	void editItemStores(int position) {
		if (debug)
			Log.d(TAG, "EditItemStores: Position: " + position);

		mItemsView.mCursorItems.moveToPosition(position);
		// mEditItemPosition = position;
		long itemId = mItemsView.mCursorItems.getLong(mStringItemsITEMID);
	
		Intent intent;
		intent = new Intent(this, ItemStoresActivity.class);
		intent.setData(mListUri.buildUpon().appendPath(String.valueOf(itemId)).build());
		startActivity(intent);
	}
	
	int mDeleteItemPosition;

	/** delete item */
	void deleteItemDialog(int position) {
		if (debug)
			Log.d(TAG, "EditItems: Position: " + position);
		mItemsView.mCursorItems.moveToPosition(position);
		mDeleteItemPosition = position;

		showDialog(DIALOG_DELETE_ITEM);
	}

	/** delete item */
	void deleteItem(int position) {
		Cursor c = mItemsView.mCursorItems;
		c.moveToPosition(position);
		// Delete item from all lists
		// by deleting contains row
		getContentResolver().delete(Contains.CONTENT_URI, "item_id = ?",
				new String[] { c.getString(mStringItemsITEMID) });

		// and delete item
		getContentResolver().delete(Items.CONTENT_URI, "_id = ?",
				new String[] { c.getString(mStringItemsITEMID) });

		// c.requery();
		mItemsView.requery();
	}

	/** move item */
	void moveItem(int position, int targetListId) {
		Cursor c = mItemsView.mCursorItems;
		mItemsView.mCursorItems.requery();
		c.moveToPosition(position);

		long listId = getSelectedListId();
		if (false && listId < 0) {
			// No valid list - probably view is not active
			// and no item is selected.
			return;
		}
		listId = Integer.parseInt(mListUri.getLastPathSegment());

		// add item to new list
		ShoppingUtils.addItemToList(this, c.getInt(mStringItemsITEMID),
				targetListId, Status.WANT_TO_BUY,
				c.getString(mStringItemsPRIORITY),
				c.getString(mStringItemsQUANTITY), false);

		// Delete item from currentList
		// by deleting contains row
		getContentResolver().delete(
				Contains.CONTENT_URI,
				"item_id = ? and list_id = ?",
				new String[] { c.getString(mStringItemsITEMID),
						String.valueOf(listId) });

		mItemsView.requery();
	}

	/** removeItemFromList */
	void removeItemFromList(int position) {
		Cursor c = mItemsView.mCursorItems;
		c.moveToPosition(position);
		// Remember old values before delete (for share below)
		String itemName = c.getString(mStringItemsITEMNAME);
		long oldstatus = c.getLong(mStringItemsSTATUS);

		// Delete item by changing its state
		ContentValues values = new ContentValues();
		values.put(Contains.STATUS, Status.REMOVED_FROM_LIST);
		getContentResolver().update(Contains.CONTENT_URI, values, "_id = ?",
				new String[] { c.getString(mStringItemsCONTAINSID) });

		// c.requery();

		mItemsView.requery();

		// If we share items, mark item on other lists:
		// TODO ???
		/*
		 * String recipients =
		 * mCursorListFilter.getString(mStringListFilterSHARECONTACTS); if (!
		 * recipients.equals("")) { String shareName =
		 * mCursorListFilter.getString(mStringListFilterSHARENAME); long
		 * newstatus = Shopping.Status.BOUGHT;
		 * 
		 * Log.i(TAG, "Update shared item. " + " recipients: " + recipients +
		 * ", shareName: " + shareName + ", status: " + newstatus);
		 * mGTalkSender.sendItemUpdate(recipients, shareName, itemName,
		 * itemName, oldstatus, newstatus); }
		 */
	}

	/**
	 * Calls the share settings with the currently selected list.
	 */
	void setShareSettings() {
		// Obtain URI of current list

		// Call share settings as subactivity
		Intent intent = new Intent(OpenIntents.SET_SHARE_SETTINGS_ACTION,
				mListUri);
		startActivityForResult(intent, SUBACTIVITY_LIST_SHARE_SETTINGS);

	}

	void setThemeSettings() {
		showDialog(DIALOG_THEME);
	}

	@Override
	public String onLoadTheme() {
		return loadListTheme();
	}

	@Override
	public void onSaveTheme(String theme) {
		saveListTheme(theme);
	}

	@Override
	public void onSetTheme(String theme) {
		mItemsView.setListTheme(theme);
	}

	@Override
	public void onSetThemeForAll(String theme) {
		setThemeForAll(this, theme);
	}

	/**
	 * Set theme for all lists.
	 * 
	 * @param context
	 * @param theme
	 */
	public static void setThemeForAll(Context context, String theme) {
		ContentValues values = new ContentValues();
		values.put(Lists.SKIN_BACKGROUND, theme);
		context.getContentResolver().update(Lists.CONTENT_URI, values, null,
				null);
	}

	/**
	 * Loads the theme settings for the currently selected theme.
	 * 
	 * Up to version 1.2.1, only one of 3 hardcoded themes are available. These
	 * are stored in 'skin_background' as '1', '2', or '3'.
	 * 
	 * Starting in 1.2.2, also themes of other packages are allowed.
	 * 
	 * @return
	 */
	public String loadListTheme() {
		/*
		 * long listId = getSelectedListId(); if (listId < 0) { // No valid list
		 * - probably view is not active // and no item is selected. return 1;
		 * // return default theme }
		 */

		// Return default theme if something unexpected happens:
		if (mCursorShoppingLists == null)
			return "1";
		if (mCursorShoppingLists.getPosition() < 0)
			return "1";

		// mCursorListFilter has been set to correct position
		// by calling getSelectedListId(),
		// so we can read out further elements:
		String skinBackground = mCursorShoppingLists
				.getString(mStringListFilterSKINBACKGROUND);

		return skinBackground;
	}

	public void saveListTheme(String theme) {
		long listId = getSelectedListId();
		if (listId < 0) {
			// No valid list - probably view is not active
			// and no item is selected.
			return; // return default theme
		}

		ContentValues values = new ContentValues();
		values.put(Lists.SKIN_BACKGROUND, theme);
		getContentResolver().update(
				Uri.withAppendedPath(Lists.CONTENT_URI,
						mCursorShoppingLists.getString(0)), values, null, null);

		mCursorShoppingLists.requery();
	}

	/**
	 * Calls a dialog for setting the locations alert.
	 */
	void addLocationAlert() {

		// Call dialog as activity
		Intent intent = new Intent(OpenIntents.ADD_LOCATION_ALERT_ACTION,
				mListUri);
		// startSubActivity(intent, SUBACTIVITY_ADD_LOCATION_ALERT);
		startActivity(intent);
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {

		case DIALOG_NEW_LIST:
			return new NewListDialog(this, new DialogActionListener() {

				public void onAction(String name) {
					createNewList(name);
				}
			});

		case DIALOG_RENAME_LIST:
			return new RenameListDialog(this, getCurrentListName(),
					new DialogActionListener() {

						public void onAction(String name) {
							renameList(name);
						}
					});

		case DIALOG_EDIT_ITEM:
			return new EditItemDialog(this, mItemUri, mRelationUri);

		case DIALOG_DELETE_ITEM:
			return new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.menu_delete_item)
					.setMessage(R.string.delete_item_confirm)
					.setPositiveButton(R.string.delete,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									deleteItem(mDeleteItemPosition);
								}
							})
					.setNegativeButton(android.R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Don't do anything
								}
							}).create();

		case DIALOG_THEME:
			return new ThemeDialog(this, this);
		}
		return super.onCreateDialog(id);

	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);

		switch (id) {

		case DIALOG_RENAME_LIST:
			((RenameListDialog) dialog).setName(getCurrentListName());
			break;

		case DIALOG_EDIT_ITEM:
			EditItemDialog d = (EditItemDialog) dialog;
			d.setItemUri(mItemUri);
			d.setRelationUri(mRelationUri);
			d.setFocusField(mEditItemFocusField);

			String[] taglist = getTaglist();
			d.setTagList(taglist);

			d.setRequeryCursor(mItemsView.mCursorItems);
			break;

		case DIALOG_THEME:
			((ThemeDialog) dialog).prepareDialog();
			break;
		}
	}

	// /////////////////////////////////////////////////////
	//
	// Helper functions
	//
	/**
	 * Returns the ID of the selected shopping list.
	 * 
	 * As a side effect, the item URI is updated. Returns -1 if nothing is
	 * selected.
	 * 
	 * @return ID of selected shopping list.
	 */
	private long getSelectedListId() {
		int pos = mShoppingListsView.getSelectedItemPosition();
		//Temp- Due to architecture requirements of OS 3, the value can not be passed directly
		if(pos==-1 && Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
			try {
				pos=(Integer)mShoppingListsView.getTag();	
				pos=mCursorShoppingLists.getCount()<=pos?-1:pos;
			} catch (Exception e) {
//				e.printStackTrace();
			}
		}
		if (pos < 0) {
			// nothing selected - probably view is out of focus:
			// Do nothing.
			return -1;
		}

		// Obtain Id of currently selected shopping list:
		mCursorShoppingLists.moveToPosition(pos);

		long listId = mCursorShoppingLists.getLong(mStringListFilterID);

		mListUri = Uri
				.withAppendedPath(ShoppingContract.Lists.CONTENT_URI, "" + listId);

		getIntent().setData(mListUri);

		return listId;
	};

	/**
	 * sets the selected list to a specific list Id
	 */
	private void setSelectedListId(int id) {
		// Is there a nicer way to accomplish the following?
		// (we look through all elements to look for the
		// one entry that has the same ID as returned by
		// getDefaultList()).
		//
		// unfortunately, a SQL query won't work, as it would
		// return 1 row, but I still would not know which
		// row in the mCursorListFilter corresponds to that id.
		//
		// one could use: findViewById() but how will this
		// translate to the position in the list?
		mCursorShoppingLists.moveToPosition(-1);
		while (mCursorShoppingLists.moveToNext()) {
			int posId = mCursorShoppingLists.getInt(mStringListFilterID);
			if (posId == id) {
				int row = mCursorShoppingLists.getPosition();

				// if we found the Id, then select this in
				// the Spinner:
				setSelectedListPos(row);
				break;
			}
		}
	}
	
	private void setSelectedListPos(int pos){
		mShoppingListsView.setTag(pos);
		
		mShoppingListsView.setSelection(pos);	
		
		if (getSelectedListId() != mItemsView.getListId()) {
		   fillItems();
		}
		// Now set the theme based on the selected list:
		mItemsView.setListTheme(loadListTheme());
		
		if (mShoppingListsView instanceof ListView){
			((ListView) mShoppingListsView).setItemChecked(pos,true);
		}
	}
	

	/**
     *
     */
	private void fillListFilter() {
		// Get a cursor with all lists
		mCursorShoppingLists = getContentResolver().query(Lists.CONTENT_URI,
				mStringListFilter, null, null, Lists.DEFAULT_SORT_ORDER);
		startManagingCursor(mCursorShoppingLists);

		if (mCursorShoppingLists == null) {
			Log.e(TAG, "missing shopping provider");
			ArrayAdapter adapter=new ArrayAdapter(this,
					android.R.layout.simple_spinner_item,
					new String[] { getString(R.string.no_shopping_provider) });
			setSpinnerListAdapter(adapter);
			
			return;
		}

		if (mCursorShoppingLists.getCount() < 1) {
			// We have to create default shopping list:
			long listId = ShoppingUtils.getList(this,
					getText(R.string.my_shopping_list).toString());

			// Check if insertion really worked. Otherwise
			// we may end up in infinite recursion.
			if (listId < 0) {
				// for some reason insertion did not work.
				return;
			}

			// The insertion should have worked, so let us call ourselves
			// to try filling the list again:
			fillListFilter();
			return;
		}

		class mListContentObserver extends ContentObserver {

			public mListContentObserver(Handler handler) {
				super(handler);
				if (debug)
					Log.i(TAG, "mListContentObserver: Constructor");
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.database.ContentObserver#deliverSelfNotifications()
			 */
			@Override
			public boolean deliverSelfNotifications() {
				// TODO Auto-generated method stub
				if (debug)
					Log.i(TAG, "mListContentObserver: deliverSelfNotifications");
				return super.deliverSelfNotifications();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.database.ContentObserver#onChange(boolean)
			 */
			@Override
			public void onChange(boolean arg0) {
				// TODO Auto-generated method stub
				if (debug)
					Log.i(TAG, "mListContentObserver: onChange");

				mCursorShoppingLists.requery();

				super.onChange(arg0);
			}

		}
		;
		mListContentObserver observer = new mListContentObserver(new Handler());
		mCursorShoppingLists.registerContentObserver(observer);

		// Register a ContentObserver, so that a new list can be
		// automatically detected.
		// mCursor

		/*
		 * ArrayList<String> list = new ArrayList<String>(); // TODO Create
		 * summary of all lists // list.add(ALL); while
		 * (mCursorListFilter.next()) {
		 * list.add(mCursorListFilter.getString(mStringListFilterNAME)); }
		 * ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
		 * android.R.layout.simple_spinner_item, list);
		 * adapter.setDropDownViewResource(
		 * android.R.layout.simple_spinner_dropdown_item);
		 * mSpinnerListFilter.setAdapter(adapter);
		 */

		SimpleCursorAdapter adapter;
		
		if (mShoppingListsView instanceof Spinner){
		adapter = new SimpleCursorAdapter(this,
				// Use a template that displays a text view
				android.R.layout.simple_spinner_item,
				// Give the cursor to the list adapter
				mCursorShoppingLists, new String[] { Lists.NAME },
				new int[] { android.R.id.text1 });
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		} else {
			// mShoppingListView is a ListView
			adapter = new SimpleCursorAdapter(this,
					// Use a template that displays a text view
					R.layout.list_item_shopping_list,
					// Give the cursor to the list adapter
					mCursorShoppingLists, new String[] { Lists.NAME },
					new int[] { R.id.text1 });
		}
//		mSpinnerListFilter.setAdapter(adapter);//Temp- redirected through method
		setSpinnerListAdapter(adapter);

	}

	private void onModeChanged() {

		if (debug)
			Log.d(TAG, "onModeChanged()");
		fillItems();

		if (mItemsView.mMode == MODE_IN_SHOP) {
			setTitle(getString(R.string.shopping_title,
					getCurrentListName()));
			registerSensor();
		} else {
			setTitle(getString(R.string.pick_items_titel,
					getCurrentListName()));
			unregisterSensor();
		}
	}

	private String getCurrentListName() {
		return ((Cursor) mShoppingListsView.getSelectedItem())
				.getString(mStringListFilterNAME);
	}	
	

	private void fillItems() {
		if (debug)
			Log.d(TAG, "fillItems()");

		long listId = getSelectedListId();
		if (listId < 0) {
			// No valid list - probably view is not active
			// and no item is selected.
			return;
		}
		
		if (debug)
			Log.d(TAG, "fillItems() for list " + listId);
		mItemsView.fillItems(this, listId);

		// Insert any pending items received either through intents
		// or in onActivityResult:
		if (mExtraItems != null) {
			insertItemsFromExtras();
		}
	}

	/**
	 * Create list of tags.
	 * 
	 * Tags for notes can be comma-separated. Here we create a list of the
	 * unique tags.
	 * 
	 * @param c
	 * @return
	 */
	String[] getTaglist() {
		Cursor c = getContentResolver().query(ShoppingContract.Items.CONTENT_URI,
				new String[] { ShoppingContract.Items.TAGS }, null, null,
				ShoppingContract.Items.DEFAULT_SORT_ORDER);
		// Create a set of all tags (every tag should only appear once).
		HashSet<String> tagset = new HashSet<String>();
		c.moveToPosition(-1);
		while (c.moveToNext()) {
			String tags = c.getString(0);
			if (tags != null) {
				// Split several tags in a line, separated by comma
				String[] smalltaglist = tags.split(",");
				for (String tag : smalltaglist) {
					if (!tag.equals("")) {
						tagset.add(tag.trim());
					}
				}
			}
		}
		c.close();

		// Sort the list
		// 1. Convert HashSet to String list.
		ArrayList<String> list = new ArrayList<String>();
		list.addAll(tagset);
		// 2. Sort the String list
		Collections.sort(list);
		// 3. Convert it to String array
		return list.toArray(new String[0]);
	}


	/**
	 * Tests whether the current list is shared via GTalk. (not local sharing!)
	 * 
	 * @return true if SHARE_CONTACTS contains the '@' character.
	 */
	boolean isCurrentListShared() {
		long listId = getSelectedListId();
		if (listId < 0) {
			// No valid list - probably view is not active
			// and no item is selected.
			return false;
		}

		// mCursorListFilter has been set to correct position
		// by calling getSelectedListId(),
		// so we can read out further elements:
		// String shareName =
		// mCursorListFilter.getString(mStringListFilterSHARENAME);
		String recipients = mCursorShoppingLists
				.getString(mStringListFilterSHARECONTACTS);

		// If recipients contains the '@' symbol, it is shared.
		return recipients.contains("@");
	}

	// Handle the process of automatically updating enabled sensors:
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MESSAGE_UPDATE_CURSORS) {
				mCursorShoppingLists.requery();

				if (mUpdating) {
					sendMessageDelayed(obtainMessage(MESSAGE_UPDATE_CURSORS),
							mUpdateInterval);
				}

			}
		}
	};

	/**
	 * Listens for intents for updates in the database.
	 * 
	 * @param context
	 * @param intent
	 */
	// TODO ???
	/*
	 * public class ListIntentReceiver extends IntentReceiver {
	 * 
	 * public void onReceiveIntent(Context context, Intent intent) { String
	 * action = intent.getAction(); Log.i(TAG, "ShoppingList received intent " +
	 * action);
	 * 
	 * if (action.equals(OpenIntents.REFRESH_ACTION)) {
	 * mCursorListFilter.requery();
	 * 
	 * } } }
	 */
	/*
	 * ListIntentReceiver mIntentReceiver;
	 */

	/**
	 * This method is called when the sending activity has finished, with the
	 * result it supplied.
	 * 
	 * @param requestCode
	 *            The original request code as given to startActivity().
	 * @param resultCode
	 *            From sending activity as per setResult().
	 * @param data
	 *            From sending activity as per setResult().
	 * @param extras
	 *            From sending activity as per setResult().
	 * 
	 * @see android.app.Activity#onActivityResult(int, int, java.lang.String,
	 *      android.os.Bundle)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (debug)
			Log.i(TAG, "ShoppingView: onActivityResult. ");

		if (requestCode == SUBACTIVITY_LIST_SHARE_SETTINGS) {
			if (debug)
				Log.i(TAG, "SUBACTIVITY_LIST_SHARE_SETTINGS");

			if (resultCode == RESULT_CANCELED) {
				// Don't do anything.
				if (debug)
					Log.i(TAG, "RESULT_CANCELED");

			} else {
				// Broadcast the intent
				if (debug)
					Log.i(TAG, "Broadcast intent.");

				// TODO ???
				/*
				 * Uri uri = Uri.parse(data);
				 */
				Uri uri = Uri.parse(data.getDataString());

				if (!mListUri.equals(uri)) {
					Log.e(TAG, "Unexpected uri returned: Should be " + mListUri
							+ " but was " + uri);
					return;
				}

				// TODO ???
				Bundle extras = data.getExtras();

				String sharename = extras.getString(ShoppingContract.Lists.SHARE_NAME);
				String contacts = extras
						.getString(ShoppingContract.Lists.SHARE_CONTACTS);

				if (debug)
					Log.i(TAG, "Received bundle: sharename: " + sharename
							+ ", contacts: " + contacts);				

				// Here we also send the current content of the list
				// to all recipients.
				// This could probably be optimized - by sending
				// content only to the new recipients, as the
				// old ones should be in sync already.
				// First delete all items in list
				/*
				 * mCursorItems.moveToPosition(-1); while
				 * (mCursorItems.moveToNext()) { String itemName = mCursorItems
				 * .getString(mStringItemsITEMNAME); Long status =
				 * mCursorItems.getLong(mStringItemsSTATUS); Log.i(TAG,
				 * "Update shared item. " + " recipients: " + contacts +
				 * ", shareName: " + sharename + ", item: " + itemName); // TODO
				 * ??? /* mGTalkSender.sendItemUpdate(contacts, sharename,
				 * itemName, itemName, status, status); / }
				 */
			}

		} else if (REQUEST_CODE_CATEGORY_ALTERNATIVE == requestCode) {
			if (debug)
				Log.d(TAG, "result received");
			if (RESULT_OK == resultCode) {
				if (debug)
					Log.d(TAG, "result OK");
				// Check if any results have been returned:
				/*
				 * if ((data.getDataString() != null) &&
				 * (data.getDataString().startsWith
				 * (Shopping.Lists.CONTENT_URI.toString()))) { // We received a
				 * valid shopping list URI.
				 * 
				 * // Set current list to received list: mListUri =
				 * data.getData(); intent.setData(mListUri); }
				 */
				if (data.getExtras() != null) {
					if (debug)
						Log.d(TAG, "extras received");
					getShoppingExtras(data);
				}
			}
		} else if (REQUEST_PICK_LIST == requestCode) {
			if (debug)
				Log.d(TAG, "result received");

			if (RESULT_OK == resultCode) {
				int position = mMoveItemPosition;
				if (mMoveItemPosition >= 0) {
					moveItem(position, Integer.parseInt(data.getData()
							.getLastPathSegment()));
				}
			}

			mMoveItemPosition = -1;
		}
	}

	public void changeList(int value) {

		int pos = mShoppingListsView.getSelectedItemPosition();
		//Temp- Due to architecture requirements of OS 3, the value can not be passed directly
		if(pos==-1 && Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
			try {
				pos=(Integer)mShoppingListsView.getTag();	
				pos=mCursorShoppingLists.getCount()<=pos?-1:pos;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		int newPos;

		if (pos < 0) {
			// nothing selected - probably view is out of focus:
			// Do nothing.
			newPos = -1;
		} else if (pos == 0) {
			newPos = mShoppingListsView.getCount() - 1;
		} else if (pos == mShoppingListsView.getCount()) {
			newPos = 0;
		} else {
			newPos = pos + value;
		}
		setSelectedListPos(newPos);	
	}
	
	/**
	 * With the requirement of OS3, making an intermediary decision depending upon the widget
	 * @param adapter
	 */
	private void setSpinnerListAdapter(ListAdapter adapter){
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){//Temp - restricted for OS3
			mShoppingListsView.setAdapter(adapter);
		}else{
			ShoppingListFilterFragment os3=(ShoppingListFilterFragment)getSupportFragmentManager().findFragmentById(R.id.sidelist);
			os3.setAdapter(adapter);
		}
	}

}
