package app.otakureader.data.repository

import app.otakureader.core.database.dao.DataUsageDao
import app.otakureader.core.database.entity.DataUsageEntity
import app.otakureader.domain.model.DataUsageRecord
import app.otakureader.domain.repository.DataUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataUsageRepositoryImpl @Inject constructor(
    private val dao: DataUsageDao
) : DataUsageRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun today() = dateFormat.format(Date())

    private fun DataUsageEntity.toDomain() = DataUsageRecord(
        date = date,
        category = category,
        network = network,
        bytes = bytes,
    )

    override fun observeToday(): Flow<List<DataUsageRecord>> =
        dao.observeForDate(today()).map { entities -> entities.map { it.toDomain() } }

    override fun observeSince(startDate: String): Flow<List<DataUsageRecord>> =
        dao.observeSince(startDate).map { entities -> entities.map { it.toDomain() } }

    override fun totalBytesSince(startDate: String): Flow<Long> =
        dao.totalBytesSince(startDate).map { it ?: 0L }

    override suspend fun recordBytes(category: String, network: String, bytes: Long) {
        val date = today()
        val entity = DataUsageEntity(date = date, category = category, network = network, bytes = 0)
        dao.upsert(entity)
        dao.addBytes(date = date, category = category, network = network, delta = bytes)
    }
}
