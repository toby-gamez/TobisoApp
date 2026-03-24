package com.tobiso.tobisoappnative.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateSerializer : KSerializer<Date> {
    private const val PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)
    private fun formatter() = SimpleDateFormat(PATTERN, Locale.US)
    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(formatter().format(value))
    }
    override fun deserialize(decoder: Decoder): Date {
        val str = decoder.decodeString()
        return formatter().parse(str) ?: Date(0)
    }
}
