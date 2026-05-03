package app.otakureader.feature.statistics

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import app.otakureader.domain.model.ReadingStats
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

/**
 * A shareable card showing reading statistics in a social-media-friendly format.
 */
@Composable
fun StatisticsShareCard(
    stats: ReadingStats,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.15f),
                            surfaceColor
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "My Reading Year",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Big stat
            Text(
                text = NumberFormat.getInstance().format(stats.totalChaptersRead),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                fontWeight = FontWeight.ExtraBold,
                color = primaryColor
            )
            Text(
                text = "chapters read",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Secondary stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ShareStatItem(
                    icon = Icons.Default.Book,
                    value = stats.totalMangaInLibrary.toString(),
                    label = "in library"
                )
                ShareStatItem(
                    icon = Icons.Default.Schedule,
                    value = formatDuration(stats.totalReadingTimeMs),
                    label = "time spent"
                )
                ShareStatItem(
                    icon = Icons.Default.Whatshot,
                    value = stats.currentStreak.toString(),
                    label = "day streak"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = "📚 Otaku Reader",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ShareStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    return if (hours >= 100) "${hours}h" else "${hours}h ${TimeUnit.MILLISECONDS.toMinutes(ms) % 60}m"
}

/**
 * Captures a composable to a PNG file and returns a shareable URI.
 */
fun captureShareableCard(
    context: Context,
    composeView: androidx.compose.ui.platform.ComposeView
): Uri? {
    val bitmap = createBitmap(composeView.width, composeView.height)
    composeView.draw(android.graphics.Canvas(bitmap))

    val statsDir = File(context.cacheDir, "shared_stats").also { it.mkdirs() }
    val file = File(statsDir, "reading_stats_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

/**
 * Creates a share intent for the statistics card.
 */
fun createShareIntent(uri: Uri): Intent {
    return Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Check out my reading stats! 📚")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
