package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey val barcode: String,
    val name: String,
    val brand: String? = null,
    val servingSize: String? = null,
    val caloriesPerServing: Int,
    val proteinPerServing: Double? = null,
    val carbsPerServing: Double? = null,
    val fatPerServing: Double? = null,
    val fiberPerServing: Double? = null,
    val sugarPerServing: Double? = null,
    val sodiumPerServing: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)