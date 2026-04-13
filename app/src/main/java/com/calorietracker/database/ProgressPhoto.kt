package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a progress photo taken by the user
 * Used to track visual progress over time for motivation and accountability
 */
@Entity(tableName = "progress_photos")
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val photoPath: String,            // Path to the saved photo file
    val thumbnailPath: String?,       // Optional smaller thumbnail for performance
    val date: String,                 // Date photo was taken (yyyy-MM-dd)
    val timestamp: Long = System.currentTimeMillis(), // When record was created
    val weight: Double? = null,       // Optional weight at time of photo
    val bodyFatPercentage: Double? = null, // Optional body fat % if available
    val notes: String? = null,        // User notes about progress, feelings, etc.
    val tags: String? = null,         // Comma-separated tags like "front,gym,morning"
    val isVisible: Boolean = true,    // Allow users to hide photos without deleting
    val reminderSet: Boolean = false, // Whether user has set reminders for this type
    val photoType: String = "progress", // Type: "progress", "before", "after", "milestone"
    val moodRating: Int? = null,      // 1-5 rating of how user felt about their progress
    val reminderFrequency: String? = null // "weekly", "monthly", "custom" for reminder frequency
)