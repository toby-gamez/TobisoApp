package com.tobiso.tobisoappnative.repository

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AuthErrorMappingTest {

    private fun http(code: Int, body: String) = HttpException(
        Response.error<Any>(code, body.toResponseBody("application/json".toMediaType()))
    )

    @Test
    fun `403 email_unverified maps to EmailUnverified`() {
        val f = mapAuthError(http(403, """{"code":"email_unverified","message":"x"}"""))
        assertTrue(f is AuthFailure.EmailUnverified)
    }

    @Test
    fun `401 maps to InvalidCredentials`() {
        assertTrue(mapAuthError(http(401, "{}")) is AuthFailure.InvalidCredentials)
    }

    @Test
    fun `429 maps to RateLimited`() {
        assertTrue(mapAuthError(http(429, "")) is AuthFailure.RateLimited)
    }

    @Test
    fun `400 keeps the server message`() {
        val f = mapAuthError(http(400, """{"message":"Heslo musí mít alespoň 10 znaků."}"""))
        assertEquals("Heslo musí mít alespoň 10 znaků.", f.message)
    }

    @Test
    fun `io errors map to Network and others to Unknown`() {
        assertTrue(mapAuthError(IOException("boom")) is AuthFailure.Network)
        assertTrue(mapAuthError(IllegalStateException()) is AuthFailure.Unknown)
        assertTrue(mapAuthError(http(500, "")) is AuthFailure.Unknown)
    }
}
