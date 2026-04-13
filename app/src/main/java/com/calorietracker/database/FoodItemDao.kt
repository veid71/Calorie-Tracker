package com.calorietracker.database

import androidx.room.*
import androidx.lifecycle.LiveData

@Dao
interface FoodItemDao {
    
    @Query("SELECT * FROM food_items WHERE barcode = :barcode")
    suspend fun getFoodItemByBarcode(barcode: String): FoodItem?
    
    @Query("SELECT * FROM food_items WHERE name LIKE :name ORDER BY lastUpdated DESC LIMIT :limit")
    suspend fun searchFoodsByName(name: String, limit: Int): List<FoodItem>
    
    @Query("SELECT * FROM food_items WHERE name = :name")
    suspend fun getFoodItemByName(name: String): FoodItem?
    
    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :name || '%'")
    suspend fun searchFoodItemsByName(name: String): List<FoodItem>
    
    @Query("SELECT * FROM food_items")
    fun getAllFoodItems(): LiveData<List<FoodItem>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(foodItems: List<FoodItem>)
    
    @Update
    suspend fun updateFoodItem(foodItem: FoodItem)
    
    @Delete
    suspend fun deleteFoodItem(foodItem: FoodItem)
    
    @Query("DELETE FROM food_items")
    suspend fun deleteAllFoodItems()
    
    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun getFoodItemCount(): Int
}