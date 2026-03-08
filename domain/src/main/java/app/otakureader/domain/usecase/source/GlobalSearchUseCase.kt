package app.otakureader.domain.usecase.source

import app.otakureader.domain.repository.SourceRepository
import app.otakureader.sourceapi.MangaPage

/**
 * Use case for searching manga across all available sources concurrently.
 * Each source is searched in parallel; a failure in one source does not affect others.
 */
class GlobalSearchUseCase(
    private val sourceRepository: SourceRepository
) {
    suspend operator fun invoke(sourceId: String, query: String): Result<MangaPage> {
        return sourceRepository.searchManga(sourceId, query, 1)
    }
}
