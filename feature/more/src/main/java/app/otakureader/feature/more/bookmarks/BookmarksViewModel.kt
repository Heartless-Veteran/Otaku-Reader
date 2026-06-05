package app.otakureader.feature.more.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.repository.PageBookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Bookmarks screen.
 *
 * Exposes a [StateFlow<BookmarksState>] built reactively from the repository flow.
 * All user actions arrive through [onIntent] and are reduced synchronously (search/expand)
 * or launched as suspend operations (delete/navigate).
 *
 * Why [combine] + [mapLatest]?
 * - [pageBookmarkRepository.getAllBookmarks()] is a Room Flow that re-emits whenever the DB
 *   changes (e.g. the user just deleted a bookmark from inside the reader).
 * - [searchQuery] is a local StateFlow updated by the search bar.
 * - Combining them means the list automatically re-filters whenever either changes, with no
 *   manual "reload" calls needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val pageBookmarkRepository: PageBookmarkRepository,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) : ViewModel() {

    // One-shot navigation / snackbar events delivered to the screen.
    private val _effect = Channel<BookmarksEffect>(Channel.BUFFERED)
    val effect: Flow<BookmarksEffect> = _effect.receiveAsFlow()

    // Local UI state not derived from the DB (collapse flags).
    private val _collapsedManga = MutableStateFlow<Set<Long>>(emptySet())

    /** Enriched item list derived reactively from the repository. */
    private val enrichedBookmarks = pageBookmarkRepository.getAllBookmarks()
        .mapLatest { bookmarks ->
            if (bookmarks.isEmpty()) return@mapLatest emptyList()

            // Fetch all parent manga in a single batch call, then build a lookup map.
            val mangaMap = mangaRepository
                .getMangaByIds(bookmarks.map { it.mangaId }.distinct())
                .associate { manga -> manga.id to manga }

            // Fetch all chapters in parallel to avoid N+1 sequential DB queries.
            val chapterMap = coroutineScope {
                bookmarks.map { it.chapterId }.distinct()
                    .map { id -> async { id to chapterRepository.getChapterById(id) } }
                    .awaitAll()
                    .toMap()
            }

            bookmarks.map { bm ->
                val manga = mangaMap[bm.mangaId]
                BookmarkItem(
                    id = bm.id,
                    mangaId = bm.mangaId,
                    chapterId = bm.chapterId,
                    pageIndex = bm.pageIndex,
                    note = bm.note,
                    mangaTitle = manga?.title.orEmpty(),
                    chapterName = chapterMap[bm.chapterId]?.name.orEmpty(),
                    mangaCoverUrl = manga?.thumbnailUrl,
                )
            }
        }
        .catch { emit(emptyList()) }

    /** The mutable search query from the search bar. */
    private val _searchQuery = MutableStateFlow("")

    /**
     * UI state combining:
     * 1. The enriched bookmark list from Room.
     * 2. The current search query.
     * 3. Which manga groups are collapsed.
     */
    val state: StateFlow<BookmarksState> = combine(
        enrichedBookmarks,
        _searchQuery,
        _collapsedManga,
    ) { items, query, collapsed ->
        BookmarksState(
            isLoading = false,
            bookmarks = items,
            searchQuery = query,
            collapsedManga = collapsed,
        )
    }
        .catch { emit(BookmarksState(isLoading = false)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookmarksState(),
        )

    /** Single entry-point for all user actions. */
    fun onIntent(intent: BookmarksIntent) {
        when (intent) {
            is BookmarksIntent.SearchQueryChanged -> _searchQuery.value = intent.query

            is BookmarksIntent.ToggleMangaExpanded -> _collapsedManga.update { current ->
                if (intent.mangaId in current) current - intent.mangaId
                else current + intent.mangaId
            }

            is BookmarksIntent.DeleteBookmark -> deleteBookmark(intent.item)

            is BookmarksIntent.OpenBookmark -> viewModelScope.launch {
                _effect.send(
                    BookmarksEffect.NavigateToReader(intent.mangaId, intent.chapterId)
                )
            }
        }
    }

    private fun deleteBookmark(item: BookmarkItem) {
        viewModelScope.launch {
            try {
                pageBookmarkRepository.removeBookmark(item.chapterId, item.pageIndex)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _effect.send(BookmarksEffect.ShowSnackbar("Failed to delete bookmark"))
            }
        }
    }
}
