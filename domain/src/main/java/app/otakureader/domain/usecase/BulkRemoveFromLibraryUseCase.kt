package app.otakureader.domain.usecase

import app.otakureader.domain.repository.MangaRepository
import javax.inject.Inject

/**
 * Use case to remove multiple manga from the library at once.
 */
class BulkRemoveFromLibraryUseCase @Inject constructor(
    private val mangaRepository: MangaRepository
) {
    /**
     * Remove multiple manga from the library.
     * @param mangaIds List of manga IDs to remove
     * @param deleteDownloads Whether to also delete downloaded chapters
     * @return Result with count of successfully removed manga
     */
    suspend operator fun invoke(
        mangaIds: List<Long>,
        deleteDownloads: Boolean = false
    ): BulkRemoveResult {
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()

        mangaIds.forEach { mangaId ->
            try {
                mangaRepository.removeFromFavorites(mangaId)
                if (deleteDownloads) {
                    mangaRepository.deleteDownloadsForManga(mangaId)
                }
                successCount++
            } catch (e: Exception) {
                failCount++
                errors.add("Error removing manga $mangaId: ${e.message}")
            }
        }

        return BulkRemoveResult(
            successCount = successCount,
            failCount = failCount,
            totalCount = mangaIds.size,
            errors = errors
        )
    }

    data class BulkRemoveResult(
        val successCount: Int,
        val failCount: Int,
        val totalCount: Int,
        val errors: List<String>
    )
}
