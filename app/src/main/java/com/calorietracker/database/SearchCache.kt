package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entity for caching search results to improve offline search experience
 */
@Entity(
    tableName = "search_cache",
    indices = [
        Index(value = ["timestamp"], unique = false),
        Index(value = ["resultCount"], unique = false)
    ]
)
data class SearchCache(
    @PrimaryKey val query: String, // Normalized search query (lowercase, trimmed)
    val resultCount: Int, // Number of results found
    val resultBarcodes: String, // Comma-separated list of barcodes for results
    val timestamp: Long, // When this search was cached
    val source: String = "mixed", // Which APIs were used for this search
    val locale: String = "en_US" // Locale for language-specific searches
)