package com.tobiso.tobisoappnative.domain.usecase

import com.tobiso.tobisoappnative.model.InteractiveExerciseResponse
import com.tobiso.tobisoappnative.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetExerciseUseCaseTest {

    private lateinit var repository: ExerciseRepository
    private lateinit var useCase: GetExerciseUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetExerciseUseCase(repository)
    }

    @Test
    fun `returns success when repository returns exercise`() = runTest {
        val exercise = InteractiveExerciseResponse(
            id = 42,
            title = "Časová osa WWI",
            type = "timeline",
            configJson = """{"timeRange":{"start":1914,"end":1918},"events":[]}""",
            instructionsMarkdown = "Seřaď události"
        )
        coEvery { repository.getExercise(42) } returns Result.success(exercise)

        val result = useCase(42)

        assertTrue(result.isSuccess)
        assertEquals(exercise, result.getOrNull())
    }

    @Test
    fun `returns failure when repository returns failure`() = runTest {
        val exception = RuntimeException("Network error")
        coEvery { repository.getExercise(99) } returns Result.failure(exception)

        val result = useCase(99)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `delegates to repository with correct exercise id`() = runTest {
        coEvery { repository.getExercise(7) } returns Result.success(
            InteractiveExerciseResponse(id = 7, title = null, type = null, configJson = null, instructionsMarkdown = null)
        )

        useCase(7)

        coVerify(exactly = 1) { repository.getExercise(7) }
    }

    @Test
    fun `offline failure wraps correct error message`() = runTest {
        val offlineError = IllegalStateException("Cvičení není dostupné v offline režimu")
        coEvery { repository.getExercise(1) } returns Result.failure(offlineError)

        val result = useCase(1)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
