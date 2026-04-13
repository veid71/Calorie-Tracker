package com.calorietracker.database

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * This is like a helper that talks to the database about nutrition goals
 * It knows how to save, load, and update the user's daily nutrition targets
 */
@Dao
interface NutritionGoalsDao {
    
    /**
     * Get the user's current nutrition goals (this updates automatically when goals change)
     * LiveData means the UI will automatically refresh when goals are updated
     */
    @Query("SELECT * FROM nutrition_goals WHERE id = 1")
    fun getNutritionGoals(): LiveData<NutritionGoals?>
    
    /**
     * Get the user's nutrition goals right now (not automatically updating)
     * This is useful when we need the goals immediately in background tasks
     */
    @Query("SELECT * FROM nutrition_goals WHERE id = 1")
    suspend fun getNutritionGoalsSync(): NutritionGoals?
    
    /**
     * Save or update the user's nutrition goals
     * If goals already exist, this replaces them with new ones
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNutritionGoals(goals: NutritionGoals)
    
    /**
     * Update just the region preference (like changing from US to UK foods)
     */
    @Query("UPDATE nutrition_goals SET selectedRegion = :region, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateRegion(region: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Update just the calorie goal (most common change users make)
     */
    @Query("UPDATE nutrition_goals SET calorieGoal = :calorieGoal, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateCalorieGoal(calorieGoal: Int, timestamp: Long = System.currentTimeMillis())
}