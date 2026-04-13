package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity for comprehensive offline food caching
 * Stores complete food data for offline functionality
 */
@Entity(
    tableName = "offline_foods",
    indices = [
        Index(value = ["name"], unique = false),
        Index(value = ["barcode"], unique = false),
        Index(value = ["brand"], unique = false),
        Index(value = ["lastUpdated"], unique = false)
    ]
)
data class OfflineFood(
    @PrimaryKey val barcode: String,
    val name: String,
    val calories: Int,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodium: Double? = null,
    val servingSize: String? = null,
    val servingUnit: String? = null,
    val brand: String? = null,
    val categories: String? = null, // Comma-separated categories
    val ingredients: String? = null,
    val allergens: String? = null, // Comma-separated allergens
    val source: String, // "USDA", "Open Food Facts", "Nutritionix", etc.
    val lastUpdated: Long,
    val popularity: Int = 0, // Track usage for cache prioritization
    val isVerified: Boolean = false // Mark as manually verified by user
)