package org.openintents.shopping;

import org.openintents.provider.Shopping.Contains;
import org.openintents.util.BackupManagerWrapper;
import org.openintents.util.IntentUtils;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.method.KeyListener;
import android.text.method.TextKeyListener;

public class PreferenceActivity extends android.preference.PreferenceActivity {
	private static boolean mBackupManagerAvailable;


	static {
		try {
			BackupManagerWrapper.checkAvailable();
			mBackupManagerAvailable = true;
		} catch (Throwable e) {
			mBackupManagerAvailable = false;
		}
	}

	private static final String TAG = "PreferenceActivity";

	public static final String PREFS_SORTORDER = "sortorder";
	public static final String PREFS_SORTORDER_DEFAULT = "3";
	public static final String PREFS_FONTSIZE = "fontsize";
	public static final String PREFS_FONTSIZE_DEFAULT = "2";
	public static final String PREFS_LOADLASTUSED = "loadlastused";
	public static final boolean PREFS_LOADLASTUSED_DEFAULT = true;
	public static final String PREFS_LASTUSED = "lastused";
	public static final String PREFS_HIDECHECKED = "hidechecked";
	public static final boolean PREFS_HIDECHECKED_DEFAULT = false;
	public static final String PREFS_CAPITALIZATION = "capitalization";
	public static final String PREFS_SHOW_PRICE = "showprice";
	public static final boolean PREFS_SHOW_PRICE_DEFAULT = true;
	public static final String PREFS_SHOW_TAGS = "showtags";
	public static final boolean PREFS_SHOW_TAGS_DEFAULT = true;
	public static final String PREFS_SHOW_QUANTITY = "showquantity";
	public static final boolean PREFS_SHOW_QUANTITY_DEFAULT = true;
	public static final String PREFS_SHOW_PRIORITY = "showpriority";
	public static final boolean PREFS_SHOW_PRIORITY_DEFAULT = false;
	public static final String PREFS_SHAKE = "shake";
	public static final boolean PREFS_SHAKE_DEFAULT = false;
	public static final String PREFS_MARKET_EXTENSIONS = "preference_market_extensions";
	public static final String PREFS_MARKET_THEMES = "preference_market_themes";
	public static final String PREFS_THEME_SET_FOR_ALL = "theme_set_for_all";
	public static final String PREFS_SCREEN_ADDONS = "preference_screen_addons";

	public static final int PREFS_CAPITALIZATION_DEFAULT = 1;

	public static final String EXTRA_SHOW_GET_ADD_ONS = "show_get_add_ons";

	private static final TextKeyListener.Capitalize smCapitalizationSettings[] = {
			TextKeyListener.Capitalize.NONE,
			TextKeyListener.Capitalize.SENTENCES,
			TextKeyListener.Capitalize.WORDS };

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
	}

	@Override
	protected void onPause() {
		if (mBackupManagerAvailable) {
			new BackupManagerWrapper(this).dataChanged();
		}

		super.onPause();
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

	/**
	 * Returns the sort order for the notes list based on the user preferences.
	 * Performs error-checking.
	 * 
	 * @param context
	 *            The context to grab the preferences from.
	 */
	static public String getSortOrderFromPrefs(Context context) {
		int sortOrder = 0;
		try {
			sortOrder = Integer.parseInt(PreferenceManager
					.getDefaultSharedPreferences(context).getString(
							PREFS_SORTORDER, PREFS_SORTORDER_DEFAULT));
		} catch (NumberFormatException e) {
			// Guess somebody messed with the preferences and put a string into
			// this
			// field. We'll use the default value then.
		}

		if (sortOrder >= 0 && sortOrder < Contains.SORT_ORDERS.length) {
			return Contains.SORT_ORDERS[sortOrder];
		}

		// Value out of range - somebody messed with the preferences.
		return Contains.SORT_ORDERS[0];
	}

	public static boolean getHideCheckedItemsFromPrefs(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		return prefs.getBoolean(PREFS_HIDECHECKED, PREFS_HIDECHECKED_DEFAULT);
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
