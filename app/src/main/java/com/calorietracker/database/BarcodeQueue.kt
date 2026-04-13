package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcode_queue")
data class BarcodeQueue(
    @PrimaryKey val barcode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)