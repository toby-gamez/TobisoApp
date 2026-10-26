package com.tobiso.tobisoappnative.gamesync

import android.content.Context
import com.tobiso.tobisoappnative.fake.FakeSharedPreferences
import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.AuthUser
import com.tobiso.tobisoappnative.model.GameStatePutRequest
import com.tobiso.tobisoappnative.model.GameStatePutResponse
import com.tobiso.tobisoappnative.model.GameStateResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.util.Base64

class GameSyncManagerTest {
    private val stores = HashMap<String, FakeSharedPreferences>()
    private val ctx = mockk<Context>(relaxed = true)
    private val api = mockk<ApiService>()
    private lateinit var sync: GameSyncManager

    private fun prefs(name: String) = stores.getOrPut(name) { FakeSharedPreferences() }
    private val meta get() = prefs("game_sync_meta")

    private fun jwt(sub: String): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        return listOf("""{"alg":"HS256"}""", """{"sub":"$sub"}""", "sig").joinToString(".") { enc.encodeToString(it.toByteArray()) }
    }

    private fun serverState(points: Float): JsonObject =
        mapOf(POINTS_FILE to mapOf(KEY_POINTS to PrefValue.F(points))).toStateJson()

    /** Local progress the server lacks, so a sync has something to push. */
    private fun localOnlyPurchase() = prefs("shop_prefs").edit().putBoolean("purchased_5", true).apply()

    private fun conflict() = Response.error<GameStatePutResponse>(409, "{}".toResponseBody("application/json".toMediaType()))

    @Before
    fun setUp() {
        every { ctx.applicationContext } returns ctx
        every { ctx.getSharedPreferences(any(), any()) } answers { prefs(firstArg()) }
        sync = GameSyncManager(ctx, api)
        AuthSession.signIn(jwt("7"), AuthUser("Jana"))
    }

    @After
    fun tearDown() = AuthSession.signOut()

    @Test
    fun `anonymous users never touch the server`() = runBlocking {
        AuthSession.signOut()
        assertFalse(sync.sync())
        coVerify(exactly = 0) { api.getGameState() }
    }

    @Test
    fun `account with no stored state pushes local progress at revision 0`() = runBlocking {
        prefs(POINTS_FILE).edit().putFloat(KEY_POINTS, 42f).apply()
        coEvery { api.getGameState() } returns Response.success(204, null as GameStateResponse?)
        val sent = mutableListOf<GameStatePutRequest>()
        coEvery { api.putGameState(capture(sent)) } returns Response.success(GameStatePutResponse(revision = 1))

        assertTrue(sync.sync())
        assertEquals(0, sent.single().baseRevision)
        assertEquals(1, meta.getInt("u7.revision", -1))
        assertFalse(meta.getBoolean("u7.dirty", true))
    }

    @Test
    fun `409 conflict pulls again, re-merges and retries with the new revision`() = runBlocking {
        prefs(POINTS_FILE).edit().putFloat(KEY_POINTS, 10f).apply()
        localOnlyPurchase()
        coEvery { api.getGameState() } returnsMany listOf(
            Response.success(GameStateResponse(3, null, serverState(20f))),
            Response.success(GameStateResponse(4, null, serverState(25f)))
        )
        val sent = mutableListOf<GameStatePutRequest>()
        coEvery { api.putGameState(capture(sent)) } returnsMany listOf(conflict(), Response.success(GameStatePutResponse(revision = 5)))

        assertTrue(sync.sync())
        assertEquals(listOf(3, 4), sent.map { it.baseRevision })
        assertEquals(5, meta.getInt("u7.revision", -1))
        // first sync takes the larger side, and the winning value is applied locally
        assertEquals(25f, prefs(POINTS_FILE).getFloat(KEY_POINTS, 0f))
    }

    @Test
    fun `gives up after three conflicts`() = runBlocking {
        localOnlyPurchase()
        coEvery { api.getGameState() } returns Response.success(GameStateResponse(1, null, serverState(1f)))
        coEvery { api.putGameState(any()) } returns conflict()

        assertFalse(sync.sync())
        coVerify(exactly = 3) { api.putGameState(any()) }
    }

    @Test
    fun `network failure and server errors are swallowed and reported as not synced`() = runBlocking {
        localOnlyPurchase()
        coEvery { api.getGameState() } throws IOException("offline")
        assertFalse(sync.sync())

        coEvery { api.getGameState() } returns Response.success(GameStateResponse(1, null, serverState(1f)))
        coEvery { api.putGameState(any()) } returns Response.error(500, "".toResponseBody(null))
        assertFalse(sync.sync())

        coEvery { api.getGameState() } returns Response.error(503, "".toResponseBody(null))
        assertFalse(sync.sync())
    }

    @Test
    fun `nothing is pushed when the merged state already equals the server's`() = runBlocking {
        prefs(POINTS_FILE).edit().putFloat(KEY_POINTS, 5f).apply()
        coEvery { api.getGameState() } returns Response.success(GameStateResponse(2, null, serverState(20f)))

        assertTrue(sync.sync())
        coVerify(exactly = 0) { api.putGameState(any()) }
    }

    @Test
    fun `local changes since the last sync win over the server for non-monotonic fields`() = runBlocking {
        meta.edit().putInt("u7.revision", 2).putFloat("u7.points", 10f).putBoolean("u7.dirty", true).apply()
        prefs("pet_prefs").edit().putFloat("pet_food", 9f).apply()
        val server = mapOf("pet_prefs" to mapOf("pet_food" to PrefValue.F(1f))).toStateJson()
        coEvery { api.getGameState() } returns Response.success(GameStateResponse(2, null, server))
        val sent = mutableListOf<GameStatePutRequest>()
        coEvery { api.putGameState(capture(sent)) } returns Response.success(GameStatePutResponse(revision = 3))

        assertTrue(sync.sync())
        assertEquals(9f, prefs("pet_prefs").getFloat("pet_food", 0f))
        assertEquals(PrefValue.F(9f), parseStateJson(sent.single().state)["pet_prefs"]?.get("pet_food"))
    }

    @Test
    fun `sign-out flush is free when nothing changed and fails when a dirty state cannot be pushed`() = runBlocking {
        assertTrue(sync.flushBeforeSignOut())
        coVerify(exactly = 0) { api.getGameState() }

        meta.edit().putBoolean("u7.dirty", true).apply()
        coEvery { api.getGameState() } throws IOException("offline")
        assertFalse(sync.flushBeforeSignOut())
    }

    @Test
    fun `wipeLocalState clears the synced prefs and only that account's bookkeeping`() {
        SYNCED_PREFS.forEach { prefs(it).edit().putString("k", "v").apply() }
        prefs("settings").edit().putString("theme", "dark").apply() // not gamification: must survive
        meta.edit().putInt("u7.revision", 4).putBoolean("u7.dirty", true).putInt("u8.revision", 9).apply()

        sync.wipeLocalState("u7")

        SYNCED_PREFS.forEach { assertTrue("$it should be empty", prefs(it).all.isEmpty()) }
        assertEquals("dark", prefs("settings").getString("theme", null))
        assertFalse(meta.contains("u7.revision"))
        assertFalse(meta.contains("u7.dirty"))
        assertEquals(9, meta.getInt("u8.revision", -1))
    }
}
