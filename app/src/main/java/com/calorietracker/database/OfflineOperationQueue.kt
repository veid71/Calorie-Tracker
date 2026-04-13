package com.calorietracker.database

// 🧰 DATABASE TOOLS
import androidx.room.*
import androidx.lifecycle.LiveData

/**
 * 📋 OFFLINE OPERATION QUEUE - TRACK PENDING SYNC OPERATIONS
 * 
 * Hey young programmer! When users are offline, we store their actions here
 * and sync them when internet returns.
 * 
 * 🎯 What gets queued for sync?
 * - 🍎 **Food Logging**: New food entries logged while offline
 * - ⚖️ **Weight Updates**: Weight measurements taken offline
 * - 📸 **Photo Uploads**: Food photos captured offline
 * - ⭐ **Recipe Interactions**: Favorites and ratings while offline
 * - 🎯 **Goal Changes**: Nutrition goal updates made offline
 * 
 * 🔄 Sync Process:
 * 1. User performs action while offline
 * 2. Action gets stored in this queue
 * 3. When network returns, background worker processes queue
 * 4. Successful operations are marked complete
 * 5. Failed operations are retried (up to 3 times)
 * 6. Permanent failures are flagged for manual review
 */
@Entity(
    tableName = "offline_operation_queue",
    indices = [
        Index(value = ["operationType"], unique = false),
        Index(value = ["status"], unique = false),
        Index(value = ["createdAt"], unique = false),
        Index(value = ["priority"], unique = false)
    ]
)
data class OfflineOperationQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                           // 🆔 Unique operation ID
    
    // ⚙️ OPERATION DETAILS
    val operationType: String,                  // 🏷️ "LOG_FOOD", "UPDATE_WEIGHT", etc.
    val operationData: String,                  // 📦 JSON data for the operation
    val priority: Int = 1,                      // 🎯 1=High, 2=Medium, 3=Low priority
    
    // 📅 TIMING
    val createdAt: Long = System.currentTimeMillis(),  // ⏰ When operation was queued
    val scheduledFor: Long? = null,             // 📅 When to process (for delayed ops)
    val lastAttemptAt: Long? = null,            // ⏰ Last time we tried to sync
    val completedAt: Long? = null,              // ✅ When operation succeeded
    
    // 🔄 RETRY LOGIC
    val retryCount: Int = 0,                    // 🔢 How many times we've retried
    val maxRetries: Int = 3,                    // 🎯 Maximum retry attempts
    val retryDelayMs: Long = 30000,             // ⏱️ Delay between retries (30 seconds)
    
    // 📊 STATUS TRACKING
    val status: String = "pending",             // 📋 "pending", "processing", "completed", "failed"
    val errorMessage: String? = null,           // ❌ Error details if operation failed
    val requiresUserAction: Boolean = false,    // 👤 Needs user to resolve conflict?
    
    // 🔒 DEPENDENCIES
    val dependsOnOperation: Long? = null,       // 🔗 Must wait for another operation first
    val groupId: String? = null                 // 📋 Group related operations together
)

/**
 * 🗄️ OFFLINE OPERATION DAO - DATABASE ACCESS FOR SYNC QUEUE
 */
@Dao
interface OfflineOperationDao {
    
    // ➕ CREATE OPERATIONS
    @Insert
    suspend fun insertOperation(operation: OfflineOperationQueue): Long
    
    @Insert
    suspend fun insertOperations(operations: List<OfflineOperationQueue>)
    
    // 📖 READ OPERATIONS
    @Query("SELECT * FROM offline_operation_queue WHERE status = 'pending' ORDER BY priority ASC, createdAt ASC")
    suspend fun getPendingOperations(): List<OfflineOperationQueue>
    
    @Query("SELECT * FROM offline_operation_queue WHERE status = 'pending' AND priority = :priority ORDER BY createdAt ASC")
    suspend fun getPendingOperationsByPriority(priority: Int): List<OfflineOperationQueue>
    
    @Query("SELECT * FROM offline_operation_queue WHERE status = 'failed' AND retryCount < maxRetries")
    suspend fun getRetryableOperations(): List<OfflineOperationQueue>
    
    @Query("SELECT * FROM offline_operation_queue WHERE groupId = :groupId ORDER BY createdAt ASC")
    suspend fun getOperationsByGroup(groupId: String): List<OfflineOperationQueue>
    
