package org.openintents.shopping.ui.compose.settings

import org.openintents.shopping.R

/** A setting on the Settings screen. */
sealed interface Setting {
    val key: String
    val titleRes: Int
}

/** A boolean (switch) setting. */
data class BoolSetting(override val key: String, override val titleRes: Int, val default: Boolean) : Setting

/** A single-choice (dropdown) setting, with entry labels + values from string arrays. */
data class ChoiceSetting(
    override val key: String,
    override val titleRes: Int,
    val default: String,
    val entriesRes: Int,
    val valuesRes: Int,
) : Setting

/** A titled group of settings. */
data class SettingsSection(val titleRes: Int, val settings: List<Setting>)

/**
 * The settings the app honors, with the keys + defaults of res/xml/preferences.xml
 * (shared with the legacy code, so earlier choices carry over).
 */
object AppSettingsCatalog {

    val sections = listOf(
        SettingsSection(
            R.string.preference_general, listOf(
                ChoiceSetting(
                    "fontsize", R.string.preference_fontsize, "2",
                    R.array.preference_fontsize_entries, R.array.preference_fontsize_entryvalues
                ),
                ChoiceSetting(
                    "capitalization", R.string.preference_capitalization_title, "1",
                    R.array.preference_capitalization_entries, R.array.preference_capitalization_entryvalues
                ),
                ChoiceSetting(
                    "orientation", R.string.preference_orientation, "-1",
                    R.array.preference_orientation_entries, R.array.preference_orientation_entryvalues
                ),
                BoolSetting("holosearch", R.string.preference_holo_search_title, false),
                BoolSetting("compact", R.string.compose_compact_view, false),
                BoolSetting("fastscroll", R.string.preference_fastscroll_title, false),
                BoolSetting("hidechecked", R.string.preference_hidechecked_title, false),
                BoolSetting("screenlock", R.string.preference_screenlock_title, false),
                BoolSetting("shake", R.string.preference_shake_title, false),
                BoolSetting("use_filters", R.string.preference_usefilters_title, false),
                BoolSetting("resetquantity", R.string.preference_reset_quantity, false),
                BoolSetting("barcode_button", R.string.compose_barcode_button, true),
                BoolSetting("barcode_lookup", R.string.compose_barcode_lookup, true),
                BoolSetting("autocomplete_only_this_list", R.string.preference_complete_by_list_title, false),
            )
        ),
        SettingsSection(
            R.string.preference_appearance, listOf(
                BoolSetting("showprice", R.string.preference_showprice_title, true),
                BoolSetting("perstoreprices", R.string.preference_perstoreprice_title, false),
                BoolSetting("showtags", R.string.preference_showtags_title, true),
                BoolSetting("showunits", R.string.preference_showunits_title, true),
                BoolSetting("showquantity", R.string.preference_showquantity_title, true),
                BoolSetting("showpriority", R.string.preference_showpriority_title, true),
            )
        ),
        SettingsSection(
            R.string.preference_sorting, listOf(
                ChoiceSetting(
                    "sortorder", R.string.preference_sortorder_title, "3",
                    R.array.preference_sortorder_entries, R.array.preference_sortorder_entryvalues
                ),
                BoolSetting("perListSort", R.string.preference_perListSort_title, false),
                BoolSetting("samesortforpick", R.string.preference_samesortforpick_title, false),
                ChoiceSetting(
                    "sortorderForPickItems", R.string.preference_sortorder_for_pick_title, "1",
                    R.array.preference_sortorder_entries, R.array.preference_sortorder_entryvalues
                ),
                ChoiceSetting(
                    "sortorderForShoppingLists", R.string.preference_sortorder_for_shopping_lists, "0",
                    R.array.preference_sortorder_slists, R.array.preference_sortorder_slistsvalues
                ),
            )
        ),
        SettingsSection(
            R.string.preference_prio_subtotal, listOf(
                ChoiceSetting(
                    "priority_subtotal_threshold", R.string.preference_prioritytotal_title, "0",
                    R.array.preference_prioritytotal_entries, R.array.preference_prioritytotal_entryvalues
                ),
                BoolSetting("priosubtotal_includes_checked", R.string.preference_priosubtotalchecked_title, true),
            )
        ),
    )

    val toggles: List<BoolSetting> = sections.flatMap { it.settings }.filterIsInstance<BoolSetting>()

    val choices: List<ChoiceSetting> = sections.flatMap { it.settings }.filterIsInstance<ChoiceSetting>()
}
