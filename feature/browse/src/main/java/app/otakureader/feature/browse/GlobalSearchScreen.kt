package app.otakureader.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.otakureader.sourceapi.SourceManga
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import java.util.Locale

/**
 * Global search screen that queries all installed sources simultaneously and
 * displays results grouped by source as horizontal rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    initialQuery: String = "",
    onMangaClick: (sourceId: String, mangaUrl: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: GlobalSearchViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Pre-populate query from navigation argument
    LaunchedEffect(initialQuery) {
        viewModel.initQuery(initialQuery)
    }

    // Handle side effects
    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is GlobalSearchEffect.NavigateToMangaDetail -> {
                    onMangaClick(effect.sourceId, effect.mangaUrl)
                }
                is GlobalSearchEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = { viewModel.onEvent(GlobalSearchEvent.OnQueryChange(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.browse_global_search_placeholder)) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { viewModel.onEvent(GlobalSearchEvent.Search) }) {
                                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.browse_global_search))
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.browse_global_back)
                            )
                        }
                    }
                )
                if (state.searchProgress in 1..<state.searchTotal) {
                    LinearProgressIndicator(
                        progress = { state.searchProgress / state.searchTotal.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.onlyShowHasResults,
                        onClick = { viewModel.onEvent(GlobalSearchEvent.OnToggleOnlyResults) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        label = { Text(stringResource(R.string.browse_global_only_has_results)) }
                    )
                }
                HorizontalDivider()
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Recent searches chip row
            if (state.recentSearches.isNotEmpty() && !state.isSearching && state.sourceResults.isEmpty()) {
                SearchHistoryChips(
                    history = state.recentSearches,
                    onItemClick = { viewModel.onEvent(GlobalSearchEvent.OnHistoryItemClick(it)) },
                    onRemoveItem = { viewModel.onEvent(GlobalSearchEvent.OnRemoveHistoryItem(it)) },
                    onClearHistory = { viewModel.onEvent(GlobalSearchEvent.OnClearHistory) }
                )
            }
            GlobalSearchContent(
                state = state,
                onMangaClick = { sourceId, manga ->
                    viewModel.onEvent(GlobalSearchEvent.OnMangaClick(sourceId, manga))
                }
            )
        }
    }
}

@Composable
private fun GlobalSearchContent(
    state: GlobalSearchState,
    onMangaClick: (sourceId: String, manga: SourceManga) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.sourceResults.isEmpty() && !state.isSearching -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.browse_global_search_idle_hint),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        state.filteredSourceResults.isEmpty() && !state.isSearching -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.browse_global_no_filter_results),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.filteredSourceResults, key = { it.sourceId }) { sourceResult ->
                    SourceSection(
                        sourceResult = sourceResult,
                        onMangaClick = { manga -> onMangaClick(sourceResult.sourceId, manga) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SourceSection(
    sourceResult: SourceSearchResult,
    onMangaClick: (SourceManga) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = sourceResult.sourceName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (sourceResult.sourceLanguage.isNotEmpty()) {
            val displayLang = remember(sourceResult.sourceLanguage) {
                Locale(sourceResult.sourceLanguage).getDisplayLanguage(Locale.ENGLISH)
                    .replaceFirstChar { it.uppercase() }
            }
            Text(
                text = displayLang,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        when {
            sourceResult.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            sourceResult.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sourceResult.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            sourceResult.results.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.browse_no_results),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sourceResult.results, key = { it.url }) { manga ->
                        GlobalSearchMangaCard(
                            manga = manga,
                            onClick = { onMangaClick(manga) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GlobalSearchMangaCard(
    manga: SourceManga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(100.dp)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(manga.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = manga.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    // Tonal backdrop so failed/missing covers show a neutral block
                    // instead of an empty hole in the result row.
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Text(
                text = manga.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
private fun SearchHistoryChips(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.search_history_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onClearHistory) {
                Text(stringResource(R.string.search_history_clear))
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(history, key = { it }) { query ->
                InputChip(
                    selected = false,
                    onClick = { onItemClick(query) },
                    label = { Text(query, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = {
                        IconButton(onClick = { onRemoveItem(query) }, modifier = Modifier.size(18.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_history_remove),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}
