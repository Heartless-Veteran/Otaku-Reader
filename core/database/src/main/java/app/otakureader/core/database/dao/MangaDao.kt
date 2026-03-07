package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.otakureader.core.database.entity.MangaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    @Query("SELECT * FROM manga WHERE favorite = 1 ORDER BY title ASC")
    fun getFavoriteManga(): Flow<List<MangaEntity>>
    
    @Query("SELECT * FROM manga WHERE id = :id")
    suspend fun getMangaById(id: Long): MangaEntity?
    
    @Query("SELECT * FROM manga WHERE id = :id")
    fun getMangaByIdFlow(id: Long): Flow<MangaEntity?>
    
    @Query("SELECT * FROM manga WHERE sourceId = :sourceId AND url = :url")
    suspend fun getMangaBySourceAndUrl(sourceId: Long, url: String): MangaEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(manga: MangaEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(manga: List<MangaEntity>)
    
    @Update
    suspend fun update(manga: MangaEntity)
    
    @Delete
    suspend fun delete(manga: MangaEntity)
    
    @Query("DELETE FROM manga WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("UPDATE manga SET favorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, favorite: Boolean)
    
    @Query("SELECT EXISTS(SELECT 1 FROM manga WHERE id = :id AND favorite = 1)")
    fun isFavorite(id: Long): Flow<Boolean>
    
    @Query("SELECT COUNT(*) FROM manga WHERE favorite = 1")
    fun getFavoriteMangaCount(): Flow<Int>
}
