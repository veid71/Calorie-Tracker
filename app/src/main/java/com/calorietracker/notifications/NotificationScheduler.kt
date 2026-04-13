package com.calorietracker.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.calorietracker.MainActivity
import com.calorietracker.R
import com.calorietracker.database.ReminderNotification
import java.util.*

/**
 * Handles scheduling and managing notification reminders
 */
class NotificationScheduler(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "calorie_tracker_reminders"
        const val CHANNEL_NAME = "Calorie Tracker Reminders"
        const val CHANNEL_DESCRIPTION = "Reminders for logging food, weight, and water intake"
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Schedule a reminder notification
     */
    fun scheduleReminder(reminder: ReminderNotification) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("title", reminder.title)
            putExtra("message", reminder.message)
            putExtra("type", reminder.type)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Schedule for each day of the week
        val daysOfWeek = reminder.daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }
        
        for (dayOfWeek in daysOfWeek) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, reminder.hour)
                set(Calendar.MINUTE, reminder.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If the time has passed today, schedule for next week
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
    
    /**
     * Cancel a scheduled reminder
     */
    fun cancelReminder(reminderId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
    
    /**
     * Schedule all active reminders
     */
    fun scheduleAllReminders(reminders: List<ReminderNotification>) {
        reminders.filter { it.isEnabled }.forEach { reminder ->
            scheduleReminder(reminder)
        }
    }
    
    /**
     * Cancel all scheduled reminders
     */
    fun cancelAllReminders(reminders: List<ReminderNotification>) {
        reminders.forEach { reminder ->
            cancelReminder(reminder.id)
        }
    }
    
    /**
     * Show a notification immediately
     */
    fun showNotification(
        id: Int,
        title: String,
        message: String,
        type: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val icon = when (type) {
            "weight" -> R.drawable.ic_scale
            "water" -> R.drawable.ic_water_drop
            "breakfast", "lunch", "dinner", "snack" -> R.drawable.ic_add
            else -> R.drawable.ic_notification
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        notificationManager.notify(id, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Get default reminder templates
     */
    fun getDefaultReminders(): List<ReminderNotification> {
        return listOf(
            ReminderNotification(
                type = "breakfast",
                title = "Log Your Breakfast",
                message = "Don't forget to log your breakfast calories!",
                hour = 8,
                minute = 0,
                daysOfWeek = "2,3,4,5,6,7,1" // Monday to Sunday
            ),
            ReminderNotification(
                type = "lunch", 
                title = "Log Your Lunch",
                message = "Time to log your lunch!",
                hour = 12,
                minute = 30,
                daysOfWeek = "2,3,4,5,6,7,1"
            ),
            ReminderNotification(
                type = "dinner",
                title = "Log Your Dinner", 
                message = "Remember to log your dinner calories!",
                hour = 19,
                minute = 0,
                daysOfWeek = "2,3,4,5,6,7,1"
            ),
            ReminderNotification(
                type = "weight",
                title = "Weigh Yourself",
                message = "Track your daily weight progress!",
                hour = 7,
                minute = 0,
                daysOfWeek = "2,3,4,5,6,7,1"
            ),
            ReminderNotification(
                type = "water",
                title = "Drink Water",
                message = "Stay hydrated! Log your water intake.",
                hour = 14,
                minute = 0,
                daysOfWeek = "2,3,4,5,6,7,1"
            )
        )
    }
}