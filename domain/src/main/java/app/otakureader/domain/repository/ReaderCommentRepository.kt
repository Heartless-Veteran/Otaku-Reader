package app.otakureader.domain.repository

import app.otakureader.domain.model.ReaderComment
import kotlinx.coroutines.flow.Flow

interface ReaderCommentRepository {
    /** Book-level comments for a manga, oldest first. */
    fun observeBookComments(mangaId: Long): Flow<List<ReaderComment>>

    /** Chapter-level comments, oldest first. */
    fun observeChapterComments(chapterId: Long): Flow<List<ReaderComment>>

    /** Adds a comment; a null [chapterId] creates a book-level comment. */
    suspend fun addComment(mangaId: Long, chapterId: Long?, body: String)

    suspend fun deleteComment(commentId: Long)
}
