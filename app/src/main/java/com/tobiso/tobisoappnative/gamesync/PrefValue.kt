package com.tobiso.tobisoappnative.gamesync

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * One SharedPreferences value, typed so a round trip through JSON restores the exact prefs type.
 * JSON form: a bare boolean/string, `{"i":3}`, `{"l":3}`, `{"f":1.5}` or `{"ss":["a","b"]}`.
 */
sealed interface PrefValue {
    data class I(val v: Int) : PrefValue
    data class L(val v: Long) : PrefValue
    data class F(val v: Float) : PrefValue
    data class B(val v: Boolean) : PrefValue
    data class S(val v: String) : PrefValue
    data class SS(val v: Set<String>) : PrefValue

    fun toJson(): JsonElement = when (this) {
        is B -> JsonPrimitive(v)
        is S -> JsonPrimitive(v)
        is I -> JsonObject(mapOf("i" to JsonPrimitive(v)))
        is L -> JsonObject(mapOf("l" to JsonPrimitive(v)))
        is F -> JsonObject(mapOf("f" to JsonPrimitive(v)))
        is SS -> JsonObject(mapOf("ss" to JsonArray(v.sorted().map { JsonPrimitive(it) })))
    }

    companion object {
        /** Returns null for anything we don't understand, so a newer/foreign state never crashes the app. */
        fun fromJson(e: JsonElement): PrefValue? = when (e) {
            is JsonPrimitive -> if (e.isString) S(e.content) else e.booleanOrNull?.let { B(it) }
            is JsonObject -> {
                val (k, v) = e.entries.singleOrNull() ?: return null
                when (k) {
                    "i" -> (v as? JsonPrimitive)?.longOrNull?.let { I(it.toInt()) }
                    "l" -> (v as? JsonPrimitive)?.longOrNull?.let { L(it) }
                    "f" -> (v as? JsonPrimitive)?.doubleOrNull?.let { F(it.toFloat()) }
                    "ss" -> (v as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }?.toSet()?.let { SS(it) }
                    else -> null
                }
            }
            else -> null
        }
    }
}

/** prefs file name → (key → value). */
typealias PrefsSnapshot = Map<String, Map<String, PrefValue>>
