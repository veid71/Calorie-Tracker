package com.calorietracker.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for barcode scanning history
 * Manages recently scanned barcode items
 */
@Dao
interface BarcodeHistoryDao {
    
    @Query("SELECT * FROM barcode_history ORDER BY lastScanned DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 20): List<BarcodeHistory>
    
    @Query("SELECT * FROM barcode_history ORDER BY lastScanned DESC LIMIT :limit")
    fun getRecentHistoryFlow(limit: Int = 20): Flow<List<BarcodeHistory>>
    
    @Query("SELECT * FROM barcode_history WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): BarcodeHistory?
    
    @Query("SELECT * FROM barcode_history WHERE foodName LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' ORDER BY lastScanned DESC")
    suspend fun searchHistory(query: String): List<BarcodeHistory>
    
    @Query("SELECT * FROM barcode_history WHERE wasSuccessful = 1 ORDER BY timesScanned DESC, lastScanned DESC LIMIT :limit")
    suspend fun getMostScannedSuccessful(limit: Int = 10): List<BarcodeHistory>
    
    @Query("SELECT * FROM barcode_history WHERE wasSuccessful = 0 ORDER BY lastScanned DESC LIMIT :limit")
    suspend fun getFailedScans(limit: Int = 10): List<BarcodeHistory>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: BarcodeHistory): Long
    
    @Update
    suspend fun updateHistory(history: BarcodeHistory)
    
    @Delete
    suspend fun deleteHistory(history: BarcodeHistory)
    
    @Query("DELETE FROM barcode_history WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("UPDATE barcode_history SET lastScanned = :timestamp, timesScanned = timesScanned + 1 WHERE barcode = :barcode")
    suspend fun incrementScanCount(barcode: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM barcode_history")
    suspend fun getHistoryCount(): Int
    
    @Query("SELECT COUNT(*) FROM barcode_history WHERE wasSuccessful = 1")
    suspend fun getSuccessfulScanCount(): Int
    
    @Query("SELECT COUNT(*) FROM barcode_history WHERE wasSuccessful = 0")
    suspend fun getFailedScanCount(): Int
    
    // Clean up old history (keep only last 100 items or items from last 30 days)
    @Query("DELETE FROM barcode_history WHERE id NOT IN (SELECT id FROM barcode_history ORDER BY lastScanned DESC LIMIT 100)")
    suspend fun cleanupOldHistory()
    
    @Query("DELETE FROM barcode_history WHERE lastScanned < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
    
    // Analytics queries
    @Query("SELECT source, COUNT(*) as count FROM barcode_history WHERE source IS NOT NULL GROUP BY source ORDER BY count DESC")
    suspend fun getScanSourceStats(): List<ScanSourceStats>
    
    @Query("SELECT DATE(lastScanned/1000, 'unixepoch') as date, COUNT(*) as count FROM barcode_history WHERE lastScanned > :since GROUP BY date ORDER BY date DESC")
    suspend fun getDailyScanCounts(since: Long): List<DailyScanStats>
}

/**
 * Data class for scan source statistics
 */
data class ScanSourceStats(
    val source: String,
    val count: Int
)

/**
 * Data class for daily scan statistics
 */
data class DailyScanStats(
    val date: String,
    val count: Int
)