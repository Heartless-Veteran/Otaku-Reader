package app.otakureader.domain.backup

import app.otakureader.domain.model.ImportResult
import app.otakureader.domain.model.TachiyomiBackupPreview

/**
 * Imports a Mihon/Tachiyomi backup (`.tachibk`, `.proto.gz`, or legacy JSON) into the library.
 */
interface TachiyomiBackupImporter {

    /** Parses the backup and returns a summary of its contents without writing anything. */
    suspend fun preview(data: ByteArray): TachiyomiBackupPreview

    /**
     * Imports the backup.
     *
     * @param overwriteExisting when true, manga already in the library are updated from the
     *   backup; when false they are skipped.
     * @param onProgress invoked as manga are processed: (processed, total).
     */
    suspend fun importBackup(
        data: ByteArray,
        overwriteExisting: Boolean,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): ImportResult
}
