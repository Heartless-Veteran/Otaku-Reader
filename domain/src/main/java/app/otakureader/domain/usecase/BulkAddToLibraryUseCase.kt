package app.otakureader.domain.usecase

import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.MangaRepository
import javax.inject.Inject

/**
 * Use case to add multiple manga to the library at once.
 * Used for bulk operations in browse/search screens.
 */
class BulkAddToLibraryUseCase @Inject constructor(
    private val mangaRepository: MangaRepository
) {
    /**
     * Add multiple manga to the library.
     * @param mangaIds List of manga IDs to add
     * @param categoryId Optional category to assign (null = default category)
     * @return Result with count of successfully added manga
     */
    suspend operator fun invoke(mangaIds: List<Long>, categoryId: Long? = null): BulkResult {
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()

        mangaIds.forEach { mangaId ->
            try {
                val manga = mangaRepository.getMangaById(mangaId)
                if (manga != null) {
                    mangaRepository.addToFavorites(mangaId)
                    categoryId?.let {
                        mangaRepository.addMangaToCategory(mangaId, it)
                    }
                    successCount++
                } else {
                    failCount++
                    errors.add("Manga not found: $mangaId")
                }
            } catch (e: Exception) {
                failCount++
                errors.add("Error adding manga $mangaId: ${e.message}")
            }
        }

        return BulkResult(
            successCount = successCount,
            failCount = failCount,
            totalCount = mangaIds.size,
            errors = errors
        )
    }

    data class BulkResult(
        val successCount: Int,
        val failCount: Int,
        val totalCount: Int,
        val errors: List<String>
    ) {
        val isSuccess: Boolean get() = failCount == 0
        val isPartialSuccess: Boolean get() = successCount > 0 && failCount > 0
    }
}
