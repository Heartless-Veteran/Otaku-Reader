package app.otakureader.domain.usecase

import app.otakureader.domain.repository.CategoryRepository
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.MangaRepository
import javax.inject.Inject

/**
 * Merges one or more duplicate manga entries into a chosen primary entry (#997).
 *
 * The merge preserves reading progress: any chapter marked as read in a secondary
 * manga is also marked as read in the primary (matched by chapter number). Category
 * memberships are copied to the primary, and then secondaries are deleted.
 */
class MergeDuplicateMangaUseCase @Inject constructor(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(primaryId: Long, secondaryIds: List<Long>) {
        val primaryChapters = chapterRepository.getChaptersByMangaIdSync(primaryId)
        val primaryByNumber = primaryChapters.associateBy { it.chapterNumber }

        for (secondaryId in secondaryIds) {
            val secondaryChapters = chapterRepository.getChaptersByMangaIdSync(secondaryId)

            // Promote read progress from secondary to primary by chapter number
            val toMarkRead = secondaryChapters
                .filter { it.read }
                .mapNotNull { sec -> primaryByNumber[sec.chapterNumber] }
                .filter { !it.read }
                .map { it.id }

            if (toMarkRead.isNotEmpty()) {
                chapterRepository.updateChapterProgress(toMarkRead, read = true, lastPageRead = 0)
            }

            // Copy category memberships. Tolerate per-category failures (e.g. already a member)
            // but never swallow cancellation — runCatching catches Throwable, so rethrow it.
            val categoryIds = mangaRepository.getCategoryIdsForManga(secondaryId)
            for (catId in categoryIds) {
                runCatching { categoryRepository.addMangaToCategory(primaryId, catId) }
                    .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
            }

            // Remove secondary (cascade-deletes its chapters via foreign key)
            mangaRepository.deleteManga(secondaryId)
        }
    }
}
