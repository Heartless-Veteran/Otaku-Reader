package app.otakureader.domain.usecase

import app.otakureader.domain.model.ContentRating
import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.EhFavoritesRepository
import app.otakureader.domain.repository.MangaRepository
import javax.inject.Inject

/**
 * Fetches the authenticated user's E-Hentai favorites and inserts any entries that are
 * not yet in the local library.
 *
 * Additive-only: existing library entries are never modified or removed.
 * The caller is responsible for gating this use case on [GeneralPreferences.showNsfwContent].
 */
class SyncEhFavoritesUseCase @Inject constructor(
    private val ehFavoritesRepository: EhFavoritesRepository,
    private val mangaRepository: MangaRepository,
) {
    suspend operator fun invoke(): EhSyncResult {
        val favorites = ehFavoritesRepository.fetchFavorites()
        var added = 0
        var skipped = 0
        for (fav in favorites) {
            val existing = mangaRepository.getMangaBySourceAndUrl(EH_SOURCE_ID, fav.galleryUrl)
            if (existing != null) {
                skipped++
                continue
            }
            mangaRepository.insertManga(
                Manga(
                    id = 0L,
                    sourceId = EH_SOURCE_ID,
                    url = fav.galleryUrl,
                    title = fav.title,
                    thumbnailUrl = fav.thumbnailUrl,
                    favorite = true,
                    initialized = false,
                    contentRating = ContentRating.PORNOGRAPHIC,
                    dateAdded = System.currentTimeMillis(),
                )
            )
            added++
        }
        return EhSyncResult(added = added, skipped = skipped)
    }

    companion object {
        /**
         * Well-known source ID for the E-Hentai extension in the Tachiyomi ecosystem.
         * When the EH extension is installed, manga added here will be fully functional.
         */
        const val EH_SOURCE_ID = 1875628763L
    }
}

data class EhSyncResult(val added: Int, val skipped: Int)
