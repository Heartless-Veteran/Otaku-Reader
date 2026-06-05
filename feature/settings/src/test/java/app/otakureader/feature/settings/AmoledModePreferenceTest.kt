package app.otakureader.feature.settings

import app.cash.turbine.test
import app.otakureader.core.discord.DiscordRpcService
import app.otakureader.core.preferences.GeneralPreferences
import app.otakureader.core.preferences.LocalSourcePreferences
import app.otakureader.feature.settings.delegate.AppearanceSettingsDelegate
import app.otakureader.feature.settings.viewmodel.AppearanceViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Pure Black (AMOLED) mode preference.
 *
 * Verifies that:
 * 1. The [AppearanceViewModel] reflects the initial AMOLED preference value from [GeneralPreferences].
 * 2. Dispatching [SettingsEvent.SetPureBlackDarkMode] calls [GeneralPreferences.setUsePureBlackDarkMode].
 * 3. The [AppearanceViewModel] state flow updates when the underlying preference flow emits a new value.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AmoledModePreferenceTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var generalPreferences: GeneralPreferences
    private lateinit var discordRpcService: DiscordRpcService
    private lateinit var delegate: AppearanceSettingsDelegate

    // Mutable backing flow so tests can push new values
    private val pureBlackFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkObject(LocalSourcePreferences.Companion)
        every { LocalSourcePreferences.defaultDirectory() } returns "/test/local"

        generalPreferences = mockk(relaxed = true) {
            // Wire the mutable flow so tests can emit values
            every { usePureBlackDarkMode } returns pureBlackFlow
            every { themeMode } returns flowOf(0)
            every { useDynamicColor } returns flowOf(true)
            every { useHighContrast } returns flowOf(false)
            every { colorScheme } returns flowOf(0)
            every { customAccentColor } returns flowOf(0xFF1976D2L)
            every { locale } returns flowOf("")
            every { notificationsEnabled } returns flowOf(true)
            every { updateCheckInterval } returns flowOf(12)
            every { showNsfwContent } returns flowOf(false)
            every { discordRpcEnabled } returns flowOf(false)
            every { autoThemeColor } returns flowOf(false)
            every { visualEffectsEnabled } returns flowOf(true)
            every { appUpdateCheckEnabled } returns flowOf(true)
            every { lastAppUpdateCheck } returns flowOf(0L)
        }

        discordRpcService = mockk(relaxed = true)

        delegate = AppearanceSettingsDelegate(
            generalPreferences = generalPreferences,
            discordRpcService = discordRpcService,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(LocalSourcePreferences.Companion)
    }

    private fun createViewModel() = AppearanceViewModel(appearanceDelegate = delegate)

    // ─────────────────────────────────────────────────────────────────────────
    // Initial default value
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `initial state has AMOLED mode disabled by default`() = runTest {
        pureBlackFlow.value = false
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(
            "usePureBlackDarkMode should be false by default",
            viewModel.state.value.usePureBlackDarkMode,
        )
    }

    @Test
    fun `initial state reflects AMOLED mode enabled from preferences`() = runTest {
        pureBlackFlow.value = true
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(
            "usePureBlackDarkMode should be true when preference is set",
            viewModel.state.value.usePureBlackDarkMode,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Preference flow emission
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `state flow updates when usePureBlackDarkMode preference emits new value`() = runTest {
        pureBlackFlow.value = false
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.state.test {
            // Consume the current item so we are waiting for the next emission
            val initial = awaitItem()
            assertFalse(initial.usePureBlackDarkMode)

            // Simulate the preference being changed externally (e.g., from another screen)
            pureBlackFlow.value = true
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertTrue(
                "State should reflect the new preference value",
                updated.usePureBlackDarkMode,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SetPureBlackDarkMode event
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `SetPureBlackDarkMode true calls setUsePureBlackDarkMode on GeneralPreferences`() = runTest {
        coEvery { generalPreferences.setUsePureBlackDarkMode(true) } returns Unit

        val viewModel = createViewModel()
        viewModel.onEvent(SettingsEvent.SetPureBlackDarkMode(true))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { generalPreferences.setUsePureBlackDarkMode(true) }
    }

    @Test
    fun `SetPureBlackDarkMode false calls setUsePureBlackDarkMode on GeneralPreferences`() = runTest {
        coEvery { generalPreferences.setUsePureBlackDarkMode(false) } returns Unit

        val viewModel = createViewModel()
        viewModel.onEvent(SettingsEvent.SetPureBlackDarkMode(false))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { generalPreferences.setUsePureBlackDarkMode(false) }
    }
}
