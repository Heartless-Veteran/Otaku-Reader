package app.otakureader.feature.statistics

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.otakureader.domain.model.ReadingStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

/** Layout variants for the shareable statistics card. */
enum class ShareCardLayout { DETAILED, COMPACT }

/**
 * A shareable card showing reading statistics in a social-media-friendly format.
 *
 * @param layout DETAILED shows the secondary stats row; COMPACT shows only the headline number.
 * @param anonymized hides the library size (the only potentially-identifying figure).
 */
@Composable
fun StatisticsShareCard(
    stats: ReadingStats,
    modifier: Modifier = Modifier,
    layout: ShareCardLayout = ShareCardLayout.DETAILED,
    anonymized: Boolean = false,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
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
                text = stringResource(R.string.statistics_share_my_reading_year),
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
                text = stringResource(R.string.statistics_share_chapters_read),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (layout == ShareCardLayout.DETAILED) {
                Spacer(modifier = Modifier.height(24.dp))

                // Secondary stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (!anonymized) {
                        ShareStatItem(
                            icon = Icons.Default.Book,
                            value = stats.totalMangaInLibrary.toString(),
                            label = stringResource(R.string.statistics_share_in_library)
                        )
                    }
                    ShareStatItem(
                        icon = Icons.Default.Schedule,
                        value = formatDuration(stats.totalReadingTimeMs),
                        label = stringResource(R.string.statistics_share_time_spent)
                    )
                    ShareStatItem(
                        icon = Icons.Default.Whatshot,
                        value = stats.currentStreak.toString(),
                        label = stringResource(R.string.statistics_share_day_streak)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = stringResource(R.string.statistics_share_footer),
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
 * Bottom sheet that previews the shareable card and lets the user pick a layout, anonymize, then
 * share to other apps or save to the gallery. The card is rendered into a [GraphicsLayer] so it
 * can be captured to a bitmap reliably (no offscreen ComposeView measuring required).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsShareSheet(
    stats: ReadingStats,
    shareText: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var layout by remember { mutableStateOf(ShareCardLayout.DETAILED) }
    var anonymized by remember { mutableStateOf(false) }

    suspend fun capture(): Bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer)
                },
            ) {
                StatisticsShareCard(stats = stats, layout = layout, anonymized = anonymized)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                FilterChip(
                    selected = layout == ShareCardLayout.DETAILED,
                    onClick = { layout = ShareCardLayout.DETAILED },
                    label = { Text(stringResource(R.string.statistics_share_layout_detailed)) },
                )
                Spacer(modifier = Modifier.size(8.dp))
                FilterChip(
                    selected = layout == ShareCardLayout.COMPACT,
                    onClick = { layout = ShareCardLayout.COMPACT },
                    label = { Text(stringResource(R.string.statistics_share_layout_compact)) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.statistics_share_anonymize))
                Spacer(modifier = Modifier.size(8.dp))
                Switch(checked = anonymized, onCheckedChange = { anonymized = it })
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val saved = saveBitmapToGallery(context, capture())
                            val msg = if (saved) R.string.statistics_share_saved
                            else R.string.statistics_share_save_failed
                            Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
                        }
                    },
                ) { Text(stringResource(R.string.statistics_share_save_gallery)) }

                Button(
                    onClick = {
                        scope.launch {
                            val uri = bitmapToShareUri(context, capture())
                            context.startActivity(
                                Intent.createChooser(createShareIntent(uri, shareText), null),
                            )
                            onDismiss()
                        }
                    },
                ) { Text(stringResource(R.string.statistics_share_button)) }
            }
        }
    }
}

/** Writes [bitmap] to cache and returns a FileProvider URI shareable with other apps. */
private fun bitmapToShareUri(context: Context, bitmap: Bitmap): Uri {
    val statsDir = File(context.cacheDir, "shared_stats").also { it.mkdirs() }
    val file = File(statsDir, "reading_stats_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Saves [bitmap] to the device gallery via MediaStore. Returns true on success. */
private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean =
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "otaku_stats_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OtakuReader")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false
        runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: return@withContext false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrElse {
            resolver.delete(uri, null, null)
            false
        }
    }

/**
 * Creates a share intent for the statistics card.
 */
fun createShareIntent(uri: Uri, shareText: String): Intent {
    return Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
