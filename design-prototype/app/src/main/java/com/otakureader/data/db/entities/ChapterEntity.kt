package com.otakureader.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = MangaEntity::class,
        parentColumns = ["id"],
        childColumns = ["mangaId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mangaId: Int,
    val chapterNumber: Float,
    val title: String,
    val pages: Int = 0,
    val read: Boolean = false,
    val downloaded: Boolean = false,
    val scanlator: String = "",
    val uploadDate: Long = 0L,
    val lastPageRead: Int = 0,
)
