package app.otakureader.core.ui.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun ScanlineBackgroundPreview() {
    ScanlineBackground()
}

@Composable
fun ScanlineBackground(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val lineHeight = 1.dp.toPx()
        val spacing = 3.dp.toPx()
        val alpha = 0.03f

        val rows = (size.height / spacing).toInt() + 1

        for (row in 0..rows) {
            val y = row * spacing
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = lineHeight
            )
        }
    }
}
