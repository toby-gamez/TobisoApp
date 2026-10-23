package com.tobiso.tobisoappnative.viewmodel.auth

import com.tobiso.tobisoappnative.model.SubjectBadge
import com.tobiso.tobisoappnative.model.UserStats
import com.tobiso.tobisoappnative.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AccountViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)

    @Test
    fun `loadStats exposes the server stats and keeps the old ones on failure`() {
        val repo = mockk<AuthRepository>()
        val stats = UserStats(streakDays = 4, totalRead = 12)
        coEvery { repo.stats() } returns Result.success(stats)
        val vm = AccountViewModel(repo)
        assertNull(vm.stats.value)
        vm.loadStats()
        assertEquals(stats, vm.stats.value)

        coEvery { repo.stats() } returns Result.failure(RuntimeException("offline"))
        vm.loadStats()
        assertEquals(stats, vm.stats.value)
    }

    @Test
    fun `stats json from the web decodes, including numeric badge levels`() {
        val json = Json { ignoreUnknownKeys = true }
        val s = json.decodeFromString<UserStats>(
            """{"streakDays":3,"totalRead":9,"perSubject":[{"subjectName":"Matematika","count":5}],
               "badges":[{"subjectName":"Matematika","level":"Gold"},{"subjectName":"Fyzika","level":2}],"extra":1}"""
        )
        assertEquals(3, s.streakDays)
        assertEquals(5, s.perSubject.single().count)
        assertEquals(listOf(SubjectBadge("Matematika", "Gold"), SubjectBadge("Fyzika", "2")), s.badges)
    }
}
