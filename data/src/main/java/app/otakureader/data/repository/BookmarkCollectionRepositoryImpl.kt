package app.otakureader.data.repository

import app.otakureader.core.database.dao.BookmarkCollectionDao
import app.otakureader.core.database.entity.BookmarkCollectionEntity
import app.otakureader.domain.model.BookmarkCollection
import app.otakureader.domain.repository.BookmarkCollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkCollectionRepositoryImpl @Inject constructor(
    private val bookmarkCollectionDao: BookmarkCollectionDao
) : BookmarkCollectionRepository {

    override fun getAllCollections(): Flow<List<BookmarkCollection>> {
        return bookmarkCollectionDao.getAllCollections().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addCollection(name: String): Long {
        return bookmarkCollectionDao.insert(BookmarkCollectionEntity(name = name))
    }

    override suspend fun renameCollection(id: Long, name: String) {
        bookmarkCollectionDao.rename(id, name)
    }

    override suspend fun deleteCollection(id: Long) {
        bookmarkCollectionDao.deleteById(id)
    }

    private fun BookmarkCollectionEntity.toDomain() = BookmarkCollection(
        id = id,
        name = name,
        createdAt = createdAt
    )
}
