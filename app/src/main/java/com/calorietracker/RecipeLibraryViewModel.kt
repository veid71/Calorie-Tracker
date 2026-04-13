package com.calorietracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.calorietracker.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecipeLibraryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = CalorieDatabase.getDatabase(application)
    private val recipeDao = database.recipeDao()
    private val recipeIngredientDao = database.recipeIngredientDao()
    
    private val _recipes = MutableLiveData<List<Recipe>>()
    val recipes: LiveData<List<Recipe>> = _recipes
    
    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean> = _deleteResult
    
    private val _favoriteUpdateResult = MutableLiveData<Boolean>()
    val favoriteUpdateResult: LiveData<Boolean> = _favoriteUpdateResult
    
    private val _shareData = MutableLiveData<Pair<Recipe, List<RecipeIngredient>>?>()
    val shareData: LiveData<Pair<Recipe, List<RecipeIngredient>>?> = _shareData
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * Load all recipes from database
     */
    fun loadRecipes() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.postValue(true)
                val recipes = recipeDao.getAllRecipesList()
                _recipes.postValue(recipes)
            } catch (e: Exception) {
                _recipes.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
    
    /**
     * Load only favorite recipes
     */
    fun loadFavoriteRecipes() {
        // Since we don't have a suspend version, let's observe the LiveData
        recipeDao.getFavoriteRecipes().observeForever { recipes ->
            _recipes.value = recipes
        }
    }
    
    /**
     * Delete a recipe and its ingredients
     */
    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Delete ingredients first (foreign key constraint)
                recipeIngredientDao.deleteIngredientsForRecipe(recipe.id)
                // Delete the recipe
                recipeDao.deleteRecipe(recipe)
                
                _deleteResult.postValue(true)
            } catch (e: Exception) {
                _deleteResult.postValue(false)
            }
        }
    }
    
    /**
     * Toggle favorite status of a recipe
     */
    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedRecipe = recipe.copy(isFavorite = !recipe.isFavorite)
                recipeDao.updateRecipe(updatedRecipe)
                
                _favoriteUpdateResult.postValue(true)
            } catch (e: Exception) {
                _favoriteUpdateResult.postValue(false)
            }
        }
    }
    
    /**
     * Prepare recipe data for sharing
     */
    fun prepareRecipeForSharing(recipe: Recipe) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ingredients = recipeIngredientDao.getIngredientsForRecipeList(recipe.id)
                _shareData.postValue(recipe to ingredients)
            } catch (e: Exception) {
                // Handle error - could post null or empty data
                _shareData.postValue(null)
            }
        }
    }
    
    /**
     * Clear share data after use
     */
    fun clearShareData() {
        _shareData.value = null
    }
    
    /**
     * Search recipes by name or category
     */
    fun searchRecipes(query: String) {
        // Observe the search results
        recipeDao.searchRecipes(query).observeForever { recipes ->
            _recipes.value = recipes
        }
    }
    
    /**
     * Get recipes by category
     */
    fun getRecipesByCategory(category: String) {
        // Observe the category results
        recipeDao.getRecipesByCategory(category).observeForever { recipes ->
            _recipes.value = recipes
        }
    }
    
    /**
     * Get most used recipes
     */
    fun getMostUsedRecipes() {
        // Use the recently used recipes method
        recipeDao.getRecentlyUsedRecipes().observeForever { recipes ->
            _recipes.value = recipes
        }
    }
}