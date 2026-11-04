package com.tobiso.tobisoappnative.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class DutyPerson(val id: Int, val name: String)

@Serializable
data class DutyPair(
    val id: Int,
    val position: Int = 0,
    val personAId: Int,
    val personAName: String,
    val personBId: Int,
    val personBName: String
) {
    val label: String get() = "$personAName a $personBName"
}

/** One Monday–Friday week. [kind] is Regular, Penalty or Off; Off has no [pair]. */
@Serializable
data class DutyWeek(
    val id: Int,
    /** ISO date (yyyy-MM-dd) of the Monday. */
    val weekStart: String,
    @Serializable(with = LenientStringSerializer::class) val kind: String = "Regular",
    val pair: DutyPair? = null
) {
    val isPenalty: Boolean get() = kind.equals("Penalty", ignoreCase = true) || kind == "1"
    val monday: LocalDate? get() = runCatching { LocalDate.parse(weekStart.take(10)) }.getOrNull()
}

/** `GET duty/overview` (204 without an approved class): this and the next week. */
@Serializable
data class DutyOverview(val thisWeek: DutyWeek? = null, val nextWeek: DutyWeek? = null)

/** `GET duty/me`: the roster name the student picked plus the whole roster. */
@Serializable
data class MyDutyPerson(val dutyPersonId: Int? = null, val people: List<DutyPerson> = emptyList())

@Serializable
data class SetMyDutyPersonRequest(val dutyPersonId: Int?)
