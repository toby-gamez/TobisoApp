package com.tobiso.tobisoappnative.viewmodel.auth

import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.AuthUser
import com.tobiso.tobisoappnative.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LegalNoticeTest {

    private lateinit var repo: AuthRepository
    private lateinit var vm: AccountViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        repo = mockk()
        vm = AccountViewModel(repo, mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        AuthSession.signOut()
        Dispatchers.setMain(Dispatchers.Default)
    }

    @Test
    fun `signed out users are never asked`() {
        vm.loadLegalNotice()

        coVerify(exactly = 0) { repo.pendingLegalNotice() }
        assertNull(vm.legalChangedAt.value)
    }

    @Test
    fun `pending notice is exposed for signed in users`() {
        AuthSession.signIn("t", AuthUser(displayName = "Ája"))
        coEvery { repo.pendingLegalNotice() } returns Result.success("2026-11-01T08:00:00")

        vm.loadLegalNotice()

        assertEquals("2026-11-01T08:00:00", vm.legalChangedAt.value)
    }

    @Test
    fun `a failing check shows nothing`() {
        AuthSession.signIn("t", AuthUser(displayName = "Ája"))
        coEvery { repo.pendingLegalNotice() } returns Result.failure(RuntimeException("offline"))

        vm.loadLegalNotice()

        assertNull(vm.legalChangedAt.value)
    }

    @Test
    fun `dismissing hides the notice and echoes the server timestamp`() {
        AuthSession.signIn("t", AuthUser(displayName = "Ája"))
        coEvery { repo.pendingLegalNotice() } returns Result.success("2026-11-01T08:00:00")
        coEvery { repo.dismissLegalNotice(any()) } returns Result.success(Unit)
        vm.loadLegalNotice()

        vm.dismissLegalNotice()

        assertNull(vm.legalChangedAt.value)
        coVerify(exactly = 1) { repo.dismissLegalNotice("2026-11-01T08:00:00") }
    }

    @Test
    fun `dismissing with nothing pending does nothing`() {
        vm.dismissLegalNotice()

        coVerify(exactly = 0) { repo.dismissLegalNotice(any()) }
    }
}
