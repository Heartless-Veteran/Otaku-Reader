package app.otakureader.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun NeonSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "neonSlider"
    )

    var sliderWidth by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }

    val sparkleRandom = remember { Random(42) }
    val sparkles = remember {
        List(8) {
            Sparkle(
                offset = sparkleRandom.nextFloat(),
                size = 2f + sparkleRandom.nextFloat() * 3f,
                phase = sparkleRandom.nextFloat() * 1000
            )
        }
    }

    val pulseAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.7f,
        animationSpec = tween(200),
        label = "neonPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val newValue = (change.position.x / sliderWidth).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                )
            }
            .onSizeChanged { sliderWidth = it.width.toFloat() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = 6.dp.toPx()
            val trackY = size.height / 2f
            val filledWidth = animatedValue * size.width

            // Background track with subtle glow
            drawRoundRect(
                color = Color(0xFF1E1E2A),
                topLeft = Offset(0f, trackY - trackHeight / 2),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2)
            )

            // Neon fill with gradient
            val neonBrush = Brush.horizontalGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.9f * pulseAlpha),
                    glowColor.copy(alpha = pulseAlpha),
                    Color(0xFF00D2D3).copy(alpha = pulseAlpha * 0.8f)
                ),
                startX = 0f,
                endX = filledWidth.coerceAtLeast(1f)
            )

            drawRoundRect(
                brush = neonBrush,
                topLeft = Offset(0f, trackY - trackHeight / 2),
                size = Size(filledWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2)
            )

            // Glow line on top of fill
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.6f * pulseAlpha),
                        Color(0xFFFFFFFF).copy(alpha = 0.8f * pulseAlpha),
                        glowColor.copy(alpha = 0.6f * pulseAlpha)
                    )
                ),
                start = Offset(0f, trackY - trackHeight / 2 + 1),
                end = Offset(filledWidth, trackY - trackHeight / 2 + 1),
                strokeWidth = 1.5f
            )

            // Particle sparkles near thumb
            if (isDragging || animatedValue > 0.05f) {
                sparkles.forEach { sparkle ->
                    val sparkleAlpha = ((kotlin.math.sin(
                        (System.currentTimeMillis() + sparkle.phase) * 0.005
                    ) + 1) / 2).toFloat() * 0.8f * pulseAlpha

                    val sparkleX = (filledWidth - 20.dp.toPx() + sparkle.offset * 40.dp.toPx())
                        .coerceIn(0f, size.width)
                    val sparkleY = trackY - 10.dp.toPx() + sparkle.offset * 20.dp.toPx()

                    drawCircle(
                        color = Color(0xFF00D2D3).copy(alpha = sparkleAlpha),
                        radius = sparkle.size,
                        center = Offset(sparkleX, sparkleY),
                        shadow = Shadow(
                            color = glowColor.copy(alpha = sparkleAlpha * 0.5f),
                            blurRadius = sparkle.size * 2
                        )
                    )
                }
            }

            // Neon thumb
            val thumbRadius = 12.dp.toPx()
            drawCircle(
                color = Color(0xFF16161F),
                radius = thumbRadius,
                center = Offset(filledWidth, trackY)
            )
            drawCircle(
                color = glowColor,
                radius = thumbRadius - 2.dp.toPx(),
                center = Offset(filledWidth, trackY),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 4.dp.toPx(),
                center = Offset(filledWidth, trackY),
                shadow = Shadow(
                    color = glowColor.copy(alpha = 0.8f),
                    blurRadius = 8f
                )
            )
        }
    }
}

private data class Sparkle(
    val offset: Float,
    val size: Float,
    val phase: Long
)

