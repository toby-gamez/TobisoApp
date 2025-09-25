package com.example.tobisoappnative.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object LocalEventManager {
    private const val LOCAL_EVENTS_FILE = "local_events.json"
    private var nextLocalId = -1 // Používáme negativní ID pro místní eventy
    
    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()
    
    private fun getLocalEventsFile(context: Context): File {
        return File(context.filesDir, LOCAL_EVENTS_FILE)
    }
    
    private fun getNextLocalId(): Int {
        nextLocalId--
        return nextLocalId
    }
    
    suspend fun loadLocalEvents(context: Context): List<Event> = withContext(Dispatchers.IO) {
        try {
            val file = getLocalEventsFile(context)
            if (!file.exists()) {
                return@withContext emptyList()
            }
            
            val json = file.readText()
            if (json.isEmpty()) {
                return@withContext emptyList()
            }
            
            // Použití Array místo TypeToken pro Android 15 kompatibilitu
            val eventsArray = gson.fromJson(json, Array<Event>::class.java)
            val events: List<Event> = eventsArray?.toList() ?: emptyList()
            
            // Ujistíme se, že všechny místní eventy mají isLocal = true
            return@withContext events.map { event ->
                event.copy(isLocal = true)
            }
        } catch (e: Exception) {
            android.util.Log.e("LocalEventManager", "Error loading local events", e)
            return@withContext emptyList()
        }
    }
    
    private suspend fun saveLocalEvents(context: Context, events: List<Event>) = withContext(Dispatchers.IO) {
        try {
            val file = getLocalEventsFile(context)
            val json = gson.toJson(events)
            file.writeText(json)
        } catch (e: Exception) {
            android.util.Log.e("LocalEventManager", "Error saving local events", e)
            throw e
        }
    }
    
    suspend fun addLocalEvent(context: Context, event: Event): Event {
        val events = loadLocalEvents(context).toMutableList()
        val newEvent = event.copy(
            id = getNextLocalId(),
            isLocal = true
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
        android.util.Log.d("LocalEventManager", "Deleting event with ID: $eventId")
        val events = loadLocalEvents(context).toMutableList()
        android.util.Log.d("LocalEventManager", "Loaded ${events.size} events, looking for ID $eventId")
        
        // Debug: vypíš všechna ID
        events.forEach { event ->
            android.util.Log.d("LocalEventManager", "Found event ID: ${event.id}, title: ${event.getTitleSafe()}")
        }
        
        val removed = events.removeAll { it.id == eventId }
        android.util.Log.d("LocalEventManager", "Removal result: $removed, events after removal: ${events.size}")
        
        if (removed) {
            saveLocalEvents(context, events)
            android.util.Log.d("LocalEventManager", "Events saved successfully")
        }
        
        return removed
    }
    
    suspend fun getLocalEvent(context: Context, eventId: Int): Event? {
        val events = loadLocalEvents(context)
        return events.find { it.id == eventId }
    }
    
    suspend fun getLocalEventsInRange(context: Context, startDate: Date, endDate: Date): List<Event> {
        val allEvents = loadLocalEvents(context)
        
        return allEvents.filter { event ->
            val eventStart = event.getStartDateSafe()
            val eventEnd = event.getEndDateSafe()
            
            // Událost se překrývá s požadovaným rozsahem
            eventStart.before(endDate) && eventEnd.after(startDate) ||
            eventStart == startDate || eventStart == endDate ||
            eventEnd == startDate || eventEnd == endDate
        }
    }
    
    // Pomocná funkce pro generování eventů s opakováním
    suspend fun expandRecurringEvents(context: Context, startDate: Date, endDate: Date): List<Event> {
        val localEvents = loadLocalEvents(context)
        val expandedEvents = mutableListOf<Event>()
        
        localEvents.forEach { event ->
            if (event.isRecurringSafe()) {
                expandedEvents.addAll(generateRecurringInstances(event, startDate, endDate))
            } else {
                // Pouze přidej událost pokud se překrývá se zadaným rozsahem
                val eventStart = event.getStartDateSafe()
                val eventEnd = event.getEndDateSafe()
                
                if (eventStart.before(endDate) && eventEnd.after(startDate)) {
                    expandedEvents.add(event)
                }
            }
        }
        
        return expandedEvents
    }
    
    private fun generateRecurringInstances(event: Event, rangeStart: Date, rangeEnd: Date): List<Event> {
        val instances = mutableListOf<Event>()
        val eventStart = event.getStartDateSafe()
        val eventEnd = event.getEndDateSafe()
        val calendar = Calendar.getInstance()
        
        calendar.time = eventStart
        val duration = eventEnd.time - eventStart.time
        
        // Generuj instance na základě recurrence pattern
        while (calendar.time.before(rangeEnd) || calendar.time == rangeStart) {
            val instanceStart = calendar.time
            val instanceEnd = Date(instanceStart.time + duration)
            
            // Zkontroluj, jestli se instance překrývá s požadovaným rozsahem
            if ((instanceStart.before(rangeEnd) || instanceStart == rangeEnd) &&
                (instanceEnd.after(rangeStart) || instanceEnd == rangeStart)) {
                
                instances.add(event.copy(
                    startDate = instanceStart,
                    endDate = instanceEnd
                ))
            }
            
            // Zkontroluj recurrence end date
            if (event.recurrenceEndDate != null && instanceStart.after(event.recurrenceEndDate)) {
                break
            }
            
            // Přesuň na další instanci podle vzoru
            when (event.recurrencePattern?.lowercase()) {
                "daily" -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "monthly" -> calendar.add(Calendar.MONTH, 1)
                "yearly" -> calendar.add(Calendar.YEAR, 1)
                else -> break // Neznámý pattern, zastav generování
            }
        }
        
        return instances
    }
}