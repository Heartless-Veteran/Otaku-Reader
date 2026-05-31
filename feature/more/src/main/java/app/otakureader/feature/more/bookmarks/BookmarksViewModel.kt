package app.otakureader.feature.more.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.repository.PageBookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A single page bookmark enriched with the manga title and chapter name for display. */
data class BookmarkItem(
    val id: Long,
    val mangaId: Long,
    val chapterId: Long,
    val pageIndex: Int,
    val note: String?,
    val mangaTitle: String,
    val chapterName: String,
)

data class BookmarksState(
    val isLoading: Boolean = true,
    val bookmarks: List<BookmarkItem> = emptyList(),
) {
    /** Grouped by manga (preserving newest-first order of first appearance). */
    val grouped: Map<String, List<BookmarkItem>> get() = bookmarks.groupBy { it.mangaTitle }
    val isEmpty: Boolean get() = !isLoading && bookmarks.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val pageBookmarkRepository: PageBookmarkRepository,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) : ViewModel() {

    val state: StateFlow<BookmarksState> = pageBookmarkRepository.getAllBookmarks()
        .mapLatest { bookmarks ->
            val titles = mangaRepository.getMangaByIds(bookmarks.map { it.mangaId }.distinct())
                .associate { it.id to it.title }
            val items = bookmarks.map { bm ->
                BookmarkItem(
                    id = bm.id,
                    mangaId = bm.mangaId,
                    chapterId = bm.chapterId,
                    pageIndex = bm.pageIndex,
                    note = bm.note,
                    mangaTitle = titles[bm.mangaId].orEmpty(),
                    chapterName = chapterRepository.getChapterById(bm.chapterId)?.name.orEmpty(),
                )
            }
            BookmarksState(isLoading = false, bookmarks = items)
        }
        .catch { emit(BookmarksState(isLoading = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookmarksState())

    fun deleteBookmark(item: BookmarkItem) {
        viewModelScope.launch {
            pageBookmarkRepository.removeBookmark(item.chapterId, item.pageIndex)
        }
    }
}
