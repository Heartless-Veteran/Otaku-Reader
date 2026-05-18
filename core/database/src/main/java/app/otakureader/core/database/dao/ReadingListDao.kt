package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.otakureader.core.database.entity.ReadingListEntity
import app.otakureader.core.database.entity.ReadingListItemEntity
import app.otakureader.core.database.entity.ReadingListWithMangaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_lists ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllLists(): Flow<List<ReadingListEntity>>

    @Query("SELECT * FROM reading_lists WHERE id = :listId LIMIT 1")
    suspend fun getListById(listId: Long): ReadingListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ReadingListEntity): Long

    @Update
    suspend fun updateList(list: ReadingListEntity)

    @Delete
    suspend fun deleteList(list: ReadingListEntity)

    @Query("DELETE FROM reading_lists WHERE id = :listId")
    suspend fun deleteListById(listId: Long)

    @Transaction
    @Query("SELECT * FROM reading_lists WHERE id = :listId")
    fun getListWithManga(listId: Long): Flow<ReadingListWithMangaEntity?>

    @Query("SELECT * FROM reading_list_items WHERE listId = :listId ORDER BY sortOrder ASC, addedAt DESC")
    fun getItemsForList(listId: Long): Flow<List<ReadingListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMangaToList(item: ReadingListItemEntity)

    @Query("DELETE FROM reading_list_items WHERE listId = :listId AND mangaId = :mangaId")
    suspend fun removeMangaFromList(listId: Long, mangaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM reading_list_items WHERE listId = :listId AND mangaId = :mangaId)")
    suspend fun isMangaInList(listId: Long, mangaId: Long): Boolean

    @Query("SELECT * FROM reading_list_items WHERE mangaId = :mangaId")
    fun getListsForManga(mangaId: Long): Flow<List<ReadingListItemEntity>>

    @Query("UPDATE reading_list_items SET note = :note WHERE listId = :listId AND mangaId = :mangaId")
    suspend fun updateItemNote(listId: Long, mangaId: Long, note: String?)

    @Query("UPDATE reading_list_items SET sortOrder = :sortOrder WHERE listId = :listId AND mangaId = :mangaId")
    suspend fun updateItemSortOrder(listId: Long, mangaId: Long, sortOrder: Int)

    @Query("SELECT COUNT(*) FROM reading_list_items WHERE listId = :listId")
    fun getItemCount(listId: Long): Flow<Int>
}
