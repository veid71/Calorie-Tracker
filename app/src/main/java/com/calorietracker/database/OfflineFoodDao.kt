package com.calorietracker.database

import androidx.room.*

/**
 * DAO for offline food cache operations
 */
@Dao
interface OfflineFoodDao {
    
    @Query("SELECT * FROM offline_foods WHERE barcode = :barcode")
    suspend fun getOfflineFoodByBarcode(barcode: String): OfflineFood?
    
    @Query("SELECT * FROM offline_foods WHERE name LIKE :query ORDER BY popularity DESC LIMIT :limit")
    suspend fun searchOfflineFoodsByName(query: String, limit: Int): List<OfflineFood>
    
    @Query("SELECT * FROM offline_foods WHERE brand = :brand ORDER BY popularity DESC LIMIT :limit")
    suspend fun getOfflineFoodsByBrand(brand: String, limit: Int): List<OfflineFood>
    
    @Query("SELECT * FROM offline_foods ORDER BY lastUpdated DESC LIMIT :limit")
    suspend fun getRecentOfflineFoods(limit: Int): List<OfflineFood>
    
    @Query("SELECT * FROM offline_foods ORDER BY popularity DESC LIMIT :limit")
    suspend fun getPopularOfflineFoods(limit: Int): List<OfflineFood>
    
    @Query("SELECT COUNT(*) FROM offline_foods")
    suspend fun getTotalFoodCount(): Int
    
    @Query("SELECT COUNT(*) FROM offline_foods WHERE source = :source")
    suspend fun getFoodCountBySource(source: String): Int
    
    @Query("SELECT DISTINCT source FROM offline_foods")
    suspend fun getAllSources(): List<String>
    
    @Query("SELECT DISTINCT brand FROM offline_foods WHERE brand IS NOT NULL")
    suspend fun getAllBrands(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineFood(food: OfflineFood)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineFoods(foods: List<OfflineFood>)
    
    @Update
    suspend fun updateOfflineFood(food: OfflineFood)
    
    @Query("UPDATE offline_foods SET popularity = popularity + 1 WHERE barcode = :barcode")
    suspend fun incrementPopularity(barcode: String)
    
    @Query("DELETE FROM offline_foods WHERE barcode = :barcode")
    suspend fun deleteOfflineFoodByBarcode(barcode: String)
    
    @Query("DELETE FROM offline_foods WHERE lastUpdated < :timestamp")
    suspend fun deleteExpiredFoods(timestamp: Long)
    
    @Query("DELETE FROM offline_foods WHERE barcode IN (SELECT barcode FROM offline_foods ORDER BY lastUpdated ASC LIMIT :count)")
    suspend fun deleteOldestFoods(count: Int)
    
    @Query("DELETE FROM offline_foods WHERE source = :source")
    suspend fun deleteOfflineFoodsBySource(source: String)
    
    @Query("DELETE FROM offline_foods")
    suspend fun deleteAllOfflineFoods()
    
    @Query("SELECT barcode FROM offline_foods GROUP BY name HAVING COUNT(*) > 1")
    suspend fun getDuplicateFoods(): List<String>
    
    @Query("SELECT * FROM offline_foods WHERE lastUpdated < :timestamp AND popularity < :minPopularity")
    suspend fun getStaleUnpopularFoods(timestamp: Long, minPopularity: Int): List<OfflineFood>
}