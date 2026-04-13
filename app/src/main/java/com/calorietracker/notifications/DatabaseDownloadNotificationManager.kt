package com.calorietracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.calorietracker.DatabaseDownloadActivity
import com.calorietracker.R

/**
 * Manages notifications for database download progress
 * Shows real-time progress with download speed and estimated completion time
 */
class DatabaseDownloadNotificationManager(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "database_download_channel"
        private const val CHANNEL_COMPLETE_ID = "database_download_complete_channel"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_COMPLETE_ID = 1003
        private const val CANCEL_ACTION_ID = 1002
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private var startTime: Long = 0
    private var lastUpdateTime: Long = 0
    private var lastDownloadedItems: Int = 0
    
    init {
        try {
            createNotificationChannel()
        } catch (e: Exception) {
            Log.e("DatabaseDownloadNotification", "Failed to create notification channel", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val progressChannel = NotificationChannel(
                CHANNEL_ID,
                "Database Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for food database downloads"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            systemNotificationManager.createNotificationChannel(progressChannel)

            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Database Download Complete",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when a food database download finishes"
                setShowBadge(true)
            }
            systemNotificationManager.createNotificationChannel(completeChannel)
        }
    }
    
    /**
     * Show initial download notification
     */
    fun showDownloadStarted(databaseName: String) {
        try {
            startTime = System.currentTimeMillis()
            lastUpdateTime = startTime
            lastDownloadedItems = 0
            
            val notification = createBaseNotification(databaseName)
                .setContentText("Starting download...")
                .setProgress(100, 0, true)
                .build()
                
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("DatabaseDownloadNotification", "Failed to show download started notification", e)
        }
    }
    
    /**
     * Update download progress with speed calculation and ETA
     */
    fun updateProgress(
        databaseName: String,
        currentItems: Int,
        totalItems: Int,
        currentOperation: String
    ) {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - startTime
        val timeSinceLastUpdate = currentTime - lastUpdateTime
        
        // Calculate download speed (items per second)
        val itemsSinceLastUpdate = currentItems - lastDownloadedItems
        val downloadSpeed = if (timeSinceLastUpdate > 0) {
            (itemsSinceLastUpdate * 1000.0) / timeSinceLastUpdate
        } else {
            0.0
        }
        
        // Calculate ETA
        val remainingItems = totalItems - currentItems
        val etaMillis = if (downloadSpeed > 0) {
            (remainingItems / downloadSpeed * 1000).toLong()
        } else {
            0L
        }
        
        val progressPercentage = if (totalItems > 0) {
            (currentItems * 100 / totalItems).coerceAtMost(100)
        } else {
            0
        }
        
        // Format progress text
        val progressText = buildString {
            append("$currentItems / $totalItems items")
            if (downloadSpeed > 0) {
                append(" • ${String.format("%.1f", downloadSpeed)} items/sec")
            }
            if (etaMillis > 0) {
                append(" • ETA: ${formatDuration(etaMillis)}")
            }
        }
        
        val notification = createBaseNotification(databaseName)
            .setContentText(progressText)
            .setSubText(currentOperation)
            .setProgress(100, progressPercentage, false)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Update tracking variables
        lastUpdateTime = currentTime
        lastDownloadedItems = currentItems
    }
    
    /**
     * Show download completed notification (uses higher-importance channel, stays until dismissed)
     */
    fun showDownloadCompleted(databaseName: String, totalItems: Int) {
        val elapsedTime = System.currentTimeMillis() - startTime
        val avgSpeed = if (elapsedTime > 0) {
            (totalItems * 1000.0) / elapsedTime
        } else {
            0.0
        }

        val intent = Intent(context, DatabaseDownloadActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("$databaseName download complete")
            .setContentText("%,d products ready to use".format(totalItems))
            .setSubText("Took ${formatDuration(elapsedTime)} • avg ${String.format("%.0f", avgSpeed)} items/sec")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Cancel the in-progress notification, then post the completion one
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.notify(NOTIFICATION_COMPLETE_ID, notification)
    }
    
    /**
     * Show download failed notification
     */
    fun showDownloadFailed(databaseName: String, errorMessage: String) {
        val notification = createBaseNotification(databaseName)
            .setContentText("Download failed")
            .setSubText(errorMessage)
            .setProgress(0, 0, false)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Hide/cancel the download notification
     */
    fun hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
    
    private fun createBaseNotification(databaseName: String): NotificationCompat.Builder {
        // Intent to open the database download activity when tapped
        val intent = Intent(context, DatabaseDownloadActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Cancel action intent
        val cancelIntent = Intent(context, DatabaseDownloadCancelReceiver::class.java)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, CANCEL_ACTION_ID, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Downloading $databaseName")
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_back, "Cancel", cancelPendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
    }
    
    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        
        return when {
            hours > 0 -> String.format("%dh %dm", hours, minutes)
            minutes > 0 -> String.format("%dm %ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }
}