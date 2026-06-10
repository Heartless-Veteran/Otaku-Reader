package app.otakureader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent link between two library manga entries that represent the same series
 * from different sources (e.g. MangaDex vs. MangaPlus). Used by
 * LinkAlternativeSourceUseCase and FillMissingChaptersUseCase (#1053).
 *
 * The relationship is symmetric: (A, B) and (B, A) are the same link.
 * The caller stores one row and queries using OR on both columns.
 */
@Entity(
    tableName = "manga_alternative_source",
    foreignKeys = [
        ForeignKey(
            entity = MangaEntity::class,
            parentColumns = ["id"],
            childColumns = ["manga_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MangaEntity::class,
            parentColumns = ["id"],
            childColumns = ["alt_manga_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("manga_id"),
        Index("alt_manga_id"),
        Index(value = ["manga_id", "alt_manga_id"], unique = true),
    ],
)
data class MangaAlternativeSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "manga_id") val mangaId: Long,
    @ColumnInfo(name = "alt_manga_id") val altMangaId: Long,
)
