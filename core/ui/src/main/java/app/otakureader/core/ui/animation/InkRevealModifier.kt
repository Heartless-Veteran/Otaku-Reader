package app.otakureader.core.ui.animation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private fun inkBlobPath(progress: Float, size: Size): Path {
    val path = Path()
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val maxRadius = sqrt(centerX * centerX + centerY * centerY) * progress * 1.5f
    val points = 12
    val random = Random(42)

    for (i in 0..points) {
        val angle = (i.toFloat() / points) * Math.PI * 2
        val noise = (random.nextFloat() * 0.3f + 0.85f)
        val radius = maxRadius * noise
        val x = centerX + cos(angle).toFloat() * radius
        val y = centerY + sin(angle).toFloat() * radius

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            val prevAngle = ((i - 1).toFloat() / points) * Math.PI * 2
            val cpx1 = centerX + cos(prevAngle + 0.3).toFloat() * radius * 1.1f
            val cpy1 = centerY + sin(prevAngle + 0.3).toFloat() * radius * 1.1f
            path.quadraticTo(cpx1, cpy1, x, y)
        }
    }
    path.close()
    return path
}

fun Modifier.inkReveal(progress: Float): Modifier = composed {
    val clampedProgress = progress.coerceIn(0f, 1f)
    clip(
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density,
            ): Outline = Outline.Generic(inkBlobPath(clampedProgress, size))
        }
    )
}
