package app.otakureader.domain.usecase

import app.otakureader.domain.model.LibraryManga
import app.otakureader.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case that provides a filtered and searched library stream.
 */
class GetLibraryUseCase(
    private val mangaRepository: MangaRepository
) {
    operator fun invoke(query: String = ""): Flow<List<LibraryManga>> {
        return if (query.isBlank()) {
            mangaRepository.observeLibrary()
        } else {
            mangaRepository.searchLibrary(query)
        }
    }
}
