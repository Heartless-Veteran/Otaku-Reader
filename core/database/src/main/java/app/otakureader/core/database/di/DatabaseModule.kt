package app.otakureader.core.database.di

import android.content.Context
import androidx.room.Room
import app.otakureader.core.database.OtakuReaderDatabase
import app.otakureader.core.database.dao.CategoryDao
import app.otakureader.core.database.dao.ChapterDao
import app.otakureader.core.database.dao.MangaCategoryDao
import app.otakureader.core.database.dao.MangaDao
import app.otakureader.core.database.dao.ReadingHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OtakuReaderDatabase =
        Room.databaseBuilder(
            context,
            OtakuReaderDatabase::class.java,
            OtakuReaderDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun provideMangaDao(db: OtakuReaderDatabase): MangaDao = db.mangaDao()

    @Provides
    fun provideChapterDao(db: OtakuReaderDatabase): ChapterDao = db.chapterDao()

    @Provides
    fun provideCategoryDao(db: OtakuReaderDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideMangaCategoryDao(db: OtakuReaderDatabase): MangaCategoryDao = db.mangaCategoryDao()

    @Provides
    fun provideReadingHistoryDao(db: OtakuReaderDatabase): ReadingHistoryDao = db.readingHistoryDao()
}
