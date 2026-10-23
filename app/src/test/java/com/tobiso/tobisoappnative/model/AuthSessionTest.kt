package com.tobiso.tobisoappnative.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionTest {

    @After
    fun reset() {
        AuthSession.signOut()
        AuthSession.consumeExpired()
    }

    @Test
    fun `signIn sets token and user`() {
        AuthSession.signIn("jwt", AuthUser("Jana", "j@x.cz", 3))
        assertEquals("jwt", AuthSession.token)
        assertEquals("Jana", AuthSession.user.value?.displayName)
        assertTrue(AuthSession.isSignedIn)
    }

    @Test
    fun `401 clears the session and flags it as expired`() {
        AuthSession.signIn("jwt", AuthUser("Jana"))
        AuthSession.onUnauthorized()
        assertNull(AuthSession.token)
        assertNull(AuthSession.user.value)
        assertTrue(AuthSession.sessionExpired.value)
    }

    @Test
    fun `401 while anonymous is ignored`() {
        AuthSession.onUnauthorized()
        assertFalse(AuthSession.sessionExpired.value)
    }

    @Test
    fun `signOut does not flag expiry`() {
        AuthSession.signIn("jwt", AuthUser("Jana"))
        AuthSession.signOut()
        assertFalse(AuthSession.sessionExpired.value)
        assertFalse(AuthSession.isSignedIn)
    }
}
