package app.otakureader.domain.usecase.library

import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaStatus
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.sourceapi.SourceManga
import app.otakureader.sourceapi.toSourceId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case for adding manga from a source to the library (favorites).
 * Converts SourceManga to domain Manga and inserts/updates in database.
 */
class AddMangaToLibraryUseCase(
    private val mangaRepository: MangaRepository
) {
    /**
     * Add a single SourceManga to the library.
     * Returns the manga ID if successful.
     */
    suspend operator fun invoke(sourceManga: SourceManga, sourceId: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Check if manga already exists by source URL
            val existingManga = mangaRepository.getMangaBySourceAndUrl(
                sourceId = sourceId.toSourceId(),
                url = sourceManga.url
            )
            
            if (existingManga != null) {
                // Already exists, just mark as favorite if not already
                if (!existingManga.favorite) {
                    mangaRepository.toggleFavorite(existingManga.id)
                }
                return@withContext Result.success(existingManga.id)
            }
            
            // Create new manga from SourceManga
            val newManga = Manga(
                id = 0, // Will be auto-generated
                sourceId = sourceId.toSourceId(),
                url = sourceManga.url,
                title = sourceManga.title,
                artist = sourceManga.artist,
                author = sourceManga.author,
                description = sourceManga.description,
                genre = sourceManga.genre
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList(),
                status = MangaStatus.UNKNOWN, // Will be updated when details fetched
                thumbnailUrl = sourceManga.thumbnailUrl,
                favorite = true, // Add to favorites immediately
                dateAdded = System.currentTimeMillis(),
                notifyNewChapters = false,
                notes = null,
                readerDirection = null,
                readerMode = null,
                readerColorFilter = null,
                readerCustomTintColor = null,
                readerBackgroundColor = null,
                preloadPagesBefore = null,
                preloadPagesAfter = null
            )
            
            val mangaId = mangaRepository.insertManga(newManga)
            Result.success(mangaId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add multiple SourceManga entries to the library.
     * Returns count of successfully added manga.
     */
    suspend operator fun invoke(sourceMangas: List<SourceManga>, sourceId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var addedCount = 0
            sourceMangas.forEach { sourceManga ->
                invoke(sourceManga, sourceId).onSuccess { addedCount++ }
            }
            Result.success(addedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
