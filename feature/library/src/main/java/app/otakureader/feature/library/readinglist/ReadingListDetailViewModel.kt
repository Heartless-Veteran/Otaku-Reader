package app.otakureader.feature.library.readinglist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.core.navigation.Route
import app.otakureader.domain.repository.ReadingListRepository
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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
class ReadingListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val readingListRepository: ReadingListRepository,
) : ViewModel() {

    private val listId: Long = savedStateHandle.toRoute<Route.ReadingListDetail>().listId

    private val _state = MutableStateFlow(ReadingListDetailState())
    val state: StateFlow<ReadingListDetailState> = _state.asStateFlow()

    private val _effect = Channel<ReadingListDetailEffect>(Channel.BUFFERED)
    val effect: Flow<ReadingListDetailEffect> = _effect.receiveAsFlow()

    init {
        readingListRepository.getListWithManga(listId)
            .onEach { (list, manga) ->
                _state.update { it.copy(list = list, manga = manga, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ReadingListDetailEvent) {
        when (event) {
            is ReadingListDetailEvent.RemoveManga -> removeManga(event.mangaId)
            is ReadingListDetailEvent.OpenManga ->
                viewModelScope.launch { _effect.send(ReadingListDetailEffect.NavigateToManga(event.mangaId)) }
            is ReadingListDetailEvent.ExportAsCsv -> exportList(asCsv = true)
            is ReadingListDetailEvent.ExportAsJson -> exportList(asCsv = false)
        }
    }

    private fun exportList(asCsv: Boolean) {
        viewModelScope.launch {
            val list = _state.value.list ?: return@launch
            val manga = _state.value.manga
            val content = if (asCsv) buildCsv(manga) else buildJson(list.name, manga)
            val ext = if (asCsv) "csv" else "json"
            val mime = if (asCsv) "text/csv" else "application/json"
            val safeName = list.name.replace(Regex("[^A-Za-z0-9_\\-]"), "_")
            _effect.send(ReadingListDetailEffect.ShareExport(content, "$safeName.$ext", mime))
        }
    }

    private fun buildCsv(manga: List<app.otakureader.domain.model.ReadingListMangaItem>): String {
        val sb = StringBuilder()
        sb.appendLine("title,author,artist,status,unread_count,genres,note")
        manga.forEach { item ->
            val m = item.manga
            fun escape(s: String?) = "\"${(s ?: "").replace("\"", "\"\"")}\""
            sb.appendLine(
                listOf(
                    escape(m.title),
                    escape(m.author),
                    escape(m.artist),
                    escape(m.status.name),
                    m.unreadCount.toString(),
                    escape(m.genre.joinToString("; ")),
                    escape(item.note),
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    private fun buildJson(listName: String, manga: List<app.otakureader.domain.model.ReadingListMangaItem>): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"list\": ${jsonString(listName)},")
        sb.appendLine("  \"manga\": [")
        manga.forEachIndexed { idx, item ->
            val m = item.manga
            val trailing = if (idx < manga.size - 1) "," else ""
            sb.appendLine("    {")
            sb.appendLine("      \"title\": ${jsonString(m.title)},")
            sb.appendLine("      \"author\": ${jsonString(m.author)},")
            sb.appendLine("      \"artist\": ${jsonString(m.artist)},")
            sb.appendLine("      \"status\": ${jsonString(m.status.name)},")
            sb.appendLine("      \"unread_count\": ${m.unreadCount},")
            sb.appendLine("      \"genres\": [${m.genre.joinToString(", ") { jsonString(it) }}],")
            sb.appendLine("      \"note\": ${jsonString(item.note)}")
            sb.appendLine("    }$trailing")
        }
        sb.appendLine("  ]")
        sb.append("}")
        return sb.toString()
    }

    private fun jsonString(s: String?): String =
        if (s == null) "null" else "\"${s.replace("\\", "\\\\").replace("\"", "\\\"")}\""

    private fun removeManga(mangaId: Long) {
        viewModelScope.launch {
            try {
                readingListRepository.removeMangaFromList(listId, mangaId)
                _effect.send(ReadingListDetailEffect.ShowSnackbar("Removed from list"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(ReadingListDetailEffect.ShowSnackbar("Failed to remove: ${e.message}"))
            }
        }
    }
}
