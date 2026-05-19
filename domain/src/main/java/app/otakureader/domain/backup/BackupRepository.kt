package app.otakureader.domain.backup

import java.io.File

/** Repository for managing backup and restore operations. */
interface BackupRepository {
    /** Creates a backup and writes it to the provided URI. */
    suspend fun createBackup(uriString: String)

    /** Restores a backup from the provided URI. */
    suspend fun restoreBackup(uriString: String)

    /** Creates an automatic backup and saves it to the app's private backup directory. */
    suspend fun createLocalBackup(): File

    /** Restores a backup from a local file. */
    suspend fun restoreLocalBackup(file: File)

    /** Returns a list of automatic backup files stored locally, sorted newest first. */
    suspend fun listLocalBackups(): List<File>

    /**
     * Removes old automatic backup files, keeping only the [maxCount] most recent ones.
     * [maxCount] is coerced to at least 1 to always retain the most recent backup.
     */
    suspend fun pruneLocalBackups(maxCount: Int)
}
