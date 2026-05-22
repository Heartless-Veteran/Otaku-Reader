package app.otakureader.data.tracking.repository

import app.otakureader.core.database.dao.TrackEntryDao
import app.otakureader.domain.model.TrackEntry
import app.otakureader.domain.tracking.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepositoryImpl @Inject constructor(
    private val dao: TrackEntryDao,
) : TrackRepository {

    override fun observeEntriesForManga(mangaId: Long): Flow<List<TrackEntry>> =
        dao.getByMangaId(mangaId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getEntry(trackerId: Int, remoteId: Long): TrackEntry? =
        dao.getByTrackerAndRemote(trackerId, remoteId)?.toDomain()

    override suspend fun upsertEntry(entry: TrackEntry) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun deleteEntry(trackerId: Int, remoteId: Long) {
        dao.deleteByTrackerAndRemote(trackerId, remoteId)
    }
}
