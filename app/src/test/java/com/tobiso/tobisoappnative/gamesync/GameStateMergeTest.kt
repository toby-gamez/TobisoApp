package com.tobiso.tobisoappnative.gamesync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameStateMergeTest {
    private val first = MergeContext(firstSync = true, preferLocal = false, basePoints = null)
    private fun later(base: Float, dirty: Boolean = false) = MergeContext(false, dirty, base)

    private fun snap(file: String, vararg v: Pair<String, PrefValue>): PrefsSnapshot = mapOf(file to mapOf(*v))
    private fun PrefsSnapshot.get(file: String, key: String) = this[file]?.get(key)

    @Test fun `no server state keeps local untouched`() {
        val local = snap("points_prefs", "points_float" to PrefValue.F(5f))
        assertEquals(local, mergeGameState(local, null, first))
    }

    @Test fun `first sync takes the larger points and never lowers totals`() {
        val local = snap("points_prefs", "points_float" to PrefValue.F(50f), "total_earned_points" to PrefValue.I(900))
        val server = snap("points_prefs", "points_float" to PrefValue.F(80f), "total_earned_points" to PrefValue.I(500))
        val m = mergeGameState(local, server, first)
        assertEquals(PrefValue.F(80f), m.get("points_prefs", "points_float"))
        assertEquals(PrefValue.I(900), m.get("points_prefs", "total_earned_points"))
    }

    @Test fun `later sync replays the local delta on top of the server value`() {
        // synced at 100; this device earned 30 (now 130); another device pushed 150
        val local = snap("points_prefs", "points_float" to PrefValue.F(130f))
        val server = snap("points_prefs", "points_float" to PrefValue.F(150f))
        assertEquals(PrefValue.F(180f), mergeGameState(local, server, later(100f)).get("points_prefs", "points_float"))
    }

    @Test fun `points never go below zero`() {
        val local = snap("points_prefs", "points_float" to PrefValue.F(0f))
        val server = snap("points_prefs", "points_float" to PrefValue.F(10f))
        assertEquals(PrefValue.F(0f), mergeGameState(local, server, later(100f)).get("points_prefs", "points_float"))
    }

    @Test fun `after a points reset the side with the larger divisor wins`() {
        val local = snap("points_prefs", "points_float" to PrefValue.F(3f), "inflation_divisor" to PrefValue.I(10))
        val server = snap("points_prefs", "points_float" to PrefValue.F(9000f), "inflation_divisor" to PrefValue.I(1))
        val m = mergeGameState(local, server, later(8000f))
        assertEquals(PrefValue.F(3f), m.get("points_prefs", "points_float"))
        assertEquals(PrefValue.I(10), m.get("points_prefs", "inflation_divisor"))
    }

    @Test fun `purchases union, cooldowns max, freezes capped at three`() {
        val local = mapOf(
            "shop_prefs" to mapOf("purchased_5" to PrefValue.B(true), "cooldown_7" to PrefValue.L(10)),
            "streak_freeze_prefs" to mapOf("freeze_count" to PrefValue.I(3), "used_freezes" to PrefValue.SS(setOf("2026-10-01")))
        )
        val server = mapOf(
            "shop_prefs" to mapOf("purchased_6" to PrefValue.B(true), "cooldown_7" to PrefValue.L(20)),
            "streak_freeze_prefs" to mapOf("freeze_count" to PrefValue.I(2), "used_freezes" to PrefValue.SS(setOf("2026-10-02")))
        )
        val m = mergeGameState(local, server, first)
        assertEquals(PrefValue.B(true), m.get("shop_prefs", "purchased_5"))
        assertEquals(PrefValue.B(true), m.get("shop_prefs", "purchased_6"))
        assertEquals(PrefValue.L(20), m.get("shop_prefs", "cooldown_7"))
        assertEquals(PrefValue.I(3), m.get("streak_freeze_prefs", "freeze_count"))
        assertEquals(PrefValue.SS(setOf("2026-10-01", "2026-10-02")), m.get("streak_freeze_prefs", "used_freezes"))
    }

    @Test fun `streak days union and an alive pet beats a dead one`() {
        val local = mapOf(
            "StreakData" to mapOf("streak_days" to PrefValue.SS(setOf("a", "b"))),
            "pet_prefs" to mapOf("is_dead_3" to PrefValue.B(true), "growth_days_3" to PrefValue.I(2))
        )
        val server = mapOf(
            "StreakData" to mapOf("streak_days" to PrefValue.SS(setOf("b", "c"))),
            "pet_prefs" to mapOf("is_dead_3" to PrefValue.B(false), "growth_days_3" to PrefValue.I(5))
        )
        val m = mergeGameState(local, server, first)
        assertEquals(PrefValue.SS(setOf("a", "b", "c")), m.get("StreakData", "streak_days"))
        assertEquals(PrefValue.B(false), m.get("pet_prefs", "is_dead_3"))
        assertEquals(PrefValue.I(5), m.get("pet_prefs", "growth_days_3"))
    }

    @Test fun `equipped items follow the server unless the device changed since the last sync`() {
        val local = snap("backpack_prefs", "equipped_quote" to PrefValue.I(1))
        val server = snap("backpack_prefs", "equipped_quote" to PrefValue.I(2))
        assertEquals(PrefValue.I(2), mergeGameState(local, server, later(0f)).get("backpack_prefs", "equipped_quote"))
        assertEquals(PrefValue.I(1), mergeGameState(local, server, later(0f, dirty = true)).get("backpack_prefs", "equipped_quote"))
    }

    @Test fun `question stats merge takes the larger counters per question`() {
        val local = snap("question_progress_prefs", "question_stats_json" to PrefValue.S("""{"1":[2,3],"2":[1,1]}"""))
        val server = snap("question_progress_prefs", "question_stats_json" to PrefValue.S("""{"1":[1,5],"3":[4,4]}"""))
        val merged = (mergeGameState(local, server, first).get("question_progress_prefs", "question_stats_json") as PrefValue.S).v
        val json = kotlinx.serialization.json.Json.parseToJsonElement(merged).toString()
        assertEquals("""{"1":[2,5],"2":[1,1],"3":[4,4]}""", json)
    }

    @Test fun `snapshot survives the json round trip with exact pref types`() {
        val snapshot = mapOf(
            "points_prefs" to mapOf("points_float" to PrefValue.F(12.5f), "total_earned_points" to PrefValue.I(7)),
            "shop_prefs" to mapOf("purchased_1" to PrefValue.B(true), "cooldown_1" to PrefValue.L(1761300000000L)),
            "StreakData" to mapOf("streak_days" to PrefValue.SS(setOf("x", "y"))),
            "question_progress_prefs" to mapOf("question_stats_json" to PrefValue.S("{}"))
        )
        val json = snapshot.toStateJson()
        assertEquals(12.5, (json["points"] as kotlinx.serialization.json.JsonObject)["value"].toString().toDouble(), 0.0)
        assertEquals(snapshot, parseStateJson(json))
    }

    @Test fun `unknown files and garbage values are ignored when parsing`() {
        val json = kotlinx.serialization.json.Json.parseToJsonElement(
            """{"schema":9,"prefs":{"evil_prefs":{"a":true},"shop_prefs":{"ok":true,"bad":{"zzz":1},"list":[1]}}}"""
        ) as kotlinx.serialization.json.JsonObject
        val parsed = parseStateJson(json)
        assertNull(parsed["evil_prefs"])
        assertEquals(mapOf("ok" to PrefValue.B(true)), parsed["shop_prefs"])
    }
}
