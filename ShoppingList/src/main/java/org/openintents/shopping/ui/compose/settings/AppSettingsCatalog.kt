package org.openintents.shopping.ui.compose.settings

import org.openintents.shopping.R

/** A boolean (switch) setting. */
data class BoolSetting(val key: String, val titleRes: Int, val default: Boolean)

/** A single-choice (dropdown) setting, with entry labels + values from string arrays. */
data class ChoiceSetting(
    val key: String,
    val titleRes: Int,
    val default: String,
    val entriesRes: Int,
    val valuesRes: Int,
)

/**
 * The app settings, mirroring res/xml/preferences.xml exactly (keys + defaults),
 * so writing them is equivalent to the legacy PreferenceActivity.
 */
object AppSettingsCatalog {

    val choices = listOf(
        ChoiceSetting(
            "fontsize", R.string.preference_fontsize, "2",
            R.array.preference_fontsize_entries, R.array.preference_fontsize_entryvalues
        ),
        ChoiceSetting(
            "sortorder", R.string.preference_sortorder_title, "3",
            R.array.preference_sortorder_entries, R.array.preference_sortorder_entryvalues
        ),
        ChoiceSetting(
            "capitalization", R.string.preference_capitalization_title, "1",
            R.array.preference_capitalization_entries, R.array.preference_capitalization_entryvalues
        ),
        ChoiceSetting(
            "orientation", R.string.preference_orientation, "-1",
            R.array.preference_orientation_entries, R.array.preference_orientation_entryvalues
        ),
    )

    val toggles = listOf(
        BoolSetting("hidechecked", R.string.preference_hidechecked_title, false),
        BoolSetting("fastscroll", R.string.preference_fastscroll_title, false),
        BoolSetting("loadlastused", R.string.preference_loadlastused_title, true),
        BoolSetting("shake", R.string.preference_shake_title, false),
        BoolSetting("perstoreprices", R.string.preference_perstoreprice_title, false),
        BoolSetting("addforbarcode", R.string.preference_add_for_barcode, false),
        BoolSetting("screenlock", R.string.preference_screenlock_title, false),
        BoolSetting("quickedit", R.string.preference_quickedit, false),
        BoolSetting("use_filters", R.string.preference_usefilters_title, false),
        BoolSetting("holosearch", R.string.preference_holo_search_title, true),
        BoolSetting("resetquantity", R.string.preference_reset_quantity, false),
        BoolSetting("autocomplete_only_this_list", R.string.preference_complete_by_list_title, false),
        BoolSetting("showprice", R.string.preference_showprice_title, true),
    )
}
