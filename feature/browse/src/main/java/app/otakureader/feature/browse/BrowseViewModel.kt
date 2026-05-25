package app.otakureader.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.core.ui.selection.SelectionManager
import app.otakureader.domain.repository.FeedRepository
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.usecase.library.AddMangaToLibraryUseCase
import app.otakureader.domain.usecase.ToggleFavoriteMangaUseCase
import app.otakureader.domain.usecase.source.GetLatestUpdatesUseCase
import app.otakureader.domain.usecase.source.GetPopularMangaUseCase
import app.otakureader.domain.usecase.source.GetSourceFiltersUseCase
import app.otakureader.domain.usecase.source.GetSourcesUseCase
import app.otakureader.domain.usecase.source.SearchMangaUseCase
import app.otakureader.sourceapi.FilterList
import app.otakureader.sourceapi.MangaSource
import app.otakureader.sourceapi.SourceManga
import app.otakureader.sourceapi.toSourceId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val getSourcesUseCase: GetSourcesUseCase,
    private val getPopularMangaUseCase: GetPopularMangaUseCase,
    private val getLatestUpdatesUseCase: GetLatestUpdatesUseCase,
    private val searchMangaUseCase: SearchMangaUseCase,
    private val getSourceFiltersUseCase: GetSourceFiltersUseCase,
    private val addMangaToLibraryUseCase: AddMangaToLibraryUseCase,
    private val toggleFavoriteMangaUseCase: ToggleFavoriteMangaUseCase,
    private val mangaRepository: MangaRepository,
    private val feedRepository: FeedRepository,
    private val generalPreferences: GeneralPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseState())
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BrowseState(),
    )

    private val _sources = MutableStateFlow<List<MangaSource>>(emptyList())

    /** Atomic selection manager — keys are manga URLs. */
    private val selection = SelectionManager<String>()

    private val _effect = Channel<BrowseEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        // Collect sources and filter by NSFW preference
        viewModelScope.launch {
            combine(
                getSourcesUseCase(),
                generalPreferences.showNsfwContent
            ) { sources, showNsfw ->
                if (showNsfw) {
                    sources
                } else {
                    sources.filter { !it.isNsfw }
                }
            }.collect { filteredSources ->
                _sources.value = filteredSources
                _state.update { it.copy(sources = filteredSources.map { s -> s.id }) }
            }
        }
        observeSavedSearches()
        observeLibraryFavorites()
        // Sync selection manager into state so UI recomposes
        combine(selection.selected, selection.isActive) { ids, active ->
            ids to active
        }.onEach { (ids, active) ->
            _state.update { it.copy(selectedManga = ids, isBulkSelectionMode = active) }
        }.launchIn(viewModelScope)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun onEvent(event: BrowseEvent) {
        when (event) {
            is BrowseEvent.SelectSource -> {
                _state.update {
                    it.copy(
                        currentSourceId = event.sourceId,
                        searchQuery = "",
                        availableFilters = FilterList(),
                        activeFilters = FilterList(),
                        hasSearchResults = false,
                        searchResults = emptyList()
                    )
                }
                loadPopularManga(event.sourceId)
                loadSourceFilters(event.sourceId)
            }
            is BrowseEvent.OnMangaClick -> {
                val sourceId = _state.value.currentSourceId ?: return
                navigateToDetail(sourceId, event.manga.url)
            }
            is BrowseEvent.OnSearchQueryChange -> {
                _state.update {
                    it.copy(
                        searchQuery = event.query,
                        hasSearchResults = if (event.query.isBlank()) false else it.hasSearchResults
                    )
                }
            }
            is BrowseEvent.Search -> performSearch()
            is BrowseEvent.LoadNextPage -> loadNextPage()
            is BrowseEvent.RefreshSources -> refreshSources()
            is BrowseEvent.LoadLatest -> loadLatestUpdates()
            is BrowseEvent.ToggleFilterSheet -> {
                _state.update { it.copy(showFilterSheet = !it.showFilterSheet) }
            }
            is BrowseEvent.UpdateFilter -> {
                val currentFilters = _state.value.activeFilters.filters.toMutableList()
                if (event.index < currentFilters.size) {
                    currentFilters[event.index] = event.filter
                    _state.update { it.copy(activeFilters = FilterList(currentFilters)) }
                }
            }
            is BrowseEvent.ResetFilters -> {
                _state.update {
                    it.copy(activeFilters = it.availableFilters)
                }
            }
            is BrowseEvent.ApplyFilters -> {
                _state.update { it.copy(showFilterSheet = false) }
                performSearch()
            }

            // Bulk favorite events
            is BrowseEvent.OnMangaLongClick -> {
                toggleMangaSelection(event.manga)
            }
            is BrowseEvent.ToggleMangaSelection -> {
                toggleMangaSelection(event.manga)
            }
            is BrowseEvent.ClearSelection -> {
                selection.clear()
            }
            is BrowseEvent.AddSelectedToLibrary -> {
                addSelectedToLibrary()
            }
            is BrowseEvent.ExitBulkSelectionMode -> {
                selection.clear()
            }
            is BrowseEvent.LongClickManga -> quickToggleFavorite(event.manga)
            is BrowseEvent.SaveCurrentSearch -> saveCurrentSearch()
            is BrowseEvent.DeleteSavedSearch -> deleteSavedSearch(event.searchId)
            is BrowseEvent.ApplySavedSearch -> applySavedSearch(event.search)
        }
    }

    private fun loadPopularManga(sourceId: String, page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getPopularMangaUseCase(sourceId, page)
                .onSuccess { mangaPage ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            popularManga = if (page == 1) mangaPage.mangas else it.popularManga + mangaPage.mangas,
                            hasNextPage = mangaPage.hasNextPage,
                            currentPage = page,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load manga"
                        )
                    }
                }
        }
    }

    private fun loadSourceFilters(sourceId: String) {
        viewModelScope.launch {
            val filters = getSourceFiltersUseCase(sourceId)
            _state.update {
                it.copy(
                    availableFilters = filters,
                    activeFilters = filters
                )
            }
        }
    }

    private fun performSearch() {
        val query = _state.value.searchQuery
        val sourceId = _state.value.currentSourceId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, hasSearchResults = true, error = null) }
            searchMangaUseCase(sourceId, query, 1, _state.value.activeFilters)
                .onSuccess { mangaPage ->
                    _state.update {
                        it.copy(
                            isSearching = false,
                            searchResults = mangaPage.mangas,
                            hasNextPage = mangaPage.hasNextPage,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSearching = false,
                            hasSearchResults = false,
                            error = error.message ?: "Search failed"
                        )
                    }
                }
        }
    }

    private fun loadLatestUpdates() {
        val sourceId = _state.value.currentSourceId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getLatestUpdatesUseCase(sourceId, 1)
                .onSuccess { mangaPage ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            popularManga = mangaPage.mangas,
                            hasNextPage = mangaPage.hasNextPage,
                            currentPage = 1,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load latest updates"
                        )
                    }
                }
        }
    }

    private fun loadNextPage() {
        val currentState = _state.value
        if (!currentState.hasNextPage || currentState.isLoading) return

        val sourceId = currentState.currentSourceId ?: return

        if (currentState.searchQuery.isNotBlank()) {
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true) }
                searchMangaUseCase(
                    sourceId,
                    currentState.searchQuery,
                    currentState.currentPage + 1,
                    currentState.activeFilters
                )
                    .onSuccess { mangaPage ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                searchResults = it.searchResults + mangaPage.mangas,
                                hasNextPage = mangaPage.hasNextPage,
                                currentPage = it.currentPage + 1
                            )
                        }
                    }
                    .onFailure {
                        _state.update { it.copy(isLoading = false) }
                    }
            }
        } else {
            // Load next page of popular manga
            loadPopularManga(sourceId, currentState.currentPage + 1)
        }
    }

    private fun refreshSources() {
        viewModelScope.launch {
            // Sources are automatically refreshed through the flow
            _effect.send(BrowseEffect.ShowSnackbar("Sources refreshed"))
        }
    }

    private fun navigateToDetail(sourceId: String, mangaUrl: String) {
        viewModelScope.launch {
            _effect.send(BrowseEffect.NavigateToMangaDetail(sourceId, mangaUrl))
        }
    }

    /**
     * Helper to get the currently selected source.
     * Kept for future refactoring opportunities.
     */
    @Suppress("UnusedPrivateMember")
    private fun getCurrentSource(): MangaSource? {
        val sourceId = _state.value.currentSourceId ?: return null
        return _sources.value.find { it.id == sourceId }
    }

    /**
     * Toggles selection of a manga for bulk favorite.
     * Enables bulk selection mode when first manga is selected.
     */
    private fun toggleMangaSelection(manga: SourceManga) {
        selection.toggle(manga.url)
    }
    
    /**
     * Adds all selected manga to the library.
     * Shows success/failure message and optionally navigates to library.
     */
    private fun addSelectedToLibrary() {
        viewModelScope.launch {
            val sourceId = _state.value.currentSourceId ?: return@launch
            val selectedUrls = selection.snapshotAndClear()
            if (selectedUrls.isEmpty()) return@launch

            // Look up SourceManga from available lists by URL
            val allManga = _state.value.popularManga + _state.value.searchResults
            val selected = selectedUrls.mapNotNull { url -> allManga.find { it.url == url } }
            
            if (selected.isEmpty()) return@launch
            
            addMangaToLibraryUseCase(selected, sourceId)
                .onSuccess { addedCount ->
                    _effect.send(BrowseEffect.ShowSnackbar("$addedCount manga added to library"))
                    _effect.send(BrowseEffect.NavigateToLibrary)
                }
                .onFailure { error ->
                    _effect.send(BrowseEffect.ShowSnackbar("Failed to add manga: ${error.message}"))
                }
        }
    }

    // --- Quick Favorite Toggle (long-click) ---

    /**
     * Keeps [BrowseState.favoritedMangaUrls] up-to-date by observing the library.
     * This lets the UI indicate which browse results are already in the library.
     */
    private fun observeLibraryFavorites() {
        mangaRepository.getLibraryManga()
            .map { mangaList -> mangaList.map { it.url }.toSet() }
            .onEach { urls -> _state.update { it.copy(favoritedMangaUrls = urls) } }
            .launchIn(viewModelScope)
    }

    /**
     * Quickly adds or removes a single [SourceManga] from the library without
     * entering bulk selection mode. Shows a snackbar confirming the action.
     */
    private fun quickToggleFavorite(manga: app.otakureader.sourceapi.SourceManga) {
        val sourceId = _state.value.currentSourceId ?: return
        viewModelScope.launch {
            val alreadyFavorited = manga.url in _state.value.favoritedMangaUrls
            if (alreadyFavorited) {
                // Look up the DB record to get the ID needed for toggleFavorite
                val dbManga = mangaRepository.getMangaBySourceAndUrl(
                    sourceId = sourceId.toSourceId(),
                    url = manga.url
                )
                if (dbManga != null) {
                    toggleFavoriteMangaUseCase(dbManga.id)
                    _effect.send(BrowseEffect.ShowSnackbar("Removed from library"))
                }
            } else {
                addMangaToLibraryUseCase(manga, sourceId)
                    .onSuccess { _effect.send(BrowseEffect.ShowSnackbar("Added to library")) }
                    .onFailure { _effect.send(BrowseEffect.ShowSnackbar("Failed to add to library")) }
            }
        }
    }

    // --- Saved Searches ---

    private fun observeSavedSearches() {
        combine(
            feedRepository.getSavedSearches(),
            _state.map { it.currentSourceId }.distinctUntilChanged()
        ) { searches, sourceId ->
            if (sourceId != null) searches.filter { it.sourceName == sourceId }
            else searches
        }
            .onEach { filtered -> _state.update { it.copy(savedSearches = filtered) } }
            .launchIn(viewModelScope)
    }

    private fun saveCurrentSearch() {
        val state = _state.value
        val sourceId = state.currentSourceId ?: return
        val query = state.searchQuery.trim()
        if (query.isBlank()) {
            viewModelScope.launch { _effect.send(BrowseEffect.ShowSnackbar("Enter a search query to save")) }
            return
        }
        viewModelScope.launch {
            runCatching {
                feedRepository.addSavedSearch(
                    sourceId = sourceId.toLongOrNull() ?: 0L,
                    sourceName = sourceId,
                    query = query,
                    filters = emptyMap(),
                )
            }.onSuccess {
                _effect.send(BrowseEffect.ShowSnackbar("Search saved"))
            }.onFailure {
                _effect.send(BrowseEffect.ShowSnackbar("Failed to save search"))
            }
        }
    }

    private fun deleteSavedSearch(searchId: Long) {
        viewModelScope.launch {
            runCatching { feedRepository.removeSavedSearch(searchId) }
        }
    }

    private fun applySavedSearch(search: app.otakureader.domain.model.FeedSavedSearch) {
        _state.update { it.copy(searchQuery = search.query) }
        performSearch()
    }
}
