package com.tobiso.tobisoappnative.gamesync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/** SharedPreferences files that make up the gamification state (everything else stays device-only). */
val SYNCED_PREFS = listOf(
    "points_prefs", "shop_prefs", "backpack_prefs", "streak_freeze_prefs",
    "StreakData", "streak_milestones", "pet_prefs", "question_progress_prefs"
)

const val POINTS_FILE = "points_prefs"
const val KEY_POINTS = "points_float"
const val KEY_DIVISOR = "inflation_divisor"
private const val MAX_FREEZES = 3

/**
 * @param firstSync this account has never synced from this device: never lose local progress, take the larger side.
 * @param preferLocal the device changed something since the last sync, so non-monotonic fields keep the local value.
 * @param basePoints points as of the last successful sync (to replay the local delta on top of a newer server value).
 */
data class MergeContext(val firstSync: Boolean, val preferLocal: Boolean, val basePoints: Float?)

/** Merges the device state with the server state following the rules in `docs/WEB_SPEC.md` §2. Pure. */
fun mergeGameState(local: PrefsSnapshot, server: PrefsSnapshot?, ctx: MergeContext): PrefsSnapshot {
    if (server == null) return local
    val files = (local.keys + server.keys).associateWith { file ->
        mergeFile(file, local[file].orEmpty(), server[file].orEmpty(), ctx)
    }
    return files.filterValues { it.isNotEmpty() }
}

private fun mergeFile(file: String, l: Map<String, PrefValue>, s: Map<String, PrefValue>, ctx: MergeContext): Map<String, PrefValue> {
    val out = LinkedHashMap<String, PrefValue>()
    for (key in l.keys + s.keys) {
        val lv = l[key]; val sv = s[key]
        val v = when {
            lv == null -> sv
            sv == null -> lv
            lv == sv -> lv
            else -> mergeValue(file, key, lv, sv, ctx)
        }
        if (v != null) out[key] = v
    }
    if (file == POINTS_FILE) {
        mergePoints(l, s, ctx)?.let { out[KEY_POINTS] = it }
        // The multiplier value follows whichever side's boost lasts longer.
        val le = (l["multiplier_end_time"] as? PrefValue.L)?.v ?: 0L
        val se = (s["multiplier_end_time"] as? PrefValue.L)?.v ?: 0L
        val winner = if (le >= se) l else s
        winner["active_multiplier"]?.let { out["active_multiplier"] = it }
    }
    return out
}

private fun fallback(l: PrefValue, s: PrefValue, ctx: MergeContext) = if (ctx.preferLocal) l else s

private fun mergeValue(file: String, key: String, l: PrefValue, s: PrefValue, ctx: MergeContext): PrefValue = when {
    // Monotonic numbers: never go backwards.
    file == POINTS_FILE && key in setOf("total_earned_points", "highest_prestige_shown", KEY_DIVISOR) -> maxOf(l, s)
    file == POINTS_FILE && key == "multiplier_end_time" -> maxOf(l, s)
    file == POINTS_FILE && key == "active_multiplier" -> fallback(l, s, ctx) // follows the end time below
    file == "shop_prefs" && key.startsWith("purchased_date_") -> minPositive(l, s)
    file == "shop_prefs" && key.startsWith("cooldown_") -> maxOf(l, s)
    file == "shop_prefs" && key.startsWith("purchased_") -> orOf(l, s)
    file == "streak_freeze_prefs" && key == "freeze_count" ->
        PrefValue.I(minOf(maxOf(asInt(l), asInt(s)), MAX_FREEZES))
    file == "streak_freeze_prefs" && key == "used_freezes" -> union(l, s)
    file == "StreakData" && key == "streak_days" -> union(l, s)
    file == "streak_milestones" -> orOf(l, s)
    file == "pet_prefs" && (key.startsWith("last_fed_") || key.startsWith("last_watered_") || key.startsWith("growth_days_")) -> maxOf(l, s)
    file == "pet_prefs" && key.startsWith("acquired_") -> orOf(l, s)
    // A pet that one side already revived is alive.
    file == "pet_prefs" && key.startsWith("is_dead_") -> andOf(l, s)
    file == "pet_prefs" && (key == "pet_food" || key == "pet_water") ->
        if (ctx.firstSync) maxOf(l, s) else fallback(l, s, ctx)
    file == "question_progress_prefs" && key == "question_stats_json" -> mergeQuestionStats(l, s)
    else -> fallback(l, s, ctx)
}

/** Points: take the larger on first sync; later replay the local delta since the last sync onto the server value. */
private fun mergePoints(l: Map<String, PrefValue>, s: Map<String, PrefValue>, ctx: MergeContext): PrefValue? {
    val lp = (l[KEY_POINTS] as? PrefValue.F)?.v
    val sp = (s[KEY_POINTS] as? PrefValue.F)?.v
    if (lp == null || sp == null) return (l[KEY_POINTS] ?: s[KEY_POINTS])
    val ld = (l[KEY_DIVISOR] as? PrefValue.I)?.v ?: 1
    val sd = (s[KEY_DIVISOR] as? PrefValue.I)?.v ?: 1
    // After a points reset (deflation) the side with the larger divisor has the post-reset balance.
    if (ld != sd) return PrefValue.F(if (ld > sd) lp else sp)
    val merged = if (ctx.firstSync || ctx.basePoints == null) maxOf(lp, sp)
    else maxOf(0f, sp + (lp - ctx.basePoints))
    return PrefValue.F(merged)
}

