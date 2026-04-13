package com.calorietracker.utils

import java.util.*

/**
 * Utility class for week-based date calculations
 */
class WeekUtils {
    
    /**
     * Get all dates for a week starting from the given calendar's week
     */
    fun getWeekDates(calendar: Calendar): List<Date> {
        val dates = mutableListOf<Date>()
        val tempCalendar = Calendar.getInstance()
        tempCalendar.time = calendar.time
        
        // Set to Monday (start of week)
        tempCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        
        // Add all 7 days of the week
        for (i in 0 until 7) {
            dates.add(Date(tempCalendar.timeInMillis))
            tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return dates
    }
    
    /**
     * Get the start of the current week (Monday)
     */
    fun getCurrentWeekStart(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }
    
    /**
     * Format week identifier for database storage
     */
    fun getWeekIdentifier(calendar: Calendar): String {
        val formatter = java.text.SimpleDateFormat("yyyy-'W'ww", Locale.getDefault())
        return formatter.format(calendar.time)
    }
}
