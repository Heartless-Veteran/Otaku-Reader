package com.otakureader.data.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.otakureader.data.db.entities.MangaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    @Query("SELECT * FROM manga ORDER BY title ASC")
    fun getAllPaged(): PagingSource<Int, MangaEntity>

    @Query("SELECT * FROM manga WHERE categoryId = :categoryId ORDER BY title ASC")
    fun getByCategoryPaged(categoryId: String): PagingSource<Int, MangaEntity>

    @Query("SELECT * FROM manga WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchPaged(query: String): PagingSource<Int, MangaEntity>

    @Query("SELECT * FROM manga ORDER BY title ASC")
    fun getAll(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga WHERE id = :id")
    fun getById(id: Int): Flow<MangaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(manga: List<MangaEntity>)

    @Update
    suspend fun update(manga: MangaEntity)

    @Delete
    suspend fun delete(manga: MangaEntity)
}
