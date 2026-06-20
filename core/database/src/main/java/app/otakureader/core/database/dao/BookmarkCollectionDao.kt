package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.otakureader.core.database.entity.BookmarkCollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkCollectionDao {
    @Query("SELECT * FROM bookmark_collections ORDER BY created_at ASC")
    fun getAllCollections(): Flow<List<BookmarkCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: BookmarkCollectionEntity): Long

    @Query("UPDATE bookmark_collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM bookmark_collections WHERE id = :id")
    suspend fun deleteById(id: Long)
}
