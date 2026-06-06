package app.otakureader.data.download

import java.io.File
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility for creating and reading CBZ (Comic Book ZIP) archives.
 *
 * A CBZ file is a standard ZIP archive containing page image files and an optional
 * `ComicInfo.xml` metadata file. The page files inside the archive are named using
 * the same `{index}.jpg` convention as the loose-file storage layout.
 *
 * The CBZ file is stored inside the chapter directory alongside the loose page files
 * under the fixed name [CBZ_FILE_NAME]:
 * ```
 * OtakuReader/{source}/{manga}/{chapter}/
 *   0.jpg          ← loose page (deleted after CBZ creation in auto mode)
 *   1.jpg
 *   chapter.cbz    ← CBZ archive
 * ```
 */
object CbzCreator {

    /** Fixed filename used for the CBZ archive within each chapter directory. */
    const val CBZ_FILE_NAME = "chapter.cbz"

    /**
     * Magic header bytes prepended to AES-GCM encrypted CBZ files.
     * Chosen to not overlap with the ZIP magic bytes (PK = 0x504B).
     */
    private val MAGIC = "OTKREADER_ENC".toByteArray(Charsets.US_ASCII) // 13 bytes
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val GCM_TAG_LEN = 128
    private const val PBKDF2_ITERATIONS = 10_000
    private const val KEY_LEN_BITS = 256

    /** Image file extensions that are included when packing or unpacking CBZ archives. */
    private val PAGE_EXTENSIONS get() = DownloadProvider.PAGE_EXTENSIONS

    // -------------------------------------------------------------------------
    // Data
    // -------------------------------------------------------------------------

    /**
     * Optional metadata to embed as `ComicInfo.xml` inside the CBZ archive.
     *
     * @param title       Chapter title.
     * @param series      Series / manga title.
     * @param number      Chapter number as a string (e.g. "12" or "12.5").
     * @param writer      Author / writer of the series.
     * @param language    BCP-47 language tag (e.g. "en", "ja").
     */
    data class ComicInfoMetadata(
        val title: String,
        val series: String,
        val number: String? = null,
        val writer: String? = null,
        val language: String? = null
    )

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Packs all page image files inside [chapterDir] into a CBZ archive named
     * [CBZ_FILE_NAME] within that same directory.
     *
     * Optionally embeds a `ComicInfo.xml` entry when [metadata] is provided.
     *
     * The archive is written to a temporary file first and atomically renamed to
     * [CBZ_FILE_NAME] on success, so a failed/interrupted call never leaves a
     * truncated archive behind. If [CBZ_FILE_NAME] already exists it is returned
     * immediately without modification.
     *
     * @param chapterDir directory that contains the downloaded page files.
     * @param metadata   optional comic metadata to embed; `null` skips the XML entry.
     * @return [Result.success] wrapping the created (or existing) CBZ [File] on success,
     *         or [Result.failure] with the cause on any error.
     */
    fun createCbz(chapterDir: File, metadata: ComicInfoMetadata? = null): Result<File> = runCatching {
        require(chapterDir.isDirectory) { "chapterDir must be an existing directory: $chapterDir" }

        // Return the existing archive unchanged – prevents accidentally overwriting
        // CBZ-only chapters that have already had their loose files deleted.
        val cbzFile = File(chapterDir, CBZ_FILE_NAME)
        if (cbzFile.exists()) return@runCatching cbzFile

        val pages = chapterDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in PAGE_EXTENSIONS }
            ?.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
            ?: emptyList()

        require(pages.isNotEmpty()) { "No page images found in $chapterDir; nothing to pack into CBZ" }

