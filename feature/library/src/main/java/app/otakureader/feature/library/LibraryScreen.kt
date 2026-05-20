package app.otakureader.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.otakureader.core.ui.components.DownloadBadge
import app.otakureader.core.ui.components.IncognitoBanner
import app.otakureader.core.ui.components.MangaCard
import app.otakureader.core.ui.components.ManhwaCard
import app.otakureader.core.ui.components.OtakuChip
import app.otakureader.core.ui.theme.LocalOtakuColors
import app.otakureader.domain.model.Manga
import coil3.compose.AsyncImage
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import app.otakureader.core.ui.adaptive.WindowWidthSizeClass
import app.otakureader.core.ui.adaptive.isExpanded
import app.otakureader.core.ui.adaptive.rememberWindowWidthSizeClass
import app.otakureader.domain.model.MangaStatus
import app.otakureader.domain.model.ReadingGoal
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import app.otakureader.core.ui.theme.OtakuReaderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onMangaClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToMigration: (List<Long>) -> Unit = {},
    onNavigateToCategoryManagement: () -> Unit = {},
    onNavigateToReader: (mangaId: Long, chapterId: Long) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is LibraryEffect.NavigateToManga -> onMangaClick(effect.mangaId)
                is LibraryEffect.NavigateToReader -> onNavigateToReader(effect.mangaId, effect.chapterId)
                is LibraryEffect.ShowError -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
                is LibraryEffect.NavigateToMigration -> {
                    onNavigateToMigration(effect.selectedMangaIds)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            when {
                // ── Selection mode: action buttons ──────────────────────────
                state.selectedManga.isNotEmpty() -> TopAppBar(
                    title = {
                        Text(stringResource(R.string.library_selected_count, state.selectedManga.size))
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ClearSelection) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.library_deselect_all))
                        }
                    },
                    actions = {
                        // Mark all selected as read
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.MarkSelectedAsRead) }) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.library_mark_selected_read)
                            )
                        }
                        // Mark all selected as unread
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.MarkSelectedAsUnread) }) {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = stringResource(R.string.library_mark_selected_unread)
                            )
                        }
                        // Download selected
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.DownloadSelected) }) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = stringResource(R.string.library_download_selected)
                            )
                        }
                        // Remove selected from library
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.RemoveSelectedFromLibrary) }) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = stringResource(R.string.library_remove_selected)
                            )
                        }
                    }
                )

                // ── Search mode: inline search field ────────────────────────
                state.showSearchBar -> TopAppBar(
                    title = {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onEvent(LibraryEvent.OnSearchQueryChange(it)) },
                            placeholder = { Text(stringResource(R.string.library_search_placeholder)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {}),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ToggleSearchBar) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.library_search_close))
                        }
                    }
                )

                // ── Normal mode ──────────────────────────────────────────────
                else -> TopAppBar(
                    title = { Text(stringResource(R.string.library_title)) },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ToggleIncognito) }) {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = "Toggle incognito mode",
                                tint = if (state.incognitoMode)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { viewModel.onEvent(LibraryEvent.ToggleSearchBar) }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.library_search))
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.library_more))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_downloads)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToDownloads()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_settings)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_filter_has_notes)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(LibraryEvent.FilterHasNotes(!state.filterHasNotes))
                                },
                                trailingIcon = {
                                    Checkbox(
                                        checked = state.filterHasNotes,
                                        onCheckedChange = { checked ->
                                            showMenu = false
                                            viewModel.onEvent(LibraryEvent.FilterHasNotes(checked))
                                        }
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.library_manage_categories)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToCategoryManagement()
                                }
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LibraryContent(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryContent(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val widthSizeClass = rememberWindowWidthSizeClass()
    val isExpandedWidth = widthSizeClass.isExpanded
    val adaptiveColumns = when (widthSizeClass) {
        WindowWidthSizeClass.Expanded -> (state.gridSize + 2).coerceAtLeast(5)
        WindowWidthSizeClass.Medium -> (state.gridSize + 1).coerceAtLeast(4)
        WindowWidthSizeClass.Compact -> state.gridSize
    }
    var detailManga by remember { mutableStateOf<LibraryMangaItem?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.incognitoMode) {
            IncognitoBanner()
        }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(LibraryEvent.Refresh) },
        modifier = Modifier.weight(1f)
    ) {
        when {
            state.isLoading && state.mangaList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            state.mangaList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    EmptyLibraryMessage(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            isExpandedWidth -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    MangaGrid(
                        state = state,
                        onEvent = onEvent,
                        adaptiveColumns = adaptiveColumns,
                        onMangaSelect = { manga -> detailManga = manga },
                        modifier = Modifier.weight(0.55f)
                    )
                    VerticalDivider()
                    MangaDetailPanel(
                        manga = detailManga,
                        onOpenFullDetails = { onEvent(LibraryEvent.OnMangaClick(it)) },
                        onClose = { detailManga = null },
                        modifier = Modifier.weight(0.45f)
                    )
                }
            }
            else -> {
                MangaGrid(
                    state = state,
                    onEvent = onEvent,
                    adaptiveColumns = adaptiveColumns
                )
            }
        }
    }
    } // end Column
}

