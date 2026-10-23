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
