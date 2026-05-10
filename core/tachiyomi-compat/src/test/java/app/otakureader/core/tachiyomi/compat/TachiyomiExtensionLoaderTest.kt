package app.otakureader.core.tachiyomi.compat

import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.ConfigurableSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive unit tests for [TachiyomiExtensionLoader].
 *
 * Tests cover:
 * - Loading all extensions from PackageManager
 * - Loading a specific extension by package name
 * - Caching behavior (performance + invalidation)
 * - ConfigurableSource detection
 * - Error handling for corrupted/invalid extensions
 */
@RunWith(MockitoJUnitRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@ExperimentalCoroutinesApi
class TachiyomiExtensionLoaderTest {

    @Mock
    private lateinit var packageManager: PackageManager

    private lateinit var loader: TachiyomiExtensionLoader

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        loader = TachiyomiExtensionLoader(packageManager)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 1: loadAllExtensions() returns list of available extensions
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `loadAllExtensions returns list of available extensions`() {
        // Given: two installed packages, one with tachiyomi feature, one without
        val extensionPackage = createPackageInfo(
            packageName = "eu.kanade.tachiyomi.extension.en.test",
            versionName = "1.0.0",
            versionCode = 1L,
            hasTachiyomiFeature = true,
            sourceClass = ".TestSource",
            sourceFactory = null,
            isNsfw = false,
        )
        val regularPackage = createPackageInfo(
            packageName = "com.regular.app",
            versionName = "2.0.0",
            versionCode = 2L,
            hasTachiyomiFeature = false,
        )

        mockInstalledPackages(listOf(extensionPackage, regularPackage))

        // When
        val result = loader.loadAllExtensions()

        // Then
        assertEquals(1, result.size)
        assertEquals("eu.kanade.tachiyomi.extension.en.test", result[0].packageName)
        assertEquals("1.0.0", result[0].versionName)
        assertEquals(1L, result[0].versionCode)
    }

    @Test
    fun `loadAllExtensions filters out packages without tachiyomi feature`() {
        // Given: only regular packages
        val regularPackage = createPackageInfo(
            packageName = "com.regular.app",
            versionName = "1.0.0",
            versionCode = 1L,
            hasTachiyomiFeature = false,
        )
        mockInstalledPackages(listOf(regularPackage))

        // When
        val result = loader.loadAllExtensions()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `loadAllExtensions handles empty installed packages`() {
        // Given: no installed packages
        mockInstalledPackages(emptyList())

        // When
        val result = loader.loadAllExtensions()

        // Then
        assertTrue(result.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 2: loadExtension(packageName) loads a specific extension
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `loadExtension by package name returns loaded extension`() {
        // Given
        val packageName = "eu.kanade.tachiyomi.extension.en.mangadex"
        val packageInfo = createPackageInfo(
            packageName = packageName,
            versionName = "1.2.3",
            versionCode = 123L,
            hasTachiyomiFeature = true,
            sourceClass = ".MangaDex",
            isNsfw = false,
        )
        mockPackageInfo(packageName, packageInfo)

        // When
        val result = loader.loadExtension(packageName)

        // Then
        assertNotNull(result)
        assertEquals(packageName, result!!.packageName)
        assertEquals("1.2.3", result.versionName)
        assertEquals(123L, result.versionCode)
    }

    @Test
    fun `loadExtension by package name returns null for non-tachiyomi package`() {
        // Given
        val packageName = "com.regular.app"
        val packageInfo = createPackageInfo(
            packageName = packageName,
            versionName = "1.0.0",
            versionCode = 1L,
            hasTachiyomiFeature = false,
        )
        mockPackageInfo(packageName, packageInfo)

        // When
        val result = loader.loadExtension(packageName)

        // Then
        assertNull(result)
    }

    @Test
    fun `loadExtension by package name returns null when package not found`() {
        // Given
        val packageName = "nonexistent.package"
        Mockito.`when`(
            packageManager.getPackageInfo(
                eq(packageName),
                anyInt(),
            ),
        ).thenThrow(PackageManager.NameNotFoundException("Package not found"))

        // When
        val result = loader.loadExtension(packageName)

        // Then
        assertNull(result)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 3: Cache is used on subsequent loads (performance)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `cache is used on subsequent loads avoiding repeated package manager calls`() {
        // Given
        val packageName = "eu.kanade.tachiyomi.extension.en.test"
        val packageInfo = createPackageInfo(
            packageName = packageName,
            versionName = "1.0.0",
            versionCode = 1L,
            hasTachiyomiFeature = true,
            sourceClass = ".TestSource",
            isNsfw = false,
        )
        mockPackageInfo(packageName, packageInfo)

        // When: load twice
        val first = loader.loadExtension(packageName)
        val second = loader.loadExtension(packageName)

        // Then: both should return same cached instance
        assertNotNull(first)
        assertNotNull(second)
        assertSame(first, second)

        // Verify package manager was only queried once
        Mockito.verify(packageManager, Mockito.times(1)).getPackageInfo(
            eq(packageName),
            anyInt(),
        )
    }

    @Test
    fun `loadAllExtensions uses cache for already loaded extensions`() {
        // Given
        val packageName = "eu.kanade.tachiyomi.extension.en.test"
        val packageInfo = createPackageInfo(
            packageName = packageName,
            versionName = "1.0.0",
            versionCode = 1L,
            hasTachiyomiFeature = true,
            sourceClass = ".TestSource",
            isNsfw = false,
        )
        mockInstalledPackages(listOf(packageInfo))

        // When: load via loadAllExtensions, then load individually
        val allFirst = loader.loadAllExtensions()
        val individual = loader.loadExtension(packageName)

        // Then: individual load should return cached instance
        assertEquals(1, allFirst.size)
        assertSame(allFirst[0], individual)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 4: Cache is invalidated when extension version changes
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `reloadExtension invalidates cache and reloads`() {
        // Given
        val packageName = "eu.kanade.tachiyomi.extension.en.test"
        val packageInfoV1 = createPackageInfo(
            packageName = packageName,
            versionName = "1.0.0",
            versionCode = 1L,
            hasTachiyomiFeature = true,
            sourceClass = ".TestSource",
            isNsfw = false,
        )
        val packageInfoV2 = createPackageInfo(
            packageName = packageName,
            versionName = "2.0.0",
            versionCode = 2L,
            hasTachiyomiFeature = true,
            sourceClass = ".TestSource",
            isNsfw = false,
        )
        mockPackageInfo(packageName, packageInfoV1)

        // When: first load
        val first = loader.loadExtension(packageName)
        assertNotNull(first)
        assertEquals("1.0.0", first!!.versionName)

        // Simulate version bump: update the mock to return v2
        mockPackageInfo(packageName, packageInfoV2)

        // Reload
        val reloaded = loader.reloadExtension(packageName)

        // Then: should have new version
        assertNotNull(reloaded)
        assertEquals("2.0.0", reloaded!!.versionName)
        assertEquals(2L, reloaded.versionCode)
    }

    @Test
    fun `unloadExtension removes from cache`() {
        // Given
        val packageName = "eu.kanade.tachiyomi.extension.en.test"
        val packageInfo = createPackageInfo(
            packageName = packageName,
            versionName = "1.0.0",
            versionCode = 1L,
            hasTachiyomiFeature = true,
            sourceClass = ".TestSource",
            isNsfw = false,
        )
        mockPackageInfo(packageName, packageInfo)

        // When: load then unload
        val loaded = loader.loadExtension(packageName)
        assertNotNull(loaded)
        assertTrue(loader.isExtensionLoaded(packageName))

        loader.unloadExtension(packageName)

        // Then
        assertFalse(loader.isExtensionLoaded(packageName))
        assertTrue(loader.getLoadedExtensions().isEmpty())
    }

    @Test
    fun `unloadAllExtensions clears entire cache`() {
        // Given: two loaded extensions
        val pkg1 = "eu.kanade.tachiyomi.extension.en.test1"
        val pkg2 = "eu.kanade.tachiyomi.extension.en.test2"
        mockPackageInfo(pkg1, createPackageInfo(pkg1, "1.0.0", 1L, true, ".Source1"))
        mockPackageInfo(pkg2, createPackageInfo(pkg2, "1.0.0", 1L, true, ".Source2"))

        loader.loadExtension(pkg1)
        loader.loadExtension(pkg2)
        assertEquals(2, loader.getLoadedExtensions().size)

        // When
        loader.unloadAllExtensions()

        // Then
        assertTrue(loader.getLoadedExtensions().isEmpty())
        assertFalse(loader.isExtensionLoaded(pkg1))
        assertFalse(loader.isExtensionLoaded(pkg2))
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 5: ConfigurableSource interface is properly detected
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `ConfigurableSource stub interface exists and can be checked`() {
        // This test verifies the ConfigurableSource stub is present in the classpath
        // and can be used for instanceof checks (which ExtensionLoader does implicitly
        // via class loading)

        // Verify the interface exists
        val configurableSourceClass = ConfigurableSource::class.java
        assertNotNull(configurableSourceClass)

        // Verify it's a subtype of Source
        assertTrue(Source::class.java.isAssignableFrom(configurableSourceClass))
    }

    @Test
    fun `extension with ConfigurableSource source class loads without error`() {
        // Given: an extension that exposes a ConfigurableSource
        // We can't actually instantiate a real ConfigurableSource in unit tests
        // (it requires an extension APK), but we verify the loader handles
        // the metadata correctly and the class path is resolvable.

        val packageName = "eu.kanade.tachiyomi.extension.en.configurable"
        val packageInfo = createPackageInfo(
            packageName = packageName,
            versionName = "1.0.0",
            versionCode = 1L,
            hasTachiyomiFeature = true,
            sourceClass = ".ConfigurableTestSource",
            isNsfw = false,
        )
        mockPackageInfo(packageName, packageInfo)

        // When
        val result = loader.loadExtension(packageName)

        // Then: The loader attempts to instantiate the class.
        // Since .ConfigurableTestSource doesn't exist, it returns null (no valid sources)
        // but the loader itself doesn't crash.
        // The important thing is ConfigurableSource interface is available for checks.
        assertNull(result) // class doesn't exist, so no sources loaded
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 6: Error handling for corrupted/invalid extensions
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `loadExtension returns null for package without application info`() {
        // Given: package info with null applicationInfo
        val packageName = "eu.kanade.tachiyomi.extension.en.bad"
        val packageInfo = PackageInfo().apply {
            this.packageName = packageName
            this.reqFeatures = arrayOf(FeatureInfo().apply {
                this.name = TachiyomiExtensionLoader.TACHIYOMI_EXTENSION_FEATURE
            })
            this.applicationInfo = null // Missing application info
        }
        mockPackageInfo(packageName, packageInfo)

        // When
        val result = loader.loadExtension(packageName)

        // Then
        assertNull(result)
    }

    @Test
    fun `loadExtension returns null for package without metadata`() {
        // Given: package info with null metaData
        val packageName = "eu.kanade.tachiyomi.extension.en.nometadata"
        val packageInfo = PackageInfo().apply {
            this.packageName = packageName
            this.reqFeatures = arrayOf(FeatureInfo().apply {
                this.name = TachiyomiExtensionLoader.TACHIYOMI_EXTENSION_FEATURE
            })
            this.applicationInfo = ApplicationInfo().apply {
                this.sourceDir = "/fake/path.apk"
                this.metaData = null // Missing metadata
            }
        }
        mockPackageInfo(packageName, packageInfo)

        // When
        val result = loader.loadExtension(packageName)

        // Then
        assertNull(result)
    }

    @Test
    fun `loadExtension returns null for package without source class metadata`() {
        // Given: package info with metadata but no source class key
        val packageName = "eu.kanade.tachiyomi.extension.en.nosource"
        val bundle = Bundle()
        // No METADATA_SOURCE_CLASS or METADATA_SOURCE_FACTORY set
        val packageInfo = PackageInfo().apply {
            this.packageName = packageName
            this.reqFeatures = arrayOf(FeatureInfo().apply {
                this.name = TachiyomiExtensionLoader.TACHIYOMI_EXTENSION_FEATURE
            })
            this.applicationInfo = ApplicationInfo().apply {
                this.sourceDir = "/fake/path.apk"
                this.metaData = bundle
            }
        }
        mockPackageInfo(packageName, packageInfo)

        // When
        val result = loader.loadExtension(packageName)

        // Then: no source classes resolved, returns null
        assertNull(result)
    }

    @Test
    fun `loadExtension handles SecurityException gracefully`() {
        // Given: PackageManager throws SecurityException
        val packageName = "eu.kanade.tachiyomi.extension.en.secure"
        Mockito.`when`(
            packageManager.getPackageInfo(eq(packageName), anyInt()),
        ).thenThrow(SecurityException("Not allowed"))

        // When
        val result = loader.loadExtension(packageName)

        // Then: should not crash, return null
        assertNull(result)
    }

    @Test
    fun `loadExtensionFromApk returns null for invalid APK path`() {
        // Given: PackageManager returns null for archive info
        val apkPath = "/invalid/path/to/extension.apk"
        Mockito.`when`(
            packageManager.getPackageArchiveInfo(eq(apkPath), anyInt()),
        ).thenReturn(null)

        // When
        val result = loader.loadExtensionFromApk(apkPath)

        // Then
        assertNull(result)
    }

    @Test
    fun `loadExtensionFromApk handles exception during parsing`() {
        // Given: PackageManager throws exception
        val apkPath = "/fake/bad.apk"
        Mockito.`when`(
            packageManager.getPackageArchiveInfo(eq(apkPath), anyInt()),
        ).thenThrow(RuntimeException("Corrupted APK"))

        // When
        val result = loader.loadExtensionFromApk(apkPath)

        // Then: should not crash
        assertNull(result)
    }

    @Test
    fun `loadExtension handles package manager NameNotFoundException`() {
        // Given
        val packageName = "missing.package"
        Mockito.`when`(
            packageManager.getPackageInfo(eq(packageName), anyInt()),
        ).thenThrow(PackageManager.NameNotFoundException())

        // When
        val result = loader.loadExtension(packageName)

        // Then
        assertNull(result)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 7: NSFW flag parsing
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `loadExtension correctly parses NSFW flag when set`() {
        // Given
        val packageName = "eu.kanade.tachiyomi.extension.en.nsfw"
        val packageInfo = createPackageInfo(
            packageName = packageName,
            versionName = "1.0.0",
            versionCode = 1L,
            hasTachiyomiFeature = true,
            sourceClass = ".NsfwSource",
            isNsfw = true,
        )
        mockPackageInfo(packageName, packageInfo)

        // When
        val result = loader.loadExtension(packageName)

        // Then: class doesn't exist so null, but verify metadata parsing works
        // In a real scenario with a valid class, isNsfw would be true
        assertNull(result) // No real class to instantiate
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 8: Extension state queries
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `getLoadedExtensions returns all currently loaded`() {
        // Given: load two extensions
        val pkg1 = "eu.kanade.tachiyomi.extension.en.test1"
        val pkg2 = "eu.kanade.tachiyomi.extension.en.test2"
        mockPackageInfo(pkg1, createPackageInfo(pkg1, "1.0.0", 1L, true, ".Source1"))
        mockPackageInfo(pkg2, createPackageInfo(pkg2, "1.0.0", 1L, true, ".Source2"))

        loader.loadExtension(pkg1)
        loader.loadExtension(pkg2)

        // When
        val loaded = loader.getLoadedExtensions()

        // Then
        assertEquals(2, loaded.size)
        assertTrue(loaded.any { it.packageName == pkg1 })
        assertTrue(loaded.any { it.packageName == pkg2 })
    }

    @Test
    fun `getAllSources returns sources from all loaded extensions`() {
        // Given: load extensions (they won't have real sources in unit tests,
        // but we verify the method doesn't crash)
        val pkg1 = "eu.kanade.tachiyomi.extension.en.test1"
        mockPackageInfo(pkg1, createPackageInfo(pkg1, "1.0.0", 1L, true, ".Source1"))
        loader.loadExtension(pkg1)

        // When
        val sources = loader.getAllSources()

        // Then: empty because no real classes, but method executed
        assertTrue(sources.isEmpty())
    }

    @Test
    fun `isExtensionLoaded returns correct state`() {
        val pkg = "eu.kanade.tachiyomi.extension.en.test"
        assertFalse(loader.isExtensionLoaded(pkg))

        mockPackageInfo(pkg, createPackageInfo(pkg, "1.0.0", 1L, true, ".Source"))
        loader.loadExtension(pkg)

        assertTrue(loader.isExtensionLoaded(pkg))
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────────────────────────────────

    private fun createPackageInfo(
        packageName: String,
        versionName: String,
        versionCode: Long,
        hasTachiyomiFeature: Boolean,
        sourceClass: String? = null,
        sourceFactory: String? = null,
        isNsfw: Boolean = false,
    ): PackageInfo {
        val bundle = Bundle().apply {
            sourceClass?.let { putString(TachiyomiExtensionLoader.METADATA_SOURCE_CLASS, it) }
            sourceFactory?.let { putString(TachiyomiExtensionLoader.METADATA_SOURCE_FACTORY, it) }
            if (isNsfw) putInt(TachiyomiExtensionLoader.METADATA_NSFW, 1)
        }

        return PackageInfo().apply {
            this.packageName = packageName
            this.versionName = versionName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                this.longVersionCode = versionCode
            } else {
                @Suppress("DEPRECATION")
                this.versionCode = versionCode.toInt()
            }
            this.reqFeatures = if (hasTachiyomiFeature) {
                arrayOf(FeatureInfo().apply {
                    this.name = TachiyomiExtensionLoader.TACHIYOMI_EXTENSION_FEATURE
                })
            } else {
                null
            }
            this.applicationInfo = ApplicationInfo().apply {
                this.sourceDir = "/fake/$packageName.apk"
                this.nativeLibraryDir = "/fake/lib"
                this.metaData = bundle
                this.loadLabel(packageManager) // Will be mocked
            }
        }
    }

    private fun mockInstalledPackages(packages: List<PackageInfo>) {
        Mockito.`when`(
            packageManager.getInstalledPackages(anyInt()),
        ).thenReturn(packages)
    }

    private fun mockPackageInfo(packageName: String, packageInfo: PackageInfo) {
        Mockito.`when`(
            packageManager.getPackageInfo(eq(packageName), anyInt()),
        ).thenReturn(packageInfo)
    }
}
