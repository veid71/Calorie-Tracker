package com.calorietracker.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterIntakeEntryDao {
    
    @Query("SELECT * FROM water_intake_entries WHERE date = :date ORDER BY timestamp ASC")
    fun getWaterEntriesForDate(date: String): Flow<List<WaterIntakeEntry>>
    
    @Query("SELECT * FROM water_intake_entries WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getWaterEntriesForDateSync(date: String): List<WaterIntakeEntry>
    
    @Query("SELECT SUM(amount) FROM water_intake_entries WHERE date = :date")
    suspend fun getTotalWaterForDate(date: String): Int?
    
    @Query("SELECT SUM(amount) FROM water_intake_entries WHERE date = :date")
    fun getTotalWaterForDateFlow(date: String): Flow<Int?>
    
    @Query("SELECT date, SUM(amount) as total FROM water_intake_entries WHERE date BETWEEN :startDate AND :endDate GROUP BY date ORDER BY date DESC")
    suspend fun getWaterTotalsInRange(startDate: String, endDate: String): List<WaterDailyTotal>
    
    @Query("SELECT AVG(daily_total) FROM (SELECT SUM(amount) as daily_total FROM water_intake_entries WHERE date BETWEEN :startDate AND :endDate GROUP BY date)")
    suspend fun getAverageWaterIntake(startDate: String, endDate: String): Double?
    
    @Query("SELECT * FROM water_intake_entries ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentWaterEntries(limit: Int = 50): List<WaterIntakeEntry>
    
    @Insert
    suspend fun insertWaterEntry(entry: WaterIntakeEntry): Long
    
    @Insert
    suspend fun insertWaterEntries(entries: List<WaterIntakeEntry>)
    
    @Update
    suspend fun updateWaterEntry(entry: WaterIntakeEntry)
    
    @Delete
    suspend fun deleteWaterEntry(entry: WaterIntakeEntry)
    
    @Query("DELETE FROM water_intake_entries WHERE id = :id")
    suspend fun deleteWaterEntryById(id: Int)
    
    @Query("DELETE FROM water_intake_entries WHERE date = :date")
    suspend fun deleteWaterEntriesForDate(date: String)
    
    @Query("SELECT COUNT(*) FROM water_intake_entries WHERE date = :date")
    suspend fun getWaterEntryCountForDate(date: String): Int
    
    // Quick add methods for common amounts
    @Query("INSERT INTO water_intake_entries (date, amount, drinkType, timestamp) VALUES (:date, :amount, :drinkType, :timestamp)")
    suspend fun quickAddWater(date: String, amount: Int, drinkType: String = "water", timestamp: Long = System.currentTimeMillis())
}

// Data class for water daily totals
data class WaterDailyTotal(
    val date: String,
    val total: Int
)