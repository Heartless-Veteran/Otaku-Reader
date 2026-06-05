package app.otakureader.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefetchStrategyTest {

    private val defaultBehavior = ReadingBehavior.DEFAULT
    private val predictable = ReadingBehavior(
        forwardNavigationRatio = 0.95f,
        sequentialNavigationRatio = 0.9f,
        sampleSize = 100,
        completionRate = 0.9f
    )
    private val backwardProne = ReadingBehavior(
        forwardNavigationRatio = 0.7f,
        sequentialNavigationRatio = 0.9f,
        sampleSize = 100,
        completionRate = 0.9f
    )
    private val fastReader = ReadingBehavior(
        forwardNavigationRatio = 0.95f,
        averagePageDurationMs = 1500L,
        sequentialNavigationRatio = 0.95f,
        sampleSize = 100,
        completionRate = 0.9f
    )
    private val slowReader = ReadingBehavior(
        forwardNavigationRatio = 0.92f,
        averagePageDurationMs = 10000L,
        sequentialNavigationRatio = 0.85f,
        sampleSize = 100,
        completionRate = 0.9f
    )
    private val normalReader = ReadingBehavior(
        forwardNavigationRatio = 0.92f,
        averagePageDurationMs = 5000L,
        sequentialNavigationRatio = 0.9f,
        sampleSize = 100,
        completionRate = 0.9f
    )
    private val lowCompletion = ReadingBehavior(
        forwardNavigationRatio = 0.9f,
        sequentialNavigationRatio = 0.9f,
        sampleSize = 100,
        completionRate = 0.3f
    )

    // ── Conservative ──────────────────────────────────────────────────────────

    @Test
    fun `Conservative pagesBefore always returns zero`() {
        assertEquals(0, PrefetchStrategy.Conservative.pagesBefore(5, 20, defaultBehavior))
        assertEquals(0, PrefetchStrategy.Conservative.pagesBefore(0, 10, predictable))
        assertEquals(0, PrefetchStrategy.Conservative.pagesBefore(9, 10, defaultBehavior))
    }

    @Test
    fun `Conservative pagesAfter returns 2 when not near end`() {
        assertEquals(2, PrefetchStrategy.Conservative.pagesAfter(0, 10, defaultBehavior))
        assertEquals(2, PrefetchStrategy.Conservative.pagesAfter(5, 10, defaultBehavior))
        assertEquals(2, PrefetchStrategy.Conservative.pagesAfter(7, 10, defaultBehavior))
    }

    @Test
    fun `Conservative pagesAfter returns 1 at last two pages`() {
        assertEquals(1, PrefetchStrategy.Conservative.pagesAfter(8, 10, defaultBehavior))
        assertEquals(1, PrefetchStrategy.Conservative.pagesAfter(9, 10, defaultBehavior))
    }

    @Test
    fun `Conservative never prefetches adjacent chapters`() {
        assertFalse(PrefetchStrategy.Conservative.shouldPrefetchNextChapter(9, 10, defaultBehavior))
        assertFalse(PrefetchStrategy.Conservative.shouldPrefetchNextChapter(0, 10, defaultBehavior))
        assertFalse(PrefetchStrategy.Conservative.shouldPrefetchPreviousChapter(0, defaultBehavior))
        assertFalse(PrefetchStrategy.Conservative.shouldPrefetchPreviousChapter(1, defaultBehavior))
    }

    // ── Balanced ──────────────────────────────────────────────────────────────

    @Test
    fun `Balanced pagesBefore always returns 1`() {
        assertEquals(1, PrefetchStrategy.Balanced.pagesBefore(0, 20, defaultBehavior))
        assertEquals(1, PrefetchStrategy.Balanced.pagesBefore(10, 20, predictable))
        assertEquals(1, PrefetchStrategy.Balanced.pagesBefore(19, 20, backwardProne))
    }

    @Test
    fun `Balanced pagesAfter always returns 3`() {
        assertEquals(3, PrefetchStrategy.Balanced.pagesAfter(0, 20, defaultBehavior))
        assertEquals(3, PrefetchStrategy.Balanced.pagesAfter(10, 20, fastReader))
        assertEquals(3, PrefetchStrategy.Balanced.pagesAfter(19, 20, defaultBehavior))
    }

    @Test
    fun `Balanced shouldPrefetchNextChapter true on last 3 pages`() {
        assertTrue(PrefetchStrategy.Balanced.shouldPrefetchNextChapter(17, 20, defaultBehavior))
        assertTrue(PrefetchStrategy.Balanced.shouldPrefetchNextChapter(18, 20, defaultBehavior))
        assertTrue(PrefetchStrategy.Balanced.shouldPrefetchNextChapter(19, 20, defaultBehavior))
    }

    @Test
    fun `Balanced shouldPrefetchNextChapter false before last 3 pages`() {
        assertFalse(PrefetchStrategy.Balanced.shouldPrefetchNextChapter(16, 20, defaultBehavior))
        assertFalse(PrefetchStrategy.Balanced.shouldPrefetchNextChapter(0, 20, defaultBehavior))
    }

    @Test
    fun `Balanced never prefetches previous chapter`() {
        assertFalse(PrefetchStrategy.Balanced.shouldPrefetchPreviousChapter(0, defaultBehavior))
        assertFalse(PrefetchStrategy.Balanced.shouldPrefetchPreviousChapter(0, backwardProne))
    }

    // ── Aggressive ────────────────────────────────────────────────────────────

    @Test
    fun `Aggressive pagesBefore returns 3 for backward-prone readers`() {
        assertEquals(3, PrefetchStrategy.Aggressive.pagesBefore(5, 20, backwardProne))
    }

    @Test
    fun `Aggressive pagesBefore returns 2 for mostly-forward readers`() {
        assertEquals(2, PrefetchStrategy.Aggressive.pagesBefore(5, 20, predictable))
    }

    @Test
    fun `Aggressive pagesAfter returns 10 near chapter end`() {
        assertEquals(10, PrefetchStrategy.Aggressive.pagesAfter(15, 20, defaultBehavior))
        assertEquals(10, PrefetchStrategy.Aggressive.pagesAfter(16, 20, defaultBehavior))
        assertEquals(10, PrefetchStrategy.Aggressive.pagesAfter(19, 20, defaultBehavior))
    }

    @Test
    fun `Aggressive pagesAfter returns 7 elsewhere`() {
        assertEquals(7, PrefetchStrategy.Aggressive.pagesAfter(0, 20, defaultBehavior))
        assertEquals(7, PrefetchStrategy.Aggressive.pagesAfter(10, 20, defaultBehavior))
        assertEquals(7, PrefetchStrategy.Aggressive.pagesAfter(14, 20, defaultBehavior))
    }

    @Test
    fun `Aggressive shouldPrefetchNextChapter true on last 5 pages`() {
        assertTrue(PrefetchStrategy.Aggressive.shouldPrefetchNextChapter(15, 20, defaultBehavior))
        assertTrue(PrefetchStrategy.Aggressive.shouldPrefetchNextChapter(19, 20, defaultBehavior))
    }

    @Test
    fun `Aggressive shouldPrefetchNextChapter false before last 5 pages`() {
        assertFalse(PrefetchStrategy.Aggressive.shouldPrefetchNextChapter(14, 20, defaultBehavior))
        assertFalse(PrefetchStrategy.Aggressive.shouldPrefetchNextChapter(0, 20, defaultBehavior))
    }

    @Test
    fun `Aggressive shouldPrefetchPreviousChapter when at start and backward prone`() {
        assertTrue(PrefetchStrategy.Aggressive.shouldPrefetchPreviousChapter(0, backwardProne))
        assertTrue(PrefetchStrategy.Aggressive.shouldPrefetchPreviousChapter(2, backwardProne))
    }

    @Test
    fun `Aggressive shouldPrefetchPreviousChapter false for forward readers`() {
        assertFalse(PrefetchStrategy.Aggressive.shouldPrefetchPreviousChapter(0, predictable))
    }

    @Test
    fun `Aggressive shouldPrefetchPreviousChapter false past page 2`() {
        assertFalse(PrefetchStrategy.Aggressive.shouldPrefetchPreviousChapter(3, backwardProne))
    }

    // ── Adaptive ─────────────────────────────────────────────────────────────

    @Test
    fun `Adaptive pagesBefore returns 1 for unpredictable readers`() {
        assertEquals(1, PrefetchStrategy.Adaptive.pagesBefore(5, 20, defaultBehavior))
    }

    @Test
    fun `Adaptive pagesBefore returns 2 for backward-prone predictable readers`() {
        assertEquals(2, PrefetchStrategy.Adaptive.pagesBefore(5, 20, backwardProne))
    }

    @Test
    fun `Adaptive pagesBefore returns 1 for forward predictable readers`() {
        assertEquals(1, PrefetchStrategy.Adaptive.pagesBefore(5, 20, predictable))
    }

    @Test
    fun `Adaptive pagesAfter returns 3 for unpredictable readers`() {
        assertEquals(3, PrefetchStrategy.Adaptive.pagesAfter(5, 20, defaultBehavior))
    }

    @Test
    fun `Adaptive pagesAfter returns 6 for fast reader not near end`() {
        assertEquals(6, PrefetchStrategy.Adaptive.pagesAfter(5, 20, fastReader))
    }

    @Test
    fun `Adaptive pagesAfter returns 8 for fast reader near end`() {
        assertEquals(8, PrefetchStrategy.Adaptive.pagesAfter(17, 20, fastReader))
    }

    @Test
    fun `Adaptive pagesAfter returns 2 for slow reader not near end`() {
        assertEquals(2, PrefetchStrategy.Adaptive.pagesAfter(5, 20, slowReader))
    }

    @Test
    fun `Adaptive pagesAfter returns 4 for slow reader near end`() {
        assertEquals(4, PrefetchStrategy.Adaptive.pagesAfter(17, 20, slowReader))
    }

    @Test
    fun `Adaptive pagesAfter returns 3 for normal speed not near end`() {
        assertEquals(3, PrefetchStrategy.Adaptive.pagesAfter(5, 20, normalReader))
    }

    @Test
    fun `Adaptive pagesAfter returns 5 for normal speed near end`() {
        assertEquals(5, PrefetchStrategy.Adaptive.pagesAfter(17, 20, normalReader))
    }

    @Test
    fun `Adaptive shouldPrefetchNextChapter false when unlikely to complete`() {
        assertFalse(PrefetchStrategy.Adaptive.shouldPrefetchNextChapter(17, 20, lowCompletion))
    }

    @Test
    fun `Adaptive shouldPrefetchNextChapter for unpredictable reader near end`() {
        val unpredictableCompleter = ReadingBehavior(completionRate = 0.9f, sampleSize = 0)
        assertTrue(PrefetchStrategy.Adaptive.shouldPrefetchNextChapter(17, 20, unpredictableCompleter))
        assertFalse(PrefetchStrategy.Adaptive.shouldPrefetchNextChapter(16, 20, unpredictableCompleter))
    }

    @Test
    fun `Adaptive shouldPrefetchNextChapter fast reader triggers at last 5 pages`() {
        assertTrue(PrefetchStrategy.Adaptive.shouldPrefetchNextChapter(15, 20, fastReader))
        assertFalse(PrefetchStrategy.Adaptive.shouldPrefetchNextChapter(14, 20, fastReader))
    }

    @Test
    fun `Adaptive shouldPrefetchNextChapter normal speed triggers at last 3 pages`() {
        assertTrue(PrefetchStrategy.Adaptive.shouldPrefetchNextChapter(17, 20, normalReader))
        assertFalse(PrefetchStrategy.Adaptive.shouldPrefetchNextChapter(16, 20, normalReader))
    }

    @Test
    fun `Adaptive shouldPrefetchPreviousChapter when at start backward prone and predictable`() {
        assertTrue(PrefetchStrategy.Adaptive.shouldPrefetchPreviousChapter(0, backwardProne))
        assertTrue(PrefetchStrategy.Adaptive.shouldPrefetchPreviousChapter(2, backwardProne))
    }

    @Test
    fun `Adaptive shouldPrefetchPreviousChapter false for unpredictable reader`() {
        assertFalse(PrefetchStrategy.Adaptive.shouldPrefetchPreviousChapter(0, defaultBehavior))
    }

    @Test
    fun `Adaptive shouldPrefetchPreviousChapter false past page 2`() {
        assertFalse(PrefetchStrategy.Adaptive.shouldPrefetchPreviousChapter(3, backwardProne))
    }

    @Test
    fun `Adaptive shouldPrefetchPreviousChapter false for forward predictable reader`() {
        assertFalse(PrefetchStrategy.Adaptive.shouldPrefetchPreviousChapter(0, predictable))
    }

    // ── Companion: fromOrdinal / toOrdinal ────────────────────────────────────

    @Test
    fun `fromOrdinal maps to correct strategy`() {
        assertEquals(PrefetchStrategy.Conservative, PrefetchStrategy.fromOrdinal(0))
        assertEquals(PrefetchStrategy.Balanced, PrefetchStrategy.fromOrdinal(1))
        assertEquals(PrefetchStrategy.Aggressive, PrefetchStrategy.fromOrdinal(2))
        assertEquals(PrefetchStrategy.Adaptive, PrefetchStrategy.fromOrdinal(3))
    }

    @Test
    fun `fromOrdinal out of bounds defaults to Balanced`() {
        assertEquals(PrefetchStrategy.Balanced, PrefetchStrategy.fromOrdinal(-1))
        assertEquals(PrefetchStrategy.Balanced, PrefetchStrategy.fromOrdinal(99))
    }

    @Test
    fun `toOrdinal maps each strategy to correct index`() {
        assertEquals(0, PrefetchStrategy.toOrdinal(PrefetchStrategy.Conservative))
        assertEquals(1, PrefetchStrategy.toOrdinal(PrefetchStrategy.Balanced))
        assertEquals(2, PrefetchStrategy.toOrdinal(PrefetchStrategy.Aggressive))
        assertEquals(3, PrefetchStrategy.toOrdinal(PrefetchStrategy.Adaptive))
    }

    @Test
    fun `fromOrdinal and toOrdinal are inverses`() {
        listOf(
            PrefetchStrategy.Conservative,
            PrefetchStrategy.Balanced,
            PrefetchStrategy.Aggressive,
            PrefetchStrategy.Adaptive
        ).forEach { strategy ->
            assertEquals(strategy, PrefetchStrategy.fromOrdinal(PrefetchStrategy.toOrdinal(strategy)))
        }
    }

    // ── ReadingBehavior computed properties ────────────────────────────────────

    @Test
    fun `ReadingBehavior DEFAULT is unpredictable due to zero sample size`() {
        assertTrue(ReadingBehavior.DEFAULT.isUnpredictable)
    }

    @Test
    fun `ReadingBehavior isPrimaryForwardReader when high ratios`() {
        assertTrue(predictable.isPrimaryForwardReader)
        assertFalse(backwardProne.isPrimaryForwardReader)
    }

    @Test
    fun `ReadingBehavior likelyToCompleteChapter when completion rate meets threshold`() {
        assertTrue(predictable.likelyToCompleteChapter)
        assertFalse(lowCompletion.likelyToCompleteChapter)
    }

    @Test
    fun `ReadingBehavior isUnpredictable when sequential ratio too low`() {
        val chaotic = ReadingBehavior(sequentialNavigationRatio = 0.5f, sampleSize = 100)
        assertTrue(chaotic.isUnpredictable)
    }

    @Test
    fun `ReadingBehavior isUnpredictable false when sufficient sample and sequential ratio`() {
        assertFalse(predictable.isUnpredictable)
        assertFalse(normalReader.isUnpredictable)
    }

    // ── PageNavigationEvent properties ────────────────────────────────────────

    @Test
    fun `PageNavigationEvent isForward true when toPage greater than fromPage`() {
        val event = PageNavigationEvent(
            mangaId = 1L, chapterId = 2L, fromPage = 3, toPage = 4,
            pageDurationMs = 2000L, readerMode = 0
        )
        assertTrue(event.isForward)
    }

    @Test
    fun `PageNavigationEvent isForward false when going backward`() {
        val event = PageNavigationEvent(
            mangaId = 1L, chapterId = 2L, fromPage = 5, toPage = 3,
            pageDurationMs = 1000L, readerMode = 0
        )
        assertFalse(event.isForward)
    }

    @Test
    fun `PageNavigationEvent isSequential true for adjacent page in single mode`() {
        val event = PageNavigationEvent(
            mangaId = 1L, chapterId = 2L, fromPage = 3, toPage = 4,
            pageDurationMs = 3000L, readerMode = 0
        )
        assertTrue(event.isSequential)
    }

    @Test
    fun `PageNavigationEvent isSequential true for 2-page jump in dual mode`() {
        val event = PageNavigationEvent(
            mangaId = 1L, chapterId = 2L, fromPage = 2, toPage = 4,
            pageDurationMs = 3000L, readerMode = 1
        )
        assertTrue(event.isSequential)
    }

    @Test
    fun `PageNavigationEvent isSequential false for non-adjacent jump`() {
        val event = PageNavigationEvent(
            mangaId = 1L, chapterId = 2L, fromPage = 1, toPage = 10,
            pageDurationMs = 500L, readerMode = 0
        )
        assertFalse(event.isSequential)
    }
}
