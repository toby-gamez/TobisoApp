package com.tobiso.tobisoappnative.model

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * Turns master events (server or local) into concrete occurrences overlapping a date range.
 * Occurrences are computed from the original start (start + n × period), so monthly/yearly events
 * on the 29th–31st don't drift after a short month.
 */
object RecurrenceExpander {
    private const val MAX_OCCURRENCES = 1000

    /** True when [event] repeats with a pattern we understand. */
    fun isRepeating(event: Event): Boolean =
        event.isRecurringSafe() && normalizedPattern(event.recurrencePattern) != null

    /** Could a repeating [event] have an occurrence in [rangeStart]..[rangeEnd]? (cheap pre-filter) */
    fun mayReach(event: Event, rangeStart: Date, rangeEnd: Date): Boolean {
        if (!isRepeating(event)) return false
        if (event.getStartDateSafe().after(rangeEnd)) return false
        val until = event.recurrenceEndDate ?: return true
        return !until.before(rangeStart)
    }

    fun expand(events: List<Event>, rangeStart: Date, rangeEnd: Date): List<Event> {
        val out = ArrayList<Event>()
        for (event in events) {
            if (isRepeating(event)) out.addAll(occurrences(event, rangeStart, rangeEnd))
            else if (overlaps(event.getStartDateSafe(), event.getEndDateSafe(), rangeStart, rangeEnd)) out.add(event)
        }
        // A server that already expands occurrences must not make them show twice.
        return out.distinctBy { it.id to it.getStartDateSafe().time }
    }

    private fun overlaps(start: Date, end: Date, rangeStart: Date, rangeEnd: Date) =
        !start.after(rangeEnd) && !end.before(rangeStart)

    private fun normalizedPattern(raw: String?): String? {
        val p = raw?.trim()?.lowercase() ?: return null
        return when {
            p == "daily" || p.startsWith("freq=daily") -> "daily"
            p == "weekly" || p.startsWith("freq=weekly") -> "weekly"
            p == "monthly" || p.startsWith("freq=monthly") -> "monthly"
            p == "yearly" || p == "annually" || p.startsWith("freq=yearly") -> "yearly"
            else -> null
        }
    }

    private fun occurrences(event: Event, rangeStart: Date, rangeEnd: Date): List<Event> {
        val pattern = normalizedPattern(event.recurrencePattern) ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val start = event.getStartDateSafe()
        val duration = (event.getEndDateSafe().time - start.time).coerceAtLeast(0)
        val first = LocalDateTime.ofInstant(start.toInstant(), zone)
        val lastDay = event.recurrenceEndDate?.toInstant()?.atZone(zone)?.toLocalDate()

        // Skip straight to the first period that can reach the range instead of walking from the origin.
        val approxPeriodMs = when (pattern) {
            "daily" -> 86_400_000L
            "weekly" -> 7 * 86_400_000L
            "monthly" -> 28 * 86_400_000L
            else -> 365 * 86_400_000L
        }
        var n = ((rangeStart.time - start.time - duration) / approxPeriodMs - 1).coerceAtLeast(0)

        val result = ArrayList<Event>()
        var guard = 0
        while (guard++ < MAX_OCCURRENCES) {
            val current = when (pattern) {
                "daily" -> first.plusDays(n)
                "weekly" -> first.plusWeeks(n)
                "monthly" -> first.plusMonths(n)
                else -> first.plusYears(n)
            }
            if (lastDay != null && current.toLocalDate().isAfter(lastDay)) break
            val s = Date.from(current.atZone(zone).toInstant())
            if (s.after(rangeEnd)) break
            val e = Date(s.time + duration)
            if (!e.before(rangeStart)) result.add(event.copy(startDate = s, endDate = e))
            n++
        }
        return result
    }
}
