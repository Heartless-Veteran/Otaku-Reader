package app.otakureader.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun InkSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "inkSlider"
    )

    var sliderWidth by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }

    val wavePhase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "wavePhase"
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
            val trackHeight = 8.dp.toPx()
            val trackY = size.height / 2f
            val filledWidth = animatedValue * size.width

            // Background track
            drawRoundRect(
                color = Color(0xFF2A2A35),
                topLeft = Offset(0f, trackY - trackHeight / 2),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2)
            )

            // Ink fill with wave
            val path = Path().apply {
                moveTo(0f, trackY - trackHeight / 2)

                val waveCount = 3
                val waveAmplitude = if (isDragging) 6f else 2f
                val steps = 50

                for (i in 0..steps) {
                    val x = (i.toFloat() / steps) * filledWidth
                    val progress = i.toFloat() / steps
                    val wave = if (progress < 0.95f) {
                        sin(progress * waveCount * PI * 2 + wavePhase) * waveAmplitude
                    } else 0f
                    lineTo(x, trackY - trackHeight / 2 + wave.toFloat())
                }

                lineTo(filledWidth, trackY + trackHeight / 2)

                for (i in steps downTo 0) {
                    val x = (i.toFloat() / steps) * filledWidth
                    val progress = i.toFloat() / steps
                    val wave = if (progress < 0.95f) {
                        sin(progress * waveCount * PI * 2 + wavePhase + PI) * waveAmplitude
                    } else 0f
                    lineTo(x, trackY + trackHeight / 2 + wave.toFloat())
                }

                close()
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFF4757),
                        Color(0xFFFF6B81)
                    )
                )
            )

            // Thumb
            val thumbRadius = 10.dp.toPx()
            drawCircle(
                color = Color(0xFFFF4757),
                radius = thumbRadius,
                center = Offset(filledWidth, trackY),
                shadow = Shadow(
                    color = Color(0xFFFF4757).copy(alpha = 0.5f),
                    blurRadius = 12f
                )
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadius * 0.4f,
                center = Offset(filledWidth, trackY)
            )
        }
    }
}

@Preview
@Composable
private fun InkSliderPreview() {
    var value by remember { mutableFloatStateOf(0.5f) }
    InkSlider(
        value = value,
        onValueChange = { value = it },
        modifier = Modifier.padding(16.dp)
    )
}
