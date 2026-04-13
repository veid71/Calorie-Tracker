package com.calorietracker.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyWeightEntryDao {
    
    @Query("SELECT * FROM daily_weight_entries ORDER BY date DESC")
    fun getAllWeightEntries(): Flow<List<DailyWeightEntry>>
    
    @Query("SELECT * FROM daily_weight_entries WHERE date = :date")
    suspend fun getWeightEntryByDate(date: String): DailyWeightEntry?
    
    @Query("SELECT * FROM daily_weight_entries ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentWeightEntries(limit: Int = 30): List<DailyWeightEntry>
    
    @Query("SELECT * FROM daily_weight_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getWeightEntriesInRange(startDate: String, endDate: String): List<DailyWeightEntry>
    
    @Query("SELECT * FROM daily_weight_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeightEntry(): DailyWeightEntry?
    
    @Query("SELECT * FROM daily_weight_entries ORDER BY date DESC LIMIT 1")
    fun getLatestWeightEntryFlow(): Flow<DailyWeightEntry?>
    
    @Query("SELECT AVG(weight) FROM daily_weight_entries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAverageWeight(startDate: String, endDate: String): Double?
    
    @Query("SELECT weight FROM daily_weight_entries ORDER BY date DESC LIMIT 2")
    suspend fun getLastTwoWeights(): List<Double>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntry(entry: DailyWeightEntry)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntries(entries: List<DailyWeightEntry>)
    
    @Update
    suspend fun updateWeightEntry(entry: DailyWeightEntry)
    
    @Delete
    suspend fun deleteWeightEntry(entry: DailyWeightEntry)
    
    @Query("DELETE FROM daily_weight_entries WHERE date = :date")
    suspend fun deleteWeightEntryByDate(date: String)
    
    @Query("SELECT COUNT(*) FROM daily_weight_entries")
    suspend fun getWeightEntryCount(): Int
    
    // Get weight trend (positive = gaining, negative = losing, 0 = stable)
    @Query("""
        SELECT 
            CASE 
                WHEN COUNT(*) < 2 THEN 0
                ELSE (MAX(weight) - MIN(weight)) / (COUNT(*) - 1)
            END as trend
        FROM (
            SELECT weight FROM daily_weight_entries 
            ORDER BY date DESC 
            LIMIT :days
        )
    """)
    suspend fun getWeightTrend(days: Int = 7): Double?
}