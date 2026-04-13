package com.calorietracker.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.network.NetworkManager
import com.calorietracker.repository.CalorieRepository
import kotlinx.coroutines.*

class DataSyncService : Service() {

    // Single scope for all coroutines in this service; cancelled in onDestroy.
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    private lateinit var networkManager: NetworkManager
    private lateinit var repository: CalorieRepository

    override fun onCreate() {
        super.onCreate()
        networkManager = NetworkManager(this)
        repository = CalorieRepository(CalorieDatabase.getDatabase(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PRELOAD_COMMON_FOODS -> preloadCommonFoods()
            ACTION_SYNC_SCANNED_FOODS -> syncScannedFoods()
            ACTION_SYNC_BARCODE_QUEUE -> syncBarcodeQueue()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun preloadCommonFoods() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            try {
                val userGoals = repository.getNutritionGoalsSync()
                val userRegion = userGoals?.selectedRegion ?: "US"
                val commonFoods = networkManager.getCommonFoodItems(userRegion)
                for (foodItem in commonFoods) {
                    repository.addFoodItem(foodItem)
                }
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(PREF_LAST_PRELOAD, System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                Log.e(TAG, "preloadCommonFoods failed", e)
            } finally {
                stopSelf()
            }
        }
    }

    private fun syncScannedFoods() {
        if (!networkManager.isNetworkAvailable()) { stopSelf(); return }
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            try {
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(PREF_LAST_SYNC, System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                Log.e(TAG, "syncScannedFoods failed", e)
            } finally {
                stopSelf()
            }
        }
    }

    private fun syncBarcodeQueue() {
        if (!networkManager.isNetworkAvailable()) { stopSelf(); return }
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            try {
                repository.syncQueuedBarcodes()
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(PREF_LAST_QUEUE_SYNC, System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                Log.e(TAG, "syncBarcodeQueue failed", e)
            } finally {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "DataSyncService"
        const val ACTION_PRELOAD_COMMON_FOODS = "com.calorietracker.PRELOAD_COMMON_FOODS"
        const val ACTION_SYNC_SCANNED_FOODS = "com.calorietracker.SYNC_SCANNED_FOODS"
        const val ACTION_SYNC_BARCODE_QUEUE = "com.calorietracker.SYNC_BARCODE_QUEUE"

        private const val PREFS_NAME = "data_sync_prefs"
        private const val PREF_LAST_PRELOAD = "last_preload"
        private const val PREF_LAST_SYNC = "last_sync"
        private const val PREF_LAST_QUEUE_SYNC = "last_queue_sync"

        fun startPreloadService(context: Context) {
            context.startService(Intent(context, DataSyncService::class.java).apply {
                action = ACTION_PRELOAD_COMMON_FOODS
            })
        }

        fun startSyncService(context: Context) {
            context.startService(Intent(context, DataSyncService::class.java).apply {
                action = ACTION_SYNC_SCANNED_FOODS
            })
        }

        fun startBarcodeQueueSyncService(context: Context) {
            context.startService(Intent(context, DataSyncService::class.java).apply {
                action = ACTION_SYNC_BARCODE_QUEUE
            })
        }

        fun getLastPreloadTime(context: Context): Long =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(PREF_LAST_PRELOAD, 0)

        fun getLastSyncTime(context: Context): Long =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(PREF_LAST_SYNC, 0)

        fun getLastQueueSyncTime(context: Context): Long =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(PREF_LAST_QUEUE_SYNC, 0)
    }
}
