package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga_feature_cache")
data class RecommendationEntity(
    @PrimaryKey val mangaId: Long,
    val title: String,
    val thumbnailUrl: String?,
    val sourceId: Long,
    val genresJson: String,
    val score: Float,
    val lastComputed: Long,
)
