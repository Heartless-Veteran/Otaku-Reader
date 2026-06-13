package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.otakureader.core.database.entity.ReaderCommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReaderCommentDao {

    /** Book-level comments for a manga (chapter_id IS NULL), oldest first so it reads like a thread. */
    @Query("SELECT * FROM reader_comments WHERE manga_id = :mangaId AND chapter_id IS NULL ORDER BY created_at ASC")
    fun getBookComments(mangaId: Long): Flow<List<ReaderCommentEntity>>

    /** Chapter-level comments, oldest first. */
    @Query("SELECT * FROM reader_comments WHERE chapter_id = :chapterId ORDER BY created_at ASC")
    fun getChapterComments(chapterId: Long): Flow<List<ReaderCommentEntity>>

    @Insert
    suspend fun insert(comment: ReaderCommentEntity): Long

    @Query("DELETE FROM reader_comments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
