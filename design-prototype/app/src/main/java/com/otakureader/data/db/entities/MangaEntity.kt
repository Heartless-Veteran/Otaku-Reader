package com.otakureader.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga")
data class MangaEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val author: String,
    val coverUrl: String? = null,
    val description: String? = null,
    val chapters: Int = 0,
    val read: Int = 0,
    val status: String = "Ongoing",
    val rating: Float = 0f,
    val year: Int = 0,
    val hue: Float = 0f,
    val genres: String = "",        // comma-separated
    val scanlator: String = "",
    val source: String = "",
    val newChapters: Int = 0,
    val downloaded: Int = 0,
    val contentRating: String = "Safe",
    val categoryId: String = "reading",
    val autoDownload: Boolean = false,
    val downloadQuality: String = "High",
    val lastRead: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
)
