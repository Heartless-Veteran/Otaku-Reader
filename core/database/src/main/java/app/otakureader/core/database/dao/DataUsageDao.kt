package app.otakureader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.otakureader.core.database.entity.DataUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DataUsageDao {

    @Upsert
    suspend fun upsert(entity: DataUsageEntity)

    @Query("""
        UPDATE data_usage SET bytes = bytes + :delta
        WHERE date = :date AND category = :category AND network = :network
    """)
    suspend fun addBytes(date: String, category: String, network: String, delta: Long)

    @Query("SELECT * FROM data_usage WHERE date = :date")
    fun observeForDate(date: String): Flow<List<DataUsageEntity>>

    @Query("SELECT * FROM data_usage WHERE date >= :startDate ORDER BY date ASC")
    fun observeSince(startDate: String): Flow<List<DataUsageEntity>>

    @Query("SELECT SUM(bytes) FROM data_usage WHERE date >= :startDate")
    fun totalBytesSince(startDate: String): Flow<Long?>

    @Query("DELETE FROM data_usage WHERE date < :cutoffDate")
    suspend fun pruneOlderThan(cutoffDate: String)
}
