package app.otakureader.core.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Suppress("CyclomaticComplexMethod")
suspend fun extractTheme(bitmap: Bitmap, contentType: ContentType): MangaCoverTheme =
    withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).generate()
        val swatches = palette.swatches

        if (swatches.isEmpty()) {
            return@withContext if (contentType == ContentType.MANGA) MangaDefaultTheme else ManhwaDefaultTheme
        }

        val dominantSwatch = palette.dominantSwatch
        val vibrantSwatch = palette.vibrantSwatch
        val mutedSwatch = palette.mutedSwatch
        val darkVibrantSwatch = palette.darkVibrantSwatch

        val dominant = dominantSwatch?.rgb?.let { Color(it) }
            ?: if (contentType == ContentType.MANGA) MangaDefaultTheme.dominant else ManhwaDefaultTheme.dominant

        val vibrantRaw = vibrantSwatch?.rgb?.let { Color(it) }
            ?: if (contentType == ContentType.MANGA) MangaDefaultTheme.vibrant else ManhwaDefaultTheme.vibrant

        val vibrant = when (contentType) {
            ContentType.MANHWA -> boostSaturation(vibrantRaw, factor = 1.3f)
            else -> vibrantRaw
        }

        val muted = mutedSwatch?.rgb?.let { Color(it) }
            ?: if (contentType == ContentType.MANGA) MangaDefaultTheme.muted else ManhwaDefaultTheme.muted

        val darkVibrant = darkVibrantSwatch?.rgb?.let { Color(it) }
            ?: dominant.copy(
                red = dominant.red * 0.4f,
                green = dominant.green * 0.4f,
                blue = dominant.blue * 0.4f
            )

        val bg = dominant
        val proposedBody = (mutedSwatch?.bodyTextColor ?: 0xFFE0E0E5.toInt()).let { Color(it) }
        val proposedTitle = (vibrantSwatch?.titleTextColor ?: 0xFFFFFFFF.toInt()).let { Color(it) }

        val bodyText = ensureContrast(proposedBody, bg, minRatio = 4.5)
        val titleText = ensureContrast(proposedTitle, bg, minRatio = 3.0)

        MangaCoverTheme(
            dominant = clampLuminance(dominant, minLum = 0.05f, maxLum = 0.25f),
            vibrant = vibrant,
            muted = muted,
            darkVibrant = clampLuminance(darkVibrant, minLum = 0.02f, maxLum = 0.12f),
            bodyText = bodyText,
            titleText = titleText,
            contentType = contentType,
            accentGlow = vibrant.copy(alpha = 0.3f)
        )
    }

private fun boostSaturation(color: Color, factor: Float): Color {
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsl)
    hsl[1] = (hsl[1] * factor).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsl))
}

private fun Color.luminanceValue(): Float {
    // Linear luminance approximation for Compose Color
    return red * 0.2126f + green * 0.7152f + blue * 0.0722f
}

private fun clampLuminance(color: Color, minLum: Float, maxLum: Float): Color {
    val lum = color.luminanceValue()
    return when {
        lum < minLum -> color.copy(
            red = (color.red + minLum).coerceAtMost(1f),
            green = (color.green + minLum).coerceAtMost(1f),
            blue = (color.blue + minLum).coerceAtMost(1f)
        )
        lum > maxLum -> color.copy(
            red = (color.red - (lum - maxLum)).coerceAtLeast(0f),
            green = (color.green - (lum - maxLum)).coerceAtLeast(0f),
            blue = (color.blue - (lum - maxLum)).coerceAtLeast(0f)
        )
        else -> color
    }
}
