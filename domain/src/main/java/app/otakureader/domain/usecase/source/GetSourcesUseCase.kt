package app.otakureader.domain.usecase.source

import app.otakureader.domain.repository.SourceRepository
import app.otakureader.sourceapi.MangaSource
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting all available sources.
 */
class GetSourcesUseCase(
    private val sourceRepository: SourceRepository
) {
    operator fun invoke(): Flow<List<MangaSource>> {
        return sourceRepository.getSources()
    }
}
