package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Water intake entry for tracking daily hydration
 */
@Entity(tableName = "water_intake_entries")
data class WaterIntakeEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // Format: "yyyy-MM-dd"
    val amount: Int, // Amount in ml
    val drinkType: String = "water", // "water", "tea", "coffee", "juice", etc.
    val timestamp: Long = System.currentTimeMillis()
)