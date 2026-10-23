package com.tobiso.tobisoappnative.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/** Web `EventType`. Czech labels are what the calendar shows. */
enum class EventType(val label: String, val defaultColor: String) {
    DayOff("Volno", "#007bff"),
    Test("Test", "#d9534f"),
    Homework("Domácí úkol", "#f0ad4e"),
    Other("Jiné", "#5b8def");

    companion object {
        /** Accepts the name ("Test") or the numeric value (1) so a serializer change on the web can't break us. */
        fun parse(raw: String?): EventType? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                ?: raw?.toIntOrNull()?.let { entries.getOrNull(it) }
    }
}

/** Web `EventScope`: who sees an event. */
enum class EventScope(val label: String) {
    Global("Všichni"),
    Class("Třída"),
    Group("Skupina"),
    Personal("Osobní");

    companion object {
        fun parse(raw: String?): EventScope? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                ?: raw?.toIntOrNull()?.let { entries.getOrNull(it) }
    }
}

/** A theme (Téma) of an event: a post or a category — exactly one of the ids is set. */
@Serializable
data class EventTheme(
    val postId: Int? = null,
    val categoryId: Int? = null,
    val name: String = "",
    val postFilePath: String? = null
) {
    /** Identity of the theme regardless of display name (a post and a category can share an id). */
    val key: String get() = if (postId != null) "p$postId" else "c$categoryId"
}

/** Everything a student can pick as an event theme: categories first, then posts, each sorted by name. */
fun buildThemeOptions(categories: List<Category>, posts: List<PostSummaryResponse>): List<EventTheme> =
    categories.sortedBy { it.name.lowercase() }.map { EventTheme(categoryId = it.id, name = it.name) } +
        posts.sortedBy { it.title.lowercase() }.map { EventTheme(postId = it.id, name = it.title, postFilePath = it.filePath) }

/** Case-insensitive match on the name; blank query yields nothing so the list isn't dumped on open. */
fun filterThemeOptions(options: List<EventTheme>, query: String, exclude: Set<String>, limit: Int = 30): List<EventTheme> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    return options.asSequence().filter { it.key !in exclude && it.name.contains(q, ignoreCase = true) }.take(limit).toList()
}

/**
 * Reads a JSON string *or* number as a String. The web serializes the enums as strings,
 * but this keeps us working if one of them ever goes out as a number.
 */
object LenientStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientString", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
    override fun deserialize(decoder: Decoder): String =
        (decoder as? JsonDecoder)?.let { (it.decodeJsonElement() as? JsonPrimitive)?.content }
            ?: decoder.decodeString()
}
