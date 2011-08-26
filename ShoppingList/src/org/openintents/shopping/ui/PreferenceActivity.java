package org.openintents.shopping.ui;

import org.openintents.shopping.R;
import org.openintents.shopping.R.string;
import org.openintents.shopping.R.xml;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.util.BackupManagerWrapper;
import org.openintents.util.IntentUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.method.KeyListener;
import android.text.method.TextKeyListener;

public class PreferenceActivity extends android.preference.PreferenceActivity implements OnSharedPreferenceChangeListener {
	private static boolean mBackupManagerAvailable;
	public static int updateCount = 0;

	static {
		try {
			BackupManagerWrapper.checkAvailable();
			mBackupManagerAvailable = true;
		} catch (Throwable e) {
			mBackupManagerAvailable = false;
		}
	}

	private static final String TAG = "PreferenceActivity";

	public static final String PREFS_SAMESORTFORPICK = "samesortforpick";
	public static final boolean PREFS_SAMESORTFORPICK_DEFAULT = false;
	
	public static final String PREFS_SORTORDER = "sortorder";
	public static final String PREFS_PICKITEMS_SORTORDER = "sortorderForPickItems";

	public static final String PREFS_SORTORDER_DEFAULT = "3";
	public static final String PREFS_PICKITEMS_SORTORDER_DEFAULT = "1";

	public static final String PREFS_FONTSIZE = "fontsize";
	public static final String PREFS_FONTSIZE_DEFAULT = "2";
	@Deprecated
	public static final String PREFS_LOADLASTUSED = "loadlastused";
	@Deprecated
	public static final boolean PREFS_LOADLASTUSED_DEFAULT = true;
	public static final String PREFS_LASTUSED = "lastused";
	public static final String PREFS_LASTLIST_POSITION = "lastlist_position";
	public static final String PREFS_LASTLIST_TOP = "lastlist_top";
	public static final String PREFS_HIDECHECKED = "hidechecked";
	public static final boolean PREFS_HIDECHECKED_DEFAULT = false;
	public static final String PREFS_CAPITALIZATION = "capitalization";
	public static final String PREFS_SHOW_PRICE = "showprice";
	public static final boolean PREFS_SHOW_PRICE_DEFAULT = true;
	public static final String PREFS_PERSTOREPRICES = "perstoreprices";
	public static final boolean PREFS_PERSTOREPRICES_DEFAULT = false;
	public static final String PREFS_SHOW_TAGS = "showtags";
	public static final boolean PREFS_SHOW_TAGS_DEFAULT = true;
	public static final String PREFS_SHOW_QUANTITY = "showquantity";
	public static final boolean PREFS_SHOW_QUANTITY_DEFAULT = true;
	public static final String PREFS_SHOW_UNITS = "showunits";
	public static final boolean PREFS_SHOW_UNITS_DEFAULT = true;
	public static final String PREFS_SHOW_PRIORITY = "showpriority";
	public static final boolean PREFS_SHOW_PRIORITY_DEFAULT = true;
	public static final String PREFS_SHAKE = "shake";
	public static final boolean PREFS_SHAKE_DEFAULT = false;
	public static final String PREFS_MARKET_EXTENSIONS = "preference_market_extensions";
	public static final String PREFS_MARKET_THEMES = "preference_market_themes";
	public static final String PREFS_THEME_SET_FOR_ALL = "theme_set_for_all";
	public static final String PREFS_SCREEN_ADDONS = "preference_screen_addons";
	public static final String PREFS_PRIOSUBTOTAL = "priority_subtotal_threshold";
	public static final String PREFS_PRIOSUBTOTAL_DEFAULT = "0";
	public static final String PREFS_PRIOSUBINCLCHECKED = "priosubtotal_includes_checked";
	public static final boolean PREFS_PRIOSUBINCLCHECKED_DEFAULT = true;
	public static final String PREFS_PICKITEMSINLIST = "pickitemsinlist";
	public static final boolean PREFS_PICKITEMSINLIST_DEFAULT = false;
	
	public static final int PREFS_CAPITALIZATION_DEFAULT = 1;

	public static final String EXTRA_SHOW_GET_ADD_ONS = "show_get_add_ons";

