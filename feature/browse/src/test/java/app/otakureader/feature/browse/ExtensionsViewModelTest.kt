package app.otakureader.feature.browse

import app.otakureader.core.extension.domain.model.Extension
import app.otakureader.core.extension.domain.model.ExtensionSource
import app.otakureader.core.extension.domain.model.InstallStatus
import app.otakureader.core.extension.domain.repository.ExtensionRepoRepository
import app.otakureader.core.extension.domain.repository.ExtensionRepository
import app.otakureader.core.extension.installer.ExtensionInstaller
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.domain.repository.ExtensionManagementRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExtensionsViewModelTest {

    private lateinit var viewModel: ExtensionsViewModel

    private val extensionRepository: ExtensionRepository = mockk(relaxed = true)
    private val extensionInstaller: ExtensionInstaller = mockk(relaxed = true)
    private val extensionRepoRepository: ExtensionRepoRepository = mockk(relaxed = true)
    private val extensionManagementRepository: ExtensionManagementRepository = mockk(relaxed = true)
    private val generalPreferences: GeneralPreferences = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    // Mutable flows to control repository emissions
    private val installedExtensionsFlow = MutableStateFlow(emptyList<Extension>())
    private val availableExtensionsFlow = MutableStateFlow(emptyList<Extension>())
    private val extensionsWithUpdatesFlow = MutableStateFlow(emptyList<Extension>())
    private val repositoriesFlow = MutableStateFlow(emptyList<String>())
    private val showNsfwFlow = MutableStateFlow(false)

    companion object {
        private fun createExtension(
            id: Long = 1L,
            pkgName: String = "app.test.extension",
            name: String = "Test Extension",
            versionCode: Int = 1,
            versionName: String = "1.0",
            sources: List<ExtensionSource> = emptyList(),
            status: InstallStatus = InstallStatus.INSTALLED,
            apkPath: String? = null,
            apkUrl: String? = "https://example.com/test.apk",
            iconUrl: String? = null,
            lang: String = "en",
            isNsfw: Boolean = false,
            installDate: Long? = System.currentTimeMillis(),
            signatureHash: String? = null,
            isShared: Boolean = true,
            isEnabled: Boolean = true
        ) = Extension(
            id = id,
            pkgName = pkgName,
            name = name,
            versionCode = versionCode,
            versionName = versionName,
            sources = sources,
            status = status,
            apkPath = apkPath,
            apkUrl = apkUrl,
            iconUrl = iconUrl,
            lang = lang,
            isNsfw = isNsfw,
            installDate = installDate,
            signatureHash = signatureHash,
            isShared = isShared,
            isEnabled = isEnabled
        )

        private fun createSource(
            id: Long = 1L,
            name: String = "Test Source",
            lang: String = "en",
            baseUrl: String = "https://example.com",
            supportsSearch: Boolean = true,
            supportsLatest: Boolean = true
        ) = ExtensionSource(
            id = id,
            name = name,
            lang = lang,
            baseUrl = baseUrl,
            supportsSearch = supportsSearch,
            supportsLatest = supportsLatest
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Default flow stubs — must be set BEFORE ViewModel creation because init block triggers them
        every { extensionRepository.getInstalledExtensions() } returns installedExtensionsFlow
        every { extensionRepository.getAvailableExtensions() } returns availableExtensionsFlow
        every { extensionRepository.getExtensionsWithUpdates() } returns extensionsWithUpdatesFlow
        every { generalPreferences.showNsfwContent } returns showNsfwFlow
        every { extensionRepoRepository.getRepositories() } returns repositoriesFlow
        coEvery { extensionRepoRepository.getActiveRepository() } returns null
        coEvery { extensionRepository.refreshAvailableExtensions() } returns Result.success(Unit)
        coEvery { extensionRepository.checkForUpdates() } returns 0
        coEvery { extensionManagementRepository.refreshSources() } just runs

        viewModel = ExtensionsViewModel(
            extensionRepository = extensionRepository,
            extensionInstaller = extensionInstaller,
            extensionRepoRepository = extensionRepoRepository,
            extensionManagementRepository = extensionManagementRepository,
            generalPreferences = generalPreferences
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Initial State & Loading
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `initial state is loading with empty lists`() = runTest {
        val state = viewModel.state.value
        assertTrue("Expected loading initially", state.isLoading)
        assertEquals(emptyList<Extension>(), state.installedExtensions)
        assertEquals(emptyList<Extension>(), state.availableExtensions)
        assertEquals(emptyList<Extension>(), state.extensionsWithUpdates)
        assertEquals(0, state.updateCount)
        assertEquals("", state.searchQuery)
        assertEquals(SortMode.NAME, state.sortMode)
        assertFalse(state.showNsfw)
        assertNull(state.error)
    }

    @Test
    fun `loadExtensions populates installed extensions`() = runTest {
        val ext1 = createExtension(id = 1L, pkgName = "app.ext1", name = "Ext 1")
        val ext2 = createExtension(id = 2L, pkgName = "app.ext2", name = "Ext 2")

        installedExtensionsFlow.value = listOf(ext1, ext2)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.installedExtensions.size)
        assertEquals("Ext 1", state.installedExtensions[0].name)
        assertEquals("Ext 2", state.installedExtensions[1].name)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadExtensions populates available extensions`() = runTest {
        val ext = createExtension(id = 3L, pkgName = "app.ext3", name = "Available Ext", status = InstallStatus.AVAILABLE)

        availableExtensionsFlow.value = listOf(ext)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.availableExtensions.size)
        assertEquals("Available Ext", state.availableExtensions[0].name)
    }

    @Test
    fun `loadExtensions populates extensions with updates`() = runTest {
        val ext = createExtension(id = 4L, pkgName = "app.ext4", name = "Update Ext", status = InstallStatus.HAS_UPDATE)

        extensionsWithUpdatesFlow.value = listOf(ext)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.extensionsWithUpdates.size)
        assertEquals(1, state.updateCount)
    }

    @Test
    fun `repository failure sets error state`() = runTest {
        // Create fresh ViewModel with failing installed extensions flow
        every { extensionRepository.getInstalledExtensions() } returns flow {
            throw RuntimeException("Network error")
        }

        val vm = ExtensionsViewModel(
            extensionRepository = extensionRepository,
            extensionInstaller = extensionInstaller,
            extensionRepoRepository = extensionRepoRepository,
            extensionManagementRepository = extensionManagementRepository,
            generalPreferences = generalPreferences
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Search & Filter
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `search query change updates state`() = runTest {
        viewModel.onEvent(ExtensionsEvent.OnSearchQueryChange("One Piece"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("One Piece", viewModel.state.value.searchQuery)
    }

    @Test
    fun `search filters installed extensions by name`() = runTest {
        val ext1 = createExtension(id = 1L, pkgName = "app.one", name = "One Piece Ext")
        val ext2 = createExtension(id = 2L, pkgName = "app.naruto", name = "Naruto Ext")

        installedExtensionsFlow.value = listOf(ext1, ext2)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ExtensionsEvent.OnSearchQueryChange("One"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.installedExtensions.size)
        assertEquals("One Piece Ext", viewModel.state.value.installedExtensions[0].name)
    }

    @Test
    fun `search filters extensions by source name`() = runTest {
        val source = createSource(name = "MangaDex")
        val ext = createExtension(id = 1L, name = "Generic Ext", sources = listOf(source))

        installedExtensionsFlow.value = listOf(ext)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ExtensionsEvent.OnSearchQueryChange("MangaDex"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.installedExtensions.size)
    }

    @Test
    fun `NSFW filter hides NSFW extensions when disabled`() = runTest {
        val safeExt = createExtension(id = 1L, name = "Safe Ext", isNsfw = false)
        val nsfwExt = createExtension(id = 2L, name = "NSFW Ext", isNsfw = true)

        installedExtensionsFlow.value = listOf(safeExt, nsfwExt)
        testDispatcher.scheduler.advanceUntilIdle()

        showNsfwFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.installedExtensions.size)
        assertEquals("Safe Ext", viewModel.state.value.installedExtensions[0].name)
    }

    @Test
    fun `NSFW filter shows NSFW extensions when enabled`() = runTest {
        val safeExt = createExtension(id = 1L, name = "Safe Ext", isNsfw = false)
        val nsfwExt = createExtension(id = 2L, name = "NSFW Ext", isNsfw = true)

        installedExtensionsFlow.value = listOf(safeExt, nsfwExt)
        showNsfwFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.installedExtensions.size)
    }

    @Test
    fun `toggle NSFW updates preference`() = runTest {
        coEvery { generalPreferences.setShowNsfwContent(true) } just runs

        viewModel.onEvent(ExtensionsEvent.ToggleNsfw(true))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { generalPreferences.setShowNsfwContent(true) }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Sorting
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `sort by name orders alphabetically`() = runTest {
        val extB = createExtension(id = 1L, name = "Beta Ext")
        val extA = createExtension(id = 2L, name = "Alpha Ext")

        installedExtensionsFlow.value = listOf(extB, extA)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ExtensionsEvent.SetSortMode(SortMode.NAME))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Alpha Ext", viewModel.state.value.installedExtensions[0].name)
        assertEquals("Beta Ext", viewModel.state.value.installedExtensions[1].name)
    }

    @Test
    fun `sort by recently added orders by install date descending`() = runTest {
        val extOld = createExtension(id = 1L, name = "Old Ext", installDate = 1000L)
        val extNew = createExtension(id = 2L, name = "New Ext", installDate = 5000L)

        installedExtensionsFlow.value = listOf(extOld, extNew)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ExtensionsEvent.SetSortMode(SortMode.RECENTLY_ADDED))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("New Ext", viewModel.state.value.installedExtensions[0].name)
        assertEquals("Old Ext", viewModel.state.value.installedExtensions[1].name)
    }

    @Test
    fun `sort by language groups by lang then name`() = runTest {
        val extEn = createExtension(id = 1L, name = "English Ext", lang = "en")
        val extJa = createExtension(id = 2L, name = "Japanese Ext", lang = "ja")
        val extEn2 = createExtension(id = 3L, name = "Another English", lang = "en")

        installedExtensionsFlow.value = listOf(extEn, extJa, extEn2)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ExtensionsEvent.SetSortMode(SortMode.LANGUAGE))
        testDispatcher.scheduler.advanceUntilIdle()

        val sorted = viewModel.state.value.installedExtensions
        assertEquals("en", sorted[0].lang)
        assertEquals("en", sorted[1].lang)
        assertEquals("ja", sorted[2].lang)
        assertEquals("Another English", sorted[0].name) // alphabetical within en
        assertEquals("English Ext", sorted[1].name)
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Repository Management
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `observe repositories populates repository list`() = runTest {
        repositoriesFlow.value = listOf("https://repo1.com", "https://repo2.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.repositories.size)
        assertEquals("https://repo1.com", viewModel.state.value.repositories[0])
    }

    @Test
    fun `add repository calls repo repository`() = runTest {
        coEvery { extensionRepoRepository.addRepository("https://newrepo.com") } just runs

        viewModel.onEvent(ExtensionsEvent.AddRepository("https://newrepo.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { extensionRepoRepository.addRepository("https://newrepo.com") }
    }

    @Test
    fun `add blank repository does nothing`() = runTest {
        viewModel.onEvent(ExtensionsEvent.AddRepository("   "))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { extensionRepoRepository.addRepository(any()) }
    }

    @Test
    fun `remove repository calls repo repository`() = runTest {
        coEvery { extensionRepoRepository.removeRepository("https://repo1.com") } just runs

        viewModel.onEvent(ExtensionsEvent.RemoveRepository("https://repo1.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { extensionRepoRepository.removeRepository("https://repo1.com") }
    }

    @Test
    fun `set active repository updates state and refreshes`() = runTest {
        coEvery { extensionRepoRepository.setActiveRepository("https://repo1.com") } just runs
        coEvery { extensionRepository.refreshAvailableExtensions() } returns Result.success(Unit)
        coEvery { extensionRepository.checkForUpdates() } returns 0

        viewModel.onEvent(ExtensionsEvent.SetActiveRepository("https://repo1.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://repo1.com", viewModel.state.value.activeRepository)
        coVerify { extensionRepoRepository.setActiveRepository("https://repo1.com") }
        coVerify { extensionRepository.refreshAvailableExtensions() }
    }

    @Test
    fun `set active repository with failure does not update state`() = runTest {
        coEvery { extensionRepoRepository.setActiveRepository("https://bad.com") } throws RuntimeException("Bad repo")

        val before = viewModel.state.value.activeRepository

        viewModel.onEvent(ExtensionsEvent.SetActiveRepository("https://bad.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(before, viewModel.state.value.activeRepository)
    }

    // ─────────────────────────────────────────────────────────────
    // 5. Extension Installation
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `install extension success emits snackbar and refreshes sources`() = runTest {
        val ext = createExtension(id = 1L, name = "New Ext", apkUrl = "https://example.com/ext.apk")

        coEvery { extensionInstaller.downloadAndInstall(ext) } returns Result.success(ext)
        coEvery { extensionManagementRepository.refreshSources() } just runs

        viewModel.onEvent(ExtensionsEvent.InstallExtension(ext))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { extensionInstaller.downloadAndInstall(ext) }
        coVerify { extensionManagementRepository.refreshSources() }

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowSnackbar)
        assertEquals("Extension installed: New Ext", (effect as ExtensionsEffect.ShowSnackbar).message)
    }

    @Test
    fun `install extension failure emits error effect`() = runTest {
        val ext = createExtension(id = 1L, name = "Bad Ext", apkUrl = "https://example.com/bad.apk")
        val error = RuntimeException("Download failed")

        coEvery { extensionInstaller.downloadAndInstall(ext) } returns Result.failure(error)

        viewModel.onEvent(ExtensionsEvent.InstallExtension(ext))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowError)
        assertTrue((effect as ExtensionsEffect.ShowError).message.contains("Failed to install"))
    }

    @Test
    fun `install extension exception emits error effect`() = runTest {
        val ext = createExtension(id = 1L, name = "Crash Ext", apkUrl = "https://example.com/crash.apk")

        coEvery { extensionInstaller.downloadAndInstall(ext) } throws RuntimeException("Crash!")

        viewModel.onEvent(ExtensionsEvent.InstallExtension(ext))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowError)
    }

    // ─────────────────────────────────────────────────────────────
    // 6. Extension Uninstallation
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `uninstall extension success emits snackbar and refreshes sources`() = runTest {
        val ext = createExtension(id = 1L, pkgName = "app.ext1", name = "Remove Me")

        coEvery { extensionInstaller.uninstall("app.ext1") } returns Result.success(Unit)
        coEvery { extensionManagementRepository.refreshSources() } just runs

        viewModel.onEvent(ExtensionsEvent.UninstallExtension(ext))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { extensionInstaller.uninstall("app.ext1") }
        coVerify { extensionManagementRepository.refreshSources() }

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowSnackbar)
        assertEquals("Extension uninstalled: Remove Me", (effect as ExtensionsEffect.ShowSnackbar).message)
    }

    @Test
    fun `uninstall extension failure emits error effect`() = runTest {
        val ext = createExtension(id = 1L, pkgName = "app.stuck", name = "Stuck Ext")

        coEvery { extensionInstaller.uninstall("app.stuck") } returns Result.failure(RuntimeException("Can't uninstall"))

        viewModel.onEvent(ExtensionsEvent.UninstallExtension(ext))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowError)
        assertTrue((effect as ExtensionsEffect.ShowError).message.contains("Failed to uninstall"))
    }

    // ─────────────────────────────────────────────────────────────
    // 7. Extension Update
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `update extension success emits snackbar and refreshes sources`() = runTest {
        val ext = createExtension(id = 1L, name = "Update Me", apkUrl = "https://example.com/update.apk")

        coEvery { extensionInstaller.downloadAndInstall(ext) } returns Result.success(ext)
        coEvery { extensionManagementRepository.refreshSources() } just runs

        viewModel.onEvent(ExtensionsEvent.UpdateExtension(ext))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowSnackbar)
        assertEquals("Extension updated: Update Me", (effect as ExtensionsEffect.ShowSnackbar).message)
    }

    @Test
    fun `update extension failure emits error effect`() = runTest {
        val ext = createExtension(id = 1L, name = "Fail Update", apkUrl = "https://example.com/fail.apk")

        coEvery { extensionInstaller.downloadAndInstall(ext) } returns Result.failure(RuntimeException("Update failed"))

        viewModel.onEvent(ExtensionsEvent.UpdateExtension(ext))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowError)
        assertTrue((effect as ExtensionsEffect.ShowError).message.contains("Failed to update"))
    }

    // ─────────────────────────────────────────────────────────────
    // 8. Update All Extensions
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `update all extensions with no updates does nothing`() = runTest {
        extensionsWithUpdatesFlow.value = emptyList()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ExtensionsEvent.UpdateAllExtensions)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { extensionInstaller.downloadAndInstall(any()) }
    }

    @Test
    fun `update all extensions succeeds for all`() = runTest {
        val ext1 = createExtension(id = 1L, name = "Ext 1", status = InstallStatus.HAS_UPDATE)
        val ext2 = createExtension(id = 2L, name = "Ext 2", status = InstallStatus.HAS_UPDATE)

        extensionsWithUpdatesFlow.value = listOf(ext1, ext2)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { extensionInstaller.downloadAndInstall(ext1) } returns Result.success(ext1)
        coEvery { extensionInstaller.downloadAndInstall(ext2) } returns Result.success(ext2)
        coEvery { extensionManagementRepository.refreshSources() } just runs

        viewModel.onEvent(ExtensionsEvent.UpdateAllExtensions)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isUpdatingAll)

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowSnackbar)
        assertEquals("All 2 extensions updated", (effect as ExtensionsEffect.ShowSnackbar).message)
    }

    @Test
    fun `update all extensions partial failure reports mixed results`() = runTest {
        val ext1 = createExtension(id = 1L, name = "Ext 1", status = InstallStatus.HAS_UPDATE)
        val ext2 = createExtension(id = 2L, name = "Ext 2", status = InstallStatus.HAS_UPDATE)

        extensionsWithUpdatesFlow.value = listOf(ext1, ext2)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { extensionInstaller.downloadAndInstall(ext1) } returns Result.success(ext1)
        coEvery { extensionInstaller.downloadAndInstall(ext2) } returns Result.failure(RuntimeException("Fail"))
        coEvery { extensionManagementRepository.refreshSources() } just runs

        viewModel.onEvent(ExtensionsEvent.UpdateAllExtensions)
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowSnackbar)
        assertEquals("1 updated, 1 failed", (effect as ExtensionsEffect.ShowSnackbar).message)
    }

    @Test
    fun `update all extensions complete failure reports error`() = runTest {
        val ext = createExtension(id = 1L, name = "Fail Ext", status = InstallStatus.HAS_UPDATE)

        extensionsWithUpdatesFlow.value = listOf(ext)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { extensionInstaller.downloadAndInstall(ext) } returns Result.failure(RuntimeException("Fail"))

        viewModel.onEvent(ExtensionsEvent.UpdateAllExtensions)
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowError)
        assertEquals("All updates failed", (effect as ExtensionsEffect.ShowError).message)
    }

    @Test
    fun `update all sets isUpdatingAll flag during operation`() = runTest {
        val ext = createExtension(id = 1L, name = "Slow Ext", status = InstallStatus.HAS_UPDATE)

        extensionsWithUpdatesFlow.value = listOf(ext)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { extensionInstaller.downloadAndInstall(ext) } coAnswers {
            // Simulate some work
            kotlinx.coroutines.delay(100)
            Result.success(ext)
        }
        coEvery { extensionManagementRepository.refreshSources() } just runs

        viewModel.onEvent(ExtensionsEvent.UpdateAllExtensions)
        testDispatcher.scheduler.advanceTimeBy(50)

        assertTrue(viewModel.state.value.isUpdatingAll)

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isUpdatingAll)
    }

    // ─────────────────────────────────────────────────────────────
    // 9. Toggle Extension Enabled
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `toggle extension enabled calls repository and refreshes sources`() = runTest {
        val ext = createExtension(id = 1L, pkgName = "app.ext1", name = "Toggle Me")

        coEvery { extensionRepository.setExtensionEnabled("app.ext1", false) } just runs
        coEvery { extensionManagementRepository.refreshSources() } just runs

        viewModel.onEvent(ExtensionsEvent.ToggleExtensionEnabled(ext, false))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { extensionRepository.setExtensionEnabled("app.ext1", false) }
        coVerify { extensionManagementRepository.refreshSources() }
    }

    @Test
    fun `toggle extension enabled failure emits error effect`() = runTest {
        val ext = createExtension(id = 1L, pkgName = "app.broken", name = "Broken Toggle")

        coEvery { extensionRepository.setExtensionEnabled("app.broken", true) } throws RuntimeException("Can't toggle")

        viewModel.onEvent(ExtensionsEvent.ToggleExtensionEnabled(ext, true))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowError)
        assertTrue((effect as ExtensionsEffect.ShowError).message.contains("Failed to update extension"))
    }

    // ─────────────────────────────────────────────────────────────
    // 10. Refresh
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `refresh calls repository refresh and checkForUpdates`() = runTest {
        coEvery { extensionRepository.refreshAvailableExtensions() } returns Result.success(Unit)
        coEvery { extensionRepository.checkForUpdates() } returns 3

        viewModel.onEvent(ExtensionsEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { extensionRepository.refreshAvailableExtensions() }
        coVerify { extensionRepository.checkForUpdates() }
    }

    @Test
    fun `refresh failure sets error state`() = runTest {
        coEvery { extensionRepository.refreshAvailableExtensions() } returns Result.failure(RuntimeException("Refresh failed"))

        viewModel.onEvent(ExtensionsEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertEquals("Refresh failed", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    // ─────────────────────────────────────────────────────────────
    // 11. Combined Search + NSFW + Sort
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `combined filter search and NSFW and sort`() = runTest {
        val sourceA = createSource(name = "Source A")
        val extSafeA = createExtension(id = 1L, name = "Alpha Safe", isNsfw = false, lang = "en", sources = listOf(sourceA))
        val extSafeB = createExtension(id = 2L, name = "Beta Safe", isNsfw = false, lang = "ja")
        val extNsfw = createExtension(id = 3L, name = "Alpha NSFW", isNsfw = true, lang = "en")

        installedExtensionsFlow.value = listOf(extSafeA, extSafeB, extNsfw)
        testDispatcher.scheduler.advanceUntilIdle()

        // Search for "Alpha" — should find safeA and nsfw
        viewModel.onEvent(ExtensionsEvent.OnSearchQueryChange("Alpha"))
        testDispatcher.scheduler.advanceUntilIdle()

        // NSFW off — only safeA
        assertEquals(1, viewModel.state.value.installedExtensions.size)
        assertEquals("Alpha Safe", viewModel.state.value.installedExtensions[0].name)

        // Enable NSFW — now safeA + nsfw
        showNsfwFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.installedExtensions.size)

        // Sort by language
        viewModel.onEvent(ExtensionsEvent.SetSortMode(SortMode.LANGUAGE))
        testDispatcher.scheduler.advanceUntilIdle()

        val sorted = viewModel.state.value.installedExtensions
        assertEquals("en", sorted[0].lang)
        assertEquals("Alpha Safe", sorted[0].name)
        assertEquals("Alpha NSFW", sorted[1].name)
    }

    // ─────────────────────────────────────────────────────────────
    // 12. Error Handling for Available & Updates Streams
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `available extensions failure does not crash`() = runTest {
        every { extensionRepository.getAvailableExtensions() } returns flow {
            throw RuntimeException("Available stream error")
        }

        val vm = ExtensionsViewModel(
            extensionRepository = extensionRepository,
            extensionInstaller = extensionInstaller,
            extensionRepoRepository = extensionRepoRepository,
            extensionManagementRepository = extensionManagementRepository,
            generalPreferences = generalPreferences
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not crash and state should still be usable
        assertNotNull(vm.state.value)
    }

    @Test
    fun `extensions with updates failure does not crash`() = runTest {
        every { extensionRepository.getExtensionsWithUpdates() } returns flow {
            throw RuntimeException("Updates stream error")
        }

        val vm = ExtensionsViewModel(
            extensionRepository = extensionRepository,
            extensionInstaller = extensionInstaller,
            extensionRepoRepository = extensionRepoRepository,
            extensionManagementRepository = extensionManagementRepository,
            generalPreferences = generalPreferences
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.state.value)
    }

    // ─────────────────────────────────────────────────────────────
    // 13. State Persistence — SortMode & SearchQuery
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `sort mode persists in ViewModel state`() = runTest {
        viewModel.onEvent(ExtensionsEvent.SetSortMode(SortMode.RECENTLY_ADDED))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SortMode.RECENTLY_ADDED, viewModel.state.value.sortMode)

        viewModel.onEvent(ExtensionsEvent.SetSortMode(SortMode.LANGUAGE))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SortMode.LANGUAGE, viewModel.state.value.sortMode)
    }

    @Test
    fun `search query persists after other events`() = runTest {
        viewModel.onEvent(ExtensionsEvent.OnSearchQueryChange("Naruto"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ExtensionsEvent.SetSortMode(SortMode.NAME))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Naruto", viewModel.state.value.searchQuery)
    }

    @Test
    fun `active repository persists in state`() = runTest {
        coEvery { extensionRepoRepository.setActiveRepository("https://repo.com") } just runs
        coEvery { extensionRepository.refreshAvailableExtensions() } returns Result.success(Unit)
        coEvery { extensionRepository.checkForUpdates() } returns 0

        viewModel.onEvent(ExtensionsEvent.SetActiveRepository("https://repo.com"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://repo.com", viewModel.state.value.activeRepository)
    }

    // ─────────────────────────────────────────────────────────────
    // 14. Install/Update/Uninstall — Race Condition: refreshSources before ShowSnackbar
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `install extension completes refreshSources before emitting snackbar`() = runTest {
        val ext = createExtension(id = 1L, name = "Race Ext", apkUrl = "https://example.com/race.apk")
        var refreshCalled = false

        coEvery { extensionInstaller.downloadAndInstall(ext) } returns Result.success(ext)
        coEvery { extensionManagementRepository.refreshSources() } coAnswers {
            refreshCalled = true
            // Small delay to ensure ordering is observable
            kotlinx.coroutines.delay(10)
        }

        viewModel.onEvent(ExtensionsEvent.InstallExtension(ext))
        testDispatcher.scheduler.advanceUntilIdle()

        // The effect is emitted after refreshSources() completes (sequential in the same coroutine)
        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue("refreshSources should have been called before effect emission", refreshCalled)
        assertTrue(effect is ExtensionsEffect.ShowSnackbar)
    }

    @Test
    fun `uninstall extension completes refreshSources before emitting snackbar`() = runTest {
        val ext = createExtension(id = 1L, pkgName = "app.race", name = "Race Uninstall")
        var refreshCalled = false

        coEvery { extensionInstaller.uninstall("app.race") } returns Result.success(Unit)
        coEvery { extensionManagementRepository.refreshSources() } coAnswers {
            refreshCalled = true
            kotlinx.coroutines.delay(10)
        }

        viewModel.onEvent(ExtensionsEvent.UninstallExtension(ext))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue("refreshSources should have been called before effect emission", refreshCalled)
        assertTrue(effect is ExtensionsEffect.ShowSnackbar)
    }

    @Test
    fun `update extension completes refreshSources before emitting snackbar`() = runTest {
        val ext = createExtension(id = 1L, name = "Race Update", apkUrl = "https://example.com/race.apk")
        var refreshCalled = false

        coEvery { extensionInstaller.downloadAndInstall(ext) } returns Result.success(ext)
        coEvery { extensionManagementRepository.refreshSources() } coAnswers {
            refreshCalled = true
            kotlinx.coroutines.delay(10)
        }

        viewModel.onEvent(ExtensionsEvent.UpdateExtension(ext))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue("refreshSources should have been called before effect emission", refreshCalled)
        assertTrue(effect is ExtensionsEffect.ShowSnackbar)
    }

    // ─────────────────────────────────────────────────────────────
    // 15. Optimistic / Status States
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `extension with INSTALLING status is included in lists`() = runTest {
        val installingExt = createExtension(id = 1L, name = "Installing Ext", status = InstallStatus.INSTALLING)

        installedExtensionsFlow.value = listOf(installingExt)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.installedExtensions.size)
        assertEquals(InstallStatus.INSTALLING, viewModel.state.value.installedExtensions[0].status)
    }

    @Test
    fun `extension with UPDATING status is included in lists`() = runTest {
        val updatingExt = createExtension(id = 1L, name = "Updating Ext", status = InstallStatus.UPDATING)

        installedExtensionsFlow.value = listOf(updatingExt)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.installedExtensions.size)
        assertEquals(InstallStatus.UPDATING, viewModel.state.value.installedExtensions[0].status)
    }

    @Test
    fun `extension with UNINSTALLING status is included in lists`() = runTest {
        val uninstallingExt = createExtension(id = 1L, name = "Uninstalling Ext", status = InstallStatus.UNINSTALLING)

        installedExtensionsFlow.value = listOf(uninstallingExt)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.installedExtensions.size)
        assertEquals(InstallStatus.UNINSTALLING, viewModel.state.value.installedExtensions[0].status)
    }

    @Test
    fun `extension with ERROR status is included in lists`() = runTest {
        val errorExt = createExtension(id = 1L, name = "Error Ext", status = InstallStatus.ERROR)

        installedExtensionsFlow.value = listOf(errorExt)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.installedExtensions.size)
        assertEquals(InstallStatus.ERROR, viewModel.state.value.installedExtensions[0].status)
    }

    // ─────────────────────────────────────────────────────────────
    // 16. Error effect emission (ShowError)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `add repository failure emits ShowError effect`() = runTest {
        coEvery { extensionRepoRepository.addRepository("https://bad") } throws IllegalArgumentException("Invalid")

        viewModel.onEvent(ExtensionsEvent.AddRepository("https://bad"))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = withTimeoutOrNull(1000) { viewModel.effect.first() }
        assertTrue(effect is ExtensionsEffect.ShowError)
        assertEquals("Invalid repository URL", (effect as ExtensionsEffect.ShowError).message)
    }

    @Test
    fun `refresh sets loading true during operation`() = runTest {
        coEvery { extensionRepository.refreshAvailableExtensions() } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }
        coEvery { extensionRepository.checkForUpdates() } coAnswers {
            kotlinx.coroutines.delay(50)
            0
        }

        viewModel.onEvent(ExtensionsEvent.Refresh)
        testDispatcher.scheduler.advanceTimeBy(50)

        assertTrue(viewModel.state.value.isLoading)

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
    }

    // ─────────────────────────────────────────────────────────────
    // 17. Empty query shows all extensions
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `empty search query shows all extensions`() = runTest {
        val ext1 = createExtension(id = 1L, name = "Ext 1")
        val ext2 = createExtension(id = 2L, name = "Ext 2")

        installedExtensionsFlow.value = listOf(ext1, ext2)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ExtensionsEvent.OnSearchQueryChange("Ext"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.installedExtensions.size)

        viewModel.onEvent(ExtensionsEvent.OnSearchQueryChange(""))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.installedExtensions.size)
    }

    // ─────────────────────────────────────────────────────────────
    // 18. Repository error does not block other streams
    // ─────────────────────────────────────────────────────────────

    @Test
    fun `repository stream error does not block installed extensions`() = runTest {
        every { extensionRepoRepository.getRepositories() } returns emptyFlow()
        coEvery { extensionRepoRepository.getActiveRepository() } throws RuntimeException("Repo error")

        val ext = createExtension(id = 1L, name = "Still Works")
        installedExtensionsFlow.value = listOf(ext)

        val vm = ExtensionsViewModel(
            extensionRepository = extensionRepository,
            extensionInstaller = extensionInstaller,
            extensionRepoRepository = extensionRepoRepository,
            extensionManagementRepository = extensionManagementRepository,
            generalPreferences = generalPreferences
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Installed extensions should still be loaded despite repo error
        assertEquals(1, vm.state.value.installedExtensions.size)
        assertEquals("Still Works", vm.state.value.installedExtensions[0].name)
    }
}
