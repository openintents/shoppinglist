package org.openintents.shopping.ui.widget;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.openintents.distribution.DownloadAppDialog;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.library.provider.ShoppingContract.Subtotals;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.shopping.theme.ThemeAttributes;
import org.openintents.shopping.theme.ThemeShoppingList;
import org.openintents.shopping.theme.ThemeUtils;
import org.openintents.shopping.ui.PreferenceActivity;
import org.openintents.shopping.ui.ShoppingActivity;
import org.openintents.shopping.ui.dialog.EditItemDialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v2.os.Build;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

/**
 * View to show a shopping list with its items
 * 
 */
public class ShoppingItemsView extends ListView {
	private final static String TAG = "ShoppingListView";
	private final static boolean debug = false;

	Typeface mCurrentTypeface = null;

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
	public int mUpdateLastListPosition = 0;
	public int mLastListPosition;
	public int mLastListTop;
	public long mNumChecked = 0;
	
	private ThemeAttributes mThemeAttributes;
	private PackageManager mPackageManager;
	private String mPackageName;

	NumberFormat mPriceFormatter = DecimalFormat
			.getNumberInstance(Locale.ENGLISH);

	public int mMode = ShoppingActivity.MODE_IN_SHOP;
	public Cursor mCursorItems = null;
	
	private Activity mCursorActivity = null;
	
	private View mThemedBackground;
	private long mListId;

	private TextView mTotalTextView;
	private TextView mPriTotalTextView;
	private TextView mTotalCheckedTextView;

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
	
	/**
	 * Extend the SimpleCursorAdapter to strike through items. if STATUS ==
	 * Shopping.Status.BOUGHT
	 */
	public class mSimpleCursorAdapter extends SimpleCursorAdapter implements
			ViewBinder {

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

			mPriceFormatter.setMaximumFractionDigits(2);
			mPriceFormatter.setMinimumFractionDigits(2);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = super.newView(context, cursor, parent);
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.CUPCAKE) {
				view.findViewById(R.id.price).setVisibility(mPriceVisibility);
				view.findViewById(R.id.tags).setVisibility(mTagsVisibility);
				view.findViewById(R.id.quantity).setVisibility(mQuantityVisibility);
				view.findViewById(R.id.units).setVisibility(mUnitsVisibility);
				view.findViewById(R.id.priority).setVisibility(mPriorityVisibility);
			} else {
				// avoid problems on Cupcake with views positioned relative to 
				// invisible views
				view.findViewById(R.id.price).setVisibility(View.VISIBLE);
				view.findViewById(R.id.tags).setVisibility(View.VISIBLE);
				view.findViewById(R.id.quantity).setVisibility(View.VISIBLE);
				view.findViewById(R.id.units).setVisibility(View.VISIBLE);
				view.findViewById(R.id.priority).setVisibility(View.VISIBLE);
			}
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
			final int cursorpos = cursor.getPosition();
			Integer tag = new Integer(cursorpos);

			view.setTag(tag);
			
			int styled_as_name [] = {R.id.name, R.id.units, R.id.quantity};
			int i;
			
			for (i = 0; i < styled_as_name.length; i++) {
				int res_id = styled_as_name[i];
				TextView t = (TextView) view.findViewById(res_id);

			// set style for name view
			// Set font
			t.setTypeface(mCurrentTypeface);
			t.setTag(tag);

			// Set size
			t.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);

			// Check for upper case:
			if (mTextUpperCaseFont) {
				// Only upper case should be displayed
				CharSequence cs = t.getText();
				t.setText(cs.toString().toUpperCase());
			}

			t.setTextColor(mTextColor);