	private static final TextKeyListener.Capitalize smCapitalizationSettings[] = {
			TextKeyListener.Capitalize.NONE,
			TextKeyListener.Capitalize.SENTENCES,
			TextKeyListener.Capitalize.WORDS };

	
	private ListPreference mPrioSubtotal; 
	private CheckBoxPreference mIncludesChecked;
	private ListPreference mPickItemsSort;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		// Set enabled state of Market preference
		PreferenceScreen sp = (PreferenceScreen) findPreference(PREFS_MARKET_EXTENSIONS);
		sp.setEnabled(isMarketAvailable());
		sp = (PreferenceScreen) findPreference(PREFS_MARKET_THEMES);
		sp.setEnabled(isMarketAvailable());
		
		mPrioSubtotal = (ListPreference) findPreference(PREFS_PRIOSUBTOTAL);
		mPickItemsSort = (ListPreference) findPreference(PREFS_PICKITEMS_SORTORDER);

		mIncludesChecked = (CheckBoxPreference) findPreference(PREFS_PRIOSUBINCLCHECKED);
		SharedPreferences shared = getPreferenceScreen().getSharedPreferences();
		updatePrioSubtotalSummary(shared);
		updatePickItemsSortPref(shared);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (getIntent() != null && getIntent().hasExtra(EXTRA_SHOW_GET_ADD_ONS)) {
			// Open License section directly:
			PreferenceScreen licensePrefScreen = (PreferenceScreen) getPreferenceScreen()
					.findPreference(PREFS_SCREEN_ADDONS);
			setPreferenceScreen(licensePrefScreen);
		}
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		if (mBackupManagerAvailable) {
			new BackupManagerWrapper(this).dataChanged();
		}
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		updateCount++;
        if (key.equals(PREFS_PRIOSUBTOTAL)) {
        	updatePrioSubtotalSummary(prefs);
        }
        if (key.equals(PREFS_SAMESORTFORPICK)) {
        	updatePickItemsSortPref(prefs);
        }
	}
	
	private void updatePrioSubtotalSummary(SharedPreferences prefs) {
    	int threshold = getSubtotalByPriorityThreshold(prefs);
    	CharSequence labels[] = mPrioSubtotal.getEntries();
        mPrioSubtotal.setSummary(labels[threshold]);
        mIncludesChecked.setEnabled(threshold != 0);
	}
	
	private void updatePickItemsSortPref(SharedPreferences prefs) {
    	boolean sameSort = prefs.getBoolean(PREFS_SAMESORTFORPICK,
    			PREFS_SAMESORTFORPICK_DEFAULT);
        mPickItemsSort.setEnabled(!sameSort);
        //maybe we should set the label to say the active sort order.
        //but not tonight.
    	//CharSequence labels[] = mPickItemsSort.getEntries();
	}

	/**
	 * Check whether Market is available.
	 * 
	 * @return true if Market is available
	 */
	private boolean isMarketAvailable() {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri
				.parse(getString(R.string.preference_market_extensions_link)));
		return IntentUtils.isIntentAvailable(this, i);
	}

	public static int getFontSizeFromPrefs(Context context) {
		int size = Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(context).getString(PREFS_FONTSIZE,
						PREFS_FONTSIZE_DEFAULT));
		return size;
	}

	public static boolean getUsingPerStorePricesFromPrefs(Context context) {
		boolean using = PreferenceManager.getDefaultSharedPreferences(context)
		.getBoolean(PREFS_PERSTOREPRICES,PREFS_PERSTOREPRICES_DEFAULT);
		return using;
	}
	
	public static boolean getPickItemsInListFromPrefs(Context context) {
		boolean using = PreferenceManager.getDefaultSharedPreferences(context)
		.getBoolean(PREFS_PICKITEMSINLIST,PREFS_PICKITEMSINLIST_DEFAULT);
		return using;
	}
	
	/**
	 * Returns the sort order for the notes list based on the user preferences.
	 * Performs error-checking.
	 * 
	 * @param context
	 *            The context to grab the preferences from.
	 */
	static public int getSortOrderIndexFromPrefs(Context context, int mode) {
		int sortOrder = 0;
		
		if (mode != ShoppingActivity.MODE_IN_SHOP) {
		  boolean followShopping = PreferenceManager.getDefaultSharedPreferences(context)
		.getBoolean(PREFS_SAMESORTFORPICK,PREFS_SAMESORTFORPICK_DEFAULT);
		  if (followShopping) {
			  mode = ShoppingActivity.MODE_IN_SHOP;
		  }
		}
		
		if (mode != ShoppingActivity.MODE_IN_SHOP) {
			// use the pick-items-specific value, if there is one
			try {
				sortOrder = Integer.parseInt(PreferenceManager
						.getDefaultSharedPreferences(context).getString(
								PREFS_PICKITEMS_SORTORDER, PREFS_PICKITEMS_SORTORDER_DEFAULT));			
			} catch (NumberFormatException e) {
				// Guess somebody messed with the preferences and put a string into
				// this field. We'll follow shopping mode then.
				mode = ShoppingActivity.MODE_IN_SHOP;
			}
		}

		if (mode == ShoppingActivity.MODE_IN_SHOP) {
			try {
			    sortOrder = Integer.parseInt(PreferenceManager
			    		.getDefaultSharedPreferences(context).getString(
							PREFS_SORTORDER, PREFS_SORTORDER_DEFAULT));
			} catch (NumberFormatException e) {
			// Guess somebody messed with the preferences and put a string into
			// this field. We'll use the default value then.
			}
		}

		if (sortOrder >= 0 && sortOrder < Contains.SORT_ORDERS.length) {
			return sortOrder;
		}

		// Value out of range - somebody messed with the preferences.
		return 0;
	}

	static public String getSortOrderFromPrefs(Context context, int mode) {
		int index = getSortOrderIndexFromPrefs(context, mode);
		return Contains.SORT_ORDERS[index];
	}
	
	static public boolean prefsStatusAffectsSort(Context context, int mode) {
		int index = getSortOrderIndexFromPrefs(context, mode);
		boolean affects = Contains.StatusAffectsSortOrder[index];
		if (mode == ShoppingActivity.MODE_IN_SHOP && !affects) {
			// in shopping mode we should also invalidate display when
			// marking items if we are hiding checked items.
			affects = getHideCheckedItemsFromPrefs(context);
		}
		return affects;
	}
	
	public static boolean getHideCheckedItemsFromPrefs(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		return prefs.getBoolean(PREFS_HIDECHECKED, PREFS_HIDECHECKED_DEFAULT);
	}
	
	private static int getSubtotalByPriorityThreshold(SharedPreferences prefs) {
		String pref = prefs.getString(PREFS_PRIOSUBTOTAL, PREFS_PRIOSUBTOTAL_DEFAULT);
		int threshold = 0;
		try {
			threshold = Integer.parseInt(pref);
		} catch (NumberFormatException e) {
			// Guess somebody messed with the preferences and put a string into
			// this
			// field. We'll use the default value then.
		}
		return threshold;
	}
	
	public static int getSubtotalByPriorityThreshold(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return getSubtotalByPriorityThreshold(prefs);
	}

	public static boolean prioritySubtotalIncludesChecked(Context context) {
		SharedPreferences prefs = PreferenceManager
		.getDefaultSharedPreferences(context);
		return prefs.getBoolean(PREFS_PRIOSUBINCLCHECKED, PREFS_PRIOSUBINCLCHECKED_DEFAULT);
	}
	
	/**
	 * Returns a KeyListener for edit texts that will match the capitalization
	 * preferences of the user.
	 * 
	 * @ param context The context to grab the preferences from.
	 */
	static public KeyListener getCapitalizationKeyListenerFromPrefs(
			Context context) {
		int capitalization = PREFS_CAPITALIZATION_DEFAULT;
		try {
			capitalization = Integer.parseInt(PreferenceManager
					.getDefaultSharedPreferences(context).getString(
							PREFS_CAPITALIZATION,
							Integer.toString(PREFS_CAPITALIZATION_DEFAULT)));
		} catch (NumberFormatException e) {
			// Guess somebody messed with the preferences and put a string
			// into this
			// field. We'll use the default value then.
		}

		if (capitalization < 0
				|| capitalization > smCapitalizationSettings.length) {
			// Value out of range - somebody messed with the preferences.
			capitalization = PREFS_CAPITALIZATION_DEFAULT;
		}

		return new TextKeyListener(smCapitalizationSettings[capitalization],
				true);
	}

	public static boolean getThemeSetForAll(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		return prefs.getBoolean(PREFS_THEME_SET_FOR_ALL, false);
	}

	public static void setThemeSetForAll(Context context, boolean setForAll) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor ed = prefs.edit();
		ed.putBoolean(PREFS_THEME_SET_FOR_ALL, setForAll);
		ed.commit();
	}

}
