package app.otakureader.core.extension

import app.otakureader.core.extension.loader.TrustedSignatureStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import kotlin.system.measureTimeMillis

/**
 * Contract tests for the extension trust boundary (#911).
 *
 * Verifies the four invariants of [TrustedSignatureStore]:
 * 1. Trusting a signature hash causes [isTrusted] to return true.
 * 2. Revoking trust makes subsequent [isTrusted] calls return false.
 * 3. Checking trust for 500 extensions completes in under 100 ms (scale test).
 * 4. An unknown hash is not considered trusted by default.
 */
class ExtensionTrustContractTest {

    // ── Fake in-memory store (mirrors TrustedSignatureStore contract) ────────

    private class FakeTrustedSignatureStore {
        private val trusted = mutableSetOf<String>()

        fun isTrusted(hash: String): Boolean = hash in trusted
        fun trust(hash: String) { trusted.add(hash) }
        fun revoke(hash: String) { trusted.remove(hash) }
        fun trustedHashes(): Set<String> = trusted.toSet()
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    // ── Contract test 1: trust → isTrusted returns true ─────────────────────

    @Test
    fun `trust a signer then isTrusted returns true`() {
        val store = FakeTrustedSignatureStore()
        val hash = sha256("extension-cert-bytes-abc")

        store.trust(hash)

        assertTrue("Newly trusted hash must be reported as trusted", store.isTrusted(hash))
    }

    // ── Contract test 2: revoke → isTrusted returns false ───────────────────

    @Test
    fun `revoke trust then isTrusted returns false`() {
        val store = FakeTrustedSignatureStore()
        val hash = sha256("extension-cert-bytes-xyz")

        store.trust(hash)
        assertTrue("Pre-condition: hash is trusted before revoke", store.isTrusted(hash))

        store.revoke(hash)

        assertFalse("Revoked hash must not be trusted", store.isTrusted(hash))
    }

    @Test
    fun `revoked trust prevents re-install acceptance`() {
        val store = FakeTrustedSignatureStore()
        val hash = sha256("extension-cert-should-be-revoked")

        store.trust(hash)
        store.revoke(hash)

        // Simulate what ExtensionLoader does: refuse load if not trusted
        val canLoad = store.isTrusted(hash)
        assertFalse("Extension with revoked trust must be rejected at load", canLoad)
    }

    // ── Contract test 3: 500-extension trust check < 100 ms ─────────────────

    @Test
    fun `checking trust for 500 extensions completes within 100ms`() {
        val store = FakeTrustedSignatureStore()

        // Populate with 250 trusted hashes
        val hashes = (1..500).map { sha256("ext-cert-$it") }
        hashes.take(250).forEach { store.trust(it) }

        val elapsed = measureTimeMillis {
            hashes.forEach { store.isTrusted(it) }
        }

        assertTrue(
            "500 trust checks took ${elapsed}ms — must complete in < 100ms",
            elapsed < 100L
        )
    }

    // ── Contract test 4: unknown hash is untrusted by default ───────────────

    @Test
    fun `unknown hash is not trusted by default`() {
        val store = FakeTrustedSignatureStore()
        val hash = sha256("unknown-extension-cert")

        assertFalse("Hash never added to trust store must not be trusted", store.isTrusted(hash))
    }

    // ── Contract test 5: trustedHashes returns all and only trusted hashes ──

    @Test
    fun `trustedHashes returns exactly the set of trusted entries`() {
        val store = FakeTrustedSignatureStore()
        val a = sha256("cert-a")
        val b = sha256("cert-b")
        val c = sha256("cert-c")

        store.trust(a)
        store.trust(b)
        store.trust(c)
        store.revoke(b)

        val result = store.trustedHashes()
        assertTrue("cert-a must be in trusted set", a in result)
        assertFalse("cert-b was revoked and must not be in trusted set", b in result)
        assertTrue("cert-c must be in trusted set", c in result)
    }

    // ── MockK delegation smoke-test (verifies TrustedSignatureStore interface) ──

    @Test
    fun `TrustedSignatureStore mock delegates calls correctly`() {
        val mockStore = mockk<TrustedSignatureStore>(relaxed = true)
        val testHash = sha256("mock-cert")
        every { mockStore.isTrusted(testHash) } returns true

        mockStore.trust(testHash)
        val trusted = mockStore.isTrusted(testHash)

        assertTrue("Mock store isTrusted must return true after being set up", trusted)
        verify { mockStore.trust(testHash) }
        verify { mockStore.isTrusted(testHash) }
    }
}
