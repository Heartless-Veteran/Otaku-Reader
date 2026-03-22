package app.otakureader.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline profile generator for Otaku Reader.
 *
 * **T-4:** Expanded from a minimal launch-only profile to exercise the critical user
 * journeys that benefit most from AOT compilation:
 *
 * 1. Cold start — app launch and main screen render
 * 2. Library scroll — the most common interaction after launch
 * 3. Navigation to Browse — triggers extension loading code paths
 * 4. Navigation to Settings — exercises preferences and AI key UI
 *
 * Run with:
 * ```
 * ./gradlew :baselineprofile:generateBaselineProfile
 * ```
 * The generated profile is written to `app/src/main/baseline-prof.txt`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "app.otakureader",
        ) {
            // Journey 1: Cold start — press home first to ensure a cold launch
            pressHome()
            startActivityAndWait()

            // Journey 2: Library scroll — wait for the library list to appear and scroll it
            device.wait(Until.hasObject(By.res("app.otakureader", "library_list")), 3_000)
            device.findObject(By.res("app.otakureader", "library_list"))
                ?.also { list ->
                    repeat(3) { list.scroll(androidx.test.uiautomator.Direction.DOWN, 0.5f) }
                }

            // Journey 3: Navigate to Browse tab
            device.findObject(By.res("app.otakureader", "nav_browse"))?.click()
            device.wait(Until.hasObject(By.res("app.otakureader", "browse_screen")), 3_000)

            // Journey 4: Navigate to Settings tab
            device.findObject(By.res("app.otakureader", "nav_settings"))?.click()
            device.wait(Until.hasObject(By.res("app.otakureader", "settings_screen")), 3_000)
        }
    }
}
