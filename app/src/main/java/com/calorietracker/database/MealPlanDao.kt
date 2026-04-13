package com.calorietracker.database

import androidx.room.*
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for meal planning operations
 * Provides database operations for weekly meal planning
 */
@Dao
interface MealPlanDao {
    
    @Query("SELECT * FROM meal_plans WHERE date = :date ORDER BY mealType, sortOrder")
    suspend fun getMealPlansForDate(date: String): List<MealPlan>
    
    @Query("SELECT * FROM meal_plans WHERE date = :date ORDER BY mealType, sortOrder")
    fun getMealPlansForDateFlow(date: String): Flow<List<MealPlan>>
    
    @Query("SELECT * FROM meal_plans WHERE date BETWEEN :startDate AND :endDate ORDER BY date, mealType, sortOrder")
    suspend fun getMealPlansForDateRange(startDate: String, endDate: String): List<MealPlan>
    
    @Query("SELECT * FROM meal_plans WHERE date BETWEEN :startDate AND :endDate ORDER BY date, mealType, sortOrder")
    fun getMealPlansForDateRangeFlow(startDate: String, endDate: String): Flow<List<MealPlan>>
    
    @Query("SELECT * FROM meal_plans WHERE planWeek = :week ORDER BY date, mealType, sortOrder")
    suspend fun getMealPlansForWeek(week: String): List<MealPlan>
    
    @Query("SELECT * FROM meal_plans WHERE planWeek = :week ORDER BY date, mealType, sortOrder")
    fun getMealPlansForWeekFlow(week: String): Flow<List<MealPlan>>
    
    @Query("SELECT * FROM meal_plans WHERE date = :date AND mealType = :mealType ORDER BY sortOrder")
    suspend fun getMealPlansForDateAndType(date: String, mealType: String): List<MealPlan>
    
    @Query("SELECT * FROM meal_plans WHERE mealName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun searchMealPlans(query: String): List<MealPlan>
    
    @Query("SELECT * FROM meal_plans WHERE id = :id")
    suspend fun getMealPlanById(id: Long): MealPlan?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlan): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlans(mealPlans: List<MealPlan>)
    
    @Update
    suspend fun updateMealPlan(mealPlan: MealPlan)
    
    @Delete
    suspend fun deleteMealPlan(mealPlan: MealPlan)
    
    @Query("DELETE FROM meal_plans WHERE id = :id")
    suspend fun deleteMealPlanById(id: Long)
    
    @Query("DELETE FROM meal_plans WHERE date = :date")
    suspend fun deleteMealPlansForDate(date: String)
    
    @Query("DELETE FROM meal_plans WHERE date BETWEEN :startDate AND :endDate")
    suspend fun deleteMealPlansForDateRange(startDate: String, endDate: String)
    
    // Completion tracking
    @Query("UPDATE meal_plans SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun markMealAsCompleted(id: Long, completed: Boolean, completedAt: Long?)
    
    @Query("SELECT * FROM meal_plans WHERE isCompleted = 1 AND date BETWEEN :startDate AND :endDate")
    suspend fun getCompletedMealsInRange(startDate: String, endDate: String): List<MealPlan>
    
    // Shopping list integration
    @Query("UPDATE meal_plans SET addedToShoppingList = :added, shoppingListDate = :dateAdded WHERE id = :id")
    suspend fun updateShoppingListStatus(id: Long, added: Boolean, dateAdded: Long?)
    
    @Query("SELECT * FROM meal_plans WHERE addedToShoppingList = 1 ORDER BY shoppingListDate DESC")
    suspend fun getMealsAddedToShoppingList(): List<MealPlan>
    
    @Query("SELECT * FROM meal_plans WHERE addedToShoppingList = 0 AND ingredients IS NOT NULL")
    suspend fun getMealsNotInShoppingList(): List<MealPlan>
    
    // Statistics and analytics
    @Query("SELECT COUNT(*) FROM meal_plans WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getMealPlanCountForRange(startDate: String, endDate: String): Int
    
    @Query("SELECT COUNT(*) FROM meal_plans WHERE isCompleted = 1 AND date BETWEEN :startDate AND :endDate")
    suspend fun getCompletedMealCountForRange(startDate: String, endDate: String): Int
    
    @Query("SELECT AVG(estimatedCalories) FROM meal_plans WHERE date BETWEEN :startDate AND :endDate AND estimatedCalories > 0")
    suspend fun getAverageCaloriesForRange(startDate: String, endDate: String): Double?
    
    @Query("SELECT DISTINCT mealName FROM meal_plans ORDER BY mealName")
    suspend fun getAllUniqueMealNames(): List<String>
    
    @Query("SELECT DISTINCT tags FROM meal_plans WHERE tags IS NOT NULL")
    suspend fun getAllTags(): List<String>
    
    // Recent and popular meals
    @Query("SELECT mealName, COUNT(*) as usage_count FROM meal_plans GROUP BY mealName ORDER BY usage_count DESC LIMIT :limit")
    suspend fun getMostPopularMeals(limit: Int = 10): List<MealNameCount>
    
    @Query("SELECT * FROM meal_plans ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMealPlans(limit: Int = 20): List<MealPlan>
    
    // Batch operations
    @Query("UPDATE meal_plans SET sortOrder = :newOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, newOrder: Int)
    
    @Query("UPDATE meal_plans SET planWeek = :week WHERE date BETWEEN :startDate AND :endDate")
    suspend fun updateWeekForRange(startDate: String, endDate: String, week: String)
    
    // Cleanup operations
    @Query("DELETE FROM meal_plans WHERE createdAt < :cutoffTime AND isCompleted = 0")
    suspend fun cleanupOldUncompletedPlans(cutoffTime: Long)
}

/**
 * Data class for meal name count queries
 */
data class MealNameCount(
    val mealName: String,
    val usage_count: Int
)