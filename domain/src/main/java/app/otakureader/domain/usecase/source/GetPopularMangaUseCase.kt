package app.otakureader.domain.usecase.source

import app.otakureader.domain.repository.SourceRepository
import app.otakureader.sourceapi.MangaPage

/**
 * Use case for getting popular manga from a source.
 */
class GetPopularMangaUseCase(
    private val sourceRepository: SourceRepository
) {
    suspend operator fun invoke(sourceId: String, page: Int = 1): Result<MangaPage> {
        return sourceRepository.getPopularManga(sourceId, page)
    }
}