private fun mergeQuestionStats(l: PrefValue, s: PrefValue): PrefValue {
    val lj = parseStats(l); val sj = parseStats(s)
    val ids = lj.keys + sj.keys
    val merged = ids.associateWith { id ->
        val a = lj[id]; val b = sj[id]
        JsonArray(listOf(
            JsonPrimitive(maxOf(a?.first ?: 0, b?.first ?: 0)),
            JsonPrimitive(maxOf(a?.second ?: 0, b?.second ?: 0))
        ))
    }
    return PrefValue.S(JsonObject(merged).toString())
}

private fun parseStats(v: PrefValue): Map<String, Pair<Int, Int>> = runCatching {
    Json.parseToJsonElement((v as PrefValue.S).v).jsonObject.mapNotNull { (k, e) ->
        val arr = e as? JsonArray ?: return@mapNotNull null
        val correct = (arr.getOrNull(0) as? JsonPrimitive)?.intOrNull ?: 0
        val total = (arr.getOrNull(1) as? JsonPrimitive)?.intOrNull ?: 0
        k to (correct to total)
    }.toMap()
}.getOrDefault(emptyMap())

private fun asInt(v: PrefValue) = (v as? PrefValue.I)?.v ?: 0
private fun maxOf(a: PrefValue, b: PrefValue): PrefValue = when {
    a is PrefValue.I && b is PrefValue.I -> if (a.v >= b.v) a else b
    a is PrefValue.L && b is PrefValue.L -> if (a.v >= b.v) a else b
    a is PrefValue.F && b is PrefValue.F -> if (a.v >= b.v) a else b
    else -> a
}
private fun minPositive(a: PrefValue, b: PrefValue): PrefValue {
    if (a is PrefValue.L && b is PrefValue.L) return when {
        a.v <= 0 -> b
        b.v <= 0 -> a
        else -> if (a.v <= b.v) a else b
    }
    return a
}
private fun orOf(a: PrefValue, b: PrefValue): PrefValue =
    if (a is PrefValue.B && b is PrefValue.B) PrefValue.B(a.v || b.v) else a
private fun andOf(a: PrefValue, b: PrefValue): PrefValue =
    if (a is PrefValue.B && b is PrefValue.B) PrefValue.B(a.v && b.v) else a
private fun union(a: PrefValue, b: PrefValue): PrefValue =
    if (a is PrefValue.SS && b is PrefValue.SS) PrefValue.SS(a.v + b.v) else a

// ---- JSON (de)serialisation of the whole snapshot ---------------------------------------------------------------

private const val SCHEMA = 1

/** `{"schema":1,"points":{"value":12.5},"prefs":{file:{key:value}}}` — `points.value` is mirrored by the server. */
fun PrefsSnapshot.toStateJson(): JsonObject = JsonObject(
    mapOf(
        "schema" to JsonPrimitive(SCHEMA),
        "streak" to streakJson(),
        "points" to JsonObject(mapOf("value" to JsonPrimitive(((this[POINTS_FILE]?.get(KEY_POINTS) as? PrefValue.F)?.v ?: 0f)))),
        "prefs" to JsonObject(
            this.filterKeys { it in SYNCED_PREFS }.mapValues { (_, m) ->
                JsonObject(m.mapValues { it.value.toJson() })
            }
        )
    )
)

/**
 * Streak in the shape the web reads (`state.streak`, see docs/WEB_SPEC.md): active days, owned freezes and the days
 * freezes were used on. A read-only mirror of the prefs; [parseStateJson] ignores it.
 */
fun PrefsSnapshot.streakJson(): JsonObject {
    fun days(file: String, key: String) =
        ((this[file]?.get(key) as? PrefValue.SS)?.v ?: emptySet()).sorted().map { JsonPrimitive(it) }
    val freezes = (this["streak_freeze_prefs"]?.get("freeze_count") as? PrefValue.I)?.v ?: 0
    return JsonObject(
        mapOf(
            "days" to JsonArray(days("StreakData", "streak_days")),
            "freezes" to JsonPrimitive(freezes.coerceIn(0, MAX_FREEZES)),
            "usedFreezes" to JsonArray(days("streak_freeze_prefs", "used_freezes"))
        )
    )
}

/** Parses what [toStateJson] wrote. Unknown files/keys/values are dropped; a missing `prefs` yields an empty snapshot. */
fun parseStateJson(state: JsonObject): PrefsSnapshot {
    val prefs = state["prefs"] as? JsonObject ?: return emptyMap()
    return prefs.entries.filter { it.key in SYNCED_PREFS }.associate { (file, body) ->
        file to ((body as? JsonObject)?.entries?.mapNotNull { (k, e) -> PrefValue.fromJson(e)?.let { k to it } }?.toMap().orEmpty())
    }
}

/** Points balance of a snapshot, if it has one. */
fun PrefsSnapshot.points(): Float? = (this[POINTS_FILE]?.get(KEY_POINTS) as? PrefValue.F)?.v
