package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.gamesync.GameSyncManager
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.AuthUser
import com.tobiso.tobisoappnative.model.DeleteAccountRequest
import com.tobiso.tobisoappnative.viewmodel.auth.AccountViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AccountDeletionTest {

    private lateinit var api: ApiService

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        api = mockk()
        AuthSession.signIn("header.eyJzdWIiOiI3In0.sig", AuthUser(displayName = "Ája"))
    }

    @After
    fun tearDown() {
        AuthSession.signOut()
        Dispatchers.setMain(Dispatchers.Default)
    }

    @Test
    fun `successful deletion sends the confirmation and signs out`() = runBlocking {
        coEvery { api.deleteAccount(any()) } returns Response.success(Unit)

        val result = AuthRepositoryImpl(api).deleteAccount()

        assertTrue(result.isSuccess)
        assertFalse(AuthSession.isSignedIn)
        coVerify { api.deleteAccount(DeleteAccountRequest(confirm = true)) }
    }

    @Test
    fun `rejected deletion keeps the session`() = runBlocking {
        coEvery { api.deleteAccount(any()) } returns
            Response.error(400, """{"message":"x"}""".toResponseBody("application/json".toMediaType()))

        val result = AuthRepositoryImpl(api).deleteAccount()

        assertTrue(result.isFailure)
        assertTrue(AuthSession.isSignedIn)
    }

    @Test
    fun `network failure keeps the session`() = runBlocking {
        coEvery { api.deleteAccount(any()) } throws java.io.IOException("offline")

        val result = AuthRepositoryImpl(api).deleteAccount()

        assertTrue(result.exceptionOrNull() is AuthFailure.Network)
        assertTrue(AuthSession.isSignedIn)
    }

    @Test
    fun `view model wipes local state only after a successful deletion`() {
        val repo = mockk<AuthRepository>()
        val gameSync = mockk<GameSyncManager>(relaxed = true)
        val vm = AccountViewModel(repo, gameSync)

        coEvery { repo.deleteAccount() } returns Result.failure(AuthFailure.Network())
        var ok: Boolean? = null
        vm.deleteAccount { ok = it }
        assertEquals(false, ok)
        verify(exactly = 0) { gameSync.wipeLocalState(any()) }

        coEvery { repo.deleteAccount() } returns Result.success(Unit)
        vm.deleteAccount { ok = it }
        assertEquals(true, ok)
        verify(exactly = 1) { gameSync.wipeLocalState(any()) }
    }
}
