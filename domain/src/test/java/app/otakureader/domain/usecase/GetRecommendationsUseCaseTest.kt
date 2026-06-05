package app.otakureader.domain.usecase

import app.otakureader.domain.model.Recommendation
import app.otakureader.domain.repository.RecommendationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetRecommendationsUseCaseTest {

    private lateinit var recommendationRepository: RecommendationRepository
    private lateinit var useCase: GetRecommendationsUseCase

    @Before
    fun setUp() {
        recommendationRepository = mockk()
        useCase = GetRecommendationsUseCase(recommendationRepository)
    }

    @Test
    fun invoke_returnsFlowFromRepository() = runTest {
        val recommendations = listOf(
            Recommendation(mangaId = 1L, title = "Test Manga", thumbnailUrl = null, sourceId = 1L, score = 0.9f)
        )
        every { recommendationRepository.getRecommendations() } returns flowOf(recommendations)

        val result = useCase().first()

        assertEquals(recommendations, result)
    }

    @Test
    fun invoke_delegatesToRepository() = runTest {
        every { recommendationRepository.getRecommendations() } returns flowOf(emptyList())

        useCase()

        verify(exactly = 1) { recommendationRepository.getRecommendations() }
    }
}