/** Returns true when a manga item belongs to the manhwa/webtoon content type. */
private fun isManhwa(manga: LibraryMangaItem): Boolean {
    val src = manga.sourceId.toString().lowercase()
    return src.contains("manhwa") || src.contains("webtoon") ||
        src.contains("korean") || src.contains("toon") || src.contains("naver")
}

@Composable
private fun MangaGrid(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
    adaptiveColumns: Int = state.gridSize,
    onMangaSelect: ((LibraryMangaItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedContentFilter by remember { mutableIntStateOf(0) }
    val contentTabs = listOf("All", "Manga", "Manhwa")

    val displayedManga = when (selectedContentFilter) {
        1 -> state.mangaList.filter { !isManhwa(it) }
        2 -> state.mangaList.filter { isManhwa(it) }
        else -> state.mangaList
    }

    // Header items shared by both grid variants
    val headerContent: @Composable () -> Unit = {
        // Daily reading goal banner
        if (state.readingGoal.dailyGoal > 0) {
            DailyGoalBanner(readingGoal = state.readingGoal)
        }

        // Continue Reading section
        if (state.continueReadingItems.isNotEmpty()) {
            ContinueReadingSection(
                items = state.continueReadingItems,
                onItemClick = { mangaId, chapterId ->
                    onEvent(LibraryEvent.ContinueReadingClick(mangaId, chapterId))
                }
            )
        }

        // Category filter chips
        CategoryFilterChips(
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            onCategorySelected = { onEvent(LibraryEvent.OnCategorySelected(it)) },
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Reading list filter chips
        if (state.readingLists.isNotEmpty()) {
            ReadingListFilterChips(
                readingLists = state.readingLists,
                selectedListId = state.filterReadingListId,
                onListSelected = { onEvent(LibraryEvent.SetFilterReadingList(it)) },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Content-type filter tabs
        TabRow(
            selectedTabIndex = selectedContentFilter,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (selectedContentFilter < tabPositions.size) {
                    val indicatorColor = when (selectedContentFilter) {
                        1 -> Color(0xFFFF4757)
                        2 -> Color(0xFF9B59B6)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = tabPositions[selectedContentFilter].left)
                            .width(tabPositions[selectedContentFilter].width)
                            .height(3.dp)
                            .background(indicatorColor, RoundedCornerShape(2.dp))
                    )
                }
            }
        ) {
            contentTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedContentFilter == index,
                    onClick = { selectedContentFilter = index },
                    text = { Text(title) }
                )
            }
        }
    }

    if (state.isStaggeredGrid) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(130.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = modifier.fillMaxSize()
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                headerContent()
            }

            staggeredItems(
                items = displayedManga,
                key = { it.id },
                contentType = { "manga_card" }
            ) { manga ->
                val cardContent: @Composable () -> Unit = {
                    if (isManhwa(manga)) {
                        ManhwaCard(
                            coverUrl = manga.thumbnailUrl ?: "",
                            title = manga.title,
                            unreadCount = if (state.showBadges) manga.unreadCount else 0,
                            onClick = {
                                if (onMangaSelect != null) onMangaSelect(manga)
                                else onEvent(LibraryEvent.OnMangaClick(manga.id))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val readProgress = if (manga.totalChapterCount > 0) {
                            (manga.totalChapterCount - manga.unreadCount)
                                .coerceAtLeast(0)
                                .toFloat() / manga.totalChapterCount
                        } else null
                        val downloadCount = state.downloadCountByManga[manga.id] ?: 0
                        MangaCard(
                            title = manga.title,
                            coverUrl = manga.thumbnailUrl,
                            onClick = {
                                if (onMangaSelect != null) onMangaSelect(manga)
                                else onEvent(LibraryEvent.OnMangaClick(manga.id))
                            },
                            onLongClick = { onEvent(LibraryEvent.OnMangaLongClick(manga.id)) },
                            isSelected = manga.id in state.selectedManga,
                            readProgress = readProgress,
                            badge = when {
                                state.showBadges && manga.unreadCount > 0 && state.showDownloadBadge && downloadCount > 0 -> {
                                    {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            UnreadBadge(count = manga.unreadCount)
                                            DownloadBadge(count = downloadCount)
                                        }
                                    }
                                }
                                state.showBadges && manga.unreadCount > 0 -> {
                                    { UnreadBadge(count = manga.unreadCount) }
                                }
                                state.showDownloadBadge && downloadCount > 0 -> {
                                    { DownloadBadge(count = downloadCount) }
                                }
                                else -> null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (state.visualEffectsEnabled) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    ) {
                        cardContent()
                    }
                } else {
                    cardContent()
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(adaptiveColumns),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize()
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) },
                contentType = "header_section"
            ) {
                headerContent()
            }

            gridItems(
                items = displayedManga,
                key = { it.id },
                contentType = { "manga_card" }
            ) { manga ->
                val readProgress = if (manga.totalChapterCount > 0) {
                    (manga.totalChapterCount - manga.unreadCount)
                        .coerceAtLeast(0)
                        .toFloat() / manga.totalChapterCount
                } else null
                val downloadCount = state.downloadCountByManga[manga.id] ?: 0
                val cardContent: @Composable () -> Unit = {
                    MangaCard(
                        title = manga.title,
                        coverUrl = manga.thumbnailUrl,
                        onClick = {
                            if (onMangaSelect != null) onMangaSelect(manga)
                            else onEvent(LibraryEvent.OnMangaClick(manga.id))
                        },
                        onLongClick = { onEvent(LibraryEvent.OnMangaLongClick(manga.id)) },
                        isSelected = manga.id in state.selectedManga,
                        readProgress = readProgress,
                        badge = when {
                            state.showBadges && manga.unreadCount > 0 && state.showDownloadBadge && downloadCount > 0 -> {
                                {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        UnreadBadge(count = manga.unreadCount)
                                        DownloadBadge(count = downloadCount)
                                    }
                                }
                            }
                            state.showBadges && manga.unreadCount > 0 -> {
                                { UnreadBadge(count = manga.unreadCount) }
                            }
                            state.showDownloadBadge && downloadCount > 0 -> {
                                { DownloadBadge(count = downloadCount) }
                            }
                            else -> null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state.visualEffectsEnabled) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    ) {
                        cardContent()
                    }
                } else {
                    cardContent()
                }
            }
        }
    }
}

