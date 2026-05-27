package app.otakureader.domain.usecase

import app.otakureader.domain.model.DynamicCategoryRule
import app.otakureader.domain.model.Manga
import app.otakureader.domain.model.MangaStatus
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Computes which library manga belong to a dynamic category by evaluating its [rules].
 *
 * Rules are combined with AND logic (a manga must satisfy every rule), matching
 * [DynamicCategoryRule]'s contract. Pure and side-effect free so it can be unit tested and
 * recomputed reactively whenever the library changes.
 */
class EvaluateDynamicCategoryUseCase @Inject constructor() {

    /** @return the ids of [manga] that satisfy all [rules]. Empty [rules] matches nothing. */
    operator fun invoke(
        rules: List<DynamicCategoryRule>,
        manga: List<Manga>,
        now: Long = System.currentTimeMillis(),
    ): Set<Long> {
        if (rules.isEmpty()) return emptySet()
        return manga.filter { m -> rules.all { matches(m, it, now) } }
            .map { it.id }
            .toSet()
    }

    private fun matches(manga: Manga, rule: DynamicCategoryRule, now: Long): Boolean = when (rule) {
        is DynamicCategoryRule.UnreadAtLeast -> manga.unreadCount >= rule.count
        is DynamicCategoryRule.RecentlyUpdated ->
            manga.lastUpdate in 1..now && now - manga.lastUpdate <= TimeUnit.DAYS.toMillis(rule.withinDays.toLong())
        is DynamicCategoryRule.GenreContains ->
            manga.genre.any { it.contains(rule.genre, ignoreCase = true) }
        is DynamicCategoryRule.Completed -> manga.status == MangaStatus.COMPLETED
        is DynamicCategoryRule.Ongoing -> manga.status == MangaStatus.ONGOING
        is DynamicCategoryRule.RecentlyAdded ->
            manga.dateAdded in 1..now && now - manga.dateAdded <= TimeUnit.DAYS.toMillis(rule.withinDays.toLong())
    }
}
