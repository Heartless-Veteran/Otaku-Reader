package app.otakureader.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Performance benchmarks targeting the 3 critical bottlenecks identified
 * in Jarvis analysis (April 13, 2026):
 *
 * 1. N+1 Query in Updates Feed — measures time to load updates screen
 * 2. Library Scroll Performance — measures frame drops during scroll
 * 3. Chapter List Thumbnail Loading — measures jank when thumbnails appear
 *
 * Run with:
 * ./gradlew :baselineprofile:connectedCheck
 */
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmarks {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * BOTTLENECK #1: N+1 Query in Updates Feed
     * Measures time to load updates screen with 200+ chapters
     * Target: < 500ms for updates feed to become responsive
     */
    @Test
    fun updatesFeedLoadTime() {
        benchmarkRule.measureRepeated(
            packageName = "app.otakureader",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                // Start from library
                pressHome()
                startActivityAndWait()
                waitForLibraryLoad()
            }
        ) {
            // Navigate to Updates tab
            device.findObject(By.res("app.otakureader", "nav_updates"))?.click()
            
            // Measure time until updates list is visible and responsive
            device.wait(
                Until.hasObject(By.res("app.otakureader", "updates_list")), 
                5_000
            )
            
            // Small scroll to ensure content is rendered
            device.findObject(By.res("app.otakureader", "updates_list"))
                ?.scroll(androidx.test.uiautomator.Direction.DOWN, 0.3f)
        }
    }

    /**
     * BOTTLENECK #2: Library Scroll Performance  
     * Measures frame timing during library scroll with >100 manga
     * Target: > 55fps (16ms frame time) during scroll
     */
    @Test
    fun libraryScrollPerformance() {
        benchmarkRule.measureRepeated(
            packageName = "app.otakureader",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                waitForLibraryLoad()
            }
        ) {
            val libraryList = device.findObject(By.res("app.otakureader", "library_list"))
                ?: throw AssertionError("Library list not found")

            // Perform scroll gesture and measure frame timing
            repeat(5) {
                libraryList.scroll(androidx.test.uiautomator.Direction.DOWN, 0.8f)
            }
        }
    }

    /**
     * BOTTLENECK #3: Chapter List Thumbnail Loading
     * Measures jank when chapter list loads with thumbnails
     * Target: < 3 frame drops when thumbnails appear
     */
    @Test
    fun chapterListThumbnailLoading() {
        benchmarkRule.measureRepeated(
            packageName = "app.otakureader",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                waitForLibraryLoad()
            }
        ) {
            // Open first manga in library
            device.findObject(By.res("app.otakureader", "library_grid_item"))?.click()
            
            // Wait for details screen with chapter list
            device.wait(
                Until.hasObject(By.res("app.otakureader", "chapter_list")),
                3_000
            )
            
            // Scroll through chapter list to trigger thumbnail loads
            val chapterList = device.findObject(By.res("app.otakureader", "chapter_list"))
            repeat(3) {
                chapterList?.scroll(androidx.test.uiautomator.Direction.DOWN, 0.5f)
            }
            
            // Go back
            device.pressBack()
        }
    }

    /**
     * Full cold start benchmark for regression tracking
     */
    @Test
    fun coldStartup() {
        benchmarkRule.measureRepeated(
            packageName = "app.otakureader",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.COLD,
            iterations = 5
        ) {
            pressHome()
            startActivityAndWait()
        }
    }

    private fun MacrobenchmarkScope.waitForLibraryLoad() {
        device.wait(
            Until.hasObject(By.res("app.otakureader", "library_list")),
            5_000
        )
    }
}
