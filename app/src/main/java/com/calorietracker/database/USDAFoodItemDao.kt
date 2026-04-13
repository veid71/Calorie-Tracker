package com.calorietracker.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface USDAFoodItemDao {
    
    @Query("SELECT * FROM usda_food_items WHERE description LIKE '%' || :query || '%' ORDER BY description LIMIT :limit")
    suspend fun searchFoods(query: String, limit: Int = 20): List<USDAFoodItem>
    
    @Query("SELECT * FROM usda_food_items WHERE fdcId = :fdcId")
    suspend fun getFoodById(fdcId: Int): USDAFoodItem?
    
    @Query("SELECT * FROM usda_food_items WHERE foodCategory = :category ORDER BY description LIMIT :limit")
    suspend fun getFoodsByCategory(category: String, limit: Int = 50): List<USDAFoodItem>
    
    @Query("SELECT DISTINCT foodCategory FROM usda_food_items WHERE foodCategory IS NOT NULL ORDER BY foodCategory")
    suspend fun getAllCategories(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: USDAFoodItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<USDAFoodItem>)
    
    @Update
    suspend fun updateFood(food: USDAFoodItem)
    
    @Delete
    suspend fun deleteFood(food: USDAFoodItem)
    
    @Query("DELETE FROM usda_food_items")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM usda_food_items")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM usda_food_items")
    fun getCountFlow(): Flow<Int>
    
    // Full text search for better search performance
    @Query("""
        SELECT * FROM usda_food_items 
        WHERE description LIKE '%' || :query || '%' 
           OR brandName LIKE '%' || :query || '%'
           OR brandOwner LIKE '%' || :query || '%'
           OR ingredients LIKE '%' || :query || '%'
        ORDER BY 
            CASE 
                WHEN description LIKE :query || '%' THEN 1
                WHEN description LIKE '%' || :query || '%' THEN 2
                ELSE 3
            END,
            description
        LIMIT :limit
    """)
    suspend fun searchFoodsDetailed(query: String, limit: Int = 20): List<USDAFoodItem>
}