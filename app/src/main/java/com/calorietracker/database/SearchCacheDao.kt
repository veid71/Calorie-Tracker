package com.calorietracker.database

import androidx.room.*

/**
 * DAO for search cache operations
 */
@Dao
interface SearchCacheDao {
    
    @Query("SELECT * FROM search_cache WHERE query = :query")
    suspend fun getSearchCache(query: String): SearchCache?
    
    @Query("SELECT * FROM search_cache WHERE query LIKE :pattern ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getSimilarSearches(pattern: String, limit: Int): List<SearchCache>
    
    @Query("SELECT * FROM search_cache ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSearches(limit: Int): List<SearchCache>
    
    @Query("SELECT COUNT(*) FROM search_cache")
    suspend fun getTotalSearchCount(): Int
    
    @Query("SELECT query FROM search_cache ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentQueries(limit: Int): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchCache(searchCache: SearchCache)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchCaches(searchCaches: List<SearchCache>)
    
    @Update
    suspend fun updateSearchCache(searchCache: SearchCache)
    
    @Query("DELETE FROM search_cache WHERE query = :query")
    suspend fun deleteSearchCache(query: String)
    
    @Query("DELETE FROM search_cache WHERE timestamp < :timestamp")
    suspend fun deleteExpiredSearchCache(timestamp: Long)
    
    @Query("DELETE FROM search_cache")
    suspend fun deleteAllSearchCache()
    
    @Query("SELECT * FROM search_cache WHERE resultBarcodes NOT LIKE '%,%' OR resultBarcodes = ''")
    suspend fun getOrphanedSearches(): List<SearchCache>
    
    @Query("DELETE FROM search_cache WHERE query IN (SELECT query FROM search_cache ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestSearches(count: Int)
}