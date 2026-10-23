package com.tobiso.tobisoappnative.repository

import com.tobiso.tobisoappnative.model.ApiService
import com.tobiso.tobisoappnative.model.AuthSession
import com.tobiso.tobisoappnative.model.Event
import com.tobiso.tobisoappnative.model.EventScope
import com.tobiso.tobisoappnative.model.OfflineDataManager
import com.tobiso.tobisoappnative.model.StudentEventRequest
import retrofit2.HttpException
import timber.log.Timber
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * Server calendar: public events for everyone, plus the signed-in student's class / group / personal
 * events via `events/mine`. Falls back to the Room cache when the server can't be reached.
 */
class CalendarRepository(
    private val api: ApiService,
    private val offline: OfflineDataManager
) {
    private val apiFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    fun formatApiDate(date: Date): String = apiFormat.format(date.toInstant().atZone(ZoneId.systemDefault()))

    private fun inRange(events: List<Event>, start: Date, end: Date) = events.filter { ev ->
        val s = ev.getStartDateSafe()
        val e = ev.getEndDateSafe()
        !s.after(end) && !e.before(start)
    }

    /** monthKey("owner", range) → time of the last successful fetch; keeps a month from being refetched for [CACHE_MINUTES]. */
    private val fetchedAt = mutableMapOf<String, Long>()

    private fun rangeKey(key: String?, start: Date, end: Date) = "${key.orEmpty()}|${start.time}|${end.time}"

    /** Server events overlapping [start]..[end]. Never throws; returns cached data (or nothing) offline. */
    suspend fun loadRange(start: Date, end: Date): List<Event> {
        val key = AuthSession.ownerKey
        val rk = rangeKey(key, start, end)
        val fresh = fetchedAt[rk]?.let { System.currentTimeMillis() - it < CACHE_MINUTES * 60_000L } == true
        if (fresh) {
            runCatching { offline.getCachedEvents(key) }.getOrNull()?.let { return inRange(it, start, end) }
        }

        val s = formatApiDate(start)
        val e = formatApiDate(end)
        val fetched: List<Event>? = try {
            if (key != null) api.getMyEvents(s, e) else api.getEventsInRange(s, e)
        } catch (ex: Exception) {
            Timber.w(ex, "Loading events failed (signed in = ${key != null})")
            // Signed in but rejected (e.g. expired token): the public calendar beats an empty one.
            if (key != null) runCatching { api.getEventsInRange(s, e) }.getOrNull() else null
        }

        if (fetched != null) {
            val valid = fetched.filter { it.startDate != null }
            try {
                val global = valid.filter { it.eventScope == null || it.eventScope == EventScope.Global }
                if (global.isNotEmpty()) offline.mergeEvents(global)
                if (key != null && AuthSession.ownerKey == key) {
                    offline.saveMyEventsInRange(key, start.time, end.time, valid)
                }
                fetchedAt[rk] = System.currentTimeMillis()
            } catch (ex: Exception) {
                Timber.w(ex, "Failed to cache events")
            }
            return valid
        }
        return try {
            inRange(offline.getCachedEvents(AuthSession.ownerKey) ?: emptyList(), start, end)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** One event by id from whatever we have cached (scoped events can't be fetched by id as a guest). */
    suspend fun cachedEvent(id: Int): Event? =
        runCatching { offline.getCachedEvents(AuthSession.ownerKey)?.find { it.id == id } }.getOrNull()

    /** Public event by id straight from the server (null when missing or not visible to us). */
    suspend fun fetchEvent(id: Int): Event? = try { api.getEvent(id) } catch (e: Exception) {
        Timber.w(e, "API fetch failed for event $id")
        null
    }

    /** Forget fetch times so the next load goes to the server (after the user changed an event). */
    fun invalidate() = fetchedAt.clear()

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block()).also { invalidate() }
    } catch (e: Exception) {
        Result.failure(mapApiError(e))
    }

    suspend fun createStudentEvent(request: StudentEventRequest): Result<Event> = call { api.createStudentEvent(request) }

    suspend fun updateStudentEvent(id: Int, request: StudentEventRequest): Result<Unit> = call {
        val r = api.updateStudentEvent(id, request)
        if (!r.isSuccessful) throw HttpException(r)
    }

    suspend fun deleteStudentEvent(id: Int): Result<Unit> = call {
        val r = api.deleteStudentEvent(id)
        if (!r.isSuccessful) throw HttpException(r)
    }

    private companion object {
        const val CACHE_MINUTES = 15
    }
}
