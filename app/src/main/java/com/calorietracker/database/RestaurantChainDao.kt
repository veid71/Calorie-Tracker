package com.calorietracker.database

// 🏗️ ROOM DATABASE TOOLS
import androidx.lifecycle.LiveData    // Auto-updating data
import androidx.room.*               // Database operations

/**
 * 🍔 RESTAURANT CHAIN DAO - FAST FOOD DATABASE MANAGER
 * 
 * Hey young programmer! This manages all the restaurant and fast food nutrition data.
 * Think of it like having a digital menu from every restaurant chain!
 * 
 * 🎯 What can we do with restaurant data?
 * - Search McDonald's menu for "Big Mac"
 * - Find all Subway sandwiches under 500 calories
 * - Get nutrition facts for Starbucks drinks
 * - Compare similar items across different chains
 */
@Dao
interface RestaurantChainDao {
    
    /**
     * 🔍 SEARCH RESTAURANT FOODS BY NAME
     * 
     * Find menu items that contain specific words.
     * Like searching for "chicken" to find all chicken items across all chains.
     */
    @Query("""
        SELECT * FROM restaurant_chains 
        WHERE itemName LIKE '%' || :query || '%' 
        AND isAvailable = 1 
        ORDER BY chainName, itemName 
        LIMIT :limit
    """)
    suspend fun searchRestaurantFoods(query: String, limit: Int = 50): List<RestaurantChain>
    
    /**
     * 🏪 GET FOODS BY RESTAURANT CHAIN
     * 
     * Get all menu items from a specific restaurant.
     * Like "Show me everything McDonald's has"
     */
    @Query("""
        SELECT * FROM restaurant_chains 
        WHERE chainName = :chainName 
        AND isAvailable = 1 
        ORDER BY category, itemName
    """)
    suspend fun getFoodsByChain(chainName: String): List<RestaurantChain>
    
    /**
     * 🍽️ GET FOODS BY CATEGORY
     * 
     * Find all items in a specific category across all restaurants.
     * Like "Show me all burger options" or "Show me all salads"
     */
    @Query("""
        SELECT * FROM restaurant_chains 
        WHERE category = :category 
        AND isAvailable = 1 
        ORDER BY chainName, calories ASC
    """)
    suspend fun getFoodsByCategory(category: String): List<RestaurantChain>
    
    /**
     * 🔥 FIND LOW CALORIE OPTIONS
     * 
     * Find menu items under a certain calorie limit.
     * Great for users trying to find healthy fast food options!
     */
    @Query("""
        SELECT * FROM restaurant_chains 
        WHERE calories <= :maxCalories 
        AND isAvailable = 1 
        ORDER BY calories ASC 
        LIMIT :limit
    """)
    suspend fun getLowCalorieOptions(maxCalories: Int, limit: Int = 20): List<RestaurantChain>
    
    /**
     * 💪 FIND HIGH PROTEIN OPTIONS
     * 
     * Find menu items with good protein content.
     * Perfect for fitness enthusiasts and people trying to build muscle!
     */
    @Query("""
        SELECT * FROM restaurant_chains 
        WHERE protein IS NOT NULL 
        AND protein >= :minProtein 
        AND isAvailable = 1 
        ORDER BY protein DESC 
        LIMIT :limit
    """)
    suspend fun getHighProteinOptions(minProtein: Double, limit: Int = 20): List<RestaurantChain>
    
    /**
     * 🏪 GET ALL RESTAURANT CHAINS
     * 
     * Get list of all restaurant chains we have data for.
     * Useful for showing user which restaurants are supported.
     */
    @Query("SELECT DISTINCT chainName FROM restaurant_chains ORDER BY chainName")
    suspend fun getAllChainNames(): List<String>
    
    /**
     * 📊 GET CHAIN STATISTICS
     * 
     * How many menu items do we have for each restaurant?
     */
    @Query("""
        SELECT chainName, COUNT(*) as itemCount 
        FROM restaurant_chains 
        WHERE isAvailable = 1 
        GROUP BY chainName 
        ORDER BY itemCount DESC
    """)
    suspend fun getChainStatistics(): List<ChainStats>
    
    /**
     * 💾 SAVE RESTAURANT FOODS
     * 
     * Add new restaurant menu items to our database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestaurantFoods(foods: List<RestaurantChain>)
    
    /**
     * 🔢 COUNT TOTAL RESTAURANT ITEMS
     * 
     * How many restaurant menu items do we have in total?
     */
    @Query("SELECT COUNT(*) FROM restaurant_chains WHERE isAvailable = 1")
    suspend fun getTotalRestaurantItemCount(): Int
}

/**
 * 📊 CHAIN STATISTICS DATA CLASS
 * 
 * Simple container for restaurant chain statistics.
 */
data class ChainStats(
    val chainName: String,  // 🏪 Restaurant name
    val itemCount: Int      // 🔢 Number of menu items we have
)

/**
 * 📸 MEAL PHOTO DAO - VISUAL FOOD DIARY MANAGER
 * 
 * This manages all the meal photos users take for visual food tracking.
 */
@Dao
interface MealPhotoDao {
    
    /**
     * 📸 SAVE A NEW MEAL PHOTO
     * 
     * When user takes a photo of their meal, save it with metadata.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPhoto(photo: MealPhoto): Long
    
    /**
     * 📅 GET PHOTOS FOR A SPECIFIC DATE
     * 
     * Show all meal photos from a particular day.
     */
    @Query("SELECT * FROM meal_photos WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getPhotosForDate(date: String): List<MealPhoto>
    
    /**
     * 🍽️ GET PHOTOS BY MEAL TYPE
     * 
     * Find all breakfast photos, lunch photos, etc.
     */
    @Query("SELECT * FROM meal_photos WHERE mealType = :mealType ORDER BY date DESC LIMIT :limit")
    suspend fun getPhotosByMealType(mealType: String, limit: Int = 50): List<MealPhoto>
    
    /**
     * 📊 GET RECENT MEAL PHOTOS
     * 
     * Get the most recent meal photos for the photo timeline.
     */
    @Query("SELECT * FROM meal_photos ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPhotos(limit: Int = 20): LiveData<List<MealPhoto>>
    
    /**
     * ⭐ GET HIGHLY RATED MEALS
     * 
     * Find meals user rated 4-5 stars for inspiration.
     */
    @Query("SELECT * FROM meal_photos WHERE rating >= 4 ORDER BY rating DESC, timestamp DESC")
    suspend fun getHighlyRatedMeals(): List<MealPhoto>
    
    /**
     * 🗑️ DELETE OLD PHOTOS
     * 
     * Clean up photos older than specified days to save storage space.
     */
    @Query("DELETE FROM meal_photos WHERE date < :cutoffDate")
    suspend fun deletePhotosOlderThan(cutoffDate: String)
    
    /**
     * 🔢 COUNT PHOTOS BY MEAL TYPE
     * 
     * Statistics about user's photo-taking habits.
     */
    @Query("SELECT mealType, COUNT(*) as count FROM meal_photos GROUP BY mealType")
    suspend fun getPhotoCountsByMealType(): List<MealTypePhotoCount>
}

/**
 * 📊 MEAL TYPE PHOTO COUNT
 * 
 * Statistics about photos per meal type.
 */
data class MealTypePhotoCount(
    val mealType: String,  // 🍽️ Meal type
    val count: Int         // 📸 Number of photos
)