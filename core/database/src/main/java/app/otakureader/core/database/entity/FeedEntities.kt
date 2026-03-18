package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Entity for feed items - latest chapters from various sources.
 */
@Entity(
    tableName = "feed_items",
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["timestamp"]),
        Index(value = ["mangaId"])
    ]
)
data class FeedItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mangaId: Long,
    val mangaTitle: String,
    val mangaThumbnailUrl: String?,
    val chapterId: Long,
    val chapterName: String,
    val chapterNumber: Float,
    val sourceId: Long,
    val sourceName: String,
    val timestamp: Instant,
    val isRead: Boolean = false
)

@Entity(
    tableName = "feed_sources",
    indices = [Index(value = ["sourceId"], unique = true)]
)
data class FeedSourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceId: Long,
    val sourceName: String,
    val isEnabled: Boolean = true,
    val itemCount: Int = 20,
    val order: Int = 0
)

@Entity(
    tableName = "feed_saved_searches",
    indices = [Index(value = ["sourceId"])]
)
data class FeedSavedSearchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceId: Long,
    val sourceName: String,
    val query: String,
    val filtersJson: String?, // Serialized Map<String, String>
    val order: Int = 0
)
