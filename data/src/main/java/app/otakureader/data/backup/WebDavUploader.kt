package app.otakureader.data.backup

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import app.otakureader.core.preferences.CloudBackupUploader
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebDAV implementation of [CloudBackupUploader].
 *
 * Call [configure] before using [upload] or [testConnection]. Uses HTTP Basic Auth over OkHttp.
 * The instance is a singleton; [configure] updates internal state so the worker can reuse it
 * without rebuilding the [OkHttpClient].
 */
@Singleton
class WebDavUploader @Inject constructor() : CloudBackupUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Volatile private var baseUrl: String = ""
    @Volatile private var authHeader: String = ""

    override fun configure(url: String, username: String, password: String) {
        baseUrl = url.trimEnd('/')
        val credentials = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
        authHeader = "Basic $credentials"
    }

    override suspend fun upload(file: File): Result<Unit> = runCatching {
        require(baseUrl.isNotBlank()) { "WebDAV URL not configured" }
        val url = "$baseUrl/${file.name}"
        val body = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .header("Authorization", authHeader)
            .put(body)
            .build()
        val response = client.newCall(request).execute()
        response.use {
            when (it.code) {
                HttpURLConnection.HTTP_CREATED,
                HttpURLConnection.HTTP_NO_CONTENT,
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_FORBIDDEN ->
                    error("Authentication failed (${it.code})")
                else -> error("Upload failed: HTTP ${it.code}")
            }
        }
    }

    override suspend fun testConnection(): Result<Unit> = runCatching {
        require(baseUrl.isNotBlank()) { "WebDAV URL not configured" }
        val request = Request.Builder()
            .url(baseUrl)
            .header("Authorization", authHeader)
            .method("PROPFIND", null)
            .header("Depth", "0")
            .build()
        val response = client.newCall(request).execute()
        response.use {
            when (it.code) {
                207, HttpURLConnection.HTTP_OK -> Unit  // 207 Multi-Status is the standard WebDAV success
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_FORBIDDEN ->
                    error("Authentication failed (${it.code})")
                else -> error("Connection test failed: HTTP ${it.code}")
            }
        }
    }
}
