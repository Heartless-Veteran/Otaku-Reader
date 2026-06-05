package app.otakureader.domain.model

/** Thrown by [DownloadManager.enqueue] when a download is prevented by the Data Saver setting. */
class DownloadBlockedException(reason: String) : Exception(reason)
