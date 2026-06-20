package app.otakureader.data.worker

import app.otakureader.core.preferences.LibraryPreferences
import app.otakureader.domain.model.CategoryUpdateFrequency
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaStatus
import app.otakureader.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates the two-stage filter that decides which library manga are eligible for
 * a chapter-update check in a given run:
 *
 * 1. **Smart-skip** — excludes manga based on user-configured conditions (has unread
 *    chapters, is completed, has never been started).
 * 2. **Per-category frequency** — excludes manga whose update interval has not elapsed
 *    since the category was last successfully updated.
 *
 * Extracted from [LibraryUpdateWorker] to keep that class focused on coordination.
 */
@Singleton
class LibraryUpdateFilter @Inject constructor(
    private val libraryPreferences: LibraryPreferences,
    private val categoryRepository: CategoryRepository,
) {

    data class Result(
        val filtered: List<Manga>,
        val skipped: List<Manga>,
        val updatedCategoryIds: Set<Long>,
    )

    suspend fun apply(libraryManga: List<Manga>, now: Long): Result {
        val (afterSmartSkip, skipped) = applySmartSkip(libraryManga)
        val (filtered, updatedCategoryIds) = applyFrequencyFilter(afterSmartSkip, now)
        return Result(filtered, skipped, updatedCategoryIds)
    }

    private suspend fun applySmartSkip(libraryManga: List<Manga>): Pair<List<Manga>, List<Manga>> {
        val skipWithUnread = libraryPreferences.skipUpdatesWithUnread.first()
        val skipCompleted = libraryPreferences.skipUpdatesWithCompleted.first()
        val skipNeverStarted = libraryPreferences.skipUpdatesNeverStarted.first()
        if (!skipWithUnread && !skipCompleted && !skipNeverStarted) {
            return libraryManga to emptyList()
        }
        val skipped = mutableListOf<Manga>()
        val filtered = libraryManga.filter { manga ->
            when {
                skipWithUnread && manga.unreadCount > 0 -> { skipped += manga; false }
                skipCompleted && manga.status == MangaStatus.COMPLETED -> { skipped += manga; false }
                // lastRead null/0 means the user has never opened any chapter.
                skipNeverStarted && (manga.lastRead == null || manga.lastRead == 0L) -> { skipped += manga; false }
                else -> true
            }
        }
        return filtered to skipped
    }

    private suspend fun applyFrequencyFilter(
        libraryManga: List<Manga>,
        now: Long,
    ): Pair<List<Manga>, Set<Long>> {
        val categoryFrequencyMap = categoryRepository.getCategories().first()
            .associate { it.id to it.updateFrequency }
        val categoryLastUpdate = libraryPreferences.categoryLastUpdateMs.first()
        val updatedCategoryIds = mutableSetOf<Long>()
        // Evaluate ALL categories for each manga so every due category has its
        // timestamp refreshed — not just the first one found by short-circuit evaluation.
        val filtered = libraryManga.filter { manga ->
            val catIds = manga.categoryIds
            if (catIds.isEmpty()) return@filter true
            val dueCatIds = catIds.filter { catId ->
                val freq = categoryFrequencyMap[catId] ?: CategoryUpdateFrequency.DAILY
                val elapsed = now - (categoryLastUpdate[catId] ?: 0L)
                when (freq) {
                    CategoryUpdateFrequency.NEVER -> false
                    CategoryUpdateFrequency.DAILY -> elapsed >= TimeUnit.DAYS.toMillis(1)
                    CategoryUpdateFrequency.EVERY_3_DAYS -> elapsed >= TimeUnit.DAYS.toMillis(3)
                    CategoryUpdateFrequency.WEEKLY -> elapsed >= TimeUnit.DAYS.toMillis(7)
                }
            }
            updatedCategoryIds.addAll(dueCatIds)
            dueCatIds.isNotEmpty()
        }
        return filtered to updatedCategoryIds
    }
}
