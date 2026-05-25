package app.otakureader.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.otakureader.domain.model.MangaStatus
import coil3.compose.AsyncImage

@Composable
internal fun MangaDetailPanel(
    manga: LibraryMangaItem?,
    onOpenFullDetails: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (manga == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.library_detail_panel_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = manga.thumbnailUrl,
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(2f / 3f)
                .align(Alignment.CenterHorizontally)
                .clip(MaterialTheme.shapes.medium)
        )
        Text(
            text = manga.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (manga.unreadCount > 0) {
            Text(
                text = stringResource(R.string.library_detail_unread_chapters, manga.unreadCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        val statusText = when (manga.status) {
            MangaStatus.ONGOING -> stringResource(R.string.manga_status_ongoing)
            MangaStatus.COMPLETED -> stringResource(R.string.manga_status_completed)
            MangaStatus.LICENSED -> stringResource(R.string.manga_status_licensed)
            MangaStatus.PUBLISHING_FINISHED -> stringResource(R.string.manga_status_publishing_finished)
            MangaStatus.CANCELLED -> stringResource(R.string.manga_status_cancelled)
            MangaStatus.ON_HIATUS -> stringResource(R.string.manga_status_on_hiatus)
            else -> null
        }
        if (statusText != null) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onOpenFullDetails(manga.id) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.library_detail_open))
            }
            OutlinedButton(onClick = onClose) {
                Text(stringResource(R.string.library_detail_close))
            }
        }
    }
}
