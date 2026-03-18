package app.otakureader.core.common.util

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target

/**
 * Utility class for extracting theme colors from images.
 * Used for Auto Theme Color feature.
 */
object ThemeColorExtractor {

    /**
     * Extract a vibrant color from a bitmap for theming purposes.
     * Falls back through various targets until a suitable color is found.
     *
     * @param bitmap The image to extract color from
     * @param fallbackColor Color to use if extraction fails
     * @return The extracted color or fallback
     */
    @ColorInt
    fun extractColor(bitmap: Bitmap, @ColorInt fallbackColor: Int): Int {
        val palette = Palette.from(bitmap)
            .maximumColorCount(32)
            .addTarget(Target.VIBRANT)
            .addTarget(Target.LIGHT_VIBRANT)
            .addTarget(Target.DARK_VIBRANT)
            .addTarget(Target.MUTED)
            .addTarget(Target.LIGHT_MUTED)
            .addTarget(Target.DARK_MUTED)
            .generate()

        return palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.darkVibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.lightMutedSwatch?.rgb
            ?: palette.darkMutedSwatch?.rgb
            ?: fallbackColor
    }

    /**
     * Extract both a primary and secondary color from a bitmap.
     *
     * @param bitmap The image to extract colors from
     * @param fallbackColor Color to use if extraction fails
     * @return Pair of (primaryColor, secondaryColor)
     */
    fun extractColors(bitmap: Bitmap, @ColorInt fallbackColor: Int): Pair<Int, Int> {
        val palette = Palette.from(bitmap)
            .maximumColorCount(32)
            .generate()

        val primary = palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: fallbackColor

        val secondary = palette.darkVibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.darkMutedSwatch?.rgb
            ?: primary

        return primary to secondary
    }

    /**
     * Determine if a color is considered "light" (for choosing text color).
     */
    fun isLightColor(@ColorInt color: Int): Boolean {
        val darkness = 1 - (0.299 * android.graphics.Color.red(color) +
                0.587 * android.graphics.Color.green(color) +
                0.114 * android.graphics.Color.blue(color)) / 255
        return darkness < 0.5
    }
}
