package app.otakureader.domain.repository

import app.otakureader.domain.model.BookmarkCollection
import kotlinx.coroutines.flow.Flow

interface BookmarkCollectionRepository {
    fun getAllCollections(): Flow<List<BookmarkCollection>>
    suspend fun addCollection(name: String): Long
    suspend fun renameCollection(id: Long, name: String)
    suspend fun deleteCollection(id: Long)
}
