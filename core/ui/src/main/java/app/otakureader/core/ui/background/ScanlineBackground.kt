package app.otakureader.core.ui.background

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.random.Random

@Composable
fun ScanlineBackground(
    modifier: Modifier = Modifier,
    opacity: Float = 0.03f
) {
    val bitmap = remember(opacity) {
        val width = 256
        val height = 256
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            val linePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb((opacity * 255).toInt(), 0, 0, 0)
            }
            val noisePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb((opacity * 0.6f * 255).toInt(), 255, 255, 255)
            }

            // Draw horizontal scanlines
            for (y in 0 until height step 2) {
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), linePaint)
            }

            // Draw noise pixels
            val random = Random(42)
            repeat(200) {
                val x = random.nextInt(width)
                val y = random.nextInt(height)
                canvas.drawPoint(x.toFloat(), y.toFloat(), noisePaint)
            }
        }
    }

    Canvas(modifier = modifier) {
        val imageBitmap = bitmap.asImageBitmap()
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(0, 0),
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
    }
}
