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
 */
enum class ListTheme(
    val displayName: String,
    val backgroundArgb: Long,
    val textArgb: Long,
    val checkedTextArgb: Long,
    val fontAsset: String?,
    val strikethroughChecked: Boolean,
) {
    DEFAULT("Default", 0xFF121212, 0xFFFFFFFF, 0xFF8A8A8A, null, true),
    CLASSIC("Classic", 0xFFF5ECD9, 0xFF000000, 0xFF7A7A6A, "fonts/AnkeHand.ttf", true),
    ANDROID("Bugdroid", 0xFF1A1A1A, 0xFFFFFF66, 0xFF8A8A55, "fonts/Crysta.ttf", true);

    companion object {
        fun fromName(name: String?): ListTheme =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
