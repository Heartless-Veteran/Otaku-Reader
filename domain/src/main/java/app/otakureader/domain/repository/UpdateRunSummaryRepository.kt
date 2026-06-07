package app.otakureader.domain.repository

import app.otakureader.domain.model.UpdateRunSummary
import kotlinx.coroutines.flow.Flow

interface UpdateRunSummaryRepository {
    /** Emits the most recent 20 update run records, newest first. */
    fun getRecentRuns(): Flow<List<UpdateRunSummary>>

    /** Persists a completed run summary. */
    suspend fun insert(summary: UpdateRunSummary)

    /** Deletes records older than [cutoffMs] to keep the table small. */
    suspend fun deleteOlderThan(cutoffMs: Long)
}
