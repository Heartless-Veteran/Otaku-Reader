package app.otakureader.domain.usecase.source

import app.otakureader.domain.repository.SourceRepository
import app.otakureader.sourceapi.SourceManga

/**
 * Use case for getting manga details from a source.
 */
class GetMangaDetailsUseCase(
    private val sourceRepository: SourceRepository
) {
    suspend operator fun invoke(sourceId: String, manga: SourceManga): Result<SourceManga> {
        return sourceRepository.getMangaDetails(sourceId, manga)
    }
}
