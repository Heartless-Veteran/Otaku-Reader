package app.otakureader.buildlogic

import org.gradle.api.artifacts.VersionCatalog

/**
 * Central Android SDK configuration sourced from the version catalog.
 *
 * To upgrade API levels for the entire project, change [compile-sdk], [target-sdk], and
 * [min-sdk] in `gradle/libs.versions.toml`. Every module that uses a convention plugin
 * picks up the new values automatically — no plugin source files need editing.
 */
internal object AndroidConfig {
    fun compileSdk(libs: VersionCatalog): Int =
        libs.findVersion("compile-sdk").get().requiredVersion.toInt()

    fun targetSdk(libs: VersionCatalog): Int =
        libs.findVersion("target-sdk").get().requiredVersion.toInt()

    fun minSdk(libs: VersionCatalog): Int =
        libs.findVersion("min-sdk").get().requiredVersion.toInt()
}