@Composable
private fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(24.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) stringResource(R.string.library_badge_overflow) else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun EmptyLibraryMessage(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = stringResource(R.string.library_empty_icon_description),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.library_empty_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.library_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryFilterChips(
    categories: List<CategoryItem>,
    selectedCategory: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // "All" chip
        item {
            OtakuChip(
                label = stringResource(R.string.library_category_all),
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
            )
        }

        // Category chips
        items(
            items = categories,
            key = { it.id }
        ) { category ->
            OtakuChip(
                label = category.name,
                selected = selectedCategory == category.id,
                onClick = { onCategorySelected(category.id) },
            )
        }
    }
}

@Composable
private fun ReadingListFilterChips(
    readingLists: List<ReadingListFilterItem>,
    selectedListId: Long?,
    onListSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            OtakuChip(
                label = stringResource(R.string.library_reading_list_all),
                selected = selectedListId == null,
                onClick = { onListSelected(null) },
            )
        }

        items(
            items = readingLists,
            key = { it.id }
        ) { list ->
            OtakuChip(
                label = list.name,
                selected = selectedListId == list.id,
                onClick = { onListSelected(list.id) },
            )
        }
    }
}

// ── Continue Reading ────────────────────────────────────────────────────────

@Composable
private fun ContinueReadingSection(
    items: List<app.otakureader.domain.model.ContinueReadingItem>,
    onItemClick: (mangaId: Long, chapterId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 4.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.library_continue_reading_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = items,
                key = { it.mangaId }
            ) { item ->
                ContinueReadingCard(
                    item = item,
                    onClick = { onItemClick(item.mangaId, item.chapterId) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ContinueReadingCard(
    item: app.otakureader.domain.model.ContinueReadingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(130.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box {
            coil3.compose.AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = stringResource(
                    R.string.library_continue_reading_cover,
                    item.mangaTitle
                ),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )

            // Gradient + text overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 80f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = item.mangaTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.library_continue_reading_chapter,
                        item.chapterNumber
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
private fun MangaDetailPanel(
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
            androidx.compose.material3.Button(
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

/**
 * Compact banner showing progress toward today's chapter reading goal.
 * Displayed at the top of the library grid whenever a daily goal is configured.
 */
@Composable
private fun DailyGoalBanner(
    readingGoal: ReadingGoal,
    modifier: Modifier = Modifier
) {
    // Guard: dailyGoal must be positive before computing the fraction to avoid division by zero.
    if (readingGoal.dailyGoal <= 0) return
    val isComplete = readingGoal.dailyProgress >= readingGoal.dailyGoal
    val fraction = (readingGoal.dailyProgress.toFloat() / readingGoal.dailyGoal.toFloat())
        .coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.library_daily_goal_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isComplete)
                        stringResource(R.string.library_daily_goal_complete)
                    else
                        pluralStringResource(
                            R.plurals.library_daily_goal_progress,
                            readingGoal.dailyGoal,
                            readingGoal.dailyProgress,
                            readingGoal.dailyGoal
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isComplete)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isComplete)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12121A)
@Composable
private fun EmptyLibraryMessagePreview() {
    OtakuReaderTheme {
        EmptyLibraryMessage()
    }
}

