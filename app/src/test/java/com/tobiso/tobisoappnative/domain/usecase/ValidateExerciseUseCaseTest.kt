package com.tobiso.tobisoappnative.domain.usecase

import com.tobiso.tobisoappnative.model.ExerciseValidationResult
import com.tobiso.tobisoappnative.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateExerciseUseCaseTest {

    private lateinit var repository: ExerciseRepository
    private lateinit var useCase: ValidateExerciseUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = ValidateExerciseUseCase(repository)
    }

    @Test
    fun `returns success with correct validation when answer is right`() = runTest {
        val validationResult = ExerciseValidationResult(
            isCorrect = true,
            score = 100,
            feedback = "Správně!"
        )
        coEvery { repository.validateExercise(1, """{"order":["A","B","C"]}""") } returns
            Result.success(validationResult)

        val result = useCase(1, """{"order":["A","B","C"]}""")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isCorrect)
        assertEquals(100, result.getOrNull()!!.score)
    }

    @Test
    fun `returns success with isCorrect false when answer is wrong`() = runTest {
        val validationResult = ExerciseValidationResult(
            isCorrect = false,
            score = 0,
            feedback = "Špatně, zkus to znovu."
        )
        coEvery { repository.validateExercise(1, """{"order":["C","A","B"]}""") } returns
            Result.success(validationResult)

        val result = useCase(1, """{"order":["C","A","B"]}""")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.isCorrect)
        assertEquals(0, result.getOrNull()!!.score)
    }

    @Test
    fun `returns failure when offline`() = runTest {
        coEvery { repository.validateExercise(any(), any()) } returns Result.failure(IllegalStateException("Validace vyžaduje internetové připojení"))

        val result = useCase(5, "{}")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Validace vyžaduje"))
    }

    @Test
    fun `returns failure on network error`() = runTest {
        coEvery { repository.validateExercise(2, any()) } returns Result.failure(RuntimeException("HTTP 500"))

        val result = useCase(2, """{"pairs":[]}""")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("HTTP 500"))
    }

    @Test
    fun `delegates to repository with correct parameters`() = runTest {
        val solution = """{"placements":{"cat1":"item1"}}"""
        coEvery { repository.validateExercise(3, solution) } returns
            Result.success(ExerciseValidationResult(isCorrect = true, score = 50, feedback = "OK"))

        useCase(3, solution)

        coVerify(exactly = 1) { repository.validateExercise(3, solution) }
    }

    @Test
    fun `returns partial score with detailed results`() = runTest {
        val partial = ExerciseValidationResult(
            isCorrect = false,
            score = 60,
            feedback = "Částečně správně",
            detailedResults = mapOf("item1" to true, "item2" to false)
        )
        coEvery { repository.validateExercise(10, any()) } returns Result.success(partial)

        val result = useCase(10, """{"placements":{}}""")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.isCorrect)
        assertEquals(60, result.getOrNull()!!.score)
        assertEquals(2, result.getOrNull()!!.detailedResults?.size)
    }

    @Test
    fun `rejects zero or negative exerciseId`() = runTest {
        val result = useCase(0, "{}")
        assertTrue(result.isFailure)

        val resultNeg = useCase(-5, "{}")
        assertTrue(resultNeg.isFailure)
    }

    @Test
    fun `rejects blank solution`() = runTest {
        val resultBlank = useCase(1, "")
        assertTrue(resultBlank.isFailure)

        val resultSpaces = useCase(1, "   ")
        assertTrue(resultSpaces.isFailure)
    }

    @Test
    fun `rejects oversized solution`() = runTest {
        val oversized = "x".repeat(100_001)
        val result = useCase(1, oversized)
        assertTrue(result.isFailure)
    }
}
