package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the status of food database downloads and updates
 */
@Entity(tableName = "food_database_status")
data class FoodDatabaseStatus(
    @PrimaryKey val databaseName: String, // "usda" or "openfoodfacts"
    val lastDownloadDate: Long? = null,
    val totalItems: Int = 0,
    val downloadedItems: Int = 0,
    val isDownloading: Boolean = false,
    val isComplete: Boolean = false,
    val version: String? = null,
    val errorMessage: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)