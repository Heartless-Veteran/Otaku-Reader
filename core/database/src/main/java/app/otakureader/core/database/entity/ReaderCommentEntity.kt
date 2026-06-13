package app.otakureader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for local reader comments — private, on-device notes written from the
 * reader's comments overlay.
 *
 * Scope is encoded by [chapterId]: a null chapter id is a book-level comment (visible
 * from any chapter of the manga), a non-null chapter id ties the comment to one chapter.
 * Comments cascade-delete with their manga or chapter.
 */
@Entity(
    tableName = "reader_comments",
    foreignKeys = [
        ForeignKey(
            entity = MangaEntity::class,
            parentColumns = ["id"],
            childColumns = ["manga_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["manga_id"]),
        Index(value = ["chapter_id"])
    ]
)
data class ReaderCommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "manga_id")
    val mangaId: Long,
    /** Null for book-level comments; the owning chapter for chapter-level comments. */
    @ColumnInfo(name = "chapter_id")
    val chapterId: Long? = null,
    @ColumnInfo(name = "body")
    val body: String,
    /** Epoch millis when the comment was created. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    /** Epoch millis of the last edit; equals [createdAt] until editing ships. */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
