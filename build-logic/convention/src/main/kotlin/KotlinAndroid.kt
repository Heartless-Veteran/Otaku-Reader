import app.otakureader.buildlogic.AndroidConfig
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Configures common Kotlin/Android settings for both application and library modules.
 *
 * SDK API levels are **not** hardcoded here. They are read from the version catalog
 * via [AndroidConfig] so that the entire project can be updated by editing a single
 * file (`gradle/libs.versions.toml`).
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension
) {
    val libs = the<VersionCatalogsExtension>().named("libs")

    commonExtension.apply {
        compileSdk = AndroidConfig.compileSdk(libs)

        defaultConfig.minSdk = AndroidConfig.minSdk(libs)

        // H-10: Explicitly declare targetSdk for application modules so the app does not
        // inherit an outdated default, which would cause Play Store rejection and missing
        // behavioral changes. Library modules don't have targetSdk in their defaultConfig.
        // Keep this in sync with compileSdk unless there is a specific reason to target
        // an older API level (e.g. a breaking behavior change in the new SDK).
        // SDK values are centrally managed in gradle/libs.versions.toml via AndroidConfig.
        if (commonExtension is ApplicationExtension) {
            (commonExtension as ApplicationExtension).defaultConfig.targetSdk = AndroidConfig.targetSdk(libs)
        }

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }
    }

    extensions.configure(KotlinAndroidProjectExtension::class.java) {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                listOf(
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-opt-in=kotlinx.coroutines.FlowPreview"
                )
            )
        }
    }

    dependencies.add("coreLibraryDesugaring", libs.findLibrary("android.desugar.jdk.libs").get())
}
