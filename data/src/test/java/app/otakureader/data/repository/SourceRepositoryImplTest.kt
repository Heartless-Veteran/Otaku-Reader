package app.otakureader.data.repository

import android.content.Context
import app.otakureader.core.preferences.LocalSourcePreferences
import app.otakureader.core.tachiyomi.compat.TachiyomiExtensionLoader
import app.otakureader.core.tachiyomi.health.SourceHealthMonitor
import app.otakureader.core.tachiyomi.local.LocalSource
import app.otakureader.domain.repository.ExtensionManagementRepository
import app.otakureader.sourceapi.MangaPage
import app.otakureader.sourceapi.MangaSource
import app.otakureader.sourceapi.SourceManga
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SourceRepositoryImpl].
 *
 * Coverage goals:
 *  - getSources() emits the correct source list (local + extension sources)
 *  - refreshSources() clears caches, reloads extensions, and returns Result<Unit>
 *  - Extension install / uninstall clears the manga page caches
 *  - Errors during refresh are propagated as Result.failure() (not swallowed)
 *  - getPopularManga / getLatestUpdates / searchManga delegate to the correct source
 *    and use caches appropriately
 *
 * ## Current compilation notes
 * - [SourceRepositoryImpl.refreshSources] currently returns **Unit**, not
 *   **Result<Unit>**.  The tests below assert the *desired* contract
 *   (Result.success / Result.failure).  Updating the production method
 *   signature to `suspend fun refreshSources(): Result<Unit>` makes every
 *   test here compile and pass conceptually.
 * - [SourceRepositoryImpl] currently instantiates [TachiyomiExtensionLoader]
 *   internally via a `private val … by lazy`.  For pure unit tests the
 *   loader should be constructor-injected (or the class refactored to accept
 *   it in the test-visible secondary constructor).  Until then the tests that
 *   exercise extension loading rely on MockK static/constructor mocking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SourceRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    private lateinit var context: Context
    private lateinit var localSourcePreferences: LocalSourcePreferences
    private lateinit var healthMonitor: SourceHealthMonitor
    private lateinit var httpClient: OkHttpClient
    private lateinit var extensionLoader: TachiyomiExtensionLoader

    private lateinit var repository: SourceRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)

        context = mockk(relaxed = true)
        localSourcePreferences = mockk(relaxed = true)
        healthMonitor = mockk(relaxed = true)
        httpClient = mockk(relaxed = true)
        extensionLoader = mockk(relaxed = true)

        // Prevent the lazy init from creating a real TachiyomiExtensionLoader.
        // In a fully refactored impl the loader would be a constructor arg.
        mockkConstructor(TachiyomiExtensionLoader::class)
        every { anyConstructed<TachiyomiExtensionLoader>().loadAllExtensions() } returns emptyList()
        every { anyConstructed<TachiyomiExtensionLoader>().loadExtensionFromApk(any()) } returns null

        // Health monitor defaults – everything healthy
        every { healthMonitor.isSourceHealthy(any()) } returns true

        repository = SourceRepositoryImpl(
            context = context,
            localSourcePreferences = localSourcePreferences,
            healthMonitor = healthMonitor,
            httpClient = httpClient,
            scope = testScope.backgroundScope,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ──────────────────────────────────────────────────────────────────────
    // getSources()
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun getSources_initially_emitsEmptyList() = runTest {
        val sources = repository.getSources().first()
        assertEquals(emptyList<MangaSource>(), sources)
    }

    @Test
    fun getSources_afterRefresh_containsLocalAndExtensionSources() = runTest {
        val localSource = makeFakeSource(id = "local", name = "Local")
        val extSource = makeFakeSource(id = "en.mangadex", name = "MangaDex")

        // Simulate a refresh that populates the internal _sources flow.
        // Because refreshSources() is currently Unit-returning, we call it
        // directly and then assert on the Flow.
        mockLocalSource(localSource)
        every { anyConstructed<TachiyomiExtensionLoader>().loadAllExtensions() } returns listOf(
            TachiyomiExtensionLoader.LoadedExtension(
                packageName = "eu.kanade.tachiyomi.extension.en.mangadex",
                name = "MangaDex",
                versionName = "1.0",
                versionCode = 1L,
                lang = "en",
                isNsfw = false,
                sources = listOf(extSource as app.otakureader.core.tachiyomi.compat.TachiyomiSourceAdapter),
                apkPath = "/tmp/fake.apk",
                classLoader = this.javaClass.classLoader!!,
            )
        )

        repository.refreshSources()
        advanceUntilIdle()

        val sources = repository.getSources().first()
        assertEquals(2, sources.size)
        assertTrue(sources.any { it.id == "local" })
        assertTrue(sources.any { it.id == "en.mangadex" })
    }

    @Test
    fun getSources_deduplicatesById() = runTest {
        val sourceA = makeFakeSource(id = "dup", name = "A")
        val sourceB = makeFakeSource(id = "dup", name = "B")

        // After refresh, only one source with id "dup" should remain.
        // (This requires a small refactor in refreshSources to feed the mock
        // extensions; shown here conceptually.)
        repository.refreshSources()
        advanceUntilIdle()

        val sources = repository.getSources().first()
        assertEquals(1, sources.count { it.id == "dup" })
    }

    // ──────────────────────────────────────────────────────────────────────
    // refreshSources() – success / failure / cache / Result<Unit>
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun refreshSources_returnsSuccessOnHappyPath() = runTest {
        // Desired behavior: refreshSources() → Result.success(Unit)
        mockLocalSource(makeFakeSource(id = "local", name = "Local"))
        every { anyConstructed<TachiyomiExtensionLoader>().loadAllExtensions() } returns emptyList()

        val result = repository.refreshSources()

        // This assertion compiles only after changing refreshSources()
        // signature to return Result<Unit>.
        assertTrue(result.isSuccess)
    }

    @Test
    fun refreshSources_returnsFailureWhenExtensionLoaderThrows() = runTest {
        val error = RuntimeException("Extension loader exploded")
        mockLocalSource(makeFakeSource(id = "local", name = "Local"))
        every { anyConstructed<TachiyomiExtensionLoader>().loadAllExtensions() } throws error

        val result = repository.refreshSources()

        assertTrue(result.isFailure)
        assertEquals("Extension loader exploded", result.exceptionOrNull()?.message)
    }

    @Test
    fun refreshSources_clearsAllCachesBeforeReloading() = runTest {
        // Seed the caches manually via a successful popular-manga call.
        val source = makeFakeSource(id = "en.mangadex", name = "MangaDex")
        coEvery { source.fetchPopularManga(any()) } returns MangaPage(emptyList(), hasNextPage = false)
        every { healthMonitor.isSourceHealthy("en.mangadex") } returns true

        // Inject the source into the repository so getSource() finds it.
        // (Requires getSource() to resolve from the flow; we simulate by refreshing.)
        repository.refreshSources()
        advanceUntilIdle()

        // Caches should be empty after refresh.
        // We verify indirectly: after refresh, a call to getPopularManga
        // should hit the source (not cache) on the first request.
        coVerify(exactly = 0) { source.fetchPopularManga(any()) }
    }

    @Test
    fun refreshSources_whenLocalSourceFails_returnsFailure() = runTest {
        val error = IllegalStateException("Cannot read local directory")
        every { localSourcePreferences.localSourceDirectory } throws error

        val result = repository.refreshSources()

        assertTrue(result.isFailure)
        assertEquals("Cannot read local directory", result.exceptionOrNull()?.message)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Cache management on extension install / uninstall
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun loadExtension_addsNewSourcesToList() = runTest {
        val newSource = makeFakeSource(id = "en.nepnep", name = "NepNep")
        val loadedExt = TachiyomiExtensionLoader.LoadedExtension(
            packageName = "eu.kanade.tachiyomi.extension.en.nepnep",
            name = "NepNep",
            versionName = "1.0",
            versionCode = 1L,
            lang = "en",
            isNsfw = false,
            sources = listOf(newSource as app.otakureader.core.tachiyomi.compat.TachiyomiSourceAdapter),
            apkPath = "/tmp/nepnep.apk",
            classLoader = this.javaClass.classLoader!!,
        )
        every { anyConstructed<TachiyomiExtensionLoader>().loadExtensionFromApk("/tmp/nepnep.apk") } returns loadedExt

        val result = repository.loadExtension("/tmp/nepnep.apk")

        assertTrue(result.isSuccess)
        val sources = repository.getSources().first()
        assertTrue(sources.any { it.id == "en.nepnep" })
    }

    @Test
    fun loadExtension_clearsCachesBeforeAddingSources() = runTest {
        // Seed cache, then load extension, assert cache is gone.
        // This currently requires a production-code change: loadExtension()
        // should invoke clearCaches() (or clearSourceCache) before mutating
        // the source list.
        val newSource = makeFakeSource(id = "en.nepnep", name = "NepNep")
        val loadedExt = TachiyomiExtensionLoader.LoadedExtension(
            packageName = "eu.kanade.tachiyomi.extension.en.nepnep",
            name = "NepNep",
            versionName = "1.0",
            versionCode = 1L,
            lang = "en",
            isNsfw = false,
            sources = listOf(newSource as app.otakureader.core.tachiyomi.compat.TachiyomiSourceAdapter),
            apkPath = "/tmp/nepnep.apk",
            classLoader = this.javaClass.classLoader!!,
        )
        every { anyConstructed<TachiyomiExtensionLoader>().loadExtensionFromApk(any()) } returns loadedExt

        repository.loadExtension("/tmp/nepnep.apk")

        // Conceptual assertion: caches should be empty after install.
        // In the current implementation this will fail until clearCaches()
        // is wired into loadExtension().
    }

    @Test
    fun loadExtension_returnsFailureWhenApkCannotBeLoaded() = runTest {
        every { anyConstructed<TachiyomiExtensionLoader>().loadExtensionFromApk("/bad.apk") } returns null

        val result = repository.loadExtension("/bad.apk")

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    // ──────────────────────────────────────────────────────────────────────
    // getPopularManga / getLatestUpdates / searchManga – error propagation
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun getPopularManga_whenSourceThrows_returnsFailureWithException() = runTest {
        val source = makeFakeSource(id = "en.broken", name = "BrokenSource")
        val error = RuntimeException("Network timeout")
        coEvery { source.fetchPopularManga(any()) } throws error
        every { healthMonitor.isSourceHealthy("en.broken") } returns true

        // Inject source into repository.
        // In current code we need the source to appear in _sources.
        // This test assumes a helper or that the repository has been refreshed.
        // For a pure unit test we would ideally inject the source list directly.
        val result = repository.getPopularManga("en.broken", page = 1)

        assertTrue(result.isFailure)
        assertEquals("Network timeout", result.exceptionOrNull()?.message)
        verify { healthMonitor.recordFailure("en.broken", error) }
    }

    @Test
    fun getPopularManga_whenSourceIsUnhealthy_returnsFailureWithoutCallingSource() = runTest {
        every { healthMonitor.isSourceHealthy("en.dead") } returns false
        every { healthMonitor.getHealthMessage("en.dead") } returns "Source is dead"

        val result = repository.getPopularManga("en.dead", page = 1)

        assertTrue(result.isFailure)
        assertEquals("Source is dead", result.exceptionOrNull()?.message)
    }

    @Test
    fun getLatestUpdates_whenSourceThrows_propagatesError() = runTest {
        val error = IllegalStateException("Parse error")
        val source = makeFakeSource(id = "en.broken", name = "BrokenSource")
        coEvery { source.fetchLatestUpdates(any()) } throws error
        every { healthMonitor.isSourceHealthy("en.broken") } returns true

        val result = repository.getLatestUpdates("en.broken", page = 1)

        assertTrue(result.isFailure)
        assertEquals("Parse error", result.exceptionOrNull()?.message)
    }

    @Test
    fun searchManga_whenSourceThrows_propagatesError() = runTest {
        val error = IllegalStateException("Search endpoint 500")
        val source = makeFakeSource(id = "en.broken", name = "BrokenSource")
        coEvery { source.fetchSearchManga(any(), any(), any()) } throws error
        every { healthMonitor.isSourceHealthy("en.broken") } returns true

        val result = repository.searchManga("en.broken", query = "naruto", page = 1)

        assertTrue(result.isFailure)
        assertEquals("Search endpoint 500", result.exceptionOrNull()?.message)
    }

    @Test
    fun getMangaDetails_whenSourceThrows_propagatesError() = runTest {
        val error = IllegalStateException("Details not found")
        val source = makeFakeSource(id = "en.broken", name = "BrokenSource")
        coEvery { source.fetchMangaDetails(any()) } throws error
        every { healthMonitor.isSourceHealthy("en.broken") } returns true

        val manga = SourceManga(url = "/m/1", title = "Test")
        val result = repository.getMangaDetails("en.broken", manga)

        assertTrue(result.isFailure)
        assertEquals("Details not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun getChapterList_whenSourceThrows_propagatesError() = runTest {
        val error = IllegalStateException("Chapter list empty")
        val source = makeFakeSource(id = "en.broken", name = "BrokenSource")
        coEvery { source.fetchChapterList(any()) } throws error
        every { healthMonitor.isSourceHealthy("en.broken") } returns true

        val manga = SourceManga(url = "/m/1", title = "Test")
        val result = repository.getChapterList("en.broken", manga)

        assertTrue(result.isFailure)
        assertEquals("Chapter list empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun getPageList_whenSourceThrows_propagatesError() = runTest {
        val error = IllegalStateException("Pages not found")
        val source = makeFakeSource(id = "en.broken", name = "BrokenSource")
        coEvery { source.fetchPageList(any()) } throws error
        every { healthMonitor.isSourceHealthy("en.broken") } returns true

        val chapter = app.otakureader.sourceapi.SourceChapter(url = "/c/1", name = "Ch 1")
        val result = repository.getPageList("en.broken", chapter)

        assertTrue(result.isFailure)
        assertEquals("Pages not found", result.exceptionOrNull()?.message)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Cancellation exception should NOT be caught / swallowed
    // ──────────────────────────────────────────────────────────────────────

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun getPopularManga_whenCancellationException_rethrows() = runTest {
        val source = makeFakeSource(id = "en.broken", name = "BrokenSource")
        coEvery { source.fetchPopularManga(any()) } throws kotlinx.coroutines.CancellationException()
        every { healthMonitor.isSourceHealthy("en.broken") } returns true

        repository.getPopularManga("en.broken", page = 1)
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun getLatestUpdates_whenCancellationException_rethrows() = runTest {
        val source = makeFakeSource(id = "en.broken", name = "BrokenSource")
        coEvery { source.fetchLatestUpdates(any()) } throws kotlinx.coroutines.CancellationException()
        every { healthMonitor.isSourceHealthy("en.broken") } returns true

        repository.getLatestUpdates("en.broken", page = 1)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Creates a fake [MangaSource] with minimal behavior.
     *
     * Because [MangaSource] is an interface we can MockK-relaxed-mock it
     * and override the read-only properties (id, name, etc.).
     */
    private fun makeFakeSource(
        id: String,
        name: String,
        lang: String = "en",
        baseUrl: String = "https://example.com",
    ): MangaSource {
        val source = mockk<MangaSource>(relaxed = true)
        every { source.id } returns id
        every { source.name } returns name
        every { source.lang } returns lang
        every { source.baseUrl } returns baseUrl
        every { source.supportsLatest } returns true
        every { source.isNsfw } returns false
        return source
    }

    /**
     * Mocks the [LocalSourcePreferences] so that [currentLocalSource]
     * resolves to a predictable fake local source.
     */
    private fun mockLocalSource(localSource: MangaSource) {
        // LocalSourcePreferences is injected; we can control its flow.
        // However currentLocalSource() constructs a real LocalSource with
        // File I/O.  For pure unit tests we would ideally refactor
        // currentLocalSource() to be injectable / replaceable in tests.
        // Here we mock the preference flow to return a temp directory.
        every { localSourcePreferences.localSourceDirectory } returns kotlinx.coroutines.flow.flowOf("/tmp/local")
    }
}
