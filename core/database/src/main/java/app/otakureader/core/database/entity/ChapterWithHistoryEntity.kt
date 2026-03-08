package app.otakureader.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Represents a chapter with its associated reading history.
 */
data class ChapterWithHistoryEntity(
    @Embedded
    val history: ReadingHistoryEntity,

    @Relation(
        parentColumn = "chapter_id",
        entityColumn = "id"
    )
    val chapter: ChapterEntity
)
