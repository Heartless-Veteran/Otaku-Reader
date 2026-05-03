package com.otakureader.data.model

data class Manga(
    val id: Int,
    val title: String,
    val author: String,
    val chapters: Int,
    val read: Int,
    val totalPages: Int,
    val status: String,
    val rating: Float,
    val year: Int,
    val hue: Float,
    val genres: List<String>,
    val scanlator: String,
    val source: String,
    val newChapters: Int,
    val downloaded: Int,
    val contentRating: String = "Safe",
) {
    val readProgress: Float get() = if (chapters > 0) read.toFloat() / chapters else 0f
    val unread: Int get() = chapters - read
}

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val count: Int,
    val color: String = "#6B4EFF",
)

data class Chapter(
    val num: Int,
    val title: String,
    val date: String,
    val pages: Int,
    val downloaded: Boolean,
    val read: Boolean,
    val reading: Boolean,
    val readingPage: Int = 0,
    val totalPages: Int = 22,
)

data class HistoryEntry(
    val manga: Manga,
    val chapter: String,
    val page: String,
    val when_: String,
)

data class UpdateEntry(
    val manga: Manga,
    val chapter: String,
    val when_: String,
    val isNew: Boolean,
    val downloaded: Boolean,
)

data class Source(
    val name: String,
    val lang: String,
    val count: Int,
    val status: String,
)

data class DownloadItem(
    val manga: Manga,
    val chapter: String,
    val progress: Float,
    val state: DownloadState,
    val size: String,
)

enum class DownloadState { DOWNLOADING, QUEUED, PAUSED, DONE }
enum class ReaderMode { PAGED, SPREAD, WEBTOON }
