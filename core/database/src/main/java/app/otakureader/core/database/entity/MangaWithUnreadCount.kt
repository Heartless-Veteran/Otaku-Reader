package app.otakureader.core.database.entity

import androidx.room.Embedded

/**
 * Entity representing a Manga with its unread chapter count and most-recent read timestamp.
 * Used for efficient library queries that need both manga data and reading-activity info.
 *
 * [lastRead] is the max `read_at` across all of the manga's chapters (null if never read).
 * It powers the "skip never-started manga" library-update filter and stale-detection rules.
 */
data class MangaWithUnreadCount(
    @Embedded val manga: MangaEntity,
    val unreadCount: Int,
    val lastRead: Long? = null
)
