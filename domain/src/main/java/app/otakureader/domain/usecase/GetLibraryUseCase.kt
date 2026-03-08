package app.otakureader.domain.usecase

import app.otakureader.domain.model.LibraryManga
import app.otakureader.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case that provides a filtered and searched library stream.
 */
class GetLibraryUseCase(
    private val mangaRepository: MangaRepository
) {
    operator fun invoke(query: String = ""): Flow<List<LibraryManga>> {
        return mangaRepository.getLibraryManga().map { mangas ->
            val filtered = if (query.isBlank()) {
                mangas
            } else {
                mangas.filter { it.title.contains(query, ignoreCase = true) }
            }
            filtered.map { manga -> LibraryManga(manga = manga, unreadCount = manga.unreadCount) }
        }
    }
}
