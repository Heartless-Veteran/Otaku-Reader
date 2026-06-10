package app.otakureader.tools.extensionsmoketest

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.zip.ZipInputStream
import kotlin.time.measureTimedValue

/**
 * Network smoke tests for the Keiyoushi extension repository distribution pipeline.
 *
 * These tests require internet access and are intentionally excluded from regular CI.
 * Run via: ./gradlew :tools:extension-smoke-test:test
 * or the manual-dispatch extension-smoke-test.yml workflow.
 *
 * What is verified:
 *  1. The default index.min.json is fetchable and parses correctly
 *  2. All entries in the index carry required fields
 *  3. The pinned target extension is present in the index
 *  4. The pinned extension's APK is downloadable (HTTP 200)
 *  5. The downloaded APK is a valid ZIP containing AndroidManifest.xml and classes.dex
 *
 * Latency for each step is reported in test output.
 */
class ExtensionIndexSmokeTest {

    // Default Keiyoushi repo — mirrors DEFAULT_REPO_URL in ExtensionRepoRepository
    private val repoBaseUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo"
    private val indexMinUrl = "$repoBaseUrl/index.min.json"

    // Pinned stable target chosen for longevity (multi-language source, long track record)
    private val pinnedPkg = "eu.kanade.tachiyomi.extension.all.mangadex"

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var client: OkHttpClient
    private lateinit var entries: List<IndexEntry>

    @Before
    fun setup() {
        client = OkHttpClient.Builder()
            .callTimeout(java.time.Duration.ofSeconds(30))
            .build()
    }

    // ── Check 1: index reachable ─────────────────────────────────────────────

    @Test
    fun indexIsFetchableAndParseable() = runTest {
        val (result, duration) = measureTimedValue {
            val request = Request.Builder().url(indexMinUrl).build()
            client.newCall(request).execute().use { response ->
                assertTrue("index.min.json HTTP ${response.code}", response.isSuccessful)
                val body = checkNotNull(response.body?.string()) { "Empty response body" }
                json.decodeFromString<List<IndexEntry>>(body)
            }
        }
        println("[PASS] index fetched in ${duration.inWholeMilliseconds}ms — ${result.size} entries")
        entries = result
        assertTrue("index should be non-empty", result.isNotEmpty())
    }

    // ── Check 2: index well-formed ───────────────────────────────────────────

    @Test
    fun allEntriesHaveRequiredFields() = runTest {
        val index = fetchIndex()
        val (malformed, duration) = measureTimedValue {
            index.filter { entry ->
                entry.name.isBlank() ||
                    entry.pkg.isBlank() ||
                    entry.apk.isBlank() ||
                    entry.lang.isBlank() ||
                    entry.version.isBlank() ||
                    entry.sources.isEmpty()
            }
        }
        println(
            "[PASS] field check in ${duration.inWholeMilliseconds}ms — " +
                "${index.size} entries, ${malformed.size} malformed",
        )
        assertTrue(
            "Malformed entries (missing required fields): ${malformed.map { it.pkg }}",
            malformed.isEmpty(),
        )
    }

    // ── Check 3: pinned extension present ────────────────────────────────────

    @Test
    fun pinnedExtensionIsPresentInIndex() = runTest {
        val index = fetchIndex()
        val (entry, duration) = measureTimedValue {
            index.find { it.pkg == pinnedPkg }
        }
        println("[PASS] pinned lookup in ${duration.inWholeMilliseconds}ms — found: ${entry != null}")
        assertNotNull("Pinned extension '$pinnedPkg' not found in index", entry)
        assertFalse("Pinned extension APK filename is blank", entry!!.apk.isBlank())
        assertTrue(
            "Pinned extension should have at least one source",
            entry.sources.isNotEmpty(),
        )
        println("  name=${entry.name} version=${entry.version} apk=${entry.apk}")
    }

    // ── Check 4: APK URL returns 200 ─────────────────────────────────────────

    @Test
    fun pinnedExtensionApkIsDownloadable() = runTest {
        val index = fetchIndex()
        val entry = index.find { it.pkg == pinnedPkg }
            ?: error("Pinned extension '$pinnedPkg' not found in index")

        val apkUrl = "$repoBaseUrl/apk/${entry.apk}"
        val (responseCode, duration) = measureTimedValue {
            val request = Request.Builder().url(apkUrl).head().build()
            client.newCall(request).execute().use { it.code }
        }
        println("[PASS] HEAD $apkUrl → HTTP $responseCode in ${duration.inWholeMilliseconds}ms")
        assertTrue("APK HEAD returned HTTP $responseCode (expected 200)", responseCode == 200)
    }

    // ── Check 5: APK is a valid ZIP with expected entries ────────────────────

    @Test
    fun pinnedExtensionApkIsValidZip() = runTest {
        val index = fetchIndex()
        val entry = index.find { it.pkg == pinnedPkg }
            ?: error("Pinned extension '$pinnedPkg' not found in index")

        val apkUrl = "$repoBaseUrl/apk/${entry.apk}"
        val (zipEntryNames, duration) = measureTimedValue {
            val request = Request.Builder().url(apkUrl).build()
            client.newCall(request).execute().use { response ->
                assertTrue("APK download HTTP ${response.code}", response.isSuccessful)
                val bytes = checkNotNull(response.body?.bytes()) { "Empty APK body" }
                ZipInputStream(bytes.inputStream()).use { zip ->
                    generateSequence { zip.nextEntry }
                        .map { it.name }
                        .toList()
                }
            }
        }

        println(
            "[PASS] APK zip check in ${duration.inWholeMilliseconds}ms — " +
                "${zipEntryNames.size} entries",
        )
        assertTrue(
            "APK missing AndroidManifest.xml",
            zipEntryNames.any { it == "AndroidManifest.xml" },
        )
        assertTrue(
            "APK missing classes.dex",
            zipEntryNames.any { it.endsWith(".dex") },
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun fetchIndex(): List<IndexEntry> {
        val request = Request.Builder().url(indexMinUrl).build()
        return client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "index.min.json HTTP ${response.code}" }
            val body = checkNotNull(response.body?.string()) { "Empty index body" }
            json.decodeFromString(body)
        }
    }

    // ── DTOs (mirrors MinifiedExtensionDto in ExtensionRemoteDataSource) ─────

    @Serializable
    private data class IndexEntry(
        @SerialName("name") val name: String = "",
        @SerialName("pkg") val pkg: String = "",
        @SerialName("apk") val apk: String = "",
        @SerialName("lang") val lang: String = "",
        @SerialName("code") val code: Int = 0,
        @SerialName("version") val version: String = "",
        @SerialName("nsfw") val nsfw: Int = 0,
        @SerialName("sources") val sources: List<IndexSource> = emptyList(),
    )

    @Serializable
    private data class IndexSource(
        @SerialName("name") val name: String = "",
        @SerialName("lang") val lang: String = "",
        @SerialName("id") val id: String = "",
        @SerialName("baseUrl") val baseUrl: String = "",
    )
}
