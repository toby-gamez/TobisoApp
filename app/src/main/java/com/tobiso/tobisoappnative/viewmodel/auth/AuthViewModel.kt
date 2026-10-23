package com.tobiso.tobisoappnative.viewmodel.auth

import androidx.lifecycle.viewModelScope
import com.tobiso.tobisoappnative.base.BaseViewModel
import com.tobiso.tobisoappnative.repository.AuthFailure
import com.tobiso.tobisoappnative.repository.AuthRepository
import com.tobiso.tobisoappnative.repository.mapAuthError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : BaseViewModel<AuthState, AuthIntent, AuthEffect>(AuthState()) {

    override fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged -> setState { copy(email = intent.value, error = null) }
            is AuthIntent.DisplayNameChanged -> setState { copy(displayName = intent.value, error = null) }
            is AuthIntent.PasswordChanged -> setState { copy(password = intent.value, error = null) }
            is AuthIntent.TermsChanged -> setState { copy(acceptedTerms = intent.value, error = null) }
            is AuthIntent.SetMode -> setState { copy(mode = intent.mode, error = null) }
            AuthIntent.BackToLogin -> setState {
                copy(mode = AuthMode.LOGIN, verificationEmail = null, justRegistered = false, password = "", error = null)
            }
            AuthIntent.Submit -> submit()
        }
    }

    private fun validate(s: AuthState): String? = when {
        !s.email.contains('@') || s.email.length > 256 -> "Zadej platný e-mail."
        s.password.isEmpty() -> "Zadej heslo."
        s.mode == AuthMode.REGISTER && s.password.length < MIN_PASSWORD_LENGTH ->
            "Heslo musí mít alespoň $MIN_PASSWORD_LENGTH znaků."
        s.mode == AuthMode.REGISTER && !s.acceptedTerms ->
            "Pro registraci je nutný souhlas s Podmínkami použití a Zásadami ochrany osobních údajů."
        else -> null
    }

    private fun submit() {
        val s = currentState
        if (s.isLoading) return
        validate(s)?.let { msg -> setState { copy(error = msg) }; return }
        setState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (s.mode) {
                AuthMode.LOGIN -> repo.login(s.email, s.password).fold(
                    onSuccess = {
                        setState { copy(isLoading = false, password = "") }
                        emitEffect(AuthEffect.SignedIn)
                    },
                    onFailure = { e -> onFailure(mapAuthError(e), s.email) }
                )
                AuthMode.REGISTER -> repo.register(s.email, s.displayName, s.password, s.acceptedTerms).fold(
                    onSuccess = {
                        setState {
                            copy(isLoading = false, password = "", verificationEmail = s.email.trim(), justRegistered = true)
                        }
                    },
                    onFailure = { e -> onFailure(mapAuthError(e), s.email) }
                )
            }
        }
    }

    private fun onFailure(failure: AuthFailure, email: String) {
        if (failure is AuthFailure.EmailUnverified) {
            setState { copy(isLoading = false, password = "", verificationEmail = email.trim(), justRegistered = false) }
        } else {
            setState { copy(isLoading = false, error = failure.message) }
        }
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 10
    }
}
