package app.otakureader.core.ui.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun CoverSplashBackground(
    dominant: Color,
    vibrant: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MangaSplashBackground(dominant = dominant, vibrant = vibrant)
    }
}

@Composable
private fun MangaSplashBackground(dominant: Color, vibrant: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Radial gradient base
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    dominant,
                    vibrant.copy(alpha = 0.4f),
                    Color(0xFF0A0A0F)
                ),
                center = center,
                radius = size.width * 0.8f
            )
        )

        // Vignette overlay
        val vignetteBrush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.6f)
            ),
            center = center,
            radius = size.width * 0.5f
        )
        drawRect(brush = vignetteBrush)
    }

    ScreentoneBackground(
        modifier = Modifier.fillMaxSize(),
        tint = vibrant.copy(alpha = 0.05f)
    )
}
