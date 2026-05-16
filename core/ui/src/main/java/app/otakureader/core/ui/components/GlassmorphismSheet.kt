package app.otakureader.core.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun GlassmorphismSheet(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val pulseAlpha by rememberInfiniteTransition(label = "sheetPulse").animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sheetPulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF16161F).copy(alpha = 0.85f),
                shape = shape
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF9B59B6).copy(alpha = pulseAlpha),
                        Color(0xFF00D2D3).copy(alpha = pulseAlpha * 0.5f),
                        Color(0xFF9B59B6).copy(alpha = pulseAlpha * 0.3f)
                    )
                ),
                shape = shape
            )
            .padding(20.dp),
        content = content
    )
}
