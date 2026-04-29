package app.otakureader.data.backup.tachiyomi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tachiyomi/Mihon backup JSON model.
 * 
 * Tachiyomi backup format (version 2+):
 * ```json
 * {
 *   "version": 2,
 *   "manga": [...],
 *   "categories": [...],
 *   "extensions": [...],
 *   "chapters": [...],
 *   "tracking": [...],
 *   "history": [...],
 *   "updates": [...]
 * }
 * ```
 */
@Serializable
class TachiyomiBackup(
    val version: Int = 2,
    val manga: List<TachiyomiManga> = emptyList(),
    val categories: List<TachiyomiCategory> = emptyList(),
    val extensions: List<TachiyomiExtension> = emptyList(),
    val chapters: List<TachiyomiChapter> = emptyList(),
    val tracking: List<TachiyomiTracking> = emptyList(),
    val history: List<TachiyomiHistory> = emptyList(),
    val updates: List<TachiyomiUpdate> = emptyList()
)

@Serializable
class TachiyomiManga(
    val id: Long? = null,
    val source: Long,
    val url: String,
    val title: String,
    val artist: String? = null,
    val author: String? = null,
    val description: String? = null,
    val genre: List<String> = emptyList(),
    val status: Int = 0, // 0=unknown, 1=ongoing, 2=completed, 3=licensed, 4=publishing finished, 5=cancelled, 6=on hiatus
    val thumbnailUrl: String? = null,
    val favorite: Boolean = true,
    val lastUpdate: Long = 0,
    val nextUpdate: Long? = null,
    val calculateInterval: Int = 0,
    val dateAdded: Long = 0,
    val viewerFlags: Int = 0,
    val chapterFlags: Int = 0,
    val coverLastModified: Long = 0,
    val filteredScanlators: String? = null
)

@Serializable
class TachiyomiCategory(
    val id: Long? = null,
    val name: String,
    val order: Int = 0,
    val flags: Long = 0
)

@Serializable
class TachiyomiExtension(
    val name: String,
    val pkgName: String,
    val version: String
)

@Serializable
class TachiyomiChapter(
    val id: Long? = null,
    val mangaId: Long,
    val url: String,
    val name: String,
    val read: Boolean = false,
    val bookmark: Boolean = false,
    val lastPageRead: Int = 0,
    val chapterNumber: Float? = null,
    val sourceOrder: Int = 0,
    val dateFetch: Long = 0,
    val dateUpload: Long = 0
)

@Serializable
class TachiyomiTracking(
    val id: Long? = null,
    val mangaId: Long,
    val syncId: Int,
    val remoteId: Long,
    val libraryId: Long? = null,
    val title: String? = null,
    val lastChapterRead: Int? = null,
    val totalChapters: Int? = null,
    val score: Float? = null,
    val status: Int? = null,
    val startedReadingDate: Long = 0,
    val finishedReadingDate: Long = 0,
    val trackingUrl: String? = null
)

@Serializable
class TachiyomiHistory(
    val id: Long? = null,
    val chapterId: Long,
    val lastRead: Long = 0,
    val timeRead: Long = 0
)

@Serializable
class TachiyomiUpdate(
    val id: Long? = null,
    val mangaId: Long,
    val chapterId: Long,
    val read: Boolean = false,
    val dateFetch: Long = 0
)
