package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Daily weight entry for tracking weight progress over time
 */
@Entity(tableName = "daily_weight_entries")
data class DailyWeightEntry(
    @PrimaryKey val date: String, // Format: "yyyy-MM-dd"
    val weight: Double, // Weight in kg or lbs based on user preference
    val bodyFatPercentage: Double? = null,
    val muscleMass: Double? = null,
    val visceralFat: Int? = null,
    val waterPercentage: Double? = null,
    val bmi: Double? = null,
    val notes: String? = null,
    val dataSource: String = "manual", // "manual", "renpho", "healthconnect"
    val timestamp: Long = System.currentTimeMillis()
)