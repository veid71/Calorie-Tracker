package com.calorietracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles cancellation of database downloads from notification
 */
class DatabaseDownloadCancelReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "com.calorietracker.ACTION_CANCEL_DOWNLOAD"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DatabaseDownloadCancel", "Cancel download requested from notification")
        
        // Send broadcast to cancel the download
        val cancelIntent = Intent(ACTION_CANCEL_DOWNLOAD)
        context.sendBroadcast(cancelIntent)
        
        // Hide the notification
        val notificationManager = DatabaseDownloadNotificationManager(context)
        notificationManager.hideNotification()
    }
}