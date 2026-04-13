package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking cache operations and metadata
 */
@Entity(tableName = "cache_metadata")
data class CacheMetadata(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operation: String, // "init", "clear", "sync", "pre_cache", etc.
    val timestamp: Long,
    val details: String? = null, // Additional information about the operation
    val cacheVersion: Int = 1, // Version of cache format
    val affectedRows: Int? = null // Number of rows affected by operation
)