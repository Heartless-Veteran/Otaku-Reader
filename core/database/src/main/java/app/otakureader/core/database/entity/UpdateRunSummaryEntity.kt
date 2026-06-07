package app.otakureader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "update_run_summary")
data class UpdateRunSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val checkedCount: Int,
    val newChaptersCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val durationMs: Long,
)
