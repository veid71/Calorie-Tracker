package com.calorietracker.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightEntryDao {
    
    @Query("SELECT * FROM weight_entries ORDER BY date DESC")
    fun getAllWeightEntries(): Flow<List<WeightEntry>>
    
    @Query("SELECT * FROM weight_entries ORDER BY date DESC")
    suspend fun getAllWeightEntriesSync(): List<WeightEntry>
    
    @Query("SELECT * FROM weight_entries WHERE date = :date")
    suspend fun getWeightForDate(date: String): WeightEntry?
    
    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeight(): WeightEntry?
    
    @Query("SELECT * FROM weight_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getWeightEntriesInRange(startDate: String, endDate: String): List<WeightEntry>
    
    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentWeightEntries(limit: Int): List<WeightEntry>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weightEntry: WeightEntry)
    
    @Update
    suspend fun updateWeight(weightEntry: WeightEntry)
    
    @Delete
    suspend fun deleteWeight(weightEntry: WeightEntry)
    
    @Query("DELETE FROM weight_entries WHERE date = :date")
    suspend fun deleteWeightForDate(date: String)
}