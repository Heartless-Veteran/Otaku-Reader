package app.otakureader.data.di

import app.otakureader.domain.repository.OpdsRepository
import app.otakureader.domain.usecase.opds.BrowseOpdsCatalogUseCase
import app.otakureader.domain.usecase.opds.DeleteOpdsServerUseCase
import app.otakureader.domain.usecase.opds.GetOpdsServersUseCase
import app.otakureader.domain.usecase.opds.SaveOpdsServerUseCase
import app.otakureader.domain.usecase.opds.SearchOpdsCatalogUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * OPDS use cases require explicit @Provides because they do not have @Inject constructors.
 * All other use cases are resolved via @Inject and do not need entries here.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideGetOpdsServersUseCase(opdsRepository: OpdsRepository): GetOpdsServersUseCase =
        GetOpdsServersUseCase(opdsRepository)

    @Provides
    fun provideSaveOpdsServerUseCase(opdsRepository: OpdsRepository): SaveOpdsServerUseCase =
        SaveOpdsServerUseCase(opdsRepository)

    @Provides
    fun provideDeleteOpdsServerUseCase(opdsRepository: OpdsRepository): DeleteOpdsServerUseCase =
        DeleteOpdsServerUseCase(opdsRepository)

    @Provides
    fun provideBrowseOpdsCatalogUseCase(opdsRepository: OpdsRepository): BrowseOpdsCatalogUseCase =
        BrowseOpdsCatalogUseCase(opdsRepository)

    @Provides
    fun provideSearchOpdsCatalogUseCase(opdsRepository: OpdsRepository): SearchOpdsCatalogUseCase =
        SearchOpdsCatalogUseCase(opdsRepository)
}
