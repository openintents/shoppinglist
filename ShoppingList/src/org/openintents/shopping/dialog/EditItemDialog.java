package org.openintents.shopping.dialog;


import org.openintents.distribution.DownloadAppDialog;
import org.openintents.provider.Shopping;
import org.openintents.provider.Shopping.Contains;
import org.openintents.provider.Shopping.Items;
import org.openintents.shopping.PreferenceActivity;
import org.openintents.shopping.R;
import org.openintents.shopping.util.PriceConverter;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

public class EditItemDialog extends AlertDialog implements OnClickListener {

	Context mContext;
	Uri mItemUri;
	long mItemId = 0;
	String mNoteText = null;
	
	EditText mEditText;
	MultiAutoCompleteTextView mTags;
	EditText mPrice;
	EditText mQuantity;
	EditText mPriority;

	TextView mPriceLabel;
	ImageView mNote;

    String[] mTagList;
    
    /** Cursor to be requeried after modifications */
    Cursor mRequeryCursor;

    public enum FieldType { ITEMNAME, QUANTITY, PRICE, PRIORITY };
    
	public EditItemDialog(Context context, Uri itemUri, Uri relationUri) {
		super(context);
		mContext = context;
		
		LayoutInflater inflater = LayoutInflater.from(context);
		final View view = inflater
				.inflate(R.layout.dialog_edit_item, null);
		setView(view);

		mEditText = (EditText) view.findViewById(R.id.edittext);
		mTags = (MultiAutoCompleteTextView) view.findViewById(R.id.edittags);
		mPrice = (EditText) view.findViewById(R.id.editprice);
		mQuantity= (EditText) view.findViewById(R.id.editquantity);
		mPriority= (EditText) view.findViewById(R.id.editpriority);
		mNote = (ImageView) view.findViewById(R.id.note);
		
		mNote.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Uri uri = ContentUris.withAppendedId(Shopping.Notes.CONTENT_URI, mItemId);
				
				if (mNoteText == null) {
				   // Maybe an earlier edit activity added it? If so, 
				   // we should not replace with empty string below.
					String[] shortProjection = {Shopping.Items.NOTE}; 
					
					Cursor c = mContext.getContentResolver().query(mItemUri, 
							shortProjection, null, null, null);
					if (c != null && c.moveToFirst()) {
						mNoteText = c.getString(0);
					}
				}
				
				if (mNoteText == null) {
					// can't edit a null note, put an empty one instead.
					ContentValues values = new ContentValues();
					values.put("note", "");
					mContext.getContentResolver().update(mItemUri, values, null, null);
					mContext.getContentResolver().notifyChange(mItemUri, null);
				}
									
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(uri);
				try {
				    mContext.startActivity(i);
				} catch (ActivityNotFoundException e) {
					Dialog g = new DownloadAppDialog(mContext, 
							R.string.notepad_not_available, 
							R.string.notepad, 
							R.string.notepad_package, 
							R.string.notepad_website);
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
	        
		//setIcon(android.R.drawable.ic_menu_edit);
		setTitle(R.string.ask_edit_item);
		
		setItemUri(itemUri);
		setRelationUri(relationUri);
		
		setButton(context.getText(R.string.ok), this);
		setButton2(context.getText(R.string.cancel), this);
		
		
		/*
		setButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {

						dialog.dismiss();
						doTextEntryDialogAction(mTextEntryMenu,
								(Dialog) dialog);

					}
				}).setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {

						dialog.cancel();
					}
				}).create();
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
     * @param c
     */
    public void setRequeryCursor(Cursor c) {
    	mRequeryCursor = c;
    }
    
	private void toggleTaglistPopup() {
		if (mTags.isPopupShowing()) {
			mTags.dismissDropDown();
		} else {
			mTags.showDropDown();
		}
	}
	
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
	
	void updateQuantityPrice() {
		try {
			double price = Double.parseDouble(mPrice.getText().toString());
			String quantityString = mQuantity.getText().toString();
			if (!TextUtils.isEmpty(quantityString)) {
				double quantity = Double.parseDouble(quantityString);
				price = quantity * price;
				String s = PriceConverter.mPriceFormatter.format(price);
				mPriceLabel.setText(mContext.getText(R.string.price) + ": " + s);
				return;
			}
		} catch (NumberFormatException e) {
			// do nothing
		}
		
		// Otherwise show default label:
		mPriceLabel.setText(mContext.getText(R.string.price));
	}
	
	private final String[] mProjection = { 
			Shopping.Items.NAME,
			Shopping.Items.TAGS,
			Shopping.Items.PRICE,
			Shopping.Items.NOTE,
			Shopping.Items._ID
	};
	private final String[] mRelationProjection = { 
			Shopping.Contains.QUANTITY,
			Shopping.Contains.PRIORITY
	};

	private Uri mRelationUri;
			
	public void setItemUri(Uri itemUri) {
		mItemUri = itemUri;
		
		Cursor c = mContext.getContentResolver().query(mItemUri, 
				mProjection, null, null, null);
		if (c != null && c.moveToFirst()) {
			String text = c.getString(0);
			String tags = c.getString(1);
			long pricecent = c.getLong(2);
			String price = PriceConverter.getStringFromCentPrice(pricecent);
			mNoteText = c.getString(3);
			mItemId = c.getLong(4);
			
			mEditText.setText(text);
			mTags.setText(tags);
			mPrice.setText(price);
		}
		c.close();
	}

	public void setRelationUri(Uri relationUri){
		mRelationUri = relationUri;
		Cursor c= mContext.getContentResolver().query(mRelationUri, mRelationProjection, null, null, null);
		if (c != null && c.moveToFirst()){
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

		Long priceLong = PriceConverter.getCentPriceFromString(price);

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
		mContext.getContentResolver().update(mItemUri, values, null, null);
		mContext.getContentResolver().notifyChange(mItemUri, null);

		values.clear();
		values.put(Contains.QUANTITY, quantity);
		values.put(Contains.PRIORITY, priority);

		mContext.getContentResolver().update(mRelationUri, values, null, null);
		mContext.getContentResolver().notifyChange(mRelationUri, null);
		
		if (mRequeryCursor != null) {
			mRequeryCursor.requery();
		}
	}

	private void focus_field (EditText e, Boolean selectAll) {
		InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (selectAll)
			e.selectAll();
		if (e.requestFocus())
			// this part doesn't seem to work:
		    imm.showSoftInput(e, 0);
		imm.toggleSoftInputFromWindow(e.getWindowToken(), 0, 0);
	}
	
	public void setFocusField(FieldType focusField) {

		switch(focusField) {
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
		case ITEMNAME:
			focus_field(mEditText, false);
			break;
			
		}
	}
	
}
