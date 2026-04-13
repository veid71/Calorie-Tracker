package com.calorietracker.database

import androidx.room.*
import androidx.lifecycle.LiveData

@Dao
interface DailyGoalDao {
    
    @Query("SELECT * FROM daily_goals WHERE id = 1")
    fun getDailyGoal(): LiveData<DailyGoal?>
    
    @Query("SELECT * FROM daily_goals WHERE id = 1")
    suspend fun getDailyGoalSync(): DailyGoal?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyGoal(goal: DailyGoal)
    
    @Update
    suspend fun updateDailyGoal(goal: DailyGoal)
}