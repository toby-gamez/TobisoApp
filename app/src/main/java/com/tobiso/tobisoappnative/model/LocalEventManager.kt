package com.tobiso.tobisoappnative.model
import timber.log.Timber

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

object LocalEventManager {
    private const val LOCAL_EVENTS_FILE = "local_events.json"

    private val json = Json { ignoreUnknownKeys = true }

    private fun getLocalEventsFile(context: Context): File {
        return File(context.filesDir, LOCAL_EVENTS_FILE)
    }

    // Derives the next ID from existing events so it never conflicts after process death.
    private fun getNextLocalId(existingEvents: List<Event>): Int {
        val minExisting = existingEvents.minOfOrNull { it.id } ?: 0
        return minOf(minExisting, 0) - 1
    }
    
    suspend fun loadLocalEvents(context: Context): List<Event> = withContext(Dispatchers.IO) {
        try {
            val file = getLocalEventsFile(context)
            if (!file.exists()) {
                return@withContext emptyList()
            }
            
            val jsonString = file.readText()
            if (jsonString.isEmpty()) {
                return@withContext emptyList()
            }
            
            // Použití Array místo TypeToken pro Android 15 kompatibilitu (kotlinx.serialization)
            val events: List<Event> = try {
                json.decodeFromString<List<Event>>(jsonString)
            } catch (_: Exception) {
                emptyList()
            }
            
            // Ujistíme se, že všechny místní eventy mají isLocal = true
            return@withContext events.map { event ->
                event.copy(isLocal = true)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading local events")
            return@withContext emptyList()
        }
    }
    
    private suspend fun saveLocalEvents(context: Context, events: List<Event>) = withContext(Dispatchers.IO) {
        try {
            val file = getLocalEventsFile(context)
            val jsonOut = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Event.serializer()), events)
            file.writeText(jsonOut)
        } catch (e: Exception) {
            Timber.e(e, "Error saving local events")
            throw e
        }
    }
    
    suspend fun addLocalEvent(context: Context, event: Event): Event {
        val events = loadLocalEvents(context).toMutableList()
        val newEvent = event.copy(
            id = getNextLocalId(events),
            isLocal = true,
        )
        events.add(newEvent)
        saveLocalEvents(context, events)
        return newEvent
    }
    
    suspend fun updateLocalEvent(context: Context, updatedEvent: Event): Event? {
        val events = loadLocalEvents(context).toMutableList()
        val index = events.indexOfFirst { it.id == updatedEvent.id }
        
        return if (index >= 0) {
            val event = updatedEvent.copy(isLocal = true)
            events[index] = event
            saveLocalEvents(context, events)
            event
        } else {
            null
        }
    }
    
    suspend fun deleteLocalEvent(context: Context, eventId: Int): Boolean {
        Timber.d("Deleting event with ID: $eventId")
        val events = loadLocalEvents(context).toMutableList()
        Timber.d("Loaded ${events.size} events, looking for ID $eventId")
        
        // Debug: vypíš všechna ID
        events.forEach { event ->
            Timber.d("Found event ID: ${event.id}, title: ${event.getTitleSafe()}")
        }
        
        val removed = events.removeAll { it.id == eventId }
        Timber.d("Removal result: $removed, events after removal: ${events.size}")
        
        if (removed) {
            saveLocalEvents(context, events)
            Timber.d("Events saved successfully")
        }
        
        return removed
    }
    
    suspend fun getLocalEvent(context: Context, eventId: Int): Event? {
        val events = loadLocalEvents(context)
        return events.find { it.id == eventId }
    }
    
    // Local events (recurring ones expanded) overlapping the range
    suspend fun expandRecurringEvents(context: Context, startDate: Date, endDate: Date): List<Event> =
        RecurrenceExpander.expand(loadLocalEvents(context), startDate, endDate)
}
