package com.tobiso.tobisoappnative.viewmodel.auth

import app.cash.turbine.test
import com.tobiso.tobisoappnative.model.AuthUser
import com.tobiso.tobisoappnative.repository.AuthFailure
import com.tobiso.tobisoappnative.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthViewModelTest {

    private lateinit var repo: AuthRepository
    private lateinit var vm: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        repo = mockk()
        vm = AuthViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    private fun fillLogin(email: String = "a@b.cz", password: String = "secret") {
        vm.onIntent(AuthIntent.EmailChanged(email))
        vm.onIntent(AuthIntent.PasswordChanged(password))
    }

    @Test
    fun `login success emits SignedIn`() = runTest {
        coEvery { repo.login("a@b.cz", "secret") } returns Result.success(AuthUser("A"))
        fillLogin()
        vm.effect.test {
            vm.onIntent(AuthIntent.Submit)
            assertEquals(AuthEffect.SignedIn, awaitItem())
        }
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("", vm.uiState.value.password)
    }

    @Test
    fun `invalid email is rejected without calling the server`() = runTest {
        fillLogin(email = "nope")
        vm.onIntent(AuthIntent.Submit)
        assertNotNull(vm.uiState.value.error)
        coVerify(exactly = 0) { repo.login(any(), any()) }
    }

    @Test
    fun `register enforces password length and terms`() = runTest {
        vm.onIntent(AuthIntent.SetMode(AuthMode.REGISTER))
        fillLogin(password = "short")
        vm.onIntent(AuthIntent.Submit)
        assertTrue(vm.uiState.value.error!!.contains("10"))

        vm.onIntent(AuthIntent.PasswordChanged("longenough1"))
        vm.onIntent(AuthIntent.Submit)
        assertTrue(vm.uiState.value.error!!.contains("souhlas"))
        coVerify(exactly = 0) { repo.register(any(), any(), any(), any()) }
    }

    @Test
    fun `register success shows the check-your-inbox state`() = runTest {
        coEvery { repo.register("a@b.cz", any(), "longenough1", true) } returns Result.success(Unit)
        vm.onIntent(AuthIntent.SetMode(AuthMode.REGISTER))
        fillLogin(password = "longenough1")
        vm.onIntent(AuthIntent.TermsChanged(true))
        vm.onIntent(AuthIntent.Submit)

        assertEquals("a@b.cz", vm.uiState.value.verificationEmail)
        assertTrue(vm.uiState.value.justRegistered)
        assertEquals("", vm.uiState.value.password)
    }

    @Test
    fun `unverified login shows the inbox state instead of an error`() = runTest {
        coEvery { repo.login(any(), any()) } returns Result.failure(AuthFailure.EmailUnverified())
        fillLogin()
        vm.onIntent(AuthIntent.Submit)

        assertEquals("a@b.cz", vm.uiState.value.verificationEmail)
        assertFalse(vm.uiState.value.justRegistered)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `wrong credentials surface the server message`() = runTest {
        coEvery { repo.login(any(), any()) } returns Result.failure(AuthFailure.InvalidCredentials())
        fillLogin()
        vm.onIntent(AuthIntent.Submit)

        assertEquals("Nesprávný email nebo heslo.", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    // ── Google sign-in ───────────────────────────────────────────────────

    @Test
    fun `google token for an existing account signs in`() = runTest {
        coEvery { repo.loginWithGoogle("tok", false) } returns Result.success(AuthUser("A"))
        vm.effect.test {
            vm.onIntent(AuthIntent.GoogleToken("tok"))
            assertEquals(AuthEffect.SignedIn, awaitItem())
        }
        assertNull(vm.uiState.value.googleTermsToken)
    }

    @Test
    fun `new google account asks for the terms and retries with the same token`() = runTest {
        coEvery { repo.loginWithGoogle("tok", false) } returns Result.failure(AuthFailure.TermsRequired("terms"))
        coEvery { repo.loginWithGoogle("tok", true) } returns Result.success(AuthUser("A"))

        vm.onIntent(AuthIntent.GoogleToken("tok"))
        assertEquals("tok", vm.uiState.value.googleTermsToken)
        assertNull(vm.uiState.value.error)

        vm.effect.test {
            vm.onIntent(AuthIntent.AcceptGoogleTerms)
            assertEquals(AuthEffect.SignedIn, awaitItem())
        }
        assertNull(vm.uiState.value.googleTermsToken)
        coVerify { repo.loginWithGoogle("tok", true) }
    }

    @Test
    fun `declining the terms drops the token`() = runTest {
        coEvery { repo.loginWithGoogle("tok", false) } returns Result.failure(AuthFailure.TermsRequired("terms"))
        vm.onIntent(AuthIntent.GoogleToken("tok"))
        vm.onIntent(AuthIntent.DismissGoogleTerms)
        assertNull(vm.uiState.value.googleTermsToken)
        coVerify(exactly = 0) { repo.loginWithGoogle(any(), true) }
    }

    @Test
    fun `a rejected google token after accepting terms shows the error`() = runTest {
        coEvery { repo.loginWithGoogle("tok", false) } returns Result.failure(AuthFailure.TermsRequired("terms"))
        coEvery { repo.loginWithGoogle("tok", true) } returns Result.failure(AuthFailure.InvalidCredentials())
        vm.onIntent(AuthIntent.GoogleToken("tok"))
        vm.onIntent(AuthIntent.AcceptGoogleTerms)
        assertEquals("Nesprávný email nebo heslo.", vm.uiState.value.error)
        assertNull(vm.uiState.value.googleTermsToken)
    }

    @Test
    fun `google picker failure is shown`() = runTest {
        vm.onIntent(AuthIntent.GoogleFailed("Na zařízení není žádný účet Google."))
        assertEquals("Na zařízení není žádný účet Google.", vm.uiState.value.error)
    }
}
