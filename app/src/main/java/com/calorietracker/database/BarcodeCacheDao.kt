package com.calorietracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for managing cached barcode scan results
 */
@Dao
interface BarcodeCacheDao {
    
    /**
     * Get cached food item by barcode
     * Updates last accessed time when found
     */
    @Query("SELECT * FROM barcode_cache WHERE barcode = :barcode")
    suspend fun getCachedItem(barcode: String): BarcodeCache?
    
    /**
     * Cache a new barcode scan result
     * Replaces existing entries with the same barcode
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheItem(item: BarcodeCache)
    
    /**
     * Update last accessed time for a cached item
     */
    @Query("UPDATE barcode_cache SET lastAccessed = :timestamp WHERE barcode = :barcode")
    suspend fun updateLastAccessed(barcode: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Get all cached items ordered by last accessed (most recent first)
     */
    @Query("SELECT * FROM barcode_cache ORDER BY lastAccessed DESC")
    suspend fun getAllCachedItems(): List<BarcodeCache>
    
    /**
     * Delete old cached items (older than specified days)
     * This helps manage cache size and keeps only frequently used items
     */
    @Query("DELETE FROM barcode_cache WHERE cachedAt < :cutoffTime")
    suspend fun deleteOldCachedItems(cutoffTime: Long)
    
    /**
     * Get cache statistics for debugging
     */
    @Query("SELECT COUNT(*) as total, cacheSource, MIN(cachedAt) as oldest, MAX(cachedAt) as newest FROM barcode_cache GROUP BY cacheSource")
    suspend fun getCacheStats(): List<CacheStats>
    
    /**
     * Clear all cached items (for debugging or reset)
     */
    @Query("DELETE FROM barcode_cache")
    suspend fun clearCache()
    
    /**
     * Insert barcode cache (alternative method name for compatibility)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcodeCache(item: BarcodeCache)
    
    /**
     * Get barcode cache by barcode (alternative method name for compatibility)
     */
    @Query("SELECT * FROM barcode_cache WHERE barcode = :barcode")
    suspend fun getBarcodeCache(barcode: String): BarcodeCache?
    
    /**
     * Get total barcode count
     */
    @Query("SELECT COUNT(*) FROM barcode_cache")
    suspend fun getTotalBarcodeCount(): Int
    
    /**
     * Delete all barcode cache
     */
    @Query("DELETE FROM barcode_cache")
    suspend fun deleteAllBarcodeCache()
    
    /**
     * Delete expired barcode cache
     */
    @Query("DELETE FROM barcode_cache WHERE cachedAt < :timestamp")
    suspend fun deleteExpiredBarcodeCache(timestamp: Long)
    
    /**
     * Search cached items by name
     */
    @Query("SELECT * FROM barcode_cache WHERE name LIKE :query ORDER BY lastAccessed DESC LIMIT :limit")
    suspend fun searchByName(query: String, limit: Int): List<BarcodeCache>
}

/**
 * Data class for cache statistics
 */
data class CacheStats(
    val total: Int,
    val cacheSource: String,
    val oldest: Long,
    val newest: Long
)