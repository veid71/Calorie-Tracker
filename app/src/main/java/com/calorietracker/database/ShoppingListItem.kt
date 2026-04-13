package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a shopping list item generated from meal plans
 * Supports automatic generation from planned meals and manual additions
 */
@Entity(tableName = "shopping_list_items")
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val itemName: String,               // Name of the ingredient/item
    val category: String? = null,       // Category like "Produce", "Dairy", "Meat", etc.
    val quantity: String? = null,       // Quantity like "2 cups", "1 lb", "3 items"
    val unit: String? = null,           // Unit of measurement
    val notes: String? = null,          // Additional notes or brand preferences
    val estimatedCost: Double? = null,  // Estimated cost for budgeting
    val priority: Int = 0,              // Priority level (0=normal, 1=high, -1=low)
    val isChecked: Boolean = false,     // Whether item has been purchased
    val checkedAt: Long? = null,        // When item was checked off
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Meal plan integration
    val fromMealPlanId: Long? = null,   // Reference to originating meal plan
    val mealDate: String? = null,       // Date of the meal this item is for
    val mealType: String? = null,       // Breakfast, lunch, dinner, snack
    val servings: Int = 1,              // Number of servings needed
    
    // Shopping trip organization
    val shoppingTrip: String? = null,   // Group items by shopping trip
    val store: String? = null,          // Preferred store for this item
    val aisle: String? = null,          // Store aisle location
    val brand: String? = null,          // Preferred brand
    
    // Smart features
    val isRecurring: Boolean = false,   // Whether this is a recurring need
    val lastPurchased: Long? = null,    // When this item was last purchased
    val averageCost: Double? = null,    // Historical average cost
    val isStaple: Boolean = false,      // Whether this is a pantry staple
    val alternativeItems: String? = null, // JSON array of alternative products
    
    // User organization
    val color: String? = null,          // Color coding for organization
    val sortOrder: Int = 0              // Custom sort order within category
)