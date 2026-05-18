package app.otakureader.sourceapi

@Deprecated(
    message = "Use MangaSource with suspend functions instead. HttpSource retains the legacy " +
        "get* naming convention incompatible with the MangaSource fetch* pipeline.",
    replaceWith = ReplaceWith("MangaSource", "app.otakureader.sourceapi.MangaSource")
)
interface HttpSource : Source {
    val baseUrl: String
    suspend fun getPopularManga(page: Int): MangasPage
    suspend fun getLatestUpdates(page: Int): MangasPage
    suspend fun searchManga(page: Int, query: String, filters: FilterList = FilterList()): MangasPage
    suspend fun getMangaDetails(manga: SManga): SManga
    suspend fun getChapterList(manga: SManga): List<SChapter>
    suspend fun getPageList(chapter: SChapter): List<Page>
}