				if (res_id == R.id.quantity) {
					
					if ( TextUtils.isEmpty(t.getText()) && 
						mQuantityVisibility == View.VISIBLE) {
					// mixed feelings about this.
					//   t.setText("1 "); 
					}
					
					t.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							if (debug) Log.d(TAG, "Quantity Click ");
							if (mListener != null) {
								mListener.onCustomClick(cursor, cursorpos,
										EditItemDialog.FieldType.QUANTITY, v);
							}
						}

					});
				}
				
				
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

					if (res_id == R.id.name && mTextSuffixChecked != null) {
					// very simple
					t.append(mTextSuffixChecked);
				}

			} else {
				// item not bought:
					if (res_id == R.id.name && mTextSuffixUnchecked != null) {
					t.append(mTextSuffixUnchecked);
				}
			}
			}

			// we have a check box now.. more visual and gets the point across
			CheckBox c = (CheckBox) view.findViewById(R.id.check);
			ImageView nc = (ImageView) view.findViewById(R.id.nocheck);

			if (debug) Log.i(TAG, "bindview: pos = " + cursor.getPosition());

			// set style for check box
			c.setTag(new Integer(cursor.getPosition()));
			nc.setTag(new Integer(cursor.getPosition()));

			if (mShowCheckBox) {
				c.setVisibility(CheckBox.VISIBLE);
				c.setChecked(status == ShoppingContract.Status.BOUGHT);
			} else {
				c.setVisibility(CheckBox.GONE);
			}
			
			if (mMode == ShoppingActivity.MODE_IN_SHOP) {
				nc.setVisibility(ImageView.GONE);
			} else {  // mMode == ShoppingActivity.MODE_ADD_ITEMS
				if (status == ShoppingContract.Status.REMOVED_FROM_LIST) {
					nc.setVisibility(ImageView.VISIBLE);
					if (mShowCheckBox) {
						// replace check box
						c.setVisibility(CheckBox.INVISIBLE);
					}
				} else {
					nc.setVisibility(ImageView.INVISIBLE);
				}
			}

			// The parent view knows how to deal with clicks.
			// We just pass the click through.
			// c.setClickable(false);

			c.setOnClickListener(new OnClickListener() {
			   @Override
			   public void onClick(View v) {
				   if (debug) Log.d(TAG, "Click: ");
				   toggleItemBought(cursorpos);
			   }
			});

			// also check around check box
			RelativeLayout l = (RelativeLayout) view
					.findViewById(R.id.check_surround);

			l.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (debug) Log.d(TAG, "Click around: ");
					toggleItemBought(cursorpos);
				}

			});

			// Check for clicks on price
			View v;
			v = view.findViewById(R.id.price);
			v.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (debug) Log.d(TAG, "Click on price: ");
					if (mListener != null) {
						mListener.onCustomClick(cursor, cursorpos,
								EditItemDialog.FieldType.PRICE, v);
					}
				}

			});
			// Check for clicks on units
			v = view.findViewById(R.id.units);
			v.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (debug) Log.d(TAG, "Click on units: ");
					if (mListener != null) {
						mListener.onCustomClick(cursor, cursorpos,
								EditItemDialog.FieldType.UNITS, v);
					}
				}

			});
			// Check for clicks on priority
			v = view.findViewById(R.id.priority);
			v.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (debug) Log.d(TAG, "Click on priority: ");
					if (mListener != null) {
						mListener.onCustomClick(cursor, cursorpos,
								EditItemDialog.FieldType.PRIORITY, v);
					}
				}

			});
			// Check for clicks on tags
			v = view.findViewById(R.id.tags);
			v.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (debug) Log.d(TAG, "Click on tags: ");
					if (mListener != null) {
						mListener.onCustomClick(cursor, cursorpos,
								EditItemDialog.FieldType.TAGS, v);
					}
				}

			});

			// Check for clicks on item text
			RelativeLayout r = (RelativeLayout) view
					.findViewById(R.id.description);

			r.setTag(cursorpos);
			r.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (debug) Log.d(TAG, "Click on description: ");
					if (mListener != null) {
						mListener.onCustomClick(cursor, cursorpos,
								EditItemDialog.FieldType.ITEMNAME, v);
					}
				}

			});

		}

		private void hideTextView(TextView view) {
			// Cupcake doesn't compute position for invisible
			// members of RelativeLayout, so we can't make 
			// views invisible when running Cupcake.
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.CUPCAKE) {
				view.setVisibility(View.GONE);
			}	
			view.setText("");
		}
		
		private class ClickableNoteSpan extends ClickableSpan {
            public void onClick(View view) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				int cursorpos = (Integer) view.getTag();
            	if (debug) Log.d(TAG, "Click on has_note: " + cursorpos);
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
				if (debug) Log.d(TAG, "Click on description: ");
				if (mListener != null) {
					int cursorpos = (Integer) view.getTag();
					mListener.onCustomClick(mCursorItems, cursorpos,
							EditItemDialog.FieldType.ITEMNAME, view);
				}
				
            }
            
        	public void updateDrawState (TextPaint ds) {
        		// Override the parent's method to avoid having the text 
        		// in this span look like a link.
			}
		}
		
		public boolean setViewValue(View view, Cursor cursor, int i) {
			int id = view.getId();
			long price = 0;
			boolean hasPrice = false;
			String tags = null;
			boolean hasTags = false;
			if (mPriceVisibility == View.VISIBLE) {  
				price = getQuantityPrice(cursor);
				hasPrice = (price != 0);
			}
			if (mTagsVisibility == View.VISIBLE) {
				tags = cursor.getString(ShoppingActivity.mStringItemsITEMTAGS);
				hasTags = !TextUtils.isEmpty(tags);
			}

			if (id == R.id.name) {
				int has_note = cursor
						.getInt(ShoppingActivity.mStringItemsITEMHASNOTE);
				String name = cursor
						.getString(ShoppingActivity.mStringItemsITEMNAME);
				TextView tv = (TextView) view;
				SpannableString name_etc = null;
				if (has_note == 0)
				{
					name_etc = new SpannableString(name);
		            name_etc.setSpan(new ClickableItemSpan(), 0, name.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
				}
				else
				{
					name_etc = new SpannableString(name + "   ");
					int note_start = name.length() + 1;
					int note_end = note_start + 2;
					Drawable d = getResources().getDrawable(R.drawable.ic_launcher_notepad_small);
					float ratio = d.getIntrinsicWidth() / d.getIntrinsicHeight();
				    d.setBounds(0, 0, (int)(ratio * mTextSize), (int)mTextSize); 
		            ImageSpan imgspan = new ImageSpan(d, ImageSpan.ALIGN_BASELINE); 
		            name_etc.setSpan(imgspan, note_start, note_end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
		            name_etc.setSpan(new ClickableNoteSpan(), note_start, note_end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
		            name_etc.setSpan(new ClickableItemSpan(), 0, note_start, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
				}
	            tv.setText(name_etc);
	            tv.setMovementMethod(LinkMovementMethod.getInstance());
	            if (hasPrice && ! hasTags) {
	            	// don't overlap the price
	            	RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams)tv.getLayoutParams();
	            	rlp.addRule(RelativeLayout.LEFT_OF, R.id.price);
	            }
				return true;
			} else if (id == R.id.price) {
				TextView tv = (TextView) view;
				if (hasPrice) {
					tv.setVisibility(View.VISIBLE);
					String s = mPriceFormatter.format(price * 0.01d);
					tv.setTextColor(mTextColorPrice);
					tv.setText(s);
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
			           	RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams)tv.getLayoutParams();
			           	rlp.addRule(RelativeLayout.LEFT_OF, R.id.price);
			        }
				} else {
					hideTextView(tv);
				}
				return true;
			} else if (id == R.id.quantity) { 
				String quantity = cursor.getString(ShoppingActivity.mStringItemsQUANTITY);
				TextView tv =(TextView) view; 
				if (mQuantityVisibility == View.VISIBLE  &&
					!TextUtils.isEmpty(quantity)) { 
					tv.setVisibility(View.VISIBLE); 
					// tv.setTextColor(mPriceTextColor);  
					tv.setText(quantity + " "); }
			    else { 
				    hideTextView(tv); 
			    } 
				return true; 
			} else if (id == R.id.units) { 
				String units = cursor.getString(ShoppingActivity.mStringItemsITEMUNITS);
				String quantity = cursor.getString(ShoppingActivity.mStringItemsQUANTITY);
				TextView tv =(TextView) view; 
				// looks more natural if you only show units when showing qty.
				if (mUnitsVisibility == View.VISIBLE  &&
					mQuantityVisibility == View.VISIBLE  &&
					!TextUtils.isEmpty(units) && !TextUtils.isEmpty(quantity)) { 
					tv.setVisibility(View.VISIBLE); 
					// tv.setTextColor(mPriceTextColor);  
					tv.setText(units + " "); }
			    else { 
				    hideTextView(tv); 
			    } 
				return true; 
			} else if (id == R.id.priority) { 
				String priority = cursor.getString(ShoppingActivity.mStringItemsPRIORITY);
				TextView tv =(TextView) view; 
				if (mPriorityVisibility == View.VISIBLE  &&
					!TextUtils.isEmpty(priority)) { 
					tv.setVisibility(View.VISIBLE); 
					tv.setTextColor(mTextColorPriority);
					tv.setText("-" + priority + "- "); }
			    else { 
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

	}

	private void disposeItemsCursor () {
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
	
	private boolean mContentObserverRegistered = false;
	ContentObserver mContentObserver = new ContentObserver(new Handler()) {

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			if (mCursorItems != null) {
				try {
					requery();
				} catch (IllegalStateException e) {
					Log.e(TAG, "IllegalStateException ", e);
					// Somehow the logic is not completely right yet...
					disposeItemsCursor();
					
				}
			}

		}

	};
	
	private TextView mCountTextView;

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

	private void init() {
		mItemHeightNormal = 45;
		mItemHeightHalf = mItemHeightNormal / 2;
		mItemHeightExpanded = 90;

		// Remember standard divider
		mDefaultDivider = getDivider();
	}

	public void setActionBarListener(ActionBarListener listener) {
		mActionBarListener = listener;
	}

	public void onResume() {
		// Content observer registered at fillItems()
		// registerContentObserver();
		setFastScrollEnabled(PreferenceActivity.getFastScrollEnabledFromPrefs(getContext()));
	}

	public void onPause() {
		unregisterContentObserver();
	}

	public long getListId() {
		return mListId;
	}
	
	/**
	 * 
	 * @param activity
	 *            Activity to manage the cursor.
	 * @param listId
	 * @return
	 */
	public Cursor fillItems(Activity activity, long listId) {

		mListId = listId;
		String sortOrder = PreferenceActivity.getSortOrderFromPrefs(this
				.getContext(), mMode);
		boolean hideBought = PreferenceActivity
				.getHideCheckedItemsFromPrefs(this.getContext());
		String selection;
		if (mMode == ShoppingActivity.MODE_IN_SHOP) {
			if (hideBought) {
				selection = "list_id = ? AND " + ShoppingContract.Contains.STATUS
						+ " == " + ShoppingContract.Status.WANT_TO_BUY;
			} else {
				selection = "list_id = ? AND " + ShoppingContract.Contains.STATUS
						+ " <> " + ShoppingContract.Status.REMOVED_FROM_LIST;
			}
		} else {
			selection = "list_id = ? ";
		}

		if (mCursorItems != null) {
			disposeItemsCursor();
		}

		// Get a cursor for all items that are contained
		// in currently selected shopping list.
		mCursorItems = getContext().getContentResolver().query(
				ContainsFull.CONTENT_URI, ShoppingActivity.mStringItems,
				selection, new String[] { String.valueOf(listId) }, sortOrder);

		// Activate the following for a striped list.
		// setupListStripes(mListItems, this);
		
		registerContentObserver();

		if (mCursorItems == null) {
			Log.e(TAG, "missing shopping provider");
			setAdapter(new ArrayAdapter<String>(this.getContext(),
					android.R.layout.simple_list_item_1,
					new String[] { "no shopping provider" }));
			return mCursorItems;
		}
		mCursorActivity = activity;
		mCursorActivity.startManagingCursor(mCursorItems);


		int layout_row = R.layout.list_item_shopping_item;

		int size = PreferenceActivity.getFontSizeFromPrefs(getContext());
		if (size < 3) {
			layout_row = R.layout.list_item_shopping_item_small;
		}

		Context context = getContext();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// If background is light, we apply the light holo theme to widgets.
			
			// determine color from text color:
			int gray = (Color.red(mTextColor) + Color.green(mTextColor) + Color.blue(mTextColor));
			if (gray < 3 * 128) {
				// dark text color <-> light background color => use light holo theme.
				context = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light);
			}
		}

		
		mSimpleCursorAdapter adapter = new mSimpleCursorAdapter(
				context,
		// Use a template that displays a text view
				layout_row,
				// Give the cursor to the list adapter
				mCursorItems,
				// Map the IMAGE and NAME to...
				new String[] { ContainsFull.ITEM_NAME, /*
														 * ContainsFull.ITEM_IMAGE
														 * ,
														 */
				ContainsFull.ITEM_TAGS, ContainsFull.ITEM_PRICE,  
				ContainsFull.QUANTITY, ContainsFull.PRIORITY,
				ContainsFull.ITEM_UNITS				
				},
				// the view defined in the XML template
				new int[] { R.id.name, /* R.id.image_URI, */R.id.tags,
						R.id.price, R.id.quantity, R.id.priority, R.id.units });
		setAdapter(adapter);

		// called in requery():
		updateTotal();

		return mCursorItems;
	}

	/**
	 * 
	 */
	private void registerContentObserver() {
		if (mContentObserverRegistered == false) {
			mContentObserverRegistered = true;
			getContext().getContentResolver().registerContentObserver(
				ShoppingContract.Items.CONTENT_URI, true, mContentObserver);
		}
	}

	private void unregisterContentObserver() {
		mContentObserverRegistered = false;
		getContext().getContentResolver().unregisterContentObserver(
				mContentObserver);
	}

	/**
	 * Set theme according to Id.
	 * 
	 * @param themeId
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
			if (debug)
				Log.e(TAG, "Empty style name: " + styleName);
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
			mCurrentTypeface = null;

			// Look for special cases:
			if ("monospace".equals(mTextTypeface)) {
				mCurrentTypeface = Typeface.create(Typeface.MONOSPACE,
						Typeface.NORMAL);
			} else if ("sans".equals(mTextTypeface)) {
				mCurrentTypeface = Typeface.create(Typeface.SANS_SERIF,
						Typeface.NORMAL);
			} else if ("serif".equals(mTextTypeface)) {
				mCurrentTypeface = Typeface.create(Typeface.SERIF,
						Typeface.NORMAL);
			} else if (!TextUtils.isEmpty(mTextTypeface)) {

				try {
					if (debug) Log.d(TAG, "Reading typeface: package: " + mPackageName
							+ ", typeface: " + mTextTypeface);
					Resources remoteRes = mPackageManager
							.getResourcesForApplication(mPackageName);
					mCurrentTypeface = Typeface.createFromAsset(remoteRes
							.getAssets(), mTextTypeface);
					if (debug) Log.d(TAG, "Result: " + mCurrentTypeface);
				} catch (NameNotFoundException e) {
					Log.e(TAG, "Package not found for Typeface", e);
				}
			}

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
			if (debug)
				Log.d(TAG, "textSize: " + mTextSize);

			mTextColorChecked = mThemeAttributes.getColor(ThemeShoppingList.textColorChecked,
					android.R.color.white);
			mShowCheckBox = mThemeAttributes.getBoolean(ThemeShoppingList.showCheckBox, true);
			mShowStrikethrough = mThemeAttributes.getBoolean(
					ThemeShoppingList.textStrikethroughChecked, false);
			mTextSuffixUnchecked = mThemeAttributes
					.getString(ThemeShoppingList.textSuffixUnchecked);
			mTextSuffixChecked = mThemeAttributes
					.getString(ThemeShoppingList.textSuffixChecked);

			int divider = mThemeAttributes.getInteger(ThemeShoppingList.divider, 0);

			Drawable div = null;
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
		float size = ta.getDimensionPixelOffset(
				ThemeShoppingList.textSizeMedium, (int) (23 * scale + 0.5f));
		return size;
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
	
	public void toggleOnAllItems(){
		for(int i=0;i<mCursorItems.getCount();i++){
			mCursorItems.moveToPosition(i);
			
			long oldstatus = mCursorItems
					.getLong(ShoppingActivity.mStringItemsSTATUS);
			
			// Toggle status:
			// bought -> bought
			// want_to_buy -> bought
			// removed_from_list -> want_to_buy
			long newstatus = ShoppingContract.Status.WANT_TO_BUY;
			if(oldstatus == ShoppingContract.Status.WANT_TO_BUY){
				newstatus = ShoppingContract.Status.BOUGHT;
				
				ContentValues values = new ContentValues();
				values.put(ShoppingContract.Contains.STATUS, newstatus);
				if (debug) Log.d(TAG, "update row " + mCursorItems.getString(0) + ", newstatus "
						+ newstatus);
				getContext().getContentResolver().update(
						Uri.withAppendedPath(ShoppingContract.Contains.CONTENT_URI,
								mCursorItems.getString(0)), values, null, null);
				
			}
		}
		
		requery();

		invalidate();
	}
	
	public void toggleItemBought(int position) {
		if (mCursorItems.getCount() <= position) {
			Log.e(TAG, "toggle inexistent item. Probably clicked too quickly?");
			return;
		}

		mCursorItems.moveToPosition(position);

		long oldstatus = mCursorItems
				.getLong(ShoppingActivity.mStringItemsSTATUS);

		// Toggle status depending on mode:
		long newstatus = ShoppingContract.Status.WANT_TO_BUY;
		
		if (mMode == ShoppingActivity.MODE_IN_SHOP) {
			if (oldstatus == ShoppingContract.Status.WANT_TO_BUY) {
				newstatus = ShoppingContract.Status.BOUGHT;
			} // else old was BOUGHT, new should be WANT_TO_BUY, which is the default.
		} else  { // MODE_ADD_ITEMS or MODE_PICK_ITEMS_DLG
			// when we are in integrated add items mode, all three states 
			// might be displayed, but the user can only create two of them.
			// want_to_buy-> removed_from_list
			// bought -> want_to_buy
			// removed_from_list -> want_to_buy
			if (oldstatus == ShoppingContract.Status.WANT_TO_BUY) {
				newstatus = ShoppingContract.Status.REMOVED_FROM_LIST;
			} // else old is REMOVE_FROM_LIST or BOUGHT, new is WANT_TO_BUY, which is the default.
		} 
		
		ContentValues values = new ContentValues();
		values.put(ShoppingContract.Contains.STATUS, newstatus);
		if (debug) Log.d(TAG, "update row " + mCursorItems.getString(0) + ", newstatus "
				+ newstatus);
		getContext().getContentResolver().update(
				Uri.withAppendedPath(ShoppingContract.Contains.CONTENT_URI,
						mCursorItems.getString(0)), values, null, null);
		requery();

		if (PreferenceActivity.prefsStatusAffectsSort(getContext(),  mMode)) {
			invalidate();		
		} 
	}

	public boolean cleanupList() {

		boolean nothingdeleted = true;
		if (false) {
			// by deleteing items

			nothingdeleted = getContext().getContentResolver().delete(
					ShoppingContract.Contains.CONTENT_URI,
					ShoppingContract.Contains.LIST_ID + " = " + mListId + " AND "
							+ ShoppingContract.Contains.STATUS + " = "
							+ ShoppingContract.Status.BOUGHT, null) == 0;

		} else {
			// by changing state
			ContentValues values = new ContentValues();
			values.put(Contains.STATUS, Status.REMOVED_FROM_LIST);
			if (PreferenceActivity.getResetQuantity(getContext()))
				values.put(Contains.QUANTITY, "");
			nothingdeleted = getContext().getContentResolver().update(
					Contains.CONTENT_URI,
					values,
					ShoppingContract.Contains.LIST_ID + " = " + mListId + " AND "
							+ ShoppingContract.Contains.STATUS + " = "
							+ ShoppingContract.Status.BOUGHT, null) == 0;
		}

		requery();

		return !nothingdeleted;

	}

	/**
	 * 
	 * @param activity
	 *            Activity to manage new Cursor.
	 * @param newItem
	 * @param quantity
	 * @param price
	 * @param barcode
	 */
	public void insertNewItem(Activity activity, String newItem,
			String quantity, String priority, String price, String barcode) {

		newItem = newItem.trim();

		long itemId = ShoppingUtils.updateOrCreateItem(getContext(), newItem,
				null, price, barcode);

		if (debug) Log.i(TAG, "Insert new item. " + " itemId = " + itemId + ", listId = "
				+ mListId);
		boolean resetQuantity = PreferenceActivity.getResetQuantity(getContext());
		ShoppingUtils.addItemToList(getContext(), itemId, mListId, Status.WANT_TO_BUY,
				priority, quantity, true, false, resetQuantity);

		fillItems(activity, mListId);

		// Set the item that we have just selected:
		// Get position of ID:
		mCursorItems.moveToPosition(-1);
		while (mCursorItems.moveToNext()) {
			if (mCursorItems.getLong(ShoppingActivity.mStringItemsITEMID) == itemId) {
				int pos = mCursorItems.getPosition();
				// if (pos > 0) {
				// Set selection one before, so that the item is fully
				// visible.
				// setSelection(pos - 1);
				// } else {
				postDelayedSetSelection(pos);
				// }
				break;
			}
		}

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
		if (debug)
			Log.d(TAG, "requery()");
		
		// Test for null pointer exception (issue 313)
		if (mCursorItems != null) {
			mCursorItems.requery();
			updateTotal();

			
			if (mUpdateLastListPosition > 0) {
				if (debug) Log.d(TAG, "Restore list position: pos: " + mLastListPosition
						+ ", top: " + mLastListTop + ", tries: " + mUpdateLastListPosition);
				setSelectionFromTop(mLastListPosition, mLastListTop);
				mUpdateLastListPosition--;
			}
		}
	}

	public void setTotalTextView(TextView tv) {
		mTotalTextView = tv;
	}

	public void setPrioritySubtotalTextView(TextView tv) {
		mPriTotalTextView = tv;
	}
	
	public void setTotalCheckedTextView(TextView tv) {
		mTotalCheckedTextView = tv;
	}
	

	public void setCountTextView(TextView tv) {
		mCountTextView = tv;
		
	}


	/**
	 * Update the text fields for "Total:" and "Checked:" with corresponding
	 * price information.
	 */
	public void updateTotal() {
		if (debug)
			Log.d(TAG, "updateTotal()");
		
		mNumChecked = 0;
		long total = 0;
		long totalchecked = 0;
		long priority_total = 0;
		int priority_threshold = PreferenceActivity.getSubtotalByPriorityThreshold(this
				.getContext());
		boolean prioIncludesChecked = 
			PreferenceActivity.prioritySubtotalIncludesChecked(this.getContext());

		if (false && debug) 
		{
			/* This is the old way of computing the totals. Leaving it here for a bit 
			 * in case some issue comes up. To check whether there is a discrepancy 
			 * between the old way and the new way with your data, you can change the 
			 * above false to true, and look in the logcat for two totals.
			 * 
			 *  There are two known sources of discrepancy:
			 *  
			 *  (1) when "Hide checked items" is set, the old way does not include 
			 *  checked items in any totals, while the new way does. This is intentional.
			 *  
			 *  (2) A discrepancy measured in cents can be caused by a rounding bug in 
			 *  OI Convert CSV when importing prices from HandyShopper. This bug has 
			 *  since been fixed. It should be possible to fix your data by exporting
			 *  in HandyShopper csv format and re-importing that file.
			 */
			if (mCursorItems.isClosed()) {
				// Can happen through onShake() in ShoppingActivity.
				return;
			}
			mCursorItems.moveToPosition(-1);
				while (mCursorItems.moveToNext()) {
			long item_status = mCursorItems.getLong(ShoppingActivity.mStringItemsSTATUS);
			boolean isChecked = (item_status == ShoppingContract.Status.BOUGHT);

			if (item_status == ShoppingContract.Status.REMOVED_FROM_LIST)
				continue;
			
			long price = getQuantityPrice(mCursorItems);
			total += price;
			
			if (isChecked) {
				totalchecked += price;
				mNumChecked++;
			}
			
			if (priority_threshold != 0 && (prioIncludesChecked || !isChecked)) {
				String priority_str = mCursorItems.getString(ShoppingActivity.mStringItemsPRIORITY);
				if (priority_str != null) {
					int priority = 0;
					try {
						priority = Integer.parseInt(priority_str);
					} catch (NumberFormatException e) {
						// pretend it's a 0 then...
					}
					if (priority != 0 && priority <= priority_threshold) {
						priority_total += price;
					}
				}
			}		
			
		}
		if (debug) Log.d(TAG, "Total (old way): " + total + ", Checked: " + totalchecked + "(#" + mNumChecked + ")");
		
		total = priority_total = mNumChecked = totalchecked = 0;
		}

		Cursor total_cursor = getContext().getContentResolver().query(
				Subtotals.CONTENT_URI.buildUpon().appendPath("" + mListId).build(),
				Subtotals.PROJECTION, null, null, null);
		total_cursor.moveToPosition(-1);
		while (total_cursor.moveToNext()) {
			long item_status = total_cursor.getLong(Subtotals.STATUS_INDEX);
			boolean isChecked = (item_status == ShoppingContract.Status.BOUGHT);

			if (item_status == ShoppingContract.Status.REMOVED_FROM_LIST)
				continue;
	
			long price = total_cursor.getLong(Subtotals.SUBTOTAL_INDEX);
			total += price;
	
			if (isChecked) {
				totalchecked += price;
				mNumChecked += total_cursor.getLong(Subtotals.COUNT_INDEX);;
			}
	
			if (priority_threshold != 0 && (prioIncludesChecked || !isChecked)) {
				String priority_str = total_cursor.getString(Subtotals.PRIORITY_INDEX);
				if (priority_str != null) {
					int priority = 0;
					try {
						priority = Integer.parseInt(priority_str);
					} catch (NumberFormatException e) {
						// pretend it's a 0 then...
					}
					if (priority != 0 && priority <= priority_threshold) {
						priority_total += price;
					}
				}
			}			
		}	
		total_cursor.deactivate();
		total_cursor.close();

		if (debug) Log.d(TAG, "Total: " + total + ", Checked: " + totalchecked + "(#" + mNumChecked + ")");

		// Update ActionBar in ShoppingActivity
		// for the "Clean up list" command
		if (mActionBarListener != null) {
			mActionBarListener.updateActionBar();
		}

		if (mTotalTextView == null || mTotalCheckedTextView == null) {
			// Most probably in "Add item" mode where no total is displayed
			return;
		}

		if (mPriceVisibility != View.VISIBLE) {
			// If price is not displayed, do not display total
			mTotalTextView.setVisibility(View.GONE);
			mPriTotalTextView.setVisibility(View.GONE);
			mTotalCheckedTextView.setVisibility(View.GONE);
			return;
		}

		mTotalTextView.setTextColor(mTextColorPrice);
		mPriTotalTextView.setTextColor(mTextColorPrice);
		mTotalCheckedTextView.setTextColor(mTextColorPrice);
		mCountTextView.setTextColor(mTextColorPrice);

		if (total != 0) {
			String s = mPriceFormatter.format(total * 0.01d);
			s = getContext().getString(R.string.total, s);
			mTotalTextView.setText(s);
			mTotalTextView.setVisibility(View.VISIBLE);
		} else {
			mTotalTextView.setVisibility(View.GONE);
		}

		if (priority_total != 0) {
			final int captions[] = {0, R.string.priority1_total, R.string.priority2_total, 
					R.string.priority3_total, R.string.priority4_total };
			String s = mPriceFormatter.format(priority_total * 0.01d);
			s = getContext().getString(captions[priority_threshold], s);
			mPriTotalTextView.setText(s);
			mPriTotalTextView.setVisibility(View.VISIBLE);
		} else {
			mPriTotalTextView.setVisibility(View.GONE);
		}
		
		if (totalchecked != 0) {
			String s = mPriceFormatter.format(totalchecked * 0.01d);
			s = getContext().getString(R.string.total_checked, s);
			mTotalCheckedTextView.setText(s);
			mTotalCheckedTextView.setVisibility(View.VISIBLE);
			mCountTextView.setVisibility(View.VISIBLE);
		} else {
			mTotalCheckedTextView.setVisibility(View.GONE);
			mCountTextView.setVisibility(View.GONE);
		}
		
		mCountTextView.setText("#" + mNumChecked);
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

	OnCustomClickListener mListener = null;
	private boolean mDragAndDropEnabled = false;
	
	public void setCustomClickListener(OnCustomClickListener listener) {
		mListener = listener;
	}

	public interface OnCustomClickListener {
		public void onCustomClick(Cursor c, int pos, EditItemDialog.FieldType field, View v);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (mDragAndDropEnabled ) {
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
		for (int i = 0;; i++) {
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

		for (int i = 0;; i++) {
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
}
