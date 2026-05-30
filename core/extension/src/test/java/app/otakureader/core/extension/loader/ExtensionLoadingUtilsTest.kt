package app.otakureader.core.extension.loader

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit tests for ExtensionLoadingUtils focusing on NPE prevention and error handling.
 */
class ExtensionLoadingUtilsTest {

    @Test
    fun `createClassLoader throws IllegalArgumentException for blank apkPath`() {
        try {
            ExtensionLoadingUtils.createClassLoader(
                apkPath = "",
                nativeLibDir = null
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `instantiateClass throws IllegalArgumentException for blank className`() {
        val mockClassLoader = mockk<ClassLoader>(relaxed = true)

        try {
            ExtensionLoadingUtils.instantiateClass(
                classLoader = mockClassLoader,
                className = ""
            )
            fail("Expected IllegalArgumentException for blank className")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Diagnostic tests — lock in the reason-string format so a future refactor
    // doesn't quietly regress the loader's user-visible error message. Each test
    // exercises a distinct catch branch in `instantiateClass` using a locally
    // declared fixture loaded through the host JVM classloader (no Android, no
    // DexClassLoader needed for these branches).
    // ────────────────────────────────────────────────────────────────────────────

    /** Constructor that always throws — exercises the InvocationTargetException branch. */
    class CtorThrowsFixture {
        init { error("ctor boom") }
    }

    /** Class with no no-arg constructor — exercises the NoSuchMethodException branch. */
    @Suppress("UNUSED_PARAMETER")
    class NoNoArgCtorFixture(value: Int)

    @Test
    fun `instantiateClass returns NotFound for a missing class`() {
        val result = ExtensionLoadingUtils.instantiateClass(
            classLoader = javaClass.classLoader!!,
            className = "com.example.definitely.not.a.real.Class"
        )
        assertEquals(InstantiationResult.NotFound, result)
    }

    @Test
    fun `instantiateClass returns Failure with NoSuchMethodException reason for parameterised ctor`() {
        val result = ExtensionLoadingUtils.instantiateClass(
            classLoader = javaClass.classLoader!!,
            className = NoNoArgCtorFixture::class.java.name
        )
        assertTrue("Expected Failure but got $result", result is InstantiationResult.Failure)
        val failure = result as InstantiationResult.Failure
        assertTrue(
            "Reason should mention NoSuchMethodException, was: ${failure.reason}",
            failure.reason.contains("NoSuchMethodException")
        )
    }

    @Test
    fun `instantiateClass returns Failure with InvocationTargetException reason when ctor throws`() {
        val result = ExtensionLoadingUtils.instantiateClass(
            classLoader = javaClass.classLoader!!,
            className = CtorThrowsFixture::class.java.name
        )
        assertTrue("Expected Failure but got $result", result is InstantiationResult.Failure)
        val failure = result as InstantiationResult.Failure
        assertTrue(
            "Reason should mention InvocationTargetException, was: ${failure.reason}",
            failure.reason.contains("InvocationTargetException")
        )
        // The underlying ctor-thrown exception must be preserved so the loader can include
        // its message in the user-visible toast — that's the whole point of the diagnostic.
        assertTrue(
            "Reason should include the underlying ctor message, was: ${failure.reason}",
            failure.reason.contains("ctor boom")
        )
    }

    @Test
    fun `resolveClassName expands relative class name`() {
        val result = ExtensionLoadingUtils.resolveClassName(
            className = ".MySource",
            pkgName = "com.example"
        )
        assertEquals("com.example.MySource", result)
    }

    @Test
    fun `resolveClassName keeps absolute class name`() {
        val result = ExtensionLoadingUtils.resolveClassName(
            className = "com.example.MySource",
            pkgName = "com.test"
        )
        assertEquals("com.example.MySource", result)
    }
}
