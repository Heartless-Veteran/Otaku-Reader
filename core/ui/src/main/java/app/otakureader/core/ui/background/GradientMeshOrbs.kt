package app.otakureader.core.ui.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Orb(
    val baseX: Float,
    val baseY: Float,
    val radius: Float,
    val color: Color,
    val phaseX: Float,
    val phaseY: Float,
    val speedX: Float,
    val speedY: Float
)

private const val FPS_CAP = 15
private const val FRAME_DURATION = 1000L / FPS_CAP

@Composable
fun GradientMeshOrbs(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")

    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val orbs = remember(colors) {
        val random = Random(123)
        List(5) { i ->
            Orb(
                baseX = random.nextFloat(),
                baseY = random.nextFloat(),
                radius = (120 + random.nextInt(100)).toFloat(),
                color = colors[i % colors.size],
                phaseX = random.nextFloat() * (Math.PI * 2).toFloat(),
                phaseY = random.nextFloat() * (Math.PI * 2).toFloat(),
                speedX = 0.3f + random.nextFloat() * 0.5f,
                speedY = 0.2f + random.nextFloat() * 0.4f
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(Color(0xFF0D0D12))

        orbs.forEach { orb ->
            val x = (orb.baseX + cos(time * orb.speedX + orb.phaseX) * 0.15f).toFloat() * w
            val y = (orb.baseY + sin(time * orb.speedY + orb.phaseY) * 0.15f).toFloat() * h

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        orb.color.copy(alpha = 0.5f),
                        orb.color.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = orb.radius * density.density
                ),
                radius = orb.radius * density.density,
                center = Offset(x, y)
            )
        }
    }
}
