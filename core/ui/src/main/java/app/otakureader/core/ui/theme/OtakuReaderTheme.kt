package app.otakureader.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme()
private val DarkColorScheme = darkColorScheme()

// ─── Design-system token layer ────────────────────────────────────────────────

@Immutable
data class OtakuColors(
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val border: Color,
    val borderStrong: Color,
    val fg: Color,
    val fgMuted: Color,
    val fgDim: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentDim: Color,
    val accentGlow: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val isDark: Boolean,
)

val LocalOtakuColors = staticCompositionLocalOf { darkOtakuColors() }

fun darkOtakuColors(accent: Color = Color(0xFF6B4EFF)) = OtakuColors(
    bg = Color(0xFF0B0B12),
    surface1 = Color(0xFF14141F),
    surface2 = Color(0xFF1C1C2A),
    surface3 = Color(0xFF25253A),
    border = Color(0xFF2A2A3E),
    borderStrong = Color(0xFF3A3A55),
    fg = Color(0xFFF2F2F7),
    fgMuted = Color(0xFF9D9DAE),
    fgDim = Color(0xFF65657A),
    accent = accent,
    accentSoft = Color(0xFF8F7BFF),
    accentDim = Color(0x296B4EFF),
    accentGlow = Color(0x406B4EFF),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFF87171),
    isDark = true,
)

fun oledOtakuColors(accent: Color = Color(0xFF6B4EFF)) = OtakuColors(
    bg = Color(0xFF000000),
    surface1 = Color(0xFF0A0A0F),
    surface2 = Color(0xFF15151E),
    surface3 = Color(0xFF1F1F2C),
    border = Color(0xFF1E1E2E),
    borderStrong = Color(0xFF2D2D42),
    fg = Color(0xFFF2F2F7),
    fgMuted = Color(0xFF9D9DAE),
    fgDim = Color(0xFF65657A),
    accent = accent,
    accentSoft = Color(0xFF8F7BFF),
    accentDim = Color(0x296B4EFF),
    accentGlow = Color(0x406B4EFF),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFF87171),
    isDark = true,
)

fun lightOtakuColors(accent: Color = Color(0xFF6B4EFF)) = OtakuColors(
    bg = Color(0xFFF7F7FB),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF0F0F8),
    surface3 = Color(0xFFE8E8F4),
    border = Color(0xFFE2E2EE),
    borderStrong = Color(0xFFD0D0E0),
    fg = Color(0xFF1A1A2E),
    fgMuted = Color(0xFF6B6B80),
    fgDim = Color(0xFF9999B0),
    accent = accent,
    accentSoft = Color(0xFF8F7BFF),
    accentDim = Color(0x186B4EFF),
    accentGlow = Color(0x306B4EFF),
    success = Color(0xFF16A34A),
    warning = Color(0xFFD97706),
    danger = Color(0xFFDC2626),
    isDark = false,
)

fun sepiaOtakuColors(accent: Color = Color(0xFF6B4EFF)) = OtakuColors(
    bg = Color(0xFFF4ECD8),
    surface1 = Color(0xFFFAF4E8),
    surface2 = Color(0xFFEDE3CE),
    surface3 = Color(0xFFE4D8BE),
    border = Color(0xFFD4C9AE),
    borderStrong = Color(0xFFC4B89E),
    fg = Color(0xFF2C2018),
    fgMuted = Color(0xFF6B5A4A),
    fgDim = Color(0xFF9B8A7A),
    accent = accent,
    accentSoft = Color(0xFF8F7BFF),
    accentDim = Color(0x186B4EFF),
    accentGlow = Color(0x306B4EFF),
    success = Color(0xFF16A34A),
    warning = Color(0xFFD97706),
    danger = Color(0xFFDC2626),
    isDark = false,
)

/** Color scheme ID for the user-defined custom accent color. */
const val COLOR_SCHEME_CUSTOM_ACCENT = 11

/**
 * Otaku Reader app theme.
 * Supports dynamic color (Material You) on Android 12+, custom color schemes,
 * Pure Black (AMOLED) dark mode, high-contrast mode, custom accent color,
 * and manual dark/light mode.
 *
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param colorScheme Color scheme selection (0=System Default, 1=Dynamic, 2-10=Custom schemes, [COLOR_SCHEME_CUSTOM_ACCENT]=Custom accent)
 * @param usePureBlack Whether to use Pure Black (#000000) background in dark mode (AMOLED)
 * @param useHighContrast Whether to boost contrast for improved accessibility
 * @param customAccentColor ARGB Long used when [colorScheme] == [COLOR_SCHEME_CUSTOM_ACCENT]
 */
