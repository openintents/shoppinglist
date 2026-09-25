package org.openintents.shopping.ui

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceManager
import android.preference.PreferenceScreen
import android.text.InputType
import android.text.method.KeyListener
import android.text.method.TextKeyListener
import android.widget.Toast

import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract.Contains
import org.openintents.shopping.library.provider.ShoppingContract.Lists
import org.openintents.shopping.library.util.ShoppingUtils
import org.openintents.util.BackupManagerWrapper
import org.openintents.util.IntentUtils

class PreferenceActivity : android.preference.PreferenceActivity(),
        SharedPreferences.OnSharedPreferenceChangeListener {

    private var mPrioSubtotal: ListPreference? = null
    private var mIncludesChecked: CheckBoxPreference? = null
    private var mPickItemsSort: ListPreference? = null

    companion object {
        const val PREFS_SAMESORTFORPICK = "samesortforpick"
        const val PREFS_SAMESORTFORPICK_DEFAULT = false
        /** Sort mode for the shopping list (other values: Pick items). */
        const val MODE_IN_SHOP = 1
        const val PREFS_SORTORDER = "sortorder"
        const val PREFS_PICKITEMS_SORTORDER = "sortorderForPickItems"
        const val PREFS_SORTORDER_DEFAULT = "3"
        const val PREFS_PICKITEMS_SORTORDER_DEFAULT = "1"
        const val PREFS_SORTORDER_SHOPPINGLISTS = "sortorderForShoppingLists"
        const val PREFS_SORTORDER_SHOPPINGLISTS_DEFAULT = "0"
        const val PREFS_FONTSIZE = "fontsize"
        const val PREFS_FONTSIZE_DEFAULT = "2"
        const val PREFS_ORIENTATION = "orientation"
        const val PREFS_ORIENTATION_DEFAULT = "-1"
        @Deprecated("") const val PREFS_LOADLASTUSED = "loadlastused"
        @Deprecated("") const val PREFS_LOADLASTUSED_DEFAULT = true
        const val PREFS_LASTUSED = "lastused"
        const val PREFS_LASTLIST_POSITION = "lastlist_position"
        const val PREFS_LASTLIST_TOP = "lastlist_top"
        const val PREFS_HIDECHECKED = "hidechecked"
        const val PREFS_HIDECHECKED_DEFAULT = false
        const val PREFS_FASTSCROLL = "fastscroll"
        const val PREFS_FASTSCROLL_DEFAULT = false
        const val PREFS_CAPITALIZATION = "capitalization"
        const val PREFS_SHOW_PRICE = "showprice"
        const val PREFS_SHOW_PRICE_DEFAULT = true
        const val PREFS_PERSTOREPRICES = "perstoreprices"
        const val PREFS_PERSTOREPRICES_DEFAULT = false
        const val PREFS_SHOW_TAGS = "showtags"
        const val PREFS_SHOW_TAGS_DEFAULT = true
        const val PREFS_SHOW_QUANTITY = "showquantity"
        const val PREFS_SHOW_QUANTITY_DEFAULT = true
        const val PREFS_SHOW_UNITS = "showunits"
        const val PREFS_SHOW_UNITS_DEFAULT = true
        const val PREFS_SHOW_PRIORITY = "showpriority"
        const val PREFS_SHOW_PRIORITY_DEFAULT = true
        const val PREFS_SCREENLOCK = "screenlock"
        const val PREFS_SCREENLOCK_DEFAULT = false
        const val PREFS_RESETQUANTITY = "resetquantity"
        const val PREFS_RESETQUANTITY_DEFAULT = false
        const val PREFS_ADDFORBARCODE = "addforbarcode"
        const val PREFS_ADDFORBARCODE_DEFAULT = false
        const val PREFS_SHAKE = "shake"
        const val PREFS_SHAKE_DEFAULT = false
        const val PREFS_MARKET_EXTENSIONS = "preference_market_extensions"
        const val PREFS_MARKET_THEMES = "preference_market_themes"
        const val PREFS_THEME_SET_FOR_ALL = "theme_set_for_all"
        const val PREFS_SCREEN_ADDONS = "preference_screen_addons"
        const val PREFS_PRIOSUBTOTAL = "priority_subtotal_threshold"
        const val PREFS_PRIOSUBTOTAL_DEFAULT = "0"
        const val PREFS_PRIOSUBINCLCHECKED = "priosubtotal_includes_checked"
        const val PREFS_PRIOSUBINCLCHECKED_DEFAULT = true
        const val PREFS_PICKITEMSINLIST = "pickitemsinlist"
        const val PREFS_PICKITEMSINLIST_DEFAULT = false
        const val PREFS_QUICKEDITMODE = "quickedit"
        const val PREFS_QUICKEDITMODE_DEFAULT = false
        const val PREFS_USE_FILTERS = "use_filters"
        const val PREFS_USE_FILTERS_DEFAULT = false
        const val PREFS_CURRENT_LIST_COMPLETE = "autocomplete_only_this_list"
        const val PREFS_CURRENT_LIST_COMPLETE_DEFAULT = false
        const val PREFS_SORT_PER_LIST = "perListSort"
        const val PREFS_SORT_PER_LIST_DEFAULT = false
        const val PREFS_HOLO_SEARCH = "holosearch"
        const val PREFS_HOLO_SEARCH_DEFAULT = true
        const val PREFS_SHOW_LAYOUT_CHOICE = "show_layout_choice"
        const val PREFS_RESET_ALL_SETTINGS = "reset_all_settings"
        const val PREFS_CAPITALIZATION_DEFAULT = 1
        const val EXTRA_SHOW_GET_ADD_ONS = "show_get_add_ons"

        private const val TAG = "PreferenceActivity"

        private val smCapitalizationSettings = arrayOf(
                TextKeyListener.Capitalize.NONE,
                TextKeyListener.Capitalize.SENTENCES,
                TextKeyListener.Capitalize.WORDS)

        private val smCapitalizationInputTypes = intArrayOf(
                InputType.TYPE_CLASS_TEXT,
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                InputType.TYPE_TEXT_FLAG_CAP_WORDS)

        @JvmField
        var updateCount: Int = 0

        private var mBackupManagerAvailable: Boolean
        private var mFilterCompletionChanged: Boolean = false

        init {
            mBackupManagerAvailable = try {
                BackupManagerWrapper.checkAvailable()
                true
            } catch (e: Throwable) {
                false
            }
        }

        @JvmStatic
        fun getFontSizeFromPrefs(context: Context): Int {
            return Integer.parseInt(PreferenceManager
                    .getDefaultSharedPreferences(context).getString(PREFS_FONTSIZE,
                            PREFS_FONTSIZE_DEFAULT))
        }

        @JvmStatic
        fun getOrientationFromPrefs(context: Context): Int {
            return Integer.parseInt(PreferenceManager
                    .getDefaultSharedPreferences(context).getString(
                            PREFS_ORIENTATION, PREFS_ORIENTATION_DEFAULT))
        }

        @JvmStatic
        fun getCompleteFromCurrentListOnlyFromPrefs(context: Context): Boolean {
            return PreferenceManager
                    .getDefaultSharedPreferences(context).getBoolean(PREFS_CURRENT_LIST_COMPLETE,
                            PREFS_CURRENT_LIST_COMPLETE_DEFAULT)
        }

        @JvmStatic
        fun getCompletionSettingChanged(context: Context): Boolean {
            return mFilterCompletionChanged
        }

        @JvmStatic
        fun getUsingPerStorePricesFromPrefs(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(PREFS_PERSTOREPRICES, PREFS_PERSTOREPRICES_DEFAULT)
        }

        @JvmStatic
        fun getQuickEditModeFromPrefs(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(PREFS_QUICKEDITMODE, PREFS_QUICKEDITMODE_DEFAULT)
        }

        @JvmStatic
        fun getUsingFiltersFromPrefs(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(PREFS_USE_FILTERS, PREFS_USE_FILTERS_DEFAULT)
        }

        @JvmStatic
        fun getUsingHoloSearchFromPrefs(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(PREFS_HOLO_SEARCH, PREFS_HOLO_SEARCH_DEFAULT)
        }

        @JvmStatic
        fun getPickItemsInListFromPrefs(context: Context): Boolean {
            // boolean using = PreferenceManager.getDefaultSharedPreferences(context)
            //		.getBoolean(PREFS_PICKITEMSINLIST,
            //				PREFS_PICKITEMSINLIST_DEFAULT);
            // return using;
            return true
        }

        @JvmStatic
        fun getUsingPerListSortFromPrefs(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(PREFS_SORT_PER_LIST,
                            PREFS_SORT_PER_LIST_DEFAULT)
        }

        /**
         * Returns the sort order for the notes list based on the user preferences.
         * Performs error-checking.
         *
         * @param context The context to grab the preferences from.
         */
        @JvmStatic
        fun getSortOrderIndexFromPrefs(context: Context, mode: Int, listId: Long): Int {
            var sortOrder = 0
            var effectiveMode = mode

            if (effectiveMode != MODE_IN_SHOP) {
                val followShopping = PreferenceManager
                        .getDefaultSharedPreferences(context).getBoolean(
                                PREFS_SAMESORTFORPICK,
                                PREFS_SAMESORTFORPICK_DEFAULT)
                if (followShopping) {
                    effectiveMode = MODE_IN_SHOP
                }
            }

            if (effectiveMode != MODE_IN_SHOP) {
                // use the pick-items-specific value, if there is one
                try {
                    sortOrder = Integer.parseInt(PreferenceManager
                            .getDefaultSharedPreferences(context).getString(
                                    PREFS_PICKITEMS_SORTORDER,
                                    PREFS_PICKITEMS_SORTORDER_DEFAULT))
                } catch (e: NumberFormatException) {
                    // Guess somebody messed with the preferences and put a string
                    // into
                    // this field. We'll follow shopping mode then.
                    effectiveMode = MODE_IN_SHOP
                }
            }

            if (effectiveMode == MODE_IN_SHOP) {

                var set = false
                if (getUsingPerListSortFromPrefs(context)) {
                    val sortOrderStr = ShoppingUtils.getListSortOrder(context, listId)
                    if (sortOrderStr != null) {
                        try {
                            sortOrder = Integer.parseInt(sortOrderStr)
                            set = true
                        } catch (e: NumberFormatException) {
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
                                        PREFS_SORTORDER, PREFS_SORTORDER_DEFAULT))
                    } catch (e: NumberFormatException) {
                        // Guess somebody messed with the preferences and put a string
                        // into
                        // this field. We'll use the default value then.
                    }
                }
            }

            if (sortOrder >= 0 && sortOrder < Contains.SORT_ORDERS.size) {
                return sortOrder
            }

            // Value out of range - somebody messed with the preferences.
            return 0
        }

        @JvmStatic
        fun getSortOrderIndexFromPrefs(context: Context, mode: Int): Int {
            val listId = ShoppingUtils.getDefaultList(context)
            return getSortOrderIndexFromPrefs(context, mode, listId)
        }

        @JvmStatic
        fun getSortOrderFromPrefs(context: Context, mode: Int): String {
            val index = getSortOrderIndexFromPrefs(context, mode)
            return Contains.SORT_ORDERS[index]
        }

        @JvmStatic
        fun getSortOrderFromPrefs(context: Context, mode: Int, listId: Long): String {
            val index = getSortOrderIndexFromPrefs(context, mode, listId)
            return Contains.SORT_ORDERS[index]
        }

        @JvmStatic
        fun prefsStatusAffectsSort(context: Context, mode: Int): Boolean {
            val index = getSortOrderIndexFromPrefs(context, mode)
            var affects = Contains.StatusAffectsSortOrder[index]
            if (mode == MODE_IN_SHOP && !affects) {
                // in shopping mode we should also invalidate display when
                // marking items if we are hiding checked items.
                affects = getHideCheckedItemsFromPrefs(context)
            }
            return affects
        }

        @JvmStatic
        fun getShoppingListSortOrderFromPrefs(context: Context): String {
            val index = Integer.parseInt(PreferenceManager
                    .getDefaultSharedPreferences(context).getString(
                            PREFS_SORTORDER_SHOPPINGLISTS,
                            PREFS_SORTORDER_SHOPPINGLISTS_DEFAULT))
            if (index >= 0 && index < Lists.SORT_ORDERS.size) {
                return Lists.SORT_ORDERS[index]
            }

            return Lists.DEFAULT_SORT_ORDER
        }

        @JvmStatic
        fun getHideCheckedItemsFromPrefs(context: Context): Boolean {
            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(context)
            return prefs.getBoolean(PREFS_HIDECHECKED, PREFS_HIDECHECKED_DEFAULT)
        }

        @JvmStatic
        fun getFastScrollEnabledFromPrefs(context: Context): Boolean {
            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(context)
            return prefs.getBoolean(PREFS_FASTSCROLL, PREFS_FASTSCROLL_DEFAULT)
        }

        private fun getSubtotalByPriorityThreshold(prefs: SharedPreferences): Int {
            val pref = prefs.getString(PREFS_PRIOSUBTOTAL,
                    PREFS_PRIOSUBTOTAL_DEFAULT)
            var threshold = 0
            try {
                threshold = Integer.parseInt(pref)
            } catch (e: NumberFormatException) {
                // Guess somebody messed with the preferences and put a string into
                // this
                // field. We'll use the default value then.
            }
            return threshold
        }

        @JvmStatic
        fun getSubtotalByPriorityThreshold(context: Context): Int {
            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(context)
            return getSubtotalByPriorityThreshold(prefs)
        }

        @JvmStatic
        fun prioritySubtotalIncludesChecked(context: Context): Boolean {
            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(context)
            return prefs.getBoolean(PREFS_PRIOSUBINCLCHECKED,
                    PREFS_PRIOSUBINCLCHECKED_DEFAULT)
        }

        /**
         * Returns a KeyListener for edit texts that will match the capitalization
         * preferences of the user.
         *
         * @ param context The context to grab the preferences from.
         */
        @JvmStatic
        fun getCapitalizationKeyListenerFromPrefs(context: Context): KeyListener {
            var capitalization = PREFS_CAPITALIZATION_DEFAULT
            try {
                capitalization = Integer.parseInt(PreferenceManager
                        .getDefaultSharedPreferences(context).getString(
                                PREFS_CAPITALIZATION,
                                Integer.toString(PREFS_CAPITALIZATION_DEFAULT)))
            } catch (e: NumberFormatException) {
                // Guess somebody messed with the preferences and put a string
                // into this
                // field. We'll use the default value then.
            }

            if (capitalization < 0
                    || capitalization > smCapitalizationSettings.size) {
                // Value out of range - somebody messed with the preferences.
                capitalization = PREFS_CAPITALIZATION_DEFAULT
            }

            return TextKeyListener(smCapitalizationSettings[capitalization], true)
        }

        /**
         * Returns InputType for the search bar based on the capitalization
         * preferences of the user.
         *
         * @ param context The context to grab the preferences from.
         */
        @JvmStatic
        fun getSearchInputTypeFromPrefs(context: Context): Int {
            var capitalization = PREFS_CAPITALIZATION_DEFAULT
            try {
                capitalization = Integer.parseInt(PreferenceManager
                        .getDefaultSharedPreferences(context).getString(
                                PREFS_CAPITALIZATION,
                                Integer.toString(PREFS_CAPITALIZATION_DEFAULT)))
            } catch (e: NumberFormatException) {
                // Guess somebody messed with the preferences and put a string
                // into this
                // field. We'll use the default value then.
            }

            if (capitalization < 0
                    || capitalization > smCapitalizationSettings.size) {
                // Value out of range - somebody messed with the preferences.
                capitalization = PREFS_CAPITALIZATION_DEFAULT
            }

            return smCapitalizationInputTypes[capitalization]
        }

        @JvmStatic
        fun getThemeSetForAll(context: Context): Boolean {
            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(context)
            return prefs.getBoolean(PREFS_THEME_SET_FOR_ALL, false)
        }

        @JvmStatic
        fun getResetQuantity(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(PREFS_RESETQUANTITY, PREFS_RESETQUANTITY_DEFAULT)
        }

        @JvmStatic
        fun getAddForBarcode(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(PREFS_ADDFORBARCODE, PREFS_ADDFORBARCODE_DEFAULT)
        }

        @JvmStatic
        fun setThemeSetForAll(context: Context, setForAll: Boolean) {
            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(context)
            val ed = prefs.edit()
            ed.putBoolean(PREFS_THEME_SET_FOR_ALL, setForAll)
            ed.commit()
        }

        @JvmStatic
        fun setUsingHoloSearch(context: Context, useHoloSearch: Boolean) {
            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(context)
            val ed = prefs.edit()
            ed.putBoolean(PREFS_HOLO_SEARCH, useHoloSearch)
            ed.apply()
        }

        @JvmStatic
        fun getShowLayoutChoice(context: Context): Boolean {
            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(context)
            return prefs.getBoolean(PREFS_SHOW_LAYOUT_CHOICE, true)
        }

        @JvmStatic
        fun setShowLayoutChoice(context: Context, showLayoutChoice: Boolean) {
            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(context)

            prefs.edit()
                    .putBoolean(PREFS_SHOW_LAYOUT_CHOICE, showLayoutChoice)
                    .apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences)

        // Set enabled state of Market preference
        var sp = findPreference(PREFS_MARKET_EXTENSIONS) as PreferenceScreen
        sp.isEnabled = isMarketAvailable()
        sp = findPreference(PREFS_MARKET_THEMES) as PreferenceScreen
        sp.isEnabled = isMarketAvailable()

        mPrioSubtotal = findPreference(PREFS_PRIOSUBTOTAL) as ListPreference
        mPickItemsSort = findPreference(PREFS_PICKITEMS_SORTORDER) as ListPreference

        mIncludesChecked = findPreference(PREFS_PRIOSUBINCLCHECKED) as CheckBoxPreference

        val shared = preferenceScreen.sharedPreferences
        updatePrioSubtotalSummary(shared)
        updatePickItemsSortPref(shared)
        resetAllSettings(shared)
    }

    override fun onResume() {
        super.onResume()

        if (intent != null && intent.hasExtra(EXTRA_SHOW_GET_ADD_ONS)) {
            // Open License section directly:
            val licensePrefScreen = preferenceScreen
                    .findPreference(PREFS_SCREEN_ADDONS) as PreferenceScreen
            preferenceScreen = licensePrefScreen
        }
        preferenceScreen.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this)
        mFilterCompletionChanged = false
    }

    override fun onPause() {
        if (mBackupManagerAvailable) {
            BackupManagerWrapper(this).dataChanged()
        }
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        updateCount++
        if (key == PREFS_PRIOSUBTOTAL) {
            updatePrioSubtotalSummary(prefs!!)
        }
        if (key == PREFS_SAMESORTFORPICK) {
            updatePickItemsSortPref(prefs!!)
        }
        if (key == PREFS_CURRENT_LIST_COMPLETE) {
            mFilterCompletionChanged = true
        }
    }

    private fun resetAllSettings(prefs: SharedPreferences) {
        val resetAllSettings = findPreference(PREFS_RESET_ALL_SETTINGS)
        resetAllSettings.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
            val alert = AlertDialog.Builder(this@PreferenceActivity).create()
            alert.setTitle(R.string.preference_reset_all_settings)
            alert.setMessage(getString(R.string.preference_reset_all_settings_alert))
            alert.setButton(getString(android.R.string.yes),
                    DialogInterface.OnClickListener { _, _ ->
                        val editor = prefs.edit()
                        // Main
                        editor.putString(PREFS_FONTSIZE, PREFS_FONTSIZE_DEFAULT)
                        editor.putString(PREFS_SORTORDER, PREFS_SORTORDER_DEFAULT)
                        // Main advanced
                        editor.putString(PREFS_CAPITALIZATION,
                                PREFS_CAPITALIZATION_DEFAULT.toString())
                        editor.putString(PREFS_ORIENTATION, PREFS_ORIENTATION_DEFAULT)
                        editor.putBoolean(PREFS_HIDECHECKED, PREFS_HIDECHECKED_DEFAULT)
                        editor.putBoolean(PREFS_FASTSCROLL, PREFS_FASTSCROLL_DEFAULT)
                        editor.putBoolean(PREFS_SHAKE, PREFS_SHAKE_DEFAULT)
                        editor.putBoolean(PREFS_PERSTOREPRICES, PREFS_PERSTOREPRICES_DEFAULT)
                        editor.putBoolean(PREFS_ADDFORBARCODE, PREFS_ADDFORBARCODE_DEFAULT)
                        editor.putBoolean(PREFS_SCREENLOCK, PREFS_SCREENLOCK_DEFAULT)
                        editor.putBoolean(PREFS_QUICKEDITMODE, PREFS_QUICKEDITMODE_DEFAULT)
                        editor.putBoolean(PREFS_USE_FILTERS, PREFS_USE_FILTERS_DEFAULT)
                        editor.putBoolean(PREFS_RESETQUANTITY, PREFS_RESETQUANTITY_DEFAULT)
                        editor.putBoolean(PREFS_HOLO_SEARCH, PREFS_HOLO_SEARCH_DEFAULT)
                        // Appearance
                        editor.putBoolean(PREFS_SHOW_PRICE, PREFS_SHOW_PRICE_DEFAULT)
                        editor.putBoolean(PREFS_SHOW_TAGS, PREFS_SHOW_TAGS_DEFAULT)
                        editor.putBoolean(PREFS_SHOW_UNITS, PREFS_SHOW_UNITS_DEFAULT)
                        editor.putBoolean(PREFS_SHOW_QUANTITY, PREFS_SHOW_QUANTITY_DEFAULT)
                        editor.putBoolean(PREFS_SHOW_PRIORITY, PREFS_SHOW_PRIORITY_DEFAULT)
                        // Pick items
                        editor.putBoolean(PREFS_SAMESORTFORPICK, PREFS_SAMESORTFORPICK_DEFAULT)
                        editor.putString(PREFS_PICKITEMS_SORTORDER,
                                PREFS_PICKITEMS_SORTORDER_DEFAULT)
                        editor.putBoolean(PREFS_PICKITEMSINLIST, PREFS_PICKITEMSINLIST_DEFAULT)
                        editor.putString(PREFS_SORTORDER_SHOPPINGLISTS,
                                PREFS_SORTORDER_SHOPPINGLISTS_DEFAULT)
                        // Subtotal
                        editor.putString(PREFS_PRIOSUBTOTAL, PREFS_PRIOSUBTOTAL_DEFAULT)
                        editor.putBoolean(PREFS_PRIOSUBINCLCHECKED,
                                PREFS_PRIOSUBINCLCHECKED_DEFAULT)

                        editor.putBoolean(PREFS_CURRENT_LIST_COMPLETE,
                                PREFS_CURRENT_LIST_COMPLETE_DEFAULT)
                        editor.putBoolean(PREFS_SORT_PER_LIST, PREFS_SORT_PER_LIST_DEFAULT)

                        editor.commit()

                        Toast.makeText(
                                this@PreferenceActivity,
                                R.string.preference_reset_all_settings_done,
                                Toast.LENGTH_LONG).show()
                        finish()
                    }
            )
            alert.setButton2(getString(android.R.string.cancel),
                    DialogInterface.OnClickListener { dialog, _ ->
                        dialog.dismiss()
                    }
            )
            alert.show()
            false
        }
    }

    private fun updatePrioSubtotalSummary(prefs: SharedPreferences) {
        val threshold = getSubtotalByPriorityThreshold(prefs)
        val labels = mPrioSubtotal!!.entries
        mPrioSubtotal!!.summary = labels[threshold]
        mIncludesChecked!!.isEnabled = threshold != 0
    }

    private fun updatePickItemsSortPref(prefs: SharedPreferences) {
        val sameSort = prefs.getBoolean(PREFS_SAMESORTFORPICK,
                PREFS_SAMESORTFORPICK_DEFAULT)
        mPickItemsSort!!.isEnabled = !sameSort
        // maybe we should set the label to say the active sort order.
        // but not tonight.
        // CharSequence labels[] = mPickItemsSort.getEntries();
    }

    /**
     * Check whether Market is available.
     *
     * @return true if Market is available
     */
    private fun isMarketAvailable(): Boolean {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(getString(R.string.preference_market_extensions_link))
        return IntentUtils.isIntentAvailable(this, i)
    }
}
