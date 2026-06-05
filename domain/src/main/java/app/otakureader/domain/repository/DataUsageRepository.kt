package app.otakureader.domain.repository

import app.otakureader.domain.model.DataUsageRecord
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for recording and querying network data usage.
 * Implementations live in :data and aggregate bytes by date, category, and network type.
 */
interface DataUsageRepository {
    fun observeToday(): Flow<List<DataUsageRecord>>
    fun observeSince(startDate: String): Flow<List<DataUsageRecord>>
    fun totalBytesSince(startDate: String): Flow<Long>
    suspend fun recordBytes(category: String, network: String, bytes: Long)
}
