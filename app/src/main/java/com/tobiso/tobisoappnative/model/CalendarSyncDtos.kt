package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable

/** `GET calendar/sync`: the personal feed URL (a secret) and what it contains. [types] are event types: DayOff, Test, Homework, Other. */
@Serializable
data class CalendarSync(val url: String, val types: List<String> = emptyList(), val duty: Boolean = true) {
    /** The same address with the `webcal://` scheme, which calendar apps subscribe to. */
    val webcalUrl: String get() = "webcal://" + url.substringAfter("://")
}

@Serializable
data class UpdateCalendarSyncRequest(val types: List<String>, val duty: Boolean)
