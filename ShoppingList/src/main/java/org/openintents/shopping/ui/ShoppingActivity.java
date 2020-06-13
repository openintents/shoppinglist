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

import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
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
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.WrapperListAdapter;

import org.openintents.OpenIntents;
import org.openintents.convertcsv.shoppinglist.ConvertCsvActivity;
import org.openintents.distribution.DistributionLibraryFragmentActivity;
import org.openintents.distribution.DownloadOIAppDialog;
import org.openintents.intents.GeneralIntents;
import org.openintents.intents.ShoppingListIntents;
import org.openintents.provider.Alert;
import org.openintents.shopping.LogConstants;
import org.openintents.shopping.R;
import org.openintents.shopping.ShoppingApplication;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.Items;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.library.provider.ShoppingContract.Stores;
import org.openintents.shopping.library.util.PriceConverter;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.shopping.ui.dialog.DialogActionListener;
import org.openintents.shopping.ui.dialog.EditItemDialog;
import org.openintents.shopping.ui.dialog.EditItemDialog.FieldType;
import org.openintents.shopping.ui.dialog.EditItemDialog.OnItemChangedListener;
import org.openintents.shopping.ui.dialog.NewListDialog;
import org.openintents.shopping.ui.dialog.RenameListDialog;
import org.openintents.shopping.ui.dialog.ThemeDialog;
import org.openintents.shopping.ui.dialog.ThemeDialog.ThemeDialogListener;
import org.openintents.shopping.ui.widget.QuickSelectMenu;
import org.openintents.shopping.ui.widget.ShoppingItemsView;
import org.openintents.shopping.ui.widget.ShoppingItemsView.ActionBarListener;
import org.openintents.shopping.ui.widget.ShoppingItemsView.DragListener;
import org.openintents.shopping.ui.widget.ShoppingItemsView.DropListener;
import org.openintents.shopping.ui.widget.ShoppingItemsView.OnCustomClickListener;
import org.openintents.shopping.widgets.CheckItemsWidget;
import org.openintents.util.MenuIntentOptionsWithIcons;
import org.openintents.util.ShakeSensorListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.openintents.shopping.ui.widget.ShoppingItemsView.MODE_IN_SHOP;

/**
 * Displays a shopping list.
 */
