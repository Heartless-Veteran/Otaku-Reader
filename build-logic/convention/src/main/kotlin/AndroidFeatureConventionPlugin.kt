import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Convention plugin for feature modules.
 * Applies the Android library+Compose convention and Hilt, and includes common feature
 * dependencies: project `:core:ui`, `:core:navigation`, and `:domain`.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("otakureader.android.library")
                apply("otakureader.android.library.compose")
                apply("otakureader.android.hilt")
            }

            dependencies {
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:navigation"))
                add("implementation", project(":domain"))
            }
        }
    }
}
