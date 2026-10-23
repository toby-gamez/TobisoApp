package com.tobiso.tobisoappnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class JwtSubjectTest {
    private fun jwt(payload: String): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        return listOf("""{"alg":"HS256"}""", payload, "sig").joinToString(".") { enc.encodeToString(it.toByteArray()) }
    }

    @Test
    fun `reads the sub claim`() {
        assertEquals("42", jwtSubject(jwt("""{"sub":"42","name":"Jana"}""")))
    }

    @Test
    fun `missing sub or garbage gives null`() {
        assertNull(jwtSubject(jwt("""{"name":"x"}""")))
        assertNull(jwtSubject("not-a-jwt"))
        assertNull(jwtSubject("a.%%%.c"))
    }

    @Test
    fun `userId parses numeric subjects`() {
        AuthSession.signIn(jwt("""{"sub":"7"}"""), AuthUser("x"))
        try {
            assertEquals(7, AuthSession.userId)
        } finally {
            AuthSession.signOut()
        }
    }

    @Test
    fun `ownerKey is based on the account id and survives profile updates`() {
        AuthSession.signIn(jwt("""{"sub":"12"}"""), AuthUser("Jana"))
        try {
            assertEquals("u12", AuthSession.ownerKey)
            AuthSession.updateUser(AuthUser("Jana", "j@x.cz"))
            assertEquals("u12", AuthSession.ownerKey)
        } finally {
            AuthSession.signOut()
        }
        assertNull(AuthSession.ownerKey)
    }
}
