package app.otakureader.core.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

fun Modifier.inkReveal(
    progress: Float,
    shape: Shape = GenericShape { size, _ ->
        addPath(inkBlobPath(progress.coerceIn(0f, 1f), size))
    }
): Modifier = composed {
    clip(shape)
}

@Preview
@Composable
private fun InkRevealPreview() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(200.dp)
            .inkReveal(0.8f)
    ) {
        androidx.compose.foundation.background(androidx.compose.ui.graphics.Color.Red)
    }
}
