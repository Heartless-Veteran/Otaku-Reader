package app.otakureader.core.ui.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

@Composable
private fun ManhwaSplashBackground(dominant: Color, vibrant: Color) {
    GradientMeshOrbs(
        colors = listOf(
            vibrant.copy(alpha = 0.4f),
            dominant.copy(alpha = 0.6f * 0.3f),
            vibrant.copy(alpha = 0.3f),
            Color(0xFF00D2D3).copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxSize()
    )

    ScanlineBackground(
        modifier = Modifier.fillMaxSize(),
        opacity = 0.03f
    )

    // Cover glow overlay
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    vibrant.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.3f),
                radius = size.width * 0.6f
            )
        )
    }
}
