package app.otakureader.feature.reader.viewmodel.delegate

import android.content.Context
import android.util.Log
import app.otakureader.domain.model.Manga
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.DownloadRepository
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.repository.SourceRepository
import app.otakureader.core.preferences.DownloadPreferences
import app.otakureader.sourceapi.Page
import app.otakureader.sourceapi.SourceChapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class ReaderDownloadAheadDelegate @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadPreferences: DownloadPreferences,
    private val downloadRepository: DownloadRepository,
    private val sourceRepository: SourceRepository,
    private val chapterRepository: ChapterRepository,
    private val mangaRepository: MangaRepository,
) {
    @Suppress("CognitiveComplexMethod", "LoopWithTooManyJumpStatements")
    fun maybeDownloadNextChapter(
        scope: CoroutineScope,
        currentPage: Int,
        totalPages: Int,
        mangaId: Long,
        chapterId: Long,
        getCurrentManga: () -> Manga?,
    ) {
        if (totalPages == 0) return
        val progress = currentPage.toFloat() / totalPages
        if (progress < PROGRESS_THRESHOLD) return

        scope.launch {
            val downloadAheadChapters = downloadPreferences.downloadAheadWhileReading.first()
            if (downloadAheadChapters <= 0) return@launch

            val onlyOnWifi = downloadPreferences.downloadAheadOnlyOnWifi.first()
            if (onlyOnWifi && !isOnWifi()) return@launch

            val chapters = chapterRepository.getChaptersByMangaId(mangaId).first()
            val currentIndex = chapters.indexOfFirst { it.id == chapterId }
            if (currentIndex == -1 || currentIndex >= chapters.size - 1) return@launch

            val manga = getCurrentManga() ?: mangaRepository.getMangaById(mangaId) ?: return@launch
            val sourceName = manga.sourceId.toString()
            val existingDownloads = downloadRepository.observeDownloads().first()

            // Download up to downloadAheadChapters ahead, respecting user preference.
            val endIndex = minOf(currentIndex + downloadAheadChapters, chapters.size - 1)
            for (i in currentIndex + 1..endIndex) {
                val nextChapter = chapters[i]

                val alreadyDownloading = existingDownloads.find { it.chapterId == nextChapter.id }
                if (alreadyDownloading != null) continue

                if (downloadRepository.isChapterDownloaded(sourceName, manga.title, nextChapter.name)) continue

                val sourceChapter = SourceChapter(
                    url = nextChapter.url,
                    name = nextChapter.name,
                    dateUpload = nextChapter.dateUpload,
                    chapterNumber = nextChapter.chapterNumber,
                    scanlator = nextChapter.scanlator,
                )
                val pageListResult = sourceRepository.getPageList(sourceName, sourceChapter)
                pageListResult.onFailure { throwable ->
                    runCatching {
                        Log.w(TAG, "Failed to fetch page list for download-ahead " +
                            "(mangaId=${manga.id}, chapterId=${nextChapter.id})", throwable)
                    }
                }

                val pageUrls = pageListResult.getOrNull()
                    ?.mapNotNull { page -> page.effectiveUrl() }
                    .orEmpty()
                if (pageUrls.isEmpty()) continue

                downloadRepository.enqueueChapter(
                    mangaId = manga.id,
                    chapterId = nextChapter.id,
                    sourceName = sourceName,
                    mangaTitle = manga.title,
                    chapterTitle = nextChapter.name,
                    pageUrls = pageUrls,
                )
            }
        }
    }

    private fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val networkCapabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun Page.effectiveUrl(): String? = when {
        !imageUrl.isNullOrBlank() -> imageUrl
        url.isNotBlank() -> url
        else -> null
    }

    companion object {
        private const val TAG = "ReaderDownloadAheadDelegate"
        private const val PROGRESS_THRESHOLD = 0.8f
    }
}
