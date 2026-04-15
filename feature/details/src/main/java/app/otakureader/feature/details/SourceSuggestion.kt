package app.otakureader.feature.details

/**
 * UI model for source suggestions (related manga from the source website)
 */
data class SourceSuggestion(
    val title: String,
    val thumbnailUrl: String?,
    val mangaUrl: String,
    val sourceId: String,
    val sourceName: String,
    val reason: String? = null // e.g., "Same author", "Related series", "Similar genre"
)
