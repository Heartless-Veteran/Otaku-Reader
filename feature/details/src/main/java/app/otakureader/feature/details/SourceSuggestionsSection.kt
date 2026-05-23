@file:Suppress("MaxLineLength")
package app.otakureader.feature.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.otakureader.feature.details.R
import coil3.compose.AsyncImage

@Composable
internal fun SourceSuggestionsSection(
    suggestions: List<SourceSuggestion>,
    isLoading: Boolean,
    error: String?,
    onSuggestionClick: (SourceSuggestion) -> Unit,
    onLoadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty() && !isLoading && error == null) {
        // Show load button when no suggestions loaded yet
        OutlinedButton(
            onClick = onLoadClick,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.details_load_suggestions))
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.details_source_suggestions_title),
                style = MaterialTheme.typography.titleMedium
            )
            if (!isLoading && (suggestions.isNotEmpty() || error != null)) {
                TextButton(onClick = onLoadClick) {
                    Text(stringResource(R.string.details_refresh))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            suggestions.isNotEmpty() -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.height(160.dp)
                ) {
                    items(suggestions, key = { it.mangaUrl }) { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            onClick = { onSuggestionClick(suggestion) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SuggestionItem(
    suggestion: SourceSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(100.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = suggestion.thumbnailUrl,
            contentDescription = suggestion.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = suggestion.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        suggestion.reason?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
