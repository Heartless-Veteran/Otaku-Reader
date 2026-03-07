package app.otakureader.domain.usecase

import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLibraryMangaUseCase @Inject constructor(
    private val mangaRepository: MangaRepository
) {
    operator fun invoke(): Flow<List<Manga>> {
        return mangaRepository.getLibraryManga()
    }
}
