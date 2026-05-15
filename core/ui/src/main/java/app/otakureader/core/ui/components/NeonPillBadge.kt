package app.otakureader.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NeonPillBadge(
    count: Int,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    val pulse by rememberInfiniteTransition(label = "neonPulse").animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val shadowColor = glowColor.copy(alpha = 0.5f * pulse)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 32.dp, minHeight = 20.dp)
            .background(
                color = glowColor,
                shape = RoundedCornerShape(10.dp)
            )
            .shadow(
                elevation = (6.dp * pulse),
                shape = RoundedCornerShape(10.dp),
                ambientColor = shadowColor,
                spotColor = shadowColor
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White,
                shadow = Shadow(
                    color = glowColor.copy(alpha = 0.8f),
                    blurRadius = 4f
                ),
                textAlign = TextAlign.Center
            )
        )
    }
}

@Preview
@Composable
private fun NeonPillBadgePreview() {
    NeonPillBadge(count = 12, glowColor = Color(0xFF9B59B6))
}
