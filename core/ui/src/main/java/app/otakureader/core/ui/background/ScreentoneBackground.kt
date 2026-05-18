package app.otakureader.core.ui.background

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

@Composable
fun ScreentoneBackground(
    modifier: Modifier = Modifier,
    tint: Color = Color.White.copy(alpha = 0.05f)
) {
    val density = LocalDensity.current
    val dotRadius = with(density) { 1.5f.dp.toPx() }
    val dotSpacing = with(density) { 8.dp.toPx() }

    val bitmap = remember(tint, dotRadius, dotSpacing) {
        val width = 256
        val height = 256
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (tint.alpha * 255).toInt(),
                    (tint.red * 255).toInt(),
                    (tint.green * 255).toInt(),
                    (tint.blue * 255).toInt()
                )
                isAntiAlias = true
            }

            val cols = ceil(width.toFloat() / dotSpacing).toInt() + 1
            val rows = ceil(height.toFloat() / dotSpacing).toInt() + 1

            for (row in 0 until rows) {
                val offsetX = if (row % 2 == 0) 0f else dotSpacing / 2
                for (col in 0 until cols) {
                    val cx = col * dotSpacing + offsetX
                    val cy = row * dotSpacing
                    canvas.drawCircle(cx, cy, dotRadius, paint)
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val imageBitmap = bitmap.asImageBitmap()
        val canvasWidth = size.width
        val canvasHeight = size.height
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize(
                canvasWidth.toInt(),
                canvasHeight.toInt()
            )
        )
    }
}
