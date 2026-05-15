package app.otakureader.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val pulseAlpha by rememberInfiniteTransition(label = "glassPulse").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glassPulse"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = 16f
            }
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0xFF9B59B6).copy(alpha = 0.2f),
                spotColor = Color(0xFF00D2D3).copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16161F).copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.03f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF9B59B6).copy(alpha = pulseAlpha),
                            Color(0xFF00D2D3).copy(alpha = pulseAlpha * 0.6f),
                            Color(0xFF9B59B6).copy(alpha = pulseAlpha * 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = content
            )
        }
    }
}

@Preview
@Composable
private fun GlassCardPreview() {
    GlassCard {
        androidx.compose.material3.Text(
            "Glass Card Content",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}
