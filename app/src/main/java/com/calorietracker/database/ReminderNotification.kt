package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Notification reminder settings for various app activities
 */
@Entity(tableName = "reminder_notifications")
data class ReminderNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "weight", "breakfast", "lunch", "dinner", "snack", "water"
    val title: String,
    val message: String,
    val hour: Int, // 24-hour format
    val minute: Int,
    val daysOfWeek: String, // Comma-separated: "1,2,3,4,5,6,7" (Sunday=1)
    val isEnabled: Boolean = true,
    val lastTriggered: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)