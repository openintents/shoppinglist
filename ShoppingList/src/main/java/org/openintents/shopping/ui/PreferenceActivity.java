package org.openintents.shopping.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.InputType;
import android.text.method.KeyListener;
import android.text.method.TextKeyListener;
import android.widget.Toast;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.util.BackupManagerWrapper;
import org.openintents.util.IntentUtils;

public class PreferenceActivity extends android.preference.PreferenceActivity
        implements OnSharedPreferenceChangeListener {
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

    public static final String PREFS_SORTORDER_SHOPPINGLISTS = "sortorderForShoppingLists";
    public static final String PREFS_SORTORDER_SHOPPINGLISTS_DEFAULT = "0";

    public static final String PREFS_FONTSIZE = "fontsize";
    public static final String PREFS_FONTSIZE_DEFAULT = "2";

    public static final String PREFS_ORIENTATION = "orientation";
    public static final String PREFS_ORIENTATION_DEFAULT = "-1";
    @Deprecated
    public static final String PREFS_LOADLASTUSED = "loadlastused";
    @Deprecated
    public static final boolean PREFS_LOADLASTUSED_DEFAULT = true;
    public static final String PREFS_LASTUSED = "lastused";
    public static final String PREFS_LASTLIST_POSITION = "lastlist_position";
    public static final String PREFS_LASTLIST_TOP = "lastlist_top";
    public static final String PREFS_HIDECHECKED = "hidechecked";
    public static final boolean PREFS_HIDECHECKED_DEFAULT = false;
    public static final String PREFS_FASTSCROLL = "fastscroll";
    public static final boolean PREFS_FASTSCROLL_DEFAULT = false;
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
    public static final String PREFS_SCREENLOCK = "screenlock";
    public static final boolean PREFS_SCREENLOCK_DEFAULT = false;
    public static final String PREFS_RESETQUANTITY = "resetquantity";
    public static final boolean PREFS_RESETQUANTITY_DEFAULT = false;
    public static final String PREFS_ADDFORBARCODE = "addforbarcode";
    public static final boolean PREFS_ADDFORBARCODE_DEFAULT = false;
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
    public static final String PREFS_QUICKEDITMODE = "quickedit";
    public static final boolean PREFS_QUICKEDITMODE_DEFAULT = false;
    public static final String PREFS_USE_FILTERS = "use_filters";
    public static final boolean PREFS_USE_FILTERS_DEFAULT = false;
    public static final String PREFS_CURRENT_LIST_COMPLETE = "autocomplete_only_this_list";
    public static final boolean PREFS_CURRENT_LIST_COMPLETE_DEFAULT = false;
    public static final String PREFS_SORT_PER_LIST = "perListSort";
    public static final boolean PREFS_SORT_PER_LIST_DEFAULT = false;
    public static final String PREFS_HOLO_SEARCH = "holosearch";
    public static final boolean PREFS_HOLO_SEARCH_DEFAULT = true;

    public static final String PREFS_RESET_ALL_SETTINGS = "reset_all_settings";

    public static final int PREFS_CAPITALIZATION_DEFAULT = 1;

    public static final String EXTRA_SHOW_GET_ADD_ONS = "show_get_add_ons";

    private static final TextKeyListener.Capitalize smCapitalizationSettings[] = {
            TextKeyListener.Capitalize.NONE,
            TextKeyListener.Capitalize.SENTENCES,
            TextKeyListener.Capitalize.WORDS};
    private static final int smCapitalizationInputTypes[] = {
            InputType.TYPE_CLASS_TEXT,
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
            InputType.TYPE_TEXT_FLAG_CAP_WORDS};

    private ListPreference mPrioSubtotal;
    private CheckBoxPreference mIncludesChecked;
    private ListPreference mPickItemsSort;

    private static boolean mFilterCompletionChanged = false;

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
        resetAllSettings(shared);
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
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        mFilterCompletionChanged = false;
    }

    @Override
    protected void onPause() {
        if (mBackupManagerAvailable) {
            new BackupManagerWrapper(this).dataChanged();
        }
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
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
        if (key.equals(PREFS_CURRENT_LIST_COMPLETE)) {
            mFilterCompletionChanged = true;
        }
    }

    private void resetAllSettings(final SharedPreferences prefs) {
        Preference resetAllSettings = findPreference(PREFS_RESET_ALL_SETTINGS);
        resetAllSettings
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        AlertDialog alert = new AlertDialog.Builder(
                                PreferenceActivity.this).create();
                        alert.setTitle(R.string.preference_reset_all_settings);
                        alert.setMessage(getString(R.string.preference_reset_all_settings_alert));
                        alert.setButton(getString(android.R.string.yes),
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        SharedPreferences.Editor editor = prefs
                                                .edit();
                                        // Main
                                        editor.putString(PREFS_FONTSIZE,
                                                PREFS_FONTSIZE_DEFAULT);
                                        editor.putString(PREFS_SORTORDER,
                                                PREFS_SORTORDER_DEFAULT);
                                        // Main advanced
                                        editor.putString(
                                                PREFS_CAPITALIZATION,
                                                String.valueOf(PREFS_CAPITALIZATION_DEFAULT));
                                        editor.putString(PREFS_ORIENTATION,
                                                PREFS_ORIENTATION_DEFAULT);
                                        editor.putBoolean(PREFS_HIDECHECKED,
                                                PREFS_HIDECHECKED_DEFAULT);
                                        editor.putBoolean(PREFS_FASTSCROLL,
                                                PREFS_FASTSCROLL_DEFAULT);
                                        editor.putBoolean(PREFS_SHAKE,
                                                PREFS_SHAKE_DEFAULT);
                                        editor.putBoolean(PREFS_PERSTOREPRICES,
                                                PREFS_PERSTOREPRICES_DEFAULT);
                                        editor.putBoolean(PREFS_ADDFORBARCODE,
                                                PREFS_ADDFORBARCODE_DEFAULT);
                                        editor.putBoolean(PREFS_SCREENLOCK,
                                                PREFS_SCREENLOCK_DEFAULT);
                                        editor.putBoolean(PREFS_QUICKEDITMODE,
                                                PREFS_QUICKEDITMODE_DEFAULT);
                                        editor.putBoolean(PREFS_USE_FILTERS,
                                                PREFS_USE_FILTERS_DEFAULT);
                                        editor.putBoolean(PREFS_RESETQUANTITY,
                                                PREFS_RESETQUANTITY_DEFAULT);
                                        editor.putBoolean(PREFS_HOLO_SEARCH,
                                                PREFS_HOLO_SEARCH_DEFAULT);
                                        // Appearance
                                        editor.putBoolean(PREFS_SHOW_PRICE,
                                                PREFS_SHOW_PRICE_DEFAULT);
                                        editor.putBoolean(PREFS_SHOW_TAGS,
                                                PREFS_SHOW_TAGS_DEFAULT);
                                        editor.putBoolean(PREFS_SHOW_UNITS,
                                                PREFS_SHOW_UNITS_DEFAULT);
                                        editor.putBoolean(PREFS_SHOW_QUANTITY,
                                                PREFS_SHOW_QUANTITY_DEFAULT);
                                        editor.putBoolean(PREFS_SHOW_PRIORITY,
                                                PREFS_SHOW_PRIORITY_DEFAULT);
                                        // Pick items
                                        editor.putBoolean(
                                                PREFS_SAMESORTFORPICK,
                                                PREFS_SAMESORTFORPICK_DEFAULT);
                                        editor.putString(
                                                PREFS_PICKITEMS_SORTORDER,
                                                PREFS_PICKITEMS_SORTORDER_DEFAULT);
                                        editor.putBoolean(
                                                PREFS_PICKITEMSINLIST,
                                                PREFS_PICKITEMSINLIST_DEFAULT);
                                        editor.putString(
                                                PREFS_SORTORDER_SHOPPINGLISTS,
                                                PREFS_SORTORDER_SHOPPINGLISTS_DEFAULT);
                                        // Subtotal
                                        editor.putString(PREFS_PRIOSUBTOTAL,
                                                PREFS_PRIOSUBTOTAL_DEFAULT);
                                        editor.putBoolean(
                                                PREFS_PRIOSUBINCLCHECKED,
                                                PREFS_PRIOSUBINCLCHECKED_DEFAULT);

                                        editor.putBoolean(PREFS_CURRENT_LIST_COMPLETE, PREFS_CURRENT_LIST_COMPLETE_DEFAULT);
                                        editor.putBoolean(PREFS_SORT_PER_LIST, PREFS_SORT_PER_LIST_DEFAULT);

                                        editor.commit();

                                        Toast.makeText(
                                                PreferenceActivity.this,
                                                R.string.preference_reset_all_settings_done,
                                                Toast.LENGTH_LONG).show();
                                        finish();
                                    }
                                }
                        );
                        alert.setButton2(getString(android.R.string.cancel),
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        dialog.dismiss();
                                    }
                                }
                        );
                        alert.show();
                        return false;
                    }
                });
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
        // maybe we should set the label to say the active sort order.
        // but not tonight.
        // CharSequence labels[] = mPickItemsSort.getEntries();
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

    public static int getOrientationFromPrefs(Context context) {
        int orientation = Integer.parseInt(PreferenceManager
                .getDefaultSharedPreferences(context).getString(
                        PREFS_ORIENTATION, PREFS_ORIENTATION_DEFAULT));
        return orientation;
    }

    public static boolean getCompleteFromCurrentListOnlyFromPrefs(Context context) {
        boolean filter = PreferenceManager
                .getDefaultSharedPreferences(context).getBoolean(PREFS_CURRENT_LIST_COMPLETE,
                        PREFS_CURRENT_LIST_COMPLETE_DEFAULT);
        return filter;
    }

    public static boolean getCompletionSettingChanged(Context context) {
        return mFilterCompletionChanged;
    }

    public static boolean getUsingPerStorePricesFromPrefs(Context context) {
        boolean using = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_PERSTOREPRICES, PREFS_PERSTOREPRICES_DEFAULT);
        return using;
    }

    public static boolean getQuickEditModeFromPrefs(Context context) {
        boolean using = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_QUICKEDITMODE, PREFS_QUICKEDITMODE_DEFAULT);
        return using;
    }

    public static boolean getUsingFiltersFromPrefs(Context context) {
        boolean using = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_USE_FILTERS, PREFS_USE_FILTERS_DEFAULT);
        return using;
    }

    public static boolean getUsingHoloSearchFromPrefs(Context context) {
        boolean using = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_HOLO_SEARCH, PREFS_HOLO_SEARCH_DEFAULT);
        return using;
    }

    public static boolean getPickItemsInListFromPrefs(Context context) {
        // boolean using = PreferenceManager.getDefaultSharedPreferences(context)
        //		.getBoolean(PREFS_PICKITEMSINLIST,
        //				PREFS_PICKITEMSINLIST_DEFAULT);
        // return using;
        return true;
    }

    public static boolean getUsingPerListSortFromPrefs(Context context) {
        boolean perListSort = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_SORT_PER_LIST,
                        PREFS_SORT_PER_LIST_DEFAULT);
        return perListSort;
    }

    /**
     * Returns the sort order for the notes list based on the user preferences.
     * Performs error-checking.
     *
     * @param context The context to grab the preferences from.
     */
    static public int getSortOrderIndexFromPrefs(Context context, int mode, long listId) {
        int sortOrder = 0;

        if (mode != ShoppingActivity.MODE_IN_SHOP) {
            boolean followShopping = PreferenceManager
                    .getDefaultSharedPreferences(context).getBoolean(
                            PREFS_SAMESORTFORPICK,
                            PREFS_SAMESORTFORPICK_DEFAULT);
            if (followShopping) {
                mode = ShoppingActivity.MODE_IN_SHOP;
            }
        }

        if (mode != ShoppingActivity.MODE_IN_SHOP) {
            // use the pick-items-specific value, if there is one
            try {
                sortOrder = Integer.parseInt(PreferenceManager
                        .getDefaultSharedPreferences(context).getString(
                                PREFS_PICKITEMS_SORTORDER,
                                PREFS_PICKITEMS_SORTORDER_DEFAULT));
            } catch (NumberFormatException e) {
                // Guess somebody messed with the preferences and put a string
                // into
                // this field. We'll follow shopping mode then.
                mode = ShoppingActivity.MODE_IN_SHOP;
            }
        }

        if (mode == ShoppingActivity.MODE_IN_SHOP) {

            boolean set = false;
            if (PreferenceActivity.getUsingPerListSortFromPrefs(context)) {
                String sortOrderStr = ShoppingUtils.getListSortOrder(context,
                        listId);
                if (sortOrderStr != null) {
                    try {
                        sortOrder = Integer.parseInt(sortOrderStr);
                        set = true;
                    } catch (NumberFormatException e) {
                        // Guess somebody messed with the preferences and put a string
                        // into
                        // this field. We'll use the default value then.
                    }
                }
            }

            if (set == false) {
                try {
                    sortOrder = Integer.parseInt(PreferenceManager
                            .getDefaultSharedPreferences(context).getString(
                                    PREFS_SORTORDER, PREFS_SORTORDER_DEFAULT));
                } catch (NumberFormatException e) {
                    // Guess somebody messed with the preferences and put a string
                    // into
                    // this field. We'll use the default value then.
                }
            }
        }

        if (sortOrder >= 0 && sortOrder < Contains.SORT_ORDERS.length) {
            return sortOrder;
        }

        // Value out of range - somebody messed with the preferences.
        return 0;
    }

    static public int getSortOrderIndexFromPrefs(Context context, int mode) {
        long listId = ShoppingUtils.getDefaultList(context);
        return getSortOrderIndexFromPrefs(context, mode, listId);
    }

    static public String getSortOrderFromPrefs(Context context, int mode) {
        int index = getSortOrderIndexFromPrefs(context, mode);
        return Contains.SORT_ORDERS[index];
    }

    static public String getSortOrderFromPrefs(Context context, int mode, long listId) {
        int index = getSortOrderIndexFromPrefs(context, mode, listId);
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

    public static String getShoppingListSortOrderFromPrefs(Context context) {
        int index = Integer.parseInt(PreferenceManager
                .getDefaultSharedPreferences(context).getString(
                        PREFS_SORTORDER_SHOPPINGLISTS,
                        PREFS_SORTORDER_SHOPPINGLISTS_DEFAULT));
        if (index >= 0 && index < Lists.SORT_ORDERS.length) {
            return Lists.SORT_ORDERS[index];
        }

        return Lists.DEFAULT_SORT_ORDER;
    }

    public static boolean getHideCheckedItemsFromPrefs(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_HIDECHECKED, PREFS_HIDECHECKED_DEFAULT);
    }

    public static boolean getFastScrollEnabledFromPrefs(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_FASTSCROLL, PREFS_FASTSCROLL_DEFAULT);
    }

    private static int getSubtotalByPriorityThreshold(SharedPreferences prefs) {
        String pref = prefs.getString(PREFS_PRIOSUBTOTAL,
                PREFS_PRIOSUBTOTAL_DEFAULT);
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
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return getSubtotalByPriorityThreshold(prefs);
    }

    public static boolean prioritySubtotalIncludesChecked(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_PRIOSUBINCLCHECKED,
                PREFS_PRIOSUBINCLCHECKED_DEFAULT);
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

    /**
     * Returns InputType for the search bar based on the capitalization
     * preferences of the user.
     *
     * @ param context The context to grab the preferences from.
     */
    static public int getSearchInputTypeFromPrefs(
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

        return smCapitalizationInputTypes[capitalization];
    }

    public static boolean getThemeSetForAll(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_THEME_SET_FOR_ALL, false);
    }

    public static boolean getResetQuantity(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_RESETQUANTITY, PREFS_RESETQUANTITY_DEFAULT);
    }

    public static boolean getAddForBarcode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_ADDFORBARCODE, PREFS_ADDFORBARCODE_DEFAULT);
    }

    public static void setThemeSetForAll(Context context, boolean setForAll) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        Editor ed = prefs.edit();
        ed.putBoolean(PREFS_THEME_SET_FOR_ALL, setForAll);
        ed.commit();
    }

}
