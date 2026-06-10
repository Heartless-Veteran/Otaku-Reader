package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.otakureader.core.database.entity.MangaAlternativeSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaAlternativeSourceDao {

    @Query("SELECT * FROM manga_alternative_source WHERE manga_id = :mangaId OR alt_manga_id = :mangaId")
    fun getAlternativesForManga(mangaId: Long): Flow<List<MangaAlternativeSourceEntity>>

    @Query(
        """
        SELECT alt_manga_id FROM manga_alternative_source WHERE manga_id = :mangaId
        UNION
        SELECT manga_id FROM manga_alternative_source WHERE alt_manga_id = :mangaId
        """,
    )
    suspend fun getAlternativeIdsForMangaSync(mangaId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: MangaAlternativeSourceEntity)

    @Query(
        """
        DELETE FROM manga_alternative_source
        WHERE (manga_id = :mangaId AND alt_manga_id = :altMangaId)
           OR (manga_id = :altMangaId AND alt_manga_id = :mangaId)
        """,
    )
    suspend fun unlink(mangaId: Long, altMangaId: Long)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM manga_alternative_source
            WHERE (manga_id = :mangaId AND alt_manga_id = :altMangaId)
               OR (manga_id = :altMangaId AND alt_manga_id = :mangaId)
        )
        """,
    )
    suspend fun areLinked(mangaId: Long, altMangaId: Long): Boolean
}
