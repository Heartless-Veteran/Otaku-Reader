package app.otakureader.core.common

import kotlin.math.max
import kotlin.math.min

/**
 * Estimates reading time for a chapter based on page count and user's average reading speed.
 *
 * @param pageCount number of pages in the chapter (may be estimated or actual)
 * @param pagesPerMinute user's average reading speed, or null to use default
 * @return estimated minutes to read, minimum 1 minute
 */
fun estimateReadTimeMinutes(pageCount: Int, pagesPerMinute: Float? = null): Int {
    if (pageCount <= 0) return 1
    val ppm = pagesPerMinute ?: 3.0f // Default: 20 seconds per page
    return max(1, (pageCount.toFloat() / ppm).toInt())
}

/**
 * Formats read time estimate for display.
 * Returns "1 min", "5 min", "1 hr 15 min", etc.
 */
fun formatReadTime(minutes: Int): String {
    return when {
        minutes < 1 -> "< 1 min"
        minutes < 60 -> "$minutes min"
        else -> {
            val hrs = minutes / 60
            val mins = minutes % 60
            if (mins == 0) "$hrs hr" else "$hrs hr $mins min"
        }
    }
}

/**
 * Estimates page count for a chapter when actual count is unknown.
 * Uses heuristics based on chapter number patterns.
 */
fun estimatePageCount(chapterNumber: Float, mangaTotalChapters: Int): Int {
    return when {
        mangaTotalChapters <= 5 -> 50 // One-shots or very short series
        chapterNumber <= 1f -> 45 // First chapters often longer
        else -> 20 // Standard chapter
    }
}
