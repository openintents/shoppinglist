package org.openintents.shopping.ui.widget

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.database.DatabaseUtils
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.ContextMenu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.TextView

import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.ItemStores
import org.openintents.shopping.library.provider.ShoppingContract.Stores
import org.openintents.shopping.library.util.PriceConverter
import org.openintents.shopping.library.util.ShoppingUtils
import org.openintents.shopping.ui.PreferenceActivity

/**
 * View to show a list of stores for a specific item
 */
class StoreListView : ListView {

    companion object {
        private const val TAG = "StoreListView"
        private const val debug = false
        private const val cursorColumnID = 0
        private const val cursorColumnNAME = 1
        private const val cursorColumnSTOCKS_ITEM = 2
        private const val cursorColumnPRICE = 3
        private const val cursorColumnAISLE = 4
        private const val cursorColumnSTORE_ID = 5
    }

    private val mStringItems = arrayOf(
            "itemstores." + ItemStores._ID, Stores.NAME,
            ItemStores.STOCKS_ITEM, ItemStores.PRICE, ItemStores.AISLE,
            "stores._id as store_id")

    @JvmField var mPriceVisibility: Int = 0
    @JvmField var mTextTypeface: String? = null
    @JvmField var mTextSize: Float = 0f
    @JvmField var mTextUpperCaseFont: Boolean = false
    @JvmField var mTextColor: Int = 0
    @JvmField var mTextColorPrice: Int = 0
    @JvmField var mTextColorChecked: Int = 0
    @JvmField var mShowCheckBox: Boolean = false
    @JvmField var mInTextInput: Boolean = false
    @JvmField var mBinding: Boolean = false

    private var mCurrentTypeface: Typeface? = null
    private var mTextChanged: Boolean = false
    private var mCursorItemstores: Cursor? = null
    private var mItemId: Long = 0
    private var mListId: Long = 0
    private var mBackup: Array<ContentValues?>? = null
    private var mDirty: Boolean = false

    private var m_lastView: EditText? = null
    private var m_lastCol: Int = 0