@Composable
fun OtakuReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: Int = 0,
    usePureBlack: Boolean = false,
    useHighContrast: Boolean = false,
    customAccentColor: Long = 0xFF1976D2L,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val baseColorScheme: ColorScheme = when (colorScheme) {
        // 0 = System Default (use dynamic on Android 12+ if available, otherwise default)
        0 -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
        // 1 = Dynamic (Material You - forced, only on Android 12+)
        1 -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            // Fallback to default if dynamic not available
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
        // COLOR_SCHEME_CUSTOM_ACCENT = Custom accent color
        COLOR_SCHEME_CUSTOM_ACCENT -> {
            val accent = Color(customAccentColor.toInt())
            if (darkTheme) buildCustomDarkScheme(accent) else buildCustomLightScheme(accent)
        }
        // 2-10 = Custom color schemes
        else -> {
            ColorSchemes[colorScheme]?.let { (light, dark) ->
                if (darkTheme) dark else light
            } ?: if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }

    // Apply Pure Black background if enabled and in dark mode
    val pureBlackScheme = if (usePureBlack && darkTheme) {
        baseColorScheme.copy(
            background = PureBlack,
            surface = PureBlack,
            surfaceVariant = Color(0xFF1A1A1A),
            surfaceContainer = Color(0xFF0D0D0D),
            surfaceContainerHigh = Color(0xFF1A1A1A),
            surfaceContainerHighest = Color(0xFF262626),
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainerLowest = PureBlack,
        )
    } else {
        baseColorScheme
    }

    // Apply high-contrast adjustments for improved accessibility
    val finalColorScheme = if (useHighContrast) {
        if (darkTheme) {
            pureBlackScheme.copy(
                onBackground = Color.White,
                onSurface = Color.White,
                onSurfaceVariant = Color(0xFFE0E0E0),
                outline = Color(0xFFBDBDBD),
                outlineVariant = Color(0xFF9E9E9E),
            )
        } else {
            pureBlackScheme.copy(
                onBackground = Color.Black,
                onSurface = Color.Black,
                onSurfaceVariant = Color(0xFF212121),
                outline = Color(0xFF424242),
                outlineVariant = Color(0xFF616161),
            )
        }
    } else {
        pureBlackScheme
    }

    val otakuColors = when {
        usePureBlack && darkTheme -> oledOtakuColors(accent = finalColorScheme.primary)
        darkTheme -> darkOtakuColors(accent = finalColorScheme.primary)
        else -> lightOtakuColors(accent = finalColorScheme.primary)
    }

    CompositionLocalProvider(LocalOtakuColors provides otakuColors) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = OtakuReaderTypography,
            content = content
        )
    }
}

/**
 * Builds a light [ColorScheme] using the given [accent] as primary color.
 * Generates complementary container/surface colors for a cohesive Material 3 theme.
 */
private fun buildCustomLightScheme(accent: Color): ColorScheme {
    val argb = accent.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    // Lighter variant for containers
    val containerColor = Color(
        red = (r + (255 - r) * 0.7f).toInt().coerceIn(0, 255),
        green = (g + (255 - g) * 0.7f).toInt().coerceIn(0, 255),
        blue = (b + (255 - b) * 0.7f).toInt().coerceIn(0, 255)
    )
    // Darker variant for onPrimaryContainer
    val onContainerColor = Color(
        red = (r * 0.3f).toInt().coerceIn(0, 255),
        green = (g * 0.3f).toInt().coerceIn(0, 255),
        blue = (b * 0.3f).toInt().coerceIn(0, 255)
    )
    return lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = containerColor,
        onPrimaryContainer = onContainerColor,
        secondary = accent,
        onSecondary = Color.White,
        secondaryContainer = containerColor.copy(alpha = 0.5f),
        onSecondaryContainer = onContainerColor,
    )
}

/**
 * Builds a dark [ColorScheme] using the given [accent] as primary color.
 * Generates complementary container/surface colors for a cohesive Material 3 dark theme.
 */
private fun buildCustomDarkScheme(accent: Color): ColorScheme {
    val argb = accent.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    // Lighter tint for dark mode primary
    val lightTint = Color(
        red = (r + (255 - r) * 0.4f).toInt().coerceIn(0, 255),
        green = (g + (255 - g) * 0.4f).toInt().coerceIn(0, 255),
        blue = (b + (255 - b) * 0.4f).toInt().coerceIn(0, 255)
    )
    // Darker container color for dark mode
    val containerColor = Color(
        red = (r * 0.4f).toInt().coerceIn(0, 255),
        green = (g * 0.4f).toInt().coerceIn(0, 255),
        blue = (b * 0.4f).toInt().coerceIn(0, 255)
    )
    return darkColorScheme(
        primary = lightTint,
        onPrimary = Color(
            red = (r * 0.2f).toInt().coerceIn(0, 255),
            green = (g * 0.2f).toInt().coerceIn(0, 255),
            blue = (b * 0.2f).toInt().coerceIn(0, 255)
        ),
        primaryContainer = containerColor,
        onPrimaryContainer = lightTint,
        secondary = lightTint,
        onSecondary = Color(0xFF1A1A1A),
        secondaryContainer = containerColor.copy(alpha = 0.5f),
        onSecondaryContainer = lightTint,
    )
}
