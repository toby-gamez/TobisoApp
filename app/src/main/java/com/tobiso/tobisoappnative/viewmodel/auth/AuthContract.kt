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
    val justRegistered: Boolean = false,
    /** Google ID token waiting for the user to accept the terms (new Google account). */
    val googleTermsToken: String? = null
) : UiState

sealed interface AuthIntent : UiIntent {
    data class EmailChanged(val value: String) : AuthIntent
    data class DisplayNameChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data class TermsChanged(val value: Boolean) : AuthIntent
    data class SetMode(val mode: AuthMode) : AuthIntent
    object Submit : AuthIntent
    data class GoogleToken(val idToken: String) : AuthIntent
    data class GoogleFailed(val message: String) : AuthIntent
    /** The user accepted the terms for the pending Google sign-up. */
    object AcceptGoogleTerms : AuthIntent
    object DismissGoogleTerms : AuthIntent
    /** Leave the "check your inbox" state and go back to the login form. */
    object BackToLogin : AuthIntent
}

sealed interface AuthEffect : UiEffect {
    object SignedIn : AuthEffect
}
