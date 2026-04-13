package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * USDA FoodData Central food item entity
 * Contains comprehensive nutrition data from the USDA database
 */
@Entity(
    tableName = "usda_food_items",
    indices = [Index(value = ["description"], unique = false)]
)
data class USDAFoodItem(
    @PrimaryKey val fdcId: Int,
    val description: String,
    val dataType: String,
    val publicationDate: String? = null,
    val brandOwner: String? = null,
    val brandName: String? = null,
    val ingredients: String? = null,
    val servingSize: Double? = null,
    val servingSizeUnit: String? = null,
    val householdServingFullText: String? = null,
    
    // Nutrition per 100g
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbohydrates: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val sodium: Double = 0.0,
    val calcium: Double? = null,
    val iron: Double? = null,
    val vitaminC: Double? = null,
    val cholesterol: Double? = null,
    val saturatedFat: Double? = null,
    val transFat: Double? = null,
    
    // Metadata
    val foodCategory: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)