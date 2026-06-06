package app.otakureader.data.backup

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM backup encryption/decryption with PBKDF2-derived keys.
 *
 * File format: magic[6] | salt[16] | iv[12] | ciphertext+tag
 * The GCM authentication tag (last 16 bytes of ciphertext block) provides integrity
 * verification — a wrong password throws [javax.crypto.AEADBadTagException] on decrypt.
 */
internal object AesBackupCipher {

    private val MAGIC = "OTBAK1".toByteArray(Charsets.US_ASCII)
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val KEY_BITS = 256
    private const val PBKDF2_ITERATIONS = 100_000
    private const val PBKDF2_ALGO = "PBKDF2WithHmacSHA256"
    private const val AES_ALGO = "AES/GCM/NoPadding"

    fun isEncrypted(header: ByteArray): Boolean =
        header.size >= MAGIC.size &&
            header.copyOf(MAGIC.size).contentEquals(MAGIC)

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        val salt = SecureRandom().let { rng -> ByteArray(SALT_LENGTH).also { rng.nextBytes(it) } }
        val iv   = SecureRandom().let { rng -> ByteArray(IV_LENGTH).also   { rng.nextBytes(it) } }
        val key  = deriveKey(password, salt)
        val cipher = Cipher.getInstance(AES_ALGO).apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        val ciphertext = cipher.doFinal(plaintext)
        return MAGIC + salt + iv + ciphertext
    }

    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        require(isEncrypted(data)) { "Not a recognised Otaku Reader encrypted backup (missing magic bytes)" }
        var off = MAGIC.size
        val salt       = data.copyOfRange(off, off + SALT_LENGTH).also { off += SALT_LENGTH }
        val iv         = data.copyOfRange(off, off + IV_LENGTH).also   { off += IV_LENGTH   }
        val ciphertext = data.copyOfRange(off, data.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(AES_ALGO).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(ciphertext)
    }

    /** SHA-256 hex digest of the password — stored so the wrong-password error is user-friendly. */
    fun hashPassword(password: CharArray): String {
        val bytes = String(password).toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        val raw  = SecretKeyFactory.getInstance(PBKDF2_ALGO).generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(raw, "AES")
    }

    private operator fun ByteArray.plus(other: ByteArray): ByteArray {
        val out = ByteArray(size + other.size)
        copyInto(out)
        other.copyInto(out, size)
        return out
    }
}
