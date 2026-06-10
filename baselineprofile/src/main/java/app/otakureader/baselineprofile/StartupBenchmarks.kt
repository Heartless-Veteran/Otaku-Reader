package app.otakureader.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Startup benchmark that measures cold-start time for Otaku Reader.
 *
 * This exercises the full cold-start path:
 * 1. Application creation (Hilt injection, WorkManager init)
 * 2. MainActivity launch
 * 3. NavHost + BottomBar render
 * 4. Library screen initial load (first page of lazy grid)
 *
 * Run with:
 * ```
 * ./gradlew :baselineprofile:connectedCheck -P android.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=StartupBenchmark
 * ```
 *
 * Baseline profile is auto-generated into `app/src/main/baseline-prof.txt`.
 * ProfileInstaller (androidx.profileinstaller) ensures the profile is written
 * to ART on first run so subsequent launches benefit from AOT compilation.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmarks {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupColdDefault() = startup(CompilationMode.DEFAULT)

    @Test
    fun startupColdBaselineProfileDisabled() = startup(CompilationMode.None())

    @Test
    fun startupColdBaselineProfileEnabled() = startup(CompilationMode.Partial())

    private fun startup(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = "app.otakureader",
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = 5,
            setupBlock = {
                // Force stop the app to ensure cold start every iteration
                pressHome()
            },
        ) {
            startActivityAndWait()

            // Wait until the library list is visible (indicates first frame drawn)
            device.wait(
                androidx.test.uiautomator.Until.hasObject(
                    androidx.test.uiautomator.By.res("app.otakureader", "library_list")
                ),
                5_000,
            )
        }
    }
}
