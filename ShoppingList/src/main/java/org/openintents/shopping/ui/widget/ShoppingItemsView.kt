package org.openintents.shopping.ui.widget

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.CursorLoader
import android.content.Intent
import android.content.Loader
import android.app.LoaderManager
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.widget.SearchView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.SimpleCursorAdapter
import android.widget.SimpleCursorAdapter.ViewBinder
import android.widget.TextView

import org.openintents.distribution.DownloadAppDialog
import org.openintents.shopping.R
import org.openintents.shopping.ShoppingApplication
import org.openintents.shopping.SyncSupport
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.Contains
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull
import org.openintents.shopping.library.provider.ShoppingContract.Status
import org.openintents.shopping.library.util.ShoppingUtils
import org.openintents.shopping.provider.ShoppingProvider
import org.openintents.shopping.theme.ThemeAttributes
import org.openintents.shopping.theme.ThemeShoppingList
import org.openintents.shopping.theme.ThemeUtils
import org.openintents.shopping.ui.PreferenceActivity
import org.openintents.shopping.ui.ShoppingActivity
import org.openintents.shopping.ui.ShoppingTotalsHandler
import org.openintents.shopping.ui.SnackbarUndoMultipleItemStatusOperation
import org.openintents.shopping.ui.SnackbarUndoSingleItemStatusOperation
import org.openintents.shopping.ui.UndoListener
import org.openintents.shopping.ui.dialog.EditItemDialog

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * View to show a shopping list with its items
 */
open class ShoppingItemsView : ListView, LoaderManager.LoaderCallbacks<Cursor> {

    @JvmField
    var mPriceVisibility: Int = 0
    @JvmField
    var mTagsVisibility: Int = 0
    @JvmField
    var mQuantityVisibility: Int = 0
    @JvmField
    var mUnitsVisibility: Int = 0
    @JvmField
    var mPriorityVisibility: Int = 0
    @JvmField
    var mTextTypeface: String? = null
    @JvmField
    var mTextSize: Float = 0f
    @JvmField
    var mTextUpperCaseFont: Boolean = false
    @JvmField
    var mTextColor: Int = 0
    @JvmField
    var mTextColorPrice: Int = 0
    @JvmField
    var mTextColorChecked: Int = 0
    @JvmField
    var mTextColorPriority: Int = 0
    @JvmField
    var mShowCheckBox: Boolean = false
    @JvmField
    var mShowStrikethrough: Boolean = false
    @JvmField
    var mTextSuffixUnchecked: String? = null
    @JvmField
    var mTextSuffixChecked: String? = null
    @JvmField
    var mBackgroundPadding: Int = 0
    @JvmField
    var mUpdateLastListPosition: Int = 0
    @JvmField
    var mLastListPosition: Int = 0
    @JvmField
    var mLastListTop: Int = 0
    @JvmField
    var mNumChecked: Long = 0
    @JvmField
    var mNumUnchecked: Long = 0
    private var mMode = MODE_IN_SHOP
    @JvmField
    var mModeBeforeSearch: Int = 0
    @JvmField
    var mCursorItems: Cursor? = null
    private var mCurrentTypeface: Typeface? = null
    private var mThemeAttributes: ThemeAttributes? = null
    private var mPackageManager: PackageManager? = null
    private var mPackageName: String? = null
    private val mPriceFormatter: NumberFormat = DecimalFormat
        .getNumberInstance(Locale.ENGLISH)
    private var mFilter: String? = null
    private var mInSearch = false
    private var mCursorActivity: Activity? = null

    private var mThemedBackground: View? = null
    private var mListId: Long = 0

    private var mFocusItemId: Long = -1

    private var mDefaultDivider: Drawable? = null

    private var mDragPos = 0 // which item is being dragged
    private var mFirstDragPos = 0 // where was the dragged item originally
    private var mDragPoint = 0 // at what offset inside the item did the user grab
    // it
    private var mCoordOffset = 0 // the difference between screen coordinates and
    // coordinates in this view

    private var mWindowManager: WindowManager? = null
    private var mWindowParams: WindowManager.LayoutParams? = null
    private val mTempRect = Rect()

    // dragging elements
    private var mDragBitmap: Bitmap? = null
    private var mDragView: ImageView? = null
    private var mHeight = 0
    private var mUpperBound = 0
    private var mLowerBound = 0
    private val mTouchSlop = 0
    private var mItemHeightHalf = 0
    private var mItemHeightNormal = 0
    private var mItemHeightExpanded = 0

    private var mDragListener: DragListener? = null
    private var mDropListener: DropListener? = null

    private var mActionBarListener: ActionBarListener? = null
    private var mUndoListener: UndoListener? = null
    private var mSnackbar: Snackbar? = null
    private var mSyncSupport: SyncSupport? = null
    private var mTotalsHandler: ShoppingTotalsHandler? = null
    private var mSearchView: SearchView? = null
    private var mListener: OnCustomClickListener? = null
    private var mModeChangeListener: OnModeChangeListener? = null
    private val mDragAndDropEnabled = false

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    fun getSearchView(): View? {
        val context = getContext()
        if (PreferenceActivity.getUsingHoloSearchFromPrefs(context)) {
            mSearchView = SearchView(mCursorActivity!!)
            mSearchView!!.setSubmitButtonEnabled(true)
            mSearchView!!.setInputType(PreferenceActivity.getSearchInputTypeFromPrefs(context))
            mSearchView!!.setOnQueryTextListener(SearchQueryListener())
            mSearchView!!.setOnCloseListener(SearchDismissedListener())
            mSearchView!!.setImeOptions(EditorInfo.IME_ACTION_UNSPECIFIED)
        }
        return mSearchView
    }

    private fun disposeItemsCursor() {
        if (mCursorActivity != null) {
            mCursorActivity!!.stopManagingCursor(mCursorItems)
            mCursorActivity = null
        }
        mCursorItems!!.deactivate()
        if (!mCursorItems!!.isClosed()) {
            mCursorItems!!.close()
        }
        mCursorItems = null
    }

    private fun init() {
        mItemHeightNormal = 45
        mItemHeightHalf = mItemHeightNormal / 2
        mItemHeightExpanded = 90

        // Remember standard divider
        mDefaultDivider = getDivider()
        mSyncSupport = (getContext().getApplicationContext() as ShoppingApplication).dependencies().getSyncSupport(getContext())
    }

    fun initTotals() {
        // Can't be called during init because that happens while
        // still inflating the parent activity, so findViewById
        // doesn't work yet.
        mTotalsHandler = ShoppingTotalsHandler(this)
    }

    fun setActionBarListener(listener: ActionBarListener?) {
        mActionBarListener = listener
    }

    fun setUndoListener(listener: UndoListener?) {
        mUndoListener = listener
    }

    fun onResume() {
        setFastScrollEnabled(PreferenceActivity.getFastScrollEnabledFromPrefs(getContext()))
    }

    fun onPause() {
    }

    fun getListId(): Long {
        return mListId
    }

    fun getInSearch(): Boolean {
        return mInSearch
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val loader = CursorLoader(mCursorActivity)
        createItemsCursor(mListId, loader)
        return loader
    }

