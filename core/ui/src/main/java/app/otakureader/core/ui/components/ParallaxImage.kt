package app.otakureader.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.otakureader.core.ui.theme.ContentType
import coil3.compose.AsyncImage

@Composable
fun ParallaxImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentType: ContentType = ContentType.MANGA
) {
    val scrollOffset by remember { mutableFloatStateOf(0f) }

    val parallaxX = scrollOffset * 0.1f
    val parallaxY = scrollOffset * 0.15f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = parallaxX
                    translationY = parallaxY
                    scaleX = 1.15f
                    scaleY = 1.15f
                },
            contentScale = ContentScale.Crop
        )

        // Content type specific overlay
        when (contentType) {
            ContentType.MANHWA -> {
                // Glow overlay for manhwa
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF9B59B6).copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                center = Offset(0.5f, 0.3f),
                                radius = 0.8f
                            )
                        )
                )
            }
            else -> { /* No overlay for manga */ }
        }
    }
}

@Preview
@Composable
private fun ParallaxImagePreview() {
    ParallaxImage(
        model = null,
        contentDescription = "Cover",
        modifier = Modifier.size(200.dp),
        contentType = ContentType.MANHWA
    )
}
