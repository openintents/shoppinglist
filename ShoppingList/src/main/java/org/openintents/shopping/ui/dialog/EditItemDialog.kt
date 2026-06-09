package org.openintents.shopping.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.KeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.FilterQueryProvider
import android.widget.ImageButton
import android.widget.MultiAutoCompleteTextView
import android.widget.SimpleCursorAdapter
import android.widget.SimpleCursorAdapter.CursorToStringConverter
import android.widget.TextView

import org.openintents.distribution.DownloadAppDialog
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.Contains
import org.openintents.shopping.library.provider.ShoppingContract.Items
import org.openintents.shopping.library.provider.ShoppingContract.Units
import org.openintents.shopping.library.util.PriceConverter
import org.openintents.shopping.ui.ItemStoresActivity
import org.openintents.shopping.ui.PreferenceActivity

open class EditItemDialog(
    context: Context,
    itemUri: Uri,
    relationUri: Uri,
    listItemUri: Uri
) : AlertDialog(context), OnClickListener {

    private val mProjection = arrayOf(
        ShoppingContract.Items.NAME,
        ShoppingContract.Items.TAGS,
        ShoppingContract.Items.PRICE,
        ShoppingContract.Items.NOTE,
        ShoppingContract.Items._ID,
        ShoppingContract.Items.UNITS
    )
    private val mRelationProjection = arrayOf(
        ShoppingContract.Contains.QUANTITY,
        ShoppingContract.Contains.PRIORITY
    )

    private val mContext: Context = context
    private var mItemUri: Uri = itemUri
    private var mListItemUri: Uri = listItemUri
    private var mItemId: Long = 0
    private var mNoteText: String? = null
    private val mEditText: EditText
    private val mTags: MultiAutoCompleteTextView
    private val mPrice: EditText
    private val mPriceStore: Button
    private val mQuantity: EditText
    private val mPriority: EditText
    private val mUnits: AutoCompleteTextView
    private val mPriceLabel: TextView
    private val mNote: ImageButton
    private var mTagList: Array<String>? = null
    private var mOnItemChangedListener: OnItemChangedListener? = null
    private val mUnitsAdapter: SimpleCursorAdapter
    private var mRelationUri: Uri = relationUri

    private val mTextWatcher: TextWatcher = object : TextWatcher {
        override fun afterTextChanged(arg0: Editable) {
            updateQuantityPrice()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    }

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_edit_item, null)
        setView(view)

        mEditText = view.findViewById(R.id.edittext)
        mTags = view.findViewById(R.id.edittags)
        mPrice = view.findViewById(R.id.editprice)
        mQuantity = view.findViewById(R.id.editquantity)
        mPriority = view.findViewById(R.id.editpriority)
        mUnits = view.findViewById(R.id.editunits)

        mUnitsAdapter = SimpleCursorAdapter(
            mContext,
            android.R.layout.simple_dropdown_item_1line,
            null,
            // Map the units name...
            arrayOf(Units.NAME),
            // to the view defined in the XML template
            intArrayOf(android.R.id.text1)
        )
        mUnitsAdapter.cursorToStringConverter = CursorToStringConverter { cursor ->
            cursor.getString(1)
        }
        mUnitsAdapter.filterQueryProvider = FilterQueryProvider { constraint ->
            // Search for units whose names begin with the specified letters.
            var query: String? = null
            val args: Array<String>? = null

            if (constraint != null) {
                // query = "units." + Units.NAME + " like '?%' ";
                // args = new String[] {(constraint != null ?
                // constraint.toString() : null)} ;
                // http://code.google.com/p/android/issues/detail?id=3153
                //
                // workaround:
                query = "units." + Units.NAME + " like '" + constraint.toString() + "%' "
            }

            mContext.contentResolver.query(
                Units.CONTENT_URI,
                arrayOf(Units._ID, Units.NAME),
                query,
                args,
                Units.NAME
            )
        }
        mUnits.setAdapter(mUnitsAdapter)
        mUnits.threshold = 0

        mPriceStore = view.findViewById(R.id.pricestore)

        mPriceStore.setOnClickListener {
            val intent = Intent(context, ItemStoresActivity::class.java)
            intent.data = mListItemUri
            context.startActivity(intent)
        }

        mNote = view.findViewById(R.id.note)
        mNote.setOnClickListener {
            val uri = ContentUris.withAppendedId(
                ShoppingContract.Notes.CONTENT_URI, mItemId
            )

            if (mNoteText == null) {
                // Maybe an earlier edit activity added it? If so,
                // we should not replace with empty string below.
                val c: Cursor? = mContext.contentResolver.query(
                    mItemUri,
                    arrayOf(ShoppingContract.Items.NOTE),
                    null,
                    null,
                    null
                )
                if (c != null) {
                    if (c.moveToFirst()) {
                        mNoteText = c.getString(0)
                    }
                    c.close()
                }
            }

            if (mNoteText == null) {
                // can't edit a null note, put an empty one instead.
                val values = ContentValues()
                values.put("note", "")
                mContext.contentResolver.update(mItemUri, values, null, null)
                mContext.contentResolver.notifyChange(mItemUri, null)
            }

            val i = Intent(Intent.ACTION_VIEW)
            i.data = uri
            try {
                mContext.startActivity(i)
            } catch (e: ActivityNotFoundException) {
                val g: Dialog = DownloadAppDialog(
                    mContext,
                    R.string.notepad_not_available,
                    R.string.notepad,
                    R.string.notepad_package,
                    R.string.notepad_website
                )
                g.show()
            }
        }

        mPriceLabel = view.findViewById(R.id.labeleditprice)

        val kl: KeyListener = PreferenceActivity.getCapitalizationKeyListenerFromPrefs(context)
        mEditText.keyListener = kl
        mTags.keyListener = kl

        mTags.imeOptions = EditorInfo.IME_ACTION_DONE
        mTags.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        mTags.threshold = 0
        mTags.setOnClickListener {
            toggleTaglistPopup()
        }

        // setIcon(android.R.drawable.ic_menu_edit);
        setTitle(R.string.ask_edit_item)

        setItemUri(itemUri, listItemUri)
        setRelationUri(relationUri)

        setButton(context.getText(R.string.ok), this)
        @Suppress("DEPRECATION")
        setButton2(context.getText(R.string.cancel), this)

        /*
         * setButton(R.string.ok, new DialogInterface.OnClickListener() { public
         * void onClick(DialogInterface dialog, int whichButton) {
         *
         * dialog.dismiss(); doTextEntryDialogAction(mTextEntryMenu, (Dialog)
         * dialog);
         *
         * } }).setNegativeButton(R.string.cancel, new
         * DialogInterface.OnClickListener() { public void
         * onClick(DialogInterface dialog, int whichButton) {
         *
         * dialog.cancel(); } }).create();
         */

        mQuantity.addTextChangedListener(mTextWatcher)
        mPrice.addTextChangedListener(mTextWatcher)
    }

    fun setTagList(taglist: Array<String>?) {
        mTagList = taglist

        if (taglist != null) {
            val adapter = ArrayAdapter<String>(
                mContext,
                android.R.layout.simple_dropdown_item_1line,
                mTagList!!
            )
            mTags.setAdapter(adapter)
        }
    }

    /**
     * Set cursor to be requeried if item is changed.
     *
     * @param listener
     */
    fun setOnItemChangedListener(listener: OnItemChangedListener?) {
        mOnItemChangedListener = listener
    }

    private fun toggleTaglistPopup() {
        if (mTags.isPopupShowing) {
            mTags.dismissDropDown()
        } else {
            mTags.showDropDown()
        }
    }

    internal fun updateQuantityPrice() {
        try {
            val price = mPrice.text.toString().toDouble()
            val quantityString = mQuantity.text.toString()
            if (!TextUtils.isEmpty(quantityString)) {
                val quantity = quantityString.toDouble()
                val total = quantity * price
                val s = PriceConverter.mPriceFormatter.format(total)
                mPriceLabel.text = mContext.getText(R.string.price).toString() + ": " + s
                return
            }
        } catch (e: NumberFormatException) {
            // do nothing
        }

        // Otherwise show default label:
        mPriceLabel.setText(mContext.getText(R.string.price))
    }

    fun setItemUri(itemUri: Uri, listItemUri: Uri) {
        mItemUri = itemUri
        mListItemUri = listItemUri

        val c: Cursor? = mContext.contentResolver.query(mItemUri, mProjection, null, null, null)
        if (c != null && c.moveToFirst()) {
            val text = c.getString(0)
            val tags = c.getString(1)
            val pricecent = c.getLong(2)
            val price = PriceConverter.getStringFromCentPrice(pricecent)
            mNoteText = c.getString(3)
            mItemId = c.getLong(4)
            var units = c.getString(5)

            mEditText.setText(text)
            mTags.setText(tags)
            mPrice.setText(price)

            if (units == null) {
                units = ""
            }
            mUnits.setText(units)

            val trackPerStorePrices = PreferenceActivity.getUsingPerStorePricesFromPrefs(mContext)

            if (!trackPerStorePrices) {
                mPrice.visibility = View.VISIBLE
                mPriceStore.visibility = View.GONE
            } else {
                mPrice.visibility = View.GONE
                mPriceStore.visibility = View.VISIBLE
            }
        }
        c!!.close()
    }

    fun setRelationUri(relationUri: Uri) {
        mRelationUri = relationUri
        val c: Cursor? = mContext.contentResolver.query(
            mRelationUri, mRelationProjection, null, null, null
        )
        if (c != null && c.moveToFirst()) {
            val quantity = c.getString(0)
            mQuantity.setText(quantity)
            val priority = c.getString(1)
            mPriority.setText(priority)
        }
        c!!.close()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == BUTTON1) {
            editItem()
        }
    }

    internal fun editItem() {
        val text = mEditText.text.toString()
        val tags = mTags.text.toString()
        val price = mPrice.text.toString()
        val quantity = mQuantity.text.toString()
        val priority = mPriority.text.toString()
        val units = mUnits.text.toString()

        val priceLong: Long? = PriceConverter.getCentPriceFromString(price)

        val trimmedText = text.trim()

        // Remove trailing ","
        var trimmedTags = tags.trim()
        if (trimmedTags.endsWith(",")) {
            trimmedTags = trimmedTags.substring(0, trimmedTags.length - 1)
        }
        trimmedTags = trimmedTags.trim()

        val values = ContentValues()
        values.put(Items.NAME, trimmedText)
        values.put(Items.TAGS, trimmedTags)
        if (price != null) {
            values.put(Items.PRICE, priceLong)
        }
        if (units != null) {
            values.put(Items.UNITS, units)
        }
        mContext.contentResolver.update(mItemUri, values, null, null)
        mContext.contentResolver.notifyChange(mItemUri, null)

        values.clear()
        values.put(Contains.QUANTITY, quantity)
        values.put(Contains.PRIORITY, priority)

        mContext.contentResolver.update(mRelationUri, values, null, null)
        mContext.contentResolver.notifyChange(mRelationUri, null)

        mOnItemChangedListener?.onItemChanged()
    }

    private fun focus_field(e: EditText, selectAll: Boolean) {
        val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (selectAll) {
            e.selectAll()
        }
        if (e.requestFocus())
        // this part doesn't seem to work:
        {
            imm.showSoftInput(e, 0)
        }
        imm.toggleSoftInputFromWindow(e.windowToken, 0, 0)
    }

    fun setFocusField(focusField: FieldType) {
        when (focusField) {
            // hack, need to share some values with ShoppingActivity.
            FieldType.QUANTITY -> focus_field(mQuantity, true)
            FieldType.PRIORITY -> focus_field(mPriority, true)
            FieldType.PRICE -> focus_field(mPrice, true)
            FieldType.UNITS -> focus_field(mUnits, true)
            FieldType.TAGS -> focus_field(mTags, false)
            FieldType.ITEMNAME -> focus_field(mEditText, false)
            else -> {}
        }
    }

    enum class FieldType {
        ITEMNAME, QUANTITY, PRICE, PRIORITY, UNITS, TAGS
    }

    interface OnItemChangedListener {
        fun onItemChanged()
    }
}
