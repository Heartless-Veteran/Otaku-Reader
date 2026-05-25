package app.otakureader.domain.model

/**
 * Rule that determines automatic manga membership in a dynamic category.
 *
 * Rules are evaluated against the user's library at category load time.
 * Multiple rules on the same category are combined with AND logic
 * (manga must satisfy all rules to be included).
 */
sealed class DynamicCategoryRule {
    /** Manga with at least [count] unread chapters. */
    data class UnreadAtLeast(val count: Int) : DynamicCategoryRule()

    /** Manga updated within the last [withinDays] days (based on most recent chapter date). */
    data class RecentlyUpdated(val withinDays: Int) : DynamicCategoryRule()

    /** Manga whose genre list contains [genre] (case-insensitive substring match). */
    data class GenreContains(val genre: String) : DynamicCategoryRule()

    companion object {
        const val TYPE_UNREAD_AT_LEAST = "unread_at_least"
        const val TYPE_RECENTLY_UPDATED = "recently_updated"
        const val TYPE_GENRE_CONTAINS = "genre_contains"
    }
}
