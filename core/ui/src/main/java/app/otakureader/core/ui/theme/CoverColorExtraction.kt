package app.otakureader.core.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts a Material 3 [ColorScheme] from a manga cover image [Bitmap].
 * Uses AndroidX Palette to sample dominant colors and maps them to
 * primary / secondary / tertiary / surface roles.
 *
 * @param bitmap the cover art bitmap to extract colors from
 * @param darkTheme whether to generate a dark or light scheme
 * @return a [ColorScheme] derived from the cover art, or null if extraction fails
 */
fun extractColorSchemeFromBitmap(bitmap: Bitmap, darkTheme: Boolean): ColorScheme? {
    val palette = Palette.from(bitmap).generate()

    val dominant = palette.getDominantColor(0)
    val vibrant = palette.getVibrantColor(dominant)
    val muted = palette.getMutedColor(dominant)
    val darkVibrant = palette.getDarkVibrantColor(dominant)
    val lightVibrant = palette.getLightVibrantColor(dominant)

    if (dominant == 0) return null

    val primary = Color(vibrant)
    val onPrimary = if (isColorDark(vibrant)) Color.White else Color.Black
    val secondary = Color(muted)
    val onSecondary = if (isColorDark(muted)) Color.White else Color.Black
    val tertiary = Color(darkVibrant)
    val onTertiary = if (isColorDark(darkVibrant)) Color.White else Color.Black
    val primaryContainer = Color(lightVibrant)
    val onPrimaryContainer = if (isColorDark(lightVibrant)) Color.White else Color.Black

    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            tertiary = tertiary,
            onTertiary = onTertiary,
            background = Color(0xFF1A1A1A),
            surface = Color(0xFF121212),
            surfaceVariant = Color(0xFF2D2D2D),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            tertiary = tertiary,
            onTertiary = onTertiary,
            background = Color(0xFFFDFDFD),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE8E8E8),
        )
    }
}

/**
 * Asynchronously loads a cover image from [imageUrl] and extracts a [ColorScheme].
 * Safe to call from composables via [rememberCoverColorScheme].
 *
 * @param context Android context for Coil
 * @param imageUrl URL or file path to the cover image
 * @param darkTheme whether to generate a dark or light scheme
 * @return extracted [ColorScheme] or null on failure
 */
suspend fun extractColorSchemeFromUrl(
    context: Context,
    imageUrl: String,
    darkTheme: Boolean
): ColorScheme? = withContext(Dispatchers.IO) {
    try {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .size(256, 256)
            .build()

        val result = ImageLoader(context).execute(request)
        if (result is SuccessResult) {
            val drawable = result.image.toDrawable(context.resources)
            if (drawable is BitmapDrawable) {
                extractColorSchemeFromBitmap(drawable.bitmap, darkTheme)
            } else null
        } else null
    } catch (_: Exception) {
        null
    }
}

/** Returns true if the ARGB color is perceptually dark (brightness < 128). */
private fun isColorDark(argb: Int): Boolean {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return (r * 0.299 + g * 0.587 + b * 0.114) < 128
}
