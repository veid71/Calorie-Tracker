package com.calorietracker.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for favorite meals
 * Handles database operations for user's favorite foods
 */
@Dao
interface FavoriteMealDao {
    
    @Query("SELECT * FROM favorite_meals ORDER BY timesUsed DESC, lastUsed DESC LIMIT :limit")
    suspend fun getTopFavorites(limit: Int = 10): List<FavoriteMeal>
    
    @Query("SELECT * FROM favorite_meals ORDER BY timesUsed DESC, lastUsed DESC LIMIT :limit")
    fun getTopFavoritesFlow(limit: Int = 10): Flow<List<FavoriteMeal>>
    
    @Query("SELECT * FROM favorite_meals WHERE category = :category ORDER BY timesUsed DESC, lastUsed DESC")
    suspend fun getFavoritesByCategory(category: String): List<FavoriteMeal>
    
    @Query("SELECT * FROM favorite_meals WHERE foodName LIKE '%' || :query || '%' ORDER BY timesUsed DESC")
    suspend fun searchFavorites(query: String): List<FavoriteMeal>
    
    @Query("SELECT * FROM favorite_meals ORDER BY lastUsed DESC LIMIT :limit")
    suspend fun getRecentFavorites(limit: Int = 5): List<FavoriteMeal>
    
    @Query("SELECT * FROM favorite_meals WHERE barcode = :barcode LIMIT 1")
    suspend fun getFavoriteByBarcode(barcode: String): FavoriteMeal?
    
    @Query("SELECT * FROM favorite_meals WHERE foodName = :foodName AND COALESCE(brand, '') = COALESCE(:brand, '') LIMIT 1")
    suspend fun getFavoriteByName(foodName: String, brand: String?): FavoriteMeal?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteMeal): Long
    
    @Update
    suspend fun updateFavorite(favorite: FavoriteMeal)
    
    @Delete
    suspend fun deleteFavorite(favorite: FavoriteMeal)
    
    @Query("DELETE FROM favorite_meals WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)
    
    @Query("UPDATE favorite_meals SET timesUsed = timesUsed + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM favorite_meals")
    suspend fun getFavoritesCount(): Int
    
    @Query("SELECT DISTINCT category FROM favorite_meals WHERE category IS NOT NULL ORDER BY category")
    suspend fun getAllCategories(): List<String>
    
    // Clean up old unused favorites (remove items not used in 90 days with usage < 3)
    @Query("DELETE FROM favorite_meals WHERE lastUsed < :cutoffTime AND timesUsed < 3")
    suspend fun cleanupUnusedFavorites(cutoffTime: Long)
}