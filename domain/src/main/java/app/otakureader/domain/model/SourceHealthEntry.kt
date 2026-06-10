package app.otakureader.domain.model

data class SourceHealthEntry(
    val sourceId: Long,
    val sourceName: String,
    val lastErrorMessage: String?,
    val lastErrorAt: Long?,      // epoch ms
    val consecutiveFailures: Int,
    val isDisabled: Boolean,
)
