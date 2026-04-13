package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a planned meal for a specific date and meal type
 * Used for weekly meal planning and drag-drop interface
 */
@Entity(tableName = "meal_plans")
data class MealPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val date: String,                    // Date in yyyy-MM-dd format
    val mealType: String,               // "breakfast", "lunch", "dinner", "snack"
    val mealName: String,               // Name of the planned meal
    val description: String? = null,    // Optional description or notes
    val estimatedCalories: Int = 0,     // Estimated calories for planning
    val estimatedPrep: Int? = null,     // Prep time in minutes
    val difficulty: String? = null,     // "easy", "medium", "hard"
    val tags: String? = null,           // Comma-separated tags
    val ingredients: String? = null,    // JSON string of ingredients list
    val recipe: String? = null,         // Optional cooking instructions
    val servings: Int = 1,              // Number of servings
    val isCompleted: Boolean = false,   // Whether this meal was actually eaten
    val completedAt: Long? = null,      // When meal was marked as completed
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Shopping list integration
    val addedToShoppingList: Boolean = false,
    val shoppingListDate: Long? = null,
    
    // Recipe reference (if from recipe database)
    val recipeId: Long? = null,
    val isFromRecipe: Boolean = false,
    
    // Meal planning metadata
    val planWeek: String? = null,       // Week identifier (e.g., "2024-W01")
    val sortOrder: Int = 0,             // For custom ordering within meal types
    val color: String? = null,          // Optional color coding for UI
    val reminder: Boolean = false,      // Whether to set preparation reminders
    val reminderTime: String? = null    // Time to remind (HH:mm format)
)