    @Query("SELECT COUNT(*) FROM offline_operation_queue WHERE status IN ('pending', 'processing')")
    suspend fun getQueueSize(): Int
    
    @Query("SELECT COUNT(*) FROM offline_operation_queue WHERE status = 'failed'")
    suspend fun getFailedOperationCount(): Int
    
    @Query("SELECT MAX(completedAt) FROM offline_operation_queue WHERE status = 'completed'")
    suspend fun getLastSuccessfulSync(): Long?
    
    // 🔄 UPDATE OPERATIONS
    @Query("UPDATE offline_operation_queue SET status = 'processing', lastAttemptAt = :timestamp WHERE id = :operationId")
    suspend fun markOperationProcessing(operationId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE offline_operation_queue SET status = 'completed', completedAt = :timestamp WHERE id = :operationId")
    suspend fun markOperationCompleted(operationId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE offline_operation_queue SET status = 'failed', errorMessage = :error, lastAttemptAt = :timestamp WHERE id = :operationId")
    suspend fun markOperationFailed(operationId: Long, error: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE offline_operation_queue SET retryCount = retryCount + 1, lastAttemptAt = :timestamp WHERE id = :operationId")
    suspend fun incrementRetryCount(operationId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE offline_operation_queue SET lastSyncTime = :timestamp WHERE id = 1")
    suspend fun updateLastSyncTime(timestamp: Long)
    
    // 🗑️ DELETE OPERATIONS
    @Query("DELETE FROM offline_operation_queue WHERE status = 'completed' AND completedAt < :cutoffTime")
    suspend fun deleteCompletedOperations(cutoffTime: Long)
    
    @Query("DELETE FROM offline_operation_queue WHERE status = 'failed' AND retryCount >= maxRetries AND createdAt < :cutoffTime")
    suspend fun deleteFailedOperations(cutoffTime: Long)
    
    @Delete
    suspend fun deleteOperation(operation: OfflineOperationQueue)
    
    // 📊 ANALYTICS
    @Query("SELECT operationType, COUNT(*) as count FROM offline_operation_queue WHERE status = 'completed' GROUP BY operationType")
    suspend fun getSyncStatistics(): List<OperationStats>
    
    @Query("SELECT AVG(completedAt - createdAt) FROM offline_operation_queue WHERE status = 'completed' AND completedAt > :since")
    suspend fun getAverageSyncTime(since: Long): Long?
}

/**
 * 📊 OPERATION STATISTICS
 */
data class OperationStats(
    val operationType: String,
    val count: Int
)

/**
 * 🗄️ OFFLINE CACHE DAO EXTENSIONS
 * 
 * Additional methods for enhanced offline functionality.
 */
interface OfflineCacheDaoExtensions {
    
    // 🔍 ENHANCED SEARCH METHODS
    @Query("SELECT * FROM offline_foods WHERE foodName LIKE '%' || :query || '%' ORDER BY searchFrequency DESC, lastUpdated DESC LIMIT :limit")
    suspend fun searchCachedFoods(query: String, limit: Int): List<OfflineFood>
    
    @Query("SELECT * FROM offline_foods WHERE foodName = :name AND source = :source LIMIT 1")
    suspend fun getFoodByNameAndSource(name: String, source: String): OfflineFood?
    
    @Query("UPDATE offline_foods SET searchFrequency = searchFrequency + 1 WHERE id = :foodId")
    suspend fun incrementSearchFrequency(foodId: Long)
    
    // 📊 CACHE STATISTICS
    @Query("SELECT COUNT(*) FROM offline_foods")
    suspend fun getCachedFoodCount(): Int
    
    @Query("SELECT SUM(LENGTH(foodName) + LENGTH(operationData) + LENGTH(source)) FROM offline_foods")
    suspend fun getCacheSizeBytes(): Long
    
    @Query("SELECT * FROM offline_foods ORDER BY searchFrequency DESC LIMIT :limit")
    suspend fun getMostSearchedCachedFoods(limit: Int): List<OfflineFood>
    
    // 🧹 CACHE CLEANUP
    @Query("DELETE FROM offline_foods WHERE lastUpdated < :cutoffTime AND searchFrequency < 2")
    suspend fun deleteOldCachedFoods(cutoffTime: Long)
    
    @Query("DELETE FROM offline_foods WHERE searchFrequency = 0 AND lastUpdated < :cutoffTime")
    suspend fun deleteUnusedCachedFoods(cutoffTime: Long)
}