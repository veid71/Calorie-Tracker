package com.calorietracker.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDatabaseStatusDao {
    
    @Query("SELECT * FROM food_database_status WHERE databaseName = :databaseName")
    suspend fun getStatus(databaseName: String): FoodDatabaseStatus?
    
    @Query("SELECT * FROM food_database_status WHERE databaseName = :databaseName")
    fun getStatusFlow(databaseName: String): Flow<FoodDatabaseStatus?>
    
    @Query("SELECT * FROM food_database_status")
    suspend fun getAllStatuses(): List<FoodDatabaseStatus>
    
    @Query("SELECT * FROM food_database_status")
    fun getAllStatusesFlow(): Flow<List<FoodDatabaseStatus>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: FoodDatabaseStatus)
    
    @Update
    suspend fun updateStatus(status: FoodDatabaseStatus)
    
    @Query("DELETE FROM food_database_status WHERE databaseName = :databaseName")
    suspend fun deleteStatus(databaseName: String)
    
    @Query("DELETE FROM food_database_status")
    suspend fun deleteAll()
}