        // Write to a temp file so that an error never leaves a corrupt/empty archive.
        val tempFile = File(chapterDir, "$CBZ_FILE_NAME.tmp")
        try {
            ZipOutputStream(tempFile.outputStream().buffered()).use { zos ->
                metadata?.let {
                    zos.putNextEntry(ZipEntry("ComicInfo.xml"))
                    zos.write(buildComicInfoXml(it).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
                pages.forEach { page ->
                    zos.putNextEntry(ZipEntry(page.name))
                    page.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            // Atomic rename: replaces any stale temp file atomically.
            tempFile.renameTo(cbzFile)
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
        cbzFile
    }

    /**
     * Extracts all page image files from [cbzFile] into [destDir].
     *
     * Only entries whose extensions are in [PAGE_EXTENSIONS] are extracted;
     * metadata files such as `ComicInfo.xml` are skipped.
     *
     * @param cbzFile path to the `.cbz` archive.
     * @param destDir directory to which page files are written.
     * @return [Result.success] wrapping an ordered list of extracted [File]s on success,
     *         or [Result.failure] on any error.
     */
    fun extractCbzPages(cbzFile: File, destDir: File): Result<List<File>> = runCatching {
        destDir.mkdirs()
        val destCanonical = destDir.canonicalPath + File.separator

        // Pre-scan: reject archives containing path traversal entries before writing any file.
        ZipFile(cbzFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val candidate = File(destDir, entry.name.replace('\\', '/'))
                if (!candidate.canonicalPath.startsWith(destCanonical)) {
                    throw SecurityException("Path traversal attempt in CBZ entry: ${entry.name}")
                }
            }
        }

        val extracted = mutableListOf<File>()
        ZipFile(cbzFile).use { zip ->
            zip.entries().asSequence()
                .filter { entry ->
                    !entry.isDirectory &&
                        entry.name.substringAfterLast('.').lowercase() in PAGE_EXTENSIONS
                }
                .sortedBy { entry ->
                    entry.name.substringAfterLast('/').substringBeforeLast('.').toIntOrNull()
                        ?: Int.MAX_VALUE
                }
                .forEach { entry ->
                    val outFile = File(destDir, entry.name.replace('\\', '/'))
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input -> outFile.outputStream().use { input.copyTo(it) } }
                    extracted += outFile
                }
        }
        extracted.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
    }

    // -------------------------------------------------------------------------
    // Encryption helpers (AES-256-GCM with PBKDF2 key derivation)
    // -------------------------------------------------------------------------

    /**
     * Returns true if [cbzFile] has been encrypted with [encryptInPlace].
     * Reads only the first [MAGIC].size bytes — fast and does not buffer the full file.
     */
    fun isEncrypted(cbzFile: File): Boolean {
        if (!cbzFile.exists() || cbzFile.length() < MAGIC.size) return false
        val header = ByteArray(MAGIC.size)
        cbzFile.inputStream().use { it.read(header) }
        return header.contentEquals(MAGIC)
    }

    /**
     * Encrypts [cbzFile] in-place using AES-256-GCM.
     *
     * The resulting file layout is:
     * `MAGIC (13) | SALT (16) | IV (12) | AES-GCM ciphertext+tag`
     *
     * The key is derived with PBKDF2-HMAC-SHA256 from [passphrase] + a random per-file [SALT].
     */
    fun encryptInPlace(cbzFile: File, passphrase: String) {
        val plaintext = cbzFile.readBytes()
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        cbzFile.outputStream().buffered().use { out ->
            out.write(MAGIC)
            out.write(salt)
            out.write(iv)
            out.write(ciphertext)
        }
    }

    /**
     * Decrypts an encrypted CBZ file to a temporary file inside [tempDir].
     *
     * @return [Result.success] wrapping the decrypted temp [File], or
     *         [Result.failure] if the file is not encrypted or decryption fails.
     */
    fun decryptToTempFile(cbzFile: File, passphrase: String, tempDir: File): Result<File> = runCatching {
        tempDir.mkdirs()
        val bytes = cbzFile.readBytes()
        require(bytes.size > MAGIC.size + SALT_LEN + IV_LEN) { "File too small to be an encrypted CBZ" }
        val salt = bytes.copyOfRange(MAGIC.size, MAGIC.size + SALT_LEN)
        val iv = bytes.copyOfRange(MAGIC.size + SALT_LEN, MAGIC.size + SALT_LEN + IV_LEN)
        val ciphertext = bytes.copyOfRange(MAGIC.size + SALT_LEN + IV_LEN, bytes.size)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LEN, iv))
        val plaintext = cipher.doFinal(ciphertext)
        val tempFile = File(tempDir, "${cbzFile.name}.dec")
        tempFile.writeBytes(plaintext)
        tempFile
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LEN_BITS)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    fun buildComicInfoXml(metadata: ComicInfoMetadata): String = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<ComicInfo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">""")
        appendLine("""  <Title>${escapeXml(metadata.title)}</Title>""")
        appendLine("""  <Series>${escapeXml(metadata.series)}</Series>""")
        metadata.number?.let { appendLine("""  <Number>${escapeXml(it)}</Number>""") }
        metadata.writer?.let { appendLine("""  <Writer>${escapeXml(it)}</Writer>""") }
        metadata.language?.let { appendLine("""  <LanguageISO>${escapeXml(it)}</LanguageISO>""") }
        append("""</ComicInfo>""")
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
