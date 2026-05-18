package app.otakureader.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * Gradient placeholder cover art matching the design prototype's CoverArt component.
 * Renders a linear gradient, diagonal stripe texture, and title/author overlay.
 *
 * @param hue Hue in degrees (0–360) used to derive the gradient colors
 * @param title Manga title shown at the bottom
 * @param author Author name shown below the title
 */
@Composable
fun CoverArtPlaceholder(
    hue: Float,
    title: String,
    author: String,
    modifier: Modifier = Modifier,
) {
    val topColor = Color.hsl(hue, 0.70f, 0.45f)
    val bottomColor = Color.hsl((hue + 30f) % 360f, 0.50f, 0.22f)
    val stripeColor = Color.White.copy(alpha = 0.06f)

    Box(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Background gradient
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(topColor, bottomColor),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                )
            )
            // Diagonal stripe texture
            val stripeSpacing = 18.dp.toPx()
            val diagonalLength = sqrt(size.width * size.width + size.height * size.height)
            var offset = -diagonalLength
            while (offset < diagonalLength * 2) {
                drawLine(
                    color = stripeColor,
                    start = Offset(offset, 0f),
                    end = Offset(offset + size.height, size.height),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Square,
                )
                offset += stripeSpacing
            }
        }

        // Title and author overlay at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (author.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = author,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
