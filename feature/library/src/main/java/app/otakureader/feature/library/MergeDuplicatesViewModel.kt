package app.otakureader.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.repository.SourceRepository
import app.otakureader.domain.usecase.MergeDuplicateMangaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MergeDuplicatesViewModel @Inject constructor(
    private val mangaRepository: MangaRepository,
    private val sourceRepository: SourceRepository,
    private val mergeDuplicateManga: MergeDuplicateMangaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MergeDuplicatesState())
    val state: StateFlow<MergeDuplicatesState> = _state.asStateFlow()

    private val _effect = Channel<MergeDuplicatesEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeDuplicates()
        observeSourceNames()
    }

    fun onEvent(event: MergeDuplicatesEvent) {
        when (event) {
            is MergeDuplicatesEvent.LoadDuplicates -> observeDuplicates()
            is MergeDuplicatesEvent.SelectPrimary -> selectPrimary(event.groupIndex, event.primaryId)
            is MergeDuplicatesEvent.MergeGroup -> mergeGroup(event.group)
            is MergeDuplicatesEvent.MergeAll -> mergeAll()
        }
    }

    private fun observeDuplicates() {
        mangaRepository.findDuplicates()
            .onEach { duplicateLists ->
                _state.update { current ->
                    val newGroups = duplicateLists.map { entries ->
                        // Preserve user's primary selection for groups already shown
                        val existing = current.duplicateGroups.firstOrNull { g ->
                            g.entries.map { it.id }.toSet() == entries.map { it.id }.toSet()
                        }
                        existing?.copy(entries = entries) ?: DuplicateGroup(entries)
                    }
                    current.copy(isLoading = false, duplicateGroups = newGroups, error = null)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSourceNames() {
        sourceRepository.getSources()
            .onEach { sources ->
                val names = sources.associate { source ->
                    // MangaSource.id is the Long source ID serialized to String
                    val id = source.id.toLongOrNull() ?: return@associate source.id.hashCode().toLong() to source.name
                    id to source.name
                }
                _state.update { it.copy(sourceNames = names) }
            }
            .launchIn(viewModelScope)
    }

    private fun selectPrimary(groupIndex: Int, primaryId: Long) {
        _state.update { current ->
            val updated = current.duplicateGroups.toMutableList()
            if (groupIndex in updated.indices) {
                updated[groupIndex] = updated[groupIndex].copy(primaryId = primaryId)
            }
            current.copy(duplicateGroups = updated)
        }
    }

    private fun mergeGroup(group: DuplicateGroup) {
        viewModelScope.launch {
            _state.update { it.copy(isMerging = true) }
            try {
                val secondaryIds = group.entries.map { it.id }.filter { it != group.primaryId }
                mergeDuplicateManga(group.primaryId, secondaryIds)
                _effect.send(MergeDuplicatesEffect.ShowSnackbar("Merged ${secondaryIds.size + 1} entries into one"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(MergeDuplicatesEffect.ShowSnackbar("Merge failed: ${e.message}"))
            } finally {
                _state.update { it.copy(isMerging = false) }
            }
        }
    }

    private fun mergeAll() {
        val groups = _state.value.duplicateGroups
        if (groups.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isMerging = true) }
            var mergedCount = 0
            var failCount = 0
            for (group in groups) {
                try {
                    val secondaryIds = group.entries.map { it.id }.filter { it != group.primaryId }
                    mergeDuplicateManga(group.primaryId, secondaryIds)
                    mergedCount++
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failCount++
                }
            }
            _state.update { it.copy(isMerging = false) }
            val msg = if (failCount == 0) {
                "Merged $mergedCount duplicate group(s)"
            } else {
                "Merged $mergedCount group(s), $failCount failed"
            }
            _effect.send(MergeDuplicatesEffect.ShowSnackbar(msg))
            if (_state.value.duplicateGroups.isEmpty()) {
                _effect.send(MergeDuplicatesEffect.NavigateBack)
            }
        }
    }
}
