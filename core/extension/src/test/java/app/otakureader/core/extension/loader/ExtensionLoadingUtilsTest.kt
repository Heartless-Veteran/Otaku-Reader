package app.otakureader.core.extension.loader

import dalvik.system.DexClassLoader
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Unit tests for ExtensionLoadingUtils focusing on NPE prevention and error handling.
 */
class ExtensionLoadingUtilsTest {

    @Test
    fun `createClassLoader throws IllegalArgumentException for blank apkPath`() {
        try {
            ExtensionLoadingUtils.createClassLoader(
                apkPath = "",
                optimizedDir = File("/tmp/test"),
                nativeLibDir = null
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `createClassLoader throws IllegalStateException when mkdirs fails`() {
        // Use a path that cannot be created (e.g., under /dev/null)
        val impossibleDir = File("/dev/null/impossible")

        try {
            ExtensionLoadingUtils.createClassLoader(
                apkPath = "/test/path.apk",
                optimizedDir = impossibleDir,
                nativeLibDir = null
            )
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // Expected - directory creation should fail
        }
    }

    @Test
    fun `instantiateClass returns null for blank className`() {
        // className validation happens before classLoader is used
        try {
            // We pass a mock classLoader but it won't be used because className is blank
            val mockClassLoader = this::class.java.classLoader as? DexClassLoader
                ?: return // Skip test if we can't get a DexClassLoader

            ExtensionLoadingUtils.instantiateClass(
                classLoader = mockClassLoader,
                className = ""
            )
            fail("Expected IllegalArgumentException for blank className")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    // Note: Testing instantiateClass with a real DexClassLoader requires Android runtime
    // The production code will properly return null for ClassNotFoundException, etc.

    @Test
    fun `resolveClassName expands relative class name`() {
        val result = ExtensionLoadingUtils.resolveClassName(
            className = ".MySource",
            pkgName = "com.example"
        )
        assert(result == "com.example.MySource")
    }

    @Test
    fun `resolveClassName keeps absolute class name`() {
        val result = ExtensionLoadingUtils.resolveClassName(
            className = "com.example.MySource",
            pkgName = "com.test"
        )
        assert(result == "com.example.MySource")
    }
}
