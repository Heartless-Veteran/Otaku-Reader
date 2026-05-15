package app.otakureader.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@Composable
fun ManhwaCard(
    coverUrl: String,
    title: String,
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "scale"
    )

    val liftDp by animateDpAsState(
        targetValue = if (isHovered) (-8).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "lift"
    )

    Card(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .offset(y = liftDp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isHovered) 24f else 8f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16161F)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Cover image
            AsyncImage(
                model = coverUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            // Gradient rim light overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF9B59B6).copy(alpha = 0.15f)
                            ),
                            center = Offset(1.2f, -0.2f),
                            radius = 1.5f
                        )
                    )
            )

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF0D0D12).copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // Title
            Text(
                text = title,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color(0xFF9B59B6).copy(alpha = 0.5f),
                        offset = Offset(0f, 0f),
                        blurRadius = 8f
                    )
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Neon pill badge
            if (unreadCount > 0) {
                NeonPillBadge(
                    count = unreadCount,
                    glowColor = Color(0xFF9B59B6),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            // Border glow on hover
            if (isHovered) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF9B59B6).copy(alpha = 0.6f),
                                    Color(0xFF00D2D3).copy(alpha = 0.4f),
                                    Color(0xFF9B59B6).copy(alpha = 0.6f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }
        }
    }
}

@Preview
@Composable
private fun ManhwaCardPreview() {
    ManhwaCard(
        coverUrl = "",
        title = "Solo Leveling",
        unreadCount = 3,
        onClick = {},
        modifier = Modifier.size(140.dp, 210.dp)
    )
}
