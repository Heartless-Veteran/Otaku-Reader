package com.otakureader.data

import com.otakureader.data.model.*

val MANGA = listOf(
    Manga(1, "Crimson Tide Bay", "Hara Riku", 124, 87, 2480, "Ongoing", 4.8f, 2019, 350f, listOf("Action", "Drama"), "Aurora", "MangaDex", 3, 12, "Mature"),
    Manga(2, "Paper Lanterns of Kyoto", "Yui Sato", 56, 56, 1120, "Completed", 4.6f, 2021, 25f, listOf("Romance", "Slice of Life"), "Petals", "MangaDex", 0, 56),
    Manga(3, "Iron Garden", "D. Kong", 203, 92, 4060, "Ongoing", 4.9f, 2017, 200f, listOf("Sci-Fi", "Action"), "Pyre", "Mangaplus", 5, 0),
    Manga(4, "The Seventh Floor", "Mira Volkov", 38, 12, 760, "Ongoing", 4.4f, 2022, 270f, listOf("Mystery", "Horror"), "Nightowl", "MangaDex", 2, 5),
    Manga(5, "Salt & Echo", "Bao Linh", 72, 45, 1440, "Hiatus", 4.5f, 2018, 180f, listOf("Adventure"), "Tide", "Komga", 0, 30),
    Manga(6, "Half Light Diner", "F. Cortez", 22, 22, 440, "Completed", 4.7f, 2023, 45f, listOf("Romance", "Drama"), "Petals", "MangaDex", 0, 22),
    Manga(7, "Voltigeur", "A. Bouchard", 89, 34, 1780, "Ongoing", 4.3f, 2020, 130f, listOf("Action", "Comedy"), "Aurora", "Mangaplus", 8, 0),
    Manga(8, "Wolves of the North Wind", "Eira Solberg", 156, 156, 3120, "Completed", 4.9f, 2015, 220f, listOf("Fantasy", "Action"), "Pyre", "MangaDex", 0, 156),
    Manga(9, "Soft Static", "Naoko Mura", 18, 4, 360, "Ongoing", 4.2f, 2024, 310f, listOf("Slice of Life"), "Petals", "MangaDex", 1, 0),
    Manga(10, "Brass Choir", "M. Demir", 67, 50, 1340, "Ongoing", 4.6f, 2019, 35f, listOf("Drama", "Music"), "Aurora", "MangaDex", 0, 12),
    Manga(11, "Halcyon Cipher", "R. Aoyama", 110, 0, 2200, "Ongoing", 4.8f, 2018, 280f, listOf("Sci-Fi", "Mystery"), "Nightowl", "Mangaplus", 12, 0),
    Manga(12, "Lacebridge", "C. Whitlock", 41, 41, 820, "Completed", 4.5f, 2021, 5f, listOf("Romance"), "Petals", "MangaDex", 0, 0),
)

val CATEGORIES = listOf(
    Category("all", "All", "library", 247),
    Category("reading", "Reading", "book", 12, "#6B4EFF"),
    Category("on_hold", "On Hold", "pause_circle", 8, "#FBBF24"),
    Category("completed", "Completed", "check_circle", 64, "#4ADE80"),
    Category("plan", "Plan to Read", "bookmark", 38, "#FF6B9D"),
    Category("favorites", "Favorites", "star", 19, "#4DDFFF"),
)

val SOURCES = listOf(
    Source("MangaDex", "EN", 84000, "online"),
    Source("Mangaplus", "EN", 1240, "online"),
    Source("Komga", "EN", 320, "local"),
    Source("Bato.to", "EN", 45000, "online"),
    Source("WebtoonSrc", "EN", 12000, "online"),
)

val HISTORY = listOf(
    HistoryEntry(MANGA[0], "Ch. 87", "14/22", "12 min ago"),
    HistoryEntry(MANGA[3], "Ch. 12", "8/18", "2h ago"),
    HistoryEntry(MANGA[6], "Ch. 34", "22/22", "Yesterday"),
    HistoryEntry(MANGA[1], "Ch. 42", "20/20", "Yesterday"),
    HistoryEntry(MANGA[9], "Ch. 50", "12/19", "3 days ago"),
)

val UPDATES = listOf(
    UpdateEntry(MANGA[0], "Ch. 125", "Just now", true, true),
    UpdateEntry(MANGA[2], "Ch. 204", "2h ago", true, false),
    UpdateEntry(MANGA[2], "Ch. 203", "2h ago", true, false),
    UpdateEntry(MANGA[6], "Ch. 90", "5h ago", true, false),
    UpdateEntry(MANGA[3], "Ch. 39", "Yesterday", false, true),
    UpdateEntry(MANGA[10], "Ch. 111", "Yesterday", false, false),
    UpdateEntry(MANGA[8], "Ch. 19", "2 days ago", false, false),
)

val DOWNLOADS = listOf(
    DownloadItem(MANGA[0], "Ch. 125", 0.65f, DownloadState.DOWNLOADING, "2.1 / 3.4 MB"),
    DownloadItem(MANGA[2], "Ch. 204", 0.20f, DownloadState.DOWNLOADING, "0.7 / 3.5 MB"),
    DownloadItem(MANGA[2], "Ch. 203", 0f, DownloadState.QUEUED, "~3.5 MB"),
    DownloadItem(MANGA[6], "Ch. 90", 0f, DownloadState.QUEUED, "~2.8 MB"),
    DownloadItem(MANGA[3], "Ch. 39", 0f, DownloadState.PAUSED, "~2.1 MB"),
    DownloadItem(MANGA[10], "Ch. 111", 1f, DownloadState.DONE, "3.2 MB"),
    DownloadItem(MANGA[10], "Ch. 110", 1f, DownloadState.DONE, "3.1 MB"),
)

fun chaptersForManga(manga: Manga): List<Chapter> = List(14) { i ->
    val num = manga.chapters - i
    val read = num <= manga.read
    val reading = num == manga.read + 1
    Chapter(
        num = num,
        title = "Chapter $num",
        date = if (i < 3) "${i + 1}d ago" else "${i + 1}w ago",
        pages = 18 + (i % 6),
        downloaded = i < 3 || (i in 7..9),
        read = read,
        reading = reading,
        readingPage = if (reading) 14 else 0,
        totalPages = 22,
    )
}
