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
    data class UnreadAtLeast(val count: Int) : DynamicCategoryRule() {
        init { require(count >= 0) { "count must be non-negative, was $count" } }
    }

    /** Manga updated within the last [withinDays] days (based on most recent chapter date). */
    data class RecentlyUpdated(val withinDays: Int) : DynamicCategoryRule() {
        init { require(withinDays >= 0) { "withinDays must be non-negative, was $withinDays" } }
    }

    /** Manga whose genre list contains [genre] (case-insensitive substring match). */
    data class GenreContains(val genre: String) : DynamicCategoryRule()

    /** Manga whose status is COMPLETED. */
    data object Completed : DynamicCategoryRule()

    /** Manga whose status is ONGOING. */
    data object Ongoing : DynamicCategoryRule()

    /** Manga added to the library within the last [withinDays] days. */
    data class RecentlyAdded(val withinDays: Int) : DynamicCategoryRule() {
        init { require(withinDays >= 0) { "withinDays must be non-negative, was $withinDays" } }
    }

    /** Manga from a specific source by [sourceId]. */
    data class FromSource(val sourceId: Long) : DynamicCategoryRule()

    /** Manga that user has marked as dropped. */
    data object UserDropped : DynamicCategoryRule()

    /** Manga that user has marked as completed (userCompleted = true). */
    data object UserCompleted : DynamicCategoryRule()

    /** Manga with at least one chapter read (currently reading). */
    data object CurrentlyReading : DynamicCategoryRule()

    /** Manga with no activity (last read) for [days] or more. */
    data class InactiveForDays(val days: Int) : DynamicCategoryRule() {
        init { require(days >= 0) { "days must be non-negative, was $days" } }
    }

    /** Manga with at least [chapterCount] total chapters (for "Binge Ready" scenarios). */
    data class MinimumChapters(val chapterCount: Int) : DynamicCategoryRule() {
        init { require(chapterCount >= 0) { "chapterCount must be non-negative, was $chapterCount" } }
    }

    companion object {
        const val TYPE_UNREAD_AT_LEAST = "unread_at_least"
        const val TYPE_RECENTLY_UPDATED = "recently_updated"
        const val TYPE_GENRE_CONTAINS = "genre_contains"
        const val TYPE_COMPLETED = "completed"
        const val TYPE_ONGOING = "ongoing"
        const val TYPE_RECENTLY_ADDED = "recently_added"
        const val TYPE_FROM_SOURCE = "from_source"
        const val TYPE_USER_DROPPED = "user_dropped"
        const val TYPE_USER_COMPLETED = "user_completed"
        const val TYPE_CURRENTLY_READING = "currently_reading"
        const val TYPE_INACTIVE_FOR_DAYS = "inactive_for_days"
        const val TYPE_MINIMUM_CHAPTERS = "minimum_chapters"
    }
}
