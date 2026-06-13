package app.otakureader.data.repository

import app.otakureader.core.database.dao.ReaderCommentDao
import app.otakureader.core.database.entity.ReaderCommentEntity
import app.otakureader.domain.model.ReaderComment
import app.otakureader.domain.repository.ReaderCommentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderCommentRepositoryImpl @Inject constructor(
    private val readerCommentDao: ReaderCommentDao,
) : ReaderCommentRepository {

    override fun observeBookComments(mangaId: Long): Flow<List<ReaderComment>> {
        return readerCommentDao.getBookComments(mangaId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeChapterComments(chapterId: Long): Flow<List<ReaderComment>> {
        return readerCommentDao.getChapterComments(chapterId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addComment(mangaId: Long, chapterId: Long?, body: String) {
        val now = System.currentTimeMillis()
        readerCommentDao.insert(
            ReaderCommentEntity(
                mangaId = mangaId,
                chapterId = chapterId,
                body = body,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun deleteComment(commentId: Long) {
        readerCommentDao.deleteById(commentId)
    }

    private fun ReaderCommentEntity.toDomain() = ReaderComment(
        id = id,
        mangaId = mangaId,
        chapterId = chapterId,
        body = body,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
