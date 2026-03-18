package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for caching AI-generated manga recommendations.
 * Stores recommendations locally to reduce API calls and improve performance.
 */
@Entity(
    tableName = "recommendations",
    indices = [
        Index(value = ["generatedAt"]),
        Index(value = ["recommendationType"]),
        Index(value = ["viewed"]),
        Index(value = ["dismissed"])
    ]
)
data class RecommendationEntity(
    @PrimaryKey
    val id: String,
    val mangaId: Long? = null,
    val title: String,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val genres: String = "", // Comma-separated genres
    val sourceId: String,
    val sourceUrl: String,
    val reasonExplanation: String,
    val confidenceScore: Float = 0.0f,
    val basedOnMangaIds: String = "", // Comma-separated manga IDs
    val basedOnGenres: String = "", // Comma-separated genres
    val recommendationType: String = "SIMILAR",
    val generatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + DEFAULT_CACHE_DURATION_MS,
    val viewed: Boolean = false,
    val actioned: Boolean = false,
    val dismissed: Boolean = false,
    val actionedMangaId: Long? = null
) {
    companion object {
        const val DEFAULT_CACHE_DURATION_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }
}

/**
 * Entity for storing user reading pattern snapshots.
 * Used to track how recommendations were generated.
 */
@Entity(
    tableName = "reading_patterns",
    indices = [Index(value = ["generatedAt"])]
)
data class ReadingPatternEntity(
    @PrimaryKey
    val id: String,
    val favoriteGenres: String = "", // JSON encoded
    val favoriteAuthors: String = "", // JSON encoded
    val preferredStatus: String = "", // Comma-separated
    val averageReadingTimeMs: Long = 0L,
    val preferredMinChapters: Int? = null,
    val preferredMaxChapters: Int? = null,
    val commonThemes: String = "", // Comma-separated
    val readingVelocity: String = "MODERATE",
    val favoriteTropes: String = "", // Comma-separated
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity for tracking recommendation refresh history.
 */
@Entity(
    tableName = "recommendation_refreshes",
    indices = [Index(value = ["timestamp"], unique = true)]
)
data class RecommendationRefreshEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val errorMessage: String? = null,
    val recommendationsCount: Int = 0,
    val patternId: String? = null
)
