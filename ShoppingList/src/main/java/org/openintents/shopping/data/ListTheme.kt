package org.openintents.shopping.data

/**
 * Built-in list themes, mirroring the three styles in res/values/themes.xml
 * (Default / Classic / Bugdroid). Colors are ARGB; [fontAsset] points at a
 * bundled font under assets/ (the Compose layer loads it), or null for the
 * default font. The selected theme is stored per-list in Lists.SKIN_BACKGROUND.
 *
 * Note: the legacy app can also load themes exported by *other* installed apps
 * via ThemeUtils/ThemeAttributes; that resolver would feed values into the same
 * shape as this enum. Only the built-in themes are reproduced here.
 *
 * [storedValue] is the legacy encoding ("1"/"2"/"3") so both UIs read the same
 * value; [fromName] also accepts the legacy style resource names.
 */
enum class ListTheme(
    val displayName: String,
    val backgroundArgb: Long,
    val textArgb: Long,
    val checkedTextArgb: Long,
    val fontAsset: String?,
    val strikethroughChecked: Boolean,
    val storedValue: String,
) {
    DEFAULT("Default", 0xFF121212, 0xFFFFFFFF, 0xFF8A8A8A, null, true, "1"),
    CLASSIC("Classic", 0xFFF5ECD9, 0xFF000000, 0xFF7A7A6A, "fonts/AnkeHand.ttf", true, "2"),
    ANDROID("Bugdroid", 0xFF1A1A1A, 0xFFFFFF66, 0xFF8A8A55, "fonts/Crysta.ttf", true, "3");

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
