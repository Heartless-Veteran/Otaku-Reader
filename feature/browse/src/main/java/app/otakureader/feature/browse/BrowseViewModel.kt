package app.otakureader.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.extension.domain.repository.ExtensionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val extensionRepository: ExtensionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseState())
    val state = combine(
        _state,
        extensionRepository.getInstalledExtensions()
            .catch { emit(emptyList()) }
    ) { state, extensions ->
        // Extract source IDs from installed extensions
        val sourceIds = extensions.flatMap { ext ->
            ext.sources.map { "${ext.pkgName}:${it.id}" }
        }
        state.copy(
            sources = sourceIds,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BrowseState(isLoading = true),
    )

    private val _effect = Channel<BrowseEffect>()
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: BrowseEvent) {
        when (event) {
            is BrowseEvent.SelectSource -> _state.update { it.copy(currentSourceId = event.sourceId) }
            is BrowseEvent.OnMangaClick -> {
                val sourceId = _state.value.currentSourceId ?: return
                navigateToDetail(sourceId, event.manga.url)
            }
            is BrowseEvent.OnSearchQueryChange -> _state.update { it.copy(searchQuery = event.query) }
            is BrowseEvent.Search -> performSearch()
            is BrowseEvent.LoadNextPage -> { /* TODO: Phase 1 — paginate source results */ }
        }
    }

    private fun navigateToDetail(sourceId: String, mangaUrl: String) {
        viewModelScope.launch { _effect.send(BrowseEffect.NavigateToMangaDetail(sourceId, mangaUrl)) }
    }

    private fun performSearch() {
        // TODO: Phase 1 — implement search via source-api
    }
}
