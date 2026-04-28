package app.otakureader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId

/**
 * Entity storing daily reading streak state.
 * One row per day the user has read at least one chapter.
 * Used for fast streak calculation without scanning full history.
 */
@Entity(tableName = "reading_streaks")
data class ReadingStreakEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String, // ISO-8601 date: "2026-04-29"

    @ColumnInfo(name = "chapter_count")
    val chapterCount: Int = 0,

    @ColumnInfo(name = "read_duration_ms")
    val readDurationMs: Long = 0L,

    @ColumnInfo(name = "last_read_at")
    val lastReadAt: Long = 0L
) {
    companion object {
        fun fromTimestamp(timestamp: Long): String {
            return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
        }
    }
}
