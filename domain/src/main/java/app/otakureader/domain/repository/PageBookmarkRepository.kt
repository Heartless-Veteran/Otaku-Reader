package app.otakureader.domain.repository

import app.otakureader.domain.model.PageBookmark
import kotlinx.coroutines.flow.Flow

interface PageBookmarkRepository {
    fun getBookmarksForManga(mangaId: Long): Flow<List<PageBookmark>>
    fun getBookmarksForChapter(chapterId: Long): Flow<List<PageBookmark>>
    suspend fun getBookmark(chapterId: Long, pageIndex: Int): PageBookmark?
    fun isPageBookmarked(chapterId: Long, pageIndex: Int): Flow<Boolean>
    suspend fun addBookmark(bookmark: PageBookmark): Long
    suspend fun removeBookmark(chapterId: Long, pageIndex: Int)
    suspend fun removeAllBookmarksForManga(mangaId: Long)
    fun getBookmarkCountForManga(mangaId: Long): Flow<Int>
}
