package app.otakureader.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.core.preferences.LibraryPreferences
import app.otakureader.core.preferences.ReadingGoalPreferences
import app.otakureader.domain.model.ContentRating
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaStatus
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.DownloadRepository
import app.otakureader.domain.repository.ReadingListRepository
import app.otakureader.domain.repository.StatisticsRepository
import app.otakureader.domain.tracking.TrackRepository
import app.otakureader.domain.usecase.GetCategoriesUseCase
import app.otakureader.domain.usecase.GetContinueReadingUseCase
import app.otakureader.domain.usecase.GetLibraryMangaUseCase
import app.otakureader.domain.usecase.SearchLibraryMangaUseCase
import app.otakureader.domain.usecase.ToggleFavoriteMangaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibraryManga: GetLibraryMangaUseCase,
    private val searchLibraryManga: SearchLibraryMangaUseCase,
    private val toggleFavoriteManga: ToggleFavoriteMangaUseCase,
    private val libraryPreferences: LibraryPreferences,
    private val generalPreferences: GeneralPreferences,
    private val chapterRepository: ChapterRepository,
    private val downloadRepository: DownloadRepository,
    private val trackRepository: TrackRepository,
    private val getCategories: GetCategoriesUseCase,
    private val getContinueReading: GetContinueReadingUseCase,
    private val readingGoalPreferences: ReadingGoalPreferences,
    private val statisticsRepository: StatisticsRepository,
    private val readingListRepository: ReadingListRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val _effect = Channel<LibraryEffect>(Channel.BUFFERED)
    val effect: Flow<LibraryEffect> = _effect.receiveAsFlow()

    /** Holds the full, unfiltered library items for reactive filtering. */
    private val _allItems = MutableStateFlow<List<LibraryMangaItem>>(emptyList())

    /** IDs of manga matching the current search query; null when no search is active. */
    private val _searchMatchingIds = MutableStateFlow<Set<Long>?>(null)
    private var searchJob: Job? = null

    init {
        loadLibrary()
        loadCategories()
        observeLibraryPreferences()
        observeFilteredItems()
        observeNewUpdatesCount()
        observeContinueReading()
        observeGoalProgress()
        observeReadingLists()
    }

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.Refresh,
            is LibraryEvent.OnMangaClick,
            is LibraryEvent.OnMangaLongClick,
            is LibraryEvent.ContinueReadingClick -> handleNavigationEvent(event)
            is LibraryEvent.OnSearchQueryChange,
            is LibraryEvent.ToggleSearchBar,
            is LibraryEvent.OnCategorySelected,
            is LibraryEvent.ClearSelection -> handleUiStateEvent(event)
            is LibraryEvent.FilterHasNotes,
            is LibraryEvent.SetSortMode,
            is LibraryEvent.SetFilterMode,
            is LibraryEvent.SetFilterSource,
            is LibraryEvent.ToggleNsfw,
            is LibraryEvent.SetFilterReadingList -> handleFilterSortEvent(event)
            is LibraryEvent.ToggleFavorite,
            is LibraryEvent.MarkSelectedAsRead,
            is LibraryEvent.MarkSelectedAsUnread,
            is LibraryEvent.RemoveSelectedFromLibrary,
            is LibraryEvent.DownloadSelected -> handleActionEvent(event)
        }
    }

    private fun handleNavigationEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.Refresh -> onRefresh()
            is LibraryEvent.OnMangaClick -> onMangaClick(event.mangaId)
            is LibraryEvent.OnMangaLongClick -> onMangaLongClick(event.mangaId)
            is LibraryEvent.ContinueReadingClick -> onContinueReadingClick(event.mangaId, event.chapterId)
            else -> Unit // unreachable due to outer when
        }
    }

    private fun handleUiStateEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.OnSearchQueryChange -> onSearchQueryChange(event.query)
            is LibraryEvent.ToggleSearchBar -> toggleSearchBar()
            is LibraryEvent.OnCategorySelected -> onCategorySelected(event.categoryId)
            is LibraryEvent.ClearSelection -> clearSelection()
            else -> Unit // unreachable due to outer when
        }
    }

    private fun handleFilterSortEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.FilterHasNotes -> onFilterHasNotes(event.enabled)
            is LibraryEvent.SetSortMode -> onSetSortMode(event.mode)
            is LibraryEvent.SetFilterMode -> onSetFilterMode(event.mode)
            is LibraryEvent.SetFilterSource -> onSetFilterSource(event.sourceId)
            is LibraryEvent.ToggleNsfw -> onToggleNsfw(event.show)
            is LibraryEvent.SetFilterReadingList -> onSetFilterReadingList(event.listId)
            else -> Unit // unreachable due to outer when
        }
    }

    private fun handleActionEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.ToggleFavorite -> toggleFavorite(event.mangaId)
            is LibraryEvent.MarkSelectedAsRead -> markSelectedAsRead()
            is LibraryEvent.MarkSelectedAsUnread -> markSelectedAsUnread()
            is LibraryEvent.RemoveSelectedFromLibrary -> removeSelectedFromLibrary()
            is LibraryEvent.DownloadSelected -> downloadSelected()
            else -> Unit // unreachable due to outer when
        }
    }

    private fun onRefresh() {
        loadLibrary()
    }

    private fun observeLibraryPreferences() {
        // Observe each preference independently to avoid 6-flow combine type-inference limitation
        libraryPreferences.gridSize
            .onEach { gridSize -> _state.update { it.copy(gridSize = gridSize) } }
            .launchIn(viewModelScope)
        libraryPreferences.showBadges
            .onEach { showBadges -> _state.update { it.copy(showBadges = showBadges) } }
            .launchIn(viewModelScope)
        libraryPreferences.librarySortMode
            .onEach { sortModeInt ->
                _state.update {
                    it.copy(sortMode = LibrarySortMode.entries.getOrElse(sortModeInt) { LibrarySortMode.ALPHABETICAL })
                }
            }
            .launchIn(viewModelScope)
        libraryPreferences.libraryFilterMode
            .onEach { filterModeInt ->
                _state.update {
                    it.copy(filterMode = LibraryFilterMode.entries.getOrElse(filterModeInt) { LibraryFilterMode.ALL })
                }
            }
            .launchIn(viewModelScope)
        libraryPreferences.libraryFilterSourceId
            .onEach { filterSourceId -> _state.update { it.copy(filterSourceId = filterSourceId) } }
            .launchIn(viewModelScope)
        generalPreferences.showNsfwContent
            .onEach { showNsfw -> _state.update { it.copy(showNsfw = showNsfw) } }
            .launchIn(viewModelScope)
    }

    private fun observeNewUpdatesCount() {
        generalPreferences.lastUpdatesViewedAt
            .flatMapLatest { since -> chapterRepository.countNewUpdatesSince(since) }
            .onEach { count -> _state.update { it.copy(newUpdatesCount = count) } }
            .launchIn(viewModelScope)
    }

    private fun loadLibrary() {
        val isRefreshing = _state.value.mangaList.isNotEmpty()
        _state.update { it.copy(isLoading = !isRefreshing, isRefreshing = isRefreshing) }

        getLibraryManga()
            .map { mangaList ->
                coroutineScope {
                    // Build tracking lookup in parallel
                    val trackingDeferred = mangaList.map { manga ->
                        async {
                            val hasEntries = trackRepository.observeEntriesForManga(manga.id)
                                .first().isNotEmpty()
                            if (hasEntries) manga.id else null
                        }
                    }

                    // Build download lookup in parallel
                    val downloadDeferred = mangaList.map { manga ->
                        async {
                            val hasDownloads = downloadRepository.hasMangaDownloads(
                                sourceName = manga.sourceId.toString(),
                                mangaTitle = manga.title
                            )
                            if (hasDownloads) manga.id else null
                        }
                    }

                    val trackedMangaIds = trackingDeferred.awaitAll().filterNotNull().toSet()
                    val downloadedMangaIds = downloadDeferred.awaitAll().filterNotNull().toSet()

                    mangaList.map { manga ->
                        manga.toLibraryItem(
                            isDownloaded = manga.id in downloadedMangaIds,
                            hasTracking = manga.id in trackedMangaIds
                        )
                    }
                }
            }
            .onEach { items ->
                _allItems.value = items
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = null) }
            }
            .catch { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategories()
                .collect { categories ->
                    val items = categories.map { category ->
                        CategoryItem(
                            id = category.id,
                            name = category.name,
                            count = category.mangaCount
                        )
                    }
                    _state.update { it.copy(categories = items) }
                }
        }
    }

    /** Encapsulates all filter/sort parameters derived from state for use in the filtered-items combine. */
    private data class FilterSortParams(
        val query: String,
        val searchMatchingIds: Set<Long>?,
        val filterHasNotes: Boolean,
        val sortMode: LibrarySortMode,
        val filterMode: LibraryFilterMode,
        val filterSourceId: Long?,
        val showNsfw: Boolean,
        val selectedCategory: Long?,
        val categoryMangaIds: Set<Long> = emptySet(),
        val filterReadingListId: Long? = null,
        val readingListMangaIds: Set<Long> = emptySet()
    )

    private fun observeFilteredItems() {
        // When category selection changes, fetch the manga IDs in that category
        viewModelScope.launch {
            _state.map { it.selectedCategory }
                .distinctUntilChanged()
                .collect { categoryId ->
                    if (categoryId != null) {
                        val mangaIds = getCategories.getMangaIdsForCategory(categoryId).first()
                        _state.update { it.copy(categoryFilterMangaIds = mangaIds.toSet()) }
                    } else {
                        _state.update { it.copy(categoryFilterMangaIds = emptySet()) }
                    }
                }
        }

        // When reading list filter changes, reactively track the manga IDs in that list
        _state.map { it.filterReadingListId }
            .distinctUntilChanged()
            .flatMapLatest { listId ->
                if (listId != null) {
                    readingListRepository.getListWithManga(listId)
                        .map { (_, items) -> items.map { it.manga.id }.toSet() }
                } else {
                    flowOf(emptySet())
                }
            }
            .onEach { ids -> _state.update { it.copy(readingListMangaIds = ids) } }
            .launchIn(viewModelScope)

        // Combine all items with filter params (now including category, reading list, and search result IDs from state)
        combine(
            _allItems,
            _searchMatchingIds,
            _state.map {
                FilterSortParams(
                    query = it.searchQuery,
                    searchMatchingIds = null, // populated by combine below
                    filterHasNotes = it.filterHasNotes,
                    sortMode = it.sortMode,
                    filterMode = it.filterMode,
                    filterSourceId = it.filterSourceId,
                    showNsfw = it.showNsfw,
                    selectedCategory = it.selectedCategory,
                    categoryMangaIds = it.categoryFilterMangaIds,
                    filterReadingListId = it.filterReadingListId,
                    readingListMangaIds = it.readingListMangaIds
                )
            }.distinctUntilChanged()
        ) { items, matchingIds, params ->
            applyFiltersAndSort(items, params.copy(searchMatchingIds = matchingIds))
        }
            .onEach { filtered ->
                _state.update { it.copy(mangaList = filtered) }
            }
            .launchIn(viewModelScope)
    }

    private fun applyFiltersAndSort(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> {
        val filtered = items
            .let { applyCategoryFilter(it, params) }
            .let { applyNsfwFilter(it, params) }
            .let { applySearchFilter(it, params) }
            .let { applyHasNotesFilter(it, params) }
            .let { applySourceFilter(it, params) }
            .let { applyReadingListFilter(it, params) }
            .let { applyFilterMode(it, params.filterMode) }
        return applySort(filtered, params.sortMode)
    }

    private fun applyCategoryFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> {
        return if (params.selectedCategory != null) {
            items.filter { it.id in params.categoryMangaIds }
        } else {
            items
        }
    }

    private fun applyNsfwFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> {
        return if (!params.showNsfw) items.filter { !it.isNsfw } else items
    }

    private fun applySearchFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> {
        val matchingIds = params.searchMatchingIds ?: return items
        return items.filter { it.id in matchingIds }
    }

    private fun applyHasNotesFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> {
        return if (params.filterHasNotes) items.filter { it.hasNote } else items
    }

    private fun applySourceFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> {
        return if (params.filterSourceId != null) {
            items.filter { it.sourceId == params.filterSourceId }
        } else {
            items
        }
    }

    private fun applyReadingListFilter(items: List<LibraryMangaItem>, params: FilterSortParams): List<LibraryMangaItem> {
        return if (params.filterReadingListId != null) {
            items.filter { it.id in params.readingListMangaIds }
        } else {
            items
        }
    }

    private fun applyFilterMode(items: List<LibraryMangaItem>, filterMode: LibraryFilterMode): List<LibraryMangaItem> {
        return when (filterMode) {
            LibraryFilterMode.DOWNLOADED -> items.filter { it.isDownloaded }
            LibraryFilterMode.UNREAD -> items.filter { it.unreadCount > 0 }
            LibraryFilterMode.COMPLETED -> items.filter { it.userCompleted }
            LibraryFilterMode.DROPPED -> items.filter { it.userDropped }
            LibraryFilterMode.TRACKING -> items.filter { it.hasTracking }
            LibraryFilterMode.READING_LIST -> items
            LibraryFilterMode.ALL -> items
        }
    }

    private fun applySort(items: List<LibraryMangaItem>, sortMode: LibrarySortMode): List<LibraryMangaItem> {
        return when (sortMode) {
            LibrarySortMode.ALPHABETICAL -> items.sortedBy { it.title }
            LibrarySortMode.LAST_READ -> items.sortedByDescending { it.lastRead ?: 0L }
            LibrarySortMode.DATE_ADDED -> items.sortedByDescending { it.dateAdded }
            LibrarySortMode.UNREAD_COUNT -> items.sortedByDescending { it.unreadCount }
            LibrarySortMode.SOURCE -> items.sortedBy { it.sourceId }
        }
    }

    private fun onMangaClick(mangaId: Long) {
        if (_state.value.selectedManga.isNotEmpty()) {
            toggleSelection(mangaId)
        } else {
            viewModelScope.launch {
                _effect.send(LibraryEffect.NavigateToManga(mangaId))
            }
        }
    }

    private fun onMangaLongClick(mangaId: Long) {
        toggleSelection(mangaId)
    }

    private fun toggleSelection(mangaId: Long) {
        _state.update { state ->
            val currentSelection = state.selectedManga
            val newSelection = if (currentSelection.contains(mangaId)) {
                currentSelection - mangaId
            } else {
                currentSelection + mangaId
            }
            state.copy(selectedManga = newSelection)
        }
    }

    private fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchMatchingIds.value = null
            return
        }
        searchJob = viewModelScope.launch {
            searchLibraryManga(query).collect { mangas ->
                _searchMatchingIds.value = mangas.map { it.id }.toSet()
            }
        }
    }

    private fun toggleSearchBar() {
        _state.update { state ->
            if (state.showSearchBar) {
                // Closing: also clear the query
                searchJob?.cancel()
                _searchMatchingIds.value = null
                state.copy(showSearchBar = false, searchQuery = "")
            } else {
                state.copy(showSearchBar = true)
            }
        }
    }

    private fun onCategorySelected(categoryId: Long?) {
        _state.update { it.copy(selectedCategory = categoryId) }
    }

    private fun clearSelection() {
        _state.update { it.copy(selectedManga = emptySet()) }
    }

    private fun markSelectedAsRead() = markChaptersForSelectedManga(read = true)

    private fun markSelectedAsUnread() = markChaptersForSelectedManga(read = false)

    private fun markChaptersForSelectedManga(read: Boolean) {
        val mangaIds = _state.value.selectedManga
        if (mangaIds.isEmpty()) return
        viewModelScope.launch {
            val chapterIds = mangaIds.flatMap { mangaId ->
                chapterRepository.getChaptersByMangaIdSync(mangaId).map { it.id }
            }
            if (chapterIds.isNotEmpty()) {
                chapterRepository.updateChapterProgress(chapterIds, read = read, lastPageRead = 0)
            }
            clearSelection()
        }
    }

    private fun removeSelectedFromLibrary() {
        val ids = _state.value.selectedManga
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { mangaId -> toggleFavoriteManga(mangaId) }
            clearSelection()
        }
    }

    private fun downloadSelected() {
        val mangaIds = _state.value.selectedManga
        if (mangaIds.isEmpty()) return
        viewModelScope.launch {
            val mangaById = _allItems.value.associateBy { it.id }
            mangaIds.forEach { mangaId ->
                val manga = mangaById[mangaId] ?: return@forEach
                val chapters = chapterRepository.getChaptersByMangaId(mangaId).first()
                chapters.filter { !it.read }.forEach { chapter ->
                    downloadRepository.enqueueChapter(
                        mangaId = mangaId,
                        chapterId = chapter.id,
                        mangaTitle = manga.title,
                        chapterTitle = chapter.name
                    )
                }
            }
            clearSelection()
        }
    }

    private fun toggleFavorite(mangaId: Long) {
        viewModelScope.launch {
            toggleFavoriteManga(mangaId)
        }
    }

    private fun onFilterHasNotes(enabled: Boolean) {
        _state.update { it.copy(filterHasNotes = enabled) }
    }

    private fun onSetSortMode(mode: LibrarySortMode) {
        viewModelScope.launch {
            libraryPreferences.setLibrarySortMode(mode.ordinal)
        }
    }

    private fun onSetFilterMode(mode: LibraryFilterMode) {
        viewModelScope.launch {
            libraryPreferences.setLibraryFilterMode(mode.ordinal)
        }
    }

    private fun onSetFilterSource(sourceId: Long?) {
        viewModelScope.launch {
            libraryPreferences.setLibraryFilterSourceId(sourceId)
        }
    }

    private fun onToggleNsfw(show: Boolean) {
        viewModelScope.launch {
            generalPreferences.setShowNsfwContent(show)
        }
    }

    private fun observeContinueReading() {
        getContinueReading()
            .onEach { items -> _state.update { it.copy(continueReadingItems = items) } }
            .catch { e -> android.util.Log.w("LibraryViewModel", "observeContinueReading failed", e) }
            .launchIn(viewModelScope)
    }

    private fun observeGoalProgress() {
        combine(
            readingGoalPreferences.dailyChapterGoal,
            readingGoalPreferences.weeklyChapterGoal
        ) { daily, weekly -> Pair(daily, weekly) }
            .flatMapLatest { (dailyGoal, weeklyGoal) ->
                statisticsRepository.getReadingGoalProgress(dailyGoal, weeklyGoal)
            }
            .onEach { goal -> _state.update { it.copy(readingGoal = goal) } }
            .catch { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("LibraryViewModel", "observeGoalProgress failed", e)
            }
            .launchIn(viewModelScope)
    }

    private fun onContinueReadingClick(mangaId: Long, chapterId: Long) {
        viewModelScope.launch {
            _effect.send(LibraryEffect.NavigateToReader(mangaId, chapterId))
        }
    }

    private fun observeReadingLists() {
        readingListRepository.getAllLists()
            .map { lists ->
                lists.map { list ->
                    ReadingListFilterItem(
                        id = list.id,
                        name = list.name,
                        count = list.itemCount
                    )
                }
            }
            .onEach { items ->
                _state.update { state ->
                    val currentListId = state.filterReadingListId
                    val stillExists = currentListId == null || items.any { it.id == currentListId }
                    state.copy(
                        readingLists = items,
                        filterReadingListId = if (stillExists) currentListId else null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onSetFilterReadingList(listId: Long?) {
        _state.update { it.copy(filterReadingListId = listId) }
    }

    private fun Manga.toLibraryItem(
        isDownloaded: Boolean = false,
        hasTracking: Boolean = false
    ) = LibraryMangaItem(
        id = id,
        title = title,
        thumbnailUrl = thumbnailUrl,
        unreadCount = unreadCount,
        isFavorite = favorite,
        hasNote = !notes.isNullOrBlank(),
        sourceId = sourceId,
        isDownloaded = isDownloaded,
        hasTracking = hasTracking,
        isNsfw = contentRating == ContentRating.EROTICA || contentRating == ContentRating.PORNOGRAPHIC,
        lastRead = lastRead,
        dateAdded = dateAdded,
        status = status,
        totalChapterCount = totalChapters,
        userCompleted = userCompleted,
        userDropped = userDropped,
    )
}
