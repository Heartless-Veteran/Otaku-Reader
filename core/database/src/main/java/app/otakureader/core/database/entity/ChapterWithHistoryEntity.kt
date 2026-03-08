package app.otakureader.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room join result that pairs a [ChapterEntity] with its [ReadingHistoryEntity].
 * Used by [app.otakureader.core.database.dao.ReadingHistoryDao.observeHistoryWithChapters].
 */
data class ChapterWithHistoryEntity(
    @Embedded val chapter: ChapterEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "chapter_id"
    )
    val history: ReadingHistoryEntity
)
