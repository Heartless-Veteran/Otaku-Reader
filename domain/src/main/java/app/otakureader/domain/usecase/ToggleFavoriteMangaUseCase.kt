package app.otakureader.domain.usecase

import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.MangaRepository
import javax.inject.Inject

class ToggleFavoriteMangaUseCase @Inject constructor(
    private val mangaRepository: MangaRepository
) {
    suspend operator fun invoke(mangaId: Long) {
        mangaRepository.toggleFavorite(mangaId)
    }
}
