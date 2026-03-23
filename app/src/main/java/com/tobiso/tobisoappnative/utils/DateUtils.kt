package com.tobiso.tobisoappnative.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val csDisplayFormatter = DateTimeFormatter.ofPattern("d. M. yyyy 'v' HH:mm", Locale.forLanguageTag("cs-CZ"))
private val csDateOnlyFormatter = DateTimeFormatter.ofPattern("d. M. yyyy", Locale.forLanguageTag("cs-CZ"))

fun parseDateToMillis(dateStr: String?): Long? {
    if (dateStr.isNullOrBlank()) return null
    // Try ISO formats with offset/Z (yyyy-MM-ddTHH:mm:ssZ or +HH:mm)
    try {
        return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
            .toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {}
    // Try ISO local datetime (handles yyyy-MM-ddTHH:mm:ss with 0-9 fractional digits, e.g. .1234567)
    try {
        return java.time.LocalDateTime.parse(dateStr)
            .atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {}
    // Try with space separator (yyyy-MM-dd HH:mm:ss)
    try {
        return java.time.LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {}
    // Try plain date
    try {
        return java.time.LocalDate.parse(dateStr)
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {}
    return null
}

fun formatDateDisplay(millis: Long?): String {
    if (millis == null) return "Neznámé datum"
    return try {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(csDisplayFormatter)
    } catch (_: Exception) {
        "Neznámé datum"
    }
}

fun formatDateOnly(millis: Long?): String {
    if (millis == null) return "Neznámé datum"
    return try {
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(csDateOnlyFormatter)
    } catch (_: Exception) {
        "Neznámé datum"
    }
}
