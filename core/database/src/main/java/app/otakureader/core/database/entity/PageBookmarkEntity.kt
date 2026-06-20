package app.otakureader.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.otakureader.core.database.entity.BookmarkCollectionEntity

/**
 * Room entity for page-level bookmarks within a chapter.
 *
 * Users can bookmark specific pages to quickly return to key scenes,
 * artwork, or dialogue they want to revisit later.
 */
@Entity(
    tableName = "page_bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MangaEntity::class,
            parentColumns = ["id"],
            childColumns = ["manga_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookmarkCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["chapter_id"]),
        Index(value = ["manga_id"]),
        Index(value = ["manga_id", "created_at"]),
        Index(value = ["collection_id"])
    ]
)
data class PageBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "manga_id")
    val mangaId: Long,
    @ColumnInfo(name = "chapter_id")
    val chapterId: Long,
    /** Page index within the chapter (0-based) */
    @ColumnInfo(name = "page_index")
    val pageIndex: Int,
    /** Optional user note about why they bookmarked this page */
    @ColumnInfo(name = "note")
    val note: String? = null,
    /** Epoch millis when bookmark was created */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    /** Optional collection this bookmark belongs to; null = uncollected. */
    @ColumnInfo(name = "collection_id")
    val collectionId: Long? = null
)