    override fun onLoadFinished(loader: Loader<Cursor>, items: Cursor?) {

        // Get a cursor for all items that are contained
        // in currently selected shopping list.
        mCursorItems = items

        // Activate the following for a striped list.
        // setupListStripes(mListItems, this);

        if (mCursorItems == null) {
            Log.e(TAG, "missing shopping provider")
            setAdapter(ArrayAdapter(
                this.getContext(),
                android.R.layout.simple_list_item_1,
                arrayOf("no shopping provider")
            ))
            return
        }

        var layout_row = R.layout.list_item_shopping_item
        val size = PreferenceActivity.getFontSizeFromPrefs(getContext())
        if (size < 3) {
            layout_row = R.layout.list_item_shopping_item_small
        }

        var context = getContext()

        // If background is light, we apply the light holo theme to widgets.

        // determine color from text color:
        val gray = (Color.red(mTextColor) + Color.green(mTextColor) + Color.blue(mTextColor))
        if (gray < 3 * 128) {
            // dark text color <-> light background color => use light holo theme.
            context = ContextThemeWrapper(context, android.R.style.Theme_Holo_Light)
        }

        var adapter = getAdapter() as mSimpleCursorAdapter?

        if (adapter != null) {
            adapter.swapCursor(mCursorItems)
        } else {
            adapter = mSimpleCursorAdapter(
                context,  // Use a template that displays a text view
                layout_row,  // Give the cursor to the list adapter
                mCursorItems,  // Map the IMAGE and NAME to...
                arrayOf(
                    ContainsFull.ITEM_NAME, /*
                                                         * ContainsFull.ITEM_IMAGE
                                                         * ,
                                                         */
                    ContainsFull.ITEM_TAGS, ContainsFull.ITEM_PRICE,
                    ContainsFull.QUANTITY, ContainsFull.PRIORITY,
                    ContainsFull.ITEM_UNITS
                ),  // the view defined in the XML template
                intArrayOf(
                    R.id.name, /* R.id.image_URI, */R.id.tags,
                    R.id.price, R.id.quantity, R.id.priority, R.id.units
                )
            )
            setAdapter(adapter)
        }

        if (mFocusItemId != -1L) {
            // Set the item that we have just selected:
            // Get position of ID:
            mCursorItems!!.moveToPosition(-1)
            while (mCursorItems!!.moveToNext()) {
                if (mCursorItems!!.getLong(ShoppingActivity.mStringItemsITEMID) == mFocusItemId) {
                    var pos = mCursorItems!!.getPosition()
                    // scroll item near top, but not all the way to top, to provide context.
                    pos = Math.max(pos - 3, 0)
                    postDelayedSetSelection(pos)
                    break
                }
            }
            if (!mInSearch) {
                mFocusItemId = -1
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mCursorItems = null
    }

    /**
     * @param activity Activity to manage the cursor.
     * @param listId
     * @return
     */
    fun fillItems(activity: Activity, listId: Long) {

        // Keep the previous cursor until onLoadFinished() swaps in the new one:
        // the adapter still shows (and lets the user click) the old rows.
        mCursorActivity = activity

        mListId = listId

        setSearchModePref()
        activity.getLoaderManager().restartLoader(ShoppingActivity.LOADER_ITEMS, null, this)

        updateTotal()
    }

    private fun setSearchModePref() {
        // this is not a real user preference, just used to communicate
        // current app state to the content provider.
        val sp = mCursorActivity!!.getSharedPreferences(
            "org.openintents.shopping_preferences", Context.MODE_PRIVATE
        )
        val editor = sp.edit()
        editor.putBoolean("_searching", mInSearch)
        editor.apply()
    }

    private fun createItemsCursor(listId: Long, loader: CursorLoader?): Cursor? {
        val sortOrder = PreferenceActivity.getSortOrderFromPrefs(
            this.getContext(), mMode, listId
        )
        val hideBought = PreferenceActivity
            .getHideCheckedItemsFromPrefs(this.getContext())
        val selection: String
        val selection_args = arrayOf(listId.toString())
        if (mFilter != null) {
            selection = "list_id = ? AND " + ContainsFull.ITEM_NAME +
                    " like '%" + ShoppingProvider.escapeSQLChars(mFilter!!) + "%' ESCAPE '`'"
        } else if (inShopMode()) {
            if (hideBought) {
                selection = "list_id = ? AND " + Contains.STATUS +
                        " == " + Status.WANT_TO_BUY
            } else {
                selection = "list_id = ? AND " + Contains.STATUS +
                        " <> " + Status.REMOVED_FROM_LIST
            }
        } else {
            selection = "list_id = ? "
        }

        if (loader != null) {
            loader.setUri(ContainsFull.CONTENT_URI)
            loader.setProjection(ShoppingActivity.PROJECTION_ITEMS)
            loader.setSelection(selection)
            loader.setSelectionArgs(selection_args)
            loader.setSortOrder(sortOrder)
            return null
        }

        return getContext().getContentResolver().query(
            ContainsFull.CONTENT_URI, ShoppingActivity.PROJECTION_ITEMS,
            selection, selection_args, sortOrder
        )
    }

    /**
     * Set theme according to Id.
     *
     * @param themeName
     */
    fun setListTheme(themeName: String?) {
        val size = PreferenceActivity.getFontSizeFromPrefs(getContext())

        // backward compatibility:
        if (themeName == null) {
            setLocalStyle(R.style.Theme_ShoppingList, size)
        } else if (themeName == "1") {
            setLocalStyle(R.style.Theme_ShoppingList, size)
        } else if (themeName == "2") {
            setLocalStyle(R.style.Theme_ShoppingList_Classic, size)
        } else if (themeName == "3") {
            setLocalStyle(R.style.Theme_ShoppingList_Android, size)
        } else {
            // New styles:
            val themeFound = setRemoteStyle(themeName, size)

            if (!themeFound) {
                // Some error occured, let's use default style:
                setLocalStyle(R.style.Theme_ShoppingList, size)
            }
        }

        invalidate()
        if (mCursorItems != null) {
            requery()
        }
    }

    private fun setLocalStyle(styleResId: Int, size: Int) {
        val styleName = getResources().getResourceName(styleResId)

        val themefound = setRemoteStyle(styleName, size)

        if (!themefound) {
            // Actually this should never happen.
            Log.e(TAG, "Local theme not found: $styleName")
        }
    }

    private fun setRemoteStyle(styleName: String?, size: Int): Boolean {
        if (TextUtils.isEmpty(styleName)) {
            if (debug) {
                Log.e(TAG, "Empty style name: $styleName")
            }
            return false
        }

        mPackageManager = getContext().getPackageManager()

        mPackageName = ThemeUtils.getPackageNameFromStyle(styleName!!)

        if (mPackageName == null) {
            Log.e(TAG, "Invalid style name: $styleName")
            return false
        }

        val c: Context
        try {
            c = getContext().createPackageContext(mPackageName, 0)
        } catch (e: NameNotFoundException) {
            Log.e(
                TAG, "Package for style not found: " + mPackageName + ", " +
                        styleName
            )
            return false
        }

        val res = c.getResources()

        val themeid = res.getIdentifier(styleName, null, null)

        if (themeid == 0) {
            Log.e(TAG, "Theme name not found: $styleName")
            return false
        }

        try {
            mThemeAttributes = ThemeAttributes(c, mPackageName!!, themeid)

            mTextTypeface = mThemeAttributes!!.getString(ThemeShoppingList.textTypeface)
            mCurrentTypeface = createTypeface(mTextTypeface)

            mTextUpperCaseFont = mThemeAttributes!!.getBoolean(
                ThemeShoppingList.textUpperCaseFont, false
            )

            mTextColor = mThemeAttributes!!.getColor(
                ThemeShoppingList.textColor,
                android.R.color.white
            )

            mTextColorPrice = mThemeAttributes!!.getColor(
                ThemeShoppingList.textColorPrice,
                android.R.color.white
            )

            // Use color of price if color of priority has not been defined
            mTextColorPriority = mThemeAttributes!!.getColor(
                ThemeShoppingList.textColorPriority,
                mTextColorPrice
            )

            if (size == 0) {
                mTextSize = getTextSizeTiny(mThemeAttributes!!)
            } else if (size == 1) {
                mTextSize = getTextSizeSmall(mThemeAttributes!!)
            } else if (size == 2) {
                mTextSize = getTextSizeMedium(mThemeAttributes!!)
            } else {
                mTextSize = getTextSizeLarge(mThemeAttributes!!)
            }
            if (debug) {
                Log.d(TAG, "textSize: $mTextSize")
            }

            mTextColorChecked = mThemeAttributes!!.getColor(
                ThemeShoppingList.textColorChecked,
                android.R.color.white
            )
            mShowCheckBox = mThemeAttributes!!.getBoolean(ThemeShoppingList.showCheckBox, true)
            mShowStrikethrough = mThemeAttributes!!.getBoolean(
                ThemeShoppingList.textStrikethroughChecked, false
            )
            mTextSuffixUnchecked = mThemeAttributes!!
                .getString(ThemeShoppingList.textSuffixUnchecked)
            mTextSuffixChecked = mThemeAttributes!!
                .getString(ThemeShoppingList.textSuffixChecked)

            // field was named divider, until a conflict with the appcompat library
            // forced us to rename it. To continue to support old themes, check for
            // shopping_divider first, but if it's not found, check for divider also.
            var divider = mThemeAttributes!!.getInteger(ThemeShoppingList.shopping_divider, 0)
            if (divider == 0) {
                divider = mThemeAttributes!!.getInteger(ThemeShoppingList.divider, 0)
            }

            val div: Drawable?
            if (divider > 0) {
                div = getResources().getDrawable(divider)
            } else if (divider < 0) {
                div = null
            } else {
                div = mDefaultDivider
            }

            setDivider(div)

            return true
        } catch (e: UnsupportedOperationException) {
            // This exception is thrown e.g. if one attempts
            // to read an integer attribute as dimension.
            Log.e(TAG, "UnsupportedOperationException", e)
            return false
        } catch (e: NumberFormatException) {
            // This exception is thrown e.g. if one attempts
            // to read a string as integer.
            Log.e(TAG, "NumberFormatException", e)
            return false
        }
    }

    private fun createTypeface(typeface: String?): Typeface? {
        var newTypeface: Typeface? = null
        try {
            // Look for special cases:
            if ("monospace" == typeface) {
                newTypeface = Typeface.create(
                    Typeface.MONOSPACE,
                    Typeface.NORMAL
                )
            } else if ("sans" == typeface) {
                newTypeface = Typeface.create(
                    Typeface.SANS_SERIF,
                    Typeface.NORMAL
                )
            } else if ("serif" == typeface) {
                newTypeface = Typeface.create(
                    Typeface.SERIF,
                    Typeface.NORMAL
                )
            } else if (!TextUtils.isEmpty(typeface)) {
                try {
                    if (debug) {
                        Log.d(
                            TAG, "Reading typeface: package: " + mPackageName +
                                    ", typeface: " + typeface
                        )
                    }
                    val remoteRes = mPackageManager!!
                        .getResourcesForApplication(mPackageName!!)
                    newTypeface = Typeface.createFromAsset(
                        remoteRes
                            .getAssets(), typeface
                    )
                    if (debug) {
                        Log.d(TAG, "Result: $newTypeface")
                    }
                } catch (e: NameNotFoundException) {
                    Log.e(TAG, "Package not found for Typeface", e)
                }
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "type face can't be made $typeface")
        }
        return newTypeface
    }

    /**
     * Must be called after setListTheme();
     */
    fun applyListTheme() {

        if (mThemedBackground != null) {
            mBackgroundPadding = mThemeAttributes!!.getDimensionPixelOffset(
                ThemeShoppingList.backgroundPadding, -1
            )
            val backgroundPaddingLeft = mThemeAttributes!!.getDimensionPixelOffset(
                ThemeShoppingList.backgroundPaddingLeft,
                mBackgroundPadding
            )
            val backgroundPaddingTop = mThemeAttributes!!.getDimensionPixelOffset(
                ThemeShoppingList.backgroundPaddingTop,
                mBackgroundPadding
            )
            val backgroundPaddingRight = mThemeAttributes!!.getDimensionPixelOffset(
                ThemeShoppingList.backgroundPaddingRight,
                mBackgroundPadding
            )
            val backgroundPaddingBottom = mThemeAttributes!!.getDimensionPixelOffset(
                ThemeShoppingList.backgroundPaddingBottom,
                mBackgroundPadding
            )
            try {
                val remoteRes = mPackageManager!!
                    .getResourcesForApplication(mPackageName!!)
                val resid = mThemeAttributes!!.getResourceId(
                    ThemeShoppingList.background,
                    0
                )
                if (resid != 0) {
                    val d = remoteRes.getDrawable(resid)
                    mThemedBackground!!.setBackgroundDrawable(d)
                } else {
                    // remove background
                    mThemedBackground!!.setBackgroundResource(0)
                }
            } catch (e: NameNotFoundException) {
                Log.e(TAG, "Package not found for Theme background.", e)
            } catch (e: Resources.NotFoundException) {
                Log.e(TAG, "Resource not found for Theme background.", e)
            }

            // Apply padding
            if (mBackgroundPadding >= 0 || backgroundPaddingLeft >= 0 ||
                backgroundPaddingTop >= 0 ||
                backgroundPaddingRight >= 0 ||
                backgroundPaddingBottom >= 0
            ) {
                mThemedBackground!!.setPadding(
                    backgroundPaddingLeft,
                    backgroundPaddingTop, backgroundPaddingRight,
                    backgroundPaddingBottom
                )
            } else {
                // 9-patches do the padding automatically
                // todo clear padding
            }
        }
    }

    private fun getTextSizeTiny(ta: ThemeAttributes): Float {
        var size = ta.getDimensionPixelOffset(
            ThemeShoppingList.textSizeTiny,
            -1
        ).toFloat()
        if (size == -1f) {
            // Try to obtain from small:
            size = (12f / 18f) * getTextSizeSmall(ta)
        }
        return size
    }

    private fun getTextSizeSmall(ta: ThemeAttributes): Float {
        var size = ta.getDimensionPixelOffset(
            ThemeShoppingList.textSizeSmall, -1
        ).toFloat()
        if (size == -1f) {
            // Try to obtain from small:
            size = (18f / 23f) * getTextSizeMedium(ta)
        }
        return size
    }

    private fun getTextSizeMedium(ta: ThemeAttributes): Float {
        val scale = getResources().getDisplayMetrics().scaledDensity
        return ta.getDimensionPixelOffset(
            ThemeShoppingList.textSizeMedium, (23 * scale + 0.5f).toInt()
        ).toFloat()
    }

    private fun getTextSizeLarge(ta: ThemeAttributes): Float {
        var size = ta.getDimensionPixelOffset(
            ThemeShoppingList.textSizeLarge, -1
        ).toFloat()
        if (size == -1f) {
            // Try to obtain from small:
            size = (28f / 23f) * getTextSizeMedium(ta)
        }
        return size
    }

    fun setThemedBackground(background: View?) {
        mThemedBackground = background
    }

    /**
     * set the status of all items according to the parameter
     *
     * @param on if true all want_to_buy items are set to bought, if false all bought items are set to want_to_buy
     */
    fun toggleAllItems(on: Boolean) {
        val cursor = mCursorItems
        if (cursor == null || cursor.isClosed) {
            return
        }
        val op_type = if (on) SnackbarUndoMultipleItemStatusOperation.MARK_ALL else SnackbarUndoMultipleItemStatusOperation.UNMARK_ALL
        var op: SnackbarUndoMultipleItemStatusOperation? = null

        if (mUndoListener != null) {
            op = SnackbarUndoMultipleItemStatusOperation(
                this, mCursorActivity!!,
                op_type, mListId, false
            )
        }

        for (i in 0 until cursor.getCount()) {
            mCursorItems!!.moveToPosition(i)

            val oldstatus = mCursorItems!!
                .getLong(ShoppingActivity.mStringItemsSTATUS)

            // Toggle status ON:
            // bought -> bought
            // want_to_buy -> bought
            // removed_from_list -> removed_from_list

            // Toggle status OFF:
            // bought -> want_to_buy
            // want_to_buy -> want_to_buy
            // removed_from_list -> removed_from_list

            val newstatus: Long
            val doUpdate: Boolean
            if (on) {
                newstatus = ShoppingContract.Status.BOUGHT
                doUpdate = (oldstatus == ShoppingContract.Status.WANT_TO_BUY)
            } else {
                newstatus = ShoppingContract.Status.WANT_TO_BUY
                doUpdate = (oldstatus == ShoppingContract.Status.BOUGHT)
            }

            if (doUpdate) {
                val values = ContentValues()
                values.put(ShoppingContract.Contains.STATUS, newstatus)
                if (debug) {
                    Log.d(
                        TAG, "update row " + mCursorItems!!.getString(0) + ", newstatus " +
                                newstatus
                    )
                }
                val itemUri = Uri.withAppendedPath(
                    Contains.CONTENT_URI,
                    mCursorItems!!.getString(0)
                )
                getContext().getContentResolver().update(
                    itemUri, values, null, null
                )
                pushUpdatedItemToWear(values, itemUri)
            }
        }

        requery()

        invalidate()
        if (mUndoListener != null) {
            mUndoListener!!.onUndoAvailable(op)
        }
    }

    fun toggleItemBought(position: Int) {
        var shouldFocusItem = false

        val cursor = mCursorItems
        if (cursor == null || cursor.isClosed) {
            Log.e(TAG, "toggle item while list is reloading.")
            return
        }
        if (cursor.getCount() <= position) {
            Log.e(TAG, "toggle inexistent item. Probably clicked too quickly?")
            return
        }

        mCursorItems!!.moveToPosition(position)

        val oldstatus = mCursorItems!!
            .getLong(ShoppingActivity.mStringItemsSTATUS)

        // Toggle status depending on mode:
        var newstatus = ShoppingContract.Status.WANT_TO_BUY

        if (inShopMode()) {
            if (oldstatus == ShoppingContract.Status.WANT_TO_BUY) {
                newstatus = ShoppingContract.Status.BOUGHT
            } // else old was BOUGHT, new should be WANT_TO_BUY, which is the default.
        } else { // MODE_ADD_ITEMS or MODE_PICK_ITEMS_DLG
            // when we are in integrated add items mode, all three states
            // might be displayed, but the user can only create two of them.
            // want_to_buy-> removed_from_list
            // bought -> want_to_buy
            // removed_from_list -> want_to_buy
            if (oldstatus == ShoppingContract.Status.WANT_TO_BUY) {
                newstatus = ShoppingContract.Status.REMOVED_FROM_LIST
                shouldFocusItem = mInSearch && mFilter != null && mFilter!!.length > 0
            } else { // old is REMOVE_FROM_LIST or BOUGHT, new is WANT_TO_BUY, which is the default.
                if (mInSearch) {
                    shouldFocusItem = true
                }
            }
        }

        val contains_id = mCursorItems!!.getString(0)
        val values = ContentValues()
        values.put(ShoppingContract.Contains.STATUS, newstatus)
        if (debug) {
            Log.d(
                TAG, "update row " + mCursorItems!!.getString(0) + ", newstatus " +
                        newstatus
            )
        }

        if (mInSearch && newstatus == ShoppingContract.Status.WANT_TO_BUY) {
            val item_id = mCursorItems!!.getLong(ShoppingActivity.mStringItemsITEMID)
            ShoppingUtils.addDefaultsToAddedItem(getContext(), mListId, item_id)
        }

        if (shouldFocusItem) {
            mFocusItemId = mCursorItems!!.getLong(ShoppingActivity.mStringItemsITEMID)
        }

        val itemUri = Uri.withAppendedPath(Contains.CONTENT_URI, contains_id)
        getContext().getContentResolver().update(
            itemUri, values, null, null
        )

        pushUpdatedItemToWear(values, itemUri)

        val affectsSort = PreferenceActivity.prefsStatusAffectsSort(getContext(), mMode)
        val hidesItem = true /* TODO */
        if (mUndoListener != null && (affectsSort || hidesItem)) {
            val item_name = mCursorItems!!.getString(ShoppingActivity.mStringItemsITEMNAME)
            val op = SnackbarUndoSingleItemStatusOperation(
                this, getContext(),
                contains_id, item_name, oldstatus, newstatus, 0, false
            )
            mUndoListener!!.onUndoAvailable(op)
        }

        requery()

        if (affectsSort) {
            invalidate()
        }
    }

    fun cleanupList(): Boolean {

        val nothingdeleted: Boolean

        var op: SnackbarUndoMultipleItemStatusOperation? = null
        if (mUndoListener != null) {
            op = SnackbarUndoMultipleItemStatusOperation(
                this, mCursorActivity!!,
                SnackbarUndoMultipleItemStatusOperation.CLEAN_LIST, mListId, false
            )
        }

        // by changing state
        val values = ContentValues()
        values.put(Contains.STATUS, Status.REMOVED_FROM_LIST)
        if (PreferenceActivity.getResetQuantity(getContext())) {
            values.put(Contains.QUANTITY, "")
        }
        nothingdeleted = getContext().getContentResolver().update(
            Contains.CONTENT_URI,
            values,
            ShoppingContract.Contains.LIST_ID + " = " + mListId + " AND " +
                    ShoppingContract.Contains.STATUS + " = " +
                    ShoppingContract.Status.BOUGHT, null
        ) == 0

        requery()

        if (mUndoListener != null) {
            mUndoListener!!.onUndoAvailable(op)
        }

        return !nothingdeleted
    }

    /**
     * @param activity Activity to manage new Cursor.
     * @param newItem
     * @param quantity
     * @param price
     * @param barcode
     */
    fun insertNewItem(
        activity: Activity, newItem: String,
        quantity: String?, priority: String?, price: String?, barcode: String?
    ) {
        var newItem = newItem

        var list_id: String? = null
        if (PreferenceActivity.getCompleteFromCurrentListOnlyFromPrefs(getContext())) {
            list_id = mListId.toString()
        }

        newItem = newItem.trim()

        val itemId = ShoppingUtils.updateOrCreateItem(
            getContext(), newItem,
            null, price, barcode, list_id
        )

        if (debug) {
            Log.i(
                TAG, "Insert new item. " + " itemId = " + itemId + ", listId = " +
                        mListId
            )
        }
        val resetQuantity = PreferenceActivity.getResetQuantity(getContext())
        ShoppingUtils.addItemToList(
            getContext(), itemId, mListId, Status.WANT_TO_BUY,
            priority, quantity, false, false, resetQuantity
        )
        ShoppingUtils.addDefaultsToAddedItem(getContext(), mListId, itemId)
        mFocusItemId = itemId
        fillItems(activity, mListId)
    }

    fun isWearSupportAvailable(): Boolean {
        return mSyncSupport != null && mSyncSupport!!.isAvailable()
    }

    fun pushItemsToWear() {
        if (mSyncSupport!!.isAvailable()) {
            object : Thread() {
                override fun run() {
                    val cursor = createItemsCursor(mListId, null) ?: return
                    Log.d(TAG, "pushing " + cursor.getCount() + " items")
                    cursor.use {
                        while (it.moveToNext()) {
                            mSyncSupport!!.pushListItem(mListId, it)
                        }
                    }
                }
            }.start()
        }
    }

    private fun pushUpdatedItemToWear(values: ContentValues, itemUri: Uri) {
        if (mSyncSupport!!.isAvailable() && mSyncSupport!!.isSyncEnabled())
            object : Thread() {
                override fun run() {
                    mSyncSupport!!.updateListItem(mListId, itemUri, values)
                }
            }.start()
    }

    /**
     * Post setSelection delayed, because onItemSelected() may be called more
     * than once, leading to fillItems() being called more than once as well.
     * Posting delayed ensures that items added through intents that return
     * results (like a barcode scanner) are put into visible position.
     *
     * @param pos
     */
    fun postDelayedSetSelection(pos: Int) {
        // set immediately
        setSelection(pos)

        // if for any reason this does not work, a delayed version
        // will succeed:
        postDelayed({
            setSelection(pos)
        }, 1000)
    }

    fun requery() {
        if (debug) {
            Log.d(TAG, "requery()")
        }

        // Test for null pointer exception (issue 313)
        if (mCursorItems != null) {
            mCursorItems!!.requery()
            updateTotal()

            if (mUpdateLastListPosition > 0) {
                if (debug) {
                    Log.d(
                        TAG, "Restore list position: pos: " + mLastListPosition +
                                ", top: " + mLastListTop + ", tries: " + mUpdateLastListPosition
                    )
                }
                setSelectionFromTop(mLastListPosition, mLastListTop)
                mUpdateLastListPosition--
            }
        }
    }

    /**
     * Update the text fields for "Total:" and "Checked:" with corresponding
     * price information.
     */
    fun updateTotal() {
        if (debug) {
            Log.d(TAG, "updateTotal()")
        }
        mTotalsHandler!!.update(mCursorActivity!!.getLoaderManager(), mListId)
    }

    fun updateNumChecked(numChecked: Long, numUnchecked: Long) {

        mNumChecked = numChecked
        mNumUnchecked = numUnchecked

        // Update ActionBar in ShoppingActivity
        // for the "Clean up list" command
        if (mActionBarListener != null && !mInSearch) {
            mActionBarListener!!.updateActionBar()
        }
    }

    private fun getQuantityPrice(cursor: Cursor): Long {
        var price = cursor.getLong(ShoppingActivity.mStringItemsITEMPRICE)
        if (price != 0L) {
            val quantityString = cursor
                .getString(ShoppingActivity.mStringItemsQUANTITY)
            if (!TextUtils.isEmpty(quantityString)) {
                try {
                    val quantity = quantityString.toDouble()
                    price = (price * quantity).toLong()
                } catch (e: NumberFormatException) {
                    // do nothing
                }
            }
        }
        return price
    }

    fun setCustomClickListener(listener: OnCustomClickListener?) {
        mListener = listener
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {

        if (mDragAndDropEnabled) {
            if (mDragListener != null || mDropListener != null) {
                when (ev.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        val x = ev.getX().toInt()
                        val y = ev.getY().toInt()
                        val itemnum = pointToPosition(x, y)
                        if (itemnum == AdapterView.INVALID_POSITION) {
                            // break
                        } else {
                            val item = getChildAt(
                                itemnum
                                        - getFirstVisiblePosition()
                            ) as ViewGroup
                            mDragPoint = y - item.getTop()
                            mCoordOffset = (ev.getRawY().toInt()) - y
                            item.setDrawingCacheEnabled(true)
                            val bitmap = Bitmap.createBitmap(item.getDrawingCache())
                            startDragging(bitmap, y)
                            mDragPos = itemnum
                            mFirstDragPos = mDragPos
                            mHeight = getHeight()
                            val touchSlop = mTouchSlop
                            mUpperBound = Math.min(y - touchSlop, mHeight / 3)
                            mLowerBound = Math.max(y + touchSlop, mHeight * 2 / 3)
                            return false
                        }
                    }
                    else -> {
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun myPointToPosition(x: Int, y: Int): Int {
        if (y < 0) {
            val pos = myPointToPosition(x, y + mItemHeightNormal)
            if (pos > 0) {
                return pos - 1
            }
        }
        val frame = mTempRect
        val count = getChildCount()
        for (i in count - 1 downTo 0) {
            val child = getChildAt(i)
            child.getHitRect(frame)
            if (frame.contains(x, y)) {
                return getFirstVisiblePosition() + i
            }
        }
        return INVALID_POSITION
    }

    private fun getItemForPosition(y: Int): Int {
        val adjustedy = y - mDragPoint - mItemHeightHalf
        var pos = myPointToPosition(0, adjustedy)
        if (pos >= 0) {
            if (pos <= mFirstDragPos) {
                pos += 1
            }
        } else if (adjustedy < 0) {
            pos = 0
        }
        return pos
    }

    private fun adjustScrollBounds(y: Int) {
        if (y >= mHeight / 3) {
            mUpperBound = mHeight / 3
        }
        if (y <= mHeight * 2 / 3) {
            mLowerBound = mHeight * 2 / 3
        }
    }

    private fun unExpandViews(deletion: Boolean) {
        var i = 0
        while (true) {
            var v = getChildAt(i)
            if (v == null) {
                if (deletion) {
                    val position = getFirstVisiblePosition()
                    val y = getChildAt(0).getTop()
                    setAdapter(getAdapter())
                    setSelectionFromTop(position, y)
                }
                layoutChildren()
                v = getChildAt(i)
                if (v == null) {
                    break
                }
            }
            val params = v.getLayoutParams()
            params.height = mItemHeightNormal
            v.setLayoutParams(params)
            v.setVisibility(View.VISIBLE)
            i++
        }
    }

    private fun doExpansion() {
        var childnum = mDragPos - getFirstVisiblePosition()
        if (mDragPos > mFirstDragPos) {
            childnum++
        }

        val first = getChildAt(mFirstDragPos - getFirstVisiblePosition())

        var i = 0
        while (true) {
            val vv = getChildAt(i)
            if (vv == null) {
                break
            }
            var height = mItemHeightNormal
            var visibility = View.VISIBLE
            if (vv == first) {
                if (mDragPos == mFirstDragPos) {
                    visibility = View.INVISIBLE
                } else {
                    height = 1
                }
            } else if (i == childnum) {
                if (mDragPos < getCount() - 1) {
                    height = mItemHeightExpanded
                }
            }
            val params = vv.getLayoutParams()
            params.height = height
            vv.setLayoutParams(params)
            vv.setVisibility(visibility)
            i++
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {

        if ((mDragListener != null || mDropListener != null) &&
            mDragView != null
        ) {
            val action = ev.getAction()
            when (action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val r = mTempRect
                    mDragView!!.getDrawingRect(r)
                    stopDragging()
                    if (mDropListener != null && mDragPos >= 0 &&
                        mDragPos < getCount()
                    ) {
                        mDropListener!!.drop(mFirstDragPos, mDragPos)
                    }
                    unExpandViews(false)
                }
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val x = ev.getX().toInt()
                    val y = ev.getY().toInt()
                    dragView(x, y)
                    val itemnum = getItemForPosition(y)
                    if (itemnum >= 0) {
                        if (action == MotionEvent.ACTION_DOWN || itemnum != mDragPos) {
                            if (mDragListener != null) {
                                mDragListener!!.drag(mDragPos, itemnum)
                            }
                            mDragPos = itemnum
                            doExpansion()
                        }
                        var speed = 0
                        adjustScrollBounds(y)
                        if (y > mLowerBound) {
                            // scroll the list up a bit
                            speed = if (y > (mHeight + mLowerBound) / 2) 16 else 4
                        } else if (y < mUpperBound) {
                            // scroll the list down a bit
                            speed = if (y < mUpperBound / 2) -16 else -4
                        }
                        if (speed != 0) {
                            var ref = pointToPosition(0, mHeight / 2)
                            if (ref == AdapterView.INVALID_POSITION) {
                                // we hit a divider or an invisible view, check
                                // somewhere else
                                ref = pointToPosition(
                                    0, mHeight / 2 +
                                            getDividerHeight() + 64
                                )
                            }
                            val v = getChildAt(ref - getFirstVisiblePosition())
                            if (v != null) {
                                val pos = v.getTop()
                                setSelectionFromTop(ref, pos - speed)
                            }
                        }
                    }
                }
                else -> {
                }
            }
            return true
        }
        return super.onTouchEvent(ev)
    }

    private fun startDragging(bm: Bitmap, y: Int) {
        stopDragging()

        mWindowParams = WindowManager.LayoutParams()
        mWindowParams!!.gravity = Gravity.TOP
        mWindowParams!!.x = 0
        mWindowParams!!.y = y - mDragPoint + mCoordOffset

        mWindowParams!!.height = WindowManager.LayoutParams.WRAP_CONTENT
        mWindowParams!!.width = WindowManager.LayoutParams.WRAP_CONTENT
        mWindowParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        mWindowParams!!.format = PixelFormat.TRANSLUCENT
        mWindowParams!!.windowAnimations = 0

        val context = getContext()
        val v = ImageView(context)
        val backGroundColor = context.getResources()
            .getColor(R.color.darkgreen)
        v.setBackgroundColor(backGroundColor)
        v.setImageBitmap(bm)
        mDragBitmap = bm

        mWindowManager = context
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mWindowManager!!.addView(v, mWindowParams)
        mDragView = v
    }

    private fun dragView(x: Int, y: Int) {
        mWindowParams!!.y = y - mDragPoint + mCoordOffset
        mWindowManager!!.updateViewLayout(mDragView, mWindowParams)
    }

    private fun stopDragging() {
        if (mDragView != null) {
            val wm = getContext().getSystemService(
                Context.WINDOW_SERVICE
            ) as WindowManager
            wm.removeView(mDragView)
            mDragView!!.setImageDrawable(null)
            mDragView = null
        }
        if (mDragBitmap != null) {
            mDragBitmap!!.recycle()
            mDragBitmap = null
        }
    }

    fun setDragListener(l: DragListener?) {
        mDragListener = l
    }

    fun setDropListener(l: DropListener?) {
        mDropListener = l
    }

    fun setOnModeChangeListener(listener: OnModeChangeListener?) {
        mModeChangeListener = listener
    }

    fun setModes(mode: Int, modeBeforeSearch: Int) {
        mMode = mode
        mModeBeforeSearch = modeBeforeSearch
    }

    fun setPickItemsDlgMode() {
        mMode = MODE_PICK_ITEMS_DLG
    }

    fun setAddItemsMode() {
        mMode = MODE_ADD_ITEMS
    }

    fun setInShopMode() {
        mMode = MODE_IN_SHOP
    }

    fun getMode(): Int {
        return mMode
    }

    interface OnCustomClickListener {
        fun onCustomClick(c: Cursor?, pos: Int, field: EditItemDialog.FieldType?, v: View?)
    }

    interface DragListener {
        fun drag(from: Int, to: Int)
    }

    interface DropListener {
        fun drop(from: Int, to: Int)
    }

    interface RemoveListener {
        fun remove(which: Int)
    }

    interface ActionBarListener {
        fun updateActionBar()
    }

    interface OnModeChangeListener {
        fun onModeChanged()
    }

    /**
     * Extend the SimpleCursorAdapter to strike through items. if STATUS ==
     * Shopping.Status.BOUGHT
     */
    inner class mSimpleCursorAdapter
    /**
     * Constructor simply calls super class.
     *
     * @param context Context.
     * @param layout  Layout.
     * @param c       Cursor.
     * @param from    Projection from.
     * @param to      Projection to.
     */
        (
        context: Context, layout: Int,
        c: Cursor?, from: Array<String>, to: IntArray
    ) : SimpleCursorAdapter(context, layout, c, from, to), ViewBinder {

        init {
            super.setViewBinder(this)

            mPriceFormatter.setMaximumFractionDigits(2)
            mPriceFormatter.setMinimumFractionDigits(2)
        }

        override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
            val view = super.newView(context, cursor, parent)
            val rowState = mItemRowState(view) // sets view tags
            return view
        }

        /**
         * Additionally to the standard bindView, we also check for STATUS, and
         * strike the item through if BOUGHT.
         */
        override fun bindView(
            view: View, context: Context,
            cursor: Cursor
        ) {
            super.bindView(view, context, cursor)

            val status = cursor.getLong(ShoppingActivity.mStringItemsSTATUS)
            val state = view.getTag() as mItemRowState
            state.mCursorPos = cursor.getPosition()
            state.mCursor = cursor


            // set style for name view and friends
            val styled_as_name = arrayOf(state.mNameView, state.mUnitsView, state.mQuantityView)
            var i = 0
            while (i < styled_as_name.size) {
                val t = styled_as_name[i]

                // Set font
                if (mCurrentTypeface != null) {
                    t!!.setTypeface(mCurrentTypeface)
                }

                // Set size
                t!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize)

                // Check for upper case:
                if (mTextUpperCaseFont) {
                    // Only upper case should be displayed
                    val cs = t.getText()
                    t.setText(cs.toString().uppercase())
                }

                t.setTextColor(mTextColor)

                if (status == ShoppingContract.Status.BOUGHT) {
                    t.setTextColor(mTextColorChecked)

                    if (mShowStrikethrough) {
                        // We have bought the item,
                        // so we strike it through:

                        // First convert text to 'spannable'
                        t.setText(t.getText(), TextView.BufferType.SPANNABLE)
                        val str = t.getText() as Spannable

                        // Strikethrough
                        str.setSpan(
                            StrikethroughSpan(), 0, str.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

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
                        t.append(mTextSuffixChecked)
                    }
                } else {
                    // item not bought:
                    if (i == 0 && mTextSuffixUnchecked != null) {
                        t.append(mTextSuffixUnchecked)
                    }
                }
                i++
            }

            // we have a check box now.. more visual and gets the point across

            if (debug) {
                Log.i(TAG, "bindview: pos = " + cursor.getPosition())
            }

            // set style for check box

            if (mShowCheckBox) {
                state.mCheckView!!.setVisibility(CheckBox.VISIBLE)
                state.mCheckView!!.setChecked(status == ShoppingContract.Status.BOUGHT)
            } else {
                state.mCheckView!!.setVisibility(CheckBox.GONE)
            }

            if (inShopMode()) {
                state.mNoCheckView!!.setVisibility(ImageView.GONE)
            } else {  // mMode == ShoppingActivity.MODE_ADD_ITEMS
                if (status == ShoppingContract.Status.REMOVED_FROM_LIST) {
                    state.mNoCheckView!!.setVisibility(ImageView.VISIBLE)
                    if (mShowCheckBox) {
                        // replace check box
                        state.mCheckView!!.setVisibility(CheckBox.INVISIBLE)
                    }
                } else {
                    state.mNoCheckView!!.setVisibility(ImageView.INVISIBLE)
                }
            }
        }

        private fun hideTextView(view: TextView) {
            view.setVisibility(View.GONE)
            view.setText("")
        }

        override fun setViewValue(view: View, cursor: Cursor, i: Int): Boolean {
            val id = view.getId()
            var price: Long = 0
            var hasPrice = false
            var tags: String? = null
            var priceString: String? = null
            var hasTags = false
            val state = view.getTag() as mItemRowState
            if (mPriceVisibility == View.VISIBLE) {
                price = getQuantityPrice(cursor)
                hasPrice = (price != 0L)
            }
            if (mTagsVisibility == View.VISIBLE) {
                tags = cursor.getString(ShoppingActivity.mStringItemsITEMTAGS)
                hasTags = !TextUtils.isEmpty(tags)
            }

            if (id == R.id.name) {
                val hasNote = cursor
                    .getInt(ShoppingActivity.mStringItemsITEMHASNOTE) != 0
                val name = cursor
                    .getString(ShoppingActivity.mStringItemsITEMNAME)
                val tv = view as TextView
                val name_etc = SpannedStringBuilder()
                name_etc.appendSpannedString(ClickableItemSpan(), name)
                if (name.equals(mFilter, ignoreCase = true)) {
                    name_etc.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, name_etc.length, 0)
                }
                if (hasNote) {
                    val d = getResources().getDrawable(R.drawable.ic_launcher_notepad_small)
                    val ratio = (d.getIntrinsicWidth() / d.getIntrinsicHeight()).toFloat()
                    d.setBounds(0, 0, (ratio * mTextSize).toInt(), mTextSize.toInt())
                    val noteimgspan = ImageSpan(d, ImageSpan.ALIGN_BASELINE)
                    name_etc.appendSpannedString(noteimgspan, ClickableNoteSpan(), " ")
                }

                if (hasPrice) {
                    // set price text while setting name, so that correct size is known below
                    priceString = mPriceFormatter.format(price * 0.01)
                    state.mPriceView!!.setText(priceString)
                }
                if (hasPrice && !hasTags) {
                    val paint = state.mPriceView!!.getPaint()
                    val bounds = Rect()
                    val price_overlay = ColorDrawable()
                    price_overlay.setAlpha(0)
                    paint.getTextBounds(priceString, 0, priceString!!.length, bounds)
                    price_overlay.setBounds(0, 0, bounds.width(), bounds.height())
                    val priceimgspan = ImageSpan(price_overlay, ImageSpan.ALIGN_BASELINE)
                    name_etc.appendSpannedString(priceimgspan, " ")
                }
                tv.setText(name_etc)
                tv.setMovementMethod(LinkMovementMethod.getInstance())
                return true
            } else if (id == R.id.price) {
                val tv = view as TextView
                if (hasPrice) {
                    tv.setVisibility(View.VISIBLE)
                    tv.setTextColor(mTextColorPrice)
                } else {
                    hideTextView(tv)
                }
                return true
            } else if (id == R.id.tags) {
                val tv = view as TextView
                if (hasTags) {
                    tv.setVisibility(View.VISIBLE)
                    tv.setTextColor(mTextColorPrice)
                    tv.setText(tags)
                    if (hasPrice) {
                        // don't overlap the price
                        val rlp = tv.getLayoutParams() as RelativeLayout.LayoutParams
                        rlp.addRule(RelativeLayout.LEFT_OF, R.id.price)
                    }
                } else {
                    hideTextView(tv)
                }
                return true
            } else if (id == R.id.quantity) {
                val quantity = cursor.getString(ShoppingActivity.mStringItemsQUANTITY)
                val tv = view as TextView
                if (mQuantityVisibility == View.VISIBLE &&
                    !TextUtils.isEmpty(quantity)
                ) {
                    tv.setVisibility(View.VISIBLE)
                    // tv.setTextColor(mPriceTextColor);
                    tv.setText("$quantity ")
                } else {
                    hideTextView(tv)
                }
                return true
            } else if (id == R.id.units) {
                val units = cursor.getString(ShoppingActivity.mStringItemsITEMUNITS)
                val quantity = cursor.getString(ShoppingActivity.mStringItemsQUANTITY)
                val tv = view as TextView
                // looks more natural if you only show units when showing qty.
                if (mUnitsVisibility == View.VISIBLE &&
                    mQuantityVisibility == View.VISIBLE &&
                    !TextUtils.isEmpty(units) && !TextUtils.isEmpty(quantity)
                ) {
                    tv.setVisibility(View.VISIBLE)
                    // tv.setTextColor(mPriceTextColor);
                    tv.setText("$units ")
                } else {
                    hideTextView(tv)
                }
                return true
            } else if (id == R.id.priority) {
                val priority = cursor.getString(ShoppingActivity.mStringItemsPRIORITY)
                val tv = view as TextView
                if (mPriorityVisibility == View.VISIBLE &&
                    !TextUtils.isEmpty(priority)
                ) {
                    tv.setVisibility(View.VISIBLE)
                    tv.setTextColor(mTextColorPriority)
                    tv.setText("-$priority- ")
                } else {
                    hideTextView(tv)
                }
                return true
            } else {
                return false
            }
        }

        override fun setViewBinder(viewBinder: ViewBinder?) {
            throw RuntimeException("this adapter implements setViewValue")
        }

        inner class mItemRowState(view: View) {
            @JvmField
            var mParentView: View
            @JvmField
            var mNameView: TextView?
            @JvmField
            var mQuantityView: TextView?
            @JvmField
            var mUnitsView: TextView?
            @JvmField
            var mPriceView: TextView?
            @JvmField
            var mPriorityView: TextView?
            @JvmField
            var mTagsView: TextView?
            @JvmField
            var mCheckView: CheckBox?
            @JvmField
            var mNoCheckView: ImageView?

            @JvmField
            var mCursor: Cursor? = null
            @JvmField
            var mCursorPos = 0

            init {
                // This class is here to initialize state information related
                // to a single reusable item row, to reduce the amount of
                // setup that needs to be done each time the row is reused.
                //
                // Callbacks can be bound up-front here if they depend on cursor position.

                mParentView = view
                mNameView = view.findViewById<View>(R.id.name) as TextView
                mPriceView = view.findViewById<View>(R.id.price) as TextView
                mTagsView = view.findViewById<View>(R.id.tags) as TextView
                mQuantityView = view.findViewById<View>(R.id.quantity) as TextView
                mUnitsView = view.findViewById<View>(R.id.units) as TextView
                mPriorityView = view.findViewById<View>(R.id.priority) as TextView
                mCheckView = view.findViewById<View>(R.id.check) as CheckBox
                mNoCheckView = view.findViewById<View>(R.id.nocheck) as ImageView

                mParentView.setTag(this)
                mNameView!!.setTag(this)
                mPriceView!!.setTag(this)
                mTagsView!!.setTag(this)
                mQuantityView!!.setTag(this)
                mUnitsView!!.setTag(this)
                mPriorityView!!.setTag(this)
                mCheckView!!.setTag(this)
                mNoCheckView!!.setTag(this)

                mQuantityView!!.setOnClickListener(
                    mItemClickListener(
                        "Quantity Click ",
                        EditItemDialog.FieldType.QUANTITY
                    )
                )
                mPriceView!!.setOnClickListener(
                    mItemClickListener(
                        "Click on price: ",
                        EditItemDialog.FieldType.PRICE
                    )
                )
                mUnitsView!!.setOnClickListener(
                    mItemClickListener(
                        "Click on units: ",
                        EditItemDialog.FieldType.UNITS
                    )
                )
                mPriorityView!!.setOnClickListener(
                    mItemClickListener(
                        "Click on priority: ",
                        EditItemDialog.FieldType.PRIORITY
                    )
                )
                mTagsView!!.setOnClickListener(
                    mItemClickListener(
                        "Click on tags: ",
                        EditItemDialog.FieldType.TAGS
                    )
                )

                mCheckView!!.setOnClickListener(mItemToggleListener("Click: "))
                // also check around check box
                val l = view.findViewById<View>(R.id.check_surround) as RelativeLayout
                l.setTag(this)
                l.setOnClickListener(mItemToggleListener("Click around: "))

                // Check for clicks on and around item text
                val r = view.findViewById<View>(R.id.description) as RelativeLayout
                r.setTag(this)
                r.setOnClickListener(
                    mItemClickListener(
                        "Click on description: ",
                        EditItemDialog.FieldType.ITEMNAME
                    )
                )

                mPriceView!!.setVisibility(mPriceVisibility)
                mTagsView!!.setVisibility(mTagsVisibility)
                mQuantityView!!.setVisibility(mQuantityVisibility)
                mUnitsView!!.setVisibility(mUnitsVisibility)
                mPriorityView!!.setVisibility(mPriorityVisibility)
            }

            private inner class mItemClickListener(
                private val mLogMessage: String,
                private val mFieldType: EditItemDialog.FieldType
            ) : OnClickListener {

                override fun onClick(v: View) {
                    if (debug) {
                        Log.d(TAG, mLogMessage)
                    }
                    if (mListener != null) {
                        val state = v.getTag() as mItemRowState
                        mListener!!.onCustomClick(
                            state.mCursor, state.mCursorPos,
                            mFieldType, v
                        )
                    }
                }
            }

            private inner class mItemToggleListener(private val mLogMessage: String) : OnClickListener {

                override fun onClick(v: View) {
                    if (debug) {
                        Log.d(TAG, mLogMessage)
                    }
                    val state = v.getTag() as mItemRowState
                    toggleItemBought(state.mCursorPos)
                }
            }
        }

        private inner class ClickableNoteSpan : ClickableSpan() {
            override fun onClick(view: View) {
                val i = Intent(Intent.ACTION_VIEW)
                val state = view.getTag() as mItemRowState
                val cursorpos = state.mCursorPos
                if (debug) {
                    Log.d(TAG, "Click on has_note: $cursorpos")
                }
                val cursor = mCursorItems
                if (cursor == null || cursor.isClosed || !cursor.moveToPosition(cursorpos)) {
                    return
                }
                val note_id = cursor.getLong(ShoppingActivity.mStringItemsITEMID)
                val uri = ContentUris.withAppendedId(ShoppingContract.Notes.CONTENT_URI, note_id)
                i.setData(uri)
                val context = getContext()
                try {
                    context.startActivity(i)
                } catch (e: ActivityNotFoundException) {
                    // we could add a simple edit note dialog, but for now...
                    val g: Dialog = DownloadAppDialog(
                        context,
                        R.string.notepad_not_available,
                        R.string.notepad,
                        R.string.notepad_package,
                        R.string.notepad_website
                    )
                    g.show()
                }
            }
        }

        private inner class ClickableItemSpan : ClickableSpan() {
            override fun onClick(view: View) {
                if (debug) {
                    Log.d(TAG, "Click on description: ")
                }
                if (mListener != null) {
                    val state = view.getTag() as mItemRowState
                    val cursorpos = state.mCursorPos
                    mListener!!.onCustomClick(
                        mCursorItems, cursorpos,
                        EditItemDialog.FieldType.ITEMNAME, view
                    )
                }
            }

            override fun updateDrawState(ds: TextPaint) {
                // Override the parent's method to avoid having the text
                // in this span look like a link.
            }
        }

        private inner class SpannedStringBuilder : SpannableStringBuilder() {
            fun appendSpannedString(o: Any, text: CharSequence): SpannedStringBuilder {
                val spanStart = length
                super.append(text)
                setSpan(o, spanStart, spanStart + text.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                return this
            }

            fun appendSpannedString(o: Any, p: Any, text: CharSequence): SpannedStringBuilder {
                val spanStart = length
                super.append(text)
                setSpan(o, spanStart, spanStart + text.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                setSpan(p, spanStart, spanStart + text.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                return this
            }
        }
    }

    fun inShopMode(): Boolean {
        return mMode == MODE_IN_SHOP
    }

    fun inAddItemsMode(): Boolean {
        return mMode == MODE_ADD_ITEMS
    }

    fun inPickItemsDialogMode(): Boolean {
        return mMode == MODE_PICK_ITEMS_DLG
    }

    private inner class SearchQueryListener : SearchView.OnQueryTextListener {
        override fun onQueryTextChange(query: String?): Boolean {
            var query = query
            val isIconified = mSearchView!!.isIconified()
            val prevFilter = mFilter

            if (isIconified) {
                // Something tries to restore the query text after the drawer is dismissed, but
                // it doesn't re-expand the search view. Force the query string empty when it is
                // not shown, and switch back to non-search mode.
                if (query != null && query.length > 0) {
                    mSearchView!!.setQuery("", false)
                }
                query = null
                if (mInSearch) {
                    mMode = mModeBeforeSearch
                    mInSearch = false
                }
            }

            if (mInSearch == false && !isIconified) {
                mInSearch = true
                mModeBeforeSearch = mMode
                mMode = MODE_ADD_ITEMS
            }

            if (query == null || query.length == 0) {
                mFilter = null
            } else {
                mFilter = query
            }

            if ((prevFilter == null && mFilter == null) ||
                (prevFilter != null && prevFilter == mFilter)
            ) {
                return true
            }

            fillItems(mCursorActivity!!, mListId)

            return true
        }

        override fun onQueryTextSubmit(query: String?): Boolean {
            if (query!!.length > 0) {
                insertNewItem(mCursorActivity!!, query, null, null, null, null)
                mSearchView!!.setQuery("", false)
                fillItems(mCursorActivity!!, mListId)
            }
            return true
        }
    }

    private inner class SearchDismissedListener : SearchView.OnCloseListener {
        override fun onClose(): Boolean {
            if (mInSearch) {
                mMode = mModeBeforeSearch
                if (mModeChangeListener != null) {
                    mModeChangeListener!!.onModeChanged()
                }
            }
            mInSearch = false
            mFilter = null
            fillItems(mCursorActivity!!, mListId)
            // invalidate();
            return false
        }
    }

    companion object {
        /**
         * mode: separate dialog to add items from existing list
         */
        private const val MODE_PICK_ITEMS_DLG = 3

        /**
         * mode: add items from existing list
         */
        private const val MODE_ADD_ITEMS = 2

        /**
         * mode: I am in the shop
         */
        const val MODE_IN_SHOP = 1

        private const val TAG = "ShoppingListView"
        private const val debug = false
    }
}
