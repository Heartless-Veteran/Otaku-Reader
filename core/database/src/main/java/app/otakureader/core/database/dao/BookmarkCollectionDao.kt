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

    // ABORT (not REPLACE): REPLACE performs delete+insert which triggers ON DELETE SET NULL
    // on child page_bookmarks rows, silently detaching all bookmarks from the collection.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(collection: BookmarkCollectionEntity): Long

    @Query("UPDATE bookmark_collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM bookmark_collections WHERE id = :id")
    suspend fun deleteById(id: Long)
}
