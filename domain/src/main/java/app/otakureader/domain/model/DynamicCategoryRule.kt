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

    /** Manga the user has never opened a chapter of (no read history). "Plan to read". */
    data object NeverStarted : DynamicCategoryRule()

    /** Manga last read within the last [withinDays] days. "Currently reading". */
    data class ReadWithinDays(val withinDays: Int) : DynamicCategoryRule() {
        init { require(withinDays >= 0) { "withinDays must be non-negative, was $withinDays" } }
    }

    /**
     * Manga that has been read at least once but not within the last [withinDays] days.
     * "Almost forgotten" — never-started manga are excluded (they belong to [NeverStarted]).
     */
    data class NotReadInDays(val withinDays: Int) : DynamicCategoryRule() {
        init { require(withinDays >= 0) { "withinDays must be non-negative, was $withinDays" } }
    }

    /** Manga the user has manually marked completed. */
    data object MarkedCompleted : DynamicCategoryRule()

    /** Manga the user has manually marked dropped. */
    data object MarkedDropped : DynamicCategoryRule()

    companion object {
        const val TYPE_UNREAD_AT_LEAST = "unread_at_least"
        const val TYPE_RECENTLY_UPDATED = "recently_updated"
        const val TYPE_GENRE_CONTAINS = "genre_contains"
        const val TYPE_COMPLETED = "completed"
        const val TYPE_ONGOING = "ongoing"
        const val TYPE_RECENTLY_ADDED = "recently_added"
        const val TYPE_NEVER_STARTED = "never_started"
        const val TYPE_READ_WITHIN_DAYS = "read_within_days"
        const val TYPE_NOT_READ_IN_DAYS = "not_read_in_days"
        const val TYPE_MARKED_COMPLETED = "marked_completed"
        const val TYPE_MARKED_DROPPED = "marked_dropped"
    }
}
