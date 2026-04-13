package com.calorietracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity representing a custom recipe created by the user.
 * 
 * This stores the basic recipe information like name, instructions, and serving details.
 * The actual ingredients are stored separately in RecipeIngredient entities to allow
 * for flexible ingredient lists and proper nutrition calculations.
 * 
 * @property id Unique identifier for this recipe
 * @property name User-friendly recipe name (e.g., "Mom's Chicken Casserole")
 * @property description Optional description or notes about the recipe
 * @property instructions Step-by-step cooking instructions
 * @property servings Number of servings this recipe makes (for nutrition per-serving calculation)
 * @property prepTime Preparation time in minutes (optional)
 * @property cookTime Cooking time in minutes (optional)
 * @property category Recipe category (e.g., "Dinner", "Dessert", "Breakfast")
 * @property createdBy Creator's name/identifier for sharing purposes
 * @property createdDate When this recipe was created (YYYY-MM-DD format)
 * @property lastModified Last modification timestamp
 * @property isShared Whether this recipe has been shared with others
 * @property shareId Unique identifier for sharing (generated when first shared)
 * @property isFavorite Whether this recipe is marked as favorite
 * @property timesUsed How many times this recipe has been logged as a meal
 * @property averageRating User rating for this recipe (1-5 stars, nullable)
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Unique identifier for database operations
    
    val name: String, // Recipe title displayed to user
    
    val description: String? = null, // Optional recipe description or notes
    
    val instructions: String? = null, // Step-by-step cooking instructions
    
    val servings: Int, // Number of servings (for per-serving nutrition calculation)
    
    // Time estimates for cooking planning
    val prepTime: Int? = null,  // Preparation time in minutes
    val cookTime: Int? = null,  // Cooking time in minutes
    
    // Organization and categorization
    val category: String? = null, // Recipe category (e.g., "Dinner", "Breakfast")
    
    // Sharing and collaboration features
    val createdBy: String? = null,     // Creator's name for sharing attribution
    val createdDate: String,           // Creation date (YYYY-MM-DD format)
    val lastModified: Long = System.currentTimeMillis(), // Last modification timestamp
    
    // Sharing system fields
    val isShared: Boolean = false,     // Whether recipe has been shared
    val shareId: String? = null,       // Unique sharing identifier (UUID)
    
    // User experience features
    val isFavorite: Boolean = false,   // User's favorite recipes
    val timesUsed: Int = 0,           // Usage frequency tracking
    val averageRating: Float? = null,  // User rating (1.0-5.0 stars)
    
    // Calculated nutrition totals (stored for performance, calculated from ingredients)
    val totalCalories: Int = 0,        // Total calories for entire recipe
    val totalProtein: Double = 0.0,    // Total protein in grams for entire recipe
    val totalCarbs: Double = 0.0,      // Total carbohydrates in grams for entire recipe
    val totalFat: Double = 0.0,        // Total fat in grams for entire recipe
    val totalFiber: Double = 0.0,      // Total fiber in grams for entire recipe
    val totalSugar: Double = 0.0,      // Total sugar in grams for entire recipe
    val totalSodium: Double = 0.0      // Total sodium in milligrams for entire recipe
)

/**
 * Extension functions for Recipe to provide per-serving nutrition calculations.
 * These divide the total nutrition values by the number of servings.
 */

/**
 * Calculate calories per serving.
 * @return Calories per serving, rounded to nearest whole number
 */
fun Recipe.getCaloriesPerServing(): Int = 
    if (servings > 0) (totalCalories / servings) else 0

/**
 * Calculate protein per serving.
 * @return Protein in grams per serving, rounded to 1 decimal place
 */
fun Recipe.getProteinPerServing(): Double = 
    if (servings > 0) (totalProtein / servings) else 0.0

/**
 * Calculate carbohydrates per serving.
 * @return Carbs in grams per serving, rounded to 1 decimal place
 */
fun Recipe.getCarbsPerServing(): Double = 
    if (servings > 0) (totalCarbs / servings) else 0.0

/**
 * Calculate fat per serving.
 * @return Fat in grams per serving, rounded to 1 decimal place
 */
fun Recipe.getFatPerServing(): Double = 
    if (servings > 0) (totalFat / servings) else 0.0

/**
 * Calculate fiber per serving.
 * @return Fiber in grams per serving, rounded to 1 decimal place
 */
fun Recipe.getFiberPerServing(): Double = 
    if (servings > 0) (totalFiber / servings) else 0.0

/**
 * Calculate sugar per serving.
 * @return Sugar in grams per serving, rounded to 1 decimal place
 */
fun Recipe.getSugarPerServing(): Double = 
    if (servings > 0) (totalSugar / servings) else 0.0

/**
 * Calculate sodium per serving.
 * @return Sodium in milligrams per serving, rounded to 1 decimal place
 */
fun Recipe.getSodiumPerServing(): Double = 
    if (servings > 0) (totalSodium / servings) else 0.0

/**
 * Get total cooking time (prep + cook time).
 * @return Total time in minutes, or null if either time is not specified
 */
fun Recipe.getTotalTime(): Int? = 
    if (prepTime != null && cookTime != null) prepTime + cookTime else null

/**
 * Check if this recipe has complete nutrition data.
 * @return True if the recipe has at least calories and one macronutrient
 */
fun Recipe.hasNutritionData(): Boolean = 
    totalCalories > 0 && (totalProtein > 0 || totalCarbs > 0 || totalFat > 0)

/**
 * Generate a shareable summary of this recipe.
 * @return Formatted string with recipe name, servings, and key nutrition facts
 */
fun Recipe.getShareableSummary(): String = buildString {
    append("📝 $name\n")
    append("👥 Makes $servings servings\n")
    append("🔥 ${getCaloriesPerServing()} calories per serving\n")
    if (hasNutritionData()) {
        append("📊 Per serving: ${getProteinPerServing()}g protein, ")
        append("${getCarbsPerServing()}g carbs, ${getFatPerServing()}g fat\n")
    }
    getTotalTime()?.let { append("⏱️ Total time: $it minutes\n") }
    if (!description.isNullOrBlank()) {
        append("💭 $description\n")
    }
}