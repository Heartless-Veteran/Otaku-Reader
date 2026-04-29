package app.otakureader.core.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Remember a [ColorScheme] extracted from a manga cover [imageUrl].
 * Re-computes when [imageUrl] changes.
 *
 * @param imageUrl the cover image URL / file path
 * @param darkTheme whether to generate a dark or light scheme
 * @param enabled if false, returns null immediately (no extraction)
 * @return the extracted color scheme, or null if disabled / failed
 */
@Composable
fun rememberCoverColorScheme(
    imageUrl: String?,
    darkTheme: Boolean,
    enabled: Boolean = true
): ColorScheme? {
    val context = LocalContext.current
    var colorScheme by remember(imageUrl, darkTheme, enabled) {
        mutableStateOf<ColorScheme?>(null)
    }

    LaunchedEffect(imageUrl, darkTheme, enabled) {
        if (!enabled || imageUrl.isNullOrBlank()) {
            colorScheme = null
            return@LaunchedEffect
        }
        colorScheme = extractColorSchemeFromUrl(context, imageUrl, darkTheme)
    }

    return colorScheme
}
