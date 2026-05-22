package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.otakureader.core.database.entity.TrackEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackEntryDao {

    @Query("SELECT * FROM track_entries WHERE manga_id = :mangaId")
    fun getByMangaId(mangaId: Long): Flow<List<TrackEntryEntity>>

    @Query("SELECT * FROM track_entries WHERE tracker_id = :trackerId AND remote_id = :remoteId LIMIT 1")
    suspend fun getByTrackerAndRemote(trackerId: Int, remoteId: Long): TrackEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TrackEntryEntity): Long

    @Query("DELETE FROM track_entries WHERE tracker_id = :trackerId AND remote_id = :remoteId")
    suspend fun deleteByTrackerAndRemote(trackerId: Int, remoteId: Long)
}
