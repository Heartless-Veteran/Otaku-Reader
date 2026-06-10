package app.otakureader.domain.model

data class UpdateRunSummary(
    val id: Long = 0,
    val timestamp: Long,
    val checkedCount: Int,
    val newChaptersCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val durationMs: Long,
)
