package app.otakureader.domain.usecase

import app.otakureader.domain.repository.MangaRepository
import javax.inject.Inject

/**
 * Persists a bidirectional alternative-source link between two library manga entries (#1053).
 *
 * Two manga are "alternative sources" when they are the same series published on different
 * platforms (e.g. MangaDex vs. MangaPlus). Linking them allows FillMissingChaptersUseCase
 * to copy missing chapter entries across sources without performing a full merge.
 */
class LinkAlternativeSourceUseCase @Inject constructor(
    private val mangaRepository: MangaRepository,
) {
    /**
     * Links [mangaId] and [altMangaId] as alternative sources. Idempotent — calling it
     * again for the same pair is a no-op (IGNORE conflict strategy at the DAO level).
     *
     * @throws IllegalArgumentException if mangaId == altMangaId.
     */
    suspend operator fun invoke(mangaId: Long, altMangaId: Long) {
        require(mangaId != altMangaId) { "Cannot link a manga to itself" }
        mangaRepository.linkAlternativeSource(mangaId, altMangaId)
    }
}
