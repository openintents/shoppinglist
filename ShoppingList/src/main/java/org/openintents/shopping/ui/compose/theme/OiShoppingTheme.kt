package org.openintents.shopping.ui.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The app's Material 3 color scheme, matching the legacy OI look: a near-black
 * background with a teal accent (instead of the Material 3 default purple).
 */
private val OiDarkColors = darkColorScheme(
    primary = Color(0xFF80CBC4),            // light teal — accent on dark surfaces
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFFA7F2E9),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF2A4641),
    onSecondaryContainer = Color(0xFFA7F2E9),
    tertiary = Color(0xFF4DD0E1),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE4E4E4),
    surface = Color(0xFF161616),            // app bar / sheets
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFC4C7C5),
    outline = Color(0xFF8A8F8D),
)

@Composable
fun OiShoppingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OiDarkColors,
        content = content,
    )
}
