package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.otakureader.core.database.entity.PageBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageBookmarkDao {

    @Query("SELECT * FROM page_bookmarks WHERE manga_id = :mangaId ORDER BY created_at DESC")
    fun getBookmarksForManga(mangaId: Long): Flow<List<PageBookmarkEntity>>

    @Query("SELECT * FROM page_bookmarks WHERE chapter_id = :chapterId ORDER BY page_index ASC")
    fun getBookmarksForChapter(chapterId: Long): Flow<List<PageBookmarkEntity>>

    @Query("SELECT * FROM page_bookmarks WHERE chapter_id = :chapterId AND page_index = :pageIndex LIMIT 1")
    suspend fun getBookmark(chapterId: Long, pageIndex: Int): PageBookmarkEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM page_bookmarks WHERE chapter_id = :chapterId AND page_index = :pageIndex)")
    fun isPageBookmarked(chapterId: Long, pageIndex: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: PageBookmarkEntity): Long

    @Query("DELETE FROM page_bookmarks WHERE chapter_id = :chapterId AND page_index = :pageIndex")
    suspend fun deleteBookmark(chapterId: Long, pageIndex: Int)

    @Delete
    suspend fun deleteBookmark(bookmark: PageBookmarkEntity)

    @Query("DELETE FROM page_bookmarks WHERE manga_id = :mangaId")
    suspend fun deleteAllBookmarksForManga(mangaId: Long)

    @Query("SELECT COUNT(*) FROM page_bookmarks WHERE manga_id = :mangaId")
    fun getBookmarkCountForManga(mangaId: Long): Flow<Int>
}
