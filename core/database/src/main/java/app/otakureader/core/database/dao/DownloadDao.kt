package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.otakureader.core.database.entity.DownloadEntity
import app.otakureader.core.database.entity.DownloadPageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing downloads.
 */
@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun observeAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE mangaId = :mangaId ORDER BY timestamp DESC")
    fun observeDownloadsByMangaId(mangaId: Long): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE chapterId = :chapterId LIMIT 1")
    suspend fun getDownloadByChapterId(chapterId: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Query("UPDATE downloads SET state = :state WHERE chapterId = :chapterId")
    suspend fun updateDownloadState(chapterId: Long, state: String)

    @Query(
        """UPDATE downloads
        SET downloadedPages = :downloadedPages,
            totalPages = :totalPages,
            progress = :progress
        WHERE chapterId = :chapterId"""
    )
    suspend fun updateDownloadProgress(
        chapterId: Long,
        downloadedPages: Int,
        totalPages: Int,
        progress: Int
    )

    @Query("UPDATE downloads SET error = :error WHERE chapterId = :chapterId")
    suspend fun updateDownloadError(chapterId: Long, error: String)

    @Query("DELETE FROM downloads WHERE chapterId = :chapterId")
    suspend fun deleteDownload(chapterId: Long)

    @Query("DELETE FROM downloads WHERE mangaId = :mangaId")
    suspend fun deleteDownloadsByMangaId(mangaId: Long)

    @Query("SELECT COUNT(*) > 0 FROM downloads WHERE chapterId = :chapterId AND state = 'Completed'")
    suspend fun isChapterDownloaded(chapterId: Long): Boolean

    // Page operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DownloadPageEntity>)

    @Query("SELECT * FROM download_pages WHERE chapterId = :chapterId ORDER BY pageIndex ASC")
    suspend fun getPagesByChapterId(chapterId: Long): List<DownloadPageEntity>

    @Query("SELECT * FROM download_pages WHERE chapterId = :chapterId ORDER BY pageIndex ASC")
    fun observePagesByChapterId(chapterId: Long): Flow<List<DownloadPageEntity>>

    @Query("UPDATE download_pages SET localPath = :localPath, downloaded = 1 WHERE id = :pageId")
    suspend fun updatePageLocalPath(pageId: Long, localPath: String)

    @Query("DELETE FROM download_pages WHERE chapterId = :chapterId")
    suspend fun deletePagesByChapterId(chapterId: Long)

    @Transaction
    suspend fun deleteDownloadWithPages(chapterId: Long) {
        deletePagesByChapterId(chapterId)
        deleteDownload(chapterId)
    }
}
