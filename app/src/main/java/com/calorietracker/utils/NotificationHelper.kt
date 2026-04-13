package com.calorietracker.utils

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

/**
 * Helper class for managing app notifications
 */
object NotificationHelper {
    
    private const val CHANNEL_DATABASE_UPDATES = "database_updates"
    private const val CHANNEL_SYNC_STATUS = "sync_status"
    
    private const val NOTIFICATION_ID_DATABASE_UPDATE = 1001
    private const val NOTIFICATION_ID_SYNC_STATUS = 1002
    
    /**
     * Initialize notification channels (call from Application class)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Database updates channel
            val databaseChannel = NotificationChannel(
                CHANNEL_DATABASE_UPDATES,
                "Database Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for food database updates"
                setShowBadge(false)
            }
            
            // Sync status channel
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC_STATUS,
                "Sync Status",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Status notifications for data synchronization"
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannels(listOf(databaseChannel, syncChannel))
        }
    }
    
    /**
     * Show notification for database update progress/status
     */
    fun showDatabaseUpdateNotification(
        context: Context,
        title: String,
        message: String,
        isOngoing: Boolean = false,
        progress: Int? = null,
        maxProgress: Int = 100
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val builder = NotificationCompat.Builder(context, CHANNEL_DATABASE_UPDATES)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setOngoing(isOngoing)
                .setAutoCancel(!isOngoing)
                .setPriority(NotificationCompat.PRIORITY_LOW)
            
            // Add progress bar if progress is specified
            if (progress != null) {
                builder.setProgress(maxProgress, progress, false)
            } else if (isOngoing) {
                builder.setProgress(0, 0, true) // Indeterminate progress
            }
            
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_DATABASE_UPDATE, builder.build())
                
        } catch (e: Exception) {
            // Notification permission might be denied, handle silently
            android.util.Log.w("NotificationHelper", "Could not show database update notification", e)
        }
    }
    
    /**
     * Show notification for sync status (Health Connect, etc.)
     */
    fun showSyncStatusNotification(
        context: Context,
        title: String,
        message: String,
        isError: Boolean = false
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val builder = NotificationCompat.Builder(context, CHANNEL_SYNC_STATUS)
                .setSmallIcon(if (isError) R.drawable.ic_notification else R.drawable.ic_download)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(if (isError) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_SYNC_STATUS, builder.build())
                
        } catch (e: Exception) {
            android.util.Log.w("NotificationHelper", "Could not show sync status notification", e)
        }
    }
    
    /**
     * Cancel database update notification
     */
    fun cancelDatabaseUpdateNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_DATABASE_UPDATE)
    }
    
    /**
     * Cancel sync status notification
     */
    fun cancelSyncStatusNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SYNC_STATUS)
    }
    
    /**
     * Cancel all notifications from this app
     */
    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}