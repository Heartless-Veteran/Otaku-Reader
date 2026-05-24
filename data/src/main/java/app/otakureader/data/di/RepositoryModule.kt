package app.otakureader.data.di

import app.otakureader.domain.repository.CategoryRepository
import app.otakureader.domain.repository.ChapterRepository
import app.otakureader.domain.repository.DownloadRepository
import app.otakureader.domain.repository.ExtensionManagementRepository
import app.otakureader.domain.repository.FeedRepository
import app.otakureader.domain.repository.MangaRepository
import app.otakureader.domain.repository.OpdsRepository
import app.otakureader.domain.repository.PageBookmarkRepository
import app.otakureader.domain.repository.ReadingListRepository
import app.otakureader.domain.repository.SourceRepository
import app.otakureader.domain.repository.StatisticsRepository
import app.otakureader.data.opds.OpdsRepositoryImpl
import app.otakureader.data.repository.CategoryRepositoryImpl
import app.otakureader.data.repository.ChapterRepositoryImpl
import app.otakureader.data.repository.DownloadRepositoryImpl
import app.otakureader.data.repository.FeedRepositoryImpl
import app.otakureader.data.repository.MangaRepositoryImpl
import app.otakureader.data.repository.ReaderSettingsRepository
import app.otakureader.data.loader.PageLoader as PageLoaderImpl
import app.otakureader.data.history.WorkManagerHistoryScheduler
import app.otakureader.data.backup.BackupScheduler as BackupSchedulerImpl
import app.otakureader.data.backup.repository.BackupRepository as BackupRepositoryImpl
import app.otakureader.data.backup.tachiyomi.TachiyomiBackupImporter as TachiyomiBackupImporterImpl
import app.otakureader.data.tracking.TrackManager as TrackManagerImpl
import app.otakureader.data.updater.AppUpdateChecker as AppUpdateCheckerImpl
import app.otakureader.data.worker.CoverRefreshSchedulerImpl
import app.otakureader.data.worker.LibraryUpdateScheduler as LibraryUpdateSchedulerImpl
import app.otakureader.data.worker.ReadingReminderScheduler as ReadingReminderSchedulerImpl
import app.otakureader.domain.backup.BackupRepository
import app.otakureader.domain.backup.BackupScheduler
import app.otakureader.domain.backup.TachiyomiBackupImporter
import app.otakureader.domain.history.ReadingHistoryScheduler
import app.otakureader.domain.loader.PageLoader
import app.otakureader.domain.repository.ReaderSettingsRepository as ReaderSettingsRepositoryInterface
import app.otakureader.data.repository.PageBookmarkRepositoryImpl
import app.otakureader.data.repository.ReadingListRepositoryImpl
import app.otakureader.data.repository.SourceRepositoryImpl
import app.otakureader.data.repository.StatisticsRepositoryImpl
import app.otakureader.data.worker.TrackerSyncSchedulerImpl
import app.otakureader.domain.scheduler.CoverRefreshScheduler
import app.otakureader.domain.scheduler.LibraryUpdateScheduler
import app.otakureader.domain.scheduler.ReminderScheduler
import app.otakureader.domain.scheduler.TrackerSyncScheduler
import app.otakureader.domain.tracking.TrackManager
import app.otakureader.domain.updater.AppUpdateChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Hilt module that binds data-layer repository implementations to their domain interfaces. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMangaRepository(impl: MangaRepositoryImpl): MangaRepository

    @Binds
    abstract fun bindChapterRepository(impl: ChapterRepositoryImpl): ChapterRepository

    @Binds
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun bindStatisticsRepository(impl: StatisticsRepositoryImpl): StatisticsRepository

    @Binds
    abstract fun bindOpdsRepository(impl: OpdsRepositoryImpl): OpdsRepository

    @Binds
    abstract fun bindFeedRepository(impl: FeedRepositoryImpl): FeedRepository

    @Binds
    abstract fun bindReaderSettingsRepository(impl: ReaderSettingsRepository): ReaderSettingsRepositoryInterface

    @Binds
    abstract fun bindPageLoader(impl: PageLoaderImpl): PageLoader

    @Binds
    abstract fun bindReadingHistoryScheduler(impl: WorkManagerHistoryScheduler): ReadingHistoryScheduler

    @Binds
    abstract fun bindPageBookmarkRepository(impl: PageBookmarkRepositoryImpl): PageBookmarkRepository

    @Binds
    abstract fun bindSourceRepository(impl: SourceRepositoryImpl): SourceRepository

    @Binds
    abstract fun bindExtensionManagementRepository(impl: SourceRepositoryImpl): ExtensionManagementRepository

    @Binds
    abstract fun bindReadingListRepository(impl: ReadingListRepositoryImpl): ReadingListRepository

    @Binds
    abstract fun bindReminderScheduler(impl: ReadingReminderSchedulerImpl): ReminderScheduler

    @Binds
    abstract fun bindLibraryUpdateScheduler(impl: LibraryUpdateSchedulerImpl): LibraryUpdateScheduler

    @Binds
    abstract fun bindBackupScheduler(impl: BackupSchedulerImpl): BackupScheduler

    @Binds
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    abstract fun bindTachiyomiBackupImporter(impl: TachiyomiBackupImporterImpl): TachiyomiBackupImporter

    @Binds
    abstract fun bindTrackManager(impl: TrackManagerImpl): TrackManager

    @Binds
    abstract fun bindAppUpdateChecker(impl: AppUpdateCheckerImpl): AppUpdateChecker

    @Binds
    abstract fun bindCoverRefreshScheduler(impl: CoverRefreshSchedulerImpl): CoverRefreshScheduler

    @Binds
    abstract fun bindTrackerSyncScheduler(impl: TrackerSyncSchedulerImpl): TrackerSyncScheduler
}
