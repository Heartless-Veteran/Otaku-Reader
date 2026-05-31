package app.otakureader.core.ui.components

import app.otakureader.core.ui.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import app.otakureader.core.ui.theme.LocalOtakuColors
import app.otakureader.core.ui.modifiers.bottomGradientScrim

/**
 * Enhanced manga card with loading states, error handling, selection overlay,
 * reading progress bar, and Mihon-style title-on-gradient layout.
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
    statusBadge: @Composable (() -> Unit)? = null,
    contentDescription: String? = null,
    isSelected: Boolean = false,
    readProgress: Float? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    val otaku = LocalOtakuColors.current

    Card(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box {
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

            // Title overlaid at bottom
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 8.dp, bottom = if (readProgress != null) 10.dp else 8.dp)
                    .fillMaxWidth(),
            )

            // Unread / custom badge
            badge?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    it()
                }
            }

            // Status badge (e.g., completed / dropped marker), top-start corner
            statusBadge?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
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
