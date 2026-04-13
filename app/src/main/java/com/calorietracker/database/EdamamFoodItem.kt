package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Edamam food item entity for Room database
 */
@Entity(tableName = "edamam_foods")
data class EdamamFoodItem(
    @PrimaryKey
    val foodId: String,
    
    // Basic info
    val label: String,
    val knownAs: String? = null,
    val category: String? = null,
    val categoryLabel: String? = null,
    val brand: String? = null,
    val foodContentsLabel: String? = null,
    val servingsPerContainer: Double? = null,
    val imageUrl: String? = null,
    
    // Nutrition per 100g
    val calories: Double? = null, // kcal
    val protein: Double? = null, // g
    val fat: Double? = null, // g
    val carbohydrates: Double? = null, // g
    val fiber: Double? = null, // g
    val sugar: Double? = null, // g
    val sodium: Double? = null, // mg
    val calcium: Double? = null, // mg
    val magnesium: Double? = null, // mg
    val potassium: Double? = null, // mg
    val iron: Double? = null, // mg
    val zinc: Double? = null, // mg
    val phosphorus: Double? = null, // mg
    val vitaminA: Double? = null, // µg
    val vitaminC: Double? = null, // mg
    val thiamin: Double? = null, // mg
    val riboflavin: Double? = null, // mg
    val niacin: Double? = null, // mg
    val vitaminB6: Double? = null, // mg
    val folate: Double? = null, // µg
    val vitaminB12: Double? = null, // µg
    val vitaminD: Double? = null, // µg
    val vitaminE: Double? = null, // mg
    val vitaminK: Double? = null, // µg
    val saturatedFat: Double? = null, // g
    val monounsaturatedFat: Double? = null, // g
    val polyunsaturatedFat: Double? = null, // g
    val transFat: Double? = null, // g
    val cholesterol: Double? = null, // mg
    val water: Double? = null, // g
    
    // Metadata
    val dateAdded: Long = System.currentTimeMillis(),
    val source: String = "Edamam"
)