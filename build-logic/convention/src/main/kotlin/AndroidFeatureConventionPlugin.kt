import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Convention plugin for feature modules.
 * Applies the Android library plugin and includes common feature dependencies:
 * Compose, Hilt, Navigation, Lifecycle, and project `:core:ui` and `:core:navigation`.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("otakureader.android.library")
                apply("otakureader.android.hilt")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            extensions.configure<LibraryExtension> {
                buildFeatures { compose = true }
            }

            dependencies {
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:navigation"))
                add("implementation", project(":domain"))
            }
        }
    }
}
