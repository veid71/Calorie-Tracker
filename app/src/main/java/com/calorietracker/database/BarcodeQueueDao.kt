package com.calorietracker.database

import androidx.room.*

@Dao
interface BarcodeQueueDao {
    
    @Query("SELECT * FROM barcode_queue WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedBarcodes(): List<BarcodeQueue>
    
    @Query("SELECT * FROM barcode_queue WHERE barcode = :barcode")
    suspend fun getBarcodeQueueItem(barcode: String): BarcodeQueue?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBarcodeQueue(barcodeQueue: BarcodeQueue)
    
    @Query("UPDATE barcode_queue SET synced = 1 WHERE barcode = :barcode")
    suspend fun markAsSynced(barcode: String)
    
    @Query("DELETE FROM barcode_queue WHERE synced = 1 AND timestamp < :cutoffTime")
    suspend fun deleteSyncedOlderThan(cutoffTime: Long)
    
    @Query("DELETE FROM barcode_queue")
    suspend fun deleteAll()
}