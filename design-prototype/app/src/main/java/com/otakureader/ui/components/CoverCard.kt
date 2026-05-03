package com.otakureader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakureader.data.model.Manga
import com.otakureader.ui.theme.JetbrainsMonoFamily
import com.otakureader.ui.theme.LocalOtakuColors

@Composable
fun CoverArtPlaceholder(
    hue: Float,
    title: String,
    author: String,
    modifier: Modifier = Modifier,
) {
    val topColor = Color.hsl(hue.coerceIn(0f, 360f), 0.70f, 0.45f)
    val midColor = Color.hsl(((hue + 30f) % 360f), 0.50f, 0.28f)
    val botColor = Color.hsl(hue.coerceIn(0f, 360f), 0.30f, 0.15f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to topColor,
                        0.65f to midColor,
                        1f to botColor,
                    )
                )
            )
    ) {
        // Diagonal stripe overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.03f), Color.Transparent),
                    )
                )
        )
        // Accent line at top-left
        Box(
            modifier = Modifier
                .padding(12.dp)
                .width(24.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.4f))
                .align(Alignment.TopStart)
        )
        // Title + author at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp, 0.dp, 10.dp, 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.95f),
                lineHeight = 12.sp,
                maxLines = 3,
            )
            Text(
                text = author.uppercase(),
                fontSize = 8.sp,
                fontFamily = JetbrainsMonoFamily,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun CoverCard(
    manga: Manga,
    modifier: Modifier = Modifier,
    showProgress: Boolean = true,
) {
    val colors = LocalOtakuColors.current

    Box(modifier = modifier.aspectRatio(2f / 3f)) {
        CoverArtPlaceholder(
            hue = manga.hue,
            title = manga.title,
            author = manga.author,
            modifier = Modifier.fillMaxSize(),
        )

        // Unread badge
        if (manga.newChapters > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.accent)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = manga.newChapters.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetbrainsMonoFamily,
                    color = Color.White,
                )
            }
        }

        // Downloaded indicator
        if (manga.downloaded > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(11.dp),
                )
            }
        }

        // Progress bar at bottom
        if (showProgress && manga.readProgress > 0f && manga.readProgress < 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.Black.copy(alpha = 0.55f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(manga.readProgress)
                        .background(colors.accent),
                )
            }
        }
    }
}
