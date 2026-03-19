package com.tobiso.tobisoappnative.utils

import java.text.SimpleDateFormat
import java.util.*

object CalendarDateUtils {
    // Formát pro kalendářové eventy: "2025-10-28 00:00:00.000"
    private val calendarFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * Převede Date na String ve formátu kalendáře
     */
    fun formatToCalendarString(date: Date?): String? {
        return date?.let { calendarFormatter.format(it) }
    }
    
    /**
     * Převede String z kalendáře na Date
     */
    fun parseFromCalendarString(dateString: String?): Date? {
        if (dateString == null) return null
        return try {
            calendarFormatter.parse(dateString)
        } catch (e: Exception) {
            android.util.Log.w("CalendarDateUtils", "Failed to parse calendar date: $dateString", e)
            null
        }
    }
    
    /**
     * Vytvoří Date ze základních hodnot pro kalendář
     */
    fun createCalendarDate(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hour, minute, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}