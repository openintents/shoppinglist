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
 * The settings the app honors, with the keys + defaults of res/xml/preferences.xml
 * (so they are shared with the legacy code). Legacy-only settings are not shown.
 */
object AppSettingsCatalog {

    val choices = listOf(
        ChoiceSetting(
            "fontsize", R.string.preference_fontsize, "2",
            R.array.preference_fontsize_entries, R.array.preference_fontsize_entryvalues
        ),
        ChoiceSetting(
            "capitalization", R.string.preference_capitalization_title, "1",
            R.array.preference_capitalization_entries, R.array.preference_capitalization_entryvalues
        ),
    )

    val toggles = listOf(
        BoolSetting("hidechecked", R.string.preference_hidechecked_title, false),
        BoolSetting("perstoreprices", R.string.preference_perstoreprice_title, false),
        BoolSetting("holosearch", R.string.preference_holo_search_title, false),
        BoolSetting("autocomplete_only_this_list", R.string.preference_complete_by_list_title, false),
        BoolSetting("showprice", R.string.preference_showprice_title, true),
    )
}
