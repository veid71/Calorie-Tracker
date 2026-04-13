package com.calorietracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.calorietracker.MainActivity
import com.calorietracker.R
import android.app.PendingIntent

/**
 * Broadcast receiver for handling scheduled reminder notifications
 */
class ReminderReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("reminder_id", 0)
        val title = intent.getStringExtra("title") ?: "Calorie Tracker Reminder"
        val message = intent.getStringExtra("message") ?: "Don't forget to log your progress!"
        val type = intent.getStringExtra("type") ?: "general"
        
        showNotification(context, reminderId, title, message, type)
    }
    
    private fun showNotification(
        context: Context,
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
        
        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(id, notification)
    }
}