package app.otakureader.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.otakureader.core.ui.components.CompletedBadge
import app.otakureader.core.ui.components.DownloadBadge
import app.otakureader.core.ui.components.DroppedBadge
import app.otakureader.core.ui.components.MangaCard
import app.otakureader.core.ui.components.ManhwaCard
import app.otakureader.core.ui.theme.LocalOtakuColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import coil3.compose.AsyncImage
import java.util.Calendar

/** Layout constants for the compact list-mode row. */
private object LibraryListRowDefaults {
    val HorizontalPadding = 12.dp
    val VerticalPadding = 6.dp
    val ItemSpacing = 12.dp
    val ThumbnailWidth = 40.dp
    val ThumbnailCornerRadius = 4.dp
    const val ThumbnailAspectRatio = 3f / 4f
    const val TitleMaxLines = 2
}

/** Returns true when a manga item belongs to the manhwa/webtoon content type. */
internal fun isManhwa(manga: LibraryMangaItem): Boolean {
    val src = manga.sourceId.toString().lowercase()
    return src.contains("manhwa") || src.contains("webtoon") ||
        src.contains("korean") || src.contains("toon") || src.contains("naver")
}

/** Max title lines for the caption shown below covers in comfortable grid mode. */
private const val COMFORTABLE_TITLE_MAX_LINES = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MangaGrid(
    state: LibraryState,
    onEvent: (LibraryEvent) -> Unit,
    adaptiveColumns: Int = state.gridSize,
    onMangaSelect: ((LibraryMangaItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedContentFilter by remember { mutableIntStateOf(0) }
    val contentTabs = listOf(
        stringResource(R.string.library_content_tab_all),
        stringResource(R.string.library_content_tab_manga),
        stringResource(R.string.library_content_tab_manhwa),
    )

    // Long-press opens the quick context menu. Fire haptic so the gesture is felt (#L7).
    val haptic = LocalHapticFeedback.current
    val onMangaLongClick: (Long) -> Unit = remember(haptic, onEvent) {
        { mangaId ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onEvent(LibraryEvent.ShowContextMenu(mangaId))
        }
    }
    // Taps open the detail pane only in two-pane mode with no active selection; otherwise route
    // through OnMangaClick so taps toggle selection while selection mode is active (and navigate
    // in single-pane). Without the selection guard, two-pane taps could never toggle items.
    val onMangaTap: (LibraryMangaItem) -> Unit = remember(haptic, onMangaSelect, onEvent, state.selectedManga.isEmpty()) {
        { manga ->
            if (onMangaSelect != null && state.selectedManga.isEmpty()) {
                onMangaSelect(manga)
            } else {
                if (state.selectedManga.isNotEmpty()) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onEvent(LibraryEvent.OnMangaClick(manga.id))
            }
        }
    }

    val displayedManga by remember(state.mangaList, selectedContentFilter) {
        derivedStateOf {
            when (selectedContentFilter) {
                1 -> state.mangaList.filter { !isManhwa(it) }
                2 -> state.mangaList.filter { isManhwa(it) }
                else -> state.mangaList
            }
        }
    }

    // GAP 1: page 0 = "All" (null), pages 1..n = each category id
    val categoryPages: List<Long?> = remember(state.categories) {
        listOf(null) + state.categories.map { it.id }
    }
    val pagerState = rememberPagerState(
        initialPage = categoryPages.indexOf(state.selectedCategory).coerceAtLeast(0),
        pageCount = { categoryPages.size },
    )

    // Swipe → update selected category in ViewModel
    LaunchedEffect(pagerState.currentPage) {
        val id = categoryPages.getOrNull(pagerState.currentPage)
        onEvent(LibraryEvent.OnCategorySelected(id))
    }
    // Chip / tab tap → scroll pager to matching page
    // Guard against fighting an in-progress user swipe gesture
    LaunchedEffect(state.selectedCategory) {
        if (pagerState.isScrollInProgress) return@LaunchedEffect
        val target = categoryPages.indexOf(state.selectedCategory).coerceAtLeast(0)
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Fixed header: greeting, goal banner, continue reading, category controls
        LibraryGreetingHeader(
            mangaCount = state.mangaList.size,
            unreadCount = state.mangaList.sumOf { it.unreadCount },
            userName = state.displayName,
        )

        if (state.readingGoal.dailyGoal > 0) {
            DailyGoalBanner(readingGoal = state.readingGoal)
        }

        if (state.continueReadingItems.isNotEmpty()) {
            ContinueReadingSection(
                items = state.continueReadingItems,
                onItemClick = { mangaId, chapterId ->
                    onEvent(LibraryEvent.ContinueReadingClick(mangaId, chapterId))
                }
            )
        }

        if (state.showRecommendations && state.recommendations.isNotEmpty()) {
            RecommendationsCarousel(
                items = state.recommendations,
                onMangaClick = { mangaId -> onEvent(LibraryEvent.OnMangaClick(mangaId)) },
                onDismiss = { mangaId -> onEvent(LibraryEvent.DismissRecommendation(mangaId)) },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.library_section_categories),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onEvent(LibraryEvent.ToggleBottomSheet) }) {
                Text(
                    text = stringResource(R.string.library_section_categories_manage),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        CategoryFilterChips(
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            onCategorySelected = { onEvent(LibraryEvent.OnCategorySelected(it)) },
            modifier = Modifier.padding(vertical = 4.dp)
        )

        CategoryTabRow(
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            onCategorySelected = { onEvent(LibraryEvent.OnCategorySelected(it)) },
            modifier = Modifier.padding(vertical = 4.dp)
        )

        if (state.readingLists.isNotEmpty()) {
            ReadingListFilterChips(
                readingLists = state.readingLists,
                selectedListId = state.filterReadingListId,
                onListSelected = { onEvent(LibraryEvent.SetFilterReadingList(it)) },
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Content-type filter tabs (All / Manga / Manhwa)
        TabRow(
            selectedTabIndex = selectedContentFilter,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                // getOrNull: the indicator lambda runs during layout, after which
                // selectedContentFilter may have changed — never index unchecked.
                tabPositions.getOrNull(selectedContentFilter)?.let { position ->
                    val otakuColors = LocalOtakuColors.current
                    val indicatorColor = when (selectedContentFilter) {
                        1 -> otakuColors.contentFilterManga
                        2 -> otakuColors.contentFilterManhwa
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = position.left)
                            .width(position.width)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.library_section_all_titles),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }

        // Swipeable pager — each page shows the active category's manga list.
        // Content is extracted to a separate composable to prevent ColumnScope from leaking
        // into AnimatedVisibility resolution (Kotlin 2.x context receiver change).
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { _ ->
            LibraryMangaPageContent(
                state = state,
                adaptiveColumns = adaptiveColumns,
                displayedManga = displayedManga,
                onMangaTap = onMangaTap,
                onMangaLongClick = onMangaLongClick,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun LibraryMangaPageContent(
    state: LibraryState,
    adaptiveColumns: Int,
    displayedManga: List<LibraryMangaItem>,
    onMangaTap: (LibraryMangaItem) -> Unit,
    onMangaLongClick: (Long) -> Unit,
    onEvent: (LibraryEvent) -> Unit,
) {
    if (state.displayMode == LibraryDisplayMode.LIST) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            columnItems(
                items = displayedManga,
                key = { it.id },
                contentType = { "manga_row" },
            ) { manga ->
                val downloadCount = state.downloadCountByManga[manga.id] ?: 0
                Box {
                    LibraryListRow(
                        manga = manga,
                        isSelected = manga.id in state.selectedManga,
                        unreadCount = if (state.showBadges) manga.unreadCount else 0,
                        downloadCount = if (state.showDownloadBadge) downloadCount else 0,
                        onClick = { onMangaTap(manga) },
                        onLongClick = { onMangaLongClick(manga.id) },
                    )
                    MangaContextMenu(
                        expanded = state.contextMenuMangaId == manga.id,
                        mangaId = manga.id,
                        onEvent = onEvent,
                    )
                }
            }
        }
    } else if (state.isStaggeredGrid && state.displayMode != LibraryDisplayMode.COMFORTABLE_GRID) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(130.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
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
                            onClick = { onMangaTap(manga) },
                            onLongClick = { onMangaLongClick(manga.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val readProgress = if (manga.totalChapterCount > 0) {
                            (manga.totalChapterCount - manga.unreadCount)
                                .coerceAtLeast(0)
                                .toFloat() / manga.totalChapterCount
                        } else null
                        val downloadCount = state.downloadCountByManga[manga.id] ?: 0
                        val continueReading = manga.lastRead != null && manga.unreadCount > 0
                        MangaCard(
                            title = manga.title,
                            coverUrl = manga.thumbnailUrl,
                            onClick = { onMangaTap(manga) },
                            onLongClick = { onMangaLongClick(manga.id) },
                            isSelected = manga.id in state.selectedManga,
                            readProgress = readProgress,
                            continueReading = continueReading,
                            isNew = manga.unreadCount > 0,
                            showTitle = state.showTitle,
                            badge = when {
                                state.showBadges && manga.unreadCount > 0 &&
                                    state.showDownloadBadge && downloadCount > 0 -> {
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
                            statusBadge = when {
                                manga.userCompleted -> { { CompletedBadge() } }
                                manga.userDropped -> { { DroppedBadge() } }
                                else -> null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Box {
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
                    MangaContextMenu(
                        expanded = state.contextMenuMangaId == manga.id,
                        mangaId = manga.id,
                        onEvent = onEvent,
                    )
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(adaptiveColumns),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
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
                val continueReading = manga.lastRead != null && manga.unreadCount > 0
                // Comfortable grid (#Komikku parity): cover with the title shown as a caption
                // below it rather than overlaid on the cover.
                val comfortable = state.displayMode == LibraryDisplayMode.COMFORTABLE_GRID
                val card: @Composable () -> Unit = {
                    MangaCard(
                        title = manga.title,
                        coverUrl = manga.thumbnailUrl,
                        onClick = { onMangaTap(manga) },
                        onLongClick = { onMangaLongClick(manga.id) },
                        isSelected = manga.id in state.selectedManga,
                        readProgress = readProgress,
                        continueReading = continueReading,
                        isNew = manga.unreadCount > 0,
                        showTitle = if (comfortable) false else state.showTitle,
                        badge = when {
                            state.showBadges && manga.unreadCount > 0 &&
                                state.showDownloadBadge && downloadCount > 0 -> {
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
                        statusBadge = when {
                            manga.userCompleted -> { { CompletedBadge() } }
                            manga.userDropped -> { { DroppedBadge() } }
                            else -> null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                val cardContent: @Composable () -> Unit = {
                    if (comfortable) {
                        // Whole item (cover + caption + padding) is tappable. Null indication
                        // avoids a second ripple layering over the card's own ripple.
                        Column(
                            modifier = Modifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onMangaTap(manga) },
                                onLongClick = { onMangaLongClick(manga.id) },
                            ),
                        ) {
                            card()
                            Text(
                                text = manga.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = COMFORTABLE_TITLE_MAX_LINES,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                            )
                        }
                    } else {
                        card()
                    }
                }
                Box {
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
                    MangaContextMenu(
                        expanded = state.contextMenuMangaId == manga.id,
                        mangaId = manga.id,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

/**
 * Compact single-row representation of a library entry, used when
 * [LibraryState.displayMode] is [LibraryDisplayMode.LIST].
 *
 * Mirrors the grid card's interaction contract: tap opens (or toggles selection when a
 * selection is active), long-press enters selection mode. The whole row tints with the
 * selection container colour while selected.
 */
@Composable
private fun LibraryListRow(
    manga: LibraryMangaItem,
    isSelected: Boolean,
    unreadCount: Int,
    downloadCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(
                horizontal = LibraryListRowDefaults.HorizontalPadding,
                vertical = LibraryListRowDefaults.VerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LibraryListRowDefaults.ItemSpacing),
    ) {
        AsyncImage(
            // Decorative: the title is shown as text right beside it, so a description
            // would make screen readers announce the title twice.
            model = manga.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(LibraryListRowDefaults.ThumbnailWidth)
                .aspectRatio(LibraryListRowDefaults.ThumbnailAspectRatio)
                .clip(RoundedCornerShape(LibraryListRowDefaults.ThumbnailCornerRadius)),
        )
        Text(
            text = manga.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = LibraryListRowDefaults.TitleMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (downloadCount > 0) {
            DownloadBadge(count = downloadCount)
        }
        if (unreadCount > 0) {
            UnreadBadge(count = unreadCount)
        }
    }
}

@Composable
private fun MangaContextMenu(
    expanded: Boolean,
    mangaId: Long,
    onEvent: (LibraryEvent) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onEvent(LibraryEvent.DismissContextMenu) },
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_context_menu_open)) },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            },
            onClick = {
                onEvent(LibraryEvent.DismissContextMenu)
                onEvent(LibraryEvent.OnMangaClick(mangaId))
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_context_menu_resume)) },
            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
            onClick = { onEvent(LibraryEvent.ResumeFromContextMenu(mangaId)) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_context_menu_mark_read)) },
            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            onClick = { onEvent(LibraryEvent.MarkMangaAsReadFromMenu(mangaId)) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_context_menu_share)) },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
            onClick = { onEvent(LibraryEvent.ShareMangaFromMenu(mangaId)) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_context_menu_migrate)) },
            leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
            onClick = { onEvent(LibraryEvent.MigrateMangaFromMenu(mangaId)) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.library_context_menu_select)) },
            leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null) },
            onClick = { onEvent(LibraryEvent.SelectMangaFromMenu(mangaId)) },
        )
    }
}

@Composable
internal fun CategoryTabRow(
    categories: List<CategoryItem>,
    selectedCategory: Long?,
    onCategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = categories.sumOf { it.count }
    val categoryTabs = listOf(
        CategoryItem(id = -1, name = stringResource(R.string.library_category_all), count = totalCount)
    ) + categories

    val selectedIndex = when (selectedCategory) {
        null -> 0
        else -> categoryTabs.indexOfFirst { it.id == selectedCategory }.coerceAtLeast(0)
    }

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 16.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        categoryTabs.forEachIndexed { index, category ->
            Tab(
                selected = selectedIndex == index,
                onClick = {
                    if (category.id == -1L) onCategorySelected(null)
                    else onCategorySelected(category.id)
                },
                text = {
                    Text(
                        text = "${category.name} ${category.count}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}

@Composable
internal fun LibraryGreetingHeader(
    mangaCount: Int,
    unreadCount: Int,
    userName: String,
    modifier: Modifier = Modifier,
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> R.string.library_greeting_morning
            hour < 18 -> R.string.library_greeting_afternoon
            else -> R.string.library_greeting_evening
        }
    }
    val greetingText = if (userName.isBlank()) {
        "${stringResource(greeting)} 👋"
    } else {
        "${stringResource(greeting)}, $userName 👋"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = greetingText,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.library_greeting_stats, mangaCount, unreadCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun UnreadBadge(
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
