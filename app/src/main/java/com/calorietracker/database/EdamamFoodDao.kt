package com.calorietracker.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Edamam food items
 */
@Dao
interface EdamamFoodDao {
    
    @Query("SELECT * FROM edamam_foods WHERE label LIKE '%' || :query || '%' OR knownAs LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' ORDER BY label ASC LIMIT :limit")
    suspend fun searchFoods(query: String, limit: Int = 50): List<EdamamFoodItem>
    
    @Query("SELECT * FROM edamam_foods WHERE label LIKE '%' || :query || '%' OR knownAs LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' ORDER BY label ASC LIMIT :limit")
    fun searchFoodsFlow(query: String, limit: Int = 50): Flow<List<EdamamFoodItem>>
    
    @Query("SELECT * FROM edamam_foods WHERE foodId = :foodId")
    suspend fun getFoodById(foodId: String): EdamamFoodItem?
    
    @Query("SELECT * FROM edamam_foods ORDER BY label ASC LIMIT :limit OFFSET :offset")
    suspend fun getFoods(limit: Int = 50, offset: Int = 0): List<EdamamFoodItem>
    
    @Query("SELECT COUNT(*) FROM edamam_foods")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM edamam_foods")
    fun getCountFlow(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: EdamamFoodItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<EdamamFoodItem>)
    
    @Update
    suspend fun updateFood(food: EdamamFoodItem)
    
    @Delete
    suspend fun deleteFood(food: EdamamFoodItem)
    
    @Query("DELETE FROM edamam_foods")
    suspend fun deleteAllFoods()
    
    @Query("SELECT * FROM edamam_foods WHERE category = :category ORDER BY label ASC LIMIT :limit")
    suspend fun getFoodsByCategory(category: String, limit: Int = 50): List<EdamamFoodItem>
    
    @Query("SELECT * FROM edamam_foods WHERE brand = :brand ORDER BY label ASC LIMIT :limit")
    suspend fun getFoodsByBrand(brand: String, limit: Int = 50): List<EdamamFoodItem>
    
    @Query("SELECT DISTINCT category FROM edamam_foods WHERE category IS NOT NULL ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>
    
    @Query("SELECT DISTINCT brand FROM edamam_foods WHERE brand IS NOT NULL ORDER BY brand ASC")
    suspend fun getAllBrands(): List<String>
    
    @Query("SELECT * FROM edamam_foods WHERE calories BETWEEN :minCalories AND :maxCalories ORDER BY label ASC LIMIT :limit")
    suspend fun getFoodsByCalorieRange(minCalories: Double, maxCalories: Double, limit: Int = 50): List<EdamamFoodItem>
    
    @Query("SELECT * FROM edamam_foods WHERE protein >= :minProtein ORDER BY protein DESC LIMIT :limit")
    suspend fun getHighProteinFoods(minProtein: Double, limit: Int = 50): List<EdamamFoodItem>
    
    @Query("SELECT * FROM edamam_foods WHERE fiber >= :minFiber ORDER BY fiber DESC LIMIT :limit")
    suspend fun getHighFiberFoods(minFiber: Double, limit: Int = 50): List<EdamamFoodItem>
}