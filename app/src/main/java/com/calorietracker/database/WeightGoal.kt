package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity representing a user's weight goal and timeline
 * 
 * This stores all the information needed to calculate personalized daily calorie recommendations:
 * - Current and target weights for goal calculation
 * - Timeline to determine safe rate of weight change
 * - Activity level for TDEE (Total Daily Energy Expenditure) calculation
 * - Optional demographics (age, height, gender) for precise BMR calculation using Mifflin-St Jeor equation
 * 
 * Only one weight goal is active at a time (enforced by primary key = 1)
 * When a new goal is set, it replaces the previous one
 */
@Entity(tableName = "weight_goals")
data class WeightGoal(
    @PrimaryKey val id: Int = 1, // Only one active goal at a time
    val currentWeight: Double, // Starting weight in kg or lbs
    val targetWeight: Double, // Goal weight in kg or lbs
    val targetDays: Int, // Number of days to reach goal
    val activityLevel: String, // "sedentary", "lightly_active", "moderately_active", "very_active", "extra_active"
    val age: Int? = null, // Optional for more accurate BMR calculation
    val height: Double? = null, // Height in cm or inches
    val gender: String? = null, // "male" or "female" for BMR calculation
    val createdDate: String, // Date goal was set (YYYY-MM-DD)
    val lastUpdated: Long = System.currentTimeMillis()
)