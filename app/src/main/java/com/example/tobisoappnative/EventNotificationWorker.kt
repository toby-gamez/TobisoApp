package com.example.tobisoappnative

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.tobisoappnative.model.ApiClient
import com.example.tobisoappnative.model.Event
import com.example.tobisoappnative.model.LocalEventManager
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class EventNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun doWork(): Result {
        return try {
            val notificationType = inputData.getString("notification_type") ?: "today_events"
            
            when (notificationType) {
                "tomorrow_events" -> handleTomorrowEventsNotification()
                "today_events" -> handleTodayEventsNotification()
            }
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("EventNotificationWorker", "Error in notification worker", e)
            Result.failure()
        }
    }

    private fun handleTomorrowEventsNotification() {
        runBlocking {
            try {
                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                
                android.util.Log.d("EventNotificationWorker", "=== TOMORROW NOTIFICATION CHECK ===")
                android.util.Log.d("EventNotificationWorker", "Date: ${SimpleDateFormat("yyyy-MM-dd EEEE", Locale.getDefault()).format(tomorrow.time)}")
                
                val events = getEventsForDate(tomorrow.time)
                android.util.Log.d("EventNotificationWorker", "Found ${events.size} events for tomorrow")
                
                if (events.isNotEmpty()) {
                    // Máš volno - jsou naplánované události (prázdniny/víkend)
                    val title = "Zítra máš volno! 🎉"
                    val content = when {
                        events.size == 1 -> "Máš ${events.first().getTitleSafe()}"
                        events.size <= 3 -> events.joinToString(", ") { it.getTitleSafe() }
                        else -> "${events.take(2).joinToString(", ") { it.getTitleSafe() }} a další..."
                    }
                    
                    showEventNotification(
                        notificationId = 3000,
                        channelId = "tobiso_events_tomorrow",
                        channelName = "Zítřejší volno",
                        title = title,
                        content = content,
                        isCritical = false
                    )
                } else {
                    // Zítra jdeš do školy - nejsou naplánované události
                    showEventNotification(
                        notificationId = 3000,
                        channelId = "tobiso_events_tomorrow",
                        channelName = "Zítřejší škola",
                        title = "Zítra jdeš do školy 📚",
                        content = "Připrav si věci a nezapomeň na úkoly!",
                        isCritical = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("EventNotificationWorker", "Error loading tomorrow events", e)
            }
        }
    }

    private fun handleTodayEventsNotification() {
        runBlocking {
            try {
                val today = Calendar.getInstance().time
                android.util.Log.d("EventNotificationWorker", "=== TODAY NOTIFICATION CHECK ===")
                android.util.Log.d("EventNotificationWorker", "Date: ${SimpleDateFormat("yyyy-MM-dd EEEE", Locale.getDefault()).format(today)}")
                
                val events = getEventsForDate(today)
                android.util.Log.d("EventNotificationWorker", "Found ${events.size} events for today")
                
                if (events.isNotEmpty()) {
                    // Máš volno - jsou naplánované události (prázdniny/víkend)
                    val title = "Dnes máš volno! 🎉"
                    val content = when {
                        events.size == 1 -> {
                            val event = events.first()
                            if (event.isAllDaySafe()) {
                                "${event.getTitleSafe()} (celý den)"
                            } else {
                                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(event.getStartDateSafe())
                                "${event.getTitleSafe()} v $timeStr"
                            }
                        }
                        events.size <= 3 -> {
                            events.joinToString("\n") { event ->
                                if (event.isAllDaySafe()) {
                                    "• ${event.getTitleSafe()}"
                                } else {
                                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(event.getStartDateSafe())
                                    "• ${event.getTitleSafe()} ($timeStr)"
                                }
                            }
                        }
                        else -> {
                            val firstEvents = events.take(2).joinToString("\n") { event ->
                                if (event.isAllDaySafe()) {
                                    "• ${event.getTitleSafe()}"
                                } else {
                                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(event.getStartDateSafe())
                                    "• ${event.getTitleSafe()} ($timeStr)"
                                }
                            }
                            "$firstEvents\n• a další ${events.size - 2} ${getEventCountText(events.size - 2)}..."
                        }
                    }
                    
                    showEventNotification(
                        notificationId = 3001,
                        channelId = "tobiso_events_today",
                        channelName = "Dnešní volno",
                        title = title,
                        content = content,
                        isCritical = false
                    )
                } else {
                    // Dnes jdeš do školy - nejsou naplánované události
                    showEventNotification(
                        notificationId = 3001,
                        channelId = "tobiso_events_today",
                        channelName = "Dnešní škola",
                        title = "Dnes jdeš do školy 📚",
                        content = "Hodně štěstí a užij si den ve škole!",
                        isCritical = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("EventNotificationWorker", "Error loading today events", e)
            }
        }
    }

    private suspend fun getEventsForDate(date: Date): List<Event> {
        val calendar = Calendar.getInstance().apply { time = date }
        
        // Začátek dne
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = dateFormat.format(calendar.time)
        
        // Konec dne
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = dateFormat.format(calendar.time)
        
        return try {
            val allEventsArray = ApiClient.apiService.getEventsInRange(startDate, endDate)
            val allEvents = allEventsArray.toList()
            
            // Filtruj události, které se skutečně překrývají s daným dnem
            allEvents.filter { event ->
                event.startDate != null && doesEventOverlapDay(event, date)
            }
        } catch (e: Exception) {
            android.util.Log.e("EventNotificationWorker", "Error fetching events for date $date", e)
            android.util.Log.w("EventNotificationWorker", "API failed, trying fallback logic...")
            
            // FALLBACK: Zkus použít lokální data nebo základní víkendovou logiku
            return getFallbackEventsForDate(date)
        }
    }

    private fun doesEventOverlapDay(event: Event, targetDate: Date): Boolean {
        val eventStart = event.getStartDateSafe()
        val eventEnd = event.getEndDateSafe()
        
        val targetCal = Calendar.getInstance().apply { time = targetDate }
        val targetDayStart = Calendar.getInstance().apply {
            set(targetCal.get(Calendar.YEAR), targetCal.get(Calendar.MONTH), targetCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val targetDayEnd = Calendar.getInstance().apply {
            set(targetCal.get(Calendar.YEAR), targetCal.get(Calendar.MONTH), targetCal.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
        
        // Speciální ošetření pro jednodenní události
        if (eventStart == eventEnd) {
            val eventCal = Calendar.getInstance().apply { time = eventStart }
            val eventDay = Calendar.getInstance().apply {
                set(eventCal.get(Calendar.YEAR), eventCal.get(Calendar.MONTH), eventCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            return eventDay == targetDayStart
        }
        
        // Standardní logika pro rozpětí událostí
        return (eventStart.before(targetDayEnd) || eventStart == targetDayEnd) &&
               (eventEnd.after(targetDayStart) || eventEnd == targetDayStart)
    }

    private fun getEventCountText(count: Int): String {
        return when (count) {
            1 -> "událost"
            2, 3, 4 -> "události" 
            else -> "událostí"
        }
    }

    private fun showEventNotification(
        notificationId: Int,
        channelId: String,
        channelName: String,
        title: String,
        content: String,
        isCritical: Boolean
    ) {
        createNotificationChannel(channelId, channelName, isCritical)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Přesměruj na kalendář
            putExtra("navigate_to", "calendar")
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        // Kontrola oprávnění pro notifikace
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        if (hasPermission) {
            try {
                with(NotificationManagerCompat.from(context)) {
                    notify(notificationId, builder.build())
                }
            } catch (_: SecurityException) {
                // Oprávnění nebylo uděleno, notifikace nebude zobrazena
            }
        }
    }

    /**
     * Fallback logika když API není dostupné (background worker problém)
     */
    private suspend fun getFallbackEventsForDate(date: Date): List<Event> {
        android.util.Log.d("EventNotificationWorker", "Using fallback logic for date: $date")
        
        try {
            // 1. Zkus nejdřív lokální události (místně uložené)
            val localEvents = LocalEventManager.expandRecurringEvents(
                context, 
                date, 
                Date(date.time + 24 * 60 * 60 * 1000) // +1 den
            )
            
            // Filtruj události pro konkrétní den
            val relevantLocalEvents = localEvents.filter { event ->
                doesEventOverlapDay(event, date)
            }
            
            android.util.Log.d("EventNotificationWorker", "Found ${relevantLocalEvents.size} local events")
            
            if (relevantLocalEvents.isNotEmpty()) {
                return relevantLocalEvents
            }
            
            // 2. Pokud nejsou lokální události, použij základní víkendovou logiku
            val calendar = Calendar.getInstance().apply { time = date }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            
            android.util.Log.d("EventNotificationWorker", "No local events, checking day of week: $dayOfWeek")
            
            // Pokud je víkend, vytvoř umělou událost "víkend"
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                val dayName = if (dayOfWeek == Calendar.SATURDAY) "Sobota" else "Neděle"
                android.util.Log.d("EventNotificationWorker", "Creating artificial weekend event for $dayName")
                
                // Vytvoř umělou událost pro víkend
                val weekendEvent = Event(
                    id = -999999, // Speciální ID pro fallback události
                    title = "Víkend - $dayName",
                    description = "Automaticky vytvořeno - není připojení k serveru",
                    startDate = date,
                    endDate = date,
                    isAllDay = true,
                    location = null,
                    color = "#33d17a",
                    isRecurring = false,
                    recurrencePattern = null,
                    recurrenceEndDate = null,
                    isLocal = true
                )
                
                return listOf(weekendEvent)
            }
            
            android.util.Log.d("EventNotificationWorker", "Weekday and no local events - assuming school day")
            return emptyList() // Všední den bez událostí = škola
            
        } catch (e: Exception) {
            android.util.Log.e("EventNotificationWorker", "Error in fallback logic", e)
            
            // Poslední záchrana: základní víkendová logika bez lokálních dat
            val calendar = Calendar.getInstance().apply { time = date }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                android.util.Log.w("EventNotificationWorker", "Emergency fallback: assuming weekend is free day")
                return listOf(Event(
                    id = -999998,
                    title = "Víkend",
                    description = "Nouzové řešení - chyba při načítání dat",
                    startDate = date,
                    endDate = date,
                    isAllDay = true,
                    location = null,
                    color = "#ff6b6b",
                    isRecurring = false,
                    recurrencePattern = null,
                    recurrenceEndDate = null,
                    isLocal = true
                ))
            }
            
            return emptyList()
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String, isCritical: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val descriptionText = when (channelId) {
                "tobiso_events_today" -> "Upozornění na dnešní volno nebo školu"
                "tobiso_events_tomorrow" -> "Upozornění na zítřejší volno nebo školu"
                else -> "Notifikace o školním kalendáři"
            }
            val importance = if (isCritical) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}