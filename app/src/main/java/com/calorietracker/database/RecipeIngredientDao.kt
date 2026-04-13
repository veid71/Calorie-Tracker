package com.calorietracker.database

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Data Access Object (DAO) for RecipeIngredient-related database operations.
 * Provides methods for managing ingredients within recipes.
 */
@Dao
interface RecipeIngredientDao {
    
    // ================== RecipeIngredient CRUD Operations ==================
    
    /**
     * Insert a new recipe ingredient.
     * @param ingredient RecipeIngredient to insert
     * @return Row ID of the inserted ingredient
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: RecipeIngredient): Long
    
    /**
     * Insert multiple recipe ingredients.
     * @param ingredients List of RecipeIngredients to insert
     * @return List of row IDs for the inserted ingredients
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredient>): List<Long>
    
    /**
     * Update an existing recipe ingredient.
     * @param ingredient RecipeIngredient to update
     */
    @Update
    suspend fun updateIngredient(ingredient: RecipeIngredient)
    
    /**
     * Delete a recipe ingredient.
     * @param ingredient RecipeIngredient to delete
     */
    @Delete
    suspend fun deleteIngredient(ingredient: RecipeIngredient)
    
    /**
     * Delete ingredient by ID.
     * @param ingredientId ID of ingredient to delete
     */
    @Query("DELETE FROM recipe_ingredients WHERE id = :ingredientId")
    suspend fun deleteIngredientById(ingredientId: Long)
    
