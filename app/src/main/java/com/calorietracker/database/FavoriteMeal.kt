package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing user's favorite meals for quick access
 * These are foods that users frequently log and want to access quickly
 */
@Entity(tableName = "favorite_meals")
data class FavoriteMeal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val foodName: String,
    val brand: String? = null,
    val servingSize: String? = null,
    val calories: Int,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodium: Double? = null,
    val barcode: String? = null,
    
    // Frequency tracking
    val timesUsed: Int = 1,
    val lastUsed: Long = System.currentTimeMillis(),
    val dateAdded: Long = System.currentTimeMillis(),
    
    // Custom meal categorization
    val category: String? = null, // breakfast, lunch, dinner, snack
    val tags: String? = null // comma-separated tags like "protein,healthy,quick"
)