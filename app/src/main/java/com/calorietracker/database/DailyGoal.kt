package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoal(
    @PrimaryKey val id: Int = 1, // Single row table
    val calorieGoal: Int = 2000,
    val proteinGoal: Double? = null,
    val carbsGoal: Double? = null,
    val fatGoal: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)