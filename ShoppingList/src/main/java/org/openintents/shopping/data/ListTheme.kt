package org.openintents.shopping.data

/**
 * The built-in list themes. The values mirror the legacy styles in
 * res/values/themes.xml (Theme.ShoppingList, .Classic, .Android) attribute for
 * attribute, so a list looks the same in both UIs.
 *
 * Colors are ARGB. [fontAsset] is a font under assets/ (null = default font).
 * [paperBackground] draws the Classic notepad paper (res/drawable/shoppinglist01d).
 * Text sizes (sp) are indexed by the "fontsize" setting: 0 tiny, 1 small,
 * 2 medium (default), 3 large.
 *
 * The selected theme is stored per list in Lists.SKIN_BACKGROUND as
 * [storedValue] ("1"/"2"/"3"), the legacy encoding; [fromName] also accepts the
 * legacy style resource names.
 */
enum class ListTheme(
    val storedValue: String,
    val backgroundArgb: Long,
    val paperBackground: Boolean,
    val textSizesSp: List<Float>,
    val textArgb: Long,
    val priceArgb: Long,
    val priorityArgb: Long,
    val checkedTextArgb: Long,
    val fontAsset: String?,
    /** The font only has upper-case glyphs: show text in upper case. */
    val upperCase: Boolean,
    val strikethroughChecked: Boolean,
    /** Append the "... OK" suffix to checked items. */
    val checkedSuffix: Boolean,
    val showCheckBox: Boolean,
    val showDivider: Boolean,
) {
    DEFAULT(
        "1", 0xFF121212, false, sizes(18f, 23f, 28f),
        0xFFFFFFFF, 0xFFCCCCCC, 0xFFCCCCCC, 0xFFCCCCCC,
        null, false, false, false, true, true
    ),
    CLASSIC(
        "2", 0xFFF5ECD9, true, sizes(15f, 20f, 25f),
        0xFF000000, 0xFF444444, 0xFFAA8844, 0xFF008800,
        "fonts/AnkeHand.ttf", false, true, false, false, false
    ),
    ANDROID(
        "3", 0xFF1A1A1A, false, sizes(21f, 26f, 31f),
        0xFFFFFF66, 0xFFCCCCCC, 0xFFEECC88, 0xFF66FF66,
        "fonts/Crysta.ttf", true, false, true, false, true
    );

    /** Text size for the "fontsize" setting (0..3; out of range = medium). */
    fun textSizeSp(fontSize: Int): Float = textSizesSp.getOrElse(fontSize) { textSizesSp[2] }

    companion object {
        fun fromName(name: String?): ListTheme {
            // Legacy style names look like "org.openintents.shopping:style/Theme.ShoppingList.Classic".
            val style = name?.replace('.', '_').orEmpty()
            return when {
                name == CLASSIC.storedValue || name == CLASSIC.name ||
                    style.endsWith(":style/Theme_ShoppingList_Classic") -> CLASSIC
                name == ANDROID.storedValue || name == ANDROID.name ||
                    style.endsWith(":style/Theme_ShoppingList_Android") -> ANDROID
                else -> DEFAULT
            }
        }
    }
}

/** Tiny is derived from small like the legacy ShoppingItemsView (12/18). */
private fun sizes(small: Float, medium: Float, large: Float) =
    listOf(small * 12f / 18f, small, medium, large)
