package app.otakureader.core.ui.components

import app.otakureader.core.ui.R
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import app.otakureader.core.ui.theme.LocalOtakuColors
import app.otakureader.core.ui.modifiers.bottomGradientScrim

/**
 * Premium manga card with hover/press animations, depth, shimmer, and physical book feel.
 *
 * @param title The manga title to display
 * @param coverUrl The URL of the manga cover image
 * @param onClick Callback when the card is clicked
 * @param modifier Modifier for customizing the layout
 * @param badge Optional composable for the top-right badge (e.g., unread count)
 * @param contentDescription Accessibility description for the cover image
 * @param isSelected Whether the card is in selected state (shows checkmark overlay)
 * @param readProgress 0f–1f fraction of chapters read; null hides the progress bar
 * @param onLongClick Optional long-click callback (enables multi-select)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaCard(
    title: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable (() -> Unit)? = null,
    contentDescription: String? = null,
    isSelected: Boolean = false,
    readProgress: Float? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val otaku = LocalOtakuColors.current

    // Hover + Press detection — both trigger same animations (DeX + touch)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isActive = isHovered || isPressed

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "mangaCardScale"
    )

    val tilt by animateFloatAsState(
        targetValue = if (isActive) -1.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "mangaCardTilt"
    )

    // Shimmer sweep on hover/press
    val shimmerProgress by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = tilt
                shadowElevation = if (isActive) 16f else 6f
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12121A))
    ) {
        Box(
            modifier = if (isActive) {
                Modifier.drawWithContent {
                    drawContent()
                    // Shimmer sweep overlay
                    val shimmerBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        start = androidx.compose.ui.geometry.Offset(
                            shimmerProgress * size.width,
                            0f
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            (shimmerProgress + 0.5f) * size.width,
                            size.height
                        )
                    )
                    drawRect(brush = shimmerBrush, blendMode = BlendMode.Plus)
                }
            } else {
                Modifier
            }
        ) {
            // Cover image
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription
                    ?: stringResource(R.string.manga_cover_content_description, title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
                loading = { MangaCardShimmer() },
                error = { MangaCardError() }
            )

            // Left spine shadow for physical book depth
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Gradient scrim for title readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .bottomGradientScrim(
                        heightPercent = 0.45f,
                        startAlpha = 0.0f,
                        endAlpha = 0.85f
                    )
            )

            // Title overlaid at bottom with shadow for readability
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                        blurRadius = 4f
                    )
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 8.dp, bottom = if (readProgress != null) 10.dp else 8.dp)
                    .fillMaxWidth(),
            )

            // Unread / custom badge — offset so it "floats" off the card edge
            badge?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                ) {
                    it()
                }
            }

            // Selection overlay — semi-transparent tint + checkmark
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .background(otaku.accent.copy(alpha = 0.38f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.manga_card_selected),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Reading progress bar — thin strip at the very bottom of the card
            if (readProgress != null && readProgress > 0f) {
                LinearProgressIndicator(
                    progress = { readProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = otaku.accent,
                    trackColor = otaku.surface3.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun MangaCardPreview() {
    MangaCard(
        title = "Attack on Titan",
        coverUrl = null,
        onClick = {},
        badge = { HankoBadge(count = 5) },
        readProgress = 0.6f
    )
}

@Preview
@Composable
private fun MangaCardSelectedPreview() {
    MangaCard(
        title = "Demon Slayer",
        coverUrl = null,
        onClick = {},
        isSelected = true,
        readProgress = 0.3f
    )
}

@Composable
private fun MangaCardError() {
    val otaku = LocalOtakuColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .background(otaku.danger.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "?",
            style = MaterialTheme.typography.headlineLarge,
            color = otaku.danger
        )
    }
}
