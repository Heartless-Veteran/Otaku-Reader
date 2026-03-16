package app.otakureader.domain.usecase.ai

import app.otakureader.domain.ai.AiFeature
import app.otakureader.domain.ai.AiFeatureGate
import app.otakureader.domain.repository.AiRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GenerateAiContentUseCaseTest {

    private lateinit var aiRepository: AiRepository
    private lateinit var aiFeatureGate: AiFeatureGate
    private lateinit var useCase: GenerateAiContentUseCase

    @Before
    fun setUp() {
        aiRepository = mockk()
        aiFeatureGate = mockk()
        useCase = GenerateAiContentUseCase(aiRepository, aiFeatureGate)
    }

    // ---- blank prompt ----

    @Test
    fun `returns failure for blank prompt without calling gate or repository`() = runTest {
        val result = useCase("   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { aiFeatureGate.isAiAvailable() }
        coVerify(exactly = 0) { aiRepository.generateContent(any()) }
    }

    // ---- gate checks ----

    @Test
    fun `returns failure when gate reports AI unavailable (no feature)`() = runTest {
        coEvery { aiFeatureGate.isAiAvailable() } returns false

        val result = useCase("describe this manga")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { aiRepository.generateContent(any()) }
    }

    @Test
    fun `returns failure when gate reports feature unavailable`() = runTest {
        coEvery { aiFeatureGate.isFeatureAvailable(AiFeature.READING_INSIGHTS) } returns false

        val result = useCase("show stats", AiFeature.READING_INSIGHTS)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { aiRepository.generateContent(any()) }
    }

    @Test
    fun `calls isFeatureAvailable when feature is provided`() = runTest {
        coEvery { aiFeatureGate.isFeatureAvailable(AiFeature.SMART_SEARCH) } returns true
        coEvery { aiRepository.generateContent(any()) } returns Result.success("result")

        useCase("search for action manga", AiFeature.SMART_SEARCH)

        coVerify(exactly = 1) { aiFeatureGate.isFeatureAvailable(AiFeature.SMART_SEARCH) }
        coVerify(exactly = 0) { aiFeatureGate.isAiAvailable() }
    }

    @Test
    fun `calls isAiAvailable when no feature is specified`() = runTest {
        coEvery { aiFeatureGate.isAiAvailable() } returns true
        coEvery { aiRepository.generateContent(any()) } returns Result.success("result")

        useCase("generic prompt")

        coVerify(exactly = 1) { aiFeatureGate.isAiAvailable() }
        coVerify(exactly = 0) { aiFeatureGate.isFeatureAvailable(any()) }
    }

    // ---- success path ----

    @Test
    fun `returns repository result when gate allows and repository succeeds`() = runTest {
        coEvery { aiFeatureGate.isAiAvailable() } returns true
        coEvery { aiRepository.generateContent("my prompt") } returns Result.success("AI response")

        val result = useCase("my prompt")

        assertTrue(result.isSuccess)
        assertEquals("AI response", result.getOrNull())
    }

    // ---- repository failure propagated ----

    @Test
    fun `propagates repository failure when gate allows`() = runTest {
        coEvery { aiFeatureGate.isAiAvailable() } returns true
        coEvery { aiRepository.generateContent(any()) } returns
            Result.failure(RuntimeException("network error"))

        val result = useCase("some prompt")

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is RuntimeException)
        assertEquals("network error", result.exceptionOrNull()!!.message)
    }
}
