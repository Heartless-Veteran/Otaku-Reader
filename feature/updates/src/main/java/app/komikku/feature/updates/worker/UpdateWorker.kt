package app.komikku.feature.updates.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.komikku.domain.repository.ChapterRepository
import app.komikku.domain.repository.MangaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay

@HiltWorker
class UpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val libraryMangas = mangaRepository.observeLibrary().first()

            for (libraryManga in libraryMangas) {
                // Determine the last chapter number from local DB to use as a baseline
                val localChapters = chapterRepository.observeChaptersByManga(libraryManga.manga.id).first()
                val lastChapterNumber = localChapters.maxOfOrNull { it.chapterNumber } ?: 0f

                // TODO: Fetch remote chapters from SourceManager when implemented
                // val source = sourceManager.get(libraryManga.manga.sourceId)
                // val remoteChapters = source.getChapterList(libraryManga.manga.toSManga())
                // val newChapters = remoteChapters.filter { it.chapterNumber > lastChapterNumber }

                // If there were new chapters, we would upsert them here and update the manga's lastUpdate
                // if (newChapters.isNotEmpty()) {
                //     chapterRepository.upsertChapters(newChapters.map { it.toDomain(libraryManga.manga.id) })
                //     mangaRepository.upsertManga(libraryManga.manga.copy(lastUpdate = System.currentTimeMillis()))
                // }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