public class ShoppingActivity extends DistributionLibraryFragmentActivity
        implements ThemeDialogListener, OnCustomClickListener,
        ActionBarListener, OnItemChangedListener,
        UndoListener, ShoppingItemsView.OnModeChangeListener { // implements
    // AdapterView.OnItemClickListener
    // {

    public static final int DIALOG_GET_FROM_MARKET = 7;
    public static final int LOADER_TOTALS = 0;
    public static final int LOADER_ITEMS = 1;

    public static final String[] PROJECTION_ITEMS = new String[]{
            ContainsFull._ID, ContainsFull.ITEM_NAME, ContainsFull.ITEM_IMAGE,
            ContainsFull.ITEM_TAGS, ContainsFull.ITEM_PRICE,
            ContainsFull.QUANTITY, ContainsFull.STATUS, ContainsFull.ITEM_ID,
            ContainsFull.SHARE_CREATED_BY, ContainsFull.SHARE_MODIFIED_BY,
            ContainsFull.PRIORITY, ContainsFull.ITEM_HAS_NOTE,
            ContainsFull.ITEM_UNITS};
    public static final int mStringItemsITEMNAME = 1;
    public static final int mStringItemsITEMTAGS = 3;
    public static final int mStringItemsITEMPRICE = 4;
    public static final int mStringItemsQUANTITY = 5;
    public static final int mStringItemsSTATUS = 6;
    public static final int mStringItemsITEMID = 7;
    public static final int mStringItemsPRIORITY = 10;
    public static final int mStringItemsITEMHASNOTE = 11;
    public static final int mStringItemsITEMUNITS = 12;
    /**
     * TAG for logging.
     */
    private static final String TAG = "ShoppingActivity";
    private static final boolean debug = LogConstants.debug;
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
    private static final int MENU_INSERT_FROM_EXTRAS = Menu.FIRST + 10;
    private static final int MENU_COPY_ITEM = Menu.FIRST + 11;
    private static final int MENU_SORT_LIST = Menu.FIRST + 12;
    private static final int MENU_SEARCH_ADD = Menu.FIRST + 13;
    // TODO: obsolete pick items button, now in drawer
    private static final int MENU_PICK_ITEMS = Menu.FIRST + 14;
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
    private static final int MENU_UNMARK_ALL_ITEMS = Menu.FIRST + 23;
    private static final int MENU_SYNC_WEAR = Menu.FIRST + 25;
    private static final int MENU_CONVERT_CSV = Menu.FIRST + 26;
    private static final int MENU_DISTRIBUTION_START = Menu.FIRST + 100; // MUST BE LAST
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
     * <p/>
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
     * Definition of the requestCode for the subactivity.
     */
    static final private int SUBACTIVITY_LIST_SHARE_SETTINGS = 0;
    /**
     * Definition for message handler:
     */
    static final private int MESSAGE_UPDATE_CURSORS = 1;
    private static final String[] mStringListFilter = new String[]{Lists._ID,
            Lists.NAME, Lists.IMAGE, Lists.SHARE_NAME, Lists.SHARE_CONTACTS,
            Lists.SKIN_BACKGROUND};
    private static final int mStringListFilterID = 0;
    private static final int mStringListFilterNAME = 1;
    private static final int mStringListFilterIMAGE = 2;
    private static final int mStringListFilterSHARENAME = 3;
    private static final int mStringListFilterSHARECONTACTS = 4;
    private static final int mStringListFilterSKINBACKGROUND = 5;
    private static final int mStringItemsCONTAINSID = 0;
    private static final int mStringItemsITEMIMAGE = 2;
    private static final int mStringItemsSHARECREATEDBY = 8;
    private static final int mStringItemsSHAREMODIFIEDBY = 9;
    // private static final String BUNDLE_TEXT_ENTRY_MENU = "text entry menu";
    // private static final String BUNDLE_CURSOR_ITEMS_POSITION =
    // "cursor items position";
    private static final String BUNDLE_ITEM_URI = "item uri";
    private static final String BUNDLE_RELATION_URI = "relation_uri";
    private static final String BUNDLE_MODE = "mode";

    // private Cursor mCursorItems;
    private static final String BUNDLE_MODE_BEFORE_SEARCH = "mode_before_search";
    // TODO: Set up state information for onFreeze(), ...
    // State data to be stored when freezing:
    private final String ORIGINAL_ITEM = "original item";
    private ItemsFromExtras mItemsFromExtras = new ItemsFromExtras();
    private ToggleBoughtInputMethod toggleBoughtInputMethod;
    /**
     * Current state
     */
    private int mState;
    /*
     * Value of PreferenceActivity.updateCount last time we called fillItems().
     */
    private int lastAppliedPrefChange = -1;
    private boolean mEditingFilter;
    /**
     * URI of current list
     */
    private Uri mListUri;
    /**
     * URI of selected item
     */
    private Uri mItemUri;
    /**
     * URI of current list and item
     */
    private Uri mListItemUri;
    /**
     * Update interval for automatic requires.
     * <p/>
     * (Workaround since ContentObserver does not work.)
     */
    private int mUpdateInterval;
    private boolean mUpdating;
    /**
     * Private members connected to list of shopping lists
     */
    // Temp - making it generic for tablet compatibility
    private AdapterView mShoppingListsView;
    private Cursor mCursorShoppingLists;
    private ShoppingItemsView mItemsView;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListsView;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle, mDrawerTitle, mSubTitle;
    private LinearLayout.LayoutParams mLayoutParamsItems;
    private int mAllowedListHeight; // Height for the list allowed in this view.
    private AutoCompleteTextView mEditText;
    private Button mButton;
    private View mAddPanel;
    private Button mStoresFilterButton;
    private Button mTagsFilterButton;
    private Button mShoppingListsFilterButton;
    private String mSortOrder;

    private ListSortActionProvider mListSortActionProvider;

    // Skins --------------------------

    /**
     * Remember position for screen orientation change.
     */
    // int mEditItemPosition = -1;

    // public int mPriceVisibility;
    // private int mTagsVisibility;
    private SensorManager mSensorManager;
    private SensorEventListener mMySensorListener = new ShakeSensorListener() {

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
    private boolean mIsActive;

    /**
     * Whether to use the sensor for shake.
     */
    private boolean mUseSensor;
    private Uri mRelationUri;
    private int mMoveItemPosition;

    private EditItemDialog.FieldType mEditItemFocusField = EditItemDialog.FieldType.ITEMNAME;
    private GestureDetector mGestureDetector;
    private View.OnTouchListener mGestureListener;
    private int mDeleteItemPosition;
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
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (debug) {
            Log.d(TAG, "Shopping list onCreate()");
        }

        mSortOrder = PreferenceActivity.getShoppingListSortOrderFromPrefs(this);

        mDistribution.setFirst(MENU_DISTRIBUTION_START,
                DIALOG_DISTRIBUTION_START);

        // Check whether EULA has been accepted
        // or information about new version can be presented.
        if (false && mDistribution.showEulaOrNewVersion()) {
            return;
        }

        if (LayoutChoiceActivity.show(this)) {
            finish();
        }
        setContentView(R.layout.activity_shopping);

        // mEditItemPosition = -1;

        // Automatic requeries (once a second)
        mUpdateInterval = 2000;
        mUpdating = false;

        // General Uris:
        mListUri = ShoppingContract.Lists.CONTENT_URI;
        mItemUri = ShoppingContract.Items.CONTENT_URI;
        mListItemUri = ShoppingContract.Items.CONTENT_URI;

        int defaultShoppingList = getLastUsedListFromPrefs();

        // Handle the calling intent
        final Intent intent = getIntent();
        final String type = intent.resolveType(this);
        final String action = intent.getAction();

        if (action == null) {
            // Main action
            mState = STATE_MAIN;
            mListUri = buildDefaultShoppingListUri(defaultShoppingList);
            intent.setData(mListUri);

        } else if (Intent.ACTION_MAIN.equals(action)) {
            // Main action
            mState = STATE_MAIN;
            mListUri = buildDefaultShoppingListUri(defaultShoppingList);
            intent.setData(mListUri);

        } else if (Intent.ACTION_VIEW.equals(action)) {
            mState = STATE_VIEW_LIST;
            mListUri = setListUriFromIntent(intent.getData(), type);

        } else if (Intent.ACTION_INSERT.equals(action)) {
            mState = STATE_VIEW_LIST;
            mListUri = setListUriFromIntent(intent.getData(), type);

        } else if (Intent.ACTION_PICK.equals(action)) {
            mState = STATE_PICK_ITEM;
            mListUri = buildDefaultShoppingListUri(defaultShoppingList);

        } else if (Intent.ACTION_GET_CONTENT.equals(action)) {
            mState = STATE_GET_CONTENT_ITEM;
            mListUri = buildDefaultShoppingListUri(defaultShoppingList);

        } else if (GeneralIntents.ACTION_INSERT_FROM_EXTRAS.equals(action)) {
            if (ShoppingListIntents.TYPE_STRING_ARRAYLIST_SHOPPING.equals(type)) {
                /*
                 * Need to insert new items from a string array in the intent
                 * extras Use main action but add an item to the options menu
                 * for adding extra items
                 */
                mItemsFromExtras.getShoppingExtras(intent);
                mState = STATE_MAIN;
                mListUri = buildDefaultShoppingListUri(defaultShoppingList);
                intent.setData(mListUri);
            } else if (intent.getDataString().startsWith(
                    ShoppingContract.Lists.CONTENT_URI.toString())) {
                // Somewhat quick fix to pass data from ShoppingListsActivity to
                // this activity.

                // We received a valid shopping list URI:
                mListUri = intent.getData();

                mItemsFromExtras.getShoppingExtras(intent);
                mState = STATE_MAIN;
                intent.setData(mListUri);
            }
        } else {
            // Unknown action.
            Log.e(TAG, "Shopping: Unknown action, exiting");
            Toast.makeText(this, "Don't know how to handle " + action, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // hook up all buttons, lists, edit text:
        createView();

        // populate the lists
        fillListFilter();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

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
            List<String> pathSegs = mItemUri.getPathSegments();
            int num = pathSegs.size();
            mListItemUri = Uri
                    .withAppendedPath(mListUri, pathSegs.get(num - 1));
            if (icicle.containsKey(BUNDLE_RELATION_URI)) {
                mRelationUri = Uri.parse(icicle.getString(BUNDLE_RELATION_URI));
            }
            mItemsView.setModes(icicle.getInt(BUNDLE_MODE), icicle.getInt(BUNDLE_MODE_BEFORE_SEARCH));
        }

        // set focus to the edit line:
        mEditText.requestFocus();

        // TODO remove initFromPreferences from onCreate
        // we need it in resume to update after settings have changed
        initFromPreferences();
        // now update title and fill all items
        onModeChanged();

        mItemsView.setActionBarListener(this);
        mItemsView.setUndoListener(this);

        toggleBoughtInputMethod = ((ShoppingApplication) getApplication()).dependencies().getToggleBoughtInputMethod(this, mItemsView);
    }

    private Uri buildDefaultShoppingListUri(final int defaultShoppingList) {
        return Uri.withAppendedPath(Lists.CONTENT_URI,
                String.valueOf(defaultShoppingList));
    }

    private Uri setListUriFromIntent(Uri data, String type) {
        if (ShoppingContract.ITEM_TYPE.equals(type)) {
            return ShoppingUtils.getListForItem(this, data
                    .getLastPathSegment());
        } else return data;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            // Sync the toggle state after onRestoreInstanceState has occurred.
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        updateWidgets();
    }

    private void updateWidgets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.getAppWidgetIds(new ComponentName(this
                .getPackageName(), CheckItemsWidget.class.getName()));
        List<AppWidgetProviderInfo> b = appWidgetManager.getInstalledProviders();
        for (AppWidgetProviderInfo i : b) {
            if (i.provider.getPackageName().equals(this.getPackageName())) {
                int[] a = appWidgetManager.getAppWidgetIds(i.provider);
                new CheckItemsWidget().onUpdate(this, appWidgetManager, a);
            }
        }
    }

    // used at startup, otherwise use getSelectedListId
    private int getLastUsedListFromPrefs() {

        SharedPreferences sp = getSharedPreferences(
                "org.openintents.shopping_preferences", MODE_PRIVATE);

        return sp.getInt(PreferenceActivity.PREFS_LASTUSED, 1);
    }

    private void initFromPreferences() {

        SharedPreferences sp = getSharedPreferences(
                "org.openintents.shopping_preferences", MODE_PRIVATE);

        if (mItemsView != null) {
            // UGLY WORKAROUND:
            // On screen orientation changes, fillItems() is called twice.
            // That is why we have to set the list position twice.
            // Nov14: Seems to not be required anymore, so change to 1 to
            // avoid unwanted scrolling when marking items.
            mItemsView.mUpdateLastListPosition = 1;

            mItemsView.mLastListPosition = sp.getInt(
                    PreferenceActivity.PREFS_LASTLIST_POSITION, 0);
            mItemsView.mLastListTop = sp.getInt(
                    PreferenceActivity.PREFS_LASTLIST_TOP, 0);

            if (debug) {
                Log.d(TAG, "Load list position: pos: "
                        + mItemsView.mLastListPosition + ", top: "
                        + mItemsView.mLastListTop);
            }

            // selected list must be set after changing ordering
            String sortOrder = PreferenceActivity
                    .getShoppingListSortOrderFromPrefs(this);
            if (!mSortOrder.equals(sortOrder)) {
                mSortOrder = sortOrder;
                fillListFilter();
                setSelectedListId(getLastUsedListFromPrefs());
            } else if (getSelectedListId() == -1) {
                setSelectedListId(getLastUsedListFromPrefs());
            }
        }

        if (sp.getBoolean(PreferenceActivity.PREFS_SCREENLOCK,
                PreferenceActivity.PREFS_SCREENLOCK_DEFAULT)) {
            getWindow()
                    .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        boolean nowEditingFilter = sp.getBoolean(
                PreferenceActivity.PREFS_USE_FILTERS,
                PreferenceActivity.PREFS_USE_FILTERS_DEFAULT);
        if (mStoresFilterButton != null && mEditingFilter != nowEditingFilter) {
            updateFilterWidgets();
            fillItems(false);
        }

        if (PreferenceActivity.getCompletionSettingChanged(this)) {
            fillAutoCompleteTextViewAdapter(mEditText);
        }
    }

    private void registerSensor() {
        if (!mUseSensor) {
            // Don't use sensors
            return;
        }

        if (mItemsView.inShopMode()) {
            registerAcceleratorSensor();
        }

    }

    private void registerAcceleratorSensor() {
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }
        mSensorManager.registerListener(mMySensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterSensor() {
        if (mSensorManager != null) {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mSensorManager.unregisterListener(mMySensorListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((ShoppingApplication) getApplication()).dependencies().onResumeShoppingActivity(this);

        // Reload preferences, in case something changed
        initFromPreferences();

        mIsActive = true;

        this.setRequestedOrientation(PreferenceActivity
                .getOrientationFromPrefs(this));

        if (getSelectedListId() != -1) {
            setListTheme(loadListTheme());
            applyListTheme();
            updateTitle();
        }
        mItemsView.onResume();

        mEditText
                .setKeyListener(PreferenceActivity
                        .getCapitalizationKeyListenerFromPrefs(getApplicationContext()));

        if (!mUpdating) {
            mUpdating = true;
            // mHandler.sendMessageDelayed(mHandler.obtainMessage(
            // MESSAGE_UPDATE_CURSORS), mUpdateInterval);
        }


        if (mItemsFromExtras.hasItems()) {
            mItemsFromExtras.insertInto(this, mItemsView);
        }

        // Items received through intents are added in
        // fillItems().

        registerSensor();

        if (debug) {
            Log.i(TAG, "Shopping list onResume() finished");
        }
    }

    private void updateTitle() {
        // Modify our overall title depending on the mode we are running in.
        // In most cases, "title" is the name of the current list, and subtitle
        // depends on the mode -- shopping vs pick items, for example.

        mTitle = getCurrentListName();
        mSubTitle = getText(R.string.app_name);

        if (mState == STATE_MAIN || mState == STATE_VIEW_LIST) {
            if (PreferenceActivity
                    .getPickItemsInListFromPrefs(getApplicationContext())) {
                // 2 different modes
                if (mItemsView.inShopMode()) {
                    mSubTitle = getString(R.string.menu_start_shopping);
                    registerSensor();
                } else {
                    mSubTitle = getString(R.string.menu_pick_items);
                    unregisterSensor();
                }
            }
        } else if ((mState == STATE_PICK_ITEM)
                || (mState == STATE_GET_CONTENT_ITEM)) {
            mSubTitle = (getText(R.string.pick_item));
            setTitleColor(0xFFAAAAFF);
        }

        getSupportActionBar().setTitle(mTitle);
        getSupportActionBar().setSubtitle(mSubTitle);

        // also update the button label
        updateButton();
    }

    private void updateButton() {
        if (mItemsView.inAddItemsMode()) {
            String newItem = mEditText.getText().toString();
            if (TextUtils.isEmpty(newItem)) {
                // If in "add items" mode and the text field is empty,
                // set the button text to "Shopping"
                mButton.setText(R.string.menu_start_shopping);
            } else {
                mButton.setText(R.string.add);
            }
        } else {
            mButton.setText(R.string.add);
        }
    }

    private void saveActiveList(boolean savePos) {
        SharedPreferences sp = getSharedPreferences(
                "org.openintents.shopping_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        // need to do something about the fact that the spinner is driving this
        // even though it isn't always used. also the long vs int is fishy.
        // but for now, just don't overwrite previous setting with -1.
        int list_id = new Long(getSelectedListId()).intValue();
        if (list_id != -1) {
            editor.putInt(PreferenceActivity.PREFS_LASTUSED, list_id);
        }
        // Save position and pixel position of first visible item
        // of current shopping list
        int listposition = mItemsView.getFirstVisiblePosition();
        if (savePos) {
            View v = mItemsView.getChildAt(0);
            int listtop = (v == null) ? 0 : v.getTop();
            if (debug) {
                Log.d(TAG, "Save list position: pos: " + listposition + ", top: "
                        + listtop);
            }

            editor.putInt(PreferenceActivity.PREFS_LASTLIST_POSITION, listposition);
            editor.putInt(PreferenceActivity.PREFS_LASTLIST_TOP, listtop);
        }
        editor.commit();
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
        if (debug) {
            Log.i(TAG, "Shopping list onPause()");
        }
        if (debug) {
            Log.i(TAG, "Spinner: onPause: " + mIsActive);
        }
        mIsActive = false;
        if (debug) {
            Log.i(TAG, "Spinner: onPause: " + mIsActive);
        }

        unregisterSensor();

        saveActiveList(true);
        // TODO ???
        /*
         * // Unregister refresh intent receiver
         * unregisterReceiver(mIntentReceiver);
         */

        mItemsView.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        if (toggleBoughtInputMethod != null) {
            toggleBoughtInputMethod.release();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (debug) {
            Log.i(TAG, "Shopping list onSaveInstanceState()");
        }

        // Save original text from edit box
        String s = mEditText.getText().toString();
        outState.putString(ORIGINAL_ITEM, s);

        outState.putString(BUNDLE_ITEM_URI, mItemUri.toString());
        if (mRelationUri != null) {
            outState.putString(BUNDLE_RELATION_URI, mRelationUri.toString());
        }
        // When app is stopped, it will be thrown out of search mode; thus if we are in search mode,
        // we need to ensure the app resumes in the original mode it was in before entering search.
        final int saveMode = mItemsView.getInSearch() ? mItemsView.mModeBeforeSearch : mItemsView.getMode();
        outState.putInt(BUNDLE_MODE, saveMode);
        outState.putInt(BUNDLE_MODE_BEFORE_SEARCH, saveMode);
        mUpdating = false;

        // after items have been added through an "insert from extras" the
        // action name should be different to avoid duplicate inserts e.g. on
        // rotation.
        if (mItemsFromExtras.hasBeenInserted()
                && GeneralIntents.ACTION_INSERT_FROM_EXTRAS.equals(getIntent()
                .getAction())) {
            setIntent(getIntent().setAction(Intent.ACTION_VIEW));
        }
    }

    /**
     * Hook up buttons, lists, and edittext with functionality.
     */
    private void createView() {

        // Temp-create either Spinner or List based upon the Display
        createList();

        mAddPanel = findViewById(R.id.add_panel);
        mEditText = (AutoCompleteTextView) findViewById(R.id.autocomplete_add_item);

        fillAutoCompleteTextViewAdapter(mEditText);
        mEditText.setThreshold(1);
        mEditText.setOnKeyListener(new OnKeyListener() {

            private int mLastKeyAction = KeyEvent.ACTION_UP;

            public boolean onKey(View v, int keyCode, KeyEvent key) {
                // Shortcut: Instead of pressing the button,
                // one can also press the "Enter" key.
                if (debug) {
                    Log.i(TAG, "Key action: " + key.getAction());
                }
                if (debug) {
                    Log.i(TAG, "Key code: " + keyCode);
                }
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
                return false;
            }
        });
        mEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (mItemsView.inAddItemsMode()) {
                    // small optimization: Only care about updating
                    // the button label on each key pressed if we
                    // are in "add items" mode.
                    updateButton();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

        });

        mButton = (Button) findViewById(R.id.button_add_item);
        mButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                insertNewItem();
            }
        });
        mButton.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                if (PreferenceActivity
                        .getAddForBarcode(getApplicationContext()) == false) {
                    if (debug) {
                        Log.v(TAG,
                                "barcode scanner on add button long click disabled");
                    }
                    return false;
                }

                Intent intent = new Intent();
                intent.setData(mListUri);
                intent.setClassName("org.openintents.barcodescanner",
                        "org.openintents.barcodescanner.BarcodeScanner");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                try {
                    startActivityForResult(intent,
                            REQUEST_CODE_CATEGORY_ALTERNATIVE);
                } catch (ActivityNotFoundException e) {
                    if (debug) {
                        Log.v(TAG, "barcode scanner not found");
                    }
                    showDialog(DIALOG_GET_FROM_MARKET);
                    return false;
                }

                // Instead of calling the class of barcode
                // scanner directly, a more generic approach would
                // be to use a general activity picker.
                //
                // TODO: Implement onActivityResult.
                // Problem: User has to pick activity every time.
                // Choice should be storeable in Stettings.
                // Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                // intent.setData(mListUri);
                // intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                //
                // Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
                // pickIntent.putExtra(Intent.EXTRA_INTENT, intent);
                // pickIntent.putExtra(Intent.EXTRA_TITLE,
                // getText(R.string.title_select_item_from));
                // try {
                // startActivityForResult(pickIntent,
                // REQUEST_CODE_CATEGORY_ALTERNATIVE);
                // } catch (ActivityNotFoundException e) {
                // Log.v(TAG, "barcode scanner not found");
                // return false;
                // }
                return true;
            }
        });

        mLayoutParamsItems = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mItemsView = (ShoppingItemsView) findViewById(R.id.list_items);
        mItemsView.setThemedBackground(findViewById(R.id.background));
        mItemsView.setCustomClickListener(this);
        mItemsView.setOnModeChangeListener(this);
        mItemsView.initTotals();

        mItemsView.setItemsCanFocus(true);
        mItemsView.setDragListener(new DragListener() {

            @Override
            public void drag(int from, int to) {
                if (debug) {
                    Log.v("DRAG", "" + from + "/" + to);
                }

            }
        });
        mItemsView.setDropListener(new DropListener() {

            @Override
            public void drop(int from, int to) {
                if (debug) {
                    Log.v("DRAG", "" + from + "/" + to);
                }

            }
        });

        mItemsView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView parent, View v, int pos, long id) {
                Cursor c = (Cursor) parent.getItemAtPosition(pos);
                onCustomClick(c, pos, EditItemDialog.FieldType.ITEMNAME, v);
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
                                R.string.menu_item_stores)
                                .setShortcut('3', 's');
                        contextmenu.add(0, MENU_REMOVE_ITEM_FROM_LIST, 0,
                                R.string.menu_remove_item)
                                .setShortcut('4', 'r');
                        contextmenu.add(0, MENU_COPY_ITEM, 0,
                                R.string.menu_copy_item).setShortcut('5', 'c');
                        contextmenu.add(0, MENU_DELETE_ITEM, 0,
                                R.string.menu_delete_item)
                                .setShortcut('6', 'd');
                        contextmenu.add(0, MENU_MOVE_ITEM, 0,
                                R.string.menu_move_item).setShortcut('7', 'l');
                    }

                });
    }

    private void createList() {

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mTitle = mDrawerTitle = getTitle();
        if (mDrawerLayout != null) {
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

                /**
                 * Called when a drawer has settled in a completely closed state.
                 */
                public void onDrawerClosed(View view) {
                    getSupportActionBar().setTitle(mTitle);
                    getSupportActionBar().setSubtitle(mSubTitle);
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                /**
                 * Called when a drawer has settled in a completely open state.
                 */
                public void onDrawerOpened(View drawerView) {
                    getSupportActionBar().setTitle(mDrawerTitle);
                    getSupportActionBar().setSubtitle(null);
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }
            };

            // Set the drawer toggle as the DrawerListener
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

        mDrawerListsView = (ListView) findViewById(R.id.left_drawer);


        mShoppingListsView = (Spinner) findViewById(R.id.spinner_listfilter);
        mShoppingListsView
                .setOnItemSelectedListener(new OnItemSelectedListener() {
                    public void onItemSelected(AdapterView parent, View v,
                                               int position, long id) {
                        if (debug) {
                            Log.d(TAG, "Spinner: onItemSelected");
                        }

                        // Update list cursor:
                        getSelectedListId();

                        // Set the theme based on the selected list:
                        setListTheme(loadListTheme());

                        // If it's the same list we had before, requery only
                        // if a preference has changed since then.
                        fillItems(id == mItemsView.getListId());

                        updateTitle();

                        // Apply the theme after the list has been filled:
                        applyListTheme();
                    }

                    public void onNothingSelected(AdapterView arg0) {
                        if (debug) {
                            Log.d(TAG, "Spinner: onNothingSelected: "
                                    + mIsActive);
                        }
                        if (mIsActive) {
                            fillItems(false);
                        }
                    }
                });

        mShoppingListsFilterButton = (Button) findViewById(R.id.listfilter);
        if (mShoppingListsFilterButton != null) {
            mShoppingListsFilterButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    showListFilter(v);
                }
            });
        }


        mStoresFilterButton = (Button) findViewById(R.id.storefilter);
        mStoresFilterButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showStoresFilter(v);
            }
        });
        mTagsFilterButton = (Button) findViewById(R.id.tagfilter);
        mTagsFilterButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showTagsFilter(v);
            }
        });
    }

    protected void showListFilter(final View v) {
        QuickSelectMenu popup = new QuickSelectMenu(this, v);
        int i_list;

        Menu menu = popup.getMenu();
        if (menu == null) {
            return;
        }

        // get the list of lists
        mCursorShoppingLists.requery();
        int count = mCursorShoppingLists.getCount();
        mCursorShoppingLists.moveToFirst();
        for (i_list = 0; i_list < count; i_list++) {
            String name = mCursorShoppingLists.getString(mStringListFilterNAME);
            menu.add(0, i_list, Menu.NONE, name);
            mCursorShoppingLists.moveToNext();
        }

        popup.setOnItemSelectedListener(new QuickSelectMenu.OnItemSelectedListener() {
            public void onItemSelected(CharSequence name, int pos) {
                setSelectedListPos(pos);
            }
        });

        popup.show();
    }

    protected void showStoresFilter(final View v) {
        QuickSelectMenu popup = new QuickSelectMenu(this, v);

        Menu menu = popup.getMenu();
        if (menu == null) {
            return;
        }
        Cursor c = getContentResolver()
                .query(Stores.QUERY_BY_LIST_URI.buildUpon()
                                .appendPath(this.mListUri.getLastPathSegment()).build(),
                        new String[]{Stores._ID, Stores.NAME}, null, null,
                        "stores.name COLLATE NOCASE ASC"
                );
        int i_store, count = c.getCount();
        if (count == 0) {
            Toast.makeText(this, R.string.no_stores_available,
                    Toast.LENGTH_SHORT).show();
        }

        // prepend the "no filter" option
        menu.add(0, -1, Menu.NONE, R.string.unfiltered);

        // get the list of stores
        c.moveToFirst();
        for (i_store = 0; i_store < count; i_store++) {
            long id = c.getLong(0);
            String name = c.getString(1);
            menu.add(0, (int) id, Menu.NONE, name);
            c.moveToNext();
        }
        c.deactivate();
        c.close();

        popup.setOnItemSelectedListener(new QuickSelectMenu.OnItemSelectedListener() {
            public void onItemSelected(CharSequence name, int id) {
                // set the selected store filter
                // update the filter summary? not until filter region collapsed.
                ContentValues values = new ContentValues();
                values.put(Lists.STORE_FILTER, (long) id);
                getContentResolver().update(mListUri, values, null, null);
                if (id == -1) {
                    ((Button) v).setText(R.string.stores);
                } else {
                    ((Button) v).setText(name);
                }
                fillItems(false);
            }
        });

        popup.show();
    }

    protected void showTagsFilter(final View v) {
        QuickSelectMenu popup = new QuickSelectMenu(this, v);

        Menu menu = popup.getMenu();
        if (menu == null) {
            return;
        }
        String[] tags = getTaglist(mListUri.getLastPathSegment());
        int i_tag, count = tags.length;

        if (count == 0) {
            Toast.makeText(this, R.string.no_tags_available, Toast.LENGTH_SHORT)
                    .show();
        }

        // prepend the "no filter" option
        menu.add(0, -1, Menu.NONE, R.string.unfiltered);

        for (i_tag = 0; i_tag < count; i_tag++) {
            menu.add(tags[i_tag]);
        }

        popup.setOnItemSelectedListener(new QuickSelectMenu.OnItemSelectedListener() {
            public void onItemSelected(CharSequence name, int id) {
                // set the selected tags filter
                ContentValues values = new ContentValues();
                values.put(Lists.TAGS_FILTER, id == -1 ? "" : (String) name);
                getContentResolver().update(mListUri, values, null, null);
                if (id == -1) {
                    ((Button) v).setText(R.string.tags);
                } else {
                    ((Button) v).setText(name);
                }
                fillItems(false);
            }
        });

        popup.show();
    }

    protected void updateFilterWidgets() {
        mEditingFilter = PreferenceActivity.getUsingFiltersFromPrefs(this);

        mStoresFilterButton.setVisibility(mEditingFilter ? View.VISIBLE : View.GONE);
        mTagsFilterButton.setVisibility(mEditingFilter ? View.VISIBLE : View.GONE);

        if (mShoppingListsFilterButton != null) {
            mShoppingListsFilterButton
                    .setVisibility(View.GONE);
        }
        // spinner goes the opposite way
        if (mShoppingListsView != null) {
            mShoppingListsView.setVisibility(View.GONE);
        }

        if (mEditingFilter) {
            String storeName = ShoppingUtils.getListFilterStoreName(this,
                    mListUri);
            if (storeName != null) {
                mStoresFilterButton.setText(storeName);
            } else {
                mStoresFilterButton.setText(R.string.stores);
            }
            String tagFilter = ShoppingUtils.getListTagsFilter(this, mListUri);
            if (tagFilter != null) {
                mTagsFilterButton.setText(tagFilter);
            } else {
                mTagsFilterButton.setText(R.string.tags);
            }
        }
    }

    public void onCustomClick(Cursor c, int pos,
                              EditItemDialog.FieldType field, View clicked_view) {
        if (mState == STATE_PICK_ITEM) {
            pickItem(c);
        } else {
            if (mItemsView.mShowCheckBox) {
                boolean handled = false;
                // In default theme, there is an extra check box,
                // so clicking on anywhere else means to edit the
                // item.

                if (field == EditItemDialog.FieldType.PRICE
                        && PreferenceActivity
                        .getUsingPerStorePricesFromPrefs(this))
                // should really be a per-list preference
                {
                    editItemStores(pos);
                    handled = true;
                }

                if ((field == EditItemDialog.FieldType.PRIORITY || field == EditItemDialog.FieldType.QUANTITY)
                        && PreferenceActivity.getQuickEditModeFromPrefs(this)) {
                    handled = QuickEditFieldPopupMenu(c, pos, field,
                            clicked_view);
                }

                if (!handled) {
                    editItem(pos, field);
                }
            } else {
                // For themes without a checkbox, clicking anywhere means
                // to toggle the item.
                mItemsView.toggleItemBought(pos);
            }
        }
    }

    // Menu

    private boolean QuickEditFieldPopupMenu(final Cursor c, final int pos,
                                            final FieldType field, View v) {
        QuickSelectMenu popup = new QuickSelectMenu(this, v);

        Menu menu = popup.getMenu();
        if (menu == null) {
            return false;
        }
        menu.add("1");
        menu.add("2");
        menu.add("3");
        menu.add("4");
        menu.add("5");
        if (field == FieldType.QUANTITY) {
            menu.add(R.string.otherqty);
        }
        if (field == FieldType.PRIORITY) {
            menu.add(R.string.otherpri);
        }

        popup.setOnItemSelectedListener(new QuickSelectMenu.OnItemSelectedListener() {
            public void onItemSelected(CharSequence name, int id) {
                // TODO: use a flavor of menu.add which takes id,
                // then identifying the selection becomes easier here.

                if (name.length() > 1) {
                    // Other ... use edit dialog
                    editItem(pos, field);
                } else {
                    long number = name.charAt(0) - '0';
                    ContentValues values = new ContentValues();
                    switch (field) {
                        case PRIORITY:
                            values.put(Contains.PRIORITY, number);
                            break;
                        case QUANTITY:
                            values.put(Contains.QUANTITY, number);
                            break;
                        default:
                            break;
                    }
                    mItemsView.mCursorItems.moveToPosition(pos);
                    String containsId = mItemsView.mCursorItems
                            .getString(mStringItemsCONTAINSID);
                    Uri uri = Uri.withAppendedPath(
                            ShoppingContract.Contains.CONTENT_URI, containsId);
                    getApplicationContext().getContentResolver().update(uri,
                            values, null, null);
                    onItemChanged(); // probably overkill
                    mItemsView.updateTotal();
                }
            }
        });

        popup.show();
        return true;
    }

    /**
     * Inserts new item from edit box into currently selected shopping list.
     */
    private void insertNewItem() {
        String newItem = mEditText.getText().toString().trim();

        // Only add if there is something to add:
        if (newItem.length() > 0) {
            long listId = getSelectedListId();
            if (listId < 0) {
                // No valid list - probably view is not active
                // and no item is selected.
                return;
            }
            mItemsView.insertNewItem(this, newItem, null, null, null, null);
            mEditText.setText("");
            fillAutoCompleteTextViewAdapter(mEditText);
        } else {
            // Open list to select item from
            pickItems();
        }
    }

    /**
     * Picks an item and returns to calling activity.
     */
    private void pickItem(Cursor c) {
        long itemId = c.getLong(mStringItemsITEMID);
        Uri url = ContentUris.withAppendedId(
                ShoppingContract.Items.CONTENT_URI, itemId);

        Intent intent = new Intent();
        intent.setData(url);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        /*
         * int MENU_ACTION_WITH_TEXT=0;
         *
         * //Temp- for backward compatibility with OS 3 features
         *
         * if(!usingListSpinner()){ try{ //setting the value equivalent to
         * desired expression
         * //MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT
         * java.lang.reflect.Field
         * field=MenuItem.class.getDeclaredField("SHOW_AS_ACTION_IF_ROOM");
         * MENU_ACTION_WITH_TEXT=field.getInt(MenuItem.class);
         * field=MenuItem.class.getDeclaredField("SHOW_AS_ACTION_WITH_TEXT");
         * MENU_ACTION_WITH_TEXT|=field.getInt(MenuItem.class); }catch(Exception
         * e){ //reset value irrespective of cause MENU_ACTION_WITH_TEXT=0; }
         *
         * }
         */

        // Add menu option for auto adding items from string array in intent
        // extra if they exist
        if (mItemsFromExtras.hasItems()) {
            menu.add(0, MENU_INSERT_FROM_EXTRAS, 0, R.string.menu_auto_add)
                    .setIcon(android.R.drawable.ic_menu_upload);
        }

        MenuItem item;

        View searchView = mItemsView.getSearchView();
        if (searchView != null) {
            item = menu.add(0, MENU_SEARCH_ADD, 0, R.string.menu_search_add);
            MenuItemCompat.setActionView(item, searchView);
            MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        mAddPanel.setVisibility(searchView == null ? View.VISIBLE : View.GONE);

        item = menu.add(0, MENU_SORT_LIST, 0, R.string.menu_sort_list)
                .setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_IF_ROOM
                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        if (mListSortActionProvider == null) {
            mListSortActionProvider = new ListSortActionProvider(this);
        }
        MenuItemCompat.setActionProvider(item, mListSortActionProvider);

        // Standard menu

        // tentatively moved "new list" to drawer
        //item = menu.add(0, MENU_NEW_LIST, 0, R.string.new_list)
        //		.setIcon(R.drawable.ic_menu_add_list).setShortcut('0', 'n');
        // MenuCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(0, MENU_CLEAN_UP_LIST, 0, R.string.clean_up_list)
                .setIcon(R.drawable.ic_menu_cleanup).setShortcut('1', 'c');
        MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_IF_ROOM
                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(0, MENU_PICK_ITEMS, 0, R.string.menu_pick_items)
                .setIcon(android.R.drawable.ic_menu_add).setShortcut('2', 'p').
                // tentatively replaced by buttons in drawer.
                        setVisible(false);

        /*
         * menu.add(0, MENU_SHARE, 0, R.string.share)
         * .setIcon(R.drawable.contact_share001a) .setShortcut('4', 's');
         */

        menu.add(0, MENU_THEME, 0, R.string.theme)
                .setIcon(android.R.drawable.ic_menu_manage)
                .setShortcut('3', 't');

        menu.add(0, MENU_PREFERENCES, 0, R.string.preferences)
                .setIcon(android.R.drawable.ic_menu_preferences)
                .setShortcut('4', 'p');

        menu.add(0, MENU_RENAME_LIST, 0, R.string.rename_list)
                .setIcon(android.R.drawable.ic_menu_edit).setShortcut('5', 'r');

        menu.add(0, MENU_DELETE_LIST, 0, R.string.delete_list).setIcon(
                android.R.drawable.ic_menu_delete);

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

        menu.add(0, MENU_UNMARK_ALL_ITEMS, 0, R.string.unmark_all_items);

        menu.add(0, MENU_SYNC_WEAR, 0, R.string.sync_wear);

        menu.add(0, MENU_CONVERT_CSV, 0, R.string.convert_csv);

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
        Intent intent = new Intent(Intent.ACTION_PICK, Uri.parse("geo:"));
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
        if (debug) {
            Log.d(TAG, "Pick location possible: " + pick_location_possible);
        }
        if (debug) {
            Log.d(TAG, "View alerts possible: " + view_alerts_possible);
        }
        return pick_location_possible && view_alerts_possible;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        boolean drawerOpen = mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawerListsView);
        boolean holoSearch = PreferenceActivity.getUsingHoloSearchFromPrefs(this);

        // Add menu option for auto adding items from string array in intent
        // extra if they exist
        if (mItemsFromExtras.hasBeenInserted()) {
            menu.removeItem(MENU_INSERT_FROM_EXTRAS);
        }

        // Selected list:
        long listId = getSelectedListId();

        // set menu title for change mode
        MenuItem menuItem = menu.findItem(MENU_PICK_ITEMS);

        if (mItemsView.inAddItemsMode()) {
            menuItem.setTitle(R.string.menu_start_shopping);
            menuItem.setIcon(android.R.drawable.ic_menu_myplaces);
        } else {
            menu.findItem(MENU_PICK_ITEMS).setTitle(R.string.menu_pick_items);
            menuItem.setIcon(android.R.drawable.ic_menu_add);
        }

        menuItem = menu.findItem(MENU_SEARCH_ADD);
        if (menuItem != null) {
            menuItem.setVisible(holoSearch && !drawerOpen);
            if (!holoSearch) {
                mAddPanel.setVisibility(View.VISIBLE);
            }

            View searchView = menuItem.getActionView();
            int searchImgId = getResources().getIdentifier("android:id/search_button", null, null);
            View imageView = searchView.findViewById(searchImgId);
            if (imageView instanceof ImageView) {
                ((ImageView) imageView).setImageResource(android.R.drawable.ic_menu_add);
            }

        }

        menuItem = menu.findItem(MENU_SYNC_WEAR);
        if (menuItem != null) {
            menuItem.setVisible(mItemsView.isWearSupportAvailable());
        }

        menu.findItem(MENU_MARK_ALL_ITEMS).setVisible(mItemsView.mNumUnchecked > 0);
        menu.findItem(MENU_UNMARK_ALL_ITEMS).setVisible(mItemsView.mNumChecked > 0);

        menu.findItem(MENU_CLEAN_UP_LIST).setEnabled(
                mItemsView.mNumChecked > 0).setVisible(!drawerOpen);


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
                new ComponentName(this,
                        org.openintents.shopping.ShoppingActivity.class), null,
                intent, 0, null
        );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (debug) {
            Log.d(TAG, "onOptionsItemSelected getItemId: " + item.getItemId());
        }
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
                pickItems();
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
                mItemsFromExtras.insertInto(this, mItemsView);
                return true;
            case MENU_MARK_ALL_ITEMS:
                mItemsView.toggleAllItems(true);
                return true;
            case MENU_UNMARK_ALL_ITEMS:
                mItemsView.toggleAllItems(false);
                return true;
            case MENU_SYNC_WEAR:
                mItemsView.pushItemsToWear();
                return true;
            case MENU_CONVERT_CSV:
                startActivity(new Intent(this, ConvertCsvActivity.class).setData(mListUri));
                return true;
            default:
                break;
        }
        if (debug) {
            Log.d(TAG, "Start intent group id : " + item.getGroupId());
        }
        if (Menu.CATEGORY_ALTERNATIVE == item.getGroupId()) {
            // Start alternative cateogory intents with option to return a
            // result.
            if (debug) {
                Log.d(TAG, "Start alternative intent for : "
                        + item.getIntent().getDataString());
            }
            startActivityForResult(item.getIntent(),
                    REQUEST_CODE_CATEGORY_ALTERNATIVE);
            return true;
        }
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    // /////////////////////////////////////////////////////
    //
    // Menu functions
    //

    /**
     *
     */
    private void pickItems() {
        if (PreferenceActivity
                .getPickItemsInListFromPrefs(getApplicationContext())) {
            if (mItemsView.inShopMode()) {
                mItemsView.setAddItemsMode();
            } else {
                mItemsView.setInShopMode();
            }
            onModeChanged();
        } else {
            if (!mItemsView.inShopMode()) {
                mItemsView.setInShopMode();
                onModeChanged();
            }
            pickItemsUsingDialog();
        }
    }

    private void pickItemsUsingDialog() {
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
            case MENU_COPY_ITEM:
                copyItem(menuInfo.position);
                break;
            case MENU_ITEM_STORES:
                editItemStores(menuInfo.position);
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * Creates a new list from dialog.
     *
     * @return true if new list was created. False if new list was not created,
     * because user has not given any name.
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
        setListTheme(previousTheme);
        applyListTheme();

        return true;
    }

    private void setListTheme(String theme) {
        mItemsView.setListTheme(theme);
    }

    private void applyListTheme() {
        mItemsView.applyListTheme();

        // In Holo themes, apply the theme text color also to the
        // input box and the button, because the background is
        // semi-transparent.
        mEditText.setTextColor(mItemsView.mTextColor);
        if (mStoresFilterButton != null) {
            mStoresFilterButton.setTextColor(mItemsView.mTextColor);
        }
        if (mTagsFilterButton != null) {
            mTagsFilterButton.setTextColor(mItemsView.mTextColor);
        }
        if (mShoppingListsFilterButton != null) {
            mShoppingListsFilterButton.setTextColor(mItemsView.mTextColor);
        }
        if (mShoppingListsView instanceof Spinner) {
            View view = mShoppingListsView.getChildAt(0);
            setSpinnerTextColorInHoloTheme(view);
        }
    }

    /**
     * For holo themes with transparent widgets, set font color of the spinner
     * using theme color.
     *
     * @param view
     */
    private void setSpinnerTextColorInHoloTheme(View view) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(mItemsView.mTextColor);
        }
    }

    // TODO: Convert into proper dialog that remains across screen orientation
    // changes.

    /**
     * Rename list from dialog.
     *
     * @return true if new list was renamed. False if new list was not renamed,
     * because user has not given any name.
     */
    private boolean renameList(String newName) {

        if (newName.equals("")) {
            // User has not provided any name
            Toast.makeText(this, getString(R.string.please_enter_name),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // Rename currently selected list:
        long listId = getSelectedListId();

        ContentValues values = new ContentValues();
        values.put(Lists.NAME, "" + newName);
        getContentResolver().update(mListUri, values, null, null
        );

        //
        mCursorShoppingLists.requery();
        setSelectedListId((int) listId);
        updateTitle();
        return true;
    }

    private void sendList() {
        if (mItemsView.getAdapter() instanceof CursorAdapter) {
            StringBuilder sb = new StringBuilder();
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
                    Toast.LENGTH_SHORT).show();
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
                .setTitle(R.string.confirm_delete_list)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // click Ok
                                deleteList();
                            }
                        }
                )
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // click Cancel
                            }
                        }
                )
                // .create()
                .show();
    }

    /**
     * Deletes currently selected shopping list.
     */
    private void deleteList() {
        String listId = mCursorShoppingLists.getString(0);
        ShoppingUtils.deleteList(this, listId);

        // Update view
        fillListFilter();

        getSelectedListId();

        // Set the theme based on the selected list:
        setListTheme(loadListTheme());

        fillItems(false);
        applyListTheme();
        updateTitle();
    }

    /**
     * Mark item
     */
    void markItem(int position) {
        mItemsView.toggleItemBought(position);
    }

    /**
     * Edit item
     *
     * @param field
     */
    void editItem(long itemId, long containsId, EditItemDialog.FieldType field) {
        mItemUri = Uri.withAppendedPath(ShoppingContract.Items.CONTENT_URI, Long.toString(itemId));
        mListItemUri = Uri.withAppendedPath(mListUri, Long.toString(itemId));
        mRelationUri = Uri.withAppendedPath(
                ShoppingContract.Contains.CONTENT_URI, Long.toString(containsId));
        mEditItemFocusField = field;

        showDialog(DIALOG_EDIT_ITEM);
    }

    /**
     * Edit item
     *
     * @param field
     */
    void editItem(int position, EditItemDialog.FieldType field) {
        if (debug) {
            Log.d(TAG, "EditItems: Position: " + position);
        }
        mItemsView.mCursorItems.moveToPosition(position);
        // mEditItemPosition = position;

        long itemId = mItemsView.mCursorItems.getLong(mStringItemsITEMID);
        long containsId = mItemsView.mCursorItems
                .getLong(mStringItemsCONTAINSID);

        editItem(itemId, containsId, field);
    }

    void editItemStores(int position) {
        if (debug) {
            Log.d(TAG, "EditItemStores: Position: " + position);
        }

        mItemsView.mCursorItems.moveToPosition(position);
        // mEditItemPosition = position;
        long itemId = mItemsView.mCursorItems.getLong(mStringItemsITEMID);

        Intent intent;
        intent = new Intent(this, ItemStoresActivity.class);
        intent.setData(mListUri.buildUpon().appendPath(String.valueOf(itemId))
                .build());
        startActivity(intent);
    }

    /**
     * delete item
     */
    void deleteItemDialog(int position) {
        if (debug) {
            Log.d(TAG, "EditItems: Position: " + position);
        }
        mItemsView.mCursorItems.moveToPosition(position);
        mDeleteItemPosition = position;

        showDialog(DIALOG_DELETE_ITEM);
    }

    /**
     * delete item
     */
    void deleteItem(int position) {
        Cursor c = mItemsView.mCursorItems;
        c.moveToPosition(position);

        String listId = mListUri.getLastPathSegment();
        String itemId = c.getString(mStringItemsITEMID);
        ShoppingUtils.deleteItem(this, itemId, listId);

        // c.requery();
        mItemsView.requery();
        fillAutoCompleteTextViewAdapter(mEditText);
    }

    /**
     * move item
     */
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

        // Attach item to new list, preserving all other fields
        String containsId = c.getString(mStringItemsCONTAINSID);
        ContentValues cv = new ContentValues(1);
        cv.put(Contains.LIST_ID, targetListId);
        getContentResolver().update(
                Uri.withAppendedPath(Contains.CONTENT_URI, containsId), cv,
                null, null);

        mItemsView.requery();
    }

    /**
     * copy item
     */
    void copyItem(int position) {
        Cursor c = mItemsView.mCursorItems;
        mItemsView.mCursorItems.requery();
        c.moveToPosition(position);
        String containsId = c.getString(mStringItemsCONTAINSID);
        Long newContainsId;
        Long newItemId;

        c = getContentResolver().query(
                Uri.withAppendedPath(
                        Uri.withAppendedPath(Contains.CONTENT_URI, "copyof"),
                        containsId), new String[]{"item_id", "contains_id"},
                null, null, null
        );

        if (c.getCount() != 1) {
            return;
        }

        c.moveToFirst();
        newItemId = c.getLong(0);
        newContainsId = c.getLong(1);
        c.deactivate();
        c.close();

        editItem(newItemId, newContainsId, FieldType.ITEMNAME);

        // mItemsView.requery();
    }

    /**
     * removeItemFromList
     */
    void removeItemFromList(int position) {
        Cursor c = mItemsView.mCursorItems;
        c.moveToPosition(position);
        // Remember old values before delete (for share below)
        String itemName = c.getString(mStringItemsITEMNAME);
        long oldstatus = c.getLong(mStringItemsSTATUS);

        // Delete item by changing its state
        ContentValues values = new ContentValues();
        values.put(Contains.STATUS, Status.REMOVED_FROM_LIST);
        if (PreferenceActivity.getResetQuantity(getApplicationContext())) {
            values.put(Contains.QUANTITY, "");
        }
        getContentResolver().update(Contains.CONTENT_URI, values, "_id = ?",
                new String[]{c.getString(mStringItemsCONTAINSID)});

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
        setListTheme(theme);
        applyListTheme();
    }

    @Override
    public void onSetThemeForAll(String theme) {
        setThemeForAll(this, theme);
    }

    /**
     * Loads the theme settings for the currently selected theme.
     * <p/>
     * Up to version 1.2.1, only one of 3 hardcoded themes are available. These
     * are stored in 'skin_background' as '1', '2', or '3'.
     * <p/>
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
        if (mCursorShoppingLists == null) {
            return "1";
        }
        if (mCursorShoppingLists.getPosition() < 0) {
            return "1";
        }

        // mCursorListFilter has been set to correct position
        // by calling getSelectedListId(),
        // so we can read out further elements:
        return mCursorShoppingLists
                .getString(mStringListFilterSKINBACKGROUND);
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
                        mCursorShoppingLists.getString(0)), values, null, null
        );

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
                        }
                );

            case DIALOG_EDIT_ITEM:
                return new EditItemDialog(this, mItemUri, mRelationUri,
                        mListItemUri);

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
                                }
                        )
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        // Don't do anything
                                    }
                                }
                        ).create();

            case DIALOG_THEME:
                return new ThemeDialog(this, this);

            case DIALOG_GET_FROM_MARKET:
                return new DownloadOIAppDialog(this,
                        DownloadOIAppDialog.OI_BARCODESCANNER);
            default:
                break;
        }
        return super.onCreateDialog(id);

    }

    // /////////////////////////////////////////////////////
    //
    // Helper functions
    //

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {

            case DIALOG_RENAME_LIST:
                ((RenameListDialog) dialog).setName(getCurrentListName());
                break;

            case DIALOG_EDIT_ITEM:
                EditItemDialog d = (EditItemDialog) dialog;
                d.setItemUri(mItemUri, mListItemUri);
                d.setRelationUri(mRelationUri);
                d.setFocusField(mEditItemFocusField);

                String[] taglist = getTaglist();
                d.setTagList(taglist);

                d.setOnItemChangedListener(this);
                break;

            case DIALOG_THEME:
                ((ThemeDialog) dialog).prepareDialog();
                break;
            case DIALOG_GET_FROM_MARKET:
                DownloadOIAppDialog.onPrepareDialog(this, dialog);
                break;
            case DIALOG_DELETE_ITEM:
                if (mItemsView != null && dialog instanceof AlertDialog) {
                    ListAdapter adapter = mItemsView.getAdapter();
                    if (adapter != null && adapter instanceof CursorAdapter) {
                        Cursor c = (Cursor) adapter.getItem(mDeleteItemPosition);
                        if (c != null) {
                            String itemName = c.getString(mStringItemsITEMNAME);
                            if (itemName != null) {
                                ((AlertDialog) dialog).setMessage(
                                        getResources().getString(R.string.delete_item_confirm, itemName));
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Returns the ID of the selected shopping list.
     * <p/>
     * As a side effect, the item URI is updated. Returns -1 if nothing is
     * selected.
     *
     * @return ID of selected shopping list.
     */
    long getSelectedListId() {
        int pos = mShoppingListsView.getSelectedItemPosition();
        if (pos < 0) {
            // nothing selected - probably view is out of focus:
            // Do nothing.
            return -1;
        }

        // Obtain Id of currently selected shopping list:
        mCursorShoppingLists.moveToPosition(pos);

        long listId = mCursorShoppingLists.getLong(mStringListFilterID);

        mListUri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                Long.toString(listId));

        getIntent().setData(mListUri);

        return listId;
    }

    /**
     * sets the selected list to a specific list Id
     */
    void setSelectedListId(int id) {
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

    private void setSelectedListPos(int pos) {
        mShoppingListsView.setTag(pos);

        mShoppingListsView.setSelection(pos);

        long id = getSelectedListId();

        // Set the theme based on the selected list:
        setListTheme(loadListTheme());

        if (id != mItemsView.getListId()) {
            fillItems(false);
        }
        applyListTheme();
        updateTitle();
        if (mShoppingListsView instanceof ListView) {
            ((ListView) mShoppingListsView).setItemChecked(pos, true);
        }
    }

    /**
     *
     */
    private void fillListFilter() {
        // Get a cursor with all lists

        mCursorShoppingLists = getContentResolver().query(Lists.CONTENT_URI,
                mStringListFilter, null, null, mSortOrder);
        startManagingCursor(mCursorShoppingLists);

        if (mCursorShoppingLists == null) {
            Log.e(TAG, "missing shopping provider");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    R.layout.list_item_shopping_list,
                    new String[]{getString(R.string.no_shopping_provider)});
            setSpinnerAndDrawerListAdapter(adapter);

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
                if (debug) {
                    Log.i(TAG, "mListContentObserver: Constructor");
                }
            }

            /*
             * (non-Javadoc)
             *
             * @see android.database.ContentObserver#deliverSelfNotifications()
             */
            @Override
            public boolean deliverSelfNotifications() {
                // TODO Auto-generated method stub
                if (debug) {
                    Log.i(TAG, "mListContentObserver: deliverSelfNotifications");
                }
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
                if (debug) {
                    Log.i(TAG, "mListContentObserver: onChange");
                }

                mCursorShoppingLists.requery();

                super.onChange(arg0);
            }

        }

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

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                // Use a template that displays a text view
                R.layout.list_item_shopping_list,
                // Give the cursor to the list adapter
                mCursorShoppingLists, new String[]{Lists.NAME},
                new int[]{R.id.text1});
        setSpinnerAndDrawerListAdapter(adapter);

    }

    public void onModeChanged() {

        if (debug) {
            Log.d(TAG, "onModeChanged()");
        }
        fillItems(false);

        invalidateOptionsMenu();

        updateTitle();
    }

    @Override
    public void updateActionBar() {
        invalidateOptionsMenu();
    }

    private String getCurrentListName() {
        long listId = getSelectedListId();

        // calling getSelectedListId also updates mCursorShoppingLists:
        if (listId >= 0) {
            return mCursorShoppingLists.getString(mStringListFilterNAME);
        } else {
            return "";
        }
    }

    private void fillItems(boolean onlyIfPrefsChanged) {
        if (debug) {
            Log.d(TAG, "fillItems()");
        }

        long listId = getSelectedListId();
        if (listId < 0) {
            // No valid list - probably view is not active
            // and no item is selected.
            Log.d(TAG, "fillItems: listId not availalbe");
            return;
        }

        // Insert any pending items received either through intents
        // or in onActivityResult:
        if (mItemsFromExtras.hasItems()) {
            mItemsFromExtras.insertInto(this, mItemsView);
        }

        updateFilterWidgets();

        if (onlyIfPrefsChanged
                && (lastAppliedPrefChange == PreferenceActivity.updateCount)) {
            return;
        }

        if (debug) {
            Log.d(TAG, "fillItems() for list " + listId);
        }
        lastAppliedPrefChange = PreferenceActivity.updateCount;
        mItemsView.fillItems(this, listId);

        // Also refresh AutoCompleteTextView:
        fillAutoCompleteTextViewAdapter(mEditText);
    }

    /**
     * Fill input field (AutoCompleteTextView) with suggestions: Unique item
     * names from all lists are collected. The adapter is filled in the
     * background.
     */
    public void fillAutoCompleteTextViewAdapter(final AutoCompleteTextView textView) {
        boolean limit_selections = PreferenceActivity.getCompleteFromCurrentListOnlyFromPrefs(this);
        String listId = null;
        if (limit_selections) {
            listId = mListUri.getLastPathSegment();
        }

        // TODO: Optimize: This routine is called too often.
        if (debug) {
            Log.d(TAG, "fill AutoCompleteTextViewAdapter");
        }

        new AsyncTask<String, Integer, ArrayAdapter<String>>() {

            private ArrayAdapter<String> adapter;

            @Override
            protected ArrayAdapter<String> doInBackground(String... params) {
                return fillAutoCompleteAdapter(params[0]);
            }

            @Override
            protected void onPostExecute(ArrayAdapter<String> adapter) {
                if (textView != null) {
                    // use result from doInBackground()
                    textView.setAdapter(adapter);
                }
            }

            private ArrayAdapter<String> fillAutoCompleteAdapter(String listId) {
                // Create list of item names
                Uri uri;
                String retCol;
                if (listId == null) {
                    uri = Items.CONTENT_URI;
                    retCol = Items.NAME;
                } else {
                    uri = Uri.parse("content://org.openintents.shopping/containsfull/list").buildUpon().
                            appendPath(listId).build();
                    retCol = "items.name";
                }
                List<String> autocompleteItems = new LinkedList<>();
                Cursor c = getContentResolver().query(uri, new String[]{retCol}, null, null, retCol + " asc");
                if (c != null) {
                    String lastitem = "";
                    while (c.moveToNext()) {
                        String newitem = c.getString(0);
                        // Only add items if they have not been added previously
                        // (list is sorted)
                        if (!newitem.equals(lastitem)) {
                            autocompleteItems.add(newitem);
                            lastitem = newitem;
                        }
                    }
                    c.close();
                }
                return new ArrayAdapter<>(ShoppingActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        autocompleteItems);
            }

        }.execute(listId);
    }

    /**
     * Create list of tags.
     * <p/>
     * Tags for notes can be comma-separated. Here we create a list of the
     * unique tags.
     *
     * @return
     */
    String[] getTaglist() {
        return getTaglist(null);
    }

    /**
     * Create list of tags.
     * <p/>
     * Tags for notes can be comma-separated. Here we create a list of the
     * unique tags.
     *
     * @param listId
     * @return
     */
    String[] getTaglist(String listId) {
        Cursor c;

        if (listId == null) {
            c = getContentResolver().query(ShoppingContract.Items.CONTENT_URI,
                    new String[]{ShoppingContract.Items.TAGS}, null, null,
                    ShoppingContract.Items.DEFAULT_SORT_ORDER);
        } else {
            Uri uri = Uri.parse("content://org.openintents.shopping/listtags")
                    .buildUpon().appendPath(listId).build();
            c = getContentResolver().query(uri,
                    new String[]{ShoppingContract.ContainsFull.ITEM_TAGS},
                    null, null, null);
        }

        // Create a set of all tags (every tag should only appear once).
        HashSet<String> tagset = new HashSet<>();
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
        ArrayList<String> list = new ArrayList<>();
        list.addAll(tagset);
        // 2. Sort the String list
        Collections.sort(list);
        // 3. Convert it to String array
        return list.toArray(new String[list.size()]);
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

    /**
     * This method is called when the sending activity has finished, with the
     * result it supplied.
     *
     * @param requestCode The original request code as given to startActivity().
     * @param resultCode  From sending activity as per setResult().
     * @param data        From sending activity as per setResult().
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (debug) {
            Log.i(TAG, "ShoppingView: onActivityResult. ");
        }

        if (requestCode == SUBACTIVITY_LIST_SHARE_SETTINGS) {
            if (debug) {
                Log.i(TAG, "SUBACTIVITY_LIST_SHARE_SETTINGS");
            }

            if (resultCode == RESULT_OK) {
                // Broadcast the intent

                Uri uri = data.getData();

                if (!mListUri.equals(uri)) {
                    Log.e(TAG, "Unexpected uri returned: Should be " + mListUri
                            + " but was " + uri);
                    return;
                }

                // TODO ???
                Bundle extras = data.getExtras();

                String sharename = extras
                        .getString(ShoppingContract.Lists.SHARE_NAME);
                String contacts = extras
                        .getString(ShoppingContract.Lists.SHARE_CONTACTS);

                if (debug) {
                    Log.i(TAG, "Received bundle: sharename: " + sharename
                            + ", contacts: " + contacts);
                }
            }

        } else if (REQUEST_CODE_CATEGORY_ALTERNATIVE == requestCode) {
            if (debug) {
                Log.d(TAG, "result received");
            }
            if (RESULT_OK == resultCode) {
                if (debug) {
                    Log.d(TAG, "result OK");
                }

                if (data.getExtras() != null) {
                    if (debug) {
                        Log.d(TAG, "extras received");
                    }
                    mItemsFromExtras.getShoppingExtras(data);
                }
            }
        } else if (REQUEST_PICK_LIST == requestCode) {
            if (debug) {
                Log.d(TAG, "result received");
            }

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

    private void closeDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mDrawerListsView);
        }
    }

    /**
     * With the requirement of OS3, making an intermediary decision depending
     * upon the widget
     *
     * @param adapter
     */
    private void setSpinnerAndDrawerListAdapter(ListAdapter adapter) {
        DrawerListAdapter adapterWrapper = (new DrawerListAdapter(this, adapter));
        mDrawerListsView.setAdapter(adapterWrapper);
        mDrawerListsView.setOnItemClickListener(adapterWrapper);
        mShoppingListsView.setAdapter(adapter);
    }

    @Override
    public void onItemChanged() {
        mItemsView.mCursorItems.requery();
        fillAutoCompleteTextViewAdapter(mEditText);
    }

    @Override
    public void onUndoAvailable(SnackbarUndoOperation undoOp) {
        Snackbar snackbar = Snackbar.make(mItemsView, undoOp.getDescription(this),
                Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, undoOp);
        snackbar.show();
    }

    private class DrawerListAdapter implements WrapperListAdapter, OnItemClickListener {

        private ListAdapter mAdapter;
        private int mNumAboveList = 3;
        private int mNumBelowList = 1;
        private int mViewTypeNum;
        private LayoutInflater mInflater;

        public DrawerListAdapter(Context context, ListAdapter adapter) {
            mAdapter = adapter;
            mViewTypeNum = mAdapter.getViewTypeCount();
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            int list_pos, post_pos;
            int list_count = mAdapter.getCount();
            if (position < mNumAboveList) {
                return (position != 2); // not the "Lists" header
            } else if ((list_pos = position - mNumAboveList) < list_count) {
                // actual list entries can be selected
            } else {
                post_pos = list_pos - list_count;
                // New List button can be selected
            }
            return true;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mAdapter.getCount() + mNumAboveList + mNumBelowList;
        }

        @Override
        public Object getItem(int position) {
            int list_pos, post_pos;
            int list_count = mAdapter.getCount();
            if (position < mNumAboveList) {
            } else if ((list_pos = position - mNumAboveList) < list_count) {
                return mAdapter.getItem(list_pos);
            } else {
                post_pos = list_pos - list_count;
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            int list_pos, post_pos;
            int list_count = mAdapter.getCount();
            if (position < mNumAboveList) {

            } else if ((list_pos = position - mNumAboveList) < list_count) {
                return mAdapter.getItemId(list_pos);
            } else {
                post_pos = list_pos - list_count;
            }
            return -1;
        }

        @Override
        public int getItemViewType(int position) {
            int list_pos, post_pos;
            int list_count = mAdapter.getCount();
            if (position < mNumAboveList) {
            } else if ((list_pos = position - mNumAboveList) < list_count) {
                return mAdapter.getItemViewType(list_pos);
            }
            return IGNORE_ITEM_VIEW_TYPE;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int list_pos;
            int list_count = mAdapter.getCount();
            View v = null;
            View v2;

            if (position < mNumAboveList) {
                switch (position) {
                    case 1:
                        v = mInflater.inflate(R.layout.drawer_item_radio, parent, false);
                        v2 = v.findViewById(R.id.text1);
                        ((TextView) v2).setText(R.string.menu_pick_items);
                        v2 = v.findViewById(R.id.mode_radio_button);
                        v2.setSelected(mItemsView.inAddItemsMode());
                        break;
                    case 0:
                        v = mInflater.inflate(R.layout.drawer_item_radio, parent, false);
                        v2 = v.findViewById(R.id.text1);
                        ((TextView) v2).setText(R.string.menu_start_shopping);
                        v2 = v.findViewById(R.id.mode_radio_button);
                        v2.setSelected(mItemsView.inShopMode());
                        break;
                    case 2:
                        v = mInflater.inflate(R.layout.drawer_item_header, parent, false);
                        ((TextView) v).setText(R.string.list); // fix me
                        break;
                    default:
                        break;

                }
            } else if ((list_pos = position - mNumAboveList) < list_count) {
                int curListPos = mShoppingListsView.getSelectedItemPosition();
                if (list_pos == curListPos) {
                    mDrawerListsView.setItemChecked(position, true);
                }
                v = mAdapter.getView(list_pos, convertView, parent);
            } else {
                v = mInflater.inflate(R.layout.drawer_item_radio, parent, false);
                v2 = v.findViewById(R.id.text1);
                ((TextView) v2).setText(R.string.new_list);
                v2 = v.findViewById(R.id.mode_radio_button);
                ((ImageView) v2).setImageResource(R.drawable.ic_menu_add_list);
            }
            return v;
        }

        @Override
        public int getViewTypeCount() {
            return mViewTypeNum;
        }

        @Override
        public boolean hasStableIds() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mAdapter.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mAdapter.unregisterDataSetObserver(observer);
        }

        @Override
        public ListAdapter getWrappedAdapter() {
            return mAdapter;
        }

        public void onItemClick(AdapterView parent, View v,
                                int position, long id) {
            if (debug) {
                Log.d(TAG, "DrawerListAdapter: onItemClick");
            }

            int list_pos;
            int list_count = mAdapter.getCount();
            if (position < mNumAboveList) {
                // Pick Items or Shopping selected
                closeDrawer();
                if (position == 1) mItemsView.setAddItemsMode();
                else mItemsView.setInShopMode();
                mDrawerListsView.setItemChecked(position, true);
                mDrawerListsView.setItemChecked(1 - position, false);
                mDrawerListsView.invalidateViews();
                onModeChanged();
            } else if ((list_pos = position - mNumAboveList) < list_count) {
                // Update list cursor:
                mShoppingListsView.setSelection(list_pos);
                getSelectedListId();

                // Set the theme based on the selected list:
                setListTheme(loadListTheme());

                // If it's the same list we had before, requery only
                // if a preference has changed since then.
                fillItems(id == mItemsView.getListId());

                // Apply the theme after the list has been filled:
                applyListTheme();

                updateTitle();

                mDrawerListsView.setItemChecked(position, true);
                closeDrawer();
            } else {
                closeDrawer();
                showDialog(DIALOG_NEW_LIST);
            }
        }
    }

    private class ListSortActionProvider extends ActionProvider implements OnMenuItemClickListener {

        private Context mContext;
        private String[] mSortLabels;
        private String[] mSortVals;
        private Integer[] mSortValInts;

        public ListSortActionProvider(Context context) {
            super(context);
            mContext = context;
            mSortLabels = mContext.getResources().getStringArray(R.array.preference_sortorder_entries);
            mSortVals = mContext.getResources().getStringArray(R.array.preference_sortorder_entryvalues);
            mSortValInts = new Integer[mSortVals.length];
            for (int i = 0; i < mSortVals.length; i++)
                mSortValInts[i] = Integer.parseInt(mSortVals[i]);
        }

        @Override
        public View onCreateActionView() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean hasSubMenu() {
            return true;
        }

        public void onPrepareSubMenu(SubMenu subMenu) {
            int i;
            saveActiveList(false);
            subMenu.clear();
            int curSort = PreferenceActivity.getSortOrderIndexFromPrefs(mContext, MODE_IN_SHOP);
            for (i = 0; i < mSortLabels.length; i++) {
                subMenu.add(1, i, i, mSortLabels[i]).setOnMenuItemClickListener(this)
                        .setChecked(mSortValInts[i] == curSort);
            }
            subMenu.setGroupCheckable(1, true, true);
        }

        public boolean overridesItemVisibility() {
            return true;
        }

        public boolean isVisible() {
            boolean vis = true;
            if (!mItemsView.inShopMode()) {
                vis = false;
            } else if (!PreferenceActivity.getUsingPerListSortFromPrefs(mContext)) {
                vis = false;
            }
            if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawerListsView)) {
                vis = false;
            }
            return vis;
        }

        public boolean onMenuItemClick(MenuItem item) {
            int sortOrderNum = Integer.parseInt(mSortVals[item.getItemId()]);
            // String sortOrder = Contains.SORT_ORDERS[sortOrderNum];
            ContentValues values = new ContentValues();
            values.put(Lists.ITEMS_SORT, sortOrderNum);
            getContentResolver().update(mListUri, values, null, null);
            fillItems(false);
            return true;
        }
    }
}
