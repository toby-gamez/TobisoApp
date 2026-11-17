package com.tobiso.tobisoappnative.repository

import android.content.Context
import com.tobiso.tobisoappnative.fake.FakeSharedPreferences
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.PreferredGradeRequest
import com.tobiso.tobisoappnative.model.StudentProfile
import com.tobiso.tobisoappnative.utils.loadGradeId
import com.tobiso.tobisoappnative.utils.saveGradeId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class PreferredGradeSyncTest {

    private lateinit var api: ApiService
    private lateinit var context: Context
    private lateinit var repo: AuthRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        context = mockk()
        every { context.getSharedPreferences("grade_prefs", Context.MODE_PRIVATE) } returns FakeSharedPreferences()
        coEvery { api.setPreferredGrade(any()) } returns Response.success(Unit)
        repo = AuthRepositoryImpl(api, context)
    }

    private fun profile(grade: Int?) = StudentProfile("Ája", "a@b.cz", 3, "student", grade)

    @Test
    fun `account grade wins over a different local grade`() = runBlocking {
        saveGradeId(context, 4)
        coEvery { api.getMe() } returns profile(7)

        repo.refreshProfile()

        assertEquals(7, loadGradeId(context))
        coVerify(exactly = 0) { api.setPreferredGrade(any()) }
    }

    @Test
    fun `account grade is applied when there is no local grade`() = runBlocking {
        coEvery { api.getMe() } returns profile(6)

        repo.refreshProfile()

        assertEquals(6, loadGradeId(context))
    }

    @Test
    fun `local grade is uploaded when the account has none`() = runBlocking {
        saveGradeId(context, 5)
        coEvery { api.getMe() } returns profile(null)

        repo.refreshProfile()

        assertEquals(5, loadGradeId(context))
        coVerify(exactly = 1) { api.setPreferredGrade(PreferredGradeRequest(5)) }
    }

    @Test
    fun `nothing happens when both sides agree`() = runBlocking {
        saveGradeId(context, 8)
        coEvery { api.getMe() } returns profile(8)

        repo.refreshProfile()

        assertEquals(8, loadGradeId(context))
        coVerify(exactly = 0) { api.setPreferredGrade(any()) }
    }

    @Test
    fun `nothing happens when neither side has a grade`() = runBlocking {
        coEvery { api.getMe() } returns profile(null)

        repo.refreshProfile()

        assertNull(loadGradeId(context))
        coVerify(exactly = 0) { api.setPreferredGrade(any()) }
    }

    @Test
    fun `a failing upload does not fail the profile refresh`() = runBlocking {
        saveGradeId(context, 5)
        coEvery { api.getMe() } returns profile(null)
        coEvery { api.setPreferredGrade(any()) } returns
            Response.error(404, "".toResponseBody("application/json".toMediaType()))

        val result = repo.refreshProfile()

        assertTrue(result.isSuccess)
        assertEquals(5, loadGradeId(context))
    }

    @Test
    fun `setPreferredGrade fails on a non success response`() = runBlocking {
        coEvery { api.setPreferredGrade(any()) } returns
            Response.error(400, """{"message":"Neznámý ročník."}""".toResponseBody("application/json".toMediaType()))

        assertTrue(repo.setPreferredGrade(99).isFailure)
    }

    @Test
    fun `repository without a context skips the sync`() = runBlocking {
        coEvery { api.getMe() } returns profile(7)

        AuthRepositoryImpl(api).refreshProfile()

        coVerify(exactly = 0) { api.setPreferredGrade(any()) }
    }
}
