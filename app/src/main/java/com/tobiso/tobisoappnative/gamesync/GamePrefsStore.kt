package com.tobiso.tobisoappnative.gamesync

import android.content.Context
import android.content.SharedPreferences

/** Reads and writes the synced SharedPreferences files as a [PrefsSnapshot]. */
class GamePrefsStore(context: Context) {
    private val ctx = context.applicationContext

    fun prefs(file: String): SharedPreferences = ctx.getSharedPreferences(file, Context.MODE_PRIVATE)

    fun read(): PrefsSnapshot = SYNCED_PREFS.associateWith { file ->
        prefs(file).all.mapNotNull { (k, v) -> toPrefValue(v)?.let { k to it } }.toMap()
    }.filterValues { it.isNotEmpty() }

    /** Current value of one key (null when absent), used to tell our own writes from the user's changes. */
    fun valueOf(file: String, key: String): PrefValue? = toPrefValue(prefs(file).all[key])

    private fun toPrefValue(v: Any?): PrefValue? = when (v) {
        is Int -> PrefValue.I(v)
        is Long -> PrefValue.L(v)
        is Float -> PrefValue.F(v)
        is Boolean -> PrefValue.B(v)
        is String -> PrefValue.S(v)
        is Set<*> -> PrefValue.SS(v.filterIsInstance<String>().toSet())
        else -> null
    }

    /** Writes every value of [snapshot] (never removes keys; the merge keeps the union). */
    fun write(snapshot: PrefsSnapshot) {
        snapshot.forEach { (file, values) ->
            if (file !in SYNCED_PREFS) return@forEach
            val e = prefs(file).edit()
            values.forEach { (k, v) ->
                when (v) {
                    is PrefValue.I -> e.putInt(k, v.v)
                    is PrefValue.L -> e.putLong(k, v.v)
                    is PrefValue.F -> e.putFloat(k, v.v)
                    is PrefValue.B -> e.putBoolean(k, v.v)
                    is PrefValue.S -> e.putString(k, v.v)
                    is PrefValue.SS -> e.putStringSet(k, v.v)
                }
            }
            e.apply()
        }
    }
}
