package app.otakureader.core.database.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * POJO for ReadingListDao.getListWithManga() — a reading list with all its manga.
 */
data class ReadingListWithMangaEntity(
    @Embedded
    val list: ReadingListEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ReadingListItemEntity::class,
            parentColumn = "listId",
            entityColumn = "mangaId"
        )
    )
    val manga: List<MangaEntity> = emptyList()
)
