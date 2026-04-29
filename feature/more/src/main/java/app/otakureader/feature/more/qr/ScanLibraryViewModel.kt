package app.otakureader.feature.more.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.model.ShareableLibrary
import app.otakureader.domain.repository.MangaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [ScanLibraryScreen].
 *
 * Handles importing a scanned [ShareableLibrary] by matching each entry against
 * the local database via source ID + URL and adding matched manga to the user's
 * library (favorites). Entries not yet present in the local database are counted
 * as skipped — they cannot be imported without fetching from a live source.
 */
@HiltViewModel
class ScanLibraryViewModel @Inject constructor(
    private val mangaRepository: MangaRepository,
) : ViewModel() {

    sealed interface ImportState {
        data object Idle : ImportState
        data object Importing : ImportState
        data class Done(val imported: Int, val skipped: Int) : ImportState
        data class Error(val message: String) : ImportState
    }

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    /**
     * Attempts to import all manga from [library] into the user's library.
     *
     * For each entry, the local database is queried by source ID and URL.
     * If a match is found the manga is marked as a favourite; otherwise it is
     * counted as skipped (the source must be installed and browsed first).
     *
     * Calling this while an import is already in progress is a no-op.
     */
    fun importLibrary(library: ShareableLibrary) {
        if (_importState.value is ImportState.Importing) return
        viewModelScope.launch {
            _importState.value = ImportState.Importing
            var imported = 0
            var skipped = 0
            try {
                library.manga.forEach { item ->
                    val sourceId = item.sourceId.toLongOrNull()
                    val manga = if (sourceId != null) {
                        mangaRepository.getMangaBySourceAndUrl(sourceId, item.url)
                    } else {
                        null
                    }
                    if (manga != null) {
                        mangaRepository.addToFavorites(manga.id)
                        imported++
                    } else {
                        skipped++
                    }
                }
                _importState.value = ImportState.Done(imported, skipped)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Import failed")
            }
        }
    }
}
