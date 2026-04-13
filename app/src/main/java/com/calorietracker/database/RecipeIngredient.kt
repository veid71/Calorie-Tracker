package com.calorietracker.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity representing an individual ingredient within a recipe.
 * 
 * This entity creates a one-to-many relationship between Recipe and ingredients,
 * allowing each recipe to have multiple ingredients with specific quantities
 * and units. The nutrition values are stored denormalized for performance.
 * 
 * @property id Unique identifier for this ingredient entry
 * @property recipeId Foreign key linking to the parent Recipe
 * @property ingredientName Name of the ingredient (e.g., "Chicken Breast", "All-Purpose Flour")
 * @property quantity Amount of this ingredient (e.g., 2.0 for "2 cups")
 * @property unit Unit of measurement (e.g., "cups", "tablespoons", "grams", "ounces")
 * @property calories Calories contributed by this ingredient amount
 * @property protein Protein in grams contributed by this ingredient
 * @property carbs Carbohydrates in grams contributed by this ingredient
 * @property fat Fat in grams contributed by this ingredient
 * @property fiber Fiber in grams contributed by this ingredient (optional)
 * @property sugar Sugar in grams contributed by this ingredient (optional)
 * @property sodium Sodium in milligrams contributed by this ingredient (optional)
 * @property foodItemId Optional reference to FoodItem if ingredient came from food database
 * @property barcode Optional barcode if ingredient was scanned
 * @property originalServingSize Original serving size from food database (for scaling calculations)
 * @property originalServingUnit Original serving unit from food database
 * @property notes Optional notes about this ingredient (e.g., "chopped", "cooked", "optional")
 * @property order Display order for this ingredient in the recipe (for UI sorting)
 * @property isOptional Whether this ingredient is optional in the recipe
 * @property substitutions Suggested substitutions for this ingredient (comma-separated)
 */
@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("recipeId"),
            onDelete = ForeignKey.CASCADE // Delete ingredients when recipe is deleted
        )
    ],
    indices = [
        Index(value = ["recipeId"]), // Index for efficient recipe ingredient queries
        Index(value = ["ingredientName"]) // Index for ingredient name searches
    ]
)
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Unique identifier for this ingredient entry
    
    val recipeId: Long, // Foreign key to parent Recipe
    
    // Ingredient identification and quantity
    val ingredientName: String, // Human-readable ingredient name
    val quantity: Double,       // Amount of ingredient (numeric portion)
    val unit: String,          // Unit of measurement (text portion)
    
    // Nutrition data for this specific ingredient amount
    val calories: Int,           // Calories contributed by this ingredient
    val protein: Double,         // Protein in grams
    val carbs: Double,          // Carbohydrates in grams  
    val fat: Double,            // Fat in grams
    val fiber: Double? = null,   // Fiber in grams (optional)
    val sugar: Double? = null,   // Sugar in grams (optional)
    val sodium: Double? = null,  // Sodium in milligrams (optional)
    
    // Source tracking for ingredient data
    val foodItemId: Long? = null,  // Reference to FoodItem if from database
    val barcode: String? = null,   // Barcode if ingredient was scanned
    
    // Serving size information for scaling calculations
    val originalServingSize: Double? = null, // Original serving size from food database
    val originalServingUnit: String? = null, // Original serving unit from food database
    
    // Recipe organization and presentation
    val notes: String? = null,           // Additional notes (e.g., "diced", "optional")
    val order: Int = 0,                  // Display order in ingredient list
    val isOptional: Boolean = false,     // Whether ingredient is optional
    val substitutions: String? = null    // Comma-separated list of substitutions
)

/**
 * Extension functions for RecipeIngredient to provide utility operations.
 */

/**
 * Get a formatted display string for this ingredient.
 * @return Human-readable ingredient description (e.g., "2 cups All-Purpose Flour")
 */
fun RecipeIngredient.getDisplayText(): String = buildString {
    // Format quantity nicely (avoid unnecessary decimals for whole numbers)
    val formattedQuantity = if (quantity == quantity.toInt().toDouble()) {
        quantity.toInt().toString()
    } else {
        String.format("%.1f", quantity)
    }
    
    append("$formattedQuantity $unit $ingredientName")
    
    // Add notes in parentheses if they exist
    if (!notes.isNullOrBlank()) {
        append(" ($notes)")
    }
    
    // Add optional indicator
    if (isOptional) {
        append(" (optional)")
    }
}

/**
 * Get nutrition summary for this ingredient.
 * @return Formatted nutrition string
 */
fun RecipeIngredient.getNutritionSummary(): String = buildString {
    append("${calories} cal")
    if (protein > 0) append(", ${String.format("%.1f", protein)}g protein")
    if (carbs > 0) append(", ${String.format("%.1f", carbs)}g carbs") 
    if (fat > 0) append(", ${String.format("%.1f", fat)}g fat")
}

/**
 * Calculate scaled nutrition for a different quantity.
 * @param newQuantity New quantity to scale to
 * @param newUnit New unit (must be compatible with original unit)
 * @return New RecipeIngredient with scaled nutrition values
 */
fun RecipeIngredient.scaleTo(newQuantity: Double, newUnit: String = unit): RecipeIngredient {
    // Calculate scaling factor
    val scaleFactor = newQuantity / quantity
    
    return copy(
        quantity = newQuantity,
        unit = newUnit,
        calories = (calories * scaleFactor).toInt(),
        protein = protein * scaleFactor,
        carbs = carbs * scaleFactor,
        fat = fat * scaleFactor,
        fiber = fiber?.times(scaleFactor),
        sugar = sugar?.times(scaleFactor),
        sodium = sodium?.times(scaleFactor)
    )
}

/**
 * Check if this ingredient has complete macronutrient data.
 * @return True if protein, carbs, and fat are all greater than 0
 */
fun RecipeIngredient.hasCompleteMacros(): Boolean = 
    protein > 0 && carbs > 0 && fat > 0

/**
 * Get the primary macronutrient for this ingredient.
 * @return "protein", "carbs", or "fat" based on which has the highest percentage
 */
fun RecipeIngredient.getPrimaryMacro(): String? {
    if (!hasCompleteMacros()) return null
    
    val proteinCals = protein * 4
    val carbCals = carbs * 4  
    val fatCals = fat * 9
    
    return when {
        proteinCals >= carbCals && proteinCals >= fatCals -> "protein"
        carbCals >= fatCals -> "carbs"
        else -> "fat"
    }
}

/**
 * Check if this ingredient is a common allergen.
 * @return True if ingredient name contains common allergen keywords
 */
fun RecipeIngredient.isCommonAllergen(): Boolean {
    val allergens = listOf(
        "milk", "egg", "peanut", "tree nut", "soy", "wheat", "fish", "shellfish",
        "dairy", "gluten", "lactose", "casein", "whey"
    )
    val nameLower = ingredientName.lowercase()
    return allergens.any { allergen -> nameLower.contains(allergen) }
}

/**
 * Get suggested substitutions as a list.
 * @return List of substitution options, empty if none specified
 */
fun RecipeIngredient.getSubstitutionsList(): List<String> = 
    substitutions?.split(",")?.map { it.trim() } ?: emptyList()