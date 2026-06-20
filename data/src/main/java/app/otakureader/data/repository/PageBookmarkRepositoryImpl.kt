package app.otakureader.data.repository

import app.otakureader.core.database.dao.PageBookmarkDao
import app.otakureader.core.database.entity.PageBookmarkEntity
import app.otakureader.domain.model.PageBookmark
import app.otakureader.domain.repository.PageBookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PageBookmarkRepositoryImpl @Inject constructor(
    private val pageBookmarkDao: PageBookmarkDao
) : PageBookmarkRepository {

    override fun getAllBookmarks(): Flow<List<PageBookmark>> {
        return pageBookmarkDao.getAllBookmarks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getBookmarksForManga(mangaId: Long): Flow<List<PageBookmark>> {
        return pageBookmarkDao.getBookmarksForManga(mangaId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getBookmarksForChapter(chapterId: Long): Flow<List<PageBookmark>> {
        return pageBookmarkDao.getBookmarksForChapter(chapterId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getBookmark(chapterId: Long, pageIndex: Int): PageBookmark? {
        return pageBookmarkDao.getBookmark(chapterId, pageIndex)?.toDomain()
    }

    override fun isPageBookmarked(chapterId: Long, pageIndex: Int): Flow<Boolean> {
        return pageBookmarkDao.isPageBookmarked(chapterId, pageIndex)
    }

    override suspend fun addBookmark(bookmark: PageBookmark): Long {
        return pageBookmarkDao.insertBookmark(bookmark.toEntity())
    }

    override suspend fun removeBookmark(chapterId: Long, pageIndex: Int) {
        pageBookmarkDao.deleteBookmark(chapterId, pageIndex)
    }

    override suspend fun removeAllBookmarksForManga(mangaId: Long) {
        pageBookmarkDao.deleteAllBookmarksForManga(mangaId)
    }

    override fun getBookmarkCountForManga(mangaId: Long): Flow<Int> {
        return pageBookmarkDao.getBookmarkCountForManga(mangaId)
    }

    override fun getBookmarksByCollection(collectionId: Long): Flow<List<PageBookmark>> {
        return pageBookmarkDao.getBookmarksByCollection(collectionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    private fun PageBookmarkEntity.toDomain() = PageBookmark(
        id = id,
        mangaId = mangaId,
        chapterId = chapterId,
        pageIndex = pageIndex,
        note = note,
        createdAt = createdAt,
        collectionId = collectionId
    )

    private fun PageBookmark.toEntity() = PageBookmarkEntity(
        id = id,
        mangaId = mangaId,
        chapterId = chapterId,
        pageIndex = pageIndex,
        note = note,
        createdAt = createdAt,
        collectionId = collectionId
    )
}
