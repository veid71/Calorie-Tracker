package com.calorietracker.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.network.FoodDatabaseManager
import com.calorietracker.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for scheduled database updates
 * Automatically downloads latest food databases on a configurable schedule
 */
class DatabaseUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "DatabaseUpdateWorker"
        const val WORK_NAME = "database_update_work"
        const val UPDATE_TYPE_KEY = "update_type"
        
        // Update types
        const val UPDATE_ALL = "all"
        const val UPDATE_USDA = "usda"
        const val UPDATE_OPEN_FOOD_FACTS = "off"
        
        /**
         * Schedule periodic database updates
         * @param context Application context
         * @param intervalHours How often to update (minimum 15 minutes for testing, 1 day for production)
         * @param updateType Which databases to update (all, usda, off)
         */
        fun schedulePeriodicUpdates(context: Context, intervalHours: Long = 24, updateType: String = UPDATE_ALL) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()
            
            val inputData = Data.Builder()
                .putString(UPDATE_TYPE_KEY, updateType)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<DatabaseUpdateWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.HOURS)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, workRequest)
            
            Log.d(TAG, "Scheduled periodic database updates every $intervalHours hours for $updateType")
        }
        
        /**
         * Schedule a one-time database update
         */
        fun scheduleOneTimeUpdate(context: Context, updateType: String = UPDATE_ALL) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val inputData = Data.Builder()
                .putString(UPDATE_TYPE_KEY, updateType)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<DatabaseUpdateWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Scheduled one-time database update for $updateType")
        }
        
        /**
         * Cancel all scheduled updates
         */
        fun cancelScheduledUpdates(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled all scheduled database updates")
        }
        
        /**
         * Check if updates are currently scheduled
         */
        suspend fun areUpdatesScheduled(context: Context): Boolean {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME).get()
            return workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val updateType = inputData.getString(UPDATE_TYPE_KEY) ?: UPDATE_ALL
            Log.d(TAG, "Starting scheduled database update: $updateType")
            
            // Show notification that update is starting
            showUpdateNotification("Starting database update...", isOngoing = true)
            
            // Initialize database manager
            val database = CalorieDatabase.getDatabase(applicationContext)
            val databaseManager = FoodDatabaseManager(applicationContext, database)
            
            // Perform updates based on type
            val result = when (updateType) {
                UPDATE_USDA -> updateUSDADatabase(databaseManager)
                UPDATE_OPEN_FOOD_FACTS -> updateOpenFoodFactsDatabase(databaseManager)
                UPDATE_ALL -> updateAllDatabases(databaseManager)
                else -> {
                    Log.w(TAG, "Unknown update type: $updateType")
                    false
                }
            }
            
            if (result) {
                Log.d(TAG, "Database update completed successfully")
                showUpdateNotification("Database update completed successfully!", isOngoing = false)
                Result.success()
            } else {
                Log.e(TAG, "Database update failed")
                showUpdateNotification("Database update failed. Will retry later.", isOngoing = false)
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during scheduled database update", e)
            showUpdateNotification("Database update error: ${e.message}", isOngoing = false)
            Result.failure()
        }
    }
    
    /**
     * Update USDA database with full CSV parsing
     */
    private suspend fun updateUSDADatabase(databaseManager: FoodDatabaseManager): Boolean {
        return try {
            Log.d(TAG, "Updating USDA database...")
            val result = databaseManager.downloadUSDADatabaseFull()
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error updating USDA database", e)
            false
        }
    }
    
    /**
     * Update Open Food Facts database
     */
    private suspend fun updateOpenFoodFactsDatabase(databaseManager: FoodDatabaseManager): Boolean {
        return try {
            Log.d(TAG, "Updating Open Food Facts database...")
            val result = databaseManager.downloadOpenFoodFactsDatabase()
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Open Food Facts database", e)
            false
        }
    }
    
    /**
     * Update all databases
     */
    private suspend fun updateAllDatabases(databaseManager: FoodDatabaseManager): Boolean {
        return try {
            Log.d(TAG, "Updating all databases...")
            val result = databaseManager.downloadAllDatabases()
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error updating all databases", e)
            false
        }
    }
    
    /**
     * Show notification about update status
     */
    private suspend fun showUpdateNotification(message: String, isOngoing: Boolean) {
        try {
            NotificationHelper.showDatabaseUpdateNotification(
                context = applicationContext,
                title = "Database Update",
                message = message,
                isOngoing = isOngoing
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }
}