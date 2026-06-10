package app.otakureader.domain.usecase

import app.otakureader.domain.model.Chapter
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.MangaRepository
import javax.inject.Inject

/**
 * Copies chapter entries from an alternative-source manga into a primary manga where
 * chapter numbers are absent (#1053).
 *
 * This is a non-destructive complement to MergeDuplicateMangaUseCase: it does not delete
 * either manga entry, only adds the chapters that the primary is missing so readers can
 * track progress against the larger chapter list.
 *
 * Matching is by chapter number (float equality). Chapters present in [primaryMangaId]
 * are never duplicated; only chapter numbers that appear exclusively in [altMangaId]
 * (and have a valid number ≥ 0) are inserted into the primary.
 */
class FillMissingChaptersUseCase @Inject constructor(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) {
    /**
     * @return The number of chapter entries inserted into [primaryMangaId].
     */
    suspend operator fun invoke(primaryMangaId: Long, altMangaId: Long): Int {
        require(primaryMangaId != altMangaId) { "Primary and alternative manga must differ" }

        val primaryChapters = chapterRepository.getChaptersByMangaIdSync(primaryMangaId)
        val primaryNumbers = primaryChapters
            .filter { it.chapterNumber >= 0f }
            .map { it.chapterNumber }
            .toSet()

        val altChapters = chapterRepository.getChaptersByMangaIdSync(altMangaId)
        val missing = altChapters.filter { alt ->
            alt.chapterNumber >= 0f && alt.chapterNumber !in primaryNumbers
        }

        if (missing.isEmpty()) return 0

        val toInsert = missing.map { alt ->
            Chapter(
                id = 0,
                mangaId = primaryMangaId,
                url = alt.url,
                name = alt.name,
                read = false,
                chapterNumber = alt.chapterNumber,
            )
        }
        chapterRepository.insertChapters(toInsert)
        return toInsert.size
    }
}
