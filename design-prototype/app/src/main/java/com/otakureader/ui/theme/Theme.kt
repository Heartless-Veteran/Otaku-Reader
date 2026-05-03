package com.otakureader.ui.theme

import android.os.Build
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
import androidx.compose.ui.platform.LocalContext

enum class AppTheme { DARK, OLED, LIGHT, SEPIA }

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

fun darkOtakuColors(accent: Color = AccentPurple) = OtakuColors(
    bg = DarkBg, surface1 = DarkSurface1, surface2 = DarkSurface2, surface3 = DarkSurface3,
    border = DarkBorder, borderStrong = DarkBorderStr,
    fg = DarkFg, fgMuted = DarkFgMuted, fgDim = DarkFgDim,
    accent = accent, accentSoft = AccentSoft, accentDim = AccentDim, accentGlow = AccentGlow,
    success = SuccessGreen, warning = WarningYellow, danger = DangerRed, isDark = true,
)

fun oledOtakuColors(accent: Color = AccentPurple) = OtakuColors(
    bg = OledBg, surface1 = OledSurface1, surface2 = OledSurface2, surface3 = OledSurface3,
    border = OledBorder, borderStrong = OledBorderStr,
    fg = DarkFg, fgMuted = DarkFgMuted, fgDim = DarkFgDim,
    accent = accent, accentSoft = AccentSoft, accentDim = AccentDim, accentGlow = AccentGlow,
    success = SuccessGreen, warning = WarningYellow, danger = DangerRed, isDark = true,
)

fun lightOtakuColors(accent: Color = AccentPurple) = OtakuColors(
    bg = LightBg, surface1 = LightSurface1, surface2 = LightSurface2, surface3 = LightSurface3,
    border = LightBorder, borderStrong = LightBorderStr,
    fg = LightFg, fgMuted = LightFgMuted, fgDim = LightFgDim,
    accent = accent, accentSoft = accent, accentDim = accent.copy(alpha = 0.16f), accentGlow = accent.copy(alpha = 0.45f),
    success = SuccessGreen, warning = WarningYellow, danger = DangerRed, isDark = false,
)

fun sepiaOtakuColors(accent: Color = AccentPurple) = OtakuColors(
    bg = SepiaBg, surface1 = SepiaSurface1, surface2 = SepiaSurface2, surface3 = SepiaSurface3,
    border = SepiaBorder, borderStrong = SepiaBorderStr,
    fg = SepiaFg, fgMuted = SepiaFgMuted, fgDim = SepiaFgDim,
    accent = accent, accentSoft = accent, accentDim = accent.copy(alpha = 0.16f), accentGlow = accent.copy(alpha = 0.45f),
    success = SuccessGreen, warning = WarningYellow, danger = DangerRed, isDark = false,
)

private val DarkMaterialScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    primaryContainer = AccentDim,
    onPrimaryContainer = AccentSoft,
    secondary = AccentSoft,
    background = DarkBg,
    surface = DarkSurface1,
    surfaceVariant = DarkSurface2,
    onBackground = DarkFg,
    onSurface = DarkFg,
    onSurfaceVariant = DarkFgMuted,
    outline = DarkBorderStr,
)

private val LightMaterialScheme = lightColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    background = LightBg,
    surface = LightSurface1,
    surfaceVariant = LightSurface2,
    onBackground = LightFg,
    onSurface = LightFg,
    onSurfaceVariant = LightFgMuted,
    outline = LightBorderStr,
)

@Composable
fun OtakuReaderTheme(
    theme: AppTheme = AppTheme.DARK,
    accent: Color = AccentPurple,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val otakuColors = when (theme) {
        AppTheme.DARK -> darkOtakuColors(accent)
        AppTheme.OLED -> oledOtakuColors(accent)
        AppTheme.LIGHT -> lightOtakuColors(accent)
        AppTheme.SEPIA -> sepiaOtakuColors(accent)
    }

    val materialColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (theme == AppTheme.LIGHT || theme == AppTheme.SEPIA) {
                dynamicLightColorScheme(context)
            } else {
                dynamicDarkColorScheme(context)
            }
        }
        theme == AppTheme.LIGHT || theme == AppTheme.SEPIA -> LightMaterialScheme
        else -> DarkMaterialScheme
    }

    CompositionLocalProvider(LocalOtakuColors provides otakuColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = OtakuTypography,
            content = content,
        )
    }
}
