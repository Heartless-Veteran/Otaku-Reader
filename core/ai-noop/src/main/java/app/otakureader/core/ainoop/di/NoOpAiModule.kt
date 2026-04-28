package app.otakureader.core.ainoop.di

import app.otakureader.core.ainoop.NoOpAiRepository
import app.otakureader.core.ainoop.NoOpCategorizationRepository
import app.otakureader.core.ainoop.NoOpChapterSummaryRepository
import app.otakureader.core.ainoop.NoOpOcrTranslationRepository
import app.otakureader.core.ainoop.NoOpRecommendationRepository
import app.otakureader.core.ainoop.NoOpSfxTranslationRepository
import app.otakureader.core.ainoop.NoOpSmartSearchCacheRepository
import app.otakureader.core.ainoop.NoOpSourceIntelligenceRepository
import app.otakureader.domain.repository.AiRepository
import app.otakureader.domain.repository.CategorizationRepository
import app.otakureader.domain.repository.ChapterSummaryRepository
import app.otakureader.domain.repository.OcrTranslationRepository
import app.otakureader.domain.repository.RecommendationRepository
import app.otakureader.domain.repository.SfxTranslationRepository
import app.otakureader.domain.repository.SmartSearchCacheRepository
import app.otakureader.domain.repository.SourceIntelligenceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides no-op AI repository implementations.
 *
 * AI features (Gemini-backed implementations) are not currently shipped with the
 * app — they live in a separate companion repo (see #708). This module binds
 * stubs for every AI-related repository so the rest of the app can depend on
 * those interfaces without pulling in the Gemini SDK.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NoOpAiModule {

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: NoOpAiRepository): AiRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(impl: NoOpRecommendationRepository): RecommendationRepository

    @Binds
    @Singleton
    abstract fun bindSfxTranslationRepository(impl: NoOpSfxTranslationRepository): SfxTranslationRepository

    @Binds
    @Singleton
    abstract fun bindOcrTranslationRepository(impl: NoOpOcrTranslationRepository): OcrTranslationRepository

    @Binds
    @Singleton
    abstract fun bindChapterSummaryRepository(impl: NoOpChapterSummaryRepository): ChapterSummaryRepository

    @Binds
    @Singleton
    abstract fun bindSourceIntelligenceRepository(impl: NoOpSourceIntelligenceRepository): SourceIntelligenceRepository

    @Binds
    @Singleton
    abstract fun bindSmartSearchCacheRepository(impl: NoOpSmartSearchCacheRepository): SmartSearchCacheRepository

    @Binds
    @Singleton
    abstract fun bindCategorizationRepository(impl: NoOpCategorizationRepository): CategorizationRepository
}
