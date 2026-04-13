package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity for tracking barcode scanning history
 * Stores recently scanned items for quick re-access
 */
@Entity(
    tableName = "barcode_history",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class BarcodeHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val barcode: String,
    val foodName: String,
    val brand: String? = null,
    val calories: Int,
    val servingSize: String? = null,
    
    // Tracking info
    val firstScanned: Long = System.currentTimeMillis(),
    val lastScanned: Long = System.currentTimeMillis(),
    val timesScanned: Int = 1,
    
    // Success/failure tracking
    val wasSuccessful: Boolean = true, // Whether the scan found a product
    val source: String? = null, // "cache", "offline_openfoodfacts", "online_api", etc.
    
    // Quick access data
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodium: Double? = null
)