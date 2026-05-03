package com.otakureader.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mangaId: Int,
    val chapterId: Long,
    val chapterNumber: Float,
    val page: Int = 1,
    val totalPages: Int = 1,
    val readAt: Long = System.currentTimeMillis(),
)
