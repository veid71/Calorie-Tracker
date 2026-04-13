package com.calorietracker.database

import androidx.room.*

/**
 * DAO for cache metadata operations
 */
@Dao
interface CacheMetadataDao {
    
    @Query("SELECT * FROM cache_metadata ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMetadata(limit: Int): List<CacheMetadata>
    
    @Query("SELECT * FROM cache_metadata WHERE operation = :operation ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMetadataByOperation(operation: String): CacheMetadata?
    
    @Query("SELECT * FROM cache_metadata ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestCacheUpdate(): CacheMetadata?
    
    @Query("SELECT COUNT(*) FROM cache_metadata")
    suspend fun getTotalMetadataCount(): Int
    
    @Query("SELECT * FROM cache_metadata WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getMetadataInRange(startTime: Long, endTime: Long): List<CacheMetadata>
    
    @Insert
    suspend fun insertCacheMetadata(metadata: CacheMetadata)
    
    @Insert
    suspend fun insertCacheMetadataList(metadataList: List<CacheMetadata>)
    
    @Update
    suspend fun updateCacheMetadata(metadata: CacheMetadata)
    
    @Query("DELETE FROM cache_metadata WHERE id = :id")
    suspend fun deleteCacheMetadata(id: Long)
    
    @Query("DELETE FROM cache_metadata WHERE timestamp < :timestamp")
    suspend fun deleteOldMetadata(timestamp: Long)
    
    @Query("DELETE FROM cache_metadata")
    suspend fun deleteAllCacheMetadata()
    
    @Query("SELECT operation, COUNT(*) as count FROM cache_metadata GROUP BY operation")
    suspend fun getOperationCounts(): List<OperationCount>
}

/**
 * Data class for operation count results
 */
data class OperationCount(
    val operation: String,
    val count: Int
)