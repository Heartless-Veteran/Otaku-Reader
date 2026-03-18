import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                resourcePrefix = path.split("""\W""".toRegex()).drop(1).distinct().joinToString("_").lowercase() + "_"
                // The :data module uses a "distribution" flavor dimension (full/foss).
                // Library modules that depend on :data but don't declare the dimension
                // themselves need a default selection so Gradle can resolve the variant.
                defaultConfig {
                    missingDimensionStrategy("distribution", "full")
                }
            }
        }
    }
}
