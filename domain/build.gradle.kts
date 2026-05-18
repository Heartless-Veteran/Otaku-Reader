plugins {
    alias(libs.plugins.otakureader.kotlin.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(projects.sourceApi)
    compileOnly("javax.inject:javax.inject:1")
    
    // Compose runtime for @Immutable annotations (compile-only, not bundled)
    compileOnly(platform(libs.compose.bom))
    compileOnly(libs.compose.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

kover {
    reports {
        verify {
            rule {
                minBound(60)
            }
        }
    }
}