    /**
     * Delete all ingredients for a recipe.
     * @param recipeId Recipe ID whose ingredients to delete
     */
    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: Long)
    
    // ================== RecipeIngredient Queries ==================
    
    /**
     * Get ingredient by ID.
     * @param ingredientId Ingredient ID to search for
     * @return RecipeIngredient if found, null otherwise
     */
    @Query("SELECT * FROM recipe_ingredients WHERE id = :ingredientId")
    suspend fun getIngredientById(ingredientId: Long): RecipeIngredient?
    
    /**
     * Get all ingredients for a recipe, ordered by display order.
     * @param recipeId Recipe ID to get ingredients for
     * @return LiveData list of ingredients for the recipe
     */
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY `order` ASC, ingredientName ASC")
    fun getIngredientsForRecipe(recipeId: Long): LiveData<List<RecipeIngredient>>
    
    /**
     * Get all ingredients for a recipe as a list (not LiveData).
     * @param recipeId Recipe ID to get ingredients for
     * @return List of ingredients for the recipe
     */
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY `order` ASC, ingredientName ASC")
    suspend fun getIngredientsForRecipeList(recipeId: Long): List<RecipeIngredient>
    
    /**
     * Search ingredients across all recipes by name.
     * @param searchQuery Search term for ingredient names
     * @return LiveData list of matching ingredients
     */
    @Query("""
        SELECT * FROM recipe_ingredients 
        WHERE ingredientName LIKE '%' || :searchQuery || '%'
        ORDER BY ingredientName ASC
    """)
    fun searchIngredientsByName(searchQuery: String): LiveData<List<RecipeIngredient>>
    
    /**
     * Get optional ingredients for a recipe.
     * @param recipeId Recipe ID to get optional ingredients for
     * @return LiveData list of optional ingredients
     */
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId AND isOptional = 1 ORDER BY `order` ASC")
    fun getOptionalIngredientsForRecipe(recipeId: Long): LiveData<List<RecipeIngredient>>
    
    /**
     * Get required (non-optional) ingredients for a recipe.
     * @param recipeId Recipe ID to get required ingredients for
     * @return LiveData list of required ingredients
     */
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId AND isOptional = 0 ORDER BY `order` ASC")
    fun getRequiredIngredientsForRecipe(recipeId: Long): LiveData<List<RecipeIngredient>>
    
    /**
     * Find ingredients that might be allergens.
     * @param recipeId Recipe ID to check for allergens
     * @return List of ingredients that contain common allergen keywords
     */
    @Query("""
        SELECT * FROM recipe_ingredients 
        WHERE recipeId = :recipeId 
        AND (
            LOWER(ingredientName) LIKE '%milk%' OR
            LOWER(ingredientName) LIKE '%egg%' OR
            LOWER(ingredientName) LIKE '%peanut%' OR
            LOWER(ingredientName) LIKE '%tree nut%' OR
            LOWER(ingredientName) LIKE '%soy%' OR
            LOWER(ingredientName) LIKE '%wheat%' OR
            LOWER(ingredientName) LIKE '%fish%' OR
            LOWER(ingredientName) LIKE '%shellfish%' OR
            LOWER(ingredientName) LIKE '%dairy%' OR
            LOWER(ingredientName) LIKE '%gluten%'
        )
        ORDER BY ingredientName ASC
    """)
    suspend fun getPotentialAllergensForRecipe(recipeId: Long): List<RecipeIngredient>
    
    // ================== Recipe Ingredient Statistics ==================
    
    /**
     * Get ingredient count for a recipe.
     * @param recipeId Recipe ID to count ingredients for
     * @return Number of ingredients in the recipe
     */
    @Query("SELECT COUNT(*) FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun getIngredientCountForRecipe(recipeId: Long): Int
    
    /**
     * Get total nutrition for a recipe by summing all ingredients.
     * @param recipeId Recipe ID to calculate nutrition for
     * @return RecipeNutritionSummary with totals
     */
    @Query("""
        SELECT 
            SUM(calories) as totalCalories,
            SUM(protein) as totalProtein,
            SUM(carbs) as totalCarbs,
            SUM(fat) as totalFat,
            SUM(COALESCE(fiber, 0)) as totalFiber,
            SUM(COALESCE(sugar, 0)) as totalSugar,
            SUM(COALESCE(sodium, 0)) as totalSodium
        FROM recipe_ingredients 
        WHERE recipeId = :recipeId
    """)
    suspend fun getRecipeNutritionSummary(recipeId: Long): RecipeNutritionSummary?
    
    /**
     * Get the most commonly used ingredients across all recipes.
     * @param limit Number of top ingredients to return
     * @return List of ingredient names with usage counts
     */
    @Query("""
        SELECT ingredientName, COUNT(*) as usageCount
        FROM recipe_ingredients
        GROUP BY LOWER(ingredientName)
        ORDER BY usageCount DESC
        LIMIT :limit
    """)
    suspend fun getMostUsedIngredients(limit: Int = 20): List<IngredientUsage>
    
    /**
     * Get unique ingredient names for autocomplete/suggestions.
     * @return List of unique ingredient names
     */
    @Query("""
        SELECT DISTINCT ingredientName 
        FROM recipe_ingredients 
        ORDER BY ingredientName ASC
    """)
    suspend fun getUniqueIngredientNames(): List<String>
    
    /**
     * Get unique units used across all ingredients.
     * @return List of unique units for dropdown/suggestions
     */
    @Query("""
        SELECT DISTINCT unit 
        FROM recipe_ingredients 
        WHERE unit IS NOT NULL AND unit != ''
        ORDER BY unit ASC
    """)
    suspend fun getUniqueUnits(): List<String>
    
    // ================== Recipe Ingredient Actions ==================
    
    /**
     * Update ingredient order for recipe organization.
     * @param ingredientId Ingredient ID to update
     * @param newOrder New display order
     */
    @Query("UPDATE recipe_ingredients SET `order` = :newOrder WHERE id = :ingredientId")
    suspend fun updateIngredientOrder(ingredientId: Long, newOrder: Int)
    
    /**
     * Update ingredient quantities for recipe scaling.
     * @param ingredientId Ingredient ID to update
     * @param newQuantity New quantity amount
     * @param newUnit New unit (optional)
     */
    @Query("""
        UPDATE recipe_ingredients 
        SET quantity = :newQuantity, unit = COALESCE(:newUnit, unit)
        WHERE id = :ingredientId
    """)
    suspend fun updateIngredientQuantity(ingredientId: Long, newQuantity: Double, newUnit: String?)
    
    /**
     * Mark ingredient as optional or required.
     * @param ingredientId Ingredient ID to update
     * @param isOptional New optional status
     */
    @Query("UPDATE recipe_ingredients SET isOptional = :isOptional WHERE id = :ingredientId")
    suspend fun setIngredientOptional(ingredientId: Long, isOptional: Boolean)
    
    /**
     * Update ingredient notes.
     * @param ingredientId Ingredient ID to update
     * @param notes New notes text
     */
    @Query("UPDATE recipe_ingredients SET notes = :notes WHERE id = :ingredientId")
    suspend fun updateIngredientNotes(ingredientId: Long, notes: String?)
    
    /**
     * Update ingredient substitutions.
     * @param ingredientId Ingredient ID to update
     * @param substitutions Comma-separated list of substitutions
     */
    @Query("UPDATE recipe_ingredients SET substitutions = :substitutions WHERE id = :ingredientId")
    suspend fun updateIngredientSubstitutions(ingredientId: Long, substitutions: String?)
    
    /**
     * Reorder all ingredients for a recipe.
     * Useful for drag-and-drop reordering in UI.
     * @param ingredientOrders Map of ingredient IDs to their new order positions
     */
    @Transaction
    suspend fun reorderIngredients(ingredientOrders: Map<Long, Int>) {
        ingredientOrders.forEach { (ingredientId, order) ->
            updateIngredientOrder(ingredientId, order)
        }
    }
}

/**
 * Data class for recipe nutrition summary calculations.
 */
data class RecipeNutritionSummary(
    val totalCalories: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val totalFiber: Double,
    val totalSugar: Double,
    val totalSodium: Double
)

/**
 * Data class for ingredient usage statistics.
 */
data class IngredientUsage(
    val ingredientName: String,
    val usageCount: Int
)