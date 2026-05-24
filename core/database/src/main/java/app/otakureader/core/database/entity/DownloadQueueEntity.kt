package app.otakureader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "download_queue",
    indices = [
        Index("manga_id"),
        Index("chapter_id"),
        Index(value = ["manga_id", "status"])
    ]
)
data class DownloadQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "chapter_id")
    val chapterId: Long,
    @ColumnInfo(name = "manga_id")
    val mangaId: Long,
    @ColumnInfo(name = "manga_title")
    val mangaTitle: String,
    @ColumnInfo(name = "chapter_title")
    val chapterTitle: String,
    @ColumnInfo(name = "source_name")
    val sourceName: String,
    @ColumnInfo(name = "page_urls_json")
    val pageUrlsJson: String,
    val priority: Int = 1,
    val status: String = "QUEUED",
    @ColumnInfo(name = "added_at")
    val addedAt: Long
)