    private val mContentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            mDirty = true
            if (mCursorItemstores != null && !mInTextInput) {
                try {
                    requery()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "IllegalStateException ", e)
                    // Somehow the logic is not completely right yet...
                    mCursorItemstores = null
                }
            }
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    fun applyUpdate() {
        if (m_lastView == null) {
            return
        }
        var `val` = m_lastView!!.text.toString()
        if (m_lastCol == cursorColumnPRICE) {
            `val` = PriceConverter.getCentPriceFromString(`val`).toString()
        }
        val row = m_lastView!!.tag as? Int
        if (row != null) {
            if (debug) {
                Log.d(TAG, "Text changed to " + `val` + " @ pos " + row
                        + ", col " + m_lastCol)
            }
            maybeUpdate(row, m_lastCol, `val`)
        }
        m_lastView = null
    }

    private fun init() {

    }

    fun onResume() {

        // Content observer registered at fillItems()
        // registerContentObserver();
    }

    fun onPause() {
        unregisterContentObserver()
    }

    private fun backupValues() {
        val nRows = mCursorItemstores!!.count
        if (mBackup != null) {
            return
        }
        mBackup = arrayOfNulls(nRows)
        var i = 0
        while (mCursorItemstores!!.moveToNext()) {
            mBackup!![i] = ContentValues()
            DatabaseUtils.cursorRowToContentValues(mCursorItemstores, mBackup!![i++])
        }
    }

    fun undoChanges() {
        for (i in mBackup!!.indices) {
            val cv = mBackup!![i]!!
            val storeId = cv.getAsString("store_id")

            if (cv.getAsString("_id") == null) {
                // dummy record. delete any itemstores for this item id and
                // store id.
                // (may have been created during editing)
                val cr = context.contentResolver
                val existingItems = cr.query(ItemStores.CONTENT_URI,
                        arrayOf(ItemStores._ID),
                        "store_id = ? AND item_id = ?", arrayOf(storeId,
                                mItemId.toString()), null
                )
                if (existingItems != null) {
                    if (existingItems.count > 0) {
                        existingItems.moveToFirst()
                        val id = existingItems.getLong(cursorColumnID)
                        cr.delete(
                                ItemStores.CONTENT_URI.buildUpon()
                                        .appendPath(id.toString()).build(),
                                null, null
                        )
                    }
                    existingItems.close()
                }
            } else {
                // real record, restore its values.
                // long itemstore_id = cv.getAsLong("_id");
                val has_item = cv.getAsBoolean("stocks_item")!!
                val price = cv.getAsString("price")
                val aisle = cv.getAsString("aisle")
                ShoppingUtils.addItemToStore(context, mItemId,
                        storeId.toLong(), has_item, aisle, price, false)
            }
        }
    }

    /**
     * @param activity Activity to manage the cursor.
     * @param listId
     * @return
     */
    fun fillItems(activity: Activity, listId: Long, itemId: Long): Cursor? {

        mListId = listId
        mItemId = itemId
        val sortOrder = "stores.name"

        if (mCursorItemstores != null && !mCursorItemstores!!.isClosed) {
            mCursorItemstores!!.close()
        }

        // Get a cursor for all stores
        mCursorItemstores = context.contentResolver.query(
                ItemStores.CONTENT_URI.buildUpon().appendPath("item")
                        .appendPath(mItemId.toString())
                        .appendPath(mListId.toString()).build(),
                mStringItems, null, null, sortOrder
        )
        activity.startManagingCursor(mCursorItemstores)

        registerContentObserver()

        if (mCursorItemstores == null) {
            Log.e(TAG, "missing shopping provider")
            setAdapter(ArrayAdapter(this.context,
                    android.R.layout.simple_list_item_1,
                    arrayOf("no shopping provider")))
            return mCursorItemstores
        }

        backupValues()

        val layout_row = R.layout.list_item_store
        mPriceVisibility = if (PreferenceActivity
                .getUsingPerStorePricesFromPrefs(context)) View.VISIBLE
        else View.INVISIBLE

        val adapter = mSimpleCursorAdapter(
                this.context,
                // Use a template that displays a text view
                layout_row,
                // Give the cursor to the list adapter
                mCursorItemstores,
                // Map the IMAGE and NAME to...
                arrayOf(Stores.NAME, ItemStores.PRICE, ItemStores.AISLE),
                // the view defined in the XML template
                intArrayOf(R.id.name, R.id.price, R.id.aisle))
        setAdapter(adapter)

        return mCursorItemstores
    }

    /**
     *
     */
    private fun registerContentObserver() {
        context.contentResolver
                .registerContentObserver(
                        ShoppingContract.ItemStores.CONTENT_URI, true,
                        mContentObserver)
    }

    private fun unregisterContentObserver() {
        context.contentResolver.unregisterContentObserver(
                mContentObserver)
    }

    fun toggleItemstore(position: Int) {
        if (mCursorItemstores!!.count <= position) {
            Log.e(TAG, "toggle inexistent item. Probably clicked too quickly?")
            return
        }

        mCursorItemstores!!.moveToPosition(position)

        var oldstatus: Long = 0

        // should first check if the itemstore record exists...
        val itemstore_id: String

        if (mCursorItemstores!!.isNull(0)) {
            val storeId = mCursorItemstores!!.getLong(cursorColumnSTORE_ID)
            val isid = ShoppingUtils.addItemToStore(context, mItemId,
                    storeId, "", "", false)
            itemstore_id = isid.toString()
        } else {
            itemstore_id = mCursorItemstores!!.getString(cursorColumnID)
            oldstatus = mCursorItemstores!!.getLong(cursorColumnSTOCKS_ITEM)
        }

        // Toggle status:
        val newstatus = 1 - oldstatus

        val values = ContentValues()
        values.put(ItemStores.STOCKS_ITEM, newstatus)
        if (debug) {
            Log.d(TAG, "update row " + itemstore_id + ", newstatus "
                    + newstatus)
        }
        context.contentResolver.update(
                Uri.withAppendedPath(ShoppingContract.ItemStores.CONTENT_URI,
                        itemstore_id), values, null, null
        )

        requery()
        invalidate()
    }

    fun maybeUpdate(position: Int, column: Int, new_val: String) {
        if (mCursorItemstores!!.count <= position) {
            Log.e(TAG, "edit nonexistent item.")
            return
        }

        mCursorItemstores!!.moveToPosition(position)
        val old_val = mCursorItemstores!!.getString(column)
        if (new_val == old_val) {
            return
        }

        if (mCursorItemstores!!.isNull(0)) {
            val storeId = mCursorItemstores!!.getLong(cursorColumnSTORE_ID)
            var aisle = ""
            var price = ""

            if (column == 3) {
                price = new_val
            }
            if (column == 4) {
                aisle = new_val
            }
            ShoppingUtils.addItemToStore(context, mItemId, storeId, aisle,
                    price, false)

            /*
             * At the corresponding points in the item view, we would requery
             * and invalidate. However that is mainly because the editing
             * happens in widgets outside the list view itself, where here it
             * happens in EditTexts directly in the list. So we probably don't
             * need to invalidate() here. Do we really need to requery()?
             * Probably somewhere, perhaps not here.
             */
            // requery();
            // invalidate();
            // need to do those somewhere else.
            mDirty = true
            return
        }

        val itemstore_id = mCursorItemstores!!.getString(cursorColumnID)
        val uri = Uri.withAppendedPath(ItemStores.CONTENT_URI, itemstore_id)
        val cv = ContentValues()
        cv.put(mStringItems[column], new_val)
        context.contentResolver.update(uri, cv, null, null)

        // see comment above
        // requery();
        // invalidate();
        mDirty = true
    }

    fun requery() {
        if (debug) {
            Log.d(TAG, "requery()")
        }
        mCursorItemstores!!.requery()
        mDirty = false
    }

    fun getStoreName(cursorPosition: Int): String {
        var name = ""
        val c = mCursorItemstores
        if (c != null) {
            if (c.moveToPosition(cursorPosition)) {
                name = c.getString(cursorColumnNAME)
            }
        }
        return name
    }

    fun getStoreId(cursorPosition: Int): String? {
        var id: String? = null
        val c = mCursorItemstores
        if (c != null) {
            if (c.moveToPosition(cursorPosition)) {
                id = c.getString(cursorColumnSTORE_ID)
            }
        }
        return id
    }

    /**
     * Extend the SimpleCursorAdapter to handle updates to the data
     */
    inner class mSimpleCursorAdapter(
            context: Context,
            layout: Int,
            c: Cursor?,
            from: Array<String>,
            to: IntArray
    ) : SimpleCursorAdapter(context, layout, c, from, to),
            SimpleCursorAdapter.ViewBinder {

        init {
            super.setViewBinder(this)
        }

        override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
            val view = super.newView(context, cursor, parent)

            var v: EditText
            v = view.findViewById(R.id.price)
            v.addTextChangedListener(EditTextWatcher(v, cursorColumnPRICE))
            v.visibility = mPriceVisibility

            v = view.findViewById(R.id.aisle)
            v.addTextChangedListener(EditTextWatcher(v, cursorColumnAISLE))
            v.visibility = mPriceVisibility

            return view
        }

        /**
         * Additionally to the standard bindView, we also check for STATUS, and
         * strike the item through if BOUGHT.
         */
        override fun bindView(view: View, context: Context, cursor: Cursor) {

            // set tags to null during binding, to help avoid extra db updates
            // while binding
            var v: EditText
            v = view.findViewById(R.id.price)
            v.tag = null
            v = view.findViewById(R.id.aisle)
            v.tag = null

            mBinding = true
            super.bindView(view, context, cursor)
            mBinding = false

            val status = cursor.getInt(cursorColumnSTOCKS_ITEM) != 0
            val cursorpos = cursor.position

            val c = view.findViewById<CheckBox>(R.id.check)

            if (debug) {
                Log.i(TAG, "bindview: pos = " + cursor.position)
            }

            // set style for check box
            c.tag = cursor.position

            c.visibility = CheckBox.VISIBLE
            c.isChecked = status

            // The parent view knows how to deal with clicks.
            // We just pass the click through.
            // c.setClickable(false);

            c.setOnClickListener { _ ->
                if (debug) {
                    Log.d(TAG, "Click: ")
                }
                toggleItemstore(cursorpos)
            }

            val t = view.findViewById<TextView>(R.id.name)
            t.setOnCreateContextMenuListener { contextmenu, view, info ->
                // Context menus are created in the main activity
                // ItemStoresActivity
            }

            v = view.findViewById(R.id.price)
            v.tag = cursor.position
            v = view.findViewById(R.id.aisle)
            v.tag = cursor.position
        }

        override fun setViewValue(view: View, cursor: Cursor, i: Int): Boolean {
            val id = view.id
            if (id == R.id.price) {
                val price = cursor.getLong(cursorColumnPRICE)
                if (price != 0L) {
                    val text = PriceConverter.getStringFromCentPrice(price)
                    (view as TextView).text = text
                    return true
                }
            }
            // let SimpleCursorAdapter handle the binding.
            return false
        }

        override fun setViewBinder(viewBinder: SimpleCursorAdapter.ViewBinder) {
            throw RuntimeException("this adapter implements setViewValue")
        }

        private inner class EditTextWatcher(
                private val mView: EditText,
                private val mCol: Int
        ) : TextWatcher, View.OnFocusChangeListener {

            init {
                if (debug) {
                    Log.d(TAG, "New EditTextWatcher for " + mView.toString()
                            + " col " + mCol)
                }
            }

            override fun afterTextChanged(s: Editable) {

                if (mBinding) {
                    return // for update purposes, doesn't count as change
                }

                if (mView !== m_lastView) {
                    mView.onFocusChangeListener = this
                    // applyUpdate();
                }

                m_lastView = mView
                m_lastCol = mCol
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun onFocusChange(v: View, hasFocus: Boolean) {
                if (v === m_lastView && hasFocus == false) {
                    mInTextInput = true
                    applyUpdate()
                    mInTextInput = false
                }
            }
        }
    }
}
