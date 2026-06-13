package app.otakureader.widget

import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.Image
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.otakureader.MainActivity
import app.otakureader.R
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.MangaRepository
import coil3.ImageLoader
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Large home-screen widget showing a vertical carousel of the user's most-recently-read manga
 * with cover art, reading-progress bar, and tap-to-resume. (#949)
 *
 * Pulls the top [MAX_ITEMS] favourites by `lastRead` and loads their covers up-front via Coil
 * so Glance can render bitmaps directly (Glance has no built-in async image loader).
 */
class HomeWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HomeWidgetEntryPoint {
        fun mangaRepository(): MangaRepository
        fun chapterRepository(): ChapterRepository
    }

    private companion object {
        const val MAX_ITEMS = 5
        const val COVER_PX = 256
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HomeWidgetEntryPoint::class.java,
        )
        val mangaRepository = entryPoint.mangaRepository()
        val chapterRepository = entryPoint.chapterRepository()

        val items = try {
            buildItems(context, mangaRepository, chapterRepository)
        } catch (e: Exception) {
            Log.w("HomeWidget", "Failed to load home widget items", e)
            emptyList()
        }

        val title = context.getString(R.string.widget_home_title)
        val emptyText = context.getString(R.string.widget_no_manga_in_progress)

        provideContent {
            GlanceTheme {
                HomeWidgetContent(title = title, items = items, emptyText = emptyText)
            }
        }
    }

    private suspend fun buildItems(
        context: Context,
        mangaRepository: MangaRepository,
        chapterRepository: ChapterRepository,
    ): List<HomeWidgetItem> {
        val candidates = mangaRepository.getLibraryManga().first()
            .filter { it.favorite && it.lastRead != null }
            .sortedByDescending { it.lastRead }
            .take(MAX_ITEMS)
        return coroutineScope {
            candidates.map { manga ->
                async {
                    val nextChapter = chapterRepository.getNextUnreadChapter(manga.id)
                    val cover = loadCoverBitmap(context, manga.thumbnailUrl)
                    val progress = if (manga.totalChapters > 0) {
                        val read = (manga.totalChapters - manga.unreadCount).coerceAtLeast(0)
                        read.toFloat() / manga.totalChapters
                    } else null
                    HomeWidgetItem(
                        mangaId = manga.id,
                        chapterId = nextChapter?.id,
                        title = manga.title,
                        subtitle = when {
                            nextChapter != null -> context.getString(
                                R.string.widget_home_next_chapter,
                                nextChapter.chapterNumber,
                            )
                            manga.unreadCount == 0 -> context.getString(R.string.widget_up_to_date)
                            else -> context.getString(R.string.widget_chapters_remaining, manga.unreadCount)
                        },
                        cover = cover,
                        progress = progress,
                    )
                }
            }.map { it.await() }
        }
    }

    private suspend fun loadCoverBitmap(context: Context, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .size(COVER_PX, COVER_PX)
                    .build()
                val loader: ImageLoader = context.imageLoader
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.image.asDrawable(context.resources)
                    (drawable as? BitmapDrawable)?.bitmap
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
}

private data class HomeWidgetItem(
    val mangaId: Long,
    val chapterId: Long?,
    val title: String,
    val subtitle: String,
    val cover: Bitmap?,
    val progress: Float?,
)

@Composable
private fun HomeWidgetContent(
    title: String,
    items: List<HomeWidgetItem>,
    emptyText: String,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                text = title,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (items.isEmpty()) {
                Text(
                    text = emptyText,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                )
            } else {
                items.forEach { item ->
                    HomeWidgetRow(item)
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeWidgetRow(item: HomeWidgetItem) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(
                actionStartActivity<MainActivity>(
                    parameters = if (item.chapterId != null) {
                        actionParametersOf(
                            WidgetKeys.MANGA_ID_KEY to item.mangaId,
                            WidgetKeys.CHAPTER_ID_KEY to item.chapterId,
                        )
                    } else {
                        actionParametersOf(WidgetKeys.MANGA_ID_KEY to item.mangaId)
                    },
                ),
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(width = 36.dp, height = 48.dp)
                .cornerRadius(4.dp)
                .background(GlanceTheme.colors.surfaceVariant),
        ) {
            if (item.cover != null) {
                Image(
                    provider = ImageProvider(item.cover),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = item.title,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
                maxLines = 1,
            )
            Text(
                text = item.subtitle,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                maxLines = 1,
            )
            if (item.progress != null && item.progress > 0f) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                LinearProgressIndicator(
                    progress = item.progress.coerceIn(0f, 1f),
                    modifier = GlanceModifier.fillMaxWidth().height(3.dp),
                    color = GlanceTheme.colors.primary,
                )
            }
        }
    }
}
