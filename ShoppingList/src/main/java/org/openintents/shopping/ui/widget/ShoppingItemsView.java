package org.openintents.shopping.ui.widget;

import android.app.Activity;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.SearchView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import org.openintents.distribution.DownloadAppDialog;
import org.openintents.shopping.R;
import org.openintents.shopping.ShoppingApplication;
import org.openintents.shopping.SyncSupport;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.shopping.provider.ShoppingProvider;
import org.openintents.shopping.theme.ThemeAttributes;
import org.openintents.shopping.theme.ThemeShoppingList;
import org.openintents.shopping.theme.ThemeUtils;
import org.openintents.shopping.ui.PreferenceActivity;
import org.openintents.shopping.ui.ShoppingActivity;
import org.openintents.shopping.ui.ShoppingTotalsHandler;
import org.openintents.shopping.ui.SnackbarUndoMultipleItemStatusOperation;
import org.openintents.shopping.ui.SnackbarUndoSingleItemStatusOperation;
import org.openintents.shopping.ui.UndoListener;
import org.openintents.shopping.ui.dialog.EditItemDialog;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * View to show a shopping list with its items
 */
public class ShoppingItemsView extends ListView implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * mode: separate dialog to add items from existing list
     */
    private static final int MODE_PICK_ITEMS_DLG = 3;
    /**
     * mode: add items from existing list
     */
    private static final int MODE_ADD_ITEMS = 2;
    /**
     * mode: I am in the shop
     */
    public static final int MODE_IN_SHOP = 1;

    private final static String TAG = "ShoppingListView";
    private final static boolean debug = false;
    public int mPriceVisibility;
    public int mTagsVisibility;
    public int mQuantityVisibility;
    public int mUnitsVisibility;
    public int mPriorityVisibility;
    public String mTextTypeface;
    public float mTextSize;
    public boolean mTextUpperCaseFont;
    public int mTextColor;
    public int mTextColorPrice;
    public int mTextColorChecked;
    public int mTextColorPriority;
    public boolean mShowCheckBox;
    public boolean mShowStrikethrough;
    public String mTextSuffixUnchecked;
    public String mTextSuffixChecked;
    public int mBackgroundPadding;
    public int mUpdateLastListPosition;
    public int mLastListPosition;
    public int mLastListTop;
    public long mNumChecked;
    public long mNumUnchecked;
    private int mMode = MODE_IN_SHOP;
    public int mModeBeforeSearch;
    public Cursor mCursorItems;
    private Typeface mCurrentTypeface;
    private ThemeAttributes mThemeAttributes;
    private PackageManager mPackageManager;
    private String mPackageName;
    private NumberFormat mPriceFormatter = DecimalFormat
            .getNumberInstance(Locale.ENGLISH);
    private String mFilter;
    private boolean mInSearch;
    private Activity mCursorActivity;

    private View mThemedBackground;
    private long mListId;

    private long mFocusItemId = -1;

    private Drawable mDefaultDivider;

    private int mDragPos; // which item is being dragged
    private int mFirstDragPos; // where was the dragged item originally
    private int mDragPoint; // at what offset inside the item did the user grab
    // it
    private int mCoordOffset; // the difference between screen coordinates and
    // coordinates in this view

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private Rect mTempRect = new Rect();

    // dragging elements
    private Bitmap mDragBitmap;
    private ImageView mDragView;
    private int mHeight;
    private int mUpperBound;
    private int mLowerBound;
    private int mTouchSlop;
    private int mItemHeightHalf;
    private int mItemHeightNormal;
    private int mItemHeightExpanded;

    private DragListener mDragListener;
    private DropListener mDropListener;

    private ActionBarListener mActionBarListener;
    private UndoListener mUndoListener;
    private Snackbar mSnackbar;
    private SyncSupport mSyncSupport;
    private ShoppingTotalsHandler mTotalsHandler;
    private SearchView mSearchView;
    private OnCustomClickListener mListener;
    private OnModeChangeListener mModeChangeListener;
    private boolean mDragAndDropEnabled;

    public ShoppingItemsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ShoppingItemsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShoppingItemsView(Context context) {
        super(context);
        init();
    }

    public View getSearchView() {
        Context context = getContext();
        if (PreferenceActivity.getUsingHoloSearchFromPrefs(context)) {
            mSearchView = new SearchView(mCursorActivity);
            mSearchView.setSubmitButtonEnabled(true);
            mSearchView.setInputType(PreferenceActivity.getSearchInputTypeFromPrefs(context));
            mSearchView.setOnQueryTextListener(new SearchQueryListener());
            mSearchView.setOnCloseListener(new SearchDismissedListener());
            mSearchView.setImeOptions(EditorInfo.IME_ACTION_UNSPECIFIED);
        }
        return mSearchView;
    }

    private void disposeItemsCursor() {
        if (mCursorActivity != null) {
            mCursorActivity.stopManagingCursor(mCursorItems);
            mCursorActivity = null;
        }
        mCursorItems.deactivate();
        if (!mCursorItems.isClosed()) {
            mCursorItems.close();
        }
        mCursorItems = null;
    }

    private void init() {
        mItemHeightNormal = 45;
        mItemHeightHalf = mItemHeightNormal / 2;
        mItemHeightExpanded = 90;

        // Remember standard divider
        mDefaultDivider = getDivider();
        mSyncSupport = ((ShoppingApplication) getContext().getApplicationContext()).dependencies().getSyncSupport(getContext());
    }

    public void initTotals() {
        // Can't be called during init because that happens while
        // still inflating the parent activity, so findViewById
        // doesn't work yet.
        mTotalsHandler = new ShoppingTotalsHandler(this);
    }

    public void setActionBarListener(ActionBarListener listener) {
        mActionBarListener = listener;
    }

    public void setUndoListener(UndoListener listener) {
        mUndoListener = listener;
    }

    public void onResume() {
        setFastScrollEnabled(PreferenceActivity.getFastScrollEnabledFromPrefs(getContext()));
    }

    public void onPause() {

    }

    public long getListId() {
        return mListId;
    }

    public boolean getInSearch() {
        return mInSearch;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(mCursorActivity);
        createItemsCursor(mListId, loader);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor items) {

        // Get a cursor for all items that are contained
        // in currently selected shopping list.
        mCursorItems = items;

        // Activate the following for a striped list.
        // setupListStripes(mListItems, this);

        if (mCursorItems == null) {
            Log.e(TAG, "missing shopping provider");
            setAdapter(new ArrayAdapter<>(this.getContext(),
                    android.R.layout.simple_list_item_1,
                    new String[]{"no shopping provider"}));
            return;
        }

        int layout_row = R.layout.list_item_shopping_item;
        int size = PreferenceActivity.getFontSizeFromPrefs(getContext());
        if (size < 3) {
            layout_row = R.layout.list_item_shopping_item_small;
        }

        Context context = getContext();

        // If background is light, we apply the light holo theme to widgets.

        // determine color from text color:
        int gray = (Color.red(mTextColor) + Color.green(mTextColor) + Color.blue(mTextColor));
        if (gray < 3 * 128) {
            // dark text color <-> light background color => use light holo theme.
            context = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light);
        }

        mSimpleCursorAdapter adapter = (mSimpleCursorAdapter) getAdapter();

        if (adapter != null) {
            adapter.swapCursor(mCursorItems);
        } else {
            adapter = new mSimpleCursorAdapter(
                    context,
                    // Use a template that displays a text view
                    layout_row,
                    // Give the cursor to the list adapter
                    mCursorItems,
                    // Map the IMAGE and NAME to...
                    new String[]{ContainsFull.ITEM_NAME, /*
                                                         * ContainsFull.ITEM_IMAGE
														 * ,
														 */
                            ContainsFull.ITEM_TAGS, ContainsFull.ITEM_PRICE,
                            ContainsFull.QUANTITY, ContainsFull.PRIORITY,
                            ContainsFull.ITEM_UNITS
                    },
                    // the view defined in the XML template
                    new int[]{R.id.name, /* R.id.image_URI, */R.id.tags,
                            R.id.price, R.id.quantity, R.id.priority, R.id.units}
            );
            setAdapter(adapter);
        }

        if (mFocusItemId != -1) {
            // Set the item that we have just selected:
            // Get position of ID:
            mCursorItems.moveToPosition(-1);
            while (mCursorItems.moveToNext()) {
                if (mCursorItems.getLong(ShoppingActivity.mStringItemsITEMID) == mFocusItemId) {
                    int pos = mCursorItems.getPosition();
                    // scroll item near top, but not all the way to top, to provide context.
                    pos = Math.max(pos - 3, 0);
                    postDelayedSetSelection(pos);
                    break;
                }
            }
            if (!mInSearch) {
                mFocusItemId = -1;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorItems = null;
    }

    /**
     * @param activity Activity to manage the cursor.
     * @param listId
     * @return
     */
    public void fillItems(Activity activity, long listId) {

        mCursorItems = null;
        mCursorActivity = activity;

        mListId = listId;

        setSearchModePref();
        activity.getLoaderManager().restartLoader(ShoppingActivity.LOADER_ITEMS, null, this);

        updateTotal();

    }

    private void setSearchModePref() {
        // this is not a real user preference, just used to communicate
        // current app state to the content provider.
        SharedPreferences sp = mCursorActivity.getSharedPreferences(
                "org.openintents.shopping_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("_searching", mInSearch);
        editor.apply();
    }

    private Cursor createItemsCursor(long listId, CursorLoader loader) {
        String sortOrder = PreferenceActivity.getSortOrderFromPrefs(this
                .getContext(), mMode, listId);
        boolean hideBought = PreferenceActivity
                .getHideCheckedItemsFromPrefs(this.getContext());
        String selection;
        String[] selection_args = new String[]{String.valueOf(listId)};
        if (mFilter != null) {
            selection = "list_id = ? AND " + ContainsFull.ITEM_NAME +
                    " like '%" + ShoppingProvider.escapeSQLChars(mFilter) + "%' ESCAPE '`'";
        } else if (inShopMode()) {
            if (hideBought) {
                selection = "list_id = ? AND " + Contains.STATUS
                        + " == " + Status.WANT_TO_BUY;
            } else {
                selection = "list_id = ? AND " + Contains.STATUS
                        + " <> " + Status.REMOVED_FROM_LIST;
            }
        } else {
            selection = "list_id = ? ";
        }

        if (loader != null) {
            loader.setUri(ContainsFull.CONTENT_URI);
            loader.setProjection(ShoppingActivity.PROJECTION_ITEMS);
            loader.setSelection(selection);
            loader.setSelectionArgs(selection_args);
            loader.setSortOrder(sortOrder);
            return null;
        }

        return getContext().getContentResolver().query(
                ContainsFull.CONTENT_URI, ShoppingActivity.PROJECTION_ITEMS,
                selection, selection_args, sortOrder);
    }

    /**
     * Set theme according to Id.
     *
     * @param themeName
     */
    public void setListTheme(String themeName) {
        int size = PreferenceActivity.getFontSizeFromPrefs(getContext());

        // backward compatibility:
        if (themeName == null) {
            setLocalStyle(R.style.Theme_ShoppingList, size);
        } else if (themeName.equals("1")) {
            setLocalStyle(R.style.Theme_ShoppingList, size);
        } else if (themeName.equals("2")) {
            setLocalStyle(R.style.Theme_ShoppingList_Classic, size);
        } else if (themeName.equals("3")) {
            setLocalStyle(R.style.Theme_ShoppingList_Android, size);
        } else {
            // New styles:
            boolean themeFound = setRemoteStyle(themeName, size);

            if (!themeFound) {
                // Some error occured, let's use default style:
                setLocalStyle(R.style.Theme_ShoppingList, size);
            }
        }

        invalidate();
        if (mCursorItems != null) {
            requery();
        }
    }

    private void setLocalStyle(int styleResId, int size) {
        String styleName = getResources().getResourceName(styleResId);

        boolean themefound = setRemoteStyle(styleName, size);

        if (!themefound) {
            // Actually this should never happen.
            Log.e(TAG, "Local theme not found: " + styleName);
        }
    }

    private boolean setRemoteStyle(String styleName, int size) {
        if (TextUtils.isEmpty(styleName)) {
            if (debug) {
                Log.e(TAG, "Empty style name: " + styleName);
            }
            return false;
        }

        mPackageManager = getContext().getPackageManager();

        mPackageName = ThemeUtils.getPackageNameFromStyle(styleName);

        if (mPackageName == null) {
            Log.e(TAG, "Invalid style name: " + styleName);
            return false;
        }

        Context c = null;
        try {
            c = getContext().createPackageContext(mPackageName, 0);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Package for style not found: " + mPackageName + ", "
                    + styleName);
            return false;
        }

        Resources res = c.getResources();

        int themeid = res.getIdentifier(styleName, null, null);

        if (themeid == 0) {
            Log.e(TAG, "Theme name not found: " + styleName);
            return false;
        }

        try {
            mThemeAttributes = new ThemeAttributes(c, mPackageName, themeid);

            mTextTypeface = mThemeAttributes.getString(ThemeShoppingList.textTypeface);
            mCurrentTypeface = createTypeface(mTextTypeface);

            mTextUpperCaseFont = mThemeAttributes.getBoolean(
                    ThemeShoppingList.textUpperCaseFont, false);

            mTextColor = mThemeAttributes.getColor(ThemeShoppingList.textColor,
                    android.R.color.white);

            mTextColorPrice = mThemeAttributes.getColor(ThemeShoppingList.textColorPrice,
                    android.R.color.white);

            // Use color of price if color of priority has not been defined
            mTextColorPriority = mThemeAttributes.getColor(ThemeShoppingList.textColorPriority,
                    mTextColorPrice);

            if (size == 0) {
                mTextSize = getTextSizeTiny(mThemeAttributes);
            } else if (size == 1) {
                mTextSize = getTextSizeSmall(mThemeAttributes);
            } else if (size == 2) {
                mTextSize = getTextSizeMedium(mThemeAttributes);
            } else {
                mTextSize = getTextSizeLarge(mThemeAttributes);
            }
            if (debug) {
                Log.d(TAG, "textSize: " + mTextSize);
            }

            mTextColorChecked = mThemeAttributes.getColor(ThemeShoppingList.textColorChecked,
                    android.R.color.white);
            mShowCheckBox = mThemeAttributes.getBoolean(ThemeShoppingList.showCheckBox, true);
            mShowStrikethrough = mThemeAttributes.getBoolean(
                    ThemeShoppingList.textStrikethroughChecked, false);
            mTextSuffixUnchecked = mThemeAttributes
                    .getString(ThemeShoppingList.textSuffixUnchecked);
            mTextSuffixChecked = mThemeAttributes
                    .getString(ThemeShoppingList.textSuffixChecked);

            // field was named divider, until a conflict with the appcompat library
            // forced us to rename it. To continue to support old themes, check for
            // shopping_divider first, but if it's not found, check for divider also.
            int divider = mThemeAttributes.getInteger(ThemeShoppingList.shopping_divider, 0);
            if (divider == 0) {
                divider = mThemeAttributes.getInteger(ThemeShoppingList.divider, 0);
            }

            Drawable div;
            if (divider > 0) {
                div = getResources().getDrawable(divider);
            } else if (divider < 0) {
                div = null;
            } else {
                div = mDefaultDivider;
            }

            setDivider(div);

            return true;

        } catch (UnsupportedOperationException e) {
            // This exception is thrown e.g. if one attempts
            // to read an integer attribute as dimension.
            Log.e(TAG, "UnsupportedOperationException", e);
            return false;
        } catch (NumberFormatException e) {
            // This exception is thrown e.g. if one attempts
            // to read a string as integer.
            Log.e(TAG, "NumberFormatException", e);
            return false;
        }
    }

    private Typeface createTypeface(String typeface) {
        Typeface newTypeface = null;
        try {
            // Look for special cases:
            if ("monospace".equals(typeface)) {
                newTypeface = Typeface.create(Typeface.MONOSPACE,
                        Typeface.NORMAL);
            } else if ("sans".equals(typeface)) {
                newTypeface = Typeface.create(Typeface.SANS_SERIF,
                        Typeface.NORMAL);
            } else if ("serif".equals(typeface)) {
                newTypeface = Typeface.create(Typeface.SERIF,
                        Typeface.NORMAL);
            } else if (!TextUtils.isEmpty(typeface)) {

                try {
                    if (debug) {
                        Log.d(TAG, "Reading typeface: package: " + mPackageName
                                + ", typeface: " + typeface);
                    }
                    Resources remoteRes = mPackageManager
                            .getResourcesForApplication(mPackageName);
                    newTypeface = Typeface.createFromAsset(remoteRes
                            .getAssets(), typeface);
                    if (debug) {
                        Log.d(TAG, "Result: " + newTypeface);
                    }
                } catch (NameNotFoundException e) {
                    Log.e(TAG, "Package not found for Typeface", e);
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "type face can't be made " + typeface);
        }
        return newTypeface;
    }

    /**
     * Must be called after setListTheme();
     */
    public void applyListTheme() {

        if (mThemedBackground != null) {
            mBackgroundPadding = mThemeAttributes.getDimensionPixelOffset(
                    ThemeShoppingList.backgroundPadding, -1);
            int backgroundPaddingLeft = mThemeAttributes.getDimensionPixelOffset(
                    ThemeShoppingList.backgroundPaddingLeft,
                    mBackgroundPadding);
            int backgroundPaddingTop = mThemeAttributes.getDimensionPixelOffset(
                    ThemeShoppingList.backgroundPaddingTop,
                    mBackgroundPadding);
            int backgroundPaddingRight = mThemeAttributes.getDimensionPixelOffset(
                    ThemeShoppingList.backgroundPaddingRight,
                    mBackgroundPadding);
            int backgroundPaddingBottom = mThemeAttributes.getDimensionPixelOffset(
                    ThemeShoppingList.backgroundPaddingBottom,
                    mBackgroundPadding);
            try {
                Resources remoteRes = mPackageManager
                        .getResourcesForApplication(mPackageName);
                int resid = mThemeAttributes.getResourceId(ThemeShoppingList.background,
                        0);
                if (resid != 0) {
                    Drawable d = remoteRes.getDrawable(resid);
                    mThemedBackground.setBackgroundDrawable(d);
                } else {
                    // remove background
                    mThemedBackground.setBackgroundResource(0);
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Package not found for Theme background.", e);
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Resource not found for Theme background.", e);
            }

            // Apply padding
            if (mBackgroundPadding >= 0 || backgroundPaddingLeft >= 0
                    || backgroundPaddingTop >= 0
                    || backgroundPaddingRight >= 0
                    || backgroundPaddingBottom >= 0) {
                mThemedBackground.setPadding(backgroundPaddingLeft,
                        backgroundPaddingTop, backgroundPaddingRight,
                        backgroundPaddingBottom);
            } else {
                // 9-patches do the padding automatically
                // todo clear padding
            }
        }

    }

    private float getTextSizeTiny(ThemeAttributes ta) {
        float size = ta.getDimensionPixelOffset(ThemeShoppingList.textSizeTiny,
                -1);
        if (size == -1) {
            // Try to obtain from small:
            size = (12f / 18f) * getTextSizeSmall(ta);
        }
        return size;
    }

    private float getTextSizeSmall(ThemeAttributes ta) {
        float size = ta.getDimensionPixelOffset(
                ThemeShoppingList.textSizeSmall, -1);
        if (size == -1) {
            // Try to obtain from small:
            size = (18f / 23f) * getTextSizeMedium(ta);
        }
        return size;
    }

    private float getTextSizeMedium(ThemeAttributes ta) {
        final float scale = getResources().getDisplayMetrics().scaledDensity;
        return ta.getDimensionPixelOffset(
                ThemeShoppingList.textSizeMedium, (int) (23 * scale + 0.5f));
    }

    private float getTextSizeLarge(ThemeAttributes ta) {
        float size = ta.getDimensionPixelOffset(
                ThemeShoppingList.textSizeLarge, -1);
        if (size == -1) {
            // Try to obtain from small:
            size = (28f / 23f) * getTextSizeMedium(ta);
        }
        return size;
    }

    public void setThemedBackground(View background) {
        mThemedBackground = background;

    }

    /**
     * set the status of all items according to the parameter
     *
     * @param on if true all want_to_buy items are set to bought, if false all bought items are set to want_to_buy
     */
    public void toggleAllItems(boolean on) {
        int op_type = on ? SnackbarUndoMultipleItemStatusOperation.MARK_ALL : SnackbarUndoMultipleItemStatusOperation.UNMARK_ALL;
        SnackbarUndoMultipleItemStatusOperation op = null;

        if (mUndoListener != null) {
            op = new SnackbarUndoMultipleItemStatusOperation(this, mCursorActivity,
                    op_type, mListId, false);
        }

        for (int i = 0; i < mCursorItems.getCount(); i++) {
            mCursorItems.moveToPosition(i);

            long oldstatus = mCursorItems
                    .getLong(ShoppingActivity.mStringItemsSTATUS);

            // Toggle status ON:
            // bought -> bought
            // want_to_buy -> bought
            // removed_from_list -> removed_from_list

            // Toggle status OFF:
            // bought -> want_to_buy
            // want_to_buy -> want_to_buy
            // removed_from_list -> removed_from_list

            long newstatus;
            boolean doUpdate;
            if (on) {
                newstatus = ShoppingContract.Status.BOUGHT;
                doUpdate = (oldstatus == ShoppingContract.Status.WANT_TO_BUY);
            } else {
                newstatus = ShoppingContract.Status.WANT_TO_BUY;
                doUpdate = (oldstatus == ShoppingContract.Status.BOUGHT);
            }

            if (doUpdate) {
                final ContentValues values = new ContentValues();
                values.put(ShoppingContract.Contains.STATUS, newstatus);
                if (debug) {
                    Log.d(TAG, "update row " + mCursorItems.getString(0) + ", newstatus "
                            + newstatus);
                }
                final Uri itemUri = Uri.withAppendedPath(Contains.CONTENT_URI,
                        mCursorItems.getString(0));
                getContext().getContentResolver().update(
                        itemUri, values, null, null
                );
                pushUpdatedItemToWear(values, itemUri);

            }
        }

        requery();

        invalidate();
        if (mUndoListener != null) {
            mUndoListener.onUndoAvailable(op);
        }
    }

    public void toggleItemBought(int position) {
        boolean shouldFocusItem = false;

        if (mCursorItems.getCount() <= position) {
            Log.e(TAG, "toggle inexistent item. Probably clicked too quickly?");
            return;
        }

        mCursorItems.moveToPosition(position);

        long oldstatus = mCursorItems
                .getLong(ShoppingActivity.mStringItemsSTATUS);

        // Toggle status depending on mode:
        long newstatus = ShoppingContract.Status.WANT_TO_BUY;

        if (inShopMode()) {
            if (oldstatus == ShoppingContract.Status.WANT_TO_BUY) {
                newstatus = ShoppingContract.Status.BOUGHT;
            } // else old was BOUGHT, new should be WANT_TO_BUY, which is the default.
        } else { // MODE_ADD_ITEMS or MODE_PICK_ITEMS_DLG
            // when we are in integrated add items mode, all three states
            // might be displayed, but the user can only create two of them.
            // want_to_buy-> removed_from_list
            // bought -> want_to_buy
            // removed_from_list -> want_to_buy
            if (oldstatus == ShoppingContract.Status.WANT_TO_BUY) {
                newstatus = ShoppingContract.Status.REMOVED_FROM_LIST;
                shouldFocusItem = mInSearch && mFilter != null && mFilter.length() > 0;
            } else { // old is REMOVE_FROM_LIST or BOUGHT, new is WANT_TO_BUY, which is the default.
                if (mInSearch) {
                    shouldFocusItem = true;
                }
            }
        }

        String contains_id = mCursorItems.getString(0);
        final ContentValues values = new ContentValues();
        values.put(ShoppingContract.Contains.STATUS, newstatus);
        if (debug) {
            Log.d(TAG, "update row " + mCursorItems.getString(0) + ", newstatus "
                    + newstatus);
        }

        if (mInSearch && newstatus == ShoppingContract.Status.WANT_TO_BUY) {
            long item_id = mCursorItems.getLong(ShoppingActivity.mStringItemsITEMID);
            ShoppingUtils.addDefaultsToAddedItem(getContext(), mListId, item_id);
        }

        if (shouldFocusItem) {
            mFocusItemId = mCursorItems.getLong(ShoppingActivity.mStringItemsITEMID);
        }

        final Uri itemUri = Uri.withAppendedPath(Contains.CONTENT_URI, contains_id);
        getContext().getContentResolver().update(
                itemUri, values, null, null
        );

        pushUpdatedItemToWear(values, itemUri);

        boolean affectsSort = PreferenceActivity.prefsStatusAffectsSort(getContext(), mMode);
        boolean hidesItem = true /* TODO */;
        if (mUndoListener != null && (affectsSort || hidesItem)) {
            String item_name = mCursorItems.getString(ShoppingActivity.mStringItemsITEMNAME);
            SnackbarUndoSingleItemStatusOperation op = new SnackbarUndoSingleItemStatusOperation(this, getContext(),
                    contains_id, item_name, oldstatus, newstatus, 0, false);
            mUndoListener.onUndoAvailable(op);
        }

        requery();

        if (affectsSort) {
            invalidate();
        }
    }

    public boolean cleanupList() {

        boolean nothingdeleted;

        SnackbarUndoMultipleItemStatusOperation op = null;
        if (mUndoListener != null) {
            op = new SnackbarUndoMultipleItemStatusOperation(this, mCursorActivity,
                    SnackbarUndoMultipleItemStatusOperation.CLEAN_LIST, mListId, false);
        }

        // by changing state
        ContentValues values = new ContentValues();
        values.put(Contains.STATUS, Status.REMOVED_FROM_LIST);
        if (PreferenceActivity.getResetQuantity(getContext())) {
            values.put(Contains.QUANTITY, "");
        }
        nothingdeleted = getContext().getContentResolver().update(
                Contains.CONTENT_URI,
                values,
                ShoppingContract.Contains.LIST_ID + " = " + mListId + " AND "
                        + ShoppingContract.Contains.STATUS + " = "
                        + ShoppingContract.Status.BOUGHT, null
        ) == 0;

        requery();

        if (mUndoListener != null) {
            mUndoListener.onUndoAvailable(op);
        }

        return !nothingdeleted;

    }

    /**
     * @param activity Activity to manage new Cursor.
     * @param newItem
     * @param quantity
     * @param price
     * @param barcode
     */
    public void insertNewItem(Activity activity, String newItem,
                              String quantity, String priority, String price, String barcode) {

        String list_id = null;
        if (PreferenceActivity.getCompleteFromCurrentListOnlyFromPrefs(getContext())) {
            list_id = String.valueOf(mListId);
        }

        newItem = newItem.trim();

        long itemId = ShoppingUtils.updateOrCreateItem(getContext(), newItem,
                null, price, barcode, list_id);

        if (debug) {
            Log.i(TAG, "Insert new item. " + " itemId = " + itemId + ", listId = "
                    + mListId);
        }
        boolean resetQuantity = PreferenceActivity.getResetQuantity(getContext());
        ShoppingUtils.addItemToList(getContext(), itemId, mListId, Status.WANT_TO_BUY,
                priority, quantity, false, false, resetQuantity);
        ShoppingUtils.addDefaultsToAddedItem(getContext(), mListId, itemId);
        mFocusItemId = itemId;
        fillItems(activity, mListId);
    }

    public boolean isWearSupportAvailable() {
        return mSyncSupport != null && mSyncSupport.isAvailable();
    }

    public void pushItemsToWear() {
        if (mSyncSupport.isAvailable()) {
            new Thread() {
                @Override
                public void run() {
                    Cursor cursor = createItemsCursor(mListId, null);
                    Log.d(TAG, "pushing " + cursor.getCount() + " items");
                    cursor.moveToFirst();
                    while (cursor.moveToNext()) {
                        mSyncSupport.pushListItem(mListId, cursor);
                    }
                }
            }.start();
        }
    }

    private void pushUpdatedItemToWear(final ContentValues values, final Uri itemUri) {
        if (mSyncSupport.isAvailable() && mSyncSupport.isSyncEnabled())
            new Thread() {
                @Override
                public void run() {
                    mSyncSupport.updateListItem(mListId, itemUri, values);
                }
            }.start();
    }

    /**
     * Post setSelection delayed, because onItemSelected() may be called more
     * than once, leading to fillItems() being called more than once as well.
     * Posting delayed ensures that items added through intents that return
     * results (like a barcode scanner) are put into visible position.
     *
     * @param pos
     */
    void postDelayedSetSelection(final int pos) {
        // set immediately
        setSelection(pos);

        // if for any reason this does not work, a delayed version
        // will succeed:
        postDelayed(new Runnable() {

            @Override
            public void run() {
                setSelection(pos);
            }

        }, 1000);
    }

    public void requery() {
        if (debug) {
            Log.d(TAG, "requery()");
        }

        // Test for null pointer exception (issue 313)
        if (mCursorItems != null) {
            mCursorItems.requery();
            updateTotal();

            if (mUpdateLastListPosition > 0) {
                if (debug) {
                    Log.d(TAG, "Restore list position: pos: " + mLastListPosition
                            + ", top: " + mLastListTop + ", tries: " + mUpdateLastListPosition);
                }
                setSelectionFromTop(mLastListPosition, mLastListTop);
                mUpdateLastListPosition--;
            }
        }
    }

    /**
     * Update the text fields for "Total:" and "Checked:" with corresponding
     * price information.
     */
    public void updateTotal() {
        if (debug) {
            Log.d(TAG, "updateTotal()");
        }
        mTotalsHandler.update(mCursorActivity.getLoaderManager(), mListId);
    }

    public void updateNumChecked(long numChecked, long numUnchecked) {

        mNumChecked = numChecked;
        mNumUnchecked = numUnchecked;

        // Update ActionBar in ShoppingActivity
        // for the "Clean up list" command
        if (mActionBarListener != null && !mInSearch) {
            mActionBarListener.updateActionBar();
        }
    }

    private long getQuantityPrice(Cursor cursor) {
        long price = cursor.getLong(ShoppingActivity.mStringItemsITEMPRICE);
        if (price != 0) {
            String quantityString = cursor
                    .getString(ShoppingActivity.mStringItemsQUANTITY);
            if (!TextUtils.isEmpty(quantityString)) {
                try {
                    double quantity = Double.parseDouble(quantityString);
                    price = (long) (price * quantity);
                } catch (NumberFormatException e) {
                    // do nothing
                }
            }
        }
        return price;
    }

    public void setCustomClickListener(OnCustomClickListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (mDragAndDropEnabled) {
            if (mDragListener != null || mDropListener != null) {
                switch (ev.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        int x = (int) ev.getX();
                        int y = (int) ev.getY();
                        int itemnum = pointToPosition(x, y);
                        if (itemnum == AdapterView.INVALID_POSITION) {
                            break;
                        }
                        ViewGroup item = (ViewGroup) getChildAt(itemnum
                                - getFirstVisiblePosition());
                        mDragPoint = y - item.getTop();
                        mCoordOffset = ((int) ev.getRawY()) - y;
                        item.setDrawingCacheEnabled(true);
                        Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
                        startDragging(bitmap, y);
                        mDragPos = itemnum;
                        mFirstDragPos = mDragPos;
                        mHeight = getHeight();
                        int touchSlop = mTouchSlop;
                        mUpperBound = Math.min(y - touchSlop, mHeight / 3);
                        mLowerBound = Math.max(y + touchSlop, mHeight * 2 / 3);
                        return false;
                    default:
                        break;
                }
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    private int myPointToPosition(int x, int y) {
        if (y < 0) {
            int pos = myPointToPosition(x, y + mItemHeightNormal);
            if (pos > 0) {
                return pos - 1;
            }
        }
        Rect frame = mTempRect;
        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(x, y)) {
                return getFirstVisiblePosition() + i;
            }
        }
        return INVALID_POSITION;
    }

    private int getItemForPosition(int y) {
        int adjustedy = y - mDragPoint - mItemHeightHalf;
        int pos = myPointToPosition(0, adjustedy);
        if (pos >= 0) {
            if (pos <= mFirstDragPos) {
                pos += 1;
            }
        } else if (adjustedy < 0) {
            pos = 0;
        }
        return pos;
    }

    private void adjustScrollBounds(int y) {
        if (y >= mHeight / 3) {
            mUpperBound = mHeight / 3;
        }
        if (y <= mHeight * 2 / 3) {
            mLowerBound = mHeight * 2 / 3;
        }
    }

    private void unExpandViews(boolean deletion) {
        for (int i = 0; ; i++) {
            View v = getChildAt(i);
            if (v == null) {
                if (deletion) {
                    int position = getFirstVisiblePosition();
                    int y = getChildAt(0).getTop();
                    setAdapter(getAdapter());
                    setSelectionFromTop(position, y);
                }
                layoutChildren();
                v = getChildAt(i);
                if (v == null) {
                    break;
                }
            }
            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = mItemHeightNormal;
            v.setLayoutParams(params);
            v.setVisibility(View.VISIBLE);
        }
    }

    private void doExpansion() {
        int childnum = mDragPos - getFirstVisiblePosition();
        if (mDragPos > mFirstDragPos) {
            childnum++;
        }

        View first = getChildAt(mFirstDragPos - getFirstVisiblePosition());

        for (int i = 0; ; i++) {
            View vv = getChildAt(i);
            if (vv == null) {
                break;
            }
            int height = mItemHeightNormal;
            int visibility = View.VISIBLE;
            if (vv.equals(first)) {
                if (mDragPos == mFirstDragPos) {
                    visibility = View.INVISIBLE;
                } else {
                    height = 1;
                }
            } else if (i == childnum) {
                if (mDragPos < getCount() - 1) {
                    height = mItemHeightExpanded;
                }
            }
            ViewGroup.LayoutParams params = vv.getLayoutParams();
            params.height = height;
            vv.setLayoutParams(params);
            vv.setVisibility(visibility);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if ((mDragListener != null || mDropListener != null)
                && mDragView != null) {
            int action = ev.getAction();
            switch (action) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Rect r = mTempRect;
                    mDragView.getDrawingRect(r);
                    stopDragging();
                    if (mDropListener != null && mDragPos >= 0
                            && mDragPos < getCount()) {
                        mDropListener.drop(mFirstDragPos, mDragPos);
                    }
                    unExpandViews(false);
                    break;
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    dragView(x, y);
                    int itemnum = getItemForPosition(y);
                    if (itemnum >= 0) {
                        if (action == MotionEvent.ACTION_DOWN
                                || itemnum != mDragPos) {
                            if (mDragListener != null) {
                                mDragListener.drag(mDragPos, itemnum);
                            }
                            mDragPos = itemnum;
                            doExpansion();
                        }
                        int speed = 0;
                        adjustScrollBounds(y);
                        if (y > mLowerBound) {
                            // scroll the list up a bit
                            speed = y > (mHeight + mLowerBound) / 2 ? 16 : 4;
                        } else if (y < mUpperBound) {
                            // scroll the list down a bit
                            speed = y < mUpperBound / 2 ? -16 : -4;
                        }
                        if (speed != 0) {
                            int ref = pointToPosition(0, mHeight / 2);
                            if (ref == AdapterView.INVALID_POSITION) {
                                // we hit a divider or an invisible view, check
                                // somewhere else
                                ref = pointToPosition(0, mHeight / 2
                                        + getDividerHeight() + 64);
                            }
                            View v = getChildAt(ref - getFirstVisiblePosition());
                            if (v != null) {
                                int pos = v.getTop();
                                setSelectionFromTop(ref, pos - speed);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }

    private void startDragging(Bitmap bm, int y) {
        stopDragging();

        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP;
        mWindowParams.x = 0;
        mWindowParams.y = y - mDragPoint + mCoordOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;

        Context context = getContext();
        ImageView v = new ImageView(context);
        int backGroundColor = context.getResources()
                .getColor(R.color.darkgreen);
        v.setBackgroundColor(backGroundColor);
        v.setImageBitmap(bm);
        mDragBitmap = bm;

        mWindowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
    }

    private void dragView(int x, int y) {
        mWindowParams.y = y - mDragPoint + mCoordOffset;
        mWindowManager.updateViewLayout(mDragView, mWindowParams);
    }

    private void stopDragging() {
        if (mDragView != null) {
            WindowManager wm = (WindowManager) getContext().getSystemService(
                    Context.WINDOW_SERVICE);
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }
        if (mDragBitmap != null) {
            mDragBitmap.recycle();
            mDragBitmap = null;
        }
    }

    public void setDragListener(DragListener l) {
        mDragListener = l;
    }

    public void setDropListener(DropListener l) {
        mDropListener = l;
    }

    public void setOnModeChangeListener(OnModeChangeListener listener) {
        mModeChangeListener = listener;
    }

    public void setModes(int mode, int modeBeforeSearch) {
        mMode = mode;
        mModeBeforeSearch = modeBeforeSearch;
    }

    public void setPickItemsDlgMode() {
        mMode = MODE_PICK_ITEMS_DLG;
    }

    public void setAddItemsMode() {
        mMode = MODE_ADD_ITEMS;
    }

    public void setInShopMode() {
        mMode = MODE_IN_SHOP;
    }

    public int getMode() {
        return mMode;
    }

    public interface OnCustomClickListener {
        void onCustomClick(Cursor c, int pos, EditItemDialog.FieldType field, View v);
    }

    public interface DragListener {
        void drag(int from, int to);
    }

    public interface DropListener {
        void drop(int from, int to);
    }

    public interface RemoveListener {
        void remove(int which);
    }

    public interface ActionBarListener {
        void updateActionBar();
    }

    public interface OnModeChangeListener {
        void onModeChanged();
    }

    /**
     * Extend the SimpleCursorAdapter to strike through items. if STATUS ==
     * Shopping.Status.BOUGHT
     */
    public class mSimpleCursorAdapter extends SimpleCursorAdapter implements
            ViewBinder {

        /**
         * Constructor simply calls super class.
         *
         * @param context Context.
         * @param layout  Layout.
         * @param c       Cursor.
         * @param from    Projection from.
         * @param to      Projection to.
         */
        mSimpleCursorAdapter(final Context context, final int layout,
                             final Cursor c, final String[] from, final int[] to) {
            super(context, layout, c, from, to);
            super.setViewBinder(this);

            mPriceFormatter.setMaximumFractionDigits(2);
            mPriceFormatter.setMinimumFractionDigits(2);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            mItemRowState rowState = new mItemRowState(view); // sets view tags
            return view;
        }

        /**
         * Additionally to the standard bindView, we also check for STATUS, and
         * strike the item through if BOUGHT.
         */
        @Override
        public void bindView(final View view, final Context context,
                             final Cursor cursor) {
            super.bindView(view, context, cursor);

            long status = cursor.getLong(ShoppingActivity.mStringItemsSTATUS);
            mItemRowState state = (mItemRowState) view.getTag();
            state.mCursorPos = cursor.getPosition();
            state.mCursor = cursor;


            // set style for name view and friends
            TextView[] styled_as_name = {state.mNameView, state.mUnitsView, state.mQuantityView};
            int i;
            for (i = 0; i < styled_as_name.length; i++) {
                TextView t = styled_as_name[i];

                // Set font
                if (mCurrentTypeface != null) {
                    t.setTypeface(mCurrentTypeface);
                }

                // Set size
                t.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);

                // Check for upper case:
                if (mTextUpperCaseFont) {
                    // Only upper case should be displayed
                    CharSequence cs = t.getText();
                    t.setText(cs.toString().toUpperCase());
                }

                t.setTextColor(mTextColor);

                if (status == ShoppingContract.Status.BOUGHT) {
                    t.setTextColor(mTextColorChecked);

                    if (mShowStrikethrough) {
                        // We have bought the item,
                        // so we strike it through:

                        // First convert text to 'spannable'
                        t.setText(t.getText(), TextView.BufferType.SPANNABLE);
                        Spannable str = (Spannable) t.getText();

                        // Strikethrough
                        str.setSpan(new StrikethroughSpan(), 0, str.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        // apply color
                        // TODO: How to get color from resource?
                        // Drawable colorStrikethrough = context
                        // .getResources().getDrawable(R.drawable.strikethrough);
                        // str.setSpan(new ForegroundColorSpan(0xFF006600), 0,
                        // str.setSpan(new ForegroundColorSpan
                        // (getResources().getColor(R.color.darkgreen)), 0,
                        // str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        // color: 0x33336600
                    }

                    if (i == 0 && mTextSuffixChecked != null) {
                        // very simple
                        t.append(mTextSuffixChecked);
                    }

                } else {
                    // item not bought:
                    if (i == 0 && mTextSuffixUnchecked != null) {
                        t.append(mTextSuffixUnchecked);
                    }
                }
            }

            // we have a check box now.. more visual and gets the point across

            if (debug) {
                Log.i(TAG, "bindview: pos = " + cursor.getPosition());
            }

            // set style for check box

            if (mShowCheckBox) {
                state.mCheckView.setVisibility(CheckBox.VISIBLE);
                state.mCheckView.setChecked(status == ShoppingContract.Status.BOUGHT);
            } else {
                state.mCheckView.setVisibility(CheckBox.GONE);
            }

            if (inShopMode()) {
                state.mNoCheckView.setVisibility(ImageView.GONE);
            } else {  // mMode == ShoppingActivity.MODE_ADD_ITEMS
                if (status == ShoppingContract.Status.REMOVED_FROM_LIST) {
                    state.mNoCheckView.setVisibility(ImageView.VISIBLE);
                    if (mShowCheckBox) {
                        // replace check box
                        state.mCheckView.setVisibility(CheckBox.INVISIBLE);
                    }
                } else {
                    state.mNoCheckView.setVisibility(ImageView.INVISIBLE);
                }
            }
        }

        private void hideTextView(TextView view) {
            view.setVisibility(View.GONE);
            view.setText("");
        }

        public boolean setViewValue(View view, Cursor cursor, int i) {
            int id = view.getId();
            long price = 0;
            boolean hasPrice = false;
            String tags = null;
            String priceString = null;
            boolean hasTags = false;
            mItemRowState state = (mItemRowState) view.getTag();
            if (mPriceVisibility == View.VISIBLE) {
                price = getQuantityPrice(cursor);
                hasPrice = (price != 0);
            }
            if (mTagsVisibility == View.VISIBLE) {
                tags = cursor.getString(ShoppingActivity.mStringItemsITEMTAGS);
                hasTags = !TextUtils.isEmpty(tags);
            }

            if (id == R.id.name) {
                boolean hasNote = cursor
                        .getInt(ShoppingActivity.mStringItemsITEMHASNOTE) != 0;
                String name = cursor
                        .getString(ShoppingActivity.mStringItemsITEMNAME);
                TextView tv = (TextView) view;
                SpannedStringBuilder name_etc = new SpannedStringBuilder();
                name_etc.appendSpannedString(new ClickableItemSpan(), name);
                if (name.equalsIgnoreCase(mFilter)) {
                    name_etc.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, name_etc.length(), 0);
                }
                if (hasNote) {
                    Drawable d = getResources().getDrawable(R.drawable.ic_launcher_notepad_small);
                    float ratio = d.getIntrinsicWidth() / d.getIntrinsicHeight();
                    d.setBounds(0, 0, (int) (ratio * mTextSize), (int) mTextSize);
                    ImageSpan noteimgspan = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                    name_etc.appendSpannedString(noteimgspan, new ClickableNoteSpan(), "\u00A0");
                }

                if (hasPrice) {
                    // set price text while setting name, so that correct size is known below
                    priceString = mPriceFormatter.format(price * 0.01d);
                    state.mPriceView.setText(priceString);
                }
                if (hasPrice && !hasTags) {
                    TextPaint paint = state.mPriceView.getPaint();
                    Rect bounds = new Rect();
                    ColorDrawable price_overlay = new ColorDrawable();
                    price_overlay.setAlpha(0);
                    paint.getTextBounds(priceString, 0, priceString.length(), bounds);
                    price_overlay.setBounds(0, 0, bounds.width(), bounds.height());
                    ImageSpan priceimgspan = new ImageSpan(price_overlay, ImageSpan.ALIGN_BASELINE);
                    name_etc.appendSpannedString(priceimgspan, " ");
                }
                tv.setText(name_etc);
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                return true;
            } else if (id == R.id.price) {
                TextView tv = (TextView) view;
                if (hasPrice) {
                    tv.setVisibility(View.VISIBLE);
                    tv.setTextColor(mTextColorPrice);
                } else {
                    hideTextView(tv);
                }
                return true;
            } else if (id == R.id.tags) {

                TextView tv = (TextView) view;
                if (hasTags) {
                    tv.setVisibility(View.VISIBLE);
                    tv.setTextColor(mTextColorPrice);
                    tv.setText(tags);
                    if (hasPrice) {
                        // don't overlap the price
                        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) tv.getLayoutParams();
                        rlp.addRule(RelativeLayout.LEFT_OF, R.id.price);
                    }
                } else {
                    hideTextView(tv);
                }
                return true;
            } else if (id == R.id.quantity) {
                String quantity = cursor.getString(ShoppingActivity.mStringItemsQUANTITY);
                TextView tv = (TextView) view;
                if (mQuantityVisibility == View.VISIBLE &&
                        !TextUtils.isEmpty(quantity)) {
                    tv.setVisibility(View.VISIBLE);
                    // tv.setTextColor(mPriceTextColor);
                    tv.setText(quantity + " ");
                } else {
                    hideTextView(tv);
                }
                return true;
            } else if (id == R.id.units) {
                String units = cursor.getString(ShoppingActivity.mStringItemsITEMUNITS);
                String quantity = cursor.getString(ShoppingActivity.mStringItemsQUANTITY);
                TextView tv = (TextView) view;
                // looks more natural if you only show units when showing qty.
                if (mUnitsVisibility == View.VISIBLE &&
                        mQuantityVisibility == View.VISIBLE &&
                        !TextUtils.isEmpty(units) && !TextUtils.isEmpty(quantity)) {
                    tv.setVisibility(View.VISIBLE);
                    // tv.setTextColor(mPriceTextColor);
                    tv.setText(units + " ");
                } else {
                    hideTextView(tv);
                }
                return true;
            } else if (id == R.id.priority) {
                String priority = cursor.getString(ShoppingActivity.mStringItemsPRIORITY);
                TextView tv = (TextView) view;
                if (mPriorityVisibility == View.VISIBLE &&
                        !TextUtils.isEmpty(priority)) {
                    tv.setVisibility(View.VISIBLE);
                    tv.setTextColor(mTextColorPriority);
                    tv.setText("-" + priority + "- ");
                } else {
                    hideTextView(tv);
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void setViewBinder(ViewBinder viewBinder) {
            throw new RuntimeException("this adapter implements setViewValue");
        }

        private class mItemRowState {
            public View mParentView;
            public TextView mNameView;
            public TextView mQuantityView;
            public TextView mUnitsView;
            public TextView mPriceView;
            public TextView mPriorityView;
            public TextView mTagsView;
            public CheckBox mCheckView;
            public ImageView mNoCheckView;

            public Cursor mCursor;
            public int mCursorPos;

            public mItemRowState(View view) {
                // This class is here to initialize state information related
                // to a single reusable item row, to reduce the amount of
                // setup that needs to be done each time the row is reused.
                //
                // Callbacks can be bound up-front here if they depend on cursor position.

                mParentView = view;
                mNameView = (TextView) view.findViewById(R.id.name);
                mPriceView = (TextView) view.findViewById(R.id.price);
                mTagsView = (TextView) view.findViewById(R.id.tags);
                mQuantityView = (TextView) view.findViewById(R.id.quantity);
                mUnitsView = (TextView) view.findViewById(R.id.units);
                mPriorityView = (TextView) view.findViewById(R.id.priority);
                mCheckView = (CheckBox) view.findViewById(R.id.check);
                mNoCheckView = (ImageView) view.findViewById(R.id.nocheck);

                mParentView.setTag(this);
                mNameView.setTag(this);
                mPriceView.setTag(this);
                mTagsView.setTag(this);
                mQuantityView.setTag(this);
                mUnitsView.setTag(this);
                mPriorityView.setTag(this);
                mCheckView.setTag(this);
                mNoCheckView.setTag(this);

                mQuantityView.setOnClickListener(new mItemClickListener("Quantity Click ",
                        EditItemDialog.FieldType.QUANTITY));
                mPriceView.setOnClickListener(new mItemClickListener("Click on price: ",
                        EditItemDialog.FieldType.PRICE));
                mUnitsView.setOnClickListener(new mItemClickListener("Click on units: ",
                        EditItemDialog.FieldType.UNITS));
                mPriorityView.setOnClickListener(new mItemClickListener("Click on priority: ",
                        EditItemDialog.FieldType.PRIORITY));
                mTagsView.setOnClickListener(new mItemClickListener("Click on tags: ",
                        EditItemDialog.FieldType.TAGS));

                mCheckView.setOnClickListener(new mItemToggleListener("Click: "));
                // also check around check box
                RelativeLayout l = (RelativeLayout) view.findViewById(R.id.check_surround);
                l.setTag(this);
                l.setOnClickListener(new mItemToggleListener("Click around: "));

                // Check for clicks on and around item text
                RelativeLayout r = (RelativeLayout) view.findViewById(R.id.description);
                r.setTag(this);
                r.setOnClickListener(new mItemClickListener("Click on description: ",
                        EditItemDialog.FieldType.ITEMNAME));

                mPriceView.setVisibility(mPriceVisibility);
                mTagsView.setVisibility(mTagsVisibility);
                mQuantityView.setVisibility(mQuantityVisibility);
                mUnitsView.setVisibility(mUnitsVisibility);
                mPriorityView.setVisibility(mPriorityVisibility);

            }

            private class mItemClickListener implements OnClickListener {
                private String mLogMessage;
                private EditItemDialog.FieldType mFieldType;

                public mItemClickListener(String logMessage, EditItemDialog.FieldType fieldType) {
                    mLogMessage = logMessage;
                    mFieldType = fieldType;
                }

                public void onClick(View v) {
                    if (debug) {
                        Log.d(TAG, mLogMessage);
                    }
                    if (mListener != null) {
                        mItemRowState state = (mItemRowState) v.getTag();
                        mListener.onCustomClick(state.mCursor, state.mCursorPos,
                                mFieldType, v);
                    }
                }
            }

            private class mItemToggleListener implements OnClickListener {
                private String mLogMessage;

                public mItemToggleListener(String logMessage) {
                    mLogMessage = logMessage;
                }

                public void onClick(View v) {
                    if (debug) {
                        Log.d(TAG, mLogMessage);
                    }
                    mItemRowState state = (mItemRowState) v.getTag();
                    toggleItemBought(state.mCursorPos);
                }
            }
        }

        private class ClickableNoteSpan extends ClickableSpan {
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                mItemRowState state = (mItemRowState) view.getTag();
                int cursorpos = state.mCursorPos;
                if (debug) {
                    Log.d(TAG, "Click on has_note: " + cursorpos);
                }
                mCursorItems.moveToPosition(cursorpos);
                long note_id = mCursorItems.getLong(ShoppingActivity.mStringItemsITEMID);
                Uri uri = ContentUris.withAppendedId(ShoppingContract.Notes.CONTENT_URI, note_id);
                i.setData(uri);
                Context context = getContext();
                try {
                    context.startActivity(i);
                } catch (ActivityNotFoundException e) {
                    // we could add a simple edit note dialog, but for now...
                    Dialog g = new DownloadAppDialog(context,
                            R.string.notepad_not_available,
                            R.string.notepad,
                            R.string.notepad_package,
                            R.string.notepad_website);
                    g.show();
                }
            }
        }

        private class ClickableItemSpan extends ClickableSpan {
            public void onClick(View view) {
                if (debug) {
                    Log.d(TAG, "Click on description: ");
                }
                if (mListener != null) {
                    mItemRowState state = (mItemRowState) view.getTag();
                    int cursorpos = state.mCursorPos;
                    mListener.onCustomClick(mCursorItems, cursorpos,
                            EditItemDialog.FieldType.ITEMNAME, view);
                }

            }

            public void updateDrawState(TextPaint ds) {
                // Override the parent's method to avoid having the text
                // in this span look like a link.
            }
        }

        private class SpannedStringBuilder extends SpannableStringBuilder {
            public SpannedStringBuilder appendSpannedString(Object o, CharSequence text) {
                int spanStart = length();
                super.append(text);
                setSpan(o, spanStart, spanStart + text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                return this;
            }

            public SpannedStringBuilder appendSpannedString(Object o, Object p, CharSequence text) {
                int spanStart = length();
                super.append(text);
                setSpan(o, spanStart, spanStart + text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                setSpan(p, spanStart, spanStart + text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                return this;
            }
        }

    }

    public boolean inShopMode() {
        return mMode == MODE_IN_SHOP;
    }

    public boolean inAddItemsMode() {
        return mMode == MODE_ADD_ITEMS;
    }

    public boolean inPickItemsDialogMode() {
        return mMode == MODE_PICK_ITEMS_DLG;
    }

    private class SearchQueryListener implements SearchView.OnQueryTextListener {
        public boolean onQueryTextChange(String query) {
            boolean isIconified = mSearchView.isIconified();
            String prevFilter = mFilter;

            if (isIconified) {
                // Something tries to restore the query text after the drawer is dismissed, but
                // it doesn't re-expand the search view. Force the query string empty when it is
                // not shown, and switch back to non-search mode.
                if (query != null && query.length() > 0) {
                    mSearchView.setQuery("", false);
                }
                query = null;
                if (mInSearch) {
                    mMode = mModeBeforeSearch;
                    mInSearch = false;
                }
            }

            if (mInSearch == false && !isIconified) {
                mInSearch = true;
                mModeBeforeSearch = mMode;
                mMode = MODE_ADD_ITEMS;
            }

            if (query == null || query.length() == 0) {
                mFilter = null;
            } else {
                mFilter = query;
            }

            if ((prevFilter == null && mFilter == null) ||
                    (prevFilter != null && prevFilter.equals(mFilter))) {
                return true;
            }

            fillItems(mCursorActivity, mListId);

            return true;
        }

        public boolean onQueryTextSubmit(String query) {
            if (query.length() > 0) {
                insertNewItem(mCursorActivity, query, null, null, null, null);
                mSearchView.setQuery("", false);
                fillItems(mCursorActivity, mListId);
            }
            return true;
        }
    }

    private class SearchDismissedListener implements SearchView.OnCloseListener {
        public boolean onClose() {
            if (mInSearch) {
                mMode = mModeBeforeSearch;
                if (mModeChangeListener != null) {
                    mModeChangeListener.onModeChanged();
                }
            }
            mInSearch = false;
            mFilter = null;
            fillItems(mCursorActivity, mListId);
            // invalidate();
            return false;
        }
    }

}
