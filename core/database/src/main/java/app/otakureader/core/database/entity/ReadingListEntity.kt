package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created reading list (collection) for organizing manga beyond categories.
 * Examples: "Summer Binge", "Re-read Later", "Hidden Gems".
 */
@Entity(tableName = "reading_lists")
data class ReadingListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    /** Color for the list icon (Material color hex). Default: random or preset. */
    val color: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Order among lists for custom sorting. */
    val sortOrder: Int = 0
)
