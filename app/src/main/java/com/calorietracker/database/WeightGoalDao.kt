package com.calorietracker.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightGoalDao {
    
    @Query("SELECT * FROM weight_goals WHERE id = 1")
    fun getCurrentWeightGoal(): Flow<WeightGoal?>
    
    @Query("SELECT * FROM weight_goals WHERE id = 1")
    suspend fun getCurrentWeightGoalSync(): WeightGoal?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightGoal(weightGoal: WeightGoal)
    
    @Update
    suspend fun updateWeightGoal(weightGoal: WeightGoal)
    
    @Query("DELETE FROM weight_goals WHERE id = 1")
    suspend fun deleteCurrentWeightGoal()
}