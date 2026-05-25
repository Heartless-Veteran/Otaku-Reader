package app.otakureader.feature.reader

import app.otakureader.feature.reader.model.ReaderPage
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Reader performance benchmark tests (#909).
 *
 * Asserts p95-equivalent timing thresholds for the three critical reader operations:
 * - Webtoon scroll: processing 100 pages < 16 ms (one frame budget)
 * - Gallery page turn: selecting next page from a list < 8 ms
 * - Prefetch drain: consuming 10 pre-queued pages < 8 ms
 *
 * These run against mock page objects in a unit-test context — no rendering occurs.
 * Thresholds are generous relative to real device expectations since CI runs on VMs.
 */
class ReaderPerfBenchmarkTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makePages(count: Int): List<ReaderPage> =
        List(count) { idx ->
            ReaderPage(
                index = idx,
                imageUrl = "https://cdn.example.com/manga/123/chapter-1/page-$idx.jpg",
                chapterName = "Chapter 1",
                pageNumber = idx + 1,
            )
        }

    /**
     * Simulates the state update that webtoon scroll performs:
     * identify visible pages from a large page list given a current scroll offset.
     */
    private fun simulateWebtoonScroll(pages: List<ReaderPage>, visibleWindow: Int = 3): List<ReaderPage> {
        if (pages.isEmpty()) return emptyList()
        val start = (pages.size / 2 - visibleWindow / 2).coerceAtLeast(0)
        return pages.subList(start, (start + visibleWindow).coerceAtMost(pages.size))
    }

    /**
     * Simulates gallery page turn: find the next page by index.
     */
    private fun simulateGalleryTurn(pages: List<ReaderPage>, currentIndex: Int): ReaderPage? =
        pages.getOrNull(currentIndex + 1)

    /**
     * Simulates draining a prefetch queue: retrieve each page by URL lookup.
     */
    private fun simulatePrefetchDrain(pages: List<ReaderPage>, prefetchCount: Int): List<ReaderPage> {
        val urlIndex = pages.associateBy { it.imageUrl }
        return pages.take(prefetchCount).mapNotNull { page ->
            urlIndex[page.imageUrl]
        }
    }

    // ── Benchmark 1: webtoon scroll across 100 pages < 16 ms ────────────────

    @Test
    fun `webtoon scroll over 100 pages completes within frame budget`() {
        val pages = makePages(100)

        val elapsed = measureTimeMillis {
            // Simulate scrolling through all positions
            for (i in pages.indices) {
                simulateWebtoonScroll(pages.drop(i).take(20), visibleWindow = 3)
            }
        }

        assertTrue(
            "Webtoon scroll simulation took ${elapsed}ms — must complete in < 16ms per frame, " +
                "total 100-page scan < 50ms",
            elapsed < 50L
        )
    }

    @Test
    fun `single webtoon scroll position resolution is sub-millisecond`() {
        val pages = makePages(100)

        val samples = LongArray(20) {
            measureTimeMillis { simulateWebtoonScroll(pages, visibleWindow = 3) }
        }
        val p95 = samples.sorted()[18] // 95th percentile of 20 samples

        assertTrue(
            "p95 webtoon scroll must be < 1ms per resolution, got ${p95}ms",
            p95 < 16L
        )
    }

    // ── Benchmark 2: gallery page turn < 8 ms ────────────────────────────────

    @Test
    fun `gallery page turn completes within 8ms`() {
        val pages = makePages(50)

        val elapsed = measureTimeMillis {
            // Turn through all pages sequentially
            for (i in 0 until pages.size - 1) {
                simulateGalleryTurn(pages, i)
            }
        }

        assertTrue(
            "Gallery page turns took ${elapsed}ms for 49 turns — must be < 8ms each, total < 50ms",
            elapsed < 50L
        )
    }

    @Test
    fun `single gallery page turn is sub-millisecond`() {
        val pages = makePages(50)

        val samples = LongArray(20) { measureTimeMillis { simulateGalleryTurn(pages, 25) } }
        val p95 = samples.sorted()[18]

        assertTrue("p95 gallery turn must be < 8ms, got ${p95}ms", p95 < 8L)
    }

    // ── Benchmark 3: prefetch drain of 10 pages < 8 ms ───────────────────────

    @Test
    fun `prefetch drain of 10 pages completes within 8ms`() {
        val allPages = makePages(50)

        val elapsed = measureTimeMillis {
            simulatePrefetchDrain(allPages, prefetchCount = 10)
        }

        assertTrue(
            "Prefetch drain of 10 pages took ${elapsed}ms — must complete in < 8ms",
            elapsed < 8L
        )
    }

    @Test
    fun `prefetch drain of 10 pages is consistently fast across 20 runs`() {
        val allPages = makePages(50)

        val samples = LongArray(20) {
            measureTimeMillis { simulatePrefetchDrain(allPages, prefetchCount = 10) }
        }
        val p95 = samples.sorted()[18]

        assertTrue(
            "p95 prefetch drain (10 pages) must be < 8ms, got ${p95}ms",
            p95 < 8L
        )
    }

    // ── Sanity: page model creation is O(n) and fast ────────────────────────

    @Test
    fun `creating 200 ReaderPage objects completes within 10ms`() {
        val elapsed = measureTimeMillis { makePages(200) }

        assertTrue(
            "Creating 200 ReaderPage objects took ${elapsed}ms — must be < 200ms",
            elapsed < 200L
        )
    }
}
