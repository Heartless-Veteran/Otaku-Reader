package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction entity linking a manga to a reading list.
 * Many-to-many: one manga can be in multiple lists, one list has many manga.
 */
@Entity(
    tableName = "reading_list_items",
    primaryKeys = ["listId", "mangaId"],
    foreignKeys = [
        ForeignKey(
            entity = ReadingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
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
        Index(value = ["listId"]),
        Index(value = ["mangaId"]),
        Index(value = ["listId", "addedAt"])
    ]
)
data class ReadingListItemEntity(
    val listId: Long,
    val mangaId: Long,
    /** User-defined order within the list. */
    val sortOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    /** Optional user note about why this manga is in the list. */
    val note: String? = null
)
