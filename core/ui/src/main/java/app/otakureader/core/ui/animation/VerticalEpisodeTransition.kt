package app.otakureader.core.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VerticalEpisodeTransition(
    nextEpisodeTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gradient divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF9B59B6).copy(alpha = 0.6f),
                            Color(0xFF00D2D3).copy(alpha = 0.6f),
                            Color(0xFF9B59B6).copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "NEXT EPISODE",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                color = Color(0xFF00D2D3).copy(alpha = 0.7f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = nextEpisodeTitle,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Glow indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = Color(0xFF9B59B6).copy(alpha = glowAlpha),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .shadow(
                    elevation = 8.dp,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    ambientColor = Color(0xFF9B59B6),
                    spotColor = Color(0xFF9B59B6)
                )
        )
    }
}
