package app.otakureader.domain.model

data class Recommendation(
    val mangaId: Long,
    val title: String,
    val thumbnailUrl: String?,
    val sourceId: Long,
    val score: Float,
)
