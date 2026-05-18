package app.otakureader.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Conditionally applies a [ColorScheme] extracted from a manga cover.
 * If [colorScheme] is null, falls back to the current app theme unchanged.
 *
 * Usage:
 * ```
 * val dynamicScheme = rememberCoverColorScheme(manga.thumbnailUrl, isSystemInDarkTheme(), autoThemeEnabled)
 * MangaDynamicTheme(dynamicScheme) {
 *     // Reader or Details content
 * }
 * ```
 *
 * @param colorScheme the extracted scheme, or null to use app default
 * @param content the UI to theme
 */
@Composable
fun MangaDynamicTheme(
    colorScheme: ColorScheme?,
    content: @Composable () -> Unit
) {
    if (colorScheme != null) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            content = content
        )
    } else {
        content()
    }
}
