package app.otakureader.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.panelBorder(
    color: Color = Color.White,
    width: Dp = 2.dp
): Modifier = composed {
    this
        .padding(width)
        .border(
            width = width,
            brush = Brush.linearGradient(
                colors = listOf(
                    color.copy(alpha = 0.9f),
                    color.copy(alpha = 0.6f),
                    color.copy(alpha = 0.8f),
                    color.copy(alpha = 0.5f),
                    color.copy(alpha = 0.9f)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            ),
            shape = RectangleShape
        )
}
