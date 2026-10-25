package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.AuthUser
import com.tobiso.tobisoappnative.model.GoogleTokenRequest
import com.tobiso.tobisoappnative.model.LoginRequest
import com.tobiso.tobisoappnative.model.RegisterRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

/** Why an auth call failed, in terms the UI can act on. */
sealed class AuthFailure(message: String) : Exception(message) {
    /** Account exists but the emailed link wasn't opened yet (the server re-sends the mail). */
    class EmailUnverified : AuthFailure("Nejdřív potvrď svůj e-mail – poslali jsme ti na něj odkaz.")
    class InvalidCredentials : AuthFailure("Nesprávný email nebo heslo.")
    class RateLimited : AuthFailure("Příliš mnoho pokusů. Zkus to prosím za chvíli.")
    class Network : AuthFailure("Nepodařilo se spojit se serverem. Zkontroluj připojení.")
    class Rejected(message: String) : AuthFailure(message)
    /** A new Google account that hasn't accepted the terms yet (server code `terms_required`). */
    class TermsRequired(message: String) : AuthFailure(message)
    class Unknown : AuthFailure("Něco se pokazilo. Zkus to prosím znovu.")
}

private val errorJson = Json { ignoreUnknownKeys = true }

/** Maps Retrofit/IO errors from the auth endpoints to an [AuthFailure]. */
fun mapAuthError(e: Throwable): AuthFailure = when (e) {
    is AuthFailure -> e
    is HttpException -> {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val obj = body?.let { runCatching { errorJson.parseToJsonElement(it).jsonObject }.getOrNull() }
        val code = obj?.get("code")?.jsonPrimitive?.contentOrNull
        val message = obj?.get("message")?.jsonPrimitive?.contentOrNull
        when {
            e.code() == 403 && code == "email_unverified" -> AuthFailure.EmailUnverified()
            e.code() == 400 && code == "terms_required" -> AuthFailure.TermsRequired(message ?: "")
            e.code() == 401 -> AuthFailure.InvalidCredentials()
            e.code() == 429 -> AuthFailure.RateLimited()
            e.code() in 400..499 && !message.isNullOrBlank() -> AuthFailure.Rejected(message)
            e.code() == 503 && !message.isNullOrBlank() -> AuthFailure.Rejected(message)
            else -> AuthFailure.Unknown()
        }
    }
    is IOException -> AuthFailure.Network()
    else -> AuthFailure.Unknown()
}

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthUser>
    /**
     * Trades a Google ID token for a Tobiso session. Fails with [AuthFailure.TermsRequired] (HTTP 400, code `terms_required`) when the Google
     * account is new and [acceptedTerms] is false.
     */
    suspend fun loginWithGoogle(idToken: String, acceptedTerms: Boolean): Result<AuthUser>
    /** Creates an unverified account; success means the confirmation mail was sent. */
    suspend fun register(email: String, displayName: String?, password: String, acceptedTerms: Boolean): Result<Unit>
    suspend fun refreshProfile(): Result<AuthUser>
    suspend fun logout()
    suspend fun stats(): Result<com.tobiso.tobisoappnative.model.UserStats>
    /** Revokes every session of the account (server-side security stamp), then signs out locally. */
    suspend fun logoutEverywhere(): Result<Unit>
}

class AuthRepositoryImpl(private val api: ApiService) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthUser> = try {
        val r = api.studentLogin(LoginRequest(email.trim(), password))
        val user = AuthUser(displayName = r.displayName, email = email.trim(), credits = r.credits)
        AuthSession.signIn(r.token, user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(mapAuthError(e))
    }

    override suspend fun loginWithGoogle(idToken: String, acceptedTerms: Boolean): Result<AuthUser> = try {
        val r = api.googleLogin(GoogleTokenRequest(idToken, acceptedTerms))
        // The response carries no email; refresh the profile lazily (auth/me) — until then the display name is the key.
        val user = AuthUser(displayName = r.displayName, email = null, credits = r.credits)
        AuthSession.signIn(r.token, user)
        refreshProfile()
        Result.success(AuthSession.user.value ?: user)
    } catch (e: Exception) {
        Result.failure(mapAuthError(e))
    }

    override suspend fun register(
        email: String, displayName: String?, password: String, acceptedTerms: Boolean
    ): Result<Unit> = try {
        api.register(RegisterRequest(email.trim(), displayName?.trim()?.takeIf { it.isNotEmpty() }, password, acceptedTerms))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapAuthError(e))
    }

    override suspend fun refreshProfile(): Result<AuthUser> = try {
        val p = api.getMe()
        val user = AuthUser(p.displayName, p.email.ifBlank { null }, p.credits)
        AuthSession.updateUser(user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(mapAuthError(e))
    }

    override suspend fun logout() = AuthSession.signOut()

    override suspend fun logoutEverywhere(): Result<Unit> = try {
        val response = api.logoutEverywhere()
        if (!response.isSuccessful) throw HttpException(response)
        AuthSession.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapAuthError(e))
    }

    override suspend fun stats(): Result<com.tobiso.tobisoappnative.model.UserStats> =
        runCatching { api.getUserStats() }
}
