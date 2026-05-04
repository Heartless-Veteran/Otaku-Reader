package app.otakureader.data.backup.tachiyomi

import androidx.room.withTransaction
import app.otakureader.core.database.OtakuReaderDatabase
import app.otakureader.core.database.dao.CategoryDao
import app.otakureader.core.database.dao.ChapterDao
import app.otakureader.core.database.dao.MangaDao
import app.otakureader.core.database.entity.CategoryEntity
import app.otakureader.core.database.entity.ChapterEntity
import app.otakureader.core.database.entity.MangaCategoryEntity
import app.otakureader.core.database.entity.MangaEntity
import app.otakureader.domain.model.MangaStatus
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Imports Tachiyomi/Mihon backup JSON into Otaku-Reader's database.
 *
 * @param database Room database instance
 * @param mangaDao Manga DAO
 * @param chapterDao Chapter DAO
 * @param categoryDao Category DAO
 */
class TachiyomiBackupImporter @Inject constructor(
    private val database: OtakuReaderDatabase,
    private val mangaDao: MangaDao,
    private val chapterDao: ChapterDao,
    private val categoryDao: CategoryDao,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * Imports a Tachiyomi backup from JSON string.
     *
     * @param backupJson Tachiyomi backup JSON string
     * @return ImportResult with statistics
     */
    @Suppress("LongMethod")
    suspend fun importBackup(backupJson: String): ImportResult {
        val backup = json.decodeFromString<TachiyomiBackup>(backupJson)

        var mangaImported = 0
        var chaptersImported = 0
        var categoriesImported = 0
        var skipped = 0

        database.withTransaction {
            // Import categories first (so they exist for manga associations)
            val categoryIdMap = mutableMapOf<Long, Long>() // old ID -> new ID
            backup.categories.forEach { tachiyomiCategory ->
                val entity = CategoryEntity(
                    id = 0, // auto-generate
                    name = tachiyomiCategory.name,
                    order = tachiyomiCategory.order,
                    flags = tachiyomiCategory.flags.toInt()
                )
                val newId = categoryDao.insert(entity)
                tachiyomiCategory.id?.let { oldId ->
                    categoryIdMap[oldId] = newId
                }
                categoriesImported++
            }

            // Import manga
            val mangaIdMap = mutableMapOf<Long, Long>() // old ID -> new ID
            backup.manga.forEach { tachiyomiManga ->
                // Check if already exists by source+url
                val existing = mangaDao.getMangaBySourceAndUrl(
                    tachiyomiManga.source,
                    tachiyomiManga.url
                )

                val entity = MangaEntity(
                    id = existing?.id ?: 0,
                    sourceId = tachiyomiManga.source,
                    url = tachiyomiManga.url,
                    title = tachiyomiManga.title,
                    artist = tachiyomiManga.artist,
                    author = tachiyomiManga.author,
                    description = tachiyomiManga.description,
                    genre = tachiyomiManga.genre.joinToString(","),
                    status = tachiyomiStatusToDomain(tachiyomiManga.status).ordinal,
                    thumbnailUrl = tachiyomiManga.thumbnailUrl,
                    favorite = tachiyomiManga.favorite,
                    lastUpdate = tachiyomiManga.lastUpdate,
                    initialized = existing?.initialized ?: false,
                    viewerFlags = tachiyomiManga.viewerFlags,
                    chapterFlags = tachiyomiManga.chapterFlags,
                    coverLastModified = tachiyomiManga.coverLastModified,
                    dateAdded = tachiyomiManga.dateAdded,
                )

                val newId = if (existing != null) {
                    mangaDao.update(entity)
                    skipped++
                    existing.id
                } else {
                    mangaDao.insert(entity)
                    mangaImported++
                    entity.id
                }

                tachiyomiManga.id?.let { oldId ->
                    mangaIdMap[oldId] = newId
                }
            }

            // Import chapters
            backup.chapters.forEach { tachiyomiChapter ->
                // Map chapter's mangaId to our imported manga ID
                val mangaId = mangaIdMap[tachiyomiChapter.mangaId]

                // Skip chapters whose manga wasn't imported (e.g. ID mapping failure)
                if (mangaId != null) {
                    val existingChapters = chapterDao.getChaptersByMangaId(mangaId).first()
                    val existing = existingChapters.find { it.url == tachiyomiChapter.url }

                    val entity = ChapterEntity(
                        id = existing?.id ?: 0,
                        mangaId = mangaId,
                        url = tachiyomiChapter.url,
                        name = tachiyomiChapter.name,
                        read = tachiyomiChapter.read,
                        bookmark = tachiyomiChapter.bookmark,
                        lastPageRead = tachiyomiChapter.lastPageRead,
                        chapterNumber = tachiyomiChapter.chapterNumber ?: -1f,
                        sourceOrder = tachiyomiChapter.sourceOrder,
                        dateFetch = tachiyomiChapter.dateFetch,
                        dateUpload = tachiyomiChapter.dateUpload,
                        lastModified = 0
                    )

                    if (existing != null) {
                        chapterDao.update(entity)
                    } else {
                        chapterDao.insert(entity)
                        chaptersImported++
                    }
                }
            }

            // Restore manga-category associations
            // Tachiyomi stores category IDs per manga in a separate structure
            // For now, we'll skip this since the mapping isn't directly in the manga object
        }

        return ImportResult(
            mangaImported = mangaImported,
            chaptersImported = chaptersImported,
            categoriesImported = categoriesImported,
            skipped = skipped,
            totalManga = backup.manga.size,
            totalChapters = backup.chapters.size
        )
    }

    private fun tachiyomiStatusToDomain(status: Int): MangaStatus {
        return when (status) {
            1 -> MangaStatus.ONGOING
            2 -> MangaStatus.COMPLETED
            3 -> MangaStatus.LICENSED
            4 -> MangaStatus.PUBLISHING_FINISHED
            5 -> MangaStatus.CANCELLED
            6 -> MangaStatus.ON_HIATUS
            else -> MangaStatus.UNKNOWN
        }
    }
}

/**
 * Result of a Tachiyomi backup import operation.
 */
data class ImportResult(
    val mangaImported: Int,
    val chaptersImported: Int,
    val categoriesImported: Int,
    val skipped: Int,
    val totalManga: Int,
    val totalChapters: Int
) {
    val success: Boolean
        get() = mangaImported > 0 || skipped > 0
}
