package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

/** `username` carries the account's email (web `LoginRequest(Username, Password)`). */
@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class StudentLoginResponse(val token: String, val displayName: String, val credits: Int = 0)

@Serializable
data class RegisterRequest(
    val email: String,
    val displayName: String?,
    val password: String,
    val acceptedTerms: Boolean
)

@Serializable
data class StudentProfile(
    val displayName: String,
    val email: String = "",
    val credits: Int = 0,
    val role: String = "student"
)

@Serializable
data class ReadProgressRequest(val postId: Int, val scrollPercent: Int)

@Serializable
data class ContinueReadingDto(
    val postId: Int,
    val title: String,
    val scrollPercent: Int,
    val categoryPath: String? = null,
    val filePath: String? = null
)

/** Body of `POST auth/google-token`; [acceptedTerms] is only needed when the Google account has no Tobiso account yet. */
@Serializable
data class GoogleTokenRequest(val idToken: String, val acceptedTerms: Boolean = false)
