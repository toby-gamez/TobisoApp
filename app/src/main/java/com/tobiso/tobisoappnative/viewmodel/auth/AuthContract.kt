package com.tobiso.tobisoappnative.viewmodel.auth

import com.tobiso.tobisoappnative.base.UiEffect
import com.tobiso.tobisoappnative.base.UiIntent
import com.tobiso.tobisoappnative.base.UiState

enum class AuthMode { LOGIN, REGISTER }

data class AuthState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val displayName: String = "",
    val password: String = "",
    val acceptedTerms: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Set once a confirmation mail was sent / is pending: shows the "check your inbox" state. */
    val verificationEmail: String? = null,
    /** True when [verificationEmail] was set by a registration (false: login hit an unverified account). */
    val justRegistered: Boolean = false
) : UiState

sealed interface AuthIntent : UiIntent {
    data class EmailChanged(val value: String) : AuthIntent
    data class DisplayNameChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data class TermsChanged(val value: Boolean) : AuthIntent
    data class SetMode(val mode: AuthMode) : AuthIntent
    object Submit : AuthIntent
    /** Leave the "check your inbox" state and go back to the login form. */
    object BackToLogin : AuthIntent
}

sealed interface AuthEffect : UiEffect {
    object SignedIn : AuthEffect
}
