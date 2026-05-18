package app.otakureader.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

fun calculateContrast(background: Color, foreground: Color): Double {
    val bgLum = background.luminance() + 0.05
    val fgLum = foreground.luminance() + 0.05
    return max(bgLum, fgLum) / min(bgLum, fgLum).toDouble()
}

fun ensureContrast(color: Color, background: Color, minRatio: Double = 4.5): Color {
    val currentRatio = calculateContrast(background, color)
    if (currentRatio >= minRatio) return color

    val bgLum = background.luminance()
    val targetLum = if (bgLum > 0.5) {
        (bgLum + 0.05) / minRatio - 0.05
    } else {
        (bgLum + 0.05) * minRatio - 0.05
    }

    val clampedTarget = targetLum.coerceIn(0.0, 1.0).toFloat()
    val currentLum = color.luminance()
    val scale = if (currentLum > 0) clampedTarget / currentLum else 1f

    return color.copy(
        red = (color.red * scale).coerceIn(0f, 1f),
        green = (color.green * scale).coerceIn(0f, 1f),
        blue = (color.blue * scale).coerceIn(0f, 1f)
    )
}

fun Color.contrastingTextColor(darkColor: Color = Color.Black, lightColor: Color = Color.White): Color {
    return if (this.luminance() > 0.5f) darkColor else lightColor
}
