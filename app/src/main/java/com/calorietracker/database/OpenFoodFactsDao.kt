package com.calorietracker.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OpenFoodFactsDao {
    
    @Query("SELECT * FROM openfoodfacts_items WHERE productName LIKE '%' || :query || '%' ORDER BY productName LIMIT :limit")
    suspend fun searchFoods(query: String, limit: Int = 20): List<OpenFoodFactsItem>
    
    @Query("SELECT * FROM openfoodfacts_items WHERE barcode = :barcode")
    suspend fun getFoodByBarcode(barcode: String): OpenFoodFactsItem?
    
    @Query("SELECT * FROM openfoodfacts_items WHERE id = :id")
    suspend fun getFoodById(id: String): OpenFoodFactsItem?
    
    @Query("SELECT * FROM openfoodfacts_items WHERE brands LIKE '%' || :brand || '%' ORDER BY productName LIMIT :limit")
    suspend fun getFoodsByBrand(brand: String, limit: Int = 50): List<OpenFoodFactsItem>
    
    @Query("SELECT * FROM openfoodfacts_items WHERE categories LIKE '%' || :category || '%' ORDER BY productName LIMIT :limit")
    suspend fun getFoodsByCategory(category: String, limit: Int = 50): List<OpenFoodFactsItem>
    
    @Query("SELECT DISTINCT brands FROM openfoodfacts_items WHERE brands IS NOT NULL ORDER BY brands")
    suspend fun getAllBrands(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: OpenFoodFactsItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<OpenFoodFactsItem>)
    
    @Update
    suspend fun updateFood(food: OpenFoodFactsItem)
    
    @Delete
    suspend fun deleteFood(food: OpenFoodFactsItem)
    
    @Query("DELETE FROM openfoodfacts_items")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM openfoodfacts_items")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM openfoodfacts_items")
    fun getCountFlow(): Flow<Int>
    
    // Advanced search with ranking
    @Query("""
        SELECT * FROM openfoodfacts_items 
        WHERE productName LIKE '%' || :query || '%' 
           OR brands LIKE '%' || :query || '%'
           OR categories LIKE '%' || :query || '%'
           OR barcode = :query
        ORDER BY 
            CASE 
                WHEN barcode = :query THEN 1
                WHEN productName LIKE :query || '%' THEN 2
                WHEN productName LIKE '%' || :query || '%' THEN 3
                ELSE 4
            END,
            completeness DESC,
            productName
        LIMIT :limit
    """)
    suspend fun searchFoodsDetailed(query: String, limit: Int = 20): List<OpenFoodFactsItem>
}