package com.calorietracker.database

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Data Access Object (DAO) for Recipe-related database operations.
 * Provides methods for managing recipes, ingredients, and sharing functionality.
 */
@Dao
interface RecipeDao {
    
    // ================== Recipe CRUD Operations ==================
    
    /**
     * Insert a new recipe into the database.
     * @param recipe Recipe to insert
     * @return Row ID of the inserted recipe
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long
    
    /**
     * Update an existing recipe.
     * @param recipe Recipe to update
     */
    @Update
    suspend fun updateRecipe(recipe: Recipe)
    
    /**
     * Delete a recipe and all its ingredients (CASCADE).
     * @param recipe Recipe to delete
     */
    @Delete
    suspend fun deleteRecipe(recipe: Recipe)
    
    /**
     * Delete a recipe by ID.
     * @param recipeId ID of recipe to delete
     */
    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteRecipeById(recipeId: Long)
    
    // ================== Recipe Queries ==================
    
    /**
     * Get a recipe by ID.
     * @param recipeId Recipe ID to search for
     * @return Recipe if found, null otherwise
     */
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    suspend fun getRecipeById(recipeId: Long): Recipe?
    
    /**
     * Get all recipes ordered by last modified (newest first).
     * @return LiveData list of all recipes
     */
    @Query("SELECT * FROM recipes ORDER BY lastModified DESC")
    fun getAllRecipes(): LiveData<List<Recipe>>
    
    /**
     * Get all recipes as a list (not LiveData) for background operations.
     * @return List of all recipes
     */
    @Query("SELECT * FROM recipes ORDER BY lastModified DESC")
    suspend fun getAllRecipesList(): List<Recipe>
    
    /**
     * Get favorite recipes only.
     * @return LiveData list of favorite recipes
     */
    @Query("SELECT * FROM recipes WHERE isFavorite = 1 ORDER BY lastModified DESC")
    fun getFavoriteRecipes(): LiveData<List<Recipe>>
    
    /**
     * Search recipes by name or description.
     * @param searchQuery Search term (will be wrapped with % for LIKE query)
     * @return LiveData list of matching recipes
     */
    @Query("""
        SELECT * FROM recipes 
        WHERE name LIKE '%' || :searchQuery || '%' 
           OR description LIKE '%' || :searchQuery || '%'
        ORDER BY name ASC
    """)
    fun searchRecipes(searchQuery: String): LiveData<List<Recipe>>
    
    /**
     * Get recipes by category.
     * @param category Recipe category to filter by
     * @return LiveData list of recipes in the category
     */
    @Query("SELECT * FROM recipes WHERE category = :category ORDER BY name ASC")
    fun getRecipesByCategory(category: String): LiveData<List<Recipe>>
    
    /**
     * Get recently used recipes (sorted by usage frequency and last modified).
     * @param limit Number of recent recipes to return
     * @return LiveData list of recently used recipes
     */
    @Query("""
        SELECT * FROM recipes 
        WHERE timesUsed > 0 
        ORDER BY timesUsed DESC, lastModified DESC 
        LIMIT :limit
    """)
    fun getRecentlyUsedRecipes(limit: Int = 10): LiveData<List<Recipe>>
    
    /**
     * Get shared recipes only.
     * @return LiveData list of shared recipes
     */
    @Query("SELECT * FROM recipes WHERE isShared = 1 ORDER BY lastModified DESC")
    fun getSharedRecipes(): LiveData<List<Recipe>>
    
    /**
     * Get recipe by share ID for importing shared recipes.
     * @param shareId Unique sharing identifier
     * @return Recipe if found, null otherwise
     */
    @Query("SELECT * FROM recipes WHERE shareId = :shareId LIMIT 1")
    suspend fun getRecipeByShareId(shareId: String): Recipe?
    
    // ================== Recipe Statistics ==================
    
    /**
     * Get total number of recipes.
     * @return Total recipe count
     */
    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getRecipeCount(): Int
    
    /**
     * Get count of recipes by category.
     * @return List of category names with their counts
     */
    @Query("""
        SELECT category, COUNT(*) as count 
        FROM recipes 
        WHERE category IS NOT NULL 
        GROUP BY category 
        ORDER BY count DESC
    """)
    suspend fun getRecipeCountByCategory(): List<CategoryCount>
    
    /**
     * Get average rating for all recipes.
     * @return Average rating or null if no ratings exist
     */
    @Query("SELECT AVG(averageRating) FROM recipes WHERE averageRating IS NOT NULL")
    suspend fun getAverageRating(): Double?
    
    // ================== Recipe Actions ==================
    
    /**
     * Mark recipe as favorite or unfavorite.
     * @param recipeId Recipe ID to update
     * @param isFavorite New favorite status
     */
    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :recipeId")
    suspend fun setRecipeFavorite(recipeId: Long, isFavorite: Boolean)
    
    /**
     * Increment the usage count for a recipe.
     * @param recipeId Recipe ID to increment usage for
     */
    @Query("UPDATE recipes SET timesUsed = timesUsed + 1 WHERE id = :recipeId")
    suspend fun incrementRecipeUsage(recipeId: Long)
    
    /**
     * Update recipe rating.
     * @param recipeId Recipe ID to update
     * @param rating New rating (1.0-5.0)
     */
    @Query("UPDATE recipes SET averageRating = :rating WHERE id = :recipeId")
    suspend fun updateRecipeRating(recipeId: Long, rating: Float)
    
    /**
     * Update recipe sharing status and generate share ID.
     * @param recipeId Recipe ID to update
     * @param isShared Whether recipe is shared
     * @param shareId Unique sharing identifier (UUID)
     */
    @Query("UPDATE recipes SET isShared = :isShared, shareId = :shareId WHERE id = :recipeId")
    suspend fun updateRecipeSharing(recipeId: Long, isShared: Boolean, shareId: String?)
    
    /**
     * Update recipe nutrition totals (calculated from ingredients).
     * @param recipeId Recipe ID to update
     * @param calories Total calories
     * @param protein Total protein in grams
     * @param carbs Total carbohydrates in grams
     * @param fat Total fat in grams
     * @param fiber Total fiber in grams
     * @param sugar Total sugar in grams
     * @param sodium Total sodium in milligrams
     */
    @Query("""
        UPDATE recipes SET 
            totalCalories = :calories,
            totalProtein = :protein,
            totalCarbs = :carbs,
            totalFat = :fat,
            totalFiber = :fiber,
            totalSugar = :sugar,
            totalSodium = :sodium,
            lastModified = :lastModified
        WHERE id = :recipeId
    """)
    suspend fun updateRecipeNutrition(
        recipeId: Long,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double,
        sugar: Double,
        sodium: Double,
        lastModified: Long = System.currentTimeMillis()
    )
}

// CategoryCount moved to CommunityRecipe.kt to avoid redeclaration