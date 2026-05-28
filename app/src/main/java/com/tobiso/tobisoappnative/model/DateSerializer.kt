package com.tobiso.tobisoappnative.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

object DateSerializer : KSerializer<Date> {
    private const val PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)
    private val formatter = DateTimeFormatter.ofPattern(PATTERN).withZone(ZoneId.systemDefault())
    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(formatter.format(value.toInstant()))
    }
    override fun deserialize(decoder: Decoder): Date {
        val str = decoder.decodeString()
        return try {
            Date.from(formatter.parse(str, ZonedDateTime::from).toInstant())
        } catch (_: Exception) {
            Date(0)
        }
    }
}
