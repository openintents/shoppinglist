package org.openintents.shopping.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.MultiAutoCompleteTextView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.TextView;

import org.openintents.distribution.DownloadAppDialog;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.Items;
import org.openintents.shopping.library.provider.ShoppingContract.Units;
import org.openintents.shopping.library.util.PriceConverter;
import org.openintents.shopping.ui.ItemStoresActivity;
import org.openintents.shopping.ui.PreferenceActivity;

public class EditItemDialog extends AlertDialog implements OnClickListener {

    private final String[] mProjection = {ShoppingContract.Items.NAME,
            ShoppingContract.Items.TAGS, ShoppingContract.Items.PRICE,
            ShoppingContract.Items.NOTE, ShoppingContract.Items._ID,
            ShoppingContract.Items.UNITS};
    private final String[] mRelationProjection = {
            ShoppingContract.Contains.QUANTITY,
            ShoppingContract.Contains.PRIORITY};
    private Context mContext;
    private Uri mItemUri;
    private Uri mListItemUri;
    private long mItemId;
    private String mNoteText;
    private EditText mEditText;
    private MultiAutoCompleteTextView mTags;
    private EditText mPrice;
    private Button mPriceStore;
    private EditText mQuantity;
    private EditText mPriority;
    private AutoCompleteTextView mUnits;
    private TextView mPriceLabel;
    private ImageButton mNote;
    private String[] mTagList;
    private OnItemChangedListener mOnItemChangedListener;
    private SimpleCursorAdapter mUnitsAdapter;
    private TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable arg0) {
            updateQuantityPrice();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
        }

    };
    private Uri mRelationUri;

    public EditItemDialog(final Context context, final Uri itemUri,
                          final Uri relationUri, final Uri listItemUri) {
        super(context);
        mContext = context;

        LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_edit_item, null);
        setView(view);

        mEditText = (EditText) view.findViewById(R.id.edittext);
        mTags = (MultiAutoCompleteTextView) view.findViewById(R.id.edittags);
        mPrice = (EditText) view.findViewById(R.id.editprice);
        mQuantity = (EditText) view.findViewById(R.id.editquantity);
        mPriority = (EditText) view.findViewById(R.id.editpriority);
        mUnits = (AutoCompleteTextView) view.findViewById(R.id.editunits);

        mUnitsAdapter = new SimpleCursorAdapter(mContext,
                android.R.layout.simple_dropdown_item_1line, null,
                // Map the units name...
                new String[]{Units.NAME},
                // to the view defined in the XML template
                new int[]{android.R.id.text1});
        mUnitsAdapter.setCursorToStringConverter(new CursorToStringConverter() {
            public String convertToString(android.database.Cursor cursor) {
                return cursor.getString(1);
            }
        });
        mUnitsAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                // Search for units whose names begin with the specified
                // letters.
                String query = null;
                String[] args = null;

                if (constraint != null) {
                    // query = "units." + Units.NAME + " like '?%' ";
                    // args = new String[] {(constraint != null ?
                    // constraint.toString() : null)} ;
                    // http://code.google.com/p/android/issues/detail?id=3153
                    //
                    // workaround:
                    query = "units." + Units.NAME + " like '"
                            + constraint.toString() + "%' ";
                }

                return mContext.getContentResolver().query(
                        Units.CONTENT_URI,
                        new String[]{Units._ID, Units.NAME}, query, args,
                        Units.NAME);
            }
        });
        mUnits.setAdapter(mUnitsAdapter);
        mUnits.setThreshold(0);

        mPriceStore = (Button) view.findViewById(R.id.pricestore);

        mPriceStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ItemStoresActivity.class);
                intent.setData(mListItemUri);
                context.startActivity(intent);
            }
        });

        mNote = (ImageButton) view.findViewById(R.id.note);
        mNote.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Uri uri = ContentUris.withAppendedId(
                        ShoppingContract.Notes.CONTENT_URI, mItemId);

                if (mNoteText == null) {
                    // Maybe an earlier edit activity added it? If so,
                    // we should not replace with empty string below.
                    Cursor c = mContext.getContentResolver().query(mItemUri,
                            new String[]{ShoppingContract.Items.NOTE}, null,
                            null, null);
                    if (c != null) {
                        if (c.moveToFirst()) {
                            mNoteText = c.getString(0);
                        }
                        c.close();
                    }
                }

                if (mNoteText == null) {
                    // can't edit a null note, put an empty one instead.
                    ContentValues values = new ContentValues();
                    values.put("note", "");
                    mContext.getContentResolver().update(mItemUri, values,
                            null, null);
                    mContext.getContentResolver().notifyChange(mItemUri, null);
                }

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(uri);
                try {
                    mContext.startActivity(i);
                } catch (ActivityNotFoundException e) {
                    Dialog g = new DownloadAppDialog(mContext,
                            R.string.notepad_not_available, R.string.notepad,
                            R.string.notepad_package, R.string.notepad_website);
                    g.show();
                }
            }

        });

        mPriceLabel = (TextView) view.findViewById(R.id.labeleditprice);

        final KeyListener kl = PreferenceActivity
                .getCapitalizationKeyListenerFromPrefs(context);
        mEditText.setKeyListener(kl);
        mTags.setKeyListener(kl);

        mTags.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mTags.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        mTags.setThreshold(0);
        mTags.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                toggleTaglistPopup();
            }

        });

        // setIcon(android.R.drawable.ic_menu_edit);
        setTitle(R.string.ask_edit_item);

        setItemUri(itemUri, listItemUri);
        setRelationUri(relationUri);

        setButton(context.getText(R.string.ok), this);
        setButton2(context.getText(R.string.cancel), this);

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

        mQuantity.addTextChangedListener(mTextWatcher);
        mPrice.addTextChangedListener(mTextWatcher);
    }

    public void setTagList(String[] taglist) {
        mTagList = taglist;

        if (taglist != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
                    android.R.layout.simple_dropdown_item_1line, mTagList);
            mTags.setAdapter(adapter);
        }
    }

    /**
     * Set cursor to be requeried if item is changed.
     *
     * @param c
     */
    public void setOnItemChangedListener(OnItemChangedListener listener) {
        mOnItemChangedListener = listener;
    }

    private void toggleTaglistPopup() {
        if (mTags.isPopupShowing()) {
            mTags.dismissDropDown();
        } else {
            mTags.showDropDown();
        }
    }

    void updateQuantityPrice() {
        try {
            double price = Double.parseDouble(mPrice.getText().toString());
            String quantityString = mQuantity.getText().toString();
            if (!TextUtils.isEmpty(quantityString)) {
                double quantity = Double.parseDouble(quantityString);
                price = quantity * price;
                String s = PriceConverter.mPriceFormatter.format(price);
                mPriceLabel
                        .setText(mContext.getText(R.string.price) + ": " + s);
                return;
            }
        } catch (NumberFormatException e) {
            // do nothing
        }

        // Otherwise show default label:
        mPriceLabel.setText(mContext.getText(R.string.price));
    }

    public void setItemUri(Uri itemUri, Uri listItemUri) {
        mItemUri = itemUri;
        mListItemUri = listItemUri;

        Cursor c = mContext.getContentResolver().query(mItemUri, mProjection,
                null, null, null);
        if (c != null && c.moveToFirst()) {
            String text = c.getString(0);
            String tags = c.getString(1);
            long pricecent = c.getLong(2);
            String price = PriceConverter.getStringFromCentPrice(pricecent);
            mNoteText = c.getString(3);
            mItemId = c.getLong(4);
            String units = c.getString(5);

            mEditText.setText(text);
            mTags.setText(tags);
            mPrice.setText(price);

            if (units == null) {
                units = "";
            }
            mUnits.setText(units);

            boolean trackPerStorePrices = PreferenceActivity
                    .getUsingPerStorePricesFromPrefs(mContext);

            if (!trackPerStorePrices) {
                mPrice.setVisibility(View.VISIBLE);
                mPriceStore.setVisibility(View.GONE);
            } else {
                mPrice.setVisibility(View.GONE);
                mPriceStore.setVisibility(View.VISIBLE);
            }
        }
        c.close();
    }

    public void setRelationUri(Uri relationUri) {
        mRelationUri = relationUri;
        Cursor c = mContext.getContentResolver().query(mRelationUri,
                mRelationProjection, null, null, null);
        if (c != null && c.moveToFirst()) {
            String quantity = c.getString(0);
            mQuantity.setText(quantity);
            String priority = c.getString(1);
            mPriority.setText(priority);
        }
        c.close();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON1) {
            editItem();
        }

    }

    void editItem() {
        String text = mEditText.getText().toString();
        String tags = mTags.getText().toString();
        String price = mPrice.getText().toString();
        String quantity = mQuantity.getText().toString();
        String priority = mPriority.getText().toString();
        String units = mUnits.getText().toString();

        Long priceLong = PriceConverter.getCentPriceFromString(price);

        text = text.trim();

        // Remove trailing ","
        tags = tags.trim();
        if (tags.endsWith(",")) {
            tags = tags.substring(0, tags.length() - 1);
        }
        tags = tags.trim();

        ContentValues values = new ContentValues();
        values.put(Items.NAME, text);
        values.put(Items.TAGS, tags);
        if (price != null) {
            values.put(Items.PRICE, priceLong);
        }
        if (units != null) {
            values.put(Items.UNITS, units);
        }
        mContext.getContentResolver().update(mItemUri, values, null, null);
        mContext.getContentResolver().notifyChange(mItemUri, null);

        values.clear();
        values.put(Contains.QUANTITY, quantity);
        values.put(Contains.PRIORITY, priority);

        mContext.getContentResolver().update(mRelationUri, values, null, null);
        mContext.getContentResolver().notifyChange(mRelationUri, null);

        if (mOnItemChangedListener != null) {
            mOnItemChangedListener.onItemChanged();
        }
    }

    private void focus_field(EditText e, Boolean selectAll) {
        InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (selectAll) {
            e.selectAll();
        }
        if (e.requestFocus())
        // this part doesn't seem to work:
        {
            imm.showSoftInput(e, 0);
        }
        imm.toggleSoftInputFromWindow(e.getWindowToken(), 0, 0);
    }

    public void setFocusField(FieldType focusField) {

        switch (focusField) {
            // hack, need to share some values with ShoppingActivity.
            case QUANTITY:
                focus_field(mQuantity, true);
                break;
            case PRIORITY:
                focus_field(mPriority, true);
                break;
            case PRICE:
                focus_field(mPrice, true);
                break;
            case UNITS:
                focus_field(mUnits, true);
                break;
            case TAGS:
                focus_field(mTags, false);
                break;
            case ITEMNAME:
                focus_field(mEditText, false);
                break;
            default:
                break;

        }
    }

    public enum FieldType {
        ITEMNAME, QUANTITY, PRICE, PRIORITY, UNITS, TAGS
    }

    public interface OnItemChangedListener {
        public void onItemChanged();
    }

}
