package com.calorietracker.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.calorietracker.DatabaseDownloadActivity
import com.calorietracker.R
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.network.FoodDatabaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service for OFFs database downloads.
 *
 * Android cannot freeze a process that has an active foreground service, so this
 * ensures the download continues even with the screen off and the app in the background.
 *
 * The service shows a persistent "Downloading…" notification while running and stops
 * itself when the download finishes or fails.
 */
class DatabaseDownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var databaseManager: FoodDatabaseManager

    override fun onCreate() {
        super.onCreate()
        databaseManager = FoodDatabaseManager(applicationContext, CalorieDatabase.getDatabase(this))
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START     -> startDownload()
            ACTION_START_ALL -> startDownloadAll()
            ACTION_STOP      -> stopSelf()
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // ── Download ─────────────────────────────────────────────────────────────

    private fun startDownload() {
        startForeground(NOTIFICATION_ID, buildNotification("Starting Open Food Facts download…"))
        Log.d(TAG, "Foreground service started (OFFs only) — process will not be frozen")

        serviceScope.launch {
            try {
                val result = databaseManager.downloadOpenFoodFactsDatabaseOnly()
                if (result.isSuccess) {
                    Log.d(TAG, "OFFs download completed successfully")
                } else {
                    Log.w(TAG, "OFFs download finished with failure: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "OFFs download exception: ${e.message}", e)
            } finally {
                stopSelf()
            }
        }
    }

    private fun startDownloadAll() {
        startForeground(NOTIFICATION_ID, buildNotification("Starting full database download…"))
        Log.d(TAG, "Foreground service started (all databases) — process will not be frozen")

        serviceScope.launch {
            try {
                val result = databaseManager.downloadAllDatabases()
                if (result.isSuccess) {
                    Log.d(TAG, "Full download completed successfully")
                } else {
                    Log.w(TAG, "Full download finished with failure: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Full download exception: ${e.message}", e)
            } finally {
                stopSelf()
            }
        }
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Database Download Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the download running while the screen is off"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val tapIntent = Intent(this, DatabaseDownloadActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Food database download")
            .setContentText(text)
            .setContentIntent(tapPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "DBDownloadService"
        private const val CHANNEL_ID = "db_download_fg_channel"
        private const val NOTIFICATION_ID = 1010
        const val ACTION_START     = "com.calorietracker.ACTION_START_DOWNLOAD"
        const val ACTION_START_ALL = "com.calorietracker.ACTION_START_ALL_DOWNLOAD"
        const val ACTION_STOP      = "com.calorietracker.ACTION_STOP_DOWNLOAD"

        fun start(context: Context) {
            startWithAction(context, ACTION_START)
        }

        fun startAll(context: Context) {
            startWithAction(context, ACTION_START_ALL)
        }

        private fun startWithAction(context: Context, action: String) {
            val intent = Intent(context, DatabaseDownloadForegroundService::class.java).apply {
                this.action = action
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, DatabaseDownloadForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
            )
        }
    }
}
