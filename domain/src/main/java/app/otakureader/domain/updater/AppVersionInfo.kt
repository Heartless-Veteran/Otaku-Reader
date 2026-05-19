package app.otakureader.domain.updater

import kotlinx.serialization.Serializable

@Serializable
data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val releaseDate: Long,
)
