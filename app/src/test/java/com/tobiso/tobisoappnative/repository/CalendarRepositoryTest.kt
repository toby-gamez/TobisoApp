package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.AuthUser
import com.tobiso.tobisoappnative.model.Event
import com.tobiso.tobisoappnative.model.OfflineDataManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date

class CalendarRepositoryTest {
    private lateinit var api: ApiService
    private lateinit var offline: OfflineDataManager
    private lateinit var repo: CalendarRepository

    private val start = Date(1_000_000_000_000)
    private val end = Date(1_000_000_000_000 + 30L * 24 * 3600 * 1000)
    private fun ev(id: Int, scope: String? = null) =
        Event(id = id, title = "E$id", startDate = Date(start.time + 3600_000), scope = scope)

    @Before
    fun setUp() {
        api = mockk()
        offline = mockk(relaxed = true)
        repo = CalendarRepository(api, offline)
    }

    @After
    fun tearDown() = AuthSession.signOut()

    @Test
    fun `anonymous users get the public calendar`() = runTest {
        coEvery { api.getEventsInRange(any(), any()) } returns listOf(ev(1, "Global"))
        assertEquals(listOf(1), repo.loadRange(start, end).map { it.id })
        coVerify(exactly = 0) { api.getMyEvents(any(), any()) }
    }

    @Test
    fun `signed in users get events-mine and it is cached per account`() = runTest {
        AuthSession.signIn("jwt", AuthUser("Jana", "j@x.cz"))
        val events = listOf(ev(1, "Global"), ev(2, "Class"))
        coEvery { api.getMyEvents(any(), any()) } returns events
        assertEquals(listOf(1, 2), repo.loadRange(start, end).map { it.id })
        coVerify { offline.saveMyEventsInRange("j@x.cz", start.time, end.time, events) }
    }

    @Test
    fun `a rejected token falls back to the public calendar`() = runTest {
        AuthSession.signIn("jwt", AuthUser("Jana", "j@x.cz"))
        coEvery { api.getMyEvents(any(), any()) } throws RuntimeException("401")
        coEvery { api.getEventsInRange(any(), any()) } returns listOf(ev(1, "Global"))
        assertEquals(listOf(1), repo.loadRange(start, end).map { it.id })
    }

    @Test
    fun `offline returns cached events in range`() = runTest {
        coEvery { api.getEventsInRange(any(), any()) } throws java.io.IOException("offline")
        val inside = ev(5)
        val outside = Event(id = 6, title = "old", startDate = Date(start.time - 10L * 24 * 3600 * 1000))
        coEvery { offline.getCachedEvents(null) } returns listOf(inside, outside)
        assertEquals(listOf(5), repo.loadRange(start, end).map { it.id })
    }

    @Test
    fun `a month is not refetched while fresh`() = runTest {
        coEvery { api.getEventsInRange(any(), any()) } returns listOf(ev(1, "Global"))
        coEvery { offline.getCachedEvents(null) } returns listOf(ev(1, "Global"))
        repo.loadRange(start, end)
        repo.loadRange(start, end)
        coVerify(exactly = 1) { api.getEventsInRange(any(), any()) }
        repo.invalidate()
        repo.loadRange(start, end)
        coVerify(exactly = 2) { api.getEventsInRange(any(), any()) }
    }
}
