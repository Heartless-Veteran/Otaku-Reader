package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a chapter download in the database.
 */
@Entity(
    tableName = "downloads",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MangaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mangaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chapterId"], unique = true),
        Index(value = ["mangaId"])
    ]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chapterId: Long,
    val mangaId: Long,
    val sourceId: Long,
    val chapterName: String,
    val mangaTitle: String,
    val state: String, // Serialized DownloadState
    val progress: Int = 0,
    val totalPages: Int = 0,
    val downloadedPages: Int = 0,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Entity representing a downloaded page in the database.
 */
@Entity(
    tableName = "download_pages",
    foreignKeys = [
        ForeignKey(
            entity = DownloadEntity::class,
            parentColumns = ["chapterId"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chapterId", "pageIndex"], unique = true)
    ]
)
data class DownloadPageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chapterId: Long,
    val pageIndex: Int,
    val imageUrl: String,
    val localPath: String? = null,
    val downloaded: Boolean = false
)
