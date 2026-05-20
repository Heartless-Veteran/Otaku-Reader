package app.otakureader.core.tachiyomi.compat

import dalvik.system.PathClassLoader
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.Enumeration

/**
 * A parent-last class loader that will try in order:
 * - the system class loader
 * - the child class loader (extension APK)
 * - the parent class loader (host app)
 *
 * This matches Komikku's ChildFirstPathClassLoader exactly. Using this instead of
 * DexClassLoader ensures that libraries bundled inside an extension APK take
 * priority over the host app's versions, preventing class-version conflicts when
 * an extension ships a different version of a shared library (e.g. OkHttp).
 *
 * Note: This is a local copy in tachiyomi-compat to avoid a circular dependency
 * with core:extension. See docs/EXTENSION_LOADER_CONSOLIDATION.md for context.
 */
internal class ChildFirstPathClassLoader(
    dexPath: String,
    librarySearchPath: String?,
    parent: ClassLoader,
) : PathClassLoader(dexPath, librarySearchPath, parent) {

    // Store the parent explicitly: in unit-test environments the Android stub's
    // BaseDexClassLoader constructor may not call super(parent), leaving the
    // ClassLoader.parent field unset and breaking the standard delegation chain.
    // Using this field directly ensures we always reach the host app classloader.
    private val hostClassLoader: ClassLoader = parent
    private val systemClassLoader: ClassLoader? = getSystemClassLoader()

    override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        var c = findLoadedClass(name)

        if (c == null && systemClassLoader != null) {
            try {
                c = systemClassLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {}
        }

        if (c == null) {
            c = try {
                findClass(name) ?: hostClassLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {
                hostClassLoader.loadClass(name)
            } catch (_: Throwable) {
                // In unit-test environments the PathClassLoader stub may throw
                // RuntimeException instead of ClassNotFoundException for missing
                // classes. Fall back to the host app classloader so test-classpath
                // stubs are still reachable.
                hostClassLoader.loadClass(name)
            }
        }

        if (resolve) {
            resolveClass(c)
        }

        return c
    }

    override fun getResource(name: String?): URL? {
        return systemClassLoader?.getResource(name)
            ?: findResource(name)
            ?: super.getResource(name)
    }

    override fun getResources(name: String?): Enumeration<URL> {
        val systemUrls = systemClassLoader?.getResources(name)
        val localUrls = findResources(name)
        val parentUrls = parent?.getResources(name)
        val urls = buildList {
            while (systemUrls?.hasMoreElements() == true) {
                add(systemUrls.nextElement())
            }
            while (localUrls?.hasMoreElements() == true) {
                add(localUrls.nextElement())
            }
            while (parentUrls?.hasMoreElements() == true) {
                add(parentUrls.nextElement())
            }
        }

        return object : Enumeration<URL> {
            val iterator = urls.iterator()

            override fun hasMoreElements() = iterator.hasNext()
            override fun nextElement() = iterator.next()
        }
    }

    override fun getResourceAsStream(name: String?): InputStream? {
        return try {
            getResource(name)?.openStream()
        } catch (_: IOException) {
            null
        }
    }
}
