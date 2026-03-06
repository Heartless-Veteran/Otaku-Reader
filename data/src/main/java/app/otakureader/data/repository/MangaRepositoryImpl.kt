package app.otakureader.data.repository

import app.otakureader.core.database.dao.MangaDao
import app.otakureader.core.database.entity.MangaEntity
import app.otakureader.data.mapper.toEntity
import app.otakureader.data.mapper.toManga
import app.otakureader.domain.model.LibraryManga
import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaRepositoryImpl @Inject constructor(
    private val mangaDao: MangaDao
) : MangaRepository {

    override fun observeLibrary(): Flow<List<LibraryManga>> =
        mangaDao.observeLibrary().map { entities ->
            entities.map { entity ->
                LibraryManga(manga = entity.toManga())
            }
        }

    override fun observeManga(id: Long): Flow<Manga?> =
        mangaDao.observeManga(id).map { it?.toManga() }

    override suspend fun getMangaBySourceAndUrl(sourceId: String, url: String): Manga? =
        mangaDao.getMangaBySourceAndUrl(sourceId, url)?.toManga()

    override suspend fun upsertManga(manga: Manga): Long =
        mangaDao.upsert(manga.toEntity())

    override suspend fun setFavorite(id: Long, favorite: Boolean) =
        mangaDao.setFavorite(id, favorite)

    override suspend fun deleteManga(id: Long) =
        mangaDao.delete(id)

    override fun searchLibrary(query: String): Flow<List<LibraryManga>> =
        mangaDao.searchLibrary(query).map { entities ->
            entities.map { entity -> LibraryManga(manga = entity.toManga()) }
        }
}
