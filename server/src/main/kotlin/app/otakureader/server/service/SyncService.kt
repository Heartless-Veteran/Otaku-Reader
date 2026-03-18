package app.otakureader.server.service

import app.otakureader.server.config.AppConfig
import java.io.File
import java.io.IOException

/**
 * Service handling sync snapshot storage and retrieval.
 */
class SyncService(private val config: AppConfig) {

    private val snapshotFile = File(config.storagePath, "sync_snapshot.json")
    private val timestampFile = File(config.storagePath, "sync_timestamp.txt")

    /**
     * Store a sync snapshot.
     * @return The size of stored data in bytes
     */
    fun storeSnapshot(data: String, timestamp: Long): Result<Int> = try {
        val bytes = data.toByteArray(Charsets.UTF_8)
        snapshotFile.writeBytes(bytes)
        timestampFile.writeText(timestamp.toString())
        Result.success(bytes.size)
    } catch (e: IOException) {
        Result.failure(e)
    }

    /**
     * Retrieve the stored sync snapshot.
     * @return Pair of (data, timestamp) or null if no snapshot exists
     */
    fun getSnapshot(): Pair<String, Long>? {
        if (!snapshotFile.exists() || !timestampFile.exists()) {
            return null
        }

        return try {
            val data = snapshotFile.readText()
            val timestamp = timestampFile.readText().toLongOrNull() ?: System.currentTimeMillis()
            Pair(data, timestamp)
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Delete the stored snapshot.
     */
    fun deleteSnapshot(): Result<Unit> = try {
        snapshotFile.delete()
        timestampFile.delete()
        Result.success(Unit)
    } catch (e: IOException) {
        Result.failure(e)
    }

    /**
     * Get the timestamp of the stored snapshot.
     */
    fun getTimestamp(): Long? {
        if (!timestampFile.exists()) return null
        return try {
            timestampFile.readText().toLongOrNull()
        } catch (e: IOException) {
            null
        }
    }
}
