package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Open Food Facts food item entity
 * Contains barcode-linked packaged food data from Open Food Facts database
 */
@Entity(
    tableName = "openfoodfacts_items",
    indices = [Index(value = ["productName"], unique = false), Index(value = ["barcode"], unique = true)]
)
data class OpenFoodFactsItem(
    @PrimaryKey val id: String, // Use barcode as primary key
    val barcode: String,
    val productName: String,
    val brands: String? = null,
    val categories: String? = null,
    val labels: String? = null,
    val countries: String? = null,
    val ingredients: String? = null,
    val allergens: String? = null,
    
    // Serving info
    val servingSize: String? = null,
    val servingQuantity: Double? = null,
    
    // Nutrition per 100g
    val energyKj: Double? = null,
    val energyKcal: Double? = null,
    val fat: Double? = null,
    val saturatedFat: Double? = null,
    val carbohydrates: Double? = null,
    val sugars: Double? = null,
    val fiber: Double? = null,
    val proteins: Double? = null,
    val salt: Double? = null,
    val sodium: Double? = null,
    
    // Product info
    val imageUrl: String? = null,
    val productUrl: String? = null,
    val nutritionGrade: String? = null, // Nutri-Score
    val novaGroup: Int? = null, // NOVA classification
    
    // Quality indicators
    val completeness: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)